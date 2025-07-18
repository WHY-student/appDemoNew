package com.gdu.demo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;

import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
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
import com.gdu.demo.utils.ModelControlUtil;
import com.gdu.demo.utils.ScreenUtils;
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
import com.gdu.demo.widgetlist.lighttype.LightSelectedView;
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
import com.gdu.gimbal.Rotation;
import com.gdu.gimbal.RotationMode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class FlightActivity extends FragmentActivity implements TextureView.SurfaceTextureListener, MsgBoxViewCallBack, View.OnClickListener{

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

    private WebView webView;
    private PopupWindow KnowledgeGraphPopupWindow;
    private final List<String> recognizedModels = Arrays.asList("B-1B", "BMP-2");
    private int incState = 0;

    private int latestModelID = 0;
    private int tempModelID=0;
    private int modelID = 0;
    private boolean isIncrementalMode = false;
    private ArrayAdapter<String> adapter;
    private final List<String> dataList = new ArrayList<>();
    private ImageAdapter adapter1;
    private Context mContext;
    private final String IMAGE_DIR = "cropped_images";
    private final String LOAD_DIR="load_images";

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Handler handler1 = new Handler(Looper.getMainLooper());

    private GDUGimbal mGDUGimbal;
    private GDUCamera mGDUCamera;

    private ourGDUVision mGduVision;
    private final List<ImageItem> imageItems = new ArrayList<>();
    private ImageProcessingManager mImageProcessingManager;
    private ImageStorageManager storageManager;
    private int lastSavedNumber = 0;
    private PopupWindow attributePopupWindow;
    private View attributePopupView;

    private View photoPopupView;
    private int savedID=-1;
    private LightSelectedView lightSelectedView;

    private final List<String> object_labels = new ArrayList<>();
    {
        object_labels.add("尼米兹号");
        object_labels.add("标签2");
        object_labels.add("标签3");
        object_labels.add("标签4");
        object_labels.add("标签5");
        object_labels.add("标签6");
        object_labels.add("标签7");
        object_labels.add("标签8");
        object_labels.add("新类别1");
        object_labels.add("新类别2");
        object_labels.add("新类别3");
    }
    private final boolean photoIsDialog=false;

    private int native_threshold = 0;

    private ModelControlUtil model_control;

    private OnOurTargetDetectListener visual_listener;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // 功能：1.展示检测结果  涉及到的变量
        // 2. 保存增量的新图片
        // 3. 展示增量状态
        // 4. 展示增量后的图片
        // 5. 展示增量前后的知识图谱
        super.onCreate(savedInstanceState);
        mContext = this;
        viewBinding = ActivityFlightBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        viewModel = new ViewModelProvider(this).get(FlightViewModel.class);

        initView();
        initData();

        initCamera();
        initListener();
        initVisualListener();
        setNormalUI();

        initKnowledgeGraph();
        lightSelectedView.setupSelectButtonClick(() -> {
            handler1.post(() -> setPhotoShow(1));
        });
    }
    // 检查接口定义，确保完全匹配
//    public interface OnButtonStateChangeListener {
//        void onButtonStateChanged(boolean enabled);  // 注意方法名和参数
//    }

    // 然后在FlightActivity中确保实现完全匹配
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
                        }
                    }, 550);
                }
            }
        };
        quitAIRecognize = findViewById(R.id.button_quit_ai);
        quitAIRecognize.setEnabled(false);
        startIncremental = findViewById(R.id.button_start_incremental);
        startIncremental.setEnabled(false);
        aiState = findViewById(R.id.ai_state);
        AIRecognize = findViewById(R.id.ai_recognize_imageview);
        spinner = findViewById(R.id.spinner);
        imageView=findViewById(R.id.imageView);
        lightSelectedView=findViewById(R.id.light_select);
//        lightSelectedView.setOnButtonStateChangeListener(this);
        lightSelectedView.setOnClickListener(this);
        //know graph button
        Button knowledgeGraphButton = findViewById(R.id.button_know_graph);
        knowledgeGraphButton.setOnClickListener(this); // 设置点击监听器
        //webView = findViewById(R.id.webview);

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

//        云台向下及回正
        viewBinding.buttonGimbalRotate.setOnClickListener(this);
        viewBinding.buttonGimbalReset.setOnClickListener(this);
        lightSelectedView.setButtonsEnabled(true);
        lightSelectedView.setButtonBackgroundColor(R.color.white);

    }

    private void initData() {
        new MsgBoxManager(this, 1, this);
        VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(videoDataListener);
        updateSpinnerData(0);
//        showUnknownNUm();
    }
    // true的话就打开listener,false的话就关闭
    public void initVisualListener(){
        mGduVision = ((ourGDUAircraft) SdkDemoApplication.getProductInstance()).getGduVision();
        model_control = new ModelControlUtil(mGduVision);
        visual_listener = new OnOurTargetDetectListener() {
                @Override
                public void onTargetDetecting(List<TargetMode> targetModes) {
                    if (targetModes != null && !targetModes.isEmpty()) {
                        GlobalVariable.isTargetDetectMode = true;
                        viewBinding.aiPaintView.setRectParams(targetModes);
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
//                    Log.d("onTargetDetectingNew: ", "onTargetDetectingNew: ");
                    if (targetModes != null && !targetModes.isEmpty()) {
                        GlobalVariable.isTargetDetectMode = true;
                        viewBinding.aiPaintView.setRectParams(targetModes);
                        GlobalVariable.algorithmType = AlgorithmMark.AlgorithmType.DEVICE_RECOGNISE;
                        int now_threshold = targetModes.get(0).getTargetConfidence();
//                        Log.d("onTargetDetectingNew: ", ""+now_threshold);
                        if(native_threshold != now_threshold){
                            native_threshold = now_threshold;
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    viewBinding.textNativeThreshold.setText(String.valueOf(native_threshold));
                                }
                            });
                        }
                    }
                    if(modelID!=0){
                        // modelID
                        tempModelID=modelID;
                        updateModel(tempModelID);
                        updateSpinnerData(tempModelID);
                    }

//                    Log.d("savedID", "onTargetDetectingNew: "+ savedID);
                    // savedID
                    if(savedID!=0 && FlightActivity.this.savedID!=savedID){
                        FlightActivity.this.savedID = (int) savedID;
                        Log.d("savedID:success", "success"+ savedID);
                        int firstImageID = (int) savedID / 10000;
                        int tempSavedID = (int) savedID % 10000;
                        int secondImageID = tempSavedID / 100;
                        int thirdImageID = tempSavedID % 100;
                        runOnUiThread(() -> {
                            updatedPhotoList(firstImageID, secondImageID, thirdImageID);
                            updatedKnowledgeGraph(1,0,0);
                        });
                        runOnUiThread(() -> {
                            spinner.performClick();
                        });
                    }

                }
            };
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
        mGDUGimbal = (GDUGimbal) ((ourGDUAircraft) SdkDemoApplication.getProductInstance()).getGimbal();
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

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                spinner.setSelection(0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                spinner.setSelection(0);
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


    public void setPhotoShow(int temp){
        // 这部分需要修改，分屏模式下展示红外，但是为了保证不遮挡需要收回
        imageItems.clear();
        int i=lightSelectedView.get_irselected();
        if(i==1){
            //        if(displayMode1==SettingsDefinitions.DisplayMode.THERMAL_ONLY){
            imageItems.add(new ImageItem("images/00.jpg", object_labels.get(0)));
            imageItems.add(new ImageItem("images/01.jpg", object_labels.get(1)));
            imageItems.add(new ImageItem("images/02.jpg", object_labels.get(2)));
            imageItems.add(new ImageItem("images/03.jpg", object_labels.get(3)));
            imageItems.add(new ImageItem("images/04.jpg", object_labels.get(4)));
            imageItems.add(new ImageItem("images/05.jpg", object_labels.get(5)));
            imageItems.add(new ImageItem("images/06.jpg", object_labels.get(6)));
            imageItems.add(new ImageItem("images/07.jpg", object_labels.get(7)));
            Log.d("FlightActivity", "THERMAL模式 - 加载图片: " + imageItems +i);
        }else if(i==2){
//        }else if(displayMode1==SettingsDefinitions.DisplayMode.PIP){
            imageItems.add(new ImageItem("images/image1.png", object_labels.get(0)));
            imageItems.add(new ImageItem("images/image2.png", object_labels.get(1)));
            imageItems.add(new ImageItem("images/image3.png", object_labels.get(2)));
            imageItems.add(new ImageItem("images/image4.png", object_labels.get(3)));
            imageItems.add(new ImageItem("images/image5.png", object_labels.get(4)));
            imageItems.add(new ImageItem("images/image6.png", object_labels.get(5)));
            imageItems.add(new ImageItem("images/image7.png", object_labels.get(6)));
            imageItems.add(new ImageItem("images/image8.png", object_labels.get(7)));
            Log.d("FlightActivity", "可见光 - 加载图片: " + imageItems +i);
        }else{
    //        if(displayMode1==SettingsDefinitions.DisplayMode.THERMAL_ONLY){
            imageItems.add(new ImageItem("images/00.jpg", object_labels.get(0)));
            imageItems.add(new ImageItem("images/01.jpg", object_labels.get(1)));
            imageItems.add(new ImageItem("images/02.jpg", object_labels.get(2)));
            imageItems.add(new ImageItem("images/03.jpg", object_labels.get(3)));
            imageItems.add(new ImageItem("images/04.jpg", object_labels.get(4)));
            imageItems.add(new ImageItem("images/05.jpg", object_labels.get(5)));
            imageItems.add(new ImageItem("images/06.jpg", object_labels.get(6)));
            imageItems.add(new ImageItem("images/07.jpg", object_labels.get(7)));
            Log.d("FlightActivity", "分屏模式 - 加载图片: " + imageItems.toString()+i);
        }
        // 初始化 RecyclerView
        if(photoIsDialog){
            recyclerView = photoPopupView.findViewById(R.id.recyclerView);
        } else{
            recyclerView = findViewById(R.id.photo_recyclerView);
        }
        recyclerView.setLayoutManager(new GridLayoutManager(this, temp)); // 每排 temp 个
        adapter1 = new ImageAdapter(imageItems, this);
        recyclerView.setAdapter(adapter1);
    }

    @SuppressLint({"SetJavaScriptEnabled", "InflateParams"})
    public void initKnowledgeGraph(){
        // 加载弹出窗口布局
        View popupView = getLayoutInflater().inflate(R.layout.webview_popup, null);

        // 初始化WebView
        webView=popupView.findViewById(R.id.knowledge_graph_webView);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true); // 启用 JavaScript
        webSettings.setDomStorageEnabled(true); // 启用 DOM 存储
        webSettings.setAllowFileAccess(true); // 允许访问文件系统
        webSettings.setUseWideViewPort(true);  // 启用宽视口
        webSettings.setLoadWithOverviewMode(true);  // 缩放至屏幕宽度
        webSettings.setBuiltInZoomControls(true);  // 启用缩放控件
        webSettings.setDisplayZoomControls(false);  // 隐藏原生缩放按钮
        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        // 创建PopupWindow
        KnowledgeGraphPopupWindow = new PopupWindow(
                popupView,
                (int) (ScreenUtils.getScreenWidth(this) * 0.7),  // 屏幕宽度的占比
                (int) (ScreenUtils.getScreenHeight(this) * 0.6),
                true
        );
        // 设置关闭按钮
        Button closeButton = popupView.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> {
            if (KnowledgeGraphPopupWindow != null && KnowledgeGraphPopupWindow.isShowing()) {
                KnowledgeGraphPopupWindow.dismiss();
            }
        });

        KnowledgeGraphPopupWindow.setOutsideTouchable(true);
        KnowledgeGraphPopupWindow.setFocusable(true);
        // 设置背景和动画
        KnowledgeGraphPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    public Bitmap cropImage(TargetMode clickBox, byte[] yuvData) {

        if (yuvData != null) {
            Bitmap bitmap = mImageProcessingManager.convertYUVtoRGB(yuvData, codecManager.getVideoWidth(), codecManager.getVideoHeight());
//            Bitmap bitmap2 = Bitmap.createBitmap(bitmap, (int) (clickBox.getLeftX() ), (int) (clickBox.getLeftY() * 1200.0 / 1080.0), (int) (clickBox.getWidth()), (int) (clickBox.getHeight() * 1200.0 / 1080.0 ));
            Bitmap bitmap2 = Bitmap.createBitmap(bitmap, (int) (clickBox.getLeftX() ), (int) (clickBox.getLeftY()), (int) (clickBox.getWidth()), (int) (clickBox.getHeight()));
            bitmap.recycle();
            return bitmap2;
        }else {
            showToast("yuvData为空");
            return null;
        }
    }

    public void savedImage(TargetMode clickBox, byte[] yuvData,String temp,String source) {
        Bitmap bitmap2 = cropImage(clickBox, yuvData);
        if(bitmap2!=null){
            int savedNumber = storageManager.saveImage(bitmap2,temp);
            if (savedNumber > 0) {
                if(source.equals("attribute")){
                    lastSavedNumber = savedNumber;
                }
//                showToast("保存成功，编号: " + lastSavedNumber);
            }else{
                showToast("保存失败");
            }
            bitmap2.recycle();
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
//            if(thirdImageID==0){
//                thirdImageID = 1;
//            }
        String picture1=storageManager.getAbsolutePath(IMAGE_DIR,firstImageID);
//            String picture2=storageManager.getAbsolutePath(IMAGE_DIR,secondImageID);
//            String picture3=storageManager.getAbsolutePath(IMAGE_DIR,thirdImageID);
        List<ImageItem> newItems = new ArrayList<>();
//            storageManager.loadImageToView(lastSavedNumber, mYUVImageView);
        Log.d("picture1",""+firstImageID);
//            newItems.add(new ImageItem(picture1, object_labels.get(8)));
//            newItems.add(new ImageItem(picture2, object_labels.get(9)));
//            newItems.add(new ImageItem(picture3, object_labels.get(10)));
        int i=lightSelectedView.get_irselected();
        if(i==1) {
            newItems.add(new ImageItem("images/08.jpg", object_labels.get(8)));
        }else if(i==2){
            newItems.add(new ImageItem("images/image9.jpg", object_labels.get(8)));
        }else{
            newItems.add(new ImageItem("images/08.jpg", object_labels.get(8)));
        }
        GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
        assert layoutManager != null;
        adapter1.addNewItems(newItems, layoutManager);
        handler1.postDelayed(()->{
            recyclerView.smoothScrollToPosition(0);
        },500);
    }
    public void showNineGridShow(boolean show) {
        viewBinding.nightGridView.setVisibility(show ? View.VISIBLE : View.GONE);
    }


    public void beginCheckCloud() {
        showSuccess = false;
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

    // 这个函数是普宙写的为了方便测试playerView的
    //    @Override
    //    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    //        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    //        if (requestCode == 101) {
    //            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
    ////                mGduPlayView.beginRecord("/mnt/sdcard/gdu","ron.mp4");
    //            }
    //        }
    //    }

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
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (codecManager == null) {
            codecManager = new ourGDUCodecManager(FlightActivity.this, surface, width, height);
        }

        storageManager = new ImageStorageManager(this);
        mImageProcessingManager = new ImageProcessingManager(FlightActivity.this);


    }

    public void showAttributeWithBox(TargetMode clickBox){

        byte[] yuvData = codecManager.getYuvData();
        savedImage(clickBox,yuvData,LOAD_DIR,"attribute");

        ImageView instanceImageView = attributePopupView.findViewById(R.id.target_image);
        storageManager.loadImageToView(LOAD_DIR,lastSavedNumber,instanceImageView);
        storageManager.clearDirectory(LOAD_DIR);

        // 显示里面对应的各条属性，目前仅展示class
        Log.d("showAttributeWithBox:", "showAttributeWithBox: "+clickBox.getTargetType());
        String class_label = viewBinding.aiPaintView.getClassLabel().get(clickBox.getTargetType()%16);
        if(Objects.equals(class_label, "保存类别")){
            class_label="未知类别";
        }
        show(attributePopupView.findViewById(R.id.target_label_name), class_label);
        List<String> attributeList;
        attributeList = viewBinding.aiPaintView.getAttributeLabelNew();

        // 1. 将byte转换为8位二进制字符串（补前导零）
        String byteBinary = String.format("%8s", Integer.toBinaryString(clickBox.getTargetConfidence() & 0xFF))
                .replace(' ', '0');
        byteBinary = "";

        // 2. 将int转换为32位二进制字符串（补前导零）
        String intBinary = String.format("%32s", Integer.toBinaryString(clickBox.getId()))
                .replace(' ', '0');

        // 3. 拼接成40位二进制字符串
        String combined = intBinary + byteBinary;
        int len = attributeList.size();
//        Log.d("属性id",intBinary);

        StringBuilder result = new StringBuilder();
        for (int pos = 0; pos < len; pos++) {
            if (combined.charAt(pos) == '1') { // 从左到右对应高位到低位
                result.append(attributeList.get(pos)).append("\n");
            }
        }
//        showToast(result.toString());

        String attribute_labels = result.toString();
        show(attributePopupView.findViewById(R.id.target_attribute), attribute_labels);


        attributePopupWindow.showAtLocation(viewBinding.aiPaintView, Gravity.CENTER, 0, -20);
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

    private void showKnowledgeGraphPopup() {
        //popupWindow.setAnimationStyle(R.style.PopupAnimation);
        webView.loadUrl("file:///android_asset/templates/fenlei2.html");
        webView.addJavascriptInterface(new AndroidInterface(incState), "Android");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }
        });

        // 显示在屏幕中央
        KnowledgeGraphPopupWindow.showAtLocation(viewBinding.aiPaintView, Gravity.CENTER, 0, 0);
    }

    private void updatedKnowledgeGraph(int first_class_id, int second_class_id, int third_class_id){
        // 需要新类别的大类
        // 假设车是3，船是1，飞机是2
        // 按照船机车来配列新类名字，现在是1+1因此只考虑first_class_id
        // 把值和它对应的“原始下标”打包
        if(second_class_id != 0){
            int[] ids  = { first_class_id, second_class_id, third_class_id };
            Integer[] idx = {1, 2, 3 };  // 下标从 1 开始

            // 按对应的 id 值升序排序下标数组
            Arrays.sort(idx, Comparator.comparingInt(a -> ids[a - 1]));

            // 新类idx
            // 对于 2,1,3 的输入，会输出：2,1,3
            String cls_name_inc_txt = "\n新类"+idx[0]+"\n新类"+idx[1]+"\n新类"+idx[2];
            String clsName = readFileFromAssets("templates/cls_name_inc_base.txt");
            String inc_clsName = clsName + cls_name_inc_txt;
            saveTxtFile(mContext.getFilesDir().getAbsolutePath() + "templates/cls_name_inc.txt", inc_clsName);

            String cls2Fine_inc_txt = readFileFromAssets("templates/cls2fine_inc_base.txt");
            //        将其中的航母替换掉
            cls2Fine_inc_txt = cls2Fine_inc_txt.replace("航母 尼米兹级航空母舰 中途岛级航空母舰\n", "航母 尼米兹级航空母舰 中途岛级航空母舰 新类"+idx[0]+"\n");
            // 将其中的战机替换掉
            cls2Fine_inc_txt = cls2Fine_inc_txt.replace("战斗机\n", "战斗机 新类"+idx[1]+"\n");
            // 将其中的车替换掉
            cls2Fine_inc_txt = cls2Fine_inc_txt.replace("导弹发射车 BM-8-24\n", "导弹发射车 BM-8-24 新类"+idx[2]+"\n");
            saveTxtFile(mContext.getFilesDir().getAbsolutePath() + "templates/cls2fine_inc.txt", cls2Fine_inc_txt);
        }
        else {
            String cls_name_inc_txt = "\n新类1";
            String clsName = readFileFromAssets("templates/cls_name_inc_base.txt");
            String inc_clsName = clsName + cls_name_inc_txt;
            saveTxtFile(mContext.getFilesDir().getAbsolutePath() + "templates/cls_name_inc.txt", inc_clsName);

            String cls2Fine_inc_txt = readFileFromAssets("templates/cls2fine_inc_base.txt");
            //        将其中的航母替换掉
            cls2Fine_inc_txt = cls2Fine_inc_txt.replace("航母 尼米兹级航空母舰 中途岛级航空母舰\n", "航母 尼米兹级航空母舰 中途岛级航空母舰 新类1\n");
            saveTxtFile(mContext.getFilesDir().getAbsolutePath() + "templates/cls2fine_inc.txt", cls2Fine_inc_txt);
        }

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
//                Log.d("增量", ""+modelID);
                adapter.notifyDataSetChanged();
//                spinner.setSelection(0); // 重新设置选中位置
            }
        });
    }

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
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setRecognizingUI(){
        // 按钮
        quitAIRecognize.setEnabled(true);
        startIncremental.setEnabled(true);
        AIRecognize.setEnabled(false);
        AIRecognize.setImageResource(R.drawable.ai_recognize_selected);
        lightSelectedView.setButtonsEnabled(false);
        lightSelectedView.setButtonBackgroundColor(R.color.color_A8A8A8);

        // 后台状态
        codecManager.enabledYuvData(true);
        FlightActivity.this.savedID=-1;
//        viewBinding.aiPaintView.startBackgroundTask();

        //弹框
        if(attributePopupView == null){
            initAttributeDialog();
        }

        // 前台展示文字
        updateSpinnerData(0);
        show(aiState, "AI状态：未增量");
        findViewById(R.id.all_ai_state).setVisibility(View.VISIBLE);

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
                        showAttributeWithBox(clickBox);
                        v.performClick();
                        break;
                    case MotionEvent.ACTION_DOWN:
                        return true;
                }
                return true; // 返回true表示消费事件，后续事件继续传递
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setNormalUI(){
        // 按钮
        quitAIRecognize.setEnabled(false);
        startIncremental.setEnabled(false);
        AIRecognize.setEnabled(true);
        AIRecognize.setImageResource(R.drawable.ai_recognize);
        lightSelectedView.setButtonsEnabled(true);
        lightSelectedView.setButtonBackgroundColor(R.color.white);

        // 后台状态
        codecManager.enabledYuvData(false);
        storageManager.clearDirectory(IMAGE_DIR);
        FlightActivity.this.savedID=-1;
        isIncrementalMode=false;
//        //应该要加一个关闭AI绘制的后台线程
//        viewBinding.aiPaintView.stopBackgroundTask();

        // 前台展示文字
        updateSpinnerData(0);
        show(aiState, "AI状态：未增量");
        findViewById(R.id.all_ai_state).setVisibility(View.GONE);
        if(photoIsDialog){
            setPhotoShow(4);
        }else{
            setPhotoShow(1);
        }

        viewBinding.textureView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.performClick();
                return true;
            }
        });
    }



    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_msgBoxLabel:
                try {
                    if (msgData.isEmpty() || StringUtils.isEmptyString(viewBinding.tvMsgBoxNum.getText().toString())
                            || Integer.parseInt(viewBinding.tvMsgBoxNum.getText().toString().trim()) == 0) {
                        return;
                    }
                    showMsgBoxPopWindow(msgData);
                    viewBinding.ivMsgBoxLabel.setSelected(!viewBinding.ivMsgBoxLabel.isSelected());
                    break;
                }catch (Exception e){
                    showToast("检查程序是否初始化");
                }
            case R.id.ai_recognize_imageview:
                try {
                    setRecognizingUI();
                    // 开启检测
                    model_control.startAI(visual_listener, error -> {
                        if (error == null) {
                            showToast("识别开始");
                        } else {
                            showToast("开始识别失败");
                            setNormalUI();
                        }
                    });
                } catch (Exception e) {
                    showToast("程序开启识别失败");
                    setNormalUI();
                }
                break;
            case R.id.button_quit_ai:
                try{
                    setNormalUI();
                    model_control.closeAI(error -> {
                        if (error == null) {
                            showToast("退出识别");
                        } else {
                            showToast("退出识别失败");
                            setRecognizingUI();
                        }
                    });
                }catch(Exception e){
                    showToast("程序退出识别失败");
                    setRecognizingUI();
                }
                break;
            case R.id.button_gimbal_rotate:
                try{
                    mGDUGimbal = (GDUGimbal) ((ourGDUAircraft) SdkDemoApplication.getProductInstance()).getGimbal();
                    Rotation rotation = new Rotation();
                    rotation.setMode(RotationMode.ABSOLUTE_ANGLE);
                    rotation.setPitch(-90);
                    mGDUGimbal.rotate(rotation, error -> {
                        if (error == null) {
                            showToast("云台向下");
                        }
                    });
                    break;
                }catch (Exception e){
                    showToast("检查程序是否初始化");
                }
            case R.id.button_gimbal_reset:
                try{
                    mGDUGimbal = (GDUGimbal) ((ourGDUAircraft) SdkDemoApplication.getProductInstance()).getGimbal();
                    mGDUGimbal.reset(error -> {
                        if (error == null) {
                            showToast("云台回正");
                        }
                    });
                    break;
                }catch (Exception e){
                    showToast("检查程序是否初始化");
                }

            case R.id.button_start_incremental:
                if (mGduVision != null) {
                    int temp = latestModelID % 100000000;
                    int unknown_num = temp / 1000000;
                    if(unknown_num <3){
                        showToast("未知类别数目不足，请识别更多未知类别");
                        break;
                    }
                    model_control.startIncremental(error -> {
                        if (error == null) {
                            showToast("开始增量");
                            isIncrementalMode=true;
                        } else {
                            showToast("开始增量失败");
                            isIncrementalMode=false;
                        }
                    });
                } else {
                    showToast("请检查初始化是否成功");
                    isIncrementalMode=false;
                }
                break;
            case R.id.btn_take_off:
                mGDUFlightController.startLanding(error -> {
                    if (error == null) {
                        showToast("开始降落");
                    } else {
                        showToast("开始降落失败");
                    }
                });
                break;
            case R.id.btn_return_home:
                mGDUFlightController.startGoHome(error -> {
                    if (error == null) {
                        showToast("开始返航");
                    } else {
                        showToast("开始返航失败");
                    }
                });
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
                        v.performClick();
                        return false;
                    }
                });
                break;
            case R.id.btn_go_to_waypoint:
                if(getIntent().getStringExtra("source_class")!=null){
                    finish();
                }else {
                    Intent intent = new Intent(FlightActivity.this, WaypointMissionOperatorActivity.class);
                    intent.putExtra("source_class", FlightActivity.class.getName());
                    startActivity(intent);
                }
                break;
            case R.id.button_substract_threshold:
                try {
                    model_control.subOodThr(error -> {
                        if (error == null) {
                            showToast("算法阈值-1");
                        } else {
                            showToast("调整算法阈值失败");
                        }
                    });
                } catch(Exception e) {
                    showToast("请检查初始化是否成功");
                }
                break;
            case R.id.button_add_threshold:
                try {
                    model_control.addOodThr(error -> {
                        if (error == null) {
                            showToast("算法阈值+1");
                        } else {
                            showToast("调整算法阈值失败");
                        }
                    });
                } catch(Exception e) {
                    showToast("请检查初始化是否成功");
                }
                break;
        }
    }


    class AndroidInterface {
        private int incState = 0;

        public AndroidInterface(int incState){
            this.incState = incState;
        }
        @android.webkit.JavascriptInterface
        public void onJsReady() {
            if(incState==0){
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
                            Log.d("onJsReady", e.toString());
                        }
                    }
                });
            }
            else{
                runOnUiThread(new Runnable(){
                    @Override
                    public void run(){
                        try {
                            String coarseName = readFileFromAssets("templates/coarse_name_temp.txt");
                            String fineName = readFileFromAssets("templates/fine_name_inc.txt");
                            String clsName = readFileFromAssets("templates/cls_name_inc.txt");
                            String attName = readFileFromAssets("templates/att_name.txt");
                            String fine2Coarse = readFileFromAssets("templates/fine2coarse_inc.txt");
                            String cls2Fine = readFileFromAssets("templates/cls2fine_inc.txt");
                            String attribute = readFileFromAssets("templates/attribute_inc.txt");

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
                            Log.d("onJsReady", e.toString());
                        }
                    }
                });

            }

        }
        //filter know graph by detect target,need open fenlei2.html onChartReady,such 322
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
            int state = is.read(buffer);
            is.close();
            result = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Log.d("readFileFromAssets", e.toString());
        }
        return result;
    }

    public void saveTxtFile(String filePath, String content) {
        try {
            FileOutputStream fos = new FileOutputStream(filePath);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
            writer.write(content);
            writer.close();
            fos.close();
        } catch (IOException e) {
            Log.d("saveTxtFile", e.toString());
        }
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
        if(quitAIRecognize.isActivated()){
            model_control.closeAI(error -> {});
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

}

