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
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import java.security.MessageDigest

class P01 : AppCompatActivity() {
    private lateinit var btnMenu:Button
    private lateinit var btnClear:Button
    private lateinit var btnSave:Button
    private lateinit var inputPOBarcode:EditText
    private lateinit var inputPart:EditText
    private lateinit var inputQuantity:EditText
    private lateinit var inputToLocation:EditText
    private lateinit var lotNo:EditText
    private lateinit var pb:ProgressBar
    private lateinit var c: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_p01)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        btnMenu = findViewById(R.id.P01BtnMenu)
        btnClear = findViewById(R.id.P01BtnClear)
        btnSave = findViewById(R.id.P01BtnSave)
        inputPOBarcode = findViewById(R.id.P01EDPOBcodeInput)
        inputPart = findViewById(R.id.P01EDPartInput)
        inputQuantity = findViewById(R.id.P01EDQty)
        inputToLocation = findViewById(R.id.P01EDToLocinput)

        pb = findViewById(R.id.P01PB)
        c = this
        val Bnum = intent.getStringExtra("Badge").toString()
        inputPOBarcode.requestFocus()

        inputPOBarcode.setOnKeyListener(View.OnKeyListener {_, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                getOrderDetails(inputPOBarcode.text.toString(),pb)
                return@OnKeyListener true
            }
            false
        })

            btnSave.setOnClickListener {

                var deviceID = getDeviceUniqueId(c)
                if (deviceID.length > 50) {
                    deviceID = deviceID.substring(0, 49)
                }
                executeSubmitToSAP(
                    inputPOBarcode.text.toString(),
                    inputPart.text.toString(),
                    Bnum,
                    deviceID,
                    inputQuantity.text.toString(),
                    inputToLocation.text.toString(),
                    pb
                )
        }
        btnClear.setOnClickListener {
            clearEverything()
        }
        btnMenu.setOnClickListener {
            this.finish()
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

    private fun setProgressBar(v: View, show: Boolean) {
        if (show) {
            v.visibility = View.VISIBLE
            btnSave.isEnabled = false
        } else {
            v.visibility = View.GONE
            btnSave.isEnabled = true
        }
    }

    fun getOrderDetails(orderNum: String?, progressBar: View) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Show the progress bar and disable the button
                setProgressBar(progressBar, true)
                // Perform the network request in the IO dispatcher
                val result = withContext(Dispatchers.IO) {
                    fetchOrderDetails(orderNum)
                }

                // Hide the progress bar and enable the button
                setProgressBar(progressBar, false)

                // Handle the result
                result?.let { jsonObject ->
                    // Process the JSONObject, e.g., update the UI with the order details
                    // Example: val orderId = jsonObject.getString("orderId")
                    inputPart.setText(jsonObject.getString("MATERIAL"))
                    inputQuantity.requestFocus()
                } ?: run {
                    // Handle the error, e.g., show an error message
                }
            } catch (e: IllegalArgumentException) {
                // Hide the progress bar
                setProgressBar(progressBar, false)
                // Show a toast message with the error
                Toast.makeText(progressBar.context, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun submitToSAPs(orderNum: String?, materialNum: String?, badgeNum: String?,
                                    deviceID: String?, qty: String?, toLoc: String?): String? {
        // Validate all required parameters
        if (orderNum.isNullOrEmpty() || materialNum.isNullOrEmpty() || badgeNum.isNullOrEmpty() ||
            deviceID.isNullOrEmpty() || qty.isNullOrEmpty() || toLoc.isNullOrEmpty()) {
            throw IllegalArgumentException("All parameters must be provided and cannot be null or empty")
        }

        val linkString = "http://172.16.206.19/REST_API/Third/MPPP01SubmitData" +
                "?material=$materialNum&batch=$orderNum&qty=$qty&badgeno=$badgeNum&destBin=$toLoc&deviceID=$deviceID"
        return try {
            val url = URL(linkString)
            url.readText()  // Fetch the response as a String
        } catch (e: IOException) {
            e.printStackTrace().toString()
            //null
        }
    }



    private fun fetchOrderDetails(orderNum: String?): JSONObject? {
        // Check if orderNum is null or empty
        if (orderNum.isNullOrEmpty()) {
            throw IllegalArgumentException("Order number cannot be null or empty")
        }

        val urlString = "http://172.16.206.19/REST_API/Third/MPP_P01_GET_ORDER_DETAIL?orderNum=$orderNum"
        return try {
            val url = URL(urlString)
            val responseText = url.readText()  // Fetch the response as a String
            try{
                JSONObject(responseText)  // Convert the response to a JSONObject
            }
            catch (eX:JSONException){
                JSONObject("{\n" +
                        "    \"MATERIAL\": \"\",\n" +
                        "    \"QUANTITY\": 0\n" +
                        "}")  // Convert the response to a JSONObject
            }

        } catch (e: IOException) {
            e.printStackTrace()
            null
        }

    }


    private fun executeSubmitToSAP(orderNum: String?, materialNum: String?, badgeNum: String?,
                           deviceID: String?, qty: String?, toLoc: String?, progressBar: View) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Show the progress bar and disable the button
                setProgressBar(progressBar, true)

                // Perform the network request in the IO dispatcher
                val result = withContext(Dispatchers.IO) {
                    submitToSAPs(orderNum, materialNum, badgeNum, deviceID, qty, toLoc)
                }

                // Hide the progress bar and enable the button
                setProgressBar(progressBar, false)

                // Handle the result as a String
                result?.let { responseString ->
                    var resultSplit = responseString.split(':')
                    // Show the result in an AlertDialog
                    triggerAlert(resultSplit[0],resultSplit[1])
                } ?: run {
                    // Handle the error, e.g., show an error message
                    Toast.makeText(progressBar.context, "Failed to submit data", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IllegalArgumentException) {


                // Show a toast message with the error
                Toast.makeText(progressBar.context, e.message, Toast.LENGTH_SHORT).show()
            }
            finally{
                // Hide the progress bar
                setProgressBar(progressBar, false)
            }
        }
    }

    private fun triggerAlert(title:String,msg:String){
        val builder = AlertDialog.Builder(c)
        builder.setTitle(title)
        builder.setMessage(msg)
        builder.setPositiveButton("OK") { dialog, which ->
            // Do something when OK button is clicked
            clearEverything()
            dialog.dismiss()
        }
        builder.show()
    }
    private fun clearEverything(){
        inputPOBarcode.text.clear()
        inputPart.text.clear()
        inputQuantity.text.clear()
        inputToLocation.text.clear()
        inputPOBarcode.requestFocus()
    }
}

data class PODetail(var mATERIAL:String, var qUANTITY:String)