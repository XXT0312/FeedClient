package com.selflearn.feedclient.viewholder;

import android.graphics.Rect;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import com.selflearn.feedclient.adapter.FeedAdapter;
import com.selflearn.feedclient.model.FeedItem;
import java.util.Timer;
import java.util.TimerTask;

public abstract class BaseViewHolder extends RecyclerView.ViewHolder {

    protected FeedAdapter.ExposureListener exposureListener;
    protected String currentItemId;

    // 用于防止重复触发
    private Timer exposureTimer;
    private boolean hasReportedAppear = false;
    private boolean hasReportedAppear50 = false;
    private boolean hasReportedAppear100 = false;
    private boolean hasReportedDisappear = false;
    private float lastReportedRatio = 0f;

    public BaseViewHolder(View itemView) {
        super(itemView);
    }

    public void bind(FeedItem item) {
        this.currentItemId = item.getId();
        resetExposureFlags();
    }

    private void resetExposureFlags() {
        hasReportedAppear = false;
        hasReportedAppear50 = false;
        hasReportedAppear100 = false;
        hasReportedDisappear = false;
        lastReportedRatio = 0f;
    }

    public void setExposureListener(FeedAdapter.ExposureListener listener) {
        this.exposureListener = listener;
        setupExposureDetection();
    }

    protected void setupExposureDetection() {
        // 移除旧的监听器
        itemView.removeOnAttachStateChangeListener(null);

        itemView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                startExposureTracking();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                stopExposureTracking();
                // 触发disappear事件
                reportExposureEvent("disappear", 0f);
                hasReportedDisappear = true;
                // 重置其他标志，以便重新出现时可以再次触发
                hasReportedAppear = false;
                hasReportedAppear50 = false;
                hasReportedAppear100 = false;
            }
        });

        // 如果已经附加到窗口，立即开始追踪
        if (itemView.isAttachedToWindow()) {
            startExposureTracking();
        }
    }

    protected void startExposureTracking() {
        stopExposureTracking(); // 先停止已有的追踪

        exposureTimer = new Timer();
        exposureTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                itemView.post(() -> {
                    if (itemView.isAttachedToWindow()) {
                        float visibleRatio = calculateVisibleRatio();

                        // 只在可见比例变化时报告，减少重复事件
                        if (Math.abs(visibleRatio - lastReportedRatio) > 0.05f) {
                            // 触发不同阈值的事件
                            if (visibleRatio > 0 && !hasReportedAppear) {
                                reportExposureEvent("appear", visibleRatio);
                                hasReportedAppear = true;
                            }

                            if (visibleRatio >= 0.5f && !hasReportedAppear50) {
                                reportExposureEvent("appear_50", visibleRatio);
                                hasReportedAppear50 = true;
                            }

                            if (visibleRatio >= 0.95f && !hasReportedAppear100) { // 使用95%避免精度问题
                                reportExposureEvent("appear_100", visibleRatio);
                                hasReportedAppear100 = true;
                            }

                            lastReportedRatio = visibleRatio;
                        }
                    }
                });
            }
        }, 0, 300); // 每300ms检测一次
    }

    protected void stopExposureTracking() {
        if (exposureTimer != null) {
            exposureTimer.cancel();
            exposureTimer = null;
        }
    }

    /**
     * 曝光事件回调（子类可重写）
     */
    protected void onExposureEvent(String eventType, float exposureRatio) {
        // 默认实现，子类可重写
    }

    // 在reportExposureEvent方法中添加调用
    private void reportExposureEvent(String eventType, float ratio) {
        if (exposureListener != null && currentItemId != null) {
            exposureListener.onExposure(currentItemId, eventType, ratio);
        }

        // 调用子类的曝光事件处理
        onExposureEvent(eventType, ratio);
    }

    /**
     * 计算卡片在屏幕上的可见比例
     * @return 0.0f ~ 1.0f，表示可见比例
     */
    private float calculateVisibleRatio() {
        if (itemView == null || !itemView.isShown() || !itemView.isAttachedToWindow()) {
            return 0f;
        }

        try {
            Rect rect = new Rect();
            boolean isVisible = itemView.getGlobalVisibleRect(rect);

            if (!isVisible || rect.isEmpty()) {
                return 0f;
            }

            int viewHeight = itemView.getHeight();
            if (viewHeight <= 0) {
                return 0f;
            }

            // 计算可见高度
            int visibleHeight = rect.height();
            float visibleRatio = (float) visibleHeight / viewHeight;

            // 限制在0~1之间
            visibleRatio = Math.max(0f, Math.min(1f, visibleRatio));

            return visibleRatio;
        } catch (Exception e) {
            return 0f;
        }
    }
}