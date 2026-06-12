package com.manuelmaly.hn;

import android.app.Application;

import com.manuelmaly.hn.di.AppComponent;
import com.manuelmaly.hn.di.AppModule;
import com.manuelmaly.hn.di.DaggerAppComponent;

import org.androidannotations.annotations.EApplication;

@EApplication
public class App extends Application {

    private static App mInstance;
    private AppComponent mComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        mComponent = DaggerAppComponent.builder().appModule(new AppModule(this)).build();
    }

    public static App getInstance() {
        return mInstance;
    }

    public AppComponent getComponent() {
        return mComponent;
    }

    /**
     * Convenience accessor for the static facades ({@code Settings}, {@code FileUtil},
     * {@code HNCredentials}) and the task entry points. Only valid after
     * {@link #onCreate()} has run.
     */
    public static AppComponent component() {
        return mInstance.getComponent();
    }

}
