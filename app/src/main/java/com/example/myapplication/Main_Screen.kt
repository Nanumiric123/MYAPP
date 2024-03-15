package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject

class Main_Screen : AppCompatActivity() {
    private lateinit var mainLayout:LinearLayout
    private lateinit var c:Context
    private lateinit var message: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_screen)
        c = this
        //val intent = intent
        message = intent.getStringExtra("Badge").toString()

        val jsonString = intent.getStringExtra("Functions")
        val title = findViewById<View>(R.id.textView2) as TextView
        title.text = "Main menu for : $message"
        mainLayout = findViewById(R.id.linearlayout)

        var functionArray = JSONArray(jsonString)


        if(functionArray.length() > 0 ){
            var row = LinearLayout(c)
            setRowProperty(row)

            // Calculate the number of rows needed
            val numRows = (functionArray.length() + 2) / 3

            // Create a 2D list to represent the grid
            val grid = List(numRows) { row ->
                List(3) { col ->
                    val index = row * 3 + col
                    if (index < functionArray.length()) functionArray[index] else ""
                }
            }

            for(rowGrid in grid){
                for (item in rowGrid){
                    if (item != ""){
                        var function = JSONObject(item.toString()).getString("Function")
                        var description = JSONObject(item.toString()).getString("Description")
                        row.addView(addbtn(description,function,message,description))
                    }
                }
                mainLayout.addView(row)
                row = LinearLayout(c)
                setRowProperty(row)
            }
        }


    }


    private fun setRowProperty(v:LinearLayout){
        var rowParams: TableRow.LayoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.MATCH_PARENT)
        with(rowParams){
            weight = 1F
            setMargins(10,10,10,10)
        }
        with(v){
            gravity = android.view.Gravity.CENTER
            layoutParams = rowParams
            orientation = LinearLayout.HORIZONTAL
            //setBackgroundResource(R.drawable.border)
        }
    }

    private fun addbtn(desc:String,tcd:String,badgeNum:String,des:String):View{
        val funcBtn = Button(c)
        with(funcBtn){
            text = desc
            textSize = 12F
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                200,
                200,
                1.0f
            )
        }

        funcBtn.setOnClickListener{

            try {
                val classText = "com.example.myapplication.$tcd"
                val className = Class.forName(classText)
                var newActivityIntent = Intent(c,className)
                newActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                newActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                newActivityIntent.putExtra("Badge", badgeNum)
                newActivityIntent.putExtra("Desc",des)
                startActivity(newActivityIntent)

            }
            catch (ex:Exception){

            }

        }
        return funcBtn
    }


}