package com.selflearn.feedclient.viewholder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.selflearn.feedclient.R;
import com.selflearn.feedclient.model.FeedItem;

public class CardType2ViewHolder extends BaseViewHolder {

    private TextView tvTitle;
    private TextView tvContent;
    private ImageView ivImage;

    public CardType2ViewHolder(View itemView) {
        super(itemView);
        tvTitle = itemView.findViewById(R.id.tv_title);
        tvContent = itemView.findViewById(R.id.tv_content);
        ivImage = itemView.findViewById(R.id.iv_image);
    }

    public static CardType2ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
        View view = inflater.inflate(R.layout.item_card_type2, parent, false);
        return new CardType2ViewHolder(view);
    }

    @Override
    public void bind(FeedItem item) {
        // 必须调用super.bind来设置currentItemId
        super.bind(item);

        tvTitle.setText(item.getTitle());
        tvContent.setText(item.getContent());

        if (item.getImages() != null && !item.getImages().isEmpty()) {
            Glide.with(itemView.getContext())
                    .load(item.getImages().get(0))
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_error)
                    .into(ivImage);
        }
    }
}