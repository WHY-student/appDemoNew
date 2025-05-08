package com.gdu.demo.ourgdu;

import android.util.Log;
import com.gdu.api.Util.DetectPositionCalUtil;
import com.gdu.config.GduConfig;
import com.gdu.config.GlobalVariable;
import com.gdu.detect.AIModelLabel;
import com.gdu.drone.TargetMode;
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
    private int mTargetId;
    public VideoSizeMode show_mode = VideoSizeMode.MODE_16_9;
    private long currentTime = 0;
    private int frameWidth = 0;
    private int frameHeight = 0;
    private final List<TargetMode> mTargetModeList = new ArrayList();

    public void setFrameSize(VideoSizeMode mode, int width, int height) {
        this.show_mode = mode;
        this.frameWidth = width;
        this.frameHeight = height;
    }

    public void setOnTargetDetectListener(OnTargetDetectListener listener) {
        this.targetDetectListener = listener;
    }

    public void getDetectTargetNew(GduFrame3 bean) {
        if (bean == null || bean.frameContent == null) {
            if (null != this.targetDetectListener) {
                this.targetDetectListener.onTargetDetectFailed(-1);
                return;
            }
            return;
        }
        byte[] content = bean.frameContent;
        if (bean.frameCMD == 42205240 || bean.frameCMD == 16908344) {
            getAIBoxTargetNew(content);
        } else if (bean.frameCMD == 16908342) {
            getDetectTargets(content);
        } else {
            getDetectTargetNew(content);
        }
    }

    private void getAIBoxTargetNew(byte[] content) {
        int length = content.length;
        if (length <= 38) {
            this.mTargetModeList.clear();
            this.mTargetId = 0;
            if (null != this.targetDetectListener) {
                this.targetDetectListener.onTargetDetecting(null);
            }
        } else if (System.currentTimeMillis() - this.currentTime >= 20) {
            this.currentTime = System.currentTimeMillis();
            long createTime = ByteUtilsLowBefore.byte2long(content, 0);
            int modelId = ByteUtilsLowBefore.byte2Int(content, 8);
            ByteUtilsLowBefore.byte2Int(content, 24);
            ByteUtilsLowBefore.byte2Int(content, 28);
            int targetNum = ByteUtilsLowBefore.byte2short(content, 36);
            List<TargetMode> targetModes = new ArrayList<>();
            for (int i = 0; i < targetNum; i++) {
                byte[] targetBytes = new byte[14];
                System.arraycopy(content, 38 + (i * 14), targetBytes, 0, 14);
                TargetMode targetMode = parseTargetAIBox(targetBytes);
                targetMode.setCreateTime(createTime);
                targetMode.setTargetName(getTargetName(modelId, targetMode.getTargetType()));
                targetMode.setTargetType((short) -1);
                targetModes.add(targetMode);
            }
            if (!targetModes.isEmpty()) {
                updateTargetModeList(targetModes);
            }
            this.mTargetId = 0;
            if (this.targetDetectListener != null) {
                this.targetDetectListener.onTargetDetecting(this.mTargetModeList);
            }
        }
    }

    private TargetMode parseTargetAIBox(byte[] content) {
        TargetMode targetMode = new TargetMode();
        this.mTargetId++;
        short pointX = ByteUtilsLowBefore.byte2short(content, 0);
        short pointY = ByteUtilsLowBefore.byte2short(content, 2);
        short width = ByteUtilsLowBefore.byte2short(content, 4);
        short height = ByteUtilsLowBefore.byte2short(content, 6);
        byte targetConfidence = content[8];
        short targetType = ByteUtilsLowBefore.byte2UnsignedByte(content[9]);
        int detectId = ByteUtilsLowBefore.byte2Int(content, 10);
        targetMode.setId(detectId);
        if (this.frameWidth != 0 && this.frameHeight != 0) {
            pointX = DetectPositionCalUtil.transX(this.show_mode, pointX);
            pointY = DetectPositionCalUtil.transY(this.show_mode, pointY);
            width = DetectPositionCalUtil.transWidth(this.show_mode, width);
            height = DetectPositionCalUtil.transHeight(this.show_mode, height);
        }
        targetMode.setHeight(height);
        targetMode.setWidth(width);
        targetMode.setLeftX(pointX);
        targetMode.setLeftY(pointY);
        targetMode.setTargetConfidence(targetConfidence);
        targetMode.setTargetType(targetType);
        return targetMode;
    }

    private void getDetectTargets(byte[] content) {
        int length = content.length;
        if (length <= 34) {
            this.mTargetModeList.clear();
            this.mTargetId = 0;
            if (null != this.targetDetectListener) {
                this.targetDetectListener.onTargetDetecting(null);
            }
        } else if (System.currentTimeMillis() - this.currentTime >= 20) {
            this.currentTime = System.currentTimeMillis();
            int targetNum = ByteUtilsLowBefore.byte2short(content, 32);
            long createTime = ByteUtilsLowBefore.byte2long(content, 0);
            ByteUtilsLowBefore.byte2Int(content, 20);
            ByteUtilsLowBefore.byte2Int(content, 24);
            List<TargetMode> targetModes = new ArrayList<>();
            for (int i = 0; i < targetNum; i++) {
                byte[] targetBytes = new byte[10];
                System.arraycopy(content, 34 + (i * 10), targetBytes, 0, 10);
                TargetMode targetMode = parseTarget(targetBytes);
                targetMode.setCreateTime(createTime);
                targetModes.add(targetMode);
            }
            if (!targetModes.isEmpty()) {
                updateTargetModeList(targetModes);
            }
            this.mTargetId = 0;
            if (this.targetDetectListener != null) {
                this.targetDetectListener.onTargetDetecting(this.mTargetModeList);
            }
        }
    }

    private void updateTargetModeList(List<TargetMode> targetModes) {
        for (int i = 0; i < this.mTargetModeList.size(); i++) {
            for (int j = 0; j < targetModes.size(); j++) {
                if (this.mTargetModeList.get(i).getId() == targetModes.get(j).getId()) {
                    targetModes.get(j).setCreateTime(this.mTargetModeList.get(i).getCreateTime());
                }
            }
        }
        this.mTargetModeList.clear();
        this.mTargetModeList.addAll(targetModes);
    }

    private String getTargetName(int modelId, short targetIndex) {
        if (modelId > 10000) {
            if (GlobalVariable.targetDetectLabels == null) {
                return GduConfig.STR_EMPTY;
            }
            for (int i = 0; i < GlobalVariable.targetDetectLabels.size(); i++) {
                AIModelLabel aiModelLabel = GlobalVariable.targetDetectLabels.get(i);
                if (aiModelLabel.getModelId() == modelId && aiModelLabel.getLabelArray().length > targetIndex) {
                    return aiModelLabel.getLabelArray()[targetIndex];
                }
            }
            return GduConfig.STR_EMPTY;
        }
        String labelName = GduConfig.STR_EMPTY;
        TargetLabel targetLabel = TargetLabel.get(targetIndex);
        if (targetLabel != null) {
            labelName = ResourceUtil.getStringById(targetLabel.getValue());
        }
        return labelName;
    }

    private void getDetectTargetNew(byte[] content) {
        int length = content.length;
        if (length < 3) {
            return;
        }
        byte packageInfo = content[length - 2];
        byte b2 = content[length - 1];
        byte totalNum = (byte) ((packageInfo >> 4) & GduConfig.UNKNOWN_ERROR);
        byte currentNum = (byte) (packageInfo & 15);
        Log.i("getDetectTargetNew()", "totalNum = " + ((int) totalNum) + ",currentNum = " + ((int) currentNum));
        int targetNum = (length - 2) / 10;
        List<TargetMode> targetModes = new CopyOnWriteArrayList<>();
        for (int i = 0; i < targetNum; i++) {
            byte[] targetBytes = new byte[10];
            for (int j = 0; j < 10; j++) {
                targetBytes[j] = content[(i * 10) + j];
            }
            TargetMode targetMode = parseTargetModeNew(targetBytes);
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

    private TargetMode parseTargetModeNew(byte[] content) {
        TargetMode targetMode = new TargetMode();
        short pointX = ByteUtilsLowBefore.byte2short(content, 0);
        short pointY = ByteUtilsLowBefore.byte2short(content, 2);
        short width = ByteUtilsLowBefore.byte2short(content, 4);
        short height = ByteUtilsLowBefore.byte2short(content, 6);
        byte targetConfidence = content[8];
        byte targetInfo = content[9];
        byte flawType = (byte) (targetInfo & 1 & GduConfig.UNKNOWN_ERROR);
        byte b2 = (byte) ((targetInfo >> 1) & GduConfig.UNKNOWN_ERROR);
        targetMode.setHeight(height);
        targetMode.setWidth(width);
        targetMode.setLeftX(pointX);
        targetMode.setLeftY(pointY);
        targetMode.setTargetConfidence(targetConfidence);
        targetMode.setFlawType(flawType);
        return targetMode;
    }

    private TargetMode parseTarget(byte[] content) {
        MyLogUtils.i("parseTargetModeS200()");
        TargetMode targetMode = new TargetMode();
        this.mTargetId++;
        targetMode.setId(this.mTargetId);
        short pointX = ByteUtilsLowBefore.byte2short(content, 0);
        short pointY = ByteUtilsLowBefore.byte2short(content, 2);
        short width = ByteUtilsLowBefore.byte2short(content, 4);
        short height = ByteUtilsLowBefore.byte2short(content, 6);
        byte targetConfidence = content[8];
        byte targetType = content[9];
        if (this.frameWidth != 0 && this.frameHeight != 0) {
            pointX = DetectPositionCalUtil.transX(this.show_mode, pointX);
            pointY = DetectPositionCalUtil.transY(this.show_mode, pointY);
            width = DetectPositionCalUtil.transWidth(this.show_mode, width);
            height = DetectPositionCalUtil.transHeight(this.show_mode, height);
        }
        targetMode.setHeight(height);
        targetMode.setWidth(width);
        targetMode.setLeftX(pointX);
        targetMode.setLeftY(pointY);
        targetMode.setTargetConfidence(targetConfidence);
        targetMode.setTargetType(targetType);
        return targetMode;
    }
}
