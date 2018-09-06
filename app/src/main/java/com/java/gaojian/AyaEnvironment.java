package com.java.gaojian;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class AyaEnvironment {
    protected static boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null)
                return false;
            NetworkInfo ni = cm.getActiveNetworkInfo();
            if (ni != null) {
                return ni.isAvailable();
            }
        }
        return false;
    }

    protected static SharedPreferences prefs;
    protected static SharedPreferences.Editor prefs_editor;

    static class RssFeed {
        public String name;
        public String source;
        public String url;
        public String encoding;
        public boolean active;
    }

    protected static List<RssFeed> rssFeedList = new ArrayList<RssFeed>();
    protected static List<AyaNewsEntry> entryList = new LinkedList<AyaNewsEntry>();

    /*
     * Must be called ONCE at the VERY BEGINNING.
     */
    protected static void loadRSSFeedList(Context context) {
        if (context == null)
            return;

        prefs = context.getSharedPreferences("ayaPrefs", Context.MODE_PRIVATE);
        prefs_editor = prefs.edit();

        String[] nameArr = context.getResources().getStringArray(R.array.rss_name_array);
        String[] urlArr = context.getResources().getStringArray(R.array.rss_url_array);
        String[] encodingArr = context.getResources().getStringArray(R.array.rss_encoding_array);

        if (nameArr.length != urlArr.length) {
            Log.wtf("loadRSSFeedList", "Aya is angry");
            return;
        }
        Log.d("loadRSSFeedList", "" + nameArr.length);

        rssFeedList.clear();
        for (int i = 0; i < nameArr.length; ++i) {
            RssFeed rss = new RssFeed();
            rss.name = nameArr[i];
            rss.source = rss.name.substring(0, rss.name.indexOf(' '));
            rss.encoding = encodingArr[i];
            rss.url = urlArr[i];

            /*
             * @TODO  Change this to read SharedPreference
             */
            String active = prefs.getString(rss.name, null);
            if (active == null)
                rss.active = (i == 0 || i == 3);            // Domestic news
            else
                rss.active = Boolean.parseBoolean(active);
            rssFeedList.add(rss);
        }
    }

    protected static void savePreferences() {
        for (RssFeed rss : rssFeedList)
            prefs_editor.putString(rss.name, "" + rss.active);
        prefs_editor.commit();
    }

    @Nullable
    protected static RssFeed defaultRSSFeed() {
        if (rssFeedList.isEmpty())
            return null;
        return rssFeedList.get(0);
    }

    @Nullable
    protected static AyaNewsEntry findEntry(String uid) {
        for (AyaNewsEntry entry : entryList)
            if (entry.uid.equals(uid))
                return entry;
        return null;
    }
}
