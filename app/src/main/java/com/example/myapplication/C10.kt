package com.example.myapplication

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.URL

class C10 : AppCompatActivity() {
    private lateinit var title:TextView
    private lateinit var mainLayout:LinearLayout
    private lateinit var pb:ProgressBar
    private lateinit var c:Context
    private lateinit var CF:commonFunctions
    private lateinit var storageBin:EditText
    private lateinit var  badgeNumber:String
    private lateinit var menu:Button
    private lateinit var clear:Button
    private lateinit var GstorageBin:String
    private lateinit var scannedList:MutableList<T06.BarcodeData>
    private lateinit var btnSave:Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_c10)
        badgeNumber = intent.getStringExtra("Badge").toString()
        val function_description = intent.getStringExtra("Desc").toString()
        title = findViewById(R.id.C10TVTITLE)
        title.text = function_description
        mainLayout = findViewById(R.id.C10mainLayout)
        storageBin = findViewById(R.id.C10EDSTORBIN)
        clear = findViewById(R.id.C10BTNCLEAR)
        btnSave = findViewById(R.id.BTNC10SAVE)
        c = this
        scannedList = mutableListOf()
        menu = findViewById(R.id.C10BTNMENU)
        CF = commonFunctions()
        var itemsList = mutableListOf<Item_Data>()
        pb = findViewById(R.id.C10PRB)
        clear.setOnClickListener {
            if (mainLayout.childCount > 0){
                mainLayout.removeAllViews()
            }
            if(scannedList.size > 0 ){
                scannedList.clear()
            }
        }

        btnSave.setOnClickListener {
            val itemsLists:MutableList<submit_Data> = mutableListOf<submit_Data>()
            for (i in 0 until mainLayout.childCount) {
                val childView: View = mainLayout.getChildAt(i)
                // Get the ID of each child view
                val TBL:TableLayout = findViewById(childView.id)
                for (j in 1 until TBL.childCount){
                    val tableChild:View = TBL.getChildAt(j)
                    val childRow:TableRow = findViewById(tableChild.id)
                    val materialChildView = childRow.getChildAt(0)
                    val matChildViewTV = findViewById<TextView>(materialChildView.id)
                    val quantityChildView = childRow.getChildAt(1)
                    val qtyChildViewTV = findViewById<TextView>(quantityChildView.id)
                    val batchChildView = childRow.getChildAt(2)
                    val batchChildTV = findViewById<TextView>(batchChildView.id)
                    val inputLLChildView = childRow.getChildAt(3)
                    val CountedQtyTVView:TextView = findViewById(inputLLChildView.id)
                    val singleLineResult = submit_Data(material = matChildViewTV.text.toString(),
                        SAPquantity = qtyChildViewTV.text.toString() , PHYquantity = CountedQtyTVView.text.toString(),
                        storagebin = GstorageBin,batch = batchChildTV.text.toString(),BADGE = badgeNumber)
                    itemsLists.add(singleLineResult)

                }

            }

            runBlocking {
                val job = GlobalScope.launch {
                    sendRecordToDB(itemsLists)
                }
                job.join()
                if(scannedList.size > 0 ){
                    scannedList.clear()
                }
            }
        }

        storageBin.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
                || keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN){

                if(storageBin.text.toString() != ""){
                    GstorageBin = storageBin.text.toString()
                    runBlocking {
                        val job = GlobalScope.launch {

                            itemsList = parseJSON(getData(storageBin.text.toString())).items

                        }
                        job.join()
                        if(itemsList.size > 0){
                            if (mainLayout.childCount <= 0){
                                mainLayout.addView(BuildTable(itemsList))
                            }
                            else{
                                mainLayout.removeAllViews()
                                mainLayout.addView(BuildTable(itemsList))
                            }
                            if(scannedList.size > 0 ){
                                scannedList.clear()
                            }
                        }
                        else {

                        }
                        progressbarSetting(pb)
                    }
                }

                return@OnKeyListener true
            }
            false
        })


        menu.setOnClickListener {

        }

    }
    
    private suspend fun sendRecordToDB(data:MutableList<submit_Data>){
        // create a JSON PAYLOAD
        val jsonArray = JSONArray()
        var result = String()
        for (item in data) {
            var allReels = String()
            val allReelsInItem = scannedList.filter {
                it.pART_NO == item.material &&
                        it.lOT ==item.batch
            }
            for(line in allReelsInItem){
                allReels = allReels + line.rEEL_NO + ";"
            }

            val jsonObject = JSONObject()
            jsonObject.put("material", item.material)
            jsonObject.put("SAPquantity", item.SAPquantity)
            jsonObject.put("PHYquantity", item.PHYquantity)
            jsonObject.put("storagebin", item.storagebin)
            jsonObject.put("reeL_NUMBER", allReels)
            jsonObject.put("batch", item.batch)
            jsonObject.put("BADGE", item.BADGE)

            jsonArray.put(jsonObject)
        }
        //submit to database
        val jsonString = jsonArray.toString()
        val payLoad = "{\"input\": ${jsonString},\"storagE_LOCATION\": \"2006\"}"
        withContext(Dispatchers.IO){
            val urlLink = URL("http://172.16.206.19/BARCODEWEBAPI/API/SUBMITDATABASE")
            val conn = urlLink.openConnection()
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Content-Length", payLoad.length.toString())

            DataOutputStream(conn.getOutputStream()).use { it.writeBytes(payLoad) }

            BufferedReader(InputStreamReader(conn.getInputStream())).use { bf ->
                val stringBuilder = StringBuilder()
                var line:String?

                while (bf.readLine().also { line = it } != null) {
                    line?.let {
                        stringBuilder.append(it)
                    }
                }
                // Assign the final result
                result = stringBuilder.toString()
            }
            if(result == "SUCCESS"){
                runOnUiThread(kotlinx.coroutines.Runnable {
                    mainLayout.removeAllViews()
                    storageBin.text.clear()
                })
            }
            else{
                TriggerAlert(result,"NETWORK ERROR",storageBin)
            }
        }

    }

    private fun BuildTable(data:MutableList<Item_Data>) : View {
        val tableParam: TableLayout.LayoutParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.MATCH_PARENT
        )
        val rowParams: TableRow.LayoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.MATCH_PARENT
        )
        with(rowParams) {
            weight = 1F
        }
        val mainTable = TableLayout(c)
        mainTable.id = View.generateViewId()
        mainTable.layoutParams = tableParam
        //Add Header
        val headerRow = TableRow(c)
        headerRow.layoutParams = TableRow.LayoutParams(rowParams)
        headerRow.id = View.generateViewId()
        //headerRow.setBackgroundResource(R.drawable.border)

        headerRow.addView(CF.generateTVforRow("Material",c))
        headerRow.addView(CF.generateTVforRow("Quantity",c))
        headerRow.addView(CF.generateTVforRow("Batch",c))
        headerRow.addView(CF.generateTVforRow("CountedQty",c))
        headerRow.addView(CF.generateTVforRow("Count",c))
        mainTable.addView(headerRow)

        for (i in 0 until data.size) {
            mainTable.addView(generateRowView(data[i]))
        }

        return mainTable
    }

    private fun generateRowView(currentRow:Item_Data):View{
        val rowParams: TableRow.LayoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.MATCH_PARENT
        )
        with(rowParams) {
            weight = 1F
        }
        val materialTV = CF.generateTVforRow(currentRow.material,c)
        val quantityTV = CF.generateTVforRow(currentRow.quantity,c)
        val batchTV = CF.generateTVforRow(currentRow.batch,c)
        val countedView = CF.generateTVforRow("0",c)

        val edVerify = EditText(c)
        with(edVerify) {
            id = View.generateViewId()
            inputType = InputType.TYPE_CLASS_TEXT
            hint = "Scan Reel"
        }

        edVerify.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN
                || keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN){
                var transString = String()
                runBlocking {
                    if (edVerify.text.toString().length > 45){
                        val job = GlobalScope.launch {
                            transString = translateBcode(edVerify.text.toString())

                        }
                        job.join()
                        val barcodeResult = translateAPIResult(transString)
                        val matTextview:TextView = findViewById<TextView>(materialTV.id)
                        val scannedItems = scannedList.filter { it.pART_NO == barcodeResult.pART_NO &&
                                it.lOT == barcodeResult.lOT && it.rEEL_NO == barcodeResult.rEEL_NO }
                        if (barcodeResult.pART_NO == matTextview.text.toString()){
                            val batchTextView = findViewById<TextView>(batchTV.id)
                            if (barcodeResult.lOT == batchTextView.text.toString()){
                                if (scannedItems.isEmpty()){
                                    val SAPQtyTextView = findViewById<TextView>(quantityTV.id)
                                    val SAPQty = SAPQtyTextView.text.toString().toDouble()
                                    val BarcodeQty = barcodeResult.qUANTITY.toDouble()
                                    var total = 0.0
                                    val totalScannedItems = scannedList.filter { it.pART_NO == barcodeResult.pART_NO && it.lOT == barcodeResult.lOT }.sumOf { it.qUANTITY.toDouble() }
                                    if (scannedItems.isEmpty()){
                                        total = BarcodeQty + totalScannedItems
                                    }
                                    else {
                                        total = BarcodeQty + 0.0
                                    }

                                    if(total > SAPQty){
                                        TriggerAlert2("Quantity scanned is more than System QTY","Error wrong qty",edVerify)
                                    }
                                    else{
                                        val countedTV:TextView = findViewById(countedView.id)
                                        runOnUiThread(kotlinx.coroutines.Runnable{
                                            countedTV.text = total.toString()
                                            scannedList.add(T06.BarcodeData(vENDOR = barcodeResult.vENDOR, dATE = barcodeResult.dATE,
                                                pART_NO = barcodeResult.pART_NO, rEEL_NO = barcodeResult.rEEL_NO, lOT = barcodeResult.lOT,
                                                qUANTITY = barcodeResult.qUANTITY, uOM = barcodeResult.uOM))
                                        })
                                    }
                                }
                                else{
                                    TriggerAlert("Duplicate Reel","Duplicate Reel scanned already",edVerify)
                                }
                            }
                            else{
                                TriggerAlert("Wrong Batch","Batch is not the same as per scanned",edVerify)
                            }
                        }
                        else{
                            TriggerAlert("Wrong material","Material is not the same as per scanned",edVerify)
                        }
                        edVerify.setText("")
                        edVerify.requestFocus()
                    }
                }
                return@OnKeyListener true
            }
            false
        })

        val row = TableRow(c)
        row.layoutParams = rowParams
        row.id = View.generateViewId()

        row.addView(materialTV)
        row.addView(quantityTV)
        row.addView(batchTV)
        row.addView(countedView)
        row.addView(CF.createLinearLayout(edVerify,c))

        return row
    }

    private fun parseJSON(input:String):JsonResponse{
        val jObj = JSONObject(input)

        val msg = jObj.getString("msg")
        val itemsArr = jObj.getJSONArray("items")

        // Parse items from the itemsArray
        val itemsList = mutableListOf<Item_Data>()
        for (i in 0 until itemsArr.length()) {
            val itemObject = itemsArr.getJSONObject(i)
            val item = Item_Data(
                material = itemObject.getString("material"),
                quantity = itemObject.getString("quantity"),
                uom = itemObject.getString("uom"),
                storagebin = itemObject.getString("storagebin"),
                batch = itemObject.getString("batch")
            )

            itemsList.add(item)
        }

        return JsonResponse(msg, itemsList)
    }

    private suspend fun getData(strBin:String):String{
        var result:String = String()
        progressbarSetting(pb)
    withContext(Dispatchers.IO){

        try {
            val payLoad = "{\n" +
                    "  \"storagetyp\": \"006\",\n" +
                    "  \"storageloc\": \"2006\",\n" +
                    "  \"storagebin\": \"${strBin}\"\n" +
                    "}".trimIndent()
            val url = URL("http://172.16.206.19/BARCODEWEBAPI/API/BARCODEController1")
            val conn = url.openConnection()
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Content-Length", payLoad.length.toString())

            DataOutputStream(conn.getOutputStream()).use { it.writeBytes(payLoad) }
            BufferedReader(InputStreamReader(conn.getInputStream())).use { bf ->

                var line: String

                while (bf.readLine().also { line = it } != null) {
                    result = line
                }
                }

        }
        catch (e:Exception){
            var error:String = e.message.toString()

        }

    }

        return result

    }

    private fun progressbarSetting(v: View){
        runOnUiThread(kotlinx.coroutines.Runnable {
            if (v.visibility == View.VISIBLE) {
                v.visibility = View.INVISIBLE
            } else {
                v.visibility = View.VISIBLE
            }

        })
    }

    private fun TriggerAlert(errorMsg:String,errorTitle:String,inputView:EditText){
        runOnUiThread(kotlinx.coroutines.Runnable{
            val builder = AlertDialog.Builder(c)

            builder.setTitle(errorTitle)
            builder.setMessage(errorMsg)
            builder.setPositiveButton("OK") { dialog, which ->
                // Do something when OK button is clicked
                inputView.setText("")
            }
            builder.show()
        })
    }

    private fun TriggerAlert2(errorMsg:String,errorTitle:String,inputView:EditText){
        runOnUiThread(kotlinx.coroutines.Runnable{
            val builder = AlertDialog.Builder(c)

            builder.setTitle(errorTitle)
            builder.setMessage(errorMsg)
            builder.setPositiveButton("OK") { dialog, which ->
                // Do something when OK button is clicked
                inputView.requestFocus()
            }
            builder.show()
        })
    }

    private fun translateBcode(input: String):String{
        var APIResult:String = ""
        var url = URL("http://172.16.206.19/BARCODEWEBAPI/API/BARCODETRANSLATOR")
        val payLoad = "\"${input}\""
        val conn = url.openConnection()
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Content-Length", payLoad.length.toString())

        DataOutputStream(conn.getOutputStream()).use { it.writeBytes(payLoad) }
        try {
            BufferedReader(InputStreamReader(conn.getInputStream())).use { bf ->
                var line: String
                while (bf.readLine().also { line = it } != null) {
                    APIResult = line
                }
            }
        }
        catch (e:Exception){

        }

        return  APIResult
    }

    private fun translateAPIResult(input: String):T06.BarcodeData{

        val jobj = JSONObject(input)
        val material = jobj.getString("material")
        val vendor = jobj.getString("vendor")
        val date = jobj.getString("date")
        val reel = jobj.getString("reelnumber")
        val batch = jobj.getString("batch")
        val uom = jobj.getString("uom")
        val qty = jobj.getString("quantity")

        val result:T06.BarcodeData = T06.BarcodeData(vENDOR = vendor, dATE = date, pART_NO = material
            , rEEL_NO = reel, lOT = batch, qUANTITY = qty, uOM = uom)
        return result
    }

}

data class Item_Data(
    val material: String,
    val quantity: String,
    val uom: String,
    val storagebin: String,
    val batch: String
)

data class submit_Data(
    val material: String,
    val SAPquantity: String,
    val PHYquantity: String,
    val storagebin: String,
    val batch: String,
    val BADGE:String
)

data class JsonResponse(
    val msg: String,
    val items: MutableList<Item_Data>
)