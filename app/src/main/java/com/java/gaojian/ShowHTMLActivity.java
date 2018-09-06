package com.java.gaojian;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.scwang.smartrefresh.header.MaterialHeader;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.constant.RefreshState;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;

public class ShowHTMLActivity extends AppCompatActivity {

    private String url = "https://en.touhouwiki.net/wiki/Aya_Shameimaru";  //"my" page :)

    private SmartRefreshLayout mSwipeRefresher;
    private WebView mWebView;
    private LoadingIndicator loadingIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_html);

        this.setTitle(getResources().getString(R.string.title_activity_show_html));

        Intent intent = getIntent();
        if (intent != null) {
            String uid = intent.getStringExtra("uid");
            AyaNewsEntry entry = AyaEnvironment.findEntry(uid);
            if (entry != null)
                url = entry.url;
        }
        //Toast.makeText(this, url, Toast.LENGTH_SHORT).show();

        loadingIndicator = LoadingIndicator.buildInst(this);
        loadingIndicator.show();

        mWebView = (WebView) findViewById(R.id.web_view_showhtml);
        mWebView.setWebViewClient(webViewClient);
        //mWebView.clearCache(true);

        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        if (AyaEnvironment.isNetworkConnected(this))
            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        else
            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

        mWebView.loadUrl(url);

        mSwipeRefresher = (SmartRefreshLayout) findViewById(R.id.web_view_swipe_refresh);
        mSwipeRefresher.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(RefreshLayout refreshLayout) {
                mWebView.reload();
            }
        });
        mSwipeRefresher.setRefreshHeader(new MaterialHeader(this)
                .setColorSchemeColors(
                        0xFF000000 + getResources().getColor(R.color.colorPrimary)
                ));
    }

    private WebViewClient webViewClient = new WebViewClient() {
        @Override
        public void onPageFinished(WebView view, String url) {
            loadingIndicator.dismiss();
            if (mSwipeRefresher.getState() == RefreshState.Refreshing)
                mSwipeRefresher.finishRefresh(0);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            loadingIndicator.show();
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return false;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            switch (errorCode) {
                case WebViewClient.ERROR_HOST_LOOKUP:
                case WebViewClient.ERROR_CONNECT:
                case WebViewClient.ERROR_TIMEOUT:
                case WebViewClient.ERROR_FAILED_SSL_HANDSHAKE:
                    Toast.makeText(ShowHTMLActivity.this, getResources().getString(R.string.err_no_network), Toast.LENGTH_SHORT).show();
                    view.loadData("", "text/html", "UTF-8");
                    break;
                default:
                    Toast.makeText(ShowHTMLActivity.this, "Unexpected errorCode: " + errorCode, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.show_html_righttop_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.show_html_add_to_favorite:

                return true;
            default:
                return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mWebView.destroy();
        mWebView = null;
    }
}
