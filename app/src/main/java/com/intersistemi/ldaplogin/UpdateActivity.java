package com.intersistemi.ldaplogin;

import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.enums.Display;
import com.github.javiersantos.appupdater.enums.UpdateFrom;

public class UpdateActivity extends AppCompatActivity implements DialogInterface.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);

        AppUpdater appUpdater = new AppUpdater(this)
                .setDisplay(Display.NOTIFICATION)
                .setUpdateFrom(UpdateFrom.JSON)
                .setUpdateJSON("")
                .showEvery(5)
                .setTitleOnUpdateAvailable("Update available")
                .setContentOnUpdateAvailable("Check out the latest version available of my app!")
                .setTitleOnUpdateNotAvailable("Update not available")
                .setContentOnUpdateNotAvailable("No update available. Check for updates again later!")
                .setButtonUpdate("Update now?")
                .setButtonUpdateClickListener(this)
                .setButtonDismiss("Maybe later")
                .setButtonDismissClickListener(this)
                .setButtonDoNotShowAgain("Huh, not interested")
                .setButtonDoNotShowAgainClickListener(this)
                .setIcon(R.drawable.ic_update_black_24dp) // Notification icon
                .setCancelable(false) // Dialog could not be dismissable
                ;
        appUpdater.start();


    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {

    }
}