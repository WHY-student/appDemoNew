package com.gdu.demo;

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.gdu.AlgorithmMark;
import com.gdu.beans.WarnBean;
import com.gdu.common.error.GDUError;
import com.gdu.config.ConnStateEnum;
import com.gdu.config.GlobalVariable;
import com.gdu.demo.databinding.ActivityFlightBinding;
import com.gdu.demo.flight.aibox.helper.TargetDetectHelper;
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
import com.gdu.demo.viewmodel.FlightViewModel;
import com.gdu.demo.views.PaintView;
import com.gdu.demo.widget.TopStateView;
import com.gdu.demo.widget.zoomView.S220CustomSizeFocusHelper;
import com.gdu.drone.LocationCoordinate2D;
import com.gdu.drone.LocationCoordinate3D;
import com.gdu.drone.TargetMode;
import com.gdu.gimbal.GimbalState;
import com.gdu.radar.ObstaclePoint;
import com.gdu.radar.PerceptionInformation;
import com.gdu.sdk.camera.VideoFeeder;
import com.gdu.sdk.codec.GDUCodecManager;
import com.gdu.sdk.flightcontroller.GDUFlightController;
import com.gdu.sdk.gimbal.GDUGimbal;
import com.gdu.sdk.manager.GDUSDKManager;
import com.gdu.sdk.products.GDUAircraft;
import com.gdu.sdk.radar.GDURadar;
import com.gdu.sdk.util.CommonCallbacks;
import com.gdu.sdk.vision.GDUVision;
import com.gdu.sdk.vision.OnTargetDetectListener;
import com.gdu.util.CollectionUtils;
import com.gdu.util.StringUtils;
import com.gdu.util.ThreadHelper;
import com.gdu.util.ViewUtils;
import com.gdu.util.logger.MyLogUtils;
import com.gdu.gimbal.Rotation;
import com.gdu.gimbal.RotationMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FlightActivity extends FragmentActivity implements TextureView.SurfaceTextureListener, MsgBoxViewCallBack, View.OnClickListener {

    private ActivityFlightBinding viewBinding;
    private GDUCodecManager codecManager;
    private Context mContext;
    private VideoFeeder.VideoDataListener videoDataListener;
    private GDUFlightController mGDUFlightController;

    private boolean showSuccess = false;
    private S220CustomSizeFocusHelper mCustomSizeFocusHelper;
    private FlightViewModel viewModel;
    /**
     * 目标检测类
     */
    private TargetDetectHelper mTargetDetectHelper;
    private Button quitAIRecognize;
    private TextView aiState;
    private Button startIncremental;
    private TextView unKnownum;
    private AppCompatImageView AIRecognize;

    private Runnable resetStateTask; // 用于重置状态的任务
    private Runnable completeTask;
    private int incState = 0;
    private int unkonwNum = 0;
    private boolean isProcessRunning = false;
    private int latestModelID = 1;
    private int modelID = 0;

    // 定义一个 Runnable 任务，用于更新 AI 状态


    private Handler mHandler = new Handler(Looper.getMainLooper());

    private Boolean isPaused = false;
    private boolean isSelected = false;


    private GDUGimbal mGDUGimbal;

    private GDUVision mGduVision;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityFlightBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        viewModel = new ViewModelProvider(this).get(FlightViewModel.class);
        initView();
        initData();
        initBackgroundThread();
        initListener();
    }

    private void initListener() {
        mGDUFlightController = Objects.requireNonNull(SdkDemoApplication.getAircraftInstance()).getFlightController();
        if (mGDUFlightController != null) {
            mGDUFlightController.setStateCallback(flightControllerState -> {
                //航向
                float yaw = (float) flightControllerState.getAttitude().yaw;
                float roll = (float) flightControllerState.getAttitude().roll;
                LocationCoordinate3D aircraftLocation = flightControllerState.getAircraftLocation();
                double uavLon = aircraftLocation.getLongitude();
                double uavLat = aircraftLocation.getLatitude();
                LocationCoordinate2D homeLocation = flightControllerState.getHomeLocation();
                double homeLon = homeLocation.getLongitude();
                double homeLat = homeLocation.getLatitude();
                int distance = (int) GisUtil.calculateDistance(uavLon, uavLat, homeLon, homeLat);
                runOnUiThread(() -> {
                    if (yaw >= -180 && yaw < 0) {
//                        yaw1=yaw + 360;
                        viewBinding.fpvRv.setHeadingAngle(yaw + 360);
                    } else {
                        viewBinding.fpvRv.setHeadingAngle(yaw);
                    }
                    viewBinding.fpvRv.setHorizontalDipAngle(roll);
                    if (homeLon == 0 && homeLat == 0) {
                        viewBinding.fpvRv.setReturnDistance(GlobalVariable.flyDistance + "m");
                    } else {
                        viewBinding.fpvRv.setReturnDistance(distance + "m");
                    }
                });
            });
        }

        GDURadar radar = (GDURadar) SdkDemoApplication.getAircraftInstance().getRadar();
        if (radar != null) {
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
                    runOnUiThread(() -> viewBinding.fpvRv.setObstacle(showPointList, 300));
                }

                @Override
                public void onFailure(GDUError var1) {
                }
            });
        }
        GDUGimbal gimbal = (GDUGimbal) SdkDemoApplication.getAircraftInstance().getGimbal();
        if (gimbal != null) {
            gimbal.setStateCallback(state -> {
                float yaw = (float) state.getAttitudeInDegrees().yaw;
                float yaw1;
                yaw1 = (yaw % 180) / 10.0f;
                runOnUiThread(() -> viewBinding.fpvRv.setGimbalAngle(yaw1));
            });
        }

        viewModel.getToastLiveData().observe(this, data -> {
            if (data != 0) {
                showToast(getResources().getString(data));
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
        quitAIRecognize = findViewById(R.id.button_quit_ai);
        quitAIRecognize.setEnabled(false);
//        paintView = findViewById(R.id.ai_paint_view);
        startIncremental = findViewById(R.id.button_start_incremental);
        startIncremental.setEnabled(false);
        unKnownum = findViewById(R.id.unknown_num);
        aiState = findViewById(R.id.ai_state);
        AIRecognize = findViewById(R.id.ai_recognize_imageview);
        viewBinding.fpvRv.setShowObstacleOFF(!GlobalVariable.obstacleIsOpen);
        viewBinding.fpvRv.setObstacleMax(40);
        viewBinding.ivMsgBoxLabel.setOnClickListener(this);
        viewBinding.aiRecognizeImageview.setOnClickListener(this);
        ViewUtils.setViewShowOrHide(viewBinding.aiRecognizeImageview, viewModel.isShowAiBox());

        SettingDao settingDao = SettingDao.getSingle();
        boolean show = settingDao.getBooleanValue(settingDao.ZORRORLabel_Grid, false);
        showNineGridShow(show);

        mCustomSizeFocusHelper = new S220CustomSizeFocusHelper(viewBinding.zoomSeekBar);

        mTargetDetectHelper = TargetDetectHelper.getInstance();
        mTargetDetectHelper.init(this);
        mTargetDetectHelper.setOnTargetDetectListener(new TargetDetectHelper.OnTargetDetectListener() {
            @Override
            public void onTargetDetect(boolean isSuccess, List<TargetMode> targetModes) {
                LoadingDialogUtils.cancelLoadingDialog();
                //视频是主界面时，在视频上画框
                if (isSuccess && targetModes != null && !targetModes.isEmpty()) {
                    GlobalVariable.isTargetDetectMode = true;
                    mTargetDetectHelper.startShowTarget();
                    GlobalVariable.algorithmType = AlgorithmMark.AlgorithmType.DEVICE_RECOGNISE;
                }
            }

            @Override
            public void onTargetDetectSend(boolean isSuccess) {
                MyLogUtils.d("mTargetDetectHelper onTargetDetectSend() isSuccess = " + isSuccess);
                if (isSuccess) {
                    GlobalVariable.algorithmType = AlgorithmMark.AlgorithmType.DEVICE_RECOGNISE;
                    GlobalVariable.discernIsOpen = true;
                    GlobalVariable.isTargetDetectMode = true;
                } else {
                    GlobalVariable.isTargetDetectMode = false;
                }
            }

            @Override
            public void onTargetLocateSend(boolean isSuccess) {
                MyLogUtils.d("mTargetDetectHelper onTargetLocateSend() isSuccess = " + isSuccess);
            }

            @Override
            public void onTargetLocate(boolean isSuccess, TargetMode targetMode) {
                MyLogUtils.d("mTargetDetectHelper onTargetLocate() isSuccess = " + isSuccess);
//                DialogUtils.cancelLoadDialog();
            }

            @Override
            public void onDetectClosed() {
                //收到关闭目标识别成功回调后再次重置状态，防止部分极端场景本地重置状态到发送关闭中间时间段又收到周期回调，将状态还原导致无法退出的问题
            }
        });

//        云台向下及回正
        viewBinding.buttonGimbalRotate.setOnClickListener(this);
        viewBinding.buttonGimbalReset.setOnClickListener(this);
    }


    private void initData() {
        new MsgBoxManager(this, 1, this);
        VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(videoDataListener);
        mGduVision = ((GDUAircraft) SdkDemoApplication.getProductInstance()).getGduVision();
        mGduVision.setOnTargetDetectListener(new OnTargetDetectListener() {
            //                    long startTime=System.currentTimeMillis();
            @Override
            public void onTargetDetecting(List<TargetMode> targetModes) {
                if (targetModes != null && !targetModes.isEmpty()) {
                    GlobalVariable.isTargetDetectMode = true;
                    mTargetDetectHelper.startShowTarget();
                    viewBinding.aiPaintView.setRectParams(targetModes);
                    updateModel(viewBinding.aiPaintView.getModelID());
                    GlobalVariable.algorithmType = AlgorithmMark.AlgorithmType.DEVICE_RECOGNISE;
                }
            }

            @Override
            public void onTargetDetectFailed(int i) {
                showToast("检测失败");

            }

            @Override
            public void onTargetDetectStart() {
                showToast("检测开始");

            }

            @Override
            public void onTargetDetectFinished() {
                showToast("识别结束");

            }
        });
    }


    public void showNineGridShow(boolean show) {
        viewBinding.nightGridView.setVisibility(show ? View.VISIBLE : View.GONE);
    }


    public void beginCheckCloud() {
        showSuccess = false;
        mGDUGimbal = (GDUGimbal) ((GDUAircraft) SdkDemoApplication.getProductInstance()).getGimbal();
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
        if (mCustomSizeFocusHelper != null) {
            mCustomSizeFocusHelper.onDestroy();
        }
        stopBackgroundThread();
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

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private void initBackgroundThread() {
        backgroundThread = new HandlerThread("ModelUpdateThread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            backgroundThread = null;
            backgroundHandler = null;
        }
    }

    private void updateKnowNum(int modelID) {
        int num = modelID % 100;
        if (num == unkonwNum) {
            Log.d("TaskDebug", "ModelID unchanged: " + modelID + ", skipping update");
            return;
        }
        if (num != unkonwNum) {
            show(unKnownum, "  " + "未知类数目 " + num);
            unkonwNum = num;
        }
    }

    private boolean isTaskRunning = false; // 标记是否有延迟任务正在运行

    private void updateModel(int newModelID) {
        if(modelID==newModelID){
            return;
        }
        modelID = newModelID;
        int firstNum = modelID / 1000;
        int secondNum = (modelID / 100) % 10;
        int temp = modelID / 100;
        incState = firstNum -1;
        if(temp == 10){
            show(aiState, "AI状态：未增量");
        }else {
            if(secondNum==0){
                show(aiState, "AI状态：增量" + incState + "中");
            }else {
                show(aiState, "AI状态：增量" + incState + "完成");
            }
        }
        updateKnowNum(modelID);

        // 如果当前有任务正在运行，直接按照 1077 处理
//        if (isTaskRunning) {
//            Log.d("TaskDebug", "Task is running, treating modelID as 1077: " + modelID);
////            show(aiState, "AI状态：增量" + incState + "中"); // 按照 1077 处理
//            return;
//        }
//
//        // 如果 modelID 没有变化，直接返回
//        if (temp == 10) {
//            Log.d("TaskDebug", "ModelID unchanged: " + modelID + ", skipping update");
//            return;
//        }
//
//        // 确保 backgroundHandler 已初始化
//        if (backgroundHandler == null) {
//            initBackgroundThread();
//        }
//
//        // 记录最新的 modelID
//        latestModelID = firstNum;
//        Log.d("TaskDebug", "Latest modelID: " + latestModelID);
//
//        if (temp == 20 && isProcessRunning) {
//            // 标记任务开始运行
//            isTaskRunning = true;
//
//            incState = firstNum - 1;
//            // 显示“增量中”
//            show(aiState, "AI状态：增量" + incState + "中");
//            showToast("开始增量");
//
//            // 1 秒后显示“增量完成”
//            backgroundHandler.postDelayed(() -> {
//                if (!isProcessRunning) {
//                    Log.d("TaskDebug", "Process is not running, skipping resetStateTask");
//                    completeTask();
//                    return; // 如果进程被停止，则不再执行
//                }
//                show(aiState, "AI状态：增量" + incState + "完成");
//                showToast("增量完成");
//
//                // 0.5 秒后重新判断状态
//                backgroundHandler.postDelayed(() -> {
//                    completeTask();
//                }, 500); // 延迟 0.5 秒
//            }, 1000); // 延迟 1 秒
//        } else if (temp == 21 && isProcessRunning) {
//            // 如果 modelID 不是 1077，或者进程被停止，直接显示“未增量”
//            show(aiState, "AI状态：增量" + incState + "完成");
//        } else if (temp == 10 && isProcessRunning) {
//            show(aiState, "AI状态：未增量");
//        }
    }

    // 完成任务并重置状态
    private void completeTask() {
        isTaskRunning = false; // 标记任务完成
        Log.d("TaskDebug", "Task completed, latest modelID: " + latestModelID);

        // 根据最新的 modelID 更新状态
//        updateModel(latestModelID);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_msgBoxLabel:
                if (msgData.isEmpty() || StringUtils.isEmptyString(viewBinding.tvMsgBoxNum.getText().toString())
                        || Integer.parseInt(viewBinding.tvMsgBoxNum.getText().toString().trim()) == 0) {
                    return;
                }
                showMsgBoxPopWindow(msgData);
                viewBinding.ivMsgBoxLabel.setSelected(!viewBinding.ivMsgBoxLabel.isSelected());
                break;
            case R.id.ai_recognize_imageview:
//                viewModel.switchAIRecognize();
                AIRecognize.setEnabled(false);
                try {
                    // 开启检测
                    show(aiState, "AI状态：未增量");
                    show(unKnownum, "未知类数目：0");
                    AIRecognize.setImageResource(R.drawable.ai_recognize_selected);
                    if (mGduVision != null) {
                        mGduVision.setOnTargetDetectListener(new OnTargetDetectListener() {
                            //                    long startTime=System.currentTimeMillis();
                            @Override
                            public void onTargetDetecting(List<TargetMode> targetModes) {
                                if (targetModes != null && !targetModes.isEmpty()) {
                                    GlobalVariable.isTargetDetectMode = true;
                                    mTargetDetectHelper.startShowTarget();
                                    viewBinding.aiPaintView.setRectParams(targetModes);
                                    updateModel(viewBinding.aiPaintView.getModelID());
                                    GlobalVariable.algorithmType = AlgorithmMark.AlgorithmType.DEVICE_RECOGNISE;
                                }
                            }

                            @Override
                            public void onTargetDetectFailed(int i) {
                                showToast("检测失败");

                            }

                            @Override
                            public void onTargetDetectStart() {
                                showToast("检测开始");

                            }

                            @Override
                            public void onTargetDetectFinished() {
                                showToast("识别结束");

                            }
                        });
                        mGduVision.startTargetDetect(new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(GDUError var1) {
                                if (var1 == null) {
                                    showToast("识别开始");
                                } else {
                                    showToast("开始识别失败");
                                    AIRecognize.setEnabled(true);
                                }
                            }
                        });
                    }else {
                        showToast("请检查初始化是否完成");
                        AIRecognize.setEnabled(true);
                    }
                    isProcessRunning = true;
                    quitAIRecognize.setEnabled(true);
                    startIncremental.setEnabled(true);
                } catch (Exception e) {
                    AIRecognize.setEnabled(true);
                }
                break;
            case R.id.button_quit_ai:
                quitAIRecognize.setEnabled(false);
                try{
                    // 移除所有未执行的任务
                    if (resetStateTask != null) {
                        backgroundHandler.removeCallbacks(resetStateTask);
                    }
                    if (completeTask != null) {
                        backgroundHandler.removeCallbacks(completeTask);
                    }
                    // 显示“未增量”
                    show(aiState, "");
                    show(unKnownum, "");
                    AIRecognize.setImageResource(R.drawable.ai_recognize);
                    stopBackgroundThread();
                    if (mGduVision != null) {
                        mGduVision.setOnTargetDetectListener(new OnTargetDetectListener() {
                            //                    long startTime=System.currentTimeMillis();
                            @Override
                            public void onTargetDetecting(List<TargetMode> targetModes) {
                            }

                            @Override
                            public void onTargetDetectFailed(int i) {
                                showToast("检测失败");

                            }

                            @Override
                            public void onTargetDetectStart() {
                                showToast("检测开始");

                            }

                            @Override
                            public void onTargetDetectFinished() {
                                showToast("识别结束");

                            }
                        });
                        mGduVision.stopTargetDetect(new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(GDUError var1) {
                                if (var1 == null) {
//                                    showToast("退出识别");
                                } else {
                                    showToast("退出识别失败");
                                    quitAIRecognize.setEnabled(true);
                                }
                            }
                        });
                    }
                    viewBinding.aiPaintView.setRectParams(new ArrayList<>());
                    isProcessRunning = false;
                    AIRecognize.setEnabled(true);
                    startIncremental.setEnabled(false);
                }catch(Exception e){
                    quitAIRecognize.setEnabled(true);
                }
                break;
            case R.id.button_start_incremental:
                if(unkonwNum < 10){
                    showToast("未知类别数目过少，请收集更多未知类别");
                }
                if (mGduVision != null) {
                    mGduVision.targetDetect((byte) 3, (short) 0, (short) 0, (short) 0, (short) 0, (byte) 0, (byte) 0, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(GDUError var1) {
                            if (var1 == null) {
                                showToast("开始增量");
                            } else {
                                showToast("开始增量失败");
                            }
                        }
                    });
                } else {
                    showToast("请检查初始化是否成功");
                }
                break;

            case R.id.btn_take_off:
                mGDUFlightController.startLanding(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(GDUError var1) {
                        if (var1 == null) {
                            showToast("开始降落");
                        } else {
                            showToast("开始降落失败");
                        }
                    }
                });
                break;
            case R.id.btn_return_home:
                mGDUFlightController.startGoHome(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(GDUError var1) {
                    if (var1 == null) {
                        showToast("开始返航");
                    } else {
                        showToast("开始返航失败");
                    }
                }
            });
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

    public void showToast(String str) {
        ThreadHelper.runOnUiThread(() -> Toast.makeText(this, str, Toast.LENGTH_SHORT).show());
    }

    private Handler handler1 = new Handler(Looper.getMainLooper());

    public void show(TextView textView, final String toast) {
        if (toast == null) {
            return; // 如果 toast 为空，直接返回
        }
        handler1.post(new Runnable() {
            @Override
            public void run() {
                if (textView != null) {
                    textView.setText(toast);
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        viewModel.stopTarget((byte) 0x02, GlobalVariable.mCurrentLightType);
    }
}
