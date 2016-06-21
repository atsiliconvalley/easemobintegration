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
import com.hyphenate.EMGroupChangeListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.exceptions.HyphenateException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by youni on 2016/5/25.
 */
public class InvitationActivity extends Activity implements OnInvitationListener {
    private List<InvitationInfo> mInvitations;
    private MyAdapter mAdapter;
    private Handler mH = new Handler();
    private Activity me;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_contact_invitation);
        me = this;
        init();
    }

    void init(){
        mInvitations = new ArrayList<>();
        mAdapter = new MyAdapter(this,this,mInvitations);

        ListView lv = (ListView) findViewById(R.id.lv_invitation_list);

        lv.setAdapter(mAdapter);

        Model.getInstance().addContactListeners(contactListener);
        Model.getInstance().addGroupChangeListener(groupChangeListener);

        setupInvitations();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Model.getInstance().removeContactListener(contactListener);
        Model.getInstance().removeGroupChangeListener(groupChangeListener);
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
    public void onAccepted(final String hxId) {
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
                            Toast.makeText(InvitationActivity.this,error,Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    public void onRejected(final String hxId) {
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
                            Toast.makeText(InvitationActivity.this,error,Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void showMessage(final String message){
        mH.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(me,message,Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onGroupApplicationAccept(final InvitationInfo invitationInfo) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    EMClient.getInstance().groupManager().acceptApplication(invitationInfo.getGroupInfo().getGroupId(), invitationInfo.getGroupInfo().getInviteTriggerUser());
                    Model.getInstance().acceptGroupApplication(invitationInfo);
                    mAdapter.refresh(Model.getInstance().getInvitationInfo());

                    showMessage("接受申请成功");
                } catch (HyphenateException e) {
                    e.printStackTrace();

                    showMessage("群申请失败 : " + e.toString());
                }
            }
        }).start();
    }

    @Override
    public void onGroupInvitationAccept(final InvitationInfo invitationInfo) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    EMClient.getInstance().groupManager().acceptInvitation(invitationInfo.getGroupInfo().getGroupId(),invitationInfo.getGroupInfo().getInviteTriggerUser());
                    Model.getInstance().acceptGroupInvitation(invitationInfo);
                    mAdapter.refresh(Model.getInstance().getInvitationInfo());

                    showMessage("接收邀请成功");
                } catch (HyphenateException e) {
                    e.printStackTrace();

                    showMessage("接收邀请失败 : " + e.toString());
                }
            }
        }).start();
    }

    @Override
    public void onGroupApplicationReject(final InvitationInfo invitationInfo){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    EMClient.getInstance().groupManager().declineApplication(invitationInfo.getGroupInfo().getGroupId(),invitationInfo.getGroupInfo().getInviteTriggerUser(),"拒绝你的申请");
                    Model.getInstance().rejectGroupApplication(invitationInfo);
                    mAdapter.refresh(Model.getInstance().getInvitationInfo());

                    showMessage("拒绝申请成功");
                } catch (HyphenateException e) {
                    e.printStackTrace();

                    showMessage("拒绝申请失败 ：" + e.toString());
                }
            }
        }).start();
    }

    @Override
    public void onGroupInvitationReject(final InvitationInfo invitationInfo) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    EMClient.getInstance().groupManager().declineInvitation(invitationInfo.getGroupInfo().getGroupId(),invitationInfo.getGroupInfo().getInviteTriggerUser(),"拒绝加入");
                    Model.getInstance().rejectGroupInvitation(invitationInfo);
                    mAdapter.refresh(Model.getInstance().getInvitationInfo());

                    showMessage("拒绝邀请成功");
                } catch (HyphenateException e) {
                    e.printStackTrace();

                    showMessage("拒绝邀请失败 : " +e.toString());
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

    EMGroupChangeListener groupChangeListener = new EMGroupChangeListener() {
        @Override
        public void onInvitationReceived(String s, String s1, String s2, String s3) {
            mAdapter.refresh(Model.getInstance().getInvitationInfo());
        }

        @Override
        public void onApplicationReceived(String s, String s1, String s2, String s3) {
            mAdapter.refresh(Model.getInstance().getInvitationInfo());
        }

        @Override
        public void onApplicationAccept(String s, String s1, String s2) {
            mAdapter.refresh(Model.getInstance().getInvitationInfo());
        }

        @Override
        public void onApplicationDeclined(String s, String s1, String s2, String s3) {
            mAdapter.refresh(Model.getInstance().getInvitationInfo());
        }

        @Override
        public void onInvitationAccpted(String s, String s1, String s2) {
            mAdapter.refresh(Model.getInstance().getInvitationInfo());
        }

        @Override
        public void onInvitationDeclined(String s, String s1, String s2) {
            mAdapter.refresh(Model.getInstance().getInvitationInfo());
        }

        @Override
        public void onUserRemoved(String s, String s1) {

        }

        @Override
        public void onGroupDestroy(String s, String s1) {

        }

        @Override
        public void onAutoAcceptInvitationFromGroup(String s, String s1, String s2) {

        }
    };

    EMContactListener contactListener = new MyContactListener();
}

interface OnInvitationListener {
    void onAccepted(String hxId);
    void onRejected(String hxId);
    void onGroupApplicationAccept(InvitationInfo invitationInfo);
    void onGroupInvitationAccept(InvitationInfo invitationInfo);

    void onGroupApplicationReject(InvitationInfo invitationInfo);
    void onGroupInvitationReject(InvitationInfo invitationInfo);
}

class MyAdapter extends BaseAdapter{
    private final Context context;
    private final OnInvitationListener invitationListener;
    private List<InvitationInfo> inviteInfos;
    private Handler mH = new Handler();

    MyAdapter(Context context, OnInvitationListener invitationListener, List<InvitationInfo> inviteInfos){
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

        final InvitationInfo inviteInfo = inviteInfos.get(position);

        final DemoUser user  = inviteInfos.get(position).getUser();

        boolean isGroupInvite = (user == null);
        if(convertView == null){
            holder = new ViewHolder();

            convertView = View.inflate(context,R.layout.row_contact_invitation,null);

            holder.name = (TextView) convertView.findViewById(R.id.tv_user_name);
            holder.reason = (TextView) convertView.findViewById(R.id.tv_invite_reason);

            holder.btnAccept = (Button) convertView.findViewById(R.id.btn_accept);
            holder.btnReject = (Button) convertView.findViewById(R.id.btn_reject);

            convertView.setTag(holder);

            if(isGroupInvite){
                if(inviteInfo.getStatus() == InvitationInfo.InvitationStatus.NEW_GROUP_APPLICATION){
                    holder.btnAccept.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            invitationListener.onGroupApplicationAccept(inviteInfo);
                        }
                    });

                    holder.btnReject.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            invitationListener.onGroupApplicationReject(inviteInfo);
                        }
                    });

                }else if(inviteInfo.getStatus() == InvitationInfo.InvitationStatus.NEW_GROUP_INVITE){
                    holder.btnAccept.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            invitationListener.onGroupInvitationAccept(inviteInfo);
                        }
                    });

                    holder.btnReject.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            invitationListener.onGroupInvitationReject(inviteInfo);
                        }
                    });
                }
            }else{
                holder.btnAccept.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        invitationListener.onAccepted(user.getHxId());
                    }
                });

                holder.btnReject.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        invitationListener.onRejected(user.getHxId());
                    }
                });
            }
        }else{
            holder = (ViewHolder) convertView.getTag();
        }

        if(!isGroupInvite){
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
            }else if(inviteInfo.getStatus() == InvitationInfo.InvitationStatus.INVITE_ACCEPT_BY_PEER){
                holder.reason.setText(user.getNick() + " accepted your invitation");
                holder.btnAccept.setVisibility(View.GONE);
                holder.btnReject.setVisibility(View.GONE);
            }

            holder.name.setText(user.getNick());
        }else{// group invitation
            holder.name.setText(inviteInfo.getGroupInfo().getGroupName() + " : " + inviteInfo.getGroupInfo().getInviteTriggerUser());
            holder.btnReject.setVisibility(View.GONE);
            holder.btnAccept.setVisibility(View.GONE);

            switch(inviteInfo.getStatus()){
                case GROUP_APPLICATION_ACCEPTED:
                    holder.reason.setText("您的群申请请已经被接受");
                    break;

                case GROUP_INVITE_ACCEPTED:
                    holder.reason.setText("您的群邀请已经被接收");
                    break;

                case GROUP_APPLICATION_DECLINED:
                    holder.reason.setText("你的群申请已经被拒绝");
                    break;

                case GROUP_INVITE_DECLINED:
                    holder.reason.setText("您的群邀请已经被拒绝");
                    break;

                case NEW_GROUP_INVITE:
                    holder.btnReject.setVisibility(View.VISIBLE);
                    holder.btnAccept.setVisibility(View.VISIBLE);

                    holder.reason.setText("您收到了群邀请");
                    break;

                case NEW_GROUP_APPLICATION:
                    holder.btnReject.setVisibility(View.VISIBLE);
                    holder.btnAccept.setVisibility(View.VISIBLE);
                    holder.reason.setText("您收到了群申请");
                    break;

                case GROUP_ACCEPT_INVITE:
                    holder.reason.setText("您邀请已经发出，等待对方接收");
                    break;

                case GROUPO_ACCEPT_APPLICATION:
                    holder.reason.setText("你的群申请已经发出，等待批准入群");
                    break;
            }
        }

        return convertView;
    }

    public void refresh(final List<InvitationInfo> inviteInfos){

        mH.removeCallbacks(refreshRunnable);
        mH.post(refreshRunnable);
    }

    Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            MyAdapter.this.inviteInfos.clear();
            MyAdapter.this.inviteInfos.addAll(inviteInfos);
            notifyDataSetChanged();
        }
    };

    static class ViewHolder{
        TextView name;
        TextView reason;
        Button btnAccept;
        Button btnReject;
    }
}