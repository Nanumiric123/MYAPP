package com.example.myapplication

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import android.provider.Settings
import java.security.MessageDigest
import kotlin.properties.Delegates


class T51 : AppCompatActivity() {
    private lateinit var _context: Context
    private lateinit var title:TextView
    private lateinit var barcode:EditText
    private lateinit var stdBinTyp:EditText
    private lateinit var partNum:EditText
    private lateinit var KBpartNum:EditText
    private lateinit var batchNum:EditText
    private lateinit var KBqty:EditText
    private lateinit var fromLoc:EditText
    private lateinit var toLoc:EditText
    private lateinit var PB:ProgressBar
    private lateinit var saveBtn:Button
    private lateinit var menuBtn:Button
    private lateinit var clearBtn:Button
    private lateinit var badgeNumber:String
    private lateinit var scv: ScrollView
    private lateinit var reelNum:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_t51)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.T51PB)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        badgeNumber = intent.getStringExtra("Badge").toString()
        val function_description = intent.getStringExtra("Desc").toString()

        _context = this
        title = findViewById(R.id.T51TITLE)
        title.text = function_description
        barcode = findViewById(R.id.T51edBarcode)
        barcode.requestFocus()
        partNum = findViewById(R.id.T51EDPart)
        KBpartNum = findViewById(R.id.T51EDPartOnKB)
        stdBinTyp = findViewById(R.id.T51EDstdBinTyp)
        KBqty = findViewById(R.id.T51EDKBqty)
        batchNum = findViewById(R.id.T51EDLotNo)
        fromLoc = findViewById(R.id.T51EDfromLoc)
        toLoc = findViewById(R.id.T51EDtoLoc)
        PB = findViewById(R.id.T51PB)
        saveBtn = findViewById(R.id.T51btnSave)
        menuBtn = findViewById(R.id.T51btnMenu)
        clearBtn = findViewById(R.id.T51btnClear)
        scv = findViewById(R.id.T51SCV)
        barcode.setOnKeyListener(View.OnKeyListener { view, i, keyEvent ->
            if (i == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_DOWN
                || i == KeyEvent.KEYCODE_TAB && keyEvent.action == KeyEvent.ACTION_DOWN){
                progressbarSetting(PB)
                //Perform barcode Translation
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val barcodeResult = translateBarcode(barcode.text.toString())
                        val bcodeData = translateAPIResult(barcodeResult)
                        partNum.setText(bcodeData.pART_NO)
                        batchNum.setText(bcodeData.lOT)
                        KBqty.setText(bcodeData.qUANTITY)
                        reelNum = bcodeData.rEEL_NO
                        KBpartNum.requestFocus()
                        hideKeyboard()
                    }
                    catch (e:Exception){
                        TriggerAlert(_context,e.message.toString())
                    }

                    progressbarSetting(PB)
                }
                return@OnKeyListener true
            }
            false
        })

        KBpartNum.setOnKeyListener(View.OnKeyListener { view, i, keyEvent ->
            if (i == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_DOWN
                || i == KeyEvent.KEYCODE_TAB && keyEvent.action == KeyEvent.ACTION_DOWN){
                progressbarSetting(PB)
                //Perform barcode Translation
                CoroutineScope(Dispatchers.Main).launch {
                    val qtyKB = retrieveBinQuantity(KBpartNum.text.toString())
                    var lblQty = KBqty.text.toString().toInt()
                    if(qtyKB > lblQty){
                        KBqty.setText(lblQty.toString())
                    }
                    else{
                        KBqty.setText(qtyKB.toString())
                    }

                    fromLoc.requestFocus()
                    hideKeyboard()
                    progressbarSetting(PB)
                }
                return@OnKeyListener true
            }
            false
        })

        fromLoc.setOnKeyListener(View.OnKeyListener { view, i, keyEvent ->
            if (i == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_DOWN
                || i == KeyEvent.KEYCODE_TAB && keyEvent.action == KeyEvent.ACTION_DOWN){
                toLoc.requestFocus()
                hideKeyboard()
                return@OnKeyListener true
            }
            false
        })
        toLoc.setOnKeyListener(View.OnKeyListener { view, i, keyEvent ->
            if (i == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_DOWN
                || i == KeyEvent.KEYCODE_TAB && keyEvent.action == KeyEvent.ACTION_DOWN){
                scv.post{
                    scv.smoothScrollTo(0,0)
                }
                hideKeyboard()
                return@OnKeyListener true
            }
            false
        })

        menuBtn.setOnClickListener {
            finish()
        }
        clearBtn.setOnClickListener {
            resetForm()
        }
        saveBtn.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                progressbarSetting(PB)
                val checkResult = checkQuantityForMaterial(partNum.text.toString(),toLoc.text.toString(),KBqty.text.toString(),reelNum)
                val splitResult = checkResult.split('$')
                val status = splitResult[0].replace("\"", "").trim()
                val messages = splitResult[1].replace("\"", "")

                if(status == "S"){
                    updateDB(partNum.text.toString(),batchNum.text.toString(),KBqty.text.toString(),toLoc.text.toString(),fromLoc.text.toString(),badgeNumber)
                }
                else{
                    TriggerAlert(_context,messages)
                }
                progressbarSetting(PB)
            }
        }
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

    private fun getDeviceUniqueId(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val serialNumber = Build.SERIAL

        // Concatenate the two IDs and hash them to create a unique identifier
        val combinedId = "$androidId$serialNumber"
        return hashString(combinedId)
    }
    private fun hashString(input: String): String {
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("", { str, it -> str + "%02x".format(it) })
    }
    private suspend fun updateDB(mat:String,bat:String,qty:String,trolley: String,strBin:String,BadgeNum:String):Boolean{
        var APIResult = String()
        withContext(Dispatchers.IO){
            val serialNumber = getDeviceUniqueId(_context)
            var url = URL("http://172.16.206.19/FORD_SYNC/API/T51KBQTY")
            var payLoad = "{\n" +
                    "  \"parT_NUMBER\": \"${mat}\",\n" +
                    "  \"batch\": \"${bat}\",\n" +
                    "  \"quantity\": ${qty},\n" +
                    "  \"trolley\": \"${trolley}\",\n" +
                    "  \"storagE_BIN\": \"${strBin}\",\n" +
                    "  \"badge\": \"${BadgeNum}\",\n" +
                    "  \"scanner\": \"${serialNumber}\",\n" +
                    "  \"transfered\": true,\n" +
                    "  \"cartoN_NO\": \"${reelNum}\"\n" +
                    "}"
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            // Set request headers
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.setRequestProperty("Accept", "application/json")
            try{
                // Write JSON data to the connection output stream
                val wr = DataOutputStream(connection.outputStream)
                wr.write(payLoad.toByteArray(Charsets.UTF_8))
                wr.flush()
                wr.close()
                // Get response
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = BufferedReader(InputStreamReader(connection.inputStream))
                    var inputLine: String?
                    val response = StringBuffer()

                    while (inputStream.readLine().also { inputLine = it } != null) {
                        response.append(inputLine)
                    }
                    inputStream.close()
                    APIResult = response.toString()
                }
                else{
                    APIResult = "{\"type\" : \"E\",\"message\":\"INTERNAL SERVER ERROR\"}"
                }
            }
            catch (e: Exception) {
                e.printStackTrace()
            } finally {
                connection.disconnect()
            }
        }
            val jobj = JSONObject(APIResult)
        val typ = jobj.getString("type").replace("\"", "").trim()
        val msg = jobj.getString("message").replace("\"", "")

        if(typ == "S"){
            runOnUiThread(Runnable {
                TriggerAlert(_context,msg)
            })
            return true
        }
        else{
            runOnUiThread(Runnable {
                TriggerAlert(_context,msg)
            })
            return false
        }
    }

    private suspend fun checkQuantityForMaterial(mat:String,trolley:String,inputQty:String,storBin:String):String{
        var result = String()
        var materialNumber = String()
        if(mat.contains('+')){
            materialNumber = mat.replace("+","%2B")
        }
        else{
            materialNumber = mat
        }
        withContext(Dispatchers.IO){
            val url = URL("http://172.16.206.19/FORD_SYNC/API/BARCODETRANSLATOR?material=${materialNumber}&trolley=${trolley}&inputQuantity=${inputQty}&storageBin=${storBin}")
            val connection = url.openConnection() as HttpURLConnection

            // Optional: Set request method to GET (GET is the default)
            connection.requestMethod = "GET"
            try{
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
                    result = response.toString()
                    inputStream.close()
                }
                else {
                    result = "F\$GET FAILED"
                }

            }
            catch(e:Exception){
                result = "F\$ ${e.message.toString()}"
            }
            finally {
                connection.disconnect()
            }
        }
        return result
    }

    private suspend fun translateBarcode(input: String):String{
        var APIResult:String = ""
        val response = StringBuilder()
        withContext(Dispatchers.IO){
            var url = URL("http://172.16.206.19/FORD_SYNC/API/BARCODETRANSLATOR")
            val payLoad = "\"${input.trim()}\""
            val conn = url.openConnection() as HttpURLConnection
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Content-Length", payLoad.length.toString())
            DataOutputStream(conn.getOutputStream()).use { it.writeBytes(payLoad) }
            BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                var inputLine: String?
                while (reader.readLine().also { inputLine = it } != null) {
                    response.append(inputLine)
                }

            }
            conn.disconnect()

        }
        return  response.toString()
    }

    private suspend fun retrieveBinQuantity(materialPara:String):Int{
        var ApiResult:Int = 0
        withContext(Dispatchers.IO){
            //materialPara.replace('+',"%2B")
            var materialReplaced = String()
            if(materialPara.contains('+')){
                materialReplaced = materialPara.replace("+","%2B",false)
            }
            else{
                materialReplaced = materialPara
            }

            var url = URL("http://172.16.206.19/FORD_SYNC/api/T51KBQty?material=${materialReplaced}")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            val responseCode = connection.responseCode
            val response = StringBuilder()

            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                var inputLine: String?
                while (reader.readLine().also { inputLine = it } != null) {
                    response.append(inputLine)
                }

            }
            connection.disconnect()

            val jsonResult = JSONArray(response.toString())
            val json = jsonResult.getJSONObject(0).getString("qtY_PER_BIN")
            val binTypStr = jsonResult.getJSONObject(0).getString("biN_TYPE")
            runOnUiThread(kotlinx.coroutines.Runnable {
                stdBinTyp.setText(binTypStr)
            })

            ApiResult = json.toInt()
        }

        return ApiResult
    }

    private fun TriggerAlert(c:Context,message:String){
        runOnUiThread(kotlinx.coroutines.Runnable{
            val builder = AlertDialog.Builder(c)

            builder.setTitle("Message")
            builder.setMessage(message)
            builder.setPositiveButton("OK") { dialog, which ->
                // Do something when OK button is clicked
                resetForm()
            }
            builder.show()
        })
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    private fun resetForm(){
        barcode.text.clear()
        partNum.text.clear()
        KBpartNum.text.clear()
        KBqty.text.clear()
        stdBinTyp.text.clear()
        batchNum.text.clear()
        fromLoc.text.clear()
        toLoc.text.clear()
        reelNum = ""
    }

    private fun progressbarSetting(v: View){
        runOnUiThread(Runnable {
            if (v.visibility == View.VISIBLE) {
                v.visibility = View.INVISIBLE
            } else {
                v.visibility = View.VISIBLE
            }

        })
    }
}
