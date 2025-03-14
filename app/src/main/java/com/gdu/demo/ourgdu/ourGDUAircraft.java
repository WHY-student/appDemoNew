package com.gdu.demo.ourgdu;


import android.content.Context;
import android.text.TextUtils;

import com.gdu.common.error.GDUError;
import com.gdu.config.ConnStateEnum;
import com.gdu.config.GlobalVariable;
import com.gdu.drone.BinocularInfo;
import com.gdu.drone.GimbalType;
import com.gdu.sdk.airlink.GDUAirLink;
import com.gdu.sdk.base.BaseComponent;
import com.gdu.sdk.base.BaseProduct;
import com.gdu.sdk.base.GDUDiagnostics;
import com.gdu.sdk.battery.GDUBattery;
import com.gdu.sdk.camera.Camera30X;
import com.gdu.sdk.camera.Camera8K;
import com.gdu.sdk.camera.CameraPDL1K;
import com.gdu.sdk.camera.CameraPDL300g;
import com.gdu.sdk.camera.CameraPFL;
import com.gdu.sdk.camera.CameraPQL02;
import com.gdu.sdk.camera.CameraS200;
import com.gdu.sdk.camera.CameraS220Pro;
import com.gdu.sdk.camera.CameraS220ProXE;
import com.gdu.sdk.camera.GDUCamera;
import com.gdu.sdk.camera.PsdkCamera;
import com.gdu.sdk.custommsg.GDUCustomMsg;
import com.gdu.sdk.flightcontroller.GDUFlightController;
import com.gdu.sdk.gimbal.GDUGimbal;
import com.gdu.sdk.gimbal.Gimbal8K;
import com.gdu.sdk.gimbal.GimbalPDL1K;
import com.gdu.sdk.gimbal.GimbalPFL;
import com.gdu.sdk.gimbal.GimbalPQL02;
import com.gdu.sdk.gimbal.GimbalS200;
import com.gdu.sdk.gimbal.GimbalSmallDoubleLight;
import com.gdu.sdk.lte.GDULTE;
import com.gdu.sdk.noflyzone.GDUNoFlyZone;
import com.gdu.sdk.psdk.Megaphone;
import com.gdu.sdk.radar.GDURadar;
import com.gdu.sdk.remotecontroller.GDURemoteController;
import com.gdu.sdk.util.CommonCallbacks;
import com.gdu.sdk.util.CommonUtils;
import com.gdu.socket.GduFrame3;
import com.gdu.socket.GduSocketManager;
import com.gdu.socket.SocketCallBack3;
import com.gdu.util.ByteUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/* loaded from: GduLibrary-2.0.64.aar:classes.jar:com/gdu/sdk/products/GDUAircraft.class */
public class ourGDUAircraft extends BaseProduct {
    private Context mContext;
    private GDUFlightController mFlightController;
    private GDUGimbal mGDUGimbal;
    private GDUCamera mGDUCamera;
    private GDUBattery mBattery;
    private GDURadar mRadar;
    private GDURemoteController mGDURemoteController;
    private GDUAirLink mAirLink;
    private ourGDUVision vision;
    private GDULTE gduLTE;
    private GDUCustomMsg customMsg;
    private GDUNoFlyZone noFlyZone;
    private Megaphone megaphone;
    private GDUDiagnostics.DiagnosticsInformationCallback mDiagnosticsInformationCallback;
    private Timer mCycleTimer;

    public ourGDUAircraft() {
        initTimer();
        this.noFlyZone = new GDUNoFlyZone();
    }

    public void initContext(Context context) {
        this.mContext = context;
    }

    private void initTimer() {
        this.mCycleTimer = new Timer();
        this.mCycleTimer.schedule(new TimerTask() { // from class: com.gdu.sdk.products.GDUAircraft.1
            @Override // java.util.TimerTask, java.lang.Runnable
            public void run() {
                List<GDUDiagnostics> flightHealthStatusBeans = ourGDUAircraft.this.getStatusData();
                if (flightHealthStatusBeans != null && flightHealthStatusBeans.size() > 0 && ourGDUAircraft.this.mDiagnosticsInformationCallback != null) {
                    ourGDUAircraft.this.mDiagnosticsInformationCallback.onUpdate(flightHealthStatusBeans);
                }
            }
        }, 1000L, 1000L);
    }

    public GDUFlightController getFlightController() {
        if (this.mFlightController == null) {
            this.mFlightController = new GDUFlightController();
        }
        return this.mFlightController;
    }

    public GDUBattery getBattery() {
        if (this.mBattery == null) {
            this.mBattery = new GDUBattery();
        }
        return this.mBattery;
    }

    public GDURemoteController getRemoteController() {
        if (this.mGDURemoteController == null) {
            this.mGDURemoteController = new GDURemoteController();
        }
        return this.mGDURemoteController;
    }

    public GDULTE getLTE() {
        if (this.gduLTE == null) {
            this.gduLTE = new GDULTE(this.mContext);
        }
        return this.gduLTE;
    }

    private GDUNoFlyZone getNoFlyZone() {
        if (this.noFlyZone == null) {
            this.noFlyZone = new GDUNoFlyZone();
        }
        return this.noFlyZone;
    }

    public Megaphone getMegaphone() {
        if (this.megaphone == null) {
            this.megaphone = new Megaphone();
        }
        return this.megaphone;
    }

    public GDUCustomMsg getCustomMsg() {
        if (this.customMsg == null) {
            this.customMsg = new GDUCustomMsg();
        }
        return this.customMsg;
    }

    public BaseComponent getGimbal() {
        BaseComponent component = null;
        switch (GlobalVariable.gimbalType) {
            case ByrT_6k:
                component = new Gimbal8K();
                this.mGDUGimbal = (Gimbal8K) component;
                break;
            case GIMBAL_8KC:
                component = new Gimbal8K();
                this.mGDUGimbal = (Gimbal8K) component;
                break;
            case ByrT_IR_1K:
                component = new GimbalPDL1K();
                this.mGDUGimbal = (GimbalPDL1K) component;
                break;
            case GIMBAL_FOUR_LIGHT:
                component = new GimbalPFL();
                break;
            case Small_Double_Light:
                component = new GimbalSmallDoubleLight();
                break;
            case GIMBAL_PDL_S220:
                component = new GimbalS200();
                break;
            case GIMBAL_MICRO_FOUR_LIGHT:
                component = new GimbalPQL02();
                break;
        }
        return component;
    }

    public BaseComponent getCamera() {
        BaseComponent component = null;
        if (GlobalVariable.gimbalType != GimbalType.ByrdT_None_Zoom) {
            switch (GlobalVariable.gimbalType) {
                case ByrT_6k:
                case GIMBAL_8KC:
                    if (this.mGDUCamera != null && (this.mGDUCamera instanceof Camera8K)) {
                        component = this.mGDUCamera;
                    } else {
                        component = new Camera8K();
                        this.mGDUCamera = (Camera8K) component;
                    }
                    break;
                case ByrT_IR_1K:
                    if (this.mGDUCamera != null && (this.mGDUCamera instanceof CameraPDL1K)) {
                        component = this.mGDUCamera;
                    } else {
                        component = new CameraPDL1K();
                        this.mGDUCamera = (CameraPDL1K) component;
                    }
                    break;
                case GIMBAL_FOUR_LIGHT:
                    if (this.mGDUCamera != null && (this.mGDUCamera instanceof CameraPFL)) {
                        component = this.mGDUCamera;
                        break;
                    } else {
                        component = new CameraPFL();
                        this.mGDUCamera = (CameraPFL) component;
                        break;
                    }
                case Small_Double_Light:
                case GIMBAL_PDL_300C:
                    if (this.mGDUCamera != null && (this.mGDUCamera instanceof CameraPDL300g)) {
                        component = this.mGDUCamera;
                    } else {
                        component = new CameraPDL300g();
                        this.mGDUCamera = (CameraPDL300g) component;
                    }
                    break;
                case GIMBAL_PDL_S220:
                    if (this.mGDUCamera != null && (this.mGDUCamera instanceof CameraS200)) {
                        component = this.mGDUCamera;
                    } else {
                        component = new CameraS200();
                        this.mGDUCamera = (CameraS200) component;
                    }
                    break;
                case GIMBAL_MICRO_FOUR_LIGHT:
                    if (this.mGDUCamera != null && (this.mGDUCamera instanceof CameraPQL02)) {
                        component = this.mGDUCamera;
                    } else {
                        component = new CameraPQL02();
                        this.mGDUCamera = (CameraPQL02) component;
                    }
                    break;
                case ByrdT_30X_Zoom:
                    if (this.mGDUCamera != null && (this.mGDUCamera instanceof Camera30X)) {
                        component = this.mGDUCamera;
                    } else {
                        component = new Camera30X();
                        this.mGDUCamera = (Camera30X) component;
                    }
                    break;
                case GIMBAL_PDL_S220PRO_FOUR_LIGHT:
                    if (this.mGDUCamera != null && (this.mGDUCamera instanceof CameraS220Pro)) {
                        component = this.mGDUCamera;
                        break;
                    } else {
                        component = new CameraS220Pro();
                        this.mGDUCamera = (CameraS220Pro) component;
                        break;
                    }
                case GIMBAL_PDL_S220PRO_SX_FOUR_LIGHT:
                    if (this.mGDUCamera != null && (this.mGDUCamera instanceof CameraS220ProXE)) {
                        component = this.mGDUCamera;
                    } else {
                        component = new CameraS220ProXE();
                        this.mGDUCamera = (CameraS220ProXE) component;
                    }
                    break;
            }
        } else if (GlobalVariable.sPSDKCompId > 0) {
            if (this.mGDUCamera != null && (this.mGDUCamera instanceof PsdkCamera)) {
                component = this.mGDUCamera;
            } else {
                component = new PsdkCamera();
                this.mGDUCamera = (PsdkCamera) component;
            }
        }
        return component;
    }

    public List<GDUCamera> getCameras() {
        return this.mCameraList;
    }

    public List<GDUGimbal> getGimbals() {
        return this.mGimbalList;
    }

    public BaseComponent getRadar() {
        if (this.mRadar == null) {
            this.mRadar = new GDURadar();
        }
        return this.mRadar;
    }

    public GDUAirLink getAirLink() {
        if (this.mAirLink == null) {
            this.mAirLink = new GDUAirLink();
        }
        return this.mAirLink;
    }

    public ourGDUVision getGduVision() {
        if (this.vision == null) {
            this.vision = new ourGDUVision();
        }
        return this.vision;
    }

    @Override // com.gdu.sdk.base.BaseProduct
    public void getProductSN(final CommonCallbacks.CompletionCallbackWith<String> callback) {
        if (TextUtils.isEmpty(GlobalVariable.SN)) {
            GduSocketManager.getInstance().getGduCommunication().getUnique(new SocketCallBack3() { // from class: com.gdu.sdk.products.GDUAircraft.2
                @Override // com.gdu.socket.SocketCallBack3
                public void callBack(int code, GduFrame3 bean) {
                    byte[] snByte;
                    if (code == 0 && bean != null && bean.frameContent != null && bean.frameContent.length >= 17) {
                        byte[] tempFrame = bean.frameContent;
                        if (tempFrame.length > 19) {
                            snByte = new byte[20];
                        } else {
                            snByte = new byte[17];
                        }
                        System.arraycopy(bean.frameContent, 2, snByte, 0, tempFrame.length - 2);
                        String snStr = new String(snByte);
                        if (!snStr.contains("GDU") && snStr.length() == 20) {
                            snStr = snStr.substring(0, 17);
                        }
                        GlobalVariable.SN = snStr;
                        if (callback != null) {
                            callback.onSuccess(snStr);
                        }
                    } else if (callback != null) {
                        callback.onFailure(GDUError.TIMEOUT);
                    }
                }
            });
        } else if (callback != null) {
            callback.onSuccess(GlobalVariable.SN);
        }
    }

    @Override // com.gdu.sdk.base.BaseProduct
    public Model getModel() {
        Model model = Model.UNKNOWN_AIRCRAFT;
        switch (GlobalVariable.planType) {
            case S200:
            case S200BDS:
                model = Model.S200;
                break;
            case S220:
            case S220BDS:
            case S220_SD:
            case S220_SD_BDS:
                model = Model.S220;
                break;
            case S280:
            case S280BDS:
                model = Model.S280;
                break;
            case S220Pro:
            case S220ProS:
            case S220ProH:
            case S220ProBDS:
            case S220ProSBDS:
            case S220ProHBDS:
                model = Model.S220_PRO;
                break;
            case MGP12:
            case S480:
                model = Model.S400;
                break;
        }
        return model;
    }

    @Override // com.gdu.sdk.base.BaseProduct
    public void setDiagnosticsInformationCallback(GDUDiagnostics.DiagnosticsInformationCallback diagnosticsInformationCallback) {
        this.mDiagnosticsInformationCallback = diagnosticsInformationCallback;
    }

    @Override // com.gdu.sdk.base.BaseProduct
    public boolean isConnected() {
        if (GlobalVariable.connStateEnum == ConnStateEnum.Conn_Sucess) {
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public List<GDUDiagnostics> getStatusData() {
        if (GlobalVariable.connStateEnum != ConnStateEnum.Conn_Sucess) {
            return null;
        }
        List<GDUDiagnostics> gduDiagnostics = new ArrayList<>();
        if (GlobalVariable.imuAbnormal != 0 || GlobalVariable.sIMUNotCalibration != 0 || GlobalVariable.sIMURestartLabel == 1) {
            switch (GlobalVariable.imuAbnormal) {
                case 1:
                    GDUDiagnostics diagnostics1 = new GDUDiagnostics();
                    diagnostics1.setType(GDUDiagnostics.GDUDiagnosticsType.DEVICE_HEALTH_INFORMATION);
                    diagnostics1.setReason("IMU通信异常");
                    gduDiagnostics.add(diagnostics1);
                    break;
                case 2:
                    GDUDiagnostics diagnostics2 = new GDUDiagnostics();
                    diagnostics2.setType(GDUDiagnostics.GDUDiagnosticsType.DEVICE_HEALTH_INFORMATION);
                    diagnostics2.setReason("IMU数据异常");
                    gduDiagnostics.add(diagnostics2);
                    break;
            }
            if (GlobalVariable.sIMUNotCalibration != 0) {
                GDUDiagnostics diagnostics3 = new GDUDiagnostics();
                diagnostics3.setType(GDUDiagnostics.GDUDiagnosticsType.DEVICE_HEALTH_INFORMATION);
                diagnostics3.setReason("IMU未校准");
                gduDiagnostics.add(diagnostics3);
            }
            if (GlobalVariable.sIMURestartLabel == 1) {
                GDUDiagnostics diagnostics4 = new GDUDiagnostics();
                diagnostics4.setType(GDUDiagnostics.GDUDiagnosticsType.DEVICE_HEALTH_INFORMATION);
                diagnostics4.setReason("IMU未重启");
                gduDiagnostics.add(diagnostics4);
            }
            GDUDiagnostics diagnostics5 = new GDUDiagnostics();
            diagnostics5.setType(GDUDiagnostics.GDUDiagnosticsType.DEVICE_HEALTH_INFORMATION);
            diagnostics5.setReason("陀螺仪零偏");
            gduDiagnostics.add(diagnostics5);
        }
        if (GlobalVariable.gpsAbnormal == 1) {
            GDUDiagnostics diagnostics6 = new GDUDiagnostics();
            diagnostics6.setType(GDUDiagnostics.GDUDiagnosticsType.DEVICE_HEALTH_INFORMATION);
            diagnostics6.setReason("GPS通信异常");
            gduDiagnostics.add(diagnostics6);
        } else if (GlobalVariable.gpsAbnormal == 2) {
            GDUDiagnostics diagnostics62 = new GDUDiagnostics();
            diagnostics62.setType(GDUDiagnostics.GDUDiagnosticsType.DEVICE_HEALTH_INFORMATION);
            diagnostics62.setReason("GPS数据异常");
            gduDiagnostics.add(diagnostics62);
        }
        if (GlobalVariable.glassAbnormal != 0) {
            if (GlobalVariable.glassAbnormal == 1) {
                GDUDiagnostics diagnostics61 = new GDUDiagnostics();
                diagnostics61.setType(GDUDiagnostics.GDUDiagnosticsType.DEVICE_HEALTH_INFORMATION);
                diagnostics61.setReason("气压计通信异常");
                gduDiagnostics.add(diagnostics61);
            } else if (GlobalVariable.glassAbnormal == 2) {
                GDUDiagnostics diagnostics7 = new GDUDiagnostics();
                diagnostics7.setType(GDUDiagnostics.GDUDiagnosticsType.DEVICE_HEALTH_INFORMATION);
                diagnostics7.setReason("气压计数据异常");
                gduDiagnostics.add(diagnostics7);
            }
        }
        List<GDUDiagnostics> magneticDiagnostics = CommonUtils.getMagneticDiagnosticsList(this.mContext);
        gduDiagnostics.addAll(magneticDiagnostics);
        List<GDUDiagnostics> binocularWarList = parseBinocularStatus();
        gduDiagnostics.addAll(binocularWarList);
        if (GlobalVariable.gimbalType != GimbalType.ByrdT_None_Zoom) {
            byte cameraConnectStatus = (byte) ByteUtils.getBits(GlobalVariable.HolderIsErr, 0, 4);
            byte gimbalStatus = (byte) ByteUtils.getBits(GlobalVariable.HolderIsErr, 4, 4);
            if (cameraConnectStatus == 1) {
                GDUDiagnostics diagnostics8 = new GDUDiagnostics();
                diagnostics8.setType(GDUDiagnostics.GDUDiagnosticsType.GIMBAL);
                diagnostics8.setReason("相机连接异常");
                gduDiagnostics.add(diagnostics8);
            }
            if (gimbalStatus == 1) {
                GDUDiagnostics diagnostics9 = new GDUDiagnostics();
                diagnostics9.setType(GDUDiagnostics.GDUDiagnosticsType.GIMBAL);
                diagnostics9.setReason("云台堵转");
                gduDiagnostics.add(diagnostics9);
            } else if (gimbalStatus == 2) {
                GDUDiagnostics diagnostics10 = new GDUDiagnostics();
                diagnostics10.setType(GDUDiagnostics.GDUDiagnosticsType.GIMBAL);
                diagnostics10.setReason("云台自检异常");
                gduDiagnostics.add(diagnostics10);
            }
        }
        List<GDUDiagnostics> batteryDiagnostics1 = CommonUtils.getBatteryAlarmList(this.mContext);
        gduDiagnostics.addAll(batteryDiagnostics1);
        return gduDiagnostics;
    }

    private List<GDUDiagnostics> parseBinocularStatus() {
        List<GDUDiagnostics> warList = new ArrayList<>();
        if (GlobalVariable.eyesAbnormal == 1) {
            GDUDiagnostics diagnostics1 = new GDUDiagnostics();
            diagnostics1.setType(GDUDiagnostics.GDUDiagnosticsType.VISION);
            diagnostics1.setReason("双目通信异常");
            warList.add(diagnostics1);
        } else if (GlobalVariable.eyesAbnormal == 2) {
            GDUDiagnostics diagnostics2 = new GDUDiagnostics();
            diagnostics2.setType(GDUDiagnostics.GDUDiagnosticsType.VISION);
            diagnostics2.setReason("双目数据异常");
            warList.add(diagnostics2);
        }
        List<GDUDiagnostics> radarWarList = parseRadarStatus();
        warList.addAll(radarWarList);
        boolean visionAlarm = GlobalVariable.sBinocularInfo != null && GlobalVariable.sBinocularInfo.hasAbnormal();
        if (!visionAlarm) {
            return warList;
        }
        if (BinocularInfo.staticBackBinocular == 1) {
            GDUDiagnostics diagnostics3 = new GDUDiagnostics();
            diagnostics3.setType(GDUDiagnostics.GDUDiagnosticsType.VISION);
            diagnostics3.setReason("后视双目硬件异常");
            warList.add(diagnostics3);
        } else if (BinocularInfo.staticBackBinocular == 2) {
            GDUDiagnostics diagnostics4 = new GDUDiagnostics();
            diagnostics4.setType(GDUDiagnostics.GDUDiagnosticsType.VISION);
            diagnostics4.setReason("后视环境光照度低");
            warList.add(diagnostics4);
        }
        if (GlobalVariable.sBinocularInfo.forwardBinocular == 1) {
            GDUDiagnostics diagnostics5 = new GDUDiagnostics();
            diagnostics5.setType(GDUDiagnostics.GDUDiagnosticsType.VISION);
            diagnostics5.setReason("前视双目硬件异常");
            warList.add(diagnostics5);
        } else if (GlobalVariable.sBinocularInfo.forwardBinocular == 2) {
            GDUDiagnostics diagnostics6 = new GDUDiagnostics();
            diagnostics6.setType(GDUDiagnostics.GDUDiagnosticsType.VISION);
            diagnostics6.setReason("前视环境光照度低");
            warList.add(diagnostics6);
        }
        if (GlobalVariable.sBinocularInfo.forwardBinocularCalibrationParam != 0) {
            GDUDiagnostics diagnostics7 = new GDUDiagnostics();
            diagnostics7.setType(GDUDiagnostics.GDUDiagnosticsType.VISION);
            diagnostics7.setReason("无前视标定参数");
            warList.add(diagnostics7);
        }
        if (GlobalVariable.sBinocularInfo.backBinocularCalibrationParam != 0) {
            GDUDiagnostics diagnostics8 = new GDUDiagnostics();
            diagnostics8.setType(GDUDiagnostics.GDUDiagnosticsType.VISION);
            diagnostics8.setReason("无后视标定参数");
            warList.add(diagnostics8);
        }
        if (GlobalVariable.sBinocularInfo.downMono == 1) {
            GDUDiagnostics diagnostics9 = new GDUDiagnostics();
            diagnostics9.setType(GDUDiagnostics.GDUDiagnosticsType.VISION);
            diagnostics9.setReason("下视单目硬件异常");
            warList.add(diagnostics9);
        } else if (GlobalVariable.sBinocularInfo.downMono == 2) {
            GDUDiagnostics diagnostics10 = new GDUDiagnostics();
            diagnostics10.setType(GDUDiagnostics.GDUDiagnosticsType.VISION);
            diagnostics10.setReason("下视环境光照度低");
            warList.add(diagnostics10);
        }
        if (GlobalVariable.sBinocularInfo.downMonoCalibration != 0) {
            GDUDiagnostics diagnostics11 = new GDUDiagnostics();
            diagnostics11.setType(GDUDiagnostics.GDUDiagnosticsType.VISION);
            diagnostics11.setReason("无下视标定参数");
            warList.add(diagnostics11);
        }
        return warList;
    }

    private List<GDUDiagnostics> parseRadarStatus() {
        List<GDUDiagnostics> warList = new ArrayList<>();
        if (GlobalVariable.sRadarInfo != null) {
            StringBuilder radarAbnormalSb = new StringBuilder();
            radarAbnormalSb.append("雷达异常");
            boolean hasRadarAbnormal = false;
            if (GlobalVariable.sRadarInfo.forwardRadar < 0) {
                hasRadarAbnormal = true;
                radarAbnormalSb.append(":");
                radarAbnormalSb.append("前视");
            }
            if (GlobalVariable.sRadarInfo.leftRadar < 0) {
                if (hasRadarAbnormal) {
                    radarAbnormalSb.append(",");
                } else {
                    radarAbnormalSb.append(":");
                }
                hasRadarAbnormal = true;
                radarAbnormalSb.append("左视");
            }
            if (GlobalVariable.sRadarInfo.rightRadar < 0) {
                if (hasRadarAbnormal) {
                    radarAbnormalSb.append(",");
                } else {
                    radarAbnormalSb.append(":");
                }
                hasRadarAbnormal = true;
                radarAbnormalSb.append("右视");
            }
            if (GlobalVariable.sRadarInfo.downLocateHeightRadar < 0) {
                if (hasRadarAbnormal) {
                    radarAbnormalSb.append(",");
                } else {
                    radarAbnormalSb.append(":");
                }
                hasRadarAbnormal = true;
                radarAbnormalSb.append("下视定高");
            }
            if (hasRadarAbnormal) {
                GDUDiagnostics diagnostics1 = new GDUDiagnostics();
                diagnostics1.setType(GDUDiagnostics.GDUDiagnosticsType.RADAR);
                diagnostics1.setReason(radarAbnormalSb.toString());
                warList.add(diagnostics1);
            }
            boolean hasTOFAbnormal = false;
            StringBuilder TOFAbnormalSb = new StringBuilder();
            TOFAbnormalSb.append("TOF异常");
            if (GlobalVariable.sRadarInfo.backTOF < 0) {
                hasTOFAbnormal = true;
                TOFAbnormalSb.append(":");
                TOFAbnormalSb.append("后视");
            }
            if (GlobalVariable.sRadarInfo.upTOF < 0) {
                if (hasTOFAbnormal) {
                    TOFAbnormalSb.append(",");
                } else {
                    TOFAbnormalSb.append(":");
                }
                hasTOFAbnormal = true;
                TOFAbnormalSb.append("上视");
            }
            if (GlobalVariable.sRadarInfo.downTOF < 0) {
                if (hasTOFAbnormal) {
                    TOFAbnormalSb.append(",");
                } else {
                    TOFAbnormalSb.append(":");
                }
                hasTOFAbnormal = true;
                TOFAbnormalSb.append("下视");
            }
            if (hasTOFAbnormal) {
                GDUDiagnostics diagnostics2 = new GDUDiagnostics();
                diagnostics2.setType(GDUDiagnostics.GDUDiagnosticsType.RADAR);
                diagnostics2.setReason(TOFAbnormalSb.toString());
                warList.add(diagnostics2);
            }
        }
        return warList;
    }
}