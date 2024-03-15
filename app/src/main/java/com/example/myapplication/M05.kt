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
import java.net.URL

class M05 : AppCompatActivity() {
    private lateinit var pb:ProgressBar
    private lateinit var c:Context
    private lateinit var title: TextView
    private lateinit var btnSave:Button
    private lateinit var btnClear:Button
    private lateinit var btnMenu:Button
    private lateinit var edResvNo:EditText
    private lateinit var edPart:EditText
    private lateinit var edQty:EditText
    private lateinit var edfromSloc:EditText


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_m05)
        pb = findViewById(R.id.M05PB)
        c = this@M05
        title = findViewById(R.id.M05TITLE)
        title.text = intent.getStringExtra("Desc").toString()
        btnSave = findViewById(R.id.M05BTNSAVE)
        btnClear = findViewById(R.id.M05BTNCLEAR)
        btnMenu = findViewById(R.id.M05BTNMENU)
        edResvNo = findViewById(R.id.M05EDRESVNO)
        edPart = findViewById(R.id.M05EDPART)
        edQty = findViewById(R.id.M05EDQUANTITY)
        edfromSloc = findViewById(R.id.M05EDFROMSLOC)
        edResvNo.requestFocus()
        var badgeNo = intent.getStringExtra("Badge").toString()
        var deviceID = Build.ID
        edResvNo.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                edPart.requestFocus()
                return@OnKeyListener true
            }
            false
        })

        edPart.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                edQty.requestFocus()
                return@OnKeyListener true
            }
            false
        })

        edQty.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                edfromSloc.requestFocus()
                return@OnKeyListener true
            }
            false
        })

        btnMenu.setOnClickListener {
            finish()
        }
        btnClear.setOnClickListener {
            clearEverything()
        }
        btnSave.setOnClickListener {
            progressbarSetting(pb)
            runBlocking {
                val job = GlobalScope.launch {
                    try{
                        submitToSAP(edPart.text.toString(),edQty.text.toString(),badgeNo,deviceID,edResvNo.text.toString(),edfromSloc.text.toString())
                    }
                    catch(e:Exception){
                        triggerAlert("Attention",e.message.toString())
                    }

                }
                job.join()

            }
        }

    }

    private suspend fun submitToSAP(mat:String, quantity:String, badgeno:String, deviceSnr:String, SMRNum:String, storLoc:String){
        var jsonOBJ = String()
        withContext(Dispatchers.Default){
            val linkUrl = URL("http://172.16.206.19/REST_API/Fourth/MPPM05SubmitSAP?material=${mat}&qty=${quantity}&badgeno=${badgeno}&deviceID=${deviceSnr}&" +
                    "reservationNo=${SMRNum}&fromSloc=$storLoc")
            jsonOBJ = linkUrl.readText()

            progressbarSetting(pb)
        }
        runOnUiThread(kotlinx.coroutines.Runnable {
            val resTemp = jsonOBJ.split(':')
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
        edResvNo.text.clear()
        edPart.text.clear()
        edQty.text.clear()
        edfromSloc.text.clear()
    }

}