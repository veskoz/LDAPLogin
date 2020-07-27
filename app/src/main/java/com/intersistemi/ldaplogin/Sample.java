package com.intersistemi.ldaplogin;

public class Sample {
    private final String barcode;
    private final int status;
    private final String ldap_user;
    private final long time;
    private final String status_text;

    public Sample(String barcode, int status, String ldap_user, long time, String status_text) {
        this.barcode = barcode;
        this.status = status;
        this.ldap_user = ldap_user;
        this.time = time;
        this.status_text = status_text;
    }

    public String getBarcode() {
        return barcode;
    }

    public int getStatus() {
        return status;
    }

    public String getLdap_user() {
        return ldap_user;
    }

    public long getTime() {
        return time;
    }

    public String getStatus_text() {
        return status_text;
    }
}
