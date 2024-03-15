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

class M06 : AppCompatActivity() {
    private lateinit var title: TextView
    private lateinit var edResvNo: EditText
    private lateinit var edPart: EditText
    private lateinit var edQty: EditText
    private lateinit var edFromSloc: EditText
    private lateinit var edFromLoc: EditText
    private lateinit var edToSloc: EditText
    private lateinit var edToLoc: EditText
    private lateinit var c: Context
    private lateinit var pb: ProgressBar
    private lateinit var btnSave: Button
    private lateinit var btnClear: Button
    private lateinit var btnMenu: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_m06)
        title = findViewById(R.id.M06TITLE)
        title.text = intent.getStringExtra("Desc").toString()
        c = this@M06
        pb = findViewById(R.id.M06PB)
        edResvNo = findViewById(R.id.M06EDresvNo)
        edPart = findViewById(R.id.M06EDPart)
        edQty = findViewById(R.id.M06EDQty)
        edFromLoc = findViewById(R.id.M06EDFROMLOC)
        edFromSloc = findViewById(R.id.M06EDFROMSLOC)
        edToLoc = findViewById(R.id.M06EDTOLOC)
        edToSloc = findViewById(R.id.M06EDTOSLOC)
        btnSave = findViewById(R.id.M06BtnSave)
        btnMenu = findViewById(R.id.M06BTNMENU)
        btnClear = findViewById(R.id.M06BTNCLEAR)

        val badgeNo = intent.getStringExtra("Badge").toString()
        val deviceID = Build.ID
        btnSave.setOnClickListener {
            progressbarSetting(pb)
            runBlocking {
                val job = GlobalScope.launch {
                    submitToSAP(edPart.text.toString(),edQty.text.toString(),badgeNo,deviceID, edResvNo.text.toString(),
                        edFromSloc.text.toString(),edToSloc.text.toString(),edToLoc.text.toString())
                }
                job.join()
            }
        }
        btnClear.setOnClickListener {
            clearEverything()
        }
        btnMenu.setOnClickListener {
            finish()
        }
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
                edFromSloc.requestFocus()
                return@OnKeyListener true
            }
            false
        })
        edFromSloc.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                edToSloc.requestFocus()
                return@OnKeyListener true
            }
            false
        })
        edToSloc.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                edToLoc.requestFocus()
                return@OnKeyListener true
            }
            false
        })



    }

    private suspend fun submitToSAP(mat:String,quantity:String,badgeNo:String,deviceID:String,reservationNum:String,fromSloc:String,toSLoc:String,toLoc:String){
        var jsonOBJ = String()
        withContext(Dispatchers.Default){
            val linkUrl = URL("http://172.16.206.19/REST_API/Fourth/MPQM06SubmitSAP?material=${mat}&qty=${quantity}&badgeno=${badgeNo}&deviceID=${deviceID}&" +
                    "reservationNo=${reservationNum}&fromSloc=${fromSloc}&toSloc=${toSLoc}&toLoc=${toLoc}")
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
    private fun clearEverything() {
        edFromSloc.text.clear()
        edFromLoc.text.clear()
        edToLoc.text.clear()
        edToSloc.text.clear()
        edResvNo.text.clear()
        edQty.text.clear()
        edPart.text.clear()

    }

}