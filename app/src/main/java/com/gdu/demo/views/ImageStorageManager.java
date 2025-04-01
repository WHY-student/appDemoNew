package com.gdu.demo.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ImageStorageManager {
    private static final String IMAGE_DIR = "cropped_images";
    private final Context context;

    public ImageStorageManager(Context context) {
        this.context = context;
        createImageDir();
    }

    // 创建存储目录（模拟assets行为）
    private void createImageDir() {
        File dir = new File(context.getFilesDir(), IMAGE_DIR);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    // 保存Bitmap并返回编号
    public int saveImage(Bitmap bitmap) {
        File dir = new File(context.getFilesDir(), IMAGE_DIR);
        int nextNumber = getNextNumber(dir);
        String fileName = nextNumber + ".png"; // 使用PNG格式保留透明度
        File outputFile = new File(dir, fileName);

        try (OutputStream fos = new FileOutputStream(outputFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            return nextNumber;
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return -1;
        }
    }

    // 获取下一个编号
    private int getNextNumber(File dir) {
        File[] files = dir.listFiles();
        return (files == null || files.length == 0) ? 1 : files.length + 1;
    }

    // 加载图片到ImageView
    public void loadImageToView(int number, ImageView imageView) {
        File imageFile = new File(context.getFilesDir(), IMAGE_DIR + "/" + number + ".png");
        if (imageFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                Toast.makeText(context, "图片加载失败", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "图片不存在: " + number, Toast.LENGTH_SHORT).show();
        }
    }


    // 清除所有图片
    public void clearAllImages() {
        // 清空ImageView
//        for (ImageView iv : imageViews) {
//            iv.setImageDrawable(null);
//        }

        // 删除文件
        File dir = new File(context.getFilesDir(), IMAGE_DIR);
        if (dir.exists()) {
            for (File file : dir.listFiles()) {
                file.delete();
            }
        }
        Toast.makeText(context, "已清除所有图片", Toast.LENGTH_SHORT).show();
    }

    public String getImagePath(int number) {
        File imageFile = new File(context.getFilesDir(), IMAGE_DIR + "/" + number + ".png");
        if (imageFile.exists()) {
            return IMAGE_DIR + "/" + number + ".png"; // 返回相对路径
        } else {
            return null;
        }

    }

    public String getImageAbsolutePath(int number) {
        File imageFile = new File(context.getFilesDir(), IMAGE_DIR + "/" + number + ".png");
        return imageFile.exists() ? imageFile.getAbsolutePath() : null;
    }
}