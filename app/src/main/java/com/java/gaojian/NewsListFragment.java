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
import android.util.Pair;
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

import java.util.Collections;
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
     * Relationship:  mShownData <= mFilteredData <= mData
     * Description:
     *     mData:          Complete dataset. Remains unchanged until fetchData.
     *     mFilteredData:  Result subset by filtering mData with currentFilter.
     *     mShownData:     Displayed subset of mFilteredData by loadMoreData (called by user).
     */
    private List<AyaNewsEntry> mData;
    private List<AyaNewsEntry> mFilteredData = new LinkedList<AyaNewsEntry>();
    private List<AyaNewsEntry> mShownData = new LinkedList<AyaNewsEntry>();
    private int loadedItems = 0;

    private SearchView mSearcher;
    protected SmartRefreshLayout mSwipeRefresher;
    private RecyclerView mNewsList;
    private AyaNewsListAdapter mAdapter;

    private OnItemClickListener mClickListener = new AyaNewsItemClickListener();

    public NewsListFragment() {
        mData = AyaEnvironment.entryList;
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
            AyaNewsEntry entry = mShownData.get(i);
            holder.bind(entry);
            holder.itemView.setTag(new Pair<>(i, entry));
        }

        @Override
        public int getItemCount() {
            return mShownData.size();
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
            mSwipeRefresher = (SmartRefreshLayout) view.findViewById(R.id.news_list_swipe_refresh);
            mSwipeRefresher.setOnRefreshListener(new OnRefreshListener() {
                @Override
                public void onRefresh(RefreshLayout refreshLayout) {
                    mainAc.refetchRSS(false);
                }
            });
            mSwipeRefresher.setOnLoadMoreListener(new OnLoadMoreListener() {
                @Override
                public void onLoadMore(RefreshLayout refreshLayout) {
                    NewsListFragment.this.loadMoreData();
                }
            });
            mSwipeRefresher.setRefreshHeader(new MaterialHeader(getContext())
                    .setColorSchemeColors(
                            0xFF000000 + getResources().getColor(R.color.colorPrimary)
                    ));
            mSwipeRefresher.setRefreshFooter(new ClassicsFooter(getContext())
                    .setSpinnerStyle(SpinnerStyle.Scale)
                    .setAccentColorId(R.color.colorPrimary));

            mNewsList = (RecyclerView) view.findViewById(R.id.news_list);
            mNewsList.setAdapter(mAdapter = new AyaNewsListAdapter());
            mNewsList.setLayoutManager(new LinearLayoutManager(getActivity()));
            mNewsList.addItemDecoration(new AyaDividerItemDecoration(container.getContext()));

            mSearcher = (SearchView) view.findViewById(R.id.news_list_search);
            mSearcher.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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
            mSearcher.clearFocus();

            this.startDatasetRenewal();
        }
        else {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (null != parent)
                parent.removeView(view);
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
        mFilteredData.clear();

        for (AyaNewsEntry entry : mData)
            if (isFiltered(entry))
                mFilteredData.add(entry);

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
        mData.clear();
        if (dataset != null)
            mData.addAll(dataset);
        setFilter(currentFilter);
    }

    private void reloadList() {
        loadedItems = 0;
        mShownData.clear();
        mSwipeRefresher.setNoMoreData(false);
        loadMoreData();
    }

    public void fetchData(List<AyaNewsEntry> dataset) {
        if (mData.isEmpty()) {
            renewDataset(dataset);
            if (mSwipeRefresher.getState() == RefreshState.Refreshing)
                mSwipeRefresher.finishRefresh(REFRESH_DELAY_TIME);
            return;
        }
        if (dataset != null) {
            int counter = 0;
            for (AyaNewsEntry entry : dataset) {
                if (entry == null || entry.uid == null)
                    continue;
                boolean flag = true;
                for (AyaNewsEntry existed : mData)
                    if (entry.uid.equals(existed.uid)) {
                        flag = false;
                        break;
                    }
                if (flag) {
                    mData.add(entry);
                    ++counter;
                }
            }
        }
        Collections.sort(mData);
        setFilter(currentFilter);
        if (mSwipeRefresher.getState() == RefreshState.Refreshing)
            mSwipeRefresher.finishRefresh(0);
    }

    /*
     * Load items from FILTERED dataset.
     */
    private void loadMoreData() {
        Log.d("ayaDeb", "NewsList.loadMoreData: " + loadedItems + " " + mFilteredData.size());
        if (mFilteredData.size() > 0 && loadedItems == mFilteredData.size()) {
            mSwipeRefresher.finishLoadMore(0, true, true);
            return;
        }
        else if (mFilteredData.size() == 0) {
            if (mData.size() == 0)
                mSwipeRefresher.finishLoadMore(0);
            else {
                mShownData.clear();
                mAdapter.notifyDataSetChanged();
            }
            return;
        }
        int remainingItems = mFilteredData.size() - loadedItems;
        if (remainingItems > LOAD_MORE_THRESHOLD)
            remainingItems = LOAD_MORE_THRESHOLD;

        for (int i = loadedItems; i < loadedItems + remainingItems; ++i)
            mShownData.add(mFilteredData.get(i));
        loadedItems += remainingItems;

        mAdapter.notifyDataSetChanged();
        if (mSwipeRefresher.getState() == RefreshState.Loading)
            mSwipeRefresher.finishLoadMore(REFRESH_DELAY_TIME);
    }
}
