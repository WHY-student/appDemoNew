package com.gdu.demo.ourgdu;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.gdu.common.error.GDUError;
import com.gdu.config.ConnStateEnum;
import com.gdu.config.GduConfig;
import com.gdu.config.GlobalVariable;
import com.gdu.product.ComponentKey;
import com.gdu.remotecontrol.listener.OnGDUUsbListener;
import com.gdu.remotecontrol.main.GduRCManager;
import com.gdu.remotecontrol.manager.OnChargeByUsbListener;
import com.gdu.remotecontrol.usb.manager.GduUsbManager;
import com.gdu.sdk.api.AuthApi;
import com.gdu.sdk.base.BaseComponent;
import com.gdu.sdk.base.BaseProduct;
import com.gdu.sdk.customkey.DaoMaster;
import com.gdu.sdk.customkey.DaoSession;
import com.gdu.sdk.customkey.RcCustomKeyManager;
import com.gdu.sdk.dao.DatabaseContext;
import com.gdu.sdk.dao.GreenDaoUpgradeHelper;
import com.gdu.sdk.manager.GDUSDKInitEvent;
//import com.gdu.sdk.products.GDUAircraft;

import com.gdu.sdk.util.SystemUtil;
import com.gdu.socket.GduCommunication3;
import com.gdu.socket.GduSocketManager;
import com.gdu.socket.GduUDPSocket3;
import com.gdu.socket.IGduSocket;
import com.gdu.util.ChannelUtils;
import com.gdu.util.logs.RonLog;
import java.io.File;

/* loaded from: GduLibrary-2.0.64.aar:classes.jar:com/gdu/sdk/manager/GDUSDKManager.class */
public class ourGDUSDKManager {
    private static ourGDUSDKManager instance;
    private ourGDUAircraft mAircraft;
    private Context mContext;
    private GduRCManager mGduRCManager;
    private GduSocketManager mGduSocketManager;
    private GduCommunication3 mGduCommunication;
    private SDKManagerCallback mSDKManagerCallback;
    private boolean isUsbConnected;
    private boolean isDroneConnected;
    private boolean isAuth;
    private AuthApi authApi;
    private static DaoSession daoSession;
    RcCustomKeyManager rcCustomKeyManager;
    private boolean isGimbalConnected;
    private boolean isCameraConnected;
    private boolean isFCConnected;
    private boolean isRCConnected;
    private boolean isBatteryConnected;
    private boolean isAirLinkConnected;
    private OnChargeByUsbListener onChargeByUsbListener = charge -> {
        if (getGduCESocket() != null) {
        }
        if (charge) {
            GlobalVariable.RC_usb_hadConn = (byte) 1;
        } else {
            GlobalVariable.RC_usb_hadConn = (byte) 0;
        }
    };

    /* loaded from: GduLibrary-2.0.64.aar:classes.jar:com/gdu/sdk/manager/GDUSDKManager$SDKManagerCallback.class */
    public interface SDKManagerCallback {
        void onRegister(GDUError gDUError);

        void onProductDisconnect();

        void onProductConnect(BaseProduct baseProduct);

        void onProductChanged(BaseProduct baseProduct);

        void onComponentChange(BaseComponent baseComponent, BaseComponent baseComponent2);

        void onInitProcess(GDUSDKInitEvent gDUSDKInitEvent, int i);
    }

    public static synchronized ourGDUSDKManager getInstance() {
        if (instance == null) {
            synchronized (ourGDUSDKManager.class) {
                if (instance == null) {
                    instance = new ourGDUSDKManager();
                }
            }
        }
        return instance;
    }

    private ourGDUSDKManager() {
        initAllData();
    }

    private void initAllData() {
        String systemModel = SystemUtil.getSystemModel();
        String deviceBrand = SystemUtil.getDeviceBrand();
        if ((systemModel.contains("tb8788") && deviceBrand.contains("alps")) || ((systemModel.contains("RCSEE") && deviceBrand.contains("GDU")) || ((systemModel.contains("rcsee") && deviceBrand.contains(ChannelUtils.CHANNEL_GDU)) || ((systemModel.contains("AIC02") && deviceBrand.contains("GDU")) || (systemModel.contains("aic02") && deviceBrand.contains(ChannelUtils.CHANNEL_GDU)))))) {
            GlobalVariable.isRCSEE = true;
        } else if (deviceBrand.contains("Mega") || systemModel.contains("M18")) {
            GlobalVariable.isCustomRC = true;
        }
    }

    public BaseProduct getProduct() {
        if (this.mAircraft == null) {
            this.mAircraft = new ourGDUAircraft();
        }
        return this.mAircraft;
    }

    private GduUDPSocket3 getGduCESocket() {
        if (this.mGduSocketManager == null || this.mGduSocketManager.getGduCommunication() == null) {
            return null;
        }
        return this.mGduSocketManager.getGduCommunication().getGduSocket();
    }

    public void registerApp(Context context, SDKManagerCallback callback) {
        this.mContext = context.getApplicationContext();
        this.mSDKManagerCallback = callback;
        initData();
        initListener();
        if (callback != null) {
            callback.onRegister(GDUError.REGISTRATION_SUCCESS);
        }
    }

    public Context getContext() {
        return this.mContext;
    }

    private void iniDao() {
        if (daoSession == null) {
            GreenDaoUpgradeHelper helper = new GreenDaoUpgradeHelper(new DatabaseContext(this.mContext, GduConfig.BaseDirectory + File.separator + "database"), GduConfig.DATABASE_SDK_NAME);
            SQLiteDatabase db = helper.getWritableDatabase();
            DaoMaster daoMaster = new DaoMaster(db);
            daoSession = daoMaster.newSession();
        }
    }

    public static DaoSession getDaoSession() {
        return daoSession;
    }

    private void initDFIMUCallback(Context context) {
    }

    private void initData() {
        if (this.mContext != null) {
            this.mGduRCManager = GduRCManager.getInstance(this.mContext);
        }
        this.mGduSocketManager = GduSocketManager.getInstance();
        this.mGduCommunication = this.mGduSocketManager.getGduCommunication();
        getProduct();
        this.mAircraft = new ourGDUAircraft();
        this.mAircraft.initContext(this.mContext);
        this.rcCustomKeyManager = new RcCustomKeyManager();
    }

    public void setCustomRCEnable(boolean isEnable) {
        GlobalVariable.isCustomRCEnable = isEnable;
        GlobalVariable.isCustomRC = isEnable;
    }

    public boolean getCustomRCEnable() {
        return GlobalVariable.isCustomRCEnable;
    }

    public void startConnectionToProduct() {
        RonLog.LogD("connectUSB2 ===被调用，连接状态 " + GlobalVariable.connStateEnum);
        if (GlobalVariable.connStateEnum == ConnStateEnum.Conn_Sucess) {
            return;
        }
        if (GlobalVariable.isRCSEE || GlobalVariable.isCustomRC) {
            if (this.mGduSocketManager == null) {
                return;
            }
            this.mGduSocketManager.startConnect();
            return;
        }
        GduUsbManager.getInstance().onCreate(this.mContext.getApplicationContext());
        GduUsbManager.getInstance().setOnChargeByUsbListener(this.onChargeByUsbListener);
        GduUsbManager.getInstance().setOnGDUUsbListener(new OnGDUUsbListener() { // from class: com.gdu.sdk.manager.GDUSDKManager.1
            @Override // com.gdu.remotecontrol.listener.OnGDUUsbListener
            public void openUsbModel() {
                if (ourGDUSDKManager.this.mGduSocketManager != null) {
                    ourGDUSDKManager.this.isUsbConnected = true;
                    GlobalVariable.RC_usb_hadConn = (byte) 1;
                    GlobalVariable.connType = GlobalVariable.ConnType.MGP03_RC_USB;
                    RonLog.LogD("开启socket通信");
                    ourGDUSDKManager.this.mGduSocketManager.startConnect();
                }
            }

            @Override // com.gdu.remotecontrol.listener.OnGDUUsbListener
            public void closeUsbModel() {
                GlobalVariable.RC_usb_hadConn = (byte) 0;
                GlobalVariable.connType = GlobalVariable.ConnType.MGP03_NONE;
            }
        });
    }

    public String getSDKVersion(Context context) {
        return "2.0.64";
    }

    public void stopConnectionToProduct() {
    }

    private void initListener() {
        this.mGduSocketManager.setConnectCallBack(new IGduSocket.OnConnectListener() { // from class: com.gdu.sdk.manager.GDUSDKManager.2
            @Override // com.gdu.socket.IGduSocket.OnConnectListener
            public void onConnect() {
                ourGDUSDKManager.this.synTime();
                RonLog.LogD("GduDroneApi====:onConnect===:" + ourGDUSDKManager.this.isDroneConnected);
            }

            @Override // com.gdu.socket.IGduSocket.OnConnectListener
            public void onDisConnect() {
                Log.d("GDUSDKManager", "onDisConnect :" + ourGDUSDKManager.this.isDroneConnected);
                if (ourGDUSDKManager.this.isDroneConnected) {
                    if (ourGDUSDKManager.this.mSDKManagerCallback != null) {
                        ourGDUSDKManager.this.mSDKManagerCallback.onProductDisconnect();
                    }
                    ourGDUSDKManager.this.isDroneConnected = false;
                    ourGDUSDKManager.this.mGduRCManager.disconnect();
                    ourGDUSDKManager.this.mGduRCManager.closeConnect();
                    ourGDUSDKManager.this.isGimbalConnected = false;
                    ourGDUSDKManager.this.isCameraConnected = false;
                    ourGDUSDKManager.this.isFCConnected = false;
                    ourGDUSDKManager.this.isBatteryConnected = false;
                    ourGDUSDKManager.this.isAirLinkConnected = false;
                }
            }

            @Override // com.gdu.socket.IGduSocket.OnConnectListener
            public void onConnectDelay(boolean isDelay) {
            }

            @Override // com.gdu.socket.IGduSocket.OnConnectListener
            public void onConnectMore() {
                ourGDUSDKManager.this.isDroneConnected = false;
            }

            @Override // com.gdu.socket.IGduSocket.OnConnectListener
            public void onComponentChange(ComponentKey key) {
                ourGDUSDKManager.this.componentChange(key);
            }
        });
        this.mGduSocketManager.getGduCommunication().getGduSocket().setACDataReceivedListener(new IGduSocket.OnDataReceivedListener() { // from class: com.gdu.sdk.manager.GDUSDKManager.3
            @Override // com.gdu.socket.IGduSocket.OnDataReceivedListener
            public void onDataReceived(Object o) {
                if (!ourGDUSDKManager.this.isDroneConnected && GlobalVariable.connStateEnum != ConnStateEnum.Conn_None) {
                    ourGDUSDKManager.this.isDroneConnected = true;
                    ourGDUSDKManager.this.getProduct();
                    ourGDUSDKManager.this.mSDKManagerCallback.onProductConnect(ourGDUSDKManager.this.mAircraft);
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void componentChange(ComponentKey key) {
        BaseComponent component = null;
        if (this.mAircraft == null) {
            getProduct();
        }
        switch (key) {
            case GIMBAL:
                if (this.isGimbalConnected) {
                    return;
                }
                this.isGimbalConnected = true;
                component = this.mAircraft.getGimbal();
                Log.d("GIMBAL", "onComponentChange  gimbal : " + component);
                break;
            case CAMERA:
                if (this.isCameraConnected) {
                    return;
                }
                this.isCameraConnected = true;
                component = this.mAircraft.getCamera();
                break;
            case FLIGHT_CONTROLLER:
                if (this.isFCConnected) {
                    return;
                }
                this.isFCConnected = true;
                component = this.mAircraft.getFlightController();
                break;
            case REMOTE_CONTROLLER:
                if (this.isRCConnected) {
                    return;
                }
                this.isRCConnected = true;
                component = this.mAircraft.getRemoteController();
                break;
            case BATTERY:
                if (this.isBatteryConnected) {
                    return;
                }
                this.isBatteryConnected = true;
                component = this.mAircraft.getBattery();
                break;
            case AIR_LINK:
                if (this.isAirLinkConnected) {
                    return;
                }
                this.isAirLinkConnected = true;
                component = this.mAircraft.getAirLink();
                break;
        }
        if (this.mSDKManagerCallback != null && component != null) {
            this.mSDKManagerCallback.onComponentChange(null, component);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void synTime() {
        this.mGduCommunication.synTime((code, bean) -> {
        });
    }
}