package com.atguigu.imapp.model;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.atguigu.imapp.event.GlobalEventNotifer;
import com.atguigu.imapp.model.db.PreferenceUtils;
import com.atguigu.imapp.controller.activity.MainActivity;
import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMOptions;
import com.hyphenate.easeui.controller.EaseUI;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.model.EaseNotifier;
import com.hyphenate.exceptions.HyphenateException;
import com.atguigu.imapp.model.db.DBManager;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by youni on 2016/5/19.
 *
 * Model - 代表等着整个APP的数据存取模型，所有的其他的类Controller和View实体类都必须且只能通过Model类获取数据模型
 *
 */
public class Model {
    private final static String TAG = "Demo Model";
    private boolean isInited = false;
    private Context mAppContext;
    private static Model me = new Model();
    private Map<String,DemoUser> mContacts = new HashMap<>();
    private String mCurrentUser;
    private DBManager mDBManager;
    private PreferenceUtils mPreference;
    private boolean mIsContactSynced = false;
    private EventListener eventListener;

    // used to show the toast
    private Handler mH = new Handler();


    static public Model getInstance(){
        return me;
    }

    public boolean init(Context appContext){
        if(isInited){
            return false;
        }

        mAppContext = appContext;

        EMOptions options = new EMOptions();
        options.setAutoAcceptGroupInvitation(false);
        options.setAcceptInvitationAlways(false);

        if(!EaseUI.getInstance().init(appContext, options)){
            return false;
        }

        // 1. 首先必需先初始化GlobalEventNotifer
        GlobalEventNotifer.getInstance().init(appContext);

        // 2. 创建Model Eevent listener
        eventListener = new EventListener(appContext);

        mPreference = new PreferenceUtils(mAppContext);
        mIsContactSynced = mPreference.isContactSynced();

        isInited = true;

        initListener();
        initProvider();

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
                    GlobalEventNotifer.getInstance().notifyContactSyncChanged(false);
                    e.printStackTrace();

                    return;
                }

                if(users != null){
                    for(String id:users){
                        DemoUser appUser = new DemoUser();
                        appUser.setHxId(id);
                        appUser.setNick(null);
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

                GlobalEventNotifer.getInstance().notifyContactSyncChanged(true);
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
            user.setNick(user.getHxId() + "_" + NICKS[index % NICKS.length]);
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

        // 记住应该还要去自己的APP服务器上去获取联系人信息
        fetchUserFromAppServer(user);

        // save to db;
        mDBManager.saveContact(user);
    }

    public void deleteContact(String hxId){
        if(mContacts.get(hxId) == null){
            return;
        }

        mContacts.remove(hxId);
        mDBManager.deleteContact(new DemoUser(hxId));
        mDBManager.removeInvitation(hxId);
    }

    public Map<String,DemoUser> getContacts(){
        return mContacts;
    }

    /**
     *
     * @return
     */
    public boolean isContactSynced(){
        return mIsContactSynced;
    }

    /**
     * try to fetch the user info from app server
     * and when fecting is done, update the cache and the db
     * @param user
     */
    private void fetchUserFromAppServer(DemoUser user) {
        user.setNick(user.getHxId() + "_凤凰");
    }

    public void onLoggedIn(String userName){
        if(mCurrentUser == userName){
            return;
        }

        Log.d(TAG,"logined user name : " + userName);

        mCurrentUser = userName;
        mDBManager = new DBManager(mAppContext,mCurrentUser);
        eventListener.setDbManager(mDBManager);
    }

    public List<InvitationInfo> getInvitationInfo(){
        return mDBManager.getInvitations();
    }

    public void removeInvitation(String user) {
        mDBManager.removeInvitation(user);
    }

    public void updateInvitation(InvitationInfo.InvitationStatus status,String hxId){
        mDBManager.updateInvitationStatus(status,hxId);
    }

    public void updateInviteNotif(boolean hasNotify){
        mDBManager.updateInvitateNoify(hasNotify);
    }

    public boolean hasInviteNotif(){
        return mDBManager.hasInviteNotif();
    }

    public void acceptGroupInvitation(InvitationInfo invitationInfo){
        invitationInfo.setStatus(InvitationInfo.InvitationStatus.GROUP_ACCEPT_INVITE);

        mDBManager.addInvitation(invitationInfo);
    }

    public void acceptGroupApplication(InvitationInfo invitationInfo){
        invitationInfo.setStatus(InvitationInfo.InvitationStatus.GROUPO_ACCEPT_APPLICATION);

        mDBManager.addInvitation(invitationInfo);
    }

    public void rejectGroupInvitation(InvitationInfo invitationInfo){
        invitationInfo.setStatus(InvitationInfo.InvitationStatus.GROUP_REJECT_INVITE);

        mDBManager.addInvitation(invitationInfo);
    }

    public void rejectGroupApplication(InvitationInfo invitationInfo){
        invitationInfo.setStatus(InvitationInfo.InvitationStatus.GROUP_REJECT_APPLICATION);

        mDBManager.addInvitation(invitationInfo);

    }

    //==============================================================
    // please put private api here
    //==============================================================
    private void initProvider(){
        EaseUI.getInstance().getNotifier().setNotificationInfoProvider(new EaseNotifier.EaseNotificationInfoProvider() {
            @Override
            public String getDisplayedText(EMMessage message) {
                String hxId = message.getFrom();

                DemoUser user = mContacts.get(hxId);

                if (user != null) {
                    return user.getNick() + "发来一条消息";
                }
                return null;
            }

            @Override
            public String getLatestText(EMMessage message, int fromUsersNum, int messageNum) {
                return null;
            }

            @Override
            public String getTitle(EMMessage message) {
                return null;
            }

            @Override
            public int getSmallIcon(EMMessage message) {
                return 0;
            }

            @Override
            public Intent getLaunchIntent(EMMessage message) {
                return new Intent(mAppContext, MainActivity.class);
            }
        });
    }

    private void initListener() {
        EaseUI.getInstance().setUserProfileProvider(new EaseUI.EaseUserProfileProvider() {
            @Override
            public EaseUser getUser(String username) {
                DemoUser user = mContacts.get(username);

                if (user != null) {
                    EaseUser easeUser = new EaseUser(username);

                    easeUser.setNick(user.getNick());

                    easeUser.setAvatar("http://www.atguigu.com/images/logo.gif");
                    return easeUser;
                }

                return null;
            }
        });
    }
}
