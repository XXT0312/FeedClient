package com.selflearn.feedclient.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.selflearn.feedclient.R;
import com.selflearn.feedclient.adapter.FeedAdapter;
import com.selflearn.feedclient.contract.FeedContract;
import com.selflearn.feedclient.model.FeedModel;
import com.selflearn.feedclient.presenter.FeedPresenter;
import com.selflearn.feedclient.utils.CardRenderManager;
import com.selflearn.feedclient.utils.ExposureLogManager;
import com.selflearn.feedclient.model.FeedItem;
import com.selflearn.feedclient.utils.LocalCacheManager;
import com.selflearn.feedclient.utils.PreloadManager;

import java.util.List;

public class MainActivity extends AppCompatActivity implements FeedContract.View {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private FeedAdapter feedAdapter;
    private FeedPresenter presenter;

    // 添加预加载管理器
    private PreloadManager preloadManager;
    private CardRenderManager renderManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化预加载管理器
        initPreloadManager();

        initView();
        initPresenter();
        loadData();

        // 确保SwipeRefreshLayout正确初始化
        swipeRefreshLayout.setOnRefreshListener(() -> {
            presenter.loadFeedList(true);
        });
    }

    private void initPreloadManager() {
        // 初始化预加载管理器
        preloadManager = PreloadManager.getInstance(this);
        renderManager = CardRenderManager.getInstance(this);

        // 设置预加载配置
        if (feedAdapter != null) {
            feedAdapter.initPreload(this);
            feedAdapter.setEnablePreload(true);
            feedAdapter.setPreloadThreshold(3); // 提前3个位置预加载
        }
    }

    private void initView() {
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        recyclerView = findViewById(R.id.recycler_view);

        // 使用GridLayoutManager
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);

        // 创建适配器
        feedAdapter = new FeedAdapter();
        recyclerView.setAdapter(feedAdapter);

        //  初始化预加载
        feedAdapter.initPreload(this);

        // 关键：将适配器附加到RecyclerView
        feedAdapter.attachToRecyclerView(recyclerView);

        // 设置更多的缓存
        recyclerView.setItemViewCacheSize(20); // 增加View缓存
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        // 设置SpanSizeLookup - 必须在适配器创建后设置
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                FeedItem item = feedAdapter.getItemAtPosition(position);
                if (item != null) {
                    return item.getLayoutType() == 1 ? 2 : 1;
                }
                return 1;
            }
        });

        // 设置间距
        int spacingInPixels = 8; // 8dp
        recyclerView.addItemDecoration(new MixedItemDecoration(spacingInPixels));

        swipeRefreshLayout.setOnRefreshListener(() -> {
            presenter.loadFeedList(true);
        });

        // 设置加载更多监听
        feedAdapter.setOnLoadMoreListener(() -> {
            presenter.loadMore();
        });

        // 设置删卡监听 - 只接收itemId
        feedAdapter.setOnDeleteListener(itemId -> {
            showDeleteDialog(itemId);
        });

        // 设置曝光监听 - 记录到测试工具
        feedAdapter.setExposureListener((itemId, eventType, exposureRatio) -> {
            // 记录到曝光测试工具
            ExposureLogManager.getInstance().log(itemId, eventType, exposureRatio);

            // 通知Presenter
            presenter.onExposureEvent(itemId, eventType, exposureRatio);
        });

        // 设置刷新颜色
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );
    }
    private void initPresenter() {
        presenter = new FeedPresenter(this, getApplicationContext());
    }

    private void loadData() {
        presenter.loadFeedList(false);
    }

    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showDeleteDialog(String itemId) {
        // 获取卡片信息用于显示
        FeedItem item = feedAdapter.getItemById(itemId);
        if (item == null) {
            return;
        }

        String message = String.format("确定要删除这个卡片吗？\n\n标题: %s\n类型: %s",
                item.getTitle(),
                item.getLayoutType() == 1 ? "单列" : "双列");

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("删除卡片")
                .setMessage(message)
                .setPositiveButton("确定", (dialog, which) -> {
                    presenter.deleteItem(itemId);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // 添加测试四种曝光事件的菜单项
        MenuItem testItem = menu.add("测试四种曝光事件");
        testItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        testItem.setOnMenuItemClickListener(item -> {
            testAllExposureEvents();
            return true;
        });

        // 添加缓存管理菜单项
        MenuItem cacheItem = menu.add("缓存管理");
        cacheItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        cacheItem.setOnMenuItemClickListener(item -> {
            showCacheManagementDialog();
            return true;
        });

        return true;
    }


    /**
     * 显示缓存管理对话框
     */
    private void showCacheManagementDialog() {
        FeedModel model = new FeedModel(getApplicationContext());
        LocalCacheManager cacheManager = model.getCacheManager();

        // 获取当前预加载状态
        boolean isPreloadEnabled = feedAdapter != null && feedAdapter.isPreloadEnabled();

        // 添加预加载信息
        String preloadStatus = isPreloadEnabled ? "已启用" : "已禁用";
        String preloadInfo = String.format(
                "\n\n预加载状态：%s\n" +
                        "布局缓存：3种卡片类型\n" +
                        "预加载阈值：提前3个位置\n" +
                        "图片预加载：%s",
                preloadStatus,
                isPreloadEnabled ? "启用" : "禁用"
        );

        String cacheInfo;
        if (cacheManager.hasValidCache()) {
            int cachedCount = cacheManager.getCachedFeedList() != null ?
                    cacheManager.getCachedFeedList().size() : 0;
            cacheInfo = String.format(
                    "缓存状态: 有效\n" +
                            "缓存时间: %s\n" +
                            "缓存数量: %d条\n" +
                            "缓存页码: 第%d页%s",
                    cacheManager.getCacheTimeString(),
                    cachedCount,
                    cacheManager.getCachedPage(),
                    preloadInfo
            );
        } else {
            cacheInfo = "缓存状态: 无有效缓存" + preloadInfo;
        }

        // 创建对话框
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("缓存与预加载管理")
                .setMessage(cacheInfo)
                .setPositiveButton("清空缓存", (dialog, which) -> {
                    cacheManager.clearCache();
                    if (preloadManager != null) {
                        preloadManager.clearCache();
                    }
                    if (renderManager != null) {
                        renderManager.clearCache();
                    }
                    Toast.makeText(this, "缓存已清空", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("刷新缓存", (dialog, which) -> {
                    presenter.loadFeedList(true);
                    Toast.makeText(this, "正在刷新数据并更新缓存", Toast.LENGTH_SHORT).show();
                });

        // 根据当前状态设置中间按钮的文本和动作
        if (isPreloadEnabled) {
            builder.setNeutralButton("禁用预加载", (dialog, which) -> {
                if (feedAdapter != null) {
                    feedAdapter.setEnablePreload(false);
                    Toast.makeText(this, "已禁用预加载", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            builder.setNeutralButton("启用预加载", (dialog, which) -> {
                if (feedAdapter != null) {
                    feedAdapter.setEnablePreload(true);
                    Toast.makeText(this, "已启用预加载", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // 显示对话框
        builder.show();
    }

    private void testAllExposureEvents() {
        // 测试四种曝光事件
        ExposureLogManager.getInstance().log("test_card_1", "appear", 0.1f);
        ExposureLogManager.getInstance().log("test_card_1", "appear_50", 0.6f);
        ExposureLogManager.getInstance().log("test_card_1", "appear_100", 1.0f);
        ExposureLogManager.getInstance().log("test_card_1", "disappear", 0.0f);

        Toast.makeText(this, "四种曝光事件测试已添加到日志", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_exposure_test) {
            startActivity(new Intent(this, ExposureTestActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void showLoading() {
        swipeRefreshLayout.setRefreshing(true);
    }

    private void releasePreloadResources() {
        if (preloadManager != null) {
            preloadManager.release();
        }
        if (renderManager != null) {
            renderManager.release();
        }
    }

    @Override
    public void hideLoading() {
        runOnUiThread(() -> {
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    @Override
    public void showFeedList(List<FeedItem> feedList) {
        feedAdapter.setData(feedList);
        if (feedList.isEmpty()) {
            Toast.makeText(this, "没有数据", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void showLoadMore(List<FeedItem> moreList) {
        feedAdapter.addData(moreList);
    }

    @Override
    public void showError(String message) {
        runOnUiThread(() -> {
            // 重置加载更多状态
            feedAdapter.resetLoadMoreState();

            // 停止下拉刷新动画
            swipeRefreshLayout.setRefreshing(false);

            // 显示错误提示
            Toast.makeText(this, "错误: " + message, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void removeItem(String itemId) {
        // 通过itemId删除
        boolean removed = feedAdapter.removeItemById(itemId);
        if (removed) {
            Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "删除失败，卡片不存在", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onExposureEvent(String itemId, String eventType, float exposureRatio) {
        // 这里可以添加业务逻辑，比如上报到服务器
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 暂停时停止所有视频播放
        stopAllVideos();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 释放预加载资源
        releasePreloadResources();
        // 销毁时停止所有视频播放
        stopAllVideos();
        if (presenter != null) {
            presenter.onDestroy();
        }
    }

    /**
     * 停止所有视频播放
     */
    private void stopAllVideos() {
        // 这里需要遍历所有ViewHolder并停止视频播放
        // 由于ViewHolder由RecyclerView管理，我们可以通过重置数据来停止所有视频
        // 或者添加一个广播机制，通知所有ViewHolder停止播放
        // 简化方案：重新加载数据会重置所有ViewHolder
    }
}