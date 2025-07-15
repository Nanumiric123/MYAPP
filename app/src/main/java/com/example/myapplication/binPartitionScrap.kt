package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class binPartitionScrap : AppCompatActivity() {
    private lateinit var badgeNum:String
    private lateinit var binNumED:EditText
    private lateinit var infoTV:TextView
    private lateinit var btnSave:Button
    private lateinit var binData: binPartitionIssue.BinInfo
    private lateinit var currentContext: Context
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_bin_partition_scrap)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        currentContext = this@binPartitionScrap
        badgeNum = intent.getStringExtra("BADGE_NO").toString()
        binNumED = findViewById(R.id.BPSEDBINNO)
        infoTV = findViewById(R.id.BPSTVINFO)
        btnSave = findViewById(R.id.BPSbtnSave)
        binData = binPartitionIssue.BinInfo(
            id = 0, binNumber = "", type = "",
            dateRegistered = "", pic = "", status = ""
        )

        binNumED.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if(keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN
                || keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN){

                CoroutineScope(Dispatchers.IO).launch {
                    retrieveBinInfo(binNumED.text.toString())
                }

                return@OnKeyListener false
            }
            false
        })

        btnSave.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                if(infoTV.text != "") {
                    val result = savetoDB(binData.binNumber,binData.type,binData.dateRegistered,badgeNum)
                    var resultSplit = result.split(':')
                    TriggerAlert(resultSplit[1],resultSplit[0])
                }
                else{
                    TriggerAlert("Please Scan Bin ID","Error")
                }
            }
        }

    }

    private suspend fun retrieveBinInfo(binNo:String){
        withContext(Dispatchers.IO){
            val url = URL("http://172.16.206.19/BARCODEWEBAPI/api/BINS_PARTITION_STOCK_CONTROL?binNumber=${binNo}")
            val connection = url.openConnection() as HttpURLConnection
            try{
                val responseCode = connection.responseCode
                if(responseCode == HttpURLConnection.HTTP_OK) {
                    // Read the response
                    val inputStream = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var inputLine: String?

                    while (inputStream.readLine().also { inputLine = it } != null) {
                        response.append(inputLine)
                    }
                    val jsonArr = JSONArray(response.toString())
                    inputStream.close()
                    val jsonObj = JSONObject(jsonArr[0].toString())
                    val binId = jsonObj.getInt("id")
                    val binNumb = jsonObj.getString("biN_NUMBER")
                    val typ = jsonObj.getString("type")
                    val date_Registered = jsonObj.getString("datE_REGISTERED")
                    val binPic = jsonObj.getString("pic")
                    val binStatus = jsonObj.getString("status")
                    if(binStatus == "NEW"){
                        TriggerAlert("${typ} not register for Used, please register","Error")
                    }
                    else{
                        runOnUiThread(kotlinx.coroutines.Runnable {
                            infoTV.setText("ID : ${binId} \n Bin Number : ${binNumb} \n Type : ${typ} \n Date Rgistered : ${date_Registered} " +
                                    "\n PIC : ${binPic} \n STATUS : ${binStatus}")
                        })
                    }


                    binData = binPartitionIssue.BinInfo(
                        id = binId,
                        binNumber = binNumb,
                        type = typ,
                        dateRegistered = date_Registered,
                        pic = binPic,
                        status = binStatus
                    )
                }
                else{
                    TriggerAlert("Error code ${responseCode}","Error")
                }
            }
            catch (e:Exception){

                if (e.message.toString() == "Index 0 out of range [0..0)"){
                    TriggerAlert("BIN ${binNo} not register for Used, please register","Error")
                }
                else{
                    TriggerAlert("Error code ${e.message.toString()}","Error")
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
                    "  \"status\": \"SCRAP\"\n" +
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
                resetForm()
            }

        }
        return result
    }

    private fun resetForm(){
        runOnUiThread(kotlinx.coroutines.Runnable {
            infoTV.setText("")
            binData = binPartitionIssue.BinInfo(
                id = 0,
                binNumber = "",
                type = "",
                dateRegistered = "",
                pic = "",
                status = ""
            )
            binNumED.text.clear()
        })

    }

    private fun TriggerAlert(errorMsg:String,errorTitle:String){
        runOnUiThread(kotlinx.coroutines.Runnable{
            val builder = AlertDialog.Builder(currentContext)

            builder.setTitle(errorTitle)
            builder.setMessage(errorMsg)
            builder.setPositiveButton("OK") { dialog, which ->
                // Do something when OK button is clicked
                resetForm()
            }
            builder.show()
        })
    }

}