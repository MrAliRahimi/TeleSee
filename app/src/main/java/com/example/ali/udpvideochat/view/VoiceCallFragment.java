package com.example.ali.udpvideochat.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.ali.udpvideochat.Contact;
import com.example.ali.udpvideochat.R;
import com.example.ali.udpvideochat.call.CallSetting;
import com.example.ali.udpvideochat.call.OnCallEvents;
import com.example.ali.udpvideochat.web_rtc.AppRTCAudioManager;
import com.example.ali.udpvideochat.web_rtc.AppRTCClient;
import com.example.ali.udpvideochat.web_rtc.DirectRTCClient;
import com.example.ali.udpvideochat.web_rtc.PeerConnectionClient;

import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;

import java.util.Set;

import static com.example.ali.udpvideochat.Constants.STAT_CALLBACK_PERIOD;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link VoiceCallFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link VoiceCallFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VoiceCallFragment extends Fragment
        implements AppRTCClient.SignalingEvents, PeerConnectionClient.PeerConnectionEvents, OnCallEvents {

    private static final String HOST_CONTACT = "mHostContact";
    private static final String GUEST_CONTACT = "mGuestContact";
    private static final String IS_CALLER = "mIsCaller";

    private Contact mHostContact;
    private Contact mGuestContact;

    private OnVoiceCallFragmentInteractionListener mListener;
    public static final String TAG = "VoiceCallFragment";
    private Context mContext;
    private TextView mtextCallerSurname;
    private LinearLayout mLayoutButtonEndCall;
    private LinearLayout mLayoutButtonSpeaker;
    private LinearLayout mLayoutButtonToggleMic;
    private PeerConnectionClient mPeerConnectionClient;
    private AppRTCClient mAppRtcClient;
    private AppRTCClient.SignalingParameters mSignalingParameters;
    private AppRTCAudioManager mAudioManager;
    private EglBase mRootEglBase;
    private boolean activityRunning;

    private AppRTCClient.RoomConnectionParameters roomConnectionParameters;
    private PeerConnectionClient.PeerConnectionParameters peerConnectionParameters;

    private boolean mIceConnected;
    private boolean isError;
    private long callStartedTimeMs;
    private boolean micEnabled = true;
    private boolean mIsCaller;
    private String mRoomId;

    public VoiceCallFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param hostContact Parameter 1.
     * @param guestContact Parameter 2.
     * @param isCaller Parameter 3.
     * @return A new instance of fragment VoiceCallFragment.
     */
    public static VoiceCallFragment newInstance(Contact hostContact, Contact guestContact,Boolean isCaller) {
        VoiceCallFragment fragment = new VoiceCallFragment();
        Bundle args = new Bundle();
        args.putSerializable(HOST_CONTACT, hostContact);
        args.putSerializable(GUEST_CONTACT, guestContact);
        args.putBoolean(IS_CALLER,isCaller);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

            mContext = this.getContext();
            this.getActivity().setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

            if (getArguments() != null) {
                mHostContact =(Contact) getArguments().getSerializable(HOST_CONTACT);
                mGuestContact =(Contact) getArguments().getSerializable(GUEST_CONTACT);
                mIsCaller = getArguments().getBoolean(IS_CALLER);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_voice_call, container, false);
        try {
            mLayoutButtonSpeaker = (LinearLayout) view.findViewById(R.id.buttonSpeaker);
            mLayoutButtonEndCall = (LinearLayout)view.findViewById(R.id.buttonEndCall);
            mLayoutButtonToggleMic = (LinearLayout)view.findViewById(R.id.buttonToggleMic);
            mtextCallerSurname = (TextView)view.findViewById(R.id.textCallerSurname);

            // If capturing format is not specified for screencapture, use screen resolution.
            peerConnectionParameters = PeerConnectionClient.PeerConnectionParameters.createAudioDefault();
            mRootEglBase = EglBase.create();

            mAppRtcClient = new DirectRTCClient(this);
            //appRtcClient = new WebSocketRTCClient(this);

            //the roomid is for future server based plan
            //but for now it is IP of other party for the caller and IP range for the called
            if(mIsCaller){
                mRoomId = mGuestContact.getIP();
            }else{
                //todo: range of IP of clients which can connect to me
                mRoomId = "0.0.0.0";
            }
            // Create connection parameters.
            roomConnectionParameters = new AppRTCClient.RoomConnectionParameters("https://appr.tc", mRoomId, false);

            setButtonsEventHandler();

            mPeerConnectionClient = PeerConnectionClient.getInstance();
            mPeerConnectionClient.createPeerConnectionFactory(getActivity(), peerConnectionParameters, this);

            startCall();


            mtextCallerSurname.setText(mGuestContact.getSurname());

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }

        return view;
    }


    @Override
    public void onDestroyView(){
        try {
            endCall();
            activityRunning = false;
            mRootEglBase.release();
        }catch (Exception e){
            Log.e(TAG,"onDestroyView:" + e.getMessage());
            e.printStackTrace();
        }
        super.onDestroyView();
    }

    @Override
    public void onPause() {
        super.onPause();
        activityRunning = false;
        // Don't stop the video when using screencapture to allow user to show other apps to the remote
        // end.
        if (mPeerConnectionClient != null) {
            mPeerConnectionClient.stopVideoSource();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        activityRunning = true;
        // Video is not paused for screencapture. See onPause.
        if (mPeerConnectionClient != null) {
            mPeerConnectionClient.startVideoSource();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnVoiceCallFragmentInteractionListener) {
            mListener = (OnVoiceCallFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void startCall(){
        try{
            callStartedTimeMs = System.currentTimeMillis();

            // Start room connection.
            //logAndToast(getString(R.string.connecting_to, roomConnectionParameters.roomUrl));
            mAppRtcClient.connectToRoom(roomConnectionParameters);

            // Create and audio manager that will take care of audio routing,
            // audio modes, audio device enumeration etc.
            mAudioManager = AppRTCAudioManager.create(mContext);
            // Store existing audio settings and change audio mode to
            // MODE_IN_COMMUNICATION for best possible VoIP performance.
            Log.i(TAG, "Starting the audio manager...");
            mAudioManager.start(this::onAudioManagerDevicesChanged);
        }catch (Exception e){
            Log.e(TAG,"startCall:" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void endCall() {
        activityRunning = false;
        try {
            if (mAppRtcClient != null) {
                mAppRtcClient.disconnectFromRoom();
                mAppRtcClient = null;
            }
        }catch (Exception e){
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
        try {
            if (mPeerConnectionClient != null) {
                mPeerConnectionClient.close();
                mPeerConnectionClient = null;
            }
        }catch (Exception e){
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }

        try {
            if (mAudioManager != null) {
                mAudioManager.stop();
                mAudioManager = null;
            }
        }catch (Exception e){
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
        try {
            if (mIceConnected && !isError) {
                //setResult(RESULT_OK);
            } else {
                //setResult(RESULT_CANCELED);
            }
        }catch (Exception e){
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
    }

    // Should be called from UI thread
    private void callConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        Log.i(TAG, "Call connected: delay=" + delta + "ms");
        if (mPeerConnectionClient == null || isError) {
            Log.w(TAG, "Call is connected in closed or error state");
            return;
        }
        // Enable statistics callback.
        mPeerConnectionClient.enableStatsEvents(true, STAT_CALLBACK_PERIOD);
    }

    // This method is called when the audio manager reports audio device change,
    // e.g. from wired headset to speakerphone.
    private void onAudioManagerDevicesChanged(
            final AppRTCAudioManager.AudioDevice device, final Set<AppRTCAudioManager.AudioDevice> availableDevices) {
        Log.i(TAG, "onAudioManagerDevicesChanged: " + availableDevices + ", "
                + "selected: " + device);
        // TODO(henrika): add callback handler.
    }

    private void setButtonsEventHandler(){
        mLayoutButtonEndCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    onCallHangUp();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "mButtonEndCall.onClick"+e.getMessage());
                }
            }
        });

        mLayoutButtonSpeaker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    //todo:send voice to speakers
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, e.getMessage());
                }
            }
        });

        mLayoutButtonToggleMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    boolean enabled = onToggleMic();
                    mLayoutButtonToggleMic.setAlpha(enabled ? 1.0f : 0.3f);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "mButtonEndCall.onClick"+e.getMessage());
                }
            }
        });

    }

    private void disconnectWithErrorMessage(final String errorMessage) {
        if (!activityRunning) {
            Log.e(TAG, "Critical error: " + errorMessage);
            endCall();
        } else {
            new AlertDialog.Builder(mContext)
                    .setTitle(getText(R.string.channel_error_title))
                    .setMessage(errorMessage)
                    .setCancelable(false)
                    .setNeutralButton(R.string.ok,
                            (dialog, id) -> {
                                dialog.cancel();
                                endCall();
                            })
                    .create()
                    .show();
        }
    }

    private void reportError(final String description) {
        getActivity().runOnUiThread(() -> {
            if (!isError) {
                isError = true;
                disconnectWithErrorMessage(description);
            }
        });
    }

    // VoiceCallFragment.OnCallEvents interface implementation.
    @Override
    public void onCallHangUp() {
        try {
            endCall();
            if(mListener != null)
                mListener.onVoiceCallFragmentEndCall(mGuestContact);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "mButtonEndCall.onClick"+e.getMessage());
        }
    }

    @Override
    public void onCameraSwitch() {
    }

    @Override
    public void onCaptureFormatChange(int width, int height, int framerate) {
    }

    @Override
    public boolean onToggleMic() {
        if (mPeerConnectionClient != null) {
            micEnabled = !micEnabled;
            mPeerConnectionClient.setAudioEnabled(micEnabled);
        }
        return micEnabled;
    }


    // -----Implementation of AppRTCClient.AppRTCSignalingEvents ---------------
    // All callbacks are invoked from websocket signaling looper thread and
    // are routed to UI thread.
    private void onConnectedToRoomInternal(final AppRTCClient.SignalingParameters params) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;

        mSignalingParameters = params;
        Log.i(TAG,"Creating peer connection, delay=" + delta + "ms");
        mPeerConnectionClient.createPeerConnection(mRootEglBase.getEglBaseContext(), null,
                null, null, mSignalingParameters);

        if (mSignalingParameters.initiator) {
            Log.i(TAG,"Creating OFFER...");
            // Create offer. Offer SDP will be sent to answering client in
            // PeerConnectionEvents.onLocalDescription event.
            mPeerConnectionClient.createOffer();
        } else {
            if (params.offerSdp != null) {
                mPeerConnectionClient.setRemoteDescription(params.offerSdp);
                Log.i(TAG,"Creating ANSWER...");
                // Create answer. Answer SDP will be sent to offering client in
                // PeerConnectionEvents.onLocalDescription event.
                mPeerConnectionClient.createAnswer();
            }
            if (params.iceCandidates != null) {
                // Add remote ICE candidates from room.
                for (IceCandidate iceCandidate : params.iceCandidates) {
                    mPeerConnectionClient.addRemoteIceCandidate(iceCandidate);
                }
            }
        }
    }

    @Override
    public void onConnectedToRoom(final AppRTCClient.SignalingParameters params) {
        getActivity().runOnUiThread(() -> onConnectedToRoomInternal(params));
    }

    @Override
    public void onRemoteDescription(final SessionDescription sdp) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        getActivity().runOnUiThread(() -> {
            if (mPeerConnectionClient == null) {
                Log.e(TAG, "Received remote SDP for non-initilized peer connection.");
                return;
            }
            Log.i(TAG,"Received remote " + sdp.type + ", delay=" + delta + "ms");
            mPeerConnectionClient.setRemoteDescription(sdp);
            if (!mSignalingParameters.initiator) {
                Log.i(TAG,"Creating ANSWER...");
                // Create answer. Answer SDP will be sent to offering client in
                // PeerConnectionEvents.onLocalDescription event.
                mPeerConnectionClient.createAnswer();
            }
        });
    }

    @Override
    public void onRemoteIceCandidate(final IceCandidate candidate) {
        getActivity().runOnUiThread(() -> {
            if (mPeerConnectionClient == null) {
                Log.e(TAG, "Received ICE candidate for a non-initialized peer connection.");
                return;
            }
            mPeerConnectionClient.addRemoteIceCandidate(candidate);
        });
    }

    @Override
    public void onRemoteIceCandidatesRemoved(final IceCandidate[] candidates) {
        getActivity().runOnUiThread(() -> {
            if (mPeerConnectionClient == null) {
                Log.e(TAG, "Received ICE candidate removals for a non-initialized peer connection.");
                return;
            }mPeerConnectionClient.removeRemoteIceCandidates(candidates);
        });
    }

    @Override
    public void onChannelClose() {
        getActivity().runOnUiThread(() -> {
            Log.i(TAG,"Remote end hung up; dropping PeerConnection");
            endCall();
        });
    }

    @Override
    public void onChannelError(final String description) {
        reportError(description);
    }

    // -----Implementation of PeerConnectionClient.PeerConnectionEvents.---------
    // Send local peer connection SDP and ICE candidates to remote party.
    // All callbacks are invoked from peer connection client looper thread and
    // are routed to UI thread.
    @Override
    public void onLocalDescription(final SessionDescription sdp) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        getActivity().runOnUiThread(() -> {
            if (mAppRtcClient != null) {
                Log.i(TAG,"Sending " + sdp.type + ", delay=" + delta + "ms");
                if (mSignalingParameters.initiator) {
                    mAppRtcClient.sendOfferSdp(sdp);
                } else {
                    mAppRtcClient.sendAnswerSdp(sdp);
                }
            }
            if (peerConnectionParameters.videoMaxBitrate > 0) {
                Log.d(TAG, "Set video maximum bitrate: " + peerConnectionParameters.videoMaxBitrate);
                mPeerConnectionClient.setVideoMaxBitrate(peerConnectionParameters.videoMaxBitrate);
            }
        });
    }

    @Override
    public void onIceCandidate(final IceCandidate candidate) {
        getActivity().runOnUiThread(() -> {
            if (mAppRtcClient != null) {
                mAppRtcClient.sendLocalIceCandidate(candidate);
            }
        });
    }

    @Override
    public void onIceCandidatesRemoved(final IceCandidate[] candidates) {
        getActivity().runOnUiThread(() -> {
            if (mAppRtcClient != null) {
                mAppRtcClient.sendLocalIceCandidateRemovals(candidates);
            }
        });
    }

    @Override
    public void onIceConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        getActivity().runOnUiThread(() -> {
            Log.i(TAG,"ICE connected, delay=" + delta + "ms");
            mIceConnected = true;
            callConnected();
        });
    }

    @Override
    public void onIceDisconnected() {
        getActivity().runOnUiThread(() -> {
            Log.i(TAG,"ICE disconnected");
            mIceConnected = false;
            endCall();
        });
    }

    @Override
    public void onPeerConnectionClosed() {
    }

    @Override
    public void onPeerConnectionStatsReady(final StatsReport[] reports) {
        getActivity().runOnUiThread(() -> {
        });
    }

    @Override
    public void onPeerConnectionError(final String description) {
        reportError(description);
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnVoiceCallFragmentInteractionListener {
        void onVoiceCallFragmentEndCall(Contact contact);
    }

}
