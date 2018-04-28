package com.example.ali.udpvideochat.view;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.ali.udpvideochat.Contact;
import com.example.ali.udpvideochat.R;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link IncomingCallFragment.OnIncomingCallFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link IncomingCallFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class IncomingCallFragment extends Fragment {
    public static final String TAG = "IncomingCallFragment";
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String CALLER_CONTACT = "callerContact";

    private Contact mCallerContact;

    private OnIncomingCallFragmentInteractionListener mListener;

    public IncomingCallFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param callerContact Parameter 1.
     * @return A new instance of fragment IncomingCallFragment.
     */
    public static IncomingCallFragment newInstance(Contact callerContact) {
        IncomingCallFragment fragment = new IncomingCallFragment();
        Bundle args = new Bundle();
        args.putSerializable(CALLER_CONTACT, callerContact);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            if (getArguments() != null) {
                mCallerContact = (Contact) getArguments().getSerializable(CALLER_CONTACT);
            }
        }catch (Exception e){
            Log.e(TAG,"onCreate:"+e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_incoming_call, container, false);
        try {
            loadDataOnForm(view);
            bindEventHandler(view);
        }catch (Exception e){
            Log.e(TAG,"onCreateView:"+e.getMessage());
            e.printStackTrace();
        }
        return view;
    }

    private void loadDataOnForm(View view){
        // show calling to contact details in view
        TextView textOngoingCallSurname = (TextView) view.findViewById(R.id.textCallerSurname);
        TextView textCallerIp = (TextView) view.findViewById(R.id.textCallerIp);
        textOngoingCallSurname.setText(mCallerContact.getSurname());
        textCallerIp.setText(mCallerContact.getIP());
        //todo:load avatar of caller in form
    }

    private void bindEventHandler(View view){
        //bind answer button event handler
        LinearLayout  layoutAnswerCall = (LinearLayout) view.findViewById(R.id.layoutAnswerCall);
        layoutAnswerCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAcceptCall();
            }
        });

        //bind reject button event handle
        LinearLayout  layoutRejectCall = (LinearLayout ) view.findViewById(R.id.layoutRejectCall);
        layoutRejectCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRejectCall();
            }
        });
    }

    private void onAcceptCall(){
        try {
            if (mListener != null) {
                mListener.onIncomingCallFragmentAcceptCall(mCallerContact);
            }
        }catch (Exception e){
            Log.e(TAG,"onAcceptCall:"+e.getMessage());
            e.printStackTrace();
        }
    }

    private void onRejectCall(){
        try {
            if (mListener != null) {
                mListener.onIncomingCallFragmentRejectCall(mCallerContact);
            }
        }catch (Exception e){
            Log.e(TAG,"onRejectCall:"+e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnIncomingCallFragmentInteractionListener) {
            mListener = (OnIncomingCallFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnIncomingCallFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnIncomingCallFragmentInteractionListener {
        void onIncomingCallFragmentAcceptCall(Contact contact);
        void onIncomingCallFragmentRejectCall(Contact contact);
    }
}
