package com.example.myapplication

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class M02 : AppCompatActivity() {
    private lateinit var title:TextView
    private lateinit var resvNo:EditText
    private lateinit var edPart:EditText
    private lateinit var edQty:EditText
    private lateinit var edLot:EditText
    private lateinit var edFromSloc:EditText
    private lateinit var edFromLoc: EditText
    private lateinit var edToSloc:EditText
    private lateinit var edToLoc:EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_m02)

        title = findViewById(R.id.M02TITLE)
        title.text = intent.getStringExtra("Desc").toString()
        resvNo = findViewById(R.id.M02EDresvNo)
        edPart = findViewById(R.id.M02EDPart)
        edQty = findViewById(R.id.M02EDQty)
        edLot = findViewById(R.id.M02EDLOTNO)
        edFromSloc = findViewById(R.id.M02EDFROMSLOC)
        edFromLoc = findViewById(R.id.M02EDFROMLOC)
        edToSloc = findViewById(R.id.M02EDTOSLOC)
        edToLoc = findViewById(R.id.M02EDTOLOC )

    }
}