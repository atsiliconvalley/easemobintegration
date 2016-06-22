package com.atguigu.imapp.model;

/**
 * Created by youni on 16/6/22.
 */
public class IMUser {
    private String appUser;
    private String hxId;
    private String nick;
    private String avartar;

    public IMUser(String appUser){
        this.appUser = appUser;
        hxId = appUser;
        nick = appUser;
    }

    public IMUser() {
    }

    public String getHxId() {
        return hxId;
    }

    public void setHxId(String hxId) {
        this.hxId = hxId;
    }

    public String getAppUser() {
        return appUser;
    }

    public void setAppUser(String appUser) {
        this.appUser = appUser;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getAvartar() {
        return avartar;
    }

    public void setAvartar(String avartar) {
        this.avartar = avartar;
    }

}