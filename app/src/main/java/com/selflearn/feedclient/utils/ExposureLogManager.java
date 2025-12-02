package com.selflearn.feedclient.utils;

import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExposureLogManager {
    private static final String TAG = "ExposureLogManager";
    private static ExposureLogManager instance;
    private List<String> logs = new ArrayList<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
    private int maxLogCount = 100;

    public static ExposureLogManager getInstance() {
        if (instance == null) {
            synchronized (ExposureLogManager.class) {
                if (instance == null) {
                    instance = new ExposureLogManager();
                }
            }
        }
        return instance;
    }

    private ExposureLogManager() {
        // 添加说明
        logs.add("=== 曝光事件说明 ===");
        logs.add("appear: 卡片开始露出");
        logs.add("appear_50: 卡片露出超过50%");
        logs.add("appear_100: 卡片完全露出");
        logs.add("disappear: 卡片离开屏幕");
        logs.add("================================");
        logs.add("开始时间: " + dateFormat.format(new Date()));
        logs.add("请滚动列表查看四种曝光事件");
    }

    public void log(String itemId, String eventType, float ratio) {
        String time = dateFormat.format(new Date());
        String ratioPercent = String.format(Locale.getDefault(), "%.1f", ratio * 100);

        // 根据事件类型显示不同的描述
        String eventDescription;
        switch (eventType) {
            case "appear":
                eventDescription = "卡片开始露出";
                break;
            case "appear_50":
                eventDescription = "卡片露出超过50%";
                break;
            case "appear_100":
                eventDescription = "卡片完全露出";
                break;
            case "disappear":
                eventDescription = "卡片离开屏幕";
                break;
            default:
                eventDescription = eventType;
        }

        String log = String.format(Locale.getDefault(),
                "[%s] 卡片ID:%s\n事件:%s (%s)\n曝光比例:%s%%",
                time, itemId, eventType, eventDescription, ratioPercent);

        Log.d(TAG, "记录曝光事件: " + log);

        logs.add(0, log);
        if (logs.size() > maxLogCount) {
            logs.remove(logs.size() - 1);
        }
    }

    public void clear() {
        logs.clear();
        logs.add("=== 日志已清空 ===");
        logs.add("清空时间: " + dateFormat.format(new Date()));
        logs.add("请滚动列表重新触发曝光事件");
    }

    public String getLogsAsText() {
        if (logs.isEmpty()) {
            return "暂无曝光事件记录\n\n请返回主页面滚动列表";
        }

        StringBuilder sb = new StringBuilder();
        for (String log : logs) {
            sb.append(log).append("\n\n");
        }
        return sb.toString();
    }

    public int getLogCount() {
        return logs.size();
    }
}