package com.atguigu.imapp.controller.fragment;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.atguigu.imapp.R;
import com.atguigu.imapp.common.Constant;
import com.atguigu.imapp.event.GlobalEventNotifer;
import com.atguigu.imapp.event.OnSyncListener;
import com.atguigu.imapp.model.DemoUser;
import com.atguigu.imapp.model.Model;
import com.atguigu.imapp.controller.activity.AddFriendActivity;
import com.atguigu.imapp.controller.activity.ChatActivity;
import com.atguigu.imapp.controller.activity.InvitationActivity;
import com.atguigu.imapp.controller.activity.GroupListActivity;
import com.hyphenate.EMContactListener;
import com.hyphenate.EMGroupChangeListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.easeui.EaseConstant;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.ui.EaseContactListFragment;
import com.hyphenate.exceptions.HyphenateException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by youni on 2016/5/19.
 */
public class ContactListFragment extends EaseContactListFragment {

    private OnSyncListener mContactSyncListener;
    ImageView notifImageView;
    private String hxId;
    private LinearLayout groupsItem;
    private LinearLayout invitationItem;

    @Override
    public void setUpView(){
        super.setUpView();
        View headerView = LayoutInflater.from(getContext()).inflate(R.layout.activity_contact_header,null);
        notifImageView = (ImageView) headerView.findViewById(R.id.iv_invitation_notif);


        invitationItem = (LinearLayout) headerView.findViewById(R.id.ll_contact_invitation);
        invitationItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notifImageView.setVisibility(View.INVISIBLE);
                Model.getInstance().updateInviteNotif(false);
                getActivity().startActivity(new Intent(getActivity(), InvitationActivity.class));
            }
        });

        groupsItem = (LinearLayout) headerView.findViewById(R.id.ll_group_item);
        groupsItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().startActivity(new Intent(getActivity(), GroupListActivity.class));
            }
        });

        notifImageView.setVisibility(Model.getInstance().hasInviteNotif() ? View.VISIBLE : View.INVISIBLE);

        listView.addHeaderView(headerView);
        registerForContextMenu(listView);

        titleBar.setRightImageResource(R.drawable.em_add);

        titleBar.setRightLayoutClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().startActivity(new Intent(getActivity(), AddFriendActivity.class));
            }
        });

        mContactSyncListener = new OnSyncListener() {
            @Override
            public void onSuccess() {
                setupContacts();
            }

            @Override
            public void onFailed() {

            }
        };

        GlobalEventNotifer.getInstance().addOnContactSyncListener(mContactSyncListener);

        if(Model.getInstance().isContactSynced()){
            Log.d("ContactListFragment", "already synced");
            setupContacts();
        }

        // 注册本地通知事件
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(invitaionChangedReceiver,new IntentFilter(Constant.CONTACT_INVITATION_CHANGED));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(invitaionChangedReceiver,new IntentFilter(Constant.GROUP_INVITATION_MESSAGE_CHANGED));
    }

    public void setupContacts(){
        Map<String,EaseUser> easeUsers = new HashMap<>();

        Map<String,DemoUser> appUsers = Model.getInstance().getContacts();

        if(appUsers != null){
            for(DemoUser user:appUsers.values()){
                EaseUser easeUser = new EaseUser(user.getHxId());
                easeUser.setNick(user.getNick());

                easeUsers.put(user.getHxId(),easeUser );
            }
            setContactsMap(easeUsers);
            refresh();
        }
    }

    @Override
    public void initView(){
        super.initView();

        setContactListItemClickListener(new EaseContactListItemClickListener() {
            @Override
            public void onListItemClicked(EaseUser user) {
                Log.d("ContactListFragment", "onListItemClicked");

                getActivity().startActivity(new Intent(getActivity(), ChatActivity.class).putExtra(EaseConstant.EXTRA_USER_ID, user.getUsername()));
            }
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        EaseUser user = (EaseUser)listView.getItemAtPosition(((AdapterView.AdapterContextMenuInfo) menuInfo).position);
        hxId = user.getUsername();

        getActivity().getMenuInflater().inflate(R.menu.menu_contact_list,menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.contact_delete){
            deleteContact(hxId);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    private void deleteContact(final String hxId){
        final ProgressDialog pd = new ProgressDialog(getActivity());
        pd.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    EMClient.getInstance().contactManager().deleteContact(hxId);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Model.getInstance().deleteContact(hxId);
                            setupContacts();
                            pd.cancel();
                        }
                    });
                } catch (HyphenateException e) {
                    e.printStackTrace();
                    final String error = e.toString();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pd.cancel();
                            Toast.makeText(getActivity(),error,Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(invitaionChangedReceiver);
    }

    private BroadcastReceiver invitaionChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            notifImageView.setVisibility(View.VISIBLE);
        }
    };
}
