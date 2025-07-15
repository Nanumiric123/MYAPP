package com.example.myapplication

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.Toast
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URL

class commonFunctions {
    var rowParams: TableRow.LayoutParams = TableRow.LayoutParams(
        TableRow.LayoutParams.MATCH_PARENT,
        TableRow.LayoutParams.MATCH_PARENT
    )
    fun createLinearLayout(btn: EditText, c:Context): LinearLayout {
        val layoutBtn = LinearLayout(c)
        with(layoutBtn) {
            id = View.generateViewId()
            layoutParams = rowParams
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.cell_with_border)
            setPadding(15, 15, 15, 15)
        }
        layoutBtn.addView(btn)
        return layoutBtn
    }

    fun generateRow(c:Context):TableRow{
        val row = TableRow(c)
        with(rowParams){
            weight = 1F
        }
        row.layoutParams = TableRow.LayoutParams(rowParams)
        row.id = View.generateViewId()

        return row

    }

    fun generateTVforRow(ptxt:String,c: Context): View {
        val generateTV = TextView(c)
        with(generateTV) {
            id = ViewCompat.generateViewId()
            text = ptxt
            textSize = 16F
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(10, 10, 10, 10)
            layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.MATCH_PARENT
            )
            setBackgroundResource(R.drawable.cell_with_border)
        }
        return generateTV
    }

     fun translateJsonStringToList(datafromDB:String):MutableList<dataRequestor>{
         var finalResult:MutableList<dataRequestor> = mutableListOf()
         try {
             var arrObj = JSONTokener(datafromDB).nextValue() as JSONArray

             for(i in 0 until arrObj.length()) {
                 var pullListNumber = arrObj.getJSONObject(i).getString("PULLLIST")
                 var objectList = arrObj.getJSONObject(i).getJSONArray("List")
                 var tempList:MutableList<listData> = mutableListOf()
                 for (j in 0 until objectList.length() ) {
                     var OBJlIST = JSONTokener(objectList[j].toString()).nextValue() as JSONObject
                     var temp = listData(ID = OBJlIST.getInt("ID"),
                         MATERIAL = OBJlIST.getString("MATERIAL"),
                         LOCATION = OBJlIST.getString("LOCATION"),
                         QUANTITY = OBJlIST.getInt("QUANTITY"),
                         REQUESTOR = OBJlIST.getString("REQUESTOR"),
                         PULLLISTNUMBER = OBJlIST.getString("PULLLIST_NUMBER"),
                         MACHINE_NO = OBJlIST.getString("MACHINE_NO")
                     )
                     tempList.add(temp)
                 }
                 finalResult.add(dataRequestor(PULLLIST = pullListNumber, listData = tempList))
             }
         }
         catch (e:Exception){

         }



        return finalResult
    }

    fun translateBarcode(bc:String,c:Context): barcodeData {
        var tempItem = barcodeData("","","","","","","")


        try{
            val linkUrl = URL(
                c.getString(
                    R.string.http_172_16_206_19_rest_api_smt_ekanban_services_breakbarcodetoarray_barcode,
                    bc
                ))
            val jsonOBJ = JSONObject(linkUrl.readText())

            tempItem = barcodeData(jsonOBJ.getString("VENDOR"),jsonOBJ.getString("DATE")
                ,jsonOBJ.getString("PART_NO"),jsonOBJ.getString("REEL_NO"),jsonOBJ.getString("LOT"),
                jsonOBJ.getString("QUANTITY"),jsonOBJ.getString("UOM"))

        }
        catch (ex:Exception){
            Toast.makeText(c, ex.message.toString(), Toast.LENGTH_SHORT).show()
        }
        finally{

            return tempItem
        }

    }

    public fun retrieveUsageList(area:String):AREAUSAGE{

        var usage = ""
        var result = ""
        var badge = ""
        lateinit var jasonObj:JSONObject
        runBlocking<Unit> {
            val job = GlobalScope.launch {
                val urlLink = URL("http://172.16.206.19/REST_API/SMT_EKANBAN_SERVICES/GetAreaUsage?area=${area}")
                result = urlLink.readText()
                jasonObj = JSONObject(result)
            }
            job.join()
            //var
            //area = jasonObj.getString("AREA")
            usage = jasonObj.getString("USAGE")
            badge = jasonObj.getString("BADGENUM")
        }


        return AREAUSAGE(
            AREA = area,
            USAGE = usage,
            BADGE = badge
        )
    }

    fun showMessage(context: Context, title: String, message: String,positiveButtonText: String,positiveButtonAction: () -> Unit):AlertDialog{
        return AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { dialog, which ->
                positiveButtonAction()
            }
            .create()
    }
    // Function to show a dialog box
    fun showDialog(context: Context, title: String, message: String,positiveButtonText: String,
                   negativeButtonText: String,
                   positiveButtonAction: () -> Unit,
                   negativeButtonAction: () -> Unit):AlertDialog {
        return AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { dialog, which ->
                positiveButtonAction()
            }
            .setNegativeButton(negativeButtonText) { dialog, which ->
                negativeButtonAction()
            }
            .create()

    }

}
data class palletLabel(var MATERIAL:String,var PALLET_SEQUANCE:String,var LOT_NO:String,var QUANTITY:String,var PALLET_ID:String,var CARTON_QTY:String)
