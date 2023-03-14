package com.example.myapplication;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class P09 extends AppCompatActivity {

    EditText p09_barcode_txt;
    TextView pallet_info_p09;
    Button btn_save_p09;
    Button btn_clear_p09;
    String material;
    String quantity;
    String lot_number;
    String palletID;
    ProgressBar pb3;
    String g_b_num;
    ProgressDialog p09_dialog;
    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        MainActivity.setInactivityFromAnotherActivity(false);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_p09);
        Intent intent = getIntent();
        String b_num = intent.getStringExtra(Main_Screen.BADGE_NUMBER);
        g_b_num = b_num;
        pb3 = (ProgressBar) findViewById(R.id.P09BAR);
        pb3.setVisibility(View.INVISIBLE);
        p09_barcode_txt = (EditText) findViewById(R.id.txt_barcode_p09);
        pallet_info_p09= (TextView) findViewById(R.id.txt_result_p09);
        btn_save_p09 = (Button) findViewById(R.id.btn_save_p09);
        p09_barcode_txt.requestFocus();
        p09_barcode_txt.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if(keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getKeyCode() == KeyEvent.KEYCODE_SHIFT_LEFT && keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getKeyCode() == KeyEvent.KEYCODE_SHIFT_RIGHT && keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getKeyCode() == KeyEvent.KEYCODE_TAB && keyEvent.getAction() == KeyEvent.ACTION_DOWN){

                    Editable barcode = p09_barcode_txt.getText();
                    AsyncTask<String, Void, String> progress = new breakupSCANNEDTEXT().execute(barcode.toString());
                    try {

                        String CURRENT_STATUS = progress.get();
                        if(CURRENT_STATUS.equals("done")){
                            pallet_info_p09.setText(" Material : " + material + " \n\n" + " Quantity : " + quantity + " \n\n" +
                                    " Lot Number : " + lot_number + " \n\n" + "Pallet ID : " + palletID +"\n");


                            p09_barcode_txt.setText("");
                            p09_barcode_txt.requestFocus();
                        }
                        else if(CURRENT_STATUS.contains("DUPLICATE")){
                            AlertDialog trnmasg = new AlertDialog.Builder(view.getContext()).create();
                            trnmasg.setTitle("ERROR");
                            trnmasg.setMessage("Double SCAN is prohibited!");
                            trnmasg.setButton(Dialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            });
                            trnmasg.show();
                            p09_barcode_txt.setText("");
                        }
                        else{
                            AlertDialog trnmasg = new AlertDialog.Builder(view.getContext()).create();
                            trnmasg.setTitle("ERROR");
                            trnmasg.setMessage(CURRENT_STATUS);
                            trnmasg.setButton(Dialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            });
                            trnmasg.show();
                            p09_barcode_txt.setText("");
                        }
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }


                }

                return false;
            }
        });

        btn_save_p09.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btn_save_p09.setEnabled(false);

                String msg;
                String typ;

                 PostIntoSystem postIntoSystem = new PostIntoSystem(new AsyncResponse() {
                         @Override
                         public void processFinish(Object output) {
                             ArrayList<String> list = (ArrayList<String>) output;
                             list.get(0);
                             list.get(1);

                             if(list.get(1).contains("S")){
                                 AlertDialog trnmasg = new AlertDialog.Builder(view.getContext()).create();
                                 trnmasg.setTitle("Success");
                                 trnmasg.setMessage(list.get(0));
                                 trnmasg.setButton(Dialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                                     @Override
                                     public void onClick(DialogInterface dialogInterface, int i) {

                                     }
                                 });
                                 trnmasg.show();
                             }
                             else{
                                 AlertDialog trnmasg = new AlertDialog.Builder(view.getContext()).create();
                                 trnmasg.setTitle("FAIL !");
                                 trnmasg.setMessage(list.get(0));
                                 trnmasg.setButton(Dialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                                     @Override
                                     public void onClick(DialogInterface dialogInterface, int i) {

                                     }
                                 });
                                 trnmasg.show();
                             }
                             btn_save_p09.setEnabled(true);
                             material = "";
                             lot_number = "";
                             palletID = "";
                         }
                     });
                 if(material != "" && lot_number != "" && palletID != ""){
                     postIntoSystem.execute();
                 }




            }
        });


    }


    public class PostIntoSystem extends AsyncTask<Void, Void, ArrayList<String>> {


        @Override
        protected ArrayList<String> doInBackground(Void... args) {

            ArrayList<String> responselist = new ArrayList<String>();

            String device_name = Settings.Global.getString(getContentResolver(), "device_name");

            String url = "http://172.16.206.19/REST_API/Home/MPP_PACKING?material=" + material + "&quantity=" + quantity + "&lot_no=" + lot_number + "&badge_num=" + g_b_num + "_" + device_name + "&pallet_id=" + palletID;

            URL Url = null;
            try {
                Url = new URL(url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection)Url.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                conn.setRequestMethod("GET");
            } catch (ProtocolException e) {
                e.printStackTrace();
            }
            conn.setRequestProperty("Accept", "application/json");
            try {
                if (conn.getResponseCode() != 200) {


                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            InputStreamReader in = null;
            try {
                in = new InputStreamReader(conn.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(in != null){
                try {
                    BufferedReader br = new BufferedReader(in);
                    String line = br.readLine();
                    String[] line_comma = line.split(",");
                    String MSG = line_comma[0].replaceAll("[\"]", "");
                    String TYPE = line_comma[1].replaceAll("[\"]", "");;
                    responselist.add(MSG);
                    responselist.add(TYPE);


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            return responselist;
        }
        String s = "";
        public AsyncResponse delegate = null;//Call back interface

        public PostIntoSystem(AsyncResponse asyncResponse) {
            delegate = asyncResponse;//Assigning call back interfacethrough constructor
        }

        @Override
        protected void onPostExecute(ArrayList<String> result) {
            delegate.processFinish(result);
            pb3 = (ProgressBar) findViewById(R.id.P09BAR);
            pb3.setVisibility(View.INVISIBLE);
        }

        private ProgressDialog mProgressDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pb3 = (ProgressBar) findViewById(R.id.P09BAR);
            pb3.setVisibility(View.VISIBLE);
        }


    }

    public interface AsyncResponse {
        void processFinish(Object output);
    }

    private class breakupSCANNEDTEXT extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... param) {
            TextView input = (TextView)findViewById(R.id.barcoetxt);

            String barcode = param[0];
            String url = "http://172.16.206.19/REST_API/Home/breakpalletbarcodeString?barcode=" + barcode;

            URL Url = null;
            try {
                Url = new URL(url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection)Url.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                conn.setRequestMethod("GET");
            } catch (ProtocolException e) {
                e.printStackTrace();
            }
            conn.setRequestProperty("Accept", "application/json");
            try {
                if (conn.getResponseCode() != 200) {


                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            InputStreamReader in = null;
            try {
                in = new InputStreamReader(conn.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(in != null){
                try {
                    BufferedReader br = new BufferedReader(in);
                    String line = br.readLine();
                    JSONObject json = new JSONObject(line);
                    material = json.getString("MATERIAL");
                    quantity = json.getString("QUANTITY");
                    lot_number = json.getString("LOT_NO");
                    palletID = json.getString("PALLET_ID");
                    return "done";

                } catch (IOException | JSONException e) {
                    return e.getMessage().toString();
                }
            }
            else {
                return "error";
            }



        }

        //@Override
        protected void onPostExecute(String result) {
            super.onPostExecute(String.valueOf(result));
            pb3.setVisibility(View.INVISIBLE);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pb3.setVisibility(View.VISIBLE);
        }
    }

}