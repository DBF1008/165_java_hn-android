package com.manuelmaly.hn.server;

import android.app.Application;

import javax.inject.Inject;

/**
 * Default {@link INetworkStatus}, backed by {@link ConnectivityUtils} and the
 * application context.
 */
public class AndroidNetworkStatus implements INetworkStatus {

    private final Application mApp;

    @Inject
    public AndroidNetworkStatus(Application app) {
        mApp = app;
    }

    @Override
    public boolean isOnline() {
        return ConnectivityUtils.isDeviceOnline(mApp);
    }

}
