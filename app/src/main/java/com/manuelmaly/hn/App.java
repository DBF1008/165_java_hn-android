package com.manuelmaly.hn;

import android.app.Application;

import org.androidannotations.annotations.EApplication;

import com.manuelmaly.hn.util.ThemeHelper;

@EApplication
public class App extends Application {

    private static App mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        // Apply the saved dark-mode preference before any activity is created.
        ThemeHelper.apply(this);
    }

    public static App getInstance() {
        return mInstance;
    }

}
