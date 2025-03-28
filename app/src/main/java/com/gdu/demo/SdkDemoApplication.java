package com.gdu.demo;

import android.app.Application;

import com.gdu.config.GduAppEnv;
import com.gdu.demo.ourgdu.ourGDUAircraft;
import com.gdu.demo.ourgdu.ourGDUSDKManager;
import com.gdu.sdk.base.BaseProduct;
import com.gdu.sdk.manager.GDUSDKManager;
import com.gdu.sdk.products.GDUAircraft;
import com.yolanda.nohttp.NoHttp;


public class SdkDemoApplication extends Application {

    private static BaseProduct product;
    private static SdkDemoApplication gduApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        gduApplication = this;
        GduAppEnv.application = gduApplication;
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(getApplicationContext());
        NoHttp.initialize(this);
    }


    public static synchronized BaseProduct getProductInstance() {
        product = ourGDUSDKManager.getInstance().getProduct();
        return product;
    }

    public static synchronized ourGDUAircraft getAircraftInstance() {
        if (!isAircraftConnected()) {
            return null;
        }
        return (ourGDUAircraft) getProductInstance();
    }

    public static boolean isAircraftConnected() {
        return getProductInstance() != null && getProductInstance() instanceof ourGDUAircraft;
    }
    
    public static SdkDemoApplication getSingleApp() {
        return gduApplication;
    }
}
