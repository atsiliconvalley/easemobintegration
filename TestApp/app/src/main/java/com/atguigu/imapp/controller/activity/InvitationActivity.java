package com.atguigu.imapp.controller.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ListView;
import android.widget.Toast;

import com.example.youni.testapp.R;
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
    private List<InvitationInfo> mInvitations;
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
        mInvitations = new ArrayList<>();
        mAdapter = new MyInvitationAdapter(this,this,mInvitations);

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

                    showMessage("拒绝邀请失败 : " + e.toString());
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


