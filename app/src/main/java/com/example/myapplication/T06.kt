package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.media.tv.TvRecordingInfo
import android.os.Bundle
import android.text.Editable
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class T06 : AppCompatActivity() {
    private lateinit var inBarcode:EditText
    private lateinit var btnMenu:Button
    private lateinit var inLocationRack:EditText
    private lateinit var btnTransfer:Button
    private lateinit var c:Context
    private lateinit var gbadgeNum:String
    private lateinit var pb:ProgressBar
    private lateinit var sw:Switch
    private lateinit var singleTV:TextView
    private  lateinit var itemCount:TextView
    private lateinit var prefLoc:TextView
    private lateinit var cf:commonFunctions
    private lateinit var mainTL: TableLayout
    private lateinit var btnClear:Button
    private var dataList:MutableList<BarcodeData> = mutableListOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_t06)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        btnClear = findViewById(R.id.T06BtnClear)
        var g_prefLoc = String()
        cf = commonFunctions()
        btnTransfer = findViewById<Button>(R.id.T06btnTF)
        btnMenu = findViewById(R.id.T06btnMenu)
        inBarcode = findViewById(R.id.T06EDBarcode)
        inLocationRack = findViewById(R.id.T06EDRACK)
        itemCount = findViewById(R.id.tv06CountItems)
        prefLoc = findViewById(R.id.T06TVPL)
        sw = findViewById<Switch>(R.id.T06SW)
        singleTV = findViewById<TextView>(R.id.T06TVSingle)
        c = this@T06
        pb = findViewById(R.id.T06PB)
        gbadgeNum = intent.getStringExtra("Badge").toString()
        mainTL = findViewById(R.id.T06Table)
        inBarcode.requestFocus()
        var submitSingle = BarcodeData("","","","","","","")
        hideKeyboard()
        btnMenu.setOnClickListener {
            this@T06.finish()
        }
        btnClear.setOnClickListener {
            inBarcode.text.clear()
            inLocationRack.text.clear()
            itemCount.text = "Total Items Scanned :"
            singleTV.text = "TextView"
            prefLoc.text = ""
            regenerateTableHeader()
            hideKeyboard()
            if(sw.isChecked){
                inLocationRack.requestFocus()
            }
            else{
                inBarcode.requestFocus()
            }
        }
        sw.setOnClickListener {
            if(sw.isChecked){
                regenerateTableHeader()
                singleTV.text = "TextView"
                inLocationRack.requestFocus()
                inLocationRack.text.clear()
                inBarcode.text.clear()
                hideKeyboard()
            }
            else{
                inLocationRack.text.clear()
                inBarcode.text.clear()
                regenerateTableHeader()
                inBarcode.requestFocus()
                hideKeyboard()
            }
        }

        inLocationRack.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
            keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN
        ){
                inBarcode.requestFocus()
                hideKeyboard()
                return@OnKeyListener true
            }
            false
        })

        inBarcode.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN
            ) {
                //Perform Code
                if(sw.isChecked){
                    //scan multiple
                    if(inLocationRack.text.isNullOrBlank()){
                        cf.showMessage(c,"Message","Error Please Scan Rack","OK", positiveButtonAction = {
                            inLocationRack.requestFocus()

                        }).show()
                    }
                    else{
                        val result = translateBarcode(inBarcode.text.toString())
                        if(result.lOT.isNullOrBlank() && result.rEEL_NO.isNullOrBlank() && result.pART_NO.isNullOrBlank()
                            && result.qUANTITY.isNullOrBlank()){
                            cf.showMessage(c,"Message","Wrong Barcode Scan Again","OK", positiveButtonAction = {
                                inBarcode.requestFocus()
                                inBarcode.text.clear()
                                hideKeyboard()
                            }).show()
                        }
                        else{
                            CoroutineScope(Dispatchers.IO).launch{
                                var prefereedLoc = retrievePREFLOC(result.pART_NO)
                                prefereedLoc = prefereedLoc.replace("\"","")
                                withContext(Dispatchers.Main){
                                    prefLoc.text = prefereedLoc
                                }
                                if(prefereedLoc == inLocationRack.text.toString()){
                                    submitToSAP(result,gbadgeNum,inLocationRack.text.toString())
                                    hideKeyboard()
                                }
                                else{
                                    withContext(Dispatchers.Main){
                                        cf.showDialog(c,"Error","Scan location is ${inLocationRack.text.toString()}, " +
                                                "but prefered location is $prefereedLoc","Confirm","Cancel",
                                            positiveButtonAction = {
                                                CoroutineScope(Dispatchers.IO).launch{
                                                    submitToSAP(result,gbadgeNum,inLocationRack.text.toString())
                                                }
                                                hideKeyboard()
                                            },
                                            negativeButtonAction = {
                                                inLocationRack.text.clear()
                                                inBarcode.text.clear()
                                                inLocationRack.requestFocus()
                                                hideKeyboard()
                                            }).show()
                                    }
                                }

                            }

                            inBarcode.text.clear()

                        }
                    }

                }
                else{
                    val result = translateBarcode(inBarcode.text.toString())
                    CoroutineScope(Dispatchers.IO).launch{
                        var prefereedLoc = retrievePREFLOC(result.pART_NO)
                        prefereedLoc = prefereedLoc.replace("\"","")
                        withContext(Dispatchers.Main){
                            g_prefLoc = prefereedLoc
                            prefLoc.text = prefereedLoc
                        }
                    }
                    if(result.lOT.isNullOrBlank() && result.rEEL_NO.isNullOrBlank() && result.pART_NO.isNullOrBlank()
                        && result.qUANTITY.isNullOrBlank()){
                        cf.showMessage(c,"Message","Wrong Barcode Scan Again","OK", positiveButtonAction = {
                            inBarcode.requestFocus()
                            inBarcode.text.clear()

                        }).show()
                    }
                    else{

                        inLocationRack.requestFocus()
                        submitSingle = result
                        singleTV.text = "Part Number : ${result.pART_NO} \n Batch/Lot : ${result.lOT} \n Reel/Carton Number : ${result.rEEL_NO}"
                        hideKeyboard()
                    }
                }


                return@OnKeyListener true
            }
            false
        })
        btnTransfer.setOnClickListener {
            if(inLocationRack.text.isNullOrBlank()){
                cf.showMessage(c,"Error","Location not scanned yet","OK", positiveButtonAction = {

                    inLocationRack.requestFocus()

                }).show()
            }
            else{
                if(g_prefLoc == inLocationRack.text.toString()){
                    if(submitSingle.lOT.isNullOrBlank() && submitSingle.rEEL_NO.isNullOrBlank() && submitSingle.pART_NO.isNullOrBlank()
                        && submitSingle.qUANTITY.isNullOrBlank()){
                        cf.showMessage(c,"Message","Wrong Barcode Scan Again","OK", positiveButtonAction = {
                            inBarcode.requestFocus()
                            inBarcode.text.clear()

                        }).show()
                    }
                    else{
                        CoroutineScope(Dispatchers.IO).launch{
                            submitToSAP(submitSingle,gbadgeNum,inLocationRack.text.toString())
                        }
                    }

                }
                else{
                    cf.showDialog(c,"Error","Scan location is ${inLocationRack.text.toString()}, " +
                            "but prefered location is $g_prefLoc","Confirm","Cancel",
                        positiveButtonAction = {
                            CoroutineScope(Dispatchers.IO).launch{
                                submitToSAP(submitSingle,gbadgeNum,inLocationRack.text.toString())
                            }

                        },
                        negativeButtonAction = {
                            inLocationRack.text.clear()
                            inBarcode.text.clear()
                            inLocationRack.requestFocus()
                        }).show()
                }
            }

        }
    }

    private suspend fun retrievePREFLOC(materialNum:String):String{
        return withContext(Dispatchers.IO){
            val url = URL("http://172.16.206.19/EKANBANAPI/api/SMTEKANBAN?material=$materialNum")
            url.readText()
        }
    }

    private suspend fun submitToSAP(input: BarcodeData,badgeID:String,inRack: String){
        withContext(Dispatchers.IO){
            try{
                withContext(Dispatchers.Main){
                    setProgressBar(pb)
                }
                val url = URL("http://172.16.206.19/EKANBANAPI/api/T06")
                var payLoad = "{\n" +
                        "  \"batch\": \"${input.lOT}\",\n" +
                        "  \"material\": \"${input.pART_NO}\",\n" +
                        "  \"storagE_BIN\": \"${inRack}\",\n" +
                        "  \"quantity\": \"${input.qUANTITY}\",\n" +
                        "  \"REEL_NO\": \"${input.rEEL_NO}\",\n" +
                        "  \"DEVICE_ID\": \"${UUID.randomUUID().toString()}\",\n" +
                        "  \"badgE_ID\": \"${badgeID}\"\n" +
                        "}"
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
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
                            if(sw.isChecked){
                                if(respSplit[0].contains('E')){
                                    cf.showMessage(
                                        c,
                                        respSplit[0],
                                        respSplit[1],
                                        "OK",
                                        positiveButtonAction = {
                                            inBarcode.requestFocus()
                                        }).show()
                                }
                                else{
                                    addRowToTL(c,input.pART_NO,input.lOT,input.rEEL_NO,input.qUANTITY)
                                    itemCount.text = "Total Items Scanned : ${mainTL.childCount-1}"
                                }
                            }
                            else{
                                cf.showMessage(
                                    c,
                                    respSplit[0],
                                    respSplit[1],
                                    "OK",
                                    positiveButtonAction = {
                                        inBarcode.requestFocus()
                                        inBarcode.text.clear()
                                    }).show()
                            }

                        } )
                    }
                }
                else{
                    withContext(Dispatchers.Main){
                        cf.showMessage(c,"Error","Code : $responseCode","OK",
                            positiveButtonAction = {
                                inBarcode.requestFocus()
                                inBarcode.text.clear()
                            }).show()
                    }
                }
            }
            catch(ex: Exception){
                withContext(Dispatchers.Main){
                    cf.showMessage(c,"Error",ex.message.toString(),"OK",
                        positiveButtonAction = {
                            regenerateTableHeader()
                        }).show()
                }
            }
            finally {
                withContext(Dispatchers.Main){
                    setProgressBar(pb)
                }
            }
        }
    }

    private fun addRowToTL(ct: Context,partNo:String,lotNo:String,reelNum:String,p_qty:String){
        var row = cf.generateRow(ct)
        with(row){
            layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.MATCH_PARENT)
            gravity = Gravity.CENTER
        }
        var tvLayoutParam = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT)
        var partTv = cf.generateTVforRow(partNo,c)
        partTv.layoutParams = tvLayoutParam
        var lotTV = cf.generateTVforRow(lotNo,c)
        lotTV.layoutParams = tvLayoutParam
        var reelTV = cf.generateTVforRow(reelNum,c)
        reelTV.layoutParams = tvLayoutParam
        var quantityTV = cf.generateTVforRow(p_qty,c)
        quantityTV.layoutParams = tvLayoutParam
        row.addView(partTv)
        row.addView(lotTV)
        row.addView(reelTV)
        row.addView(quantityTV)
        mainTL.addView(row)
    }
    private fun regenerateTableHeader(){
        mainTL.removeAllViews()
        var row = cf.generateRow(c)
        with(row){
            layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.MATCH_PARENT)
            gravity = Gravity.CENTER
        }
        var tvLayoutParam = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT)
        var partTv = cf.generateTVforRow("Part Number",c)
        partTv.layoutParams = tvLayoutParam
        var lotTV = cf.generateTVforRow("Lot Number",c)
        lotTV.layoutParams = tvLayoutParam
        var reelTV = cf.generateTVforRow("Reel Number",c)
        reelTV.layoutParams = tvLayoutParam
        var quantityTV = cf.generateTVforRow("Quantity",c)
        quantityTV.layoutParams = tvLayoutParam
        row.addView(partTv)
        row.addView(lotTV)
        row.addView(reelTV)
        row.addView(quantityTV)
        mainTL.addView(row)
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

    private fun translateBarcode(bcString:String):BarcodeData{
        var translatedResult = BarcodeData("","","","","","","")
        val bcSplit = bcString.split('(',')')
        when {
            bcSplit.size >= 15 -> {
                translatedResult.vENDOR = bcSplit[2]
                translatedResult.lOT = bcSplit[10]
                translatedResult.dATE = bcSplit[4]
                translatedResult.pART_NO = bcSplit[6]
                translatedResult.rEEL_NO = bcSplit[8]
                translatedResult.qUANTITY = bcSplit[12]
                translatedResult.uOM = bcSplit[14]

            }
            else -> {

            }
        }

        return translatedResult
    }

    fun Activity.hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))
    }

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    data class BarcodeData(var vENDOR:String, var dATE:String, var pART_NO:String,
                           var rEEL_NO:String, var lOT:String, var qUANTITY:String, var uOM:String)
}