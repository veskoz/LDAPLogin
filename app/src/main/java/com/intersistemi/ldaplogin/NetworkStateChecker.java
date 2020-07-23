package com.intersistemi.ldaplogin;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class NetworkStateChecker extends BroadcastReceiver {

    public String pref_url_save_name;
    //context and database helper object
    private Context context;
    private DatabaseHelper db;

    Utility utility;

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d("TAG", "onReceive of NetworkStateChecker");
        this.context = context;
        utility = new Utility(this.context);
        pref_url_save_name = context.getResources().getString(R.string.url_save_name_value);

        db = new DatabaseHelper(context);

        utility.iterateList(db,pref_url_save_name);
/*
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
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
*/

    }




}
