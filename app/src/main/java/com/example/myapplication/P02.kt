package com.example.myapplication

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
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
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import java.security.MessageDigest


class P02 : AppCompatActivity() {
    private lateinit var btnMenu:Button
    private lateinit var btnClear:Button
    private lateinit var btnSave:Button
    private lateinit var partInput:EditText
    private lateinit var quantityInput:EditText
    private lateinit var toSLocInput:EditText
    private lateinit var toLocInput:EditText
    private lateinit var fromLocInput:EditText
    private lateinit var pb:ProgressBar
    private lateinit var lotNo:EditText
    private lateinit var badgeNumber:String
    private lateinit var c: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_p02)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        btnMenu = findViewById(R.id.P02BtnMenu)
        btnClear = findViewById(R.id.P02BtnClear)
        btnSave = findViewById(R.id.P02BtnSave)
        partInput = findViewById(R.id.P02EDPart)
        quantityInput = findViewById(R.id.P02EDQty)
        toSLocInput = findViewById(R.id.P02EDTOSLOC)
        toLocInput = findViewById(R.id.P02EDTOLOC)
        fromLocInput = findViewById(R.id.P02EDfromLoc)
        lotNo = findViewById(R.id.P02EDBatch)
        pb = findViewById(R.id.P02PB)
        c = this@P02

        btnClear.setOnClickListener {
            clearEverything()
        }
        btnMenu.setOnClickListener {
            this.finish()
        }
        badgeNumber = intent.getStringExtra("Badge").toString()
        var deviceID = getDeviceUniqueId(c)
        fromLocInput.requestFocus()

        fromLocInput.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                generateBinDetail(fromLocInput.text.toString(),pb)
                return@OnKeyListener true
            }
            false
        })

        toSLocInput.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                toLocInput.requestFocus()
                return@OnKeyListener true
            }
            false
        })

        quantityInput.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                toSLocInput.requestFocus()
                return@OnKeyListener true
            }
            false
        })

        btnSave.setOnClickListener {
            if(deviceID.length > 50){
                deviceID = deviceID.substring(0,49)
            }
            executeSubmitSAP(partInput.text.toString(),lotNo.text.toString(),
                toLocInput.text.toString(),fromLocInput.text.toString(),
                deviceID,toSLocInput.text.toString(),pb)

        }

    }

    private fun clearBinP08(binNumber:String?):String? {
        // Check if orderNum is null or empty
        if (binNumber.isNullOrEmpty()) {
            throw IllegalArgumentException("Order number cannot be null or empty")
        }
        val url:URL = URL("http://172.16.206.19/REST_API/Home/MPP_P08?str_loc=2102&bin_num=${binNumber}")
        return try{
            url.readText()
        }
        catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun executeClearBinP08(binNumber:String?,progressBar: View){
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Show the progress bar and disable the button
                setProgressBar(progressBar, true)
                val result = withContext(Dispatchers.IO) {
                    clearBinP08(binNumber)
                }
                // Hide the progress bar and enable the button
                setProgressBar(progressBar, false)
                // Handle the result as a String
                result?.let { responseString ->
                    var resultSplit = responseString.split(':')
                    // Show the result in an AlertDialog
                    triggerAlert(resultSplit[0],"P08 ${resultSplit[1]} Siap")
                } ?: run {
                    // Handle the error, e.g., show an error message
                    Toast.makeText(progressBar.context, "Gagal, Kena buat MANUAL", Toast.LENGTH_SHORT).show()
                }
            }
            catch (e: IllegalArgumentException) {

            }
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

    private fun submitToSAP(part:String?,lotNum:String?,d_Bin:String?,f_Bin:String?,deviceImei:String?
                                    ,toStorageLoc:String?):String?
    {
        // Validate all required parameters
        if (part.isNullOrEmpty() || lotNum.isNullOrEmpty() || f_Bin.isNullOrEmpty() ||
            deviceImei.isNullOrEmpty() || toStorageLoc.isNullOrEmpty()) {
            throw IllegalArgumentException("All parameters must be provided and cannot be null or empty")
        }

        val linkString = "http://172.16.206.19/REST_API/Third/MPPP02SubmitData?" +
                "material=${part}&batch=${lotNum}&qty=${quantityInput.text}&badgeno=${badgeNumber}&destBin=${d_Bin}&" +
                "fromBin=${f_Bin}&deviceID=${deviceImei}&line=${toStorageLoc}"
        return try {
            val url = URL(linkString)
            url.readText()  // Fetch the response as a String
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    private suspend fun fetchBinDetails(binNumber:String?):JSONObject? {
        // Check if binNumber is null or empty
        if (binNumber.isNullOrEmpty()) {
            throw IllegalArgumentException("Order number cannot be null or empty")
        }
        var urlString = "http://172.16.206.19/REST_API/Third/MPPP02GETBINDETAILS?BinNo=${binNumber}"
        return try {
            val url = URL(urlString)
            val responseText = url.readText()  // Fetch the response as a String
            JSONObject(responseText)  // Convert the response to a JSONObject
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }


    }
    private fun executeSubmitSAP(part:String?,lotNum:String?,d_Bin:String?,f_Bin:String?,deviceImei:String?
                                 ,toStorageLoc:String?,progressBar: View){
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Show the progress bar and disable the button
                setProgressBar(progressBar, true)
                // Perform the network request in the IO dispatcher
                val result = withContext(Dispatchers.IO) {
                    submitToSAP(part,lotNum,d_Bin,f_Bin,deviceImei,toStorageLoc)
                }
                // Hide the progress bar and enable the button
                setProgressBar(progressBar, false)
                // Handle the result as a String
                result?.let { responseString ->
                    var resultSplit = responseString.split(':')
                    // Show the result in an AlertDialog
                    val dialogBuilder = AlertDialog.Builder(progressBar.context)
                    dialogBuilder.setTitle(resultSplit[0])
                    dialogBuilder.setMessage("P02 ${resultSplit[1]}  : Mau Clear Bin ?")
                    dialogBuilder.setPositiveButton("Mau") { dialog, _ ->
                        executeClearBinP08(f_Bin,progressBar)
                        dialog.dismiss()
                    }
                    dialogBuilder.setNegativeButton("Tak Mau") { dialog, _ ->
                        clearEverything()
                        dialog.dismiss()
                    }
                    val alertDialog = dialogBuilder.create()
                    alertDialog.show()
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



    private fun generateBinDetail(binNumber:String?,progressBar: View){
        CoroutineScope(Dispatchers.Main).launch {
            try{
                // Show the progress bar and disable the button
                setProgressBar(progressBar, true)
                // Perform the network request in the IO dispatcher
                val result = withContext(Dispatchers.IO) {
                    fetchBinDetails(binNumber)
                }
                // Hide the progress bar and enable the button
                setProgressBar(progressBar, false)
                // Handle the result
                result?.let { jsonObject ->
                    // Process the JSONObject, e.g., update the UI with the order details
                    // Example: val orderId = jsonObject.getString("orderId")
                    val msg = jsonObject.getString("MESSAGE")
                    val msgSplit = msg.split(':')
                    if (msgSplit[0].trim().equals("S")){
                        partInput.setText(jsonObject.getString("MATERIAL"))
                        lotNo.setText(jsonObject.getString("BATCH"))
                        quantityInput.requestFocus()
                    }
                    else{
                        triggerAlert(msgSplit[0],msgSplit[1])
                    }
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

    private fun setProgressBar(v: View, show: Boolean) {
        if (show) {
            v.visibility = View.VISIBLE
            btnSave.isEnabled = false
        } else {
            v.visibility = View.GONE
            btnSave.isEnabled = true
        }
    }


    private fun clearEverything(){
        partInput.text.clear()
        quantityInput.text.clear()
        toSLocInput.text.clear()
        toLocInput.text.clear()
        fromLocInput.text.clear()
        fromLocInput.requestFocus()
        lotNo.text.clear()
    }
    private fun triggerAlert(title:String,msg:String){
        NotificationSound(c)
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

    private fun NotificationSound(con:Context){
        try {
            // Get the default notification sound URI
            val notificationSoundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            // Create a Ringtone instance
            val ringtone: Ringtone = RingtoneManager.getRingtone(con, notificationSoundUri)

            // Play the sound
            ringtone.play()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}

data class BINDETAIL(var message:String,var material:String,var batch:String,var fifo:String,var fifoBin:String)