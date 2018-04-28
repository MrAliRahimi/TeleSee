package com.example.ali.udpvideochat;

import java.util.concurrent.TimeoutException;

/**
 * Created by ali on 8/8/2017.
 */
public class IntraThreadLock {
    private Boolean lock;

    public IntraThreadLock(){
        lock = false;
    }

    public void lock(){
        while(!setLock(true)){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void unlock(){
        this.setLock(false);
    }

    public void tryLock(int timeOut) throws TimeoutException {
        while(timeOut >0){
            if(setLock(true))
                return;
            timeOut--;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        throw new TimeoutException("Unable to lock after "+timeOut+"seconds waiting");
    }

    public void forceLock(int timeOut){
        while(timeOut >0){
            if(setLock(true))
                return;
            timeOut--;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return;
    }
    synchronized private boolean setLock(Boolean value) {
        if (!lock) {
            lock = value;
            return true;
        } else if (!value) {
            lock = false;
            return true;
        } else {
            return false;
        }
    }
}
