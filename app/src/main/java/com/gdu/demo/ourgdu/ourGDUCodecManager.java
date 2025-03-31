package com.gdu.demo.ourgdu;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.graphics.Bitmap.CompressFormat;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import androidx.annotation.RequiresApi;
import com.gdu.beans.DecoderPkgBean;
import com.gdu.common.error.GDUError;
import com.gdu.config.GduConfig;
import com.gdu.config.GlobalVariable;
import com.gdu.drone.GimbalType;
import com.gdu.drone.PlanType;
import com.gdu.gdudecoder.H2642Mp4;
import com.gdu.sdk.camera.GDUCamera;
import com.gdu.sdk.camera.SystemState;
import com.gdu.sdk.codec.GduCodec;
import com.gdu.sdk.codec.GduYUVCodec;
import com.gdu.sdk.codec.ImageProcessingManager;
import com.gdu.sdk.manager.GDUSDKManager;
import com.gdu.sdk.products.GDUAircraft;
import com.gdu.sdk.util.CommonCallbacks;
import com.gdu.util.SpsPpsUtils;
import com.gdu.util.TimeUtil;
import com.gdu.util.logs.RonLog;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ourGDUCodecManager {
    private final String OUTPATH;
    private Context mContext;
    private int mCurrentWidth;
    private int mCurrentHeight;
    private SurfaceTexture mSurfaceTexture;
    private TextureView mTextureView;
    private SurfaceHolder mSurfaceHolder;
    private GduCodec mGduCodec;
    private GduCodec ourGduCodec;
    private GduYUVCodec mYUVCodec;
    private ImageProcessingManager mFastYUVtoRGB;
    private GDUCamera mCurrentCamera;
    private boolean mGimbalDecoderInitialized;
    private boolean isH265;
    private boolean isOnPause;
    private com.gdu.sdk.codec.GDUCodecManager.YuvDataCallback mYuvDataCallback;
    private SystemState.Callback mSystemStateCallback;
    private byte[] mCurrentYUVData;
    private boolean isVideoStoredLocal;
    private H2642Mp4 mH2642Mp4;
    private byte[] mCurrentSps;
    private byte[] mCurrentPps;
    private byte[] mCurrentVps;
    private boolean isPictureStoring;
    private boolean hasGetSpsPps;

    public ourGDUCodecManager(Context context, SurfaceTexture surfaceTexture, int width, int height) {
        this.OUTPATH = GduConfig.BaseDirectory + "/";
        this.mGimbalDecoderInitialized = false;
        this.hasGetSpsPps = false;
        this.mContext = context;
        this.mSurfaceTexture = surfaceTexture;
        this.mCurrentWidth = width;
        this.mCurrentHeight = height;
        this.init();
    }

    public ourGDUCodecManager(Context context, TextureView textureView, int width, int height) {
        this.OUTPATH = GduConfig.BaseDirectory + "/";
        this.mGimbalDecoderInitialized = false;
        this.hasGetSpsPps = false;
        this.mContext = context;
        this.mTextureView = textureView;
        if (textureView != null) {
            this.mSurfaceTexture = textureView.getSurfaceTexture();
        }

        this.mCurrentWidth = width;
        this.mCurrentHeight = height;
        this.init();
    }

    public void addSurface(SurfaceTexture addSurface){

        this.ourGduCodec = new GduCodec();
        this.ourGduCodec.setOnDecoderListener(new GduCodec.OnDecoderListener() {
            public void OnYUVGot(byte[] yuvData, int length) {
            }

            public void onDataGot(DecoderPkgBean decoderPkgBean) {
            }

            public void onParameterSetGot(List<byte[]> bytes, byte videoType) {
            }

            public void onParameterChanged(List<byte[]> bytes, byte videoType) {
            }
        });

        SpsPpsUtils spsPpsUtils = new SpsPpsUtils();
        RonLog.LogD(new String[]{"test decoder sCodingFormat " + GlobalVariable.sCodingFormat + " " + GlobalVariable.gimbalType});
        List<byte[]> sp = spsPpsUtils.getSpsAndPps();
        RonLog.LogD(new String[]{"test decoder getSpsAndPps " + sp.size()});
        int videoWidth = spsPpsUtils.getVideoW(GlobalVariable.ppsspsIndex);
        int videoHeight = spsPpsUtils.getVideoH(GlobalVariable.ppsspsIndex);
        Surface surface = new Surface(addSurface);
//        Surface oldsurface = new Surface(mSurfaceTexture);
        if (GlobalVariable.gimbalType != GimbalType.ByrdT_None_Zoom) {
            if (GlobalVariable.isInPlayBack) {
                if (GlobalVariable.sVideoPSList != null && this.hasGetSpsPps) {
                    Log.d("resetMediaCodec", "initCodec sVideoPSList = " + GlobalVariable.sVideoPSList.size());
                    if (GlobalVariable.sVideoPSList.size() == 2) {
                        this.ourGduCodec.init(videoWidth, videoHeight, (byte[])GlobalVariable.sVideoPSList.get(0), (byte[])GlobalVariable.sVideoPSList.get(1), (byte[])null, surface);
                        this.mGimbalDecoderInitialized = true;
                    } else if (GlobalVariable.sVideoPSList.size() == 3) {
                        this.ourGduCodec.init(videoWidth, videoHeight, (byte[])GlobalVariable.sVideoPSList.get(1), (byte[])GlobalVariable.sVideoPSList.get(2), (byte[])GlobalVariable.sVideoPSList.get(0), surface);
                        this.mGimbalDecoderInitialized = true;
                    }
                }
            } else {
                if (sp.size() == 3) {
                    //走的这个逻辑
                    this.ourGduCodec.init(videoWidth, videoHeight, (byte[])sp.get(1), (byte[])sp.get(2), (byte[])sp.get(0), surface);
                } else {
                    this.ourGduCodec.init(videoWidth, videoHeight, (byte[])sp.get(0), (byte[])sp.get(1), (byte[])null, surface);
                }

                this.mGimbalDecoderInitialized = true;
            }
        } else if (GlobalVariable.sPSDKCompId != 0 && GlobalVariable.sVideoPSList != null) {
            if (GlobalVariable.sVideoPSList.size() == 2) {
                this.ourGduCodec.init(videoWidth, videoHeight, (byte[])GlobalVariable.sVideoPSList.get(0), (byte[])GlobalVariable.sVideoPSList.get(1), (byte[])null, surface);
                this.mGimbalDecoderInitialized = true;
            } else if (GlobalVariable.sVideoPSList.size() == 3) {
                this.ourGduCodec.init(videoWidth, videoHeight, (byte[])GlobalVariable.sVideoPSList.get(1), (byte[])GlobalVariable.sVideoPSList.get(2), (byte[])GlobalVariable.sVideoPSList.get(0), surface);
                this.mGimbalDecoderInitialized = true;
            }
        }
    }



    private void init() {
        this.initCamera();
        this.mGduCodec = new GduCodec();
        this.mYUVCodec = new GduYUVCodec();
        this.mFastYUVtoRGB = new ImageProcessingManager(this.mContext);
        this.mGduCodec.setOnDecoderListener(new GduCodec.OnDecoderListener() {
            public void OnYUVGot(byte[] yuvData, int length) {
            }

            public void onDataGot(DecoderPkgBean decoderPkgBean) {
            }

            public void onParameterSetGot(List<byte[]> bytes, byte videoType) {
            }

            public void onParameterChanged(List<byte[]> bytes, byte videoType) {
            }
        });

        this.mYUVCodec.setOnDecoderListener(new GduYUVCodec.OnDecoderListener() {
            public void OnYUVGot(ByteBuffer yuvData, int length) {
                ourGDUCodecManager.this.mCurrentYUVData = new byte[length];
                yuvData.get(ourGDUCodecManager.this.mCurrentYUVData);
                if (ourGDUCodecManager.this.mYuvDataCallback != null) {
                    ourGDUCodecManager.this.mYuvDataCallback.onYuvDataReceived(ourGDUCodecManager.this.mCurrentYUVData, length, ourGDUCodecManager.this.mCurrentWidth, ourGDUCodecManager.this.mCurrentHeight);
                }

            }

            public void onDataGot(DecoderPkgBean decoderPkgBean) {
            }

            public void onParameterSetGot(List<byte[]> bytes, byte videoType) {
            }

            public void onParameterChanged(List<byte[]> bytes, byte videoType) {
            }
        });
    }

    private void initCamera() {
        GDUAircraft gduAircraft = (GDUAircraft)GDUSDKManager.getInstance().getProduct();
        this.mCurrentCamera = (GDUCamera)gduAircraft.getCamera();
        if (this.mCurrentCamera != null) {
            this.mCurrentCamera.setSystemStateCallback(new SystemState.Callback() {
                public void onUpdate(SystemState systemState) {
                    ourGDUCodecManager.this.dealPhotoOrVideo(systemState);
                }
            });
        }
    }

    private void dealPhotoOrVideo(SystemState systemState) {
    }

    private void dealPhoto() {
        final Bitmap bitmap = this.mTextureView.getBitmap();
        if (!this.isPictureStoring) {
            if (bitmap != null) {
                this.isPictureStoring = true;
                (new Thread(new Runnable() {
                    public void run() {
                        long time = System.currentTimeMillis();
                        ourGDUCodecManager.this.savePic(bitmap, ourGDUCodecManager.this.OUTPATH + GduConfig.ImageTempFileName, TimeUtil.getTime(time) + ".png");
                        ourGDUCodecManager.this.isPictureStoring = false;
                    }
                })).start();
            }

        }
    }

    public void startStoreMp4ToLocal(String path, String fileName) {
        String dir = path;
        File file3 = new File(dir);
        if (!file3.exists()) {
            file3.mkdirs();
        }

        if (this.mH2642Mp4 == null) {
            this.isVideoStoredLocal = true;
            this.mH2642Mp4 = new H2642Mp4();
            this.mH2642Mp4.setFileName(path, fileName);
            this.mH2642Mp4.start();
        }

    }

    public void stopStoreMp4ToLocal() {
        this.isVideoStoredLocal = false;
        if (this.mH2642Mp4 != null) {
            this.mH2642Mp4.stop();
            this.mH2642Mp4 = null;
        }

    }

    public void storageCurrentStreamToPicture(final String path, final String name, final CommonCallbacks.CompletionCallback callback) {
        File file = new File(path);
        if (!file.exists()) {
            callback.onResult(GDUError.COMMON_PARAM_ILLEGAL);
        } else if (this.mTextureView == null) {
            callback.onResult(GDUError.TEXTUREVIEW_NULL);
        } else if (this.isPictureStoring) {
            callback.onResult(GDUError.STORING);
        } else {
            final Bitmap bitmap = this.mTextureView.getBitmap();
            if (bitmap != null) {
                this.isPictureStoring = true;
                (new Thread(new Runnable() {
                    public void run() {
                        ourGDUCodecManager.this.savePic(bitmap, path, name);
                        callback.onResult((GDUError)null);
                        ourGDUCodecManager.this.isPictureStoring = false;
                    }
                })).start();
            }

        }
    }

    private void initCodec() {
        boolean decodeGimbal = GlobalVariable.gimbalType == GimbalType.ByrT_6k || GlobalVariable.gimbalType == GimbalType.GIMBAL_8KC || GlobalVariable.gimbalType == GimbalType.ByrdT_30X_Zoom_NEW || GlobalVariable.gimbalType == GimbalType.ByrT_IR_1K || GlobalVariable.gimbalType == GimbalType.Small_Double_Light || GlobalVariable.gimbalType == GimbalType.GIMBAL_FOUR_LIGHT || GlobalVariable.gimbalType == GimbalType.GIMBAL_FOUR_LIGHT_NEW || GlobalVariable.gimbalType == GimbalType.GIMBAL_MICRO_FOUR_LIGHT || GlobalVariable.gimbalType == GimbalType.GIMBAL_IR_1KG || GlobalVariable.gimbalType == GimbalType.GIMBAL_PTL600 || GlobalVariable.gimbalType == GimbalType.GIMBAL_PDL_10X || GlobalVariable.gimbalType == GimbalType.GIMBAL_PDL_S220 || GlobalVariable.gimbalType == GimbalType.GIMBAL_PDL_S200 || GlobalVariable.gimbalType == GimbalType.GIMBAL_PDL_S220PRO_FOUR_LIGHT || GlobalVariable.gimbalType == GimbalType.GIMBAL_PDL_S220PRO_SX_FOUR_LIGHT || GlobalVariable.gimbalType == GimbalType.GIMBAL_PDL_S220PRO_IR640_FOUR_LIGHT || GlobalVariable.gimbalType == GimbalType.GIMBAL_PDL_300C || GlobalVariable.gimbalType == GimbalType.GIMBAL_450J || GlobalVariable.sPSDKAssistantCompId != 0 || GlobalVariable.sPSDKCompId != 0;
        byte index;
        if (decodeGimbal) {
            if (GlobalVariable.sCodingFormat == 0) {
                if (GlobalVariable.sResolutionRate == 1) {
                    index = 103;
                } else {
                    index = 102;
                }

                this.isH265 = false;
            } else {
                this.isH265 = true;
                if (GlobalVariable.sResolutionRate == 1) {
                    index = 105;
                } else {
                    index = 104;
                }
            }
        } else {
            index = 2;
            this.isH265 = false;
            GlobalVariable.sCodingFormat = 0;
        }

        GlobalVariable.ppsspsIndex = index;
        if (this.mGduCodec != null && !this.mGimbalDecoderInitialized) {
            SpsPpsUtils spsPpsUtils = new SpsPpsUtils();
            RonLog.LogD(new String[]{"test decoder sCodingFormat " + GlobalVariable.sCodingFormat + " " + GlobalVariable.gimbalType});
            List<byte[]> sp = spsPpsUtils.getSpsAndPps();
            RonLog.LogD(new String[]{"test decoder getSpsAndPps " + sp.size()});
            int videoWidth = spsPpsUtils.getVideoW(index);
            int videoHeight = spsPpsUtils.getVideoH(index);
            Surface surface = new Surface(this.mSurfaceTexture);
            if (GlobalVariable.gimbalType != GimbalType.ByrdT_None_Zoom) {
                if (GlobalVariable.isInPlayBack) {
                    if (GlobalVariable.sVideoPSList != null && this.hasGetSpsPps) {
                        Log.d("resetMediaCodec", "initCodec sVideoPSList = " + GlobalVariable.sVideoPSList.size());
                        if (GlobalVariable.sVideoPSList.size() == 2) {
                            this.mGduCodec.init(videoWidth, videoHeight, (byte[])GlobalVariable.sVideoPSList.get(0), (byte[])GlobalVariable.sVideoPSList.get(1), (byte[])null, surface);
                            this.mGimbalDecoderInitialized = true;
                        } else if (GlobalVariable.sVideoPSList.size() == 3) {
                            this.mGduCodec.init(videoWidth, videoHeight, (byte[])GlobalVariable.sVideoPSList.get(1), (byte[])GlobalVariable.sVideoPSList.get(2), (byte[])GlobalVariable.sVideoPSList.get(0), surface);
                            this.mGimbalDecoderInitialized = true;
                        }
                    }
                } else {
                    if (sp.size() == 3) {
                        //走的这个逻辑
                        this.mGduCodec.init(videoWidth, videoHeight, (byte[])sp.get(1), (byte[])sp.get(2), (byte[])sp.get(0), surface);
                        this.mYUVCodec.init(videoWidth, videoHeight, (byte[])sp.get(1), (byte[])sp.get(2), (byte[])sp.get(0));
                    } else {
                        this.mGduCodec.init(videoWidth, videoHeight, (byte[])sp.get(0), (byte[])sp.get(1), (byte[])null, surface);
                        this.mYUVCodec.init(videoWidth, videoHeight, (byte[])sp.get(0), (byte[])sp.get(1), (byte[])null);
                    }

                    this.mGimbalDecoderInitialized = true;
                }
            } else if (GlobalVariable.sPSDKCompId != 0 && GlobalVariable.sVideoPSList != null) {
                if (GlobalVariable.sVideoPSList.size() == 2) {
                    this.mGduCodec.init(videoWidth, videoHeight, (byte[])GlobalVariable.sVideoPSList.get(0), (byte[])GlobalVariable.sVideoPSList.get(1), (byte[])null, surface);
                    this.mGimbalDecoderInitialized = true;
                } else if (GlobalVariable.sVideoPSList.size() == 3) {
                    this.mGduCodec.init(videoWidth, videoHeight, (byte[])GlobalVariable.sVideoPSList.get(1), (byte[])GlobalVariable.sVideoPSList.get(2), (byte[])GlobalVariable.sVideoPSList.get(0), surface);
                    this.mGimbalDecoderInitialized = true;
                }
            }
        }

    }

    private boolean isIFrame(byte[] data) {
        if (data == null) {
            return false;
        } else {
            int h264Value;
            if (this.isH265) {
                h264Value = (data[4] & 126) >> 1;
                switch (h264Value) {
                    case 19:
                    case 21:
                        return true;
                    default:
                        return false;
                }
            } else {
                h264Value = data[4] & 31;
                switch (h264Value) {
                    case 5:
                        return true;
                    default:
                        return false;
                }
            }
        }
    }

    private boolean isPFrame(byte[] data, int len) {
        if (data == null) {
            return false;
        } else {
            int h264Value;
            if (this.isH265) {
                h264Value = (data[4] & 126) >> 1;
                switch (h264Value) {
                    case 1:
                        return true;
                    default:
                        return false;
                }
            } else {
                h264Value = data[4] & 31;
                switch (h264Value) {
                    case 1:
                        return true;
                    default:
                        return false;
                }
            }
        }
    }

    public void sendDataToDecoder(byte[] datas, int len) {
        this.changeCodingFormat();
        byte[] data = new byte[len];
        System.arraycopy(datas, 0, data, 0, len);
        boolean isIFrame = this.isIFrame(data);
        boolean isPFrame = this.isPFrame(data, len);
        if (!isIFrame && !isPFrame) {
            this.getParams(data);
        }

        if (this.mGimbalDecoderInitialized) {
            if (!this.isOnPause && (isPFrame || isIFrame)) {
                if (this.mGduCodec == null) {
                    this.mGimbalDecoderInitialized = false;
                    return;
                }

                this.mGduCodec.addDataToDecoder(data, len, isIFrame);
                this.mYUVCodec.addDataToDecoder(data, len, isIFrame);
                this.sendDataToMp4(isIFrame, len, data);
                this.changeDroneType();
            }
        } else {
            this.initCodec();
        }

    }

    private void changeCodingFormat() {
        if (GlobalVariable.sCodingFormat == 0 && this.isH265) {
            this.isH265 = false;
            this.mCurrentSps = null;
            this.mCurrentPps = null;
        }

        if (GlobalVariable.sCodingFormat == 1 && !this.isH265) {
            this.isH265 = true;
            this.mCurrentSps = null;
            this.mCurrentPps = null;
            this.mCurrentVps = null;
        }

    }

    public void changeDroneType() {
        if ((GlobalVariable.gimbalType != GimbalType.ByrdT_None_Zoom || GlobalVariable.sPSDKCompId != 0) && GlobalVariable.planType != PlanType.O2Plan_Normal) {
            if (GlobalVariable.sCodingFormat == 1) {
                if (GlobalVariable.sRTPCodingFormatType == 2 || GlobalVariable.sRTPCodingFormatType == 0) {
                    this.reSetDecoderConfig();
                }
            } else if (GlobalVariable.sRTPCodingFormatType == 1 || GlobalVariable.sRTPCodingFormatType == 0) {
                this.reSetDecoderConfig();
            }

        }
    }

    public void reSetDecoderConfig() {
        boolean isSupport264_265Gimbal = (GlobalVariable.planType == PlanType.MGP12 || GlobalVariable.planType == PlanType.S480 || GlobalVariable.planType == PlanType.S450 || GlobalVariable.planType == PlanType.S220 || GlobalVariable.planType == PlanType.S280 || GlobalVariable.planType == PlanType.S200 || GlobalVariable.planType == PlanType.S220Pro || GlobalVariable.planType == PlanType.S220ProS || GlobalVariable.planType == PlanType.S220ProH || GlobalVariable.planType == PlanType.S220BDS || GlobalVariable.planType == PlanType.S280BDS || GlobalVariable.planType == PlanType.S200BDS || GlobalVariable.planType == PlanType.S220ProBDS || GlobalVariable.planType == PlanType.S220ProSBDS || GlobalVariable.planType == PlanType.S220ProHBDS) && (GlobalVariable.gimbalType == GimbalType.ByrT_6k || GlobalVariable.gimbalType == GimbalType.GIMBAL_8KC || GlobalVariable.gimbalType == GimbalType.ByrT_IR_1K || GlobalVariable.gimbalType == GimbalType.Small_Double_Light || GlobalVariable.gimbalType == GimbalType.GIMBAL_FOUR_LIGHT || GlobalVariable.gimbalType == GimbalType.GIMBAL_FOUR_LIGHT_NEW || GlobalVariable.gimbalType == GimbalType.GIMBAL_MICRO_FOUR_LIGHT || GlobalVariable.gimbalType == GimbalType.GIMBAL_IR_1KG || GlobalVariable.gimbalType == GimbalType.GIMBAL_PTL600 || GlobalVariable.gimbalType == GimbalType.GIMBAL_PDL_S220 || GlobalVariable.gimbalType == GimbalType.GIMBAL_PDL_S200 || GlobalVariable.gimbalType == GimbalType.GIMBAL_PDL_S220PRO_SX_FOUR_LIGHT || GlobalVariable.gimbalType == GimbalType.GIMBAL_PDL_S220PRO_IR640_FOUR_LIGHT || GlobalVariable.gimbalType == GimbalType.ByrdT_30X_Zoom_NEW || GlobalVariable.gimbalType == GimbalType.GIMBAL_PDL_10X || GlobalVariable.gimbalType == GimbalType.GIMBAL_PDL_300C || GlobalVariable.gimbalType == GimbalType.GIMBAL_450J || GlobalVariable.sPSDKAssistantCompId != 0 || GlobalVariable.sPSDKCompId != 0);
        byte index;
        if (isSupport264_265Gimbal) {
            if (GlobalVariable.sCodingFormat == 0) {
                index = 103;
            } else if (GlobalVariable.sResolutionRate == 1) {
                index = 105;
            } else {
                index = 104;
            }
        } else {
            index = 2;
            GlobalVariable.sCodingFormat = 0;
        }

        GlobalVariable.ppsspsIndex = index;
        SpsPpsUtils spsPpsUtils = new SpsPpsUtils();
        List<byte[]> sp = spsPpsUtils.getSpsAndPps();
        if (sp != null) {
            int width = spsPpsUtils.getVideoW(index);
            int height = spsPpsUtils.getVideoH(index);
            if (this.mGduCodec != null) {
                Log.d("reSetDecoderConfig", "sps = " + sp.size());
                if (sp.size() == 3) {
                    this.mGduCodec.reInitDecoder(width, height, (byte[])sp.get(1), (byte[])sp.get(2), (byte[])sp.get(0));
                } else {
                    this.mGduCodec.reInitDecoder(width, height, (byte[])sp.get(0), (byte[])sp.get(1), (byte[])null);
                }

            }
            if (this.ourGduCodec != null) {
                Log.d("reSetDecoderConfig", "sps = " + sp.size());
                if (sp.size() == 3) {
                    this.ourGduCodec.reInitDecoder(width, height, (byte[])sp.get(1), (byte[])sp.get(2), (byte[])sp.get(0));
                } else {
                    this.ourGduCodec.reInitDecoder(width, height, (byte[])sp.get(0), (byte[])sp.get(1), (byte[])null);
                }

            }
        }
    }

    private void getParams(byte[] data) {
        int h264Value;
        ArrayList ps;
        if (this.isH265) {
            h264Value = (data[4] & 126) >> 1;
            switch (h264Value) {
                case 32:
                    this.mCurrentVps = data;
                    if (this.mGduCodec != null) {
                        this.mGduCodec.setCurrentVps(data);
                    }
                    if (this.ourGduCodec != null) {
                        this.ourGduCodec.setCurrentVps(data);
                    }

                    if (this.mYUVCodec != null) {
                        this.mYUVCodec.setCurrentVps(data);
                    }

                    return;
                case 33:
                    this.mCurrentSps = data;
                    if (this.mGduCodec != null) {
                        this.mGduCodec.setCurrentSps(data);
                    }
                    if (this.ourGduCodec != null) {
                        this.ourGduCodec.setCurrentSps(data);
                    }

                    if (this.mYUVCodec != null) {
                        this.mYUVCodec.setCurrentSps(data);
                    }

                    return;
                case 34:
                    this.mCurrentPps = data;
                    if (this.mGduCodec != null) {
                        this.mGduCodec.setCurrentPps(data);
                    }
                    if (this.ourGduCodec != null) {
                        this.ourGduCodec.setCurrentPps(data);
                    }

                    if (this.mYUVCodec != null) {
                        this.mYUVCodec.setCurrentPps(data);
                    }

                    if (this.mCurrentVps != null && this.mCurrentSps != null && this.mCurrentPps != null) {
                        ps = new ArrayList();
                        ps.add(this.mCurrentVps);
                        ps.add(this.mCurrentSps);
                        ps.add(this.mCurrentPps);
                        GlobalVariable.sVideoPSList = ps;
                        if (GlobalVariable.isInPlayBack) {
                            this.hasGetSpsPps = true;
                        }

                        if (GlobalVariable.gimbalType == GimbalType.ByrT_IR_1K || GlobalVariable.gimbalType == GimbalType.ByrT_6k || GlobalVariable.gimbalType == GimbalType.GIMBAL_8KC || GlobalVariable.gimbalType == GimbalType.Small_Double_Light || GlobalVariable.gimbalType == GimbalType.GIMBAL_FOUR_LIGHT) {
                            if (this.mGduCodec != null && this.mGimbalDecoderInitialized) {
                                this.mGduCodec.resetMediaCodec();
                            }
                            if (this.ourGduCodec != null && this.mGimbalDecoderInitialized) {
                                this.ourGduCodec.resetMediaCodec();
                            }

                            if (this.mYUVCodec != null) {
                                this.mYUVCodec.resetMediaCodec();
                            }
                        }
                    }

                    return;
                default:
            }
        } else {
            h264Value = data[4] & 31;
            switch (h264Value) {
                case 7:
                    this.mCurrentSps = data;
                    if (this.mGduCodec != null) {
                        this.mGduCodec.setCurrentSps(data);
                    }
                    if (this.ourGduCodec != null) {
                        this.ourGduCodec.setCurrentSps(data);
                    }

                    if (this.mYUVCodec != null) {
                        this.mYUVCodec.setCurrentSps(data);
                    }

                    return;
                case 8:
                    this.mCurrentPps = data;
                    if (this.mGduCodec != null) {
                        this.mGduCodec.setCurrentPps(data);
                    }
                    if (this.ourGduCodec != null) {
                        this.ourGduCodec.setCurrentPps(data);
                    }

                    if (this.mYUVCodec != null) {
                        this.mYUVCodec.setCurrentPps(data);
                    }

                    if (this.mCurrentSps != null && this.mCurrentPps != null) {
                        ps = new ArrayList();
                        ps.add(this.mCurrentSps);
                        ps.add(this.mCurrentPps);
                        GlobalVariable.sVideoPSList = ps;
                        if (GlobalVariable.isInPlayBack) {
                            this.hasGetSpsPps = true;
                        }

                        if (GlobalVariable.gimbalType == GimbalType.ByrT_IR_1K || GlobalVariable.gimbalType == GimbalType.ByrT_6k || GlobalVariable.gimbalType == GimbalType.GIMBAL_8KC || GlobalVariable.gimbalType == GimbalType.Small_Double_Light || GlobalVariable.gimbalType == GimbalType.GIMBAL_FOUR_LIGHT) {
                            if (this.mGduCodec != null && this.mGimbalDecoderInitialized) {
                                this.mGduCodec.resetMediaCodec();
                            }
                            if (this.ourGduCodec != null && this.mGimbalDecoderInitialized) {
                                this.ourGduCodec.resetMediaCodec();
                            }

                            if (this.mYUVCodec != null) {
                                this.mYUVCodec.resetMediaCodec();
                            }
                        }
                    }

                    return;
                default:
            }
        }
    }

    private void sendDataToMp4(boolean isIFrame, int len, byte[] data) {
        if (this.isVideoStoredLocal && this.mH2642Mp4 != null) {
            DecoderPkgBean bean = new DecoderPkgBean();
            if (isIFrame) {
                bean.isI = true;
            } else {
                bean.isI = false;
            }

            bean.position = len;
            bean.data = data;
            this.mH2642Mp4.addData(bean);
        }

    }

    public void sendDataToDecoder(byte[] data, int len, MediaRecorder.VideoSource var3) {
    }

    public void sendDataToDecoder(byte[] data, int len, int var3) {
    }

    public void setYuvDataCallback(com.gdu.sdk.codec.GDUCodecManager.YuvDataCallback yuvDataCallback) {
        this.mYuvDataCallback = yuvDataCallback;
    }

    public com.gdu.sdk.codec.GDUCodecManager.YuvDataCallback getYuvDataCallback() {
        return this.mYuvDataCallback;
    }

    @RequiresApi(
            api = 23
    )
    public void enabledYuvData(Boolean aBoolean) {
        this.mYUVCodec.enabledYuvData(aBoolean);
    }

    public byte[] getYuvData() {
        return this.mCurrentYUVData;
    }

    public byte[] getRgbData() {
        if (this.mCurrentYUVData == null) {
            return null;
        } else {
            short width;
            short height;
            if (GlobalVariable.sResolutionRate == 1) {
                width = 1920;
                height = 1080;
            } else {
                width = 1280;
                height = 720;
            }

            Bitmap bitmap = this.mFastYUVtoRGB.convertYUVtoRGB(this.mCurrentYUVData, width, height);
            byte[] data = ImageProcessingManager.bitmap2RGB(bitmap);
            return data;
        }
    }

    public byte[] getRgbaData() {
        if (this.mCurrentYUVData == null) {
            return null;
        } else {
            short width;
            short height;
            if (GlobalVariable.sResolutionRate == 1) {
                width = 1920;
                height = 1080;
            } else {
                width = 1280;
                height = 720;
            }

            Bitmap bitmap = this.mFastYUVtoRGB.convertYUVtoRGB(this.mCurrentYUVData, width, height);
            byte[] data = ImageProcessingManager.bitmapToRgba(bitmap);
            return data;
        }
    }

    public void getBitmap(com.gdu.sdk.codec.GDUCodecManager.OnGetBitmapListener onGetBitmapListener) {
        if (this.mTextureView != null) {
            Bitmap bitmap = this.mTextureView.getBitmap();
            if (onGetBitmapListener != null) {
                onGetBitmapListener.onGetBitmap(bitmap);
            }
        }

    }

    public Integer getVideoWidth() {
        short videoWidth;
        if (GlobalVariable.sResolutionRate == 1) {
            videoWidth = 1920;
        } else {
            videoWidth = 1280;
        }

        return Integer.valueOf(videoWidth);
    }

    public Integer getVideoHeight() {
        short videoHeight;
        if (GlobalVariable.sResolutionRate == 1) {
            videoHeight = 1080;
        } else {
            videoHeight = 720;
        }

        return Integer.valueOf(videoHeight);
    }

    public void onResume() {
        RonLog.LogD(new String[]{"test GDUCodecManager onResume"});
        this.isOnPause = false;
        if (this.mGduCodec != null) {
            this.mGduCodec.onResume();
        }
        if (this.ourGduCodec != null) {
            this.ourGduCodec.onResume();
        }

        if (this.mYUVCodec != null) {
            this.mYUVCodec.onResume();
        }

    }

    public void onPause() {
        RonLog.LogD(new String[]{"test GDUCodecManager onPause"});
        this.isOnPause = true;
        if (this.mGduCodec != null) {
            this.mGduCodec.onPause();
        }
        if (this.ourGduCodec != null) {
            this.ourGduCodec.onPause();
        }

        if (this.mYUVCodec != null) {
            this.mYUVCodec.onPause();
        }

    }

    public void onDestroy() {
        RonLog.LogD(new String[]{"test GDUCodecManager onDestroy"});
        this.isOnPause = true;
        if (this.mGduCodec != null) {
            this.mGduCodec.onDestroy();
            this.mGduCodec = null;
        }
        if (this.ourGduCodec != null) {
            this.ourGduCodec.onDestroy();
            this.ourGduCodec = null;
        }

        if (this.mYUVCodec != null) {
            this.mYUVCodec.onDestroy();
            this.mYUVCodec = null;
        }

        if (this.mFastYUVtoRGB != null) {
            this.mFastYUVtoRGB.onDestroy();
            this.mFastYUVtoRGB = null;
        }

        if (this.mSurfaceTexture != null) {
            this.mSurfaceTexture = null;
        }

        if (this.mTextureView != null) {
            this.mTextureView = null;
        }

        if (this.mYuvDataCallback != null) {
            this.mYuvDataCallback = null;
        }

        if (this.mH2642Mp4 != null) {
            this.mH2642Mp4 = null;
        }

        this.hasGetSpsPps = false;
    }

    private void savePic(Bitmap bitmap, String path, String name) {
        if (bitmap != null) {
            File file3 = new File(path);
            if (!file3.exists()) {
                file3.mkdirs();
            }

            try {
                String destPath = path + name;
                FileOutputStream fileOutputStream = new FileOutputStream(destPath);
                bitmap.compress(CompressFormat.PNG, 99, fileOutputStream);
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch (Exception var7) {
                Exception e = var7;
                e.printStackTrace();
            }

        }
    }

    private void printData(String type, byte[] datas) {
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < datas.length; ++i) {
            sb.append(Integer.toHexString(datas[i] & 255)).append(",");
        }

        Log.d(type, sb.toString());
    }

    public interface YuvDataCallback {
        void onYuvDataReceived(byte[] var1, int var2, int var3, int var4);
    }

    public interface OnGetBitmapListener {
        void onGetBitmap(Bitmap var1);
    }

    public interface OnStorePictureListener {
        void onStorePictureStart();

        void onStorePictureFinished();
    }

    public interface OnVideoSizeChangedListener {
        void onVideoSizeChanged(int var1, int var2);
    }
}

