package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import kotlinupdatepackage.KotlinDeleteFunction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


public class MainActivity extends AppCompatActivity {

    public static TextView message;
    public static ProgressBar pb;
    public String[] tcodes;
    public static final String EXTRA_MESSAGE = "com.example.MyApplication.MESSAGE";
    private EditText username;
    private EditText passwor;
    private TextView current_version;
    private Context mContext;
    public final String APP_TAG = "MyCustomApp";
    private static final int DELAY = 10000;
    public boolean inactive = true;
    public static boolean inactive2 = true;

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            if(inactive2) {
                finishAffinity();
            } else {
                mHideHandler.postDelayed(mHideRunnable, DELAY);
                inactive2 = true;
            }
        }
    };

    public static void setInactivityFromAnotherActivity(boolean newStatus){
        inactive2 = newStatus;
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        inactive2 = false;
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.mContext = this;
        username = (EditText) findViewById(R.id.badgenum);
        username.requestFocus();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        Button btn = (Button) findViewById(R.id.button_login);
        Button update_btn = (Button) findViewById(R.id.updatebutton);
        pb = (ProgressBar)findViewById(R.id.progressBar);
        pb.setVisibility(View.INVISIBLE);
        //mHideHandler.postDelayed(mHideRunnable, DELAY);

        new checkForUpdateAsyncTask().execute();

        update_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                KotlinDeleteFunction.Companion.update_method1(MainActivity.this);
            }
        });

        btn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {


                new YourAsyncTask().execute();

                //sendMessage(view);

            }
        });


    }


    private Uri fileUri;
    @SuppressLint("NewApi")
    private void deleteDownloadedFiles() throws FileNotFoundException {

        requestForPermission();

            getFile();


        return;

    }

    private void requestForPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                101
        );
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void getFile() {
        mContext = MainActivity.this;
        File file;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString());
        } else {
            file = new File(Environment.getExternalStorageDirectory().toString() + "/Download/");
        }
        File[] allfiles = null;
        allfiles = file.listFiles();
        if (file.exists()) {
            for (File f:allfiles) {
                if (f.getAbsoluteFile().toString().contains("app-debug")){


                }
            }
        }
        if (allfiles != null) {
            Toast.makeText(this, "length is " + allfiles.length, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Array is null", Toast.LENGTH_SHORT).show();
        }
    }



    private class checkForUpdateAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... args) {

            KotlinDeleteFunction.Companion.update_method1(MainActivity.this);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            pb.setVisibility(View.INVISIBLE);
            super.onPostExecute(result);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pb.setVisibility(View.VISIBLE);
        }
    }

    private class YourAsyncTask extends AsyncTask<Void, Void, Void> {

        AlertDialog.Builder alertDialog;
        public String build_utl(String usr,String pass){
            pb.setVisibility(View.VISIBLE);
            String init = "http://172.16.206.19/REST_API/Home/Barcode_Login?user_name=" + usr + "&pwd=" + pass;
            return init;
        }

        @NonNull
        public ArrayList<ArrayList<String>> get_login_result_from_url(String url) {


            ArrayList<ArrayList<String>> functions_Description = new ArrayList<ArrayList<String>>();


            try {
                URL Url = new URL(url);
                HttpURLConnection conn = (HttpURLConnection)Url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                if (conn.getResponseCode() != 200) {
                    throw new RuntimeException("Failed : HTTP Error code : "
                            + conn.getResponseCode());
                }
                InputStreamReader in = new InputStreamReader(conn.getInputStream());
                BufferedReader br = new BufferedReader(in);
                String line = br.readLine();
                JSONArray jsonarray = new JSONArray(line);
                for (int i=0; i < jsonarray.length(); i++)
                {
                    try {
                        JSONObject oneObject = jsonarray.getJSONObject(i);
                        // Pulling items from the array
                        String oneObjectsItem = oneObject.getString("Function");
                        String TWOObjectsItem = oneObject.getString("Description");
                        ArrayList<String> functions = new ArrayList<>();
                        functions.add(oneObjectsItem);
                        functions.add(TWOObjectsItem);
                        functions_Description.add(functions);


                    } catch (JSONException e) {
                        JSONObject oneObject = jsonarray.getJSONObject(i);
                        String oneObjectsItem = oneObject.getString("Function");
                        ArrayList<String> functions = new ArrayList<>();
                        functions.add(oneObjectsItem);
                        functions.add("");
                        functions_Description.add(functions);
                    }
                }
            } catch (MalformedURLException e) {

                e.printStackTrace();
                delegate.processFinish(e.getMessage().toString());
            } catch (ProtocolException e) {
                delegate.processFinish(e.getMessage().toString());
                e.printStackTrace();
            } catch (IOException e) {
                delegate.processFinish(e.getMessage().toString());
                e.printStackTrace();
            } catch (JSONException e) {
                delegate.processFinish(e.getMessage().toString());
                e.printStackTrace();
            }


            return functions_Description;
        }

        public AsyncResponse delegate = null;

        String error_check = null;
        @Override
        protected Void doInBackground(Void... args) {
            pb = (ProgressBar)findViewById(R.id.progressBar);
            // Do something in response to button
            TextView bn = (TextView)findViewById(R.id.badgenum);
            TextView bnp = (TextView)findViewById(R.id.editTextTextPassword);
            String name = bn.getText().toString();
            int k = 0;
            String url = build_utl(bn.getText().toString(),bnp.getText().toString());
            ArrayList<ArrayList<String>> functions = get_login_result_from_url(url);

            Intent intent = new Intent(MainActivity.this, Main_Screen.class);
            intent.putExtra(EXTRA_MESSAGE, name);
            intent.putExtra("key",functions);
            String messg = functions.get(0).toString();
            if(!functions.get(0).toString().contains("User")){
                startActivity(intent);


            }
            else{
                error_check= "Invalid Username/Password";

            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            pb.setVisibility(View.INVISIBLE);
            super.onPostExecute(result);
            if(error_check != null){
                alertDialog.setTitle("Error");
                alertDialog.setMessage(error_check);
                alertDialog.setPositiveButton("ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
                AlertDialog orderError = alertDialog.create();
                orderError.show();

                error_check = null;
            }

        }

        @Override
        protected void onPreExecute() {
                alertDialog = new AlertDialog.Builder(MainActivity.this);
            super.onPreExecute();
            pb.setVisibility(View.VISIBLE);
        }



    }

    public interface AsyncResponse {
        void processFinish(String output);
    }



}