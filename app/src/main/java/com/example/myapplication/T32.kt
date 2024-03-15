package com.example.myapplication

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.gson.Gson
import kotlinx.coroutines.*
import org.json.JSONObject
import org.json.JSONTokener
import org.w3c.dom.Text
import retrofit2.http.Url
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.DecimalFormat
import kotlin.properties.Delegates


class T32 : AppCompatActivity() {

    private var pbT32:ProgressBar by Delegates.notNull<ProgressBar>()
    private var c:Context by Delegates.notNull<Context>()
    private var edT32StoreKanban:EditText by Delegates.notNull<EditText>()

    private var gMaterial:String by Delegates.notNull<String>()
    private var gquantity:String by Delegates.notNull<String>()
    private var btnSave:Button by Delegates.notNull<Button>()
    private var btnClr:Button by Delegates.notNull<Button>()
    private var btnMenu:Button by Delegates.notNull<Button>()
    private lateinit var bNum:String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_t32)
        var inT32_StorageBin:EditText = findViewById(R.id.in_T32StorageBin)
        edT32StoreKanban = findViewById(R.id.ed_T32StoreKanban)
        bNum = intent.getStringExtra("Badge").toString()
        var edT32ProdKanban:EditText = findViewById(R.id.in_T32ProdKanban)
        btnSave = findViewById(R.id.btnT32Save)
        btnClr = findViewById(R.id.btnT32Clear)
        inT32_StorageBin.requestFocus()
        pbT32 = findViewById(R.id.t32_pb)
        btnMenu = findViewById(R.id.btnT32Menu)
        clearAll()
        c = this
        inT32_StorageBin.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code

                edT32StoreKanban.requestFocus()

                return@OnKeyListener true
            }
            false
        })

        edT32StoreKanban.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                getMaterialsFromBin(inT32_StorageBin.text.toString())
                edT32ProdKanban.requestFocus()
                return@OnKeyListener true
            }
            false
        })

        edT32ProdKanban.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                checkWithProdKanban(edT32ProdKanban.text.toString())

                return@OnKeyListener true
            }
            false
        })

        btnClr.setOnClickListener {
            clearAll()
        }

        btnSave.setOnClickListener {

            SaveDataRest(edT32ProdKanban.text.toString())
            inT32_StorageBin.requestFocus()
        }

        btnMenu.setOnClickListener {

        }

    }
    private fun clearInputs() {
       var inT32_StorageBin:EditText = findViewById(R.id.in_T32StorageBin)
       var edT32ProdKanban:EditText = findViewById(R.id.in_T32ProdKanban)
        inT32_StorageBin.text.clear()
        edT32StoreKanban.text.clear()
        edT32ProdKanban.text.clear()

    }

    private fun clearAll() {
        var inT32_StorageBin:EditText = findViewById(R.id.in_T32StorageBin)
        var edT32ProdKanban:EditText = findViewById(R.id.in_T32ProdKanban)
        gMaterial = ""
        gquantity = ""
        inT32_StorageBin.text.clear()
        edT32StoreKanban.text.clear()
        edT32ProdKanban.text.clear()
        var tvT32Material:TextView = findViewById(R.id.tvT32_Material)
        var tvT32Quantity:TextView = findViewById(R.id.tvT32_Quantity)
            tvT32Material.text = "Material :"
            tvT32Quantity.text = "Quantity :"


    }


    private fun progressbar_setting(v:View){
        if (v.visibility == View.VISIBLE) {
            v.visibility = View.GONE
        }
        else {
            v.visibility = View.VISIBLE
        }
    }

    private fun SaveDataRest(prodKanban: String) = runBlocking {
        var job = GlobalScope.launch(Dispatchers.IO) {
            try{
                var inT32_StorageBin:EditText = findViewById(R.id.in_T32StorageBin)
                runOnUiThread(Runnable {

                    progressbar_setting(pbT32)
                })

                var intent: Intent = intent
                var stBin = inT32_StorageBin.text.toString()
                var loc = prodKanban.split('$')[1]
                var mat:String = ""
                if(gMaterial.contains('+'))
                {
                    mat = gMaterial.replace("+","%2B")
                }
                else{
                    mat = gMaterial
                }

                var urlLink: String = "http://172.16.206.19/REST_API/Second/MPPsubmitToSAPT32?binNo=${stBin}&kanbanMaterial=${mat}&location=${loc}&badgeNum=${bNum}"
                var url:URL = URL(urlLink)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val inputStream = connection.inputStream
                val response = inputStream.bufferedReader().use { it.readText() }

                runOnUiThread(Runnable {
                    val alertDialogBuilder = AlertDialog.Builder(c)
                    alertDialogBuilder.setTitle(response)
                    alertDialogBuilder.setMessage(response)
                    alertDialogBuilder.show()

                })
            }
            catch (e: Exception) {

            }
            finally{

                runOnUiThread(Runnable {

                    progressbar_setting(pbT32)
                })
            }
        }
        job.join()
        clearAll()
    }



    private fun checkWithProdKanban(prodKanban:String){
        var result = prodKanban.split('$')
        if(result[0] == edT32StoreKanban.text.toString()){
            if(result[0] == gMaterial) {

            }
            else {
                runOnUiThread(Runnable {
                    val alertDialogBuilder = AlertDialog.Builder(c)
                    alertDialogBuilder.setTitle(R.string.ErrorTitle)
                    alertDialogBuilder.setMessage("Wrong Bin ")
                    alertDialogBuilder.show()
                    clearInputs()
                })
            }
        }
        else{
            runOnUiThread(Runnable {
                val alertDialogBuilder = AlertDialog.Builder(c)
                alertDialogBuilder.setTitle(R.string.ErrorTitle)
                alertDialogBuilder.setMessage("Wrong Store Kanban")
                alertDialogBuilder.show()
                clearInputs()
            })
        }
    }

    private fun getMaterialsFromBin(Bin:String) = runBlocking{
        var tvT32Material:TextView = findViewById(R.id.tvT32_Material)
        var tvT32Quantity:TextView = findViewById(R.id.tvT32_Quantity)
        var inT32_StorageBin:EditText = findViewById(R.id.in_T32StorageBin)
        var q:Int = 0
        var mat:String = ""

        val job = GlobalScope.launch(Dispatchers.IO) {
            // code to run in background
            runOnUiThread(Runnable {

                progressbar_setting(pbT32)
            })
            val url = URL("http://172.16.206.19/REST_API/Second/MPPget_material_info?storageBin=$Bin")
            var res:BININFO by Delegates.notNull<BININFO>()
            var match:Boolean  = false

            try{
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val inputStream = connection.inputStream
                val response = inputStream.bufferedReader().use { it.readText() }
                val gson = Gson()
                val dataFromBin = gson.fromJson(response, Array<BININFO>::class.java)

                if(dataFromBin.size > 1){

                    for( i in dataFromBin) {
                        if(i.MATERIAL == edT32StoreKanban.text.toString()){
                            match = true
                            break
                        }

                    }

                    for( i in dataFromBin){
                        if(match){
                            if(i.MATERIAL != edT32StoreKanban.text.toString()){
                                runOnUiThread(Runnable {
                                    val alertDialogBuilder = AlertDialog.Builder(c)
                                    alertDialogBuilder.setTitle(R.string.ErrorTitle)
                                    alertDialogBuilder.setMessage(i.MATERIAL + " Also exist in this bin")
                                    alertDialogBuilder.show()

                                })
                            }
                            else{
                                q += i.QUANTITY

                                mat = i.MATERIAL
                            }
                        }
                        else {
                            runOnUiThread(Runnable {
                                val alertDialogBuilder = AlertDialog.Builder(c)
                                alertDialogBuilder.setTitle(R.string.ErrorTitle)
                                alertDialogBuilder.setMessage("Wrong kanban")
                                alertDialogBuilder.show()

                            })
                            clearInputs()
                        }

                    }

                }
                else {
                    if(dataFromBin.size <= 0){
                        runOnUiThread(Runnable {

                            val alertDialogBuilder = AlertDialog.Builder(c)
                            alertDialogBuilder.setTitle(R.string.ErrorTitle)
                            alertDialogBuilder.setMessage("Takdak barang dalam BIN ni!")
                            alertDialogBuilder.show()
                            inT32_StorageBin.requestFocus()
                        })
                        clearAll()
                    }
                    else{
                        mat = dataFromBin[0].MATERIAL
                        q = dataFromBin[0].QUANTITY
                    }

                }

                if(edT32StoreKanban.text.toString() == mat){
                    gMaterial = mat
                    gquantity = q.toString()



                }
                else {
                    runOnUiThread(Runnable {
                        val alertDialogBuilder = AlertDialog.Builder(c)
                        alertDialogBuilder.setTitle(R.string.ErrorTitle)
                        alertDialogBuilder.setMessage("Bin with Kanban Not Same")
                        alertDialogBuilder.show()
                        inT32_StorageBin.requestFocus()
                    })
                    clearAll()
                }

            } catch (e: Exception){
                runOnUiThread(Runnable {
                    val alertDialogBuilder = AlertDialog.Builder(c)
                    alertDialogBuilder.setTitle(R.string.ErrorTitle)
                    alertDialogBuilder.setMessage(e.message.toString())
                    alertDialogBuilder.show()

                })
            }
            finally{

            }


        }
        job.join()
        runOnUiThread(Runnable {

            progressbar_setting(pbT32)
        })
        tvT32Material.text = "Material :$mat"
        tvT32Quantity.text = "Quantity :$q"

    }
    data class BININFO(var MATERIAL:String,var BATCH:String, var STORAGE_BIN:String,var QUANTITY:Int)
}

