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

import static com.intersistemi.ldaplogin.Constants.NAME_NOT_SYNCED_WITH_SERVER;

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

    /**
     * method that try to save the sample to server and sync status
     *
     * @param id        id to save
     * @param barcode   barcode to save
     * @param ldap_user ldap_user to save
     * @param time      time to save
     * @param url       url of the php page to save samples
     * @param db        local database
     */
    public void saveSample(final int id, final String barcode, String ldap_user, long time, String url, DatabaseHelper db) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject obj = new JSONObject(response);
                            if (!obj.getBoolean("error")) {
                                //updating the status in sqlite
                                db.updateSampleStatus(id, Constants.NAME_SYNCED_WITH_SERVER, obj.getString("message"));
                                //sending the broadcast to refresh the list
                            } else {
                                if (!obj.getString("message").isEmpty()) {
                                    db.updateSampleStatus(id, Constants.ERROR_CODE, obj.getString("message"));
                                } else {
                                    db.updateSampleStatus(id, NAME_NOT_SYNCED_WITH_SERVER, obj.getString("message"));
                                }
                            }
                            //sending the broadcast to refresh the list
                            context.sendBroadcast(new Intent(Constants.DATA_SAVED_BROADCAST));
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

        VolleySingleton.getInstance(context).addToRequestQueue(stringRequest);
    }

    /**
     * Loop the list and try to save data (by calling saveSample)
     * on the server and sync status
     *
     * @param db  local db
     * @param url url to the php save that saves to the server's db
     */
    public void iterateList(DatabaseHelper db, String url) {
        ConnectivityManager cm = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        //if there is a network
        if (activeNetwork != null) {
            //if connected to wifi or mobile data plan
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI || activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {

                //getting all the unsynced samples
                try (Cursor cursor = db.getUnsyncedSamples()) {
                    while (cursor.moveToNext()) {
                        //calling the method to save the unsynced sample to MySQL
                        saveSample(
                                cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID)),
                                cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_BARCODE)),
                                cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_LDAP_USER)),
                                cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TIME)),
                                url, db
                        );
                    }
                }
            }
        }
    }
}
