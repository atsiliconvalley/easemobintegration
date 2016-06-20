package com.example.youni.testapp.model;

/**
 * Created by youni on 2016/5/25.
 */
public class DemoUser {
    /**

     * nick 名称
     */
    private String userName;

    /**
     * 对应的环信ID
     */
    private String hxId;

    /**
     * 个人头像
     */
    private String avatarPhoto;

    /**
     * 其他个人信息
     */
    private int age;

    /**
     * education
     */
    private String education;

    public DemoUser(String hxId){
        this.hxId = hxId;
    }

    public DemoUser(){
    }

    public String getUserName() {
        if(userName == null){
            return hxId;
        }

        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getHxId() {
        return hxId;
    }

    public void setHxId(String hxId) {
        this.hxId = hxId;
    }

    public String getAvatarPhoto() {
        return avatarPhoto;
    }

    public void setAvatarPhoto(String avatarPhoto) {
        this.avatarPhoto = avatarPhoto;
    }
}
