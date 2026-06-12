package com.manuelmaly.hn.di;

import com.manuelmaly.hn.parser.AndroidColorProvider;
import com.manuelmaly.hn.parser.HNCommentsParser;
import com.manuelmaly.hn.parser.HNFeedParser;
import com.manuelmaly.hn.parser.IColorProvider;
import com.manuelmaly.hn.parser.ICommentsParser;
import com.manuelmaly.hn.parser.IFeedParser;
import com.manuelmaly.hn.parser.IUserContext;
import com.manuelmaly.hn.storage.SettingsRepositoryImpl;

import dagger.Binds;
import dagger.Module;

/**
 * Binds the parser interfaces and their collaborators. {@link IUserContext} is
 * served by the same singleton {@link SettingsRepositoryImpl} that backs
 * {@code ISettingsRepository}.
 */
@Module
public abstract class ParserModule {

    @Binds
    abstract IFeedParser bindFeedParser(HNFeedParser impl);

    @Binds
    abstract ICommentsParser bindCommentsParser(HNCommentsParser impl);

    @Binds
    abstract IColorProvider bindColorProvider(AndroidColorProvider impl);

    @Binds
    abstract IUserContext bindUserContext(SettingsRepositoryImpl impl);

}
