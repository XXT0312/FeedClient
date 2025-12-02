package com.selflearn.feedclient.contract;

import com.selflearn.feedclient.model.FeedItem;
import java.util.List;

public interface FeedContract {
    interface View {
        void showLoading();
        void hideLoading();
        void showFeedList(List<FeedItem> feedList);
        void showLoadMore(List<FeedItem> moreList);
        void showError(String message);
        void removeItem(String itemId); // 改为通过itemId删除
        void onExposureEvent(String itemId, String eventType, float exposureRatio);
    }

    interface Presenter {
        void loadFeedList(boolean isRefresh);
        void loadMore();
        void deleteItem(String itemId); // 只传itemId
        void onExposureEvent(String itemId, String eventType, float exposureRatio);
        void onDestroy();
    }

    interface Model {
        interface OnLoadListener {
            void onSuccess(List<FeedItem> feedList);
            void onError(String message);
        }

        void loadFeedList(int page, OnLoadListener listener);
        void deleteItem(String itemId);
        void saveToCache(List<FeedItem> feedList);
        List<FeedItem> getCache();
    }
}