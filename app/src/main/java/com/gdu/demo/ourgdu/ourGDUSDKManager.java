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
import com.gdu.sdk.util.SystemUtil;
import com.gdu.socket.GduCommunication3;
import com.gdu.socket.GduSocketManager;
import com.gdu.socket.GduUDPSocket3;
import com.gdu.socket.IGduSocket;
import com.gdu.util.ChannelUtils;
import com.gdu.util.logs.RonLog;

import java.io.File;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.gdu.common.error.GDUError;
import com.gdu.config.ConnStateEnum;
import com.gdu.config.GduConfig;
import com.gdu.config.GlobalVariable;
import com.gdu.config.GlobalVariable.ConnType;
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
import com.gdu.sdk.products.GDUAircraft;
import com.gdu.sdk.util.SystemUtil;
import com.gdu.socket.GduCommunication3;
import com.gdu.socket.GduSocketManager;
import com.gdu.socket.GduUDPSocket3;
import com.gdu.socket.IGduSocket;
import com.gdu.util.logs.RonLog;
import java.io.File;

public class ourGDUSDKManager {
    private static com.gdu.demo.ourgdu.ourGDUSDKManager instance;
    private ourGDUAircraft mAircraft;
    private Context mContext;
    private GduRCManager mGduRCManager;
    private GduSocketManager mGduSocketManager;
    private GduCommunication3 mGduCommunication;
    private com.gdu.demo.ourgdu.ourGDUSDKManager.SDKManagerCallback mSDKManagerCallback;
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
    private OnChargeByUsbListener onChargeByUsbListener = (charge) -> {
        if (this.getGduCESocket() != null) {
        }

        if (charge) {
            GlobalVariable.RC_usb_hadConn = 1;
        } else {
            GlobalVariable.RC_usb_hadConn = 0;
        }

    };

    public static synchronized com.gdu.demo.ourgdu.ourGDUSDKManager getInstance() {
        if (instance == null) {
            Class var0 = com.gdu.demo.ourgdu.ourGDUSDKManager.class;
            synchronized(com.gdu.demo.ourgdu.ourGDUSDKManager.class) {
                if (instance == null) {
                    instance = new com.gdu.demo.ourgdu.ourGDUSDKManager();
                }
            }
        }

        return instance;
    }

    private ourGDUSDKManager() {
        this.initAllData();
    }

    private void initAllData() {
        String systemModel = SystemUtil.getSystemModel();
        String deviceBrand = SystemUtil.getDeviceBrand();
        if (systemModel.contains("tb8788") && deviceBrand.contains("alps") || systemModel.contains("RCSEE") && deviceBrand.contains("GDU") || systemModel.contains("rcsee") && deviceBrand.contains("gdu") || systemModel.contains("AIC02") && deviceBrand.contains("GDU") || systemModel.contains("aic02") && deviceBrand.contains("gdu")) {
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
        return this.mGduSocketManager != null && this.mGduSocketManager.getGduCommunication() != null ? this.mGduSocketManager.getGduCommunication().getGduSocket() : null;
    }

    public void registerApp(Context context, com.gdu.demo.ourgdu.ourGDUSDKManager.SDKManagerCallback callback) {
        this.mContext = context.getApplicationContext();
        this.mSDKManagerCallback = callback;
        this.initData();
        this.initListener();
        if (callback != null) {
            callback.onRegister(GDUError.REGISTRATION_SUCCESS);
        }

    }

    public Context getContext() {
        return this.mContext;
    }

    private void iniDao() {
        if (daoSession == null) {
            GreenDaoUpgradeHelper helper = new GreenDaoUpgradeHelper(new DatabaseContext(this.mContext, GduConfig.BaseDirectory + File.separator + "database"), "gdusdk.db");
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
        this.getProduct();
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
        RonLog.LogD(new String[]{"connectUSB2 ===被调用，连接状态 " + GlobalVariable.connStateEnum});
        if (GlobalVariable.connStateEnum != ConnStateEnum.Conn_Sucess) {
            if (!GlobalVariable.isRCSEE && !GlobalVariable.isCustomRC) {
                GduUsbManager.getInstance().onCreate(this.mContext.getApplicationContext());
                GduUsbManager.getInstance().setOnChargeByUsbListener(this.onChargeByUsbListener);
                GduUsbManager.getInstance().setOnGDUUsbListener(new OnGDUUsbListener() {
                    public void openUsbModel() {
                        if (com.gdu.demo.ourgdu.ourGDUSDKManager.this.mGduSocketManager != null) {
                            com.gdu.demo.ourgdu.ourGDUSDKManager.this.isUsbConnected = true;
                            GlobalVariable.RC_usb_hadConn = 1;
                            GlobalVariable.connType = ConnType.MGP03_RC_USB;
                            RonLog.LogD(new String[]{"开启socket通信"});
                            com.gdu.demo.ourgdu.ourGDUSDKManager.this.mGduSocketManager.startConnect();
                        }
                    }

                    public void closeUsbModel() {
                        GlobalVariable.RC_usb_hadConn = 0;
                        GlobalVariable.connType = ConnType.MGP03_NONE;
                    }
                });
            } else {
                if (this.mGduSocketManager == null) {
                    return;
                }

                this.mGduSocketManager.startConnect();
            }

        }
    }

    public String getSDKVersion(Context context) {
        return "2.0.66";
    }

    public void stopConnectionToProduct() {
    }

    private void initListener() {
        this.mGduSocketManager.setConnectCallBack(new IGduSocket.OnConnectListener() {
            public void onConnect() {
                com.gdu.demo.ourgdu.ourGDUSDKManager.this.synTime();
                RonLog.LogD(new String[]{"GduDroneApi====:onConnect===:" + com.gdu.demo.ourgdu.ourGDUSDKManager.this.isDroneConnected});
            }

            public void onDisConnect() {
                Log.d("ourGDUSDKManager", "onDisConnect :" + com.gdu.demo.ourgdu.ourGDUSDKManager.this.isDroneConnected);
                if (com.gdu.demo.ourgdu.ourGDUSDKManager.this.isDroneConnected) {
                    if (com.gdu.demo.ourgdu.ourGDUSDKManager.this.mSDKManagerCallback != null) {
                        com.gdu.demo.ourgdu.ourGDUSDKManager.this.mSDKManagerCallback.onProductDisconnect();
                    }

                    com.gdu.demo.ourgdu.ourGDUSDKManager.this.isDroneConnected = false;
                    com.gdu.demo.ourgdu.ourGDUSDKManager.this.mGduRCManager.disconnect();
                    com.gdu.demo.ourgdu.ourGDUSDKManager.this.mGduRCManager.closeConnect();
                    com.gdu.demo.ourgdu.ourGDUSDKManager.this.isGimbalConnected = false;
                    com.gdu.demo.ourgdu.ourGDUSDKManager.this.isCameraConnected = false;
                    com.gdu.demo.ourgdu.ourGDUSDKManager.this.isFCConnected = false;
                    com.gdu.demo.ourgdu.ourGDUSDKManager.this.isBatteryConnected = false;
                    com.gdu.demo.ourgdu.ourGDUSDKManager.this.isAirLinkConnected = false;
                }
            }

            public void onConnectDelay(boolean isDelay) {
            }

            public void onConnectMore() {
                com.gdu.demo.ourgdu.ourGDUSDKManager.this.isDroneConnected = false;
            }

            public void onComponentChange(ComponentKey key) {
                com.gdu.demo.ourgdu.ourGDUSDKManager.this.componentChange(key);
            }
        });
        this.mGduSocketManager.getGduCommunication().getGduSocket().setACDataReceivedListener(new IGduSocket.OnDataReceivedListener() {
            public void onDataReceived(Object o) {
                if (!com.gdu.demo.ourgdu.ourGDUSDKManager.this.isDroneConnected && GlobalVariable.connStateEnum != ConnStateEnum.Conn_None) {
                    com.gdu.demo.ourgdu.ourGDUSDKManager.this.isDroneConnected = true;
                    com.gdu.demo.ourgdu.ourGDUSDKManager.this.getProduct();
                    com.gdu.demo.ourgdu.ourGDUSDKManager.this.mSDKManagerCallback.onProductConnect(com.gdu.demo.ourgdu.ourGDUSDKManager.this.mAircraft);
                }

            }
        });
    }

    private void componentChange(ComponentKey key) {
        BaseComponent component = null;
        if (this.mAircraft == null) {
            this.getProduct();
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
            case RADAR:
            case BASE_STATION:
            default:
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
        }

        if (this.mSDKManagerCallback != null && component != null) {
            this.mSDKManagerCallback.onComponentChange((BaseComponent)null, (BaseComponent)component);
        }

    }

    private void synTime() {
        this.mGduCommunication.synTime((code, bean) -> {
        });
    }

    public interface SDKManagerCallback {
        void onRegister(GDUError var1);

        void onProductDisconnect();

        void onProductConnect(BaseProduct var1);

        void onProductChanged(BaseProduct var1);

        void onComponentChange(BaseComponent var1, BaseComponent var2);

        void onInitProcess(GDUSDKInitEvent var1, int var2);
    }
}
