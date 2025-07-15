package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

class SAPPartInfo : AppCompatActivity() {

    private lateinit var mainLayout:LinearLayout
    private lateinit var inLoc:EditText
    private lateinit var tile:TextView
    private lateinit var c:Context
    private lateinit var inBarcode:EditText
    private lateinit var btnClear:Button
    private lateinit var btnMenu:Button
    private lateinit var mat:EditText
    private lateinit var bat:EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sappart_info)
        tile = findViewById(R.id.Q01TVTITLE)
        tile.text = intent.getStringExtra("Desc").toString()
        c = this
        mainLayout = findViewById(R.id.MainLinearHoriz)
        inLoc = findViewById(R.id.Q01EDLOCATION)
        inBarcode = findViewById(R.id.Q01EDBARCODE)
        btnClear = findViewById(R.id.Q01btnClear)
        btnMenu = findViewById(R.id.Q01BtnRegenrate)
        mat = findViewById(R.id.Q01EDMaterial)
        bat = findViewById(R.id.Q01EDBatch)

        inBarcode.setOnClickListener {
            hideKeyboard()
        }

        btnMenu.setOnClickListener {
            try{
                runBlocking {
                    val job = GlobalScope.launch {
                        runOnUiThread(Runnable {
                            mainLayout.addView(returnTableView(getDataFromSapUsingMaterialBatch(mat.text.toString(),bat.text.toString())))
                        })

                    }
                    job.join()
                    hideKeyboard()

                }
            }
            catch (e:Exception){
                triggerAlert("Error",e.message.toString())
            }

        }

        inLoc.setOnClickListener {
            hideKeyboard()
        }

        btnClear.setOnClickListener {
            inLoc.text.clear()
            inBarcode.text.clear()
            mat.text.clear()
            bat.text.clear()
            mainLayout.removeAllViews()
        }

        inBarcode.setOnKeyListener(View.OnKeyListener { view, i, keyEvent ->
            if(i == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_UP
                || i == KeyEvent.KEYCODE_TAB && keyEvent.action == KeyEvent.ACTION_DOWN){
                try {
                    runBlocking {
                        val job = GlobalScope.launch {
                            runOnUiThread(Runnable {
                                mainLayout.addView(returnTableView(getDataFromSapUsingItemBarcode(inBarcode.text.toString())))
                            })

                        }
                        job.join()
                        hideKeyboard()

                    }
                }
                catch (e:Exception){
                    triggerAlert("Error",e.message.toString())
                }

                return@OnKeyListener true
            }
            false
        })

        inLoc.setOnKeyListener(View.OnKeyListener { view, i, keyEvent ->
            if(i == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_UP
                || i == KeyEvent.KEYCODE_TAB && keyEvent.action == KeyEvent.ACTION_DOWN){
                try {
                    runBlocking {
                        val job = GlobalScope.launch {
                            runOnUiThread(Runnable{
                                mainLayout.addView(returnTableView(getDataFromSapUsingLocationBarcode(inLoc.text.toString())))
                            })

                        }
                        job.join()
                        hideKeyboard()
                    }
                }
                catch (e:Exception){
                    triggerAlert("Error",e.message.toString())
                }


                return@OnKeyListener true
            }
            false
        })
    }

    private fun getDataFromSapUsingMaterialBatch(material:String,batch:String):ArrayList<PART_INFO>{
        mainLayout.removeAllViews()
        var tempList:ArrayList<PART_INFO> = arrayListOf()
        var resultArray = JSONArray()
        runBlocking {
            val job = GlobalScope.launch {
                try {
                    resultArray = JSONArray(URL("http://172.16.206.19/REST_API/Second/Q01MaterialAndBatch?material=${material}&batch=${batch}").readText()) as JSONArray
                }
                catch (e:Exception){
                    runOnUiThread(Runnable{
                        triggerAlert("Error",e.message.toString())
                    })

                }
                }
            job.join()
            for (i in 0 until  resultArray.length()){
                tempList.add(PART_INFO(MATERIAL = JSONObject(resultArray[i].toString()).getString("MATERIAL"),
                    BATCH = JSONObject(resultArray[i].toString()).getString("BATCH"),
                    STORAGE_BIN = JSONObject(resultArray[i].toString()).getString("STORAGE_BIN"),
                    QUANTITY = JSONObject(resultArray[i].toString()).getInt("QUANTITY"),
                    STOR_LOC = JSONObject(resultArray[i].toString()).getString("STORAGE_LOCATION"),
                    STOR_TYPE = JSONObject(resultArray[i].toString()).getString("STORAGE_TYPE")))
            }
        }

        return tempList
    }

    private fun getDataFromSapUsingItemBarcode(pBarcode:String):ArrayList<PART_INFO>{
        var translatedResult = barcodeData("","","","","","","")
        var tempList:ArrayList<PART_INFO> = arrayListOf()
        runBlocking {
            val job = GlobalScope.launch {
                var CFs = commonFunctions()
                translatedResult = CFs.translateBarcode(pBarcode,c)
            }
            job.join()
            var resultArray = JSONArray()

                runBlocking {
                    val job = GlobalScope.launch {
                        try {
                            resultArray = JSONArray(URL("http://172.16.206.19/REST_API/Second/Q01MaterialAndBatch?material=${translatedResult.PART_NO}&batch=${translatedResult.LOT}").readText()) as JSONArray
                        }
                        catch (e:Exception){
                            runOnUiThread(Runnable{
                                triggerAlert("Error",e.message.toString())
                            })

                        }
                        }
                    job.join()
                    mat.setText(translatedResult.PART_NO)
                    bat.setText(translatedResult.LOT)
                    for (i in 0 until  resultArray.length()){
                        tempList.add(PART_INFO(MATERIAL = JSONObject(resultArray[i].toString()).getString("MATERIAL"),
                            BATCH = JSONObject(resultArray[i].toString()).getString("BATCH"),
                            STORAGE_BIN = JSONObject(resultArray[i].toString()).getString("STORAGE_BIN"),
                            QUANTITY = JSONObject(resultArray[i].toString()).getInt("QUANTITY"),
                            STOR_LOC = JSONObject(resultArray[i].toString()).getString("STORAGE_LOCATION"),
                            STOR_TYPE = JSONObject(resultArray[i].toString()).getString("STORAGE_TYPE")))
                    }
                }



        }

        return tempList
    }

    private fun getDataFromSapUsingLocationBarcode(pLoc:String):ArrayList<PART_INFO>{
        var link = URL("http://172.16.206.19/REST_API/Second/Q01StorageBin?storageBin=$pLoc")
        var jsonObj = JSONArray()
        var tempList:ArrayList<PART_INFO> = arrayListOf()
        runBlocking {
            val job = GlobalScope.launch {
                try {
                    jsonObj = JSONArray(link.readText())
                }
                catch (e:Exception){
                    runOnUiThread(Runnable{
                        triggerAlert("Error",e.message.toString())
                    })

                }

            }
            job.join()
            for (i in 0 until  jsonObj.length()){
                tempList.add(PART_INFO(MATERIAL = JSONObject(jsonObj[i].toString()).getString("MATERIAL"),
                    BATCH = JSONObject(jsonObj[i].toString()).getString("BATCH"),
                    STORAGE_BIN = JSONObject(jsonObj[i].toString()).getString("STORAGE_BIN"),
                    QUANTITY = JSONObject(jsonObj[i].toString()).getInt("QUANTITY"),
                    STOR_LOC = JSONObject(jsonObj[i].toString()).getString("STORAGE_LOCATION"),
                    STOR_TYPE = JSONObject(jsonObj[i].toString()).getString("STORAGE_TYPE")))
            }
        }



        return tempList
    }

    private fun returnTableView(pData:ArrayList<PART_INFO>):View{
        var tableParam: TableLayout.LayoutParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.WRAP_CONTENT,
            TableLayout.LayoutParams.WRAP_CONTENT
        )
        with(tableParam){
            setMargins(10,10,10,10)

        }

        var dataTable = TableLayout(c)
        dataTable.layoutParams = tableParam
        dataTable.id = View.generateViewId()
        addHeaderToTable(dataTable)
        for(i in 0 until pData.size){
            dataTable.addView(generateRowForTable(pData[i]))
        }
        return dataTable
    }

    private fun addHeaderToTable(pTab:TableLayout){
        var rowParams: TableRow.LayoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT
        )
        with(rowParams) {
            weight = 1F

        }
        val dataTableRow = TableRow(c)
        with(dataTableRow) {
            gravity = Gravity.CENTER
            layoutParams = rowParams
            setBackgroundResource(R.drawable.cell_with_border)
        }
        dataTableRow.addView(generateTextViewForRow("MATERIAL"))
        dataTableRow.addView(generateTextViewForRow("BATCH"))
        dataTableRow.addView(generateTextViewForRow("QUANTITY"))
        dataTableRow.addView(generateTextViewForRow("STORAGE BIN"))
        dataTableRow.addView(generateTextViewForRow("STORAGE LOCATION"))
        dataTableRow.addView(generateTextViewForRow("STORAGE TYPE"))
        pTab.addView(dataTableRow)
    }

    private fun generateRowForTable(pRow:PART_INFO):View{
        var rowParams: TableRow.LayoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT
        )
        with(rowParams) {
            weight = 1F

        }
        val dataTableRow = TableRow(c)
        with(dataTableRow) {
            gravity = Gravity.CENTER
            layoutParams = rowParams
            setBackgroundResource(R.drawable.cell_with_border)
        }
        dataTableRow.addView(generateTextViewForRow(pRow.MATERIAL))
        dataTableRow.addView(generateTextViewForRow(pRow.BATCH))
        dataTableRow.addView(generateTextViewForRow(pRow.QUANTITY.toString()))
        dataTableRow.addView(generateTextViewForRow(pRow.STORAGE_BIN))
        dataTableRow.addView(generateTextViewForRow(pRow.STOR_TYPE))
        dataTableRow.addView(generateTextViewForRow(pRow.STOR_LOC))
        return dataTableRow
    }

    private fun generateTextViewForRow(pText:String):TextView{
        val tv = TextView(c)
        with(tv){
            text = pText
            textSize = 14F
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.MATCH_PARENT
            )
            setBackgroundResource(R.drawable.cell_with_border)
            setPadding(15, 5, 15, 10)
        }
        return tv
    }

    private fun triggerAlert(title:String,msg:String){
        val builder = AlertDialog.Builder(c)
        builder.setTitle(title)
        builder.setMessage(msg)
        builder.setPositiveButton("OK") { dialog, which ->
            // Do something when OK button is clicked
            inLoc.text.clear()
            inBarcode.text.clear()
            mat.text.clear()
            bat.text.clear()
            mainLayout.removeAllViews()
        }
        builder.show()
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

    data class PART_INFO(var MATERIAL:String,
                         var BATCH:String,
                         var STORAGE_BIN:String,
                         var QUANTITY:Int,
                         var STOR_TYPE: String,
                         var STOR_LOC: String)


}