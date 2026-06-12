package com.manuelmaly.hn.viewmodel;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.manuelmaly.hn.concurrent.AppExecutors;
import com.manuelmaly.hn.concurrent.DefaultAppExecutors;
import com.manuelmaly.hn.data.CommentsRepository;
import com.manuelmaly.hn.data.FeedRepository;
import com.manuelmaly.hn.data.FileLocalCache;
import com.manuelmaly.hn.data.HNRemoteDataSource;
import com.manuelmaly.hn.data.IHNRemoteDataSource;
import com.manuelmaly.hn.data.ILocalCache;
import com.manuelmaly.hn.data.IReadStateStore;
import com.manuelmaly.hn.data.SharedPrefsReadStateStore;

/**
 * Builds the production dependency graph for the screen ViewModels. AndroidAnnotations
 * keeps the Activity constructor no-arg, so wiring happens here instead of via
 * constructor injection. Created from {@code @AfterViews} with the application context.
 */
public class HNViewModelFactory implements ViewModelProvider.Factory {

    private final Context mAppContext;

    public HNViewModelFactory(Context context) {
        mAppContext = context.getApplicationContext();
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        AppExecutors executors = new DefaultAppExecutors();
        IHNRemoteDataSource remote = new HNRemoteDataSource();
        ILocalCache cache = new FileLocalCache();

        if (modelClass.isAssignableFrom(MainViewModel.class)) {
            IReadStateStore readStateStore = new SharedPrefsReadStateStore(mAppContext);
            FeedRepository repository = new FeedRepository(remote, cache, executors);
            return (T) new MainViewModel(repository, readStateStore, executors);
        }

        if (modelClass.isAssignableFrom(CommentsViewModel.class)) {
            CommentsRepository repository = new CommentsRepository(remote, cache, executors);
            return (T) new CommentsViewModel(repository, executors);
        }

        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
