package com.java.gaojian;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class FavoriteListActivity extends AppCompatActivity {

    public static final int REQUEST_SHOW_HTML_CODE = 0;
    public static final int REQUEST_SHOW_HTML_SUCCRET = 1;

    private List<AyaNewsEntry> mFavData = new ArrayList<>();

    private AyaFavItemClickListener mClickListener = new AyaFavItemClickListener();
    private RecyclerView mFavList;
    private AyaFavListAdapter mAdapter;

    class AyaFavListAdapter extends RecyclerView.Adapter<AyaFavListAdapter.AyaRVHolder> {
        public AyaFavListAdapter() { }

        @Override
        public AyaRVHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            AyaRVHolder holder = new AyaRVHolder(inflater.inflate(R.layout.news_item, parent, false));
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mClickListener.onItemClick(v);
                }
            });
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mClickListener.onItemLongClick(v);
                    return true;
                }
            });
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull AyaFavListAdapter.AyaRVHolder holder, int i) {
            AyaNewsEntry entry = mFavData.get(i);
            holder.bind(entry);
            holder.itemView.setTag(entry);
        }

        @Override
        public int getItemCount() {
            return mFavData.size();
        }

        class AyaRVHolder extends RecyclerView.ViewHolder {
            View view;

            public AyaRVHolder(View itemView) {
                super(itemView);
                view = itemView;
            }
            public void bind(AyaNewsEntry entry) {
                ((TextView) view.findViewById(R.id.news_item_title)).setText(entry.title);
                ((TextView) view.findViewById(R.id.news_item_pubDate)).setText(entry.pubDate);
                ((TextView) view.findViewById(R.id.news_item_source)).setText(entry.source);
            }
        }
    }

    class AyaFavItemClickListener implements OnItemClickListener {
        @Override
        public void onItemClick(View view) {
            Object tag = view.getTag();
            if (tag == null || !(tag instanceof AyaNewsEntry))
                return;
            AyaNewsEntry entry = (AyaNewsEntry) tag;

            Intent intent = new Intent(FavoriteListActivity.this, ShowHTMLActivity.class);
            intent.putExtra("uid", entry.uid);
            intent.putExtra("isCalledFromFavorites", true);
            startActivityForResult(intent, REQUEST_SHOW_HTML_CODE);
        }

        @Override
        public void onItemLongClick(View view) {
            Object tag = view.getTag();
            if (tag == null || !(tag instanceof AyaNewsEntry))
                return;
            AyaNewsEntry entry = (AyaNewsEntry) tag;
            Toast.makeText(FavoriteListActivity.this, "Long click: " + entry.uid, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SHOW_HTML_CODE)
            if (resultCode == REQUEST_SHOW_HTML_SUCCRET) {
                Toast.makeText(this, R.string.info_succ_removed_fav, Toast.LENGTH_SHORT).show();
                buildFavData();
                mAdapter.notifyDataSetChanged();
            }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_list);

        this.setTitle(R.string.title_favorites);

        buildFavData();

        mFavList = (RecyclerView) findViewById(R.id.fav_list);
        mFavList.setAdapter(mAdapter = new AyaFavListAdapter());
        mFavList.setLayoutManager(new LinearLayoutManager(this));
        mFavList.addItemDecoration(new AyaDividerItemDecoration(this));

        if (mFavData.isEmpty())
            Toast.makeText(this, R.string.warn_no_fav, Toast.LENGTH_SHORT).show();
    }

    private void buildFavData() {
        mFavData.clear();
        for (String uid : AyaEnvironment.favSet) {
            AyaNewsEntry entry = AyaEnvironment.findEntry(uid);
            if (entry != null)
                mFavData.add(entry);
        }
    }
}
