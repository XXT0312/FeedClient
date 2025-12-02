package com.selflearn.feedclient.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.selflearn.feedclient.R;
import com.selflearn.feedclient.model.FeedItem;
import com.selflearn.feedclient.utils.CardRenderManager;
import com.selflearn.feedclient.utils.PreloadManager;
import com.selflearn.feedclient.viewholder.BaseViewHolder;
import com.selflearn.feedclient.viewholder.CardType1ViewHolder;
import com.selflearn.feedclient.viewholder.CardType2ViewHolder;
import com.selflearn.feedclient.viewholder.CardType3ViewHolder;

import java.util.ArrayList;
import java.util.List;

public class FeedAdapter extends RecyclerView.Adapter<BaseViewHolder> {

    private List<FeedItem> dataList = new ArrayList<>();
    private OnLoadMoreListener loadMoreListener;
    private OnDeleteListener deleteListener;
    private ExposureListener exposureListener;
    private boolean isLoading = false;

    // 添加预加载相关成员
    private PreloadManager preloadManager;
    private CardRenderManager renderManager;
    private boolean enablePreload = true;
    private int preloadThreshold = 5; // 提前5个位置开始预加载

    // 在构造函数或初始化方法中初始化
    public void initPreload(Context context) {
        if (enablePreload) {
            preloadManager = PreloadManager.getInstance(context);
            renderManager = CardRenderManager.getInstance(context);

            // 预加载所有卡片布局
            preloadManager.preloadMultipleLayouts(
                    R.layout.item_card_type1,
                    R.layout.item_card_type2,
                    R.layout.item_card_type3
            );
        }
    }

    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    public interface OnDeleteListener {
        void onDelete(String itemId); // 只传itemId，不传position
    }

    public interface ExposureListener {
        void onExposure(String itemId, String eventType, float exposureRatio);
    }

    @NonNull
    @Override
    public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 尝试从预加载缓存获取View
        if (enablePreload && preloadManager != null) {
            int layoutRes = getLayoutResForViewType(viewType);
            View cachedView = preloadManager.getCachedView(layoutRes);

            if (cachedView != null) {
                // 使用预加载的View创建ViewHolder
                return createViewHolderFromCachedView(cachedView, viewType);
            }
        }

        // 缓存中没有，正常加载
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case 2:
                return CardType2ViewHolder.create(inflater, parent);
            case 3: // 视频卡片类型
                return CardType3ViewHolder.create(inflater, parent);
            default:
                return CardType1ViewHolder.create(inflater, parent);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder holder, int position) {
        FeedItem item = dataList.get(position);

        // 先绑定数据
        holder.bind(item);

        // 设置删卡长按监听
        holder.itemView.setOnLongClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(item.getId());
            }
            return true;
        });

        // 设置曝光监听（必须在bind之后）
        holder.setExposureListener(exposureListener);

        //  预加载后续卡片
        if(enablePreload && position + preloadThreshold < dataList.size()){
            preloadNextCards(position);
        }

        // 加载更多检测 - 修复版：在position>=0时才检查
        if (position >= 0 && position >= getItemCount() - 3 && !isLoading && loadMoreListener != null) {
            isLoading = true;
            loadMoreListener.onLoadMore();
        }
    }

    @Override
    public int getItemViewType(int position) {
        return dataList.get(position).getCardType();
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public void setData(List<FeedItem> newData) {
        dataList.clear();
        dataList.addAll(newData);
        notifyDataSetChanged();
        isLoading = false;
    }

    public void addData(List<FeedItem> moreData) {
        int startPosition = dataList.size();
        dataList.addAll(moreData);
        notifyItemRangeInserted(startPosition, moreData.size());
        isLoading = false;
    }

    // 通过itemId删除
    public boolean removeItemById(String itemId) {
        for (int i = 0; i < dataList.size(); i++) {
            if (dataList.get(i).getId().equals(itemId)) {
                dataList.remove(i);
                notifyItemRemoved(i);
                return true;
            }
        }
        return false;
    }

    // 通过position删除（备用）
    public void removeItem(int position) {
        if (position >= 0 && position < dataList.size()) {
            dataList.remove(position);
            notifyItemRemoved(position);
        }
    }

    // 根据itemId查找item
    public FeedItem getItemById(String itemId) {
        for (FeedItem item : dataList) {
            if (item.getId().equals(itemId)) {
                return item;
            }
        }
        return null;
    }

    // 根据position查找item
    public FeedItem getItemAtPosition(int position) {
        if (position >= 0 && position < dataList.size()) {
            return dataList.get(position);
        }
        return null;
    }

    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        this.loadMoreListener = listener;
    }

    public void setOnDeleteListener(OnDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void setExposureListener(ExposureListener listener) {
        this.exposureListener = listener;
    }

    private RecyclerView recyclerView;

    // 添加这个方法
    public void attachToRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;

        // 添加滚动监听，自动检测是否需要加载更多
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                // 当停止滚动时检查是否需要加载更多
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    checkAndLoadMore();
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // 滚动时也检查
                checkAndLoadMore();
            }
        });
    }

    private void checkAndLoadMore() {
        if (recyclerView == null || recyclerView.getLayoutManager() == null) return;

        GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
        int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
        int totalItemCount = layoutManager.getItemCount();

        // 如果最后一个可见项是倒数第3个或更后面，且不在加载中，则触发加载更多
        if (!isLoading && loadMoreListener != null && lastVisibleItemPosition >= totalItemCount - 3) {
            isLoading = true;
            loadMoreListener.onLoadMore();
        }
    }

    // 修改原有的resetLoadMoreState方法，添加预加载检查
    public void resetLoadMoreState() {
        isLoading = false;
        // 立即检查是否需要加载更多
        if (recyclerView != null) {
            recyclerView.post(() -> {
                checkAndLoadMore();
                // 触发预加载
                if (enablePreload && recyclerView.getLayoutManager() != null) {
                    GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                    int lastVisible = layoutManager.findLastVisibleItemPosition();
                    preloadNextCards(lastVisible);
                }
            });
        }
    }

    /**
     * 根据ViewType获取对应的布局资源
     */
    private int getLayoutResForViewType(int viewType) {
        switch (viewType) {
            case 2:
                return R.layout.item_card_type2;
            case 3:
                return R.layout.item_card_type3;
            default:
                return R.layout.item_card_type1;
        }
    }

    /**
     * 从缓存的View创建ViewHolder
     */
    private BaseViewHolder createViewHolderFromCachedView(View cachedView, int viewType) {
        switch (viewType) {
            case 2:
                return new CardType2ViewHolder(cachedView);
            case 3:
                return new CardType3ViewHolder(cachedView);
            default:
                return new CardType1ViewHolder(cachedView);
        }
    }

    /**
     * 设置是否启用预加载
     */
    public void setEnablePreload(boolean enablePreload) {
        this.enablePreload = enablePreload;

        if (!enablePreload && preloadManager != null) {
            // 禁用时清空缓存，释放内存
            preloadManager.clearCache();
        } else if (enablePreload && preloadManager != null) {
            // 启用时重新预加载
            Context context = recyclerView != null ? recyclerView.getContext() : null;
            if (context != null) {
                preloadManager.preloadMultipleLayouts(
                        R.layout.item_card_type1,
                        R.layout.item_card_type2,
                        R.layout.item_card_type3
                );
            }
        }
    }

    /**
     * 设置预加载阈值
     */
    public void setPreloadThreshold(int preloadThreshold) {
        this.preloadThreshold = preloadThreshold;
    }


    /**
     * 预加载后续的卡片
     */
    private void preloadNextCards(int currentPosition) {
        if (!enablePreload || renderManager == null) return;

        // 预加载后续3个卡片
        for (int i = 1; i <= 3; i++) {
            int nextPos = currentPosition + i;
            if (nextPos < dataList.size()) {
                FeedItem nextItem = dataList.get(nextPos);
                preloadCard(nextItem);
            }
        }
    }

    /**
     * 预加载单个卡片
     */
    private void preloadCard(FeedItem item) {
        if (item == null) return;

        // 预加载图片
        if (item.getImages() != null && !item.getImages().isEmpty()) {
            String imageUrl = item.getImages().get(0);
            renderManager.preloadImage(imageUrl);
        }

        // 如果是视频卡片，预加载视频封面
        if (item.getCardType() == 3 && item.getVideoCover() != null) {
            renderManager.preloadImage(item.getVideoCover());
        }
    }

    /**
     * 获取预加载是否启用
     */
    public boolean isPreloadEnabled() {
        return enablePreload;
    }


}