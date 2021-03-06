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
package com.antew.redditinpictures.library.ui;

import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.antew.redditinpictures.library.Constants;
import com.antew.redditinpictures.library.database.QueryCriteria;
import com.antew.redditinpictures.library.database.RedditContract;
import com.antew.redditinpictures.library.event.ForcePostRefreshEvent;
import com.antew.redditinpictures.library.event.RequestCompletedEvent;
import com.antew.redditinpictures.library.event.RequestInProgressEvent;
import com.antew.redditinpictures.library.interfaces.ActionBarTitleChanger;
import com.antew.redditinpictures.library.interfaces.RedditDataProvider;
import com.antew.redditinpictures.library.model.Age;
import com.antew.redditinpictures.library.model.Category;
import com.antew.redditinpictures.library.preferences.SharedPreferencesHelper;
import com.antew.redditinpictures.library.service.RedditService;
import com.antew.redditinpictures.library.ui.base.BaseFragment;
import com.antew.redditinpictures.library.util.AndroidUtil;
import com.antew.redditinpictures.library.util.BundleUtil;
import com.antew.redditinpictures.library.util.Ln;
import com.antew.redditinpictures.library.util.RedditUtil;
import com.antew.redditinpictures.library.util.Strings;
import com.antew.redditinpictures.library.util.SubredditUtil;
import com.antew.redditinpictures.pro.R;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.squareup.otto.Subscribe;
import java.util.ArrayList;
import java.util.List;

/**
 * Fragment with convenience methods for displaying images
 *
 * @param <T>
 *     The type of view the fragment is using, e.g. GridView, ListView
 * @param <V>
 *     The type of the cursor adapter backing the view
 */
public abstract class RedditImageAdapterViewFragment<T extends AdapterView, V extends CursorAdapter> extends BaseFragment
    implements AdapterView.OnItemClickListener, LoaderManager.LoaderCallbacks<Cursor> {
    protected V           mAdapter;
    @InjectView(R.id.no_images)
    protected TextView    mNoImages;
    @InjectView(R.id.pb_progress)
    protected ProgressBar mProgress;
    protected boolean     mRequestInProgress;
    protected Integer     mVisiblePosition;
    protected Integer     mTopOffset;
    private   String      mAfter;
    protected String   mCurrentSubreddit = Constants.Reddit.REDDIT_FRONTPAGE;
    protected Category mCategory         = Category.HOT;
    protected Age      mAge              = Age.TODAY;

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is optional, and non-graphical fragments can return null (which
     * is the default implementation).  This will be called between
     * {@link #onCreate(android.os.Bundle)} and {@link #onActivityCreated(android.os.Bundle)}.
     * <p/>
     * <p>If you return a View from here, you will later be called in
     * {@link #onDestroyView} when the view is being released.
     *
     * @param inflater
     *     The LayoutInflater object that can be used to inflate
     *     any views in the fragment,
     * @param container
     *     If non-null, this is the parent view that the fragment's
     *     UI should be attached to.  The fragment should not add the view itself,
     *     but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState
     *     If non-null, this fragment is being re-constructed
     *     from a previous saved state as given here.
     *
     * @return Return the View for the fragment's UI, or null.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(getLayoutId(), container, false);
        ButterKnife.inject(this, v);
        return v;
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * This is generally
     * tied to {@link android.app.Activity#onResume() Activity.onResume} of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onResume() {
        super.onResume();
        getActivity().getLoaderManager().restartLoader(Constants.Loader.LOADER_REDDIT, null, this);
        getActivity().getLoaderManager().restartLoader(Constants.Loader.LOADER_POSTS, null, this);
        fetchPostsIfNeeded();
    }

    /**
     * Called when the Fragment is no longer resumed.  This is generally
     * tied to {@link android.app.Activity#onPause() Activity.onPause} of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onPause() {
        super.onPause();
        getActivity().getLoaderManager().destroyLoader(Constants.Loader.LOADER_REDDIT);
        getActivity().getLoaderManager().destroyLoader(Constants.Loader.LOADER_POSTS);
    }

    protected abstract int getLayoutId();

    protected abstract V getNewAdapter();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        handleArguments(getArguments());

        // Initialize the adapter to null, the adapter will be populated in onLoadFinished
        if (mAdapter == null) {
            mAdapter = getNewAdapter();
        }

        if (getActivity() instanceof ActionBarTitleChanger) {
            ((ActionBarTitleChanger) getActivity()).setActionBarTitle(mCurrentSubreddit, RedditUtil.getSortDisplayString(mCategory, mAge));
        }

        if (getActivity() instanceof RedditDataProvider) {
            RedditDataProvider redditDataProvider = (RedditDataProvider) getActivity();
            redditDataProvider.setSubreddit(mCurrentSubreddit);
            redditDataProvider.setCategory(mCategory);
            redditDataProvider.setAge(mAge);
        }

        if (getAdapterView() != null) {
            getAdapterView().setOnItemClickListener(this);
        }

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(Constants.Extra.EXTRA_VISIBLE_POSITION)) {
                mVisiblePosition = savedInstanceState.getInt(Constants.Extra.EXTRA_VISIBLE_POSITION);
            }
            if (savedInstanceState.containsKey(Constants.Extra.EXTRA_TOP_OFFSET)) {
                mTopOffset = savedInstanceState.getInt(Constants.Extra.EXTRA_TOP_OFFSET);
            }
        }
    }

    public void handleArguments(Bundle arguments) {
        mCurrentSubreddit = BundleUtil.getString(arguments, Constants.Extra.EXTRA_SUBREDDIT, Constants.Reddit.REDDIT_FRONTPAGE);
        mCategory = Category.fromString(BundleUtil.getString(arguments, Constants.Extra.EXTRA_CATEGORY, Category.HOT.getName()));
        mAge = Age.fromString(BundleUtil.getString(arguments, Constants.Extra.EXTRA_AGE, Age.TODAY.getAge()));
    }

    protected abstract T getAdapterView();

    protected void produceRequestInProgressEvent() {
        mBus.post(new RequestInProgressEvent());
    }

    /**
     * If we're forcing a refresh from Reddit we want
     * to discard the old posts so that the user has
     * a better indication we are fetching posts anew.
     *
     * @param event
     */
    @Subscribe
    public void handleForcePostRefreshEvent(ForcePostRefreshEvent event) {
        if (mAdapter != null) {
            mAdapter.swapCursor(null);
        }
    }

    /**
     * Instantiate and return a new Loader for the given ID.
     *
     * @param id
     *     The ID whose loader is to be created.
     * @param args
     *     Any arguments supplied by the caller.
     *
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case Constants.Loader.LOADER_REDDIT:
                return new CursorLoader(getActivity(), RedditContract.RedditData.CONTENT_URI, // uri
                                        null,                                  // projection
                                        "subreddit = ?",                       // selection
                                        new String[] { mCurrentSubreddit },      // selectionArgs[]
                                        RedditContract.Posts.DEFAULT_SORT);    // sort
            case Constants.Loader.LOADER_POSTS:
                QueryCriteria queryCriteria = getPostsQueryCriteria();
                String selection = null;
                String[] selectionArgs = null;
                List<String> selectionArgsList = new ArrayList<String>();

                // If we have an aggregate subreddit we want to return relevant things.
                if (SubredditUtil.isAggregateSubreddit(mCurrentSubreddit)) {
                    selection = null;
                    selectionArgs = null;
                } else if (mCurrentSubreddit.contains("+")) {
                    // Poor mans checking for multis. If we have a multi, we want to handle all of them appropriately.
                    String[] subredditArray = mCurrentSubreddit.split("\\+");

                    for (String item : subredditArray) {
                        if (selection == null) {
                            selection = RedditContract.PostColumns.SUBREDDIT + " in (?";
                        } else {
                            selection += ",?";
                        }
                        selectionArgsList.add(item);
                    }
                    // Close the in statement.
                    selection += ")";
                } else {
                    selection = RedditContract.PostColumns.SUBREDDIT + " = ?";
                    selectionArgsList.add(mCurrentSubreddit);
                }

                // If the user doesn't want to see NSFW images, filter them out. Otherwise do nothing.
                if (!SharedPreferencesHelper.getShowNsfwImages(getActivity())) {
                    if (Strings.isEmpty(selection)) {
                        selection = RedditContract.PostColumns.OVER_18 + " = ?";
                    } else {
                        selection += " and " + RedditContract.PostColumns.OVER_18 + " = ?";
                    }
                    selectionArgsList.add("0");
                }

                if (selectionArgsList != null && selectionArgsList.size() > 0) {
                    selectionArgs = selectionArgsList.toArray(new String[] { });
                }

                Ln.d("Retrieveing Posts For %s %s", selection, Strings.toString(selectionArgs));

                return new CursorLoader(getActivity(), RedditContract.Posts.CONTENT_URI,  // uri
                                        queryCriteria.getProjection(),     // projection
                                        selection,                         // selection
                                        selectionArgs,                     // selectionArgs[]
                                        queryCriteria.getSort());          // sort
            default:
                return null;
        }
    }

    /**
     * Called when a previously created loader has finished its load.  Note
     * that normally an application is <em>not</em> allowed to commit fragment
     * transactions while in this call, since it can happen after an
     * activity's state is saved.  See {@link android.support.v4.app.FragmentManager#beginTransaction()
     * FragmentManager.openTransaction()} for further discussion on this.
     * <p/>
     * <p>This function is guaranteed to be called prior to the release of
     * the last data that was supplied for this Loader.  At this point
     * you should remove all use of the old data (since it will be released
     * soon), but should not do your own release of the data since its Loader
     * owns it and will take care of that.  The Loader will take care of
     * management of its data so you don't have to.  In particular:
     * <p/>
     * <ul>
     * <li> <p>The Loader will monitor for changes to the data, and report
     * them to you through new calls here.  You should not monitor the
     * data yourself.  For example, if the data is a {@link android.database.Cursor}
     * and you place it in a {@link android.widget.CursorAdapter}, use
     * the {@link android.widget.CursorAdapter#CursorAdapter(android.content.Context,
     * android.database.Cursor, int)} constructor <em>without</em> passing
     * in either {@link android.widget.CursorAdapter#FLAG_AUTO_REQUERY}
     * or {@link android.widget.CursorAdapter#FLAG_REGISTER_CONTENT_OBSERVER}
     * (that is, use 0 for the flags argument).  This prevents the CursorAdapter
     * from doing its own observing of the Cursor, which is not needed since
     * when a change happens you will get a new Cursor throw another call
     * here.
     * <li> The Loader will release the data once it knows the application
     * is no longer using it.  For example, if the data is
     * a {@link android.database.Cursor} from a {@link android.content.CursorLoader},
     * you should not call close() on it yourself.  If the Cursor is being placed in a
     * {@link android.widget.CursorAdapter}, you should use the
     * {@link android.widget.CursorAdapter#swapCursor(android.database.Cursor)}
     * method so that the old Cursor is not closed.
     * </ul>
     *
     * @param loader
     *     The Loader that has finished.
     * @param data
     *     The data generated by the Loader.
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case Constants.Loader.LOADER_REDDIT:
                if (data != null && data.moveToFirst()) {
                    mAfter = data.getString(data.getColumnIndex(RedditContract.RedditData.AFTER));
                }
                break;
            case Constants.Loader.LOADER_POSTS:
                mAdapter.swapCursor(data);

                if (data.getCount() == 0 && !mRequestInProgress) {
                    mNoImages.setVisibility(View.VISIBLE);
                    mProgress.setVisibility(View.GONE);
                } else if (data.getCount() == 0 && mRequestInProgress) {
                    mProgress.setVisibility(View.VISIBLE);
                } else {
                    if (mNoImages.getVisibility() == View.VISIBLE) {
                        mNoImages.setVisibility(View.GONE);
                    }
                    mProgress.setVisibility(View.GONE);
                    produceRequestCompletedEvent();
                    onPostsLoaded();
                }
                break;
            default:
                break;
        }
    }

    /**
     * Called when a previously created loader is being reset, and thus
     * making its data unavailable.  The application should at this point
     * remove any references it has to the Loader's data.
     *
     * @param loader
     *     The Loader that is being reset.
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case Constants.Loader.LOADER_REDDIT:
                break;
            case Constants.Loader.LOADER_POSTS:
                produceRequestCompletedEvent();
                mAdapter.swapCursor(null);
                break;
            default:
                break;
        }
    }

    protected void produceRequestCompletedEvent() {
        mBus.post(new RequestCompletedEvent());
    }

    protected abstract QueryCriteria getPostsQueryCriteria();

    /**
     * Callback method to be invoked when an item in this AdapterView has
     * been clicked.
     * <p/>
     * Implementers can call getItemAtPosition(position) if they need
     * to access the data associated with the selected item.
     *
     * @param parent
     *     The AdapterView where the click happened.
     * @param view
     *     The view within the AdapterView that was clicked (this
     *     will be a view provided by the adapter)
     * @param position
     *     The position of the view in the adapter.
     * @param id
     *     The row id of the item that was clicked.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        EasyTracker.getInstance(getActivity())
                   .send(MapBuilder.createEvent(Constants.Analytics.Category.UI_ACTION, Constants.Analytics.Action.OPEN_POST,
                                                mCurrentSubreddit, null).build());
        final Intent i = new Intent(getActivity(), getImageDetailActivityClass());
        Bundle b = new Bundle();
        b.putString(Constants.Extra.EXTRA_SUBREDDIT, mCurrentSubreddit);
        b.putString(Constants.Extra.EXTRA_CATEGORY, mCategory.name());
        b.putString(Constants.Extra.EXTRA_AGE, mAge.name());
        i.putExtra(Constants.Extra.EXTRA_IMAGE, position);
        i.putExtras(b);

        if (AndroidUtil.hasJellyBean()) {
            ActivityOptions options = ActivityOptions.makeScaleUpAnimation(view, 0, 0, view.getWidth(), view.getHeight());
            getActivity().startActivity(i, options.toBundle());
        } else {
            startActivity(i);
        }
    }

    protected Class<? extends ImageDetailActivity> getImageDetailActivityClass() {
        return ImageDetailActivity.class;
    }

    protected void fetchAdditionalImagesFromReddit() {
        //This should only be called when a request is not in progress and we have an "After" value. Otherwise it creates an infinite loop of doom. DOOM!
        if (!mRequestInProgress && Strings.notEmpty(mAfter)) {
            produceRequestInProgressEvent();
            RedditService.getPosts(getActivity(), mCurrentSubreddit, mAge, mCategory, mAfter);
        }
    }

    protected void fetchPostsIfNeeded() {
        produceRequestInProgressEvent();
        RedditService.getPostsIfNeeded(getActivity(), mCurrentSubreddit, mAge, mCategory);
    }

    @Subscribe
    public void requestInProgress(RequestInProgressEvent event) {
        mRequestInProgress = true;
        mNoImages.setVisibility(View.GONE);
        mProgress.setVisibility(View.VISIBLE);
    }

    @Subscribe
    public void requestCompleted(RequestCompletedEvent event) {
        mRequestInProgress = false;
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        mAdapter = null;
        mNoImages = null;
        mProgress = null;
    }

    public abstract void onPostsLoaded();
}
