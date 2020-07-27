package com.intersistemi.ldaplogin;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.facebook.stetho.Stetho;
import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.enums.Display;
import com.github.javiersantos.appupdater.enums.UpdateFrom;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchScope;

import static com.intersistemi.ldaplogin.Constants.PERMISSION_REQUEST_CODE;

public class MainActivity extends AppCompatActivity {

    //dn of the user logged in
    private static String dn;
    //progress dialog to show when user want to log in
    public ProgressDialog progressDialog;
    //variables holding values of prefernces
    public String pref_address, pref_base_dn, pref_url_save_name, pref_bind_dn, pref_password;
    public int pref_port;
    //create a new, unestablished connection.
    LDAPConnection c;
    //variables holding values inserted by user
    String username, password;
    Toolbar toolbar;
    //ui objects for username and password
    EditText editTextUsername, editTextPassword;
    //ui object for logging in
    ImageButton imageButtonLogin;
    //ui object for password reset
    Button buttonResetPassword;

    /**
     * @return Dn of who is logged in
     */
    public static String getDN() {
        return dn;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        editTextUsername = findViewById(R.id.username);
        editTextPassword = findViewById(R.id.password);
        imageButtonLogin = findViewById(R.id.signin);
        buttonResetPassword = findViewById(R.id.reset_password);

        imageButtonLogin.setVisibility(View.INVISIBLE);

        if (checkPermission()) imageButtonLogin.setVisibility(View.VISIBLE);
        else requestPermission();

        //Setting default values
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false);

        editTextUsername.setText("avescovi");
        editTextPassword.setText("12345678");

        Stetho.initializeWithDefaults(this);

        imageButtonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                username = editTextUsername.getText().toString().trim();
                password = editTextPassword.getText().toString();

                progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setTitle(R.string.logging_in);
                progressDialog.setMessage("Attendere...");
                progressDialog.setCancelable(false);
                progressDialog.setIndeterminate(true);
                progressDialog.show();

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                pref_port = Integer.parseInt(String.valueOf(sharedPreferences.getString("ldap_port_key", null)));
                pref_address = sharedPreferences.getString("ldap_address_key", null);
                pref_base_dn = sharedPreferences.getString("ldap_base_dn_key", null);
                pref_url_save_name = sharedPreferences.getString("url_save_name_key", null);
                pref_bind_dn = sharedPreferences.getString("ldap_bind_dn_key", null);
                pref_password = sharedPreferences.getString("ldap_bind_password_key", null);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            c = new LDAPConnection(pref_address, pref_port, pref_bind_dn, pref_password);
                            authenticate2(username, password);
                        } catch (LDAPException e) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    Toast.makeText(MainActivity.this, "LDAP non raggiungibile", Toast.LENGTH_SHORT).show();
                                }
                            });
                            e.printStackTrace();
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                            }
                        });
                    }
                }).start();
            }
        });//onClick
    }//onCreate

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_SHORT).show();
                imageButtonLogin.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED) {
                        showMessageOKCancel(
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        requestPermission();
                                    }
                                }
                        );
                    }
                }
            }
        }
    }

    /**
     * Inflate the menu; this adds items to the action bar if it is present.
     * instantiate menu XML files into Menu objects.
     *
     * @param menu Menu object to instantiate XML files
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * This hook is called whenever an item in your options menu is selected.
     * Handle action bar item clicks here. The action bar will
     * automatically handle clicks on the Home/Up button, so long
     * as you specify a parent activity in AndroidManifest.xml.
     *
     * @param item MenuItem
     * @return true
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;

            case R.id.action_update:
                update();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Method to authenticate the user to AD through LDAP.
     * In order to make the authentication let's try to find it
     * while binding an admin user
     *
     * @param username username to authenticate
     * @param password password to authenticate
     * @throws LDAPException Invalid credentials
     */
    public void authenticate2(String username, String password) throws LDAPException {
        Filter searchForUserFilter = Filter.createORFilter(
                Filter.createEqualityFilter("uid", username),
                Filter.createEqualityFilter("mail", username)
        );

        SearchResult sr = c.search(pref_base_dn, SearchScope.SUB, searchForUserFilter);

        if (sr.getEntryCount() == 0) {
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MainActivity.this, "Combinazione utente/password errata.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            dn = sr.getSearchEntries().get(0).getDN();
            try {
                c = new LDAPConnection(pref_address, pref_port, dn, password);
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(MainActivity.this, "Accesso eseguito", Toast.LENGTH_SHORT).show();
                    }
                });
                Intent intent = new Intent(MainActivity.this, BarcodeActivity.class);
                startActivity(intent);
            } catch (LDAPException e) {
                if (e.getResultCode() == ResultCode.INVALID_CREDENTIALS) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this, "Combinazione utente/password errata.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                throw e;
            }
        }
    }

    /**
     * Method to show a message to the user who denied permission to camera
     *
     * @param okListener DialogInterface.OnClickListener
     */
    private void showMessageOKCancel(DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage("E' necessario dare il permesso all'uso della fotocamera")
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    /**
     * @return true if permission in granted; false otherwise
     */
    private boolean checkPermission() {
        // Permission is not granted
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request permission for camera
     */
    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                Constants.PERMISSION_REQUEST_CODE);
    }

    /**
     * Method to update app from github
     */
    private void update() {
        AppUpdater appUpdater = new AppUpdater(this)
                .setDisplay(Display.NOTIFICATION)
                .setUpdateFrom(UpdateFrom.JSON)
                .setUpdateJSON(this.getString(R.string.update_json))
                .setTitleOnUpdateAvailable("Aggiornamento disponibile")
                .setContentOnUpdateAvailable("Un aggiornamento è disponibile!")
                .setTitleOnUpdateNotAvailable("Aggiornamento non disponibile")
                .setContentOnUpdateNotAvailable("Non ci sono aggiornamenti disponibili al momento")
                .setButtonUpdate("Aggiornare ora?")
                .setButtonUpdateClickListener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setButtonDismiss("Forse più tardi")
                .setButtonDismissClickListener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setButtonDoNotShowAgain("L'aggiornamento è da eseguire.")
                .setButtonDoNotShowAgainClickListener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setIcon(R.drawable.ic_update_black_24dp) // Notification icon
                .setCancelable(false) // Dialog could not be dismissable
                ;
        appUpdater.start();
    }

}//class