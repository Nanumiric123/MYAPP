package com.example.myapplication

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.size
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class T52 : AppCompatActivity() {

    data class FORD_SYNC_DATA(
        var PARTNUMBER: String,
        var BATCH:String,
        var QUANTITY: Int,
         var LINE: String,
        var REMARKS:String
    )
    data class aggregatedStructure(
        var partnumber: String,
        var line:String,
        var remarks: String,
        var quantity: Int
    )


    private lateinit var title: TextView
    private lateinit var badgeNum: String
    private lateinit var MainLinearLayout: LinearLayout
    private lateinit var _context: Context
    private lateinit var trolleyTV: EditText
    private lateinit var pb: ProgressBar
    private lateinit var totalTV: TextView
    private lateinit var scv: ScrollView
    private lateinit var totalList:MutableList<FORD_SYNC_DATA>
    private lateinit var aggregatedList:MutableList<aggregatedStructure>
    private lateinit var deviceID:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_t52)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        totalList = mutableListOf()
        aggregatedList = mutableListOf()
        title = findViewById(R.id.T52TITLE)
        title.setText(intent.getStringExtra("Desc").toString())
        badgeNum = intent.getStringExtra("Badge").toString()
        MainLinearLayout = findViewById(R.id.T51LINEARLAYOUT)
        trolleyTV = findViewById(R.id.T52EDTrolley)
        scv = findViewById(R.id.T52SV)
        _context = this@T52
        totalTV = findViewById(R.id.T52TVTotal)
        pb = findViewById(R.id.T52PB)
        trolleyTV.requestFocus()
        trolleyTV.setOnKeyListener(View.OnKeyListener { view, i, keyEvent ->
            if (i == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_DOWN
                || i == KeyEvent.KEYCODE_TAB && keyEvent.action == KeyEvent.ACTION_DOWN){
                loadData()

                return@OnKeyListener true
            }
            false
        })
    }

    private fun loadData(){
        CoroutineScope(Dispatchers.IO).launch {
            progressbarSetting(pb)
            var jsonString = generateTransferList(trolleyTV.text.toString())
            try{
                aggregatedList.clear()
                var dataList = convertJsonToList(jsonString)
                val summedData = dataList.groupBy { Triple(it.PARTNUMBER, it.LINE, it.REMARKS) }
                    .mapValues { entry ->
                        entry.value.sumBy { it.QUANTITY.toInt() }
                    }
                // Populate the aggregated list with the grouped and summed data
                summedData.forEach { (key, totalQuantity) ->
                    val (partNumber, line, remarks) = key
                    aggregatedList.add(aggregatedStructure(partNumber, line, remarks, totalQuantity))
                }
                runOnUiThread(Runnable {
                    if(MainLinearLayout.size > 0){
                        MainLinearLayout.removeAllViews()
                    }
                    MainLinearLayout.addView(generateTable(dataList))
                })
            }
            catch(ex:Exception){
                ex.message.toString()
            }

        }

    }

    private fun convertJsonToList(inputString:String):MutableList<FORD_SYNC_DATA>{

        var resultList:MutableList<FORD_SYNC_DATA> = mutableListOf()
        var jsonArrayObj = JSONArray(inputString)
        for (i in 0 until jsonArrayObj.length()){
            var temp = FORD_SYNC_DATA(PARTNUMBER = "", BATCH = "",QUANTITY=0, LINE = "",REMARKS ="")
            val jsobObj = jsonArrayObj.getJSONObject(i)
            temp.LINE = jsobObj.getString("line")
            temp.PARTNUMBER = jsobObj.getString("material")
            temp.QUANTITY = (jsobObj.getString("quantity").toDouble()).toInt()
            temp.BATCH = jsobObj.getString("batch")
            temp.REMARKS = jsobObj.getString("remarks")
            resultList.add(temp)
        }


        return resultList
    }

    //Generate child view for MainLayout which is a table which returns table as View
    private fun generateTable(inputList:MutableList<FORD_SYNC_DATA>): View {
        //set table parameters first as we set the parameters for layout xml
        val tableParam: TableLayout.LayoutParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.MATCH_PARENT
        )
        //set whatever attribute that you want in your table. This will effect the table in your end result in your app.
        with(tableParam){
            setMargins(10,10,10,10)
        }
        //set Parameters for rows
        val rowParams: TableRow.LayoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.MATCH_PARENT
        )
        //set whatever attribute that you want in your row. in this case i am adding the borders or the row with the thickness of 1F
        with(rowParams){
            weight = 1F
        }
        //create table with table Layout method passing context which we declared in main
        val table = TableLayout(_context)
        //generate table ID for if you want to reuse the table later
        table.id = View.generateViewId()
        //generate the table layout this is important
        table.layoutParams = tableParam
        //Add Header Row and pass the context this can be skipped when using for loop this table in main
        val headerRow = TableRow(_context)
        //set the row parameters this is important
        headerRow.layoutParams = TableRow.LayoutParams(rowParams)
        // you can generate view ID in case you want to call back the child row in main
        headerRow.id = View.generateViewId()

        //call the generate text view method we created below and add the view to your header row
        headerRow.addView(generateTVforRow("Material"))
        headerRow.addView(generateTVforRow("Batch"))
        headerRow.addView(generateTVforRow("Quantity"))
        headerRow.addView(generateTVforRow("Line"))
        headerRow.addView(generateTVforRow("Transfer"))
        //add the header row to your table first
        table.addView(headerRow)

        for (i in 0 until inputList.size){
            //call the method to generate rows passing the row parameters
            val rowView = generateRowsForTable(inputList[i],rowParams)
            //add row View to the table as child view
            table.addView(rowView)
        }

        return table
    }

    private fun determineButtonColor(materialName:String):Int{
        var totalListRow = aggregatedList.filter { it.partnumber == materialName }
        var remarkSplit = totalListRow[0].remarks.split('|')
        var totalBinSplit = remarkSplit[0].split(':')
        var totalQuantitySplit = remarkSplit[1].split(':')
        var totalBin = totalBinSplit[1].toInt()
        var totalQuantity = totalQuantitySplit[1].toInt()
        var totalItemInTrolley = totalBin * totalQuantity
        var materialQty = totalListRow[0].quantity
        return when {
            totalItemInTrolley >= materialQty -> Color.parseColor("#10b80d")
            totalItemInTrolley < materialQty -> Color.parseColor("#b8b80d")
            else -> Color.parseColor("#b80d0d")
        }


    }

    //create a method for generating rows for datatable here you can generate whatever row that you like
    private fun generateRowsForTable(currentRow:FORD_SYNC_DATA,rowsParam:TableRow.LayoutParams):View {
        //Generate text view for each of the columns by calling the method we created below
        val materialTV = generateTVforRow(currentRow.PARTNUMBER)
            val quantityTV = generateTVforRow(currentRow.QUANTITY.toString())
        val batchTV = generateTVforRow(currentRow.BATCH)
        val lineTV = generateTVforRow(currentRow.LINE)
        //generate button value in variable calling the method passing the button text
        val btn:Button = generateButtonForRow("Transfer: ${currentRow.PARTNUMBER}")
        try {
            btn.setBackgroundColor(determineButtonColor(currentRow.PARTNUMBER))
        }
        catch (e:Exception){

        }
        //set the button click listener
        btn.setOnClickListener{
            executeSendToSAP(currentRow,pb,btn)
        }
        //generate linear layout variable by calling the method and passing the row parameters and the button we created
        val buttonContainer = generateLinearContainerForButton(rowsParam,btn)
        //Generate rows for your table later
        val row = TableRow(_context)
        //set layouts from the parameters in the method
        row.layoutParams = rowsParam
        //generate row ID in case you want to retrieve whatever value you want in textview child
        row.id = View.generateViewId()
        //create rows with texts by add textview inside
        row.addView(materialTV)
        row.addView(batchTV)
        row.addView(quantityTV)
        row.addView(lineTV)
        //add Linear Layout with button to current row
        row.addView(buttonContainer)

        return row
    }
    fun getHardwareBasedUUID(context: Context): String {
        val androidID = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return UUID.nameUUIDFromBytes(androidID.toByteArray()).toString()
    }

    //perform The transfer
    private fun SendToSAP(data:FORD_SYNC_DATA):String{
        // Validate all required parameters
        if (data.PARTNUMBER.isNullOrEmpty() || data.BATCH.isNullOrEmpty()  ||
            data.LINE.isNullOrEmpty() ) {
            throw IllegalArgumentException("All parameters must be provided and cannot be null or empty")
        }

        val urlString = "http://172.16.206.19/FORD_SYNC/api/T52"
        var apiPayLoad = "{\n" +
                "  \"DEVICE_ID\": \"${getHardwareBasedUUID(_context)}\",\n" +
                "  \"material\": \"${data.PARTNUMBER}\",\n" +
                "  \"quantity\": \"${data.QUANTITY.toString()}\",\n" +
                "  \"storagebin\": \"${trolleyTV.text.toString()}\",\n" +
                "  \"batch\": \"${data.BATCH}\",\n" +
                "  \"line\": \"${data.LINE}\",\n" +
                "  \"badge\": \"${badgeNum}\"\n" +
                "}"
        val url = URL(urlString)
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection

        return try{
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
                connection.inputStream.bufferedReader().use { it.readText() }
            }
            else{
                "POST request failed"
            }

        } catch (e: IOException) {
            e.message.toString()
        }
        finally {
            connection.disconnect()
        }


    }
    private fun setProgressBar(v: View, show: Boolean,btnSave:Button) {
        if (show) {
            v.visibility = View.VISIBLE
            btnSave.isEnabled = false
        } else {
            v.visibility = View.GONE
            btnSave.isEnabled = true
        }
    }
    // execute the SendToSAP
    private fun executeSendToSAP(data:FORD_SYNC_DATA,progressBar:View,tfButton:Button){
        CoroutineScope(Dispatchers.Main).launch {
            try{
                // Show the progress bar and disable the button
                setProgressBar(progressBar, false,tfButton)
                // Perform the network request in the IO dispatcher
                val result = withContext(Dispatchers.IO) {
                    SendToSAP(data)
                }
                // Hide the progress bar and enable the button
                setProgressBar(progressBar, false,tfButton)
                // Handle the result
                result?.let { resultString ->
                    // Process the JSONObject, e.g., update the UI with the order details
                    // Example: val orderId = jsonObject.getString("orderId")
                    val builder = AlertDialog.Builder(_context)
                    builder.setTitle("Message")
                    builder.setMessage(resultString)
                    builder.setPositiveButton("OK") { dialog, which ->
                        // Do something when OK button is clicked
                        loadData()
                        dialog.dismiss()
                    }
                    builder.show()
                } ?: run {
                    // Handle the error, e.g., show an error message
                }
            } catch (e: IllegalArgumentException) {
                // Hide the progress bar
                setProgressBar(progressBar, false,tfButton)
                // Show a toast message with the error
                Toast.makeText(progressBar.context, e.message, Toast.LENGTH_SHORT).show()
            }
        }
        //SendToSAP(data)
    }

    //generate linearlayout for buttons
    private fun generateLinearContainerForButton(rowParams:TableRow.LayoutParams,btn:Button):LinearLayout{
        val containerLayout = LinearLayout(_context)
        with(containerLayout) {
            layoutParams = rowParams
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.cell_with_border)
            setPadding(15, 15, 15, 15)
        }
        containerLayout.addView(btn)
        return containerLayout
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
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.MATCH_PARENT
            )
            //set background border for textview table the table visible
            //cell with border xml resource file you can copy in the res -> drawable in the project folder
            setBackgroundResource(R.drawable.cell_with_border)
        }
        return generateTV
    }



    private fun generateButtonForRow(btnText: String): Button {
        // Declare a button as view
        val buttonRow = Button(_context)

        // Set the button attribute or appearance
        with(buttonRow) {
            // Set text for button
            text = btnText
            // Set background color
            setBackgroundColor(Color.GRAY)
            setTextColor(Color.WHITE)
            // Set shape and background color for button by using the drawable XML
            setBackgroundResource(R.drawable.button_draw)
            // Set text size
            textSize = 16F
            // Set text alignment
            gravity = Gravity.CENTER
            // Set the button size
            layoutParams = TableRow.LayoutParams(
                300,
                TableRow.LayoutParams.WRAP_CONTENT
            )
        }

        // Add scaling animation on button press
        buttonRow.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    val scaleDown = ScaleAnimation(
                        1f, 0.9f, 1f, 0.9f, // Scale down from 100% to 90%
                        ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                        ScaleAnimation.RELATIVE_TO_SELF, 0.5f
                    )
                    scaleDown.duration = 100
                    scaleDown.fillAfter = true
                    v.startAnimation(scaleDown)
                }

                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    val scaleUp = ScaleAnimation(
                        0.9f, 1f, 0.9f, 1f, // Scale up back to 100%
                        ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                        ScaleAnimation.RELATIVE_TO_SELF, 0.5f
                    )
                    scaleUp.duration = 100
                    scaleUp.fillAfter = true
                    v.startAnimation(scaleUp)
                }
            }
            false
        }

        return buttonRow
    }


    private suspend fun generateTransferList(trolleyNum:String):String{
        var result = String()
        withContext(Dispatchers.IO){
            val url = URL("http://172.16.206.19/FORD_SYNC/api/T52?BinNumber=${trolleyNum}")

            // Establish a connection
            val connection = url.openConnection() as HttpURLConnection
            try {
                // Set up the request method
                connection.requestMethod = "GET"

                // Set a timeout for the connection and read operations
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                // Connect to the server
                connection.connect()

                // Get the response code
                val responseCode = connection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read the input stream from the connection
                    val inputStream = connection.inputStream
                    val response = inputStream.bufferedReader().use { it.readText() }

                    // Check if the response is empty
                    if (response.isEmpty()) {

                    } else {
                        var jObj = JSONObject(response)
                        result = jObj.getString("items")
                    }
                } else {
                    println("GET request failed. Response Code: $responseCode")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // Disconnect the connection to release resources
                connection.disconnect()
            }
            progressbarSetting(pb)
        }

        return result
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
