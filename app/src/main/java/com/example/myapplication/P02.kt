package com.example.myapplication

import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class P02 : AppCompatActivity() {
    private lateinit var btnMenu:Button
    private lateinit var btnClear:Button
    private lateinit var btnSave:Button
    private lateinit var barcodeInput:EditText
    private lateinit var partInput:EditText
    private lateinit var quantityInput:EditText
    private lateinit var toSLocInput:EditText
    private lateinit var toLocInput:EditText
    private lateinit var fromLocInput:EditText
    private lateinit var pb:ProgressBar
    private lateinit var lotNo:EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_p02)

        btnMenu = findViewById(R.id.P02BtnMenu)
        btnClear = findViewById(R.id.P02BtnClear)
        btnSave = findViewById(R.id.P02BtnSave)
        barcodeInput = findViewById(R.id.P02EDItemBarcode)
        partInput = findViewById(R.id.P02EDPart)
        quantityInput = findViewById(R.id.P02EDQty)
        toSLocInput = findViewById(R.id.P02EDTOSLOC)
        toLocInput = findViewById(R.id.P02EDTOLOC)
        fromLocInput = findViewById(R.id.P02EDfromLoc)
        lotNo = findViewById(R.id.P02EDBatch)
        pb = findViewById(R.id.P02PB)

        btnClear.setOnClickListener {
            clearEverything()
        }
        btnMenu.setOnClickListener {
            this.finish()
        }
        val Bnum = intent.getStringExtra("Badge").toString()
        var deviceID = Build.ID
        fromLocInput.requestFocus()

        fromLocInput.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                var dataFromBin = BINDETAIL(message = String(),
                    material = String(),
                    batch = String(),
                    fifo = String(),
                    fifoBin = String())
                setProgressBar(pb)
                runBlocking {
                    val job = GlobalScope.launch {
                        dataFromBin = generateBinDetail(fromLocInput.text.toString())
                    }
                    job.join()
                    if(dataFromBin.fifo == "TRUE"){
                        partInput.setText(dataFromBin.material)
                        lotNo.setText(dataFromBin.batch)
                        quantityInput.requestFocus()
                    }
                    else{
                        val builder = AlertDialog.Builder(this@P02)
                        builder.setTitle(title)
                        builder.setMessage("Warning : Tak ikut FIFO \n Bin yang betul ikut FIFO ialah : ${dataFromBin.fifoBin}")
                        builder.setPositiveButton("OK") { dialog, which ->
                            // Do something when OK button is clicked
                            partInput.setText(dataFromBin.material)
                            lotNo.setText(dataFromBin.batch)
                            quantityInput.requestFocus()
                        }
                        builder.setNegativeButton("Cancel"){ dialog, which ->
                            clearEverything()
                        }
                        builder.show()
                    }
                }
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

        btnSave.setOnClickListener {
            runBlocking {
                val job = GlobalScope.launch {
                    var res = submitToSAP(partInput.text.toString(),lotNo.text.toString(),
                        toLocInput.text.toString(),fromLocInput.text.toString(),
                        deviceID,toSLocInput.text.toString())
                }
                job.join()

            }
        }

    }

    private suspend fun submitToSAP(part:String,lotNum:String,d_Bin:String,f_Bin:String,deviceImei:String,toStorageLoc:String):String
    {

        var linkString = "http://172.16.206.19/REST_API/Third/MPPP02SubmitData?" +
                "material=${part}&batch=${lotNum}&qty=${quantityInput.text}&badgeno=B0060&destBin=${d_Bin}&" +
                "fromBin=${f_Bin}&deviceID=${deviceImei}&line=${toStorageLoc}"
        var result = String()
        withContext(Dispatchers.IO){
            var linkurl = URL(linkString)
            result = linkurl.readText()
        }
        return result
    }

    private suspend fun generateBinDetail(binNumber:String):BINDETAIL{
        var linkString = "http://172.16.206.19/REST_API/Third/MPPP02GETBINDETAILS?BinNo=${binNumber}"
       var result = BINDETAIL(message = String(),
           material = String(),
           batch = String(),
           fifo = String(),
           fifoBin = String())

        withContext(Dispatchers.IO){
            var linkURL = URL(linkString)
            try{
                var obj = JSONObject(linkURL.readText())
                result.message= obj.getString("MESSAGE")
                result.material = obj.getString("MATERIAL")
                result.batch = obj.getString("BATCH")
                result.fifo = obj.getString("FIFO")
                result.fifoBin = obj.getString("FIFO_BIN")
            }
            catch (e:Exception){
                runOnUiThread(Runnable {
                    val builder = AlertDialog.Builder(this@P02)
                    builder.setTitle("Error")
                    builder.setMessage(e.message.toString())
                    builder.setPositiveButton("OK") { dialog, which ->
                        // Do something when OK button is clicked
                        clearEverything()
                    }
                    builder.show()
                })

            }

        }

        setProgressBar(pb)
        return result
    }

    private fun setProgressBar(v:View){
        runOnUiThread(kotlinx.coroutines.Runnable {
            if (v.visibility == View.VISIBLE) {
                v.visibility = View.GONE
            } else {
                v.visibility = View.VISIBLE
            }

        })
    }
    private fun clearEverything(){
        barcodeInput.text.clear()
        partInput.text.clear()
        quantityInput.text.clear()
        toSLocInput.text.clear()
        toLocInput.text.clear()
        fromLocInput.text.clear()
        fromLocInput.requestFocus()
    }


}

data class BINDETAIL(var message:String,var material:String,var batch:String,var fifo:String,var fifoBin:String)