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
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
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

    public ProgressDialog progressDialog;
    //database helper object
    public DatabaseHelper db;
    //int mSomeMemberVariable = 123;
    private String pref_url_save_name;
    private SurfaceView surfaceView;
    private CameraSource cameraSource;
    private ToneGenerator toneGen1;
    private TextView barcodeText;
    private String barcodeData;
    private ListView listViewNames;
    //List to store all the names
    private List<Name> names;
    //Broadcast receiver to know the sync status
    private BroadcastReceiver broadcastReceiver;
    private NetworkStateReceiver networkStateReceiver;
    //adapterobject for list view
    private NameAdapter nameAdapter;
    public Utility utility;

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
        names = new ArrayList<>();

        //View objects
        ImageButton buttonSave = findViewById(R.id.buttonSave);
        listViewNames = findViewById(R.id.listViewNames);
        ImageButton buttonRefreshList = findViewById(R.id.refreshList);

        //adding click listener to button
        buttonSave.setOnClickListener(this);
        buttonRefreshList.setOnClickListener(this);

        //calling the method to load all the stored names
        loadNames();

        //the broadcast receiver to update sync status
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                //loading the names again
                loadNames();
            }
        };

        pref_url_save_name = getResources().getString(R.string.url_save_name_value);

        //registering the broadcast receiver to update sync status
        registerReceiver(broadcastReceiver, new IntentFilter(DATA_SAVED_BROADCAST));
        networkStateReceiver = new NetworkStateReceiver();
        networkStateReceiver.addListener(this);

        this.registerReceiver(networkStateReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        initialiseDetectorsAndSources();
    }

    @Override
    public void onResume() {
        Log.d("TAG", "onResume");
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.d("TAG", "onPause");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d("TAG", "onDestroy");
        super.onDestroy();
        networkStateReceiver.removeListener(this);
        this.unregisterReceiver(networkStateReceiver);
    }

    @Override
    public void networkAvailable() {
        Log.d("TAG", "network available");
        /* TODO: Your connection-oriented stuff here */
    }

    @Override
    public void networkUnavailable() {
        Log.d("TAG", "network unavailable");
        /* TODO: Your disconnection-oriented stuff here */
    }

    @Override
    public void onBackPressed() {
        Log.d("TAG", "onBackPressed");
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
                saveNameToServer();
                break;

            case R.id.refreshList:
                progressDialog = new ProgressDialog(BarcodeActivity.this);
                progressDialog.setTitle(R.string.refreshlist);
                progressDialog.setMessage("Aggiorno lista...");
                progressDialog.setCancelable(false);
                progressDialog.setIndeterminate(true);
                progressDialog.show();
                Log.d("TAG", "refreshing");
                new MyTask(this).execute();
                break;

            default:
                break;
        }
    }

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

                            if (barcodes.valueAt(0).email != null) {
                                barcodeText.removeCallbacks(null);
                                barcodeData = barcodes.valueAt(0).email.address;
                            } else {

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

    /*
     * this method will
     * load the names from the database
     * with updated sync status
     * */
    private void loadNames() {
        names.clear();
        Cursor cursor = db.getNames();
        if (cursor.moveToFirst()) {
            do {
                Name name = new Name(
                        cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_BARCODE)),
                        cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_STATUS)),
                        cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_LDAP_USER)),
                        cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TIME))
                );
                names.add(name);
            } while (cursor.moveToNext());
        }
        nameAdapter = new NameAdapter(this, R.layout.names, names);
        listViewNames.setAdapter(nameAdapter);
    }

    /*
     * this method will simply refresh the list
     * */
    private void refreshList() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                nameAdapter.notifyDataSetChanged();
            }
        });

    }

    /*
     * this method is saving the name to ther server
     * */
    private void saveNameToServer() {
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
                                //storing the name to sqlite with status synced
                                saveNameToLocalStorage(barcode, NAME_SYNCED_WITH_SERVER, ldap_user, time);
                            } else {
                                //if there is some error
                                //saving the name to sqlite with status unsynced
                                saveNameToLocalStorage(barcode, NAME_NOT_SYNCED_WITH_SERVER, ldap_user, time);
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
                        //on error storing the name to sqlite with status unsynced
                        saveNameToLocalStorage(barcode, NAME_NOT_SYNCED_WITH_SERVER, ldap_user, time);
                    }
                }) {
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

    //saving the name to local storage
    private void saveNameToLocalStorage(String barcode, int status, String ldap_user, long time) {
        barcodeText.setText("");
        db.addName(barcode, status, ldap_user, time);
        Name n = new Name(barcode, status, ldap_user, time);
        names.add(n);
        refreshList();
    }

/*
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void iterateList() {
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        //if there is a network
        if (activeNetwork != null) {
            //if connected to wifi or mobile data plan
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI || activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {

                //getting all the unsynced names
                try (Cursor cursor = db.getUnsyncedNames()) {
                    while (cursor.moveToNext()) {

                        //calling the method to save the unsynced name to MySQL
                        Log.d("TAG", " calling the method to save the unsynced name to MySQL");
                        utility.saveName(
                                cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID)),
                                cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_BARCODE)),
                                cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_LDAP_USER)),
                                cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TIME)),
                                pref_url_save_name,db
                        );
                    }
                }
            }
        }
    }
*/

/*
    public void saveName(final int id, final String barcode, String ldap_user, long time) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, pref_url_save_name,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject obj = new JSONObject(response);
                            if (!obj.getBoolean("error")) {
                                //updating the status in sqlite
                                db.updateNameStatus(id, Constants.NAME_SYNCED_WITH_SERVER, ldap_user, time);

                                //sending the broadcast to refresh the list
                                getApplicationContext().sendBroadcast(new Intent(Constants.DATA_SAVED_BROADCAST));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("barcode", barcode);
                params.put("ldap_user", ldap_user);
                params.put("time", String.valueOf(time));
                return params;

            }
        };
        VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(stringRequest);
    }
*/

    /*
     * method taking two arguments
     * name that is to be saved and id of the name from SQLite
     * if the name is successfully sent
     * we will update the status as synced in SQLite
     * */
    private static class MyTask extends AsyncTask<Void, Void, String> {
        private WeakReference<BarcodeActivity> activityReference;
        // only retain a weak reference to the activity
        MyTask(BarcodeActivity context) {
            activityReference = new WeakReference<>(context);
        }
        @Override
        protected void onPreExecute() { }

        @Override
        protected String doInBackground(Void... params) {
            activityReference.get().refreshList();
            /*
            First idea was to call connectivity_change in order to trigger the intent but
            seems like Permission Denial: not allowed to send broadcast android.net.conn.CONNECTIVITY_CHANGE
            Intent intent = new Intent("android.net.conn.CONNECTIVITY_CHANGE");
            activityReference.get().sendBroadcast(intent);
             */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                activityReference.get().utility.iterateList(activityReference.get().db,activityReference.get().pref_url_save_name);
            }
            return "task finished";
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d("TAG", "finshed");
            // get a reference to the activity if it is still there
            BarcodeActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    activityReference.get().progressDialog.dismiss();
                }
            }, 3000);
            // modify the activity's UI
            //TextView textView = activity.findViewById(R.id.textview);
            //textView.setText(result);

            // access Activity member variables
            //activity.mSomeMemberVariable = 321;
        }
    }//end static class
}//end class