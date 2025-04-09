package com.gdu.demo.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageStorageManager {
    private final Context context;

    public ImageStorageManager(Context context) {
        this.context = context;
    }

    //====================== 核心方法 ======================//
    /**
     * 保存图片到指定目录，自动生成从1开始的编号
     * @param bitmap  图片
     * @param dirName 目录名（如 "crops"）
     * @return 生成的文件名（如 "1.png"），失败返回null
     */
    public int saveImage(Bitmap bitmap, String dirName) {
        File dir = ensureDirExists(dirName);
        int nextNumber = getNextNumber(dir);
        String fileName = nextNumber + ".png";
        File outputFile = new File(dir, fileName);

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            return nextNumber;
        } catch (IOException e) {
            showToast("保存失败: " + e.getMessage());
            return -1;
        }
    }
    private int getNextNumber(File dir) {
        File[] files = dir.listFiles();
        return (files == null || files.length == 0) ? 1 : files.length + 1;
    }

    /**
     * 加载指定目录的编号图片
     * @param dirName  目录名
     * @param number   文件编号（如 1, 2, 3...）
     * @return Bitmap对象，失败返回null
     */
    public Bitmap loadImage(String dirName, int number) {
        String fileName = number + ".png";
        File imageFile = new File(getDirPath(dirName), fileName);
        return imageFile.exists() ? BitmapFactory.decodeFile(imageFile.getAbsolutePath()) : null;
    }

    //====================== 删除/清空 ======================//
    /**
     * 删除指定目录的编号图片
     * @param dirName 目录名
     * @param number  文件编号
     * @return 是否删除成功
     */
    public boolean deleteImage(String dirName, int number) {
        String fileName = number + ".png";
        File file = new File(getDirPath(dirName), fileName);
        return file.exists() && file.delete();
    }

    /**
     * 清空指定目录（保留目录本身）
     * @param dirName 目录名
     */
    public void clearDirectory(String dirName) {
        File dir = getDir(dirName);
        if (dir.exists()) {
            for (File file : dir.listFiles()) {
                file.delete();
            }
        }
    }

    //====================== 路径查询 ======================//
    /**
     * 获取文件的绝对路径
     * @param dirName 目录名
     * @param number  文件编号
     * @return 完整路径，如 "/data/.../crops/1.png"
     */
    public String getAbsolutePath(String dirName, int number) {
        String fileName = number + ".png";
        File file = new File(getDirPath(dirName), fileName);
        return file.exists() ? file.getAbsolutePath() : null;
    }

    //====================== 辅助方法 ======================//
    private File ensureDirExists(String dirName) {
        File dir = getDir(dirName);
        if (!dir.exists()) dir.mkdir();
        return dir;
    }

    private File getDir(String dirName) {
        return new File(context.getFilesDir(), dirName);
    }

    private String getDirPath(String dirName) {
        return getDir(dirName).getAbsolutePath();
    }

    private String generateNextFileName(File dir) {
        File[] files = dir.listFiles();
        int nextNumber = (files == null || files.length == 0) ? 1 : files.length + 1;
        return nextNumber + ".png";
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    //====================== 兼容ImageView直接加载 ======================//
    /**
     * 加载图片到ImageView
     * @param dirName   目录名
     * @param number    文件编号
     * @param imageView 目标ImageView
     */
    public void loadImageToView(String dirName, int number, ImageView imageView) {
        Bitmap bitmap = loadImage(dirName, number);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            showToast("图片不存在: " + dirName + "/" + number + ".png");
        }
    }
}