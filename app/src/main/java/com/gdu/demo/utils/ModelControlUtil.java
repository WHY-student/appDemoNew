package com.gdu.demo.utils;

import com.gdu.common.error.GDUError;
import com.gdu.demo.ourgdu.OnOurTargetDetectListener;
import com.gdu.demo.ourgdu.ourGDUVision;
import com.gdu.drone.TargetMode;
import com.gdu.sdk.util.CommonCallbacks.CompletionCallback;

import java.util.List;

// 负责算法的控制，包括开启算法，结束算法，开启增量，增加阈值，减少阈值
public class ModelControlUtil {
    ourGDUVision mGduVision;
    public ModelControlUtil(ourGDUVision mGduVision){
        this.mGduVision = mGduVision;
    }

    public void startAI(OnOurTargetDetectListener listener, CompletionCallback<GDUError> callback){
        mGduVision.setOnTargetDetectListener(listener);
        mGduVision.startTargetDetect(callback);
    }

    public void closeAI(CompletionCallback<GDUError> callback){
        OnOurTargetDetectListener none_listener = new OnOurTargetDetectListener() {
            @Override
            public void onTargetDetectingNew(List<TargetMode> var1, int val2, long val3) {

            }

            @Override
            public void onTargetDetecting(List<TargetMode> targetModes) {
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
        };
        mGduVision.setOnTargetDetectListener(none_listener);
        mGduVision.stopTargetDetect(callback);
    }

    public void startIncremental(CompletionCallback<GDUError> callback){
        mGduVision.targetDetect((byte) 3, (short) 0, (short) 0, (short) 0, (short) 0, (byte) 0, (byte) 0, callback);
    }

    public void subOodThr(CompletionCallback<GDUError> callback){
        mGduVision.targetDetect((byte) 4, (short) 0, (short) 0, (short) 0, (short) 0, (byte) 0, (byte) 0, callback);
    }

    public void addOodThr(CompletionCallback<GDUError> callback){
        mGduVision.targetDetect((byte) 5, (short) 0, (short) 0, (short) 0, (short) 0, (byte) 0, (byte) 0, callback);
    }

    public void startUnknownCollect(CompletionCallback<GDUError> callback){
        mGduVision.targetDetect((byte) 6, (short) 0, (short) 0, (short) 0, (short) 0, (byte) 0, (byte) 0, callback);
    }

    public void stopUnknownCollect(CompletionCallback<GDUError> callback){
        mGduVision.targetDetect((byte) 7, (short) 0, (short) 0, (short) 0, (short) 0, (byte) 0, (byte) 0, callback);
    }

}
