package com.atguigu.imapp;

import android.app.Application;

import com.atguigu.imapp.model.Model;

/**
 * Created by youni on 2016/5/18.
 */
public class IMDemoApp extends Application {
    @Override
    public void onCreate(){
        super.onCreate();

        Model.getInstance().init(this);
    }
}
