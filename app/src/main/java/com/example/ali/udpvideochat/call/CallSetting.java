package com.example.ali.udpvideochat.call;

import java.io.Serializable;

/**
 * Created by ali on 8/2/2017.
 */
public class CallSetting implements Serializable {

    private int sendingImageRotation;
    private int audioBufferSize;

    public CallSetting() {
    }

    public CallSetting(int sendingImageRotation, int audioBufferSize) {
        this.sendingImageRotation = sendingImageRotation;
        this.audioBufferSize = audioBufferSize;
    }

    public int getSendingImageRotation() {
        return sendingImageRotation;
    }

    public void setSendingImageRotation(int sendingImageRotation) {
        this.sendingImageRotation = sendingImageRotation;
    }

    public int getAudioBufferSize() {
        return audioBufferSize;
    }

    public void setAudioBufferSize(int audioBufferSize) {
        this.audioBufferSize = audioBufferSize;
    }
}
