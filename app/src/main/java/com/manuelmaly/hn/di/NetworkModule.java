package com.manuelmaly.hn.di;

import com.manuelmaly.hn.server.AndroidNetworkStatus;
import com.manuelmaly.hn.server.ApiCommandFactory;
import com.manuelmaly.hn.server.ApiCommandFactoryImpl;
import com.manuelmaly.hn.server.DefaultHttpClientProvider;
import com.manuelmaly.hn.server.HNCredentialsRepositoryImpl;
import com.manuelmaly.hn.server.ICredentialsRepository;
import com.manuelmaly.hn.server.IHttpClientProvider;
import com.manuelmaly.hn.server.INetworkStatus;

import dagger.Binds;
import dagger.Module;

/** Binds the network / credentials interfaces to their implementations. */
@Module
public abstract class NetworkModule {

    @Binds
    abstract IHttpClientProvider bindHttpClientProvider(DefaultHttpClientProvider impl);

    @Binds
    abstract INetworkStatus bindNetworkStatus(AndroidNetworkStatus impl);

    @Binds
    abstract ApiCommandFactory bindApiCommandFactory(ApiCommandFactoryImpl impl);

    @Binds
    abstract ICredentialsRepository bindCredentialsRepository(HNCredentialsRepositoryImpl impl);

}
