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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class M03 : AppCompatActivity() {
    private lateinit var title:TextView
    private lateinit var c: Context
    private lateinit var resvNo:EditText
    private lateinit var itemBcode:EditText
    private lateinit var part:EditText
    private lateinit var qty:EditText
    private lateinit var lotNum:EditText
    private lateinit var fromSloc:EditText
    private lateinit var fromLoc:EditText
    private lateinit var pb:ProgressBar
    private lateinit var btnMenu:Button
    private lateinit var btnClear:Button
    private lateinit var btnSave:Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_m03)
        title = findViewById(R.id.M03Title)
        pb = findViewById(R.id.M03PB)
        title.text = intent.getStringExtra("Desc").toString()
        c = this@M03
        var badgeNo = intent.getStringExtra("Badge").toString()
        var deviceID = Build.ID
        resvNo = findViewById(R.id.M03EDRESVNO)
        itemBcode = findViewById(R.id.M03EDBARCODE)
        part = findViewById(R.id.M03EDPART)
        qty = findViewById(R.id.M03EDQTY)
        lotNum = findViewById(R.id.M03EDLOTNO)
        fromSloc = findViewById(R.id.M03EDFROMSLOC)
        fromLoc = findViewById(R.id.M03EDFROMLOC)
        btnClear = findViewById(R.id.M03btnClear)
        btnMenu = findViewById(R.id.M03btnMenu)
        btnSave = findViewById(R.id.M03btnSave)

        btnClear.setOnClickListener {
            clearEverything()
        }

        btnMenu.setOnClickListener {
            finish()
        }
        resvNo.requestFocus()
        resvNo.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                itemBcode.requestFocus()
                return@OnKeyListener true
            }
            false
        })

        itemBcode.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                progressbarSetting(pb)
                var results = barcodeData("","","","","","","")
                runBlocking {
                    val job = GlobalScope.launch {
                        results = translateBarcode(itemBcode.text.toString())
                    }
                    job.join()
                    part.setText(results.PART_NO)
                    qty.setText(results.QUANTITY)
                    lotNum.setText(results.LOT)
                    fromSloc.requestFocus()
                }
                return@OnKeyListener true
            }
            false
        })

        part.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code

                return@OnKeyListener true
            }
            false
        })

        qty.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code

                return@OnKeyListener true
            }
            false
        })

        lotNum.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code

                return@OnKeyListener true
            }
            false
        })

        fromSloc.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
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

                return@OnKeyListener true
            }
            false
        })

        btnSave.setOnClickListener{
            progressbarSetting(pb)
            runBlocking {
                val job = GlobalScope.launch {
                    submitToSAP(part.text.toString(),qty.text.toString(),badgeNo,lotNum.text.toString(),
                        deviceID,resvNo.text.toString(),fromLoc.text.toString(),fromSloc.text.toString())
                }
                job.join()

            }
        }

    }

    private suspend fun translateBarcode(bc:String):barcodeData{
        var jsonOBJ = JSONObject()
        withContext(Dispatchers.Default){
            val linkUrl = URL(getString(R.string.barcodeTranslator,bc))
            jsonOBJ = JSONObject(linkUrl.readText())

            progressbarSetting(pb)
        }

        return barcodeData(jsonOBJ.getString("VENDOR"),jsonOBJ.getString("DATE"),jsonOBJ.getString("PART_NO"),jsonOBJ.getString("REEL_NO"),jsonOBJ.getString("LOT"),jsonOBJ.getString("QUANTITY"),jsonOBJ.getString("UOM"))

    }

    private suspend fun submitToSAP(mat:String, quantity:String, badgeno:String, batchNum:String, deviceSnr:String, SMRNum:String, binNum:String, storLoc:String){
        var jsonOBJ = String()
        withContext(Dispatchers.Default){
            val linkUrl = URL("http://172.16.206.19/REST_API/Fourth/MPPM03SubmitSAP?material=${mat}&qty=${quantity}&badgeno=${badgeno}&batch=${batchNum}&" +
                    "deviceID=${deviceSnr}&reservationNo=${SMRNum}&fromLoc=${binNum}&fromSloc=${storLoc}")
            jsonOBJ = linkUrl.readText()

            progressbarSetting(pb)
        }
        runOnUiThread(kotlinx.coroutines.Runnable {
            var resTemp = jsonOBJ.split(':')
            triggerAlert(resTemp[0],resTemp[1])
        })

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

    private fun clearEverything(){
        itemBcode.text.clear()
        resvNo.text.clear()
        part.text.clear()
        fromLoc.text.clear()
        fromSloc.text.clear()
        qty.text.clear()
        lotNum.text.clear()

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
}