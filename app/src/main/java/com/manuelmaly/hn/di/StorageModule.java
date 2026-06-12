package com.manuelmaly.hn.di;

import com.manuelmaly.hn.storage.FileCommentsStore;
import com.manuelmaly.hn.storage.FileFeedStore;
import com.manuelmaly.hn.storage.ICommentsStore;
import com.manuelmaly.hn.storage.IFeedStore;
import com.manuelmaly.hn.storage.ISettingsRepository;
import com.manuelmaly.hn.storage.SettingsRepositoryImpl;
import com.manuelmaly.hn.task.ITaskResultPublisher;
import com.manuelmaly.hn.task.LocalBroadcastTaskResultPublisher;
import com.manuelmaly.hn.util.IBackgroundExecutor;
import com.manuelmaly.hn.util.RunBackgroundExecutor;

import dagger.Binds;
import dagger.Module;

/** Binds the storage / threading / result-publishing interfaces. */
@Module
public abstract class StorageModule {

    @Binds
    abstract ISettingsRepository bindSettingsRepository(SettingsRepositoryImpl impl);

    @Binds
    abstract IFeedStore bindFeedStore(FileFeedStore impl);

    @Binds
    abstract ICommentsStore bindCommentsStore(FileCommentsStore impl);

    @Binds
    abstract IBackgroundExecutor bindBackgroundExecutor(RunBackgroundExecutor impl);

    @Binds
    abstract ITaskResultPublisher bindTaskResultPublisher(LocalBroadcastTaskResultPublisher impl);

}
