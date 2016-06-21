package com.example.youni.testapp.model;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.example.youni.testapp.model.db.PreferenceUtils;
import com.example.youni.testapp.ui.MainActivity;
import com.hyphenate.EMCallBack;
import com.hyphenate.EMContactListener;
import com.hyphenate.EMGroupChangeListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMOptions;
import com.hyphenate.chat.EMTextMessageBody;
import com.hyphenate.easeui.controller.EaseUI;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.model.EaseNotifier;
import com.hyphenate.exceptions.HyphenateException;
import com.example.youni.testapp.model.db.DBManager;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    private List<EMGroupChangeListener> groupChangeListeners;

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
        options.setAutoAcceptGroupInvitation(false);
        options.setAcceptInvitationAlways(false);

        if(!EaseUI.getInstance().init(appContext, options)){
            return false;
        }

        mContactSyncLiseners = new ArrayList<>();
        groupChangeListeners = new ArrayList<>();
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
                    notifyContactSyncChanged(false);
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

        // save to db;
        mDBManager.saveContact(user);
    }

    public void deleteContact(String hxId){
        mContacts.remove(hxId);
        mDBManager.deleteContact(new DemoUser(hxId));
        mDBManager.removeInvitation(hxId);
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

    private void initProvider(){
        EaseUI.getInstance().getNotifier().setNotificationInfoProvider(new EaseNotifier.EaseNotificationInfoProvider() {
            @Override
            public String getDisplayedText(EMMessage message) {
                String hxId = message.getFrom();

                DemoUser user = mContacts.get(hxId);

                if(user != null){
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
                return new Intent(mAppContext,MainActivity.class);
            }
        });
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
                    fetchUserFromAppServer(user);

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

                if(user == null){
                    return;
                }

                deleteContact(s);

                mH.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mAppContext,"the user is removed : " + user.getNick(),Toast.LENGTH_LONG).show();
                    }
                });

                for(EMContactListener listener:mContactListeners){
                    listener.onContactDeleted(s);
                }
            }

            @Override
            public void onContactInvited(String hxId, String reason) {
                Log.d(TAG, "onContactInvited : " + hxId);

                updateInviteNotif(true);

                DemoUser user = new DemoUser(hxId);

                // 从app服务器获取昵称
                // 我在这里就设置为个临时的
                fetchUserFromAppServer(user);
                InvitationInfo inviteInfo = new InvitationInfo();
                inviteInfo.setUser(user);
                inviteInfo.setReason("加个好友吧");
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
                user.setNick(s);

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

                if (user != null) {
                    EaseUser easeUser = new EaseUser(username);

                    easeUser.setNick(user.getNick());

                    easeUser.setAvatar("http://www.atguigu.com/images/logo.gif");
                    return easeUser;
                }

                return null;
            }
        });

        EMClient.getInstance().groupManager().addGroupChangeListener(groupChangeListener);

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

    public void addGroupChangeListener(EMGroupChangeListener groupChangeListener){
        if(groupChangeListeners.contains(groupChangeListener)){
            return;
        }

        groupChangeListeners.add(groupChangeListener);
    }

    public void removeGroupChangeListener(EMGroupChangeListener groupChangeListener){
        if(groupChangeListener == null){
            return;
        }

        groupChangeListeners.remove(groupChangeListener);
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

        mCurrentUser = userName;
        mDBManager = new DBManager(mAppContext,mCurrentUser);
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

//    void 	onInvitationReceived (String groupId, String groupName, String inviter, String reason)
//
//    void 	onApplicationReceived (String groupId, String groupName, String applicant, String reason)
//
//    void 	onApplicationAccept (String groupId, String groupName, String accepter)
//
//    void 	onApplicationDeclined (String groupId, String groupName, String decliner, String reason)
//
//    void 	onInvitationAccpted (String groupId, String invitee, String reason)
//
//    void 	onInvitationDeclined (String groupId, String invitee, String reason)
//
//    void 	onUserRemoved (String groupId, String groupName)
//
//    void 	onGroupDestroy (String groupId, String groupName)
//
//    void 	onAutoAcceptInvitationFromGroup (String groupId, String inviter, String inviteMessage)

    private EMGroupChangeListener groupChangeListener = new EMGroupChangeListener() {
        @Override
        public void onInvitationReceived(String s, String s1, String s2, String s3) {
            final IMInvitationGroupInfo groupInfo = new IMInvitationGroupInfo();
            groupInfo.setGroupId(s);
            groupInfo.setGroupName(s1);
            groupInfo.setInviteTriggerUser(s2);

            InvitationInfo invitationInfo = new InvitationInfo();

            invitationInfo.setReason(s3);
            invitationInfo.setStatus(InvitationInfo.InvitationStatus.NEW_GROUP_INVITE);
            invitationInfo.setGroupInfo(groupInfo);

            mDBManager.addInvitation(invitationInfo);
            mH.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mAppContext,"收到邀请 : " + groupInfo,Toast.LENGTH_SHORT).show();
                }
            });

            for (EMGroupChangeListener listener:groupChangeListeners){
                listener.onInvitationReceived(s,s1,s2,s3);
            }

        }

        @Override
        public void onApplicationReceived(String s, String s1, String s2, String s3) {
            final IMInvitationGroupInfo groupInfo = new IMInvitationGroupInfo();
            groupInfo.setGroupId(s);
            groupInfo.setGroupName(s1);
            groupInfo.setInviteTriggerUser(s2);

            InvitationInfo invitationInfo = new InvitationInfo();

            invitationInfo.setReason(s3);
            invitationInfo.setStatus(InvitationInfo.InvitationStatus.NEW_GROUP_APPLICATION);
            invitationInfo.setGroupInfo(groupInfo);

            mDBManager.addInvitation(invitationInfo);

            mH.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mAppContext, "收到申请 : " + groupInfo, Toast.LENGTH_SHORT).show();
                }
            });

            for (EMGroupChangeListener listener:groupChangeListeners){
                listener.onApplicationReceived(s,s1,s2,s3);
            }
        }

        @Override
        public void onApplicationAccept(String s, String s1, String s2) {
            final IMInvitationGroupInfo groupInfo = new IMInvitationGroupInfo();
            groupInfo.setGroupId(s);
            groupInfo.setGroupName(s1);
            groupInfo.setInviteTriggerUser(s2);

            InvitationInfo invitationInfo = new InvitationInfo();

            invitationInfo.setGroupInfo(groupInfo);
            invitationInfo.setStatus(InvitationInfo.InvitationStatus.GROUP_APPLICATION_ACCEPTED);

            mDBManager.addInvitation(invitationInfo);

            mH.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mAppContext, "申请被接受 : " + groupInfo, Toast.LENGTH_SHORT).show();
                }
            });

            String stringInviteAccept = " 接收了你的邀请";

            // 加群申请被同意
            EMMessage msg = EMMessage.createReceiveMessage(EMMessage.Type.TXT);
            msg.setChatType(EMMessage.ChatType.GroupChat);
            msg.setFrom(groupInfo.getInviteTriggerUser());
            msg.setTo(groupInfo.getGroupId());
            msg.setMsgId(UUID.randomUUID().toString());
            msg.addBody(new EMTextMessageBody(groupInfo.getInviteTriggerUser() + " " + stringInviteAccept));
            msg.setStatus(EMMessage.Status.SUCCESS);

            // 保存同意消息
            EMClient.getInstance().chatManager().saveMessage(msg);

            for (EMGroupChangeListener listener:groupChangeListeners){
                listener.onApplicationAccept(s,s1,s2);
            }
        }

        @Override
        public void onApplicationDeclined(String s, String s1, String s2, String s3) {
            final IMInvitationGroupInfo groupInfo = new IMInvitationGroupInfo();
            groupInfo.setGroupId(s);
            groupInfo.setGroupName(s1);
            groupInfo.setInviteTriggerUser(s2);

            InvitationInfo invitationInfo = new InvitationInfo();

            invitationInfo.setReason(s3);
            invitationInfo.setStatus(InvitationInfo.InvitationStatus.GROUP_APPLICATION_DECLINED);
            invitationInfo.setGroupInfo(groupInfo);

            mDBManager.addInvitation(invitationInfo);

            mH.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mAppContext, "申请被拒绝 : " + groupInfo, Toast.LENGTH_SHORT).show();
                }
            });

            for(EMGroupChangeListener listener:groupChangeListeners){
                listener.onApplicationDeclined(s,s1,s2,s3);
            }
        }

        @Override
        public void onInvitationAccpted(String s, String s1, String s2) {
            final IMInvitationGroupInfo groupInfo = new IMInvitationGroupInfo();
            groupInfo.setGroupId(s);
            groupInfo.setGroupName(s);
            groupInfo.setInviteTriggerUser(s1);

            InvitationInfo invitationInfo = new InvitationInfo();

            invitationInfo.setGroupInfo(groupInfo);
            invitationInfo.setStatus(InvitationInfo.InvitationStatus.GROUP_INVITE_ACCEPTED);

            mDBManager.addInvitation(invitationInfo);

            mH.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mAppContext, "邀请被接收 : " + groupInfo, Toast.LENGTH_SHORT).show();
                }
            });

            for (EMGroupChangeListener listener:groupChangeListeners){
                listener.onInvitationAccpted(s,s1,s2);
            }
        }

        @Override
        public void onInvitationDeclined(String s, String s1, String s2) {
            final IMInvitationGroupInfo groupInfo = new IMInvitationGroupInfo();
            groupInfo.setGroupId(s);
            groupInfo.setGroupName(s);
            groupInfo.setInviteTriggerUser(s1);

            InvitationInfo invitationInfo = new InvitationInfo();

            invitationInfo.setGroupInfo(groupInfo);
            invitationInfo.setStatus(InvitationInfo.InvitationStatus.GROUP_INVITE_DECLINED);

            mDBManager.addInvitation(invitationInfo);

            mH.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mAppContext, "邀请被拒绝 : " + groupInfo, Toast.LENGTH_SHORT).show();
                }
            });

            for (EMGroupChangeListener listener:groupChangeListeners){
                listener.onInvitationDeclined(s,s1,s2);
            }
        }

        @Override
        public void onUserRemoved(String s, String s1) {

        }

        @Override
        public void onGroupDestroy(String s, String s1) {

        }

        @Override
        public void onAutoAcceptInvitationFromGroup(String s, String s1, String s2) {
            final IMInvitationGroupInfo groupInfo = new IMInvitationGroupInfo();
            groupInfo.setGroupId(s);
            groupInfo.setGroupName(s);
            groupInfo.setInviteTriggerUser(s1);

            InvitationInfo invitationInfo = new InvitationInfo();

            invitationInfo.setGroupInfo(groupInfo);
            invitationInfo.setStatus(InvitationInfo.InvitationStatus.GROUP_INVITE_ACCEPTED);
            mDBManager.addInvitation(invitationInfo);

            mH.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mAppContext, "邀请被接受 : " + groupInfo, Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

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
}
