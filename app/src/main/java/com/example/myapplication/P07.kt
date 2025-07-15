package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.math.Quantiles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.w3c.dom.Text
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class P07 : AppCompatActivity() {
    private lateinit var title: TextView
    private lateinit var menuBtn: Button
    private lateinit var clearBtn: Button
    private lateinit var saveBtn: Button
    private lateinit var itemBarcodeED: EditText
    private lateinit var partTV: TextView
    private lateinit var lotNoTV: TextView
    private lateinit var palletIDTV: TextView
    private lateinit var quantityTV: TextView
    private lateinit var fromlocTV: TextView
    private lateinit var toLocED: EditText
    private lateinit var pb: ProgressBar
    private lateinit var badgeNum: String
    private lateinit var ct: Context
    private var barcodeResult = palletLabel(MATERIAL = "",PALLET_SEQUANCE = "",LOT_NO = "",QUANTITY="",PALLET_ID = "",CARTON_QTY = "")
    private lateinit var cf:commonFunctions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_p07)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        ct = this@P07
        cf = commonFunctions()
        title = findViewById<TextView>(R.id.P07TVTITLE)
        title.text = intent.getStringExtra("Desc").toString()
        badgeNum = intent.getStringExtra("Badge").toString()
        menuBtn = findViewById<Button>(R.id.P07btnMenu)
        clearBtn = findViewById<Button>(R.id.P07btnClear)
        saveBtn = findViewById<Button>(R.id.P07btnSave)
        itemBarcodeED = findViewById<EditText>(R.id.P07TVEDBarcode)
        partTV = findViewById<TextView>(R.id.P07TVMat)
        lotNoTV = findViewById<TextView>(R.id.P07TVLotNo)
        palletIDTV = findViewById<TextView>(R.id.P07TVPallet)
        quantityTV = findViewById<TextView>(R.id.P07TVQty)
        fromlocTV = findViewById<TextView>(R.id.P07EDfromLoc)
        toLocED = findViewById<EditText>(R.id.P07EDtoLoc)
        pb = findViewById<ProgressBar>(R.id.P07PB)
        itemBarcodeED.requestFocus()

        itemBarcodeED.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                if(itemBarcodeED.text.isNullOrBlank()){
                    cf.showMessage(ct,"ERROR","SCAN MASTER LABEL", positiveButtonText = "OK", positiveButtonAction = { clearForm() } ).show()
                }
                else{
                    CoroutineScope(Dispatchers.Main).launch {
                        setProgressBar(pb,true)
                        val bcodeJSON = JSONObject(translatePalletLabel(itemBarcodeED.text.toString()))
                        barcodeResult.MATERIAL = bcodeJSON.getString("material")
                        barcodeResult.PALLET_ID = bcodeJSON.getString("palleT_ID")
                        barcodeResult.LOT_NO = bcodeJSON.getString("loT_NO")
                        barcodeResult.QUANTITY = bcodeJSON.getString("quantity")
                        barcodeResult.PALLET_SEQUANCE = bcodeJSON.getString("palleT_SEQUENCE")
                        partTV.text = "Part : ${bcodeJSON.getString("material")}"
                        lotNoTV.text = "Lot Number : ${bcodeJSON.getString("loT_NO")}"
                        palletIDTV.text = "Pallet ID : ${bcodeJSON.getString("palleT_ID")}"
                        quantityTV.text = "Quantity : ${bcodeJSON.getString("quantity")}"

                        setProgressBar(pb,false)
                        toLocED.requestFocus()
                    }
                }


                return@OnKeyListener true
            }
            false
        })

        clearBtn.setOnClickListener {
            clearForm()
        }

        menuBtn.setOnClickListener {
            this@P07.finish()
        }

        saveBtn.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch{
                if(barcodeResult.PALLET_ID.isNullOrBlank()){
                    cf.showMessage(ct,"ERROR","SCAN MASTER LABEL", positiveButtonText = "OK", positiveButtonAction = { clearForm() } ).show()
                }
                else{
                    transferbySAP(barcodeResult.MATERIAL,barcodeResult.LOT_NO,barcodeResult.PALLET_ID,barcodeResult.QUANTITY,toLocED.text.toString(),badgeNum,getHardwareBasedUUID(ct))
                }

            }
        }

    }

    fun getHardwareBasedUUID(context: Context): String {
        val androidID = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return UUID.nameUUIDFromBytes(androidID.toByteArray()).toString()
    }
    private fun clearForm(){
        barcodeResult = palletLabel(MATERIAL = "",PALLET_SEQUANCE = "",LOT_NO = "",QUANTITY="",PALLET_ID = "",CARTON_QTY = "")
        itemBarcodeED.requestFocus()
        itemBarcodeED.text.clear()
        partTV.text = "Part :"
        lotNoTV.text = "Lot Number :"
        palletIDTV.text = "Pallet ID :"
        quantityTV.text = "Quantity :"
        toLocED.text.clear()

    }

    private suspend fun transferbySAP(MaterialNo:String,lot_no:String,pallet_ID:String,qty:String,toLoc:String,badgeID: String,deviceID: String){
        withContext(Dispatchers.IO){
            withContext(Dispatchers.Main){
                setProgressBar(pb,true)
            }
            val urlString = "http://172.16.206.19/FORD_SYNC/API/P07"
            val payLoad = "{\n" +
                    "  \"material\": \"${MaterialNo}\",\n" +
                    "  \"loT_NO\": \"${lot_no}\",\n" +
                    "  \"palleT_ID\": \"${pallet_ID}\",\n" +
                    "  \"quantity\": ${qty},\n" +
                    "  \"froM_LOC\": \"PACKING\",\n" +
                    "  \"tO_LOC\": \"${toLoc}\",\n" +
                    "  \"badgE_ID\": \"${badgeID}\",\n" +
                    "  \"devicE_ID\": \"${deviceID}\"\n" +
                    "}"
            try {
                val url = URL(urlString)
                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                // Configure the connection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; utf-8")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true

                // Write the payload to the output stream
                val outputStream: OutputStream = connection.outputStream
                outputStream.write(payLoad.toByteArray())
                outputStream.flush()
                outputStream.close()

                // Get the response code and read the response
                val responseCode = connection.responseCode
                if(responseCode == HttpURLConnection.HTTP_OK){
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val respObj = JSONObject(response)

                    try{
                        withContext(Dispatchers.Main){
                            cf.showMessage(ct,respObj.getString("type"),respObj.getString("message"), positiveButtonText = "OK", positiveButtonAction = { clearForm() } ).show()
                        }

                    }
                    catch (exp: Exception){
                        withContext(Dispatchers.Main){
                            cf.showMessage(ct,"Error","${exp.message.toString()} \n $response", positiveButtonText = "OK", positiveButtonAction = { clearForm() } ).show()
                        }

                    }
                }
                else{
                    withContext(Dispatchers.Main){
                        cf.showMessage(ct,"Error","Cannot connect to server Error $responseCode", positiveButtonText = "OK", positiveButtonAction = { clearForm() }).show()
                    }
                }
            }
            catch (ex: Exception){
                cf.showMessage(ct,"Error",ex.message.toString(), positiveButtonText = "OK", positiveButtonAction = { clearForm() }).show()
            }
            finally {
                withContext(Dispatchers.Main){
                    setProgressBar(pb,false)
                }
            }
        }
    }

    private suspend fun translatePalletLabel(barcode: String):String{
        return withContext(Dispatchers.IO){
            val url = URL("http://172.16.206.19/FORD_SYNC/API/CARTONTRANSLATE?cartonBcode=$barcode")
            try {

                url.readText()  // Fetch the response as a String
            }
            catch (ex: Exception){
                "{\n" +
                        "    \"material\": \"${ex.message.toString()}\",\n" +
                        "    \"palleT_SEQUENCE\": \"${ex.message.toString()}\",\n" +
                        "    \"loT_NO\": \"\",\n" +
                        "    \"quantity\": \"\",\n" +
                        "    \"palleT_ID\": \"\",\n" +
                        "    \"cartoN_QTY\": \"\"\n" +
                        "}"

            }
            finally {

            }

        }

    }

    private fun setProgressBar(PB:ProgressBar,show: Boolean){
        runOnUiThread(Runnable{
            if(show){
                PB.visibility = View.VISIBLE
            }
            else{
                PB.visibility = View.INVISIBLE
            }
        })
    }

}