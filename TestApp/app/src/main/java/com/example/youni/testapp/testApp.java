package com.example.youni.testapp;

import android.app.Application;

import com.example.youni.testapp.model.Model;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMOptions;
import com.hyphenate.easeui.controller.EaseUI;

/**
 * Created by youni on 2016/5/18.
 */
public class testApp extends Application {
    @Override
    public void onCreate(){
        super.onCreate();

        Model.getInstance().init(this);
    }
}
