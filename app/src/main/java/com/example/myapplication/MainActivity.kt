package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.IOException
import java.net.URL

class MainActivity : AppCompatActivity() {
    private lateinit var username: EditText
    private lateinit var mContext: MainActivity
    private lateinit var UsernameInput: EditText
    private lateinit var PasswordInput: EditText
    private lateinit var loginbtn: Button
    private lateinit var updateBtn: Button
    private lateinit var badgeCaps: String
    private lateinit var pb: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.constraintlayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mContext = this
        username = findViewById<View>(R.id.badgenum) as EditText
        username.requestFocus()
        UsernameInput = findViewById(R.id.badgenum)
        PasswordInput = findViewById(R.id.edMainPass)
        loginbtn = findViewById(R.id.button_login)
        updateBtn = findViewById(R.id.updatebutton)
        pb = findViewById(R.id.progressBar)

        updateBtn.setOnClickListener {
            runBlocking {
                GlobalScope.launch {
                    val url: URL = URL("http://172.16.206.19/REST_API/debug/output-metadata.json")
                    val outputMetadata = url.readText()
                    val OMObject = JSONTokener(outputMetadata).nextValue() as JSONObject
                    var apkVersion = OMObject.getJSONArray("elements")
                    var apkcurrentVersion = apkVersion.getJSONObject(0).getString("versionCode")

                    val apkversionName = apkVersion.getJSONObject(0).getString("versionName")
                    val currentVersion = getCurrentVersion(applicationContext)

                    if (apkcurrentVersion != currentVersion) {

                    }


                }
            }
        }

        loginbtn.setOnClickListener {
            if (isNetworkAvailable(mContext)) {
                if(username.text.isNullOrEmpty() || PasswordInput.text.isNullOrEmpty()){
                    Toast.makeText(mContext, "InpuT Password or username", Toast.LENGTH_LONG).show()
                }
                else{
                    badgeCaps = username.text.toString().uppercase()
                    execute_get_login_result(badgeCaps, PasswordInput.text.toString(), pb)
                }


            }
            else{
                Toast.makeText(mContext, "No network connection available!", Toast.LENGTH_LONG).show()
            }

        }

    }



    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun getCurrentVersion(context: Context): String? {
        try {
            val packageInfo: PackageInfo =
                context.packageManager.getPackageInfo(context.packageName, 0)
            return packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            return "N/A"
        }
    }

    private fun get_login_result(userName: String?, password: String?): String? {
        var result = String()
        if (userName.isNullOrEmpty() || password.isNullOrEmpty()) {
            throw IllegalArgumentException("Please enter username AND password")
        }
        val loginUrl =
            "http://172.16.206.19/REST_API/Home/Barcode_Login?user_name=${userName}&pwd=${password}"


        return try {
            val url = URL(loginUrl)
            url.readText()  // Fetch the response as a String
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun setProgressBar(v: View, show: Boolean) {
        if (show) {
            v.visibility = View.VISIBLE
            loginbtn.isEnabled = false
        } else {
            v.visibility = View.GONE
            loginbtn.isEnabled = true
        }
    }

    private fun execute_get_login_result(userName: String?, password: String?, progressBar: View) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Show the progress bar and disable the button
                setProgressBar(progressBar, true)

                // Perform the network request in the IO dispatcher
                val result = withContext(Dispatchers.IO) {
                    get_login_result(userName, password)
                }
                setProgressBar(progressBar, true)
                // Handle the result
                result?.let { responseString ->

                    val function =
                        JSONObject(JSONArray(responseString).getString(0)).getString("Function")
                    if (function != "User not Registered") {

                        val intent = Intent(mContext, Main_Screen::class.java)
                        intent.putExtra("Badge", badgeCaps)
                        intent.putExtra("Functions", responseString)
                        startActivity(intent)
                    } else {
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
            }            catch(e:Exception) {}
            finally {
                setProgressBar(pb, false)
            }

        }
    }
}