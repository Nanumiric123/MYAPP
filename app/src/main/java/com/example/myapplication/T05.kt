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
import androidx.fragment.app.Fragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.UUID

class T05 : AppCompatActivity() {

    private lateinit var inBarcode:EditText
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
    private var dataList:MutableList<SCANNED_DATA> = mutableListOf()
    private lateinit var matscn:TextView
    private lateinit var prefLoc:TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_t05)
        inBarcode = findViewById(R.id.T05EDBARCODE)
        tvMat = findViewById(R.id.T05TVMATERIAL)
        tvBat = findViewById(R.id.T05TVBATCH)
        tvReel = findViewById(R.id.T05TVREELNUM)
        tvQty = findViewById(R.id.T05EDQUANTITY)
        inLoc = findViewById(R.id.T05EDINLOCATION)
        sw = findViewById(R.id.T05SWSM)
        pb = findViewById(R.id.T05PB)
        matscn = findViewById(R.id.T05TVMatScn)
        prefLoc = findViewById(R.id.T05TVPL)
        c = this
        btnTransfer = findViewById(R.id.T05BTNTRANSFER)
        gbadgeNum = intent.getStringExtra("Badge").toString()
        inBarcode.requestFocus()
        hideKeyboard()
        var barcodeResult = SCANNED_DATA(VENDOR = "",
            DATE = "",
            MATERIAL = "",
            REEL_NO = "",
            LOT = "",
            QUANTITY = "",)

        sw.setOnCheckedChangeListener { buttonView, isChecked ->
            // do something, the isChecked will be
            // true if the switch is in the On position
            val mainLayout:LinearLayout = findViewById(R.id.T05LinearHoriz)
            mainLayout.removeAllViews()
            dataList.clear()
            inLoc.requestFocus()
            hideKeyboard()
        }

        inBarcode.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN
            ) {
                //Perform Code
                hideKeyboard()
                runBlocking {
                    val job = GlobalScope.launch {
                        runOnUiThread {
                            setProgressBar(pb)
                        }
                        barcodeResult = translateBarcode(inBarcode.text.toString());
                        if(!barcodeResult.MATERIAL.isNullOrBlank()){
                            if(sw.isChecked){
                                var UUIDInfo = UUID.randomUUID().toString()
                                var res:String = ""
                                runBlocking {
                                    val job = GlobalScope.launch {
                                        res = saveToSAP(barcodeResult,gbadgeNum,UUIDInfo);

                                    }
                                    job.join()
                                    val showResultDialog = AlertDialog.Builder(c)
                                    showResultDialog.setTitle(res.split('$')[0].replace("\"",""))
                                    showResultDialog.setMessage(res.split('$')[1].replace("\"",""))
                                    showResultDialog.setPositiveButton("ok") { _, _ ->
                                        inBarcode.text.clear()
                                        inBarcode.requestFocus()
                                        runOnUiThread (
                                            Runnable {
                                                generateTable(barcodeResult)
                                            }
                                        )

                                        dataList.add(barcodeResult)
                                        matscn.text = "Material Scanned : ${dataList.size}"
                                        tvMat.text = getString(R.string.matterialTextview_description)
                                        tvBat.text = getString(R.string.BatchLabel)
                                        tvReel.text = getString(R.string.ReelNum)
                                        tvQty.setText(getString(R.string.tvQuantityTxt, ""))


                                    }
                                    runOnUiThread {
                                        showResultDialog.show()
                                    }
                                }

                            }
                            else{
                                tvMat.text = getString(R.string.tvMaterialTxt, barcodeResult.MATERIAL)
                                tvBat.text = getString(R.string.tvBatchTxt, barcodeResult.LOT)
                                tvReel.text = getString(R.string.tvReelNumTxt, barcodeResult.REEL_NO)
                                runOnUiThread ( Runnable {
                                    tvQty.setText(barcodeResult.QUANTITY)
                                })
                            }


                            runOnUiThread {
                                inLoc.requestFocus()
                                inBarcode.text.clear()
                            }

                        }
                        else{

                            val wrongBarcodeDialog = AlertDialog.Builder(c)
                            wrongBarcodeDialog.setTitle("Wrong Barcode")
                            wrongBarcodeDialog.setMessage("Wrong barcode scanned please scan the 2D barcode")
                            wrongBarcodeDialog.setPositiveButton("Scan Again") { _, _ ->
                                inBarcode.text.clear()
                                inBarcode.requestFocus()
                            }
                            runOnUiThread {
                                wrongBarcodeDialog.show()
                            }
                            barcodeResult = SCANNED_DATA(VENDOR = "",
                                DATE = "",
                                MATERIAL = "",
                                REEL_NO = "",
                                LOT = "",
                                QUANTITY = "",)
                        }
                    }
                    job.join()
                    setProgressBar(pb)
                }




                return@OnKeyListener true
            }
            false
        })

        inLoc.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN
            ) {
                //Perform Code
                hideKeyboard()
                if(sw.isChecked){
                    inBarcode.requestFocus()
                }
                return@OnKeyListener true
            }
            false
        })

        btnTransfer.setOnClickListener {
            var UUIDInfo = UUID.randomUUID().toString()
            var res:String = ""
            if (!barcodeResult.MATERIAL.isNullOrBlank() || !inLoc.text.isNullOrBlank()){
                runBlocking {
                    val job = GlobalScope.launch {

                        runOnUiThread {
                            setProgressBar(pb)
                        }
                        barcodeResult.QUANTITY = tvQty.text.toString()
                        res = saveToSAP(barcodeResult,gbadgeNum.toString(),UUIDInfo);

                    }
                    job.join()
                    val showResultDialog = AlertDialog.Builder(c)
                    showResultDialog.setTitle(res.split('$')[0].replace("\"",""))
                    showResultDialog.setMessage(res.split('$')[1].replace("\"",""))
                    showResultDialog.setPositiveButton("ok") { _, _ ->
                       inBarcode.text.clear()
                        inBarcode.requestFocus()
                        inLoc.text.clear()
                        tvMat.text = getString(R.string.matterialTextview_description)
                        tvBat.text = getString(R.string.BatchLabel)
                        tvReel.text = getString(R.string.ReelNum)
                        tvQty.setText(getString(R.string.tvQuantityTxt, ""))
                    }
                    runOnUiThread {
                        showResultDialog.show()
                    }
                    setProgressBar(pb)
                }
            }

        }

    }

    private fun generateTable(dt:SCANNED_DATA){

        val mainLayout: LinearLayout = findViewById(R.id.T05LinearHoriz)
        var tableParam: TableLayout.LayoutParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.WRAP_CONTENT)
        var rowParams: TableRow.LayoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.MATCH_PARENT)
        with(rowParams){
            weight = 1F
        }

        var dataTable = TableLayout(c)
        dataTable.layoutParams = tableParam

        val pullListRow = TableRow(c)
        with(pullListRow){
            gravity = Gravity.CENTER
            layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT)
            setBackgroundResource(R.drawable.cell_with_border)
        }

        val tvMaterial = TextView(c)
        with(tvMaterial){
            text = dt.MATERIAL
            textSize = 16F
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT)
            setBackgroundResource(R.drawable.cell_with_border)
            setPadding(15,5,15,10)
        }
        val tvQuantity = TextView(c)
        with(tvQuantity){
            text = dt.QUANTITY
            textSize = 16F
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT)
            setBackgroundResource(R.drawable.cell_with_border)
            setPadding(15,5,15,10)
        }
        val tvReelNo = TextView(c)
        with(tvReelNo){
            text = dt.REEL_NO
            textSize = 16F
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT)
            setBackgroundResource(R.drawable.cell_with_border)
            setPadding(15,5,15,10)
        }
        val tvBatchNo = TextView(c)
        with(tvBatchNo){
            text = dt.LOT
            textSize = 16F
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT)
            setBackgroundResource(R.drawable.cell_with_border)
            setPadding(15,5,15,10)
        }

        pullListRow.addView(tvMaterial)
        pullListRow.addView(tvBatchNo)
        pullListRow.addView(tvReelNo)
        pullListRow.addView(tvQuantity)

        dataTable.addView(pullListRow)

        mainLayout.addView(dataTable)
    }

    fun Fragment.hideKeyboard() {
        view?.let { activity?.hideKeyboard(it) }
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
    private suspend fun saveToSAP(barcodeData:SCANNED_DATA,bNum:String,deviceNo:String):String{
        var result:String = String()
        withContext(Dispatchers.IO){
            var linkString = "http://172.16.206.19/REST_API/SMT_EKANBAN_SERVICES/MPPT05PutMaterialInLocation?" +
                    "location=${inLoc.text}&mat=${barcodeData.MATERIAL}&batch=${barcodeData.LOT}&" +
                    "reelNum=${barcodeData.REEL_NO}&quantity=${barcodeData.QUANTITY}&badgeNumber=${bNum}&" +
                    "deviceNo=${deviceNo}"
            var urlLink = URL(linkString)
            result = urlLink.readText()
        }
            return result
    }

    private fun translateBarcode(barcodeString:String):SCANNED_DATA{
        var linkString = "http://172.16.206.19/REST_API/SMT_EKANBAN_SERVICES/BreakBarcodeToArray?barcode=${barcodeString}"
        var linkURL = URL(linkString)
        var result = linkURL.readText()
        var jsonObj = JSONObject(result)
        var resultFinal = SCANNED_DATA(VENDOR = jsonObj.getString("VENDOR"),
            DATE = jsonObj.getString("DATE"),
            MATERIAL = jsonObj.getString("PART_NO"),
            REEL_NO = jsonObj.getString("REEL_NO"),
            LOT = jsonObj.getString("LOT"),
            QUANTITY = jsonObj.getString("QUANTITY")
        )
        try{

        }
        catch (e:Exception){

        }
        finally {
            var pLoc = getPrefLoc(resultFinal.MATERIAL)
            prefLoc.setText("Prefered Location : ${pLoc[1].replace("\"","")}")
        }

        return resultFinal
    }

    private fun getPrefLoc(mat:String):List<String> {
        var PrefLocLink = URL("http://172.16.206.19/REST_API/Third/GETPREFLOC?material=${mat}&storLoc=2006")
        var result = PrefLocLink.readText()
        var resultSplit = result.split(':')

        return resultSplit;

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