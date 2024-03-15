package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.azhon.appupdate.manager.DownloadManager
import kotlinupdatepackage.KotlinDeleteFunction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URL

class MainActivity : AppCompatActivity() {
    private lateinit var username: EditText
    private lateinit var mContext: MainActivity
    private lateinit var UsernameInput:EditText
    private lateinit var PasswordInput:EditText
    private lateinit var loginbtn:Button
    private lateinit var updateBtn:Button
    private lateinit var badgeCaps:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mContext = this
        username = findViewById<View>(R.id.badgenum) as EditText
        username.requestFocus()
        UsernameInput = findViewById(R.id.badgenum)
        PasswordInput = findViewById(R.id.edMainPass)
        loginbtn = findViewById(R.id.button_login)
        updateBtn = findViewById(R.id.updatebutton)

        updateBtn.setOnClickListener{
            runBlocking {
                GlobalScope.launch {
                    val url: URL = URL("http://172.16.206.19/REST_API/debug/output-metadata.json")
                    val outputMetadata = url.readText()
                    val OMObject = JSONTokener(outputMetadata).nextValue() as JSONObject
                    var apkVersion = OMObject.getJSONArray("elements")
                    var apkcurrentVersion = apkVersion.getJSONObject(0).getString("versionCode")

                    val apkversionName = apkVersion.getJSONObject(0).getString("versionName")
                    val currentVersion = getCurrentVersion(applicationContext)

                    if (apkcurrentVersion != currentVersion){

                    }


                }
            }
        }

        loginbtn.setOnClickListener {
            var userData = String()
            badgeCaps = username.text.toString().uppercase()
            runBlocking {
                val job = GlobalScope.launch {
                    userData = get_login_result()
                }
                job.join()
                if(!userData.isNullOrBlank()){
                    val function = JSONObject(JSONArray(userData).getString(0)).getString("Function")
                    if(function != "User not Registered"){
                        val intent = Intent(mContext, Main_Screen::class.java)
                        intent.putExtra("Badge",badgeCaps)
                        intent.putExtra("Functions",userData)
                        startActivity(intent)
                    }
                    else{
                        val builder = AlertDialog.Builder(mContext)
                        builder.setTitle("Attention")
                        builder.setMessage(function)
                        builder.setPositiveButton("Ok") { dialog, which ->
                            UsernameInput.text.clear()
                            PasswordInput.text.clear()
                            UsernameInput.requestFocus()
                        }
                        builder.show()
                    }
                }


            }

        }

    }

    fun getCurrentVersion(context: Context): String {
        try {
            val packageInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            return packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            return "N/A"
        }
    }

    private suspend fun get_login_result(): String {
        var result = String()
        withContext(Dispatchers.IO){
            try {
                val loginUrl = "http://172.16.206.19/REST_API/Home/Barcode_Login?user_name=${UsernameInput.text}&pwd=${PasswordInput.text}"

                result = URL(loginUrl).readText()
            }
            catch(e:Exception) {
                runOnUiThread(kotlinx.coroutines.Runnable {
                    val builder = AlertDialog.Builder(mContext)
                    builder.setTitle("Attention")
                    builder.setMessage(e.message.toString())
                    builder.setPositiveButton("Ok") { dialog, which ->
                        UsernameInput.text.clear()
                        PasswordInput.text.clear()
                        UsernameInput.requestFocus()
                    }
                    builder.show()
                })

            }

        }

         return result
    }

    data class  loginData(var tcode:String,var desc:String)
}
