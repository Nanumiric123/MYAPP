package com.example.myapplication

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.P11.SAP_DATA
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class P12 : AppCompatActivity() {
    private lateinit var title: TextView
    private lateinit var in_barcode: EditText
    private lateinit var in_batch: EditText
    private lateinit var in_part: EditText
    private lateinit var in_quantity: EditText
    private lateinit var in_toStorageBin: EditText
    private lateinit var btnMenu: Button
    private lateinit var btnSave: Button
    private lateinit var btnClear: Button
    private lateinit var pb: ProgressBar
    private lateinit var c: Context
    private lateinit var in_carton: EditText
    private lateinit var P12_TABLE_header:TableRow
    private lateinit var P12_TABLE:TableLayout
    private lateinit var badgeNum: String
    private lateinit var partNum:String
    private lateinit var g_carton:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_p12)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        badgeNum = intent.getStringExtra("Badge").toString()
        c = this@P12
        title = findViewById(R.id.P12TITLE)
        in_barcode = findViewById(R.id.P12EDBARCODE)
        btnMenu = findViewById(R.id.P12BTNMENU)
        btnClear = findViewById(R.id.P12BTNCLEAR)
        pb = findViewById(R.id.P12PB)
        P12_TABLE_header = findViewById(R.id.P12_TABLE_HEADER)
        P12_TABLE = findViewById(R.id.P12_TABLE)
        title.text = intent.getStringExtra("Desc")
        in_barcode.requestFocus()
        addTableHeader()
        in_barcode.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                CoroutineScope(Dispatchers.Main).launch {

                    var translatedBCResult = translateBarcode(in_barcode.text.toString())
                    var mat = JSONObject(translatedBCResult).getString("material")
                    var reelNo = JSONObject(translatedBCResult).getString("reelnumber")
                    g_carton = reelNo
                    partNum = mat
                    var res = retrieveMaterialData(mat)
                    for(i in 0 until res.size){
                        P12_TABLE.addView(generateRowForTable(res[i].MATERIAL,res[i].BATCH,res[i].QUANTITY))
                    }
                }
                return@OnKeyListener true
            }
            false
        })

        btnClear.setOnClickListener {
            clearEverything()
            in_barcode.requestFocus()
        }

        btnMenu.setOnClickListener {
            this@P12.finish()
        }
    }

    private fun addTableHeader(){
        P12_TABLE_header.addView(generateTVforRow("Material"))
        P12_TABLE_header.addView(generateTVforRow("Batch"))
        P12_TABLE_header.addView(generateTVforRow("Quantity"))
        P12_TABLE_header.addView(generateTVforRow("Transfer"))
    }
    // if you want to generate a button for each of the rows you must add this method to the row view
    private fun generateButtonForRow(btnText:String):Button {
        //declare a button as view
        val buttonRow = Button(c)
        //set the button attribute or appearence
        with(buttonRow){
            //set text for button
            text = btnText
            //Set color
            setBackgroundColor(Color.GRAY)
            setTextColor(Color.WHITE)
            //set share and background color for button by using the drawable xml
            setBackgroundResource(R.drawable.button_draw)
            //set text size
            textSize = 16F
            //set the text alignment
            gravity = Gravity.CENTER
            //set the button size
            layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
        }


        return buttonRow
    }
    //Create a function that returns the View of a textview for each of the rows
    private fun generateTVforRow(Displaytext:String):View {
        val generateTV = TextView(c)
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
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.MATCH_PARENT
            )
            //set background border for textview table the table visible
            //cell with border xml resource file you can copy in the res -> drawable in the project folder
            setBackgroundResource(R.drawable.cell_with_border)
        }
        return generateTV
    }
    private fun generateRowForTable(material:String,batch:String,quantity:String):View{
        //Generate rows for your table later
        val row = TableRow(c)
        //set Parameters for rows
        val rowParams: TableRow.LayoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.MATCH_PARENT
        )
        //set whatever attribute that you want in your row. in this case i am adding the borders or the row with the thickness of 1F
        with(rowParams){
            weight = 1F
        }
        //set layouts from the parameters in the method
        row.layoutParams = rowParams
        row.id = View.generateViewId()
        var qtyED = generateEDforRow(quantity)
        val btn:Button = generateButtonForRow("Transfer")
        //set the button click listener
        btn.setOnClickListener{
            //Toast.makeText(c, "Button for  test${material} ${batch}", Toast.LENGTH_LONG).show()
            val builder = AlertDialog.Builder(c)
            builder.setTitle("Confirmation")
                .setMessage("Transferring Material : ${material} \n Batch : ${batch} \n Quantity : ${qtyED.text.toString()} Are you sure you want to proceed?")
                .setPositiveButton("OK") { dialog, _ ->
                    // Action for "OK"
                    postToDB(material,batch,"KIV_TEMP",badgeNum,qtyED.text.toString(),g_carton)

                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    // Action for "Cancel"
                    dialog.dismiss()
                }
                .show()

        }
        row.addView(generateTVforRow(material))
        row.addView(generateTVforRow(batch))
        row.addView(generateLinearContainerForEditText(rowParams,qtyED))
        row.addView(generateLinearContainerForButton(rowParams,btn))
        return row
    }
    private fun regenerateTable(){
        CoroutineScope(Dispatchers.IO).launch {
            var res = retrieveMaterialData(partNum)
            withContext(Dispatchers.Main){
                P12_TABLE.removeAllViews()
                addTableHeader()
                for(i in 0 until res.size){
                    P12_TABLE.addView(generateRowForTable(res[i].MATERIAL,res[i].BATCH,res[i].QUANTITY))
                }
            }

        }
    }
    private fun generateEDforRow(DisplayValue:String):EditText {
        val edRow = EditText(c)
        with(edRow) {
            id = View.generateViewId()
            inputType = InputType.TYPE_CLASS_TEXT
            text = DisplayValue.toEditable()
        }
        return edRow
    }

    //generate linearlayout for buttons
    private fun generateLinearContainerForButton(rowParams:TableRow.LayoutParams, btn:Button): LinearLayout {
        val containerLayout = LinearLayout(c)
        with(containerLayout) {
            layoutParams = rowParams
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.cell_with_border)
            setPadding(15, 15, 15, 15)
        }
        containerLayout.addView(btn)
        return containerLayout
    }

    //generate linearlayout for edit texts
    private fun generateLinearContainerForEditText(rowParams:TableRow.LayoutParams, edTx:EditText): LinearLayout {
        val containerLayout = LinearLayout(c)
        with(containerLayout) {
            layoutParams = rowParams
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.cell_with_border)
            setPadding(15, 15, 15, 15)
        }
        containerLayout.addView(edTx)
        return containerLayout
    }
    fun getHardwareBasedUUID(context: Context): String {
        val androidID = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return UUID.nameUUIDFromBytes(androidID.toByteArray()).toString()
    }

    private fun postToDB(material:String,batch:String,storageBin:String,badgeNo:String,qty:String,cartonNo:String) {
        CoroutineScope(Dispatchers.IO).launch {
            try{
                withContext(Dispatchers.Main){
                    showProgressBar(true)
                }
                val jsonBody = "{\n" +
                        "  \"material\": \"${material}\",\n" +
                        "  \"batch\": \"${batch}\",\n" +
                        "  \"storagE_BIN\": \"${storageBin}\",\n" +
                        "  \"quantity\": ${qty},\n" +
                        "  \"badgE_NO\": \"${badgeNo}\",\n" +
                        "  \"deviceid\": \"${getHardwareBasedUUID(c)}\",\n" +
                        "  \"cartonnum\": \"${cartonNo}\"\n" +
                        "}"
                val url = URL("http://172.16.206.19/EKANBANAPI/api/P12")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; utf-8")
                    setRequestProperty("Accept", "application/json")
                }
                // Write the JSON body to the request
                connection.outputStream.use { outputStream: OutputStream ->
                    outputStream.write(jsonBody.toByteArray(Charsets.UTF_8))
                }
                // Read the response
                val responseCode = connection.responseCode
                val response = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use(BufferedReader::readText)
                } else {
                    connection.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                }
                // Notify completion
                withContext(Dispatchers.Main) {
                    showDialog(c, "Response", response ?: "No response from server.")
                    showProgressBar(false)
                }
            }
            catch(e:Exception){
                withContext(Dispatchers.Main){
                    showProgressBar(false)
                    showDialog(c, "Error", e.message ?: "An unknown error occurred.")
                }
            }

        }

    }

    private suspend fun translateBarcode(barcodeString:String):String{
        return withContext(Dispatchers.IO){
            try{
                // Show progress bar
                withContext(Dispatchers.Main) {
                    showProgressBar(true)
                }

                val url = URL("http://172.16.206.19/FORD_SYNC/API/BARCODETRANSLATOR")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; utf-8")
                    setRequestProperty("Accept", "application/json")
                }
                val jsobBody = "\"${barcodeString}\""
                // Write the JSON body
                connection.outputStream.use { outputStream: OutputStream ->
                    outputStream.write(jsobBody.toByteArray(Charsets.UTF_8))
                }
                // Read the response
                val responseCode = connection.responseCode
                val response = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use(BufferedReader::readText)
                } else {
                    connection.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                        ?: "Error occurred with code: $responseCode"
                }

                // Hide progress bar
                withContext(Dispatchers.Main) {
                    showProgressBar(false)
                }
                response


            } catch (e: Exception) {
                e.message ?: "An unknown error occurred."
            }
        }
    }

    private suspend fun retrieveMaterialData(material:String):MutableList<SAP_DATA>{

        return withContext(Dispatchers.IO){
            var result:MutableList<SAP_DATA> = mutableListOf()
            withContext(Dispatchers.Main){
                showProgressBar(true)
            }
            val url = URL("http://172.16.206.19/BARCODEWEBAPI/API/P12")
            var apiPayLoad = "\"${material}\""
            // Establish a connection
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection

            try {
                // Configure the connection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; utf-8")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                // Write the payload to the output stream
                val outputStream: OutputStream = connection.outputStream
                outputStream.write(apiPayLoad.toByteArray())
                outputStream.flush()
                outputStream.close()
                // Get the response code and read the response
                val responseCode = connection.responseCode
                if(responseCode == HttpURLConnection.HTTP_OK){
                    var response = connection.inputStream.bufferedReader().use { it.readText() }
                    var responseArr = JSONArray(response)
                    for(i in 0 until responseArr.length()){
                        var temp = SAP_DATA(MATERIAL = responseArr.getJSONObject(i).getString("matnr"),
                            BATCH = responseArr.getJSONObject(i).getString("charg"),
                            QUANTITY = responseArr.getJSONObject(i).getString("labst"))
                        result.add(temp)
                    }
                }
                else{

                }
            }
            catch (ex:Exception){
                println(ex.message.toString())
            }
            finally {
                withContext(Dispatchers.Main){
                    showProgressBar(false)
                }
            }


            result
        }
    }

    // Function to show a dialog box
    private fun showDialog(context: Context, title: String, message: String) {
        AlertDialog.Builder(context).apply {
            setTitle(title)
            setMessage(message)
            setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                in_barcode.text.clear()
                in_barcode.requestFocus()
                regenerateTable()
            }
            create()
            show()
        }
    }
    private fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)
    private fun clearEverything(){
        in_quantity.text.clear()
        in_barcode.text.clear()
        in_batch.text.clear()
        in_toStorageBin.text.clear()
        in_part.text.clear()
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
}