package com.camera.simplemjpeg;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Calendar;

public class MjpegActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final boolean DEBUG = false;
    private static final String TAG = "MJPEG";


    MjpegView mv = null;
    String URL;

    // for settings (network and resolution)
    private static final int REQUEST_SETTINGS = 0;

    private int cmr_inp = 1;
    private int width = 176;
    private int height = 144;

    private int ip_ad1 = 192;
    private int ip_ad2 = 168;
    private int ip_ad3 = 43;
    private int ip_ad4 = 72;
    private int ip_port = 8080;
    private String ip_command = "?action=stream";

    private boolean suspending = false;

    final Handler handler = new Handler();
    ///////////////////////////////////
    final int updateFreqMs = 30; // call update every 1000 ms 1000/fps

    RelativeLayout rl;
    TextView yer_textview, tarih_textview, zaman_textview, hos_text, mvct_text, grl_text, kpst_text;

    //Gauge gauge;
    ImageView img;
    int kapasite = 3;
    int giris = 0;
    String mvct, grl;
    Mat mat1, frame;

    FaceOverlayView mFaceOverlayView;

    BaseLoaderCallback baseLoaderCallback;
    CameraBridgeViewBase cameraBridgeViewBase;
    //JavaCameraView cameraBridgeViewBase;

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    Drawable drawable;
    Bitmap bmp;

    int count = 0;

    //////////////////////////////


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //?action=stream
        //videofeed

        SharedPreferences preferences = getSharedPreferences("SAVED_VALUES", MODE_PRIVATE);
        cmr_inp = preferences.getInt("cmr_inp",cmr_inp);
        width = preferences.getInt("width", width);
        height = preferences.getInt("height", height);
        ip_ad1 = preferences.getInt("ip_ad1", ip_ad1);
        ip_ad2 = preferences.getInt("ip_ad2", ip_ad2);
        ip_ad3 = preferences.getInt("ip_ad3", ip_ad3);
        ip_ad4 = preferences.getInt("ip_ad4", ip_ad4);
        ip_port = preferences.getInt("ip_port", ip_port);
        ip_command = preferences.getString("ip_command", ip_command);

        StringBuilder sb = new StringBuilder();
        String s_http = "http://";
        String s_dot = ".";
        String s_colon = ":";
        String s_slash = "/";
        sb.append(s_http);
        sb.append(ip_ad1);
        sb.append(s_dot);
        sb.append(ip_ad2);
        sb.append(s_dot);
        sb.append(ip_ad3);
        sb.append(s_dot);
        sb.append(ip_ad4);
        sb.append(s_colon);
        sb.append(ip_port);
        sb.append(s_slash);
        sb.append(ip_command);
        URL = new String(sb);


        setContentView(R.layout.main);
        mv = findViewById(R.id.mv);
        if (mv != null) {
            mv.setResolution(width, height);
            mv.setAlpha(0);
        }

        setTitle(R.string.title_connecting);
        new DoRead().execute(URL);

        ///////////////////////////////////////////// on
        img = findViewById(R.id.imageView2);

        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        String month_format, day_format = "";
        if (month < 9) month_format = "0" + (month+1);
        else month_format = String.valueOf(month+1);

        if (day < 10) day_format = "0" + day;
        else day_format = String.valueOf(day);

        yer_textview = findViewById(R.id.textView2);
        tarih_textview = findViewById(R.id.textView3);
        tarih_textview.setText(String.format("%4s-%2s-%s",year,month_format,day_format));
        zaman_textview = findViewById(R.id.textView4);
        rl = findViewById(R.id.myRelLay);
        hos_text = findViewById(R.id.textView);
        kpst_text = findViewById(R.id.TextNumber1);
        kpst_text.setText(Integer.toString(kapasite));
        mvct_text = findViewById(R.id.TextNumber2);
        grl_text = findViewById(R.id.TextNumber3);
        //gauge = (Gauge)findViewById(R.id.gauge);

        mFaceOverlayView = findViewById( R.id.face_overlay );
        ///////////////////////////////////////////////

        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        cameraBridgeViewBase = (JavaCameraView)findViewById(R.id.myCameraView);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);
        cameraBridgeViewBase.setAlpha(0);


        baseLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);
                switch(status){
                    case BaseLoaderCallback.SUCCESS:
                        cameraBridgeViewBase.enableView();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }
            }
        };

        //PERMISSION
        checkPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE);

        // Check if Google Play Services is installed and its version is at least 20.12.14
        // On Android 8.1 Go devices ML Kit requires at least version 20.12.14 to be able to download models properly without a reboot
        int result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this, 201214000);
        if (result != ConnectionResult.SUCCESS) {
            if (GoogleApiAvailability.getInstance().isUserResolvableError(result)) {
                GoogleApiAvailability.getInstance().getErrorDialog(this, result, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }
        }

    }

    public void checkPermission(String permission, int requestCode)
    {
        if (ContextCompat.checkSelfPermission(MjpegActivity.this, permission)
                == PackageManager.PERMISSION_DENIED) {

            // Requesting the permission
            ActivityCompat.requestPermissions(MjpegActivity.this,
                    new String[] { permission },
                    requestCode);
        }
        else {
            Toast.makeText(MjpegActivity.this,
                    "Permission already granted",
                    Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super
                .onRequestPermissionsResult(requestCode,
                        permissions,
                        grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MjpegActivity.this,
                        "Camera Permission Granted",
                        Toast.LENGTH_SHORT)
                        .show();
            }
            else {
                Toast.makeText(MjpegActivity.this,
                        "Camera Permission Denied",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mat1 = new Mat(width, height, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mat1.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mat1 = inputFrame.rgba();

        //flip frame
        frame = mat1.t();
        Core.flip(mat1.t(), frame, 1);
        //Imgproc.resize(frame, frame, new Size(640, 480));
        Imgproc.resize(frame, frame, mat1.size());
        //Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);
        //Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2GRAY);
        //Imgproc.GaussianBlur(frame, frame, new Size(21, 21), 0);

        //Imgproc.line(frame, new Point(frame.width()/2, 0), new Point(frame.width()/2, frame.height()), new Scalar(0, 255, 0), 2);   //frame.width()/2 = 160, frame.height() = 720

        // Mat to Bitmap
        final Bitmap bmp1 = Bitmap.createBitmap(frame.cols(), frame.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(frame, bmp1);
        //MjpegActivity.this.runOnUiThread(new Runnable() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFaceOverlayView.setBitmap(bmp1);
            }
        });

        return frame;
    }

    public void onResume() {
        if (DEBUG) Log.d(TAG, "onResume()");
        super.onResume();
        if (mv != null) {
            if (suspending) {
                new DoRead().execute(URL);
                suspending = false;
            }
        }
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, baseLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateTime();
                handler.postDelayed(this, updateFreqMs);
            }
        }, updateFreqMs);
    }

    @SuppressLint("SetTextI18n")
    private void updateTime() {
        /*imageView.setDrawingCacheEnabled(true);
        Bitmap bmp2 = imageView.getDrawingCache();
        mFaceOverlayView.setBitmap(bmp2);*/

        if(cmr_inp == 1)
            cameraBridgeViewBase.enableView();
        else {
            cameraBridgeViewBase.disableView();
            if (mv.bmp != null)
                mFaceOverlayView.setBitmap(mv.bmp);
        }

        int hh = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int min = Calendar.getInstance().get(Calendar.MINUTE);
        int sec = Calendar.getInstance().get(Calendar.SECOND);

        String hh_format, min_format, sec_format;
        if (hh < 10) hh_format = "0" + hh;
        else hh_format = String.valueOf(hh);

        if (min < 10) min_format = "0" + min;
        else min_format = String.valueOf(min);

        if (sec < 10) sec_format = "0" + sec;
        else sec_format = String.valueOf(sec);

        zaman_textview.setText(String.format("%2s:%2s:%2s",hh_format,min_format,sec_format));

        giris = mFaceOverlayView.giris;
        mvct = Integer.toString(giris);
        grl = Integer.toString((kapasite-giris));
        mvct_text.setText(mvct);
        grl_text.setText(grl);
        //grl_text.setText(top_i);

        if (giris > kapasite) {
            String hos = "Lütfen Bekleyiniz";
            ////hos_text.setText(hos);
            //gauge.setValue(kapasite);

            rl.setBackgroundColor(Color.parseColor("#F44336"));        // --> kırmızı
            img.setImageResource(R.drawable.standing);
        }
        else{
            //gauge.setValue(cikis);
        }
    }

    public void onStart() {
        if (DEBUG) Log.d(TAG, "onStart()");
        super.onStart();
    }

    public void onPause() {
        if (DEBUG) Log.d(TAG, "onPause()");
        super.onPause();
        if (mv != null) {
            if (mv.isStreaming()) {
                mv.stopPlayback();
                suspending = true;
            }
        }
        if(cameraBridgeViewBase!=null)
            cameraBridgeViewBase.disableView();

        handler.removeCallbacksAndMessages(null);
    }

    public void onStop() {
        if (DEBUG) Log.d(TAG, "onStop()");
        super.onStop();
    }

    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "onDestroy()");

        if (mv != null)
            mv.freeCameraMemory();

        if (cameraBridgeViewBase != null)
            cameraBridgeViewBase.disableView();

        super.onDestroy();
    }

    @SuppressLint("ResourceType")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                Intent settings_intent = new Intent(MjpegActivity.this, SettingsActivity.class);
                settings_intent.putExtra("cmr_inp", cmr_inp);
                settings_intent.putExtra("width", width);
                settings_intent.putExtra("height", height);
                settings_intent.putExtra("ip_ad1", ip_ad1);
                settings_intent.putExtra("ip_ad2", ip_ad2);
                settings_intent.putExtra("ip_ad3", ip_ad3);
                settings_intent.putExtra("ip_ad4", ip_ad4);
                settings_intent.putExtra("ip_port", ip_port);
                settings_intent.putExtra("ip_command", ip_command);
                startActivityForResult(settings_intent, REQUEST_SETTINGS);
                return true;
        }
        return false;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SETTINGS:
                if (resultCode == AppCompatActivity.RESULT_OK) {
                    cmr_inp = data.getIntExtra("cmr_inp", cmr_inp);
                    width = data.getIntExtra("width", width);
                    height = data.getIntExtra("height", height);
                    ip_ad1 = data.getIntExtra("ip_ad1", ip_ad1);
                    ip_ad2 = data.getIntExtra("ip_ad2", ip_ad2);
                    ip_ad3 = data.getIntExtra("ip_ad3", ip_ad3);
                    ip_ad4 = data.getIntExtra("ip_ad4", ip_ad4);
                    ip_port = data.getIntExtra("ip_port", ip_port);
                    ip_command = data.getStringExtra("ip_command");

                    if (mv != null) {
                        mv.setResolution(width, height);
                    }
                    SharedPreferences preferences = getSharedPreferences("SAVED_VALUES", MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putInt("cmr_inp", cmr_inp);
                    editor.putInt("width", width);
                    editor.putInt("height", height);
                    editor.putInt("ip_ad1", ip_ad1);
                    editor.putInt("ip_ad2", ip_ad2);
                    editor.putInt("ip_ad3", ip_ad3);
                    editor.putInt("ip_ad4", ip_ad4);
                    editor.putInt("ip_port", ip_port);
                    editor.putString("ip_command", ip_command);

                    editor.commit();

                    new RestartApp().execute();
                }
                break;
        }
    }

    public void setImageError() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                setTitle(R.string.title_imageerror);
                return;
            }
        });
    }

    public class DoRead extends AsyncTask<String, Void, MjpegInputStream> {
        protected MjpegInputStream doInBackground(String... url) {
            //TODO: if camera has authentication deal with it and don't just not work
            HttpResponse res = null;
            DefaultHttpClient httpclient = new DefaultHttpClient();
            HttpParams httpParams = httpclient.getParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, 5 * 1000);
            HttpConnectionParams.setSoTimeout(httpParams, 5 * 1000);
            if (DEBUG) Log.d(TAG, "1. Sending http request");
            try {
                res = httpclient.execute(new HttpGet(URI.create(url[0])));
                if (DEBUG)
                    Log.d(TAG, "2. Request finished, status = " + res.getStatusLine().getStatusCode());
                if (res.getStatusLine().getStatusCode() == 401) {
                    //You must turn off camera User Access Control before this will work
                    return null;
                }
                return new MjpegInputStream(res.getEntity().getContent());
            } catch (ClientProtocolException e) {
                if (DEBUG) {
                    e.printStackTrace();
                    Log.d(TAG, "Request failed-ClientProtocolException", e);
                }
                //Error connecting to camera
            } catch (IOException e) {
                if (DEBUG) {
                    e.printStackTrace();
                    Log.d(TAG, "Request failed-IOException", e);
                }
                //Error connecting to camera
            }
            return null;
        }

        protected void onPostExecute(MjpegInputStream result) {
            mv.setSource(result);

            if (cmr_inp == 1)
                setTitle(R.string.title_android);
            else{
                if (result != null) {
                    result.setSkip(1);
                    setTitle(R.string.app_name);
                } else
                    setTitle(R.string.title_disconnected);
            }

            mv.setDisplayMode(MjpegView.SIZE_BEST_FIT);
            mv.showFps(true);


        }
    }

    public class RestartApp extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... v) {
            MjpegActivity.this.finish();
            return null;
        }

        protected void onPostExecute(Void v) {
            startActivity((new Intent(MjpegActivity.this, MjpegActivity.class)));
        }
    }

}