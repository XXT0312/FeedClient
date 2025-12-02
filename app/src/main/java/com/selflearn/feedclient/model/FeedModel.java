package com.selflearn.feedclient.model;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.selflearn.feedclient.contract.FeedContract;
import com.selflearn.feedclient.utils.LocalCacheManager;

import java.util.ArrayList;
import java.util.List;

public class FeedModel implements FeedContract.Model {
    private static final String TAG = "FeedModel";

    private LocalCacheManager cacheManager;

    private Context context;

    public FeedModel(Context context) {
        this.context = context.getApplicationContext();
        this.cacheManager = LocalCacheManager.getInstance(this.context);
    }

    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void loadFeedList(int page, OnLoadListener listener) {
        Log.d(TAG, "加载Feed列表，页码: " + page);

        // 如果是第一页，先尝试从缓存加载
        if (page == 1) {
            List<FeedItem> cachedData = cacheManager.getCachedFeedList();
            if (cachedData != null && !cachedData.isEmpty()) {
                Log.d(TAG, "从缓存加载第一页数据，数量: " + cachedData.size());
                // 先返回缓存数据给用户快速展示
                mainHandler.post(() -> {
                    if (listener != null) {
                        listener.onSuccess(cachedData);
                    }
                });
            }
        }

        // 模拟网络请求（实际项目中应替换为真实网络请求）
        new Thread(() -> {
            try {
                Thread.sleep(800); // 模拟网络延迟
                List<FeedItem> data = createMockData(page);

                // 模拟随机失败
                double random = Math.random();
                if (random < 0.2) { // 20%失败率（为了测试缓存功能）
                    Log.w(TAG, "网络请求失败，失败率: " + random);
                    mainHandler.post(() -> {
                        if (listener != null) {
                            // 网络失败时，如果也没有缓存，才报错
                            if (page == 1) {
                                List<FeedItem> fallbackCache = cacheManager.getCachedFeedList();
                                if (fallbackCache != null && !fallbackCache.isEmpty()) {
                                    Log.i(TAG, "网络失败，使用缓存数据展示");
                                    listener.onSuccess(fallbackCache);
                                } else {
                                    listener.onError("网络请求失败，且无缓存数据");
                                }
                            } else {
                                listener.onError("网络请求失败");
                            }
                        }
                    });
                } else {
                    // 成功时保存缓存（只缓存第一页）
                    if (page == 1) {
                        boolean hasMore = data.size() >= 10;
                        cacheManager.saveFeedList(data, page, hasMore);
                        Log.i(TAG, "网络请求成功，数据已缓存，数量: " + data.size());
                    }

                    mainHandler.post(() -> {
                        if (listener != null) {
                            listener.onSuccess(data);
                        }
                    });
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "网络请求被中断", e);
                mainHandler.post(() -> {
                    if (listener != null) {
                        // 中断时也尝试使用缓存
                        if (page == 1) {
                            List<FeedItem> fallbackCache = cacheManager.getCachedFeedList();
                            if (fallbackCache != null && !fallbackCache.isEmpty()) {
                                Log.i(TAG, "请求中断，使用缓存数据展示");
                                listener.onSuccess(fallbackCache);
                            } else {
                                listener.onError("请求中断: " + e.getMessage());
                            }
                        } else {
                            listener.onError("请求中断: " + e.getMessage());
                        }
                    }
                });
            }
        }).start();
    }

    /**
     * 获取缓存管理器（用于调试和测试）
     */
    public LocalCacheManager getCacheManager() {
        return cacheManager;
    }

    private List<FeedItem> createMockData(int page) {
        List<FeedItem> list = new ArrayList<>();
        int start = page * 10;

        for (int i = start; i < start + 10; i++) {
            FeedItem item = new FeedItem();
            item.setId("item_" + i);

            // 卡片类型：1-普通卡片，2-增强卡片，3-视频卡片
            // 让第3、6、9等卡片为视频卡片
            if (i % 3 == 0) {
                item.setCardType(3); // 视频卡片
            } else if (i % 2 == 0) {
                item.setCardType(2); // 增强卡片
            } else {
                item.setCardType(1); // 普通卡片
            }

            // 排版方式
            if (i < 3) {
                item.setLayoutType(1); // 前3个单列
            } else {
                item.setLayoutType((i % 2) + 1); // 后面随机单列或双列
            }

            item.setTitle("标题 " + i + (item.getLayoutType() == 1 ? " (单列)" : " (双列)"));
            item.setContent("这里是内容描述，用于测试混合排版布局。这是卡片类型" + item.getCardType()
                    + "，排版方式" + item.getLayoutType());

            // 图片
            List<String> images = new ArrayList<>();
            images.add("https://picsum.photos/400/300?random=" + i);
            item.setImages(images);

            // 如果是视频卡片，设置视频相关属性
            if (item.getCardType() == 3) {
                item.setVideoUrl("https://example.com/video/" + i);
                item.setVideoDuration(10 + i % 20); // 随机10-30秒
                item.setVideoCover("https://picsum.photos/400/600?random=" + i + "_cover");
            }

            item.setCreateTime(System.currentTimeMillis());
            list.add(item);
        }
        return list;
    }

    @Override
    public void deleteItem(String itemId) {
        // 模拟删除操作
        Log.d(TAG, "删除项目: " + itemId);

        // 从缓存中删除（实际项目中可能需要更新缓存）
        new Thread(() -> {
            try {
                Thread.sleep(300); // 模拟删除延迟
                Log.i(TAG, "项目删除完成: " + itemId);
            } catch (InterruptedException e) {
                Log.e(TAG, "删除操作被中断", e);
            }
        }).start();
    }

    @Override
    public void saveToCache(List<FeedItem> feedList) {
        if (feedList != null && !feedList.isEmpty()) {
            // 这里可以添加额外的缓存逻辑，比如分页缓存
            cacheManager.saveFeedList(feedList, 1, true);
            Log.d(TAG, "数据已保存到缓存，数量: " + feedList.size());
        }
    }

    @Override
    public List<FeedItem> getCache() {
        List<FeedItem> cachedData = cacheManager.getCachedFeedList();
        if (cachedData != null && !cachedData.isEmpty()) {
            Log.d(TAG, "从缓存获取数据成功，数量: " + cachedData.size());
            return cachedData;
        }
        Log.d(TAG, "缓存中没有数据");
        return new ArrayList<>(); // 返回空列表而不是null
    }
}