package com.manuelmaly.hn.di;

import com.manuelmaly.hn.server.ICredentialsRepository;
import com.manuelmaly.hn.storage.ICommentsStore;
import com.manuelmaly.hn.storage.IFeedStore;
import com.manuelmaly.hn.storage.ISettingsRepository;
import com.manuelmaly.hn.task.TaskFactory;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Application-scoped Dagger component. Built once in {@code App.onCreate()} and
 * reached via {@code App.component()}. Exposes the entry points the task facades
 * and static delegators need.
 */
@Singleton
@Component(modules = {AppModule.class, NetworkModule.class, ParserModule.class, StorageModule.class, TaskModule.class})
public interface AppComponent {

    TaskFactory taskFactory();

    ISettingsRepository settingsRepository();

    IFeedStore feedStore();

    ICommentsStore commentsStore();

    ICredentialsRepository credentialsRepository();

}
