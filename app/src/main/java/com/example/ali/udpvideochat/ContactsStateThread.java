package com.example.ali.udpvideochat;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.example.ali.udpvideochat.view.MainActivity;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by ali on 4/15/2018.
 */

public class ContactsStateThread extends Thread {
    class ContactState{
        String ip;
        boolean state;
        int numberOfTimeOut;

        public ContactState(String ip,boolean state){
            this.ip = ip;
            this.state = state;
            numberOfTimeOut=3;
        }
    }

    public static final String TAG = "ContactsStateThread";

    HashMap<String,ContactState> mListContactState = new HashMap<String,ContactState>();
    Context mContext;
    Activity mMainActivity;
    boolean mExit;

    public ContactsStateThread(Context context,Activity activity){
        mContext = context;
        mMainActivity = activity;
        mExit = false;

    }
    @Override
    public void run(){
        try {
            Date lastReadTime = Calendar.getInstance().getTime();
            while (!mExit){
                try {
                    Tools.threadSleep(Math.max(0, 15000 - Calendar.getInstance().getTime().compareTo(lastReadTime)));
                    lastReadTime = Calendar.getInstance().getTime();

                    loadList();

                    Iterator<HashMap.Entry<String,ContactState>> iter = mListContactState.entrySet().iterator();
                    while (iter.hasNext()) {
                        if(mExit) break;
                        try {
                            HashMap.Entry<String, ContactState> entry = iter.next();
                            ContactState contactState = entry.getValue();
                            if (isOnline(contactState.ip) == false) {
                                contactState.numberOfTimeOut--;
                                if (contactState.numberOfTimeOut <= 0) {
                                    makeOffline(entry.getKey());
                                    iter.remove();
                                }
                            }
                        }catch (Exception e){
                            Log.e(TAG,"run:"+e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }catch (Exception e){
                    Log.e(TAG,"run:"+e.getMessage());
                    e.printStackTrace();
                }
            }
        }catch (Exception e){
            Log.e(TAG,"run:"+e.getMessage());
            e.printStackTrace();
        }
    }

    public void kill(){
        mExit = true;
    }

    private void loadList(){
        DatabaseHandler db = new DatabaseHandler(mContext);
        for (Contact contact:db.getOnLineContactsByServiceName("")) {
            if(!mListContactState.containsKey(contact.getDeviceID())){
                mListContactState.put(contact.getDeviceID(),new ContactState(contact.getIP(),true));
            }
        }
    }

    private boolean isOnline(String ip){
        return Tools.ping(ip);
    }

    private void makeOffline(String deviceId){
        mListContactState.remove(deviceId);
        ContactManager manager = new ContactManager(mContext);
        manager.makeOfflineByDeviceId(deviceId);
        ((MainActivity)mMainActivity).onUpdateContact(deviceId);
    }
}
