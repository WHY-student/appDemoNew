package com.gdu.demo.views;

import android.graphics.Bitmap;

import java.io.File;

public class ImageItem {
    private String imagePath; // 资源路径（如 "images/xxx.png"）
    private String filePath;  // 本地文件绝对路径（如 "/data/.../1.png"）
    private String label;
    // 构造方法1：用于静态资源
    public ImageItem(String imagePath, String label) {
        this.imagePath = imagePath;
        this.label = label;
    }

    // 构造方法2：用于动态Bitmap
    public ImageItem(File file, String label) {
        this.filePath = file.getAbsolutePath();
        this.label = label;
    }

    // Getters
    public String getImagePath() { return imagePath; }
    public String getFilePath() { return filePath; }
    public String getLabel() { return label; }
}