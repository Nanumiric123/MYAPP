package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class viewFunctions {
    var rowParams: TableRow.LayoutParams = TableRow.LayoutParams(
        TableRow.LayoutParams.WRAP_CONTENT,
        TableRow.LayoutParams.WRAP_CONTENT
    )

    fun generateTVforpulllistT07(txt:String, c:Context):View{
        val textTV = TextView(c)
        with(textTV){
            text = txt
            textSize = 16F
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(15, 15, 15, 15)
            layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
            setBackgroundResource(R.drawable.cell_with_border)
        }
        return textTV
    }
    fun generateTVforTableT07(txt:String, c:Context):View{
        val textTV = TextView(c)
        with(textTV){
            text = txt
            textSize = 16F
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(15, 15, 15, 15)
            layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.MATCH_PARENT
            )
            setBackgroundResource(R.drawable.cell_with_border)
        }
        return textTV
    }

    fun createLinearLayout(btn: Button,c:Context):LinearLayout{
        val layoutBtn = LinearLayout(c)
        with(layoutBtn) {
            layoutParams = rowParams
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.cell_with_border)
            setPadding(15, 15, 15, 15)
        }
        layoutBtn.addView(btn)
        return layoutBtn
    }

}