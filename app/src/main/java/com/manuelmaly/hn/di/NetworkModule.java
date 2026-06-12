package com.manuelmaly.hn.di;

import com.manuelmaly.hn.data.network.HNApiClient;
import com.manuelmaly.hn.data.network.LegacyApiClient;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public abstract class NetworkModule {

    @Binds
    @Singleton
    abstract HNApiClient bindApiClient(LegacyApiClient impl);
}
