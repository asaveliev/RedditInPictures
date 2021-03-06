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
package com.antew.redditinpictures.library.ui.base;

import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnLongClick;
import com.antew.redditinpictures.library.Constants;
import com.antew.redditinpictures.library.adapter.SubredditMenuDrawerCursorAdapter;
import com.antew.redditinpictures.library.annotation.ForApplication;
import com.antew.redditinpictures.library.database.RedditContract;
import com.antew.redditinpictures.library.dialog.AboutSubredditDialogFragment;
import com.antew.redditinpictures.library.dialog.AddSubredditDialogFragment;
import com.antew.redditinpictures.library.dialog.ExitDialogFragment;
import com.antew.redditinpictures.library.dialog.LoginDialogFragment;
import com.antew.redditinpictures.library.dialog.SetDefaultSubredditsDialogFragment;
import com.antew.redditinpictures.library.interfaces.RedditDataProvider;
import com.antew.redditinpictures.library.listener.OnSubredditActionListener;
import com.antew.redditinpictures.library.model.Age;
import com.antew.redditinpictures.library.model.Category;
import com.antew.redditinpictures.library.model.reddit.RedditLoginInformation;
import com.antew.redditinpictures.library.model.reddit.SubredditData;
import com.antew.redditinpictures.library.reddit.MySubredditsResponse;
import com.antew.redditinpictures.library.service.RedditService;
import com.antew.redditinpictures.library.util.Strings;
import com.antew.redditinpictures.library.util.SubredditUtil;
import com.antew.redditinpictures.pro.R;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.squareup.picasso.Picasso;
import java.util.Date;
import javax.inject.Inject;
import net.simonvt.menudrawer.MenuDrawer;
import net.simonvt.menudrawer.Position;

public abstract class BaseFragmentActivityWithMenu extends BaseFragmentActivity
    implements LoaderManager.LoaderCallbacks<Cursor>, SetDefaultSubredditsDialogFragment.SetDefaultSubredditsDialogListener,
               AddSubredditDialogFragment.AddSubredditDialogListener, RedditDataProvider, ExitDialogFragment.ExitDialogListener {
    protected MenuDrawer                       mMenuDrawer;
    protected SubredditMenuDrawerCursorAdapter mSubredditAdapter;
    @Inject
    @ForApplication
    protected InputMethodManager mInputMethodManager;
    @InjectView(R.id.et_subreddit_filter)
    protected EditText                         mSubredditFilter;
    @InjectView(R.id.ib_clear)
    protected ImageButton                      mClearSubredditFilter;
    @InjectView(R.id.lv_subreddits)
    protected ListView                         mSubredditList;
    @InjectView(R.id.ib_add)
    protected ImageButton                      mAddSubreddit;
    @InjectView(R.id.ib_sort)
    protected ImageButton                      mSortSubreddits;
    @InjectView(R.id.ib_refresh)
    protected ImageButton                      mRefreshSubreddits;
    protected String   mSelectedSubreddit = Constants.Reddit.REDDIT_FRONTPAGE;
    protected Category mCategory          = Category.HOT;
    protected Age      mAge               = Age.TODAY;
    protected String   mSubredditSort     = RedditContract.Subreddits.SORT_ALPHABETICALLY;
    private long mLastTimeBackPressed;
    private OnSubredditActionListener       mSubredditActionListener = new OnSubredditActionListener() {

        @Override
        public void onAction(SubredditData subredditData, SubredditAction action) {
            // The keyboard doesn't always close, so let's make sure that we close it in all cases.
            hideKeyboard();

            switch (action) {
                case View:
                    EasyTracker.getInstance(BaseFragmentActivityWithMenu.this)
                               .send(MapBuilder.createEvent(Constants.Analytics.Category.SUBREDDIT_MENU_ACTION,
                                                            Constants.Analytics.Action.OPEN_SUBREDDIT, subredditData.getDisplay_name(),
                                                            null).build());
                    mMenuDrawer.closeMenu(true);
                    loadSubredditFromMenu(subredditData.getDisplay_name());
                    break;
                case Subscribe:
                    EasyTracker.getInstance(BaseFragmentActivityWithMenu.this)
                               .send(MapBuilder.createEvent(Constants.Analytics.Category.SUBREDDIT_MENU_ACTION,
                                                            Constants.Analytics.Action.SUBSCRIBE_TO_SUBREDDIT,
                                                            subredditData.getDisplay_name(), null).build());
                    subscribeToSubreddit(subredditData.getName());
                    break;
                case Unsubscribe:
                    EasyTracker.getInstance(BaseFragmentActivityWithMenu.this)
                               .send(MapBuilder.createEvent(Constants.Analytics.Category.SUBREDDIT_MENU_ACTION,
                                                            Constants.Analytics.Action.UNSUBSCRIBE_FROM_SUBREDDIT,
                                                            subredditData.getDisplay_name(), null).build());
                    unsubscribeToSubreddit(subredditData.getName());
                    break;
                case Info:
                    EasyTracker.getInstance(BaseFragmentActivityWithMenu.this)
                               .send(MapBuilder.createEvent(Constants.Analytics.Category.SUBREDDIT_MENU_ACTION,
                                                            Constants.Analytics.Action.DISPLAY_SUBREDDIT_INFO,
                                                            subredditData.getDisplay_name(), null).build());
                    displaySubredditInfo(subredditData);
                    break;
                case Delete:
                    EasyTracker.getInstance(BaseFragmentActivityWithMenu.this)
                               .send(MapBuilder.createEvent(Constants.Analytics.Category.SUBREDDIT_MENU_ACTION,
                                                            Constants.Analytics.Action.DELETE_SUBREDDIT, subredditData.getDisplay_name(),
                                                            null).build());
                    deleteSubreddit(subredditData.getName());
                    break;
            }
        }
    };
    private TextWatcher                     mSubredditFilterWatcher  = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            return;
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s == null) {
                return;
            }

            if (Strings.isEmpty(s) && mClearSubredditFilter.getVisibility() == View.VISIBLE) {
                mClearSubredditFilter.setVisibility(View.INVISIBLE);
            } else {
                mClearSubredditFilter.setVisibility(View.VISIBLE);
            }

            filterSubreddits(s.toString());
        }

        @Override
        public void afterTextChanged(Editable s) {
            return;
        }
    };
    private AdapterView.OnItemClickListener mSubredditClickListener  = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Cursor cursor = (Cursor) parent.getItemAtPosition(position);
            String subredditName = cursor.getString(cursor.getColumnIndex(RedditContract.SubredditColumns.DISPLAY_NAME));
            int priority = cursor.getInt(cursor.getColumnIndex(RedditContract.SubredditColumns.PRIORITY));

            EasyTracker.getInstance(BaseFragmentActivityWithMenu.this)
                       .send(MapBuilder.createEvent(Constants.Analytics.Category.MENU_DRAWER_ACTION,
                                                    Constants.Analytics.Action.OPEN_SUBREDDIT, subredditName, null).build());

            // TODO: Make this less hacky...
            // Load the actual frontpage of reddit if selected
            if (priority == MySubredditsResponse.DefaultSubreddit.FRONTPAGE.getPriority()) {
                subredditName = Constants.Reddit.REDDIT_FRONTPAGE;
            }

            mMenuDrawer.setActiveView(view, position);
            mSubredditAdapter.setActivePosition(position);
            mMenuDrawer.closeMenu(true);
            // The keyboard doesn't always close, so let's make sure that we close it in all cases.
            hideKeyboard();
            loadSubredditFromMenu(subredditName);
        }
    };

    protected void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            mInputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    public void onFinishExitDialog() {
        finish();
    }

    @OnClick(R.id.ib_add)
    protected void onAddSubreddit() {
        EasyTracker.getInstance(BaseFragmentActivityWithMenu.this)
                   .send(MapBuilder.createEvent(Constants.Analytics.Category.MENU_DRAWER_ACTION,
                                                Constants.Analytics.Action.SHOW_ADD_SUBREDDIT, null, null).build());
        AddSubredditDialogFragment.newInstance().show(getFragmentManager(), Constants.Dialog.DIALOG_ADD_SUBREDDIT);
    }

    @OnLongClick({ R.id.ib_add, R.id.ib_sort, R.id.ib_refresh, R.id.ib_clear })
    protected boolean onLongClickMenuOption(View view) {
        if (view != null) {
            String description = Strings.toString(view.getContentDescription());
            if (Strings.notEmpty(description)) {
                EasyTracker.getInstance(BaseFragmentActivityWithMenu.this)
                           .send(MapBuilder.createEvent(Constants.Analytics.Category.MENU_DRAWER_ACTION,
                                                        Constants.Analytics.Action.LONG_PRESS, description, null).build());
                Toast.makeText(this, description, Toast.LENGTH_SHORT).show();
                return true;
            }
        }

        return false;
    }

    @OnClick(R.id.ib_clear)
    protected void onClearSubredditFilter() {
        EasyTracker.getInstance(BaseFragmentActivityWithMenu.this)
                   .send(MapBuilder.createEvent(Constants.Analytics.Category.MENU_DRAWER_ACTION, Constants.Analytics.Action.CLEAR,
                                                Constants.Analytics.Label.SUBREDDIT_FILTER, null).build());
        if (mSubredditFilter != null) {
            mSubredditFilter.setText(null);
        }
    }

    @OnClick(R.id.ib_sort)
    protected void onSortSubreddits() {
        if (mSubredditSort.equals(RedditContract.Subreddits.SORT_ALPHABETICALLY)) {
            EasyTracker.getInstance(BaseFragmentActivityWithMenu.this)
                       .send(MapBuilder.createEvent(Constants.Analytics.Category.MENU_DRAWER_ACTION,
                                                    Constants.Analytics.Action.SORT_SUBREDDITS, Constants.Analytics.Label.POPULARITY, null)
                                       .build());
            mSubredditSort = RedditContract.Subreddits.SORT_BY_POPULARITY;
            Picasso.with(this).load(R.drawable.ic_action_sort_2_dark).into(mSortSubreddits);
            mSortSubreddits.setContentDescription(getString(R.string.sort_alphabetically));
        } else {
            EasyTracker.getInstance(BaseFragmentActivityWithMenu.this)
                       .send(MapBuilder.createEvent(Constants.Analytics.Category.MENU_DRAWER_ACTION,
                                                    Constants.Analytics.Action.SORT_SUBREDDITS, Constants.Analytics.Label.ALPHABETICALLY,
                                                    null).build());
            mSubredditSort = RedditContract.Subreddits.SORT_ALPHABETICALLY;
            Picasso.with(this).load(R.drawable.ic_action_sort_1_dark).into(mSortSubreddits);
            mSortSubreddits.setContentDescription(getString(R.string.sort_by_popularity));
        }

        getLoaderManager().restartLoader(Constants.Loader.LOADER_SUBREDDITS, null, this);
    }

    @OnClick(R.id.ib_refresh)
    protected void onRefreshSubreddits() {
        if (RedditLoginInformation.isLoggedIn()) {
            EasyTracker.getInstance(BaseFragmentActivityWithMenu.this)
                       .send(MapBuilder.createEvent(Constants.Analytics.Category.MENU_DRAWER_ACTION,
                                                    Constants.Analytics.Action.REFRESH_SUBREDDITS, Constants.Analytics.Label.LOGGED_IN,
                                                    null).build());
            // Since the user is logged in we can just run the task to update their subreddits.
            SubredditUtil.SetDefaultSubredditsTask defaultSubredditsTask = new SubredditUtil.SetDefaultSubredditsTask(this, true);
            defaultSubredditsTask.execute();
        } else {
            EasyTracker.getInstance(BaseFragmentActivityWithMenu.this)
                       .send(MapBuilder.createEvent(Constants.Analytics.Category.MENU_DRAWER_ACTION,
                                                    Constants.Analytics.Action.SHOW_SET_DEFAULTS, null, null).build());
            // If they aren't logged in, we want to make sure that they understand this will set the subreddits back to default.
            SetDefaultSubredditsDialogFragment fragment = SetDefaultSubredditsDialogFragment.newInstance();
            fragment.show(getFragmentManager(), Constants.Dialog.DIALOG_DEFAULT_SUBREDDITS);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mMenuDrawer != null) {
                    mMenuDrawer.toggleMenu();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setMenuDrawerContentView(int layoutResId) {
        initializeMenuDrawer();
        mMenuDrawer.setContentView(layoutResId);
        ButterKnife.inject(this);
        setupMenuDrawer();
    }

    private void initializeMenuDrawer() {
        mMenuDrawer = MenuDrawer.attach(this, MenuDrawer.Type.OVERLAY, Position.LEFT, MenuDrawer.MENU_DRAG_WINDOW);
        mMenuDrawer.setMenuView(R.layout.subreddit_menudrawer);
        mMenuDrawer.getMenuContainer().setBackgroundColor(0xf51b1b1b);
        mMenuDrawer.setSlideDrawable(R.drawable.ic_drawer);
        mMenuDrawer.setDrawerIndicatorEnabled(true);
        mMenuDrawer.setMenuSize(getResources().getDimensionPixelSize(R.dimen.menu_size));
    }

    private void setupMenuDrawer() {
        initializeSubredditFilter();
        initializeSubredditList();
        initializeLoaders();
    }

    private void initializeSubredditFilter() {
        if (mSubredditFilter != null) {
            mSubredditFilter.addTextChangedListener(mSubredditFilterWatcher);
        }
    }

    private void initializeSubredditList() {
        mSubredditAdapter = getSubredditMenuAdapter();
        if (mSubredditList != null) {
            mSubredditList.setAdapter(mSubredditAdapter);
            mSubredditList.setOnItemClickListener(mSubredditClickListener);
        }
    }

    private void initializeLoaders() {
        getLoaderManager().initLoader(Constants.Loader.LOADER_SUBREDDITS, null, this);
    }

    private SubredditMenuDrawerCursorAdapter getSubredditMenuAdapter() {
        if (mSubredditAdapter == null) {
            mSubredditAdapter = new SubredditMenuDrawerCursorAdapter(this, mSubredditActionListener);
        }

        return mSubredditAdapter;
    }

    protected void displaySubredditInfo(SubredditData subredditData) {
        AboutSubredditDialogFragment fragment = AboutSubredditDialogFragment.newInstance(subredditData);
        fragment.show(getFragmentManager(), Constants.Dialog.DIALOG_ABOUT_SUBREDDIT);
    }

    protected void forceRefreshCurrentSubreddit() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                RedditService.getPosts(BaseFragmentActivityWithMenu.this, mSelectedSubreddit, mAge, mCategory);
            }
        }).start();
    }

    protected void filterSubreddits(String filterText) {
        LoaderManager loaderManager = getLoaderManager();
        Bundle filterBundle = new Bundle();
        filterBundle.putString(Constants.Extra.EXTRA_QUERY, filterText);
        loaderManager.restartLoader(Constants.Loader.LOADER_SUBREDDITS, filterBundle, this);
    }

    protected abstract void subscribeToSubreddit(String subredditName);

    protected void showLogin() {
        // Only needs to be shown if they aren't currently logged in.
        if (!RedditLoginInformation.isLoggedIn()) {
            LoginDialogFragment loginFragment = LoginDialogFragment.newInstance();
            loginFragment.show(getFragmentManager(), Constants.Dialog.DIALOG_LOGIN);
        }
    }

    protected abstract void unsubscribeToSubreddit(String subredditName);

    protected void deleteSubreddit(String subredditName) {
        // If the user isn't logged in we don't care about subscribing/unsubscribing
        if (!RedditLoginInformation.isLoggedIn()) {
            RedditService.unsubscribe(this, subredditName);
        }
        ContentResolver resolver = getContentResolver();
        resolver.delete(RedditContract.Subreddits.CONTENT_URI, RedditContract.SubredditColumns.NAME + " = ?",
                        new String[] { subredditName });
    }

    /**
     * Take care of popping the fragment back stack or finishing the activity
     * as appropriate.
     */
    @Override
    public void onBackPressed() {
        long currentTime = new Date().getTime();

        // If the back button has been double clicked, finish the activity.
        if (currentTime - Constants.DOUBLE_CLICK_TIMEOUT <= mLastTimeBackPressed) {
            ExitDialogFragment.newInstance().show(getFragmentManager(), Constants.Dialog.DIALOG_LOGIN);
        } else {
            mLastTimeBackPressed = currentTime;
        }

        // If the menu drawer is open, close it. Otherwise go about the normal business.
        if (mMenuDrawer != null && mMenuDrawer.isMenuVisible()) {
            mMenuDrawer.closeMenu();
            return;
        } else if (!getFragmentManager().popBackStackImmediate()) {
            // Otherwise, let's pop the back stack. If that returns false then there is nothing left in the backstack so let's see if they want to exit.
            ExitDialogFragment.newInstance().show(getFragmentManager(), Constants.Dialog.DIALOG_LOGIN);
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
            case Constants.Loader.LOADER_SUBREDDITS:
                String selection = null;
                String[] selectionArgs = null;

                if (args != null && args.containsKey(Constants.Extra.EXTRA_QUERY)) {
                    String query = args.getString(Constants.Extra.EXTRA_QUERY);

                    if (Strings.notEmpty(query)) {
                        selection = RedditContract.SubredditColumns.DISPLAY_NAME + " LIKE ?";
                        selectionArgs = new String[] { "%" + query + "%" };
                    }
                }
                return new CursorLoader(this, RedditContract.Subreddits.CONTENT_URI, RedditContract.Subreddits.SUBREDDITS_PROJECTION,
                                        selection, selectionArgs, mSubredditSort);
        }

        return null;
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
            case Constants.Loader.LOADER_SUBREDDITS:
                mSubredditAdapter.swapCursor(data);
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
            case Constants.Loader.LOADER_SUBREDDITS:
                mSubredditAdapter.swapCursor(null);
                break;
        }
    }

    @Override
    public void onSetDefaultSubreddits() {
        EasyTracker.getInstance(BaseFragmentActivityWithMenu.this)
                   .send(MapBuilder.createEvent(Constants.Analytics.Category.MENU_DRAWER_ACTION,
                                                Constants.Analytics.Action.REFRESH_SUBREDDITS, Constants.Analytics.Label.NOT_LOGGED_IN,
                                                null).build());
        SubredditUtil.SetDefaultSubredditsTask defaultSubredditsTask = new SubredditUtil.SetDefaultSubredditsTask(this, true);
        defaultSubredditsTask.execute();
    }

    @Override
    public void onAddSubreddit(String subreddit) {
        if (Strings.notEmpty(subreddit)) {
            EasyTracker.getInstance(BaseFragmentActivityWithMenu.this)
                       .send(
                           MapBuilder.createEvent(Constants.Analytics.Category.MENU_DRAWER_ACTION, Constants.Analytics.Action.ADD_SUBREDDIT,
                                                  subreddit, null).build()
                            );
            RedditService.aboutSubreddit(this, subreddit);
            loadSubredditFromMenu(subreddit);
            closeMenuDrawerIfNeeded();
        }
    }

    protected abstract void loadSubredditFromMenu(String subreddit);

    protected void closeMenuDrawerIfNeeded() {
        if (mMenuDrawer != null && mMenuDrawer.isMenuVisible()) {
            mMenuDrawer.closeMenu();
        }
    }

    @Override
    public Age getAge() {
        return mAge;
    }

    @Override
    public void setAge(Age age) {
        mAge = age;
    }

    @Override
    public Category getCategory() {
        return mCategory;
    }

    @Override
    public void setCategory(Category category) {
        mCategory = category;
    }

    @Override
    public String getSubreddit() {
        return mSelectedSubreddit;
    }

    @Override
    public void setSubreddit(String subreddit) {
        mSelectedSubreddit = subreddit;
    }
}
