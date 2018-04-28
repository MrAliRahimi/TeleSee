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
 * {@link OutgoingCallFragment.OnOutgoingCallFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link OutgoingCallFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OutgoingCallFragment extends Fragment {
    public static final String TAG = "OutgoingCallFragment";
    private static final String CALLING_TO_CONTACT = "callingToContact";

    private Contact mCallingToContact;

    private OnOutgoingCallFragmentInteractionListener mListener;

    public OutgoingCallFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param callingToContact Parameter 1.
     * @return A new instance of fragment OutgoingCallFragment.
     */
    public static OutgoingCallFragment newInstance(Contact callingToContact) {
        OutgoingCallFragment fragment = new OutgoingCallFragment();
        Bundle args = new Bundle();
        args.putSerializable(CALLING_TO_CONTACT, callingToContact);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            if (getArguments() != null) {
                mCallingToContact = (Contact) getArguments().getSerializable(CALLING_TO_CONTACT);
            }
        }catch (Exception e){
            Log.e(TAG,"onCreate:"+e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_outgoing_call, container, false);
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
        TextView textOngoingCallSurname = (TextView) view.findViewById(R.id.textOngoingCallSurname);
        TextView textOngoingCallIp = (TextView) view.findViewById(R.id.textOngoingCallIp);
        textOngoingCallSurname.setText(mCallingToContact.getSurname());
        textOngoingCallIp.setText(mCallingToContact.getIP());
    }

    private void bindEventHandler(View view){
        //bind answer button event handler
        LinearLayout btnCancelCall = (LinearLayout) view.findViewById(R.id.btnCancelCall);
        btnCancelCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCancelCall();
            }
        });
    }

    private void onCancelCall(){
        try {
            if (mListener != null) {
                mListener.onOutgoingCallFragmentCancelCall(mCallingToContact);
            }
        }catch (Exception e){
            Log.e(TAG,"onCancelCall:"+e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnOutgoingCallFragmentInteractionListener) {
            mListener = (OnOutgoingCallFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnOutgoingCallFragmentInteractionListener");
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
     */
    public interface OnOutgoingCallFragmentInteractionListener {
        void onOutgoingCallFragmentCancelCall(Contact contact);
    }
}
