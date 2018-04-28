package com.example.ali.udpvideochat;

import android.content.Context;

import java.io.Serializable;

/**
 * Created by ali on 5/23/2017.
 */
public class Contact implements Serializable {
    private int     ID;
    private String  deviceID;
    private int     displayWidth;
    private int     displayHeight;
    private String  surname;
    private String  IP;
    private String  hostName;
    private byte[]  avatar;
    private String  OSType;
    private float   versionNumber;
    private String  phoneNumber;
    private String  serviceName;
    private Boolean isOnline;

    public Contact(){

    }

    public Contact(int ID, String deviceID, int displayWidth, int displayHeight, String surname, String IP, String hostName, byte[] avatar, String OSType, float versionNumber, String phoneNumber, String serviceName, Boolean isOnline) {
        this.ID = ID;
        this.deviceID = deviceID;
        this.displayWidth = displayWidth;
        this.displayHeight = displayHeight;
        this.surname = surname;
        this.IP = IP;
        this.hostName = hostName;
        this.avatar = avatar;
        this.OSType = OSType;
        this.versionNumber = versionNumber;
        this.phoneNumber = phoneNumber;
        this.serviceName = serviceName;
        this.isOnline = isOnline;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    public int getDisplayWidth() {
        return displayWidth;
    }

    public void setDisplayWidth(int displayWidth) {
        this.displayWidth = displayWidth;
    }

    public int getDisplayHeight() {
        return displayHeight;
    }

    public void setDisplayHeight(int displayHeight) {
        this.displayHeight = displayHeight;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public byte[] getAvatar() {
        return avatar;
    }

    public void setAvatar(byte[] avatar) {
        this.avatar = avatar;
    }

    public String getOSType() {
        return OSType;
    }

    public void setOSType(String OSType) {
        this.OSType = OSType;
    }

    public float getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(float versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Boolean getOnline() {
        return isOnline;
    }

    public void setOnline(Boolean online) {
        isOnline = online;
    }
}
