package com.selflearn.feedclient.viewholder;

import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.selflearn.feedclient.R;
import com.selflearn.feedclient.model.FeedItem;

public class CardType3ViewHolder extends BaseViewHolder {

    private TextView tvTitle;
    private TextView tvContent;
    private ImageView ivVideoCover;
    private TextView tvVideoTime;
    private View videoPlayOverlay;
    private View videoProgressBar;

    // 视频播放相关
    private CountDownTimer videoTimer;
    private boolean isVideoPlaying = false;
    private int totalVideoDuration = 0; // 视频总时长（秒）
    private int currentRemainingTime = 0; // 当前剩余时间（秒）
    private long lastPauseTime = 0; // 上次暂停的时间戳

    public CardType3ViewHolder(View itemView) {
        super(itemView);
        tvTitle = itemView.findViewById(R.id.tv_title);
        tvContent = itemView.findViewById(R.id.tv_content);
        ivVideoCover = itemView.findViewById(R.id.iv_video_cover);
        tvVideoTime = itemView.findViewById(R.id.tv_video_time);
        videoPlayOverlay = itemView.findViewById(R.id.video_play_overlay);
        videoProgressBar = itemView.findViewById(R.id.video_progress);

        // 设置点击播放/暂停
        ivVideoCover.setOnClickListener(v -> toggleVideoPlayback());
    }

    public static CardType3ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
        View view = inflater.inflate(R.layout.item_card_type3, parent, false);
        return new CardType3ViewHolder(view);
    }

    @Override
    public void bind(FeedItem item) {
        super.bind(item);

        tvTitle.setText(item.getTitle());
        tvContent.setText(item.getContent());

        // 设置视频封面
        String coverUrl = item.getVideoCover();
        if (coverUrl == null || coverUrl.isEmpty()) {
            // 如果没有单独的视频封面，使用第一张图片
            if (item.getImages() != null && !item.getImages().isEmpty()) {
                coverUrl = item.getImages().get(0);
            }
        }

        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(itemView.getContext())
                    .load(coverUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_error)
                    .into(ivVideoCover);
        }

        // 显示视频时长
        totalVideoDuration = item.getVideoDuration();
        currentRemainingTime = totalVideoDuration; // 初始时剩余时间为总时长
        updateVideoTimeDisplay(currentRemainingTime);

        // 重置播放状态
        resetVideoPlayback();
    }

    @Override
    protected void onExposureEvent(String eventType, float exposureRatio) {
        super.onExposureEvent(eventType, exposureRatio);

        // 根据曝光事件控制视频播放
        switch (eventType) {
            case "appear_50":
                // 露出超过50%时自动播放
                if (!isVideoPlaying && currentRemainingTime > 0) {
                    startVideoPlayback();
                }
                break;
            case "disappear":
                // 离开屏幕时停止播放
                if (isVideoPlaying) {
                    stopVideoPlayback();
                }
                break;
        }
    }

    /**
     * 切换视频播放状态
     */
    private void toggleVideoPlayback() {
        if (isVideoPlaying) {
            stopVideoPlayback();
        } else {
            startVideoPlayback();
        }
    }

    /**
     * 开始视频播放（模拟）
     */
    private void startVideoPlayback() {
        if (isVideoPlaying) return;

        isVideoPlaying = true;
        videoPlayOverlay.setVisibility(View.VISIBLE);
        videoProgressBar.setVisibility(View.VISIBLE);

        // 开始倒计时模拟播放，从当前剩余时间开始
        if (videoTimer != null) {
            videoTimer.cancel();
        }

        videoTimer = new CountDownTimer(currentRemainingTime * 1000L, 100) { // 每100ms更新一次
            @Override
            public void onTick(long millisUntilFinished) {
                // 计算剩余秒数
                currentRemainingTime = (int) (millisUntilFinished / 1000);
                updateVideoTimeDisplay(currentRemainingTime);

                // 更新进度条
                float progress = 1.0f - (float) currentRemainingTime / totalVideoDuration;
                updateProgressBar(progress);
            }

            @Override
            public void onFinish() {
                // 播放完成，重置
                currentRemainingTime = 0;
                resetVideoPlayback();
            }
        }.start();
    }

    /**
     * 停止视频播放（暂停）
     */
    private void stopVideoPlayback() {
        if (!isVideoPlaying) return;

        isVideoPlaying = false;
        videoPlayOverlay.setVisibility(View.GONE);
        videoProgressBar.setVisibility(View.GONE);

        if (videoTimer != null) {
            videoTimer.cancel();
            videoTimer = null;
        }

        // 记录暂停时的剩余时间，以便下次从当前位置继续播放
        // 注意：这里不需要更新currentRemainingTime，因为已经在onTick中更新了
    }

    /**
     * 重置视频播放状态（重新开始）
     */
    private void resetVideoPlayback() {
        if (isVideoPlaying) {
            stopVideoPlayback();
        }

        // 重置剩余时间为总时长
        currentRemainingTime = totalVideoDuration;
        updateVideoTimeDisplay(currentRemainingTime);

        // 重置进度条
        if (videoProgressBar != null) {
            ViewGroup.LayoutParams params = videoProgressBar.getLayoutParams();
            if (params != null) {
                params.width = 0;
                videoProgressBar.setLayoutParams(params);
            }
        }
    }

    /**
     * 更新视频时间显示
     */
    private void updateVideoTimeDisplay(int seconds) {
        if (tvVideoTime != null) {
            if (seconds > 0) {
                tvVideoTime.setText(formatTime(seconds));
                tvVideoTime.setVisibility(View.VISIBLE);
            } else {
                tvVideoTime.setText("00:00");
                tvVideoTime.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * 格式化时间显示
     */
    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }

    /**
     * 更新进度条
     */
    private void updateProgressBar(float progress) {
        if (videoProgressBar != null) {
            ViewGroup.LayoutParams params = videoProgressBar.getLayoutParams();
            if (params != null) {
                // 根据进度计算宽度
                View parentView = (View) videoProgressBar.getParent();
                if (parentView != null) {
                    int parentWidth = parentView.getWidth();
                    int newWidth = (int) (parentWidth * progress);
                    params.width = Math.max(0, newWidth);
                    videoProgressBar.setLayoutParams(params);
                }
            }
        }
    }

    /**
     * 重新开始播放（重置到开头）
     */
    public void restartVideoPlayback() {
        resetVideoPlayback();
        startVideoPlayback();
    }

    @Override
    protected void stopExposureTracking() {
        super.stopExposureTracking();
        // 停止曝光追踪时也停止视频播放
        stopVideoPlayback();
    }
}