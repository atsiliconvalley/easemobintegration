package com.atguigu.imapp.model;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.atguigu.imapp.event.GlobalEventNotifer;
import com.atguigu.imapp.model.db.PreferenceUtils;
import com.atguigu.imapp.controller.activity.MainActivity;
import com.atguigu.imapp.model.db.UserAccountDB;
import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMOptions;
import com.hyphenate.easeui.controller.EaseUI;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.model.EaseNotifier;
import com.hyphenate.exceptions.HyphenateException;
import com.atguigu.imapp.model.db.DBManager;


import java.util.ArrayList;
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
    private final static String TAG = "IM Model";
    private boolean isInited = false;
    private Context mAppContext;
    private static Model me = new Model();
    private Map<String,IMUser> mContacts = new HashMap<>();
    private String mCurrentUser;
    private DBManager mDBManager;
    private UserAccountDB userAccountDB;
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


        userAccountDB = new UserAccountDB(appContext);

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

                List<String> hxUsers = null;
                try {
                    hxUsers = EMClient.getInstance().contactManager().getAllContactsFromServer();
                    mIsContactSynced = true;
                    mPreference.setContactSynced(true);
                } catch (HyphenateException e) {
                    GlobalEventNotifer.getInstance().notifyContactSyncChanged(false);
                    e.printStackTrace();

                    return;
                }

                // fetch users from app server

                List<IMUser> appUsers = fetchUsersFromAppServer();

                // 同步联系人
                // 以环信的联系人为主，如果环信的联系人里没有app里的联系，就把app里的联系人删除
                // 如果app里的联系人没有环信的联系人，则加入到app里

                // 最后要更新本地数据库

                appUsers = syncWithHxUsers(hxUsers,appUsers);

                for(IMUser user:appUsers){
                    mContacts.put(user.getAppUser(),user);
                }

                mDBManager.saveContacts(mContacts.values());

                GlobalEventNotifer.getInstance().notifyContactSyncChanged(true);
            }
        }).start();
    }

    private List<IMUser> syncWithHxUsers(List<String> hxUsers, List<IMUser> appUsers){
        List<IMUser> syncedUsers = new ArrayList<>();

        for(String hxId:hxUsers){
            IMUser appUser = new IMUser(hxId);

            syncedUsers.add(appUser);
        }

        return syncedUsers;
    }

    IMUser getUserByHx(String hxId){
        for(IMUser user:mContacts.values()){
            if(user.getHxId().equals(hxId)){
                return user;
            }
        }

        return null;
    }

    private static String[] NICKS = new String[]{"老虎","熊猫","猴子","猎豹","灰熊","企鹅"};

    private List<IMUser> fetchUsersFromAppServer() {
       // 实际上是应该从APP服务器上获取联系人的信息

        // 不过由于缺乏我们的demo的服务器，暂时hick下，用下假数据
        //

        int index = 0;
        for(IMUser user:mContacts.values()){
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
        List<IMUser> users = mDBManager.getContacts();

        if(users != null){
            mContacts.clear();

            for(IMUser user:users){
                mContacts.put(user.getHxId(),user);
            }
        }
    }

    public void addUser(IMUser user){
        if(mContacts.containsKey(user.getAppUser())){
            return;
        }

        mContacts.put(user.getAppUser(), user);

        // 记住应该还要去自己的APP服务器上去获取联系人信息
        fetchUserFromAppServer(user);

        // save to db;
        mDBManager.saveContact(user);
    }

    public void deleteContact(String appUser){
        if(mContacts.get(appUser) == null){
            return;
        }

        mContacts.remove(appUser);
        mDBManager.deleteContact(new IMUser(appUser));
        mDBManager.removeInvitation(appUser);
    }

    public Map<String,IMUser> getContacts(){
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
    private void fetchUserFromAppServer(IMUser user) {
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
        mDBManager.updateInvitationStatus(status, hxId);
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

                IMUser user = getUserByHx(hxId);

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
                IMUser user = getUserByHx(username);

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

    public void addAccount(IMUser account){
        userAccountDB.addAccount(account);
    }

    public IMUser getAccount(String appUser){
        return userAccountDB.getAccount(appUser);
    }

    public IMUser getAccountByHxId(String hxId){
        return userAccountDB.getAccountByHxId(hxId);
    }
}
