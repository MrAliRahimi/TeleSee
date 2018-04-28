package com.example.ali.udpvideochat;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Handler;
import android.util.Log;

import com.example.ali.udpvideochat.call.CallController;
import com.example.ali.udpvideochat.call.CallSetting;
import com.example.ali.udpvideochat.view.MainActivity;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Created by ali on 5/27/2017.
 */
public class CommunicationService extends Service implements NsdHelper.INsdHelper, CallController.ICallbacks {
    private static IBinder mBinder ;
    private static final String TAG = "CommService";
    private static BroadcastReceiver mWifiBR; //monitor wifi connectivity to trigger Nsd registration
    private static Context mContext;
    private static NsdHelper mNsdHelper; //helper to service advertising and discovering
    private static int mNsdServerPort; //random port for use in Nsd. save it for future use
    private static String mConnectedWifi; //SSID of connected wifi. used for Nsd registration control
    private static String mMyIP; //local Ip.saved in contact. this member variable is for future use
    private static CallController mCallController; //object of CallController class to handle call session management
    private static ContactManager mContactManager; //object of ContactManager class to manage contacts
    private static Callbacks mActivity; //callback object of app's activity if it bind to service
    private static Boolean mIsRegistered; //state of NsdRegistration
    private static IntraThreadLock mNsdHelperLock; //lock object to control concurrency of Nsd registration
    private static Thread mNothingThread;


    //onStartCommand being called after boot and wifi communication state changed

    public synchronized int onStartCommand(Intent intent, int flags, int startId) {
        try {
            Log.i(TAG,"onStartCommand:"+"Received");

            //if the intent is stop, stop service friendly
            if(intent != null && intent.getAction().contains(Constants.STOP_ACTION)){
                this.stopForeground(false);
                this.endService();
                this.stopSelf();
                return Service.START_NOT_STICKY;
            }

            if(mContext == null) {
                Log.i(TAG,"onStartCommand:"+"First Execution");
                mContext = getBaseContext();
                mBinder = new MyBinder();
                mNsdHelperLock = new IntraThreadLock();
                mIsRegistered = false;

                // TODO: 4/22/2018 remove Nothing thread
                //this thread show execution af service
                mNothingThread = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            Log.i(TAG, "mNothingThread started:");
                            //long loop for listening to incoming connection
                            while (mContext != null) {
                                Log.i(TAG, "Nothing-"+Thread.currentThread().getId());
                                Tools.threadSleep(2000);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "mNothingThread:" + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                });
                mNothingThread.start();
            }


            if(mNsdHelper == null) {
                mIsRegistered = false;
                mNsdHelper =  RxDNSSDHelper.Instance();
                mNsdHelper.init(mContext, this);
            }

            if(mContactManager == null) {
                mContactManager = new ContactManager(mContext);
            }

            //Register receiver for wifi change
            if(mWifiBR == null){
                IntentFilter filter = new IntentFilter();
                filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                mWifiBR = new WifiChangedReceiver();
                registerReceiver(mWifiBR,filter);
            }

            if(mCallController == null) {
                mCallController = new CallController(callControllerHandler, this);
                mCallController.startListener();
            }

            //NSD registration is done based on connected wifi
            mNsdHelperLock.tryLock(20);
            Log.i(TAG,"mNsdHelperLock.lock");
            try {
                CountDownTimer unlockTimer = new CountDownTimer(15000, 15000) {
                    public void onTick(long millisUntilFinished) {}
                    public void onFinish() {
                        if(mNsdHelperLock != null )
                            mNsdHelperLock.unlock();
                    }
                }.start();

                Log.i(TAG,"OldWifi:"+mConnectedWifi);
                Log.i(TAG,"NewWifi:"+Tools.getConnectedWifiName(mContext));
                if(Tools.isConnectedViaWifi(mContext) &&  Tools.getConnectedWifiName(mContext)!= null){
                    if(!Tools.getConnectedWifiName(mContext).equals(mConnectedWifi)){
                        if(mConnectedWifi != null){
                            //unregister the prev registration
                            // TODO: 7/15/2017 I dont know how good or bad it works.
                            unregisterNsdService();
                            Tools.threadSleep(3000);
                        }
                        //register dns-sd
                        registerNsdService();
                    }else{
                        //still connected to the same Wifi,so do nothing
                        //if you make sure that you are still registered, it will be better
                        if(!mIsRegistered) {
                            //register dns-sd
                            registerNsdService();
                        }else{
                            mNsdHelperLock.unlock();
                            Log.i(TAG,"mNsdHelperLock.unlock");
                        }
                    }
                }else{
                    if(mConnectedWifi != null){
                        mConnectedWifi = null;
                        //unregister
                        unregisterNsdService();
                        Tools.threadSleep(3000);
                        mNsdHelperLock.unlock();
                        Log.i(TAG,"mNsdHelperLock.unlock");
                    }
                }
            }catch (Exception e){
                mNsdHelperLock.unlock();
                Log.i(TAG,"mNsdHelperLock.unlock");
                throw e;
            }
        }catch (Exception e){
            Log.e(TAG,"onStartCommand:"+e.getMessage());
            e.printStackTrace();
        }

        if(intent != null && intent.getAction().contains(Constants.STARTFORGROUND_ACTION)){
            startForeground(Constants.SERVICE_NOTIFICATION_ID,NotificationListener.getNotificationServiceEnable(mContext));
        }
        // TODO: 8/7/2017  check this with Start_Sticky
        return Service.START_NOT_STICKY;
    }

    //destroy never be called
    @Override
    public void onDestroy() {
        Log.i(TAG,"onDestroy");
        try {
            NotificationListener.setNotificationServiceDisable(getBaseContext());
            stopForeground(false);
            endService();
            //stopSelf();
            super.onDestroy();
        }catch (Exception e){
            Log.e(TAG,"onDestroy:"+e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    //Here Activity register to the service as Callbacks client
    public void registerClientApp(Activity activity){
        mActivity = (Callbacks)activity;
    }

    //remove activity reference to prevent extra work an memory usage where app is closed
    public void unregisterClientApp(){
        mActivity = null;
    }

    //unused
    public void reregisterNsdService() throws Exception {
        try {
            mNsdHelper.stopDiscovery();
            mNsdHelper.tearDown();
            registerNsdService();
            mNsdHelper.discoverServices();
        }catch (Exception e){
            Log.e(TAG,"reregisterNsdService:"+e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    //used to start/restart NSD discovery
    public void discoverNsdServices(){
        try {
            mNsdHelper.discoverServices();
        }catch (Exception e){
            Log.e(TAG,"discoverNsdServices:"+e.getMessage());
            e.printStackTrace();
            //throw e;
        }
    }

    //used for making call from app
    public void startVoiceCall(Contact contact) throws Exception {
        try {
            if(mCallController!= null){
                mCallController.startVoiceCall(contact);
            }
        }catch (Exception e){
            Log.e(TAG,"startVoiceCall:"+e.getMessage());
            throw e;
        }
    }

    //used for making call from app
    public void startVideoCall(Contact contact) throws Exception {
        try {
            if(mCallController!= null){
                mCallController.startVideoCall(contact);
            }
        }catch (Exception e){
            Log.e(TAG,"startVideoCall:"+e.getMessage());
            throw e;
        }
    }

    //used for ending call from app
    public void endCall(Contact contact) throws Exception {
        try {
            if(mCallController!= null){
                mCallController.endCall(contact);
            }
        }catch (Exception e){
            Log.e(TAG,"endCall:"+e.getMessage());
            e.printStackTrace();
        }
    }

    //used for accept incoming call from app
    public void acceptCall(Contact contact) throws Exception {
        try {
            if(mCallController!= null){
                mCallController.acceptCall(contact);
            }
        }catch (Exception e){
            Log.e(TAG,"acceptCall:"+e.getMessage());
            throw e;
        }
    }

    //used for reject incoming call from app
    public void rejectCall(Contact contact) throws Exception {
        try {
            if(mCallController!= null){
                mCallController.rejectCall(contact);
            }
        }catch (Exception e){
            Log.e(TAG,"rejectCall:"+e.getMessage());
            e.printStackTrace();
        }
    }

    //used for set call setting from app
    public void setCallSetting(Contact contact, CallSetting callSetting) throws Exception {
        try {
            if(mCallController!= null){
                mCallController.setCallSetting(contact, callSetting);
            }
        }catch (Exception e){
            Log.e(TAG,"setCallSetting:"+e.getMessage());
            throw e;
        }
    }

    public Boolean IsRegistered() {
        return mIsRegistered;
    }

    // manages messages for current Thread (service)
    // received from our callController Thread
    public Handler callControllerHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            try {
                Log.i(TAG, "handleMessage:"+msg.what);
                if (msg.what == 0) {
                    //Contact object received. save it and notify UI
                    mContactManager.saveContact((Contact)msg.obj);
                    if(mActivity != null)
                        mActivity.onUpdateContact(((Contact)msg.obj).getDeviceID());
                }else if (msg.what == 1) {
                    //CallSetting object received. notify UI
                    if(mActivity != null)
                        mActivity.onSetCallSetting(((CallSetting)msg.obj));
                }
            }catch (Exception e){
                Log.e(TAG,"handleMessage:"+e.getMessage());
                e.printStackTrace();
            }
         };
    };

    //the service never stop. so this method is not used until now
    private void endService(){
        try {
            unregisterNsdService();
            if(mCallController != null)
                mCallController.stopListener();
            if(mWifiBR != null)
                mContext.unregisterReceiver(mWifiBR);
            if(mNothingThread!= null)
                mNothingThread.interrupt();
            Tools.threadSleep(1000);
            mContactManager = null;
            mCallController = null;
            mWifiBR = null;
            mNothingThread = null;
            mConnectedWifi = null;
            mContext = null;
        }catch (Exception e){
            Log.e(TAG,"endService:"+e.getMessage());
            e.printStackTrace();
        }
    }

    //start app from service when it's needed. for example when incoming call received
    private void startApp(Contact callerContact,String callType){
        try {
            Log.i(TAG, "startApp");
            Intent i = new Intent();
            i.putExtra("caller",callerContact);
            i.putExtra("callType",callType);
            i.setClass(this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }catch (Exception e){
            Log.e(TAG,"startApp:"+e.getMessage());
            e.printStackTrace();
        }
    }

    //do Registration in Nsd Service
    private static void registerNsdService() throws Exception {
        try {
            Log.i(TAG, "registerNsdService:started");
            // Initialize a server socket on the next available port.
            ServerSocket serverSocket = null;
            serverSocket = new ServerSocket(0);
            // Store the chosen port.
            mNsdServerPort = serverSocket.getLocalPort();
            Log.i(TAG, "Registering service:IP:" + Tools.getIPAddress(true) + "-Port" + mNsdServerPort);
            mNsdHelper.registerService(Tools.getIPAddress(true), mNsdServerPort);

        } catch (IOException e) {
            Log.e(TAG,"registerNsdService:"+e.getMessage());
            e.printStackTrace();
            throw new Exception(e);
        }catch (Exception e){
            Log.e(TAG,"registerNsdService:"+e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    //do unregistration in Nsd Service
    //// TODO: 7/20/2017 using this method is weird
    private void unregisterNsdService(){
        try {
            if(mNsdHelper != null){
                mNsdHelper.stopDiscovery();
                Tools.threadSleep(1000);
                mNsdHelper.tearDown();
            }
            //mNsdHelper = null;
            mIsRegistered = false;
            mMyIP = null;
            //make all contact offline
            if(mContactManager != null)
                mContactManager.makeOffline("");
            if(mActivity != null)
                mActivity.onRegistrationChanged();
        }catch (Exception e){
            Log.e(TAG,"unregisterNsdService:"+e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    //event handler, called after unsuccessful Nsd registration
    synchronized public void onServiceRegistrationFailed(String serviceName){
        try {
            Log.i(TAG, "Service registration failed:"+serviceName);
            NotificationListener.setActionNotification(mContext,"Registration to network failed");
        }catch (Exception e){
            Log.e(TAG,"onServiceRegistrationFailed:"+e.getMessage());
            e.printStackTrace();
        }finally {
            mNsdHelperLock.unlock();
            Log.i(TAG, "mNsdHelperLock.unlock");
        }
    }
    //event handler, called after successful Nsd registration and successful resolve of local serviceinfo
    synchronized public void onServiceRegistered(String serviceName){
        try {
            // this event can be fired more than once per successful registration
            //use the first one as primary and others just to save serviceName
            Log.i(TAG, "Service registered:"+serviceName);
            mMyIP = Tools.getIPAddress(true);
            mConnectedWifi = Tools.getConnectedWifiName(mContext);
            mIsRegistered = true;
            if (mActivity != null) {
                mActivity.onRegistrationChanged();
            }
            NotificationListener.setActionNotification(mContext,"Device registered to network.");
            this.discoverNsdServices();
        }catch (Exception e){
            Log.e(TAG,"onServiceRegistered:"+e.getMessage());
            e.printStackTrace();
        }finally {
            mNsdHelperLock.unlock();
            Log.i(TAG, "mNsdHelperLock.unlock");
        }
    }
    //event handler, called after Nsd service found
    //purpose is info request and update contacts
    public void onServiceFound(String serviceName,String address){
        try {
            Log.i(TAG, "Service found:"+serviceName);
            //check ipv6
            if(address.indexOf(':')>0)
                return;
            if(mMyIP.equals(address) || address.equals("127.0.0.1")){
                //this is my own service, so I am already registered
                //String myHostName = serviceInfo.getHost().getHostName();
                String myServiceName = serviceName;

                //save myContact in DB
                mContactManager.updateMyContactAddress("", mMyIP, myServiceName, true);

                //notify UI
                if (mActivity != null) {
                    mActivity.onRegistrationChanged();
                    mActivity.onUpdateContact(mContactManager.getMyContact().getDeviceID());
                }

            }else{
                //this is service of others
                //send infoRequest command
                mCallController.sendInfoRequest(address);
            }
        }catch (Exception e){
            Log.e(TAG,"onServiceFound:"+e.getMessage());
            e.printStackTrace();
        }
    }
    //event handler, called after Nsd service lost
    //purpose is update contact state
    public void onServiceLost(String serviceName){
        try {
            Log.i(TAG, "Service lost:"+serviceName);
            Contact contact = mContactManager.makeOffline(serviceName);
            if(mActivity != null && contact!= null)
                mActivity.onUpdateContact(contact.getDeviceID());
        }catch (Exception e){
            Log.e(TAG,"onServiceLost:"+e.getMessage());
            e.printStackTrace();
        }
    }
    //event handler, called from Nsd Service and ... to report an error
    public void onErrorHappened(Error error){
        try {
             if(mActivity != null){
                mActivity.onServiceError(error);
            }
        }catch (Exception e){
            Log.e(TAG,"onErrorHappened:"+e.getMessage());
            e.printStackTrace();
        }
    }

    public void onStartListenerError(){
        try {
            if(mActivity != null) {
                mActivity.onServiceError(Error.CALL_CONTROLLER_START_LISTENER_ERROR);
            }else{
                // TODO: 8/20/2017 show error message in notification
            }

        }catch (Exception e){
            Log.e(TAG,"onStartListenerError:"+e.getMessage());
            e.printStackTrace();
        }
    }
    public void onNewListenError(){
        try {
            if(mActivity != null){
                mActivity.onServiceError(Error.CALL_CONTROLLER_NEW_LISTENER_ERROR);
            }else{
                // TODO: 8/20/2017 show error message in notification
            }
        }catch (Exception e){
            Log.e(TAG,"onNewListenError:"+e.getMessage());
            e.printStackTrace();
        }
    }
    public void onReceiveVoiceCall(String deviceID){
        try {
            Contact contact = mContactManager.getContactByDeviceID(deviceID);
            if(contact == null){

                //unknown contact
                //would be completed very soon
//                contact = new Contact();
//                contact.setDeviceID(deviceID);
//                mContactManager.saveContact(contact);
                return;
            }
            if(mActivity == null) {
                startApp(contact,"VOICE");
            }else{
                mActivity.onReceiveVoiceCall(contact);
            }
        }catch (Exception e){
            Log.e(TAG,"onReceiveVoiceCall:"+e.getMessage());
            e.printStackTrace();
        }

    }
    public void onReceiveVideoCall(String deviceID){
        try {
            Contact contact = mContactManager.getContactByDeviceID(deviceID);
            if(contact == null){

                //unknown contact
                //would be completed very soon
//                contact = new Contact();
//                contact.setDeviceID(deviceID);
//                mContactManager.saveContact(contact);
                return;
            }
            if(mActivity == null) {
                startApp(contact,"VIDEO");
            }else{
                mActivity.onReceiveVideoCall(contact);
            }
        }catch (Exception e){
            Log.e(TAG,"onReceiveVideoCall:"+e.getMessage());
            e.printStackTrace();
        }

    }
    public void onAcceptCall(String deviceID){
        try {
            Contact contact = mContactManager.getContactByDeviceID(deviceID);
            if(mActivity != null){
                mActivity.onAcceptCall(contact);
            }
        }catch (Exception e){
            Log.e(TAG,"onAcceptCall:"+e.getMessage());
            e.printStackTrace();
        }
    }
    public void onRejectCall(String deviceID){
        try {
            Contact contact = mContactManager.getContactByDeviceID(deviceID);
            if(mActivity != null){
                mActivity.onRejectCall(contact);
            }
        }catch (Exception e){
            Log.e(TAG,"onRejectCall:"+e.getMessage());
            e.printStackTrace();
        }
    }
    public void onEndCall(String deviceID){
        try {
            Contact contact = mContactManager.getContactByDeviceID(deviceID);
            if(mActivity != null){
                mActivity.onEndCall(contact);
            }
        }catch (Exception e){
            Log.e(TAG,"onEndCall:"+e.getMessage());
            e.printStackTrace();
        }
    }
    public void onBusy(String deviceID){
        try {
            Contact contact = mContactManager.getContactByDeviceID(deviceID);
            if(mActivity != null){
                mActivity.onBusy(contact);
            }
        }catch (Exception e){
            Log.e(TAG,"onBusy:"+e.getMessage());
            e.printStackTrace();
        }
    }

    //callbacks interface for communication with service clients!
    public interface Callbacks{
        public void onUpdateContact(String deviceID);
        public void onServiceError(Error error);
        public void onRegistrationChanged();
        public void onReceiveVoiceCall(Contact contact);
        public void onReceiveVideoCall(Contact contact);
        public void onAcceptCall(Contact contact);
        public void onRejectCall(Contact contact);
        public void onEndCall(Contact contact);
        public void onBusy(Contact contact);
        public void onSetCallSetting(CallSetting callSetting);
    }

    public class MyBinder extends Binder {
        public CommunicationService getService() {
            return CommunicationService.this;
        }
    }

}
