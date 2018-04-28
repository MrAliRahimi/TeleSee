package com.example.ali.udpvideochat.view;

import android.content.Context;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.ali.udpvideochat.ContactArrayAdapter;
import com.example.ali.udpvideochat.ContactManager;
import com.example.ali.udpvideochat.DatabaseHandler;
import com.example.ali.udpvideochat.R;
import com.example.ali.udpvideochat.sample.iconswitch.IconSwitch;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MainFragment.OnMainFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MainFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainFragment extends Fragment implements IconSwitch.CheckedChangeListener{
    public static final String TAG = "MainFragment";
    // TODO: Rename parameter arguments, choose names that match
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnMainFragmentInteractionListener mListener;
    public ContactArrayAdapter mContactArrayAdapter;
    private RecyclerView mRecyclerViewContactList;
    private IconSwitch mIconSwitch;
    private TextView mTextMainFragmentProfileSurname;
    private TextView mTextMainFragmentProfileIp;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MainFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment MainFragment.
     */
    public static MainFragment newInstance() {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            DatabaseHandler db = new DatabaseHandler(getActivity().getBaseContext());
            mContactArrayAdapter = new ContactArrayAdapter(db.getAllContacts() ,(MainActivity)getActivity() );
        }catch (Exception e){
            Log.e(TAG,"onCreate:"+e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        try {
            mRecyclerViewContactList = (RecyclerView) view.findViewById(R.id.recyclerViewContactList);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
            mRecyclerViewContactList.setLayoutManager(layoutManager);
            mRecyclerViewContactList.setAdapter(mContactArrayAdapter);
            mIconSwitch = (IconSwitch)view.findViewById(R.id.switch_contact_list);
            mTextMainFragmentProfileSurname = (TextView) view.findViewById(R.id.textMainFragmentProfileSurname);
            mTextMainFragmentProfileIp = (TextView) view.findViewById(R.id.textMainFragmentProfileIp);
            mIconSwitch.setCheckedChangeListener(this);
            if(ContactManager.getMyContact() != null) {
                mTextMainFragmentProfileSurname.setText(ContactManager.getMyContact().getSurname());
                mTextMainFragmentProfileIp.setText(ContactManager.getMyContact().getIP());
            }
        }catch (Exception e){
            Log.e(TAG,"onCreateView:"+e.getMessage());
            e.printStackTrace();
        }
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnMainFragmentInteractionListener) {
            mListener = (OnMainFragmentInteractionListener) context;
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

    @Override
    public void onCheckChanged(IconSwitch.Checked current){
        DatabaseHandler db = new DatabaseHandler(getActivity().getBaseContext());
        if(current == IconSwitch.Checked.RIGHT){
            mContactArrayAdapter.updateDataSet(db.getOnLineContactsByServiceName(""));
        }else{
            mContactArrayAdapter.updateDataSet(db.getAllContacts());
        }
    }

    public void updateContact (){
        // TODO: 7/19/2017  NullPointerException happened in this line
        DatabaseHandler db = new DatabaseHandler(getActivity().getBaseContext());
        // TODO: 7/14/2017 if service starts after app, discovered devices will not shows up until reload app
        mContactArrayAdapter.updateDataSet(db.getAllContacts());
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
    public interface OnMainFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
