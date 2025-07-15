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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.javiersantos.appupdater.DisableClickListener
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.ExceptionLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class T07_QTF : AppCompatActivity() {
    private lateinit var btnMenu: Button
    private lateinit var btnClear: Button
    private lateinit var btnSave: Button
    private lateinit var barcodeInput: EditText
    private lateinit var materialTV: TextView
    private lateinit var batchTV: TextView
    private lateinit var reelNoTV: TextView
    private lateinit var quantityTV: TextView
    private lateinit var pb: ProgressBar
    private lateinit var c: Context
    private lateinit var badgeNum: String

    data class SAP_DATA(
        var MATERIAL: String,
        var BATCH: String,
        var QUANTITY: String,
        var REEL_NO: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_t07_qtf)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        badgeNum = intent.getStringExtra("Badge").toString()
        c = this@T07_QTF
        var cf = commonFunctions()

    }
}