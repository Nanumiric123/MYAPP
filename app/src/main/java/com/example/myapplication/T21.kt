package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.w3c.dom.Text
import java.net.URL

class T21 : AppCompatActivity() {
    private lateinit var title:TextView
    private lateinit var inBcode:EditText
    private lateinit var matTV:TextView
    private lateinit var batTV:TextView
    private lateinit var quanTV:EditText
    private lateinit var reelTV:TextView
    private lateinit var fromLoc:EditText
    private lateinit var toLoc:EditText
    private lateinit var btnTransfer:Button
    private lateinit var c:Context
    private var barcodeTranslated = barcodeData("","","","","","","")
    private lateinit var gbadgeNum:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_t21)
        title = findViewById(R.id.T21TVTITLE)
        inBcode = findViewById(R.id.T21EDBARCODE)
        matTV = findViewById(R.id.T21TVMATERIAL)
        batTV = findViewById(R.id.T21TVBATCH)
        quanTV = findViewById(R.id.T21EDQUANTITY)
        reelTV = findViewById(R.id.T21TVREEL)
        fromLoc = findViewById(R.id.T21EDLOCATIONFROM)
        toLoc = findViewById(R.id.T21EDLOCATIONTO)
        btnTransfer = findViewById(R.id.T21BTNTransfer)
        c = this

        title.text = intent.getStringExtra("Desc").toString()
        gbadgeNum = intent.getStringExtra("Badge").toString()
        inBcode.requestFocus()

        inBcode.setOnKeyListener(View.OnKeyListener { view, i, keyEvent ->
            if (i == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_UP
                || i == KeyEvent.KEYCODE_TAB && keyEvent.action == KeyEvent.ACTION_DOWN) {
                if(inBcode.text.isNullOrBlank()){
                    generateDialog("Please Scan 2D barcode First","Error",inBcode)
                }
                else{
                    barcodeTranslated = translateBarcode(inBcode.text.toString())
                    barcodeTranslated
                }
                hideKeyboard()
            return@OnKeyListener true
            }
            false
        })

        fromLoc.setOnKeyListener(View.OnKeyListener { view, i, keyEvent ->
            if(i == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_UP
                || i == KeyEvent.KEYCODE_TAB && keyEvent.action == KeyEvent.ACTION_DOWN){
                if(fromLoc.text.isNullOrBlank()){
                    generateDialog("Please Scan Location barcode First","Error",inBcode)
                }
                else{
                    toLoc.requestFocus()
                }
                hideKeyboard()
                return@OnKeyListener true
            }
            false
        })

        btnTransfer.setOnClickListener {
            if(barcodeTranslated.PART_NO.isNullOrBlank() && barcodeTranslated.LOT.isNullOrBlank()){
                generateDialog("Please Scan 2D barcode First","Error",inBcode)
            }
            else{
                if (toLoc.text.isNullOrBlank() || fromLoc.text.isNullOrBlank()){
                    generateDialog("Please Scan location barcode First","Error",fromLoc)
                }
                else{
                    //send to SAP
                    var res = String()
                    runBlocking {
                        val job = GlobalScope.launch {
                            barcodeTranslated.QUANTITY = quanTV.text.toString()
                            res = sendToSap(fromLoc.text.toString(),toLoc.text.toString(),barcodeTranslated.PART_NO,barcodeTranslated.LOT,barcodeTranslated.REEL_NO,barcodeTranslated.QUANTITY,gbadgeNum)

                        }
                        job.join()
                        if(res.split('$')[0].contains('S')){
                            generateDialog(res.split('$')[1].replace("\"",""),res.split('$')[0].replace("\"",""),inBcode)
                            fromLoc.text.clear()
                            toLoc.text.clear()
                            setTextView("","","","")
                            barcodeTranslated = barcodeData("","","","","","","")
                        }
                        else{
                            generateDialog(res.split('$')[1].replace("\"",""),res.split('$')[0].replace("\"",""),inBcode)
                        }

                    }


                }
            }
            hideKeyboard()
        }

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

    private fun sendToSap(fLoc:String,tLoc:String,mat:String,bat:String,reelN:String,q:String,badge:String):String{
        val modelName = Build.MODEL
        var link = URL("http://172.16.206.19/REST_API/SMT_EKANBAN_SERVICES/MPPT21?fromLocation=${fLoc}&toLocation=${tLoc}&mat=${mat}&batch=${bat}&reelNum=${reelN}&quantity=${q}&badgeNumber=${badge}&deviceNo=${modelName}")
        return link.readText()
    }

    private fun translateBarcode(bc:String):barcodeData{
        var tempItem = barcodeData("","","","","","","")
        try{
            runBlocking {
                val job = GlobalScope.launch {
                    val linkUrl = URL(getString(R.string.barcodeTranslator,bc))
                    val jsonOBJ = JSONObject(linkUrl.readText())
                    tempItem = barcodeData(jsonOBJ.getString("VENDOR"),jsonOBJ.getString("DATE")
                        ,jsonOBJ.getString("PART_NO"),jsonOBJ.getString("REEL_NO"),jsonOBJ.getString("LOT"),
                        jsonOBJ.getString("QUANTITY"),jsonOBJ.getString("UOM"))
                }
                job.join()
                fromLoc.requestFocus()
                setTextView(tempItem.PART_NO,tempItem.LOT,tempItem.QUANTITY,tempItem.REEL_NO)
            }




        }
        catch (ex:Exception){
            runOnUiThread(Runnable {
                Toast.makeText(c, ex.message.toString(), Toast.LENGTH_SHORT).show()
            })

            inBcode.requestFocus()
        }
        finally {
            inBcode.text.clear()
            hideKeyboard()
            return tempItem
        }

    }

    private fun generateDialog(msg:String,title:String,v:EditText){
        hideKeyboard()
        runOnUiThread(Runnable {
            val builder = AlertDialog.Builder(c)
            builder.setTitle(title)
            builder.setMessage(msg)
            builder.setPositiveButton("OK") { dialog, which ->
                // Do something when OK button is clicked
                v.requestFocus()
                v.text.clear()
            }
            builder.show()
        })
    }

    private fun setTextView(mat:String,bat:String,qty:String,reel:String){
        matTV.text = "${getString(R.string.matterialTextview_description)}$mat"
        batTV.text = "${getString(R.string.BatchLabel)}$bat"
        quanTV.setText(qty)
        reelTV.text = "${getString(R.string.ReelNum)}$reel"
    }

}