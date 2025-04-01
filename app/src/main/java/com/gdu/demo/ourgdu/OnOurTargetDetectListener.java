package com.gdu.demo.ourgdu;

import com.gdu.drone.TargetMode;
import com.gdu.sdk.vision.OnTargetDetectListener;

import java.util.List;

public interface OnOurTargetDetectListener extends OnTargetDetectListener {
    void onTargetDetectingNew(List<TargetMode> var1, int val2, long val3);
}
