package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.URL

class T07areaB : AppCompatActivity() {

    private lateinit var currentArea:String
    private val INACTIVITY_DELAY = 3600000L // 3600 seconds
    private val handler = Handler(Looper.getMainLooper())
    private var lastInteractionTime = 0L
    private lateinit var titleTV: TextView
    private lateinit var mLayout: LinearLayout
    private lateinit var c: Context
    private lateinit var gBadgeNum:String
    private lateinit var dataOfJsonString:String
    private lateinit var CF: viewFunctions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_t07area_b)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        titleTV = findViewById(R.id.T07areaBTitle)
        currentArea = intent.getStringExtra("Area").toString()
        gBadgeNum = intent.getStringExtra("Badge").toString()
        titleTV.text = "Pull list View for scanner : $currentArea"
        mLayout = findViewById(R.id.T07areaBmainLayout)
        c = this
        dataOfJsonString = String()
        CF = viewFunctions()

        runBlocking {
            val job = GlobalScope.launch {
                try {
                    dataOfJsonString = getPullListData(currentArea)
                }
                catch (e:Exception){
                    Toast.makeText(c, e.message.toString(), Toast.LENGTH_SHORT).show()
                }

            }
            job.join()
            try{
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
            catch (e:Exception){
                Toast.makeText(c, e.message.toString(), Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun getPullListData(area:String):String{
        return URL("http://172.16.206.19/REST_API/SMT_EKANBAN_SERVICES/RetrieveData?trolley=$area").readText()
    }

    private fun BuildTable(data:MutableList<dataRequestor>) {
        var tableParam: TableLayout.LayoutParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.WRAP_CONTENT,
            TableLayout.LayoutParams.WRAP_CONTENT
        )
        var rowParams: TableRow.LayoutParams = TableRow.LayoutParams(
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
            val pullListTV = TextView(c)
            with(pullListTV) {
                text = "Pull List Number : " + data[i].PULLLIST
                textSize = 16F
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
                setPadding(15, 15, 15, 15)
                layoutParams = TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT
                )
                setBackgroundResource(R.drawable.cell_with_border)
            }
            pullListRow.addView(pullListTV)
            mLayout.addView(pullListRow)

            var dataTable = TableLayout(c)
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
                dataTableRow.addView(CF.generateTVforTableT07(data[i].listData[j].MATERIAL +
                        "\nRequester : " + data[i].listData[j].REQUESTOR +
                        "\nMachine Number : " + data[i].listData[j].MACHINE_NO,c))
                dataTableRow.addView(CF.generateTVforTableT07(data[i].listData[j].LOCATION,c))
                dataTableRow.addView(CF.generateTVforTableT07(data[i].listData[j].QUANTITY.toString(),c))
                dataTableRow.addView(CF.createLinearLayout(TFbtn,c))
                dataTable.addView(dataTableRow)
            }
            mLayout.addView(dataTable)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val T07Cls = com.example.myapplication.T07()
        T07Cls.updateUsageArea(currentArea,"0","")
        handler.removeCallbacksAndMessages(null) // Prevent leaks
        finish()
    }
}