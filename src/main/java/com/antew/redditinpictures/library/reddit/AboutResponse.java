/*
 * Copyright (C) 2014 Antew
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.antew.redditinpictures.library.reddit;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import com.antew.redditinpictures.library.Constants;
import com.antew.redditinpictures.library.database.RedditContract;
import com.antew.redditinpictures.library.json.JsonDeserializer;
import com.antew.redditinpictures.library.model.reddit.About;
import com.antew.redditinpictures.library.util.Ln;

public class AboutResponse extends RedditResponseHandler {

    public static final String TAG = AboutResponse.class.getSimpleName();
    private RedditResult result;

    public AboutResponse(RedditResult result) {
        this.result = result;
    }

    @Override
    public void processHttpResponse(Context context) {
        Ln.v("About Subreddit complete! = %s", result.getJson());
        About aboutSubreddit = JsonDeserializer.deserialize(result.getJson(), About.class);

        if (aboutSubreddit == null) {
            Ln.e("Something went wrong on About Subreddit status code: %d json: %s", result.getHttpStatusCode(), result.getJson());
            return;
        }

        ContentResolver resolver = context.getContentResolver();
        //TODO: Once API-11 is sunset, replace with an update instead of delete/insert.
        // Updates with parameters aren't supported prior to API-11 (Honeycomb). So instead we are just deleting the record if it exists and recreating it.
        resolver.delete(RedditContract.Subreddits.CONTENT_URI, "subredditId = ?", new String[] { aboutSubreddit.getData().getId() });
        Ln.v("Deleted row");
        resolver.insert(RedditContract.Subreddits.CONTENT_URI, aboutSubreddit.getContentValues());
        Ln.v("Inserted row");

        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.Broadcast.BROADCAST_ABOUT_SUBREDDIT));
    }
}
