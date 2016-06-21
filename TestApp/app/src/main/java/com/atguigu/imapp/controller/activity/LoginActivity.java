package com.atguigu.imapp.controller.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.youni.testapp.R;
import com.atguigu.imapp.model.Model;
import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMGroup;
import com.hyphenate.exceptions.HyphenateException;

import java.util.List;

import static android.widget.Toast.makeText;

/**
 * Created by youni on 2016/5/18.
 */
public class LoginActivity extends Activity{

    private EditText mEtName;
    private EditText mEtPwd;

    private Button mRegisterBtn;
    private Button mLoginBtn;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        init();
    }

    private void init(){
        mEtName = (EditText) findViewById(R.id.et_user_name);
        mEtPwd = (EditText) findViewById(R.id.et_pwd);

        mRegisterBtn = (Button) findViewById(R.id.btn_register);
        mLoginBtn = (Button) findViewById(R.id.btn_login);

        mRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register();
            }
        });

        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
    }

    private void register() {
        if(!isValidNameOrPwd()){
            Toast.makeText(this,"invalid pwd or name",Toast.LENGTH_LONG).show();
            return;
        }

        new Thread(){
            @Override
            public void run(){

                try {
                    EMClient.getInstance().createAccount(mEtName.getText().toString(),mEtPwd.getText().toString());
                } catch (HyphenateException e) {
                    final String msg = e.toString();

                    LoginActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(LoginActivity.this, "注册失败！" +  msg, Toast.LENGTH_LONG).show();
                        }
                    });
                }

                LoginActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(LoginActivity.this,"注册成功！",Toast.LENGTH_LONG).show();
                    }
                });
            }
        }.start();
    }

    private void login(){
        if(!isValidNameOrPwd()){
            Toast.makeText(this,"invalid pwd or name",Toast.LENGTH_LONG).show();
            return;
        }

        final ProgressDialog pd = new ProgressDialog(this);
        pd.show();

        EMClient.getInstance().login(mEtName.getText().toString(), mEtPwd.getText().toString(), new EMCallBack() {
            @Override
            public void onSuccess() {
                // 通知model login 成功
                Model.getInstance().onLoggedIn(EMClient.getInstance().getCurrentUser());

                // 取本地会话
                EMClient.getInstance().chatManager().loadAllConversations();

                // 取本地群组
                EMClient.getInstance().groupManager().loadAllGroups();

                //  同步服务器联系人
                List<String> hxUsers = null;
                try {
                    hxUsers = EMClient.getInstance().contactManager().getAllContactsFromServer();
                } catch (HyphenateException e) {
                    e.printStackTrace();
                }

                // 和本地的users做同步，我们会建立一个表，又来专门存好友信息，包括环信ID

                if (hxUsers != null) {
                    synncWithLocal(hxUsers);
                }

                // 同步服务器群组
                List<EMGroup> groups = null;

                try {
                    groups = EMClient.getInstance().groupManager().getJoinedGroupsFromServer();
                } catch (HyphenateException e) {
                    e.printStackTrace();
                }

                // 和本地的群信息表做同步
                syncGroupWithLocal(groups);

                LoginActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pd.cancel();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));

                        finish();
                    }
                });
            }

            @Override
            public void onError(int errorCode, final String error) {
                LoginActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pd.cancel();

                        Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onProgress(int i, String s) {

            }
        });
    }



    /**
     * 和本地的联系人表做同步
     * @param hxUsers
     */
    private void synncWithLocal(List<String> hxUsers) {

    }

    /**
     * 和本地的群组表做同步
     * @param groups
     */
    private void syncGroupWithLocal(List<EMGroup> groups) {

    }

    private boolean isValidNameOrPwd(){
        return (!TextUtils.isEmpty(mEtName.getText().toString()) && !TextUtils.isEmpty(mEtPwd.getText().toString()));
    }
}


