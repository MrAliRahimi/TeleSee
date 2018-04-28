package com.example.ali.udpvideochat;

/**
 * Created by ali on 2/16/2017.
 */
public class Constants {
    public static final String  PREFS_NAME = "com.example.ali.udpvideochat";
    public static final String  SERVICE_NAME = "vidlan";
    public static final String  PREF_IS_LISTEN_WIFI_CHANGE = "isListenWifiChange";
    public static final String  PREF_IS_FIRST_RUN = "isFirstRun";
    public static final String  PREF_SURNAME = "surname";
    public static final String  PREF_PHONE_NUMBER = "phoneNumber";
    public static final String  PREF_RUN_ON_STARTUP = "runOnStartup";
    public static final String  AVATAR_IMAGE_PATH = "avatars";
    public static final int     VIDEO_PORT = 10054;
    public static final int     AUDIO_PORT = 10055;
    public static final int     CONTROLLER_PORT = 10001;
    public static final int     CONTROLLER_SOCKET_TIMEOUT = 10000;

    public static final String  NSD_SERVICE_NAME = "NsdChat";
    public static final String  NSD_SERVICE_TYPE = "_http._tcp.";

    public static final int     VIDEO_HEADER_SIZE = 4+4;
    public static final int     MAX_UDP_VIDEO_DATA = 65535-20-8-VIDEO_HEADER_SIZE;
    public static final int     MAX_SUPPORTED_RESOLUTION_IMAGE_TRANSFER = 500000;

    public static final int     AUDIO_SAMPLE_RATE = 8000;
    public static final int     AUDIO_SAMPLE_INTERVAL = 20; // Milliseconds
    public static final int     AUDIO_SAMPLE_SIZE = 2; // Bytes


    public static final int     SERVICE_NOTIFICATION_ID = 1212;
    public static final int     EVENT_NOTIFICATION_ID = 1313;
    public static final String  STARTFORGROUND_ACTION = "com.example.ali.udpvideochat.communicationservice.action.startforground";
    public static final String  STOP_ACTION = "com.example.ali.udpvideochat.communicationservice.action.stop";
    public static final String  CALLING_TO_CONTACT = "com.example.ali.udpvideochat.extra.CALLING_TO_CONTACT";
    public static final String  CALLER_CONTACT = "com.example.ali.udpvideochat.extra.CALLER_CONTACT";
    public static final int     INCOMING_CALL_REQUEST_CODE = 1;
    public static final int     OUTGOING_CALL_REQUEST_CODE = 2;


    public static final String EXTRA_ROOMID = "org.appspot.apprtc.ROOMID";
    public static final int CAPTURE_PERMISSION_REQUEST_CODE = 1;

    // List of mandatory application permissions.
    public static final String[] MANDATORY_PERMISSIONS = {"android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.RECORD_AUDIO", "android.permission.INTERNET"};

    // Peer connection statistics callback period in ms.
    public static final int STAT_CALLBACK_PERIOD = 1000;
    // Local preview screen position before call is connected.
    public static final int LOCAL_X_CONNECTING = 0;
    public static final int LOCAL_Y_CONNECTING = 0;
    public static final int LOCAL_WIDTH_CONNECTING = 100;
    public static final int LOCAL_HEIGHT_CONNECTING = 100;
    // Local preview screen position after call is connected.
    public static final int LOCAL_X_CONNECTED = 72;
    public static final int LOCAL_Y_CONNECTED = 72;
    public static final int LOCAL_WIDTH_CONNECTED = 25;
    public static final int LOCAL_HEIGHT_CONNECTED = 25;
    // Remote video screen position
    public static final int REMOTE_X = 0;
    public static final int REMOTE_Y = 0;
    public static final int REMOTE_WIDTH = 100;
    public static final int REMOTE_HEIGHT = 100;

}
