package com.java.gaojian;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.scwang.smartrefresh.header.MaterialHeader;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.constant.RefreshState;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ShowHTMLActivity extends AppCompatActivity {

    public static final int MAX_TRIES = 100;
    public static final int TEXT_BUBBLE_HEIGHT = 300;

    private String url = "https://en.touhouwiki.net/wiki/Aya_Shameimaru";  //"my" page :)

    private SmartRefreshLayout mSwipeRefresher;
    private WebView mWebView;
    private LoadingIndicator loadingIndicator;

    private AyaNewsEntry entry;
    private boolean isCalledFromFavoirtes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP)
            WebView.enableSlowWholeDocumentDraw();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_html);

        this.setTitle(getResources().getString(R.string.title_activity_show_html));

        Intent intent = getIntent();
        if (intent != null) {
            String uid = intent.getStringExtra("uid");
            isCalledFromFavoirtes = intent.getBooleanExtra("isCalledFromFavorites", false);
            entry = AyaEnvironment.findEntry(uid);
            if (entry != null)
                url = entry.url;
        }
        //Toast.makeText(this, url, Toast.LENGTH_SHORT).show();

        loadingIndicator = LoadingIndicator.buildInst(this);
        //loadingIndicator.show();

        mWebView = (WebView) findViewById(R.id.web_view_showhtml);
        mWebView.setWebViewClient(webViewClient);
        if (AyaEnvironment.toClearWebViewCache) {
            mWebView.clearCache(true);
            mWebView.clearFormData();
            mWebView.clearHistory();

            AyaEnvironment.toClearWebViewCache = false;
        }

        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
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
            if (entry.source.contains("Tencent")) {
                String js =
                        "javascript:function fucktencent(){" +
                                "var bar1=document.getElementsByClassName('jsx-129655388 contentbar');" +
                                "for(var i=0;i<bar1.length;++i){bar1[i].remove();}" +
                                "var bar2=document.getElementsByClassName('nfooter-nav  comm');" +
                                "for(var i=0;i<bar2.length;++i){bar2[i].remove();}" +
                                "}";
                view.loadUrl(js);
                view.loadUrl("javascript:fucktencent();");
            }
            else {
                String js =
                        "javascript:function fucksina(){" +
                                "var bar1=document.getElementsByClassName('fl_words rf j_cmnt_bottom');" +
                                "for(var i=0;i<bar1.length;++i){bar1[i].remove();}" +
                                "var bar2=document.getElementsByClassName('specSlide2Wrap');" +
                                "for(var i=0;i<bar2.length;++i){bar2[i].remove();}" +
                                "var bar3=document.getElementsByClassName('s_card j_article_relevent j_wbrecommend_wrap');" +
                                "for(var i=0;i<bar3.length;++i){bar3[i].remove();}" +
                                "var bar4=document.getElementsByClassName('s_card j_cmt_wrap z_c2');" +
                                "for(var i=0;i<bar4.length;++i){bar4[i].remove();}" +
                                "var bar5=document.getElementsByClassName('s_card j_article_relevent_news z_c2');" +
                                "for(var i=0;i<bar5.length;++i){bar5[i].remove();}" +
                                "}";
                view.loadUrl(js);
                view.loadUrl("javascript:fucksina();");
            }
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

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            return super.shouldInterceptRequest(view, request);
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
        super.onCreateOptionsMenu(menu);

        menu.add(R.string.title_share_gist);
        menu.add(R.string.title_open_browser);
        menu.add(R.string.title_share);
        if (isCalledFromFavoirtes)
            menu.add(R.string.title_remove_from_favorites);
        else
            menu.add(R.string.title_add_to_favorites);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String title = item.getTitle().toString();
        if (title.equals(getResources().getString(R.string.title_share_gist))) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, entry.getGist());
            startActivity(Intent.createChooser(intent, getResources().getString(R.string.title_share_gist)));
        }
        else if (title.equals(getResources().getString(R.string.title_open_browser))) {
            Uri uri = Uri.parse(entry.url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            if (intent.resolveActivity(getPackageManager()) != null)
                startActivity(intent);
            else
                Toast.makeText(this, R.string.err_malformed_url, Toast.LENGTH_SHORT).show();
        }
        else if (title.equals(getResources().getString(R.string.title_share))) {
            /*
             * Reference List:
             *
             *  https://www.jianshu.com/p/8b1bcbbae4e7
             *  https://www.cnblogs.com/Sharley/p/7942142.html
             */

            mWebView.measure(
                    View.MeasureSpec.makeMeasureSpec(
                            View.MeasureSpec.UNSPECIFIED,
                            View.MeasureSpec.UNSPECIFIED
                    ),
                    View.MeasureSpec.makeMeasureSpec(
                            0,
                            View.MeasureSpec.UNSPECIFIED
                    ));
            int pageW = mWebView.getMeasuredWidth(), pageH = mWebView.getMeasuredHeight();

            mWebView.layout(0, 0, pageW, pageH);
            mWebView.setDrawingCacheEnabled(true);
            mWebView.buildDrawingCache();

            Bitmap longImage = Bitmap.createBitmap(pageW, pageH, Bitmap.Config.ARGB_8888);

            Canvas canvas = new Canvas(longImage);
            Paint paint = new Paint();
            canvas.drawBitmap(longImage, 0, pageH, paint);
            mWebView.draw(canvas);

            String galleryPath = Environment.getExternalStorageDirectory()
                    + File.separator + Environment.DIRECTORY_PICTURES;
            String filePath = null;
            File file = null;
            FileOutputStream fos = null;
            try {
                String fileName = "ayanews-" + entry.uid;
                file = new File(galleryPath, fileName + ".jpg");
                if (file.exists()) {
                    fileName = fileName + "-";
                    boolean flag = false;
                    for (int i = 0; i < MAX_TRIES; ++i) {
                        file = new File(galleryPath, fileName + i +  ".jpg");
                        if (!file.exists()) {
                            flag = true;
                            break;
                        }
                    }
                    if (!flag)
                        throw new Exception(getResources().getString(R.string.err_cannot_save_img));
                }
                filePath = file.toString();
                fos = new FileOutputStream(filePath);

                if (fos != null) {
                    longImage.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                    fos.close();

                    MediaStore.Images.Media.insertImage(this.getContentResolver(),
                            longImage, filePath, null);
                    Uri uri;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        uri = FileProvider.getUriForFile(this, "com.java.gaojian.fileprovider", file);
                    else
                        uri = Uri.fromFile(file);

                    Intent updateAlbumIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    updateAlbumIntent.setData(uri);
                    this.sendBroadcast(updateAlbumIntent);

                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("image/*");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.title_share)));
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            if (isCalledFromFavoirtes) {
                boolean succ = AyaEnvironment.removeFromFavorites(entry);
                if (succ) {
                    setResult(FavoriteListActivity.REQUEST_SHOW_HTML_SUCCRET);
                    finish();
                }
                else
                    Toast.makeText(this, R.string.err_fav_remove_failure, Toast.LENGTH_SHORT).show();
            }
            else {
                boolean succ = AyaEnvironment.addToFavorites(entry);
                Toast.makeText(this,
                        (succ ? R.string.info_succ_fav : R.string.warn_existed_fav),
                        Toast.LENGTH_SHORT).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mWebView.destroy();
        mWebView = null;
    }
}
