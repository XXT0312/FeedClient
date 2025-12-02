package com.selflearn.feedclient.presenter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.selflearn.feedclient.contract.FeedContract;
import com.selflearn.feedclient.model.FeedItem;
import com.selflearn.feedclient.model.FeedModel;
import com.selflearn.feedclient.view.MainActivity;

import java.lang.ref.WeakReference;
import java.util.List;

public class FeedPresenter implements FeedContract.Presenter {

    private WeakReference<FeedContract.View> viewRef;
    private FeedContract.Model model;
    private int currentPage = 0;
    private boolean hasMore = true;
    private Handler mainHandler;

    private Context context;

    /**
     * 标志是否正在加载中
     */
    private boolean isLoading;

    // 添加缓存状态标志
    private boolean isShowingCache = false;
    private boolean isFirstLoad = true;

    public FeedPresenter(FeedContract.View view, Context context) {
        this.viewRef = new WeakReference<>(view);
        this.context = context.getApplicationContext();
        this.model = new FeedModel(this.context);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void loadFeedList(boolean isRefresh) {
        if (isRefresh) {
            currentPage = 0;
            hasMore = true;
            isShowingCache = false;
        }

        if (!hasMore) {
            return;
        }

        // 如果是首次加载，显示缓存提示
        if (isFirstLoad) {
            isFirstLoad = false;
            showCacheHint();
        }

        runOnUiThread(() -> {
            if (viewRef.get() != null) {
                viewRef.get().showLoading();
            }
        });

        model.loadFeedList(currentPage + 1, new FeedContract.Model.OnLoadListener() {
            @Override
            public void onSuccess(List<FeedItem> feedList) {
                runOnUiThread(() -> {
                    if (viewRef.get() != null) {
                        viewRef.get().hideLoading();

                        // 如果是第一页且显示的是缓存数据，添加提示
                        if (currentPage == 0 && isShowingCache) {
                            showCacheUsedMessage();
                        }

                        if (isRefresh) {
                            viewRef.get().showFeedList(feedList);
                        } else {
                            viewRef.get().showLoadMore(feedList);
                        }
                        currentPage++;
                        if (feedList.size() < 10) {
                            hasMore = false;
                        }
                        isShowingCache = false;
                    }
                });
            }

            @Override
            public void onError(String message) {
                // 关键：失败时重置加载状态
                isLoading = false;

                runOnUiThread(() -> {
                    if (viewRef.get() != null) {
                        viewRef.get().hideLoading();

                        // 只有在第一页且没有数据时才显示缓存
                        if (currentPage == 0) {
                            List<FeedItem> cacheList = model.getCache();
                            if (cacheList != null && !cacheList.isEmpty()) {
                                isShowingCache = true;
                                viewRef.get().showFeedList(cacheList);
                                showCacheFallbackMessage();
                            } else {
                                viewRef.get().showError(message);
                            }
                        } else {
                            viewRef.get().showError(message);
                        }
                    }
                });
            }
        });
    }


    /**
     * 显示缓存提示
     */
    private void showCacheHint() {
        runOnUiThread(() -> {
            if (viewRef.get() != null) {
                // 可以在这里添加缓存状态的提示
                // 例如：Toast.makeText(context, "正在检查缓存...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 显示缓存回退消息
     */
    private void showCacheFallbackMessage() {
        runOnUiThread(() -> {
            if (viewRef.get() != null && viewRef.get() instanceof MainActivity) {
                MainActivity activity = (MainActivity) viewRef.get();
                activity.showToast("网络异常，已显示缓存数据");
            }
        });
    }

    /**
     * 显示缓存使用消息
     */
    private void showCacheUsedMessage() {
        runOnUiThread(() -> {
            if (viewRef.get() != null && viewRef.get() instanceof MainActivity) {
                MainActivity activity = (MainActivity) viewRef.get();
                activity.showToast("正在使用缓存数据加载...");
            }
        });
    }

    @Override
    public void loadMore() {
        loadFeedList(false);
    }

    @Override
    public void deleteItem(String itemId) {
        // 先通知Model删除数据
        model.deleteItem(itemId);

        // 再通知View更新UI
        runOnUiThread(() -> {
            if (viewRef.get() != null) {
                viewRef.get().removeItem(itemId);
            }
        });
    }

    @Override
    public void onExposureEvent(String itemId, String eventType, float exposureRatio) {
        runOnUiThread(() -> {
            if (viewRef.get() != null) {
                viewRef.get().onExposureEvent(itemId, eventType, exposureRatio);
            }
        });
    }

    @Override
    public void onDestroy() {
        if (viewRef != null) {
            viewRef.clear();
        }
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }

    private void runOnUiThread(Runnable action) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            action.run();
        } else {
            mainHandler.post(action);
        }
    }
}