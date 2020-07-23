package com.intersistemi.ldaplogin;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchScope;

import static com.intersistemi.ldaplogin.Constants.LOG_TAG_MainActivity;
import static com.intersistemi.ldaplogin.Constants.PERMISSION_REQUEST_CODE;

public class MainActivity extends AppCompatActivity {

    private static String dn;
    public ProgressDialog progressDialog;
    public String pref_address, pref_base_dn, pref_url_save_name, pref_bind_dn, pref_password;
    public int pref_port;
    LDAPConnection c;
    String username, password;
    Toolbar toolbar;
    EditText email, password2;
    ImageButton signin;
    Button signup;

    /**
     * @return Dn of who is connected.
     */
    public static String getDN() {
        return dn;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        deleteDatabase(DatabaseHelper.DB_NAME);

        toolbar = findViewById(R.id.toolbar);
        email = findViewById(R.id.email);
        password2 = findViewById(R.id.password2);
        signin = findViewById(R.id.signin);
        signup = findViewById(R.id.signup);

        signin.setEnabled(false);

        if (checkPermission()) signin.setEnabled(true);
        else requestPermission();

        //Setting default values
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false);

        email.setText("avescovi");
        password2.setText("12345678");

        signin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                username = email.getText().toString().trim();
                password = password2.getText().toString();

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
                signin.setEnabled(true);
                // main logic
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
                                });
                    }
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
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
            Log.d(LOG_TAG_MainActivity, "Combinazione utente/password errata.");
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MainActivity.this, "Combinazione utente/password errata.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            dn = sr.getSearchEntries().get(0).getDN();
            try {
                c = new LDAPConnection(pref_address, pref_port, dn, password);
                Log.d(LOG_TAG_MainActivity, "Login successful: " + dn);
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(MainActivity.this, "Accesso eseguito", Toast.LENGTH_SHORT).show();
                    }
                });
                Intent intent = new Intent(MainActivity.this, BarcodeActivity.class);
                startActivity(intent);
            } catch (LDAPException e) {
                if (e.getResultCode() == ResultCode.INVALID_CREDENTIALS) {
                    Log.d(LOG_TAG_MainActivity, "LDAPException: " + e.getMessage());
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


}//class