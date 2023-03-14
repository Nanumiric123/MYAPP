package com.example.myapplication;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class Main_Screen extends AppCompatActivity {
    public static final String BADGE_NUMBER = "com.example.MyApplication.MESSAGE";
    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        MainActivity.setInactivityFromAnotherActivity(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);
        Intent intent = getIntent();
        String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        TextView title = (TextView)findViewById(R.id.textView2);
        AlertDialog.Builder alertDialog = null;
        title.setText("Main menu for : " + message);
        ArrayList<ArrayList<String>> tcodes = (ArrayList<ArrayList<String>>) getIntent().getSerializableExtra("key");
        LinearLayout layout = (LinearLayout)findViewById(R.id.linearlayout);
        String res = null;
        int btn_id = 0;
        LinearLayout row = new LinearLayout(this);

        for(ArrayList<String> i : tcodes){
            btn_id++;
            String single_function = "";
            String description = null;
            for (int j=0; j < i.size(); j++){
                single_function = i.get(0);
                description = i.get(1);
            }

            if(single_function.equals("P08")){
                String test = "";
            }


            if(btn_id % 3 == 1) {

                row = new LinearLayout(this);
                row.setLayoutParams(new LinearLayout.LayoutParams
                        (LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.MATCH_PARENT));
                row.setOrientation(LinearLayout.HORIZONTAL);
                Button button = new Button(this);
                button.setText(description);
                button.setTextSize(TypedValue.COMPLEX_UNIT_PX, 18);
                //Drawable top = getResources().getDrawable(R.drawable.aikitting);
                //button.setCompoundDrawablesWithIntrinsicBounds(null,top,null,null);
                button.setId(btn_id);
                LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT,
                        1.0f
                );
                String classtext = "com.example.myapplication." + single_function;
                button.setLayoutParams(param);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Button b = (Button)view;
                        String buttonText = b.getText().toString();
                        Toast.makeText(Main_Screen.this, "Showing " + buttonText , Toast.LENGTH_LONG).show();

                        try {
                            Class<?> c = Class.forName(classtext);
                            Intent intent = new Intent(Main_Screen.this, c);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra(BADGE_NUMBER,message);
                            startActivity(intent);

                        } catch (ClassNotFoundException ignored) {
                            String eror = ignored.getMessage().toString();
                            String error = "";

                        }

                    }
                });
                row.addView(button);
            }
            else if (btn_id % 3 == 2){
                Button button = new Button(this);
                button.setText(description);
                button.setTextSize(TypedValue.COMPLEX_UNIT_PX, 18);
                //Drawable top = getResources().getDrawable(R.drawable.aikitting);
                //button.setCompoundDrawablesWithIntrinsicBounds(null,top,null,null);
                button.setId(btn_id);
                LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT,
                        1.0f
                );

                String classtext = "com.example.myapplication." + single_function;
                button.setLayoutParams(param);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Button b = (Button)view;
                        String buttonText = b.getText().toString();
                        Toast.makeText(Main_Screen.this, "Showing " + buttonText , Toast.LENGTH_LONG).show();

                        try {
                            Class<?> c = Class.forName(classtext);
                            Intent intent = new Intent(Main_Screen.this, c);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra(BADGE_NUMBER,message);
                            startActivity(intent);
                        } catch (ClassNotFoundException ignored) {
                            String eror = ignored.getMessage().toString();
                            String error = "";

                        }


                    }
                });
                row.addView(button);
                if(tcodes.size()==2){
                    layout.addView(row);
                }
            }
            else{
                Button button = new Button(this);
                button.setText(description);
                button.setTextSize(TypedValue.COMPLEX_UNIT_PX, 18);
                //Drawable top = getResources().getDrawable(R.drawable.aikitting);
                //button.setCompoundDrawablesWithIntrinsicBounds(null,top,null,null);
                button.setId(btn_id);
                LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT,
                        1.0f
                );

                String classtext = "com.example.myapplication." + single_function;
                button.setLayoutParams(param);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Button b = (Button)view;
                        String buttonText = b.getText().toString();
                        Toast.makeText(Main_Screen.this, "Showing " + buttonText , Toast.LENGTH_LONG).show();

                        try {
                            Class<?> c = Class.forName(classtext);
                            Intent intent = new Intent(Main_Screen.this, c);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra(BADGE_NUMBER,message);
                            startActivity(intent);

                        } catch (ClassNotFoundException ignored) {
                            String eror = ignored.getMessage().toString();
                            String error = "";

                        }

                    }
                });
                row.addView(button);
                layout.addView(row);
            }

            if(tcodes.size()==1){
                layout.addView(row);
            }



        }

    }
}