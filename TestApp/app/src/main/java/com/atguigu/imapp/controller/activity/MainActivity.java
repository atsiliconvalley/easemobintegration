package com.atguigu.imapp.controller.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.atguigu.imapp.R;
import com.atguigu.imapp.model.Model;
import com.atguigu.imapp.controller.fragment.ContactListFragment;
import com.atguigu.imapp.controller.fragment.ConversationListFragment;
import com.atguigu.imapp.controller.fragment.SettingsFragment;
import com.hyphenate.EMContactListener;
import com.hyphenate.EMMessageListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.easeui.controller.EaseUI;

import java.util.List;

public class MainActivity extends FragmentActivity {

    Fragment mSetttingsFragment;
    ConversationListFragment mConversationListFragment;
    ContactListFragment mContactListFragment;
    Model.OnSyncListener mContactSyncListener;
    int currentId;
    EMContactListener mContactListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        RadioGroup group = (RadioGroup) findViewById(R.id.tab_group);

        mSetttingsFragment = new SettingsFragment();
        mConversationListFragment = new ConversationListFragment();
        mContactListFragment = new ContactListFragment();

        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (currentId != checkedId) {
                    currentId = checkedId;

                    Fragment fragment = null;

                    switch (currentId) {
                        case R.id.conv_list_btn:
                            fragment = mConversationListFragment;
                            break;

                        case R.id.contact_list_btn:
                            fragment = mContactListFragment;
                            break;

                        case R.id.setting_btn:
                            fragment = mSetttingsFragment;
                            break;

                        default:
                            fragment = mSetttingsFragment;
                    }

                    switchFragment(fragment);
                }
            }
        });

        group.check(R.id.conv_list_btn);

        currentId = R.id.conv_list_btn;

        switchFragment(mConversationListFragment);

        Model.getInstance().loadLocalContacts();
        if(!Model.getInstance().isContactSynced()){
            Model.getInstance().asyncfetchUsers();
        }
    }

    private void init(){
        initListener();
    }

    private void switchFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();

        FragmentTransaction ft = fm.beginTransaction();

        ft.replace(R.id.fragment_main, fragment);
        ft.show(fragment);

        ft.commit();
    }

    void initListener(){
        mContactListener = new EMContactListener() {
            @Override
            public void onContactAdded(String s) {
//                DemoUser user = new DemoUser();
//
//                user.hxId = s;
//
//                Model.getInstance().addUser(user);

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mContactListFragment.setupContacts();
                    }
                });
            }

            @Override
            public void onContactDeleted(String s) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mContactListFragment.setupContacts();
                    }
                });
            }

            @Override
            public void onContactInvited(String s, String s1) {
//                try {
//                    EMClient.getInstance().contactManager().acceptInvitation(s);
//                } catch (HyphenateException e) {
//                    e.printStackTrace();
//                }
            }

            @Override
            public void onContactAgreed(final String s) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,s + "同意了您的好友请求",Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onContactRefused(String s) {

            }
        };

        Model.getInstance().addContactListeners(mContactListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        EMClient.getInstance().chatManager().addMessageListener(messageListener);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

        EMClient.getInstance().chatManager().removeMessageListener(messageListener);
        if(mContactListener != null){
            Model.getInstance().removeContactListener(mContactListener);
        }
    }

    private EMMessageListener messageListener = new EMMessageListener() {
        @Override
        public void onMessageReceived(List<EMMessage> list) {
            EaseUI.getInstance().getNotifier().onNewMesg(list);
            mConversationListFragment.refresh();
        }

        @Override
        public void onCmdMessageReceived(List<EMMessage> list) {

        }

        @Override
        public void onMessageReadAckReceived(List<EMMessage> list) {

        }

        @Override
        public void onMessageDeliveryAckReceived(List<EMMessage> list) {

        }

        @Override
        public void onMessageChanged(EMMessage emMessage, Object o) {

        }
    };
}