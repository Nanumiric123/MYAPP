package com.example.myapplication

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import java.util.UUID
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout.DispatchChangeEvent
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import android.provider.Settings
import java.io.BufferedReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import android.os.Build
import android.text.InputType
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.Toast
import com.google.firebase.crashlytics.buildtools.reloc.com.google.errorprone.annotations.Var
import org.json.JSONArray

class P11 : AppCompatActivity() {
    private lateinit var title:TextView
    private lateinit var in_barcode:EditText
    private lateinit var in_batch:EditText
    private lateinit var in_part:EditText
    private lateinit var in_quantity:EditText
    private lateinit var in_toStorageBin:EditText
    private lateinit var btnMenu:Button
    private lateinit var btnSave:Button
    private lateinit var btnClear:Button
    private lateinit var pb:ProgressBar
    private lateinit var c:Context
    private lateinit var g_carton:String
    private lateinit var P11_TABLE: TableLayout
    private lateinit var P11_TABLE_header: TableRow
    private lateinit var badgeNum: String
    private lateinit var partNum:String

    data class SAP_DATA (var MATERIAL:String, var BATCH:String,var QUANTITY:String )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_p11)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        badgeNum = intent.getStringExtra("Badge").toString()
        c = this@P11
        title = findViewById(R.id.P11_TITLE)
        title.text = intent.getStringExtra("Desc")
        in_barcode = findViewById(R.id.P11EDBARCODE)
        P11_TABLE = findViewById(R.id.tb_P11_TABLE)
        P11_TABLE_header = findViewById(R.id.tb_P11_TABLE_Header)
        btnMenu = findViewById(R.id.P11BtnMenu)

        btnMenu.setOnClickListener {
            this@P11.finish()
        }

        pb = findViewById(R.id.P11PB)
        addTableHeader()
        in_barcode.setOnKeyListener(View.OnKeyListener { view, i, keyEvent ->
            if (i == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_DOWN
                || i == KeyEvent.KEYCODE_TAB && keyEvent.action == KeyEvent.ACTION_DOWN){
                CoroutineScope(Dispatchers.Main).launch {
                    var translatedBC = translateBarcode(c,in_barcode.text.toString())
                    partNum = JSONObject(translatedBC).getString("material")
                    g_carton = JSONObject(translatedBC).getString("reelnumber")
                    var res = retrieveMaterialData(partNum)
                    for(i in 0 until res.size){
                        P11_TABLE.addView(generateRowForTable(res[i].MATERIAL,res[i].BATCH,res[i].QUANTITY))
                    }
                }
                return@OnKeyListener true
            }
            false
        })

    }

    private fun addTableHeader(){
        P11_TABLE_header.addView(generateTVforRow("Material"))
        P11_TABLE_header.addView(generateTVforRow("Batch"))
        P11_TABLE_header.addView(generateTVforRow("Quantity"))
        P11_TABLE_header.addView(generateTVforRow("Transfer"))
    }

    private fun regenerateTable(){
        CoroutineScope(Dispatchers.IO).launch {
            var res = retrieveMaterialData(partNum)
            withContext(Dispatchers.Main){
                P11_TABLE.removeAllViews()
                addTableHeader()
                for(i in 0 until res.size){
                    P11_TABLE.addView(generateRowForTable(res[i].MATERIAL,res[i].BATCH,res[i].QUANTITY))
                }
            }

        }
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
                    postToDB(material,batch,"KIV_TEMP",badgeNum, qtyED.text.toString(),g_carton)

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

    fun getHardwareBasedUUID(context: Context): String {
        val androidID = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return UUID.nameUUIDFromBytes(androidID.toByteArray()).toString()
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
    private fun generateLinearContainerForEditText(rowParams:TableRow.LayoutParams, edTx:EditText):LinearLayout {
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

    private fun generateEDforRow(DisplayValue:String):EditText {
        val edRow = EditText(c)
        with(edRow) {
            id = View.generateViewId()
            inputType = InputType.TYPE_CLASS_TEXT
            text = DisplayValue.toEditable()
        }
        return edRow
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

    private suspend fun retrieveMaterialData(material:String):MutableList<SAP_DATA>{

        return withContext(Dispatchers.IO){
            var result:MutableList<SAP_DATA> = mutableListOf()
            withContext(Dispatchers.Main){
                showProgressBar(true)
            }
            val url = URL("http://172.16.206.19/BARCODEWEBAPI/API/P11")
            var apiPayLoad = "{\n" +
                    "    \"material\" : \"${material}\",\n" +
                    "  \"storagetyp\": \"513\",\n" +
                    "  \"storageloc\": \"2120\",\n" +
                    "  \"storagebin\": \"KIV_TEMP\"\n" +
                    "}"
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
                    var responseObj = JSONObject(response)
                    var responseArr = JSONArray(responseObj.getString("items"))
                    for(i in 0 until responseArr.length()){
                        var temp = SAP_DATA(MATERIAL = responseArr.getJSONObject(i).getString("material"),
                            BATCH = responseArr.getJSONObject(i).getString("batch"),
                            QUANTITY = responseArr.getJSONObject(i).getString("quantity"))
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
    private fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)

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
                val url = URL("http://172.16.206.19/EKANBANAPI/api/P11")
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

    private suspend fun translateBarcode(cxt:Context,barcodeString:String):String{
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
    // Function to show a dialog box
    private fun showDialog(context: Context, title: String, message: String) {
        AlertDialog.Builder(context).apply {
            setTitle(title)
            setMessage(message)
            setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                regenerateTable()
            }
            create()
            show()
        }
    }
}