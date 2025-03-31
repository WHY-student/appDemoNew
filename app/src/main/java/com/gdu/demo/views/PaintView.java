package com.gdu.demo.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.gdu.config.GlobalVariable;
import com.gdu.demo.R;
import com.gdu.detect.AIModelLabel;
import com.gdu.drone.TargetMode;
import com.gdu.util.ResourceUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.List;


public class PaintView extends AppCompatImageView {
//    private  int aiState=0;

    List<TargetMode> detectionBox = new ArrayList<>();

    public List<String> getClassLabel() {
        return class_label;
    }

    public List<String> getAttributeLabel1() {
        return attribute_label1;
    }

    public List<String> getAttributeLabel2() {
        return attribute_label2;
    }

    public List<String> getAttributeLabel3() {
        return attribute_label3;
    }

    //    List<String> class_label = new ArrayList<>();
//    {
//        class_label.add("bus");
//        class_label.add("car");
//        class_label.add("SUV");
//        class_label.add("van");
//        class_label.add("small_freight_car");
//        class_label.add("small_truck");
//        class_label.add("new1");
//        class_label.add("new2");
//        class_label.add("new3");
//        class_label.add("unknown");
//    }
    List<String> class_label = new ArrayList<>();
    {
        class_label.add("bus");
        class_label.add("car");
        class_label.add("SUV");
        class_label.add("pickup");
        class_label.add("new1");
        class_label.add("new2");
        class_label.add("saved");
        class_label.add("unknown");
    }

    List<String> attribute_label1 = new ArrayList<>();
    {
        attribute_label1.add("has wing type: fixed wing");
        attribute_label1.add("has wing type: Variable Swept Wing");
        attribute_label1.add("has wing position: upper monoplane");
        attribute_label1.add("has wing position: center monoplane");
        attribute_label1.add("has wing position: lower monoplane");
        attribute_label1.add("has wing shape: flat");
        attribute_label1.add("has wing shape: trapezoidal");
        attribute_label1.add("has wing shape: tangent triangle");
        attribute_label1.add("has wing shape: cut-tip diamond");
        attribute_label1.add("has leading edge: flat");
        attribute_label1.add("has leading edge: swept back");
        attribute_label1.add("has trailing edge: forward swept");
        attribute_label1.add("has trailing edge: flat");
        attribute_label1.add("has trailing edge: swept back");
        attribute_label1.add("has spreading ratio: small");
        attribute_label1.add("has spreading ratio: medium");
        attribute_label1.add("has spreading ratio: large");
        attribute_label1.add("has upper anticline");
        attribute_label1.add("has inferior anticline");
        attribute_label1.add("has engine type: turboprop");
        attribute_label1.add("has engine type: turbofan");
        attribute_label1.add("has engine type: turbine engine");
        attribute_label1.add("has engine position: Front of Wing");
        attribute_label1.add("has engine position: behind the wing");
        attribute_label1.add("has engine position: rear");
        attribute_label1.add("has number of engines: 1");
        attribute_label1.add("has number of engines: 2");
        attribute_label1.add("has number of engines: 4");
        attribute_label1.add("has number of engines: 8");
        attribute_label1.add("has nose: blunt cone");
        attribute_label1.add("has nose: Round");
        attribute_label1.add("has nose: pointed cone");
        attribute_label1.add("has shape: fine");
        attribute_label1.add("has color: white");
        attribute_label1.add("has color: gray");
        attribute_label1.add("has cockpit: bubble");
        attribute_label1.add("has cockpit: stepped");
        attribute_label1.add("has tail: horizontal");
        attribute_label1.add("has tail: triangular");
        attribute_label1.add("has tail: split type");
    }
    List<String> attribute_label2 = new ArrayList<>();
    {
        attribute_label2.add("has position of the island: to the right of the bow");
        attribute_label2.add("has position of the island: right of amidships");
        attribute_label2.add("has position of the island: to the right of the transom");
        attribute_label2.add("has location of island: amidships");
        attribute_label2.add("has ship island size: medium");
        attribute_label2.add("has ship island size: large");
        attribute_label2.add("has bow shape: trapezoidal");
        attribute_label2.add("has bow shape: square");
        attribute_label2.add("has bow shape: sharp");
        attribute_label2.add("has hull profile: symmetrical");
        attribute_label2.add("has hull profile: asymmetric");
        attribute_label2.add("has deck texture: type1");
        attribute_label2.add("has deck texture: type 2");
        attribute_label2.add("has deck texture: type 3");
        attribute_label2.add("has deck texture: type 4");
        attribute_label2.add("has weapons status: armed");
        attribute_label2.add("has deck gun position: forward");
        attribute_label2.add("has helideck position: forward");
        attribute_label2.add("has helideck position: aft");
        attribute_label2.add("has integrated ship building structure");
        attribute_label2.add("has deck size: large");
        attribute_label2.add("has deck size: small");
    }
    List<String> attribute_label3 = new ArrayList<>();
    {
        attribute_label3.add("has wheeled");
        attribute_label3.add("has shoes and belt");
        attribute_label3.add("has four wheels");
        attribute_label3.add("has six wheels");
        attribute_label3.add("has eight wheels");
        attribute_label3.add("has ten wheels");
        attribute_label3.add("has armor");
        attribute_label3.add("has cannon");
        attribute_label3.add("has large artillery");
        attribute_label3.add("has number of barrels: 1");
        attribute_label3.add("has number of barrels: 4");
        attribute_label3.add("has barrel length: short");
        attribute_label3.add("has barrel length: medium");
        attribute_label3.add("has barrel length: long");
        attribute_label3.add("has number of missile launchers: 1");
        attribute_label3.add("has number of missile launchers: multiple");
        attribute_label3.add("has side entrances");
        attribute_label3.add("has hatches");
        attribute_label3.add("has periscope");
        attribute_label3.add("has function: amphibious");
    }
//    List<String> gdu_class_label = new ArrayList<>();
//    {
//        gdu_class_label.add(ResourceUtil.getStringById(R.string.target_label_0000));
//        gdu_class_label.add(ResourceUtil.getStringById(R.string.target_label_0001));
//        gdu_class_label.add(ResourceUtil.getStringById(R.string.target_label_0002));
//        gdu_class_label.add(ResourceUtil.getStringById(R.string.target_label_0003));
//        gdu_class_label.add(ResourceUtil.getStringById(R.string.target_label_0004));
//        gdu_class_label.add(ResourceUtil.getStringById(R.string.target_label_0005));
//        gdu_class_label.add(ResourceUtil.getStringById(R.string.target_label_0006));
//    }
    private Rect temp=new Rect();

    private long timestamp = System.currentTimeMillis();

    private Handler mainHandler; // 主线程的 Handler
    private HandlerThread backgroundThread; // 后台线程
    private Handler backgroundHandler; // 后台线程的 Handler

    private long lastTime;

    public PaintView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        // 初始化主线程的 Handler
        mainHandler = new Handler(Looper.getMainLooper());

//         创建并启动后台线程
        backgroundThread = new HandlerThread("BackgroundThread");
        backgroundThread.start();
        // 初始化后台线程的 Handler
        backgroundHandler = new Handler(backgroundThread.getLooper());
        GlobalVariable.targetDetectLabels = new ArrayList<>();
        // 启动后台任务
        startBackgroundTask();


//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (true) {
//                    try {
//
//                        // 在后台线程中更新数据后，通知UI线程重绘
//                        handler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                PaintView.this.invalidate();  // 触发视图重绘
//                            }
//                        });
//
//                        // 等待一段时间后继续执行
//                        Thread.sleep(30);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }).start();
    }
    private void startBackgroundTask() {
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                // 在后台线程中执行任务
                try {
                    Thread.sleep(57); // 模拟耗时操作
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                long startTime = System.currentTimeMillis();
                if(startTime - lastTime > 150){
                    // 如果超过一定时间没有检测到物体，则清空物体box
                    PaintView.this.detectionBox = new ArrayList<>();
                }

                // 通过主线程的 Handler 调用 invalidate()
                //long startTime = System.currentTimeMillis();
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        invalidate(); // 请求重绘
                    }

                });
                //long endTime = System.currentTimeMillis();

                // 计算并输出 onDraw 执行时间
                //long drawDuration = endTime - startTime;
                //Log.d("BackgroundTaskTime", "backgroundTask" + drawDuration + " ms");

                // 重复执行任务
//                long startTime = System.currentTimeMillis();
                backgroundHandler.post(this);
//                long endTime = System.currentTimeMillis();

                // 计算并输出 onDraw 执行时间
//                long drawDuration = endTime - startTime;
//                Log.d("BKTaskTime", "backgroundTask" + drawDuration + " ms");
            }
        });

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // 停止后台线程
        if (backgroundThread != null) {
            backgroundThread.quit();
        }
    }

    Paint paint = new Paint();
    {
        paint.setAntiAlias(true);//用于防止边缘的锯齿
        paint.setColor(Color.RED);//设置颜色
        paint.setStyle(Paint.Style.STROKE);//设置样式为空心矩形
        paint.setStrokeWidth(5f);//设置空心矩形边框的宽度
        paint.setAlpha(1000);//设置透明度
    }

    Paint paint2 = new Paint();
    {
        paint2.setAntiAlias(true);//用于防止边缘的锯齿
        paint2.setColor(Color.RED);//设置颜色
        paint2.setStrokeWidth(3f);//设置空心矩形边框的宽度
        paint2.setTextSize(40f);
        paint2.setAlpha(1000);//设置透明度
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        // 记录开始时间，只调用onDraw会没有延迟
        long startTime = System.currentTimeMillis();

        super.onDraw(canvas);

        // 绘制逻辑 1920*1080      1920*1152      1920*1200        1080-》1200
        for (TargetMode detection : this.detectionBox) {
            int x = (int)(detection.getLeftX());
            int y = (int)(detection.getLeftY() * 1200.0 / 1080.0);
            int maxX = (int)((detection.getLeftX() + detection.getWidth()));
            int maxY = (int)((detection.getLeftY() + detection.getHeight()) * 1200.0 / 1080.0);
//            aiState =detection.getId();
            String label = null;
            if (label == null) {

                int labelIndex = detection.getTargetType() % 16;
                if (labelIndex == -1) {
                    label = "unknown";
                } else if (labelIndex > 9) {
                    label = "test";
                } else {
                    label = class_label.get(labelIndex);
                }
            }
            if (x >= 0 && y >= 0 && maxX <= 1920 && maxY <= 1200) {
                canvas.drawRect(new Rect(x, y, maxX, maxY), paint); // 绘制矩形
                canvas.drawText(label, x, y - 5, paint2); // 绘制文本
            }
//            Log.d("detection id", "检测框id " + aiState + " ms");
//            Log.d("detectionifo", "长宽 " + x+y+ " ms");
        }

        // 清空检测框数据
//        this.detectionBox = new ArrayList<>();

        // 记录结束时间
//        long endTime = System.currentTimeMillis();
//
//        // 计算并输出 onDraw 执行时间
//        long drawDuration = endTime - startTime;
//        Log.d("detection id", "检测框id " + id + " ms");
    }

    public void setRectParams(List<TargetMode> detectionBox) {
        lastTime = System.currentTimeMillis();
        if (detectionBox == null || detectionBox.isEmpty()) {
            // 如果没有目标，清空 detectionBox
            this.detectionBox = new ArrayList<>();
        } else {
            // 如果有目标，更新 detectionBox
//            this.detectionBox = new ArrayList<>();
            this.detectionBox = new ArrayList<>(detectionBox);
        }
//        this.detectionBox = detectionBox;
//        long drawTime = System.currentTimeMillis();
//        if(drawTime - this.timestamp > 100){
//            this.timestamp = drawTime;
//            invalidate();
//        }
        //long receiveTime = System.currentTimeMillis();
        //Log.d("PaintView", "Data received at: " + receiveTime);
//        invalidate();
    }

    public TargetMode getTargetModebyXY(int clickX, int clickY){
        for (TargetMode detection : this.detectionBox) {
            int x = (int) (detection.getLeftX());
            int y = (int) (detection.getLeftY() * 1200.0 / 1080.0);
            int maxX = (int) ((detection.getLeftX() + detection.getWidth()));
            int maxY = (int) ((detection.getLeftY() + detection.getHeight()) * 1200.0 / 1080.0);
            if(clickX>=x && clickX<= maxX && clickY>=y && clickY<=maxY){
                return detection;
            }
        }
        return null;
    }

//    public int getModelID(){
//        return this.aiState;
//    }
}



