package com.java.gaojian;

import android.support.annotation.NonNull;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class AyaNewsEntry implements Comparable<AyaNewsEntry> {

    private static Pattern sinaPattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})/doc-([a-z0-9]{15}).shtml");

    public String uid;
    public String title;
    public String pubDate;
    public String desc;
    public String url;
    public String source;

    @Override
    public int compareTo(@NonNull AyaNewsEntry o) {
        if (!(o.pubDate.equals(this.pubDate)))
            return o.pubDate.compareTo(this.pubDate);
        return this.title.compareTo(o.title);
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

            Matcher m = sinaPattern.matcher(url);
            if (m.find())
                uid = "s" + m.group(1).replace("=", "") + m.group(2);
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
