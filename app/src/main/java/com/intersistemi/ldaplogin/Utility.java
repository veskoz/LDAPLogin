package com.intersistemi.ldaplogin;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Utility {

    Context context;

    public Utility() {
    }

    public Utility(Context context) {
        this.context = context;
    }


    public void showPreferences(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Map<String, ?> allEntries = preferences.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            Log.d("map values", entry.getKey() + ": " + entry.getValue().toString());
        }
    }

    public void crash() {
        String crashString = null;
        crashString.length();
    }

    public boolean deleteDB(Context context) {
        return context.deleteDatabase(DatabaseHelper.DB_NAME);
    }

    /*
     * method taking two arguments
     * name that is to be saved and id of the name from SQLite
     * if the name is successfully sent
     * we will update the status as synced in SQLite
     * */
    public void saveName(final int id, final String barcode, String ldap_user, long time, String url, DatabaseHelper db) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject obj = new JSONObject(response);
                            if (!obj.getBoolean("error")) {
                                //updating the status in sqlite
                                db.updateNameStatus(id, Constants.NAME_SYNCED_WITH_SERVER, ldap_user, time);

                                //sending the broadcast to refresh the list
                                context.sendBroadcast(new Intent(Constants.DATA_SAVED_BROADCAST));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("TAG", error.getMessage());
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

        VolleySingleton.getInstance(context).addToRequestQueue(stringRequest);
    }

    public void iterateList(DatabaseHelper db, String url) {
        ConnectivityManager cm = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
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
                        saveName(
                                cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID)),
                                cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_BARCODE)),
                                cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_LDAP_USER)),
                                cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TIME)),
                                url,db
                        );
                    }
                }
            }
        }
    }
}
