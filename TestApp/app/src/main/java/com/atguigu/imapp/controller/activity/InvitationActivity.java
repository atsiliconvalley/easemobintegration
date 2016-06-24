package com.atguigu.imapp.controller.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.ListView;
import android.widget.Toast;

import com.atguigu.imapp.R;
import com.atguigu.imapp.common.Constant;
import com.atguigu.imapp.event.GlobalEventNotifer;
import com.atguigu.imapp.model.InvitationInfo;
import com.atguigu.imapp.model.Model;
import com.atguigu.imapp.view.adapter.MyInvitationAdapter;
import com.hyphenate.EMContactListener;
import com.hyphenate.EMGroupChangeListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.exceptions.HyphenateException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by youni on 2016/5/25.
 */
public class InvitationActivity extends Activity implements MyInvitationAdapter.OnInvitationListener {
    private MyInvitationAdapter mAdapter;
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
        mAdapter = new MyInvitationAdapter(this,this,null);
        LocalBroadcastManager.getInstance(this).registerReceiver(invitationChangedReceiver,new IntentFilter(Constant.CONTACT_INVITATION_CHANGED));
        LocalBroadcastManager.getInstance(this).registerReceiver(invitationChangedReceiver,new IntentFilter(Constant.GROUP_INVITATION_MESSAGE_CHANGED));

        ListView lv = (ListView) findViewById(R.id.lv_invitation_list);

        lv.setAdapter(mAdapter);

        setupInvitations();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(invitationChangedReceiver);
    }

    void setupInvitations(){
        Model.getInstance().globalThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                mAdapter.refresh(Model.getInstance().getInvitationInfo());
            }
        });
    }

    @Override
    public void onAccepted(final InvitationInfo invitationInfo) {
        Model.getInstance().globalThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    EMClient.getInstance().contactManager().acceptInvitation(invitationInfo.getUser().getHxId());

                    Model.getInstance().updateInvitation(InvitationInfo.InvitationStatus.INVITE_ACCEPT, invitationInfo.getUser().getHxId());

                    mAdapter.refresh(Model.getInstance().getInvitationInfo());

                } catch (HyphenateException e) {
                    final String error = e.toString();
                    mH.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(InvitationActivity.this, error, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onRejected(final InvitationInfo invitationInfo) {
        Model.getInstance().globalThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    EMClient.getInstance().contactManager().declineInvitation(invitationInfo.getUser().getHxId());
                    Model.getInstance().removeInvitation(invitationInfo.getUser().getHxId());

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
        });
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
        Model.getInstance().globalThreadPool().execute(new Runnable() {
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
        });
    }

    @Override
    public void onGroupInvitationAccept(final InvitationInfo invitationInfo) {
        Model.getInstance().globalThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    EMClient.getInstance().groupManager().acceptInvitation(invitationInfo.getGroupInfo().getGroupId(), invitationInfo.getGroupInfo().getInviteTriggerUser());
                    Model.getInstance().acceptGroupInvitation(invitationInfo);
                    mAdapter.refresh(Model.getInstance().getInvitationInfo());

                    showMessage("接收邀请成功");
                } catch (HyphenateException e) {
                    e.printStackTrace();

                    showMessage("接收邀请失败 : " + e.toString());
                }
            }
        });
    }

    @Override
    public void onGroupApplicationReject(final InvitationInfo invitationInfo){
        Model.getInstance().globalThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    EMClient.getInstance().groupManager().declineApplication(invitationInfo.getGroupInfo().getGroupId(), invitationInfo.getGroupInfo().getInviteTriggerUser(), "拒绝你的申请");
                    Model.getInstance().rejectGroupApplication(invitationInfo);
                    mAdapter.refresh(Model.getInstance().getInvitationInfo());

                    showMessage("拒绝申请成功");
                } catch (HyphenateException e) {
                    e.printStackTrace();

                    showMessage("拒绝申请失败 ：" + e.toString());
                }
            }
        });
    }

    @Override
    public void onGroupInvitationReject(final InvitationInfo invitationInfo) {
        Model.getInstance().globalThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    EMClient.getInstance().groupManager().declineInvitation(invitationInfo.getGroupInfo().getGroupId(), invitationInfo.getGroupInfo().getInviteTriggerUser(), "拒绝加入");
                    Model.getInstance().rejectGroupInvitation(invitationInfo);
                    mAdapter.refresh(Model.getInstance().getInvitationInfo());

                    showMessage("拒绝邀请成功");
                } catch (HyphenateException e) {
                    e.printStackTrace();

                    showMessage("拒绝邀请失败 : " + e.toString());
                }
            }
        });
    }

    private BroadcastReceiver invitationChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()){
                case Constant.CONTACT_INVITATION_CHANGED:
                case Constant.GROUP_INVITATION_MESSAGE_CHANGED:
                    mAdapter.refresh(Model.getInstance().getInvitationInfo());
                    break;
                default:
                    break;
            }
        }
    };
}


