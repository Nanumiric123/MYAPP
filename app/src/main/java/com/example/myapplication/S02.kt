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
import androidx.core.view.size
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.properties.Delegates

class S02 : AppCompatActivity() {

    private data class S02DS(var VBELN:String,var MATNR:String,var LOT_NO:String,var PALLT_GRP:String,var EXDIV:String,var BRGEW:String,var NTGEW:String,
        var VOLUM:String,var LAENG:String,var BREIT:String,var HOEHE:String,var TAVOL:String,var NUM_CAR:String,var QUANCAR:String)
    private data class S02SL(var VBELN:String,var MATNR:String,var PALLT_GRP:String,var EXDIV:String,var BRGEW:String,var NTGEW:String,
                             var VOLUM:String,var LAENG:String,var BREIT:String,var HOEHE:String,var TAVOL:String,var NUM_CAR:String,var QUANCAR:String)

    private lateinit var TITLE:TextView
    private lateinit var badgeNum:String
    private lateinit var tvDo_num:EditText
    private lateinit var pickListTable:TableLayout
    private lateinit var scannedTable:TableLayout
    private lateinit var pb:ProgressBar
    private lateinit var c:Context
    private lateinit var menuBtn: Button
    private lateinit var clearBtn:Button
    private lateinit var saveBtn:Button
    private lateinit var nextBtn:Button
    private lateinit var okBtn:Button
    private lateinit var mainLblBcode:EditText
    private lateinit var cf:commonFunctions
    private lateinit var partTV:TextView
    private lateinit var GIDTV:TextView
    private var g_currentGroupID by Delegates.notNull<Int>()
    private lateinit var noOfcartonTV:TextView
    private lateinit var palletIDTV:TextView
    private lateinit var S02DATA:MutableList<S02DS>
    private lateinit var S02lblData:palletLabel
    private lateinit var S02SUBMITLIST:MutableList<S02SL>
    private lateinit var grossWeightED:EditText
    private lateinit var dimensionED:EditText
    private lateinit var lengthED:EditText
    private lateinit var widthED:EditText
    private lateinit var heightED:EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_s02)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        scannedTable = findViewById(R.id.S02TLSB)
        okBtn = findViewById(R.id.s02btnOK)
        nextBtn = findViewById(R.id.s02btnNxt)
        clearBtn = findViewById(R.id.s02btnClear)
        heightED = findViewById(R.id.S02EDHEI)
        widthED = findViewById(R.id.S02EDWID)
        lengthED = findViewById(R.id.S02EDLEN)
        dimensionED = findViewById(R.id.S02EDDim)
        grossWeightED = findViewById(R.id.S02EDGW)
        S02DATA = mutableListOf()
        S02SUBMITLIST = mutableListOf()
        noOfcartonTV = findViewById(R.id.s02TVNoCarton)
        palletIDTV = findViewById(R.id.s02TVpltID)
        pickListTable = findViewById(R.id.S02TLPicklist)
        g_currentGroupID = 1
        pb = findViewById(R.id.S02PB)
        cf = commonFunctions()
        TITLE = findViewById(R.id.S02TVTITLE)
        TITLE.text = intent.getStringExtra("Desc").toString()
        badgeNum = intent.getStringExtra("Badge").toString()
        c = this@S02
        tvDo_num = findViewById(R.id.S02DONUMBER)
        mainLblBcode = findViewById(R.id.S02palletLabel)
        partTV = findViewById(R.id.s02TVpart)
        GIDTV = findViewById(R.id.s02TVgroupID)
        saveBtn = findViewById(R.id.s02btnSave)
        menuBtn = findViewById(R.id.s02btnMenu)

        menuBtn.setOnClickListener { this@S02.finish() }
        clearBtn.setOnClickListener {
            clearForm()
        }
        tvDo_num.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if(keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP){
                if(!tvDo_num.text.isNullOrEmpty()){
                    if(pickListTable.size <= 1){
                        CoroutineScope(Dispatchers.Main).launch {
                            retrieveDOdata(tvDo_num.text.toString())
                            mainLblBcode.requestFocus()
                        }
                    }
                    else{
                        regeneratePLTable()
                    }

                }
                else{
                    cf.showMessage(c,"Error","Please scan pick list first","Dismiss",positiveButtonAction = {
                        // Handle positive button click
                        tvDo_num.text.clear()
                        tvDo_num.requestFocus()
                    }).show()
                }

                return@OnKeyListener true
            }
            false
        })

        mainLblBcode.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if(keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP){
                if (pickListTable.size > 1){
                    if(!mainLblBcode.text.isNullOrEmpty()){
                        CoroutineScope(Dispatchers.Main).launch{
                            S02lblData = translateText(mainLblBcode.text.toString())
                            if(checkPLLotPN(S02lblData.LOT_NO,S02lblData.MATERIAL)){
                                partTV.text = "Part Number \n ${S02lblData.MATERIAL}"
                                GIDTV.text = "Pallet Group \n ${generatePalletID(g_currentGroupID)}"
                                noOfcartonTV.text = "Number of Carton \n ${S02lblData.CARTON_QTY}"
                                palletIDTV.text = "Pallet ID \n ${S02lblData.PALLET_ID}"
                                mainLblBcode.text.clear()
                            }
                            else{
                                cf.showMessage(c,"Error","Wrong label",
                                    "Dismiss",positiveButtonAction = {
                                        // Handle positive button click
                                        mainLblBcode.text.clear()
                                    }).show()
                            }


                        }
                    }
                    else{
                        val ad = cf.showMessage(c,"Error","Please scan label first","Dismiss",positiveButtonAction = {
                            // Handle positive button click
                            mainLblBcode.text.clear()
                            mainLblBcode.requestFocus()
                        })
                        ad.show()
                    }

                }
                else{
                    var ad = cf.showMessage(c,"Error","Please scan the picklist first",
                        "Dismiss",positiveButtonAction = {
                        // Handle positive button click
                        tvDo_num.text.clear()
                        tvDo_num.requestFocus()
                        mainLblBcode.text.clear()
                        mainLblBcode.requestFocus()
                    })
                    ad.show()
                }


                return@OnKeyListener true
            }
            false
        })

        okBtn.setOnClickListener {
            if(pickListTable.size > 1){
                val temp = S02DS(tvDo_num.text.toString(),S02lblData.MATERIAL,S02lblData.LOT_NO,generatePalletID(g_currentGroupID),
                    S02lblData.PALLET_ID,grossWeightED.text.toString(),"0.000",dimensionED.text.toString(),
                    lengthED.text.toString(),widthED.text.toString(),heightED.text.toString(),S02lblData.QUANTITY,
                    S02lblData.CARTON_QTY,"0")
                if(checkPLLotPN(S02lblData.LOT_NO,S02lblData.MATERIAL)){
                    if(!checkDuplicatePallet(S02lblData.PALLET_ID)){
                        if(checktotalQuantity(S02lblData.MATERIAL,S02lblData.LOT_NO,S02lblData.QUANTITY.toDouble().toInt())){
                            S02DATA.add(temp)
                        }
                        else{
                            cf.showMessage(c,"Error","Scan Quantity is over pallet quantity",
                                "Dismiss",positiveButtonAction = {
                                    // Handle positive button click

                                }).show()
                        }

                    }
                    else{
                        cf.showMessage(c,"Error","Duplicate Pallet ID",
                            "Dismiss",positiveButtonAction = {
                                // Handle positive button click

                            }).show()
                    }

                }
                else{
                    cf.showMessage(c,"Error","Wrong label",
                        "Dismiss",positiveButtonAction = {
                            // Handle positive button click

                        }).show()
                }
            }
            else{
                cf.showMessage(c,"Error","Scan Picklist Table first",
                    "Dismiss",positiveButtonAction = {
                        // Handle positive button click

                    }).show()
            }



        }
        nextBtn.setOnClickListener {
            if(!checkpliflessQuantity(S02lblData.MATERIAL,S02lblData.LOT_NO)){
                for(i in 0 until S02DATA.size){
                    val temp = S02SL(S02DATA[i].VBELN,S02DATA[i].MATNR,S02DATA[i].PALLT_GRP,S02DATA[i].EXDIV,
                        S02DATA[i].BRGEW,S02DATA[i].NTGEW,S02DATA[i].VOLUM,S02DATA[i].LAENG,S02DATA[i].BREIT,S02DATA[i].HOEHE,
                        S02DATA[i].TAVOL,S02DATA[i].NUM_CAR,S02DATA[i].QUANCAR)
                    S02SUBMITLIST.add(temp)
                    buildSubmitTable(S02DATA[i].MATNR,S02DATA[i].LOT_NO,S02DATA[i].EXDIV,S02DATA[i].BRGEW,S02DATA[i].NTGEW,
                        S02DATA[i].VOLUM,S02DATA[i].LAENG,S02DATA[i].BREIT,S02DATA[i].HOEHE)
                }

                g_currentGroupID++
                partTV.text = "Part Number"
                GIDTV.text = "Pallet Group \n ${generatePalletID(g_currentGroupID)}"
                noOfcartonTV.text = "Number of Carton"
                palletIDTV.text = "Pallet ID"
                S02DATA.clear()
                grossWeightED.text.clear()
                lengthED.text.clear()
                widthED.text.clear()
                heightED.text.clear()

            }
            else{
                cf.showMessage(c,"Error","Quantity in pick list is more than scanned quantity \n Material : ${S02lblData.MATERIAL} Lot Number : ${S02lblData.LOT_NO}",
                    "Dismiss",positiveButtonAction = {
                        // Handle positive button click

                    }).show()
            }
        }

        saveBtn.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                val submitResult = submitS02palletData(S02SUBMITLIST)
                cf.showDialog(
                    context = c,
                    title = "Message",
                    message = submitResult,
                    positiveButtonText = "Yes",
                    negativeButtonText = "No",
                    positiveButtonAction = {
                        // Handle positive button click
                        clearForm()
                    },
                    negativeButtonAction = {
                        // Handle negclearForms()ative button click

                    }
                ).show()
            }
        }

    }

    private fun clearForm(){
        tvDo_num.text.clear()
        regeneratePLTable()
        regeneratesubmitTable()
        S02SUBMITLIST.clear()
        S02lblData = palletLabel("","","","","","")
        S02DATA.clear()
        g_currentGroupID = 1
        GIDTV.text = "Pallet Group \n ${generatePalletID(g_currentGroupID)}"
    }

    private suspend fun submitS02palletData(inputList:MutableList<S02SL>):String{
        val jsonBuilder = StringBuilder()
        jsonBuilder.append("[")
        for(i in 0 until inputList.size){
            jsonBuilder.append("{")
            jsonBuilder.append("\"vbeln\": \"${inputList[i].VBELN}\",")
            jsonBuilder.append("\"posnr\": \"\",")
            jsonBuilder.append("\"matnr\": \"${inputList[i].MATNR}\",")
            jsonBuilder.append("\"palleT_GRP\": \"${inputList[i].PALLT_GRP}\",")
            jsonBuilder.append("\"exidv\": \"${inputList[i].EXDIV}\",")
            jsonBuilder.append("\"brgew\": \"${inputList[i].BRGEW}\",")
            jsonBuilder.append("\"ntgew\": \"${inputList[i].NTGEW}\",")
            jsonBuilder.append("\"volum\": \"${inputList[i].VOLUM}\",")
            jsonBuilder.append("\"laeng\": \"${inputList[i].LAENG}\",")
            jsonBuilder.append("\"breit\": \"${inputList[i].BREIT}\",")
            jsonBuilder.append("\"hoehe\": \"${inputList[i].HOEHE}\",")
            jsonBuilder.append("\"tavol\": \"${inputList[i].TAVOL}\",")
            jsonBuilder.append("\"nuM_CAR\": \"${inputList[i].NUM_CAR}\",")
            jsonBuilder.append("\"quaN_PER_CAR\": \"${inputList[i].QUANCAR}\"")
            jsonBuilder.append("}")
            if(i < inputList.size - 1){
                jsonBuilder.append(",")
            }
        }
        jsonBuilder.append("  ]")
        val jsonPayload = jsonBuilder.toString()
        return withContext(Dispatchers.IO){
            withContext(Dispatchers.Main){
                setProgressBar(pb,true)
            }
            try {
                val urlLink = URL("http://172.16.206.19/BARCODEWEBAPI/API/S02_PALLET")
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
                    stringBuilder.toString()
                }

            }
            catch(e:Exception){
                e.message.toString()
            }
            finally {
                withContext(Dispatchers.Main){
                    setProgressBar(pb,false)
                }
            }
        }

    }

    private fun checkpliflessQuantity(mat:String,lotNo:String):Boolean{
        var result = false
        var totScQty = 0
        var totPLqty = 0
        for(i in 0 until S02DATA.size){
            if(mat == S02DATA[i].MATNR && lotNo == S02DATA[i].LOT_NO){
                totScQty += S02DATA[i].TAVOL.toDouble().toInt()
            }
        }

        for (i in 0 until pickListTable.size){
            var currentRow = findViewById<TableRow>(pickListTable.getChildAt(i).id)

            val PNTV = findViewById<TextView>(currentRow.getChildAt(0).id)
            val PNStr = PNTV.text.toString()
            if(PNStr == mat){
                val qtyTV = findViewById<TextView>(currentRow.getChildAt(1).id)
                val qtyPL = qtyTV.text.toString().toDouble().toInt()
                totPLqty += qtyPL
            }
        }

        if(totPLqty < totScQty){
            result = true
        }

        return result
    }

    private fun checktotalQuantity(mat:String,lotNo:String,lblInQty:Int):Boolean{
        var result = false
        var totScQty = 0
        var totPLqty = 0
        for(i in 0 until S02DATA.size){
            if(mat == S02DATA[i].MATNR && lotNo == S02DATA[i].LOT_NO){
                totScQty += S02DATA[i].TAVOL.toDouble().toInt()
            }
        }
        totScQty += lblInQty

        for (i in 0 until pickListTable.size){
            var currentRow = findViewById<TableRow>(pickListTable.getChildAt(i).id)

            val PNTV = findViewById<TextView>(currentRow.getChildAt(0).id)
            val PNStr = PNTV.text.toString()
            if(PNStr == mat){
                val qtyTV = findViewById<TextView>(currentRow.getChildAt(1).id)
                val qtyPL = qtyTV.text.toString().toDouble().toInt()
                totPLqty += qtyPL
            }
        }

        if(totPLqty >= totScQty){
            result = true
        }

        return result
    }

    private fun checkDuplicatePallet(palletID:String):Boolean{
        var result = false
        for(i in 0 until S02DATA.size){
            val palletIDRow = S02DATA[i].EXDIV
            if(palletID == palletIDRow){
                result = true
                break
            }
        }
        return result
    }

    private fun checkPLLotPN(lot_no:String,part_no:String):Boolean{
        var result = false
        if(pickListTable.size > 1){

            for (i in 0 until pickListTable.size){
                //check PN
                var currentRow = findViewById<TableRow>(pickListTable.getChildAt(i).id)
                val PNTV = findViewById<TextView>(currentRow.getChildAt(0).id)

                if(PNTV.text.toString() == part_no){
                    //check lot
                    val lotTV = findViewById<TextView>(currentRow.getChildAt(2).id)
                    if(lotTV.text.toString() == lot_no){
                        result = true
                        break
                    }
                }

            }

        }

        return result
    }

    private fun regeneratesubmitTable(){
        scannedTable.removeAllViews()
        val headerRow = cf.generateRow(c)

        headerRow.addView(cf.generateTVforRow("Part Number",c))
        headerRow.addView(cf.generateTVforRow("Lot No",c))
        headerRow.addView(cf.generateTVforRow("Status",c))
    }

    private fun regeneratePLTable(){
        pickListTable.removeAllViews()
        val materialTV = cf.generateTVforRow("Part No",c)
        val qtyTV = cf.generateTVforRow("Quantity",c)
        val LotNumTV = cf.generateTVforRow("Batch No",c)
        val headerRow = cf.generateRow(c)

        headerRow.addView(materialTV)
        headerRow.addView(qtyTV)
        headerRow.addView(LotNumTV)
        pickListTable.addView(headerRow)
    }



    private suspend fun retrieveDOdata(doNo:String){
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
                        buildPLtable(response.toString())
                    })


                } else {

                    runOnUiThread(Runnable{
                        cf.showMessage(c,"Error","Message : Error code ${responseCode}","Dismiss",
                            positiveButtonAction = { tvDo_num.text.clear() }).show()
                    })
                }
            }
            catch (ex:Exception){
                runOnUiThread(Runnable{
                    cf.showMessage(c,"Error","Message : ${ex.message.toString()}","Dismiss",
                        positiveButtonAction = { tvDo_num.text.clear() }).show()
                })

            }
            finally {
                withContext(Dispatchers.Main){
                    setProgressBar(pb,false)
                }
            }


        }
    }

    private fun buildSubmitTable(partNo:String,lotNo:String,status:String,
                                 gross_weight:String,net_weight:String,
                                 dim_meas:String,length:String,width:String,height:String,){
        //Generate rows for your table later
        val row = cf.generateRow(c)
        val matNoTV = cf.generateTVforRow(partNo,c)
        val batchNumTV = cf.generateTVforRow(lotNo,c)
        val statusTV = cf.generateTVforRow(status,c)
        val grossWeightTV = cf.generateTVforRow(gross_weight,c)
        val netWeightTV = cf.generateTVforRow(net_weight,c)
        val dimMeasTV = cf.generateTVforRow(dim_meas,c)
        val lengthTV = cf.generateTVforRow(length,c)
        val widthTV = cf.generateTVforRow(width,c)
        val heightTV = cf.generateTVforRow(height,c)
        row.addView(matNoTV)
        row.addView(batchNumTV)
        row.addView(statusTV)
        row.addView(grossWeightTV)
        row.addView(netWeightTV)
        row.addView(dimMeasTV)
        row.addView(lengthTV)
        row.addView(widthTV)
        row.addView(heightTV)
        scannedTable.addView(row)
    }

    private fun buildPLtable(jsonInput:String){
        if(pickListTable.size <= 1){
            val jsonArr = JSONArray(jsonInput)
            for (i in 0 until jsonArr.length()){

                //Generate rows for your table later
                val row = cf.generateRow(c)
                val matNoTV = cf.generateTVforRow(jsonArr.getJSONObject(i).getString("materiaL_NO"),c)
                val batchNumTV = cf.generateTVforRow(jsonArr.getJSONObject(i).getString("batcH_NO"),c)
                val pickQtyTV = cf.generateTVforRow(jsonArr.getJSONObject(i).getString("picK_QTY"),c)
                row.addView(matNoTV)
                row.addView(pickQtyTV)
                row.addView(batchNumTV)
                pickListTable.addView(row)
            }
        }
    }

    private fun generatePalletID(curID:Int):String{
        return if(curID < 10) "0000$curID" else "000$curID"
    }

    private suspend fun translateText(MLcode:String):palletLabel{
        val result = palletLabel("","",
            "","","","")
        S02lblData = result
        withContext(Dispatchers.IO){
            val url = URL("http://172.16.206.19/REST_API/Home/breakpalletbarcodeString?barcode=$MLcode")

            try{
                val resp =  url.readText()  // Fetch the response as a String
                val responseOBJ = JSONObject(resp)
                setProgressBar(pb,false)
                result.MATERIAL = responseOBJ.getString("MATERIAL")
                result.LOT_NO = responseOBJ.getString("LOT_NO")
                result.PALLET_ID = responseOBJ.getString("PALLET_ID")
                result.CARTON_QTY = responseOBJ.getString("CARTON_QTY")
                result.PALLET_SEQUANCE = responseOBJ.getString("PALLET_SEQUENCE")
                result.QUANTITY = responseOBJ.getString("QUANTITY")

            }
            catch(e:Exception){
                runOnUiThread(kotlinx.coroutines.Runnable {
                    cf.showMessage(c,"Error","Message : ${e.message.toString()}","Dismiss",
                        positiveButtonAction = { mainLblBcode.text.clear() }).show()
                })
            }
            finally {

            }


        }
        return result
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