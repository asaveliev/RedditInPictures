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
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import com.antew.redditinpictures.library.Constants;
import com.antew.redditinpictures.library.database.RedditContract;
import com.antew.redditinpictures.library.json.JsonDeserializer;
import com.antew.redditinpictures.library.model.Age;
import com.antew.redditinpictures.library.model.Category;
import com.antew.redditinpictures.library.model.reddit.RedditApi;
import com.antew.redditinpictures.library.model.reddit.RedditApiData;
import com.antew.redditinpictures.library.service.RedditService;
import com.antew.redditinpictures.library.util.BundleUtil;
import com.antew.redditinpictures.library.util.Ln;
import com.antew.redditinpictures.library.util.SubredditUtil;
import java.util.Date;

class PostResponse extends RedditResponseHandler {
    private RedditResult result;

    public PostResponse(RedditResult result) {
        super();
        this.result = result;
    }

    @Override
    public void processHttpResponse(Context context) {
        ContentResolver resolver = context.getContentResolver();

        RedditApi redditApi = JsonDeserializer.deserialize(result.getJson(), RedditApi.class);
        if (redditApi == null) {
            Ln.e("Error parsing Reddit api response");
            return;
        }

        Bundle arguments = result.getExtraData();
        boolean replaceAll = BundleUtil.getBoolean(arguments, RedditService.EXTRA_REPLACE_ALL, false);
        String subreddit = BundleUtil.getString(arguments, Constants.Extra.EXTRA_SUBREDDIT, Constants.Reddit.REDDIT_FRONTPAGE);
        Category category = Category.fromString(BundleUtil.getString(arguments, Constants.Extra.EXTRA_CATEGORY, Category.HOT.getName()));
        Age age = Age.fromString(BundleUtil.getString(arguments, Constants.Extra.EXTRA_AGE, Age.TODAY.getAge()));

        // If we are replacing all, go ahead and clear out the old posts.
        if (replaceAll) {
            SubredditUtil.deletePostsForSubreddit(context, subreddit);
        }

        RedditApiData data = redditApi.getData();
        if (data != null) {
            data.setRetrievedDate(new Date());
            data.setSubreddit(subreddit);
            data.setCategory(category);
            data.setAge(age);
        }

        redditApi.setData(data);

        // Each time we want to remove the old before/after/modhash rows from the Reddit data
        // TODO: Update this to support category and age in the future. This will also require changes to how we handle storing results.
        int redditRowsDeleted = resolver.delete(RedditContract.RedditData.CONTENT_URI, "subreddit = ?", new String[] { subreddit });
        Ln.i("Deleted %d reddit rows", redditRowsDeleted);

        ContentValues[] operations = redditApi.getPostDataContentValues(true);
        ContentValues redditValues = redditApi.getContentValues();

        // Add the new Reddit data
        Uri newData = resolver.insert(RedditContract.RedditData.CONTENT_URI, redditValues);
        Ln.i("Inserted reddit data %s", newData.toString());

        int rowsInserted = resolver.bulkInsert(RedditContract.Posts.CONTENT_URI, operations);
        Ln.i("Inserted %d rows", rowsInserted);
    }
}