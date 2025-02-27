package com.gdu.demo.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 自定义seekbar组件
 */
//@SuppressLint("AppCompatCustomView")
public class focalGduSeekBar extends androidx.appcompat.widget.AppCompatSeekBar {
    private int x, y, z, w;

    public focalGduSeekBar(Context context) {
        super(context);
    }

    public focalGduSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public focalGduSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

//    @Override
//    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
//        super.onSizeChanged(h, w, oldh, oldw);
//    }


//    @Override
//    protected synchronized void onDraw(Canvas canvas) {
//
//        Drawable originalThumb = getThumb();
//        // 临时清除 Thumb，阻止系统绘制原来的滑块
//        setThumb(null);
//
//        // 先保存状态
//        canvas.save();
//        // 旋转画布绘制背景轨道
//        canvas.rotate(-90);
//        canvas.translate(-getHeight(), 0);
//        // 调用 super 绘制轨道和进度（背景部分旋转后显示正常）
//        super.onDraw(canvas);
//        // 恢复画布状态，这样后面绘制的不会被旋转
//        canvas.restore();
//
//        setThumb(originalThumb);
//
//        // 单独绘制滑块 Thumb（保证不旋转，文字保持水平）
//        if (originalThumb != null) {
//            // 计算 Thumb 的位置
//            int availableHeight = getHeight() - 100;
//            float progressRatio = (float) getProgress() / getMax();
//            // 注意：进度越大，Thumb 越往顶部移动
//            int thumbY = 50 + (int) ((1 - progressRatio) * availableHeight);
//            int thumbX = getWidth() / 2;
//
//            // 根据 Thumb 的固有大小设置边界
//            int thumbHeight = originalThumb.getIntrinsicHeight();
//            int thumbWidth = originalThumb.getIntrinsicWidth();
//            int left = thumbX - thumbWidth / 2;
//            int top = thumbY - thumbHeight / 2;
//            originalThumb.setBounds(left, top, left + thumbWidth, top + thumbHeight);
//            originalThumb.draw(canvas);
//        }
//    }
//
//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        if (!isEnabled()) return false;
//        switch (event.getAction()) {
//            case MotionEvent.ACTION_DOWN:
//            case MotionEvent.ACTION_MOVE:
//            case MotionEvent.ACTION_UP:
//                int availableHeight = getHeight()-100;
//                int y = (int) event.getY();
//                // 限制 y 的范围
//                if (y < 50) y = 50;
//                if (y > getHeight() - 50) y = getHeight() - 50;
//                float ratio = (float) (getHeight() - 50 - y) / availableHeight;
//                int progress = (int) (ratio * getMax());
//                setProgress(progress);
//                // 刷新界面
//                onSizeChanged(getWidth(), getHeight(), 0, 0);
//                break;
//            case MotionEvent.ACTION_CANCEL:
//                break;
//        }
//        return true;
//    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        if (!isEnabled()) {
//            return false;
//        }
//
//        switch (event.getAction()) {
//            case MotionEvent.ACTION_DOWN:
//
//                setSelected(true);
//                setPressed(true);
//                break;
//            case MotionEvent.ACTION_MOVE:
////                setProgress(getMax() - (int) (getMax() * event.getY() / getHeight()));
////                onSizeChanged(getWidth(), getHeight(), 0, 0);
//                super.onTouchEvent(event);
//                break;
//            case MotionEvent.ACTION_UP:
//                setSelected(false);
//                setPressed(false);
//                break;
//
//            case MotionEvent.ACTION_CANCEL:
//                break;
//        }
//        return true;
//    }

//    @Override
//    public synchronized void setProgress(int progress) {
//
//        if (progress >= 0) {
//            super.setProgress(progress);
//        } else {
//            super.setProgress(0);
//        }
//        onSizeChanged(x, y, z, w);
//    }

}
