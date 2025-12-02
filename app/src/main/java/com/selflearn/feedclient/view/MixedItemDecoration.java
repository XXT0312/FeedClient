package com.selflearn.feedclient.view;

import android.graphics.Rect;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MixedItemDecoration extends RecyclerView.ItemDecoration {
    private int spacing;

    public MixedItemDecoration(int spacing) {
        this.spacing = spacing;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        if (position == RecyclerView.NO_POSITION) return;

        RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
        if (!(layoutManager instanceof GridLayoutManager)) return;

        GridLayoutManager gridLayoutManager = (GridLayoutManager) layoutManager;
        GridLayoutManager.LayoutParams params = (GridLayoutManager.LayoutParams) view.getLayoutParams();

        int spanSize = params.getSpanSize();
        int spanIndex = params.getSpanIndex();
        int spanCount = gridLayoutManager.getSpanCount();

        outRect.top = spacing;
        outRect.bottom = spacing;

        if (spanSize == spanCount) {
            // 单列：左右边距相同
            outRect.left = spacing;
            outRect.right = spacing;
        } else {
            // 双列
            if (spanIndex == 0) {
                // 左列
                outRect.left = spacing;
                outRect.right = spacing / 2;
            } else {
                // 右列
                outRect.left = spacing / 2;
                outRect.right = spacing;
            }
        }
    }
}