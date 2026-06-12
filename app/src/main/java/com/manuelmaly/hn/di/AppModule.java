package com.manuelmaly.hn.di;

import android.app.Application;
import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Provides the application-level context. Concrete {@code @Provides} module (the
 * others use {@code @Binds}).
 */
@Module
public class AppModule {

    private final Application mApp;

    public AppModule(Application app) {
        mApp = app;
    }

    @Provides
    @Singleton
    Application provideApplication() {
        return mApp;
    }

    @Provides
    Context provideContext() {
        return mApp;
    }

}
