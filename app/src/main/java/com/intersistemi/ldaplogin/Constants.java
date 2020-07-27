package com.intersistemi.ldaplogin;

public class Constants {
    public static final int REQUEST_CAMERA_PERMISSION = 201;
    //a broadcast to know weather the data is synced or not
    public static final String DATA_SAVED_BROADCAST = "com.intersistemi.datasaved";
    public static final String LOG_TAG_MainActivity = "MainActivity";
    public static final String LOG_TAG_BarcodeActivity = "BarcodeActivity";
    public static final String LOG_TAG_Utility = "Utility";

    public static final int PERMISSION_REQUEST_CODE = 200;
    //1 means data is synced and 0 means data is not synced
    public static final int NAME_SYNCED_WITH_SERVER = 1;
    public static final int NAME_NOT_SYNCED_WITH_SERVER = 0;
    public static final int ERROR_CODE = 2;
}
