package com.intersistemi.ldaplogin;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.RequiresApi;

public class NetworkStateChecker extends BroadcastReceiver {

    //hold the values of the url to the php page which saves into the db
    public String pref_url_save_name;
    //object to use for calling some methods
    Utility utility;

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onReceive(Context context, Intent intent) {

        //context and database helper object
        utility = new Utility(context);
        pref_url_save_name = context.getResources().getString(R.string.url_save_name_value);

        //database helper object
        DatabaseHelper db = new DatabaseHelper(context);

        utility.iterateList(db, pref_url_save_name);

    }
}
