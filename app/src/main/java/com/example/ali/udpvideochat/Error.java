package com.example.ali.udpvideochat;

/**
 * Created by ali on 7/14/2017.
 */
public enum Error {
    DATABASE(0, "A database error has occured."),
    DUPLICATE_USER(1, "This user already exists."),
    NSD_REGISTRATION_FAILED(10,"Service registration failed"),
    NSD_REGISTRATION_FAILED_FAILURE_MAX_LIMIT(11,"Service registration failed. Too many requests"),
    NSD_UNREGISTRATION_FAILED(14,"Service unregistration failed"),
    NSD_START_DISCOVERY_FAILED(16,"Start NSD discovery failed"),
    NSD_STOP_DISCOVERY_FAILED(17,"Stop NSD discovery failed"),
    NSD_RESOLVE_SERVICE_INFO_FAILED(19,"Resolve NSD service info failed"),
    CALL_CONTROLLER_START_LISTENER_ERROR(20,"Start listener error"),
    CALL_CONTROLLER_NEW_LISTENER_ERROR(21,"New listener error"),
    CALL_FAILED(40,"Call failed");

    private final int code;
    private final String description;

    private Error(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String toString() {
        return code + ": " + description;
    }
}