package com.gdu.demo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.gdu.beans.WarnBean;
import com.gdu.camera.SettingsDefinitions;
import com.gdu.camera.StorageState;
import com.gdu.common.error.GDUError;
import com.gdu.config.ConnStateEnum;
import com.gdu.config.GlobalVariable;
import com.gdu.demo.databinding.ActivityFlightBinding;
import com.gdu.demo.flight.msgbox.MsgBoxBean;
import com.gdu.demo.flight.msgbox.MsgBoxManager;
import com.gdu.demo.flight.msgbox.MsgBoxPopView;
import com.gdu.demo.flight.msgbox.MsgBoxViewCallBack;
import com.gdu.demo.flight.setting.fragment.SettingDialogFragment;
import com.gdu.demo.ourgdu.ourGDUAircraft;
import com.gdu.demo.ourgdu.ourGDUVision;
import com.gdu.demo.utils.GisUtil;
import com.gdu.demo.utils.LoadingDialogUtils;
import com.gdu.demo.utils.SettingDao;
import com.gdu.demo.views.PaintView;
import com.gdu.demo.widget.TopStateView;
import com.gdu.demo.widget.focalGduSeekBar;
import com.gdu.drone.LocationCoordinate2D;
import com.gdu.drone.LocationCoordinate3D;
import com.gdu.drone.TargetMode;
import com.gdu.gimbal.GimbalState;
import com.gdu.gimbal.Rotation;
import com.gdu.gimbal.RotationMode;
import com.gdu.radar.ObstaclePoint;
import com.gdu.radar.PerceptionInformation;
import com.gdu.sdk.camera.GDUCamera;
import com.gdu.sdk.camera.SystemState;
import com.gdu.sdk.camera.VideoFeeder;
import com.gdu.sdk.codec.GDUCodecManager;
import com.gdu.sdk.flightcontroller.FlightState;
import com.gdu.sdk.flightcontroller.GDUFlightController;
import com.gdu.sdk.gimbal.GDUGimbal;
//import com.gdu.sdk.products.GDUAircraft;
import com.gdu.sdk.radar.GDURadar;
import com.gdu.sdk.util.CommonCallbacks;
import com.gdu.sdk.vision.OnTargetDetectListener;
import com.gdu.socket.GduCommunication3;
import com.gdu.socket.GduFrame3;
import com.gdu.socket.GduSocketManager;
import com.gdu.socket.SocketCallBack3;
import com.gdu.util.CollectionUtils;
import com.gdu.util.StringUtils;
import com.gdu.util.ThreadHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class FlightActivity extends FragmentActivity implements TextureView.SurfaceTextureListener, MsgBoxViewCallBack, View.OnClickListener {

    private ActivityFlightBinding viewBinding;
    private GDUCodecManager codecManager;
    private VideoFeeder.VideoDataListener videoDataListener ;

    private boolean showSuccess = false;

    private ourGDUVision gduVision;
    private GDUCamera mGDUCamera;
    private GDUFlightController mGDUFlightController;

    private GDUGimbal mGDUGimbal;
    private PaintView paintView;
    private Context mContext;

    private int chacktimes=1;
    private int chacktimes1=0;
    private int chacktimes2=0;
    private FlightState flightState;
    private Button changeMode;
    private Button changeFouse;
    private Button pipScreen;
    private Button flyState;
    private Button backState;
//    private Button changeGimbalRotate;

    private Button startAIRecognize;

    private Button quitAIRecognize;

    private TextView aiState;

    private Boolean isAIStart;

    private focalGduSeekBar zoomSeekBar;

    private GduCommunication3 mGduCommunication3;


    private Handler mHandler = new Handler(Looper.getMainLooper());

    private Boolean isPaused = false;




    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityFlightBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        initView();
        initData();
        initListener();
        mContext = this;
    }

    private void initListener() {
        mGDUFlightController = SdkDemoApplication.getAircraftInstance().getFlightController();
        if (mGDUFlightController != null){
            mGDUFlightController.setStateCallback(flightControllerState -> {
                //航向
                flightState = flightControllerState.getFlightState();
                float yaw = (float) flightControllerState.getAttitude().yaw;
                float roll = (float) flightControllerState.getAttitude().roll;
                LocationCoordinate3D aircraftLocation = flightControllerState.getAircraftLocation();
                double uavLon = aircraftLocation.getLongitude();
                double uavLat = aircraftLocation.getLatitude();
                LocationCoordinate2D homeLocation = flightControllerState.getHomeLocation();
                double homeLon = homeLocation.getLongitude();
                double homeLat = homeLocation.getLatitude();
                int distance = (int) GisUtil.calculateDistance(uavLon, uavLat, homeLon, homeLat);
                runOnUiThread(()->{
                    if(yaw>=-180&&yaw<0){
//                        yaw1=yaw + 360;
                        viewBinding.fpvRv.setHeadingAngle(yaw+360);
                    }else{
                        viewBinding.fpvRv.setHeadingAngle(yaw);
                    }
//                    runOnUiThread(() -> viewBinding.fpvRv.setGimbalAngle(yaw));
//                    toast(String.format("%.2f", yaw));
                    viewBinding.fpvRv.setHorizontalDipAngle(roll);
                    if (homeLon == 0 && homeLat == 0){
                        viewBinding.fpvRv.setReturnDistance(GlobalVariable.flyDistance +"m");
                    }else {
                        viewBinding.fpvRv.setReturnDistance(distance + "m");
                    }
                });
            });
        }

        GDURadar radar = (GDURadar) SdkDemoApplication.getAircraftInstance().getRadar();
        if (radar != null){
            radar.setRadarPerceptionInformationCallback(new CommonCallbacks.CompletionCallbackWith<PerceptionInformation>() {
                @Override
                public void onSuccess(PerceptionInformation information) {
                    List<ObstaclePoint> pointList = information.getObstaclePoints();
                    List<ObstaclePoint> showPointList = new ArrayList<>();
                    for (ObstaclePoint point : pointList) {
                        if (point.getDirection() <= 4) {
                            showPointList.add(point);
                        }

                    }
                    runOnUiThread(() -> viewBinding.fpvRv.setObstacle(showPointList,300));
                }

                @Override
                public void onFailure(GDUError var1) {
                }
            });
        }
        GDUGimbal gimbal = (GDUGimbal) SdkDemoApplication.getAircraftInstance().getGimbal();
        if (gimbal != null){
            gimbal.setStateCallback(state -> {
                float yaw = (float) state.getAttitudeInDegrees().yaw;
                Log.d("gimbalangle","航向角为"+yaw);
                toast(String.format("%.2f", yaw));
                float yaw1;
                yaw1=(yaw%180)/10.0f;
//                yaw1=yaw1/10.0f;
                toast(String.format("%.2f", yaw1));
                runOnUiThread(() -> viewBinding.fpvRv.setGimbalAngle(yaw1));
            });
        }
        HandlerThread backgroundThread = new HandlerThread("BackgroundThread");
        backgroundThread.start();
        Handler backgroundHandler = new Handler(backgroundThread.getLooper());
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                // 在后台线程中执行任务
                try {
                    Thread.sleep(1000); // 模拟耗时操作
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // 通过主线程的 Handler 调用 invalidate()
                //long startTime = System.currentTimeMillis();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(!isPaused){
//                            toast(GlobalVariable.sCurrentCameraZoom+"");
                            int currentProgress = (int)((GlobalVariable.sCurrentCameraZoom-1.0f)*10);
                            zoomSeekBar.setProgress(currentProgress);
                        }
                    }

                });
                backgroundHandler.post(this);
            }
        });

    }


    private void initView() {
        viewBinding.topStateView.setViewClickListener(new TopStateView.OnClickCallBack() {
            @Override
            public void onLeftIconClick() {
                finish();
            }

            @Override
            public void onRightSettingIconCLick() {
                SettingDialogFragment.show(getSupportFragmentManager());
            }
        });

        viewBinding.textureView.setSurfaceTextureListener(this);
        videoDataListener = new VideoFeeder.VideoDataListener() {
            @Override
            public void onReceive(byte[] bytes, int size) {
                if (null != codecManager) {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // 延迟后执行的操作
                            codecManager.sendDataToDecoder(bytes, size);
                        }
                    }, 550);
                }
            }
        };
        viewBinding.fpvRv.setShowObstacleOFF(!GlobalVariable.obstacleIsOpen);
        viewBinding.fpvRv.setObstacleMax(40);
        viewBinding.ivMsgBoxLabel.setOnClickListener(this);

        SettingDao settingDao = SettingDao.getSingle();
        boolean show = settingDao.getBooleanValue(settingDao.ZORRORLabel_Grid, false);
        showNineGridShow(show);
        changeMode = findViewById(R.id.btn_mode_switch);
        changeFouse = findViewById(R.id.btn_zoom);
        pipScreen =findViewById(R.id.btn_split_screen);
        flyState=findViewById(R.id.btn_take_off);
        backState=findViewById(R.id.btn_return_home);

        paintView = findViewById(R.id.paint_view);
//        设定云台角度，如果云台角度不为0，则置为回正，否则显示向下
//        changeGimbalRotate = findViewById(R.id.button_gimbal_rotate);

        startAIRecognize = findViewById(R.id.button_ai);


        quitAIRecognize = findViewById(R.id.button_quit_ai);
        quitAIRecognize.setEnabled(false);

        aiState = findViewById(R.id.ai_state);

        isAIStart = false;


        zoomSeekBar = findViewById(R.id.zoomSeekBar);


        backState.setEnabled(false);
    }

    private void initCamera() {
        mGDUCamera = (GDUCamera) ((ourGDUAircraft) SdkDemoApplication.getProductInstance()).getCamera();
        if (mGDUCamera != null) {
            mGDUCamera.setSystemStateCallback(new SystemState.Callback() {
                @Override
                public void onUpdate(SystemState systemState) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(" isPhotoStored ");
                    sb.append(systemState.isPhotoStored());
                    sb.append(" hasError ");
                    sb.append(systemState.isHasError());
                    sb.append(" isRecording ");
                    sb.append(systemState.isRecording());
                    sb.append(" mode ");
                    sb.append(systemState.getMode());
                    sb.append(" time ");
                    sb.append(systemState.getCurrentVideoRecordingTimeInSeconds());
                    //show(mInfoTextView, sb.toString());
                }
            });
            mGDUCamera.setStorageStateCallBack(new StorageState.Callback() {
                @Override
                public void onUpdate(StorageState state) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(" isFormatting ");
                    sb.append(state.isFormatting());
                    sb.append(" isFormatted ");
                    sb.append(state.isFormatted());
                    sb.append(" TotalSpace ");
                    sb.append(state.getTotalSpace());
                    sb.append(" RemainingSpace ");
                    sb.append(state.getRemainingSpace());
                    //show(mStorageInfoTextView, sb.toString());
                }
            });
        }
    }

    private void initData() {
        new MsgBoxManager(this, 1,this);
        VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(videoDataListener);
        initGimbal();
        initGduvision();
        initCamera();
        mGDUCamera.getDisplayMode(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.DisplayMode>() {
            @Override
            public void onSuccess(SettingsDefinitions.DisplayMode displayMode) {
                if(displayMode == SettingsDefinitions.DisplayMode.THERMAL_ONLY){
                    changeMode.setText("切换为可见光");
                }else{
                    changeMode.setText("切换为红外");
                }
                if(displayMode==SettingsDefinitions.DisplayMode.ZL){
                    changeFouse.setText("变焦");
                }else{
                    changeFouse.setText("广角");
                }
                if(displayMode==SettingsDefinitions.DisplayMode.PIP){
                    pipScreen.setText("复原");
                }else{
                    pipScreen.setText("分屏");
                }

            }

            @Override
            public void onFailure(GDUError var1) {
                toast("发送失败");
            }
        });
        if (mGDUFlightController != null) {
//            mGDUFlightController.setStateCallback(flightControllerState -> {
//                flightState = flightControllerState.getFlightState();
                if (flightState == FlightState.GROUND) {
                    flyState.setText("开始起飞");
                } else if (flightState == FlightState.LANDING) {
                    flyState.setText("取消降落");
                }else{
                    flyState.setText("一键降落");
                }
                if(flightState == FlightState.GROUND){
                    backState.setEnabled(false);
                    backState.setAlpha(0.5f);
                }
                else{
                    backState.setEnabled(true);
                }

        }

        mGduCommunication3 = GduSocketManager.getInstance().getGduCommunication();


        // 假设最小倍数 1.0x，最大倍数 5.0x
        final float minZoom = 1.0f;
        final float maxZoom = 160.0f;

        String text = String.format("%.1fx", 1.0);

        // 创建新的 Thumb 并设置
        Drawable thumbDrawable = createThumbDrawable(FlightActivity.this, text);
        zoomSeekBar.setThumb(thumbDrawable);

        zoomSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 计算当前倍数
                float zoomFactor = minZoom + (progress / 10f);
                String text = String.format("%.1fx", zoomFactor);

                // 创建新的 Thumb 并设置
                Drawable thumbDrawable = createThumbDrawable(FlightActivity.this, text);
                seekBar.setThumb(thumbDrawable);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isPaused = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int focal_length = 10+seekBar.getProgress();
                isPaused = false;

                mGduCommunication3.zoomCustomSizeRatio((short)focal_length, new SocketCallBack3() {
                    public void callBack(int code, GduFrame3 bean) {
                        if(code==0){
                            toast("设定焦距为:"+focal_length);
                        }
                        else {
                            toast("设定失败");
                        }
                    }
                });
            }
        });

    }

    private void initGduvision(){
        gduVision =(ourGDUVision) ((ourGDUAircraft) SdkDemoApplication.getProductInstance()).getGduVision();
        if (gduVision==null){
            toast("gduVision出现异常");
            return;
        }
        else{
            try {
                gduVision.setOnTargetDetectListener(new OnTargetDetectListener() {
                    //                    long startTime=System.currentTimeMillis();
                    @Override
                    public void onTargetDetecting(List<TargetMode> list) {
                        if (list == null) {
                            toast("没有检测物体");
                        } else {
                            if(!isAIStart){
                                startAIRecognize.setEnabled(false);
                                quitAIRecognize.setEnabled(true);
                                isAIStart = true;
                                show(aiState, "AI状态：未增量");
                            }
                            paintView.setRectParams(list);
                            //long endTime=System.currentTimeMillis();
                            //long ver= endTime -startTime;
                            //Log.d("delaytime","延长时间"+ver);

                        }
                    }

                    @Override
                    public void onTargetDetectFailed(int i) {
                        toast("检测失败");

                    }

                    @Override
                    public void onTargetDetectStart() {
                        toast("检测开始");

                    }

                    @Override
                    public void onTargetDetectFinished() {
                        toast("检测结束");

                    }
                });
            }
            catch (Exception ignored){
                toast("添加监视器错误");
            }
        }
    }

    private void initGimbal() {
        mGDUGimbal = (GDUGimbal) ((ourGDUAircraft) SdkDemoApplication.getProductInstance()).getGimbal();
        if (mGDUGimbal == null) {
            toast("云台未识别，相关功能可能出现异常");
            return;
        }
    }

    public void showNineGridShow(boolean show) {
        viewBinding.nightGridView.setVisibility(show ? View.VISIBLE : View.GONE);
    }


    public void beginCheckCloud() {
        showSuccess = false;
        GDUGimbal mGDUGimbal = (GDUGimbal) ((ourGDUAircraft) SdkDemoApplication.getProductInstance()).getGimbal();
        if (mGDUGimbal == null) {
            return;
        }
        LoadingDialogUtils.createLoadDialog(this, getString(R.string.clound_checking), false);

        mGDUGimbal.setStateCallback(new GimbalState.Callback() {
            @Override
            public void onUpdate(GimbalState gimbalState) {
                runOnUiThread(() -> {
                    if (gimbalState.getCalibrationState() == 2) {
                        LoadingDialogUtils.cancelLoadingDialog();
                        if (!showSuccess) {
                            Toast.makeText(FlightActivity.this, "校飘完成", Toast.LENGTH_SHORT).show();
                            showSuccess = true;
                        }
                    } else if (gimbalState.getCalibrationState() == 3 || gimbalState.getCalibrationState() == 4) {
                        LoadingDialogUtils.cancelLoadingDialog();
                        if (!showSuccess) {
                            Toast.makeText(FlightActivity.this, "校飘失败", Toast.LENGTH_SHORT).show();
                            showSuccess = true;
                        }
                    }
                });
            }
        });
        mGDUGimbal.startCalibration(error -> {

        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (codecManager != null) {
            codecManager.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (codecManager != null) {
            codecManager.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (codecManager != null) {
            codecManager.onDestroy();
        }
    }
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (codecManager == null) {
            codecManager = new GDUCodecManager(FlightActivity.this, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
    private final List<MsgBoxBean> msgData = new ArrayList<>();
    private MsgBoxPopView mMsgBoxPopWin;

    private void showMsgBoxPopWindow(List<MsgBoxBean> data) {
        if (mMsgBoxPopWin == null) {
            mMsgBoxPopWin = new MsgBoxPopView(this, viewBinding.ivMsgBoxLabel);
        }
        final boolean isShow = viewBinding.ivMsgBoxLabel.isSelected();
        mMsgBoxPopWin.setOnDismissListener(() -> ThreadHelper.runOnUiThreadDelayed(()
                -> viewBinding.ivMsgBoxLabel.setSelected(false), 500));
        if (isShow) {
            mMsgBoxPopWin.dismiss();
        } else {
            mMsgBoxPopWin.updateMsgData(data);
            mMsgBoxPopWin.showAsDropDown(viewBinding.ivMsgBoxLabel, 0,
                    getResources().getDimensionPixelOffset(R.dimen.dp_6),
                    Gravity.BOTTOM);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.iv_msgBoxLabel:
                if (msgData.isEmpty() || StringUtils.isEmptyString(viewBinding.tvMsgBoxNum.getText().toString())
                        || Integer.parseInt(viewBinding.tvMsgBoxNum.getText().toString().trim()) == 0) {
                    return;
                }
                showMsgBoxPopWindow(msgData);
                viewBinding.ivMsgBoxLabel.setSelected(!viewBinding.ivMsgBoxLabel.isSelected());
                break;
            case R.id.btn_mode_switch:
                try {
                    chacktimes++;
                    if (chacktimes % 2 == 0) {
                        mGDUCamera.setDisplayMode(SettingsDefinitions.DisplayMode.THERMAL_ONLY, new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(GDUError error) {
                                if (error == null) {
//                                    initData();
                                    toast("设置成红外");
                                } else {
                                    toast("发送失败");
                                }
                            }
                        });
                        changeMode.setText("可见光");


                    } else {
                        mGDUCamera.setDisplayMode(SettingsDefinitions.DisplayMode.ZL, new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(GDUError error) {
                                if (error == null) {
//                                    initData();
                                    toast("可见光");
                                } else {
                                    toast("发送失败");
                                }
                            }
                        });
                        changeMode.setText("红外");
                    }
                }
                catch (Exception e){
                    Toast.makeText(mContext, "changeMode Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.btn_split_screen:
                try {
                    chacktimes2++;
                    if (chacktimes2 % 2 == 0) {
                        mGDUCamera.setDisplayMode(SettingsDefinitions.DisplayMode.PIP, new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(GDUError error) {
                                if (error == null) {
//                                    initData();
                                    toast("设置成分屏");
                                } else {
                                    toast("发送失败");
                                }
                            }
                        });
                        pipScreen.setText("复原");


                    } else {
                        mGDUCamera.setDisplayMode(SettingsDefinitions.DisplayMode.ZL, new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(GDUError error) {
                                if (error == null) {
//                                    initData();
                                    toast("复原");
                                } else {
                                    toast("发送失败");
                                }
                            }
                        });
                        pipScreen.setText("分屏");
                    }
                }
                catch (Exception e){
                    Toast.makeText(mContext, "changeMode Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.btn_zoom:
                try {
                    chacktimes1++;
                    if (chacktimes1 % 2 == 0) {
                        mGDUCamera.setDisplayMode(SettingsDefinitions.DisplayMode.ZL, new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(GDUError error) {
                                if (error == null) {
//                                    initData();
                                    toast("设置成变焦");
                                } else {
                                    toast("发送失败");
                                }
                            }
                        });
                        changeFouse.setText("变焦");


                    } else {
                        mGDUCamera.setDisplayMode(SettingsDefinitions.DisplayMode.WAL, new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(GDUError error) {
                                if (error == null) {
//                                    initData();
                                    toast("设置成广角");
                                } else {
                                    toast("发送失败");
                                }
                            }
                        });
                        changeFouse.setText("广角");
                    }
                }
                catch (Exception e){
                    Toast.makeText(mContext, "changeMode Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.btn_take_off:
//                GDUFlightController mGDUFlightController = SdkDemoApplication.getAircraftInstance().getFlightController();
                if (mGDUFlightController != null) {
                    if (flightState == FlightState.GROUND) {
                            //起飞高度设置成1.5米
                            flyState.setText("开始起飞");
                            mGDUFlightController.startTakeoff(150, new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(GDUError var1) {
                                    if (var1 == null) {
                                        toast("起飞成功");
                                        flyState.setText("开始降落");
                                    } else {
                                        toast("起飞失败");
                                        flyState.setText("开始起飞");
                                    }
                                }
                            });
                    } else if (flightState == FlightState.FLING||flightState==FlightState.HOVERING||flightState==FlightState.BACKING) {
                            flyState.setText("开始降落");
                            mGDUFlightController.startLanding(new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(GDUError var1) {
                                    if (var1 == null) {
                                        toast("开始降落成功");
                                        flyState.setText("取消降落");
                                    } else {
                                        toast("开始降落失败");
                                        flyState.setText("开始降落");
                                    }
                                }
                            });
                    } else if (flightState == FlightState.LANDING) {
                        mGDUFlightController.cancelLanding(new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(GDUError var1) {
                                if (var1 == null) {
                                    toast("取消降落成功");
                                } else {
                                    toast("取消降落失败");
                                }
                            }
                            });
                            flyState.setText("开始降落");


                    }
                }
                break;
            case R.id.btn_return_home:
//                 mGDUFlightController = SdkDemoApplication.getAircraftInstance().getFlightController();
                if (mGDUFlightController != null) {
//                    mGDUFlightController.setStateCallback(flightControllerState -> {
//                        flightState = flightControllerState.getFlightState();
                        if (flightState != FlightState.GROUND&&flightState!=FlightState.BACKING) {
                            mGDUFlightController.startGoHome(new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(GDUError var1) {
                                    if (var1 == null) {
                                        toast("开始返航成功");
                                    } else {
                                        toast("开始返航失败");
                                    }
                                }
                            });
                            backState.setText("取消返航");
                        } else if (flightState == FlightState.BACKING) {
                            mGDUFlightController.cancelGoHome(new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(GDUError var1) {
                                    if (var1 == null) {
                                        toast("取消返航成功");
                                    } else {
                                        toast("取消返航失败");
                                    }
                                }
                            });
                            backState.setText("一键返航");

                        }
                }
                break;
            case R.id.button_gimbal_rotate:  //TODO 俯仰，方位会变
                Log.d("demo","开始向下");
                Rotation rotation = new Rotation();
                rotation.setMode(RotationMode.ABSOLUTE_ANGLE);
                rotation.setPitch(-90);
                //                rotation.set
                mGDUGimbal.rotate(rotation, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(GDUError error) {
                        if (error == null) {
                            toast("云台向下");
                        } else {
                            toast("发送失败");
                        }
                    }
                });
                break;

            case R.id.button_gimbal_reset:
                Log.d("demo","开始回正");
                mGDUGimbal.reset(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(GDUError error) {
                        if (error == null) {
                            toast("云台回正");
                        } else {
                            toast("发送失败");
                        }
                    }
                });
                break;

            case R.id.button_ai:
                gduVision.setOnTargetDetectListener(new OnTargetDetectListener() {
                    @Override
                    public void onTargetDetecting(List<TargetMode> list) {
                        if (list == null) {
                            toast("没有检测物体");
                        } else {
                            if(!isAIStart){
                                startAIRecognize.setEnabled(false);
                                quitAIRecognize.setEnabled(true);
                                isAIStart = true;
                            }

                            paintView.setRectParams(list);
                        }
                    }

                    @Override
                    public void onTargetDetectFailed(int i) {
                        toast("检测失败");

                    }

                    @Override
                    public void onTargetDetectStart() {
                        toast("检测开始");

                    }

                    @Override
                    public void onTargetDetectFinished() {
                        toast("检测结束");

                    }
                });
                startAIRecognize.setEnabled(false);
                quitAIRecognize.setEnabled(true);
                show(aiState, "AI状态：未增量");
                break;
            case R.id.button_quit_ai:
                gduVision.setOnTargetDetectListener(new OnTargetDetectListener() {
                    //                    long startTime=System.currentTimeMillis();
                    @Override
                    public void onTargetDetecting(List<TargetMode> list) {
                    }

                    @Override
                    public void onTargetDetectFailed(int i) {
                    }

                    @Override
                    public void onTargetDetectStart() {
                    }

                    @Override
                    public void onTargetDetectFinished() {
                    }
                });
                startAIRecognize.setEnabled(true);
                quitAIRecognize.setEnabled(false);
                show(aiState, "");
                isAIStart = false;
                break;
        }
    }

    @Override
    public void updateTitleTvTxt(String title) {
        ThreadHelper.runOnUiThread(() -> viewBinding.topStateView.setStatusText(title));
    }

    @Override
    public void updateTitleTVColor(int txtColor) {
        ThreadHelper.runOnUiThread(() -> viewBinding.topStateView.setStatusTextColor(txtColor));
    }

    @Override
    public void updateHeadViewBg(int resId) {
        ThreadHelper.runOnUiThread(() -> {
            if (resId != 0) {
                viewBinding.topStateView.setStatusTextBackground(resId);
            }
        });
    }

    @Override
    public void updateWarnList(HashMap<Long, WarnBean> warnList) {
        if (GlobalVariable.connStateEnum != ConnStateEnum.Conn_Sucess || warnList.isEmpty()) {
            msgData.clear();
            ThreadHelper.runOnUiThread(() -> {
                viewBinding.tvMsgBoxNum.setText("0");
                viewBinding.tvMsgBoxNum.setVisibility(View.GONE);
            });
            return;
        }
        msgData.clear();
        for (Map.Entry<Long, WarnBean> warnBeanEntry : warnList.entrySet()) {
            if (!warnBeanEntry.getValue().isErr) {
                continue;
            }
            final MsgBoxBean bean = new MsgBoxBean();
            bean.setMsgContent(warnBeanEntry.getValue().warnStr);
            bean.setWarnLevel(warnBeanEntry.getValue().getWarnLevel());
            CollectionUtils.listAddAvoidNull(msgData, bean);
        }
        ThreadHelper.runOnUiThread(() -> {
            if (!CollectionUtils.isEmptyList(msgData)) {
                viewBinding.tvMsgBoxNum.setText(String.valueOf(msgData.size()));
                viewBinding.tvMsgBoxNum.setVisibility(View.VISIBLE);
            } else {
                viewBinding.tvMsgBoxNum.setText("0");
                viewBinding.tvMsgBoxNum.setVisibility(View.GONE);

            }
        });
    }

    public void toast(final String toast) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void show(TextView textView, final String toast) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(toast);
//                Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
            }
        });
    }

    //    添加滑动按钮，设置滑动变焦功能
    private Drawable createThumbDrawable(Context context, String text) {
        // Thumb 的直径，可根据需要调大或调小
        int size = dp2px(context, 48);

        // 创建一个空白位图
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // 1. 先绘制蓝色圆形背景
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.argb(128, 0, 191, 255));
        float radius = size / 2f;
        canvas.drawRect(10, 0, size-10, size, paint);

        // 2. 绘制白色文字
        paint.setColor(Color.WHITE);
        paint.setTextSize(sp2px(context, 14)); // 字体大小，可自行调节
        paint.setTextAlign(Paint.Align.CENTER);

        // 计算文字垂直居中的基线
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float baseline = radius;


        // 步骤3：旋转坐标系
        canvas.rotate(90);
        // 在圆心位置绘制文字
        canvas.drawText(text, baseline, -radius - (fontMetrics.descent + fontMetrics.ascent) / 2, paint);

//        canvas.restore();

        // 3. 用生成的 Bitmap 创建 Drawable
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    /**
     * dp 转 px 工具方法
     */
    private int dp2px(Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * sp 转 px 工具方法
     */
    private int sp2px(Context context, float spValue) {
        float scale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * scale + 0.5f);
    }

}


