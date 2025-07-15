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
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class T02 : AppCompatActivity() {
    private lateinit var btnMenu:Button
    private lateinit var btnClear:Button
    private lateinit var btnSave:Button
    private lateinit var barcodeED:EditText
    private lateinit var partED:EditText
    private lateinit var uomED:EditText
    private lateinit var qtyED:EditText
    private lateinit var lotNoED:EditText
    private lateinit var fromLoc:TextView
    private lateinit var toLoc:TextView
    private lateinit var PB:ProgressBar
    private lateinit var c:Context
    private lateinit var gReelNum:String
    private lateinit var gBadgeNum:String
    private lateinit var tvtitle:TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_t02)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        gBadgeNum = intent.getStringExtra("Badge").toString()
        tvtitle = findViewById(R.id.T02TVTITLE)
        tvtitle.text = intent.getStringExtra("Desc").toString()
        btnMenu = findViewById(R.id.T02BtnMenu)
        btnClear = findViewById(R.id.T02btnClear)
        btnSave = findViewById(R.id.T02btnSave)
        barcodeED = findViewById(R.id.T02EDBarcode)
        partED = findViewById(R.id.T02EDPart)
        uomED = findViewById(R.id.T02EDUOM)
        qtyED = findViewById(R.id.T02EDQty)
        lotNoED = findViewById(R.id.T02EDLotNo)
        fromLoc = findViewById(R.id.T02EDFromLoc)
        toLoc = findViewById(R.id.T02EDToLoc)
        PB = findViewById(R.id.T02PB)
        c = this@T02

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

        btnMenu.setOnClickListener {
            this@T02.finish()
        }

        btnClear.setOnClickListener {
            clearAll()
        }

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

    private fun clearAll(){
        partED.text.clear()
        uomED.text.clear()
        gReelNum = String()
        qtyED.text.clear()
        lotNoED.text.clear()
    }
    private suspend fun sendtoSAP():String{
        var result = String()
        withContext(Dispatchers.IO){
            val url = URL("http://172.16.206.19/BARCODEWEBAPI/api/T02")
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val payLoad = "{\n" +
                        "  \"parT_NUM\": \"${partED.text.toString()}\",\n" +
                        "  \"reeL_NUM\": \"\",\n" +
                        "  \"quantity\": ${qtyED.text.toString()},\n" +
                        "  \"loT_NO\": \"${lotNoED.text.toString()}\",\n" +
                        "  \"uom\": \"PC\",\n" +
                        "  \"badge\": \"${gBadgeNum}\",\n" +
                        "  \"froM_BIN\": \"${fromLoc.text.toString()}\",\n" +
                        "  \"tO_BIN\": \"${toLoc.text.toString()}\",\n" +
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