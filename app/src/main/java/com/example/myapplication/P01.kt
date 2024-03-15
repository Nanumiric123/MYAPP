package com.example.myapplication

import android.content.Context
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
                setProgressBar(pb)

                runBlocking {
                    val job = GlobalScope.launch {
                        var data = generateOrderDetail(inputPOBarcode.text.toString())
                        runOnUiThread(Runnable {
                            inputPart.setText(data.mATERIAL)
                        })

                    }
                    job.join()

                    inputQuantity.requestFocus()
                }

                return@OnKeyListener true
            }
            false
        })

            btnSave.setOnClickListener {

            var deviceID = Build.ID
            var resultSAP = ""
            setProgressBar(pb)
            runBlocking {
                val job = GlobalScope.launch {
                    resultSAP = submitToSAP(inputPOBarcode.text.toString(),inputPart.text.toString(),
                        Bnum,deviceID,inputQuantity.text.toString(),inputToLocation.text.toString())
                }
                job.join()
                var resultSplit = resultSAP.split(':')
                runOnUiThread(kotlinx.coroutines.Runnable {
                    triggerAlert(resultSplit[0],resultSplit[1])
                })

            }
        }
        btnClear.setOnClickListener {
            clearEverything()
        }
        btnMenu.setOnClickListener {
            this.finish()
        }
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

    private suspend fun generateOrderDetail(orderNumber:String):PODetail{
        var linkString = "http://172.16.206.19/REST_API/Third/MPP_P01_GET_ORDER_DETAIL?orderNum=${orderNumber}"
        var data= PODetail(mATERIAL = "", qUANTITY = "")

        withContext(Dispatchers.IO){
            try{
                var linkURL = URL(linkString)
                var obj = JSONObject(linkURL.readText())
                data.mATERIAL = obj.getString("MATERIAL")
                data.qUANTITY = obj.getString("QUANTITY")
            }
            catch (e:Exception){
                runOnUiThread(kotlinx.coroutines.Runnable {
                    triggerAlert("Error",e.message.toString() + " " + orderNumber)
                })

            }


        }
        setProgressBar(pb)
        return data
    }

    private suspend fun submitToSAP(orderNum:String,materialNum:String,badgeNum:String,
                                    deviceID:String,qty:String,toLoc:String):String {
        var linkString = "http://172.16.206.19/REST_API/Third/MPPP01SubmitData" +
                "?material=${materialNum}&batch=${orderNum}&qty=${qty}&badgeno=${badgeNum}&destBin=${toLoc}&deviceID=${deviceID}"
        var result = ""
        withContext(Dispatchers.IO){
            var linkURL = URL(linkString)
            result = linkURL.readText()
        }
        setProgressBar(pb)
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
        inputPOBarcode.text.clear()
        inputPart.text.clear()
        inputQuantity.text.clear()
        inputToLocation.text.clear()
        inputPOBarcode.requestFocus()
    }
}

data class PODetail(var mATERIAL:String, var qUANTITY:String)