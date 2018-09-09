package com.java.gaojian;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import com.scwang.smartrefresh.layout.constant.RefreshState;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private LoadingIndicator loadingIndicator;

    private NewsListFragment homeFrag;
    private RecommendListFragment recommendFrag;
    private MySpaceFragment mySpaceFrag;

    private ViewPager mViewPager;
    private BottomNavigationView mNavigation;

    private ViewPagerFragmentAdapter mViewPagerAdapter;
    private FragmentManager mFragManager;
    private List<Fragment> mFragList = new ArrayList<Fragment>();

    public String xml;
    public List<AyaNewsEntry> dataset;

    public static final int REFRESH_VIEWPAGER         = 0x1;
    public static final int REFRESH_LIST              = 0x2;
    public static final int DISMISS_LOADING_INDICATOR = 0x4;
    public static final int SHOW_MESSAGE              = 0x8;
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            String info;
            int opcode = msg.what;
            if ((opcode & REFRESH_LIST) != 0) {
                Collections.sort(dataset);
                homeFrag.fetchData(dataset);
                opcode = opcode & ~REFRESH_LIST;
            }
            if ((opcode & DISMISS_LOADING_INDICATOR) != 0) {
                loadingIndicator.dismiss();
                if (homeFrag.mSwipeRefresher.getState() == RefreshState.Refreshing)
                    homeFrag.mSwipeRefresher.finishRefresh(0);
                opcode = opcode & ~DISMISS_LOADING_INDICATOR;
            }
            if ((opcode & SHOW_MESSAGE) != 0) {
                info = (String) msg.obj;
                Toast.makeText(MainActivity.this, info, Toast.LENGTH_SHORT).show();
                opcode = opcode & ~SHOW_MESSAGE;
            }
            if ((opcode & REFRESH_VIEWPAGER) != 0) {
                mViewPagerAdapter.notifyDataSetChanged();
                opcode = opcode & ~REFRESH_VIEWPAGER;
            }

            if (opcode != 0)
                Toast.makeText(MainActivity.this, R.string.err_unexpected, Toast.LENGTH_SHORT).show();
        }
    };

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Thread netConn;
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    MainActivity.this.setTitle(getResources().getString(R.string.title_home));
                    if (mViewPager.getCurrentItem() != 0)
                        mViewPager.setCurrentItem(0);
                    return true;
                case R.id.navigation_recommend:
                    MainActivity.this.setTitle(getResources().getString(R.string.title_recommend));
                    if (mViewPager.getCurrentItem() != 1)
                        mViewPager.setCurrentItem(1);
                    return true;
                case R.id.navigation_myspace:
                    MainActivity.this.setTitle(getResources().getString(R.string.title_myspace));
                    if (mViewPager.getCurrentItem() != 2)
                        mViewPager.setCurrentItem(2);
                    return true;
            }
            return false;
        }
    };

    private Thread netConn;
    private boolean forTest = false;
    private class AyaThreadedRSSFetcher implements Runnable {

        public AyaThreadedRSSFetcher() { }

        @Override
        public void run() {
            if (!AyaEnvironment.isNetworkConnected(MainActivity.this)) {
                Message message = new Message();
                message.what = SHOW_MESSAGE;
                message.obj = getResources().getString(R.string.err_no_network);
                handler.sendMessage(message);

                homeFrag.fetchData(null);
                loadingIndicator.dismiss();
                if (homeFrag.mSwipeRefresher.getState() == RefreshState.Refreshing)
                    homeFrag.mSwipeRefresher.finishRefresh(0);

                return;
            }

            dataset = new ArrayList<AyaNewsEntry>();
            boolean failure = false;

            OkHttpClient client = new OkHttpClient();
            List<AyaEnvironment.RssFeed> rssList = AyaEnvironment.rssFeedList;
            for (AyaEnvironment.RssFeed rss : rssList) {
                if (!rss.active)
                    continue;
                Log.d("ayaDeb", "MainActivity.AyaThreadedRSSFetcher.run: " + rss.source + " " + rss.name);
                Request request = new Request.Builder().url(rss.url).build();
                try {
                    Response response = client.newCall(request).execute();
                    if (!response.isSuccessful())
                        throw new IOException("Unexpected response code " + response);

                    byte[] xml = response.body().bytes();

                    XmlPullParserFactory xppf = XmlPullParserFactory.newInstance();
                    XmlPullParser parser = xppf.newPullParser();

                    InputStream is = new ByteArrayInputStream(xml);
                    parser.setInput(is, rss.encoding);

                    int event = parser.getEventType();
                    boolean isInItem = false;
                    AyaNewsEntry entry = null;
                    String name;
                    while (event != XmlPullParser.END_DOCUMENT) {
                        String nodeName = parser.getName();
                        switch (event) {
                            case XmlPullParser.START_DOCUMENT:
                                break;
                            case XmlPullParser.START_TAG:
                                name = parser.getName();
                                if (name.equals("item")) {
                                    isInItem = true;
                                    entry = new AyaNewsEntry();
                                    entry.source = rss.name;
                                    entry.views = 0;
                                }
                                else if (isInItem) {
                                    if (name.equals("title")) {
                                        entry.title = parser.nextText().trim();
                                    }
                                    else if (name.equals("description"))
                                        entry.desc = parser.nextText();
                                    else if (name.equals("pubDate")) {
                                        entry.pubDate = parser.nextText();
                                        if (entry.source.contains("Sina"))
                                            entry.pubDate = AyaNewsEntry.convertToTencentDateFormat(entry.pubDate);
                                    }
                                    else if (name.equals("link")) {
                                        entry.url = parser.nextText();
                                        entry.refactor();
                                    }
                                }
                                break;
                            case XmlPullParser.END_TAG:
                                name = parser.getName();
                                if (name.equalsIgnoreCase("item")) {
                                    isInItem = false;
                                    dataset.add(entry);
                                }
                                break;
                            default:
                                break;
                        }
                        event = parser.next();
                    }
                }
                catch (SocketTimeoutException e) {
                    failure = true;
                }
                catch (Exception e) {
                    e.printStackTrace();
                    failure = true;
                }
                Log.d("ayaDeb", "MainActivity.AyaThreadedRSSFetcher.run: " + dataset.size() + " " + failure);
            }
            if (failure) {
                Message message = new Message();
                message.what = SHOW_MESSAGE | DISMISS_LOADING_INDICATOR;
                message.obj = getResources().getString(R.string.err_rss_failure);
                handler.sendMessage(message);
                //return;
            }
            handler.sendEmptyMessage(REFRESH_LIST | DISMISS_LOADING_INDICATOR);
        }
    }

    protected void refetchRSS(boolean showLoadingIndicator) {
        if (showLoadingIndicator) {
            loadingIndicator = LoadingIndicator.buildInst(this);
            loadingIndicator.show();
        }

        netConn = new Thread(new AyaThreadedRSSFetcher());
        netConn.start();
    }

    class ViewPagetOnPagerChangedLisenter implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

        @Override
        public void onPageScrollStateChanged(int state) { }

        @Override
        public void onPageSelected(int position) {
            mNavigation.setSelectedItemId(mNavigation.getMenu().getItem(position).getItemId());
            if (position == 1)
                recommendFrag.startRefreshList();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AyaEnvironment.loadRSSFeedList(this);
        AyaEnvironment.loadFavorites(this);

        this.setTitle(getResources().getString(R.string.title_home));
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mViewPager.setOffscreenPageLimit(2);

        mNavigation = (BottomNavigationView) findViewById(R.id.navigation);
        mNavigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        mFragManager = getSupportFragmentManager();

        mFragList.add(homeFrag = new NewsListFragment());
        mFragList.add(recommendFrag = new RecommendListFragment());
        mFragList.add(mySpaceFrag = new MySpaceFragment());
        mViewPagerAdapter = new ViewPagerFragmentAdapter(mFragManager, mFragList);

        homeFrag.setMainActivity(this);
        recommendFrag.setMainActivity(this);
        mySpaceFrag.setMainActivity(this);

        mViewPager.addOnPageChangeListener(new ViewPagetOnPagerChangedLisenter());
        mViewPager.setAdapter(mViewPagerAdapter);
        mViewPager.setCurrentItem(0);
    }

    @Override
    public void onStop() {
        AyaEnvironment.saveRssPrefs();
        AyaEnvironment.saveNewsList(this);
        AyaEnvironment.saveFavorites(this);
        super.onStop();
    }

    public void sendMessage(Message message) {
        handler.sendMessage(message);
    }

    public void sendEmptyMessage(int what) {
        handler.sendEmptyMessage(what);
    }
}
