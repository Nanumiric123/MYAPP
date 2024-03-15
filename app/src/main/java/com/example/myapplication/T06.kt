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
import android.widget.Toast
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

class T06 : AppCompatActivity() {
    private lateinit var inBarcode:EditText
    private lateinit var tvMat:TextView
    private lateinit var tvBat:TextView
    private lateinit var tvReel:TextView
    private lateinit var tvQty:EditText
    private lateinit var inLocationRack:EditText
    private lateinit var btnTransfer:Button
    private lateinit var c:Context
    private lateinit var gbadgeNum:String
    private lateinit var pb:ProgressBar
    private lateinit var sw:Switch
    private  lateinit var itemCount:TextView
    private lateinit var prefLoc:TextView

    private var dataList:MutableList<BarcodeData> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_t06)
        inBarcode = findViewById(R.id.T06EDBarcode)
        tvMat = findViewById(R.id.T06TVMaterial)
        tvBat = findViewById(R.id.T06TVBatch)
        tvReel = findViewById(R.id.T06TVREEL)
        tvQty = findViewById(R.id.T06EDQuantity)
        inLocationRack = findViewById(R.id.T06EDRACK)
        btnTransfer = findViewById(R.id.T06BTNTransfer)
        itemCount = findViewById(R.id.tv06CountItems)
        sw = findViewById(R.id.T06SWSM)
        prefLoc = findViewById(R.id.T06TVPL)
        c = this
        pb = findViewById(R.id.T06PB)
        gbadgeNum = intent.getStringExtra("Badge").toString()
        var translatedBarcode = BarcodeData("","","","","","","")
        inBarcode.requestFocus()
        hideKeyboard()
        sw.setOnCheckedChangeListener { buttonView, isChecked ->
            // do something, the isChecked will be
            // true if the switch is in the On position
            val mainLayout:LinearLayout = findViewById(R.id.T06LinearHoriz)
            mainLayout.removeAllViews()
            dataList.clear()
            inLocationRack.requestFocus()
            hideKeyboard()
        };

        inBarcode.setOnKeyListener(View.OnKeyListener { view, i, keyEvent ->
            if(i == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_DOWN
                || i == KeyEvent.KEYCODE_TAB && keyEvent.action == KeyEvent.ACTION_DOWN){
                //Perform Code
                hideKeyboard()

                if(inBarcode.text.isNotEmpty() || inBarcode.text.isNotBlank()){
                    if(sw.isChecked){

                        runBlocking {
                            val job = GlobalScope.launch {
                                translatedBarcode = translateBarcode(inBarcode.text.toString())
                            }
                            job.join()
                            if(translatedBarcode.pART_NO.isNullOrBlank() || translatedBarcode.lOT.isNullOrBlank() ||
                                translatedBarcode.qUANTITY.isNullOrBlank()){
                                inBarcode.text.clear()
                                inBarcode.requestFocus()
                            }
                            else{
                                if(checkDuplicates(translatedBarcode.pART_NO,translatedBarcode.rEEL_NO)){
                                    val builder = AlertDialog.Builder(c)
                                    builder.setTitle(R.string.Warning)
                                    builder.setMessage("Duplicate Carton")
                                    builder.setPositiveButton("OK") { dialog, which ->
                                        // Do something when OK button is clicked
                                        inBarcode.requestFocus()
                                        inBarcode.setText("")
                                        hideKeyboard()
                                    }
                                    builder.show()
                                }
                                else{
                                    var res:String = ""
                                    var UUIDInfo = UUID.randomUUID().toString()
                                    runBlocking {
                                        val job = GlobalScope.launch {
                                            setProgressBar(pb)
                                            //translatedBarcode.qUANTITY = tvQty.text.toString()
                                            res = saveToSAP(translatedBarcode,gbadgeNum,UUIDInfo);

                                        }
                                        job.join()
                                        val showResultDialog = AlertDialog.Builder(c)
                                        showResultDialog.setTitle(res.split('$')[0].replace("\"",""))
                                        showResultDialog.setMessage(res.split('$')[1].replace("\"",""))
                                        showResultDialog.setPositiveButton("ok") { _, _ ->
                                            inBarcode.text.clear()
                                            inBarcode.requestFocus()
                                            if(sw.isChecked){

                                            }
                                            else{
                                                inLocationRack.text.clear()
                                            }

                                            tvMat.text = getString(R.string.matterialTextview_description)
                                            tvBat.text = getString(R.string.BatchLabel)
                                            tvReel.text = getString(R.string.ReelNum)
                                            tvQty.setText(getString(R.string.quantity_label))
                                            hideKeyboard()
                                        }
                                        runOnUiThread {
                                            showResultDialog.show()
                                        }
                                        setProgressBar(pb)
                                    }
                                    dataList.add(translatedBarcode)
                                    generateTable(translatedBarcode)
                                    itemCount.text = "Total Items Scanned : ${dataList.size}"
                                    inBarcode.text.clear()
                                    inBarcode.requestFocus()
                                }

                            }

                        }
                    }
                    else{
                        runBlocking {
                            val job = GlobalScope.launch {
                                translatedBarcode = translateBarcode(inBarcode.text.toString())
                            }
                            job.join()
                            if(translatedBarcode.lOT.isNotBlank() && translatedBarcode.lOT.isNotEmpty()){
                                tvMat.text = getString(R.string.tvMaterialTxt, translatedBarcode.pART_NO)
                                tvBat.text = getString(R.string.tvBatchTxt, translatedBarcode.lOT)
                                tvReel.text = getString(R.string.tvReelNumTxt, translatedBarcode.rEEL_NO)
                                runOnUiThread(Runnable {
                                    tvQty.setText(translatedBarcode.qUANTITY)
                                })

                                inBarcode.text.clear()
                                inLocationRack.requestFocus()
                            }
                        }
                    }

                }
                else{
                    val builder = AlertDialog.Builder(c)
                    builder.setTitle(R.string.ErrorTitle)
                    builder.setMessage(getString(R.string.no_barcode_scanned))
                    builder.setPositiveButton("OK") { dialog, which ->
                        // Do something when OK button is clicked
                        inBarcode.requestFocus()
                        hideKeyboard()
                    }
                    builder.show()
                }
                return@OnKeyListener true
            }
            false
        })

        inLocationRack.setOnKeyListener(View.OnKeyListener { view, i, keyEvent ->
            if(i == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_DOWN
                || i == KeyEvent.KEYCODE_TAB && keyEvent.action == KeyEvent.ACTION_DOWN){
                //Perform Code
                if(sw.isChecked){
                    inBarcode.requestFocus()
                }
                hideKeyboard()
                return@OnKeyListener true
            }

            false
        })

        btnTransfer.setOnClickListener {
            var UUIDInfo = UUID.randomUUID().toString()
            var res:String = ""
            if(!translatedBarcode.pART_NO.isNullOrBlank() || !inLocationRack.text.isNullOrBlank()){
                runBlocking {
                    val job = GlobalScope.launch {
                        setProgressBar(pb)
                        translatedBarcode.qUANTITY = tvQty.text.toString()
                        res = saveToSAP(translatedBarcode,gbadgeNum,UUIDInfo);
                    }
                    job.join()
                    val showResultDialog = AlertDialog.Builder(c)
                    showResultDialog.setTitle(res.split('$')[0].replace("\"",""))
                    showResultDialog.setMessage(res.split('$')[1].replace("\"",""))
                    showResultDialog.setPositiveButton("ok") { _, _ ->
                        inBarcode.text.clear()
                        inBarcode.requestFocus()
                        inLocationRack.text.clear()
                        tvMat.text = getString(R.string.matterialTextview_description)
                        tvBat.text = getString(R.string.BatchLabel)
                        tvReel.text = getString(R.string.ReelNum)
                        tvQty.setText(getString(R.string.quantity_label))
                        hideKeyboard()
                    }
                    runOnUiThread {
                        showResultDialog.show()
                    }
                    setProgressBar(pb)
                }
            }

        }

    }

    private fun generateTable(dt:BarcodeData){
        val mainLayout:LinearLayout = findViewById(R.id.T06LinearHoriz)
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
            text = dt.pART_NO
            textSize = 12F
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT)
            setBackgroundResource(R.drawable.cell_with_border)
            setPadding(15,5,15,10)
        }
        val tvQuantity = TextView(c)
        with(tvQuantity){
            text = dt.qUANTITY
            textSize = 12F
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT)
            setBackgroundResource(R.drawable.cell_with_border)
            setPadding(15,5,15,10)
        }
        val tvReelNo = TextView(c)
        with(tvReelNo){
            text = dt.rEEL_NO
            textSize = 12F
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT)
            setBackgroundResource(R.drawable.cell_with_border)
            setPadding(15,5,15,10)
        }
        val tvBatchNo = TextView(c)
        with(tvBatchNo){
            text = dt.lOT
            textSize = 12F
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

        itemCount.text = "Items Scanned : ${dataList.size}"
        mainLayout.addView(dataTable)

    }

    private suspend fun checkDuplicates(matrn:String,ctnNo:String):Boolean{
        var res = 0
        var resF:Boolean = false

        withContext(Dispatchers.Default){
            runBlocking {
                val job = GlobalScope.launch {
                    var result = URL("http://172.16.206.19/REST_API/Fourth/CheckForDuplicated?cartonNo=${ctnNo}&material=${matrn}&tcode=T06").readText()
                    //http://172.16.206.19/REST_API/Fourth/CheckForDuplicated?cartonNo=T23-2074474&material=F1H1E225A201&tcode=T07
                    res = result.replace("\"","").toInt()

                }
                job.join()
                if(dataList.size > 0){
                    val itm = dataList.filter{it.pART_NO == matrn && it.rEEL_NO == ctnNo}
                    if(itm.isNotEmpty()){
                        res++
                    }
                }
                if(res > 0){
                    resF = true
                }
            }
        }



        return resF
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

    private fun translateBarcode(bc:String):BarcodeData {

        var tempItem = BarcodeData("","","","","","","")
        try{
            val linkUrl = URL(getString(R.string.barcodeTranslator,bc))
            val jsonOBJ = JSONObject(linkUrl.readText())

            tempItem = BarcodeData(jsonOBJ.getString("VENDOR"),jsonOBJ.getString("DATE")
                ,jsonOBJ.getString("PART_NO"),jsonOBJ.getString("REEL_NO"),jsonOBJ.getString("LOT"),
                jsonOBJ.getString("QUANTITY"),jsonOBJ.getString("UOM"))
        }
        catch (ex:Exception){
            Toast.makeText(c, ex.message.toString(), Toast.LENGTH_SHORT).show()
        }
        finally{
            var pLoc = getPrefLoc(tempItem.pART_NO)

            prefLoc.setText("Prefered Location : ${pLoc[1]}")

        }
        return tempItem
    }

    private fun getPrefLoc(mat:String):List<String> {
        var PrefLocLink = URL("http://172.16.206.19/REST_API/Third/GETPREFLOC?material=${mat}&storLoc=2006")
        var result = PrefLocLink.readText()
        var resultSplit = result.split(':')

        return resultSplit;

    }

    private fun saveToSAP(barcodeData:BarcodeData,bNum:String,deviceNo:String):String{
        var linkString = "http://172.16.206.19/REST_API/SMT_EKANBAN_SERVICES/MPPT06PutMaterialInLocation?" +
                "location=${inLocationRack.text}&mat=${barcodeData.pART_NO}&batch=${barcodeData.lOT}&" +
                "reelNum=${barcodeData.rEEL_NO}&quantity=${barcodeData.qUANTITY}&badgeNumber=${bNum}&" +
                "deviceNo=${deviceNo}"
        var urlLink = URL(linkString)
        return urlLink.readText()
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

    data class BarcodeData(var vENDOR:String, var dATE:String, var pART_NO:String,
                           var rEEL_NO:String, var lOT:String, var qUANTITY:String, val uOM:String)
}