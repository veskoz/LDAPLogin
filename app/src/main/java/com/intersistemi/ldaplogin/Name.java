package com.intersistemi.ldaplogin;

public class Name {
    private final String barcode;
    private final int status;
    private final String ldap_user;
    private final long time;

    public Name(String barcode, int status, String ldap_user, long time) {
        this.barcode = barcode;
        this.status = status;
        this.ldap_user = ldap_user;
        this.time = time;
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

}
