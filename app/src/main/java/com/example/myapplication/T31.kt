package com.example.myapplication

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.Settings.Global
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.MultiAutoCompleteTextView
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import kotlin.properties.Delegates

class T31 : AppCompatActivity() {
    private lateinit var pb: ProgressBar
    private lateinit var btnMenu: Button
    private lateinit var btnClear: Button
    private lateinit var btnSave: Button
    private lateinit var edBarcode: EditText
    private lateinit var edPart: EditText
    private lateinit var edWHKBPart: EditText
    private lateinit var edSliderPart: EditText
    private lateinit var edQty: EditText
    private lateinit var edLotNum: EditText
    private lateinit var edFromStorTyp: EditText
    private lateinit var edFromLoc: EditText
    private lateinit var edToLoc: EditText
    private lateinit var edToStorTyp: EditText
    private lateinit var c: Context
    private lateinit var  materialsLists:MutableList<T32.BININFO>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_t31)
        btnMenu = findViewById(R.id.T31BtnMenu)
        btnClear = findViewById(R.id.T31BtnClear)
        btnSave = findViewById(R.id.T31BtnSave)
        pb = findViewById(R.id.T31PB)
        edBarcode = findViewById(R.id.T31EDItemBarcode)
        edPart = findViewById(R.id.T31EDPart)
        edQty = findViewById(R.id.T31EDQty)
        edWHKBPart = findViewById(R.id.T31EDWHKBPart)
        edSliderPart = findViewById(R.id.T31EDPartOnSlider)
        edToLoc = findViewById(R.id.T31EDToLoc)
        edLotNum = findViewById(R.id.T31EDLotNo)
        edFromStorTyp = findViewById(R.id.T31EDfromStorageType)
        edFromLoc = findViewById(R.id.T31EDfromLoc)
        edToStorTyp = findViewById(R.id.T31EDToStorTyp)
        c = this@T31
        edFromStorTyp.setText("002")
        edToStorTyp.setText("005")
        materialsLists = mutableListOf()
        val Bnum = intent.getStringExtra("Badge").toString()

        edBarcode.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                progressbarSetting(pb)

                runBlocking {

                    val job = GlobalScope.launch {
                        try{
                            materialsLists = getDataFromBin(edBarcode.text.toString())
                        }
                        catch (e:Exception){

                            runOnUiThread{
                                triggerAlert("Error",e.message.toString())
                            }
                            progressbarSetting(pb)
                        }

                    }
                    job.join()
                    if(materialsLists.size > 0){
                        edToLoc.setText(edBarcode.text.toString())
                        edFromLoc.setText(edBarcode.text.toString())
                        edPart.setText(materialsLists[0].MATERIAL)
                        var totalQty = 0
                        for (i in 0 until materialsLists.size){
                            totalQty += materialsLists[i].QUANTITY
                        }
                        edQty.setText(totalQty.toString())


                        edWHKBPart.requestFocus()
                    }
                    else{
                        triggerAlert("Error","No materials in Bin")
                    }
                }
                return@OnKeyListener true
            }
            false
        })

        edWHKBPart.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                if(edWHKBPart.text.toString() == edPart.text.toString()){
                    edSliderPart.requestFocus()
                }
                else{

                    triggerAlert("Error","Wrong Kanban")
                }


                return@OnKeyListener true
            }
            false
        })

        edSliderPart.setOnKeyListener(View.OnKeyListener { view, i, keyEvent ->
            if(i == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_DOWN ||
                i == KeyEvent.KEYCODE_TAB && keyEvent.action == KeyEvent.ACTION_DOWN){

                if(edWHKBPart.text.toString() == edSliderPart.text.toString()){
                    edToLoc.requestFocus()
                }
                else {
                    triggerAlert("Error","Wrong Slider")
                }

                return@OnKeyListener true
            }
                false
        })

        btnMenu.setOnClickListener {
            this.finish()
        }
        btnClear.setOnClickListener {
            clearEverything()
        }
        btnSave.setOnClickListener {
            var deviceID = Build.ID
            var submitResult:MutableList<List<String>> = mutableListOf()
            progressbarSetting(pb)
            runBlocking {
                val job = GlobalScope.launch {
                    for (materialsList in materialsLists) {
                        submitResult.add(submitData(materialsList.MATERIAL,materialsList.BATCH,materialsList.QUANTITY.toString(),
                            edToLoc.text.toString(),Bnum,deviceID))
                    }

                }
                job.join()
                progressbarSetting(pb)
                var finalTitle = String()
                var finalMessage = String()
                for(submitres in submitResult){
                    finalTitle += submitres[0] + ","
                    finalMessage += submitres[1] + "\n"
                }
                triggerAlert(finalTitle,finalMessage)

            }

        }
    }

    private suspend fun getDataFromBin(binNumber:String):MutableList<T32.BININFO> {
        val url = "http://172.16.206.19/REST_API/Third/MPPget_material_fromBin?storageBin=${binNumber}"

        var resList:MutableList<T32.BININFO> = mutableListOf()

        withContext(Dispatchers.IO){
            var result = URL(url).readText()
            var materialArray = JSONArray(result)
            for(i in 0 until materialArray.length()){
                var tempObj = materialArray.getJSONObject(i)
                var res = T32.BININFO(MATERIAL = "", BATCH = "", STORAGE_BIN = "", QUANTITY = 0)
                res.MATERIAL = tempObj.getString("MATERIAL")
                res.BATCH = tempObj.getString("BATCH")
                res.STORAGE_BIN = tempObj.getString("STORAGE_BIN")
                res.QUANTITY = tempObj.getInt("QUANTITY")
                resList.add(res)
            }
        }
        progressbarSetting(pb)

        return resList
    }

    private suspend fun submitData(materialNum:String,batchNum:String,transferQty:String,binNumber:String,badgeNum:String,deviceImei:String):List<String>{

        val urlString = "http://172.16.206.19/REST_API/Third/MPPSubmitT31?material=${materialNum}&storageBin=${binNumber}&" +
                "qty=${transferQty}&badgeno=${badgeNum}&batch=${batchNum}&deviceID=${deviceImei}"
        var result = String()
        withContext(Dispatchers.IO){
            result = URL(urlString).readText()
        }

        return result.split(':')
    }

    private fun triggerAlert(title:String,msg:String){
        val builder = AlertDialog.Builder(c)
        builder.setTitle(title)
        builder.setMessage(msg)
        builder.setPositiveButton("OK") { dialog, which ->
            // Do something when OK button is clicked
            clearEverything()
            edBarcode.requestFocus()
        }
        builder.show()
    }

    private fun clearEverything(){
        edBarcode.text.clear()
        edPart.text.clear()
        edWHKBPart.text.clear()
        edSliderPart.text.clear()
        edQty.text.clear()
        edLotNum.text.clear()
        edFromStorTyp.text.clear()
        edFromLoc.text.clear()
        edToLoc.text.clear()
        edToStorTyp.text.clear()
        materialsLists.clear()
        edFromStorTyp.setText("002")
        edToStorTyp.setText("005")
    }
    private fun progressbarSetting(v: View) {
        runOnUiThread(kotlinx.coroutines.Runnable {
            if (v.visibility == View.VISIBLE) {
                v.visibility = View.GONE
            } else {
                v.visibility = View.VISIBLE
            }

        })
    }

}