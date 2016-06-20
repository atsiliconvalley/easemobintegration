package com.example.youni.testapp.model;

/**
 * Created by youni on 2016/5/25.
 */
public class InvitationInfo {
    private DemoUser user;
    private IMInvitationGroupInfo groupInfo;

    private String reason;

    private InvitationStatus status;

    public InvitationInfo(){
    }

    public DemoUser getUser() {
        return user;
    }

    public void setUser(DemoUser user) {
        this.user = user;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
    public InvitationInfo(String reason, DemoUser user){
        this.user = user;
        this.reason = reason;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public void setStatus(InvitationStatus status) {
        this.status = status;
    }

    public void setGroupInfo(IMInvitationGroupInfo groupInfo){
        this.groupInfo = groupInfo;
    }

    public IMInvitationGroupInfo getGroupInfo(){
        return groupInfo;
    }

    public enum InvitationStatus{
        // contact invite status
        NEW_INVITE,
        INVITE_ACCEPT,
        INVITE_ACCEPT_BY_PEER,

        //group invite status
        NEW_GROUP_INVITE,
        NEW_GROUP_APPLICATION,

        GROUP_INVITE_ACCEPTED,
        GROUP_APPLICATION_ACCEPTED,

        GROUP_ACCEPT_INVITE,
        GROUPO_ACCEPT_APPLICATION,

        GROUP_INVITE_DECLINED,
        GROUP_APPLICATION_DECLINED
    }
}
