package com.example.ali.udpvideochat;

import android.content.Context;
import android.util.Log;

public abstract class NsdHelper {
    public interface INsdHelper{
        void onServiceRegistered(String serviceName);
        void onServiceRegistrationFailed(String serviceName);
        void onServiceFound(String serviceName,String ipAddress);
        void onServiceLost(String serviceName);
        void onErrorHappened(Error error);
    }
    protected static NsdHelper instance;

    protected Context mContext;

    protected static final String TAG = "NsdHelper";
    protected String mServiceName = "";
    protected String mServiceIP = "";
    protected INsdHelper mCallback;
    protected String getServiceName(){
        return Constants.NSD_SERVICE_NAME+Tools.getDeviceUniqueId(mContext);
    }

    public NsdHelper() {}

    public void init(Context context,INsdHelper callback) {
        mContext = context;
        mCallback = callback;
    }

    public abstract void registerService(String ip, int port);
    public abstract void tearDown() ;
    public abstract void discoverServices();
    public abstract void stopDiscovery();

}
