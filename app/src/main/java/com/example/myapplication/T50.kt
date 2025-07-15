package com.example.myapplication

import android.content.Context
import android.content.pm.LauncherActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Message
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class T50 : AppCompatActivity() {
    private lateinit var barcodeTxt:EditText
    private lateinit var pb:ProgressBar
    private lateinit var partNumberED:EditText
    private lateinit var uomED:EditText
    private lateinit var qtyED:EditText
    private lateinit var lotNumED:EditText
    private lateinit var btnSave:Button
    private lateinit var partOnKB:EditText
    private lateinit var fromLoc:EditText
    private lateinit var toLoc:EditText
    private lateinit var btnClear:Button
    private lateinit var menuBtn:Button
    private lateinit var DCResult:T06.BarcodeData
    private lateinit var badgeNo:String
    private lateinit var ct:Context
    private lateinit var binTyp:EditText
    private lateinit var scv:ScrollView

    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_t50)
        pb = findViewById(R.id.T50PB)
        barcodeTxt = findViewById(R.id.T50EDBARCODE)
        partNumberED = findViewById(R.id.T50EDPART)
        uomED = findViewById(R.id.T50EDUOM)
        qtyED = findViewById(R.id.T50EDQTY)
        lotNumED = findViewById(R.id.T50EDLOTNO)
        btnSave = findViewById(R.id.T50BTNSAVE)
        partOnKB = findViewById(R.id.T50EDPARTONKANBAN)
        fromLoc = findViewById(R.id.T50EDFROMLOC)
        toLoc = findViewById(R.id.T50EDTOLOC)
        btnClear = findViewById(R.id.T50BTNCLEAR)
        menuBtn = findViewById(R.id.T50BTNMENU)
        badgeNo = intent.getStringExtra("Badge").toString()
        scv = findViewById(R.id.T50SCV)
        ct = this@T50
        binTyp = findViewById(R.id.T50EDSTDBINTYP)
        barcodeTxt.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
                || keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN){
                var BarcodeResultString = String()

                btnSave.isEnabled = false

                CoroutineScope(Dispatchers.Main).launch {
                    progressbarSetting(pb)
                    try{
                        BarcodeResultString = translateBarcode(barcodeTxt.text.toString())
                        try{
                            DCResult = translateAPIResult(BarcodeResultString)
                            partNumberED.setText(DCResult.pART_NO)

                            uomED.setText("PC")
                            lotNumED.setText(DCResult.lOT)

                            btnSave.isEnabled = false
                            partOnKB.requestFocus()
                        }
                        catch (e:Exception){
                            TriggerAlert(ct,"SILAP SCAN BARCODE")
                        }
                    }
                    catch(e:Exception){
                        TriggerAlert(ct,"SILAP SCAN BARCODE")
                    }


                    progressbarSetting(pb)

                }

                return@OnKeyListener true
            }
            false
        })
        partOnKB.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
                || keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN){

                if(partOnKB.text.toString() == partNumberED.text.toString()){
                    fromLoc.requestFocus()
                    progressbarSetting(pb)
                    CoroutineScope(Dispatchers.Main).launch {
                        val qtyKB = retrieveBinQuantity(partOnKB.text.toString())
                        qtyED.setText(qtyKB.toString())
                        DCResult.qUANTITY = qtyKB.toString()
                        progressbarSetting(pb)
                    }


                }
                else{
                    partOnKB.requestFocus()
                    partOnKB.text.clear()
                    TriggerAlert(ct,"Salah scan kanban")
                }

                return@OnKeyListener true
            }
            false
        })

        fromLoc.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
                || keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN){
                toLoc.requestFocus()
                return@OnKeyListener true
            }
            false
        })

        toLoc.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
                || keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN){
                btnSave.isEnabled = true
                barcodeTxt.requestFocus()
                CoroutineScope(Dispatchers.Main).launch {
                    progressbarSetting(pb)
                    var res = parseJSON(checkForCOGIBin(toLoc.text.toString()))
                    if (res.msg.equals("\"Error\"")){
                        scv.post{
                            scv.smoothScrollTo(0,0)
                        }
                    }
                    else{
                        val builder = AlertDialog.Builder(ct)

                        builder.setTitle("Message")
                        builder.setMessage("COGI bin \n Bin Hantu")
                        builder.setPositiveButton("OK") { dialog, which ->
                            // Do something when OK button is clicked
                            scv.post{
                                scv.smoothScrollTo(0,0)
                            }

                        }
                        builder.setNegativeButton("Reset"){dialog, which ->
                            resetForm()
                            scv.post{
                                scv.smoothScrollTo(0,0)
                            }

                        }
                        builder.show()
                    }

                }
                return@OnKeyListener true
            }
            false
        })

        btnClear.setOnClickListener{
            resetForm()
        }
        menuBtn.setOnClickListener {
            this.finish()
        }
        btnSave.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                sendDataToSAP(DCResult,badgeNo,fromLoc.text.toString(),toLoc.text.toString())
            }

        }

    }

    private fun resetForm(){
        partNumberED.text.clear()
        qtyED.text.clear()
        uomED.text.clear()
        lotNumED.text.clear()
        barcodeTxt.text.clear()
        toLoc.text.clear()
        fromLoc.text.clear()
        DCResult = T06.BarcodeData(vENDOR = "", dATE = "", pART_NO = "", rEEL_NO = "", lOT = "", qUANTITY = "", uOM = "")
        btnSave.isEnabled = false
        partOnKB.text.clear()
        binTyp.text.clear()
    }

    private fun translateAPIResult(input: String):T06.BarcodeData{

        val jobj = JSONObject(input)
        val material = jobj.getString("material")
        val vendor = jobj.getString("vendor")
        val date = jobj.getString("date")
        val reel = jobj.getString("reelnumber")
        val batch = jobj.getString("batch")
        val uom = jobj.getString("uom")
        val qty = jobj.getString("quantity")

        val result:T06.BarcodeData = T06.BarcodeData(vENDOR = vendor, dATE = date, pART_NO = material
            , rEEL_NO = reel, lOT = batch, qUANTITY = qty, uOM = uom)
        return result
    }

    private fun parseJSON(input:String):JsonResponse{
        val jObj = JSONObject(input)

        val msg = jObj.getString("msg")
        val itemsArr = jObj.getJSONArray("items")

        // Parse items from the itemsArray
        val itemsList = mutableListOf<Item_Data>()
        for (i in 0 until itemsArr.length()) {
            val itemObject = itemsArr.getJSONObject(i)
            val item = Item_Data(
                material = itemObject.getString("material"),
                quantity = itemObject.getString("quantity"),
                uom = itemObject.getString("uom"),
                storagebin = itemObject.getString("storagebin"),
                batch = itemObject.getString("batch")
            )

            itemsList.add(item)
        }

        return JsonResponse(msg, itemsList)
    }

    private suspend fun checkForCOGIBin(binNumber:String):String {
        var result:String = String()

        withContext(Dispatchers.IO){
            val url = URL("http://172.16.206.19/BARCODEWEBAPI/API/BARCODEController1")
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val payLoad = "{\n" +
                        "  \"storagetyp\": \"006\",\n" +
                        "  \"storageloc\": \"2006\",\n" +
                        "  \"storagebin\": \"${binNumber}\"\n" +
                        "}".trimIndent()

                val outputStream = OutputStreamWriter(connection.outputStream)
                outputStream.write(payLoad)
                outputStream.flush()
                val responseCode = connection.responseCode
                if(responseCode == HttpURLConnection.HTTP_OK){
                    val inputStream = connection.inputStream
                    val response = inputStream.bufferedReader().use { it.readText() }
                    result = response
                }
                else{
                    result = "{\n" +
                            "    \"msg\": \"Error\",\n" +
                            "    \"items\": []\n" +
                            "}"
                }

            }
            catch (e: Exception) {
                e.printStackTrace()
            } finally {
                connection.disconnect()
            }

        }
        progressbarSetting(pb)
        return result
    }


    private suspend fun sendDataToSAP(inputData:T06.BarcodeData,badgeNum:String,fromBin:String,toBin:String){
        var APIResult = String()
        withContext(Dispatchers.IO){
            var url = URL("http://172.16.206.19/BARCODEWEBAPI/API/TRANSFERBCODE")
            var payLoad = "{\"PART_NUM\": \"${inputData.pART_NO}\",\"REEL_NUM\": \"${inputData.rEEL_NO}\",\"QUANTITY\": \"${qtyED.text.toString()}\"," +
                    "\"LOT_NO\": \"${inputData.lOT}\",\"UOM\": \"PC\",\"BADGE\": \"${badgeNum}\",\"FROM_BIN\": \"${fromBin}\",\"TO_BIN\": \"${toBin}\"}"
            val connection = url.openConnection() as HttpURLConnection
            // Set request method to POST
            connection.requestMethod = "POST"
            // Enable output for sending data
            connection.doOutput = true
            // Set request headers
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.setRequestProperty("Accept", "application/json")
            try{
                // Write JSON data to the connection output stream
                val wr = DataOutputStream(connection.outputStream)
                wr.write(payLoad.toByteArray(Charsets.UTF_8))
                wr.flush()
                wr.close()
                // Get response
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read response
                    val inputStream = BufferedReader(InputStreamReader(connection.inputStream))
                    var inputLine: String?
                    val response = StringBuffer()

                    while (inputStream.readLine().also { inputLine = it } != null) {
                        response.append(inputLine)
                    }
                    inputStream.close()
                    APIResult = response.toString()
                }
                else{
                    APIResult = "{\"type\" : \"E\",\"message\":\"INTERNAL SERVER ERROR\"}"
                }
            }
            catch (e: Exception) {
                e.printStackTrace()
            } finally {
                connection.disconnect()
            }

            val jobj = JSONObject(APIResult)
            val typ = jobj.getString("type")
            val msg = jobj.getString("message")
            TriggerAlert(ct, "$typ : $msg")
        }
    }

    private suspend fun translateBarcode(input: String):String{
        var APIResult:String = ""
        val response = StringBuilder()
        withContext(Dispatchers.IO){
            var url = URL("http://172.16.206.19/BARCODEWEBAPI/API/BARCODETRANSLATOR")
            val payLoad = "\"${input}\""
            val conn = url.openConnection() as HttpURLConnection
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Content-Length", payLoad.length.toString())
            DataOutputStream(conn.getOutputStream()).use { it.writeBytes(payLoad) }
            BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                var inputLine: String?
                while (reader.readLine().also { inputLine = it } != null) {
                    response.append(inputLine)
                }

            }
            conn.disconnect()

        }
        return  response.toString()
    }

    private suspend fun retrieveBinQuantity(materialPara:String):Int{
        var ApiResult:Int = 0
        withContext(Dispatchers.IO){
            var url = URL("http://172.16.206.19/BARCODEWEBAPI/api/KANBANQUANTITY?material=${materialPara}")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            val responseCode = connection.responseCode
            val response = StringBuilder()

            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                var inputLine: String?
                while (reader.readLine().also { inputLine = it } != null) {
                    response.append(inputLine)
                }

            }
            connection.disconnect()

            val jsonResult = JSONArray(response.toString())
            val json = jsonResult.getJSONObject(0).getString("qtY_PER_BIN")
            val binTypStr = jsonResult.getJSONObject(0).getString("biN_TYPE")
            runOnUiThread(kotlinx.coroutines.Runnable {
                binTyp.setText(binTypStr)
            })

            ApiResult = json.toInt()
        }

        return ApiResult
    }

    private fun TriggerAlert(c:Context,message:String){
        runOnUiThread(kotlinx.coroutines.Runnable{
            val builder = AlertDialog.Builder(c)

            builder.setTitle("Message")
            builder.setMessage(message)
            builder.setPositiveButton("OK") { dialog, which ->
                // Do something when OK button is clicked
                resetForm()
            }
            builder.show()
        })
    }

    private fun progressbarSetting(v: View){
        runOnUiThread(Runnable {
            if (v.visibility == View.VISIBLE) {
                v.visibility = View.INVISIBLE
            } else {
                v.visibility = View.VISIBLE
            }

        })
    }

}