package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class BinPartition : AppCompatActivity() {
    private lateinit var title:TextView
    private lateinit var badgeNum:String
    private lateinit var btnNewReg:Button
    private lateinit var btnIssue:Button
    private lateinit var btnScrap:Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_bin_partition)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        title = findViewById(R.id.BPTVTITLE)
        title.setText(intent.getStringExtra("Desc").toString())
        badgeNum = intent.getStringExtra("Badge").toString()
        btnNewReg = findViewById(R.id.BPbtnNewReg)
        btnIssue = findViewById(R.id.BPbtnIssue)
        btnScrap = findViewById(R.id.BPbtnScrap)

        btnNewReg.setOnClickListener {
            val badgeIntent = Intent(this, binPartitionNew::class.java)
            badgeIntent.putExtra("BADGE_NO", badgeNum)
            startActivity(badgeIntent)
        }
        btnIssue.setOnClickListener {
            val badgeIntent = Intent(this, binPartitionIssue::class.java)
            badgeIntent.putExtra("BADGE_NO", badgeNum)
            startActivity(badgeIntent)
        }
        btnScrap.setOnClickListener {
            val badgeIntent = Intent(this, binPartitionScrap::class.java)
            badgeIntent.putExtra("BADGE_NO", badgeNum)
            startActivity(badgeIntent)
        }

    }
}