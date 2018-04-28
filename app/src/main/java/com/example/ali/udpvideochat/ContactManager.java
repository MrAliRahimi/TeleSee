package com.example.ali.udpvideochat;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ali on 6/16/2017.
 */
public class ContactManager {
    private static final String TAG = "ContactManager";
    private Context mContext;
    private static Contact mMyContact;

    public static Contact getMyContact() {
                return mMyContact;
    }


    public ContactManager(Context context){
        mContext = context;
        DatabaseHandler dbHelper = new DatabaseHandler(mContext);
        String deviceID = Tools.padLeft( Tools.getDeviceUniqueId(mContext),16,'0');
        mMyContact = dbHelper.getContactByDeviceID(deviceID);
        if(mMyContact == null)
            mMyContact = insertMyContact(null,null,null,false);
    }

    public Contact updateMyContactAddress(String myHostName,String myIP, String myServiceName, boolean isOnline){
            /*
            first get contact of database by device id
            if exists, update it
            if not exists, insert it
             */
        Contact contact = null;
        try {
            DatabaseHandler dbHelper = new DatabaseHandler(mContext);
            String deviceID = Tools.padLeft( Tools.getDeviceUniqueId(mContext),16,'0');

            contact= dbHelper.getContactByDeviceID(deviceID);
            if(contact != null){
                contact.setHostName(myHostName);
                contact.setIP(myIP);
                contact.setServiceName(myServiceName);
                contact.setOnline(isOnline);
                dbHelper.updateContact(contact);
                mMyContact = dbHelper.getContactByDeviceID(contact.getDeviceID());
            }else{
                mMyContact = insertMyContact(myHostName,myIP,myServiceName,isOnline);
            }
        }catch (Exception e){
            Log.e(TAG,"updateMyContactAddress:"+e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return mMyContact;
    }

    private Contact insertMyContact(String myHostName,String myIP, String myServiceName, boolean isOnline){
        Contact contact = null;
        try {
            DatabaseHandler dbHelper = new DatabaseHandler(mContext);
            String deviceID = Tools.padLeft( Tools.getDeviceUniqueId(mContext),16,'0');

                String surname = Tools.getPreference(mContext,Constants.PREFS_NAME,Constants.PREF_SURNAME);
                if(surname == null || surname == "")
                    surname= android.os.Build.MODEL;
                contact = new Contact();
                contact.setDeviceID(deviceID);
                contact.setDisplayWidth(Tools.getDisplayWidth(mContext));
                contact.setDisplayHeight(Tools.getDisplayHeight(mContext));
                contact.setSurname(surname);
                contact.setHostName(myHostName);
                contact.setIP(myIP);
                contact.setServiceName(myServiceName);
                contact.setOSType("ANDROID"+String.valueOf(android.os.Build.VERSION.SDK_INT));
                contact.setVersionNumber(Tools.getAppVersion(mContext));
                contact.setPhoneNumber(Tools.getPreference(mContext,Constants.PREFS_NAME,Constants.PREF_PHONE_NUMBER));
                contact.setOnline(isOnline);
                File folder = mContext.getExternalFilesDir(Constants.AVATAR_IMAGE_PATH);
                if (folder.exists()) {
                    try {
                        FileInputStream fis = new FileInputStream(folder+"\\"+deviceID+".jpg");
                        byte[] avatar=null;
                        fis.read(avatar);
                        contact.setAvatar(avatar);
                        fis.close();
                    } catch (FileNotFoundException e) {
                        Log.e(TAG, "File not found: " + e.getStackTrace());
                    } catch (IOException e) {
                        Log.e(TAG, "Error accessing file: " + e.getStackTrace());
                    }

                dbHelper.addContact(contact);
            }
            mMyContact = dbHelper.getContactByDeviceID(contact.getDeviceID());
        }catch (Exception e){
            Log.e(TAG,"insertMyContact:"+e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return mMyContact;
    }

    public Contact makeOffline(String serviceName){
        DatabaseHandler dbHelper = new DatabaseHandler(mContext);
        List<Contact> contacts = new ArrayList<>();
        try{
            contacts = dbHelper.getOnLineContactsByServiceName(serviceName);
            if(contacts != null && contacts.size()>0){
                for (Contact contact:contacts) {
                    contact.setOnline(false);
                    contact.setIP(null);
                    contact.setHostName(null);
                    contact.setServiceName(null);
                    dbHelper.updateContact(contact);
                }
            }
        }catch (Exception e){
            Log.e(TAG,"makeOffline:"+e.getMessage());
            e.printStackTrace();
            throw e;
        }
        if(contacts.size()>0)
            return contacts.get(0);
        else
            return null;
    }

    public void makeOfflineByDeviceId(String deviceId) {
        DatabaseHandler dbHelper = new DatabaseHandler(mContext);
        Contact contact = null;
        try {
            contact = dbHelper.getContactByDeviceID(deviceId);
            if (contact != null) {
                contact.setOnline(false);
                contact.setIP(null);
                contact.setHostName(null);
                contact.setServiceName(null);
                dbHelper.updateContact(contact);
            }
        } catch (Exception e) {
            Log.e(TAG, "makeOfflineByDeviceId:" + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    //saving contact has two mode
    //mode1:contact is incomplete(serviceName,hostName,Ip).Insert or update by Ip
    //this mode happens after Nsd service found
    //mode2:contact is complete.Insert or update by DeviceId
    //this mode happens after InfoResponse received.
    public void saveContact(Contact contact){
        try{
            //incomplete contact mode
            if(contact.getDeviceID() == null){
                DatabaseHandler dbHelper = new DatabaseHandler(mContext);
                List<Contact> contacts = dbHelper.getContactByIp(contact.getIP());
                if(contacts.size()>0) {
                    for (Contact c : contacts) {
                        if (!c.getOnline()) {
                            c.setServiceName(contact.getServiceName());
                            c.setHostName(contact.getHostName());
                            dbHelper.updateContact(c);
                            break;
                        }
                    }
                }else{
                    dbHelper.addContact(contact);
                }

            }else{//complete contact mode
            /*
            first get contact of database by device id
            if exists, update it
            if not exists, insert it
             */
                DatabaseHandler dbHelper = new DatabaseHandler(mContext);

                Contact oldContact= dbHelper.getContactByDeviceID(contact.getDeviceID());
                if(oldContact != null){
                    oldContact.setHostName(contact.getHostName());
                    oldContact.setIP(contact.getIP());
                    oldContact.setOSType(contact.getOSType());
                    oldContact.setPhoneNumber(contact.getPhoneNumber());
                    oldContact.setAvatar(contact.getAvatar());
                    oldContact.setVersionNumber(contact.getVersionNumber());
                    oldContact.setSurname(contact.getSurname());
                    oldContact.setServiceName(contact.getServiceName());
                    oldContact.setOnline(contact.getOnline());
                    dbHelper.updateContact(oldContact);
                }else{
                    dbHelper.addContact(contact);
                }
            }
        }catch (Exception e){
            Log.e(TAG,"saveContact:"+e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public Contact getContactByDeviceID(String deviceID){
        DatabaseHandler dbHelper = new DatabaseHandler(mContext);
        return dbHelper.getContactByDeviceID(deviceID);
    }

}
