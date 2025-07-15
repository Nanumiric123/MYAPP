package com.example.myapplication

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.w3c.dom.Document
import java.io.StringReader
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import org.xml.sax.InputSource
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory


class T07TransferMaterial : AppCompatActivity() {
    private lateinit var gbadgeNum:String
    private lateinit var gArea:String
    private lateinit var gLocation:String
    private lateinit var gMaterial:String
    private lateinit var gQty:String
    private lateinit var inLocation:EditText
    private lateinit var  inBarcode:EditText
    private lateinit var  btnTransfer:Button
    private lateinit var  btnClear:Button
    private lateinit var c:Context
    private lateinit var tvT07Material: TextView
    private lateinit var tvT07Location:TextView
    private lateinit var tvT07Quantity:TextView
    private lateinit var pb:ProgressBar
    private lateinit var rackList:MutableList<Item>
    private lateinit var T07Switch:Switch
    private var dataList:MutableList<barcodeData> = mutableListOf()
    private var barcodeTranslated = barcodeData("","","","","","","")
    private lateinit var totalScannedTV:TextView
    private lateinit var machineNo:String
    private lateinit var rqmnTV:TextView
    private lateinit var deviceID:String
    private lateinit var cf:commonFunctions
    private lateinit var mainTL:TableLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_t07_transfer_material)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        pb = findViewById(R.id.T07TFPB)
        cf = commonFunctions()
        tvT07Quantity = findViewById(R.id.T07TFQTYTBT)
        tvT07Location = findViewById(R.id.T07TFLOCTBT)
        tvT07Material = findViewById(R.id.T07TFMATTBT)
        inLocation = findViewById(R.id.T07TFTOLOC)
        inBarcode = findViewById(R.id.T07TFBCODE)
        T07Switch = findViewById(R.id.T07SWSCAN)
        btnTransfer = findViewById(R.id.BTNT07Transfer)
        totalScannedTV = findViewById(R.id.T07SCANTOTAL)
        rqmnTV = findViewById(R.id.T07TVMNRQ)
        mainTL = findViewById<TableLayout>(R.id.T07TL)
        gbadgeNum = intent.getStringExtra("Badge").toString()
        gArea = intent.getStringExtra("Area").toString()
        gQty = intent.getStringExtra("Quantity").toString()
        gLocation = intent.getStringExtra("Location").toString()
        gMaterial = intent.getStringExtra("Material").toString()
        machineNo = intent.getStringExtra("MachineNum").toString()
        //Requestor/Machine Num :
        rqmnTV.text = "Requestor/Machine Num : ${intent.getStringExtra("Requestor").toString()} / \n ${machineNo}"

        tvT07Material.text = "Material : $gMaterial"
        tvT07Location.text = "Location : $gLocation"
        tvT07Quantity.text = "Quantity : $gQty"
        c = this@T07TransferMaterial
        var totalQty = 0
        inLocation.requestFocus()

        hideKeyboard()
        inLocation.setOnClickListener {
            hideKeyboard()
        }
        inBarcode.setOnClickListener { hideKeyboard() }

        inLocation.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
                || keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN){
                if(inLocation.text.isNullOrBlank()){
                    val builder = AlertDialog.Builder(c)
                    builder.setTitle(R.string.Warning)
                    builder.setMessage("TOLONG SCAN DULU LOCATION ")
                    builder.setPositiveButton("OK") { dialog, which ->
                        // Do something when OK button is clicked
                        inBarcode.requestFocus()
                        hideKeyboard()
                    }
                    builder.setNegativeButton("Cancel"){ dialog, which ->
                        // Do something when OK button is clicked
                        inLocation.setText("")
                        inLocation.requestFocus()
                        hideKeyboard()
                    }
                    builder.show()
                }
                else{
                    if(gLocation == inLocation.text.toString()){
                        CoroutineScope(Dispatchers.Main).launch {
                            setProgressBar(pb)
                            rackList = getLocationInfo(inLocation.text.toString(),gMaterial)
                            inBarcode.requestFocus()
                            if(rackList.size < 0){
                                val builder = AlertDialog.Builder(c)
                                builder.setTitle(R.string.Warning)
                                builder.setMessage("JANGAN MANUAL KEY IN. TOLONG SCAN DULU LOCATION ")
                                builder.setPositiveButton("OK") { dialog, which ->
                                    // Do something when OK button is clicked
                                    inBarcode.requestFocus()
                                    hideKeyboard()
                                }
                                builder.setNegativeButton("Cancel"){ dialog, which ->
                                    // Do something when OK button is clicked
                                    inLocation.setText("")
                                    inLocation.requestFocus()
                                    hideKeyboard()
                                }
                                builder.show()
                            }
                            setProgressBar(pb)

                        }
                    }
                    else{
                        val builder = AlertDialog.Builder(c)

                        builder.setTitle(R.string.Warning)
                        builder.setMessage(R.string.LocationWarning)
                        builder.setPositiveButton("OK") { dialog, which ->
                            // Do something when OK button is clicked
                            inLocation.setText("")
                            inLocation.requestFocus()
                            hideKeyboard()
                        }
                        builder.setNegativeButton("Cancel"){ dialog, which ->
                            // Do something when OK button is clicked
                            inLocation.setText("")
                            inLocation.requestFocus()
                            hideKeyboard()
                        }
                        builder.show()
                    }
                }

                return@OnKeyListener true
            }
            false
        })
        T07Switch.setOnCheckedChangeListener { buttonView, isChecked ->
            // do something, the isChecked will be
            // true if the switch is in the On position
            regenerateHeader()
            btnTransfer.isEnabled = T07Switch.isChecked != true
            hideKeyboard()
        }

        inBarcode.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
                || keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN){
                if(inBarcode.text.isNullOrBlank()){
                    cf.showMessage(c,"Error","Error Please Scan Barcode","OK",
                        positiveButtonAction = {
                        inBarcode.requestFocus()
                    }).show()
                }
                else{
                    barcodeTranslated = translateBarcode(inBarcode.text.toString())
                    if(barcodeTranslated.PART_NO.isNullOrBlank() || barcodeTranslated.LOT.isNullOrBlank() ||
                        barcodeTranslated.REEL_NO.isNullOrBlank()){
                        cf.showMessage(c,"Error","You scanned the wrong Barcode","OK",
                            positiveButtonAction = {
                                inBarcode.requestFocus()
                            }).show()
                    }
                    else{
                        inBarcode.text.clear()
                        if(gMaterial == barcodeTranslated.PART_NO){
                            if(gLocation == inLocation.text.toString()){
                                if(T07Switch.isChecked){
                                    //SCAN MULTIPLE
                                    CoroutineScope(Dispatchers.Main).launch {
                                        setProgressBar(pb)
                                        var sapResult = SendToSAP(barcodeTranslated.PART_NO,inLocation.text.toString(),barcodeTranslated.LOT,
                                            gbadgeNum,barcodeTranslated.QUANTITY,barcodeTranslated.REEL_NO)
                                        var sapResultSplit = sapResult.split('$')
                                        if(sapResultSplit[0].contains('F') ||
                                            sapResultSplit[0].contains('E')){
                                            cf.showMessage(c,sapResultSplit[0],sapResultSplit[1],"OK",
                                                positiveButtonAction = {
                                                    inBarcode.requestFocus()
                                                }).show()
                                        }
                                        else{
                                            addRowToTL(c,barcodeTranslated.PART_NO,barcodeTranslated.LOT,
                                                barcodeTranslated.REEL_NO,barcodeTranslated.QUANTITY)
                                            var rowCnt = mainTL.childCount - 1
                                            totalScannedTV.text = getString(R.string.total_scanned_material,
                                                rowCnt.toString())
                                        }
                                        setProgressBar(pb)
                                    }
                                }
                                else{
                                    //SCAN SINGLE
                                    if(!btnTransfer.isEnabled){
                                        btnTransfer.isEnabled = true
                                    }
                                }
                            }
                            else{
                                cf.showMessage(c,"Error","You scanned the wrong Location","OK",
                                    positiveButtonAction = {
                                        inLocation.text.clear()
                                        inLocation.requestFocus()
                                    }).show()
                            }
                        }
                        else{
                            cf.showMessage(c,"Error","You scanned the wrong Material","OK",
                                positiveButtonAction = {
                                    inBarcode.text.clear()
                                    inBarcode.requestFocus()
                                }).show()
                        }

                    }

                }
                hideKeyboard()
                return@OnKeyListener true
            }
            false
        })

        btnTransfer.setOnClickListener {
            CoroutineScope(Dispatchers.Main ).launch {
                setProgressBar(pb)
                val result = SendToSAP(barcodeTranslated.PART_NO,inLocation.text.toString(),
                    barcodeTranslated.LOT,gbadgeNum,barcodeTranslated.QUANTITY
                ,barcodeTranslated.REEL_NO)
                setProgressBar(pb)
                val resultSplit = result.split('$')
                cf.showDialog(c,resultSplit[0],resultSplit[1],"OK","Cancel",
                    positiveButtonAction = {
                        this@T07TransferMaterial.finish()
                    }, negativeButtonAction = { clearEverything() }).show()

            }
        }
    }

    private fun clearEverything(){
        tvT07Material.text = "Material :"
        tvT07Quantity.text = "Reel Qty :"
        rqmnTV.text = "Requestor/Machine Num : "
        inBarcode.text.clear()
        inLocation.text.clear()
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

    private fun regenerateHeader(){
        mainTL.removeAllViews()
        var hRow = cf.generateRow(c)
        var hPart = cf.generateTVforRow("Part",c)
        var hBatch = cf.generateTVforRow("Batch",c)
        var hReelNo = cf.generateTVforRow("Reel No",c)
        var hQty = cf.generateTVforRow("Quantity",c)
        hRow.addView(hPart)
        hRow.addView(hBatch)
        hRow.addView(hReelNo)
        hRow.addView(hQty)
        mainTL.addView(hRow)
    }

    private suspend fun SendToSAP(mat:String,rack:String,lotNum:String,badgeNum:String,quantity:String,cartonNo:String):String{

        var jsonString = String()
        withContext(Dispatchers.IO){
            deviceID = getDeviceUniqueId(c)
            val itemID = intent.getIntExtra("ID", 0)
            val modelName = deviceID
            deviceID = getDeviceUniqueId(c)
            val RESTUrl = URL("http://172.16.206.19/EKANBANAPI/api/SMTEKANBAN")
            var payLoad = "{\n" +
                    "  \"material\": \"${mat}\",\n" +
                    "  \"location\": \"${rack}\",\n" +
                    "  \"batch\": \"${lotNum}\",\n" +
                    "  \"badgenum\": \"${badgeNum}\",\n" +
                    "  \"quantity\": ${quantity},\n" +
                    "  \"deviceid\": \"${modelName}\",\n" +
                    "  \"cartonnum\": \"${cartonNo}\",\n" +
                    "  \"itemid\": ${itemID}\n" +
                    "}"
            val connection = RESTUrl.openConnection() as HttpURLConnection
            // Set request method to POST
            connection.requestMethod = "POST"
            // Enable output for sending data
            connection.doOutput = true
            // Set request headers
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.setRequestProperty("Accept", "application/json")
            // Write JSON data to the connection output stream
            val wr = DataOutputStream(connection.outputStream)
            wr.write(payLoad.toByteArray(Charsets.UTF_8))
            wr.flush()
            wr.close()
            // Get response
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read response
                val inputStream = BufferedReader(InputStreamReader(connection.inputStream))
                var inputLine: String?
                val response = StringBuffer()
                while (inputStream.readLine().also { inputLine = it } != null) {
                    response.append(inputLine)
                }
                inputStream.close()
                jsonString = response.toString()
            }
            else{
                jsonString = "F\$INTERNAL SERVER ERROR CODE : $responseCode"
            }
            connection.disconnect()
        }
        return jsonString
    }

    fun Activity.hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))
    }

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun translateBarcode(bc:String):barcodeData {
        var tempItem = barcodeData("","","","","","","")
        val bcSplit = bc.split('(',')')
        when {
            bcSplit.size >= 15 -> {
                tempItem.VENDOR = bcSplit[2]
                tempItem.LOT = bcSplit[10]
                tempItem.DATE = bcSplit[4]
                tempItem.PART_NO = bcSplit[6]
                tempItem.REEL_NO = bcSplit[8]
                tempItem.QUANTITY = bcSplit[12]
                tempItem.UOM = bcSplit[14]

            }
            else -> {

            }
        }

        return tempItem
    }

    private fun setProgressBar(v:View){
        runOnUiThread {
            if (v.visibility == View.VISIBLE) {
                v.visibility = View.GONE
            } else {
                v.visibility = View.VISIBLE
            }
        }
    }
    private val homePressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_CLOSE_SYSTEM_DIALOGS) {
                // Home button was pressed
                // Perform your desired actions here
                val T07Cls = com.example.myapplication.T07()
                T07Cls.updateUsageArea(gArea,"0","")
                finish()
            }
        }
    }

    private suspend fun getLocationInfo(rackLoc:String,material:String):MutableList<Item> {
        var listofitems:MutableList<Item> = mutableListOf<Item>()

        withContext(Dispatchers.IO){
            try {
                val linkUrl = URL(getString(R.string.locationRackInfo,rackLoc,material))
                val jsonString = linkUrl.readText()
                val gson = Gson()
                val response = gson.fromJson(jsonString, Response::class.java)


                response.listInRack.forEach {
                    var tempItem = Item(it.MATERIAL,it.BATCH,it.QUANTITY)
                    listofitems.add(tempItem)

                }
            }
            catch (ex:Exception){
                Toast.makeText(c, ex.message.toString(), Toast.LENGTH_SHORT).show()
            }
        }

        return listofitems

    }

    private fun retrieveUsageList(area:String):AREAUSAGE{
        val CF:commonFunctions = commonFunctions()
        return CF.retrieveUsageList(area)
    }

    private fun getDeviceUniqueId(context: Context): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For devices running Android 10 (API level 29) or later
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        } else {
            // For devices running Android 9 (API level 28) or earlier
            @Suppress("DEPRECATION")
            android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: ""
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Call your function here
        val classText = "com.example.myapplication.T07area$gArea"
        val className = Class.forName(classText)
        val intent = Intent(this, className)
        intent.putExtra("Badge", gbadgeNum)
        intent.putExtra("Area", gArea)
        startActivity(intent)
    }
}
data class barcodeData(var VENDOR:String,var DATE:String,var PART_NO:String,var REEL_NO:String,var LOT:String,var QUANTITY:String,var UOM:String)

data class Response(
    val STATUS: String,
    val listInRack: List<Item>
)

data class Item(
    val MATERIAL: String,
    val BATCH: String,
    val QUANTITY: Double
)