package com.java.gaojian;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.RelativeLayout;

import mehdi.sakout.aboutpage.AboutPage;
import mehdi.sakout.aboutpage.Element;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        setTitle(getResources().getString(R.string.title_about));

        /*
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
        */

        View aboutPage = new AboutPage(this)
                .isRTL(false)
                .setImage(R.drawable.about_aya)
                .setDescription(getResources().getString(R.string.about_text))
                .addItem(new Element()
                        .setTitle(getResources().getString(R.string.about_project))
                        .setGravity(Gravity.CENTER))
                .addItem(new Element()
                        .setTitle(getResources().getString(R.string.about_author))
                        .setGravity(Gravity.CENTER))
                .create();

        RelativeLayout layout = (RelativeLayout) findViewById(R.id.about_layout);
        layout.addView(aboutPage);
    }
}
