package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URL

data class PO_DATA(var material: String = "",var quantity:Int)

class P03 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_p03)
        val btn_menu:Button = findViewById(R.id.P03_btn_Menu)
        val clrBtn:Button = findViewById(R.id.btn_p03_clear)
        val qty_s:EditText = findViewById(R.id.p03_ed_qty)
        val sloc:EditText = findViewById(R.id.P03_et_stor_loc)

        btn_menu.setOnClickListener {
            this.finish()
        }
        val barcode_ed:EditText = findViewById(R.id.ed_barcode_p03)
        barcode_ed.requestFocus()
        val po_numtv:TextView = findViewById(R.id.P03_tv_po_num)

        barcode_ed.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                if(barcode_ed.text.toString() != ""){
                    po_numtv.text = barcode_ed.text.toString()
                    get_order_detail(barcode_ed.text.toString(),this)
                    barcode_ed.text = null
                }


                return@OnKeyListener true
            }
            false
        })

        sloc.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP ||
                keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN) {
                //Perform Code
                qty_s.requestFocus()


                return@OnKeyListener true
            }
            false
        })

        val progressBar:ProgressBar = findViewById(R.id.p03_progress_bar)
        val btn_save:Button = findViewById<Button>(R.id.p03_btn_Save)
        val c:Context = this
        btn_save.setOnClickListener{

            if(sloc.text.toString().trim() != ""){
                //Submit_Data(this)
                runBlocking {
                    GlobalScope.launch {
                        progressbar_setting(progressBar)
                        saveDoc(c)
                        progressbar_setting(progressBar)

                    }
                }

            }
            else{
                runOnUiThread(kotlinx.coroutines.Runnable {
                    val alertDialogBuilder = AlertDialog.Builder(this)
                    alertDialogBuilder.setTitle(R.string.ErrorTitle)
                    alertDialogBuilder.setMessage(R.string.ErrorMessage1)
                    alertDialogBuilder.show()

                })
            }

        }

        clrBtn.setOnClickListener{

            val tv_mat:TextView = findViewById(R.id.p03_tv_material)
            val tvPO:TextView = findViewById(R.id.P03_tv_po_num)
            val qty_s:EditText = findViewById(R.id.p03_ed_qty)
            val sloc:EditText = findViewById(R.id.P03_et_stor_loc)
            val tv_uom:TextView = findViewById(R.id.p03_tv_uom)

            tv_uom.text = "";
            tvPO.text = ""
            tv_mat.text = ""
            qty_s.text.clear()
            sloc.text.clear()

        }

    }

    private fun progressbar_setting(v:View){
        runOnUiThread(Runnable {
            if(v.visibility == View.VISIBLE){
                v.visibility = View.GONE
            }
            else{
                v.visibility = View.VISIBLE
            }

        })
    }

    private fun get_order_detail(po_num:String,con:Context) = runBlocking{
        var k:PO_DATA = com.example.myapplication.PO_DATA("", 0)
        val progressBar:ProgressBar = findViewById(R.id.p03_progress_bar)
            val first_job = GlobalScope.launch {
                progressbar_setting(progressBar)
                val url = URL("http://172.16.206.19/REST_API/Home/GET_ORDER_DETAIL_MPP?order=$po_num")
                try {
                    val RESTData =url.readText();
                    val RESTobj = JSONTokener(RESTData).nextValue() as JSONObject

                    k.material = RESTobj.getString("MATERIAL")
                    k.quantity = RESTobj.getInt("QUANTITY")

                } catch (e: Exception){
                    runOnUiThread(Runnable {
                        val alertDialogBuilder = AlertDialog.Builder(con)
                        alertDialogBuilder.setTitle(R.string.ErrorTitle)
                        alertDialogBuilder.setMessage(e.message.toString())
                        alertDialogBuilder.show()

                    })
                }


            }

            first_job.join()
            val tv_mat:TextView = findViewById(R.id.p03_tv_material)
            val tv_uom:TextView = findViewById(R.id.p03_tv_uom)
            tv_mat.text = k.material
            tv_uom.setText("PC")
            val binText:EditText = findViewById(R.id.P03_et_stor_loc)
            binText.requestFocus()
            progressbar_setting(progressBar)
    }

    suspend fun saveDoc(con:Context) = withContext(Dispatchers.IO){
        val tv_mat:TextView = findViewById(R.id.p03_tv_material)
        val po_numtv:TextView = findViewById(R.id.P03_tv_po_num)
        val qty_s:EditText = findViewById(R.id.p03_ed_qty)
        val sloc:EditText = findViewById(R.id.P03_et_stor_loc)
        val matFTV:String = tv_mat.text.toString()
        val s_ponum:String = po_numtv.text.toString()
        val in_qty:String = qty_s.text.toString()
        val toBinLoc:String = sloc.text.toString()
        val intent = intent
        val b_num = intent.getStringExtra("Badge")

        val devName = Settings.Global.getString(getContentResolver(), Settings.Global.DEVICE_NAME)
        val url = URL("http://172.16.206.19/REST_API/Home/MPP_P03?order=$s_ponum&mat=$matFTV&batc=$s_ponum&qty=$in_qty&badge_n=$b_num&assetNo=$devName&toBin=$toBinLoc")
        try {
            val RESTData =url.readText()
            val resresult = RESTData.trim()
            runOnUiThread(Runnable {
                val alertDialogBuilder = AlertDialog.Builder(con)
                alertDialogBuilder.setTitle("System Message")
                alertDialogBuilder.setMessage(resresult)
                alertDialogBuilder.show()
            })

        }
        catch (e: Exception) {
            runOnUiThread(Runnable {
                val alertDialogBuilder = AlertDialog.Builder(con)

                alertDialogBuilder.setTitle("Error When Saving!")

                alertDialogBuilder.setMessage(e.message.toString())
                alertDialogBuilder.show()
            })

        }

        qty_s.text.clear()
        sloc.text.clear()

    }

}