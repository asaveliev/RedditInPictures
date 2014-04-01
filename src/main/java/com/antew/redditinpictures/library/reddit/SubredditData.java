package com.antew.redditinpictures.library.reddit;

import android.database.Cursor;
import com.antew.redditinpictures.sqlite.RedditContract;

public class SubredditData {
    private String  display_name;
    private String  description;
    private String  name;
    private long    created;
    private String  url;
    private String  title;
    private long    created_utc;
    private String  public_description;
    private int[]   header_size;
    private int     accounts_active;
    private boolean over18;
    private int     subscribers;
    private String  header_title;
    private String  id;
    private String  header_img;
    private int     priority;
    private boolean user_is_subscriber;

    private SubredditData() {

    }

    public SubredditData(String display_name) {
        this(display_name, 0);
    }

    public SubredditData(String display_name, int priority) {
        this.display_name = display_name;
        this.priority = priority;
    }

    public static SubredditData fromProjection(Cursor cursor) {
        SubredditData subredditData = new SubredditData();
        subredditData.display_name = cursor.getString(cursor.getColumnIndex(RedditContract.Subreddits.DISPLAY_NAME));
        subredditData.name = cursor.getString(cursor.getColumnIndex(RedditContract.Subreddits.NAME));
        subredditData.user_is_subscriber = cursor.getInt(cursor.getColumnIndex(RedditContract.Subreddits.USER_IS_SUBSCRIBER)) == 1;
        return subredditData;
    }

    public String getDisplay_name() {
        return display_name;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public long getCreated() {
        return created;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public long getCreated_utc() {
        return created_utc;
    }

    public String getPublic_description() {
        return public_description;
    }

    public int[] getHeader_size() {
        return header_size;
    }

    public boolean isOver18() {
        return over18;
    }

    public int getSubscribers() {
        return subscribers;
    }

    public String getHeader_title() {
        return header_title;
    }

    public String getId() {
        return id;
    }

    public String getHeader_img() {
        return header_img;
    }

    public int getAccountsActive() {
        return accounts_active;
    }

    public int getPriority() { return priority; }

    public boolean getUserIsSubscriber() { return user_is_subscriber; }
}