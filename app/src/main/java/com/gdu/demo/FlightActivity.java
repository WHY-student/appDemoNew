package com.gdu.demo;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.gdu.beans.WarnBean;
import com.gdu.camera.SettingsDefinitions;
import com.gdu.camera.StorageState;
import com.gdu.common.error.GDUError;
import com.gdu.config.ConnStateEnum;
import com.gdu.config.GlobalVariable;
import com.gdu.config.UavStaticVar;
import com.gdu.demo.databinding.ActivityFlightBinding;
import com.gdu.demo.flight.msgbox.MsgBoxBean;
import com.gdu.demo.flight.msgbox.MsgBoxManager;
import com.gdu.demo.flight.msgbox.MsgBoxPopView;
import com.gdu.demo.flight.msgbox.MsgBoxViewCallBack;
import com.gdu.demo.flight.setting.fragment.SettingDialogFragment;
import com.gdu.demo.ourgdu.ourGDUAircraft;
import com.gdu.demo.ourgdu.ourGDUVision;
import com.gdu.demo.utils.CommonDialog;
import com.gdu.demo.utils.GisUtil;
import com.gdu.demo.utils.LoadingDialogUtils;
import com.gdu.demo.utils.SettingDao;
import com.gdu.demo.utils.ToolManager;
import com.gdu.demo.views.PaintView;
import com.gdu.demo.widget.TopStateView;
import com.gdu.drone.LocationCoordinate2D;
import com.gdu.drone.LocationCoordinate3D;
import com.gdu.drone.TargetMode;
import com.gdu.gimbal.GimbalState;
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
import com.gdu.socketmodel.GduSocketConfig3;
import com.gdu.util.CollectionUtils;
import com.gdu.util.StatusBarUtils;
import com.gdu.util.StringUtils;
import com.gdu.util.ThreadHelper;
import com.gdu.util.logger.MyLogUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlightActivity extends FragmentActivity implements TextureView.SurfaceTextureListener, MsgBoxViewCallBack, View.OnClickListener {

    private ActivityFlightBinding viewBinding;
    private GDUCodecManager codecManager;
    private VideoFeeder.VideoDataListener videoDataListener ;

    private boolean showSuccess = false;

    private PaintView paintView;

    private ourGDUVision gduVision;
    private GDUCamera mGDUCamera;
    private GDUFlightController mGDUFlightController;

    private Context mContext;
    private int chacktimes=0;
    private int chacktimes1=0;
    private FlightState flightState;
    private Button changeMode;
    private Button changeFouse;
    private Button flyState;
    private Button backState;


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
        GDUFlightController mGDUFlightController = SdkDemoApplication.getAircraftInstance().getFlightController();
        if (mGDUFlightController != null){
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
                runOnUiThread(()->{
                    viewBinding.fpvRv.setHeadingAngle(yaw);
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
                runOnUiThread(() -> viewBinding.fpvRv.setGimbalAngle(yaw));
            });
        }
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
                    codecManager.sendDataToDecoder(bytes, size);
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
        flyState=findViewById(R.id.btn_take_off);
        backState=findViewById(R.id.btn_return_home);

        paintView = findViewById(R.id.paint_view);
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
        initGduvision();
        initCamera();
        mGDUCamera.getDisplayMode(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.DisplayMode>() {
            @Override
            public void onSuccess(SettingsDefinitions.DisplayMode displayMode) {
                if(displayMode == SettingsDefinitions.DisplayMode.THERMAL_ONLY){
                    changeMode.setText("可见光");
                }else{
                    changeMode.setText("红外");
                }
                if(displayMode==SettingsDefinitions.DisplayMode.ZL){
                    changeFouse.setText("变焦");
                }else{
                    changeFouse.setText("广角");
                }

            }

            @Override
            public void onFailure(GDUError var1) {
                toast("发送失败");
            }
        });
        GDUFlightController mGDUFlightController = SdkDemoApplication.getAircraftInstance().getFlightController();
        if (mGDUFlightController != null) {
            mGDUFlightController.setStateCallback(flightControllerState -> {
                flightState = flightControllerState.getFlightState();
                if (flightState == FlightState.BACKING) {
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

            });
        }
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
//        if (v.getId() == R.id.iv_msgBoxLabel) {
//            if (msgData.isEmpty() || StringUtils.isEmptyString(viewBinding.tvMsgBoxNum.getText().toString())
//                    || Integer.parseInt(viewBinding.tvMsgBoxNum.getText().toString().trim()) == 0) {
//                return;
//            }
//            showMsgBoxPopWindow(msgData);
//            viewBinding.ivMsgBoxLabel.setSelected(!viewBinding.ivMsgBoxLabel.isSelected());
//        }
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
//                    int width = mGduPlayView.getWidth();
//                    int height = mGduPlayView.getHeight();
//                    show(horizenDis, String.format("video width：%d", width));
//                    show(vercalDis, String.format("video height：%d", height));

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
                    mGDUCamera.setDisplayMode(SettingsDefinitions.DisplayMode.PIP, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(GDUError error) {
                            if (error == null) {
//                            initData();
                                toast("设置成分屏");
                            } else {
                                toast("发送失败");
                            }
                        }
                    });
                }
                catch (Exception e){
                    Toast.makeText(mContext, "setPipView Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.btn_zoom:
                try {
//                    int width = mGduPlayView.getWidth();
//                    int height = mGduPlayView.getHeight();
//                    show(horizenDis, String.format("video width：%d", width));
//                    show(vercalDis, String.format("video height：%d", height));

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
                GDUFlightController mGDUFlightController = SdkDemoApplication.getAircraftInstance().getFlightController();
                if (mGDUFlightController != null) {
                    mGDUFlightController.setStateCallback(flightControllerState -> {
                        flightState = flightControllerState.getFlightState();
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
                    });
                }
                break;
            case R.id.btn_return_home:
                 mGDUFlightController = SdkDemoApplication.getAircraftInstance().getFlightController();
                if (mGDUFlightController != null) {
                    mGDUFlightController.setStateCallback(flightControllerState -> {
                        flightState = flightControllerState.getFlightState();
                        if (flightState != FlightState.GROUND||flightState!=FlightState.BACKING) {
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

                    });
                }
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
}
