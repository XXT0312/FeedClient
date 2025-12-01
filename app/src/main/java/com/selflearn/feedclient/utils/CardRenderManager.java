package com.selflearn.feedclient.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.selflearn.feedclient.model.FeedItem;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 卡片预渲染管理器
 * 用于提前渲染卡片内容，加快显示速度
 */
public class CardRenderManager {
    private static final String TAG = "CardRenderManager";
    private static CardRenderManager instance;
    
    private final ExecutorService renderExecutor;
    private final Handler mainHandler;
    private final Context context;
    private final Map<String, Bitmap> renderCache;
    private final int maxCacheSize = 20;
    
    private CardRenderManager(Context context) {
        this.renderExecutor = Executors.newFixedThreadPool(2);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.context = context.getApplicationContext();
        this.renderCache = new HashMap<>();
    }
    
    public static CardRenderManager getInstance(Context context) {
        if (instance == null) {
            synchronized (CardRenderManager.class) {
                if (instance == null) {
                    instance = new CardRenderManager(context);
                }
            }
        }
        return instance;
    }
    
    /**
     * 预渲染卡片（异步）
     */
    public void preRenderCard(FeedItem item, View cardViewTemplate) {
        if (item == null || cardViewTemplate == null) return;
        
        renderExecutor.execute(() -> {
            try {
                // 复制View进行渲染
                ViewGroup parent = (ViewGroup) cardViewTemplate.getParent();
                View renderView = duplicateView(cardViewTemplate);
                
                // 绑定数据到渲染View（需要在UI线程）
                mainHandler.post(() -> {
                    bindDataToView(renderView, item);
                    
                    // 异步渲染为Bitmap
                    renderExecutor.execute(() -> {
                        renderToBitmap(item.getId(), renderView);
                    });
                });
                
            } catch (Exception e) {
                Log.e(TAG, "预渲染卡片失败: " + item.getId(), e);
            }
        });
    }
    
    /**
     * 获取预渲染的卡片位图
     */
    public Bitmap getRenderedCard(String itemId) {
        synchronized (renderCache) {
            return renderCache.get(itemId);
        }
    }
    
    /**
     * 复制View（用于渲染）
     */
    private View duplicateView(View original) {
        try {
            // 使用反射或手动复制View属性
            View duplicate = new View(original.getContext());
            duplicate.setLayoutParams(original.getLayoutParams());
            duplicate.setBackground(original.getBackground());
            // 这里简化处理，实际需要根据具体View类型进行完整复制
            
            return duplicate;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 绑定数据到View
     */
    private void bindDataToView(View view, FeedItem item) {
        // 这里简化处理，实际需要根据具体ViewHolder类型绑定数据
        // 例如：TextView tvTitle = view.findViewById(R.id.tv_title);
        // tvTitle.setText(item.getTitle());
    }
    
    /**
     * 将View渲染为Bitmap
     */
    private void renderToBitmap(String itemId, View view) {
        try {
            // 测量View
            view.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );
            
            // 布局View
            view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
            
            // 创建Bitmap
            Bitmap bitmap = Bitmap.createBitmap(
                view.getWidth(),
                view.getHeight(),
                Bitmap.Config.ARGB_8888
            );
            
            // 绘制到Bitmap
            Canvas canvas = new Canvas(bitmap);
            view.draw(canvas);
            
            // 缓存结果
            synchronized (renderCache) {
                if (renderCache.size() >= maxCacheSize) {
                    // 移除最旧的缓存
                    String oldestKey = renderCache.keySet().iterator().next();
                    renderCache.remove(oldestKey);
                }
                renderCache.put(itemId, bitmap);
                Log.d(TAG, "卡片预渲染完成: " + itemId);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "渲染卡片失败: " + itemId, e);
        }
    }
    
    /**
     * 预加载图片资源
     */
    public void preloadImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return;
        
        renderExecutor.execute(() -> {
            try {
                Glide.with(context)
                    .asBitmap()
                    .load(imageUrl)
                    .preload();
                Log.d(TAG, "图片预加载完成: " + imageUrl);
            } catch (Exception e) {
                Log.e(TAG, "图片预加载失败: " + imageUrl, e);
            }
        });
    }
    
    /**
     * 清空渲染缓存
     */
    public void clearCache() {
        synchronized (renderCache) {
            for (Bitmap bitmap : renderCache.values()) {
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            }
            renderCache.clear();
            Log.d(TAG, "已清空渲染缓存");
        }
    }
    
    /**
     * 释放资源
     */
    public void release() {
        renderExecutor.shutdown();
        clearCache();
    }
}