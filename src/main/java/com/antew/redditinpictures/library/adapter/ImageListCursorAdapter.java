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
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.CursorAdapter;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnLongClick;
import com.antew.redditinpictures.library.Constants;
import com.antew.redditinpictures.library.model.Vote;
import com.antew.redditinpictures.library.imgur.ResolveAlbumCoverWorkerTask;
import com.antew.redditinpictures.library.model.reddit.PostData;
import com.antew.redditinpictures.library.model.reddit.RedditLoginInformation;
import com.antew.redditinpictures.library.service.RedditService;
import com.antew.redditinpictures.library.ui.RedditFragmentActivity;
import com.antew.redditinpictures.library.util.Ln;
import com.antew.redditinpictures.library.util.PostUtil;
import com.antew.redditinpictures.library.util.Strings;
import com.antew.redditinpictures.pro.R;
import com.squareup.picasso.Picasso;
import java.util.regex.Pattern;

/**
 * This is used as the backing adapter for a {@link android.widget.GridView}
 *
 * @author Antew
 */
public class ImageListCursorAdapter extends CursorAdapter {
    private LayoutInflater                  mInflater;
    private ImageListItemMenuActionListener mActionListener;
    private Pattern mImgurNonAlbumPattern = Pattern.compile("^https?://imgur.com/[^/]*$");
    private Pattern mImgurAlbumPattern    = Pattern.compile("^https?://imgur.com/a/.*$");

    public interface ImageListItemMenuActionListener {
        /**
         * Request to view the given image.
         *
         * @param postData
         *     The PostData of the image to open
         * @param position
         *     The position in the cursor of the image to open
         */
        public void viewImage(PostData postData, int position);

        /**
         * Request to save the given image.
         *
         * @param postData
         *     The PostData of the image.
         */
        public void saveImage(PostData postData);

        /**
         * Request to share the given image.
         *
         * @param postData
         *     The PostData of the image.
         */
        public void shareImage(PostData postData);

        /**
         * Request to open the given image in an external application.
         *
         * @param postData
         *     The PostData of the image.
         */
        public void openPostExternal(PostData postData);

        /**
         * Request to report the given image.
         *
         * @param postData
         *     The PostData of the image.
         */
        public void reportImage(PostData postData);
    }

    public ImageListCursorAdapter(Context context, ImageListItemMenuActionListener actionListener) {
        super(context, null, 0);
        mContext = context;
        mActionListener = actionListener;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public Object getItem(int position) {
        mCursor.moveToPosition(position);
        return mCursor;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.image_list_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final ViewHolder holder;
        if (view != null && view.getTag() != null) {
            holder = (ViewHolder) view.getTag();
        } else {
            holder = new ViewHolder(view);
            view.setTag(holder);
        }

        final PostData postData = PostData.fromListViewProjection(cursor);
        final int position = cursor.getPosition();

        // If we have a thumbnail from Reddit use that, otherwise use the full URL
        // Reddit will send 'default' for one of the default alien icons, which we want to avoid using
        String url = postData.getUrl();
        String thumbnail = postData.getThumbnail();
        if (Strings.notEmpty(thumbnail) && !thumbnail.equals("default")) {
            url = thumbnail;
        } else {
            // If the url is not pointing directly to the image. (Normally at i.imgur.com not imgur.com)
            if (postData.getDomain().equals("imgur.com")) {
                // If the url is not an album but is just using a shortlink to an image append .jpg to the end and hope for the best.
                if (mImgurNonAlbumPattern.matcher(url).matches()) {
                    // The S before the extension gets us a small image.
                    url += "s.jpg";
                    Ln.d("Updating Url To: %s", url);
                } else if (mImgurAlbumPattern.matcher(url).matches()) {
                    if (ResolveAlbumCoverWorkerTask.cancelPotentialDownload(url, holder.imageView)) {
                        ResolveAlbumCoverWorkerTask task = new ResolveAlbumCoverWorkerTask(url, holder.imageView, mContext);
                        ResolveAlbumCoverWorkerTask.LoadingTaskHolder loadingTaskHolder = new ResolveAlbumCoverWorkerTask.LoadingTaskHolder(
                            task);
                        holder.imageView.setTag(loadingTaskHolder);
                        task.execute();
                    }
                    //Since this is an album, we don't want it to be attempted to be loaded.
                    url = null;
                }
            }
        }

        if (Strings.notEmpty(url)) {
            Picasso.with(mContext)
                   .load(url)
                   .placeholder(R.drawable.loading_spinner_48)
                   .error(R.drawable.empty_photo)
                   .fit()
                   .centerCrop()
                   .into(holder.imageView);
        }

        String separator = " " + "\u2022" + " ";
        String titleText = "<font color='#ffffff'>" + postData.getTitle() + "</font><font color='#BEBEBE'> (" + postData.getDomain() + ")</font>";
        holder.postTitle.setText(Html.fromHtml(titleText));
        String postInformation = "";

        //If we have a NSFW image.
        if (postData.isOver_18()) {
            postInformation = "<font color='#AC3939'>" + mContext.getString(R.string.nsfw) + "</font>" + separator;
        }

        postInformation += postData.getSubreddit() + separator + postData.getNum_comments() + " " + mContext.getString(R.string.comments);

        holder.postInformation.setText(Html.fromHtml(postInformation));
        holder.postVotes.setText("" + postData.getScore());

        if (postData.getVote() == Vote.UP) {
            holder.upVote.setTag(Vote.UP);
            holder.upVote.setImageResource(R.drawable.arrow_up_highlighted);

            holder.downVote.setTag(null);
            holder.downVote.setImageResource(R.drawable.arrow_down);
        } else if (postData.getVote() == Vote.DOWN) {
            holder.upVote.setTag(null);
            holder.upVote.setImageResource(R.drawable.arrow_up);

            holder.upVote.setTag(Vote.DOWN);
            holder.downVote.setImageResource(R.drawable.arrow_down_highlighted);
        } else {
            holder.upVote.setTag(null);
            holder.upVote.setImageResource(R.drawable.arrow_up);

            holder.downVote.setTag(null);
            holder.downVote.setImageResource(R.drawable.arrow_down);
        }

        holder.upVote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PostUtil.votePost(mContext, postData, Vote.UP);
            }
        });

        holder.downVote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PostUtil.votePost(mContext, postData, Vote.DOWN);
            }
        });

        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mActionListener != null) {
                    mActionListener.viewImage(postData, position);
                }
            }
        });

        holder.save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mActionListener != null) {
                    mActionListener.saveImage(postData);
                }
            }
        });

        holder.share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mActionListener != null) {
                    mActionListener.shareImage(postData);
                }
            }
        });

        holder.open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mActionListener != null) {
                    mActionListener.openPostExternal(postData);
                }
            }
        });

        holder.report.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mActionListener != null) {
                    mActionListener.reportImage(postData);
                }
            }
        });
    }

    protected class ViewHolder {
        @InjectView(R.id.iv_image)
        ImageView   imageView;
        @InjectView(R.id.tv_title)
        TextView    postTitle;
        @InjectView(R.id.tv_post_information)
        TextView    postInformation;
        @InjectView(R.id.tv_votes)
        TextView    postVotes;
        @InjectView(R.id.ib_upVote)
        ImageButton upVote;
        @InjectView(R.id.ib_downVote)
        ImageButton downVote;
        @InjectView(R.id.ib_view)
        ImageButton view;
        @InjectView(R.id.ib_save)
        ImageButton save;
        @InjectView(R.id.ib_share)
        ImageButton share;
        @InjectView(R.id.ib_open)
        ImageButton open;
        @InjectView(R.id.ib_report)
        ImageButton report;

        public ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }

        @OnLongClick({ R.id.ib_view, R.id.ib_save, R.id.ib_share, R.id.ib_open, R.id.ib_report })
        protected boolean onLongClickMenuOption(View view) {
            if (view != null) {
                String description = Strings.toString(view.getContentDescription());
                if (Strings.notEmpty(description)) {
                    Toast.makeText(mContext, description, Toast.LENGTH_SHORT).show();
                    return true;
                }
            }

            return false;
        }
    }
}
