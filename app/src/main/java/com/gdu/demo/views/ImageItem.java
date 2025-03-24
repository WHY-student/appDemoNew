package com.gdu.demo.views;

public class ImageItem {
    private String AssetFileName; // 图片资源 ID
    private String label;    // 标签文本

    public ImageItem(String AssetFileName, String label) {
        this.AssetFileName = AssetFileName;
        this.label = label;
    }

    public String getAssetFileName() {
        return AssetFileName;
    }

    public String getLabel() {
        return label;
    }
}