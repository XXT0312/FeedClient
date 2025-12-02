package com.selflearn.feedclient.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.selflearn.feedclient.model.FeedItem;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class LocalCacheManager {
    private static final String TAG = "LocalCacheManager";
    private static final String PREFS_NAME = "feed_client_cache";
    private static final String KEY_FEED_CACHE = "feed_list_cache";
    private static final String KEY_CACHE_TIMESTAMP = "cache_timestamp";
    private static final String KEY_LAST_PAGE = "last_page";
    private static final String KEY_HAS_MORE = "has_more";

    private static final long CACHE_EXPIRE_TIME = 30 * 60 * 1000; // 30分钟

    private static LocalCacheManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;

    public static LocalCacheManager getInstance(Context context) {
        if (instance == null) {
            synchronized (LocalCacheManager.class) {
                if (instance == null) {
                    instance = new LocalCacheManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private LocalCacheManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    /**
     * 保存Feed列表到缓存
     */
    public void saveFeedList(List<FeedItem> feedList, int page, boolean hasMore) {
        if (feedList == null || feedList.isEmpty()) {
            return;
        }

        try {
            String json = gson.toJson(feedList);
            prefs.edit()
                    .putString(KEY_FEED_CACHE, json)
                    .putLong(KEY_CACHE_TIMESTAMP, System.currentTimeMillis())
                    .putInt(KEY_LAST_PAGE, page)
                    .putBoolean(KEY_HAS_MORE, hasMore)
                    .apply();

            Log.d(TAG, "Feed列表已缓存，数量: " + feedList.size() + ", 页码: " + page);
        } catch (Exception e) {
            Log.e(TAG, "保存缓存失败", e);
        }
    }

    /**
     * 获取缓存的Feed列表
     */
    public List<FeedItem> getCachedFeedList() {
        try {
            // 检查缓存是否过期
            long lastTimestamp = prefs.getLong(KEY_CACHE_TIMESTAMP, 0);
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastTimestamp > CACHE_EXPIRE_TIME) {
                Log.d(TAG, "缓存已过期，上次缓存时间: " + lastTimestamp);
                return null;
            }

            String json = prefs.getString(KEY_FEED_CACHE, null);
            if (json == null || json.isEmpty()) {
                return null;
            }

            Type type = new TypeToken<List<FeedItem>>(){}.getType();
            List<FeedItem> cachedList = gson.fromJson(json, type);

            Log.d(TAG, "从缓存读取Feed列表，数量: " + (cachedList != null ? cachedList.size() : 0));
            return cachedList;
        } catch (Exception e) {
            Log.e(TAG, "读取缓存失败", e);
            return null;
        }
    }

    /**
     * 获取缓存的页码信息
     */
    public int getCachedPage() {
        return prefs.getInt(KEY_LAST_PAGE, 0);
    }

    /**
     * 获取是否有更多数据的缓存
     */
    public boolean getCachedHasMore() {
        return prefs.getBoolean(KEY_HAS_MORE, true);
    }

    /**
     * 清空缓存
     */
    public void clearCache() {
        prefs.edit()
                .remove(KEY_FEED_CACHE)
                .remove(KEY_CACHE_TIMESTAMP)
                .remove(KEY_LAST_PAGE)
                .remove(KEY_HAS_MORE)
                .apply();

        Log.d(TAG, "缓存已清空");
    }

    /**
     * 检查是否有有效的缓存
     */
    public boolean hasValidCache() {
        long lastTimestamp = prefs.getLong(KEY_CACHE_TIMESTAMP, 0);
        String json = prefs.getString(KEY_FEED_CACHE, null);

        if (lastTimestamp == 0 || json == null || json.isEmpty()) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        return currentTime - lastTimestamp <= CACHE_EXPIRE_TIME;
    }

    /**
     * 获取缓存时间（格式化）
     */
    public String getCacheTimeString() {
        long timestamp = prefs.getLong(KEY_CACHE_TIMESTAMP, 0);
        if (timestamp == 0) {
            return "无缓存";
        }

        long diff = System.currentTimeMillis() - timestamp;
        long minutes = diff / (60 * 1000);

        if (minutes < 1) {
            return "刚刚缓存";
        } else if (minutes < 60) {
            return minutes + "分钟前缓存";
        } else {
            long hours = minutes / 60;
            return hours + "小时前缓存";
        }
    }
}