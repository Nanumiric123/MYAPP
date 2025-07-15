package com.example.myapplication

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.*
import kotlinx.coroutines.Runnable
import org.json.JSONArray
import org.json.JSONTokener
import java.net.URL
import kotlin.properties.Delegates


class P08 : AppCompatActivity() {
    var btnClear: Button by Delegates.notNull<Button>()
    var btnSave: Button by Delegates.notNull<Button>()
    var etFromLoc: EditText by Delegates.notNull<EditText>()
    var tvPartNum: TextView by Delegates.notNull<TextView>()
    var etFromBin: EditText by Delegates.notNull<EditText>()
    var tvBatch: EditText by Delegates.notNull<EditText>()
    var progressBar08: ProgressBar by Delegates.notNull<ProgressBar>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_p08)

        btnSave = findViewById(R.id.P08_btn_save)
        tvPartNum = findViewById(R.id.P08_txt_part_num)
        tvBatch = findViewById(R.id.P08_in_batch_ed)
        etFromBin = findViewById(R.id.P08_ed_storBin_in)
        etFromLoc = findViewById(R.id.P08_ed_storLoc_in)
        btnClear = findViewById(R.id.P08_btn_clear)
        progressBar08 = findViewById(R.id.P08_pb)


        etFromBin.requestFocus()
        etFromBin.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                etFromLoc.requestFocus()

                return@OnKeyListener true
            }
            false
        })
        val c:Context = this
        btnSave.setOnClickListener {

            if(etFromLoc.text.toString() != ""){
                if(etFromLoc.text.toString() != ""){
                    //SumbitP08(fromLoc.toString(),binNo.toString(),this)

                    runBlocking {
                        GlobalScope.launch {
                            progressbarSetting(progressBar08)
                            saveDoc(etFromLoc.text.toString(),etFromBin.text.toString(),c)
                            progressbarSetting(progressBar08)
                        }
                    }

                }
                else{
                    runOnUiThread(kotlinx.coroutines.Runnable {
                        val alertDialogBuilder = AlertDialog.Builder(this   )
                        alertDialogBuilder.setTitle(R.string.ErrorTitle)
                        alertDialogBuilder.setMessage(R.string.ErrorMessage1)
                        alertDialogBuilder.show()

                    })
                }
            }
            else{
                runOnUiThread(kotlinx.coroutines.Runnable {
                    val alertDialogBuilder = AlertDialog.Builder(this   )
                    alertDialogBuilder.setTitle(R.string.ErrorTitle)
                    alertDialogBuilder.setMessage(R.string.ErrorMessage2)
                    alertDialogBuilder.show()

                })
            }


        }

        etFromLoc.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                try{
                    getDetailFromBin(etFromLoc.text.toString(),etFromBin.text.toString(),this)


                }
                catch (e:Exception){

                }


                return@OnKeyListener true
            }
            false
        })

        btnClear.setOnClickListener {
            clearAll()
        }

    }


    private fun clearAll(){
        runOnUiThread(Runnable {
        tvPartNum.setText("")
        tvBatch.setText("")
        etFromBin.setText("")
        etFromLoc.setText("")

            etFromBin.requestFocus()
        })

    }

    private fun getDetailFromBin(storLoc:String,binNum:String,con: Context) = runBlocking{
        progressbarSetting(progressBar08)
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
        progressbarSetting(progressBar08)
        tvBatch.setText(batch_string)
        tvPartNum.setText(material_string)
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


    suspend fun saveDoc(storLoc:String,binNum:String,con:Context) = withContext(Dispatchers.IO){
        val url:URL = URL("http://172.16.206.19/REST_API/Home/MPP_P08?str_loc=$storLoc&bin_num=$binNum")
        try{
            val RESTData =url.readText();

            val restList = RESTData.split(':')
            val error_code = restList.get(0)

            if(error_code == "E"){
                runOnUiThread(kotlinx.coroutines.Runnable {
                    val alertDialogBuilder = AlertDialog.Builder(con)
                    alertDialogBuilder.setTitle(R.string.ErrorTitle)
                    alertDialogBuilder.setMessage(restList.get(1))
                    alertDialogBuilder.show()

                })
            }
            else{
                runOnUiThread(kotlinx.coroutines.Runnable {
                    val alertDialogBuilder = AlertDialog.Builder(con)
                    alertDialogBuilder.setTitle(R.string.Success)
                    alertDialogBuilder.setMessage(restList.get(1))
                    alertDialogBuilder.show()

                })
                clearAll()
            }
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

}