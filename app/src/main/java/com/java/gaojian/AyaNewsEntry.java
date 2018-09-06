package com.java.gaojian;

import android.support.annotation.NonNull;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.SimpleTimeZone;

class AyaNewsEntry implements Comparable<AyaNewsEntry> {
    public String uid;
    public String title;
    public String pubDate;
    public String desc;
    public String url;
    public String source;

    @Override
    public int compareTo(@NonNull AyaNewsEntry o) {
        return o.pubDate.compareTo(this.pubDate);
    }

    /*
     * Compute uid and a new url from this.url & this.source.
     */
    public void refactor() {
        int len = url.length();
        if (source.contains("Tencent")) {
            uid = "t" + url.substring(len - 19, len - 11) + url.substring(len - 10, len - 4);
            url = "https://xw.qq.com/news/" + uid.substring(1);
        }
        else if (source.contains("Sina")) {
            url = url.substring(url.indexOf('=') + 1);
            uid = "s" + url.substring(26, 36).replace("-", "")
                    + url.substring(42, 56);
        }
        else {
            url = "INVALID";
            uid = "INVALID";
        }
    }

    public static String convertToTencentDateFormat(String stdDateFormatStr) {
        SimpleDateFormat stdFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.US);
        try {
            Date date = stdFormat.parse(stdDateFormatStr);
            SimpleDateFormat tencentFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            return tencentFormat.format(date);
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String convertToStdDateFormat(String tencentDateFormatStr) {
        SimpleDateFormat stdFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        try {
            Date date = stdFormat.parse(tencentDateFormatStr);
            SimpleDateFormat tencentFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.US);

            SimpleTimeZone aZone = new SimpleTimeZone(8, "GMT");
            tencentFormat.setTimeZone(aZone);

            return tencentFormat.format(date);
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
}
