package com.example.ali.udpvideochat;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * Created by ali on 1/17/2017.
 */

/**
 * this listener is used to monitor wifi status change
 * if wifi is connected and isListenWifiChange is true then start to DNS-SD service to register and discover
 * else stop the DNS-SD service if it is started and unregister
 */

public class WifiChangedReceiver extends BroadcastReceiver {
    public static final String TAG = "wifiChangedReceiver";
    private static Intent mIntent;

    @Override
    public void onReceive(final Context context, Intent intent) {
        mIntent = intent;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.i(TAG,"onReceive:"+"Received"+mIntent.getExtras().toString());

                    String isListenWifiChange = Tools.getPreference(context,Constants.PREFS_NAME, Constants.PREF_IS_LISTEN_WIFI_CHANGE);
                    if("true".equals(isListenWifiChange)){
                        final String action = mIntent.getAction();
                        if (action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                            //this is required to make sure android detects connection lost and connected wifi becomes null
                            Tools.threadSleep(10000);
                            Intent service = new Intent(context, CommunicationService.class);
                            service.setAction(Constants.STARTFORGROUND_ACTION);
                            context.startService(service);
                        }
                    }else{
                        // do nothing
                    }
                }catch (Exception e){
                    Log.e(TAG,"onReceive:"+e.getStackTrace());
                }
            }
        }).start();
    }

}

