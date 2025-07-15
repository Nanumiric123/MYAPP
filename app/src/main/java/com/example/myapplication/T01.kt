package com.example.myapplication

import android.content.Context
import android.icu.text.CaseMap.Title
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
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
import org.w3c.dom.Text
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class T01 : AppCompatActivity() {
    private lateinit var tvTitle:TextView
    private lateinit var btnSave:Button
    private lateinit var btnClear:Button
    private lateinit var btnMenu:Button
    private lateinit var EDBarcode:EditText
    private lateinit var EDPart:EditText
    private lateinit var EDuom:EditText
    private lateinit var EDQty:EditText
    private lateinit var EDlotNo:EditText
    private lateinit var EDfromLoc:TextView
    private lateinit var EDtoLoc:TextView
    private lateinit var PB:ProgressBar
    private lateinit var gReelNum:String
    private lateinit var gBadgeNum:String
    private lateinit var c:Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_t01)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        tvTitle = findViewById(R.id.T01TVTITLE)
        tvTitle.text = intent.getStringExtra("Desc").toString()
        gBadgeNum = intent.getStringExtra("Badge").toString()
        btnSave = findViewById(R.id.T01btnSave)
        btnClear = findViewById(R.id.T01btnClear)
        btnMenu = findViewById(R.id.T01btnMenu)
        EDBarcode = findViewById(R.id.T01EDBarcode)
        EDPart = findViewById(R.id.TV01EDPart)
        EDuom = findViewById(R.id.T01EDUOM)
        EDQty = findViewById(R.id.T01EDQty)
        EDlotNo = findViewById(R.id.T01EDLotNo)
        EDfromLoc = findViewById(R.id.T01EDFL)
        EDtoLoc = findViewById(R.id.T01EDTL)
        PB = findViewById(R.id.T01PB)
        c = this@T01

        btnMenu.setOnClickListener {
            this@T01.finish()
        }

        btnClear.setOnClickListener {
            EDPart.text.clear()
            EDuom.text.clear()
            gReelNum = String()
            EDQty.text.clear()
            EDlotNo.text.clear()
        }

        EDBarcode.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if(keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
                || keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN){
                CoroutineScope(Dispatchers.IO).launch {
                    setProgressBar(PB)
                    val bcodeResult = translateBarcode(EDBarcode.text.toString())
                    runOnUiThread(kotlinx.coroutines.Runnable {
                        EDPart.setText(bcodeResult.pART_NO)
                        EDuom.setText(bcodeResult.uOM)
                        EDQty.setText(bcodeResult.qUANTITY)
                        EDlotNo.setText(bcodeResult.lOT)
                        EDBarcode.text.clear()
                        gReelNum = bcodeResult.rEEL_NO
                    })


                }
                return@OnKeyListener true
            }
            false
        })

        btnSave.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                setProgressBar(PB)
                val resultString = sendtoSAP()
                val jsonObj = JSONObject(resultString)
                val resultCode = jsonObj.getString("type")
                val resultMessage = jsonObj.getString("message")
                TriggerAlert(c,resultCode,resultMessage)

            }

        }

    }
    fun getDeviceUniqueId(context: Context): String {
        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        var uuid = sharedPreferences.getString("device_uuid", null)
        if (uuid == null) {
            uuid = UUID.randomUUID().toString()
            sharedPreferences.edit().putString("device_uuid", uuid).apply()
        }
        return uuid
    }
    private suspend fun sendtoSAP():String{
        var result = String()
        withContext(Dispatchers.IO){
            val url = URL("http://172.16.206.19/BARCODEWEBAPI/api/T01")
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val payLoad = "{\n" +
                        "  \"parT_NUM\": \"${EDPart.text.toString()}\",\n" +
                        "  \"reeL_NUM\": \"\",\n" +
                        "  \"quantity\": ${EDQty.text.toString()},\n" +
                        "  \"loT_NO\": \"${EDlotNo.text.toString()}\",\n" +
                        "  \"uom\": \"PC\",\n" +
                        "  \"badge\": \"${gBadgeNum}\",\n" +
                        "  \"froM_BIN\": \"${EDfromLoc.text.toString()}\",\n" +
                        "  \"tO_BIN\": \"${EDtoLoc.text.toString()}\",\n" +
                        "  \"devicE_NO\": \"${getDeviceUniqueId(c)}\",\n" +
                        "  \"palleT_NO\": \"${gReelNum}\"\n" +
                        "}".trimIndent()

                val outputStream = OutputStreamWriter(connection.outputStream)
                outputStream.write(payLoad)
                outputStream.flush()
                val responseCode = connection.responseCode
                if(responseCode == HttpURLConnection.HTTP_OK){
                    val inputStream = connection.inputStream
                    val response = inputStream.bufferedReader().use { it.readText() }
                    result = response
                }
                else{
                    result = "{\n" +
                            "    \"msg\": \"Error\",\n" +
                            "    \"items\": [ERROR CODE : ${responseCode}]\n" +
                            "}"
                }

            }
            catch (e: Exception) {
                e.printStackTrace()
            } finally {
                connection.disconnect()
            }
        }
        setProgressBar(PB)
        return result
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
        }
        catch (e:Exception){
            TriggerAlert(c,"Error","SALAH SCAN LABEL")
        }
        finally {

        }

        setProgressBar(PB)
        return result
    }

    private fun TriggerAlert(c:Context,code:String,message:String){
        runOnUiThread(kotlinx.coroutines.Runnable{
            val builder = AlertDialog.Builder(c)
            builder.setTitle(code)
            builder.setMessage(message)
            builder.setPositiveButton("OK") { dialog, which ->
                // Do something when OK button is clicked
                EDPart.text.clear()
                EDuom.text.clear()
                gReelNum = String()
                EDQty.text.clear()
                EDlotNo.text.clear()
            }
            builder.show()
        })
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

}