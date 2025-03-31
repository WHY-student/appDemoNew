package com.gdu.demo.ourgdu;

import android.util.Log;
import com.gdu.api.Util.DetectPositionCalUtil;
import com.gdu.api.Util.TargetLocationUtil;
import com.gdu.beans.LocationInParam;
import com.gdu.config.GlobalVariable;
import com.gdu.detect.AIModelLabel;
import com.gdu.drone.TargetMode;
import com.gdu.library.TargetLocationResult;
import com.gdu.sdk.vision.OnTargetDetectListener;
import com.gdu.sdk.vision.aibox.bean.TargetLabel;
import com.gdu.sdk.vision.aibox.bean.VideoSizeMode;
import com.gdu.socket.GduFrame3;
import com.gdu.util.ByteUtilsLowBefore;
import com.gdu.util.ResourceUtil;
import com.gdu.util.logger.MyLogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ourAIBoxManager {
    private OnTargetDetectListener targetDetectListener;
    public VideoSizeMode show_mode;
    private int mTargetId;
    private long currentTime;
    private int frameWidth;
    private int frameHeight;
    private final List<TargetMode> mTargetModeList;
//    private int modelID = 0;

    public ourAIBoxManager() {
        this.show_mode = VideoSizeMode.MODE_16_9;
        this.currentTime = 0L;
        this.frameWidth = 0;
        this.frameHeight = 0;
        this.mTargetModeList = new ArrayList();
    }

    public void setFrameSize(VideoSizeMode mode, int width, int height) {
        this.show_mode = mode;
        this.frameWidth = width;
        this.frameHeight = height;
    }

    public void setOnTargetDetectListener(OnTargetDetectListener listener) {
        this.targetDetectListener = listener;
    }

    public void getDetectTargetNew(GduFrame3 bean) {
        if (bean != null && bean.frameContent != null) {
            byte[] content = bean.frameContent;
            if (bean.frameCMD != 42205240 && bean.frameCMD != 16908344) {
                if (bean.frameCMD == 16908342) {
                    this.getDetectTargets(content);
                } else {
                    this.getDetectTargetNew(content);
                }
            } else {
                this.getAIBoxTargetNew(content);
            }

        } else {
            if (null != this.targetDetectListener) {
                this.targetDetectListener.onTargetDetectFailed(-1);
            }

        }
    }

    private void getAIBoxTargetNew(byte[] content) {
        int length = content.length;
        if (length <= 38) {
            this.mTargetModeList.clear();
            this.mTargetId = 0;
            if (null != this.targetDetectListener) {
                this.targetDetectListener.onTargetDetecting((List)null);
            }

        } else if (System.currentTimeMillis() - this.currentTime >= 20L) {
            this.currentTime = System.currentTimeMillis();
            long createTime = ByteUtilsLowBefore.byte2long(content, 0);
            int modelId = ByteUtilsLowBefore.byte2Int(content, 8);
            int lng = ByteUtilsLowBefore.byte2Int(content, 24);
            int lat = ByteUtilsLowBefore.byte2Int(content, 28);
            short targetNum = ByteUtilsLowBefore.byte2short(content, 36);
            List<TargetMode> targetModes = new ArrayList();

            for(int i = 0; i < targetNum; ++i) {
                byte[] targetBytes = new byte[14];
                System.arraycopy(content, 38 + i * 14, targetBytes, 0, 14);
                TargetMode targetMode = this.parseTargetAIBox(targetBytes);
                targetMode.setCreateTime(createTime);
                targetMode.setTargetName(this.getTargetName(1066, targetMode.getTargetType()));
//                targetMode.setTargetType((short)-1);
                targetModes.add(targetMode);
            }

            if (!targetModes.isEmpty()) {
                this.updateTargetModeList(targetModes);
            }

            this.mTargetId = 0;
            if (this.targetDetectListener != null) {
                this.targetDetectListener.onTargetDetecting(this.mTargetModeList);
                this.targetDetectListener.onTargetDetectFailed(modelId);
            }

        }
    }

    private TargetMode parseTargetAIBox(byte[] content) {
        TargetMode targetMode = new TargetMode();
        ++this.mTargetId;
        short pointX = ByteUtilsLowBefore.byte2short(content, 0);
        short pointY = ByteUtilsLowBefore.byte2short(content, 2);
        short width = ByteUtilsLowBefore.byte2short(content, 4);
        short height = ByteUtilsLowBefore.byte2short(content, 6);
        byte targetConfidence = content[8];
        short targetType = ByteUtilsLowBefore.byte2UnsignedByte(content[9]);
        int detectId = ByteUtilsLowBefore.byte2Int(content, 10);
        targetMode.setId(detectId);
        TargetLocationResult result = this.targetLocation(pointX, width, pointY, height);
        if (result.code <= 4) {
            targetMode.setLng((long)(result.targetLon * 1.0E7));
            targetMode.setLat((long)(result.targetLat * 1.0E7));
            targetMode.setElevation((long)(result.targetHeight * 100.0));
        }

        if (this.frameWidth != 0 && this.frameHeight != 0) {
            pointX = DetectPositionCalUtil.transX(this.show_mode, (float)pointX);
            pointY = DetectPositionCalUtil.transY(this.show_mode, (float)pointY);
            width = DetectPositionCalUtil.transWidth(this.show_mode, (float)width);
            height = DetectPositionCalUtil.transHeight(this.show_mode, (float)height);
        }

        targetMode.setInParam(result.locationInParam);
        targetMode.setHeight(height);
        targetMode.setWidth(width);
        targetMode.setLeftX(pointX);
        targetMode.setLeftY(pointY);
        targetMode.setTargetConfidence((short)targetConfidence);
        targetMode.setTargetType(targetType);
        return targetMode;
    }

    private TargetLocationResult targetLocation(short pointX, short width, short pointY, short height) {
        boolean isIr = GlobalVariable.sCameraLightType == 0;
        int lightType = isIr ? 1 : 0;
        int focus = isIr ? GlobalVariable.sCurrentIrCameraFocus : GlobalVariable.sCurrentCameraFocus;
        float zoom = isIr ? GlobalVariable.sCurrentIrCameraZoom : GlobalVariable.sCurrentCameraZoom;
        LocationInParam inParam = new LocationInParam();
        inParam.lightType = lightType;
        inParam.imgX = 1920;
        inParam.imgY = 1080;
        inParam.imgCX = (int)((float)pointX + (float)width / 2.0F);
        inParam.imgCY = (int)((float)pointY + (float)height / 2.0F);
        inParam.gimbalYaw = GlobalVariable.HolderYAW;
        inParam.gimbalPitch = -GlobalVariable.HolderPitch;
        inParam.gimbalRoll = GlobalVariable.HolderRoll;
        inParam.uavYaw = (float)GlobalVariable.planeAngle / 100.0F;
        inParam.uavPitch = GlobalVariable.uavPitchAngle;
        inParam.uavRoll = GlobalVariable.uavRollAngle;
        inParam.uavLon = GlobalVariable.GPS_Lon;
        inParam.uavLat = GlobalVariable.GPS_Lat;
        inParam.uavHeight = (double)((float)GlobalVariable.height_drone / 100.0F);
        inParam.uavAlt = (double)((float)GlobalVariable.altitude_drone / 100.0F);
        inParam.gimbalType = GlobalVariable.gimbalType.getKey();
        inParam.focal = 0.0;
        inParam.zoom = zoom;
        inParam.path = "";
        return TargetLocationUtil.calTargetLocation(inParam);
    }

    private void getDetectTargets(byte[] content) {
        int length = content.length;
        if (length <= 34) {
            this.mTargetModeList.clear();
            this.mTargetId = 0;
            if (null != this.targetDetectListener) {
                this.targetDetectListener.onTargetDetecting((List)null);
            }

        } else if (System.currentTimeMillis() - this.currentTime >= 20L) {
            this.currentTime = System.currentTimeMillis();
            int targetNum = ByteUtilsLowBefore.byte2short(content, 32);
            long createTime = ByteUtilsLowBefore.byte2long(content, 0);
            int lng = ByteUtilsLowBefore.byte2Int(content, 20);
            int lat = ByteUtilsLowBefore.byte2Int(content, 24);
            List<TargetMode> targetModes = new ArrayList();

            for(int i = 0; i < targetNum; ++i) {
                byte[] targetBytes = new byte[10];
                System.arraycopy(content, 34 + i * 10, targetBytes, 0, 10);
                TargetMode targetMode = this.parseTarget(targetBytes);
                targetMode.setCreateTime(createTime);
                targetModes.add(targetMode);
            }

            if (!targetModes.isEmpty()) {
                this.updateTargetModeList(targetModes);
            }

            this.mTargetId = 0;
            if (this.targetDetectListener != null) {
                this.targetDetectListener.onTargetDetecting(this.mTargetModeList);
            }

        }
    }

    private void updateTargetModeList(List<TargetMode> targetModes) {
        for(int i = 0; i < this.mTargetModeList.size(); ++i) {
            for(int j = 0; j < targetModes.size(); ++j) {
                if (((TargetMode)this.mTargetModeList.get(i)).getId() == ((TargetMode)targetModes.get(j)).getId()) {
                    ((TargetMode)targetModes.get(j)).setCreateTime(((TargetMode)this.mTargetModeList.get(i)).getCreateTime());
                }
            }
        }

        this.mTargetModeList.clear();
        this.mTargetModeList.addAll(targetModes);
    }

    private String getTargetName(int modelId, short targetIndex) {
        if (modelId > 10000) {
            if (GlobalVariable.targetDetectLabels == null) {
                return "";
            } else {
                for(int i = 0; i < GlobalVariable.targetDetectLabels.size(); ++i) {
                    AIModelLabel aiModelLabel = (AIModelLabel)GlobalVariable.targetDetectLabels.get(i);
                    if (aiModelLabel.getModelId() == modelId && aiModelLabel.getLabelArray().length > targetIndex) {
                        return aiModelLabel.getLabelArray()[targetIndex];
                    }
                }

                return "";
            }
        } else {
            String labelName = "";
            TargetLabel targetLabel = TargetLabel.get(targetIndex);
            if (targetLabel != null) {
                labelName = ResourceUtil.getStringById(targetLabel.getValue());
            }

            return labelName;
        }
    }

    private void getDetectTargetNew(byte[] content) {
        int length = content.length;
        if (length >= 3) {
            byte packageInfo = content[length - 2];
            byte var10000 = content[length - 1];
            byte totalNum = (byte)(packageInfo >> 4 & 255);
            byte currentNum = (byte)(packageInfo & 15);
            Log.i("getDetectTargetNew()", "totalNum = " + totalNum + ",currentNum = " + currentNum);
            int targetNum = (length - 2) / 10;
            List<TargetMode> targetModes = new CopyOnWriteArrayList();

            for(int i = 0; i < targetNum; ++i) {
                byte[] targetBytes = new byte[10];

                for(int j = 0; j < 10; ++j) {
                    targetBytes[j] = content[i * 10 + j];
                }

                TargetMode targetMode = this.parseTargetModeNew(targetBytes);
                targetModes.add(targetMode);
            }

            if (this.mTargetModeList != null) {
                if (currentNum == 0) {
                    this.mTargetModeList.clear();
                }

                this.mTargetModeList.addAll(targetModes);
                if (currentNum == totalNum - 1 && this.targetDetectListener != null) {
                    this.targetDetectListener.onTargetDetecting(targetModes);
                }
            }

        }
    }

    private TargetMode parseTargetModeNew(byte[] content) {
        TargetMode targetMode = new TargetMode();
        short pointX = ByteUtilsLowBefore.byte2short(content, 0);
        short pointY = ByteUtilsLowBefore.byte2short(content, 2);
        short width = ByteUtilsLowBefore.byte2short(content, 4);
        short height = ByteUtilsLowBefore.byte2short(content, 6);
        byte targetConfidence = content[8];
        byte targetInfo = content[9];
        byte flawType = (byte)(targetInfo & 1 & 255);
        byte targetType = (byte)(targetInfo >> 1 & 255);
        targetMode.setHeight(height);
        targetMode.setWidth(width);
        targetMode.setLeftX(pointX);
        targetMode.setLeftY(pointY);
        targetMode.setTargetConfidence((short)targetConfidence);
        targetMode.setFlawType(flawType);
        return targetMode;
    }

    private TargetMode parseTarget(byte[] content) {
        MyLogUtils.i("parseTargetModeS200()");
        TargetMode targetMode = new TargetMode();
        ++this.mTargetId;
        targetMode.setId(this.mTargetId);
        short pointX = ByteUtilsLowBefore.byte2short(content, 0);
        short pointY = ByteUtilsLowBefore.byte2short(content, 2);
        short width = ByteUtilsLowBefore.byte2short(content, 4);
        short height = ByteUtilsLowBefore.byte2short(content, 6);
        byte targetConfidence = content[8];
        byte targetType = content[9];
        TargetLocationResult result = this.targetLocation(pointX, width, pointY, height);
        if (result.code <= 4) {
            targetMode.setLng((long)(result.targetLon * 1.0E7));
            targetMode.setLat((long)(result.targetLat * 1.0E7));
            targetMode.setElevation((long)(result.targetHeight * 100.0));
        }

        if (this.frameWidth != 0 && this.frameHeight != 0) {
            pointX = DetectPositionCalUtil.transX(this.show_mode, (float)pointX);
            pointY = DetectPositionCalUtil.transY(this.show_mode, (float)pointY);
            width = DetectPositionCalUtil.transWidth(this.show_mode, (float)width);
            height = DetectPositionCalUtil.transHeight(this.show_mode, (float)height);
        }

        targetMode.setInParam(result.locationInParam);
        targetMode.setHeight(height);
        targetMode.setWidth(width);
        targetMode.setLeftX(pointX);
        targetMode.setLeftY(pointY);
        targetMode.setTargetConfidence((short)targetConfidence);
        targetMode.setTargetType((short)targetType);
        return targetMode;
    }
}

