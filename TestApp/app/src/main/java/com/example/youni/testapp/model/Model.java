package com.example.youni.testapp.model;

import android.content.ContentValues;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.example.youni.testapp.model.db.PreferenceUtils;
import com.hyphenate.EMCallBack;
import com.hyphenate.EMContactListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMOptions;
import com.hyphenate.easeui.controller.EaseUI;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.exceptions.HyphenateException;
import com.example.youni.testapp.model.db.DBManager;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by youni on 2016/5/19.
 */
public class Model {
    private final static String TAG = "Demo Model";
    private boolean isInited = false;
    private Context mAppContext;
    private static Model me = new Model();
    private Map<String,DemoUser> mContacts = new HashMap<>();
    private List<OnSyncListener> mContactSyncLiseners;
    private String mCurrentUser;
    private DBManager mDBManager;
    private PreferenceUtils mPreference;
    private boolean mIsContactSynced = false;
    private List<EMContactListener> mContactListeners;

    // used to show the toast
    private Handler mH = new Handler();

    public static interface OnSyncListener{
        public void onSuccess();
        public void onFailed();
    }

    static public Model getInstance(){
        return me;
    }

    public boolean init(Context appContext){
        if(isInited){
            return false;
        }

        mAppContext = appContext;

        EMOptions options = new EMOptions();
        options.setAcceptInvitationAlways(false);

        if(!EaseUI.getInstance().init(appContext, options)){
            return false;
        }

        mContactSyncLiseners = new ArrayList<>();
        mPreference = new PreferenceUtils(mAppContext);
        mIsContactSynced = mPreference.isContactSynced();

        isInited = true;

        initListener();

        return isInited;
    }

    public void logout(final EMCallBack callBack){
        EMClient.getInstance().logout(false, new EMCallBack() {
            @Override
            public void onSuccess() {
                mPreference.setContactSynced(false);
                mIsContactSynced = false;
                mContacts.clear();
                callBack.onSuccess();
            }

            @Override
            public void onError(int i, String s) {
                callBack.onError(i, s);
            }

            @Override
            public void onProgress(int i, String s) {
                callBack.onProgress(i, s);
            }
        });
    }
    /**
     * 从远程服务器获取联系人信息
     * 1. 从环信服务器上获取
     * 2. 同时从app服务器上获取
     * 3. 等到这两个都返回时做两者的同步
     */
    public void asyncfetchUsers() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mIsContactSynced = false;

                List<String> users = null;
                try {
                    users = EMClient.getInstance().contactManager().getAllContactsFromServer();
                    mIsContactSynced = true;
                    mPreference.setContactSynced(true);
                } catch (HyphenateException e) {
                    notifyContactSyncChanged(false);
                    e.printStackTrace();

                    return;
                }

                if(users != null){
                    for(String id:users){
                        DemoUser appUser = new DemoUser();
                        appUser.setHxId(id);
                        appUser.setUserName(null);
                        mContacts.put(id, appUser);
                    }
                }
                // fetch users from app server

                List<DemoUser> appUsers = fetchUsersFromAppServer();

                // 同步联系人
                // 以环信的联系人为主，如果环信的联系人里没有app里的联系，就把app里的联系人删除
                // 如果app里的联系人没有环信的联系人，则加入到app里

                // 最后要更新本地数据库

                mDBManager.saveContacts(mContacts.values());

                notifyContactSyncChanged(true);
            }
        }).start();
    }

    private static String[] NICKS = new String[]{"老虎","熊猫","猴子","猎豹","灰熊","企鹅"};

    private List<DemoUser> fetchUsersFromAppServer() {
       // 实际上是应该从APP服务器上获取联系人的信息

        // 不过由于缺乏我们的demo的服务器，暂时hick下，用下假数据
        //

        int index = 0;
        for(DemoUser user:mContacts.values()){
            user.setUserName(user.getHxId() + "_" + NICKS[index%NICKS.length]);
            index++;
        }

        return null;
    }


    /**
     * 先加载本地的联系人
     */
    public void loadLocalContacts(){
        Log.d("Model", "load local contacts");
        List<DemoUser> users = mDBManager.getContacts();

        if(users != null){
            mContacts.clear();

            for(DemoUser user:users){
                mContacts.put(user.getHxId(),user);
            }
        }
    }

    public void addUser(DemoUser user){
        if(mContacts.containsKey(user.getHxId())){
            return;
        }

        mContacts.put(user.getHxId(), user);

        // save to db;
        mDBManager.saveContact(user);
    }

    public void addOnContactSyncListener(OnSyncListener listener){
        if(listener == null){
            return;
        }

        if(mContactSyncLiseners.contains(listener)){
            return;
        }

        mContactSyncLiseners.add(listener);
    }

    private void notifyContactSyncChanged(boolean success){
        for(OnSyncListener listener:mContactSyncLiseners){
            if(success){
                listener.onSuccess();
            }else{
                listener.onFailed();
            }
        }
    }

    public Map<String,DemoUser> getContacts(){
        return mContacts;
    }

    public boolean isContactSynced(){
        return mIsContactSynced;
    }

    private void initListener() {
        mContactListeners = new ArrayList<>();

        EMClient.getInstance().contactManager().setContactListener(new EMContactListener() {
            @Override
            public void onContactAdded(String s) {
                Log.d(TAG,"onContactAdded : " + s);
                if(!mContacts.containsKey(s)){
                    DemoUser user = new DemoUser();

                    user.setHxId(s);

                    mContacts.put(s, user);

                    // 记住应该还要去自己的APP服务器上去获取联系人信息

                    fetchUserFromAppServer(user.getHxId());

                    mDBManager.saveContact(user);

                    for(EMContactListener listener:mContactListeners){
                        listener.onContactAdded(s);
                    }
                }
            }

            @Override
            public void onContactDeleted(String s) {
                Log.d(TAG,"onContactDeleted : " + s);

                final DemoUser user = mContacts.get(s);

                if(user != null){
                    mContacts.remove(s);
                    mDBManager.deleteContact(user);
                }

                mH.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mAppContext,"the user is removed : " + user.getUserName(),Toast.LENGTH_LONG).show();
                    }
                });

                for(EMContactListener listener:mContactListeners){
                    listener.onContactDeleted(s);
                }
            }

            @Override
            public void onContactInvited(String hxId, String reason) {
                Log.d(TAG, "onContactInvited : " + hxId);

                InvitationInfo inviteInfo = new InvitationInfo();
                inviteInfo.setUser(new DemoUser(hxId));
                inviteInfo.setReason(reason);
                inviteInfo.setStatus(InvitationInfo.InvitationStatus.NEW_INVITE);

                mDBManager.addInvitation(inviteInfo);

                for(EMContactListener listener:mContactListeners){
                    listener.onContactInvited(hxId, reason);
                }
            }

            @Override
            public void onContactAgreed(String s) {
                Log.d(TAG, "onContactInvited : " + s);

                InvitationInfo inviteInfo = new InvitationInfo();
                inviteInfo.setReason("你的邀请已经被接受");
                inviteInfo.setStatus(InvitationInfo.InvitationStatus.INVITE_ACCEPT_BY_PEER);

                DemoUser user = new DemoUser(s);
                user.setUserName(s);

                inviteInfo.setUser(user);

                mDBManager.addInvitation(inviteInfo);

                for(EMContactListener listener:mContactListeners){
                    listener.onContactAgreed(s);
                }
            }

            @Override
            public void onContactRefused(String s) {
                Log.d(TAG,"onContactRefused : " + s);

                for(EMContactListener listener:mContactListeners){
                    listener.onContactRefused(s);
                }
            }
        });

        EaseUI.getInstance().setUserProfileProvider(new EaseUI.EaseUserProfileProvider() {
            @Override
            public EaseUser getUser(String username) {
                DemoUser user = mContacts.get(username);

                if(user != null){
                    EaseUser easeUser = new EaseUser(username);

                    easeUser.setNick(user.getUserName());

                    easeUser.setAvatar("http://www.atguigu.com/images/logo.gif");
                    return easeUser;
                }

                return null;
            }
        });
    }

    public void addContactListeners(EMContactListener listener){
        if(mContactListeners.contains(listener)){
            return;
        }

        mContactListeners.add(listener);
    }

    public void removeContactListener(EMContactListener listener){
        mContactListeners.remove(listener);
    }

    /**
     * try to fetch the user info from app server
     * and when fecting is done, update the cache and the db
     * @param hxId
     */
    private void fetchUserFromAppServer(String hxId) {

    }

    public void onLoggedIn(String userName){
        if(mCurrentUser == userName){
            return;
        }

        mCurrentUser = userName;
        mDBManager = new DBManager(mAppContext,mCurrentUser);
    }

    public List<InvitationInfo> getInvitationInfo(){
        return mDBManager.getContactInvitations();
    }

    public void removeInvitation(String user) {
        mDBManager.removeInvitation(user);
    }

    public void updateInvitation(InvitationInfo.InvitationStatus status,String hxId){
        mDBManager.updateInvitationStatus(status,hxId);
    }
}
