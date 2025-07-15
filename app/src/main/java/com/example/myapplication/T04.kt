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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class T04 : AppCompatActivity() {
    private lateinit var tvtitle: TextView
    private lateinit var btnMenu:Button
    private lateinit var btnClear:Button
    private lateinit var btnSave:Button
    private lateinit var barcodeED:EditText
    private lateinit var partED:EditText
    private lateinit var uomED:EditText
    private lateinit var qtyED:EditText
    private lateinit var lotNoED:EditText
    private lateinit var fromLocED: TextView
    private lateinit var toLocED:EditText
    private lateinit var TVPrefLoc:TextView
    private lateinit var c:Context
    private lateinit var gReelNum:String
    private lateinit var PB:ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_t04)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvtitle = findViewById(R.id.T04TVTITLE)
        tvtitle.text = intent.getStringExtra("Desc").toString()
        btnMenu = findViewById(R.id.T04btnMenu)
        btnClear = findViewById(R.id.T04btnClear)
        btnSave = findViewById(R.id.T04btnSave)
        barcodeED = findViewById(R.id.T04EDBarcode)
        partED = findViewById(R.id.T04EDPart)
        uomED = findViewById(R.id.T04EDUOM)
        qtyED = findViewById(R.id.T04EDQty)
        lotNoED = findViewById(R.id.T04EDLot)
        fromLocED = findViewById(R.id.T04EDfromLoc)
        toLocED = findViewById(R.id.T04EDtoLoc)
        TVPrefLoc = findViewById(R.id.T04TVPrefLoc)
        c = this@T04

        btnSave.setOnClickListener {

        }

        btnMenu.setOnClickListener {

        }
        btnClear.setOnClickListener {
            clearAll()
        }

        barcodeED.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if(keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
                || keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN){
                CoroutineScope(Dispatchers.IO).launch {
                    setProgressBar(PB)
                    val bcodeResult = translateBarcode(barcodeED.text.toString())
                    runOnUiThread(kotlinx.coroutines.Runnable {
                        partED.setText(bcodeResult.pART_NO)
                        uomED.setText(bcodeResult.uOM)
                        qtyED.setText(bcodeResult.qUANTITY)
                        lotNoED.setText(bcodeResult.lOT)
                        barcodeED.text.clear()
                        gReelNum = bcodeResult.rEEL_NO
                    })


                }
                return@OnKeyListener true
            }
            false
        })

    }

    private fun clearAll(){
        partED.text.clear()
        uomED.text.clear()
        gReelNum = String()
        qtyED.text.clear()
        lotNoED.text.clear()
    }

    private fun getDeviceUniqueId(context: Context): String {
        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        var uuid = sharedPreferences.getString("device_uuid", null)
        if (uuid == null) {
            uuid = UUID.randomUUID().toString()
            sharedPreferences.edit().putString("device_uuid", uuid).apply()
        }
        return uuid
    }

    private suspend fun translateBarcode(input: String):T06.BarcodeData{
        var APIResult:String = ""
        var result:T06.BarcodeData = T06.BarcodeData(vENDOR = "", dATE = "", pART_NO = ""
            , rEEL_NO = "", lOT = "", qUANTITY = "", uOM = "")
        val response = StringBuilder()
        try{
            withContext(Dispatchers.IO){
                var url = URL("http://172.16.206.19/BARCODEWEBAPI/API/BARCODETRANSLATOR")
                val payLoad = "\"${input}\""
                val conn = url.openConnection() as HttpURLConnection
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Content-Length", payLoad.length.toString())
                DataOutputStream(conn.getOutputStream()).use { it.writeBytes(payLoad) }
                BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                    var inputLine: String?
                    while (reader.readLine().also { inputLine = it } != null) {
                        response.append(inputLine)
                    }

                }
                conn.disconnect()
            }
            val jobj = JSONObject(response.toString())
            val material = jobj.getString("material")
            val vendor = jobj.getString("vendor")
            val date = jobj.getString("date")
            val reel = jobj.getString("reelnumber")
            val batch = jobj.getString("batch")
            val uom = jobj.getString("uom")
            val qty = jobj.getString("quantity")
            result = T06.BarcodeData(vENDOR = vendor, dATE = date, pART_NO = material
                , rEEL_NO = reel, lOT = batch, qUANTITY = qty, uOM = uom)
            getPrefLoc(material)
            runOnUiThread(kotlinx.coroutines.Runnable  {
                toLocED.requestFocus()
            })
        }
        catch (e:Exception){
            TriggerAlert(c,"Error","SALAH SCAN LABEL")
        }
        finally {

        }

        setProgressBar(PB)
        return result
    }
    private fun TriggerAlert(c: Context, code:String, message:String){
        runOnUiThread(kotlinx.coroutines.Runnable{
            val builder = AlertDialog.Builder(c)
            builder.setTitle(code)
            builder.setMessage(message)
            builder.setPositiveButton("OK") { dialog, which ->
                // Do something when OK button is clicked
                clearAll()
            }
            builder.show()
        })
    }
    private suspend fun getPrefLoc(material:String){

        withContext(Dispatchers.IO){
            val urlLink = URL("http://172.16.206.19/BARCODEWEBAPI/api/T03?material=${material}")
            val connection = urlLink.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            val responseCode = connection.responseCode
            val response = StringBuilder()

            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                var inputLine: String?
                while (reader.readLine().also { inputLine = it } != null) {
                    response.append(inputLine)
                }

            }
            connection.disconnect()
            runOnUiThread(kotlinx.coroutines.Runnable {
                TVPrefLoc.text = "Prefered Location : ${response.toString()}"
            })
        }
    }

    private fun setProgressBar(v: View){
        runOnUiThread(kotlinx.coroutines.Runnable {
            if (v.visibility == View.VISIBLE) {
                v.visibility = View.GONE
            } else {
                v.visibility = View.VISIBLE
            }

        })
    }

}