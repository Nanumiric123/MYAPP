package com.example.myapplication

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import java.security.MessageDigest

data class PO_DATA(var material: String = "",var quantity:Int)

class P03 : AppCompatActivity() {
    private lateinit var btn_save:Button
    private lateinit var po_numtv:TextView
    private lateinit var matTV:TextView
    private lateinit var qty_s:EditText
    private lateinit var sloc:EditText
    private lateinit var barcode_ed:EditText
    private lateinit var c:Context
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_p03)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val btn_menu:Button = findViewById(R.id.P03_btn_Menu)
        val clrBtn:Button = findViewById(R.id.btn_p03_clear)
        qty_s = findViewById<EditText>(R.id.p03_ed_qty)
        sloc = findViewById<EditText>(R.id.P03_et_stor_loc)
        val pb:ProgressBar = findViewById(R.id.p03_progress_bar)
        matTV = findViewById(R.id.p03_tv_material)
        btn_menu.setOnClickListener {
            this.finish()
        }
        c = this@P03
        barcode_ed = findViewById(R.id.ed_barcode_p03)
        barcode_ed.requestFocus()
        po_numtv = findViewById<TextView>(R.id.P03_tv_po_num)
        val Bnum = intent.getStringExtra("Badge").toString()

        barcode_ed.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                executeget_order_Details(barcode_ed.text.toString(),pb)
                return@OnKeyListener true
            }
            false
        })

        sloc.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                qty_s.requestFocus()


                return@OnKeyListener true
            }
            false
        })

        var deviceID = getDeviceUniqueId(c)
        btn_save = findViewById<Button>(R.id.p03_btn_Save)
        btn_save.setOnClickListener{

            if(sloc.text.toString().trim() != ""){
                executesubmitDoc(barcode_ed.text.toString(),matTV.text.toString(),qty_s.text.toString(),Bnum,deviceID,sloc.text.toString(),pb)
            }
            else{
                runOnUiThread(kotlinx.coroutines.Runnable {
                    val alertDialogBuilder = AlertDialog.Builder(this)
                    alertDialogBuilder.setTitle(R.string.ErrorTitle)
                    alertDialogBuilder.setMessage(R.string.ErrorMessage1)
                    alertDialogBuilder.show()

                })
            }

        }

        clrBtn.setOnClickListener{

            val tv_mat:TextView = findViewById(R.id.p03_tv_material)
            val tvPO:TextView = findViewById(R.id.P03_tv_po_num)
            val qty_s:EditText = findViewById(R.id.p03_ed_qty)
            val sloc:EditText = findViewById(R.id.P03_et_stor_loc)
            val tv_uom:TextView = findViewById(R.id.p03_tv_uom)

            tv_uom.text = "";
            tvPO.text = ""
            tv_mat.text = ""
            qty_s.text.clear()
            sloc.text.clear()

        }

    }
    private fun getDeviceUniqueId(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val serialNumber = Build.SERIAL

        // Concatenate the two IDs and hash them to create a unique identifier
        val combinedId = "$androidId$serialNumber"
        return hashString(combinedId)
    }
    private fun hashString(input: String): String {
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("", { str, it -> str + "%02x".format(it) })
    }

    private fun get_order_detail(po_num:String?):String? {
        if(po_num.isNullOrEmpty()){
            throw IllegalArgumentException("Scanned PO Number first")
        }
        val url = URL("http://172.16.206.19/REST_API/Home/GET_ORDER_DETAIL_MPP?order=$po_num")
        return try{
            val testResult = url.readText()
            testResult
        }catch (e: IOException) {
            e.printStackTrace().toString()
            null
        }

    }

    private fun executeget_order_Details(po_num:String?,progressBar:View){
        CoroutineScope(Dispatchers.Main).launch {
            try{
                // Show the progress bar and disable the button
                setProgressBar(progressBar, true)
                // Perform the network request in the IO dispatcher
                val result = withContext(Dispatchers.IO) {
                    get_order_detail(po_num)
                }
                // Hide the progress bar and enable the button
                setProgressBar(progressBar, false)
                // Handle the result
                result?.let { jsonObject ->
                    // Process the JSONObject, e.g., update the UI with the order details
                    // Example: val orderId = jsonObject.getString("orderId")
                    try{
                        matTV.setText(JSONObject(jsonObject).getString("MATERIAL"))
                        sloc.requestFocus()
                    }
                    catch (ex:Exception){
                        Toast.makeText(progressBar.context, ex.message.toString(), Toast.LENGTH_SHORT).show()
                    }

                } ?: run {
                    // Handle the error, e.g., show an error message
                    Toast.makeText(progressBar.context, "Wrong PO", Toast.LENGTH_SHORT).show()
                }
            }
            catch (e: IllegalArgumentException) {
                // Hide the progress bar
                setProgressBar(progressBar, false)

                // Show a toast message with the error
                Toast.makeText(progressBar.context, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun setProgressBar(v: View, show: Boolean) {
        if (show) {
            v.visibility = View.VISIBLE
            btn_save.isEnabled = false
        } else {
            v.visibility = View.GONE
            btn_save.isEnabled = true
        }
    }

    private fun submitDoc(productionOrder:String?,materialNumber:String?,quantity:String?,badgeNumber:String?,deviceName:String?,
    toBin:String?):String?{
        if(productionOrder.isNullOrEmpty() || materialNumber.isNullOrEmpty() || quantity.isNullOrEmpty() ||
            badgeNumber.isNullOrEmpty() || deviceName.isNullOrEmpty() || toBin.isNullOrEmpty()){
            throw IllegalArgumentException("Maklumat tak lengkap")
        }

        val linkString = "http://172.16.206.19/REST_API/Home/MPP_P03?order=${productionOrder}" +
                "&mat=$materialNumber&batc=${productionOrder}&qty=$quantity&badge_n=$badgeNumber&assetNo=$deviceName&toBin=$toBin"

        return try{
            val url = URL(linkString)
            url.readText()  // Fetch the response as a String
        }
        catch(e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun executesubmitDoc(productionOrder:String?,materialNumber:String?,quantity:String?,badgeNumber:String?,deviceName:String?,
                                 toBin:String?,progressBar: View) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Show the progress bar and disable the button
                setProgressBar(progressBar, true)

                // Perform the network request in the IO dispatcher
                val result = withContext(Dispatchers.IO) {
                    submitDoc(productionOrder,materialNumber,quantity,badgeNumber,deviceName,toBin)
                }

                // Hide the progress bar and enable the button
                setProgressBar(progressBar, false)

                // Handle the result as a String
                result?.let { responseString ->
                    var resultSplit = responseString.split(':')
                    // Show the result in an AlertDialog
                    if(resultSplit.count() > 1){
                        triggerAlert(resultSplit[0],resultSplit[1])
                    }
                    else{
                        triggerAlert("ERROR",responseString)
                    }

                } ?: run {
                    // Handle the error, e.g., show an error message
                    Toast.makeText(progressBar.context, "Failed to submit data", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IllegalArgumentException) {
                // Hide the progress bar
                setProgressBar(progressBar, false)

                // Show a toast message with the error
                Toast.makeText(progressBar.context, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun triggerAlert(title:String,msg:String){
        val builder = AlertDialog.Builder(c)
        builder.setTitle(title)
        builder.setMessage(msg)
        builder.setPositiveButton("OK") { dialog, which ->
            // Do something when OK button is clicked
            barcode_ed.text.clear()
            matTV.text = ""
            sloc.text.clear()
            qty_s.text.clear()
            dialog.dismiss()
        }
        builder.show()
    }

}