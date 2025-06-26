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

    public List<String> getAttributeLabelNew() {
        return attribute_label_new;
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
//    List<String> class_label = new ArrayList<>();
//    {
//        class_label.add("bus");
//        class_label.add("car");
//        class_label.add("SUV");
//        class_label.add("pickup");
//        class_label.add("new1");
//        class_label.add("new2");
//        class_label.add("saved");
//        class_label.add("unknown");
//    }

    List<String> class_label = new ArrayList<>();
    {
        class_label.add("尼米兹航空母舰");
        class_label.add("类2");
        class_label.add("新类1");
        class_label.add("未知类别");
        class_label.add("未知类别");
        class_label.add("类6");
        class_label.add("类7");
        class_label.add("类8");
        class_label.add("类9");
        class_label.add("类10");
        class_label.add("类11");
        class_label.add("类12");
        class_label.add("类13");
        class_label.add("类14");
        class_label.add("类15");
        class_label.add("类16");
        class_label.add("类17");
        class_label.add("类18");
        class_label.add("类19");
//        class_label.add("库兹涅佐夫号");
        class_label.add("新类1");
        class_label.add("保存类别");
        class_label.add("未知类");
    }

    List<String> attribute_label1 = new ArrayList<>();
    {
        attribute_label1.add("机翼类型: 固定翼");
        attribute_label1.add("机翼类型: 可变后掠翼");
        attribute_label1.add("机翼位置: 上单翼");
        attribute_label1.add("机翼位置: 中单翼");
        attribute_label1.add("机翼位置: 下单翼");
        attribute_label1.add("机翼形状: 平直翼");
        attribute_label1.add("机翼形状: 梯形翼");
        attribute_label1.add("机翼形状: 切尖三角翼");
        attribute_label1.add("机翼形状: 菱形翼");
        attribute_label1.add("前缘形态: 平直");
        attribute_label1.add("前缘形态: 后掠");
        attribute_label1.add("后缘形态: 前掠");
        attribute_label1.add("后缘形态: 平直");
        attribute_label1.add("后缘形态: 后掠");
        attribute_label1.add("展弦比: 小");
        attribute_label1.add("展弦比: 中");
        attribute_label1.add("展弦比: 大");
        attribute_label1.add("具备上反角");
        attribute_label1.add("具备下反角");
        attribute_label1.add("发动机类型: 涡轮螺旋桨");
        attribute_label1.add("发动机类型: 涡轮风扇");
        attribute_label1.add("发动机类型: 涡轮发动机");
        attribute_label1.add("发动机位置: 翼前");
        attribute_label1.add("发动机位置: 翼后");
        attribute_label1.add("发动机位置: 尾部");
        attribute_label1.add("发动机数量: 1");
        attribute_label1.add("发动机数量: 2");
        attribute_label1.add("发动机数量: 4");
        attribute_label1.add("发动机数量: 8");
        attribute_label1.add("机头形状: 钝锥形");
        attribute_label1.add("机头形状: 圆形");
        attribute_label1.add("机头形状: 尖锥形");
        attribute_label1.add("整体造型: 流线型");
        attribute_label1.add("颜色: 白色");
        attribute_label1.add("颜色: 灰色");
        attribute_label1.add("驾驶舱: 气泡型");
        attribute_label1.add("驾驶舱: 阶梯型");
        attribute_label1.add("尾翼: 水平式");
        attribute_label1.add("尾翼: 三角式");
        attribute_label1.add("尾翼: 分裂式");
    }

    List<String> attribute_label2 = new ArrayList<>();
    {
        attribute_label2.add("舰岛位置: 船首右侧");
        attribute_label2.add("舰岛位置: 右舷中部");
        attribute_label2.add("舰岛位置: 船尾右侧");
        attribute_label2.add("舰岛方位: 中部");
        attribute_label2.add("舰岛尺寸: 中等");
        attribute_label2.add("舰岛尺寸: 大型");
        attribute_label2.add("船头形状: 梯形");
        attribute_label2.add("船头形状: 方形");
        attribute_label2.add("船头形状: 尖形");
        attribute_label2.add("船体剖面: 对称型");
        attribute_label2.add("船体剖面: 非对称型");
        attribute_label2.add("甲板纹理: 类型1");
        attribute_label2.add("甲板纹理: 类型2");
        attribute_label2.add("甲板纹理: 类型3");
        attribute_label2.add("甲板纹理: 类型4");
        attribute_label2.add("武装状态: 配备武器");
        attribute_label2.add("甲板炮位置: 前部");
        attribute_label2.add("直升机甲板位置: 前部");
        attribute_label2.add("直升机甲板位置: 后部");
        attribute_label2.add("具备集成上层建筑");
        attribute_label2.add("甲板尺寸: 大型");
        attribute_label2.add("甲板尺寸: 小型");
    }

    List<String> attribute_label3 = new ArrayList<>();
    {
        attribute_label3.add("有轮式");
        attribute_label3.add("有履带式");
        attribute_label3.add("四轮驱动");
        attribute_label3.add("六轮驱动");
        attribute_label3.add("八轮驱动");
        attribute_label3.add("十轮驱动");
        attribute_label3.add("有装甲");
        attribute_label3.add("有加农炮");
        attribute_label3.add("有大型火炮");
        attribute_label3.add("炮管数量: 1");
        attribute_label3.add("炮管数量: 4");
        attribute_label3.add("炮管长度: 短");
        attribute_label3.add("炮管长度: 中等");
        attribute_label3.add("炮管长度: 长");
        attribute_label3.add("导弹发射器数量: 1");
        attribute_label3.add("导弹发射器数量: 多个");
        attribute_label3.add("有侧门");
        attribute_label3.add("有舱口");
        attribute_label3.add("有潜望镜");
        attribute_label3.add("功能: 两栖");
    }

    List<String> attribute_label_new = new ArrayList<>();
    {
        attribute_label_new.add("跑道");
        attribute_label_new.add("舰岛");
        attribute_label_new.add("起降平台");
        attribute_label_new.add("BVSA");
        attribute_label_new.add("舰炮");
        attribute_label_new.add("后掠翼");
        attribute_label_new.add("平直翼");
        attribute_label_new.add("三角翼");
        attribute_label_new.add("螺旋桨发动机");
        attribute_label_new.add("喷气发动机");
        attribute_label_new.add("炮管");
        attribute_label_new.add("导弹发射器");
        attribute_label_new.add("飞机升降机");
        attribute_label_new.add("近程防御武器系统");
        attribute_label_new.add("舰载机");
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
    private final Rect temp=new Rect();

    private final long timestamp = System.currentTimeMillis();

    private final Handler mainHandler; // 主线程的 Handler
    private final HandlerThread backgroundThread; // 后台线程
    private final Handler backgroundHandler; // 后台线程的 Handler

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
    }

    public void startBackgroundTask() {
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                // 在后台线程中执行任务
                try {
                    Thread.sleep(57); // 模拟耗时操作
                } catch (InterruptedException e) {
                    Log.d("backgroundHandler", e.toString());
                }

                long startTime = System.currentTimeMillis();
                if(startTime - lastTime > 300){
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

    public void stopBackgroundTask(){
        if (backgroundThread != null) {
            backgroundThread.quit();
        }
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
//            int x = (int)(detection.getLeftX()/1.5);
//            int y = (int)(detection.getLeftY() * 1200.0 / 1080.0/1.5);
//            int maxX = (int)((detection.getLeftX() + detection.getWidth())/1.5);
//            int maxY = (int)((detection.getLeftY() + detection.getHeight()) * 1200.0 / 1080.0/1.5);

            int x = detection.getLeftX();
            int y = (int)(detection.getLeftY() * 1200.0 / 1080.0);
            int maxX = (detection.getLeftX() + detection.getWidth());
            int maxY = (int)((detection.getLeftY() + detection.getHeight()) * 1200.0 / 1080.0);

            String label;
            int labelIndex = detection.getTargetType() % 16;
//                Log.d("targetType", "parseTargetAIBox: "+detection.getTargetType());
//                Log.d("labelIndex", "parseTargetAIBox: "+labelIndex);
            if (labelIndex == -1) {
                label = "未知类";
            } else if (labelIndex > 8) {
                label = "未知类";
            } else {
                label = class_label.get(labelIndex);
            }
            if (x >= 0 && y >= 0 && maxX <= 1920 && maxY <= 1200) {
                canvas.drawRect(new Rect(x, y, maxX, maxY), paint); // 绘制矩形
                if(y<5){
                    canvas.drawText(label, x, maxY + 5, paint2); // 绘制文本
                }else{
                    canvas.drawText(label, x, y - 5, paint2); // 绘制文本
                }
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
//        Log.d("interval time", "setRectParams: "+(System.currentTimeMillis()-lastTime));
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
//            int x = (int) (detection.getLeftX()/1.5);
//            int y = (int) (detection.getLeftY() * 1200.0 / 1080.0/1.5);
//            int maxX = (int) ((detection.getLeftX() + detection.getWidth())/1.5);
//            int maxY = (int) ((detection.getLeftY() + detection.getHeight()) * 1200.0 / 1080.0/1.5);
            int x = detection.getLeftX();
            int y = (int)(detection.getLeftY() * 1200.0 / 1080.0);
            int maxX = (detection.getLeftX() + detection.getWidth());
            int maxY = (int)((detection.getLeftY() + detection.getHeight()) * 1200.0 / 1080.0);
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



