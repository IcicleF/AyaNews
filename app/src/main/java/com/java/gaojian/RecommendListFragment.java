package com.java.gaojian;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.scwang.smartrefresh.header.MaterialHeader;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.constant.RefreshState;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;

import java.text.CollationElementIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecommendListFragment extends Fragment {

    public static final int ITEM_COUNT = 10;

    public static final double NOT_VIEWED_WEIGHT = 3.0f;
    public static final double CATEGORY_WEIGHT = 0.5f;
    public static final double RANDOM_WEIGHT = 5.0f;

    private MainActivity mainAc;

    private List<AyaNewsEntry> mData = new ArrayList<>();

    private View view = null;

    protected SmartRefreshLayout mSwipeRefresher;
    private RecyclerView mRecomList;
    private AyaRecomListAdapter mAdapter;

    private OnItemClickListener mClickListener = new AyaNewsItemClickListener();

    public RecommendListFragment() { }

    class AyaRecomListAdapter extends RecyclerView.Adapter<AyaRecomListAdapter.AyaRVHolder> {
        public AyaRecomListAdapter() { }

        @Override
        public AyaRecomListAdapter.AyaRVHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            AyaRecomListAdapter.AyaRVHolder holder = new AyaRecomListAdapter.AyaRVHolder(inflater.inflate(R.layout.news_item, parent, false));
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
        public void onBindViewHolder(@NonNull AyaRecomListAdapter.AyaRVHolder holder, int i) {
            AyaNewsEntry entry = mData.get(i);
            holder.bind(entry);
            holder.itemView.setTag(new Pair<>(i, entry));
        }

        @Override
        public int getItemCount() {
            return mData.size();
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

                String recomStr = "From category: " + entry.getCategory();
                ((TextView) view.findViewById(R.id.news_item_source)).setText(recomStr);

                view.setBackgroundResource(entry.views > 0 ? R.color.colorRead : R.color.colorPureWhite);
            }
        }
    }

    class AyaNewsItemClickListener implements OnItemClickListener {
        @Override
        public void onItemClick(View view) {
            if (view.getTag() == null)
                return;
            Pair<Integer, AyaNewsEntry> tag = (Pair<Integer, AyaNewsEntry>)(view.getTag());
            AyaNewsEntry entry = tag.second;
            AyaEnvironment.setViewed(entry);
            mAdapter.notifyItemChanged(tag.first);

            Intent intent = new Intent(mainAc, ShowHTMLActivity.class);
            intent.putExtra("uid", entry.uid);
            intent.putExtra("isCalledFromFavorites", false);
            startActivity(intent);
        }

        @Override
        public void onItemLongClick(View view) {
            return;
        }
    }

    static class AyaRecomEntry implements Comparable<AyaRecomEntry> {
        public double weight;
        public AyaNewsEntry origin;

        @Override
        public int compareTo(@NonNull AyaRecomEntry o) {
            return Double.compare(this.weight, o.weight);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.news_list, container, false);

            ((SearchView) view.findViewById(R.id.news_list_search)).setVisibility(View.GONE);

            mSwipeRefresher = (SmartRefreshLayout) view.findViewById(R.id.news_list_swipe_refresh);
            mSwipeRefresher.setOnRefreshListener(new OnRefreshListener() {
                @Override
                public void onRefresh(RefreshLayout refreshLayout) {
                    RecommendListFragment.this.reloadList();
                }
            });
            mSwipeRefresher.setRefreshHeader(new MaterialHeader(getContext())
                    .setColorSchemeColors(
                            0xFF000000 + getResources().getColor(R.color.colorPrimary)
                    ));

            mRecomList = (RecyclerView) view.findViewById(R.id.news_list);
            mRecomList.setAdapter(mAdapter = new AyaRecomListAdapter());
            mRecomList.setLayoutManager(new LinearLayoutManager(getActivity()));
            mRecomList.addItemDecoration(new AyaDividerItemDecoration(container.getContext()));
        }
        else {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (null != parent)
                parent.removeView(view);
        }
        return view;
    }

    public void setMainActivity(MainActivity mainAc) {
        this.mainAc = mainAc;
    }

    public static final int REFRESH_LIST = 0x2;
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            String info;
            int opcode = msg.what;
            if ((opcode & REFRESH_LIST) != 0) {
                if (mSwipeRefresher.getState() == RefreshState.Refreshing)
                    mSwipeRefresher.finishRefresh(0);
                mAdapter.notifyDataSetChanged();
                opcode = opcode & ~REFRESH_LIST;
            }

            if (opcode != 0)
                Toast.makeText(getContext(), R.string.err_unexpected, Toast.LENGTH_SHORT).show();
        }
    };

    public void startRefreshList() {
        if (mSwipeRefresher.getState() != RefreshState.Refreshing)
            mSwipeRefresher.autoRefresh(0);
    }

    public void reloadList() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<AyaRecomEntry> rList = new ArrayList<>();
                Map<String, Integer> rMap = new HashMap<>();

                for (AyaNewsEntry entry : AyaEnvironment.entryList) {
                    String key = entry.getCategory();
                    if (rMap.containsKey(key))
                        rMap.put(key, rMap.get(key) + entry.views);
                    else
                        rMap.put(key, entry.views);
                }

                for (AyaNewsEntry entry : AyaEnvironment.entryList) {
                    String key = entry.getCategory();
                    double weight = 0f;

                    if (entry.views == 0)
                        weight += NOT_VIEWED_WEIGHT;
                    if (rMap.get(key) > 0)
                        weight += Math.log10(rMap.get(key)) * CATEGORY_WEIGHT;
                    weight += Math.random() * RANDOM_WEIGHT;

                    AyaRecomEntry rEntry = new AyaRecomEntry();
                    rEntry.weight = weight;
                    rEntry.origin = entry;
                    rList.add(rEntry);
                }
                Collections.sort(rList);

                int endIndex = rList.size();
                if (endIndex > ITEM_COUNT)
                    endIndex = ITEM_COUNT;
                mData.clear();
                for (int i = 0; i < endIndex; ++i)
                    mData.add(rList.get(i).origin);
                Collections.shuffle(mData);

                handler.sendEmptyMessage(REFRESH_LIST);
            }
        }).start();
    }
}
