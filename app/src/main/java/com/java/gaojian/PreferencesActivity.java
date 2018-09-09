package com.java.gaojian;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class PreferencesActivity extends AppCompatActivity {

    private Button mClearCacheButton;
    private Button mClearHistoryButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        this.setTitle(R.string.title_preferences);

        mClearCacheButton = (Button) findViewById(R.id.prefs_clear_webcache);
        mClearCacheButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(PreferencesActivity.this).setTitle(getResources().getString(R.string.dlg_ask_clear_cache))
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton(getResources().getString(R.string.dlg_yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                AyaEnvironment.toClearWebViewCache = true;
                                Toast.makeText(PreferencesActivity.this, R.string.dlg_cache_cleared, Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton(getResources().getString(R.string.dlg_no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) { }
                        }).show();
            }
        });

        mClearCacheButton = (Button) findViewById(R.id.prefs_clear_view_count);
        mClearCacheButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(PreferencesActivity.this).setTitle(getResources().getString(R.string.dlg_ask_clear_view_count))
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton(getResources().getString(R.string.dlg_yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                for (AyaNewsEntry entry : AyaEnvironment.entryList)
                                    entry.views = 0;
                                Toast.makeText(PreferencesActivity.this, R.string.dlg_view_count_cleared, Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton(getResources().getString(R.string.dlg_no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) { }
                        }).show();
            }
        });
    }
}
