package com.example.myapplication

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URL
import kotlin.properties.Delegates


class P04 : AppCompatActivity() {

    var btnMenu: Button by Delegates.notNull<Button>()
    var btnClear: Button by Delegates.notNull<Button>()
    var btnSave: Button by Delegates.notNull<Button>()
    var etBcode: EditText by Delegates.notNull<EditText>()
    var tvPartNum: TextView by Delegates.notNull<TextView>()
    var tvBatch: TextView by Delegates.notNull<TextView>()
    var etQty: EditText by Delegates.notNull<EditText>()
    var etFromSloc: EditText by Delegates.notNull<EditText>()
    var etToSloc: EditText by Delegates.notNull<EditText>()
    var binFromSloc: EditText by Delegates.notNull<EditText>()
    var binToSloc: EditText by Delegates.notNull<EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_p04)

        btnMenu = findViewById(R.id.p04_menu)
        btnClear = findViewById(R.id.p04_clear)
        btnSave = findViewById(R.id.p04_save)
        etQty = findViewById(R.id.p04_qty_input)
        etFromSloc = findViewById(R.id.P04_in_from_loc)
        etToSloc = findViewById(R.id.P04_in_to_loc)
        binFromSloc=findViewById(R.id.p04_fromBin_in)
        binToSloc = findViewById(R.id.p04_toBin_in)
        tvPartNum = findViewById(R.id.p04_tv_material)
        tvBatch = findViewById(R.id.p04_batch)

        etFromSloc.requestFocus()
        btnMenu.setOnClickListener {
            this.finish()
        }
        btnClear.setOnClickListener {
            clearAll()
        }

        val progressBar: ProgressBar = findViewById(R.id.p04_pb)
        val c:Context = this
        btnSave.setOnClickListener {
            val fromLocS:String = etFromSloc.text.toString()
            if(fromLocS == ""){
                runOnUiThread(kotlinx.coroutines.Runnable {
                    val alertDialogBuilder = AlertDialog.Builder(this)
                    alertDialogBuilder.setTitle(R.string.ErrorTitle)
                    alertDialogBuilder.setMessage(R.string.ErrorMessage1)
                    alertDialogBuilder.show()

                })
            }
            else{
                //Submit_Data(this)
                runBlocking {
                    GlobalScope.launch {
                        progressbarSetting(progressBar)
                        saveDoc(c)
                        progressbarSetting(progressBar)

                    }
                }


            }

        }

        etFromSloc.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                binFromSloc.requestFocus()
                return@OnKeyListener true
            }
            false
        })

        binFromSloc.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                getDetailFromBin(etFromSloc.text.toString().trim(),binFromSloc.text.toString().trim(),this)
                binToSloc.text = binFromSloc.text
                etToSloc.requestFocus()
                return@OnKeyListener true
            }
            false
        })

        etToSloc.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                etQty.requestFocus()

                return@OnKeyListener true
            }
            false
        })

        etQty.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                btnSave.requestFocus()

                return@OnKeyListener true
            }
            false
        })

    }

    private fun progressbarSetting(v: View){
        runOnUiThread(kotlinx.coroutines.Runnable {
            if (v.visibility == View.VISIBLE) {
                v.visibility = View.GONE
            } else {
                v.visibility = View.VISIBLE
            }

        })
    }

    private fun clearAll(){
        tvPartNum.text = ""
        tvBatch.text = ""
        etQty.text.clear()
        etFromSloc.text.clear()
        etToSloc.text.clear()
        binFromSloc.text.clear()
        binToSloc.text.clear()
    }

    private fun getDetailFromBin(storLoc:String,binNum:String,con: Context) = runBlocking{
        val progressBar: ProgressBar = findViewById(R.id.p04_pb)
        progressbarSetting(progressBar)
        var material_string:String by Delegates.notNull<String>()
        var batch_string:String by Delegates.notNull<String>()
        val first_job = GlobalScope.launch {

            val url = URL("http://172.16.206.19/REST_API/Home/MPP_GET_BIN_DETAILS?str_loc=$storLoc&bin_num=$binNum")
            try{
                val RESTData =url.readText();
                val RESTobj = JSONTokener(RESTData).nextValue() as JSONArray
                material_string = RESTobj.getJSONObject(0).getString("MATERIAL")
                batch_string = RESTobj.getJSONObject(0).getString("BATCH").toString()

            }
            catch (e: Exception){
                runOnUiThread(kotlinx.coroutines.Runnable {
                    val alertDialogBuilder = AlertDialog.Builder(con)
                    alertDialogBuilder.setTitle(R.string.ErrorTitle)
                    alertDialogBuilder.setMessage(e.message.toString())
                    alertDialogBuilder.show()

                })
            }
        }
        first_job.join()
        progressbarSetting(progressBar)
        tvBatch.setText(batch_string)
        tvPartNum.setText(material_string)
    }

    private fun Submit_Data(con:Context) = runBlocking {
        val progressBar: ProgressBar = findViewById(R.id.p04_pb)
        val first_job = GlobalScope.launch {
            progressbarSetting(progressBar)
        }

        first_job.join()



    }

    suspend fun saveDoc(con:Context) = withContext(Dispatchers.IO){
        val materialNum:String = tvPartNum.text.toString().trim()
        val grQty:String = etQty.text.toString().trim()
        val b_num = intent.getStringExtra(MainActivity.EXTRA_MESSAGE)
        val fromSloc:String = etFromSloc.text.toString().trim()
        val toSloc:String = etToSloc.text.toString().trim()
        val batchNum:String = tvBatch.text.toString().trim()
        val fromBin:String = binFromSloc.text.toString().trim()
        val toBin:String = binToSloc.text.toString().trim()
        val devName = Settings.Global.getString(getContentResolver(), Settings.Global.DEVICE_NAME)
        val url = URL("http://172.16.206.19/REST_API/Home/MPP_P04?mat=$materialNum&batc=$batchNum&qty=$grQty&badge_n=$b_num&from_loc=$fromSloc&to_loc=$toSloc&assetNo=$devName&toBin=$toBin&fromBin=$fromBin")

        try {
            val RESTData =url.readText()
            val resresult = RESTData.trim()
            runOnUiThread(kotlinx.coroutines.Runnable {
                val alertDialogBuilder = AlertDialog.Builder(con)
                alertDialogBuilder.setTitle("System Message")
                alertDialogBuilder.setMessage(resresult)
                alertDialogBuilder.show()
            })
        }
        catch (e: Exception) {
            runOnUiThread(kotlinx.coroutines.Runnable {
                val alertDialogBuilder = AlertDialog.Builder(con)

                alertDialogBuilder.setTitle("Error When Saving!")

                alertDialogBuilder.setMessage(e.message.toString())
                alertDialogBuilder.show()
            })

        }

        btnClear.performClick()
        etFromSloc.requestFocus()
    }

}