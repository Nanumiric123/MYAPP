package com.example.myapplication

import android.content.Context
import android.media.tv.TvContract
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.w3c.dom.Text
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class C12 : AppCompatActivity() {
    private lateinit var edbcode: EditText
    private lateinit var tvMterial: TextView
    private lateinit var tvBatch: TextView
    private lateinit var tvVendor: TextView
    private lateinit var tvCarton: TextView
    private lateinit var tvDate: TextView
    private lateinit var edQuantity: EditText
    private lateinit var edTROLLEY: EditText
    private lateinit var btnSave: Button
    private lateinit var btnClear: Button
    private lateinit var btnMenu: Button
    private lateinit var cf: commonFunctions
    private lateinit var ct: Context
    private lateinit var pb: ProgressBar
    private lateinit var gBcodeData:barcodeData
    private lateinit var title:TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_c12)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val function_description = intent.getStringExtra("Desc").toString()
        title = findViewById<TextView>(R.id.C12TVTITLE)
        title.text = function_description
        edbcode = findViewById(R.id.C12EDBCODE)
        tvMterial = findViewById(R.id.C12TVMATERIAL)
        tvBatch = findViewById(R.id.C12TVBATCH)
        tvVendor = findViewById(R.id.C12TVVENDOR)
        tvCarton = findViewById(R.id.C12TVCARTON)
        edQuantity = findViewById(R.id.C12EDQTY)
        edTROLLEY = findViewById(R.id.C12EDTROLLEY)
        tvDate = findViewById(R.id.C12TVDATE)
        btnClear = findViewById(R.id.C12BTNCLEAR)
        btnMenu = findViewById(R.id.C12BTNMENU)
        btnSave = findViewById(R.id.C12BTNSAVE)
        pb = findViewById(R.id.C12PB)
        cf = commonFunctions()
        ct = this@C12
        edTROLLEY.requestFocus()
        setInitTextView()
        edTROLLEY.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
                || keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN
            ) {
                edbcode.requestFocus()
                return@OnKeyListener true
            }
            false
        })
        edQuantity.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
                || keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN
            ) {
                if(edQuantity.text.length > 6){
                    cf.showMessage(ct,"Error/Salah","Quantity you can is wrong! \n Kuantity yang kamu scan salah !","Scan Again / Scan lagi",
                        positiveButtonAction = {
                            edQuantity.requestFocus()
                            edQuantity.text.clear()
                        })
                }
                return@OnKeyListener true
            }
            false
        })
        edbcode.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
                || keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN
            ) {
                if (edbcode.text.isNullOrBlank()) {
                    cf.showMessage(
                        ct,
                        "Error",
                        "Scan reel barcode first",
                        "OK",
                        positiveButtonAction = { edbcode.requestFocus() }).show()
                } else {
                    if (edTROLLEY.text.isNullOrBlank()) {
                        cf.showMessage(
                            ct,
                            "Error",
                            "Scan TROLLEY barcode first",
                            "OK",
                            positiveButtonAction = {
                                edTROLLEY.requestFocus()
                                edbcode.text.clear()
                            }).show()
                    } else {
                        CoroutineScope(Dispatchers.IO).launch {
                            withContext(Dispatchers.IO) {
                                withContext(Dispatchers.Main) {
                                    //open the progress bar
                                    setProgressBar(pb, true)
                                }
                                var trBcode = cf.translateBarcode(edbcode.text.toString(), ct)
                                withContext(Dispatchers.Main) {
                                    //close the progress bar
                                    setProgressBar(pb, false)
                                }
                                gBcodeData = trBcode
                                if(checkForDuplicatedReels(gBcodeData.REEL_NO)){
                                    runOnUiThread {
                                        cf.showDialog(ct,"Warning","The Reel is Already scanned\n Ovewrite existing quantity ?",
                                            positiveButtonText = "OK",
                                            negativeButtonText = "Cancel",
                                            positiveButtonAction = {
                                                tvMterial.text = "Material : ${trBcode.PART_NO}"
                                                tvBatch.text = "Batch : ${trBcode.LOT}"
                                                tvVendor.text = "Vendor : ${trBcode.VENDOR}"
                                                tvCarton.text = "Carton : ${trBcode.REEL_NO}"
                                                tvDate.text = "Print Date : ${trBcode.DATE}"
                                                edQuantity.requestFocus()
                                            },
                                            negativeButtonAction = {
                                                clearFormsDialog()
                                                edbcode.requestFocus()
                                            }).show()
                                    }
                                }
                                else{
                                    withContext(Dispatchers.Main) {
                                        tvMterial.text = "Material : ${trBcode.PART_NO}"
                                        tvBatch.text = "Batch : ${trBcode.LOT}"
                                        tvVendor.text = "Vendor : ${trBcode.VENDOR}"
                                        tvCarton.text = "Carton : ${trBcode.REEL_NO}"
                                        tvDate.text = "Print Date : ${trBcode.DATE}"
                                        edQuantity.requestFocus()
                                    }
                                }

                            }
                        }

                    }
                }
                return@OnKeyListener true
            }
            false
        })
        btnClear.setOnClickListener {
            clearForms()
        }
        btnSave.setOnClickListener {
            var BadgeNum = intent.getStringExtra("Badge").toString()
            if(edTROLLEY.text.isNullOrBlank()){
                cf.showMessage(
                    ct,
                    "Error",
                    "Scan TROLLEY barcode first",
                    "OK",
                    positiveButtonAction = {
                        edTROLLEY.requestFocus()
                        edbcode.text.clear()
                    }).show()
            }
            else{
                if(edQuantity.text.length > 6){
                    cf.showMessage(ct,"Error/Salah","Quantity you can is wrong! \n Kuantity yang kamu scan salah !","Scan Again / Scan lagi",
                        positiveButtonAction = {
                            edQuantity.requestFocus()
                            edQuantity.text.clear()
                        })
                }
                else{
                    submitDialog(BadgeNum)
                }

            }

        }
        btnMenu.setOnClickListener {
            this@C12.finish()
        }

    }

private fun submitDialog(B_NO:String){
    CoroutineScope(Dispatchers.IO).launch {
        if(checkForDuplicatedReels(gBcodeData.REEL_NO)){
            if((gBcodeData).PART_NO.isNullOrBlank() || gBcodeData.REEL_NO.isNullOrBlank()
                || gBcodeData.LOT.isNullOrBlank()){
                cf.showMessage(
                    ct,
                    "Error",
                    "SCAN REELS AGAIN",
                    "OK",
                    positiveButtonAction = {
                        edbcode.requestFocus()
                    }).show()
            }
            else{
                runOnUiThread {
                    CoroutineScope(Dispatchers.IO).launch {
                        updateData(edTROLLEY.text.toString(),gBcodeData.REEL_NO,edQuantity.text.toString())
                    }
                }
            }


        }
        else{
            submitInvCount(gBcodeData.PART_NO,gBcodeData.LOT,
                edQuantity.text.toString(),gBcodeData.REEL_NO,gBcodeData.DATE,gBcodeData.VENDOR,
                B_NO,edTROLLEY.text.toString())
        }

    }
}
    private suspend fun updateData(trolleyNO:String,cartonNo:String,newQty:String){
        val payLoad = "{\n" +
                "  \"trolley\" :\"${trolleyNO}\",\n" +
                "  \"quantity\": ${newQty},\n" +
                "  \"cartoN_NO\": \"${cartonNo}\"\n" +
                "}"
        withContext(Dispatchers.IO){
            val url = URL("http://172.16.206.19/FORD_SYNC/API/SMT_INV")
            val connection = url.openConnection() as HttpURLConnection
            try{
                withContext(Dispatchers.Main) {
                    //open the progress bar
                    setProgressBar(pb, true)
                }
                connection.requestMethod = "PUT"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                val outputStream = OutputStreamWriter(connection.outputStream)
                outputStream.write(payLoad)
                outputStream.flush()
                val responseCode = connection.responseCode
                if(responseCode == HttpURLConnection.HTTP_OK){
                    val inputStream = connection.inputStream
                    val response = inputStream.bufferedReader().use { it.readText() }
                    var respSplit = response.split(':')
                    withContext(Dispatchers.Main){
                        runOnUiThread ( Runnable {
                            cf.showMessage(
                                ct,
                                respSplit[0],
                                respSplit[1],
                                "OK",
                                positiveButtonAction = {
                                    clearFormsDialog()
                                    edbcode.requestFocus()
                                }).show()
                        } )
                    }

                }
                else{
                    withContext(Dispatchers.Main){
                        runOnUiThread ( Runnable {
                            cf.showMessage(
                                ct,
                                "Error",
                                "Response code ${responseCode}",
                                "OK",
                                positiveButtonAction = {
                                    clearFormsDialog()
                                    edbcode.requestFocus()
                                }).show()
                        } )
                    }

                }

            }
            catch(ex:Exception){
                withContext(Dispatchers.Main){
                    runOnUiThread(Runnable{
                        cf.showMessage(
                            ct,
                            "Error",
                            ex.message.toString(),
                            "OK",
                            positiveButtonAction = {
                                clearFormsDialog()
                                edbcode.requestFocus()
                            }).show()
                    })
                }
            }
            finally {
                withContext(Dispatchers.Main) {
                    //open the progress bar
                    setProgressBar(pb, false)
                }
            }
        }
    }

    private suspend fun checkForDuplicatedReels(cartonNo:String):Boolean{
        var result:Boolean = false
        return withContext(Dispatchers.IO) {
            try {

                withContext(Dispatchers.Main) {
                    //open the progress bar
                    setProgressBar(pb, true)
                }
                val url = URL("http://172.16.206.19/FORD_SYNC/API/SMT_INV/${cartonNo}")
                val RESTresult = JSONObject(url.readText())
                val cartNo = RESTresult.getString("cartoN_NO")
                if (!cartNo.isNullOrBlank()) {
                    result = true
                }

                result
            } catch (e: Exception) {
                result
            } finally {
                withContext(Dispatchers.Main) {
                    //open the progress bar
                    setProgressBar(pb, false)
                }
            }
        }

    }

    private suspend fun submitInvCount(part_No:String,lot:String,qty:String,cartNo:String,datePrint:String,
                                       l_vendor:String,badgeNo:String,trolleyNo:String){
        var payload = "{\n" +
                "  \"parT_NO\": \"${part_No}\",\n" +
                "  \"loT_NO\": \"${lot}\",\n" +
                "  \"quantity\": ${qty},\n" +
                "  \"cartoN_NO\": \"${cartNo}\",\n" +
                "  \"vendor\": \"${l_vendor}\",\n" +
                "  \"datE_PRINTED\": \"${datePrint}\",\n" +
                "  \"badgE_NUM\": \"${badgeNo}\",\n" +
                "  \"trolley\": \"${trolleyNo}\"\n" +
                "}"
        withContext(Dispatchers.IO){
            val url = URL("http://172.16.206.19/FORD_SYNC/API/SMT_INV")
            val connection = url.openConnection() as HttpURLConnection
            try{
                withContext(Dispatchers.Main) {
                    //open the progress bar
                    setProgressBar(pb, true)
                }
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                val outputStream = OutputStreamWriter(connection.outputStream)
                outputStream.write(payload)
                outputStream.flush()
                val responseCode = connection.responseCode
                if(responseCode == HttpURLConnection.HTTP_OK){
                    val inputStream = connection.inputStream
                    val response = inputStream.bufferedReader().use { it.readText() }
                    var respSplit = response.split(':')
                    withContext(Dispatchers.Main){
                        runOnUiThread ( Runnable {
                            cf.showMessage(
                                ct,
                                respSplit[0],
                                respSplit[1],
                                "OK",
                                positiveButtonAction = {
                                    clearFormsDialog()
                                }).show()
                        } )
                    }
                }
                else{
                    withContext(Dispatchers.Main){
                        runOnUiThread ( Runnable {
                            cf.showMessage(
                                ct,
                                "Error",
                                "Response code ${responseCode}",
                                "OK",
                                positiveButtonAction = {
                                    clearFormsDialog()
                                }).show()
                        } )
                    }
                }
            }
            catch(ex:Exception){
                withContext(Dispatchers.Main){
                    runOnUiThread(Runnable{
                        cf.showMessage(
                            ct,
                            "Error",
                            ex.message.toString(),
                            "OK",
                            positiveButtonAction = {
                                clearFormsDialog()
                            }).show()
                    })
                }
            }
            finally{
                withContext(Dispatchers.Main) {
                    //open the progress bar
                    setProgressBar(pb, false)
                }
            }
        }
    }

    private fun clearForms() {
        edQuantity.text.clear()
        setInitTextView()
        edbcode.text.clear()
        edTROLLEY.text.clear()
        gBcodeData = barcodeData("","","","","","","")
    }

    private fun clearFormsDialog(){
        edQuantity.text.clear()
        setInitTextView()
        edbcode.text.clear()
        gBcodeData = barcodeData("","","","","","","")
    }

    private fun setInitTextView() {
        tvMterial.text = "Material : "
        tvBatch.text = "Batch : "
        tvVendor.text = "Vendor : "
        tvCarton.text = "Carton : "
        tvDate.text = "Print Date : "
    }

    private fun setProgressBar(PB: ProgressBar, show: Boolean) {
        runOnUiThread(Runnable {
            if (show) {
                PB.visibility = View.VISIBLE
            } else {
                PB.visibility = View.INVISIBLE
            }
        })
    }

}