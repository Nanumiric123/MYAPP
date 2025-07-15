package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.webkit.WebView.FindListener
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL

class C11 : AppCompatActivity() {
    private lateinit var title:TextView
    private lateinit var storageBin:EditText
    private lateinit var pb:ProgressBar
    private lateinit var rb002:RadioButton
    private lateinit var rbA0:RadioButton
    private lateinit var rbH0:RadioButton
    private lateinit var mainLayout:LinearLayout
    private lateinit var CF:commonFunctions
    private lateinit var c: Context
    private lateinit var badgeNumber:String
    private lateinit var itemsList:MutableList<Item_DataC11>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_c11)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        title = findViewById(R.id.C11title)
        storageBin = findViewById(R.id.C11EDSTORBIN)
        title.text = intent.getStringExtra("Desc").toString()
        badgeNumber = intent.getStringExtra("Badge").toString()
        pb = findViewById(R.id.c11PB)
        rb002 = findViewById(R.id.C11RB002)
        rbA0 = findViewById(R.id.C11RBA0)
        rbH0 = findViewById(R.id.C11RBH0)
        CF = commonFunctions()
        c = this
        mainLayout = findViewById(R.id.C11mainLayout)
        itemsList = mutableListOf<Item_DataC11>()
        storageBin.requestFocus()
        storageBin.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
                || keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN){

                CoroutineScope(Dispatchers.IO).launch {
                    progressbarSetting(pb)
                    if (storageBin.text.toString() != ""){
                        if(rb002.isChecked){
                            itemsList = parseJSON(getData(storageBin.text.toString(),"002")).items
                        }
                        else if (rbA0.isChecked){
                            itemsList = parseJSON(getData(storageBin.text.toString(),"A0")).items
                        }
                        else if (rbH0.isChecked){
                            itemsList = parseJSON(getData(storageBin.text.toString(),"H0")).items
                        }
                        else{
                            runOnUiThread(Runnable {
                                Toast.makeText(this@C11, "Select Storage Type", Toast.LENGTH_SHORT).show()
                            })

                        }
                        runOnUiThread(Runnable{
                            if(itemsList.size > 0){
                                if (mainLayout.childCount <= 0){
                                    mainLayout.addView(BuildTable(itemsList))
                                }
                                else{
                                    mainLayout.removeAllViews()
                                    mainLayout.addView(BuildTable(itemsList))
                                }
                            }
                            else{
                                runOnUiThread(Runnable {
                                    Toast.makeText(this@C11, "No Data in Storage Rack", Toast.LENGTH_SHORT).show()
                                })
                            }
                        })



                    }
                    else{
                        TriggerAlert(c,"SCAN RACK DONT TYPE","Error")
                    }
                    progressbarSetting(pb)
                }




                return@OnKeyListener true
            }
            false
        })


    }


    private fun parseJSON(input:String):JsonResponseC11{
        val jObj = JSONObject(input)

        val msg = jObj.getString("msg")
        val itemsArr = jObj.getJSONArray("items")

        // Parse items from the itemsArray
        val itemsList = mutableListOf<Item_DataC11>()
        for (i in 0 until itemsArr.length()) {
            val itemObject = itemsArr.getJSONObject(i)
            val item = Item_DataC11(
                material = itemObject.getString("material"),
                quantity = itemObject.getString("quantity"),
                countedQuantity = itemObject.getString("counteD_QUANTITY"),
                uom = itemObject.getString("uom"),
                storagebin = itemObject.getString("storagebin"),
                batch = itemObject.getString("batch")
            )

            itemsList.add(item)
        }

        return JsonResponseC11(msg, itemsList)
    }

    private suspend fun getData(strBin:String,storLoc:String):String {
        var result:String = String()
        withContext(Dispatchers.IO){
            val url = URL("http://172.16.206.19/BARCODEWEBAPI/API/BARCODEController2")
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val payLoad = "{\n" +
                        "  \"storagetyp\": \"${storLoc}\",\n" +
                        "  \"storageloc\": \"2002\",\n" +
                        "  \"storagebin\": \"${strBin}\"\n" +
                        "}".trimIndent()

                val outputStream = OutputStreamWriter(connection.outputStream)
                outputStream.write(payLoad)
                outputStream.flush()
                val responseCode = connection.responseCode
                val inputStream = connection.inputStream
                val response = inputStream.bufferedReader().use { it.readText() }
                result = response
            }
            catch (e: Exception) {
                e.printStackTrace()
            } finally {
                connection.disconnect()
            }

        }

        return result

    }

    private fun BuildTable(data:MutableList<Item_DataC11>) : View {
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

    private fun generateRowView(currentRow:Item_DataC11):View{
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
        val countedView = CF.generateTVforRow(currentRow.countedQuantity,c)

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
                CoroutineScope(Dispatchers.Main).launch{
                    transString = translateBcode(edVerify.text.toString())
                    if(transString.isNullOrBlank()){
                        TriggerAlert(c,"Wrong Barcode","Fail")
                        edVerify.text.clear()
                    }
                    else{
                        val phyQty:TextView = findViewById<TextView>(countedView.id)
                        var barcodeTranslated = translateAPIResult(transString)
                        if(barcodeTranslated.pART_NO == currentRow.material){
                            insertDataCycleCount(currentRow,barcodeTranslated.qUANTITY,barcodeTranslated.rEEL_NO)
                            runOnUiThread(kotlinx.coroutines.Runnable{
                                phyQty.text = barcodeTranslated.qUANTITY.toString()
                                edVerify.text.clear()
                            })
                        }
                        else{
                            TriggerAlert(c,"Wrong Part Number","Fail")
                        }

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

    private suspend fun insertDataCycleCount(input:Item_DataC11,phyQty:String,cartonID:String){
        var result:String = String()
        withContext(Dispatchers.IO){
            val url = URL("http://172.16.206.19/BARCODEWEBAPI/api/CYCLE_COUNT_INSERT")
            val connection = url.openConnection() as HttpURLConnection
            var sysQty = BigDecimal(input.quantity).toInt()
            var payLoad = "{\"material\": \"${input.material}\",\n" +
                    "  \"storagE_BIN\": \"${input.storagebin}\",\n" +
                    "  \"physicaL_QUANTITY\": ${phyQty},\n" +
                    "  \"SYSTEM_QUANTITY\": ${sysQty},\n" +
                    "  \"lot\": \"${input.batch}\",\n" +
                    "  \"pic\": \"${badgeNumber}\",\n" +
                    "  \"reeL_NUMBER\": \"${cartonID}\"}"
            try {
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
                    result = response
                }
                else{
                    result = "Fail : ${responseCode}"
                    TriggerAlert(c,result,"Fail")
                }

            }
            catch (e:Exception){
                result = "Fail${e.message.toString()}"
                TriggerAlert(c,result,"Fail")
            }
            finally {
                runOnUiThread(Runnable {
                    CoroutineScope(Dispatchers.IO).launch {
                        if (storageBin.text.toString() != ""){
                            if(rb002.isChecked){
                                itemsList = parseJSON(getData(storageBin.text.toString(),"002")).items
                            }
                            else if (rbA0.isChecked){
                                itemsList = parseJSON(getData(storageBin.text.toString(),"A0")).items
                            }
                            else if (rbH0.isChecked){
                                itemsList = parseJSON(getData(storageBin.text.toString(),"H0")).items
                            }
                            else{
                                runOnUiThread(Runnable {
                                    Toast.makeText(this@C11, "Select Storage Type", Toast.LENGTH_SHORT).show()
                                })

                            }
                            runOnUiThread(Runnable {
                                if(itemsList.size > 0){
                                    if (mainLayout.childCount <= 0){
                                        mainLayout.addView(BuildTable(itemsList))
                                    }
                                    else{
                                        mainLayout.removeAllViews()
                                        mainLayout.addView(BuildTable(itemsList))
                                    }
                                }
                                else{
                                    runOnUiThread(Runnable {
                                        Toast.makeText(this@C11, "No Data in Storage Rack", Toast.LENGTH_SHORT).show()
                                    })
                                }
                            })

                        }
                        else{
                            TriggerAlert(c,"PLEASE SCAN THE RACK","Error")
                        }
                    }
                })
            }

        }
    }

    private suspend fun translateBcode(input: String):String{
        var APIResult:String = ""
        withContext(Dispatchers.IO){
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

    private fun progressbarSetting(v: View){
        runOnUiThread(kotlinx.coroutines.Runnable {
            if (v.visibility == View.VISIBLE) {
                v.visibility = View.INVISIBLE
            } else {
                v.visibility = View.VISIBLE
            }

        })
    }

    private fun TriggerAlert(c:Context,message:String,title:String){
        runOnUiThread(kotlinx.coroutines.Runnable{
            val builder = AlertDialog.Builder(c)

            builder.setTitle(title)
            builder.setMessage(message)
            builder.setPositiveButton("OK") { dialog, which ->
                // Do something when OK button is clicked
            }
            builder.show()
        })
    }

}

data class Item_DataC11(
    val material: String,
    val quantity: String,
    val countedQuantity: String,
    val uom: String,
    val storagebin: String,
    val batch: String
)

data class JsonResponseC11(
    val msg: String,
    val items: MutableList<Item_DataC11>
)