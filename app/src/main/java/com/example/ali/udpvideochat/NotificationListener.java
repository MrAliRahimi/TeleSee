package com.example.ali.udpvideochat;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Created by ali on 8/7/2017.
 */
public class NotificationListener  extends BroadcastReceiver {
    private static final String TAG = "NotificationListener";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.hasExtra("NOTIFICATION_ID") && intent.getIntExtra("NOTIFICATION_ID",0)==Constants.SERVICE_NOTIFICATION_ID){
            String action = intent.getAction();
            handleEvent(context, action);}
    }

    synchronized public static void handleEvent(Context context, String action) {
        try {
            Log.i(TAG, "notification received:" + action);

            if (action != null && action.equals("Deactivate")) {
                //setNotificationServiceDisable(context);
                context.sendBroadcast(new Intent("CLOSE_APP"));
                Intent service = new Intent(context, CommunicationService.class);
                service.setAction(Constants.STOP_ACTION);
                context.startService(service);
                NotificationListener.setNotificationServiceDisable(context);
                //doUnbindService(context);
            } else if (action != null && action.equals("Activate")) {
                Intent service = new Intent(context, CommunicationService.class);
                service.setAction(Constants.STARTFORGROUND_ACTION);
                context.startService(service);

                //setNotificationServiceEnable(context);
                //doBindService(context);
            } else {
                //do nothing
                //disableService(context);
            }
        } catch (Exception e) {
            Log.e(TAG,"handleEvent"+e.getMessage());
            e.printStackTrace();
            setNotificationServiceDisable(context);
        }
    }

    public static Notification getNotificationServiceEnable(Context context){
        Log.i(TAG, "getNotificationServiceEnable");
        String title="Deactivate service";//getTitle(1,context);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        try {
            Intent Intent2 = new Intent(context, NotificationListener.class);
            Intent2.putExtra("NOTIFICATION_ID", Constants.SERVICE_NOTIFICATION_ID);
            Intent2.setAction("Deactivate");
            PendingIntent pendingIntent2 = PendingIntent.getBroadcast(context, Constants.SERVICE_NOTIFICATION_ID, Intent2, 0);
            Notification notification = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText("test")
                    .setContentIntent(pendingIntent2)
                    .setColor(Color.GREEN)
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_MAX).build();
            return notification;
            //nm.notify(Constants.SERVICE_NOTIFICATION_ID, builder.build());

        }catch (Exception e){
            Log.e(TAG,"setNotificationServiceEnable:"+e.getMessage());
            e.printStackTrace();
            setNotificationServiceDisable(context);
        }
        return null;
    }

    public static void setNotificationServiceEnable(Context context){
        Log.i(TAG, "setNotificationServiceEnable");
        String title="Deactivate service";//getTitle(1,context);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        try {
            Intent Intent2 = new Intent(context, NotificationListener.class);
            Intent2.putExtra("NOTIFICATION_ID", Constants.SERVICE_NOTIFICATION_ID);
            Intent2.setAction("Deactivate");
            PendingIntent pendingIntent2 = PendingIntent.getBroadcast(context, Constants.SERVICE_NOTIFICATION_ID, Intent2, 0);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText("test")
                    .setContentIntent(pendingIntent2)
                    .setColor(Color.GREEN)
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_MAX);
            nm.notify(Constants.SERVICE_NOTIFICATION_ID, builder.build());

        }catch (Exception e){
            Log.e(TAG,"setNotificationServiceEnable:"+e.getMessage());
            e.printStackTrace();
            setNotificationServiceDisable(context);
        }
    }

    public static void setNotificationServiceDisable(Context context){
        Log.i(TAG, "setNotificationServiceDisable");
        String title="Active service";//getTitle(0,context);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        try {
            Intent Intent1 = new Intent(context, NotificationListener.class);
            Intent1.putExtra("NOTIFICATION_ID", Constants.SERVICE_NOTIFICATION_ID);
            Intent1.setAction("Activate");
            PendingIntent pendingIntent1 = PendingIntent.getBroadcast(context, Constants.SERVICE_NOTIFICATION_ID, Intent1, 0);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText("test")
                    .setContentIntent(pendingIntent1)
                    .setColor(Color.RED)
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_MAX);
            nm.notify(Constants.SERVICE_NOTIFICATION_ID, builder.build());
        }catch (Exception e){
            Log.e(TAG,"setNotificationServiceDisable:"+e.getMessage());
            e.printStackTrace();
        }
    }

    public static void setActionNotification(Context context,String title){
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        try {
            Notification builder1 = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    //.setContentIntent(contentIntent)
                    .setContentTitle(title)
                    .setColor(Color.RED)
                    .setVibrate(new long[]{1000})
                    .setAutoCancel(true)
                    .setPriority(Notification.PRIORITY_MAX).build();
            nm.notify(Constants.EVENT_NOTIFICATION_ID, builder1);
        }catch (Exception e){
            Log.e(TAG,"setActionNotification:"+e.getMessage());
            e.printStackTrace();
        }
    }


}
