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
import com.atguigu.imapp.model.IMUser;
import com.atguigu.imapp.model.Model;
import com.atguigu.imapp.view.adapter.GroupMembersAdapter;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMGroup;
import com.hyphenate.easeui.widget.EaseExpandGridView;
import com.hyphenate.exceptions.HyphenateException;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private List<IMUser> appMembers;
    private ExecutorService singleQueue = Executors.newSingleThreadExecutor();
    private boolean isTaskRunning = false;

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
                            refresh(null);
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

        // 1. 先显示本地的群成员
        asyncLoadLocalGroup();

        // 2. 再从服务器上获取最新的群成员
        asyncFetchAndUpdateGroup();
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
    public void onDeleteMember(final IMUser user) {
        Model.getInstance().globalThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    EMClient.getInstance().groupManager().removeUserFromGroup(group.getGroupId(),user.getHxId());
                    showMessage("移除成功!");
                    asyncLoadLocalGroup();
                } catch (HyphenateException e) {
                    e.printStackTrace();

                    showMessage("移除失败 : " + e.toString());
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK){
            if(requestCode == REQUEST_PICK_UP_CONTACTS){

                final String[] members = data.getStringArrayExtra("newmembers");


                Model.getInstance().globalThreadPool().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (group.getOwner().equals(EMClient.getInstance().getCurrentUser())) {
                            try {
                                EMClient.getInstance().groupManager().addUsersToGroup(group.getGroupId(), members);
                                asyncFetchAndUpdateGroup();
                                showMessage("邀请已发");
                            } catch (HyphenateException e) {
                                e.printStackTrace();

                                showMessage("邀请失败" + e.toString());
                            }
                        } else {
                            try {
                                if (group.isAllowInvites()) {
                                    EMClient.getInstance().groupManager().inviteUser(group.getGroupId(), members, "invite you to join the group");
                                    asyncFetchAndUpdateGroup();
                                    showMessage("邀请已发");
                                } else {
                                    showMessage("没有权限");
                                }
                            } catch (HyphenateException e) {
                                e.printStackTrace();

                                showMessage("邀请失败" + e.toString());
                            }
                        }
                    }
                });

                return;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    void asyncFetchAndUpdateGroup(){
        singleQueue.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    EMClient.getInstance().groupManager().getGroupFromServer(group.getGroupId());

                    List<String> members = group.getMembers();

                    List<IMUser> appUsers = Model.getInstance().getContactsByHx(members);


                    for (IMUser user : appUsers) {
                        if (members.contains(user.getHxId())) {
                            members.remove(user.getHxId());
                        }
                    }

                    List<IMUser> serverUsers = null;
                    if (members.size() > 0) {
                        serverUsers = Model.getInstance().fetchUsersFromServer(members);
                    }

                    if (serverUsers != null && serverUsers.size() > 0) {
                        appUsers.addAll(serverUsers);
                    }


                    refresh(appUsers);
                } catch (HyphenateException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    void asyncLoadLocalGroup(){
        singleQueue.execute(new Runnable() {
            @Override
            public void run() {
                List<String> members = group.getMembers();

                List<IMUser> appUsers = Model.getInstance().getContactsByHx(members);

                refresh(appUsers);
            }
        });
    }

    void refresh(List<IMUser> members){
        membersAdapter.refresh(members);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        singleQueue.shutdownNow();
        isTaskRunning = false;
    }
}
