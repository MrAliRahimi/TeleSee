package com.example.ali.udpvideochat.call;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.example.ali.udpvideochat.Constants;
import com.example.ali.udpvideochat.Contact;
import com.example.ali.udpvideochat.ContactManager;
import com.example.ali.udpvideochat.CustomException;
import com.google.gson.Gson;


/**
 * Created by ali on 6/4/2017.
 */
public class CallController {
    public enum enumCallingState {
        CallingIn("CIN"), CallingOut("COT"), Talking("TAK"), Idle("IDL"),;
        private String value;

        enumCallingState(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        public static enumCallingState enumCallingStateOf(String val){
            switch (val){
                case "CIN":
                    return enumCallingState.CallingIn;
                case "COT":
                    return enumCallingState.CallingOut;
                case "TAK":
                    return enumCallingState.Talking;
                case "IDL":
                    return enumCallingState.Idle;
            }
            return null;
        }

    }

    public enum enumCommand {
        VoiceCall("VOC"), VideoCall("VIC"), AcceptCall("ACC"), RejectCall("REJ"), EndCall("END"), Busy("BSY"), Invalid("NVL"),
        InfoRequest("IRQ"),InfoResponse("IRS"),SetCallSetting("SCS");
        private String value;

        enumCommand(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        public static enumCommand enumCommandOf(String val){
            switch (val){
                case "VOC":
                    return enumCommand.VoiceCall;
                case "VIC":
                    return enumCommand.VideoCall;
                case "ACC":
                    return enumCommand.AcceptCall;
                case "REJ":
                    return enumCommand.RejectCall;
                case "END":
                    return enumCommand.EndCall;
                case "BSY":
                    return enumCommand.Busy;
                case "IRQ":
                    return enumCommand.InfoRequest;
                case "IRS":
                    return enumCommand.InfoResponse;
                case "SCS":
                    return enumCommand.SetCallSetting;
                default:
                    return enumCommand.Invalid;
            }
        }

    }

    private static final String TAG = "CallController";
    private static final int BUF_SIZE = 1024;
    private static final int CONTROLLER_PORT = 10001;
    private boolean mListen;
    private Thread mListenThread;
    private enumCallingState mCallingState ;

    private ServerSocket mServerSocket;
    private Handler mCommServiceHandler;
    private static ICallbacks mCommServiceCallback;
    private String  mCallingPartyDeviceID = null;


    public static Object sendCommandLock = new Object();


    public CallController(Handler handler, ICallbacks callback){
        try {
            mServerSocket = null;
            mListen = false;
            mCommServiceHandler = handler;
            mCallingState = enumCallingState.Idle;
            mCommServiceCallback = callback;
        }catch (Exception e){
            Log.e(TAG,"CallController:"+e.getMessage());
            e.printStackTrace();
        }
    }

    public void startListener(){
        if(mListen == false && mListenThread == null) {
            //Create new thread for listening to commands
            mListenThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        Log.i(TAG, "startListener:" + "Listener started!");
                        try {
                            //Create server side client socket reference
                            mServerSocket = new ServerSocket(Constants.CONTROLLER_PORT);
                            mListen = true;
                            Log.i(TAG, "startListener:" + "server socket opened");
                        } catch (IOException e) {
                            Log.e(TAG, "startListener:IOException" + e.getMessage());
                            e.printStackTrace();
                            if(mCommServiceCallback != null)
                                mCommServiceCallback.onStartListenerError();
                            return;
                        }

                        //long loop for listening to incoming connection
                        while (mListen && mServerSocket.isBound() && !Thread.currentThread().isInterrupted()) {
                            try {
                                //stop until next incoming thread
                                Socket socket = mServerSocket.accept();
                                //Create new thread for every incoming connection to handle
                                CommunicationThread commThread = new CommunicationThread(socket);
                                new Thread(commThread).start();
                            } catch (IOException e) {
                                Log.e(TAG, "startListener:IOException" + e.getMessage());
                                e.printStackTrace();
                                if(mCommServiceCallback != null)
                                    mCommServiceCallback.onNewListenError();
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "startListener:" + e.getMessage());
                        e.printStackTrace();
                    }finally {
                        try {
                            if(mServerSocket != null && !mServerSocket.isClosed()) {
                                mServerSocket.close();
                            }
                        }catch (Exception e){
                            Log.e(TAG, "startListener:finally" + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            });
            mListenThread.start();
        }
    }

    public void stopListener() {
        // Ends the listener thread
        mListen = false;
        try {
            if(mServerSocket != null && !mServerSocket.isClosed()) {
                mServerSocket.close();
            }
        }catch (IOException e){
            Log.e(TAG, "stopListener:" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processIncomingCommand(String receivedData, String callerIP){
        try {
            String deviceID = receivedData.substring(0, 16);
            String action = receivedData.substring(16, 19);
            String data = receivedData.substring(19);
            enumCommand eCommand = enumCommand.enumCommandOf(action);
            switch (eCommand){
                case VoiceCall:
                    if(mCallingState == enumCallingState.Idle){
                        mCallingState = enumCallingState.CallingIn;
                        mCallingPartyDeviceID = deviceID;
                        sendInfoRequest(callerIP);
                        if(mCommServiceCallback != null)
                            mCommServiceCallback.onReceiveVoiceCall(deviceID);
                    }else{
                        //caller is myself for test. allow connection
                        if(deviceID.equals(ContactManager.getMyContact().getDeviceID())
                                && mCallingState == enumCallingState.CallingOut){
                            mCallingState = enumCallingState.Talking;
                            mCallingPartyDeviceID = deviceID;
                            if(mCommServiceCallback != null)
                                mCommServiceCallback.onAcceptCall(deviceID);
                        }else
                        sendCommand(enumCommand.Busy,callerIP);
                    }
                    break;
                case VideoCall:
                    if(mCallingState == enumCallingState.Idle){
                        mCallingState = enumCallingState.CallingIn;
                        mCallingPartyDeviceID = deviceID;
                        sendInfoRequest(callerIP);
                        if(mCommServiceCallback != null)
                            mCommServiceCallback.onReceiveVideoCall(deviceID);
                    }else{
                        //caller is myself for test. allow connection
                        if(deviceID.equals(ContactManager.getMyContact().getDeviceID())
                                && mCallingState == enumCallingState.CallingOut){
                            mCallingState = enumCallingState.Talking;
                            mCallingPartyDeviceID = deviceID;
                            if(mCommServiceCallback != null)
                                mCommServiceCallback.onAcceptCall(deviceID);
                        }else
                            sendCommand(enumCommand.Busy,callerIP);
                    }
                    break;
                case AcceptCall:
                    if(mCallingState == enumCallingState.CallingOut){
                        mCallingState = enumCallingState.Talking;
                        mCallingPartyDeviceID = deviceID;
                        if(mCommServiceCallback != null)
                            mCommServiceCallback.onAcceptCall(deviceID);
                    }else{
                        //invalid incoming command. do nothing.
                    }
                    break;
                case RejectCall:
                    if(mCallingState == enumCallingState.CallingOut){
                        mCallingState = enumCallingState.Idle;
                        mCallingPartyDeviceID = null;
                        if(mCommServiceCallback != null)
                            mCommServiceCallback.onRejectCall(deviceID);
                    }else{
                        //invalid incoming command. do nothing.
                    }
                    break;
                case EndCall:
                    if(mCallingState == enumCallingState.Talking
                            || mCallingState == enumCallingState.CallingIn){
                        mCallingState = enumCallingState.Idle;
                        mCallingPartyDeviceID = null;
                        if(mCommServiceCallback != null)
                            mCommServiceCallback.onEndCall(deviceID);
                    }else{
                        //invalid incoming command. do nothing.
                    }
                    break;
                case Busy:
                    if(mCallingState == enumCallingState.CallingOut){
                        mCallingState = enumCallingState.Idle;
                        mCallingPartyDeviceID = null;
                        if(mCommServiceCallback != null)
                            mCommServiceCallback.onBusy(deviceID);
                    }else{
                        //invalid incoming command. do nothing.
                    }
                    break;
                case InfoRequest:
                    sendInfoResponse(callerIP,ContactManager.getMyContact());
                    break;
                case InfoResponse:
                    Contact newContact = new Gson().fromJson(data,Contact.class);
                    Message messageToParent = new Message();
                    messageToParent.what = 0;
                    messageToParent.obj = newContact;

                    // send message to CommunicationService to save contact
                    if(deviceID.equals(newContact.getDeviceID())){
                        mCommServiceHandler.sendMessage(messageToParent);
                    }else {
                        //response from invalid sender
                    }
                    break;
                case SetCallSetting:
                    if(mCallingState == enumCallingState.Talking){
                        CallSetting newCallSetting = new Gson().fromJson(data,CallSetting.class);
                        if(deviceID.equals(mCallingPartyDeviceID)){
                            messageToParent = new Message();
                            messageToParent.what = 1;
                            messageToParent.obj = newCallSetting;
                            mCommServiceHandler.sendMessage(messageToParent);
                        }
                    }
                    break;
                case Invalid:
                    //invalid incoming command. do nothing.
                    break;
            }
        }catch (Exception e) {
            Log.e(TAG, "processIncomingCommand:" + e.getMessage());
            e.printStackTrace();
        }
    }

    public void startVoiceCall(Contact contact)throws Exception{
        try {
            if(mListen == false)
                throw new CustomException("socket is not open");
            if(mCallingState != enumCallingState.Idle)
                throw new CustomException("call is not allowed");
            sendCommand(enumCommand.VoiceCall,contact.getIP());
            mCallingPartyDeviceID = contact.getDeviceID();
            mCallingState = enumCallingState.CallingOut;

        }catch (Exception e){
            throw e;
        }
    }

    public void startVideoCall(Contact contact)throws Exception{
        try {
            if(mListen == false)
                throw new CustomException("socket is not open");
            if(mCallingState != enumCallingState.Idle)
                throw new CustomException("call is not allowed");
            sendCommand(enumCommand.VideoCall,contact.getIP());
            mCallingPartyDeviceID = contact.getDeviceID();
            mCallingState = enumCallingState.CallingOut;

        }catch (Exception e){
            throw e;
        }
    }

    public void endCall(Contact contact)throws Exception{
        try {
            if(mListen == false)
                throw new CustomException("socket is not open");
            if(mCallingState != enumCallingState.Talking && mCallingState != enumCallingState.CallingOut)
                throw new CustomException("end call is not allowed");
            sendCommand(enumCommand.EndCall,contact.getIP());
            mCallingPartyDeviceID = null;
            mCallingState = enumCallingState.Idle;
            //the app close the call after sending command

        }catch (Exception e){
            throw e;
        }
    }

    public void rejectCall(Contact contact)throws Exception{
        try {
            if(mListen == false)
                throw new CustomException("socket is not open");
            if(mCallingState != enumCallingState.CallingIn)
                throw new CustomException("reject call is not allowed");
            sendCommand(enumCommand.RejectCall,contact.getIP());
            mCallingPartyDeviceID = null;
            mCallingState = enumCallingState.Idle;

        }catch (Exception e){
            throw e;
        }
    }

    public void acceptCall(Contact contact)throws Exception{
        try {
            if(mListen == false)
                throw new CustomException("socket is not open");
            if(mCallingState != enumCallingState.CallingIn)
                throw new CustomException("reject call is not allowed");
            sendCommand(enumCommand.AcceptCall,contact.getIP());
            mCallingPartyDeviceID = contact.getDeviceID();
            mCallingState = enumCallingState.Talking;
            //the app init call after sending accept command

        }catch (Exception e){
            throw e;
        }
    }

    public void setCallSetting(Contact contact,CallSetting callSetting)throws Exception{
        try {
            if(mListen == false)
                throw new CustomException("socket is not open");
            String data = new Gson().toJson(callSetting);
            sendCommand(enumCommand.SetCallSetting,contact.getIP(),data);
        }catch (Exception e){
            throw e;
        }
    }

    public void sendInfoRequest(String IP)throws Exception{
        try {
            if(mListen == false)
                throw new CustomException("socket is not open");
            sendCommand(enumCommand.InfoRequest,IP);
        }catch (Exception e){
            throw e;
        }
    }

    public void sendInfoResponse(String IP,Contact myContact)throws Exception{
        try {
            if(mListen == false)
                throw new CustomException("socket is not open");
            String data = new Gson().toJson(myContact);
            sendCommand(enumCommand.InfoResponse,IP,data);
        }catch (Exception e){
            throw e;
        }
    }

    private void sendCommand(final enumCommand eCommand, final String IP){
        sendCommand(eCommand,IP,null);
    }

    //every command sent by one thread. thread finishes after sending command
    private void sendCommand(final enumCommand eCommand, final String IP, final String data) {
        // Creates a thread used for sending commands
        Thread cmdThread = new Thread(new Runnable() {

            @Override
            public void run() {

                synchronized (sendCommandLock) {
                    Socket socket = new Socket();
                    BufferedReader read = null;
                    PrintWriter out = null;
                    try {
                        InetAddress serverAddr = InetAddress.getByName(IP);
                        //open a client socket and connect to server socket
                        socket.setSoTimeout(Constants.CONTROLLER_SOCKET_TIMEOUT);
                        socket.connect(new InetSocketAddress(serverAddr, Constants.CONTROLLER_PORT),Constants.CONTROLLER_SOCKET_TIMEOUT);

                        //waiting for hello string(benal)
                        // TODO: 8/8/2017 add timeout to waiting for hello.use SimpleTimeLimiter
                        read = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        String hello = read.readLine();
                        if (hello.equals("benal")) {
                            //send command through socket
                            out = new PrintWriter(new BufferedWriter(
                                    new OutputStreamWriter(socket.getOutputStream())), true);
                            out.println(ContactManager.getMyContact().getDeviceID()+eCommand.toString() + data);

                            Log.i(TAG, "Sent message( " + eCommand.toString() + " ) to " + IP);
                            out.flush();
                        }
                    } catch (UnknownHostException e) {
                        Log.e(TAG, "Failure. UnknownHostException in sendMessage: " + IP);
                    } catch(SocketException e) {
                        Log.e(TAG, "Failure. SocketException in sendMessage: " + e.getMessage());
                        e.printStackTrace();
                    } catch (IOException e) {
                        Log.e(TAG, "Failure. IOException in sendMessage: " + e.getMessage());
                        e.printStackTrace();
                    } catch (Exception e) {
                        Log.e(TAG, "Failure. Exception in sendMessage: " + e.getMessage());
                        e.printStackTrace();
                    }finally {
                        try {
                            if(out != null)
                                out.close();
                            if(read != null )
                                read.close();
                            if(socket != null && !socket.isClosed()){
                                socket.close();
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        cmdThread.start();
    }

    class CommunicationThread implements Runnable {
        private Socket clientSocket;
        private BufferedReader input;

        public CommunicationThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
            try {
                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            } catch (IOException e) {
                Log.e(TAG, "CommunicationThread:" + e.getMessage());
                e.printStackTrace();
            }
        }

        public void run() {
            if (clientSocket.isConnected() && !clientSocket.isClosed() && !Thread.currentThread().isInterrupted()) {
                try {
                    PrintWriter out = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(clientSocket.getOutputStream())), true);
                    out.println("benal");
                    out.flush();

                    String cmd = input.readLine();
                    Log.i(TAG, "CommunicationThread:" + "Received message( " + cmd + " ) from " + clientSocket.getInetAddress().getHostAddress());
                    out.close();
                    input.close();
                    String callerIp = clientSocket.getInetAddress().getHostAddress();
                    clientSocket.close();
                    processIncomingCommand(cmd, callerIp);
                } catch (IOException e) {
                    Log.e(TAG, "CommunicationThread:" + e.getMessage());
                    e.printStackTrace();
                    if(mCommServiceCallback != null)
                        mCommServiceCallback.onNewListenError();
                }
            }
        }

    }

    //callbacks interface for communication with Controller  caller
    public interface ICallbacks{
        public void onStartListenerError();
        public void onNewListenError();
        public void onReceiveVoiceCall(String deviceID);
        public void onReceiveVideoCall(String deviceID);
        public void onAcceptCall(String deviceID);
        public void onRejectCall(String deviceID);
        public void onEndCall(String deviceID);
        public void onBusy(String deviceID);
    }

}
