package com.java.gaojian;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

public class RSSListActivity extends AppCompatActivity {

    private RecyclerView mRssList;
    private AyaRssListAdapter mAdapter;

    class AyaRssListAdapter extends RecyclerView.Adapter<AyaRssListAdapter.AyaRVHolder> {
        public AyaRssListAdapter() { }

        @Override
        public AyaRssListAdapter.AyaRVHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new AyaRVHolder(inflater.inflate(R.layout.rss_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull AyaRssListAdapter.AyaRVHolder holder, int i) {
            AyaEnvironment.RssFeed rss = AyaEnvironment.rssFeedList.get(i);
            holder.bind(rss);
            holder.itemView.setTag(Integer.valueOf(i));
        }

        @Override
        public int getItemCount() {
            return AyaEnvironment.rssFeedList.size();
        }

        class AyaRVHolder extends RecyclerView.ViewHolder {
            View view;

            public AyaRVHolder(View itemView) {
                super(itemView);
                view = itemView;
            }
            public void bind(AyaEnvironment.RssFeed rss) {
                ((TextView) view.findViewById(R.id.rss_item_title)).setText(rss.name);

                Switch mSwitch = (Switch) view.findViewById(R.id.rss_item_switch);
                mSwitch.setChecked(rss.active);
                mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        Object tag = view.getTag();
                        if (tag == null || !(tag instanceof Integer))
                            return;
                        int index = ((Integer) tag).intValue();
                        AyaEnvironment.RssFeed rss = AyaEnvironment.rssFeedList.get(index);
                        rss.active = isChecked;
                        AyaEnvironment.rssFeedList.set(index, rss);
                    }
                });
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rsslist);

        this.setTitle(getResources().getString(R.string.title_channels));

        //Log.d("ayaDeb", "RSSListActivity.onCreate " + AyaEnvironment.rssFeedList.size());

        mRssList = (RecyclerView) findViewById(R.id.rss_list);
        mRssList.setAdapter(mAdapter = new AyaRssListAdapter());
        mRssList.setLayoutManager(new LinearLayoutManager(this));
        mRssList.addItemDecoration(new AyaDividerItemDecoration(this));
    }
}
