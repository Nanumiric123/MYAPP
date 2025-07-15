package com.example.myapplication

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class M01 : AppCompatActivity() {
    private lateinit var pb: ProgressBar
    private lateinit var title:TextView
    private lateinit var btnMenu: Button
    private lateinit var btnClear: Button
    private lateinit var btnSave: Button
    private lateinit var edBarcode: EditText
    private lateinit var resvNo: EditText
    private lateinit var edPart: EditText
    private lateinit var edQty: EditText
    private lateinit var edLot:EditText
    private lateinit var fromLoc: EditText
    private lateinit var fromStorLoc: EditText
    private lateinit var toLoc: EditText
    private lateinit var toStorLoc: EditText
    private lateinit var c:Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_m01)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        title = findViewById(R.id.M01TVTITLE)
        btnMenu = findViewById(R.id.M01BtnMenu)
        btnSave = findViewById(R.id.M01BtnSave)
        btnClear = findViewById(R.id.M01BtnClear)
        edBarcode = findViewById(R.id.M01EDBarcode)
        resvNo = findViewById(R.id.M01EDResvNo)
        edPart = findViewById(R.id.M01EDPart)
        fromLoc = findViewById(R.id.M01EDFromLoc)
        fromStorLoc = findViewById(R.id.M01EDFromStorLoc)
        toLoc = findViewById(R.id.M01EDToLoc)
        toStorLoc = findViewById(R.id.M01EDToSloc)
        pb = findViewById(R.id.M01PB)
        edQty = findViewById(R.id.M01EDQty)
        edLot = findViewById(R.id.M01EDLotNo)
        c = this@M01

        title.text = intent.getStringExtra("Desc").toString()
        var badgeNo = intent.getStringExtra("Badge").toString()
        var deviceID = Build.ID
        resvNo.requestFocus()

        btnClear.setOnClickListener {
            clearEverything()
        }
        btnMenu.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            progressbarSetting(pb)
            var results = String()
            runBlocking {
                val job = GlobalScope.launch {
                    if(!resvNo.text.toString().isNullOrBlank() || !edPart.text.toString().isNullOrBlank() ||
                        !edQty.text.toString().isNullOrBlank() || !fromStorLoc.text.toString().isNullOrBlank() ||
                        !toLoc.text.toString().isNullOrBlank() || !toStorLoc.text.toString().isNullOrBlank() ||
                        !fromLoc.text.toString().isNullOrBlank() || !edLot.text.toString().isNullOrBlank()){
                        results = submitToSAP(edPart.text.toString(),edQty.text.toString(),badgeNo,edLot.text.toString(),deviceID,resvNo.text.toString(),fromLoc.text.toString()
                            ,fromStorLoc.text.toString(),toLoc.text.toString(),"","",toStorLoc.text.toString())
                    }
                    else{
                        triggerAlert("ERROR","Scan Material Barcode")
                    }

                }
                job.join()
                var splitResult = results.split(':')
                triggerAlert(splitResult[0],splitResult[1])
            }



        }

        resvNo.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                edBarcode.requestFocus()
                return@OnKeyListener true
            }
            false
        })

        edBarcode.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                progressbarSetting(pb)
                runBlocking {

                    val job = GlobalScope.launch {
                        translateBarcode(edBarcode.text.toString())
                    }
                    job.join()
                    fromStorLoc.requestFocus()

                }



                return@OnKeyListener true
            }
            false
        })
        fromStorLoc.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                fromLoc.requestFocus()
                return@OnKeyListener true
            }
            false
        })
        fromLoc.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                toStorLoc.requestFocus()
                return@OnKeyListener true
            }
            false
        })

        toStorLoc.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                toLoc.requestFocus()
                return@OnKeyListener true
            }
            false
        })

    }

    private suspend fun submitToSAP(mat:String, q:String, badgeNum:String, batchNum:String, deviceSnr:String, resvNo:String, srcBin:String,
                                    srcStorLoc:String, destBin:String, srcStorType:String, destStrTyp:String, destStrLoc:String):String{
        var result = String()
        var urlString = "http://172.16.206.19/REST_API/Fourth/MPQM01SubmitSAP?material=${mat}&qty=${q}&badgeno=${badgeNum}&batch=${batchNum}&deviceID=${deviceSnr}&" +
                "reservationNo=${resvNo}&fromLoc=${srcBin}&fromSloc=${srcStorLoc}&toLoc=${destBin}&toStyp=${destStrTyp}&fromStyp=${srcStorType}&toSloc=${destStrLoc}"
        withContext(Dispatchers.Default){
            progressbarSetting(pb)
            result = URL(urlString).readText()
        }
        return result
    }

    private suspend fun translateBarcode(bc:String){
        var jsonOBJ = JSONObject()
        withContext(Dispatchers.Default){
            val linkUrl = URL(getString(R.string.barcodeTranslator,bc))
            jsonOBJ = JSONObject(linkUrl.readText())

            progressbarSetting(pb)
        }
        runOnUiThread(kotlinx.coroutines.Runnable {
            edPart.setText(jsonOBJ.getString("PART_NO"))
            edQty.setText(jsonOBJ.getString("QUANTITY"))
            edLot.setText(jsonOBJ.getString("LOT"))
        })

    }

    private fun clearEverything(){
        edBarcode.text.clear()
        resvNo.text.clear()
        edPart.text.clear()
        fromLoc.text.clear()
        fromStorLoc.text.clear()
        toLoc.text.clear()
        toStorLoc.text.clear()
        edQty.text.clear()
        edLot.text.clear()

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

    private fun progressbarSetting(v: View){
        runOnUiThread(kotlinx.coroutines.Runnable {
            if (v.visibility == View.VISIBLE) {
                v.visibility = View.GONE
            } else {
                v.visibility = View.VISIBLE
            }

        })
    }

}