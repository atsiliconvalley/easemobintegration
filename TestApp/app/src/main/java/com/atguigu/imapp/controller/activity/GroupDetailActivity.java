package com.atguigu.imapp.controller.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.atguigu.imapp.R;
import com.atguigu.imapp.common.Constant;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMGroup;
import com.hyphenate.exceptions.HyphenateException;

/**
 * Created by youni on 16/6/21.
 */
public class GroupDetailActivity extends Activity {
    private Button exitBtn;
    private EMGroup group;
    private Activity me;
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
    }

    private void showMessage(final String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(me,message,Toast.LENGTH_LONG).show();
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
        exitBtn = (Button) findViewById(R.id.btn_exit_group);
    }

}
