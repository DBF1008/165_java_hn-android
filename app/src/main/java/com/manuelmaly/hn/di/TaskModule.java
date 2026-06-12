package com.manuelmaly.hn.di;

import com.manuelmaly.hn.task.TaskFactory;
import com.manuelmaly.hn.task.TaskFactoryImpl;

import dagger.Binds;
import dagger.Module;

/** Binds the {@link TaskFactory} used by the task entry points. */
@Module
public abstract class TaskModule {

    @Binds
    abstract TaskFactory bindTaskFactory(TaskFactoryImpl impl);

}
