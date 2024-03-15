package com.example.myapplication

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class T33 : AppCompatActivity() {
    private lateinit var pb:ProgressBar
    private lateinit var btnMenu:Button
    private lateinit var btnClear:Button
    private lateinit var btnSave:Button
    private lateinit var edBarcode:EditText
    private lateinit var edPart:EditText
    private lateinit var edQty:EditText
    private lateinit var edLotNum:EditText
    private lateinit var edFromLoc:EditText
    private lateinit var edToLoc:EditText
    private lateinit var c:Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_t33)
        pb = findViewById(R.id.T33PB)
        btnMenu = findViewById(R.id.T33BtnMenu)
        btnClear = findViewById(R.id.T33BtnClear)
        btnSave = findViewById(R.id.T33BtnSave)
        edBarcode = findViewById(R.id.T33EDBarcode)
        edPart = findViewById(R.id.T33EDPart)
        edQty = findViewById(R.id.T33EDQty)
        edLotNum = findViewById(R.id.T33EDLot)
        edFromLoc = findViewById(R.id.T33EDfromLoc)
        edToLoc = findViewById(R.id.T33EDtoLoc)
        c = this@T33
        edBarcode.requestFocus()
        val bnum = intent.getStringExtra("Badge").toString()
        edBarcode.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                progressbarSetting(pb)
                var barcodeResult = barcodeData("","","","","","","")
                runBlocking {
                    val job = GlobalScope.launch {
                        barcodeResult = breakBarcodeToString(edBarcode.text.toString())
                    }
                    job.join()
                    edPart.setText(barcodeResult.PART_NO)
                    edQty.setText(barcodeResult.QUANTITY)
                    edLotNum.setText(barcodeResult.LOT)
                    edFromLoc.requestFocus()
                    progressbarSetting(pb)
                }

                return@OnKeyListener true
            }
            false
        })

        btnClear.setOnClickListener {
            clearEverything()
        }
        btnMenu.setOnClickListener {
            this.finish()
        }

        btnSave.setOnClickListener {
            var result=String()
            var deviceID = Build.ID
            progressbarSetting(pb)
            runBlocking {
                val job = GlobalScope.launch {
                    result = submitToSAP(edPart.text.toString(),edQty.text.toString(),edLotNum.text.toString(),edFromLoc.text.toString(),edToLoc.text.toString(),deviceID,bnum)
                }
                job.join()
                triggerAlert("Success",result)
                progressbarSetting(pb)
            }
        }

    }

    private suspend fun submitToSAP(partNo:String,qty:String,lotNo:String,fromLoc:String,toLoc:String,deviceCode:String,bnum:String):String{
        var linkString = "http://172.16.206.19/REST_API/Third/MPPT33SubmitData?material=${partNo}&batch=${lotNo}&qty=${qty}&badgeno=${bnum}" +
                "&destBin=${toLoc}&fromBin=${fromLoc}&deviceID=${deviceCode}"
        var result = String()

        try {
            withContext(Dispatchers.IO){
                var linkurl = URL(linkString)
                result = linkurl.readText()
            }

        }
        catch (e:Exception){

        }
        finally {

        }
        return result
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
        edBarcode.text.clear()
        edPart.text.clear()
        edQty.text.clear()
        edLotNum.text.clear()
        edFromLoc.text.clear()
        edToLoc.text.clear()
    }
    private fun progressbarSetting(v:View){
        runOnUiThread(kotlinx.coroutines.Runnable {
            if (v.visibility == View.VISIBLE) {
                v.visibility = View.GONE
            } else {
                v.visibility = View.VISIBLE
            }

        })
    }

}