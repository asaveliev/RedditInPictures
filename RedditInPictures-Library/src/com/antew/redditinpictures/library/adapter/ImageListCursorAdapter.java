/*
 * Copyright (C) 2012 Antew | antewcode@gmail.com
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
package com.antew.redditinpictures.library.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.antew.redditinpictures.library.R;
import com.antew.redditinpictures.library.reddit.PostData;
import com.antew.redditinpictures.library.utils.ImageFetcher;
import com.antew.redditinpictures.sqlite.RedditContract;
import com.squareup.picasso.Picasso;

/**
 * This is used as the backing adapter for the {@link android.widget.GridView} in {@link com.antew.redditinpictures.library.ui.ImageGridFragment}
 *
 * @author Antew
 *
 */
public class ImageListCursorAdapter extends CursorAdapter {
    public static final String    TAG         = ImageListCursorAdapter.class.getSimpleName();
    private int                   mItemHeight = 0;
    private int                   mNumColumns = 0;
    private GridView.LayoutParams mImageViewLayoutParams;
    private ImageFetcher          mImageFetcher;
    private Cursor                mCursor;
    private LayoutInflater mInflater;

    /**
     *
     * @param context
     *            The context
     * @param imageFetcher
     *            The image fetcher (currently using a {@link com.antew.redditinpictures.library.imgur.SizeAwareImageFetcher}
     * @param cursor
     *            Cursor to a database containing PostData information
     */
    public ImageListCursorAdapter(Context context, ImageFetcher imageFetcher, Cursor cursor) {
        super(context, cursor, 0);
        mContext = context;
        mImageFetcher = imageFetcher;
        mCursor = cursor;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public Object getItem(int position) {
        mCursor.moveToPosition(position);
        return mCursor;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ImageView imageView = (ImageView) view.findViewById(R.id.imageView);
        TextView postTitle = (TextView) view.findViewById(R.id.post_title);
        TextView postInformation = (TextView) view.findViewById(R.id.post_information);
        TextView postVotes = (TextView) view.findViewById(R.id.post_votes);
        PostData postData = PostData.fromListViewProjection(cursor);

//        String url = cursor.getString(cursor.getColumnIndex(RedditContract.PostColumns.URL));
//        String thumbnail = cursor.getString(cursor.getColumnIndex(RedditContract.PostColumns.THUMBNAIL));
//        String upvotes = cursor.getString(cursor.getColumnIndex(RedditContract.PostColumns.SCORE));

        // If we have a thumbnail from Reddit use that, otherwise use the full URL
        // Reddit will send 'default' for one of the default alien icons, which we want to avoid using
        String url = postData.getUrl();
        String thumbnail = postData.getThumbnail();
        if (!thumbnail.trim().equals("") && !thumbnail.equals("default")) {
            url = thumbnail;
        }

        Picasso.with(mContext).setDebugging(true);
        Picasso.with(mContext).load(url).placeholder(R.drawable.empty_photo).into(imageView);

        postTitle.setText(postData.getTitle());
        postInformation.setText(postData.getSelftext());
        postVotes.setText("" + postData.getScore());
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return  mInflater.inflate(R.layout.image_list_item, parent, false);
    }

}
