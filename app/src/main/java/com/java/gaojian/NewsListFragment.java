package com.java.gaojian;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.scwang.smartrefresh.header.MaterialHeader;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.constant.RefreshState;
import com.scwang.smartrefresh.layout.constant.SpinnerStyle;
import com.scwang.smartrefresh.layout.footer.ClassicsFooter;
import com.scwang.smartrefresh.layout.listener.OnLoadMoreListener;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;

import java.util.LinkedList;
import java.util.List;

public class NewsListFragment extends Fragment {

    public static final int LOAD_MORE_THRESHOLD = 10;
    public static final int REFRESH_DELAY_TIME = 500;
    public static final int BYTE_BUFFER_SIZE = 1024;

    private MainActivity mainAc;

    private String currentFilter = "";

    /*
     * Three datasets.
     *
     * Relationship:  frShownData <= frFilteredData <= frData
     * Description:
     *     frData:          Complete dataset. Remains unchanged until fetchData.
     *     frFilteredData:  Result subset by filtering frData with currentFilter.
     *     frShownData:     Displayed subset of frFilteredData by loadMoreData (called by user).
     */
    private List<AyaNewsEntry> frData;
    private List<AyaNewsEntry> frFilteredData = new LinkedList<AyaNewsEntry>();
    private List<AyaNewsEntry> frShownData = new LinkedList<AyaNewsEntry>();
    private int loadedItems = 0;

    private SearchView frSearcher;
    protected SmartRefreshLayout frSwipeRefresher;
    private RecyclerView frNewsList;
    private AyaNewsListAdapter frAdapter;

    private OnItemClickListener mClickListener = new AyaNewsItemClickListener();

    public NewsListFragment() {
        frData = AyaEnvironment.entryList;
    }

    class AyaNewsListAdapter extends RecyclerView.Adapter<AyaNewsListAdapter.AyaRVHolder> {
        public AyaNewsListAdapter() { }

        @Override
        public AyaNewsListAdapter.AyaRVHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
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
        public void onBindViewHolder(@NonNull AyaNewsListAdapter.AyaRVHolder holder, int i) {
            AyaNewsEntry entry = frShownData.get(i);
            holder.bind(entry);
            holder.itemView.setTag(entry);
        }

        @Override
        public int getItemCount() {
            return frShownData.size();
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

    class AyaNewsItemClickListener implements OnItemClickListener {
        @Override
        public void onItemClick(View view) {
            Object tag = view.getTag();
            if (tag == null || !(tag instanceof AyaNewsEntry))
                return;
            AyaNewsEntry entry = (AyaNewsEntry) tag;

            Intent intent = new Intent(mainAc, ShowHTMLActivity.class);
            intent.putExtra("uid", entry.uid);
            intent.putExtra("isCalledFromFavorites", false);
            startActivity(intent);
        }

        @Override
        public void onItemLongClick(View view) {
            Object tag = view.getTag();
            if (tag == null || !(tag instanceof AyaNewsEntry))
                return;
            AyaNewsEntry entry = (AyaNewsEntry) tag;
            Toast.makeText(mainAc, "Long click: " + entry.uid, Toast.LENGTH_SHORT).show();
        }
    }

    public void setMainActivity(MainActivity mainAc) {
        this.mainAc = mainAc;
    }

    private View view = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.news_list, container, false);
            frSwipeRefresher = (SmartRefreshLayout) view.findViewById(R.id.news_list_swipe_refresh);
            frSwipeRefresher.setOnRefreshListener(new OnRefreshListener() {
                @Override
                public void onRefresh(RefreshLayout refreshLayout) {
                    mainAc.refetchRSS(false);
                }
            });
            frSwipeRefresher.setOnLoadMoreListener(new OnLoadMoreListener() {
                @Override
                public void onLoadMore(RefreshLayout refreshLayout) {
                    NewsListFragment.this.loadMoreData();
                }
            });
            frSwipeRefresher.setRefreshHeader(new MaterialHeader(getContext())
                    .setColorSchemeColors(
                            0xFF000000 + getResources().getColor(R.color.colorPrimary)
                    ));
            frSwipeRefresher.setRefreshFooter(new ClassicsFooter(getContext())
                    .setSpinnerStyle(SpinnerStyle.Scale)
                    .setAccentColorId(R.color.colorPrimary));

            frNewsList = (RecyclerView) view.findViewById(R.id.news_list);
            frNewsList.setAdapter(frAdapter = new AyaNewsListAdapter());
            frNewsList.setLayoutManager(new LinearLayoutManager(getActivity()));
            frNewsList.addItemDecoration(new AyaDividerItemDecoration(container.getContext()));

            frSearcher = (SearchView) view.findViewById(R.id.news_list_search);
            frSearcher.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    NewsListFragment.this.setFilter(newText);
                    return false;
                }
            });
            frSearcher.clearFocus();

            this.startDatasetRenewal();
        }
        else {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (null != parent) {
                parent.removeView(view);
            }
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private boolean isFiltered(AyaNewsEntry target) {
        boolean isInSelectedChannel = false;
        for (AyaEnvironment.RssFeed rss : AyaEnvironment.rssFeedList)
            if (rss.name.equals(target.source) && rss.active) {
                isInSelectedChannel = true;
                break;
            }
        if (!isInSelectedChannel)
            return false;
        if (currentFilter.length() == 0)
            return true;
        if (target.title.toLowerCase().contains(currentFilter))
            return true;
        return false;
    }

    private void setFilter(String filter) {
        currentFilter = filter.toLowerCase();
        frFilteredData.clear();

        for (AyaNewsEntry entry : frData)
            if (isFiltered(entry))
                frFilteredData.add(entry);

        reloadList();
    }

    /*
     * Must be called before any data fetch attempts.
     */
    public void startDatasetRenewal() {
        AyaEnvironment.loadNewsList(getContext());
        setFilter("");
        loadMoreData();
        mainAc.refetchRSS(true);
    }

    public void renewDataset(List<AyaNewsEntry> dataset) {
        frData.clear();
        if (dataset != null)
            frData.addAll(dataset);
        setFilter(currentFilter);
    }

    private void reloadList() {
        loadedItems = 0;
        frShownData.clear();
        frSwipeRefresher.setNoMoreData(false);
        loadMoreData();
    }

    public void fetchData(List<AyaNewsEntry> dataset) {
        if (frData.isEmpty()) {
            renewDataset(dataset);
            if (frSwipeRefresher.getState() == RefreshState.Refreshing)
                frSwipeRefresher.finishRefresh(REFRESH_DELAY_TIME);
            return;
        }
        if (dataset != null) {
            int n = dataset.size();
            int counter = 0;
            for (int i = n - 1; i >= 0; --i) {
                AyaNewsEntry entry = dataset.get(i);
                boolean flag = true;
                for (AyaNewsEntry existed : frData)
                    if (entry.uid.equals(existed.uid)) {
                        flag = false;
                        break;
                    }
                if (flag) {
                    frData.add(0, entry);
                    ++counter;
                }
            }
        }
        setFilter(currentFilter);
        if (frSwipeRefresher.getState() == RefreshState.Refreshing)
            frSwipeRefresher.finishRefresh(0);
    }

    /*
     * Load items from FILTERED dataset.
     */
    private void loadMoreData() {
        Log.d("ayaDeb", "NewsList.loadMoreData: " + loadedItems + " " + frFilteredData.size());
        if (frFilteredData.size() > 0 && loadedItems == frFilteredData.size()) {
            frSwipeRefresher.finishLoadMore(0, true, true);
            return;
        }
        else if (frFilteredData.size() == 0) {
            if (frData.size() == 0)
                frSwipeRefresher.finishLoadMore(0);
            else {
                frShownData.clear();
                frAdapter.notifyDataSetChanged();
            }
            return;
        }
        int remainingItems = frFilteredData.size() - loadedItems;
        if (remainingItems > LOAD_MORE_THRESHOLD)
            remainingItems = LOAD_MORE_THRESHOLD;

        for (int i = loadedItems; i < loadedItems + remainingItems; ++i)
            frShownData.add(frFilteredData.get(i));
        loadedItems += remainingItems;

        frAdapter.notifyDataSetChanged();
        if (frSwipeRefresher.getState() == RefreshState.Loading)
            frSwipeRefresher.finishLoadMore(REFRESH_DELAY_TIME);
    }
}
