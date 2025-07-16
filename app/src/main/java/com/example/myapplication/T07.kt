package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.net.URL
import android.provider.Settings
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

class T07 : AppCompatActivity() {

    private lateinit var btnAreaA:Button
    private lateinit var btnAreaB:Button
    private lateinit var btnAreaC:Button
    private lateinit var btnAreaD:Button
    private lateinit var btnAreaE:Button
    private lateinit var mainLayout:LinearLayout
    private lateinit var g_badgeNum:String
    private lateinit var title:TextView
    private lateinit var currentArea:String
    private lateinit var c:Context
    private lateinit var deviceID:String
    private lateinit var btnQTF:Button


    override fun onDestroy() {
        super.onDestroy()
        // Call your function here
        updateUsageArea(currentArea,"0","")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_t07)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        btnAreaA = findViewById(R.id.btnAreaA)
        btnAreaB = findViewById(R.id.btnAreaB)
        btnAreaC = findViewById(R.id.btnAreaC)
        btnAreaD = findViewById(R.id.btnAreaD)
        btnAreaE = findViewById(R.id.btnAreaE)
        btnQTF = findViewById(R.id.BtnQTF)
        mainLayout = findViewById(R.id.t07mainLayout)
        g_badgeNum = intent.getStringExtra("Badge").toString()
        title = findViewById(R.id.T07tvtitle)
        title.text = "T07 Screen for : $g_badgeNum"
        currentArea = ""
        deviceID = getDeviceUniqueId(this@T07)
        c = this
        btnAreaA.setOnClickListener {
            try {
                checkUpdateUsage("A")
            }
            catch (e:Exception){
                Toast.makeText(c, e.message.toString(), Toast.LENGTH_SHORT).show()
            }

        }
        btnAreaB.setOnClickListener {
            try {
                checkUpdateUsage("B")
            }
            catch (e:Exception){
                Toast.makeText(c, e.message.toString(), Toast.LENGTH_SHORT).show()
            }

        }
        btnAreaC.setOnClickListener {
            try {
                checkUpdateUsage("C")
            }
            catch (e:Exception){
                Toast.makeText(c, e.message.toString(), Toast.LENGTH_SHORT).show()
            }

        }
        btnAreaD.setOnClickListener {
            try {
                checkUpdateUsage("D")
            }
            catch (e:Exception){
                Toast.makeText(c, e.message.toString(), Toast.LENGTH_SHORT).show()
            }

        }
        btnAreaE.setOnClickListener {
            try {
                checkUpdateUsage("E")
            }
            catch (e:Exception){
                Toast.makeText(c, e.message.toString(), Toast.LENGTH_SHORT).show()
            }

        }

        btnQTF.setOnClickListener {
            val classText = "com.example.myapplication.T07_QTF"
            val className = Class.forName(classText)
            var newAreaActivityIntent = Intent(c,className)
            newAreaActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            newAreaActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            newAreaActivityIntent.putExtra("Area", "QTF")
            newAreaActivityIntent.putExtra("Badge", g_badgeNum)
            startActivity(newAreaActivityIntent)

        }



    }

    private fun checkUpdateUsage(area:String){
             var Usagedata = retrieveUsageList(area)
            if (Usagedata.USAGE == "0" || Usagedata.BADGE == deviceID){
                updateUsageArea(area,"1",deviceID)
                try {
                    val classText = "com.example.myapplication.T07area$area"
                    val className = Class.forName(classText)
                    var newAreaActivityIntent = Intent(c,className)
                    runBlocking {
                        val job = GlobalScope.launch {
                            newAreaActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            newAreaActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            newAreaActivityIntent.putExtra("Area", area)
                            newAreaActivityIntent.putExtra("Badge", g_badgeNum)
                        }
                        job.join()

                        startActivity(newAreaActivityIntent)
                    }
                }
                catch (ex:Exception){
                    val test = ex.message.toString()
                }
            }
            else{
                Toast.makeText(this@T07, "Area being used !", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getPullListData(area:String):String{
        return URL("http://172.16.206.19/REST_API/SMT_EKANBAN_SERVICES/RetrieveData?trolley=$area").readText()
    }

    private fun retrieveUsageList(area:String):AREAUSAGE{
        val CF:commonFunctions = commonFunctions()
        return CF.retrieveUsageList(area)
    }

    private fun getDeviceUniqueId(context: Context): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For devices running Android 10 (API level 29) or later
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        } else {
            // For devices running Android 9 (API level 28) or earlier
            @Suppress("DEPRECATION")
            android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: ""
        }
    }

    fun updateUsageArea(area:String, updUsage:String, Fbnum:String){
        var result = ""
        runBlocking {
            val job = GlobalScope.launch {
                val urlLink = URL("http://172.16.206.19/REST_API/SMT_EKANBAN_SERVICES/UpdateAreaUsage?area=${area}&usage=${updUsage}&badgeNo=${Fbnum}")
                result = urlLink.readText()
            }
            job.join()
        }
    }

}
data class AREAUSAGE(
    var AREA:String,
    var USAGE:String,
    var BADGE: String
)

data class listData(
    var ID:Int,
    var MATERIAL:String,
    var LOCATION:String,
    var QUANTITY:Int,
    var REQUESTOR: String,
    var PULLLISTNUMBER:String,
    var MACHINE_NO:String

)

data class dataRequestor(
    var listData:List<listData>,
    var PULLLIST: String
)