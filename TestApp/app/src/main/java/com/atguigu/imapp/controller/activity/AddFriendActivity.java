package com.atguigu.imapp.controller.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.youni.testapp.R;
import com.hyphenate.chat.EMClient;
import com.hyphenate.exceptions.HyphenateException;

/**
 * Created by youni on 2016/5/22.
 */
public class AddFriendActivity extends Activity{

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_friend);

        init();
    }

    void init(){
        Button addBtn = (Button) findViewById(R.id.btn_add_friend);

        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView tvName = (TextView) findViewById(R.id.tv_friend_name);

                final String name = tvName.getText().toString();

                if(TextUtils.isEmpty(name)){
                    Toast.makeText(AddFriendActivity.this,"invalid friend name",Toast.LENGTH_LONG).show();

                    return;
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String hxId = EMClient.getInstance().getCurrentUser();
                            String nick = hxId + "_凤凰";
                            EMClient.getInstance().contactManager().addContact(name,nick);

                            AddFriendActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(AddFriendActivity.this,"invitation is sent",Toast.LENGTH_LONG).show();
                                }
                            });
                        } catch (HyphenateException e) {
                            e.printStackTrace();

                            final String error = e.toString();
                            AddFriendActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(AddFriendActivity.this, error,Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                }).start();
            }
        });
    }
}
