package com.antew.redditinpictures;

import android.app.Application;

import com.antew.redditinpictures.library.RedditInPicturesApplication;
import com.antew.redditinpictures.library.modules.RootModule;
import com.antew.redditinpictures.library.utils.Ln;

public class Modules {

    public static Object[] get(RedditInPicturesApplication app) {
        Ln.e("Called free 'Modules' class");
        return new Object[]{
                new RootModule(app),
                new ApplicationModulePro()
        };
    }
}