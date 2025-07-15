package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
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
import android.widget.inline.InlineContentView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.myapplication.T06.BarcodeData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class T05 : AppCompatActivity() {

    private lateinit var inBarcode:EditText
    private lateinit var btnMenu: Button
    private lateinit var btnClear:Button
    private lateinit var tvMat:TextView
    private lateinit var tvBat:TextView
    private lateinit var tvReel:TextView
    private lateinit var tvQty:EditText
    private lateinit var inLoc:EditText
    private lateinit var pb:ProgressBar
    private lateinit var c: Context
    private lateinit var btnTransfer:Button
    private lateinit var gbadgeNum:String
    private lateinit var sw:Switch
    private lateinit var mainTL: TableLayout
    private var dataList:MutableList<SCANNED_DATA> = mutableListOf()
    private lateinit var matscn:TextView
    private lateinit var prefLoc:TextView
    private lateinit var cf:commonFunctions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_t05)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        cf = commonFunctions()
        var g_prefLoc = String()
        mainTL = findViewById<TableLayout>(R.id.T05TL)
        btnMenu = findViewById<Button>(R.id.T05btnMenu)
        btnClear = findViewById<Button>(R.id.T05btnClear)
        inBarcode = findViewById(R.id.T05EDBARCODE)
        tvMat = findViewById(R.id.T05TVMATERIAL)
        inLoc = findViewById(R.id.T05EDINLOCATION)
        sw = findViewById(R.id.T05SWSM)
        pb = findViewById(R.id.T05PB)
        matscn = findViewById(R.id.T05TVMatScn)
        prefLoc = findViewById(R.id.T05TVPL)
        c = this@T05
        btnTransfer = findViewById(R.id.T05BTNTRANSFER)
        gbadgeNum = intent.getStringExtra("Badge").toString()
        inBarcode.requestFocus()
        hideKeyboard()
        var submitSingle = BarcodeData("","","","","","","")
        sw.setOnClickListener {
            if(sw.isChecked){
                regenerateTableHeader()
                tvMat.text = "TextView"
                inLoc.requestFocus()
                inLoc.text.clear()
                inBarcode.text.clear()
                hideKeyboard()
            }
            else{
                inLoc.text.clear()
                inBarcode.text.clear()
                regenerateTableHeader()
                inBarcode.requestFocus()
                hideKeyboard()
            }
        }
        btnClear.setOnClickListener {
            matscn.text = "Material Scanned : "
            regenerateTableHeader()
            inBarcode.text.clear()
            inLoc.text.clear()
            prefLoc.text = ""
            tvMat.text = "Material : "

        }

        btnMenu.setOnClickListener {
            this@T05.finish()
        }

        inLoc.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN
            ){
                inBarcode.requestFocus()
                hideKeyboard()
                return@OnKeyListener true
            }
            false
        })

        inBarcode.setOnKeyListener(View.OnKeyListener{_,keyCode,event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN
            ){
                if(sw.isChecked){
                    if(inLoc.text.isNullOrBlank()){
                        cf.showMessage(c,"Message","Error Please Scan Rack","OK", positiveButtonAction = {
                            inLoc.requestFocus()

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
                                if(prefereedLoc == inLoc.text.toString()){
                                    submitToSAP(result,gbadgeNum,inLoc.text.toString())
                                    hideKeyboard()
                                }
                                else{
                                    withContext(Dispatchers.Main){
                                        cf.showDialog(c,"Error","Scan location is ${inLoc.text.toString()}, " +
                                                "but prefered location is $prefereedLoc","Confirm","Cancel",
                                            positiveButtonAction = {
                                                CoroutineScope(Dispatchers.IO).launch{
                                                    submitToSAP(result,gbadgeNum,inLoc.text.toString())
                                                }
                                                hideKeyboard()
                                            },
                                            negativeButtonAction = {
                                                inLoc.text.clear()
                                                inBarcode.text.clear()
                                                inLoc.requestFocus()
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

                        inLoc.requestFocus()
                        submitSingle = result
                        tvMat.text = "Part Number : ${result.pART_NO} \n Batch/Lot : ${result.lOT} \n Reel/Carton Number : ${result.rEEL_NO}"
                        hideKeyboard()
                    }
                }
                return@OnKeyListener true
            }
            false
        })

        btnTransfer.setOnClickListener {
            if(inLoc.text.isNullOrBlank()){
                cf.showMessage(c,"Error","Location not scanned yet","OK", positiveButtonAction = {

                    inLoc.requestFocus()

                }).show()
            }
            else{
                if(g_prefLoc == inLoc.text.toString()){
                    if(submitSingle.lOT.isNullOrBlank() && submitSingle.rEEL_NO.isNullOrBlank() && submitSingle.pART_NO.isNullOrBlank()
                        && submitSingle.qUANTITY.isNullOrBlank()){
                        cf.showMessage(c,"Message","Wrong Barcode Scan Again","OK", positiveButtonAction = {
                            inBarcode.requestFocus()
                            inBarcode.text.clear()

                        }).show()
                    }
                    else{
                        CoroutineScope(Dispatchers.IO).launch{
                            submitToSAP(submitSingle,gbadgeNum,inLoc.text.toString())
                        }
                    }

                }
                else{
                    cf.showDialog(c,"Error","Scan location is ${inLoc.text.toString()}, " +
                            "but prefered location is $g_prefLoc","Confirm","Cancel",
                        positiveButtonAction = {
                            CoroutineScope(Dispatchers.IO).launch{
                                submitToSAP(submitSingle,gbadgeNum,inLoc.text.toString())
                            }

                        },
                        negativeButtonAction = {
                            inLoc.text.clear()
                            inBarcode.text.clear()
                            inLoc.requestFocus()
                        }).show()
                }
            }
        }
    }
    private suspend fun submitToSAP(input: BarcodeData,badgeID:String,inRack: String){
        withContext(Dispatchers.IO){
            try{
                withContext(Dispatchers.Main){
                    setProgressBar(pb)
                }
                val url = URL("http://172.16.206.19/EKANBANAPI/api/T05")
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
                                    matscn.text = "Total Items Scanned : ${mainTL.childCount-1}"
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
    private suspend fun retrievePREFLOC(materialNum:String):String{
        return withContext(Dispatchers.IO){
            val url = URL("http://172.16.206.19/EKANBANAPI/api/SMTEKANBAN?material=$materialNum")
            url.readText()
        }
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


    fun Activity.hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))
    }

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
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

    data class SCANNED_DATA(
        var VENDOR:String,
        var DATE:String,
        var MATERIAL:String,
        var REEL_NO:String,
        var LOT:String,
        var QUANTITY:String
    )
}