package com.intersistemi.ldaplogin;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.intersistemi.ldaplogin.Constants.DATA_SAVED_BROADCAST;
import static com.intersistemi.ldaplogin.Constants.NAME_NOT_SYNCED_WITH_SERVER;
import static com.intersistemi.ldaplogin.Constants.NAME_SYNCED_WITH_SERVER;
import static com.intersistemi.ldaplogin.Constants.REQUEST_CAMERA_PERMISSION;

public class BarcodeActivity extends AppCompatActivity implements View.OnClickListener, NetworkStateReceiver.NetworkStateReceiverListener {

    //progress dialog to show when user want to refresh list
    public ProgressDialog progressDialog;
    //database helper object
    public DatabaseHelper db;
    //object to use for calling some methods
    public Utility utility;
    //hold the values of the url to the php page which saves into the db
    private String pref_url_save_name;
    //provides a dedicated drawing surface embedded inside the view hierarchy.
    private SurfaceView surfaceView;
    //manages the camera in conjunction with an underlying Detector.
    //this receives preview frames from the camera at a specified rate
    //sending those frames to the detector as fast as it is able to process those frames.
    private CameraSource cameraSource;
    //this class provides methods to play DTMF tones (ITU-T Recommendation Q.23)
    //call supervisory tones (3GPP TS 22.001, CEPT)
    //and proprietary tones (3GPP TS 31.111).
    private ToneGenerator toneGen1;
    //show what has been scanned
    private TextView barcodeText;
    //hold the value of the scan made with camera
    private String barcodeData;
    //list to show all scans has been done
    private ListView listViewSamples;
    //list to store all the sample
    private List<Sample> samples;
    //broadcast receiver to know the sync status and network changes
    private BroadcastReceiver broadcastReceiver;
    private NetworkStateReceiver networkStateReceiver;
    //adapterobject for list view
    private Sampledapter sampledapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Objects.requireNonNull(getSupportActionBar()).hide();
        }
        utility = new Utility(getApplicationContext());
        toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        surfaceView = findViewById(R.id.surface_view);
        barcodeText = findViewById(R.id.barcode_text);
        pref_url_save_name = this.getResources().getString(R.string.url_save_name_value);

        db = new DatabaseHelper(this);
        samples = new ArrayList<>();

        //View objects
        ImageButton buttonSave = findViewById(R.id.buttonSave);
        listViewSamples = findViewById(R.id.listViewSamples);
        ImageButton buttonRefreshList = findViewById(R.id.refreshList);

        //adding click listener to buttons
        buttonSave.setOnClickListener(this);
        buttonRefreshList.setOnClickListener(this);

        //calling the method to load all the stored samples
        loadSamples();

        //the broadcast receiver to update sync status
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadSamples();
            }
        };

        //registering the broadcast receiver to update sync status and network checker
        registerReceiver(broadcastReceiver, new IntentFilter(DATA_SAVED_BROADCAST));
        networkStateReceiver = new NetworkStateReceiver();
        networkStateReceiver.addListener(this);
        this.registerReceiver(networkStateReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        initialiseDetectorsAndSources();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        networkStateReceiver.removeListener(this);
        this.unregisterReceiver(networkStateReceiver);
    }

    @Override
    public void networkAvailable() {
        /* TODO: Your connection-oriented stuff here */
    }

    @Override
    public void networkUnavailable() {
        /* TODO: Your disconnection-oriented stuff here */
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Uscire?")
                .setMessage("Uscire e tornare alla login?")
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface arg0, int arg1) {
                        BarcodeActivity.super.onBackPressed();
                        unregisterReceiver(broadcastReceiver);
                    }
                }).create().show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.buttonSave:
                saveSampleToServer();
                break;

            case R.id.refreshList:
                progressDialog = new ProgressDialog(BarcodeActivity.this);
                progressDialog.setTitle(R.string.refreshlist);
                progressDialog.setMessage("Aggiorno lista...");
                progressDialog.setCancelable(false);
                progressDialog.setIndeterminate(true);
                progressDialog.show();
                new MyTask(this).execute();
                break;

            default:
                break;
        }
    }

    /**
     * method to initialise detector,camera,surfaceview in order to start scanning
     */
    private void initialiseDetectorsAndSources() {
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(com.google.android.gms.vision.barcode.Barcode.ALL_FORMATS)
                .build();

        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(1920, 1080)
                .setAutoFocusEnabled(true)
                .build();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                try {
                    if (ActivityCompat.checkSelfPermission(BarcodeActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraSource.start(surfaceView.getHolder());
                    } else {
                        ActivityCompat.requestPermissions(BarcodeActivity.this, new
                                String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<com.google.android.gms.vision.barcode.Barcode>() {
            @Override
            public void release() {
                // Toast.makeText(getApplicationContext(), "To prevent memory leaks barcode scanner has been stopped", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void receiveDetections(Detector.Detections<com.google.android.gms.vision.barcode.Barcode> detections) {
                final SparseArray<com.google.android.gms.vision.barcode.Barcode> barcodes = detections.getDetectedItems();
                if (barcodes.size() != 0) {

                    barcodeText.post(new Runnable() {
                        @Override
                        public void run() {

                            if (barcodes.valueAt(0).displayValue != null) {
                                barcodeText.removeCallbacks(null);
                                barcodeData = barcodes.valueAt(0).displayValue;
                            }
                            barcodeText.setText(barcodeData);
                            toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
                        }
                    });
                }
            }
        });
    }

    /**
     * this method will
     * load the samples from the database
     * with updated sync status
     */
    private void loadSamples() {
        samples.clear();
        Cursor cursor = db.getSamples();
        if (cursor.moveToFirst()) {
            do {
                Sample sample = new Sample(
                        cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_BARCODE)),
                        cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_STATUS)),
                        cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_LDAP_USER)),
                        cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TIME))
                );
                samples.add(sample);
            } while (cursor.moveToNext());
        }
        sampledapter = new Sampledapter(this, R.layout.samples, samples);
        listViewSamples.setAdapter(sampledapter);
    }

    /**
     * this method will simply refresh the list
     */
    private void refreshList() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sampledapter.notifyDataSetChanged();
            }
        });

    }

    /**
     * this method is saving the sample to the server on errors save to local db
     */
    private void saveSampleToServer() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Salvataggio...");
        progressDialog.show();

        final String barcode = barcodeText.getText().toString().trim();
        final String ldap_user = MainActivity.getDN();
        final long time = Calendar.getInstance().getTimeInMillis();

        StringRequest stringRequest = new StringRequest(Request.Method.POST, pref_url_save_name,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        progressDialog.dismiss();
                        try {
                            JSONObject obj = new JSONObject(response);
                            if (!obj.getBoolean("error")) {
                                //if there is a success
                                //storing the sample to sqlite with status synced
                                saveSampleToLocalStorage(barcode, NAME_SYNCED_WITH_SERVER, ldap_user, time);
                            } else {
                                //if there is some error
                                //saving the sample to sqlite with status unsynced
                                saveSampleToLocalStorage(barcode, NAME_NOT_SYNCED_WITH_SERVER, ldap_user, time);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressDialog.dismiss();
                        //on error storing the sample to sqlite with status unsynced
                        saveSampleToLocalStorage(barcode, NAME_NOT_SYNCED_WITH_SERVER, ldap_user, time);
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("barcode", barcode);
                params.put("ldap_user", ldap_user);
                params.put("time", String.valueOf(time));
                return params;
            }
        };
        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    /**
     * saving the sample to local storage
     *
     * @param barcode   barcode to store
     * @param status    status to store
     * @param ldap_user ldap_user to store
     * @param time      time to store
     */
    private void saveSampleToLocalStorage(String barcode, int status, String ldap_user, long time) {
        barcodeText.setText("");
        db.addSample(barcode, status, ldap_user, time);
        Sample n = new Sample(barcode, status, ldap_user, time);
        samples.add(n);
        refreshList();
    }

    /**
     * Async task to help showing progress dialog, iterating the list
     * and send data to server
     */
    private static class MyTask extends AsyncTask<Void, Void, String> {
        private WeakReference<BarcodeActivity> activityReference;

        // only retain a weak reference to the activity
        MyTask(BarcodeActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(Void... params) {
            activityReference.get().refreshList();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                activityReference.get().utility.iterateList(activityReference.get().db, activityReference.get().pref_url_save_name);
            }
            return "task finished";
        }

        @Override
        protected void onPostExecute(String result) {
            BarcodeActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    activityReference.get().progressDialog.dismiss();
                }
            }, 3000);
        }
    }//end static class
}//end class