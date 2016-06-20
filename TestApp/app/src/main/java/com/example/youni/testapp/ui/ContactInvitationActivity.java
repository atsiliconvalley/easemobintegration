package com.example.youni.testapp.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.youni.testapp.R;
import com.example.youni.testapp.model.DemoUser;
import com.example.youni.testapp.model.InvitationInfo;
import com.example.youni.testapp.model.Model;
import com.hyphenate.EMContactListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.exceptions.HyphenateException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by youni on 2016/5/25.
 */
public class ContactInvitationActivity extends Activity implements OnContactInvitationListener{
    private List<InvitationInfo> mInvitations;
    private MyAdapter mAdapter;
    private Handler mH = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_contact_invitation);

        init();
    }

    void init(){
        mInvitations = new ArrayList<>();
        mAdapter = new MyAdapter(this,this,mInvitations);

        ListView lv = (ListView) findViewById(R.id.lv_invitation_list);

        lv.setAdapter(mAdapter);

        Model.getInstance().addContactListeners(contactListener);

        setupInvitations();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Model.getInstance().removeContactListener(contactListener);
    }

    void setupInvitations(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                mAdapter.refresh(Model.getInstance().getInvitationInfo());
            }
        }).start();
    }

    @Override
    public void onContactAccepted(final String hxId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    EMClient.getInstance().contactManager().acceptInvitation(hxId);

                    //Model.getInstance().removeInvitation(hxId);
                    Model.getInstance().updateInvitation(InvitationInfo.InvitationStatus.INVITE_ACCEPT,hxId);

                    mAdapter.refresh(Model.getInstance().getInvitationInfo());

                } catch (HyphenateException e) {
                    final String error = e.toString();
                    mH.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ContactInvitationActivity.this,error,Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    public void onContactRejected(final String hxId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    EMClient.getInstance().contactManager().declineInvitation(hxId);
                    Model.getInstance().removeInvitation(hxId);
                    mAdapter.refresh(Model.getInstance().getInvitationInfo());
                } catch (HyphenateException e) {
                    final String error = e.toString();

                    mH.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ContactInvitationActivity.this,error,Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    class MyContactListener implements EMContactListener{

        @Override
        public void onContactAdded(String s) {

        }

        @Override
        public void onContactDeleted(String s) {

        }

        @Override
        public void onContactInvited(String s, String s1) {

        }

        @Override
        public void onContactAgreed(String s) {
            mAdapter.refresh(Model.getInstance().getInvitationInfo());
        }

        @Override
        public void onContactRefused(String s) {

        }
    }

    EMContactListener contactListener = new MyContactListener();
}

interface OnContactInvitationListener{
    void onContactAccepted(String hxId);
    void onContactRejected(String hxId);
}

class MyAdapter extends BaseAdapter{
    private final Context context;
    private final OnContactInvitationListener invitationListener;
    private List<InvitationInfo> inviteInfos;
    private Handler mH = new Handler();

    MyAdapter(Context context, OnContactInvitationListener invitationListener, List<InvitationInfo> inviteInfos){
        inviteInfos = new ArrayList<>();

        this.inviteInfos = new ArrayList<>();
        this.inviteInfos.addAll(inviteInfos);
        this.context = context;
        this.invitationListener = invitationListener;
    }

    @Override
    public int getCount() {
        return inviteInfos.size();
    }

    @Override
    public Object getItem(int position) {
        return inviteInfos.get(position
        );
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder = null;

        InvitationInfo inviteInfo = inviteInfos.get(position);

        final DemoUser user  = inviteInfos.get(position).getUser();

        if(convertView == null){
            holder = new ViewHolder();

            convertView = View.inflate(context,R.layout.row_contact_invitation,null);

            holder.name = (TextView) convertView.findViewById(R.id.tv_user_name);
            holder.reason = (TextView) convertView.findViewById(R.id.tv_invite_reason);

            holder.btnAccept = (Button) convertView.findViewById(R.id.btn_accept);
            holder.btnReject = (Button) convertView.findViewById(R.id.btn_reject);

            convertView.setTag(holder);

            holder.btnAccept.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    invitationListener.onContactAccepted(user.getHxId());
                }
            });

            holder.btnReject.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    invitationListener.onContactRejected(user.getHxId());
                }
            });
        }else{
            holder = (ViewHolder) convertView.getTag();
        }

        if(inviteInfo.getStatus() == InvitationInfo.InvitationStatus.NEW_INVITE){
            if(inviteInfo.getReason() != null){
                holder.reason.setText(inviteInfo.getReason());
            }else{
                holder.reason.setText("加个好友吧!");
            }
        }else if(inviteInfo.getStatus() == InvitationInfo.InvitationStatus.INVITE_ACCEPT){
            holder.reason.setText("your added new friend " + user.getNick());

            holder.btnAccept.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);
        }else{
            holder.reason.setText(user.getNick() + " accepted your invitation");
            holder.btnAccept.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);
        }

        holder.name.setText(user.getNick());

        return convertView;
    }

    public void refresh(final List<InvitationInfo> inviteInfos){
        mH.post(new Runnable() {
            @Override
            public void run() {
                MyAdapter.this.inviteInfos.clear();
                MyAdapter.this.inviteInfos.addAll(inviteInfos);
                notifyDataSetChanged();
            }
        });
    }

    static class ViewHolder{
        TextView name;
        TextView reason;
        Button btnAccept;
        Button btnReject;
    }
}