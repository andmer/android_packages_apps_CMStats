package com.cyanogenmod.stats;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemProperties;
import android.util.Log;

public class ReportingService extends Service {
    private static final String PREF_NAME = "CMStats";
    private static final String TAG = "CMStats";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        if (isFirstBoot()) {
            promptUser();
        } else if (canReport() == true) {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    report();
                }
            };
            thread.start();
        }
    }

    private boolean isFirstBoot() {
        SharedPreferences settings = getSharedPreferences(PREF_NAME, 0);
        boolean firstboot = settings.getBoolean("firstboot", true);
        return firstboot;
    }

    private boolean canReport() {
        boolean vanilla = false;
        boolean optin = false;

        // Determine developer.
        String developerid = SystemProperties.get("ro.rommanager.developerid", null);
        if (developerid == "cyanogenmod" || developerid == "cyanogenmodnightly") {
            vanilla = true;
        }

        vanilla = true;

        // Determine opt-in status.
        SharedPreferences settings = getSharedPreferences(PREF_NAME, 0);
        optin = settings.getBoolean("optin", false);

        if (vanilla && optin) {
            return true;
        } else {
            return false;
        }
    }

    private void report() {
        String deviceId = Utilities.getUniqueID(getApplicationContext());
        String deviceName = Utilities.getDevice();
        String deviceVersion = Utilities.getModVersion();
        String deviceCountry = Utilities.getCountryCode(getApplicationContext());
        String deviceCarrier = Utilities.getCarrier(getApplicationContext());

        Log.d(TAG, "Device ID: " + deviceId);
        Log.d(TAG, "Device Name: " + deviceName);
        Log.d(TAG, "Device Version: " + deviceVersion);
        Log.d(TAG, "Country: " + deviceCountry);
        Log.d(TAG, "Carrier: " + deviceCarrier);

        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://cyanogenmodstats.appspot.com/submit");
        try {
            List<NameValuePair> kv = new ArrayList<NameValuePair>(5);
            kv.add(new BasicNameValuePair("id", deviceId));
            kv.add(new BasicNameValuePair("type", deviceName));
            kv.add(new BasicNameValuePair("version", deviceVersion));
            kv.add(new BasicNameValuePair("country", deviceCountry));
            kv.add(new BasicNameValuePair("carrier", deviceCarrier));
            httppost.setEntity(new UrlEncodedFormEntity(kv));
            httpclient.execute(httppost);
        } catch (Exception e) {
            Log.e(TAG, "Got Exception", e);
        }

        stopSelf();
    }

    private void promptUser() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification n = new Notification(R.drawable.icon,
                getString(R.string.notification_ticker), System.currentTimeMillis());
        Intent nI = new Intent(this, MainActivity.class);
        PendingIntent pI = PendingIntent.getActivity(getApplicationContext(), 0, nI, 0);
        n.setLatestEventInfo(getApplicationContext(),
                getString(R.string.notification_title),
                getString(R.string.notification_desc), pI);
        nm.notify(1, n);
    }
}
