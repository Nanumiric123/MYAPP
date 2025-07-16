package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.javiersantos.appupdater.DisableClickListener
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.ExceptionLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class T07_QTF : AppCompatActivity() {
    private lateinit var currentArea:String
    private lateinit var c: Context
    private lateinit var badgeNum: String
    private lateinit var titleTV: TextView
    private lateinit var gBadgeNum:String
    private lateinit var dataOfJsonString:String
    private lateinit var CF: viewFunctions
    private lateinit var mLayout: LinearLayout
    data class SAP_DATA(
        var MATERIAL: String,
        var BATCH: String,
        var QUANTITY: String,
        var REEL_NO: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_t07_qtf)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        titleTV = findViewById(R.id.T07QTFTITLE)
        badgeNum = intent.getStringExtra("Badge").toString()
        c = this@T07_QTF
        CF = viewFunctions()
        currentArea = intent.getStringExtra("Area").toString()
        titleTV.text = "Pull list View for scanner : $currentArea"
        gBadgeNum = intent.getStringExtra("Badge").toString()
        mLayout = findViewById(R.id.T07QTFMAINLAYOUT)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                dataOfJsonString = getPullListData(currentArea)
            }
            catch (e:Exception){
                Toast.makeText(c, e.message.toString(), Toast.LENGTH_SHORT).show()
            }
            var sData = commonFunctions().translateJsonStringToList(dataOfJsonString)
            if(sData.size > 0){

                try {
                    BuildTable(sData)
                }
                catch (e:Exception){
                    Toast.makeText(c, e.message.toString(), Toast.LENGTH_SHORT).show()
                }

            }
        }
    }
    private suspend fun getPullListData(area:String):String{
        return withContext(Dispatchers.IO){
             URL("http://172.16.206.19/REST_API/SMT_EKANBAN_SERVICES/RetrieveData?trolley=$area").readText()
        }
    }
    private fun BuildTable(data:MutableList<dataRequestor>) {
        val tableParam: TableLayout.LayoutParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.WRAP_CONTENT,
            TableLayout.LayoutParams.WRAP_CONTENT
        )
        val rowParams: TableRow.LayoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT
        )
        with(rowParams) {
            weight = 1F

        }
        for (i in 0 until data.size) {
            val pullListRow = TableRow(c)
            with(pullListRow) {
                gravity = Gravity.CENTER
                layoutParams = TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT
                )
                setBackgroundResource(R.drawable.cell_with_border)
            }
            pullListRow.addView(CF.generateTVforpulllistT07("Pull List Number : " + data[i].PULLLIST,c))
            mLayout.addView(pullListRow)

            val dataTable = TableLayout(c)
            dataTable.layoutParams = tableParam
            for (j in 0 until data[i].listData.size) {
                val dataTableRow = TableRow(c)
                with(dataTableRow) {
                    gravity = Gravity.CENTER
                    layoutParams = rowParams
                    setBackgroundResource(R.drawable.cell_with_border)
                }

                val TFbtn = Button(c)
                with(TFbtn) {
                    setText("Transfer")
                    setBackgroundColor(Color.GRAY)
                    setTextColor(Color.WHITE)
                    setBackgroundResource(R.drawable.button_draw)
                    textSize = 16F
                    gravity = Gravity.CENTER
                    layoutParams = TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT,
                        TableRow.LayoutParams.WRAP_CONTENT
                    )
                }
                TFbtn.setOnClickListener {
                    val newActivityIntent = Intent(c,T07TransferMaterial::class.java)
                    newActivityIntent.putExtra("ID",data[i].listData[j].ID)
                    newActivityIntent.putExtra("Material",data[i].listData[j].MATERIAL)
                    newActivityIntent.putExtra("Location",data[i].listData[j].LOCATION)
                    newActivityIntent.putExtra("Quantity",data[i].listData[j].QUANTITY.toString())
                    newActivityIntent.putExtra("MachineNum",data[i].listData[j].MACHINE_NO)
                    newActivityIntent.putExtra("Requestor",data[i].listData[j].REQUESTOR)
                    newActivityIntent.putExtra("Area",currentArea)
                    newActivityIntent.putExtra("Badge",gBadgeNum)
                    newActivityIntent.putExtra("dataFromDB",dataOfJsonString)
                    c.startActivity(newActivityIntent)
                    val intent = Intent()
                    setResult(RESULT_OK, intent)
                    finish()
                }

                dataTableRow.addView(CF.generateTVforTableT07((j + 1).toString(),c))
                var dataTVRow = CF.generateTVforTableT07(data[i].listData[j].MATERIAL +
                        "\nRequester : " + data[i].listData[j].REQUESTOR +
                        "\nMachine Number : " + data[i].listData[j].MACHINE_NO,c)
                dataTVRow.layoutParams =  TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT,
                    TableRow.LayoutParams.WRAP_CONTENT)
                dataTableRow.addView(dataTVRow)
                dataTableRow.addView(CF.generateTVforTableT07(data[i].listData[j].LOCATION,c))
                dataTableRow.addView(CF.generateTVforTableT07(data[i].listData[j].QUANTITY.toString(),c))
                dataTableRow.addView(CF.createLinearLayout(TFbtn,c))
                dataTable.addView(dataTableRow)
            }
            mLayout.addView(dataTable)

        }
    }
}