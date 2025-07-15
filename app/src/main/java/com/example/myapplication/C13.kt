package com.example.myapplication

import android.content.Context
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
import okhttp3.Dispatcher
import org.json.JSONObject
import org.w3c.dom.Text
import java.net.URL

class C13 : AppCompatActivity() {
    data class C13DATASTRUCTURE ( var PART_NO:String,var LOT_NO:String,var QUANTITY:Int,var CARTON_NO: String,var VENDOR: String,var TIME_STAMP: String,var BADGE: String,var LOCATION: String )
    private lateinit var title: TextView
    private lateinit var btnMenu: Button
    private lateinit var edBarcode: EditText
    private lateinit var tvPart: TextView
    private lateinit var tvLotNo: TextView
    private lateinit var tvQuantity: TextView
    private lateinit var tvCarton: TextView
    private lateinit var tvVendor: TextView
    private lateinit var tvDateScn: TextView
    private lateinit var tvPICScn: TextView
    private lateinit var tvLocation: TextView
    private lateinit var cf: commonFunctions
    private lateinit var ct: Context
    private lateinit var pb: ProgressBar
    private lateinit var gBcodeData:C13DATASTRUCTURE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_c13)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        ct = this@C13
        cf = commonFunctions()
        title = findViewById<TextView>(R.id.C13TVTITLE)
        title.text = intent.getStringExtra("Desc").toString()
        btnMenu = findViewById(R.id.C13BTNMenu)
        edBarcode = findViewById(R.id.C13EDBARCODE)
        tvPart = findViewById(R.id.C13TVPart)
        tvLotNo = findViewById(R.id.C13TVLotNo)
        tvQuantity = findViewById<TextView>(R.id.C13TVQuantity)
        tvCarton = findViewById<TextView>(R.id.C13TVCarton)
        tvVendor = findViewById<TextView>(R.id.C13TVVendor)
        tvDateScn = findViewById<TextView>(R.id.C13TVDateScn)
        tvPICScn = findViewById<TextView>(R.id.C13TVBadgeScn)
        tvLocation = findViewById<TextView>(R.id.C13TVLocation)
        pb = findViewById(R.id.C13PB)
        gBcodeData = C13DATASTRUCTURE("","",0,"","","","","")
        edBarcode.requestFocus()

        btnMenu.setOnClickListener {
            this@C13.finish()
        }

        edBarcode.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
            || keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                if(edBarcode.text.isNullOrBlank()){
                    cf.showMessage(
                        ct,
                        "Error",
                        "Scan reel barcode first",
                        "OK",
                        positiveButtonAction = { edBarcode.requestFocus() }).show()
                }
                else{
                    CoroutineScope(Dispatchers.IO).launch {
                        val result = translateBcode(edBarcode.text.toString())
                        if(result.REEL_NO.isNullOrBlank()){
                            withContext(Dispatchers.Main){
                                cf.showMessage(
                                    ct,
                                    "Error",
                                    "Barcode Salah",
                                    "OK",
                                    positiveButtonAction = {
                                        edBarcode.requestFocus()
                                        edBarcode.text.clear()
                                    }).show()
                            }

                        }
                        else{
                            getInvData(result.REEL_NO)
                        }
                    }
                }
                return@OnKeyListener true
            }
            false
        })

    }
    private suspend fun translateBcode(bc:String):barcodeData{
        return withContext(Dispatchers.IO){
             try{ cf.translateBarcode(bc, ct) }
            catch (ex: Exception){
                barcodeData("","","","","","","")
            }
            finally{
                    withContext(Dispatchers.Main) {
                    //open the progress bar
                    setProgressBar(pb, false)
                }
            }


        }
    }

    private suspend fun getInvData(carton_no:String){
        withContext(Dispatchers.IO){

            try {
                withContext(Dispatchers.Main) {
                    //open the progress bar
                    setProgressBar(pb, true)
                }
                val url = URL("http://172.16.206.19/FORD_SYNC/API/SMT_INV/${carton_no}")
                val RESTString = url.readText()
                val RESTresult = JSONObject(RESTString)
                gBcodeData.CARTON_NO = RESTresult.getString("cartoN_NO")
                gBcodeData.LOT_NO = RESTresult.getString("loT_NO")
                gBcodeData.PART_NO = RESTresult.getString("parT_NO")
                gBcodeData.VENDOR = RESTresult.getString("vendor")
                gBcodeData.QUANTITY = RESTresult.getInt("quantity")
                gBcodeData.TIME_STAMP = RESTresult.getString("datE_TIME")
                gBcodeData.BADGE = RESTresult.getString("badgE_NUM")
                gBcodeData.LOCATION = RESTresult.getString("trolley")

            } catch (e: Exception) {
                withContext(Dispatchers.Main){
                    cf.showMessage(
                        ct,
                        "Error",
                        "Reel Not SCANNED yet \n Reel belum scan lagi",
                        "OK",
                        positiveButtonAction = { edBarcode.requestFocus() }).show()
                }

            } finally {
                withContext(Dispatchers.Main) {
                    //open the progress bar
                    setProgressBar(pb, false)
                    tvPart.text = gBcodeData.PART_NO
                    tvLotNo.text = "Lot : ${gBcodeData.LOT_NO}"
                    tvQuantity.text = gBcodeData.QUANTITY.toString()
                    tvCarton.text = gBcodeData.CARTON_NO
                    tvVendor.text = "Vendor : ${gBcodeData.VENDOR}"
                    tvDateScn.text = "Date Scanned : ${gBcodeData.TIME_STAMP}"
                    tvPICScn.text = "PIC : ${gBcodeData.BADGE}"
                    tvLocation.text = gBcodeData.LOCATION
                    edBarcode.requestFocus()
                    edBarcode.text.clear()
                }
            }
        }
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