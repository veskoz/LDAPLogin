package com.intersistemi.ldaplogin;

import android.app.Application;
import android.content.Context;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;
import org.acra.annotation.AcraHttpSender;
import org.acra.annotation.AcraToast;
import org.acra.data.StringFormat;
import org.acra.sender.HttpSender;


@AcraCore(reportFormat = StringFormat.JSON)

@AcraHttpSender(uri = "http://192.168.1.16:8080/report",
                basicAuthLogin = "czhQb8l3bVfSabd8",
                basicAuthPassword = "CZiw77DU8tLH3RVm",
                httpMethod = HttpSender.Method.POST)

@AcraToast(resText=R.string.acra_toast_text)

public class AcraApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        // The following line triggers the initialization of ACRA
        ACRA.init(this);
        //ACRA.DEV_LOGGING = true;
    }
}