package com.example.ali.udpvideochat.view;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatDelegate;
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
import com.example.ali.udpvideochat.web_rtc.AppRTCClient.SignalingParameters;
import com.example.ali.udpvideochat.web_rtc.PeerConnectionClient;
import com.example.ali.udpvideochat.web_rtc.AppRTCClient.RoomConnectionParameters;
import com.example.ali.udpvideochat.web_rtc.PeerConnectionClient.PeerConnectionParameters;
import com.example.ali.udpvideochat.web_rtc.DirectRTCClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;

import static org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FILL;
import static org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FIT;

import static com.example.ali.udpvideochat.Constants.LOCAL_HEIGHT_CONNECTED;
import static com.example.ali.udpvideochat.Constants.LOCAL_HEIGHT_CONNECTING;
import static com.example.ali.udpvideochat.Constants.LOCAL_WIDTH_CONNECTED;
import static com.example.ali.udpvideochat.Constants.LOCAL_WIDTH_CONNECTING;
import static com.example.ali.udpvideochat.Constants.LOCAL_X_CONNECTED;
import static com.example.ali.udpvideochat.Constants.LOCAL_X_CONNECTING;
import static com.example.ali.udpvideochat.Constants.LOCAL_Y_CONNECTED;
import static com.example.ali.udpvideochat.Constants.LOCAL_Y_CONNECTING;
import static com.example.ali.udpvideochat.Constants.REMOTE_HEIGHT;
import static com.example.ali.udpvideochat.Constants.REMOTE_WIDTH;
import static com.example.ali.udpvideochat.Constants.REMOTE_X;
import static com.example.ali.udpvideochat.Constants.REMOTE_Y;
import static com.example.ali.udpvideochat.Constants.STAT_CALLBACK_PERIOD;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link VideoCallFragment.OnVideoCallFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link VideoCallFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VideoCallFragment extends Fragment
    implements AppRTCClient.SignalingEvents, PeerConnectionClient.PeerConnectionEvents, OnCallEvents {

    private static final String HOST_CONTACT = "mHostContact";
    private static final String GUEST_CONTACT = "mGuestContact";
    private static final String IS_CALLER = "mIsCaller";


    private Contact mHostContact;
    private Contact mGuestContact;
    private CallSetting mHostCallSetting;
    private CallSetting mGuestCallSetting;


    private OnVideoCallFragmentInteractionListener mListener;

    public static final String TAG = "VideoCallFragment";
    //private MediaRecorder mediaRecorder;
    private Context mContext;
    private LinearLayout mCameraPreviewLayout;
    private TextView mTextOtherPartySurname;
    private ImageButton mButtonEndCall;
    private ImageButton mButtonSwitchCamera;
    private ImageButton mButtonToggleMic;
    private SurfaceViewRenderer mViewLocalVideo;
    private SurfaceViewRenderer mViewRemoteVideo;
    private PercentFrameLayout mLayoutLocalVideo;
    private PercentFrameLayout mLayoutRemoteVideo;

    private final List<VideoRenderer.Callbacks> mRemoteRenderers = new ArrayList<>();
    private PeerConnectionClient mPeerConnectionClient;
    private AppRTCClient mAppRtcClient;
    private AppRTCClient.SignalingParameters mSignalingParameters;
    private AppRTCAudioManager mAudioManager;
    private EglBase mRootEglBase;
    private boolean activityRunning;

    private RoomConnectionParameters roomConnectionParameters;
    private PeerConnectionParameters peerConnectionParameters;

    private boolean mIceConnected;
    private boolean isError;
    private long callStartedTimeMs;
    private boolean micEnabled = true;
    private boolean mIsCaller;
    private String mRoomId;


    private boolean mCameraFront = false;//indicate witch camera is using now
    private int mSendingSessionNumber = new Random().nextInt(); //random number for each session
    private int mSendingFrameWidth,mSendingFrameHeight;// specs of sending video frames

    private int mReceivingSessionNumber = -1;//session number of receiving video.filled by other party.

    private static boolean mIsTalking = false;//indicate state of call.

    private Thread mVideoReceiveThread;//thread which listen to video port and display received video data
    private Thread mVideoSenderThread;//thread which take frames captured from camera and sends them to receiver

    private HeadsetPlugReceiver mHeadsetPlugReceiver;

    public VideoCallFragment() {
        // Required empty public constructor
    }

    /**
     * @param hostContact Parameter 1.
     * @param guestContact Parameter 2.
     * @return A new instance of fragment VideoCallFragment.
     */
    public static VideoCallFragment newInstance(Contact hostContact, Contact guestContact,Boolean isCaller) {
        VideoCallFragment fragment = new VideoCallFragment();
        Bundle args = new Bundle();
        args.putSerializable(HOST_CONTACT, hostContact);
        args.putSerializable(GUEST_CONTACT, guestContact);
        args.putBoolean(IS_CALLER,isCaller);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        super.onCreate(savedInstanceState);
        try {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

            mContext = this.getContext();
            this.getActivity().setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

            if (getArguments() != null) {
                mHostContact =(Contact) getArguments().getSerializable(HOST_CONTACT);
                mGuestContact =(Contact) getArguments().getSerializable(GUEST_CONTACT);
                mIsCaller = getArguments().getBoolean(IS_CALLER);
            }
            mGuestCallSetting = new CallSetting();

            mHostCallSetting = new CallSetting();

            mSendingFrameWidth =mSendingFrameHeight =-1;
            mReceivingSessionNumber = -1;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,"onCreate:"+ e.getMessage());
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_video_call, container, false);
        try {
            mViewLocalVideo = (SurfaceViewRenderer) view.findViewById(R.id.viewLocalVideo);
            mViewRemoteVideo = (SurfaceViewRenderer) view.findViewById(R.id.viewRemoteVideo);
            mLayoutLocalVideo = (PercentFrameLayout) view.findViewById(R.id.layoutLocalVideo);
            mLayoutRemoteVideo = (PercentFrameLayout) view.findViewById(R.id.layoutRemoteVideo);
            mButtonSwitchCamera = (ImageButton) view.findViewById(R.id.buttonSwitchCamera);
            mButtonEndCall = (ImageButton)view.findViewById(R.id.buttonEndCall);
            mButtonToggleMic = (ImageButton)view.findViewById(R.id.buttonToggleMic);
            mTextOtherPartySurname = (TextView)view.findViewById(R.id.textOtherPartySurname);

            mRemoteRenderers.add(mViewRemoteVideo);
            // Create video renderers.
            mRootEglBase = EglBase.create();
            mViewLocalVideo.init(mRootEglBase.getEglBaseContext(), null);
            mViewRemoteVideo.init(mRootEglBase.getEglBaseContext(), null);

            mViewLocalVideo.setZOrderMediaOverlay(true);
            mViewLocalVideo.setEnableHardwareScaler(true);
            mViewRemoteVideo.setEnableHardwareScaler(true);
            updateVideoView();

            // If capturing format is not specified for screencapture, use screen resolution.
            peerConnectionParameters = PeerConnectionParameters.createDefault();

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
            roomConnectionParameters = new RoomConnectionParameters("https://appr.tc", mRoomId, false);

            setButtonsEventHandler();

            mPeerConnectionClient = PeerConnectionClient.getInstance();
            mPeerConnectionClient.createPeerConnectionFactory(getActivity(), peerConnectionParameters, this);

            startCall();


            mTextOtherPartySurname.setText(mGuestContact.getSurname());

            mHeadsetPlugReceiver = new HeadsetPlugReceiver();
            IntentFilter headsetPlugFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
            mContext.registerReceiver(mHeadsetPlugReceiver, headsetPlugFilter);

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,"onCreateView:"+ e.getMessage());
        }

        return view;
    }

    @Override
    public void onDestroyView(){
        try {
            mContext.unregisterReceiver(mHeadsetPlugReceiver);
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
        if (context instanceof OnVideoCallFragmentInteractionListener) {
            mListener = (OnVideoCallFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        mListener = null;
        super.onDetach();
    }

    public void setGuestCallSetting(CallSetting callSetting){

        mGuestCallSetting = callSetting;
//        mImageViewGuestVideo.setRotation(mGuestCallSetting.getSendingImageRotation());

    }

    private void updateVideoView() {
        mLayoutRemoteVideo.setPosition(REMOTE_X, REMOTE_Y, REMOTE_WIDTH, REMOTE_HEIGHT);
        mViewRemoteVideo.setScalingType(SCALE_ASPECT_FILL);
        mViewRemoteVideo.setMirror(false);

        if (mIceConnected) {
            mLayoutLocalVideo.setPosition(
                    LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED, LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED);
            mViewLocalVideo.setScalingType(SCALE_ASPECT_FIT);
        } else {
            mLayoutLocalVideo.setPosition(
                    LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING, LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING);
            mViewLocalVideo.setScalingType(SCALE_ASPECT_FILL);
        }
        mViewLocalVideo.setMirror(true);

        mViewLocalVideo.requestLayout();
        mViewLocalVideo.requestLayout();
    }


    private void loadCamera(int cameraId) throws Exception {
        /*
        try {
            if (mCamera == null) {
                throw new Exception("Camera is null");
            } else {
                // choose my display and other party display orientation based on camera type
                if(mCameraFront) {
                    mCamera.setDisplayOrientation(90);
                    mHostCallSetting.setSendingImageRotation(-90);
                }else {
                    mCamera.setDisplayOrientation(90);
                    mHostCallSetting.setSendingImageRotation(90);
                }
                mHostCallSetting.setAudioBufferSize(Tools.getMinAudioBufferSize(Constants.AUDIO_SAMPLE_RATE));
                //send other party display orientation as CallSetting object
                if(mListener != null)
                    mListener.onVideoCallFragmentSetCallSetting(mGuestContact,mHostCallSetting);

                Camera.Parameters mParams = mCamera.getParameters();

                // choose preview size based on recommendation of other party and max size of data
                List<Camera.Size> previewSizes = mParams.getSupportedPreviewSizes();
                //sort previewSizes ascending
                Collections.sort(previewSizes, new Comparator<Camera.Size>() {
                    public int compare(final Camera.Size a, final Camera.Size b) {
                        return a.width * a.height - b.width * b.height;
                    }
                });
                //sort previewSizes descending
                Collections.reverse(previewSizes);
                Camera.Size properSize = null;
                for (Camera.Size size:previewSizes) {
                    if(size.width <= mGuestContact.getDisplayWidth()
                            && size.height <= mGuestContact.getDisplayHeight()
                            && size.width* size.height <= Constants.MAX_SUPPORTED_RESOLUTION_IMAGE_TRANSFER
                            && (mSendingFrameWidth ==-1 || size.width* size.height < mSendingFrameWidth* mSendingFrameHeight)) {
                        properSize = size;
                        break;
                    }
                }
                // TODO: 8/2/2017 can properSize be null?

                mParams.setPreviewSize(properSize.width, properSize.height);
                mSendingFrameWidth = properSize.width;
                mSendingFrameHeight = properSize.height;
                // TODO: 7/26/2017 work with  mJpegQ for better result
                mSendingJpegQ = 60;
                mParams.setJpegQuality(mSendingJpegQ);

                // TODO: 7/26/2017 choose least fps from camera
                int[] minFpsRange = mParams.getSupportedPreviewFpsRange().get(0);
                mParams.setPreviewFpsRange(minFpsRange[0],minFpsRange[1]);
                mCamera.setParameters(mParams);
                mCameraPreviewHost.refreshCamera(mCamera);
            }
        }catch (Exception e){
            Log.e(TAG,"loadCamera:" + e.getMessage());
            e.printStackTrace();
        }
        */
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
            mViewLocalVideo.release();
        }catch (Exception e){
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
        try {
            mViewRemoteVideo.release();
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
        // Update video view.
        updateVideoView();
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
        mButtonEndCall.setOnClickListener(new View.OnClickListener() {
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

        mButtonSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    onCameraSwitch();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, e.getMessage());
                }
            }
        });

        mButtonToggleMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    boolean enabled = onToggleMic();
                    mButtonToggleMic.setAlpha(enabled ? 1.0f : 0.3f);
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

    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(mContext);
    }

    private boolean captureToTexture() {
        return true;
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        Log.i(TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Log.i(TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        Log.i(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Log.i(TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    private void reportError(final String description) {
        getActivity().runOnUiThread(() -> {
            if (!isError) {
                isError = true;
                disconnectWithErrorMessage(description);
            }
        });
    }

    private VideoCapturer createVideoCapturer() {
        VideoCapturer videoCapturer;
        if (useCamera2()) {
            Log.i(TAG, "Creating capturer using camera2 API.");
            videoCapturer = createCameraCapturer(new Camera2Enumerator(mContext));
        } else {
            Log.i(TAG, "Creating capturer using camera1 API.");
            videoCapturer = createCameraCapturer(new Camera1Enumerator(captureToTexture()));
        }
        if (videoCapturer == null) {
            reportError("Failed to open camera");
            return null;
        }
        return videoCapturer;
    }



    // VideoCallFragment.OnCallEvents interface implementation.
    @Override
    public void onCallHangUp() {
        try {
            endCall();
            if(mListener != null)
                mListener.onVideoCallFragmentEndCall(mGuestContact);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "mButtonEndCall.onClick"+e.getMessage());
        }
    }

    @Override
    public void onCameraSwitch() {
        if (mPeerConnectionClient != null) {
            mPeerConnectionClient.switchCamera();
        }
    }

    @Override
    public void onCaptureFormatChange(int width, int height, int framerate) {
        if (mPeerConnectionClient != null) {
            mPeerConnectionClient.changeCaptureFormat(width, height, framerate);
        }
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
        VideoCapturer videoCapturer = null;
        if (peerConnectionParameters.videoCallEnabled) {
            videoCapturer = createVideoCapturer();
        }
        mPeerConnectionClient.createPeerConnection(mRootEglBase.getEglBaseContext(), mViewLocalVideo,
                mRemoteRenderers, videoCapturer, mSignalingParameters);

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
    public void onConnectedToRoom(final SignalingParameters params) {
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
                Log.i(TAG, "Set video maximum bitrate: " + peerConnectionParameters.videoMaxBitrate);
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
    public interface OnVideoCallFragmentInteractionListener {
        void onVideoCallFragmentEndCall(Contact contact);
        void onVideoCallFragmentSetCallSetting(Contact contact,CallSetting callSetting);
    }

    private class HeadsetPlugReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        Log.i(TAG, "Headset is unplugged");
                        break;
                    case 1:
                        Log.i(TAG, "Headset is plugged");
                        break;
                    default:
                        Log.i(TAG, "I have no idea what the headset state is");
                }
            }
        }
    }
}
