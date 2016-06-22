package com.atguigu.imapp.controller.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.atguigu.imapp.R;
import com.atguigu.imapp.common.Constant;
import com.atguigu.imapp.view.adapter.GroupMembersAdapter;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMGroup;
import com.hyphenate.easeui.widget.EaseExpandGridView;
import com.hyphenate.exceptions.HyphenateException;

/**
 * Created by youni on 16/6/21.
 */
public class GroupDetailActivity extends Activity implements GroupMembersAdapter.OnGroupMembersListener{
    private Button exitBtn;
    private EMGroup group;
    private EaseExpandGridView gridViewMembers;
    private GroupMembersAdapter membersAdapter;
    private Activity me;
    private Handler mH = new Handler();

    private int REQUEST_PICK_UP_CONTACTS = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_group_detail);

        String groupId = getIntent().getExtras().getString(Constant.GROUP_ID);

        group = EMClient.getInstance().groupManager().getGroup(groupId);

        if(group == null){
            finish();
            return;
        }

        me = this;
        findView();
        initView();
    }

    private void initView() {
        if(EMClient.getInstance().getCurrentUser().equals(group.getOwner())){
            exitBtn.setText("解散群");

            exitBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                EMClient.getInstance().groupManager().destroyGroup(group.getGroupId());
                                showMessage("解散群成功");
                                broadCastExitGroup();
                                finish();
                            } catch (HyphenateException e) {
                                e.printStackTrace();
                                showMessage("解散群失败 : " + e.toString());
                            }
                        }
                    }).start();
                }
            });
        }else{
            exitBtn.setText("退群");

            exitBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                EMClient.getInstance().groupManager().leaveGroup(group.getGroupId());
                                showMessage("退群成功");
                                broadCastExitGroup();
                                finish();
                            } catch (HyphenateException e) {
                                e.printStackTrace();
                                showMessage("退群失败 : " + e.toString());
                            }
                        }
                    }).start();
                }
            });
        }

        gridViewMembers.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (membersAdapter.getDeleteModel()) {
                            membersAdapter.setDeleteModel(false);
                            membersAdapter.refresh(group.getMembers());
                            return true;
                        }
                        break;
                    default:
                        break;
                }
                return false;
            }
        });

        boolean canAddMember = (group.getOwner().equals(EMClient.getInstance().getCurrentUser()) || group.isAllowInvites());
        membersAdapter = new GroupMembersAdapter(this,this,canAddMember);
        gridViewMembers.setAdapter(membersAdapter);

        membersAdapter.refresh(group.getMembers());
        asyncUpdateGroup();
    }

    private void showMessage(final String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(me, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void broadCastExitGroup(){
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(me);

        Intent intent = new Intent(Constant.EXIT_GROUP_ACTION);
        intent.putExtra(Constant.GROUP_ID,group.getGroupId());

        lbm.sendBroadcast(intent);
    }

    private void findView() {
        gridViewMembers = (EaseExpandGridView) findViewById(R.id.gv_member_list);

        exitBtn = (Button) findViewById(R.id.btn_exit_group);
    }

    @Override
    public void onAddMember() {
        startActivityForResult(new Intent(me, GroupPickContactsActivity.class).putExtra(Constant.GROUP_ID,group.getGroupId()), REQUEST_PICK_UP_CONTACTS);
    }

    @Override
    public void onDeleteMember(String member) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK){
            if(requestCode == REQUEST_PICK_UP_CONTACTS){

                final String[] members = data.getStringArrayExtra("newmembers");


                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if(group.getOwner().equals(EMClient.getInstance().getCurrentUser())){
                            try {
                                EMClient.getInstance().groupManager().addUsersToGroup(group.getGroupId(),members);
                                EMClient.getInstance().groupManager().getGroupFromServer(group.getGroupId());
                                membersAdapter.refresh(group.getMembers());
                            } catch (HyphenateException e) {
                                e.printStackTrace();

                                showMessage("邀请失败" + e.toString());
                            }
                        }else{
                            try {
                                if(group.isAllowInvites()){
                                    EMClient.getInstance().groupManager().inviteUser(group.getGroupId(),members,"invite you to join the group");
                                    EMClient.getInstance().groupManager().getGroupFromServer(group.getGroupId());
                                }else{
                                    showMessage("没有权限");
                                }
                            } catch (HyphenateException e) {
                                e.printStackTrace();

                                showMessage("邀请失败" + e.toString());
                            }
                        }
                    }
                }).start();

                return;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    void asyncUpdateGroup(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    EMClient.getInstance().groupManager().getGroupFromServer(group.getGroupId());

                    membersAdapter.refresh(group.getMembers());
                } catch (HyphenateException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
