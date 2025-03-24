package com.gdu.demo.views;

public class ImageItem {
    private int imageResId; // 图片资源 ID
    private String label;    // 标签文本

    public ImageItem(int imageResId, String label) {
        this.imageResId = imageResId;
        this.label = label;
    }

    public int getImageResId() {
        return imageResId;
    }

    public String getLabel() {
        return label;
    }
}