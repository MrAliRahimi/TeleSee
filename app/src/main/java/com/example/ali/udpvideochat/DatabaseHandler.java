package com.example.ali.udpvideochat;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHandler extends SQLiteOpenHelper {

    // All Static variables
    private static final String TAG = "DatabaseHandler";
    // Database Version
    private static final int DATABASE_VERSION = 8;

    // Database Name
    private static final String DATABASE_NAME = "udpVideoChatManager";

    // Contacts table name
    private static final String TABLE_CONTACT = "contact";

    // Contacts Table Columns names
    private static final String KEY_ID = "ID";
    private static final String KEY_DEVICE_ID = "deviceID";
    private static final String KEY_DISPLAY_WIDTH = "displayWidth";
    private static final String KEY_DISPLAY_HEIGHT = "displayHeight";
    private static final String KEY_SURNAME = "surname";
    private static final String KEY_IP = "IP";
    private static final String KEY_HOST_NAME = "hostName";
    private static final String KEY_AVATAR = "avatar";
    private static final String KEY_OSTYPE = "OSType";
    private static final String KEY_VER_NO = "versionNumber";
    private static final String KEY_PH_NO = "phoneNumber";
    private static final String KEY_SERVICE_NAME = "serviceName";
    private static final String KEY_IS_ONLINE = "isOnline";
    private static Context mContext;

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_CONTACT + "("
                    + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_DEVICE_ID + " TEXT," +KEY_DISPLAY_WIDTH + " INTEGER,"
                    + KEY_DISPLAY_HEIGHT + " INTEGER,"+ KEY_SURNAME + " TEXT,"
                    + KEY_IP + " TEXT," + KEY_HOST_NAME + " TEXT," + KEY_AVATAR + " BLOB,"
                    + KEY_OSTYPE + " TEXT," + KEY_VER_NO + " REAL," + KEY_PH_NO + " TEXT," + KEY_SERVICE_NAME + " TEXT," + KEY_IS_ONLINE + " INTEGER " + ")";
            db.execSQL(CREATE_CONTACTS_TABLE);

//            String INSERT_MY_CONTACT = "INSERT INTO " +TABLE_CONTACT +"("+KEY_DEVICE_ID+","+KEY_IS_ONLINE+")"
//                    + "VALUES('"+Tools.getDeviceUniqueId(mContext)+"',0)";
//            db.execSQL(INSERT_MY_CONTACT);
        }catch (Exception e){
            Log.e(TAG,"onCreate:"+e.getStackTrace());
        }
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACT);

        // Create tables again
        onCreate(db);
    }

    @Override
    protected void finalize() throws Throwable {
        this.close();
        super.finalize();
    }

    /**
     * All CRUD(Create, Read, Update, Delete) Operations
     */

    // Adding new contact
    public void addContact(Contact contact) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            //values.put(KEY_ID, contact.getID()); // Contact ID id identity
            values.put(KEY_DEVICE_ID, contact.getDeviceID()); // Contact DeviceID
            values.put(KEY_DISPLAY_WIDTH, contact.getDisplayWidth()); // Contact DisplayWidth
            values.put(KEY_DISPLAY_HEIGHT, contact.getDisplayHeight()); // Contact DisplayHeight
            values.put(KEY_SURNAME, contact.getSurname()); // Contact Surname
            values.put(KEY_IP, contact.getIP()); // Contact IP
            values.put(KEY_HOST_NAME, contact.getHostName()); // Contact hostName
            values.put(KEY_AVATAR, contact.getAvatar()); // Contact Avatar
            values.put(KEY_OSTYPE, contact.getOSType()); // Contact OSType
            values.put(KEY_VER_NO, contact.getVersionNumber()); // Contact versionNumber
            values.put(KEY_PH_NO, contact.getPhoneNumber()); // Contact PhoneNumber
            values.put(KEY_SERVICE_NAME, contact.getServiceName()); // Contact ServiceName
            values.put(KEY_IS_ONLINE, contact.getOnline()?1:0); // Contact PhoneNumber

            // Inserting Row
            db.insert(TABLE_CONTACT, null, values);
        }catch (Exception e){
            Log.e(TAG,"addContact:"+e.getMessage());
            throw e;
        }finally {
            if(db!= null && db.isOpen())
                db.close(); // Closing database connection
        }

    }

    // Updating single contact
    public int updateContact(Contact contact) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(KEY_DEVICE_ID, contact.getDeviceID());
            values.put(KEY_DISPLAY_WIDTH, contact.getDisplayWidth());
            values.put(KEY_DISPLAY_HEIGHT, contact.getDisplayHeight());
            values.put(KEY_SURNAME, contact.getSurname());
            values.put(KEY_IP, contact.getIP());
            values.put(KEY_HOST_NAME, contact.getHostName());
            values.put(KEY_AVATAR, contact.getAvatar());
            values.put(KEY_OSTYPE, contact.getOSType());
            values.put(KEY_VER_NO, contact.getVersionNumber());
            values.put(KEY_PH_NO, contact.getPhoneNumber());
            values.put(KEY_SERVICE_NAME, contact.getServiceName());
            values.put(KEY_IS_ONLINE, contact.getOnline()?1:0);

            // updating row
            return db.update(TABLE_CONTACT, values, KEY_ID + " = ?",
                    new String[] { String.valueOf(contact.getID()) });
        }catch (Exception e){
            Log.e(TAG,"updateContact:"+e.getMessage());
            throw e;
        }finally {
            if(db!= null && db.isOpen())
                db.close(); // Closing database connection
        }
    }

    // Deleting single contact
    public void deleteContact(Contact contact) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            db.delete(TABLE_CONTACT, KEY_ID + " = ?",
                    new String[] { String.valueOf(contact.getID()) });
            db.close();
        }catch (Exception e){
            Log.e(TAG,"deleteContact:"+e.getMessage());
            throw e;
        }finally {
            if(db!= null && db.isOpen())
                db.close(); // Closing database connection
        }
    }

    // Getting single contact
    public Contact getContact(int id) {
        SQLiteDatabase db = null;
        Contact contact = null;
        Cursor cursor = null;
        try {
            db = this.getReadableDatabase();
            cursor = db.query(TABLE_CONTACT, new String[] { KEY_ID,
                            KEY_DEVICE_ID, KEY_DISPLAY_WIDTH, KEY_DISPLAY_HEIGHT, KEY_SURNAME, KEY_IP, KEY_HOST_NAME, KEY_AVATAR, KEY_OSTYPE,KEY_VER_NO, KEY_PH_NO, KEY_SERVICE_NAME, KEY_IS_ONLINE }, KEY_ID + "=?",
                    new String[] { String.valueOf(id) }, null, null, null, null);
            if(cursor.moveToFirst())
                contact = mapToContact(cursor);
        }catch (Exception e){
            Log.e(TAG,"getContact:"+e.getMessage());
            throw e;
        }finally {
            if(db!= null && db.isOpen())
                db.close(); // Closing database connection
            if(cursor != null && !cursor.isClosed())
                cursor.close();
        }
        // return contact
        return contact;
    }

    // Getting  contacts
    public List<Contact> getOnLineContactsByServiceName(String serviceName) {
        List<Contact> contactList = new ArrayList<Contact>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_CONTACT + " WHERE " + KEY_IS_ONLINE + " = 1";
        if(!serviceName.isEmpty()) {
            selectQuery += " AND " + KEY_SERVICE_NAME + " = '" + serviceName + "' ";
        }

        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = this.getReadableDatabase();
            cursor = db.rawQuery(selectQuery, null);
            contactList = mapToContacts(cursor);
        }catch (Exception e){
            Log.e(TAG,"getOnLineContactsByServiceName:"+e.getMessage());
            throw e;
        }finally {
            if(db!= null && db.isOpen())
                db.close(); // Closing database connection
            if(cursor != null && !cursor.isClosed())
                cursor.close();
        }

        // return contact list
        return contactList;
    }

    // Getting  contacts
    public List<Contact> getOnLineContactsByIp(String ip) {
        List<Contact> contactList = new ArrayList<Contact>();
        if(!ip.isEmpty()) {
            // Select All Query
            String selectQuery = "SELECT  * FROM " + TABLE_CONTACT + " WHERE " + KEY_IS_ONLINE + " = 1";
            selectQuery += " WHERE " + KEY_IP + " = '" + ip + "' ";
            SQLiteDatabase db = null;
            Cursor cursor = null;
            try {
                db = this.getReadableDatabase();
                cursor = db.rawQuery(selectQuery, null);
                contactList = mapToContacts(cursor);
            }catch (Exception e){
                Log.e(TAG,"getOnLineContactsByServiceName:"+e.getMessage());
                throw e;
            }finally {
                if(db!= null && db.isOpen())
                    db.close(); // Closing database connection
                if(cursor != null && !cursor.isClosed())
                    cursor.close();
            }
        }
        // return contact list
        return contactList;
    }
    // Getting single contact
    public Contact getContactByDeviceID(String deviceID) {
        SQLiteDatabase db = null;
        Contact contact = null;
        Cursor cursor = null;
        try {
            db = this.getWritableDatabase();

            cursor = db.query(TABLE_CONTACT, new String[]{KEY_ID,
                            KEY_DEVICE_ID, KEY_DISPLAY_WIDTH, KEY_DISPLAY_HEIGHT, KEY_SURNAME, KEY_IP, KEY_HOST_NAME, KEY_AVATAR, KEY_OSTYPE, KEY_VER_NO, KEY_PH_NO, KEY_SERVICE_NAME, KEY_IS_ONLINE}, KEY_DEVICE_ID + "=?",
                    new String[]{String.valueOf(deviceID)}, null, null, null, null);
                if (cursor.moveToFirst()) {
                    contact = mapToContact(cursor);
                }
        }catch (Exception e){
            Log.e(TAG,"getContactByDeviceID:"+e.getMessage());
            throw e;
        }finally {
            if(db!= null && db.isOpen())
                db.close(); // Closing database connection
            if(cursor != null && !cursor.isClosed())
                cursor.close();
        }
        // return contact
        return contact;
    }

    // Getting possible list of contact by ip
    public List<Contact> getContactByIp(String ip) {
        SQLiteDatabase db = null;
        List<Contact> contactList = new ArrayList<Contact>();
        Cursor cursor = null;
        try {
            db = this.getWritableDatabase();

             cursor = db.query(TABLE_CONTACT, new String[] { KEY_ID,
                            KEY_DEVICE_ID,KEY_DISPLAY_WIDTH, KEY_DISPLAY_HEIGHT, KEY_SURNAME, KEY_IP, KEY_HOST_NAME, KEY_AVATAR, KEY_OSTYPE,KEY_VER_NO, KEY_PH_NO, KEY_SERVICE_NAME, KEY_IS_ONLINE }, KEY_IP + "=?",
                    new String[] { ip }, null, null, null, null);

            contactList = mapToContacts(cursor);
        }catch (Exception e){
            Log.e(TAG,"getContactByIp:"+e.getMessage());
            throw e;
        }finally {
            if(db!= null && db.isOpen())
                db.close(); // Closing database connection
            if(cursor != null && !cursor.isClosed())
                cursor.close();
        }
        // return contact list
        return contactList;
    }

    // Getting All Contacts
    public List<Contact> getAllContacts() {
        SQLiteDatabase db = null;
        List<Contact> contactList = new ArrayList<Contact>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_CONTACT;
        Cursor cursor = null;
        try {
            db = this.getWritableDatabase();

            cursor = db.rawQuery(selectQuery, null);
            contactList = mapToContacts(cursor);
        }catch (Exception e){
            Log.e(TAG,"getAllContacts:"+e.getMessage());
            throw e;
        }finally {
            if(db!= null && db.isOpen())
                db.close(); // Closing database connection
            if(cursor != null && !cursor.isClosed())
                cursor.close();
        }
        // return contact list
        return contactList;
    }

    // Getting contacts Count
    public int getContactsCount() {
        String countQuery = "SELECT  * FROM " + TABLE_CONTACT;
        SQLiteDatabase db =null;
        Cursor cursor = null;
        int count = 0;
        try {
            db = this.getReadableDatabase();
            cursor = db.rawQuery(countQuery, null);
            count = cursor.getCount();
        }catch (Exception e){
            Log.e(TAG,"getContactsCount:"+e.getMessage());
            throw e;
        }finally {
            if(db!= null && db.isOpen())
                db.close(); // Closing database connection
            if(cursor != null && !cursor.isClosed())
                cursor.close();
        }

        // return count
        return count;
    }

    private Contact mapToContact(Cursor cursor) {
        int i=0;
        Contact contact = new Contact();
        contact.setID(Integer.parseInt(cursor.getString(i++)));
        contact.setDeviceID(cursor.getString(i++));
        contact.setDisplayWidth(cursor.getInt(i++));
        contact.setDisplayHeight(cursor.getInt(i++));
        contact.setSurname(cursor.getString(i++));
        contact.setIP(cursor.getString(i++));
        contact.setHostName(cursor.getString(i++));
        contact.setAvatar(cursor.getBlob(i++));
        contact.setOSType(cursor.getString(i++));
        contact.setVersionNumber(Float.parseFloat(cursor.getString(i++)));
        contact.setPhoneNumber(cursor.getString(i++));
        contact.setServiceName(cursor.getString(i++));
        contact.setOnline(cursor.getInt(i++) == 1);
        return contact;
    }

    private List<Contact> mapToContacts(Cursor cursor){
        List<Contact> contactList = new ArrayList<Contact>();

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                // Adding contact to list
                contactList.add(mapToContact(cursor));
            } while (cursor.moveToNext());
        }
        return contactList;
    }

}