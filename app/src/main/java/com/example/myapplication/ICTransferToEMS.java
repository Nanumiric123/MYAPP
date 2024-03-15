package com.example.myapplication;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
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

public class ICTransferToEMS extends AppCompatActivity {

    EditText ICTransferBarcode;
    TextView tv_batch;
    TextView tv_material;
    TextView tv_qty;
    TextView tv_reel_num;
    ProgressBar pb_ICPROGRAM;
    Button btn_save;
    String g_b_num;
    TextView tv_ems_stor_loc;

    ArrayList<String> server_response_breakbarcode = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ic_transfer_to_ems);
        pb_ICPROGRAM = findViewById(R.id.pb_ICTransfer_menu);
        ICTransferBarcode = findViewById(R.id.txt_ICTransfer_barcode);
        tv_batch = findViewById(R.id.tv_batch_ICTrf_value);
        tv_material = findViewById(R.id.tv_ICTransfer_material_value);
        tv_qty = findViewById(R.id.tv_ICTransfer_quantity_value);
        tv_reel_num = findViewById(R.id.tv_ICTransfer_reel_no_value);
        btn_save = findViewById(R.id.btn_icTransfer_save);
        tv_ems_stor_loc = findViewById(R.id.txt_toLocation_ICTRANSFER);
        Intent intent = getIntent();
        String b_num = intent.getStringExtra("badge");
        g_b_num = b_num;
        ICTransferBarcode.requestFocus();
        ICTransferBarcode.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if(keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getKeyCode() == KeyEvent.KEYCODE_SHIFT_LEFT && keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getKeyCode() == KeyEvent.KEYCODE_SHIFT_RIGHT && keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getKeyCode() == KeyEvent.KEYCODE_TAB && keyEvent.getAction() == KeyEvent.ACTION_DOWN){
                    Editable barcode_values = ICTransferBarcode.getText();
                    AsyncTask<String, Void, String> progress = new breakupSCANNEDTEXT().execute(barcode_values.toString());
                    try{
                        String CURRENT_STATUS = progress.get();
                        if(CURRENT_STATUS.equals("done")) {
                            if(server_response_breakbarcode.size() > 1){
                                tv_material.setText(server_response_breakbarcode.get(0));
                                tv_batch.setText((server_response_breakbarcode.get(1)));
                                tv_qty.setText(server_response_breakbarcode.get(2));
                                tv_reel_num.setText(server_response_breakbarcode.get(3));
                                ICTransferBarcode.setText("");
                            }
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
                            ICTransferBarcode.setText("");
                        }

                    }catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
                return false;
            }
        });

        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg;
                String type;

                PostIntoSystem postIntoSystem = new PostIntoSystem(new AsyncResponse() {
                    @Override
                    public void processFinish(Object output) {
                        String msg = (String) output;
                        AlertDialog ic_trnmasg = new AlertDialog.Builder(view.getContext()).create();
                        ic_trnmasg.setTitle("Success");
                        ic_trnmasg.setMessage(msg);
                        ic_trnmasg.setButton(Dialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        });
                        ic_trnmasg.show();
                        tv_material.setText("");
                        tv_batch.setText("");
                        tv_qty.setText("");
                        tv_reel_num.setText("");
                        tv_ems_stor_loc.setText("");
                    }
                });

                postIntoSystem.execute();

            }
        });

    }

    private class breakupSCANNEDTEXT extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... param) {
            String barcode = param[0];
            String url = "http://172.16.206.19/REST_API/Home/breAKBarcodeString?barcode=" + barcode;
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
                    String material = json.getString("PART_NO");
                    String quantity = json.getString("QUANTITY");
                    String lot_number = json.getString("LOT");
                    String reel_no = json.getString("REEL_NO");
                    server_response_breakbarcode.add(material);
                    server_response_breakbarcode.add(lot_number);
                    server_response_breakbarcode.add(quantity);
                    server_response_breakbarcode.add(reel_no);
                    return "done";

                } catch (IOException | JSONException e) {
                    return e.getMessage().toString();
                }
            }
            else {
                return "error";
            }

        }

        protected void onPostExecute(String result) {
            super.onPostExecute(String.valueOf(result));
            pb_ICPROGRAM.setVisibility(View.INVISIBLE);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pb_ICPROGRAM.setVisibility(View.VISIBLE);
        }

    }
    public interface AsyncResponse {
        void processFinish(Object output);
    }
    public class PostIntoSystem extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... args) {
            String response = null;
            String material = tv_material.getText().toString();
            String quantity = tv_qty.getText().toString();
            String lot_number = tv_batch.getText().toString();
            String ems_location = tv_ems_stor_loc.getText().toString();

            String url = "http://172.16.206.19/REST_API/Home/MPP_IC_TRANSFER?material=" + material + "&quantity=" + quantity + "&lot_number=" + lot_number + "&badge_num=" + g_b_num + "&ems_loc=" + ems_location;
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
                    response = line;

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            return response;
        }

        public AsyncResponse delegate = null;//Call back interface

        public PostIntoSystem(AsyncResponse asyncResponse) {
            delegate = asyncResponse;//Assigning call back interfacethrough constructor
        }


        protected void onPostExecute(String result) {
            delegate.processFinish(result);
            pb_ICPROGRAM = findViewById(R.id.pb_ICTransfer_menu);
            pb_ICPROGRAM.setVisibility(View.INVISIBLE);
        }

        private ProgressDialog mProgressDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pb_ICPROGRAM = findViewById(R.id.pb_ICTransfer_menu);
            pb_ICPROGRAM.setVisibility(View.VISIBLE);
        }



    }

}