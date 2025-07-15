package com.example.myapplication

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout.DispatchChangeEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
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
    private lateinit var rbFromLoc:RadioButton
    private lateinit var rbToLoc:RadioButton

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
        rbFromLoc = findViewById(R.id.M06RBFLoc)
        rbToLoc = findViewById(R.id.M06RBTloc)

        val badgeNo = intent.getStringExtra("Badge").toString()
        val deviceID = Build.ID
        btnSave.setOnClickListener {

            if(rbFromLoc.isChecked){
                //Save To db
                CoroutineScope(Dispatchers.Main).launch {
                    insertToDB(badgeNo,edPart.text.toString(),deviceID,edResvNo.text.toString(),edFromSloc.text.toString(),
                        edQty.text.toString())
                }

            }
            else if (rbToLoc.isChecked){
                //Post to SAP
                CoroutineScope(Dispatchers.Main).launch {
                    submitToSAP(edPart.text.toString(),edQty.text.toString(),badgeNo,deviceID, edResvNo.text.toString(),
                        edFromSloc.text.toString(),edToSloc.text.toString(),edToLoc.text.toString())
                }

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
                if(rbFromLoc.isChecked){
                    edPart.requestFocus()
                }
                else if (rbToLoc.isChecked){
                    //retrieve data
                    edPart.requestFocus()
                    CoroutineScope(Dispatchers.Main).launch{

                    }
                }

                return@OnKeyListener true
            }
            false
        })
        edPart.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                if(rbFromLoc.isChecked){
                    edQty.requestFocus()
                }
                else if (rbToLoc.isChecked){
                    CoroutineScope(Dispatchers.Main).launch {
                        retrieveFromData(edPart.text.toString(),edResvNo.text.toString())
                    }
                    edQty.isEnabled = false
                }
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
        withContext(Dispatchers.IO){
            withContext(Dispatchers.Main){
                progressbarSetting(pb)
            }
            //MPQM06SubmitSAP
            val linkUrl = URL("http://172.16.206.19/REST_API/Fourth/MPPM06SubmitSAP?material=${mat}&qty=${quantity}&badgeno=${badgeNo}&deviceID=${deviceID}&" +
                    "reservationNo=${reservationNum}&fromSloc=${fromSloc}&toSloc=${toSLoc}&toLoc=${toLoc}")
            jsonOBJ = linkUrl.readText()

            withContext(Dispatchers.Main){
                progressbarSetting(pb)
                runOnUiThread(kotlinx.coroutines.Runnable {
                    val resTemp = jsonOBJ.split(':')
                    triggerAlert(resTemp[0],resTemp[1])
                })
            }

        }

    }

    private suspend fun insertToDB(badgeNo:String,partNo:String,devID:String,reservationNo:String,fromStorLoc:String,smrQty:String){
        var jsonOBJ = String()
        withContext(Dispatchers.IO){
            withContext(Dispatchers.Main){
                progressbarSetting(pb)
            }
            val linKURL = URL("http://172.16.206.19/REST_API/Fourth/insertToTempScanTable?badgeNumber=${badgeNo}&" +
                    "matNum=${partNo}&devID=${devID}&tcode=M06&cartonNum=20250122075622&resvNo=${reservationNo}&" +
                    "fromSloc=${fromStorLoc}&fromLoc=DUMMY&qty=${smrQty}")
            jsonOBJ = linKURL.readText()

            withContext(Dispatchers.Main){
                progressbarSetting(pb)
                runOnUiThread(kotlinx.coroutines.Runnable {
                    triggerAlert("Message",jsonOBJ)
                })
            }
        }
    }

    private suspend fun retrieveFromData(mat:String,resvNo:String){
        withContext(Dispatchers.IO){
            val linkUrl = URL("http://172.16.206.19/REST_API/Fourth/retrieveM06FromLoc?material=${mat}&tcode=M06&reservationNo=${resvNo}")
            withContext(Dispatchers.Main){
                progressbarSetting(pb)
            }
            var jsonString = linkUrl.readText()
            var jsonArray = JSONArray(jsonString)
            for(i in 0 until jsonArray.length()){
                val jsonObject = jsonArray.getJSONObject(i)
                // Extract string values
                val item = jsonObject.getString("item")
                val reservationNo = jsonObject.getString("reservationNo")
                val fromSloc = jsonObject.getString("fromSloc")
                val fromLoc = jsonObject.getString("fromLoc")
                val qty = jsonObject.getDouble("qty").toInt()

                withContext(Dispatchers.Main){
                    progressbarSetting(pb)
                    runOnUiThread(Runnable{
                        edPart.setText(item)
                        edQty.setText(qty.toString())
                        edFromSloc.setText(fromSloc)
                    })
                }
            }
        }
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
        rbFromLoc.isChecked = true
        edQty.isEnabled = true
        edFromSloc.text.clear()
        edFromLoc.text.clear()
        edToLoc.text.clear()
        edToSloc.text.clear()
        edResvNo.text.clear()
        edQty.text.clear()
        edPart.text.clear()

    }

}