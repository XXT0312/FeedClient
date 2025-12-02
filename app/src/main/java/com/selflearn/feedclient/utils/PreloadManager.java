package com.selflearn.feedclient.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * XML布局预加载管理器
 * 用于异步预加载布局文件，减少UI线程的卡顿
 */
public class PreloadManager {
    private static final String TAG = "PreloadManager";
    private static PreloadManager instance;
    
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final LayoutInflater layoutInflater;
    private final Map<Integer, Queue<View>> layoutCache;
    private final int maxCacheSize = 5; // 每种布局最多缓存5个
    
    private PreloadManager(Context context) {
        this.executorService = Executors.newFixedThreadPool(2);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.layoutInflater = LayoutInflater.from(context.getApplicationContext());
        this.layoutCache = new HashMap<>();
    }
    
    public static PreloadManager getInstance(Context context) {
        if (instance == null) {
            synchronized (PreloadManager.class) {
                if (instance == null) {
                    instance = new PreloadManager(context);
                }
            }
        }
        return instance;
    }
    
    /**
     * 预加载布局文件（异步）
     */
    public void preloadLayout(@LayoutRes int layoutRes, int count) {
        executorService.execute(() -> {
            try {
                for (int i = 0; i < count; i++) {
                    // 在子线程加载布局
                    View view = layoutInflater.inflate(layoutRes, null, false);
                    
                    // 测量布局（可选，提前计算尺寸）
                    view.measure(
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    );
                    
                    // 添加到缓存
                    synchronized (layoutCache) {
                        Queue<View> cache = layoutCache.get(layoutRes);
                        if (cache == null) {
                            cache = new LinkedList<>();
                            layoutCache.put(layoutRes, cache);
                        }
                        
                        if (cache.size() < maxCacheSize) {
                            cache.offer(view);
                            Log.d(TAG, "预加载布局成功: " + layoutRes + ", 缓存数量: " + cache.size());
                        } else {
                            Log.d(TAG, "布局缓存已满: " + layoutRes);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "预加载布局失败: " + layoutRes, e);
            }
        });
    }
    
    /**
     * 从缓存获取预加载的View
     */
    public View getCachedView(@LayoutRes int layoutRes) {
        synchronized (layoutCache) {
            Queue<View> cache = layoutCache.get(layoutRes);
            if (cache != null && !cache.isEmpty()) {
                View view = cache.poll();
                Log.d(TAG, "从缓存获取View: " + layoutRes + ", 剩余缓存: " + cache.size());
                
                // 异步补充缓存
                if (cache.size() < 2) {
                    preloadLayout(layoutRes, 1);
                }
                
                return view;
            }
        }
        return null;
    }
    
    /**
     * 预加载多个布局类型
     */
    public void preloadMultipleLayouts(@LayoutRes int... layoutResArray) {
        executorService.execute(() -> {
            for (int layoutRes : layoutResArray) {
                preloadLayout(layoutRes, 2);
            }
        });
    }
    
    /**
     * 清空所有缓存
     */
    public void clearCache() {
        synchronized (layoutCache) {
            layoutCache.clear();
            Log.d(TAG, "已清空所有布局缓存");
        }
    }
    
    /**
     * 释放资源
     */
    public void release() {
        executorService.shutdown();
        clearCache();
    }
}