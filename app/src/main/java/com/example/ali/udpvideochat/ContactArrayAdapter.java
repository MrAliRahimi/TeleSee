package com.example.ali.udpvideochat;

import android.annotation.SuppressLint;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import com.example.ali.udpvideochat.sample.CircularImageView;


/**
 * Created by ali on 6/22/2017.
 */
public class ContactArrayAdapter extends RecyclerView.Adapter<ContactArrayAdapter.ContactViewHolder> {
    private List<Contact> mContactList;
    private int     mItemWidth; //todo:what is this for?
    public static final String TAG = "ContactArrayAdapter";

    public static class ContactViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public TextView txtContactItemSurname;
        public TextView txtContactItemIp;
        public CircularImageView imageContactItemAvatar;
        public LinearLayout layoutContactItemState;
        public LinearLayout layoutButtonVideoCall;
        public LinearLayout layoutButtonVoiceCall;

        public ContactViewHolder(View view) {
            super(view);
            try {
                txtContactItemSurname = (TextView) view.findViewById(R.id.txtContactItemSurname);
                txtContactItemIp = (TextView) view.findViewById(R.id.txtContactItemIp);
                layoutContactItemState = (LinearLayout) view.findViewById(R.id.layoutContactItemState);
                imageContactItemAvatar = (CircularImageView) view.findViewById(R.id.imageContactItemAvatar);
                layoutButtonVideoCall = (LinearLayout) view.findViewById(R.id.layoutButtonVideoCall);
                layoutButtonVoiceCall = (LinearLayout) view.findViewById(R.id.layoutButtonVoiceCall);
                layoutButtonVideoCall.setOnClickListener(this);
                layoutButtonVoiceCall.setOnClickListener(this);
            }catch (Exception e){
                Log.e(TAG, "ContactViewHolder:"+e.getMessage());
                e.printStackTrace();
            }
        }

        // onClick Listener for buttons of view
        @Override
        public void onClick(View v) {
            try {
                if (v.getId() == layoutButtonVideoCall.getId()){
                    if(ContactArrayAdapter.callback != null){
                        callback.onContactArrayAdapterVideoCall(getAdapterPosition());
                    }
                } else if(v.getId() == layoutButtonVoiceCall.getId()) {
                    if(ContactArrayAdapter.callback != null){
                        callback.onContactArrayAdapterVoiceCall(getAdapterPosition());
                    }
                }
            }catch (Exception e){
                Log.e(TAG, "onClick:"+e.getMessage());
                e.printStackTrace();
            }

        }
    }

    public static ContactArrayAdapterCallback callback;

    public interface ContactArrayAdapterCallback {
        void onContactArrayAdapterVideoCall(int position);

        void onContactArrayAdapterVoiceCall(int position);
    }

    public ContactArrayAdapter(List<Contact> objects, ContactArrayAdapterCallback adapterCallBack) {
        this.mContactList = objects;
        this.callback = adapterCallBack;
    }

    @Override
    public ContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mItemWidth = parent.getWidth();
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_contact_list_item, parent, false);
        return new ContactViewHolder(itemView);
    }

    @SuppressLint("RecyclerView")
    @Override
    public void onBindViewHolder(ContactViewHolder holder, final int position) {
        final Contact contact = mContactList.get(position);
        holder.txtContactItemSurname.setText(contact.getSurname());
        holder.txtContactItemIp.setText(contact.getIP());
        if(contact.getOnline())
            holder.layoutContactItemState.setBackgroundResource(R.drawable.lin_solid_adapter_online);
        else
            holder.layoutContactItemState.setBackgroundResource(R.drawable.lin_solid_adapter_offline);
        //todo: bind avatar to list
        //imageView.setImageResource(_imageId[position]);
    }

    @Override
    public int getItemCount() {
        return mContactList.size();
    }

    //return selected Contact of list
    public Contact getItem(int position){
        return mContactList.get(position);
    }

    //called when contacts in database change and updated contactList o adapter and notify UI to update
    public void updateDataSet(List<Contact> contactList){
        mContactList.clear();
        mContactList.addAll(contactList);
        this.notifyDataSetChanged();
    }
}
