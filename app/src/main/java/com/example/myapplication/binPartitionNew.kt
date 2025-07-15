package com.example.myapplication

import android.content.Context
import android.icu.text.CaseMap.Title
import android.os.Bundle
import android.provider.MediaStore.Audio.Radio
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.crashlytics.buildtools.reloc.com.google.errorprone.annotations.Var
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.w3c.dom.Text
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class binPartitionNew : AppCompatActivity() {
    private lateinit var binNumTV: EditText
    private lateinit var currentDate:TextView
    private lateinit var picBadge:TextView
    private lateinit var todayDate:String
    private lateinit var binTypTV:TextView
    private lateinit var typeOfBin:String
    private lateinit var btnSave:Button
    private lateinit var currentContext:Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_bin_partition_new)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binNumTV = findViewById(R.id.BPNEDBinNumber)
        currentDate = findViewById(R.id.BPNTVDATE)
        picBadge = findViewById(R.id.BPNTVPIC)
        btnSave = findViewById(R.id.BPNbtnSave)
        currentContext = this@binPartitionNew
        binTypTV = findViewById(R.id.BPNTVBintyp)
        picBadge.text = intent.getStringExtra("BADGE_NO").toString()
        CoroutineScope(Dispatchers.IO).launch {
            todayDate = getCurrentDate()
            currentDate.text = getString(R.string.current_date, todayDate)
        }

        binNumTV.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if(keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN
                || keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN){

                if(binNumTV.text.toString().contains('_')){
                    binTypTV.setText("BIN TYPE :  PARTITION")
                }
                else{
                    binTypTV.setText("BIN TYPE : BIN")
                }

                return@OnKeyListener false
            }
            false
        })

        btnSave.setOnClickListener {
            typeOfBin = "BIN"
            if(binNumTV.text.toString() == "" || binNumTV.text.toString().isNullOrBlank()){
                TriggerAlert("Please Scan Bin ID","Error")
            }
            else{
                CoroutineScope(Dispatchers.IO).launch {
                    val result = savetoDB(binNumTV.text.toString(),typeOfBin,todayDate,picBadge.text.toString())
                    val resultSplit = result.split(':')
                    val sucessCode = resultSplit[0].trim().replace("\"","")
                    if(sucessCode == "S"){
                        TriggerAlert(resultSplit[1],resultSplit[0])
                    }
                    else{
                        TriggerAlert(resultSplit[1],resultSplit[0])
                    }
                }
            }


        }
    }


    private suspend fun savetoDB(binNo:String,binTyp:String,dat:String,usrName:String):String{
        var result:String = String()
        withContext(Dispatchers.IO){
            val urlLink = URL("http://172.16.206.19/BARCODEWEBAPI/api/BINS_PARTITION_STOCK_CONTROL")
            var payLoad = "{\n" +
                    "  \"biN_NUMBER\": \"${binNo}\",\n" +
                    "  \"type\": \"${binTyp}\",\n" +
                    "  \"datE_REGISTERED\": \"${dat}\",\n" +
                    "  \"pic\": \"${usrName}\",\n" +
                    "  \"status\": \"NEW\"\n" +
                    "}"
            // Set up the connection
            val connection = urlLink.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            try{
                // Write the JSON body
                val outputStreamWriter = OutputStreamWriter(connection.outputStream)
                outputStreamWriter.write(payLoad.toString())
                outputStreamWriter.flush()
                // Get the response code
                val responseCode = connection.responseCode

                if(responseCode == HttpURLConnection.HTTP_OK){
                    // Read the response
                    val inputStream = connection.inputStream
                    val response = inputStream.bufferedReader().use { it.readText() }
                    result = response.toString()
                }
                else{
                    result = "Problem HTTP RESPONSE : ${responseCode}"
                }

            }
            catch(e: Exception){

            }
            finally {

            }

        }
        return result
    }

    private suspend fun getCurrentDate():String{
        var result:String = String()
        withContext(Dispatchers.IO){
            val urlLink = URL("http://172.16.206.19/BARCODEWEBAPI/api/BINS_PARTITION_STOCK_CONTROL/5")
            val connection = urlLink.openConnection() as HttpURLConnection
            try{
                // Set request method to GET (GET is the default)
                connection.requestMethod = "GET"
                // Read response
                val inputStream = BufferedReader(InputStreamReader(connection.inputStream))
                var inputLine: String?
                val response = StringBuilder()

                while (inputStream.readLine().also { inputLine = it } != null) {
                    response.append(inputLine)
                }
                result = response.toString()
                inputStream.close()

            }
            catch(e:Exception){

            }
        }
        return result
    }

    private fun TriggerAlert(errorMsg:String,errorTitle:String){
        runOnUiThread(kotlinx.coroutines.Runnable{
            val builder = AlertDialog.Builder(currentContext)

            builder.setTitle(errorTitle)
            builder.setMessage(errorMsg)
            builder.setPositiveButton("OK") { dialog, which ->
                // Do something when OK button is clicked
                binNumTV.text.clear()
            }
            builder.show()
        })
    }
    
}