package com.example.myapplication

import android.content.Context
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
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

class C02 : AppCompatActivity() {
    private lateinit var btnMenu: Button
    private lateinit var btnSave:Button
    private lateinit var btnClear:Button
    private lateinit var invDoc:EditText
    private lateinit var matScanned:TextView
    private lateinit var qtyScanned:TextView
    private lateinit var c:Context
    private lateinit var pb:ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_c02)
        btnMenu = findViewById(R.id.C02btnMenu)
        btnClear = findViewById(R.id.C02btnClear)
        btnSave = findViewById(R.id.C02btnSave)
        invDoc = findViewById(R.id.C02EDInvDoc)
        matScanned = findViewById(R.id.C02TVMtrlScnd)
        qtyScanned = findViewById(R.id.C02TVQtyScn)
        c = this@C02
        pb = findViewById(R.id.C02PB)

        btnMenu.setOnClickListener {
            finish()
        }

        btnClear.setOnClickListener {
            clearEverything()
        }

        invDoc.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                progressbar_setting(pb)
                var invSplit = invDoc.text.split('$')
                invDoc.setText(invSplit[0])
                runBlocking {
                    val job = GlobalScope.launch {
                        getInvSummary(invSplit[0])
                    }
                    job.join()

                }

                return@OnKeyListener true
            }
            false
        })

        btnSave.setOnClickListener {
            progressbar_setting(pb)
            var res = String()
            runBlocking {
                val job = GlobalScope.launch {
                    res = submitInventory(invDoc.text.toString())
                }
                job.join()
                triggerAlert("Message",res)
            }
        }

    }

    private suspend fun submitInventory(invNum: String):String{
        var result = String()
        var linkString = "http://172.16.206.19/REST_API/Third/MPPSendToDB?invDoc=${invNum}"
        withContext(Dispatchers.IO){
            result = URL(linkString).readText()
        }
        progressbar_setting(pb)
        return result
    }

    private suspend fun getInvSummary(invNum:String){

        var linkString = "http://172.16.206.19/REST_API/Third/MPPRetrieveSummary?invDoc=${invNum}"

        withContext(Dispatchers.IO){
            var result = JSONObject(URL(linkString).readText())
            matScanned.text = c.getString(R.string.material_scanned_para,result.getString("ITEM"))
            qtyScanned.text = c.getString(R.string.quantity_scanned_para,result.getString("QUANTITY"))
        }
        progressbar_setting(pb)

    }

    private fun clearEverything(){
        invDoc.text.clear()
        invDoc.requestFocus()
        matScanned.text = getString(R.string.material_scanned)
        qtyScanned.text = getString(R.string.quantity_scanned)

    }
    private fun progressbar_setting(v: View){
        runOnUiThread(kotlinx.coroutines.Runnable {
            if (v.visibility == View.VISIBLE) {
                v.visibility = View.GONE
            } else {
                v.visibility = View.VISIBLE
            }

        })
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