package com.example.ali.udpvideochat.view;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.example.ali.udpvideochat.CommunicationService;
import com.example.ali.udpvideochat.Constants;
import com.example.ali.udpvideochat.Contact;
import com.example.ali.udpvideochat.ContactArrayAdapter;
import com.example.ali.udpvideochat.ContactManager;
import com.example.ali.udpvideochat.ContactsStateThread;
import com.example.ali.udpvideochat.Error;
import com.example.ali.udpvideochat.NotificationListener;
import com.example.ali.udpvideochat.R;
import com.example.ali.udpvideochat.Tools;
import com.example.ali.udpvideochat.call.CallSetting;


public class MainActivity extends FragmentActivity implements MainFragment.OnMainFragmentInteractionListener
                                                            ,VideoCallFragment.OnVideoCallFragmentInteractionListener
                                                            ,VoiceCallFragment.OnVoiceCallFragmentInteractionListener
                                                            ,IncomingCallFragment.OnIncomingCallFragmentInteractionListener
                                                            ,OutgoingCallFragment.OnOutgoingCallFragmentInteractionListener
                                                            ,ContactArrayAdapter.ContactArrayAdapterCallback
                                                            ,CommunicationService.Callbacks {




    public static final String TAG = "MainActivity";

    private static Context mContext;
    public  static Activity mMyActivity;
    public  static CommunicationService mCommunicationService;
    private boolean mIsBound;
    private Contact mSelectedContact = null;

    private MainFragment mMainFragment;
    private VideoCallFragment mVideoCallFragment;
    private VoiceCallFragment mVoiceCallFragment;
    private ContactsStateThread mContactsStateThread;
    private String  mCallType;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mCommunicationService = ((CommunicationService.MyBinder) service).getService();
            mMyActivity.runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        mCommunicationService.registerClientApp(mMyActivity);
                        if (mCommunicationService != null ) {
//                            if (mCommunicationService.IsRegistered())
//                                mTxtRegistrationStatus.setText("Registered");
//                            else
//                                mTxtRegistrationStatus.setText("Not Registered");
                        }else{
//                            mTxtRegistrationStatus.setText("");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "onServiceConnected:" + e.getMessage());
                        e.printStackTrace();
                        Tools.Msg(mMyActivity,"Service connected with error.");
                    }
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            try {
                mCommunicationService.unregisterClientApp();
                mCommunicationService = null;
            } catch (Exception e) {
                Log.e(TAG, "onServiceDisconnected:" + e.getMessage());
                e.printStackTrace();
                Tools.Msg(mMyActivity,"Service disconnection with error.");
            }
        }
    };

    private final BroadcastReceiver mCloseAppReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    private void doBindService() {
        try {
            // Establish a connection with the service.  We use an explicit
            // class name because we want a specific service implementation that
            // we know will be running in our own process (and thus won't be
            // supporting component replacement by other applications).
            getApplicationContext().bindService(new Intent(getApplicationContext(), CommunicationService.class), mConnection, Context.BIND_AUTO_CREATE);
            mIsBound = true;
        } catch (Exception e) {
            Log.e(TAG, "doBindService:" + e.getMessage());
            e.printStackTrace();
            Tools.Msg(mMyActivity,"binding service failed.");
        }
    }

    private void doUnbindService() {
        try {
            if (mIsBound) {
                this.mCommunicationService.unregisterClientApp();
                // Detach our existing connection.
                getApplicationContext().unbindService(mConnection);
                mIsBound = false;
            }
        } catch (Exception e) {
            Log.e(TAG, "doUnbindService:" + e.getMessage());
            e.printStackTrace();
            Tools.Msg(mMyActivity,"Unbinding service failed.");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try{
            super.onCreate(savedInstanceState);
            Log.i(TAG, "Creating MainActivity");
            mContext = getBaseContext();
            mMyActivity = this;
            registerReceiver(mCloseAppReceiver, new IntentFilter("CLOSE_APP"));
            setContentView(R.layout.activity_main);

            //Tools.setPreference(mContext,Constants.PREFS_NAME,"Ali-Sony",Constants.PREF_SURNAME);
            // TODO: 7/9/2017 this line is unused and should be deleted
            //DatabaseHandler db = new DatabaseHandler(this);
            //db.getWritableDatabase();

            //String isFirstRun = Tools.getPreference(mContext,Constants.PREFS_NAME, Constants.PREF_IS_FIRST_RUN);
            //check if communicationService is running or not
            Boolean isCommunicationServiceRunning = isMyServiceRunning(CommunicationService.class);

            //if service is not running
            if(!isCommunicationServiceRunning) {
                //start new thread to prevent activity load delay
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //try to start communicationService via NotificationListener class
                        NotificationListener.handleEvent(mContext,"Activate");
                        //wait to progress in Nsd registration and discovery
                        Tools.threadSleep(5000);
                        //bind to communicationService
                        //attention:there is still a chance that communicationService has not been started.
                        doBindService();
                        //Tools.setPreference(mContext,Constants.PREFS_NAME, "false", Constants.PREF_IS_FIRST_RUN);
                    }
                }).start();
            }else{
                //communicationService is running. bind to it
                doBindService();
            }

            //mTxtLocalIp.setText(Tools.getIPAddress(true));

            //open IncomingCallFragment instead of MainFragment when app is closed and call receives
            mMainFragment = MainFragment.newInstance();
            Intent intent = getIntent();
            if(intent.hasExtra("caller")){
                //app has been opened by service to show incoming call
                Contact caller = (Contact)intent.getSerializableExtra("caller");
                mCallType = intent.getStringExtra("callType");
                if (savedInstanceState == null) {
                    getSupportFragmentManager().beginTransaction()
                            .add(R.id.fragment_container, IncomingCallFragment.newInstance(caller))
                            .commit();
                }
            }else{
                //app has been opened by user
                if (savedInstanceState == null) {
                    getSupportFragmentManager().beginTransaction()
                            .add(R.id.fragment_container, mMainFragment)
                            .commit();
                }
            }

            mContactsStateThread = new ContactsStateThread(mContext,mMyActivity);
            mContactsStateThread.start();

            // TODO: 8/5/2017 change this line to setting of app
            Tools.setPreference(mContext, Constants.PREFS_NAME, "true", Constants.PREF_IS_LISTEN_WIFI_CHANGE);

        }catch (Exception e){
            Log.e(TAG,"onCreate:"+e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        try {
            Log.i(TAG, "Starting.");
            //mConnection = new ChatConnection(mUpdateHandler);

            super.onStart();
        }catch (Exception e){
            Log.e(TAG,"onStart:"+e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        try {
            Log.i(TAG, "Pausing.");
            super.onPause();
        }catch (Exception e){
            Log.e(TAG,"onPause:"+e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        try {
            Log.i(TAG, "Resuming.");
            super.onResume();
            getRequiredPermissions();
        }catch (Exception e){
            Log.e(TAG,"onResume:"+e.getMessage());
            e.printStackTrace();
        }
    }

    // For KitKat and earlier releases, it is necessary to remove the
    // service registration when the application is stopped.  There's
    // no guarantee that the onDestroy() method will be called (we're
    // killable after onStop() returns) and the NSD service won't remove
    // the registration for us if we're killed.

    // In L and later, NsdService will automatically unregister us when
    // our connection goes away when we're killed, so this step is
    // optional (but recommended).

    @Override
    protected void onStop() {
        try {
            Log.i(TAG, "Being stopped.");
            //mConnection = null;
            super.onStop();
        }catch (Exception e){
            Log.e(TAG,"onStop:"+e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        try {
            Log.i(TAG, "Being destroyed.");
            mContactsStateThread.kill();
            if(mCloseAppReceiver != null) unregisterReceiver(mCloseAppReceiver);
            ////  unbind to communicationService
            doUnbindService();
        }catch (Exception e){
            Log.e(TAG,"onDestroy:"+e.getMessage());
            e.printStackTrace();
        }finally {
            super.onDestroy();
        }
    }

    private void getRequiredPermissions(){
        try {
            getPermission(Manifest.permission.CAMERA);
            getPermission(Manifest.permission.RECORD_AUDIO);
        }catch (Exception e){
            Log.e(TAG,"getRequiredPermissions:"+e.getMessage());
            e.printStackTrace();
        }
    }

    private void getPermission(String permission){
        try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ActivityCompat.checkSelfPermission(mMyActivity, permission)!= PackageManager.PERMISSION_GRANTED) {

                            // No explanation needed, we can request the permission.

                            ActivityCompat.requestPermissions(mMyActivity,
                                    new String[]{permission},
                                    0);

                            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                            // app-defined int constant. The callback method gets the
                            // result of the request.
                    }
                }
        }catch (Exception e){
            Log.e(TAG,"getPermission:"+e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onContactArrayAdapterVideoCall(int position) {
        Contact selectedContact = null;
        try {
            selectedContact =mMainFragment.mContactArrayAdapter.getItem(position);
            mCallType = "VIDEO";
            if(mCommunicationService != null)
                mCommunicationService.startVideoCall(selectedContact);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, OutgoingCallFragment.newInstance(selectedContact))
                    //.addToBackStack(null)
                    .commit();
        }catch (Exception e){
            Log.e(TAG,"onContactArrayAdapterVideoCall:"+e.getMessage());
            e.printStackTrace();
            Tools.Msg(this, Error.CALL_FAILED.toString());
            onVideoCallFragmentEndCall(selectedContact);
        }
    }

    @Override
    public void onContactArrayAdapterVoiceCall(int position) {
        Contact selectedContact = null;
        try {
            selectedContact =mMainFragment.mContactArrayAdapter.getItem(position);
            mCallType = "VOICE";
            if(mCommunicationService != null)
                mCommunicationService.startVoiceCall(selectedContact);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, OutgoingCallFragment.newInstance(selectedContact))
                    //.addToBackStack(null)
                    .commit();
        }catch (Exception e){
            Log.e(TAG,"onContactArrayAdapterVideoCall:"+e.getMessage());
            e.printStackTrace();
            Tools.Msg(this, Error.CALL_FAILED.toString());
            //onVCallFragmentEndCall(selectedContact);
        }
    }

    //called from VoiceCallFragment when user ends ongoing call
    public void onVoiceCallFragmentEndCall(Contact contact){
        try {
            //send end call message to other party via CommunicationService
            if(mCommunicationService != null)
                mCommunicationService.endCall(contact);
            //load mMainFragment
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, mMainFragment)
                    //.addToBackStack(null)
                    .commit();
        }catch (Exception e){
            Log.e(TAG,"onVoiceCallFragmentEndCall:"+e.getMessage());
            e.printStackTrace();
        }
    }

    //called from VideoCallFragment when user ends ongoing call
    public void onVideoCallFragmentEndCall(Contact contact){
        try {
            //send end call message to other party via CommunicationService
            if(mCommunicationService != null)
                mCommunicationService.endCall(contact);
            //load mMainFragment
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, mMainFragment)
                    //.addToBackStack(null)
                    .commit();
        }catch (Exception e){
            Log.e(TAG,"onVideoCallFragmentEndCall:"+e.getMessage());
            e.printStackTrace();
        }
    }

    //called from VideoCallFragment when user ends ongoing call
    public void onVideoCallFragmentSetCallSetting(Contact contact, CallSetting callSetting){
        try {
            //send call setting message to other party via CommunicationService
            if(mCommunicationService != null)
                mCommunicationService.setCallSetting(contact,callSetting);

        }catch (Exception e){
            Log.e(TAG,"onVideoCallFragmentSetCallSetting:"+e.getMessage());
            e.printStackTrace();
        }
    }

    //called from IncomingCallFragment when user accepts incoming call
    public void onIncomingCallFragmentAcceptCall(Contact callerContact){
        try {
            //send accept message to caller via CommunicationService
            if(mCommunicationService != null)
                mCommunicationService.acceptCall(callerContact);

            if(mCallType.equals("VIDEO")){
                //load VideoCallFragment to start data transmission
                mVideoCallFragment = VideoCallFragment.newInstance(ContactManager.getMyContact(),callerContact,false);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, mVideoCallFragment)
                        //.addToBackStack(null)
                        .commit();
            }else{
                //load VoiceCallFragment to start data transmission
                mVoiceCallFragment = VoiceCallFragment.newInstance(ContactManager.getMyContact(),callerContact,false);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, mVoiceCallFragment)
                        //.addToBackStack(null)
                        .commit();
            }
        }catch (Exception e){
            Log.e(TAG,"onIncomingCallFragmentAcceptCall:"+e.getMessage());
            e.printStackTrace();
            Tools.Msg(this,Error.CALL_FAILED.toString());
            onVideoCallFragmentEndCall(callerContact);
        }
    }

    //called from IncomingCallFragment when user rejects incoming call
    public void onIncomingCallFragmentRejectCall(Contact callerContact){
        try {
            //send reject message to caller via CommunicationService
            if(mCommunicationService != null)
                mCommunicationService.rejectCall(callerContact);

            mVideoCallFragment= null;
            mVoiceCallFragment= null;
            //load MainFragment
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, mMainFragment)
                    //.addToBackStack(null)
                    .commit();
        }catch (Exception e){
            Log.e(TAG,"onIncomingCallFragmentRejectCall:"+e.getMessage());
            e.printStackTrace();
            Tools.Msg(mMyActivity,Error.CALL_FAILED.toString());
            onVideoCallFragmentEndCall(callerContact);
        }
    }

    //called from OutgoingCallFragment when user cancel call
    public void onOutgoingCallFragmentCancelCall(Contact callerContact){
        try {
            //send End message to other party via CommunicationService
            if(mCommunicationService != null)
                mCommunicationService.endCall(callerContact);

            //load MainFragment
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, mMainFragment)
                    //.addToBackStack(null)
                    .commit();
            mVideoCallFragment = null;
            mVoiceCallFragment = null;
        }catch (Exception e){
            Log.e(TAG,"onOutgoingCallFragmentCancelCall:"+e.getMessage());
            e.printStackTrace();
            Tools.Msg(mMyActivity,Error.CALL_FAILED.toString());
            onVideoCallFragmentEndCall(callerContact);
        }
    }

    //called from CommunicationService whenever a contact's data changed by Nsd discovery
    public void onUpdateContact(String deviceID) {
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if(mMainFragment != null){
                            //todo: use the deviceID parameter to update only the updated contact not all of them
                            mMainFragment.updateContact();
                        }
                    }catch (Exception e){
                        Log.e(TAG,"onUpdateContact:"+e.getStackTrace());
                    }
                }
            });
        }catch (Exception e){
            Log.e(TAG,"onUpdateContact:"+e.getMessage());
            e.printStackTrace();
        }
    }

    //called from CommunicationService whenever an error happened
    public void onServiceError(Error error) {
        try {
            if(error == Error.NSD_REGISTRATION_FAILED)
                Tools.Msg(mMyActivity,error.toString());
        }catch (Exception e){
            Log.e(TAG,"onServiceError:"+e.getMessage());
            e.printStackTrace();
        }
    }

    //called from CommunicationService whenever Nsd Registration state of service changed
    public void onRegistrationChanged() {
        //// TODO: 6/26/2017 show state of registration 
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(mCommunicationService != null) {
                        //mTxtRegistrationStatus.setText(mCommunicationService.IsRegistered()?"Registered":"Not Registered");
                    }
                }
            });
        }catch (Exception e){
            Log.e(TAG,"onRegistrationChanged:"+e.getMessage());
            e.printStackTrace();
        }
    }

    public void onFragmentInteraction(Uri uri){

    }

    public void onReceiveVoiceCall(final Contact contact){
        try {
            mCallType = "VOICE";
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //load IncomingCallFragment to show incoming call contact
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, IncomingCallFragment.newInstance(contact))
                                //.addToBackStack(null)
                                .commit();

                    }catch (Exception e){
                        Log.e(TAG,"onReceiveVoiceCallThread:"+e.getMessage());
                        e.printStackTrace();
                        Tools.Msg(mMyActivity,Error.CALL_FAILED.toString());
                    }
                }
            });

        }catch (Exception e){
            Log.e(TAG,"onReceiveVoiceCall:"+e.getMessage());
            e.printStackTrace();
        }
    }
    public void onReceiveVideoCall(final Contact contact){
        try {
            mCallType = "VIDEO";
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //load IncomingCallFragment to show incoming call contact
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, IncomingCallFragment.newInstance(contact))
                                //.addToBackStack(null)
                                .commit();

                    }catch (Exception e){
                        Log.e(TAG,"onReceiveVideoCallThread:"+e.getMessage());
                        e.printStackTrace();
                        Tools.Msg(mMyActivity,Error.CALL_FAILED.toString());
                    }
                }
            });

        }catch (Exception e){
            Log.e(TAG,"onReceiveVideoCall:"+e.getMessage());
            e.printStackTrace();
        }
    }
    public void onAcceptCall(final Contact contact){
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(mCallType.equals("VOICE")){
                        mVoiceCallFragment = VoiceCallFragment.newInstance(ContactManager.getMyContact(),contact,true);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, mVoiceCallFragment)
                                //.addToBackStack(null)
                                .commit();
                    }else{
                        mVideoCallFragment = VideoCallFragment.newInstance(ContactManager.getMyContact(),contact,true);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, mVideoCallFragment)
                                //.addToBackStack(null)
                                .commit();
                    }
                }
            });
        }catch (Exception e){
            Log.e(TAG,"onAcceptCall:"+e.getMessage());
            e.printStackTrace();
        }
    }
    public void onRejectCall(Contact contact){
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //load MainFragment
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, mMainFragment)
                            //.addToBackStack(null)
                            .commit();
                    mVideoCallFragment = null;
                }
            });
        }catch (Exception e){
            Log.e(TAG,"onRejectCall:"+e.getMessage());
            e.printStackTrace();
        }
    }
    public void onEndCall(Contact contact){
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //load MainFragment
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, mMainFragment)
                            //.addToBackStack(null)
                            .commit();
                    mVideoCallFragment = null;
                }
            });
        }catch (Exception e){
            Log.e(TAG,"onEndCall:"+e.getMessage());
            e.printStackTrace();
        }
    }
    public void onBusy(Contact contact){
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //load MainFragment
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, mMainFragment)
                            //.addToBackStack(null)
                            .commit();
                    mVideoCallFragment = null;
                }
            });
        }catch (Exception e){
            Log.e(TAG,"onBusy:"+e.getMessage());
            e.printStackTrace();
        }
    }
    public void onSetCallSetting(final CallSetting callSetting){
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if(mVideoCallFragment != null)
                        mVideoCallFragment.setGuestCallSetting(callSetting);
                }
            });
        }catch (Exception e){
            Log.e(TAG,"onSetCallSetting:"+e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
