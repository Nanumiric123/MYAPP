package com.example.myapplication

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import androidx.core.graphics.toColorInt
import androidx.core.graphics.drawable.toDrawable
import java.io.DataOutputStream

class T30 : AppCompatActivity() {
    private lateinit var menuBtn:Button
    private lateinit var  clearBtn:Button
    private lateinit var saveBtn:Button
    private lateinit var barcodeInput:EditText
    private lateinit var partInput:EditText
    private lateinit var uomInput:EditText
    private lateinit var partOnKBInput:EditText
    private lateinit var qtyInput:EditText
    private lateinit var batchInput:EditText
    private lateinit var fromBinInput:EditText
    private lateinit var toBinInput:EditText
    private lateinit var c:Context
    private lateinit var cartonNum:String
    private lateinit var pb:ProgressBar
    private lateinit var cf:commonFunctions
    private lateinit var statusInd: TextView
    //green #6db261
    //red #d24e4e
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_t30)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        menuBtn = findViewById(R.id.T30btnMenu)
        clearBtn = findViewById(R.id.T30btnClear)
        saveBtn = findViewById(R.id.T30btnSave)
        cf = commonFunctions()
        barcodeInput = findViewById(R.id.T30EDbarcode)
        partInput = findViewById(R.id.T30edPart)
        uomInput = findViewById(R.id.T30edUom)
        uomInput.setText("PC")
        partOnKBInput = findViewById(R.id.T30edPartOnKB)
        qtyInput = findViewById(R.id.T30edQTY)
        batchInput = findViewById(R.id.T30edBatch)
        fromBinInput = findViewById(R.id.T30edFromBin)
        toBinInput = findViewById(R.id.T30edToBin)
        statusInd = findViewById(R.id.T30STATUSIND)
        pb = findViewById(R.id.T30PB)
        val Bnum = intent.getStringExtra("Badge").toString()
        c = this

        menuBtn.setOnClickListener {
            finish()
        }

        clearBtn.setOnClickListener {
            clearEverything()
        }

        barcodeInput.requestFocus()

        barcodeInput.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                progressbar_setting(pb)
                runBlocking {
                    GlobalScope.launch {
                        val barcodeResult = breakBarcodeToString(barcodeInput.text.toString())

                        if (barcodeResult.PART_NO.isNullOrBlank() && barcodeResult.LOT.isNullOrBlank()){
                            runOnUiThread(kotlinx.coroutines.Runnable{
                                val dialog = cf.showMessage(c,"ERROR","ERROR, SCAN AGAIN","OK", positiveButtonAction = {
                                    barcodeInput.requestFocus()
                                    barcodeInput.text.clear()
                                    statusInd.text = "ERROR, SCAN AGAIN"
                                    statusInd.setTextColor(Color.RED)
                                })
                                dialog.show()
                                // Set background color AFTER show()
                                dialog.window?.setBackgroundDrawable(
                                    ColorDrawable(
                                        Color.parseColor(
                                            "#d24e4e"
                                        )
                                    )
                                )
                            })

                        }
                        else{
                            runOnUiThread(kotlinx.coroutines.Runnable{
                                var qty = getQtyPerBin(barcodeResult.PART_NO)
                                qtyInput.setText(qty.toString())
                                partInput.setText(barcodeResult.PART_NO)
                                batchInput.setText(barcodeResult.LOT)
                                cartonNum = barcodeResult.REEL_NO
                                partOnKBInput.requestFocus()

                            })

                        }

                    }
                }


                return@OnKeyListener true
            }
            false
        })

        partOnKBInput.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                if(partInput.text.toString() == partOnKBInput.text.toString()){

                    var res = String()
                    progressbar_setting(pb)
                    runBlocking {
                        val job = GlobalScope.launch {
                            res = UpdateWI(partInput.text.toString(),Bnum)
                        }
                        job.join()
                        if(res == "Successful"){

                            toBinInput.requestFocus()
                        }
                        else{
                            triggerAlert("Error","WI Problem cannot update WI","#d24e4e")
                        }
                    }


                }
                else{
                    triggerAlert("Error","Kanban and Carton not same","#d24e4e")
                }
                return@OnKeyListener true
            }
            false
        })

        toBinInput.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                CoroutineScope(Dispatchers.IO).launch{
                    try {
                        withContext(Dispatchers.Main){
                            progressbar_setting(pb)
                        }
                        val matInBin = itemInBin(toBinInput.text.toString())
                        if (!matInBin.isNullOrBlank()){
                            withContext(Dispatchers.Main){
                                var dialog = cf.showDialog(c,"Warning","Bin ${toBinInput.text.toString()} contains item ${matInBin}, Confirm transfer?","Yes","No"
                                    , positiveButtonAction = {
                                        statusInd.setTextColor(Color.parseColor("#e1dd56"))
                                        statusInd.text = "Bin ${toBinInput.text.toString()} contains item ${matInBin}"
                                }
                                , negativeButtonAction = {
                                        clearEverything()
                                        barcodeInput.requestFocus()
                                        statusInd.setTextColor(Color.parseColor("#e1dd56"))
                                        statusInd.text = "Bin ${toBinInput.text.toString()} contains item ${matInBin}"
                                    })
                                dialog.show()
                                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#e1dd56")))
                            }
                        }
                    }
                    catch(ex: Exception){
                        withContext(Dispatchers.Main){
                            var dialog = cf.showMessage(c,"Error",ex.message.toString(),"OK", positiveButtonAction = {

                            })
                            dialog.show()
                            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#d24e4e")))
                        }
                    }
                    finally {
                        withContext(Dispatchers.Main){
                            progressbar_setting(pb)
                        }

                    }
                }
                return@OnKeyListener true
            }
            false
        })

        saveBtn.setOnClickListener {
            var deviceID = Build.ID

            CoroutineScope(Dispatchers.Main).launch {
                var message = String()
                if(checkCartonQty(partInput.text.toString(),cartonNum,qtyInput.text.toString(),batchInput.text.toString())){
                    withContext(Dispatchers.Main){

                        triggerAlert("Error","Carton quantity is empty","#d24e4e")
                    }
                }
                else{
                    message = sendToSAP(fromBinInput.text.toString(),toBinInput.text.toString(),
                        partInput.text.toString(),Bnum,batchInput.text.toString(),qtyInput.text.toString(),
                        deviceID,
                        cartonNum)
                    withContext(Dispatchers.Main){
                        var messageSplit  = message.split(':')
                        barcodeInput.text.clear()
                        if(messageSplit[0].contains('E') || messageSplit[0].contains('F')){
                            val jsonObj = JSONObject(message)
                            triggerAlert(jsonObj.getString("type"),jsonObj.getString("message"),"#d24e4e")

                        }
                        else{
                            val jsonObj = JSONObject(message)
                            statusInd.setTextColor(Color.parseColor("#2DAB28"))
                            statusInd.text = "Success : ${jsonObj.getString("message")}"
                            clearEverything()
                        }

                    }
                }

            }
        }


    }
    private suspend fun itemInBin(BinNum:String): String{
        return withContext(Dispatchers.IO){
            val linkURL = URL("http://172.16.206.19/REST_API/Second/Q01StorageBin?storageBin=$BinNum")
            val urlText = linkURL.readText()
            val jsonArray = JSONArray(urlText)
            if(jsonArray.length() >= 1){
                val JSONresult = jsonArray.getJSONObject(0)
                JSONresult.getString("MATERIAL")
            }
            else{
                ""
            }

        }
    }

    private suspend fun breakBarcodeToString(bc:String):barcodeData{
        var result = barcodeData("","","","","","","")

        try{
            withContext(Dispatchers.IO){
                var jsonOBJ = JSONObject()
                val linkUrl = URL("http://172.16.206.19/REST_API/Home/breAKBarcodeString?barcode=${bc}")
                val urlText = linkUrl.readText()
                try {
                    jsonOBJ = JSONObject(urlText)
                    result = barcodeData(jsonOBJ.getString("VENDOR"),jsonOBJ.getString("DATE")
                        ,jsonOBJ.getString("PART_NO"),jsonOBJ.getString("REEL_NO"),jsonOBJ.getString("LOT"),
                        jsonOBJ.getString("QUANTITY"),jsonOBJ.getString("UOM"))
                }
                catch (ex: Exception){

                }
                finally {

                }

            }

        }
        catch (ex:Exception){
            Toast.makeText(c, ex.message.toString(), Toast.LENGTH_SHORT).show()
        }
        finally{
            withContext(Dispatchers.Main){
                progressbar_setting(pb)
            }


        }
        return result

    }
    private fun progressbar_setting(v:View){
        runOnUiThread(kotlinx.coroutines.Runnable {
            if (v.visibility == View.VISIBLE) {
                v.visibility = View.GONE
            } else {
                v.visibility = View.VISIBLE
            }

        })
    }
    private fun getQtyPerBin(mat:String):Int{
        val linkString = "http://172.16.206.19/REST_API/Third/retrieveQuantityPerBin?mat=${mat}"
        var res = 0.00

        try{
            val linkURL = URL(linkString)
            runBlocking {
                val job = GlobalScope.launch {
                    res = linkURL.readText().toDouble()
                }
                job.join()
                //final_res = res
            }
        }
        catch (e:Exception){

        }
        finally {

        }
        return res.toInt()
    }

    private suspend fun checkCartonQty(mat:String,cartonNum:String,inputQty:String,batchNo:String): Boolean{
        var result = String()
        var materialNumber = String()

        materialNumber = if(mat.contains('+')){
            mat.replace("+","%2B")
        } else{
            mat
        }
        return withContext(Dispatchers.IO){
            val url = URL("http://172.16.206.19/FORD_SYNC/API/T30?material=${materialNumber}&cartonNo=${cartonNum}&batch=${batchNo}&qty=${inputQty}")
            val connection = url.openConnection() as HttpURLConnection

            // Optional: Set request method to GET (GET is the default)
            connection.requestMethod = "GET"
            withContext(Dispatchers.Main){
                progressbar_setting(pb)
            }
            try{
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
                    response.toString().toBoolean()
                }
                else {
                    true
                }

            }
            catch(e:Exception){
                true
            }
            finally {
                withContext(Dispatchers.Main){
                    progressbar_setting(pb)
                }
                connection.disconnect()
            }
        }
    }

    private suspend fun UpdateWI(mat:String,badge:String):String{
        var linkStr = "http://172.16.206.19/REST_API/Third/InsertWI?newMaterial=${mat}&badgeNum=${badge}"

        var result = String()
        var linkURL:URL = URL(linkStr)
        try {
            withContext(Dispatchers.Default){

                result = linkURL.readText()
            }


        }
        catch (e:Exception){
            var error = e.message.toString()
        }
        finally {

        }
        progressbar_setting(pb)
        return result.substring(1, result.length - 1);
    }

    private suspend fun sendToSAP(
        fromBin: String, toBin: String, pMaterial: String, badge: String, batchNum: String,
        quantity: String, deviceImei: String, cartonNumber: String
    ): String {

        var finalResult = ""
        val payLoad = "{\n" +
                "  \"froM_BIN\": \"${fromBin}\",\n" +
                "  \"tO_BIN\": \"${toBin}\",\n" +
                "  \"material\": \"${pMaterial}\",\n" +
                "  \"badgE_ID\": \"${badge}\",\n" +
                "  \"batcH_NO\": \"${batchNum}\",\n" +
                "  \"quantity\": ${quantity},\n" +
                "  \"devicE_NO\": \"${deviceImei}\",\n" +
                "  \"cartoN_NO\": \"${cartonNumber}\"\n" +
                "}"
        withContext(Dispatchers.IO){
            try{

                withContext(Dispatchers.Main){
                    progressbar_setting(pb)
                }
                val RESTUrl = URL("http://172.16.206.19/FORD_SYNC/API/T30")
                val connection = RESTUrl.openConnection() as HttpURLConnection
                // Set request method to POST
                connection.requestMethod = "POST"
                // Enable output for sending data
                connection.doOutput = true
                // Set request headers
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connection.setRequestProperty("Accept", "application/json")
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
                    finalResult = response.toString()
                }
                else{
                    finalResult = "F:INTERNAL SERVER ERROR CODE : $responseCode"
                }
                connection.disconnect()
            }

            catch(ex: Exception){
                finalResult = "F:${ex.message.toString()}"
            }
            finally {
                withContext(Dispatchers.Main){
                    progressbar_setting(pb)
                }
            }

        }


        return finalResult

    }

    private fun triggerAlert(title:String,msg:String,hexColorCode:String){
        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton("OK") { dialogInterface, _ ->
                statusInd.text = msg
                statusInd.setTextColor(Color.RED)
                clearEverything()
                dialogInterface.dismiss()
            }
            .create()

        dialog.show()
        // Set background color AFTER show()
        dialog.window?.setBackgroundDrawable(hexColorCode.toColorInt().toDrawable())
    }

    private fun clearEverything(){
        barcodeInput.text.clear()
        partInput.text.clear()
        uomInput.text.clear()
        partOnKBInput.text.clear()
        qtyInput.text.clear()
        batchInput.text.clear()
        toBinInput.text.clear()
        barcodeInput.requestFocus()
        cartonNum = String()
    }

}