package com.manuelmaly.hn.di;

import com.manuelmaly.hn.data.storage.AppSettings;
import com.manuelmaly.hn.data.storage.FeedCache;
import com.manuelmaly.hn.data.storage.LegacyAppSettings;
import com.manuelmaly.hn.data.storage.LegacyFeedCache;
import com.manuelmaly.hn.data.storage.LegacyReadTracker;
import com.manuelmaly.hn.data.storage.ReadTracker;
import com.manuelmaly.hn.parser.CommentsParser;
import com.manuelmaly.hn.parser.FeedParser;
import com.manuelmaly.hn.parser.HNCommentsParserAdapter;
import com.manuelmaly.hn.parser.HNFeedParserAdapter;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public abstract class AppModule {

    @Binds
    @Singleton
    abstract AppSettings bindAppSettings(LegacyAppSettings impl);

    @Binds
    @Singleton
    abstract FeedCache bindFeedCache(LegacyFeedCache impl);

    @Binds
    @Singleton
    abstract ReadTracker bindReadTracker(LegacyReadTracker impl);

    @Binds
    @Singleton
    abstract FeedParser bindFeedParser(HNFeedParserAdapter impl);

    @Binds
    @Singleton
    abstract CommentsParser bindCommentsParser(HNCommentsParserAdapter impl);
}
