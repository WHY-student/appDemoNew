package com.gdu.demo.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.gdu.demo.R;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
    private List<ImageItem> imageItems;
    private Context context;
    private static final int TYPE_ITEM = 0;
    private static final int TYPE_EMPTY = 1;

    public ImageAdapter(List<ImageItem> imageItems, Context context) {
        this.imageItems = imageItems;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_ITEM) {
            // 正常绑定图片
            ImageItem item = imageItems.get(position);
            Glide.with(context)
                    .load("file:///android_asset/" + item.getAssetFileName())
                    .into(holder.imageView);
            holder.textView.setText(item.getLabel());
        } else {
            // 空项处理（透明占位）
            holder.imageView.setImageDrawable(null);
            holder.textView.setText("");
            holder.itemView.setBackgroundResource(android.R.color.transparent);
        }
    }

    @Override
    public int getItemCount() {
        return imageItems.size();
    }

    // 新增方法：确保添加新图片时从新行开始
    public void addNewItemsInNewLine(List<ImageItem> newItems, int spanCount) {
        // 计算当前行已使用的格子数
        int itemsInCurrentRow = imageItems.size() % spanCount;

        // 如果当前行已经有项目，先补全当前行
        if (itemsInCurrentRow > 0) {
            // 不需要添加空项，直接让RecyclerView自动换行
            // 因为GridLayoutManager会自动处理换行
        }

        // 添加新项目
        imageItems.addAll(newItems);
        notifyItemRangeInserted(imageItems.size() - newItems.size()+1, newItems.size());
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            textView = itemView.findViewById(R.id.textView);
        }
    }
    @Override
    public int getItemViewType(int position) {
        return imageItems.get(position).getAssetFileName().isEmpty() ?
                TYPE_EMPTY : TYPE_ITEM;
    }
    public void addNewItems(List<ImageItem> newItems, GridLayoutManager layoutManager) {
        int oldSize = imageItems.size();
        int spanCount = layoutManager.getSpanCount();
        int remainingSpace = spanCount - (oldSize % spanCount);

        if (remainingSpace > 0 && remainingSpace < spanCount) {
            for (int i = 0; i < remainingSpace; i++) {
                imageItems.add(new ImageItem("", "")); // 添加空项
            }
            notifyItemRangeInserted(oldSize, remainingSpace);
        }

        int newStartPos = imageItems.size();
        imageItems.addAll(newItems);
        notifyItemRangeInserted(newStartPos, newItems.size());
    }
}