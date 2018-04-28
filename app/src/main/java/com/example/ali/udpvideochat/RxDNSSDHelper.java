package com.example.ali.udpvideochat;

import android.content.Context;
import android.util.Log;

import com.github.druk.dnssd.BrowseListener;
import com.github.druk.dnssd.DNSSD;
import com.github.druk.dnssd.DNSSDException;
import com.github.druk.dnssd.DNSSDRegistration;
import com.github.druk.dnssd.DNSSDService;
import com.github.druk.dnssd.QueryListener;
import com.github.druk.dnssd.RegisterListener;
import com.github.druk.dnssd.ResolveListener;
import com.github.druk.dnssd.DNSSDBindable;
import com.github.druk.rxdnssd.BonjourService;

import java.net.InetAddress;
import java.util.Map;

/**
 * Created by ali on 9/12/2017.
 */
public class RxDNSSDHelper extends NsdHelper{

    private DNSSDBindable mDnsSD;
    private DNSSDService mBrowseService;
    private DNSSDService mRegisterService;
    private RegisterListener mRegisterListener;

    //prevent any call outside of class
    private RxDNSSDHelper() {}

    public static RxDNSSDHelper Instance(){
        //if no instance is initialized yet then create new instance
        //else return stored instance
        if (instance == null)
        {
            instance = new RxDNSSDHelper();
        }
        return (RxDNSSDHelper)instance;
    }

    public void init(Context context, INsdHelper callback){
        super.init(context,callback);
        if(mDnsSD == null) {
            mDnsSD = new DNSSDBindable(this.mContext);
        }
    }

    private BrowseListener newBrowseListener() {
        return new BrowseListener() {

            @Override
            public void serviceFound(DNSSDService browser, int flags, int ifIndex, final String serviceName, String regType, String domain) {
                try {
                    Log.i(TAG, "Service discovery success" + serviceName);
                    if (!regType.equals(Constants.NSD_SERVICE_TYPE)) {
                        Log.i(TAG, "Unknown Service Type: " + regType);
                    } else if (serviceName.startsWith(Constants.NSD_SERVICE_NAME)) {
                        startResolve(flags, ifIndex, serviceName, regType, domain);
                    }
                }catch (Exception e){
                    Log.e(TAG,"serviceFound:"+e.getMessage());
                    e.printStackTrace();
                }
            }

            @Override
            public void serviceLost(DNSSDService browser, int flags, int ifIndex, final String serviceName, String regType, String domain) {
                try {
                    Log.w(TAG, "service lost" + serviceName);
                    if(mCallback != null)
                        mCallback.onServiceLost(serviceName);
                }catch (Exception e){
                    Log.e(TAG,"serviceLost:"+e.getMessage());
                    e.printStackTrace();
                }
            }

            @Override
            public void operationFailed(DNSSDService service, int errorCode) {
                try {
                    Log.w(TAG, "Discovery failed: Error code:" + errorCode);
                    if(mCallback != null)
                        mCallback.onErrorHappened(Error.NSD_START_DISCOVERY_FAILED);
                }catch (Exception e){
                    Log.e(TAG,"operationFailed:"+e.getMessage());
                    e.printStackTrace();
                }
            }
        };
    }

    private void initializeRegistrationListener() {
        mRegisterListener = new RegisterListener() {

            @Override
            public void serviceRegistered(DNSSDRegistration registration, int flags, String serviceName, String regType, String domain) {
                try {
                    mServiceName = serviceName;
                    //nsdServiceInfo.setServiceType(Constants.NSD_SERVICE_TYPE);
                    if(mCallback != null)
                        mCallback.onServiceRegistered(serviceName);
                    //mNsdManager.resolveService(nsdServiceInfo, newResolveListener());
                    Log.i(TAG, "Service registered: " + mServiceName);
                }catch (Exception e){
                    Log.e(TAG,"onServiceRegistered:"+e.getMessage());
                    e.printStackTrace();
                }

            }

            @Override
            public void operationFailed(DNSSDService service, int errorCode) {
                try {
                    Log.w(TAG, "Service registration failed: " + errorCode);
                    if(mCallback != null) {
                        //sent to release NsdHelperLock in Service
                        mCallback.onServiceRegistrationFailed(service.toString());
                        if(errorCode == 4)
                            mCallback.onErrorHappened(Error.NSD_REGISTRATION_FAILED_FAILURE_MAX_LIMIT);
                        else
                            mCallback.onErrorHappened(Error.NSD_REGISTRATION_FAILED);
                    }
                }catch (Exception e){
                    Log.e(TAG,"onRegistrationFailed:"+e.getMessage());
                    e.printStackTrace();
                }
            }
        };
    }

    public void registerService(String ip, int port){
        try {
            mServiceIP = ip;
            if(mRegisterListener == null)
                initializeRegistrationListener();
            mRegisterService = mDnsSD.register(getServiceName(), Constants.NSD_SERVICE_TYPE, port,  mRegisterListener );
        }catch (Exception e){
            Log.e(TAG,"registerService:"+e.getMessage());
            e.printStackTrace();
            // TODO: 9/15/2017 check exception
            //throw e;
        }

    }

    public void discoverServices() {
        try {
            if(mBrowseService != null){
                mBrowseService.stop();
                mBrowseService = null;
            }
            mBrowseService = mDnsSD.browse(Constants.NSD_SERVICE_TYPE,newBrowseListener());
        }catch (Exception e){
            Log.e(TAG,"discoverServices:"+e.getMessage());
            e.printStackTrace();
            // TODO: 9/15/2017 check exception
            //throw e;
        }
    }

    private void startResolve(int flags, int ifIndex, final String serviceName, final String regType, final String domain) {
        try {
            mDnsSD.resolve(flags, ifIndex, serviceName, regType, domain, new ResolveListener() {
                @Override
                public void serviceResolved(DNSSDService resolver, int flags, int ifIndex, String fullName, String hostName, int port, Map<String, String> txtRecord) {
                    try {
                        Log.i(TAG, "Resolve Succeeded. " );
                        startQueryRecords(flags, ifIndex, serviceName, regType, domain, hostName, port, txtRecord);
                    }catch (Exception e){
                        Log.e(TAG,"serviceResolved:"+e.getMessage());
                        e.printStackTrace();
                    }
                }

                @Override
                public void operationFailed(DNSSDService service, int errorCode) {
                    try {
                        Log.w(TAG, "Resolve Failed: " + serviceName + "\tError Code: " + errorCode);
                        if(mCallback != null)
                            mCallback.onErrorHappened(Error.NSD_RESOLVE_SERVICE_INFO_FAILED);
//                        switch (errorCode) {
//                            case NsdManager.FAILURE_ALREADY_ACTIVE:
////                            Log.e(TAG, "FAILURE_ALREADY_ACTIVE");
//                                // Just try again...
//                                mNsdManager.resolveService(serviceInfo, newResolveListener());
//                                break;
//                            case NsdManager.FAILURE_INTERNAL_ERROR:
////                            Log.e(TAG, "FAILURE_INTERNAL_ERROR");
//                                break;
//                            case NsdManager.FAILURE_MAX_LIMIT:
////                            Log.e(TAG, "FAILURE_MAX_LIMIT");
//                                break;
//                        }
                    }catch (Exception e){
                        Log.e(TAG,"onResolveFailed:"+e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
        } catch (DNSSDException e) {
            e.printStackTrace();
        }
    }

    private void startQueryRecords(int flags, int ifIndex, final String serviceName, final String regType, final String domain, final String hostName, final int port, final Map<String, String> txtRecord) {
        try {
            QueryListener listener = new QueryListener() {
                @Override
                public void queryAnswered(DNSSDService query, final int flags, final int ifIndex, final String fullName, int rrtype, int rrclass, final InetAddress address, int ttl) {
                    Log.i("TAG", "Query address " + fullName);
                    if (mCallback != null)
                        mCallback.onServiceFound(serviceName,address.getHostAddress());
                }

                @Override
                public void operationFailed(DNSSDService service, int errorCode) {
                    try {
                        Log.w(TAG, "Resolve Failed: " + serviceName + "\tError Code: " + errorCode);
                        if(mCallback != null)
                            mCallback.onErrorHappened(Error.NSD_RESOLVE_SERVICE_INFO_FAILED);
                    }catch (Exception e){
                        Log.e(TAG,"onResolveFailed:"+e.getMessage());
                        e.printStackTrace();
                    }
                }
            };
            mDnsSD.queryRecord(0, ifIndex, hostName, 1, 1, listener);
            mDnsSD.queryRecord(0, ifIndex, hostName, 28, 1, listener);
        } catch (DNSSDException e) {
            e.printStackTrace();
        }
    }

    public void stopDiscovery(){
        try {
            if(mBrowseService != null){
                mBrowseService.stop();
                mBrowseService = null;
            }
        }catch (Exception e){
            Log.e(TAG,"stopDiscovery:"+e.getMessage());
            e.printStackTrace();
            // TODO: 9/15/2017 check exception
            //throw e;
        }
    }

    public void tearDown() {
        try {
            if (mRegisterService != null) {
                mRegisterService.stop();
                mRegisterService = null;
            }
        }catch (Exception e){
            Log.e(TAG,"tearDown:"+e.getMessage());
            e.printStackTrace();
        } finally {
            //// TODO: 9/15/2017 check this 
            //mRegisterService = null;
        }
    }

}
