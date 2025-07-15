package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class C01 : AppCompatActivity() {
    private lateinit var titleTV:TextView
    private lateinit var spStockType:Spinner
    private lateinit var invDoc:EditText
    private lateinit var uniqueID:EditText
    private lateinit var item:EditText
    private lateinit var qty:EditText
    private  lateinit var save:Button
    private  lateinit var menu:Button
    private  lateinit var clear:Button
    private lateinit var c:Context
    private lateinit var pb:ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_c01)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        titleTV = findViewById(R.id.C01TVTITLE)
        spStockType = findViewById(R.id.C01SPSS)
        invDoc  = findViewById(R.id.C01EDInvDoc)
        uniqueID = findViewById(R.id.C01EDUniqueId)
        item = findViewById(R.id.C01EDItem)
        qty = findViewById(R.id.C01EDQty)
        save = findViewById(R.id.C01BtnSave)
        menu = findViewById(R.id.C01BtnMenu)
        clear = findViewById(R.id.C01BtnClear)
        pb = findViewById(R.id.C01PB)
        var bNum = intent.getStringExtra("Badge").toString()
        titleTV.text = intent.getStringExtra("Desc").toString()
        c = this@C01
        invDoc.requestFocus()

        menu.setOnClickListener {
            finish()
        }

        clear.setOnClickListener {
            clearEverything()
        }

        invDoc.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                if(invDoc.text.contains('$')){
                    runOnUiThread(Runnable{
                        item.isEnabled = false
                    })


                var tagString = invDoc.text.split('$')

                invDoc.setText(tagString[0])
                uniqueID.setText(tagString[1])
                    runOnUiThread(Runnable{
                        item.isEnabled = true
                    })
                item.requestFocus()
                }
                else{
                    triggerAlert("error","Wrong barcode")
                }
                return@OnKeyListener true
            }
            false
        })

        item.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                qty.requestFocus()

                return@OnKeyListener true
            }
            false
        })

        save.setOnClickListener {
            var SS = String()
            progressbar_setting(pb)
            if (spStockType.selectedItem == "Warehouse"){
                SS = "1"
            }
            else if (spStockType.selectedItem == "Quality Inspection"){
                SS = "2"
            }
            else if (spStockType.selectedItem == "Wrhse/QuInsp.(InvSa)"){
                SS = "3"
            }
            else if (spStockType.selectedItem == "Blocked"){
                SS = "4"
            }
            else{
                SS = "1"
            }

            var result = String()
            CoroutineScope(Dispatchers.IO).launch{
                if(checkForduplicates()){
                    runOnUiThread(kotlinx.coroutines.Runnable {
                        triggerAlert("Message","Duplicated")
                        progressbar_setting(pb)
                    })
                }
                else{
                    result = sendToDb(item.text.toString(),qty.text.toString(),bNum,invDoc.text.toString(),uniqueID.text.toString(),"",SS)
                    runOnUiThread(kotlinx.coroutines.Runnable {
                        triggerAlert("Message",result)
                    })
                }



            }
        }



    }

    private fun clearEverything(){
        item.text.clear()
        uniqueID.text.clear()
        qty.text.clear()
        invDoc.text.clear()
        invDoc.requestFocus()
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

    private fun progressbar_setting(v: View){
        runOnUiThread(kotlinx.coroutines.Runnable {
            if (v.visibility == View.VISIBLE) {
                v.visibility = View.GONE
            } else {
                v.visibility = View.VISIBLE
            }

        })
    }

    private suspend fun checkForduplicates():Boolean{
        var result:Boolean = false

        withContext(Dispatchers.IO){
            var url = URL("http://172.16.206.19/BARCODEWEBAPI/api/INVENTORY?material=${item.text.toString()}&invdoc=${invDoc.text.toString()}")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            val responseCode = connection.responseCode
            val response = StringBuilder()

            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                var inputLine: String?
                while (reader.readLine().also { inputLine = it } != null) {
                    response.append(inputLine)
                }

            }
            result = response.toString().toBoolean()
            connection.disconnect()
        }
        return result
    }

    private suspend fun sendToDb(material:String,quantity:String,badgeNum:String,invNum:String,uniqueNum:String,strLoc:String,stockStats:String):String{
        var linkStr = "http://172.16.206.19/REST_API/Third/MPPInsertPIIM?material=${material}&qty=${quantity}&badgeno=${badgeNum}&InvDoc=${invNum}&" +
                "uniqueID=${uniqueNum}&stockStat=${stockStats}&countType=1&storageLoc=${strLoc}"
        var result = String()
        withContext(Dispatchers.IO){
            result = URL(linkStr).readText()
        }
        progressbar_setting(pb)
        return result
    }

    private suspend fun updateResultToDB(){
        withContext(Dispatchers.IO){
            
        }
    }

}