package com.example.myapplication;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.methods.CloseableHttpResponse;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.methods.HttpPost;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.entity.StringEntity;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.impl.client.CloseableHttpClient;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.impl.client.HttpClientBuilder;


import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.concurrent.ExecutionException;

public class ReelVerification extends AppCompatActivity {

    ProgressBar pb2;
    File photoFile;
    ImageView img;
    TextView tv_reel_verify;
    TextView tv_change_status;
    TextView vendor_label_txt;
    public final String APP_TAG = "MyCustomApp";
    public final static int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 1034;
    public String photoFileName = "photo.jpg";
    public String final_res = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reel_verification);

        pb2 = (ProgressBar)findViewById(R.id.progressBar2);
        pb2.setVisibility(View.INVISIBLE);
        img = (ImageView) findViewById(R.id.imageView2);
        TextView inputtx = (TextView) findViewById(R.id.barcoetxt);

        tv_reel_verify = (TextView) findViewById(R.id.tvreelverify);
        tv_reel_verify.setText("Material : " + "\nQuantity: ");
        vendor_label_txt = (TextView) findViewById(R.id.txt_vendor_label);
        vendor_label_txt.requestFocus();
        vendor_label_txt.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {

                if(keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getKeyCode() == KeyEvent.KEYCODE_SHIFT_LEFT && keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getKeyCode() == KeyEvent.KEYCODE_SHIFT_RIGHT && keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getKeyCode() == KeyEvent.KEYCODE_TAB && keyEvent.getAction() == KeyEvent.ACTION_DOWN){

                }

                return false;
            }
        });

        ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            // There are no request codes
                            // by this point we have the camera photo on disk
                            Bitmap takenImage = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                            String base64_img = encodeImage(takenImage);
                            new StoreDataAsyncTask().execute(base64_img);
                            Bitmap thumb = ThumbnailUtils.extractThumbnail(takenImage,409, 370);
                            img.setImageBitmap(thumb);




                        }else { // Result was a failure
                            Toast.makeText(ReelVerification.this, "Picture wasn't taken! SCAN AGAIN", Toast.LENGTH_SHORT).show();
                            if(pb2.getVisibility() ==  View.VISIBLE){
                                pb2.setVisibility(View.INVISIBLE);
                            }
                            vendor_label_txt.setText("");
                            vendor_label_txt.requestFocus();
                            inputtx.setText("");
                            final_res = "";
                        }
                    }
                });

        inputtx.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {

                String vendor_label = vendor_label_txt.getText().toString();

                if(keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN
                || keyEvent.getKeyCode() == KeyEvent.KEYCODE_SHIFT_LEFT && keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getKeyCode() == KeyEvent.KEYCODE_SHIFT_RIGHT && keyEvent.getAction() == KeyEvent.ACTION_DOWN
                || keyEvent.getKeyCode() == KeyEvent.KEYCODE_TAB && keyEvent.getAction() == KeyEvent.ACTION_DOWN){

                        String progress = null;
                    try {
                        progress = new breakupSCANNEDTEXT().execute().get();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if(final_res == "PASS" && progress == "done") {
                        vendor_label = vendor_label.trim();

                        if(material_number.equals(vendor_label)){
                            Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                            try {
                                photoFile = getPhotoFileUri(photoFileName);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Uri fileProvider = FileProvider.getUriForFile(ReelVerification.this, "com.codepath.fileprovider", photoFile);
                            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider);
                            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                                someActivityResultLauncher.launch(cameraIntent);
                            }
                        }
                        else{
                            final_res = "FAIL";
                            tv_change_status = (TextView) findViewById(R.id.tv_status);
                            tv_change_status.setText(final_res);
                            tv_change_status.setTextColor(Color.RED);
                            pb2.setVisibility(View.INVISIBLE);
                            vendor_label_txt.requestFocus();
                            vendor_label_txt.setText("");
                            inputtx.setText("");
                            final_res = "";

                        }

                    }
                    else{
                        Toast.makeText(ReelVerification.this, "PASS REEL FIRST BEFORE TAKE PHOTO", Toast.LENGTH_SHORT).show();

                        vendor_label_txt.setText("");
                        inputtx.setText("");
                        final_res = "";
                        vendor_label_txt.requestFocus();
                    }


                }
                else{

                }

                return false;
            }
        });
    }

    // Returns the File for a photo stored on disk given the fileName
    public File getPhotoFileUri(String fileName) throws IOException {
        // Get safe storage directory for photos
        // Use `getExternalFilesDir` on Context to access package-specific directories.
        // This way, we don't need to request external read/write runtime permissions.
        File mediaStorageDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), APP_TAG);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()){
            Log.d(APP_TAG, "failed to create directory");
        }

        // Return the file target for the photo based on filename
        File file = new File(mediaStorageDir.getPath() + File.separator + fileName);

        return file;
    }

    private String encodeImage(Bitmap bm)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG,100,baos);
        byte[] b = baos.toByteArray();
        String encImage = Base64.encodeToString(b, Base64.DEFAULT);

        return encImage;

    }

    private class StoreDataAsyncTask extends AsyncTask<String, Void, Void> {

        public String build_utl(String poto,String barcode){
            pb2.setVisibility(View.VISIBLE);
            String init = "";
            return init;
        }

        public String get_login_result_from_url(String brcode,String poto) {
            String result_from_api = "";
            String pay_load = "[\"" + poto + "\"," + "\"" + brcode + "\"]";

            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            try{
                HttpPost request = new HttpPost("http://172.16.206.19/REST_API_CORE/API/REEL_PHOTO");
                StringEntity params = new StringEntity(pay_load);
                request.addHeader("content-type", "application/json");
                request.setEntity(params);
                CloseableHttpResponse response = httpClient.execute(request);
                String res = response.toString();

            }
            catch (Exception ex) {
                result_from_api = ex.getMessage().toString();

            }
            finally {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }





            return result_from_api;
        }

        @Override
        protected Void doInBackground(String... params) {

            TextView inputtx = (TextView) findViewById(R.id.barcoetxt);

            String[] photobase64 = params;
            get_login_result_from_url(inputtx.getText().toString(),photobase64[0]);




            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            pb2.setVisibility(View.INVISIBLE);
            TextView inputtx = (TextView) findViewById(R.id.barcoetxt);
            vendor_label_txt.setText("");
            vendor_label_txt.requestFocus();
            inputtx.setText("");
            final_res = "";
            super.onPostExecute(result);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pb2.setVisibility(View.VISIBLE);
        }
    }
    String material_number = "";

    private class breakupSCANNEDTEXT extends AsyncTask<Void, Void, String> {



        @Override
        protected String doInBackground(Void... args) {
            TextView input = (TextView)findViewById(R.id.barcoetxt);
            String url = "http://172.16.206.19/REST_API/Home/breAKBarcodeString?barcode=" + input.getText();

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
                final_res = "FAIL";
                tv_change_status = (TextView) findViewById(R.id.tv_status);
                tv_change_status.setText(final_res);
                tv_change_status.setTextColor(Color.RED);
                //e.printStackTrace();
            }
            if(in != null){
                try {
                    BufferedReader br = new BufferedReader(in);
                    String line = br.readLine();
                    JSONObject json = new JSONObject(line);
                    String mat = json.getString("PART_NO");
                    material_number = mat;
                    String qty = json.getString("QUANTITY");
                    tv_reel_verify = (TextView) findViewById(R.id.tvreelverify);
                    tv_reel_verify.setText("Material : "+mat + "\nQuantity: " + qty );
                    final_res = "PASS";
                    tv_change_status = (TextView) findViewById(R.id.tv_status);
                    tv_change_status.setText(final_res);
                    tv_change_status.setTextColor(Color.GREEN);

                } catch (IOException | JSONException e) {
                    final_res = "FAIL";
                    tv_change_status = (TextView) findViewById(R.id.tv_status);
                    tv_change_status.setText(final_res);
                    tv_change_status.setTextColor(Color.RED);
                    pb2.setVisibility(View.INVISIBLE);

                    //e.printStackTrace();
                }
            }






            return "done";
        }

        //@Override
        protected void onPostExecute(Void result) {
            pb2.setVisibility(View.INVISIBLE);
            super.onPostExecute(String.valueOf(result));
        }

        @Override
        protected void onPreExecute() {
            pb2.setVisibility(View.VISIBLE);
            super.onPreExecute();

        }
    }

}