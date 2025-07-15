package com.example.myapplication

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.activity.enableEdgeToEdge
import androidx.core.view.size
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Text
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class S01 : AppCompatActivity() {
    private lateinit var mainTable:TableLayout
    private lateinit var _context: Context
    private lateinit var pb:ProgressBar
    private lateinit var in_PLBcode:EditText
    private lateinit var pickList:MutableList<PICKLIST_ITEM>
    private lateinit var in_pallet_barcode:EditText
    private lateinit var btnSave:Button
    private lateinit var btnClear:Button
    private lateinit var matQty:TextView
    private lateinit var qtySum:TextView
    private var matLineCount:Int = 0
    private var matQtySum:Int = 0
    private var matLineTot:Int = 0
    private var sumQtyTot:Int = 0
    private lateinit var lotNumTemp:String
    private lateinit var palletList:MutableList<String>


    private fun clearAll(){
        mainTable.removeAllViews()
        generateHeaderRow()
        palletList.clear()
        pickList.clear()
        matLineCount = 0
        matQtySum = 0
        matLineTot = 0
        sumQtyTot = 0
        matQty.text = "0/0"
        qtySum.text = "0/0"
        in_PLBcode.text.clear()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_s01)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        lotNumTemp = String()
        mainTable = findViewById(R.id.S01Table)
        _context = this@S01
        in_PLBcode = findViewById(R.id.S01EDPLBcode)
        pb = findViewById(R.id.S01PB)
        in_pallet_barcode = findViewById(R.id.S01EDMatBcode)
        btnSave = findViewById(R.id.S01BtnSave)
        btnClear = findViewById<Button>(R.id.S01BtnClear)
        matQty = findViewById(R.id.S01TVMatQty)
        qtySum = findViewById(R.id.S01TVQtyQty)
        generateHeaderRow()
        palletList = mutableListOf()

        btnClear.setOnClickListener {
            clearAll()
        }

        in_PLBcode.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                if (in_PLBcode.text.isNullOrBlank()){
                    showDialogMessage("Scan Picklist Barcode","Error")
                }
                else{
                    CoroutineScope(Dispatchers.Main).launch {
                        pickList = mutableListOf()
                        try{
                            val response = RetrievePLData(in_PLBcode.text.toString())
                            val jObj = JSONArray(response)
                            var matLineCount = 0
                            var matQtySum = 0
                            for(i in 0 until jObj.length()){
                                matLineCount++
                                val jsonObject = jObj.getJSONObject(i)
                                val MATERIAL = jsonObject.getString("materiaL_NO")
                                val DESCRIPTION = jsonObject.getString("materiaL_DESC")
                                val BATCH = jsonObject.getString("batcH_NO")
                                val QUANTITY = jsonObject.getString("picK_QTY")
                                matQtySum += QUANTITY.toDouble().toInt()
                                var lineTemp = PICKLIST_ITEM(MATERIAL,DESCRIPTION,BATCH,QUANTITY)
                                pickList.add(lineTemp)
                                generateRowFromLine(i.toString(),MATERIAL,QUANTITY,"0",BATCH)

                            }
                            matLineTot = matLineCount
                            sumQtyTot = matQtySum
                            matQty.text = "0/${matLineCount.toString()}"
                            qtySum.text = "0/${matQtySum.toString()}"
                        }
                        catch(ex:Exception){
                            showDialogMessage(ex.message.toString(),"Error")
                        }
                        finally{
                            in_pallet_barcode.requestFocus()
                        }


                    }

                }


                return@OnKeyListener true
            }
            false
        })

        in_pallet_barcode.setOnKeyListener(View.OnKeyListener{ _,keyCode,event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                if(in_pallet_barcode.text.isNullOrBlank()){
                    showDialogMessage("Pallet Barcode","Error")
                }
                else{
                    executebreakupSCANNEDTEXT(in_pallet_barcode.text.toString(),pb)
                }


                return@OnKeyListener true
            }
            false
        })

        btnSave.setOnClickListener {
            var submitList = mutableListOf<PICKLIST_ITEM>()
            if(mainTable.size > 0){
                val jsonBuilder = StringBuilder()
                // Start the JSON object
                jsonBuilder.append("{\n")
                jsonBuilder.append("  \"saP_DATA\": [\n")

                for (i in 1 until mainTable.size){

                    val tableRow = mainTable.getChildAt(i) as TableRow
                    val partNumRowTV = tableRow.getChildAt(1) as TextView
                    val lotNumRowTV = tableRow.getChildAt(4) as TextView
                    val pickQtyTV = tableRow.getChildAt(3) as TextView
                    jsonBuilder.append("{\n")
                    jsonBuilder.append("\"plant\": \"1020\",\n")
                    jsonBuilder.append("\"s_STGE_LOC\": \"2008\",\n")
                    jsonBuilder.append("\"d_STGE_LOC\": \"2008\",\n")
                    jsonBuilder.append("\"warehousE_NO\": \"102\",\n")
                    jsonBuilder.append("\"batch\": \"${lotNumRowTV.text.toString()}\",\n")
                    jsonBuilder.append("\"material\": \"${partNumRowTV.text.toString()}\",\n")
                    jsonBuilder.append("\"s_STGE_TYPE\": \"008\",\n")
                    jsonBuilder.append("\"s_STGE_BIN\": \"\",\n")
                    jsonBuilder.append("\"d_STGE_TYPE\": \"916\",\n")
                    jsonBuilder.append("\"d_STGE_BIN\": \"${in_PLBcode.text.toString()}\",\n")
                    jsonBuilder.append("\"m_STOCK_TYPE\": \"\",\n")
                    jsonBuilder.append("\"m_SPEC_STOCK\": \"\",\n")
                    jsonBuilder.append("\"t_STOCK_CAT\": \"\",\n")
                    jsonBuilder.append("\"t_SPEC_STOCK\": \"\",\n")
                    jsonBuilder.append("\"entrY_QNT\": \"${pickQtyTV.text.toString().format("%.3")}\",\n")
                    jsonBuilder.append("\"entrY_UOM\": \"PC\",\n")
                    jsonBuilder.append("\"delivery\": \"${in_PLBcode.text.toString()}\"\n")
                    jsonBuilder.append("}")
                    if(i < mainTable.size - 1){
                        jsonBuilder.append(",")
                    }
                    jsonBuilder.append("\n")
                }
                // Close the saP_DATA array and add the badge
                jsonBuilder.append("  ],\n")
                jsonBuilder.append("  \"badge\": \"B0060\"\n")
                jsonBuilder.append("}")
                val jsonBody = jsonBuilder.toString()

                // Create the OkHttp client and request
                val client = OkHttpClient()
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonBody.toRequestBody(mediaType)
                val request = Request.Builder()
                    .url("http://172.16.206.19/BARCODEWEBAPI/api/S01_PICKLIST")
                    .post(requestBody)
                    .build()
                CoroutineScope(Dispatchers.IO).launch {
                    //executeRESTAPI(request,client)
                    withContext(Dispatchers.Main){
                        setProgressBar(pb,true)
                    }
                    try{
                        val urlLink = URL("http://172.16.206.19/BARCODEWEBAPI/api/S01_PICKLIST")
                        val conn = urlLink.openConnection()
                        conn.doOutput = true
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.setRequestProperty("Content-Length", jsonBody.length.toString())

                        DataOutputStream(conn.getOutputStream()).use { it.writeBytes(jsonBody) }
                        BufferedReader(InputStreamReader(conn.getInputStream())).use { bf ->
                            val stringBuilder = StringBuilder()
                            var line:String?

                            while (bf.readLine().also { line = it } != null) {
                                line?.let {
                                    stringBuilder.append(it)
                                }
                            }
                            val response = stringBuilder.toString()
                            withContext(Dispatchers.Main){
                                runOnUiThread(Runnable{
                                    val builder = AlertDialog.Builder(_context)
                                    builder.setTitle("Message")
                                    builder.setMessage(response)
                                    builder.setPositiveButton("OK") { dialog, which ->
                                        // Do something when OK button is clicked
                                        clearAll()

                                    }
                                    builder.show()
                                })
                            }
                        }

                    }
                    catch(ex:Exception){

                    }
                    finally{
                        withContext(Dispatchers.Main){
                            setProgressBar(pb,false)
                        }
                    }
                }

            }
            else{
                showDialogMessage("Scan Picklist Barcode","Error")
            }

        }


    }

    private suspend fun executeRESTAPI (req: Request,cl: OkHttpClient){
            try{
                withContext(Dispatchers.IO) {

                    withContext(Dispatchers.Main){
                        showProgressBar(true)
                    }
                    val response: Response = cl.newCall(req).execute()
                    if (response.isSuccessful) {
                        withContext(Dispatchers.Main){
                            runOnUiThread(Runnable{
                                val builder = AlertDialog.Builder(_context)
                                builder.setTitle("Message")
                                builder.setMessage(response.body?.string())
                                builder.setPositiveButton("OK") { dialog, which ->
                                    // Do something when OK button is clicked
                                    clearAll()

                                }
                                builder.show()
                            })
                        }

                    } else {
                        var ERmsg = "Request failed with code: ${response.code} \n ${response.body?.string()}"
                        withContext(Dispatchers.Main){
                            runOnUiThread(Runnable{
                                val builder = AlertDialog.Builder(_context)
                                builder.setTitle("Error")
                                builder.setMessage(ERmsg)
                                builder.setPositiveButton("OK") { dialog, which ->
                                    // Do something when OK button is clicked
                                    in_PLBcode.text.clear()
                                    mainTable.removeAllViews()
                                    generateHeaderRow()
                                }
                                builder.show()
                            })
                        }


                        /*
                        println("Request failed with code: ${response.code}")
                        println("Response: ${response.body?.string()}")

                         */
                    }

                }




            } catch (ex: Exception) {
                runOnUiThread(Runnable{
                    showDialogMessage(ex.message.toString(),"Error")
                })

            }
        finally{
            withContext(Dispatchers.Main){
                showProgressBar(false)
            }
        }



    }

    private fun breakupSCANNEDTEXT(bc:String): String? {
        if (bc.isNullOrEmpty()){
            throw IllegalArgumentException("All parameters must be provided and cannot be null or empty")
        }

        val url = URL("http://172.16.206.19/FORD_SYNC/API/CARTONTRANSLATE?cartonBcode=$bc")

        return try {
            url.readText()  // Fetch the response as a String
        } catch (e: IOException) {
            e.message.toString()
            null
        }

    }

    private fun updateTable(matNum:String,LotNum:String,quantity:String){
        runOnUiThread(Runnable{
            if(mainTable.size > 0){
                for (i in 1 until mainTable.size){
                    val tableRow = mainTable.getChildAt(i) as TableRow
                    val partNumRowTV = tableRow.getChildAt(1) as TextView
                    val lotNumRowTV = tableRow.getChildAt(4) as TextView
                    val qtyRowTV = tableRow.getChildAt(3) as TextView
                    val pickQtyTV = tableRow.getChildAt(2) as TextView
                    val pickQty = pickQtyTV.text.toString().toDouble().toInt()
                    val curQty = qtyRowTV.text.toString().toInt()
                    val addQty = quantity.toInt()
                    val totQty = curQty + addQty

                    if(matNum == partNumRowTV.text && LotNum == lotNumRowTV.text) {
                        if(totQty > pickQty ){
                            showDialogMessage("Over Quantity","Warning !")
                        }
                        else{
                            qtyRowTV.text = totQty.toString()
                        }

                    }
                }
            }
            in_pallet_barcode.text.clear()
        })
    }

    private fun showDialogMessage(message:String,title:String){
        val builder = AlertDialog.Builder(_context)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("OK") { dialog, which ->
            // Do something when OK button is clicked

        }
        builder.show()
    }
    private fun checkLotPNAlreadyScanned(partNum:String,lotNo:String):Boolean{
        var result = false
        var totQtyBatch = 0
        for(i in 1 until mainTable.size) {
            val scannedTabRow = mainTable.getChildAt(i) as TableRow
            val partNumRowTV = scannedTabRow.getChildAt(1) as TextView
            val lotNumRowTV = scannedTabRow.getChildAt(4) as TextView
            if(partNumRowTV.text.toString() == partNum){
                if(lotNumRowTV.text.toString() == lotNo){
                    val qtyTemp = scannedTabRow.getChildAt(3) as TextView
                    totQtyBatch += qtyTemp.text.toString().toDouble().toInt()
                }

            }
        }

        return totQtyBatch > 0
    }
    private fun setProgressBar(v: View, show: Boolean) {
        if (show) {
            v.visibility = View.VISIBLE
        } else {
            v.visibility = View.GONE
        }
    }

    private fun executebreakupSCANNEDTEXT(bc:String,progressBar: View){
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Show the progress bar and disable the button
                setProgressBar(progressBar, true)

                // Perform the network request in the IO dispatcher
                val result = withContext(Dispatchers.IO) {
                    breakupSCANNEDTEXT(bc)
                }
                // Hide the progress bar and enable the button
                setProgressBar(progressBar, false)

                // Handle the result
                result?.let { responseString ->
                    // Process the JSONObject, e.g., update the UI with the order details
                    // Example: val orderId = jsonObject.getString("orderId")
                    try {
                        val responseOBJ = JSONObject(responseString)
                        val material = responseOBJ.getString("material")
                        val quantity = responseOBJ.getString("quantity")
                        val lotNumber = responseOBJ.getString("loT_NO")
                        if(checkPalletID(responseOBJ.getString("palleT_ID"))){
                            Toast.makeText(progressBar.context, "Duplicate Pallet ID ${responseOBJ.getString("PALLET_ID")}", Toast.LENGTH_SHORT).show()
                            in_pallet_barcode.text.clear()
                        }
                        else{
                            matQtySum += quantity.toDouble().toInt()

                            if(checkPNAndLot(material,lotNumber)){
                                if(lotNumber != lotNumTemp){
                                    if(!checkLotPNAlreadyScanned(material,lotNumber)){
                                        matLineCount++
                                        matQty.text = "${matLineCount}/${matLineTot.toString()}"
                                    }

                                }
                                qtySum.text = "${matQtySum}/${sumQtyTot.toString()}"
                                lotNumTemp = lotNumber
                                updateTable(material,lotNumber,quantity)
                                palletList.add(responseOBJ.getString("palleT_ID"))
                            }
                            else{
                                showDialogMessage("wrong PN / Lot","Warning !")
                            }


                        }
                    }
                    catch (ex: Exception){
                        showDialogMessage(ex.message.toString(),"error")
                    }



                } ?: run {
                    // Handle the error, e.g., show an error message
                    Toast.makeText(progressBar.context, "Failed to submit data", Toast.LENGTH_SHORT).show()
                }

            }
            catch (e: IllegalArgumentException) {
                // Hide the progress bar
                setProgressBar(progressBar, false)

                // Show a toast message with the error
                Toast.makeText(progressBar.context, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPNAndLot(mat:String,lot:String):Boolean{
        var result = false
        for (i in 1 until mainTable.size){
            val scannedTabRow = mainTable.getChildAt(i) as TableRow
            val partNumRowTV = scannedTabRow.getChildAt(1) as TextView
            val lotNumRowTV = scannedTabRow.getChildAt(4) as TextView
            if(partNumRowTV.text.toString() == mat){
                if(lotNumRowTV.text.toString() == lot){
                    result = true
                    break
                }
            }
        }
        return result
    }

    private fun checkPalletID(pltID:String):Boolean{
        var result = false
        for(i in 0 until palletList.size){
            if(pltID == palletList[i]){
                result = true
                break
            }
            else{
                result = false
            }
        }
        return result
    }

    private fun generateRowFromLine(num:String,MAT:String,PickQty:String,PickedQty:String,lotNo:String){
        //set Parameters for rows
        val rowParams: TableRow.LayoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.MATCH_PARENT
        )
        with(rowParams){
            weight = 1F
        }
        val edForRow = generateEditTextForRow("Location")
        val edContainer = generateLinearContainerForEditText(rowParams,edForRow)
        var rowLineItem = TableRow(_context)
        rowLineItem.layoutParams = TableRow.LayoutParams(rowParams)
        rowLineItem.id = View.generateViewId()
        rowLineItem.addView(generateTVforRow(num))
        rowLineItem.addView(generateTVforRow(MAT))
        rowLineItem.addView(generateTVforRow(PickQty))
        rowLineItem.addView(generateTVforRow(PickedQty))
        rowLineItem.addView(generateTVforRow(lotNo))
        mainTable.addView(rowLineItem)
    }

    private fun generateHeaderRow(){
        //set Parameters for rows
        val rowParams: TableRow.LayoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.MATCH_PARENT
        )
        with(rowParams){
            weight = 1F
        }

        val headerRow = TableRow(_context)
        headerRow.layoutParams = TableRow.LayoutParams(rowParams)
        headerRow.id = View.generateViewId()
        //call the generate text view method we created below and add the view to your header row
        headerRow.addView(generateTVforRow("No"))
        headerRow.addView(generateTVforRow("Part Number"))
        headerRow.addView(generateTVforRow("Pick"))
        headerRow.addView(generateTVforRow("Picked"))
        headerRow.addView(generateTVforRow("Lot No"))
        mainTable.addView(headerRow)
    }

    //Create a function that returns the View of a textview for each of the rows
    private fun generateTVforRow(Displaytext:String):View{
        val generateTV = TextView(_context)
        //Set whatever view attributed here
        with(generateTV) {
            //generate an ID in case you want to retrieve the texts later in main method
            id = ViewCompat.generateViewId()
            //generate the text based on the parameters you've provided
            text = Displaytext
            //set the font size
            textSize = 16F
            //set the text color
            setTextColor(Color.BLACK)
            //set the alignment of text
            gravity = Gravity.CENTER
            //inner padding for your textview
            setPadding(10, 10, 10, 10)
            //set layout parameters for your textview so far everything is made to fit the screen
            layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.MATCH_PARENT
            )
            //set background border for textview table the table visible
            //cell with border xml resource file you can copy in the res -> drawable in the project folder
            setBackgroundResource(R.drawable.cell_with_border)
        }
        return generateTV
    }



    //generate text input for each of the rows
    private fun generateEditTextForRow(l_hint:String):EditText{
        val edRow = EditText(_context)
        with(edRow) {
            id = View.generateViewId()
            inputType = InputType.TYPE_CLASS_TEXT
            hint = l_hint
            layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.MATCH_PARENT
            )
        }
        return edRow

    }
    //generate linearlayout for edit texts
    private fun generateLinearContainerForEditText(rowParams:TableRow.LayoutParams, edTx:EditText): LinearLayout {
        val containerLayout = LinearLayout(_context)
        with(containerLayout) {
            layoutParams = rowParams
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.cell_with_border)
            setPadding(15, 15, 15, 15)
        }
        containerLayout.addView(edTx)
        return containerLayout
    }


        private suspend fun RetrievePLData(doNum:String):String{
        return withContext(Dispatchers.IO){
            // Show progress bar
            withContext(Dispatchers.Main) {
                showProgressBar(true)
            }
            val urlString = "http://172.16.206.19/BARCODEWEBAPI/API/S01_PICKLIST?do_num=$doNum"
            val urlLink = URL(urlString)
            val connection = urlLink.openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    response
                } else {
                    "Error: ${connection.responseCode}"
                }
            } finally {
                // Hide progress bar
                withContext(Dispatchers.Main) {
                    showProgressBar(false)
                }
                connection.disconnect()
            }
        }



    }
    private fun showProgressBar(isVisible: Boolean) {
        runOnUiThread(Runnable{
            if (isVisible) {
                pb.visibility = View.VISIBLE
            } else {
                pb.visibility = View.GONE
            }
        })

    }

    private fun executeRetrievePLData(){

    }
data class PICKLIST_ITEM(var MATERIAL:String, var MATERIAL_DESC:String, var BATCH_NO:String,var PICK_QTY:String)
}