package com.example.myapplication

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL


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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_t07_transfer_material)
        pb = findViewById(R.id.T07TFPB)
        tvT07Quantity = findViewById(R.id.T07TFQTYTBT)
        tvT07Location = findViewById(R.id.T07TFLOCTBT)
        tvT07Material = findViewById(R.id.T07TFMATTBT)
        inLocation = findViewById(R.id.T07TFTOLOC)
        inBarcode = findViewById(R.id.T07TFBCODE)
        T07Switch = findViewById(R.id.T07SWSCAN)
        btnTransfer = findViewById(R.id.BTNT07Transfer)
        totalScannedTV = findViewById(R.id.T07SCANTOTAL)
        rqmnTV = findViewById(R.id.T07TVMNRQ)
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
                    }
                    builder.setNegativeButton("Cancel"){ dialog, which ->
                        // Do something when OK button is clicked
                        inLocation.setText("")
                        inLocation.requestFocus()
                    }
                    builder.show()
                }
                else{
                    if(gLocation == inLocation.text.toString()){
                        setProgressBar(pb)
                        runBlocking {
                            val job = GlobalScope.launch {
                                rackList = getLocationInfo(inLocation.text.toString(),gMaterial)
                            }
                            job.join()
                            inBarcode.requestFocus()
                            if(rackList.size<0){
                                val builder = AlertDialog.Builder(c)
                                builder.setTitle(R.string.Warning)
                                builder.setMessage("JANGAN MANUAL KEY IN. TOLONG SCAN DULU LOCATION ")
                                builder.setPositiveButton("OK") { dialog, which ->
                                    // Do something when OK button is clicked
                                    inBarcode.requestFocus()
                                }
                                builder.setNegativeButton("Cancel"){ dialog, which ->
                                    // Do something when OK button is clicked
                                    inLocation.setText("")
                                    inLocation.requestFocus()
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
                        }
                        builder.setNegativeButton("Cancel"){ dialog, which ->
                            // Do something when OK button is clicked
                            inLocation.setText("")
                            inLocation.requestFocus()
                        }
                        builder.show()
                    }
                }
                hideKeyboard()
                return@OnKeyListener true
            }
            false
        })
        T07Switch.setOnCheckedChangeListener { buttonView, isChecked ->
            // do something, the isChecked will be
            // true if the switch is in the On position
            val mainLayout:LinearLayout = findViewById(R.id.T07TFSCANLIST)
            mainLayout.removeAllViews()
            dataList.clear()
            btnTransfer.isEnabled = T07Switch.isChecked != true
        }

        inBarcode.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
                || keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN){
                if(inBarcode.text.isNullOrBlank()){

                }
                else{
                    if(T07Switch.isChecked){
                        setProgressBar(pb)
                        var barcodeTranslated=barcodeData("","","","","","","")
                        runBlocking {
                            val job = GlobalScope.launch {
                                barcodeTranslated = translateBarcode(inBarcode.text.toString())
                                inBarcode.text.clear()
                            }
                            job.join()
                            if(barcodeTranslated.PART_NO == gMaterial){
                                if(checkDuplicates(barcodeTranslated.PART_NO,barcodeTranslated.REEL_NO)){
                                    val builder = AlertDialog.Builder(c)
                                    builder.setTitle(R.string.Warning)
                                    builder.setMessage("Duplicate Carton")
                                    builder.setPositiveButton("OK") { dialog, which ->
                                        // Do something when OK button is clicked
                                        inBarcode.requestFocus()
                                    }
                                    builder.setNegativeButton("Cancel"){ dialog, which ->
                                        // Do something when OK button is clicked
                                        inLocation.setText("")
                                        inLocation.requestFocus()
                                    }
                                    builder.show()
                                }
                                else{
                                    var sAPResult = SendToSAP(barcodeTranslated.PART_NO,inLocation.text.toString(),barcodeTranslated.LOT,
                                        gbadgeNum,barcodeTranslated.QUANTITY,barcodeTranslated.REEL_NO)
                                    var sAPresultsplit = sAPResult.split('$')
                                    if(sAPresultsplit[0].contains('F')){
                                        runOnUiThread {
                                            val builder = AlertDialog.Builder(c)
                                            builder.setTitle(sAPresultsplit[0] + "Error")
                                            builder.setMessage(sAPresultsplit[1])
                                            builder.setPositiveButton("OK") { dialog, which ->
                                                // Do something when OK button is clicked
                                                inBarcode.requestFocus()
                                            }
                                            builder.show()
                                        }
                                    }
                                    else{
                                        dataList.add(barcodeTranslated)
                                        generateTable(barcodeTranslated)
                                        totalScannedTV.text =
                                            getString(R.string.total_scanned_material, dataList.size.toString())
                                    }
                                }
                            }
                            else{
                                val builder = AlertDialog.Builder(c)
                                builder.setTitle(R.string.Warning)
                                builder.setMessage("Material or barcode is wrong")
                                builder.setPositiveButton("OK") { dialog, which ->
                                    // Do something when OK button is clicked
                                    inBarcode.requestFocus()
                                }
                                builder.setNegativeButton("Cancel"){ dialog, which ->
                                    // Do something when OK button is clicked
                                    inLocation.setText("")
                                    inLocation.requestFocus()
                                }
                                builder.show()

                            }
                        }
                    }
                    else{
                        setProgressBar(pb)
                        runBlocking {
                            val job = GlobalScope.launch {
                                barcodeTranslated = translateBarcode(inBarcode.text.toString())
                                inBarcode.text.clear()
                            }
                            job.join()

                            if(barcodeTranslated.PART_NO == gMaterial){
                                generateTextView(barcodeTranslated.PART_NO,barcodeTranslated.LOT,barcodeTranslated.REEL_NO,barcodeTranslated.QUANTITY)
                            }
                            else{
                                val builder = AlertDialog.Builder(c)
                                builder.setTitle(R.string.Warning)
                                builder.setMessage("Material or barcode is wrong")
                                builder.setPositiveButton("OK") { dialog, which ->
                                    // Do something when OK button is clicked
                                    inBarcode.requestFocus()
                                }
                                builder.setNegativeButton("Cancel"){ dialog, which ->
                                    // Do something when OK button is clicked
                                    inLocation.setText("")
                                    inLocation.requestFocus()
                                }
                                builder.show()
                            }

                        }
                    }
                }
                hideKeyboard()
                return@OnKeyListener true
            }
            false
        })

        btnTransfer.setOnClickListener {
            if(runBlocking { checkDuplicates(barcodeTranslated.PART_NO,barcodeTranslated.REEL_NO) }){
                val builder = AlertDialog.Builder(c)
                builder.setTitle(R.string.Warning)
                builder.setMessage("Duplicate Carton")
                builder.setPositiveButton("OK") { dialog, which ->
                    // Do something when OK button is clicked
                    inBarcode.requestFocus()
                }
                builder.setNegativeButton("Cancel"){ dialog, which ->
                    // Do something when OK button is clicked
                    inLocation.setText("")
                    inLocation.requestFocus()
                }
                builder.show()
            }
            else{
                var sAPResult = String()
                runBlocking {
                    sAPResult = SendToSAP(barcodeTranslated.PART_NO,inLocation.text.toString(),barcodeTranslated.LOT,
                        gbadgeNum,barcodeTranslated.QUANTITY,barcodeTranslated.REEL_NO)
                }
                var sAPresultsplit = sAPResult.split('$')
                if(sAPresultsplit[0].contains('E')){
                    runOnUiThread {
                        val builder = AlertDialog.Builder(c)
                        builder.setTitle(sAPresultsplit[0] + "Error")
                        builder.setMessage(sAPresultsplit[1])
                        builder.setPositiveButton("OK") { dialog, which ->
                            // Do something when OK button is clicked
                            inBarcode.requestFocus()
                        }
                        builder.show()
                    }
                }
                else{
                    runOnUiThread {
                        val builder = AlertDialog.Builder(c)
                        builder.setTitle(sAPresultsplit[0])
                        builder.setMessage(sAPresultsplit[1])
                        builder.setPositiveButton("OK") { dialog, which ->
                            // Do something when OK button is clicked
                            finish()
                        }
                        builder.show()
                    }
                }
            }
        }
    }
    private suspend fun SendToSAP(mat:String,rack:String,lotNum:String,badgeNum:String,quantity:String,cartonNo:String):String{
        setProgressBar(pb)
        var jsonString = String()
        withContext(Dispatchers.Default){
            deviceID = getDeviceUniqueId(c)
            runBlocking {
                val job = GlobalScope.launch {
                    var Usagedata = retrieveUsageList(gArea)
                    if(Usagedata.USAGE == "0" || Usagedata.BADGE == deviceID){
                        val itemID= intent.getSerializableExtra("ID").toString()
                        val modelName = deviceID
                        var linkString = "http://172.16.206.19/REST_API/SMT_EKANBAN_SERVICES/MPPSMTEKANBANTransferMaterial?material=${mat}&location=${rack}&batch=${lotNum}&badgeNum=${badgeNum}&quantity=${quantity}&ItemID=${itemID}&deviceID=${modelName}&cartonNum=${cartonNo}"
                        val linkUrl = URL(linkString)
                        jsonString = linkUrl.readText()
                    }
                    else{
                        jsonString = "F\$Area is being used !"
                    }
                }
                job.join()
                setProgressBar(pb)
            }
        }
        return jsonString
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
    private suspend fun checkDuplicates(matrn:String,ctnNo:String):Boolean{
        var res = 0
        var resF:Boolean = false
        withContext(Dispatchers.Default){
            runBlocking {
                val job = GlobalScope.launch {
                    var result = URL("http://172.16.206.19/REST_API/SMT_EKANBAN_SERVICES/CheckForDuplicated?cartonNo=${ctnNo}&material=${matrn}").readText()
                    res = result.replace("\"","").toInt()
                }
                job.join()
                if(dataList.size > 0){
                    val itm = dataList.filter{it.PART_NO == matrn && it.REEL_NO == ctnNo}
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

    private fun generateTable(dt:barcodeData){
        val mainLayout:LinearLayout = findViewById(R.id.T07TFSCANLIST)
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
            text = dt.PART_NO
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

    private fun generateTextView(Mat:String,Bat:String,ReelNo:String,qtyReel:String){
        val mainLayout:LinearLayout = findViewById(R.id.T07TFSCANLIST)
        mainLayout.removeAllViews()
        val matTV:TextView = TextView(c)
        with(matTV){
            text = "Material : $Mat"
            textSize = 20F
            setTextColor(Color.BLACK)
            gravity= Gravity.CENTER
            setPadding(15,15,15,15)
            layoutParams = TableRow.LayoutParams( TableRow.LayoutParams.MATCH_PARENT,TableRow.LayoutParams.WRAP_CONTENT)
            setBackgroundResource(R.drawable.cell_with_border)
        }
        val batTV:TextView = TextView(c)
        with(batTV){
            text = "Batch : $Bat"
            textSize = 20F
            setTextColor(Color.BLACK)
            gravity= Gravity.CENTER
            setPadding(15,15,15,15)
            layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,TableRow.LayoutParams.WRAP_CONTENT)
            setBackgroundResource(R.drawable.cell_with_border)
        }
        val rnTV:TextView = TextView(c)
        with(rnTV){
            text = "Reel Num : $ReelNo"
            textSize = 20F
            setTextColor(Color.BLACK)
            gravity= Gravity.CENTER
            setPadding(15,15,15,15)
            layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,TableRow.LayoutParams.WRAP_CONTENT)
            setBackgroundResource(R.drawable.cell_with_border)
        }
        val qtyTV:TextView = TextView(c)
        with(qtyTV){
            text = "Reel Quantity : $qtyReel"
            textSize = 20F
            setTextColor(Color.BLACK)
            gravity= Gravity.CENTER
            setPadding(15,15,15,15)
            layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT)
            setBackgroundResource(R.drawable.cell_with_border)
        }

        mainLayout.addView(matTV)
        mainLayout.addView(batTV)
        mainLayout.addView(qtyTV)
        mainLayout.addView(rnTV)
    }

    suspend fun translateBarcode(bc:String):barcodeData {
        var tempItem = barcodeData("","","","","","","")
        withContext(Dispatchers.Default){
            try{
                val linkUrl = URL(getString(R.string.barcodeTranslator,bc))
                val jsonOBJ = JSONObject(linkUrl.readText())

                tempItem = barcodeData(jsonOBJ.getString("VENDOR"),jsonOBJ.getString("DATE")
                    ,jsonOBJ.getString("PART_NO"),jsonOBJ.getString("REEL_NO"),jsonOBJ.getString("LOT"),
                    jsonOBJ.getString("QUANTITY"),jsonOBJ.getString("UOM"))
            }
            catch (ex:Exception){
                Toast.makeText(c, ex.message.toString(), Toast.LENGTH_SHORT).show()
            }
            finally {
                setProgressBar(pb)
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
        withContext(Dispatchers.Default){
            val linkUrl = URL(getString(R.string.locationRackInfo,rackLoc,material))
            val jsonString = linkUrl.readText()
            val gson = Gson()
            val response = gson.fromJson(jsonString, Response::class.java)


            response.listInRack.forEach {
                var tempItem = Item(it.MATERIAL,it.BATCH,it.QUANTITY)
                listofitems.add(tempItem)

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