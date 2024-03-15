package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings.Global
import android.telephony.TelephonyManager
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_t30)
        menuBtn = findViewById(R.id.T30btnMenu)
        clearBtn = findViewById(R.id.T30btnClear)
        saveBtn = findViewById(R.id.T30btnSave)

        barcodeInput = findViewById(R.id.T30EDbarcode)
        partInput = findViewById(R.id.T30edPart)
        uomInput = findViewById(R.id.T30edUom)
        uomInput.setText("PC")
        partOnKBInput = findViewById(R.id.T30edPartOnKB)
        qtyInput = findViewById(R.id.T30edQTY)
        batchInput = findViewById(R.id.T30edBatch)
        fromBinInput = findViewById(R.id.T30edFromBin)
        toBinInput = findViewById(R.id.T30edToBin)
        pb = findViewById(R.id.T30PB)
        val Bnum = intent.getStringExtra("Badge").toString()
        c = this

        menuBtn.setOnClickListener {
            finish()
        }

        clearBtn.setOnClickListener {
            clearEverything()
        }

        saveBtn.setOnClickListener {

        }
        barcodeInput.requestFocus()

        barcodeInput.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                progressbar_setting(pb)
                runBlocking {
                    GlobalScope.launch {
                        var barcodeResult = breakBarcodeToString(barcodeInput.text.toString())
                        partInput.setText(barcodeResult.PART_NO)
                        if (barcodeResult.PART_NO.isNotBlank() &&qtyInput.text.isBlank()){
                            var qty = getQtyPerBin(barcodeResult.PART_NO)
                            qtyInput.setText(qty.toString())
                        }
                        batchInput.setText(barcodeResult.LOT)
                        cartonNum = barcodeResult.REEL_NO
                    }
                }

                partOnKBInput.requestFocus()
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
                            triggerAlert("Error","WI Problem cannot update WI")
                        }
                    }


                }
                else{
                    triggerAlert("Error","Kanban and Carton not same")
                }
                return@OnKeyListener true
            }
            false
        })

        toBinInput.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code

                return@OnKeyListener true
            }
            false
        })

        saveBtn.setOnClickListener {
            var deviceID = Build.ID
            var message = String()
            progressbar_setting(pb)
            runBlocking {
                val job = GlobalScope.launch {
                    message = sendToSAP(fromBinInput.text.toString(),toBinInput.text.toString(),
                        partInput.text.toString(),Bnum,batchInput.text.toString(),qtyInput.text.toString(),
                        deviceID,
                        cartonNum)
                }
                job.join()
                var messageSplit  = message.split(':')

                triggerAlert(messageSplit[0],messageSplit[1])
            }

        }


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
        var link =
            "http://172.16.206.19/REST_API/Third/MPPT30SubmitData?fromBIN=${fromBin}&toBin=${toBin}&material=${pMaterial}&badgeno=${badge}&lotNo=${batchNum}" +
                    "&qty=${quantity}&iMeiNo=${deviceImei}&cartonNum=${cartonNumber}"
        withContext(Dispatchers.Default){
            var urlLink = URL(link)
            finalResult = urlLink.readText()
        }
        progressbar_setting(pb)

        return finalResult

    }

    private suspend fun breakBarcodeToString(bc:String):barcodeData{
        var result = barcodeData("","","","","","","")

        try{
            withContext(Dispatchers.Default){
                var jsonOBJ = JSONObject()
                val linkUrl = URL(getString(R.string.barcodeTranslator,bc))
                jsonOBJ = JSONObject(linkUrl.readText())
                result = barcodeData(jsonOBJ.getString("VENDOR"),jsonOBJ.getString("DATE")
                    ,jsonOBJ.getString("PART_NO"),jsonOBJ.getString("REEL_NO"),jsonOBJ.getString("LOT"),
                    jsonOBJ.getString("QUANTITY"),jsonOBJ.getString("UOM"))
            }

        }
        catch (ex:Exception){
            Toast.makeText(c, ex.message.toString(), Toast.LENGTH_SHORT).show()
        }
        finally{
            progressbar_setting(pb)

        }
        return result

    }

    private fun triggerAlert(title:String,msg:String){
        val builder = AlertDialog.Builder(c)
        builder.setTitle(title)
        builder.setMessage(msg)
        builder.setPositiveButton("OK") { dialog, which ->
            // Do something when OK button is clicked
            clearEverything()
        }
        builder.show()
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