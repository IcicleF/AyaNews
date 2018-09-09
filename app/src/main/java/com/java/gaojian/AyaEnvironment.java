package com.java.gaojian;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class AyaEnvironment {

    protected static boolean toClearWebViewCache = false;

    protected static SharedPreferences prefs;
    protected static SharedPreferences.Editor prefs_editor;

    static class RssFeed {
        public String name;
        public String source;
        public String url;
        public String encoding;
        public boolean active;
    }

    protected static List<RssFeed> rssFeedList = new ArrayList<>();
    protected static List<AyaNewsEntry> entryList = new LinkedList<>();
    protected static Set<String> favSet = new TreeSet<>();

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

    /*
     * Must be called ONLY ONCE at the VERY BEGINNING.
     */
    protected static void loadRSSFeedList(Context context) {
        if (context == null)
            return;

        prefs = context.getSharedPreferences("ayaPrefs", Context.MODE_PRIVATE);

        String[] nameArr = context.getResources().getStringArray(R.array.rss_name_array);
        String[] urlArr = context.getResources().getStringArray(R.array.rss_url_array);
        String[] encodingArr = context.getResources().getStringArray(R.array.rss_encoding_array);

        Log.d("ayaDeb", "AyaEnvironment.loadRSSFeedList" + nameArr.length);

        rssFeedList.clear();
        for (int i = 0; i < nameArr.length; ++i) {
            RssFeed rss = new RssFeed();
            rss.name = nameArr[i];
            rss.source = rss.name.substring(0, rss.name.indexOf(' '));
            rss.encoding = encodingArr[i];
            rss.url = urlArr[i];

            String active = prefs.getString(rss.name, null);
            if (active == null)
                rss.active = rss.name.contains(context.getResources().getString(R.string.channel_domestic));
            else
                rss.active = Boolean.parseBoolean(active);
            rssFeedList.add(rss);
        }
    }

    /*
     * Must be called ONLY ONCE at the VERY BEGINNING.
     */
    protected static void loadFavorites(Context context) {
        if (context == null || !favSet.isEmpty())
            return;

        FileInputStream fis;
        try {
            fis = context.openFileInput(context.getResources().getString(R.string.path_fav_list));

            XmlPullParserFactory xppf = XmlPullParserFactory.newInstance();
            XmlPullParser parser = xppf.newPullParser();
            parser.setInput(fis, "UTF-8");

            int event = parser.getEventType();
            String uid, name;
            while (event != XmlPullParser.END_DOCUMENT) {
                switch (event) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        name = parser.getName();
                        if (name.equals("entry")) {
                            uid = parser.getAttributeValue(null, "uid");
                            favSet.add(uid);
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                    default:
                        break;
                }
                event = parser.next();
            }
        }
        catch (FileNotFoundException e) {
            Log.d("ayaDeb", "AyaEnvironment.loadFavorites: seems like first run");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Must be called ONLY ONCE at the VERY BEGINNING.
     */
    protected static void loadNewsList(Context context) {
        if (context == null || !entryList.isEmpty())
            return;

        FileInputStream fis;
        try {
            fis = context.openFileInput(context.getResources().getString(R.string.path_news_list));

            XmlPullParserFactory xppf = XmlPullParserFactory.newInstance();
            XmlPullParser parser = xppf.newPullParser();
            parser.setInput(fis, "UTF-8");

            int event = parser.getEventType();
            AyaNewsEntry entry;
            String name;
            while (event != XmlPullParser.END_DOCUMENT) {
                switch (event) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        name = parser.getName();
                        if (name.equals("entry")) {
                            entry = new AyaNewsEntry();
                            entry.uid = parser.getAttributeValue(null, "uid");
                            entry.title = parser.getAttributeValue(null, "title");
                            entry.pubDate = parser.getAttributeValue(null, "pubDate");
                            entry.desc = parser.getAttributeValue(null, "desc");
                            entry.url = parser.getAttributeValue(null, "url");
                            entry.source = parser.getAttributeValue(null, "source");

                            String viewsStr = parser.getAttributeValue(null, "views");
                            entry.views = Integer.parseInt(viewsStr);

                            entryList.add(entry);
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                    default:
                        break;
                }
                event = parser.next();
            }
        }
        catch (FileNotFoundException e) {
            Log.d("ayaDeb", "AyaEnvironment.loadNewsList: seems like first run");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected static void saveNewsList(Context context) {
        if (context == null)
            return;

        try {
            Log.d("ayaDeb ", "AyaEnvironment.saveNewsList: activated: " + entryList.size());

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.newDocument();

            doc.setXmlStandalone(true);
            Element root = doc.createElement("newslist");
            for (AyaNewsEntry entry : entryList) {
                Element entryNode = doc.createElement("entry");
                entryNode.setAttribute("uid", entry.uid);
                entryNode.setAttribute("title", entry.title);
                entryNode.setAttribute("desc", entry.desc);
                entryNode.setAttribute("pubDate", entry.pubDate);
                entryNode.setAttribute("url", entry.url);
                entryNode.setAttribute("source", entry.source);
                entryNode.setAttribute("views", "" + entry.views);
                root.appendChild(entryNode);
            }
            doc.appendChild(root);

            TransformerFactory trf = TransformerFactory.newInstance();
            Transformer tr = trf.newTransformer();
            tr.setOutputProperty(OutputKeys.INDENT, "yes");

            FileOutputStream fos = context.openFileOutput(context.getResources().getString(R.string.path_news_list), Context.MODE_PRIVATE);
            PrintStream ps = new PrintStream(fos);
            tr.transform(new DOMSource(doc), new StreamResult(ps));
            ps.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected static void saveFavorites(Context context) {
        if (context == null)
            return;

        try {
            Log.d("ayaDeb ", "AyaEnvironment.saveFavorites: activated: " + favSet.size());

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.newDocument();

            doc.setXmlStandalone(true);
            Element root = doc.createElement("favlist");
            for (String uid : favSet) {
                Element entryNode = doc.createElement("entry");
                entryNode.setAttribute("uid", uid);
                root.appendChild(entryNode);
            }
            doc.appendChild(root);

            TransformerFactory trf = TransformerFactory.newInstance();
            Transformer tr = trf.newTransformer();
            tr.setOutputProperty(OutputKeys.INDENT, "yes");

            FileOutputStream fos = context.openFileOutput(context.getResources().getString(R.string.path_fav_list), Context.MODE_PRIVATE);
            PrintStream ps = new PrintStream(fos);
            tr.transform(new DOMSource(doc), new StreamResult(ps));
            ps.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected static void saveRssPrefs() {
        prefs_editor = prefs.edit();
        for (RssFeed rss : rssFeedList)
            prefs_editor.putString(rss.name, "" + rss.active);
        prefs_editor.apply();
    }

    protected static boolean addToFavorites(AyaNewsEntry entry) {
        if (favSet.contains(entry.uid))
            return false;
        favSet.add(entry.uid);
        return true;
    }

    protected static boolean removeFromFavorites(AyaNewsEntry entry) {
        if (favSet.contains(entry.uid)) {
            favSet.remove(entry.uid);
            return true;
        }
        return false;
    }

    protected static void setViewed(AyaNewsEntry entry) {
        for (AyaNewsEntry existed : entryList)
            if (existed.uid.equals(entry.uid))
                ++existed.views;
    }

    @Nullable
    protected static AyaNewsEntry findEntry(String uid) {
        for (AyaNewsEntry entry : entryList)
            if (entry.uid.equals(uid))
                return entry;
        return null;
    }
}
