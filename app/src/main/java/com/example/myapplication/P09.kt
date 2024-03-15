package com.example.myapplication

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

class P09 : AppCompatActivity() {
    private lateinit var p09_barcode_txt:EditText
    private lateinit var pallet_info_p09: TextView
    private lateinit var btn_save_p09: Button
    var material: String? = null
    var quantity: String? = null
    private lateinit var pb3: ProgressBar
    var g_b_num: String? = null
    private lateinit var c:Context
    private var isButtonClickable = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_p09)
        val intent = intent
        val b_num = intent.getStringExtra("Badge").toString()
        g_b_num = b_num
        pb3 = findViewById(R.id.P09BAR)
        pb3.visibility = View.INVISIBLE
        p09_barcode_txt = findViewById(R.id.txt_barcode_p09)
        pallet_info_p09 = findViewById(R.id.txt_result_p09)
        btn_save_p09 = findViewById(R.id.btn_save_p09)
        p09_barcode_txt.requestFocus()
        c = this
        var barcodeResult = palletLabel(MATERIAL = "",PALLET_SEQUANCE = "",LOT_NO = "",QUANTITY="",PALLET_ID = "",CARTON_QTY = "")

        p09_barcode_txt.setOnKeyListener { view, i, keyEvent ->
            if (keyEvent.keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_UP ||
                keyEvent.keyCode == KeyEvent.KEYCODE_TAB && keyEvent.action == KeyEvent.ACTION_UP ||
                keyEvent.keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_DOWN ||
                keyEvent.keyCode == KeyEvent.KEYCODE_TAB && keyEvent.action == KeyEvent.ACTION_DOWN) {

                barcodeResult = breakupSCANNEDTEXT(p09_barcode_txt.text.toString())
                pallet_info_p09.text = "Material : ${barcodeResult.MATERIAL} \n Quantity : ${barcodeResult.QUANTITY} \n Pallet ID : ${barcodeResult.PALLET_ID}"

                return@setOnKeyListener true
            }
            false
        }
        btn_save_p09.setOnClickListener {

            if(isButtonClickable){
                //Disable The button to prevent multiple clicks
                isButtonClickable = false
                btn_save_p09.isEnabled = false

                //perform action
                if(barcodeResult.MATERIAL.isNotBlank() && barcodeResult.PALLET_ID.isNotBlank() && barcodeResult.LOT_NO.isNotBlank()
                    && barcodeResult.QUANTITY.isNotBlank()){
                    fetchData(barcodeResult)
                    barcodeResult = palletLabel(MATERIAL = "",PALLET_SEQUANCE = "",LOT_NO = "",QUANTITY="",PALLET_ID = "",CARTON_QTY = "")
                }
                else{
                    runOnUiThread {
                        val builder = AlertDialog.Builder(c)
                        builder.setTitle("Error")
                        builder.setMessage("Scan pallet label first!!")
                        builder.setPositiveButton("OK") { dialog, which ->
                            // Do something when OK button is clicked
                            p09_barcode_txt.text.clear()
                            p09_barcode_txt.requestFocus()
                            pallet_info_p09.text = ""
                            isButtonClickable = true
                            btn_save_p09.isEnabled = true
                        }
                        builder.show()
                    }
                }

            }
        }
    }

    private fun fetchData(bcLabelInfo: palletLabel) {
        setProgressBar(pb3)
        MainScope().launch(Dispatchers.IO) {
            var message = String()
            var msgTitle = String()
            try {
                val modelName = Build.ID
                val url = "http://172.16.206.19/REST_API/Home/MPP_PACKING?material=${bcLabelInfo.MATERIAL}&quantity=${bcLabelInfo.QUANTITY}&lot_no=${bcLabelInfo.LOT_NO}&badge_num=${g_b_num}_${modelName}&pallet_id=${bcLabelInfo.PALLET_ID}"
                val k = JSONArray(URL(url).readText())
                msgTitle = if(k[1].toString().contains("E")){
                    "Error"
                } else{
                    "Success"
                }
                message = k[0].toString()

            } catch (e: Exception) {
                // Handle other errors
                e.printStackTrace()
                msgTitle = "Error"
                message = e.message.toString()
            }
            finally {
                setProgressBar(pb3)

                runOnUiThread {
                    isButtonClickable = true
                    btn_save_p09.isEnabled = true
                    val builder = AlertDialog.Builder(c)
                    builder.setTitle(msgTitle)
                    builder.setMessage(message)
                    builder.setPositiveButton("OK") { dialog, which ->
                        // Do something when OK button is clicked
                        p09_barcode_txt.text.clear()
                        p09_barcode_txt.requestFocus()
                        pallet_info_p09.text = ""
                    }
                    builder.setNegativeButton("Back") { dialog, which ->
                        // Do something when OK button is clicked
                        finish()
                    }
                    builder.show()
                }

            }
        }
    }

    private fun setProgressBar(v:View){
        runOnUiThread(kotlinx.coroutines.Runnable {
            if (v.visibility == View.VISIBLE) {
                v.visibility = View.INVISIBLE
            } else {
                v.visibility = View.VISIBLE
            }

        })
    }

    private fun breakupSCANNEDTEXT(bc:String): palletLabel {
        val url = URL("http://172.16.206.19/REST_API/Home/breakpalletbarcodeString?barcode=$bc")
        var jsonString = JSONObject()
        var result = palletLabel(MATERIAL = "",PALLET_SEQUANCE = "",LOT_NO = "",QUANTITY="",PALLET_ID = "",CARTON_QTY = "")
        setProgressBar(pb3)
        var resultFromLink = String()
        var duplicates = false
        runBlocking {

            val job = MainScope().launch(Dispatchers.IO) {
                resultFromLink = url.readText()
            }
            job.join()
            if(resultFromLink.contains("DUPLICATE")){
                runOnUiThread {
                    val builder = AlertDialog.Builder(c)
                    builder.setTitle(R.string.Warning)
                    builder.setMessage("Duplicate Pallet Scanned")
                    builder.setPositiveButton("OK") { dialog, which ->
                        // Do something when OK button is clicked
                        p09_barcode_txt.text.clear()
                        p09_barcode_txt.requestFocus()
                        pallet_info_p09.text = ""
                    }
                    builder.show()
                }

            }
            else if(resultFromLink.contains("ERROR")){
                runOnUiThread {
                    val builder = AlertDialog.Builder(c)
                    builder.setTitle(R.string.Warning)
                    builder.setMessage("Wrong barcode scanned")
                    builder.setPositiveButton("OK") { dialog, which ->
                        // Do something when OK button is clicked
                        p09_barcode_txt.text.clear()
                        p09_barcode_txt.requestFocus()
                        pallet_info_p09.text = ""
                    }
                    builder.show()
                }
            }
            else{
                jsonString = JSONObject(resultFromLink)
                result.MATERIAL = jsonString.getString("MATERIAL")
                result.CARTON_QTY = jsonString.getString("CARTON_QTY")
                result.LOT_NO = jsonString.getString("LOT_NO")
                result.PALLET_SEQUANCE = jsonString.getString("PALLET_SEQUENCE")
                result.QUANTITY = jsonString.getString("QUANTITY")
                result.PALLET_ID = jsonString.getString("PALLET_ID")
            }

            setProgressBar(pb3)
        }

        return result

    }

}