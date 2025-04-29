package com.gdu.demo;

import static com.gdu.util.RectUtil.getScreenHeight;
import static com.gdu.util.RectUtil.getScreenWidth;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;

import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gdu.AlgorithmMark;
import com.gdu.beans.WarnBean;
import com.gdu.camera.StorageState;
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
import com.gdu.demo.ourgdu.OnOurTargetDetectListener;
import com.gdu.demo.ourgdu.ourGDUAircraft;
import com.gdu.demo.views.IncrementalStateManager;
import com.gdu.demo.ourgdu.ourGDUCodecManager;
import com.gdu.demo.ourgdu.ourGDUVision;
import com.gdu.demo.utils.GisUtil;
import com.gdu.demo.utils.LoadingDialogUtils;
import com.gdu.demo.utils.SettingDao;
import com.gdu.demo.viewmodel.FlightViewModel;
import com.gdu.demo.views.ImageAdapter;
import com.gdu.demo.views.ImageItem;
import com.gdu.demo.views.ImageStorageManager;
import com.gdu.demo.widget.TopStateView;
import com.gdu.demo.widget.zoomView.S220CustomSizeFocusHelper;
import com.gdu.drone.LocationCoordinate2D;
import com.gdu.drone.LocationCoordinate3D;
import com.gdu.drone.TargetMode;
import com.gdu.gimbal.GimbalState;
import com.gdu.radar.ObstaclePoint;
import com.gdu.radar.PerceptionInformation;
import com.gdu.sdk.camera.GDUCamera;
import com.gdu.sdk.camera.SystemState;
import com.gdu.sdk.camera.VideoFeeder;
import com.gdu.sdk.codec.ImageProcessingManager;
import com.gdu.sdk.flightcontroller.GDUFlightController;
import com.gdu.sdk.gimbal.GDUGimbal;
import com.gdu.sdk.radar.GDURadar;
import com.gdu.sdk.util.CommonCallbacks;
import com.gdu.util.CollectionUtils;
import com.gdu.util.StringUtils;
import com.gdu.util.ThreadHelper;
import com.gdu.util.ViewUtils;
import com.gdu.util.logger.MyLogUtils;
import com.gdu.gimbal.Rotation;
import com.gdu.gimbal.RotationMode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FlightActivity extends FragmentActivity implements TextureView.SurfaceTextureListener, MsgBoxViewCallBack, View.OnClickListener {

    private ActivityFlightBinding viewBinding;
    private ourGDUCodecManager codecManager=null;
    private VideoFeeder.VideoDataListener videoDataListener;
    private IncrementalStateManager IncState;
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
    private RecyclerView recyclerView;
    private AppCompatImageView AIRecognize;
    private Spinner spinner;
    private ImageView imageView;
    private Button knowlageGrape;

    private Button knowledgeGraphButton;
    private WebView webView;
    private PopupWindow popupWindow;
    private List<String> recognizedModels = Arrays.asList("B-1B", "BMP-2");
    private int clickCount = 0; // 点击计数器
    private final int[] imageRes = {R.drawable.knowgrape1, R.drawable.knowgrap2};

    private Runnable resetStateTask; // 用于重置状态的任务
    private Runnable completeTask;
    private int incState = 0;
    private int unkonwNum = 0;

    private boolean isProcessRunning = false;
    private int latestModelID = 0;
    private int tempModelID=0;
    private int modelID = 0;
    private boolean isIncrementalMode = false;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();
    private ImageAdapter adapter1;
    private Context mContext;
    private  String IMAGE_DIR = "cropped_images";
    private   String LOAD_DIR="load_images";
//    private ImageView mYUVImageView;


    // 定义一个 Runnable 任务，用于更新 AI 状态

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Handler handler1 = new Handler(Looper.getMainLooper());
//    private Boolean isPaused = false;
//    private boolean isSelected = false;


    private GDUGimbal mGDUGimbal;
    private GDUCamera mGDUCamera;

    private ourGDUVision mGduVision;
    List<ImageItem> imageItems = new ArrayList<>();
    private ImageProcessingManager mImageProcessingManager;
    private ImageStorageManager storageManager;
    private int lastSavedNumber = 0;
    PopupWindow attributePopupWindow;

    View attributePopupView;

    PopupWindow photoPopupWindow;

    View photoPopupView;
    int savedID=-1;

    List<String> car_attribute_labels = new ArrayList<>();
//    {
//        car_attribute_labels.add("Public Transport");
//        car_attribute_labels.add("Private Transport");
//        car_attribute_labels.add("Multipurpose");
//        car_attribute_labels.add("Cargo Transport");
//        car_attribute_labels.add("Large Capacity");
//        car_attribute_labels.add("Small Capacity");
//        car_attribute_labels.add("Medium Capacity");
//        car_attribute_labels.add("Comfortable Driving");
//        car_attribute_labels.add("Off-Road Driving");
//        car_attribute_labels.add("High-Speed Driving");
//        car_attribute_labels.add("City Driving Adaptability");
//        car_attribute_labels.add("Complex Terrain Adaptability");
//        car_attribute_labels.add("High Payload Capacity");
//        car_attribute_labels.add("Low Payload Capacity");
//        car_attribute_labels.add("Family-Friendly");
//        car_attribute_labels.add("Long-Distance Travel");
//        car_attribute_labels.add("Low Fuel Consumption");
//        car_attribute_labels.add("High Fuel Consumption");
//        car_attribute_labels.add("Electric Option");
//        car_attribute_labels.add("High Environmental Impact");
//        car_attribute_labels.add("Large Parking Space Requirement");
//        car_attribute_labels.add("Small Parking Space Requirement");
//        car_attribute_labels.add("High Cost");
//        car_attribute_labels.add("Low Cost");
//        car_attribute_labels.add("Low Maneuverability");
//        car_attribute_labels.add("High Maneuverability");
//        car_attribute_labels.add("High Comfort");
//        car_attribute_labels.add("Low Comfort");
//        car_attribute_labels.add("Fast Driving");
//        car_attribute_labels.add("Slow Driving");
//        car_attribute_labels.add("City Street Suitability");
//        car_attribute_labels.add("Construction/Transport Suitability");
//        car_attribute_labels.add("Outdoor Driving");
//        car_attribute_labels.add("City & Off-Road Suitability");
//        car_attribute_labels.add("High Off-Road Ability");
//        car_attribute_labels.add("Low Off-Road Ability");
//        car_attribute_labels.add("Single Function");
//        car_attribute_labels.add("Multifunction");
//        car_attribute_labels.add("Off-Road Capability");
//        car_attribute_labels.add("City Commuting Suitability");
//    }
    {
        car_attribute_labels.add("公共交通工具");
        car_attribute_labels.add("私人交通工具");
        car_attribute_labels.add("多用途");
        car_attribute_labels.add("货物运输");
        car_attribute_labels.add("大容量");
        car_attribute_labels.add("小容量");
        car_attribute_labels.add("中容量");
        car_attribute_labels.add("舒适驾驶");
        car_attribute_labels.add("越野驾驶");
        car_attribute_labels.add("高速驾驶");
        car_attribute_labels.add("城市驾驶适应性");
        car_attribute_labels.add("复杂地形适应性");
        car_attribute_labels.add("高载重能力");
        car_attribute_labels.add("低载重能力");
        car_attribute_labels.add("家庭友好型");
        car_attribute_labels.add("长途旅行");
        car_attribute_labels.add("低油耗");
        car_attribute_labels.add("高油耗");
        car_attribute_labels.add("电动选项");
        car_attribute_labels.add("高环境影响");
        car_attribute_labels.add("大停车位需求");
        car_attribute_labels.add("小停车位需求");
        car_attribute_labels.add("高成本");
        car_attribute_labels.add("低成本");
        car_attribute_labels.add("低操控性");
        car_attribute_labels.add("高操控性");
        car_attribute_labels.add("高舒适性");
        car_attribute_labels.add("低舒适性");
        car_attribute_labels.add("快速驾驶");
        car_attribute_labels.add("慢速驾驶");
        car_attribute_labels.add("城市街道适用性");
        car_attribute_labels.add("建筑运输适用性");
        car_attribute_labels.add("户外驾驶");
        car_attribute_labels.add("城市与越野适用性");
        car_attribute_labels.add("高越野能力");
        car_attribute_labels.add("低越野能力");
        car_attribute_labels.add("单一功能");
        car_attribute_labels.add("多功能");
        car_attribute_labels.add("越野能力");
        car_attribute_labels.add("城市通勤适用性");
    }

    private final boolean photoIsDialog=false;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        viewBinding = ActivityFlightBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
//        setContentView(R.layout.knowledge_graph);
        viewModel = new ViewModelProvider(this).get(FlightViewModel.class);
        initView();
        initData();
        initGduVision(true);
        initCamera();
//        initBackgroundThread();
        initListener();
//        setupExternalDisplay();
        initAttributeDialog();
        //initKnowLedgeGraph();
        if(photoIsDialog){
            initPhotoDialog();
        }else{
            setPhotoShow(1);
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
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            codecManager.sendDataToDecoder(bytes, size);
                            // 延迟后执行的操作
                            // 这种直接通过yuvData复制的操作排除掉，因为这种操作会加重CPU的消耗，导致视频信号不畅
//                            byte[] yuvData =  codecManager.getYuvData();
//                            if(yuvData==null){
//                                Log.d("run ", "run: ");
//                            }
//                            Bitmap bitmap = mImageProcessingManager.convertYUVtoRGB(yuvData, codecManager.getVideoWidth(), codecManager.getVideoHeight());
//                            if(bitmap!=null){
//                                mPresentation.setBitMap(bitmap);
//                            }
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
//        unKnownum = findViewById(R.id.unknown_num);
        aiState = findViewById(R.id.ai_state);
        AIRecognize = findViewById(R.id.ai_recognize_imageview);
        spinner = findViewById(R.id.spinner);
        imageView=findViewById(R.id.imageView);
        knowlageGrape=findViewById(R.id.btnToggle);
        //know graph button
        knowledgeGraphButton=findViewById(R.id.button_know_graph);
        knowledgeGraphButton.setOnClickListener(this); // 设置点击监听器
        //webView = findViewById(R.id.webview);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                spinner.setSelection(0);
                return;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                spinner.setSelection(0);
//                showToast(""+latestModelID);
                // 当没有选项被选中时调用
            }
        });
        findViewById(R.id.all_ai_state).setVisibility(View.GONE);

        viewBinding.fpvRv.setShowObstacleOFF(!GlobalVariable.obstacleIsOpen);
        viewBinding.fpvRv.setObstacleMax(40);
        viewBinding.ivMsgBoxLabel.setOnClickListener(this);
        viewBinding.aiRecognizeImageview.setOnClickListener(this);
//        viewBinding.aiShowPhoto.setOnClickListener(this);
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
//        mYUVImageView = findViewById(R.id.test_imageview);
    }

    private void initData() {
        new MsgBoxManager(this, 1, this);
        VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(videoDataListener);
        updateSpinnerData(0);
//        showUnknownNUm();
    }
    // true的话就打开listener,false的话就关闭
    public void initGduVision(boolean flag){
        mGduVision = ((ourGDUAircraft) SdkDemoApplication.getProductInstance()).getGduVision();
        if(flag){
            // true的话就打开
            mGduVision.setOnTargetDetectListener(new OnOurTargetDetectListener() {
                //                    long startTime=System.currentTimeMillis();
                @Override
                public void onTargetDetecting(List<TargetMode> targetModes) {
                    if (targetModes != null && !targetModes.isEmpty()) {
                        GlobalVariable.isTargetDetectMode = true;
                        mTargetDetectHelper.startShowTarget();
                        viewBinding.aiPaintView.setRectParams(targetModes);
//                    tempModelID=viewBinding.aiPaintView.getModelID();
//                    updateModel(viewBinding.aiPaintView.getModelID());
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

                @Override
                public void onTargetDetectingNew(List<TargetMode> targetModes, int modelID, long savedID){
                    if (targetModes != null && !targetModes.isEmpty()) {
                        GlobalVariable.isTargetDetectMode = true;
                        mTargetDetectHelper.startShowTarget();
                        viewBinding.aiPaintView.setRectParams(targetModes);
                        GlobalVariable.algorithmType = AlgorithmMark.AlgorithmType.DEVICE_RECOGNISE;
                        for (TargetMode targetMode:targetModes){
                            if(targetMode.getTargetType()%16==6){
                                Log.d("saveImage", "saveImage");
                                byte[] yuvData = codecManager.getYuvData();
                                backgroundHandler.post(() -> {
                                    savedImage(targetMode, yuvData,IMAGE_DIR);
                                });
                                break;
                            }
                        }
                    }
                    // modelID
                    tempModelID=modelID;
                    updateModel(tempModelID);
                    updateSpinnerData(tempModelID);

//                    Log.d("savedID", "onTargetDetectingNew: "+ savedID);
                    // savedID
                    if(savedID!=0 && FlightActivity.this.savedID!=savedID){
                        FlightActivity.this.savedID = (int) savedID;
                        Log.d("savedID:success", "success"+ savedID);
                        int firstImageID = (int) savedID / 10000;
                        int tempSavedID = (int) savedID % 10000;
                        int secondImageID = (int) tempSavedID / 100;
                        int thirdImageID = (int) tempSavedID % 100;
                        runOnUiThread(() -> {
                            updatedPhotoList(firstImageID, secondImageID, thirdImageID);
                        });
                        runOnUiThread(() -> {
                            spinner.performClick();
                        });
                    }

                }
            });
        }else{
            // false就关掉
            mGduVision.setOnTargetDetectListener(new OnOurTargetDetectListener() {
                @Override
                public void onTargetDetectingNew(List<TargetMode> var1, int val2, long val3) {

                }

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
        }
    }

    private void initCamera(){
        mGDUCamera = (GDUCamera) ((ourGDUAircraft) SdkDemoApplication.getProductInstance()).getCamera();
        if (mGDUCamera != null) {
            mGDUCamera.setSystemStateCallback(new SystemState.Callback() {
                @Override
                public void onUpdate(SystemState systemState) {

                }
            });
            mGDUCamera.setStorageStateCallBack(new StorageState.Callback() {
                @Override
                public void onUpdate(StorageState state) {

                }
            });

        }
//        codecManager.enabledYuvData(true);
    }

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private void initBackgroundThread() {
        backgroundThread = new HandlerThread("ModelUpdateThread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
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

    @SuppressLint("InflateParams")
    public void initAttributeDialog() {
        attributePopupView = LayoutInflater.from(FlightActivity.this).inflate(R.layout.layout_popup_attribute, null);
        // 创建PopupWindow实例
        attributePopupWindow = new PopupWindow(
                attributePopupView,
                720,
                600,
                true
        );
        // 设置背景（避免点击外部无法关闭）
        attributePopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        attributePopupWindow.setOutsideTouchable(true);
    }

//    @SuppressLint("InflateParams")
//    public void initKnowLedgeGraph(){
//        LayoutInflater.from(FlightActivity.this).inflate(R.layout.knowledge_graph, null);
//    }
    @SuppressLint("InflateParams")
    public void initPhotoDialog() {
        photoPopupView = LayoutInflater.from(FlightActivity.this).inflate(R.layout.popup_layout, null);
        // 创建PopupWindow实例
        photoPopupWindow = new PopupWindow(
                photoPopupView,
                1100,
                670,
                true
        );
        // 设置背景（避免点击外部无法关闭）
        photoPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        photoPopupWindow.setOutsideTouchable(true);
        setPhotoShow(4);
    }

    public void setPhotoShow(int temp){
        imageItems.clear();
        imageItems.add(new ImageItem("images/image1.png", "标签1"));
        imageItems.add(new ImageItem("images/image2.png", "标签2"));
        imageItems.add(new ImageItem("images/image3.png", "标签3"));
        imageItems.add(new ImageItem("images/image4.png", "标签4"));
        imageItems.add(new ImageItem("images/image5.png", "标签5"));
        imageItems.add(new ImageItem("images/image6.png", "标签6"));
        imageItems.add(new ImageItem("images/image7.png", "标签7"));
        imageItems.add(new ImageItem("images/image8.png", "标签8"));

        // 初始化 RecyclerView
        if(photoIsDialog){
            recyclerView = photoPopupView.findViewById(R.id.recyclerView);
        } else{
            recyclerView = findViewById(R.id.photo_recyclerView);
        }
        recyclerView.setLayoutManager(new GridLayoutManager(this, temp)); // 每排 3 个
        adapter1 = new ImageAdapter(imageItems, this);
        recyclerView.setAdapter(adapter1);
    }

    // 更新显示/隐藏状态
    private void updateImageVisibility() {
        if (clickCount % 2 == 1) { // 奇数次点击显示
            imageView.setVisibility(View.VISIBLE);
            updateImageDisplay();
        } else { // 偶数次点击隐藏
            imageView.setVisibility(View.GONE);
        }
    }

    // 根据增量状态更新图片
    private void updateImageDisplay() {
        int resId = isIncrementalMode ? imageRes[1] : imageRes[0];
        imageView.setImageResource(resId);

        // 可选：添加切换动画
//        imageView.animate()
//                .alpha(0).setDuration(100)
//                .withEndAction(() -> {
//                    imageView.setAlpha(1);
//                    imageView.setImageResource(resId);
//                }).start();
    }

    public Bitmap cropImage(TargetMode clickBox, byte[] yuvData) {

        if (yuvData != null) {
            Bitmap bitmap = mImageProcessingManager.convertYUVtoRGB(yuvData, codecManager.getVideoWidth(), codecManager.getVideoHeight());
            Bitmap bitmap2 = Bitmap.createBitmap(bitmap, (int) (clickBox.getLeftX()/1.5), (int) (clickBox.getLeftY() /1.5), (int) (clickBox.getWidth()/1.5), (int) (clickBox.getHeight() /1.5));
            bitmap.recycle();
            return bitmap2;
        }else {
            showToast("yuvData为空");
            return null;
        }
    }

    public void savedImage(TargetMode clickBox, byte[] yuvData,String temp) {
        Bitmap bitmap2 = cropImage(clickBox, yuvData);
        if(bitmap2!=null){
            int savedNumber = storageManager.saveImage(bitmap2,temp);
            if (savedNumber > 0) {
                lastSavedNumber = savedNumber;
//                showToast("保存成功，编号: " + lastSavedNumber);
            }else{
                showToast("保存失败");
            }
        }else {
            showToast("获取图像为空");
        }

    }

    public void updatedPhotoList(int firstImageID, int secondImageID, int thirdImageID){
//                    if (lastSavedNumber > 0) {
//                        storageManager.loadImageToView(lastSavedNumber, mYUVImageView);
//                    } else {
//                        Toast.makeText(this, "请先保存图片", Toast.LENGTH_SHORT).show();
//                    }
            if(thirdImageID==0){
                thirdImageID = 1;
            }
            String picture1=storageManager.getAbsolutePath(IMAGE_DIR,firstImageID);
            String picture2=storageManager.getAbsolutePath(IMAGE_DIR,secondImageID);
            String picture3=storageManager.getAbsolutePath(IMAGE_DIR,thirdImageID);
//            Log.d("picture", picture1);
//            Log.d("picture", picture2);
//            Log.d("picture", picture3);
            List<ImageItem> newItems = new ArrayList<>();
//            storageManager.loadImageToView(lastSavedNumber, mYUVImageView);
            newItems.add(new ImageItem(picture1, "标签9"));
            newItems.add(new ImageItem(picture2, "标签10"));
            newItems.add(new ImageItem(picture3, "标签11"));
            GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
            assert layoutManager != null;
            adapter1.addNewItems(newItems, layoutManager);
            handler1.postDelayed(()->{
                    recyclerView.smoothScrollToPosition(0);
            },500);

//            recyclerView.postDelayed(() -> {
//                layoutManager.scrollToPositionWithOffset(adapter1.getItemCount()-1, 0);
//            }, 200);
    }
    public void showNineGridShow(boolean show) {
        viewBinding.nightGridView.setVisibility(show ? View.VISIBLE : View.GONE);
    }


    public void beginCheckCloud() {
        showSuccess = false;
        mGDUGimbal = (GDUGimbal) ((ourGDUAircraft) SdkDemoApplication.getProductInstance()).getGimbal();
        if (mGDUGimbal == null) { return; }
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                mGduPlayView.beginRecord("/mnt/sdcard/gdu","ron.mp4");
            }
        }
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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (codecManager == null) {
            codecManager = new ourGDUCodecManager(FlightActivity.this, surface, width, height);
            // 设置为true后才能获取到图像
//            codecManager.enabledYuvData(true);
//            codecManager = mPresentation.getCodecManager();
        }

        storageManager = new ImageStorageManager(this);
        mImageProcessingManager = new ImageProcessingManager(FlightActivity.this);
        viewBinding.textureView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                // 根据事件类型处理（如ACTION_DOWN、ACTION_UP等）
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_UP:
                        int x = (int) event.getX();
                        int y = (int) event.getY();
                        TargetMode clickBox =  viewBinding.aiPaintView.getTargetModebyXY(x,y);
                        if (clickBox==null){
                            Log.d("clickBox", "onClick: 未点击到物体");
                            break;
                        }
                        showAttributewithBox(clickBox);
                        v.performClick();
                        break;
                    case MotionEvent.ACTION_DOWN:
                        return true;
                }
                return true; // 返回true表示消费事件，后续事件继续传递
            }

        });

    }

    public void showAttributewithBox(TargetMode clickBox){

        final float density = getResources().getDisplayMetrics().density;

        // 转换最大尺寸限制
        int maxWidthPx = (int) (400 * density); // 500dp -> 像素
        int maxHeightPx = (int) (200 * density); // 200dp -> 像素
        byte[] yuvData = codecManager.getYuvData();
        long time1=System.currentTimeMillis();
        savedImage(clickBox,yuvData,LOAD_DIR);

        // 创建RenderScript实例
//        byte[] croppedI420 = cropI420(yuvData, codecManager.getVideoWidth(), codecManager.getVideoHeight(), clickBox);
//        Bitmap bitmap = mImageProcessingManager.convertYUVtoRGB(yuvData, codecManager.getVideoWidth(), codecManager.getVideoHeight());
//        Bitmap bitmap=i420ToRgbWithRenderScript(yuvData,codecManager.getVideoWidth(),codecManager.getVideoHeight(),mContext);
//        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, (int) (clickBox.getLeftX()/1.5), (int) (clickBox.getLeftY() /1.5), (int) (clickBox.getWidth()/1.5), (int) (clickBox.getHeight() /1.5));
//        bitmap.recycle();
////        Bitmap croppedBitmap = cropImage(clickBox, yuvData);
//        long time4=System.currentTimeMillis();
//        Log.d("sysTime3",""+(time1-time4));
//        float widthRatio = (float) maxWidthPx / croppedBitmap.getWidth();
//        float heightRatio = (float) maxHeightPx / croppedBitmap.getHeight();
//        // 取最小值以保证两个方向都不超限
//        float scaleFactor = Math.min(widthRatio, heightRatio);
//        Matrix matrix = new Matrix();
//        matrix.postScale(scaleFactor, scaleFactor);
//        long time3=System.currentTimeMillis();
//        Log.d("sysTime1",""+(time1-time3));
//        Bitmap scaledBitmap = Bitmap.createBitmap(
//                croppedBitmap, 0, 0, croppedBitmap.getWidth(), croppedBitmap.getHeight(), matrix, true
//        );
//        long time2=System.currentTimeMillis();
//        Log.d("sysTime2",""+(time1-time2));
        ImageView instanceImageView = attributePopupView.findViewById(R.id.target_image);
//        instanceImageView.setImageBitmap(scaledBitmap);
        storageManager.loadImageToView(LOAD_DIR,lastSavedNumber,instanceImageView);
        storageManager.clearDirectory(LOAD_DIR);

        // 显示里面对应的各条属性，目前仅展示class
        String class_label = viewBinding.aiPaintView.getClassLabel().get(clickBox.getTargetType()%16);
        if(Objects.equals(class_label, "saved")){
            class_label="unknown";
        }
        show(attributePopupView.findViewById(R.id.target_label_name), class_label);
        int coarseIndex = clickBox.getTargetType() / 16;
        List<String> attributeList;
        switch (coarseIndex){
            case 1:
                attributeList = viewBinding.aiPaintView.getAttributeLabel1();
                break;
            case 2:
                attributeList = viewBinding.aiPaintView.getAttributeLabel2();
                break;
            case 3:
                attributeList = viewBinding.aiPaintView.getAttributeLabel3();
                break;
            default:
                attributeList = viewBinding.aiPaintView.getAttributeLabel1();
                Log.d("attribute", "大类没有index");
//                return;
        }
//        attributeList = car_attribute_labels;
        attributeList = viewBinding.aiPaintView.getAttributeLabel2();

        // 1. 将byte转换为8位二进制字符串（补前导零）
        String byteBinary = String.format("%8s", Integer.toBinaryString(clickBox.getTargetConfidence() & 0xFF))
                .replace(' ', '0');

        // 2. 将int转换为32位二进制字符串（补前导零）
//        String intBinary = String.format("%32s", Integer.toBinaryString(clickBox.getId()))
//                .replace(' ', '0');
        String intBinary = String.format("%32s", Integer.toBinaryString(clickBox.getId()))
                .replace(' ', '0');

        // 3. 拼接成40位二进制字符串
        String combined = intBinary + byteBinary;
        combined = "01000110001100010010100000";
        int len = attributeList.size();

        StringBuilder result = new StringBuilder();
        for (int pos = 0; pos < len; pos++) {
            if (combined.charAt(pos) == '1') { // 从左到右对应高位到低位
                result.append(attributeList.get(pos)+"\n");
            }
        }

        String attribute_labels = result.toString();
        show(attributePopupView.findViewById(R.id.target_attribute), attribute_labels);

        attributePopupWindow.showAtLocation(viewBinding.aiPaintView, Gravity.CENTER, 0, -20);
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
                -> viewBinding.ivMsgBoxLabel.setSelected(false), 650));
        if (isShow) {
            mMsgBoxPopWin.dismiss();
        } else {
            mMsgBoxPopWin.updateMsgData(data);
            mMsgBoxPopWin.showAsDropDown(viewBinding.ivMsgBoxLabel, 0,
                    getResources().getDimensionPixelOffset(R.dimen.dp_6),
                    Gravity.BOTTOM);
        }
    }



    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            backgroundThread = null;
            backgroundHandler = null;
        }
    }

//    private void showUnknownNUm(){// 结果为 34
//        dataList = new ArrayList<>();
//        dataList.add("未知类数量：0"); // 默认文本
//        dataList.add("新类1数量：0" );
//        dataList.add("新类2数量：0" );
//        dataList.add("新类3数量：0" );
//        adapter = new ArrayAdapter<>(this, R.layout.spinner_item_white, dataList);
//        adapter.setDropDownViewResource(R.layout.spinner_item_white);
//        spinner.setAdapter(adapter);
//
//        // 设置 Spinner 的选项选择监听器
//
//    }
public Bitmap i420ToRgbWithRenderScript(byte[] i420Data, int width, int height, Context context) {
    // 1. 验证I420数据长度 (Y: width×height, U+V: width×height/2)
    if (i420Data == null || i420Data.length != width * height * 3 / 2) {
        throw new IllegalArgumentException("Invalid I420 data size");
    }

    RenderScript rs = RenderScript.create(context);
    Bitmap bitmap = null;

    try {
        // 2. 将I420手动转换为NV21（因为RenderScript只支持NV21）
        byte[] nv21Data = I420ToNv21(i420Data, width, height);

        // 3. 创建YUV转RGB脚本
        ScriptIntrinsicYuvToRGB yuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

        // 4. 创建输入Allocation（NV21格式）
        Type yuvType = new Type.Builder(rs, Element.U8(rs))
                .setX(nv21Data.length)
                .create();
        Allocation input = Allocation.createTyped(rs, yuvType);
        input.copyFrom(nv21Data);

        // 5. 创建输出Allocation（RGBA）
        Type rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs))
                .setX(width)
                .setY(height)
                .create();
        Allocation output = Allocation.createTyped(rs, rgbaType);

        // 6. 执行转换
        yuvToRgb.setInput(input);
        yuvToRgb.forEach(output);

        // 7. 生成Bitmap
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        output.copyTo(bitmap);

    } finally {
        rs.destroy();
    }
    return bitmap;
}
    private byte[] I420ToNv21(byte[] i420bytes, int width, int height) {
        byte[] nv21bytes = new byte[i420bytes.length];
        int y_len = width * height;
        int uv_len = y_len / 4;
        System.arraycopy(i420bytes, 0, nv21bytes, 0, y_len);

        for(int i = 0; i < uv_len; ++i) {
            byte u = i420bytes[y_len + i];
            byte v = i420bytes[y_len + uv_len + i];
            nv21bytes[y_len + i * 2] = v;
            nv21bytes[y_len + i * 2 + 1] = u;
        }

        return nv21bytes;
    }


    private void updateSpinnerData(int modelID) {
        // 如果更新过快的话会造成dataList的notifyDataSetChanged()的时候刚好dataList.clear();，就会导致异常
        if(!dataList.isEmpty() && modelID == latestModelID) return;
        if(modelID/1000000000==2 && modelID/100000000 % 10 == 0){
            Log.d("增量中", "updateSpinnerData: ");
            return;
        }
        latestModelID = modelID;

        runOnUiThread(() -> {
            dataList.clear();
            int temp = modelID % 100000000;
            int temp2 = temp / 1000000;
            int lastSixDigits = modelID % 1000000;
            int part1 = (lastSixDigits / 10000) % 100;
            int part2 = (lastSixDigits / 100) % 100;
            int part3 = lastSixDigits % 100;

            dataList.add("未知类数量：" + temp2);
            dataList.add("新类1数量：" + part1);
            dataList.add("新类2数量：" + part2);
            dataList.add("新类3数量：" + part3);
            unkonwNum = temp2;

            // 重新设置适配器以确保更新
            if(adapter == null) {
                adapter = new ArrayAdapter<>(this, R.layout.spinner_item_white, dataList);
                adapter.setDropDownViewResource(R.layout.spinner_item_white);
                adapter.registerDataSetObserver(new DataSetObserver() {
                    @Override
                    public void onChanged() {
                        Log.d("spinner", "Changed");
                        spinner.setSelection(0, false); // 自动选中第0项
                    }
                });
                spinner.setAdapter(adapter);
            } else {
                Log.d("增量", ""+modelID);
                adapter.notifyDataSetChanged();
//                spinner.setSelection(0); // 重新设置选中位置
            }
        });
    }
//    private void updateKnowNum(int modelID) {
//        int num = modelID % 100;
//        if (num == unkonwNum) {
//            Log.d("TaskDebug", "ModelID unchanged: " + modelID + ", skipping update");
//            return;
//        }
//        if (num != unkonwNum) {
////            show(unKnownum, "  " + "未知类数目 " + num);
//            unkonwNum = num;
//        }
//    }

//    private boolean isTaskRunning = false; // 标记是否有延迟任务正在运行

    private void updateModel(int newModelID) {
        if(modelID==newModelID){
            return;
        }
        modelID = newModelID;
        int firstNum = modelID / 1000000000;
        int secondNum = (modelID%1000000000)/100000000;
        int temp = modelID / 100000000;
        incState = firstNum -1;
        if(temp == 10){
            show(aiState, "AI状态：未增量");
        }else {
            if(secondNum==0){
                show(aiState, "AI状态：增量" + incState + "中");
            }else {
//                show(aiState, "AI状态：增量" + incState + "完成");
                show(aiState, "AI状态：增量完成");
            }
        }
//        updateSpinnerData(modelID);

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

//    // 完成任务并重置状态
//    private void completeTask() {
//        isTaskRunning = false; // 标记任务完成
////        Log.d("TaskDebug", "Task completed, latest modelID: " + latestModelID);
//
//        // 根据最新的 modelID 更新状态
////        updateModel(latestModelID);
//    }


    @SuppressLint("NonConstantResourceId")
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
                codecManager.enabledYuvData(true);
                updateSpinnerData(0);
                AIRecognize.setEnabled(false);
                storageManager.clearDirectory(IMAGE_DIR);
                try {
                    FlightActivity.this.savedID=-1;
                    initBackgroundThread();
                    // 开启检测
                    initGduVision(true);
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
                    isProcessRunning = true;
                    quitAIRecognize.setEnabled(true);
                    startIncremental.setEnabled(true);
                    show(aiState, "AI状态：未增量");
                    AIRecognize.setImageResource(R.drawable.ai_recognize_selected);
                    findViewById(R.id.all_ai_state).setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    showToast("请检查初始化是否完成");
                    AIRecognize.setEnabled(true);
                }
                break;
            case R.id.button_quit_ai:
                codecManager.enabledYuvData(false);
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
                    stopBackgroundThread();
                    if (mGduVision != null) {
                        initGduVision(false);
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
                    AIRecognize.setImageResource(R.drawable.ai_recognize);
                    findViewById(R.id.all_ai_state).setVisibility(View.GONE);
                    if(photoIsDialog){
                        setPhotoShow(4);
                    }else{
                        setPhotoShow(1);
                    }
                    isIncrementalMode=false;
//                    IncrementalStateManager.getInstance().setIncremental(false);
                }catch(Exception e){
                    quitAIRecognize.setEnabled(true);
                    isIncrementalMode=true;
//                    IncrementalStateManager.getInstance().setIncremental(true);
                }
                break;
            case R.id.button_gimbal_rotate:
                mGDUGimbal = (GDUGimbal) ((ourGDUAircraft) SdkDemoApplication.getProductInstance()).getGimbal();
                Rotation rotation = new Rotation();
                rotation.setMode(RotationMode.ABSOLUTE_ANGLE);
                rotation.setPitch(-90);
                mGDUGimbal.rotate(rotation, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(GDUError error) {
                        if (error == null) {
                            showToast("云台向下");
                        }
                    }
                });
                break;
            case R.id.button_gimbal_reset:
                mGDUGimbal = (GDUGimbal) ((ourGDUAircraft) SdkDemoApplication.getProductInstance()).getGimbal();
                mGDUGimbal.reset(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(GDUError error) {
                        if (error == null) {
                            showToast("云台回正");
                        }
                    }
                });
                break;
            case R.id.button_start_incremental:
//                if(unkonwNum < 10){
//                    showToast("未知类别数目过少，请收集更多未知类别");
//                    break;
//                }

                if (mGduVision != null) {
                    mGduVision.targetDetect((byte) 3, (short) 0, (short) 0, (short) 0, (short) 0, (byte) 0, (byte) 0, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(GDUError var1) {
                            if (var1 == null) {
                                showToast("开始增量");
//                                imageItems.add(new ImageItem("images/image1.png", "标签9"));
//                                imageItems.add(new ImageItem("images/image2.png", "标签10"));
//                                imageItems.add(new ImageItem("images/image3.png", "标签11"));
//                                recyclerView = photoPopupView.findViewById(R.id.recyclerView);
//                                recyclerView.setLayoutManager(new GridLayoutManager(FlightActivity.this, 4)); // 每排 3 个
//                                ImageAdapter adapter = new ImageAdapter(imageItems, FlightActivity.this);
//                                recyclerView.setAdapter(adapter);
//                                adapter1.notifyDataSetChanged();
                                isIncrementalMode=true;
                            } else {
                                showToast("开始增量失败");
                                isIncrementalMode=false;
//                                IncrementalStateManager.getInstance().setIncremental(false);
                            }
                        }
                    });
                } else {
                    showToast("请检查初始化是否成功");
                    isIncrementalMode=false;
//                    IncrementalStateManager.getInstance().setIncremental(false);
                }
                if (imageView.getVisibility() == View.VISIBLE) {
                    updateImageDisplay();
                }

//                updatedPhotoList(1,5,9);
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
            case R.id.btnToggle:
                clickCount++;
                updateImageVisibility();
                break;
            case R.id.button_know_graph:
                showKnowledgeGraphPopup();
                break;
            case R.id.spinner:
                // 添加新数据
                spinner.setOnTouchListener(new View.OnTouchListener() {

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            updateSpinnerData(latestModelID);
                            showToast(""+latestModelID);
                        }
                        return false;
                    }
                });
                break;
//            case R.id.ai_show_photo:
//                photoPopupWindow.showAtLocation(viewBinding.aiPaintView, Gravity.CENTER, 0, 0);
//                break;
        }
    }
    private void showKnowledgeGraphPopup() {
        // 加载弹出窗口布局
        View popupView = getLayoutInflater().inflate(R.layout.webview_popup, null);

        // 初始化WebView
        WebView[] webView2 = {popupView.findViewById(R.id.webView)};
        webView=webView2[0];
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true); // 启用 JavaScript
        webSettings.setDomStorageEnabled(true); // 启用 DOM 存储
        webSettings.setAllowFileAccess(true); // 允许访问文件系统
        webSettings.setUseWideViewPort(true);  // 启用宽视口
        webSettings.setLoadWithOverviewMode(true);  // 缩放至屏幕宽度
        webSettings.setBuiltInZoomControls(true);  // 启用缩放控件
        webSettings.setDisplayZoomControls(false);  // 隐藏原生缩放按钮
        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        webView.loadUrl("file:///android_asset/templates/fenlei2.html");
        webView.addJavascriptInterface(new AndroidInterface(), "Android");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // 在 PopupWindow 显示后获取其大小并传递给 HTML 页面
//                popupWindow.getContentView().post(() -> {
//                    int width = popupWindow.getWidth();
//                    int height = popupWindow.getHeight();
//                    view.loadUrl("javascript:resizeMainDiv(" + width + ", " + height + ")");
//                });
            }
        });


        // 创建PopupWindow
        popupWindow = new PopupWindow(
                popupView,
                (int) (getScreenWidth() * 0.7),  // 屏幕宽度的80%
                (int) (getScreenHeight() * 0.6),
                true
        );
        // 设置关闭按钮
        Button closeButton = popupView.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> {
            if (popupWindow != null && popupWindow.isShowing()) {
                popupWindow.dismiss();
            }
        });

        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);
        // 设置背景和动画
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        //popupWindow.setAnimationStyle(R.style.PopupAnimation);

        // 显示在屏幕中央
        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0);

        // 设置PopupWindow消失时的监听
        popupWindow.setOnDismissListener(() -> {
            if (webView != null) {
                webView.destroy();
                webView = null;
            }
        });

    }

    // 获取屏幕宽度
    private int getScreenWidth() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return metrics.widthPixels;
    }

    // 获取屏幕高度
    private int getScreenHeight() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return metrics.heightPixels;
    }

    //todo read .txt file then send html
    class AndroidInterface {
        @android.webkit.JavascriptInterface
        public void onJsReady() {
            runOnUiThread(new Runnable(){
                @Override
                public void run(){
                    try {
                        String coarseName = readFileFromAssets("templates/coarse_name_temp.txt");
                        String fineName = readFileFromAssets("templates/fine_name_temp.txt");
                        String clsName = readFileFromAssets("templates/cls_name_temp.txt");
                        String attName = readFileFromAssets("templates/att_name.txt");
                        String fine2Coarse = readFileFromAssets("templates/fine2coarse_temp.txt");
                        String cls2Fine = readFileFromAssets("templates/cls2fine_temp.txt");
                        String attribute = readFileFromAssets("templates/attribute_temp.txt");

                        // 使用 evaluateJavascript 调用 JavaScript 方法传入txt文件内容
                        String jsCode = "receiveFiles('" + escapeJavaScript(coarseName) + "','"
                                + escapeJavaScript(fineName) + "','" + escapeJavaScript(clsName) + "','"
                                + escapeJavaScript(attName) + "','" + escapeJavaScript(fine2Coarse) + "','"
                                + escapeJavaScript(cls2Fine) + "','" + escapeJavaScript(attribute) + "')";
                        Log.d("jscode",jsCode);
                        webView.setWebChromeClient(new WebChromeClient() {
                            @Override
                            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                                Log.d("WebViewConsole", consoleMessage.message() + " -- From line "
                                        + consoleMessage.lineNumber() + " of "
                                        + consoleMessage.sourceId());
                                return super.onConsoleMessage(consoleMessage);
                            }
                        });
                        //"javascript:"+"console.log('hello world');"
                       webView.evaluateJavascript(jsCode, new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String value) {
                                Log.d("WebViewJS", "JavaScript 执行结果: " + value);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

        }

        @android.webkit.JavascriptInterface
        public void onChartReady(){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("chartReady","begin filter");
                    // 图表准备好后调用 filterByModel 函数
                    StringBuilder sb = new StringBuilder();
                    sb.append("filterByModel([");
                    for (int i = 0; i < recognizedModels.size(); i++) {
                        sb.append("'").append(recognizedModels.get(i)).append("'");
                        if (i < recognizedModels.size() - 1) {
                            sb.append(",");
                        }
                    }
                    sb.append("])");
                    webView.evaluateJavascript(sb.toString(), new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            Log.d("WebViewJS", "JavaScript 执行结果: " + value);
                        }
                    });
                }
            });
        }
    }

    private String readFileFromAssets(String fileName) {
        String result = "";
        try {
            InputStream is = getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            result = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private String escapeJavaScript(String input) {
        return input.replace("'", "\\'").replace("\r\n", "\\n").trim();//.replace("\n", "\\n")
    }

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
    public int changeNum(int num){
        if(num<0){
            num=0;
        }
        if(num%2==0){
            return num;
        }else{
            return num-1;
        }
    }
//    public  byte[] cropI420(byte[] srcI420, int srcWidth, int srcHeight, TargetMode cropRect) {
//        // 参数校验
//        int left=changeNum(cropRect.getLeftY());
//        int top=changeNum(cropRect.getLeftX());
//        int cropWidth = changeNum(cropRect.getWidth());
//        int cropHeight = changeNum(cropRect.getHeight());
////        left&=~1;
////        top&=~1;
//
//        if (left % 2 != 0 || top % 2 != 0 ||
//                cropWidth% 2 != 0 || cropHeight % 2 != 0) {
//            throw new IllegalArgumentException("Crop rect must have even coordinates/sizes");
//        }
//        byte[] dstI420 = new byte[cropWidth * cropHeight * 3 / 2]; // I420大小公式
//
//        // 1. 计算各平面起始指针
//        final int srcYSize = srcWidth * srcHeight;
//        final int srcUVSize = srcYSize / 4;
//        final int dstYSize = cropWidth * cropHeight;
//        final int dstUVSize = dstYSize / 4;
//
//        // 2. 裁剪Y平面（全分辨率）
//        cropPlane(srcI420, 0, srcWidth, srcHeight,
//                new Rect(left,top,Math.min(left+cropHeight,codecManager.getVideoHeight()),Math.min(top+cropWidth,codecManager.getVideoWidth())), dstI420, 0, cropWidth);
//
//        // 3. 裁剪U平面（1/2分辨率）
//        cropPlane(srcI420, srcYSize, srcWidth / 2, srcHeight / 2,
//                new Rect(left / 2, top / 2,
//                        (left+cropHeight)/ 2, (top+cropWidth) / 2),
//                dstI420, dstYSize, cropWidth / 2);
//
//        // 4. 裁剪V平面（1/2分辨率）
//        cropPlane(srcI420, srcYSize + srcUVSize, srcWidth / 2, srcHeight / 2,
//                new Rect(left / 2, top / 2,
//                        (left+cropHeight)/ 2, (top+cropWidth) / 2),
//                dstI420, dstYSize + dstUVSize, cropWidth / 2);
//
//        return dstI420;
//    }
//    private static void cropPlane(
//            byte[] src, int srcOffset, int srcStride, int srcHeight,
//            Rect cropRect, byte[] dst, int dstOffset, int dstStride
//    ) {
//        for (int y = 0; y < cropRect.height(); y++) {
//            int srcLineStart = srcOffset + (y + cropRect.top) * srcStride + cropRect.left;
//            int dstLineStart = dstOffset + y * dstStride;
//            System.arraycopy(
//                    src, srcLineStart,
//                    dst, dstLineStart,
//                    cropRect.width()
//            );
//        }
//    }

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

//    private void setupExternalDisplay() {
//        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
//        Display[] displays = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
//        Log.d("setupExternalDisplay", "setupExternalDisplay: "+displays.length);
//        if (displays != null && displays.length > 0) {
//            // 简单起见，使用第一个外接显示设备
//            Display externalDisplay = displays[0];
//            mPresentation = new ExternalDisplayPresentation(this, externalDisplay);
//            mPresentation.show();
//        }
//    }

