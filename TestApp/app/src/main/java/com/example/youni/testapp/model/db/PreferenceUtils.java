package com.example.youni.testapp.model.db;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by youni on 2016/5/21.
 */
public class PreferenceUtils {
    private static final String PREFERENCE_NAME = "atguigu";
    private static final String CONTACT_SYNCED = "contact_synced";
    private Context mContext;
    private SharedPreferences mPreference;

    public PreferenceUtils(Context context){
        mContext = context;
        // 讲解下shared preferences
        mPreference = mContext.getSharedPreferences(PREFERENCE_NAME,mContext.MODE_PRIVATE);
    }


    public void setContactSynced(boolean synced){
        mPreference.edit().putBoolean(CONTACT_SYNCED,synced).commit();
    }

    public boolean isContactSynced(){
        return mPreference.getBoolean(CONTACT_SYNCED,false);
    }
}
