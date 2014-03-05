package com.antew.redditinpictures.library;

import dagger.Module;

/**
 * Add all the other modules to this one.
 */
@Module
    (
        includes = {
            AndroidModule.class,
            ApplicationModule.class
        }
    )
public class RootModule {
}
