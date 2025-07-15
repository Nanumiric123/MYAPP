package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout.DispatchChangeEvent
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.core.view.size
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Text
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


class S03 : AppCompatActivity() {
    private lateinit var title: TextView
    private lateinit var btnSave: Button
    private lateinit var btnClear:Button
    private lateinit var btnMenu:Button
    private lateinit var edPLBcode: EditText
    private lateinit var edMatBcode:EditText
    private lateinit var tvPLMat:TextView
    private lateinit var tvPLQty:TextView
    private lateinit var tblPL: TableLayout
    private lateinit var pb: ProgressBar
    private lateinit var cf:commonFunctions
    private lateinit var c: Context
    private lateinit var scannedTab:TableLayout
    private lateinit var scannedOBJ:SCANNED_ITEM
    private lateinit var scannedList:MutableList<SCANNED_ITEM>
    private lateinit var DO_NO:String
    private var LineMatScanned:Int = 0
    private var sumQtyScanned:Int = 0
    private var QtyScanned:Int = 0
    private var maxLineItems:Int = 0
    private lateinit var scannedPallet:MutableList<String>

    private fun generateHeader(refTab:TableLayout){
        var hdrRow = cf.generateRow(c)
        var hdrPartNo = cf.generateTVforRow("Part Number",c)
        var hdrLotNo = cf.generateTVforRow("Lot No",c)
        var hdrQty = cf.generateTVforRow("Quantity",c)
        hdrRow.addView(hdrPartNo)
        hdrRow.addView(hdrLotNo)
        hdrRow.addView(hdrQty)
        refTab.addView(hdrRow)
    }

    private fun clearAll(){
        edPLBcode.text.clear()
        edMatBcode.text.clear()
        tvPLMat.text = "0 / 0"
        tvPLQty.text = "0 / 0"
        scannedTab.removeAllViews()
        tblPL.removeAllViews()
        scannedOBJ = SCANNED_ITEM("","","",)
        scannedList.clear()
        LineMatScanned = 0
        sumQtyScanned = 0
        QtyScanned = 0
        maxLineItems = 0
        scannedPallet.clear()
        generateHeader(scannedTab)
        generateHeader(tblPL)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_s03)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.S03main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        scannedPallet = mutableListOf()
        scannedList = mutableListOf()
        title = findViewById(R.id.S03TITLE)
        btnSave = findViewById(R.id.S03btnSave)
        btnClear = findViewById(R.id.S03btnClear)
        btnMenu = findViewById(R.id.S03btnMenu)
        edPLBcode = findViewById(R.id.S03EDPLBcode)
        tvPLMat = findViewById(R.id.S03TVMatNo)
        tvPLQty = findViewById(R.id.S03TVPLQtySum)
        edMatBcode = findViewById(R.id.S03EDMATBCODE)
        tblPL = findViewById(R.id.S03PLTAB)
        scannedTab = findViewById(R.id.S03SCTAB)
        pb = findViewById(R.id.S03PB)
        cf = commonFunctions()
        title.text = intent.getStringExtra("Desc")
        c = this@S03

        btnClear.setOnClickListener {
            clearAll()
        }
        generateHeader(scannedTab)
        generateHeader(tblPL)
        edPLBcode.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if(keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP){
                if(edPLBcode.text.isNullOrBlank()){
                    cf.showMessage(c,"Error","Scan Picklist",
                        "OK",positiveButtonAction = { edMatBcode.text.clear() })
                        .show()
                }
                else{
                    CoroutineScope(Dispatchers.Main).launch {
                        DO_NO = edPLBcode.text.toString()
                        retrievePLitems(edPLBcode.text.toString())
                        maxLineItems = countPickListItems()
                    }

                }

                return@OnKeyListener true
            }

            false
        })
        edMatBcode.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if(keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP){

                if(edMatBcode.text.isNullOrBlank()){
                    cf.showMessage(c,"Error","Scan Pallet la",
                        "OK",positiveButtonAction = { edMatBcode.text.clear() })
                        .show()
                }
                else{
                    CoroutineScope(Dispatchers.Main).launch {
                        //perform action
                        val result = performLabelTranslation(edMatBcode.text.toString())
                        val jsonResult = JSONObject(result)
                        val material = jsonResult.getString("MATERIAL")
                        val quantity = jsonResult.getString("QUANTITY")
                        val lotNumber = jsonResult.getString("LOT_NO")
                        if(!checkDuplicatePallet(jsonResult.getString("PALLET_ID"))){
                            if(checkLotAndPN(material,lotNumber)){
                                if(checkIfMoreQty(quantity.toDouble().toInt())){
                                    scannedOBJ = SCANNED_ITEM(material,lotNumber,quantity)
                                    scannedList.add(scannedOBJ)
                                    sumQtyScanned += quantity.toDouble().toInt()
                                    tvPLQty.text = "${sumQtyScanned} / ${QtyScanned}"

                                    if(!checkMatLot(material,lotNumber)){
                                        LineMatScanned++
                                        tvPLMat.text = "${LineMatScanned} / ${maxLineItems}"

                                    }


                                    addToScannedTable(material,lotNumber,quantity)
                                    scannedPallet.add(jsonResult.getString("PALLET_ID"))
                                    edMatBcode.text.clear()
                                }
                                else{
                                    cf.showMessage(c,"Error","Quantity in Picklist is more than scanned",
                                        "OK",positiveButtonAction = { edMatBcode.text.clear() })
                                        .show()
                                }


                            }
                            else{
                                //wrong mat and lot
                                cf.showMessage(c,"Error","Wrong Label",
                                    "OK",positiveButtonAction = { edMatBcode.text.clear() })
                                    .show()
                            }
                        }
                        else{
                            //wrong mat and lot
                            cf.showMessage(c,"Error","Duplicate Pallet Noob",
                                "OK",positiveButtonAction = { edMatBcode.text.clear() })
                                .show()
                        }
                    }
                }


                return@OnKeyListener true
            }

            false
        })

        btnSave.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                if(checkIfLessQuantity()){

                    cf.showMessage(c,"Error","Quantity in Picklist is less than scanned",
                        "OK",positiveButtonAction = { edMatBcode.text.clear() })
                        .show()
                }
                else{
                    submitToSAP(scannedList)
                }

            }

        }

    }

    private fun countPickListItems():Int{
        val uniqueItems = mutableSetOf<String>()
        for(i in 1 until tblPL.size){
            val child = tblPL.getChildAt(i)
            if (child is TableRow) {
                val view = child.getChildAt(1)
                when (view) {
                    is TextView -> view.text?.toString()?.let { uniqueItems.add(it) }
                    // Add other view types as needed
                }
            }
        }
        return uniqueItems.size
    }

    private fun checkMatLot(mat:String,lot:String):Boolean{
        if(scannedTab.size > 1){
            var quantityTemp = 0
            for(i in 1 until scannedTab.size){
                val curRow = scannedTab.getChildAt(i) as TableRow
                val matTV = curRow.getChildAt(0 )as TextView
                val lotTV = curRow.getChildAt(1 )as TextView
                val qtyTV = curRow.getChildAt(2 )as TextView
                if(matTV.text.toString() == mat){
                    if(lotTV.text.toString() == lot){
                        quantityTemp += qtyTV.text.toString().toDouble().toInt()
                    }
                }
            }


            return quantityTemp > 0
        }
        else{
            return false
        }
    }

    private fun checkDuplicatePallet(pltID:String):Boolean{
        var result = false
        for (i in 0 until scannedPallet.size){
            if(scannedPallet[i] == pltID){
                result = true
                break
            }
            else{
                result = false
            }
        }
        return result
    }

    private suspend fun submitToSAP(inputScData:MutableList<SCANNED_ITEM>){
        if(inputScData.size > 0){
            val jsonBuilder = StringBuilder()
            // Start the JSON object
            jsonBuilder.append("{\n")
            jsonBuilder.append("  \"saP_DATA\": [\n")
            for(i in 0 until inputScData.size){
                jsonBuilder.append("{\n")
                jsonBuilder.append("\"batch\": \"${scannedList[i].BATCH_NO}\",\n")
                jsonBuilder.append("\"material\": \"${scannedList[i].MATERIAL}\",\n")
                jsonBuilder.append("\"dO_NUM\": \"${DO_NO}\",\n")
                jsonBuilder.append("\"quantity\": \"${scannedList[i].QUANTITY}\",\n")
                jsonBuilder.append("\"headeR_TXT\": \"${intent.getStringExtra("Badge")}_S03\"\n")
                jsonBuilder.append("}")
                if(i < scannedList.size - 1){
                    jsonBuilder.append(",")
                }
                jsonBuilder.append("\n")
            }
            jsonBuilder.append("  ],\n")
            jsonBuilder.append("  \"badge\": \"B0060\"\n")
            jsonBuilder.append("}")
            val jsonPayload = jsonBuilder.toString()
            withContext(Dispatchers.IO){
                withContext(Dispatchers.Main){
                    setProgressBar(pb,true)
                }
                var response = String()
                try{
                    val urlLink = URL("http://172.16.206.19/BARCODEWEBAPI/API/S03_DOISSUE")
                    val conn = urlLink.openConnection()
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("Content-Length", jsonPayload.length.toString())

                    DataOutputStream(conn.getOutputStream()).use { it.writeBytes(jsonPayload) }
                    BufferedReader(InputStreamReader(conn.getInputStream())).use { bf ->
                        val stringBuilder = StringBuilder()
                        var line:String?

                        while (bf.readLine().also { line = it } != null) {
                            line?.let {
                                stringBuilder.append(it)
                            }
                        }
                        response = stringBuilder.toString()
                    }
                }
                catch(e:Exception){
                    response = e.message.toString()
                }
                finally {
                    withContext(Dispatchers.Main){
                        setProgressBar(pb,false)
                        runOnUiThread(kotlinx.coroutines.Runnable {
                            cf.showMessage(c,"Message",response,"OK",positiveButtonAction = { clearAll() }).show()
                        })

                    }
                }
            }

        }
    }


    private fun checkLotAndPN(material:String,l_num:String):Boolean{
        return if(tblPL.size > 1){

            var result = false
            for(i in 0 until tblPL.size){
                val scannedTabRow = tblPL.getChildAt(i) as TableRow
                val partNumRowTV = scannedTabRow.getChildAt(0) as TextView
                val lotNumRowTV = scannedTabRow.getChildAt(1) as TextView
                if(material == partNumRowTV.text.toString()){
                    if(lotNumRowTV.text.toString() == l_num){
                        result = true
                        break
                    }
                    else{
                        result = false
                    }
                }
                else{
                    result =false
                }
            }
            result
        }
        else{
             false
        }
    }

    private fun checkIfLessQuantity():Boolean{
        var qtyPL = 0
        var qtyScanned = 0
        for(i in 1 until tblPL.size){
            val curRow = tblPL.getChildAt(i) as TableRow
            val qtyTV = curRow.getChildAt(2) as TextView
            val qtyTemp = qtyTV.text.toString().toDouble().toInt()
            qtyPL += qtyTemp
        }
        for(i in 1 until scannedTab.size){
            val curRow = scannedTab.getChildAt(i) as TableRow
            val qtyTV = curRow.getChildAt(2) as TextView
            val qtyTemp = qtyTV.text.toString().toDouble().toInt()
            qtyScanned += qtyTemp
        }
        return qtyScanned < qtyPL
    }

    private fun checkIfMoreQty(qtyLbl:Int):Boolean{
        var qtyPL = 0
        var qtyScanned = 0
        for(i in 1 until tblPL.size){
            val curRow = tblPL.getChildAt(i) as TableRow
            val qtyTV = curRow.getChildAt(2) as TextView
            val qtyTemp = qtyTV.text.toString().toDouble().toInt()
            qtyPL += qtyTemp
        }
        for(i in 1 until scannedTab.size){
            val curRow = scannedTab.getChildAt(i) as TableRow
            val qtyTV = curRow.getChildAt(2) as TextView
            val qtyTemp = qtyTV.text.toString().toDouble().toInt()
            qtyScanned += qtyTemp
        }
        qtyScanned += qtyLbl
        return qtyPL >= qtyScanned
    }

    private fun addToScannedTable(p_number:String,l_number:String,qty:String){
        var itmRow = cf.generateRow(c)
        var itmPartNo = cf.generateTVforRow(p_number,c)
        var itmlotNo = cf.generateTVforRow(l_number,c)
        var itmQty = cf.generateTVforRow(qty,c)
        itmRow.addView(itmPartNo)
        itmRow.addView(itmlotNo)
        itmRow.addView(itmQty)
        scannedTab.addView(itmRow)
    }

    private suspend fun retrievePLitems(doNo:String){
        withContext(Dispatchers.IO){
            try {
                withContext(Dispatchers.Main){
                    setProgressBar(pb,true)
                }
                val urlLink = URL("http://172.16.206.19/BARCODEWEBAPI/API/S01_PICKLIST?do_num=$doNo")
                val connection = urlLink.openConnection() as HttpURLConnection
                // Set request method
                connection.requestMethod = "GET"
                // Set timeout (in milliseconds)
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                // Get response code
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read response
                    val inputStream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val response = StringBuilder()

                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()
                    runOnUiThread(Runnable{
                        buildPickListTable(response.toString())
                    })


                } else {
                    runOnUiThread(Runnable{
                        cf.showMessage(c,"Error","Message : Error code ${responseCode}","Dismiss",
                            positiveButtonAction = { tvPLMat.text = String() }).show()
                    })

                }
            }
            catch(ex:Exception){
                runOnUiThread(Runnable{
                    cf.showMessage(c,"Error","Message : ${ex.message.toString()}","Dismiss",
                        positiveButtonAction = { tvPLMat.text = String() }).show()
                })

            }
            finally {
                withContext(Dispatchers.Main){
                    setProgressBar(pb,false)
                }
            }

        }
    }
    private suspend fun performLabelTranslation(bc:String): String{
        return withContext(Dispatchers.IO){
            withContext(Dispatchers.Main){
                setProgressBar(pb,true)
            }
            val url = URL("http://172.16.206.19/REST_API/Home/breakpalletbarcodeString?barcode=$bc")
            try {
                url.readText()
            } catch (e:Exception){
                e.message.toString()
            } finally {
                withContext(Dispatchers.Main){
                    setProgressBar(pb,false)
                }

            }

        }
    }

    private fun buildPickListTable(plString:String){
        if(tblPL.size <= 1){
            val jsonArr = JSONArray(plString)
            var totalLineItems = 0
            var totalQty = 0
            for (i in 0 until jsonArr.length()){
                //Generate rows for your table later

                val row = cf.generateRow(c)
                val matNoTV = cf.generateTVforRow(jsonArr.getJSONObject(i).getString("materiaL_NO"),c)
                val batchNumTV = cf.generateTVforRow(jsonArr.getJSONObject(i).getString("batcH_NO"),c)
                val pickQtyTV = cf.generateTVforRow(jsonArr.getJSONObject(i).getString("picK_QTY"),c)
                totalQty += jsonArr.getJSONObject(i).getString("picK_QTY").toDouble().toInt()
                totalLineItems++
                row.addView(matNoTV)
                row.addView(batchNumTV)
                row.addView(pickQtyTV)
                tblPL.addView(row)
            }
            runOnUiThread(kotlinx.coroutines.Runnable {
                tvPLMat.text = "0 / ${totalLineItems}"
                tvPLQty.text = "0 / ${totalQty}"
                QtyScanned = totalQty
            })

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
    data class SCANNED_ITEM(var MATERIAL:String, var BATCH_NO:String,var QUANTITY:String)
}