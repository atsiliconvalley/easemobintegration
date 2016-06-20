package com.example.youni.testapp.model;

import com.google.android.gms.games.multiplayer.Invitation;

/**
 * Created by youni on 2016/5/25.
 */
public class InvitationInfo {
    private DemoUser user;
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

    public enum InvitationStatus{
        NEW_INVITE,
        INVITE_ACCEPT,
        INVITE_ACCEPT_BY_PEER
    }
}
