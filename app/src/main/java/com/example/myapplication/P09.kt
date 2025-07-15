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
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
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
    private var barcodeResult = palletLabel(MATERIAL = "",PALLET_SEQUANCE = "",LOT_NO = "",QUANTITY="",PALLET_ID = "",CARTON_QTY = "")

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


        p09_barcode_txt.setOnKeyListener { view, i, keyEvent ->
            if (keyEvent.keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_DOWN ||
                keyEvent.keyCode == KeyEvent.KEYCODE_TAB && keyEvent.action == KeyEvent.ACTION_DOWN ) {
                executebreakupSCANNEDTEXT(p09_barcode_txt.text.toString(),pb3)
                /*
                executebreakupSCANNEDTEXT(p09_barcode_txt.text.toString())
                pallet_info_p09.text = "Material : ${barcodeResult.MATERIAL} \n Quantity : ${barcodeResult.QUANTITY} \n Pallet ID : ${barcodeResult.PALLET_ID}"
                */
                return@setOnKeyListener true
            }
            false
        }
        btn_save_p09.setOnClickListener {
                //perform action
                if(barcodeResult.MATERIAL.isNotBlank() && barcodeResult.PALLET_ID.isNotBlank() && barcodeResult.LOT_NO.isNotBlank()
                    && barcodeResult.QUANTITY.isNotBlank()){
                    executeFetchData(barcodeResult,b_num,pb3)

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

    private fun clearAll(){
        runOnUiThread(kotlinx.coroutines.Runnable {
            barcodeResult = palletLabel(MATERIAL = "",PALLET_SEQUANCE = "",LOT_NO = "",QUANTITY="",PALLET_ID = "",CARTON_QTY = "")
            p09_barcode_txt.text.clear()
            p09_barcode_txt.requestFocus()
            pallet_info_p09.text = ""
        })
    }

    private fun fetchData(bcLabelInfo: palletLabel,badgeNum:String):String? {
        // Validate all required parameters
        if (bcLabelInfo.MATERIAL.isNullOrEmpty() || bcLabelInfo.LOT_NO.isNullOrEmpty() || badgeNum.isNullOrEmpty() ||
            bcLabelInfo.QUANTITY.isNullOrEmpty() || bcLabelInfo.PALLET_ID.isNullOrEmpty() || bcLabelInfo.CARTON_QTY.isNullOrEmpty()) {
            throw IllegalArgumentException("All parameters must be provided and cannot be null or empty")
        }
        val linkString = "http://172.16.206.19/REST_API/Home/MPP_PACKING?material=${bcLabelInfo.MATERIAL}&quantity=${bcLabelInfo.QUANTITY}" +
                "&lot_no=${bcLabelInfo.LOT_NO}&badge_num=${g_b_num}_P09&pallet_id=${bcLabelInfo.PALLET_ID}"

        return try{
            val url = URL(linkString)
            url.readText()  // Fetch the response as a String
        }
        catch (e:IOException){
            "E:${e.message.toString()}"
        }
    }

    private fun executeFetchData(bcLabelInfo: palletLabel,badgeNum:String,progressBar: View){
        CoroutineScope(Dispatchers.Main).launch {
            try{
                // Show the progress bar and disable the button
                setProgressBar(progressBar, true)
                // Perform the network request in the IO dispatcher
                val result = withContext(Dispatchers.IO) {
                    fetchData(bcLabelInfo,badgeNum)
                }
                // Hide the progress bar and enable the button
                setProgressBar(progressBar, false)
                // Handle the result string and attempt to convert it to a JSONObject
                result?.let { responseString ->

                    var message = String()
                    var msgTitle = String()
                    try {
                        val k = JSONArray(responseString)
                        msgTitle = if(k[1].toString().contains("E")){
                            "Error"
                        } else {
                            "Success"
                        }
                        message = k[0].toString()
                        // Process the JSON object as needed, or display it
                        val dialogBuilder = AlertDialog.Builder(progressBar.context)
                        dialogBuilder.setTitle(msgTitle)
                        dialogBuilder.setMessage(message)
                        dialogBuilder.setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                            clearAll()
                        }
                        val alertDialog = dialogBuilder.create()
                        alertDialog.show()
                    } catch (e: JSONException) {
                        // Handle JSON parsing error
                        e.printStackTrace()
                        // Show a dialog with the raw response string
                        val dialogBuilder = AlertDialog.Builder(progressBar.context)
                        dialogBuilder.setTitle("Submission Result")
                        dialogBuilder.setMessage("Failed to parse response as JSON.\n\nRaw Response:\n$responseString")
                        dialogBuilder.setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                            clearAll()
                        }
                        val alertDialog = dialogBuilder.create()
                        alertDialog.show()
                    }
                } ?: run {
                    // Handle the error, e.g., show an error message
                    Toast.makeText(progressBar.context, "Failed to submit data", Toast.LENGTH_SHORT).show()
                }
            }
            catch (e:IllegalArgumentException){
                // Hide the progress bar
                setProgressBar(progressBar, false)

                // Show a toast message with the error
                Toast.makeText(progressBar.context, e.message, Toast.LENGTH_SHORT).show()
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

    private fun setProgressBar(v: View, show: Boolean) {
        if (show) {
            v.visibility = View.VISIBLE
            btn_save_p09.isEnabled = false
        } else {
            v.visibility = View.GONE
            btn_save_p09.isEnabled = true
        }
    }

    private fun breakupSCANNEDTEXT(bc:String): String? {
        if (bc.isNullOrEmpty()){
            throw IllegalArgumentException("All parameters must be provided and cannot be null or empty")
        }

        val url = URL("http://172.16.206.19/REST_API/Home/breakpalletbarcodeString?barcode=$bc")

        return try {
            url.readText()  // Fetch the response as a String
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }

    }

    private fun executebreakupSCANNEDTEXT(bc:String,progressBar: View){
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Show the progress bar and disable the button
                setProgressBar(progressBar, true)

                // Perform the network request in the IO dispatcher
                val result = withContext(Dispatchers.IO) {
                        breakupSCANNEDTEXT(bc)
                }
                // Hide the progress bar and enable the button
                setProgressBar(progressBar, false)

                // Handle the result
                result?.let { responseString ->
                    // Process the JSONObject, e.g., update the UI with the order details
                    // Example: val orderId = jsonObject.getString("orderId")

                    if(responseString.toString() == "\"DUPLICATE\""){
                        val builder = AlertDialog.Builder(c)
                        builder.setTitle(R.string.Warning)
                        builder.setMessage("Duplicate Pallet Scanned")
                        builder.setPositiveButton("OK") { dialog, which ->
                            // Do something when OK button is clicked
                            clearAll()
                        }
                        builder.show()

                    }
                    else if(responseString.toString() == "\"ERROR\""){
                        val builder = AlertDialog.Builder(c)
                        builder.setTitle(R.string.Warning)
                        builder.setMessage("Wrong barcode scanned")
                        builder.setPositiveButton("OK") { dialog, which ->
                            // Do something when OK button is clicked
                            clearAll()
                        }
                        builder.show()
                    }
                    else{
                        val jsonObject = JSONObject(responseString)
                        barcodeResult.MATERIAL = jsonObject.getString("MATERIAL")
                        barcodeResult.LOT_NO = jsonObject.getString("LOT_NO")
                        barcodeResult.QUANTITY = jsonObject.getString("QUANTITY")
                        barcodeResult.PALLET_ID = jsonObject.getString("PALLET_ID")
                        barcodeResult.CARTON_QTY = jsonObject.getString("CARTON_QTY")
                        barcodeResult.PALLET_SEQUANCE = jsonObject.getString("PALLET_SEQUENCE")

                        ("Material : ${jsonObject.getString("MATERIAL")} \n Quantity : ${jsonObject.getString("QUANTITY")} " +
                                "\n Pallet ID : ${jsonObject.getString("PALLET_ID")}").also { pallet_info_p09.text = it }
                    }

                } ?: run {
                    // Handle the error, e.g., show an error message
                    Toast.makeText(progressBar.context, "Failed to submit data", Toast.LENGTH_SHORT).show()
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

}