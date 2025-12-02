package com.selflearn.feedclient.model;

import java.util.List;

public class FeedItem {
    private String id;
    private int cardType;
    private int layoutType;
    private String title;
    private String content;
    private List<String> images;
    private String videoUrl;
    private long createTime;

    // 添加视频相关字段
    private int videoDuration = 10; // 视频时长（秒），默认10秒
    private String videoCover; // 视频封面URL

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getCardType() { return cardType; }
    public void setCardType(int cardType) { this.cardType = cardType; }

    public int getLayoutType() { return layoutType; }
    public void setLayoutType(int layoutType) { this.layoutType = layoutType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }

    // 视频相关getter/setter
    public int getVideoDuration() { return videoDuration; }
    public void setVideoDuration(int videoDuration) { this.videoDuration = videoDuration; }

    public String getVideoCover() { return videoCover; }
    public void setVideoCover(String videoCover) { this.videoCover = videoCover; }
}