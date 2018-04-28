package com.example.ali.udpvideochat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.Camera;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.provider.Settings.Secure;
import android.widget.ExpandableListView;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Created by ali on 12/24/2016.
 */
public class Tools {
    public static final String TAG = "Tools";

    public static String padRight(final String s, int Length, char c) {
        String result = s;
        int S_Length = result.length();
        if (S_Length < Length) {
            for (int i = S_Length; i < Length; i++) {
                result = result + c;
            }
        }
        return result;
    }

    public static String padLeft(final String s, int Length, char c) {
        String result = s;
        int S_Length = result.length();
        if (S_Length < Length) {
            for (int i = S_Length; i < Length; i++) {
                result =  c + result;
            }
        }
        return result;
    }

    public static String getCRC(byte[] buf, int len) {
        int crc = 0xFFFF;
        int val = 0;

        for (int pos = 0; pos < len; pos++) {
            crc ^= (int) (0x00ff & buf[pos]);  // FIX HERE -- XOR byte into least sig. byte of crc

            for (int i = 8; i != 0; i--) {    // Loop over each bit
                if ((crc & 0x0001) != 0) {      // If the LSB is set
                    crc >>= 1;                    // Shift right and XOR 0xA001
                    crc ^= 0xA001;
                } else                            // Else LSB is not set
                    crc >>= 1;                    // Just shift right
            }
        }
        // Note, crc has low and high bytes swapped, so use it accordingly (or swap bytes)
        val = (crc & 0xff) << 8;
        val = val + ((crc >> 8) & 0xff);
        System.out.printf("Calculated a CRC of 0x%x, swapped: 0x%x\n", crc, val);
        /*if (val <= 0x0FFF) {
            return "0" + Integer.toHexString(val);
        } else {
            return Integer.toHexString(val);
        }*/
        return padLeft(Integer.toHexString(val),4,'0');

    }

    public static boolean haveSpace(String str) {
        return ( str == null || str.indexOf(" ")>-1);
    }

    public static boolean isEnglish(String str) {
        int length = str.length();
        for (int i = 0; i < length; i++) {
            char ch = str.charAt(i);
            Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
            if (!Character.UnicodeBlock.BASIC_LATIN.equals(block)) {
                return false;
            }
        }
        return true;
    }

    public static String getDateTimeString() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmm", Locale.ENGLISH);
        String strDate = sdf.format(c.getTime());
        return strDate;
    }

    public static boolean isValidTime(String time){

        String[] times = time.split(":");
        if(times.length != 2 && times.length != 3 )
            return false;
        if(times[0] == null || times[0].trim()=="" || times[0].trim().length()>2 || !times[0].matches("[0-9]+")
            || Integer.valueOf( times[0] )<0 || Integer.valueOf( times[0] ) > 23)
            return false;
        if(times[1] == null || times[1].trim()=="" || times[1].trim().length()>2 || !times[1].matches("[0-9]+")
                || Integer.valueOf( times[1] )<0 || Integer.valueOf( times[1] ) > 59)
            return false;
        if(times[2] != null &&( times[2].trim()=="" || times[2].trim().length()>2 || !times[2].matches("[0-9]+")
                || Integer.valueOf( times[2] )<0 || Integer.valueOf( times[2] ) > 59))
            return false;
        return true;
    }

    public static boolean setPreference( Context c,String prefsName, String value, String key) {
        SharedPreferences settings = c.getSharedPreferences(prefsName, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        return editor.commit();
    }

    public static String getPreference(Context c, String prefsName,String key) {
        SharedPreferences settings = c.getSharedPreferences(prefsName, 0);
        if(settings == null)
            return null;
        else {
            String value = settings.getString(key, "");
            return value;
        }
    }

    public static void setLocalization(Context ctx, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        ctx.getResources().updateConfiguration(config,
                ctx.getResources().getDisplayMetrics());
    }

    public static void threadSleep(int miliSeconds){
        try {
            Thread.sleep(miliSeconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void alertSingleButton(Context context, String title, String MSG, String btnText) {
        final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(MSG);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, btnText,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.BLACK);
                    alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setBackgroundColor(Color.LTGRAY);
                }
            });
        }
        alertDialog.show();
    }

    public static void alertYesNo(Context context, String title, String MSG, String btnText) {
        final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(MSG);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, btnText,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.BLACK);
                    alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setBackgroundColor(Color.LTGRAY);
                }
            });
        }
        alertDialog.show();
    }
    public static boolean isLocationEnabled(Context ctx){
        String le = Context.LOCATION_SERVICE;
        LocationManager locationManager = (LocationManager) ctx.getSystemService(le);
        if(!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            return false;
        } else {
            return true;
        }
    }

    public static ArrayAdapter<String> makeSpinner(Context context,int start,int end,String prompt){
        ArrayList<String> arr= new ArrayList<>();
        if(prompt != null)
            arr.add(prompt);
        for(int i=start;i<=end;i++){
            arr.add(String.valueOf(i));
        }
        ArrayAdapter<String> adp = new ArrayAdapter<String>(context,android.R.layout.simple_spinner_item,arr);
        adp.setDropDownViewResource(android.R.layout.simple_spinner_item);
        return adp;
    }

    public static ArrayAdapter<String> makeSpinner(Context context,String[] data ,String prompt){
        ArrayList<String> arr= new ArrayList<>();
        if(prompt != null)
            arr.add(prompt);
        for (String d:data ) {
            arr.add(d);
        }
        ArrayAdapter<String> adp = new ArrayAdapter<String>(context,android.R.layout.simple_spinner_item,arr);
        adp.setDropDownViewResource(android.R.layout.simple_spinner_item);
        return adp;
    }

    public static byte[] encrypt(String str, String key) {
        byte[] output = new byte[str.length()];
        for(int i = 0; i < str.length(); i++) {
            byte o = (byte)((Integer.valueOf(str.charAt(i)) ^ Integer.valueOf(key.charAt(i % (key.length())))) );
            output[i] = o;
        }
        return output;
    }

    public static String decrypt(byte[] input, String key) {
        String output = "";
        for(int i = 0; i < input.length; i++) {
            output += (char) ((input[i] ) ^ (int) key.charAt(i % (key.length() )));
        }
        return output;
    }

    public static String byteArrayToHexString(byte[] byteArray){
        StringBuilder sb = new StringBuilder();
        for(byte b:byteArray){
            sb.append(String.format("%02X",b));
        }
        String str = sb.toString();
        return str;
    }

    public static byte[] hexStringToByteArray(String s){
        int len = s.length();
        byte[] data = new byte[len/2];
        for(int i=0;i<len;i+=2){
            data[i/2] = (byte) ((Character.digit(s.charAt(i),16)<<4)
            + Character.digit(s.charAt(i+1),16));
        }
        return data;
    }

    public static boolean isConnectedViaWifi(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifiInfo.isConnected();
    }

    public static String getConnectedWifiName(Context context) {
        WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (manager.isWifiEnabled()) {
            WifiInfo wifiInfo = manager.getConnectionInfo();
            if (wifiInfo != null) {
                NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());
                if (state == NetworkInfo.DetailedState.CONNECTED || state == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
                    return wifiInfo.getSSID();
                }
            }
        }
        return null;
    }

    /**
     * Get IP address from first non-localhost interface
     * @param ipv4  true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }


    public static String getPhoneNumberOfDevice(Context context){
        TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        String myPhoneNumber =  tm.getLine1Number();
        return myPhoneNumber;
    }

    public static String getDeviceUniqueId(Context context){
        return Secure.getString(context.getContentResolver(),Secure.ANDROID_ID);
    }

    public static float getAppVersion(Context context){
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            float version =Float.parseFloat( pInfo.versionName);
            return version;
        }catch (PackageManager.NameNotFoundException e){
            return 0;
        }
    }

    public static int getDisplayWidth(Context context){
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        return metrics.widthPixels;
    }

    public static int getDisplayHeight(Context context){
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        return metrics.heightPixels;
    }

    public static boolean hasCamera(Context context) {
        // check if the device has camera
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }

    public static int findFrontFacingCamera() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                //cameraFront = true;
                break;
            }
        }
        return cameraId;
    }

    public static int findBackFacingCamera() {
        int cameraId = -1;
        // Search for the back facing camera
        // get the number of cameras
        int numberOfCameras = Camera.getNumberOfCameras();
        // for every camera check
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    public static int getMinAudioBufferSize(int sampleRate){
        return AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
    }

    public static boolean ping(String ip){
        Runtime runtime = Runtime.getRuntime();
        try
        {
            Process  mIpAddrProcess = runtime.exec("/system/bin/ping -c 1 "+ip);
            int mExitValue = mIpAddrProcess.waitFor();
            System.out.println(" mExitValue "+mExitValue);
            if(mExitValue==0){
                return true;
            }else{
                return false;
            }
        }
        catch (InterruptedException e) {
            Log.e(TAG,"ping:"+e.getMessage());
            e.printStackTrace();
        }
        catch (IOException e)        {
            Log.e(TAG,"ping:"+e.getMessage());
            e.printStackTrace();
        }
        return false;

    }

    public static void Msg(Activity myActivity, final String msg){
        myActivity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(myActivity, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
