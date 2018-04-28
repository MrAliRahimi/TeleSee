package com.example.ali.udpvideochat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootCompleteReceiver extends BroadcastReceiver {
    public static final String TAG = "BootCompleteRec";

    @Override
    public void onReceive(Context context, Intent intent) {

        try{
            //// TODO: 5/24/2017 move set of runOnStartup to setting of app
            Tools.setPreference(context,Constants.PREFS_NAME,"true",Constants.PREF_RUN_ON_STARTUP);
            String runOnStartup = Tools.getPreference(context,Constants.PREFS_NAME,Constants.PREF_RUN_ON_STARTUP);
            if("true".equals(runOnStartup)) {
                Log.i(TAG, "BootCompleteReceiver started");
                NotificationListener.handleEvent(context, "Activate");
                //Intent service = new Intent(context, CommunicationService.class);
                //context.startService(service);
            }
        }catch (Exception e){
            Log.e(TAG,"onReceive:"+e.getMessage());
            e.printStackTrace();
        }
    }
}