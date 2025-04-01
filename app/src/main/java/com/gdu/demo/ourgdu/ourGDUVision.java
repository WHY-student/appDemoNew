//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//
package com.gdu.demo.ourgdu;


import android.util.Log;
import com.gdu.common.error.GDUError;
import com.gdu.drone.TargetMode;
import com.gdu.sdk.util.CommonCallbacks;
import com.gdu.sdk.vision.OnTargetDetectListener;
import com.gdu.sdk.vision.OnTargetDetectModelListener;
import com.gdu.sdk.vision.OnTargetTrackListener;
import com.gdu.sdk.vision.aibox.AIBoxManager;
import com.gdu.sdk.vision.aibox.bean.VideoSizeMode;
import com.gdu.socket.GduCommunication3;
import com.gdu.socket.GduFrame3;
import com.gdu.socket.GduSocketManager;
import com.gdu.socket.SocketCallBack3;
import com.gdu.util.ByteUtilsLowBefore;
import com.gdu.util.RectUtil;
import com.gdu.util.logger.MyLogUtils;
import com.gdu.util.logs.AppLog;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ourGDUVision {
    private GduCommunication3 mGduCommunication3 = GduSocketManager.getInstance().getGduCommunication();
    private OnOurTargetDetectListener targetDetectListener;
    private OnTargetTrackListener targetTrackListener;
    private OnTargetDetectModelListener targetDetectModelListener;
    private final List<TargetMode> mTargetModeList = new ArrayList();
    private ourAIBoxManager aiBoxManager = new ourAIBoxManager();
    private final SocketCallBack3 targetDetectCallBack = new SocketCallBack3() {
        public void callBack(int code, GduFrame3 bean) {
            if (code == 0 && bean != null) {
                try {
                    ourGDUVision.this.aiBoxManager.getDetectTargetNew(bean);
                } catch (Exception var4) {
                    Exception e = var4;
                    MyLogUtils.e("获取监测目标出错", e);
                }
            } else if (null != ourGDUVision.this.targetDetectListener) {
                ourGDUVision.this.targetDetectListener.onTargetDetectFailed(-1);
            }

        }
    };
    private final SocketCallBack3 videoTrackCallback = (code, bean) -> {
        if (bean.frameContent != null) {
            this.getTrackResult(bean.frameContent[0], ByteUtilsLowBefore.byte2short(bean.frameContent, 1), ByteUtilsLowBefore.byte2short(bean.frameContent, 3), ByteUtilsLowBefore.byte2short(bean.frameContent, 5), ByteUtilsLowBefore.byte2short(bean.frameContent, 7));
        }
    };
    private final SocketCallBack3 multipleTargetTrackCb = new SocketCallBack3() {
        public void callBack(int code, GduFrame3 bean) {
            if (code == 0 && bean != null && bean.frameContent != null) {
                byte state = bean.frameContent[0];
                if (state != 33) {
                    if (state == 32) {
                        ourGDUVision.this.getMultiDetectTargetNew(bean.frameContent);
                    } else {
                        short pointX = 0;
                        short pointY = 0;
                        short width = 0;
                        short height = 0;
                        if (bean.frameContent.length >= 9) {
                            pointX = (short)((int)((double)((bean.frameContent[1] & 255) * 1920) / 255.0));
                            pointY = (short)((int)((double)((bean.frameContent[2] & 255) * 1080) / 255.0));
                            width = (short)((int)((double)((bean.frameContent[3] & 255) * 1920) / 255.0));
                            height = (short)((int)((double)((bean.frameContent[4] & 255) * 1080) / 255.0));
                        }

                        ourGDUVision.this.getTrackResult(state, pointX, pointY, width, height);
                    }
                }
            }

        }
    };
    private final SocketCallBack3 targetDetectModelsCallback = (code, bean) -> {
        if (bean != null && bean.frameContent != null) {
            if (code == 0) {
                byte[] content = bean.frameContent;
                int m = ByteUtilsLowBefore.byte2Int(content, 0);
                String sJson = new String(bean.frameContent, 4, m);
                AppLog.e("SettingCommonFragment", "getTargetDetectModels " + new String(sJson.getBytes(StandardCharsets.UTF_8)));
                if (null != this.targetDetectModelListener) {
                    this.targetDetectModelListener.onTargetDetectModel(sJson);
                }
            } else if (null != this.targetDetectModelListener) {
                this.targetDetectModelListener.onTargetDetectModel((String)null);
            }

        } else {
            if (null != this.targetDetectModelListener) {
                this.targetDetectModelListener.onTargetDetectModel((String)null);
            }

        }
    };

    public ourGDUVision() {
        this.mGduCommunication3.addCycleACKCB(16908309, this.targetDetectCallBack);
        this.mGduCommunication3.addCycleACKCB(49283093, this.targetDetectCallBack);
        this.mGduCommunication3.addCycleACKCB(33555224, this.targetDetectCallBack);
        this.mGduCommunication3.addCycleACKCB(42205205, this.targetDetectCallBack);
        this.mGduCommunication3.addCycleACKCB(16908342, this.targetDetectCallBack);
        this.mGduCommunication3.addCycleACKCB(42205240, this.targetDetectCallBack);
        this.mGduCommunication3.addCycleACKCB(16908344, this.targetDetectCallBack);
        this.mGduCommunication3.addCycleACKCB(16908304, this.videoTrackCallback);
        this.mGduCommunication3.addCycleACKCB(49283088, this.videoTrackCallback);
        this.mGduCommunication3.addCycleACKCB(16908305, this.multipleTargetTrackCb);
        this.mGduCommunication3.addCycleACKCB(49283089, this.multipleTargetTrackCb);
        this.mGduCommunication3.addCycleACKCB(42205201, this.multipleTargetTrackCb);
        this.mGduCommunication3.addCycleACKCB(42205239, this.targetDetectModelsCallback);
    }

    public void setOnTargetDetectListener(OnOurTargetDetectListener listener) {
        this.targetDetectListener = listener;
        this.aiBoxManager.setOnTargetDetectListener(listener);
    }

    public void setOnTargetTrackListener(OnTargetTrackListener listener) {
        this.targetTrackListener = listener;
    }

    public void setFrameSize(VideoSizeMode mode, int width, int height) {
        this.aiBoxManager.setFrameSize(mode, width, height);
    }

    private void getMultiDetectTargetNew(byte[] content) {
        int length = content.length;
        if (length >= 3) {
            byte packageInfo = content[length - 3];
            byte totalNum = (byte)(packageInfo >> 4 & 255);
            byte currentNum = (byte)(packageInfo & 15);
            int targetNum = (length - 4) / 10;
            List<TargetMode> targetModes = new CopyOnWriteArrayList();

            for(int i = 0; i < targetNum; ++i) {
                byte[] targetBytes = new byte[10];

                for(int j = 0; j < 10; ++j) {
                    targetBytes[j] = content[1 + i * 10 + j];
                }

                TargetMode targetMode = this.parseMultiTargetModeNew(targetBytes);
                targetModes.add(targetMode);
            }

            if (this.mTargetModeList != null) {
                if (currentNum == 0) {
                    this.mTargetModeList.clear();
                }

                this.mTargetModeList.addAll(targetModes);
                if (currentNum == totalNum - 1 && this.targetTrackListener != null) {
                    this.targetTrackListener.onTargetDetecting(this.mTargetModeList);
                }
            }

        }
    }

    private TargetMode parseMultiTargetModeNew(byte[] content) {
        TargetMode targetMode = new TargetMode();
        short rectX = (short)((int)((double)((content[0] & 255) * 1920) / 255.0));
        short rectY = (short)((int)((double)((content[1] & 255) * 1080) / 255.0));
        short width = (short)((int)((double)((content[2] & 255) * 1920) / 255.0));
        short height = (short)((int)((double)((content[3] & 255) * 1080) / 255.0));
        byte targetType = content[4];
        byte targetConfidence = content[5];
        short targetId = ByteUtilsLowBefore.byte2short(content, 6);
        short pointX = (short)((int)((double)((content[8] & 255) * 1920) / 255.0));
        short pointY = (short)((int)((double)((content[9] & 255) * 1080) / 255.0));
        targetMode.setHeight(height);
        targetMode.setWidth(width);
        targetMode.setLeftX(rectX);
        targetMode.setLeftY(rectY);
        targetMode.setTargetCenterPointX(pointX);
        targetMode.setTargetCenterPointY(pointY);
        targetMode.setTargetType((short)targetType);
        targetMode.setTargetConfidence((short)targetConfidence);
        targetMode.setId(targetId);
        targetMode.setType(1);
        return targetMode;
    }

    private void getTrackResult(byte state, short pointX, short pointY, short width, short height) {
        List<Short> points = null;
        switch (state) {
            case 0:
            case 3:
            case 4:
            case 9:
                if (this.targetTrackListener != null) {
                    TargetMode targetMode = new TargetMode();
                    targetMode.setHeight(height);
                    targetMode.setWidth(width);
                    targetMode.setLeftX(pointX);
                    targetMode.setLeftY(pointY);
                    this.targetTrackListener.onTargetTracking(targetMode);
                }
                break;
            case 1:
                if (this.targetTrackListener != null) {
                    this.targetTrackListener.onTargetTrackStop();
                }
            case 2:
            case 5:
            case 6:
            case 8:
            default:
                break;
            case 7:
                if (this.targetTrackListener != null) {
                    this.targetTrackListener.onTargetTrackModelClose();
                }
        }

    }

    public void startTargetDetect(CommonCallbacks.CompletionCallback callback) {
        this.targetDetect((byte)1, (short)0, (short)0, (short)0, (short)0, (byte)0, (byte)0, callback);
    }

    public void startTargetDetect(byte lightType, CommonCallbacks.CompletionCallback callback) {
        this.targetDetect((byte)1, (short)0, (short)0, (short)0, (short)0, (byte)0, lightType, callback);
    }

    public void targetDetect(byte detectType, short leftX, short leftY, short width, short height, byte detectMethod, byte lightType, CommonCallbacks.CompletionCallback callback) {
        this.mGduCommunication3.targetDetect(detectType, leftX, leftY, width, height, detectMethod, lightType, (code, bean) -> {
            if (callback != null) {
                if (code == 0) {
                    callback.onResult((GDUError)null);
                } else {
                    callback.onResult(GDUError.COMMON_TIMEOUT);
                }
            }

        });
    }

    public void stopTargetDetect(CommonCallbacks.CompletionCallback callback) {
        this.mGduCommunication3.targetDetect((byte)2, (short)0, (short)0, (short)0, (short)0, (byte)0, (byte)0, (code, bean) -> {
            if (callback != null) {
                if (code == 0) {
                    callback.onResult((GDUError)null);
                    if (this.targetDetectListener != null) {
                        this.targetDetectListener.onTargetDetectFinished();
                    }
                } else {
                    callback.onResult(GDUError.COMMON_TIMEOUT);
                }
            }

        });
    }

    public void stopTargetDetect(byte lightType, CommonCallbacks.CompletionCallback callback) {
        this.mGduCommunication3.targetDetect((byte)2, (short)0, (short)0, (short)0, (short)0, (byte)0, lightType, (code, bean) -> {
            if (callback != null) {
                if (code == 0) {
                    callback.onResult((GDUError)null);
                    if (this.targetDetectListener != null) {
                        this.targetDetectListener.onTargetDetectFinished();
                    }
                } else {
                    callback.onResult(GDUError.COMMON_TIMEOUT);
                }
            }

        });
    }

    public void startSmartTrack(CommonCallbacks.CompletionCallback callback) {
        this.mGduCommunication3.videoTrackOrSurrondALG((byte)1, (byte)0, (short)0, (short)0, (short)0, (short)0, (short)0, (byte)0, (code, bean) -> {
            if (callback != null) {
                if (code == 0) {
                    callback.onResult((GDUError)null);
                    if (this.targetTrackListener != null) {
                        this.targetTrackListener.onTargetTrackStart();
                    }
                } else {
                    callback.onResult(GDUError.COMMON_TIMEOUT);
                }
            }

        });
    }

    public void detectTarget(byte selectedType, short targetId, int pointX, int pointY, int pointX1, int pointY1, byte targetType, CommonCallbacks.CompletionCallback callback) {
        Log.i("detectTarget", "detectTarget() targetType = " + selectedType + ", targetId = " + targetId + ", pointX = " + pointX + "; pointY = " + pointY + "; pointX1 = " + pointX1 + "; pointY1 = " + pointY1);
        if (selectedType == 0) {
            List<Short> data = RectUtil.screenPoint2VideoArg(pointX, pointX1, pointY, pointY1);
            pointX = (Short)data.get(0);
            pointY = (Short)data.get(1);
            pointX1 = (Short)data.get(2);
            pointY1 = (Short)data.get(3);
        }

        this.mGduCommunication3.videoTrackOrSurrondALG((byte)3, selectedType, targetId, (short)pointX, (short)pointY, (short)pointX1, (short)pointY1, targetType, (code, bean) -> {
            if (callback != null) {
                if (code == 0) {
                    callback.onResult((GDUError)null);
                } else {
                    callback.onResult(GDUError.COMMON_TIMEOUT);
                }
            }

        });
    }

    public void stopSmartTrack(CommonCallbacks.CompletionCallback callback) {
        this.mGduCommunication3.videoTrackOrSurrondALG((byte)4, (byte)0, (short)0, (short)0, (short)0, (short)0, (short)0, (byte)0, (code, bean) -> {
            if (callback != null) {
                if (code == 0) {
                    callback.onResult((GDUError)null);
                } else {
                    callback.onResult(GDUError.COMMON_TIMEOUT);
                }
            }

        });
    }

    public void cancelSmartTrack(CommonCallbacks.CompletionCallback callback) {
        this.mGduCommunication3.videoTrackOrSurrondALG((byte)2, (byte)0, (short)0, (short)0, (short)0, (short)0, (short)0, (byte)0, (code, bean) -> {
            if (callback != null) {
                if (code == 0) {
                    callback.onResult((GDUError)null);
                } else {
                    callback.onResult(GDUError.COMMON_TIMEOUT);
                }
            }

        });
    }

    public void setAIBoxTargetType(int modelId, byte detectType, int typeCount, byte[] typeState, CommonCallbacks.CompletionCallback callback) {
        this.mGduCommunication3.setAIBoxTargetType(modelId, detectType, (short)typeCount, typeState, (code, bean) -> {
            if (callback != null) {
                if (code == 0) {
                    callback.onResult((GDUError)null);
                } else {
                    callback.onResult(GDUError.COMMON_TIMEOUT);
                }
            }

        });
    }

    public void setTargetType(byte modelType, byte detectType, short typeCount, byte[] typeState, CommonCallbacks.CompletionCallback callback) {
        this.mGduCommunication3.setTargetType(modelType, detectType, typeCount, typeState, (code, bean) -> {
            if (callback != null) {
                if (code == 0) {
                    callback.onResult((GDUError)null);
                } else {
                    callback.onResult(GDUError.COMMON_TIMEOUT);
                }
            }

        });
    }

    public void setAITargetType(byte modelType, byte detectType, short typeCount, byte[] typeState, CommonCallbacks.CompletionCallback callback) {
        this.mGduCommunication3.setAITargetType(modelType, detectType, typeCount, typeState, (code, bean) -> {
            if (callback != null) {
                if (code == 0) {
                    callback.onResult((GDUError)null);
                } else {
                    callback.onResult(GDUError.COMMON_TIMEOUT);
                }
            }

        });
    }

    public void getTargetDetectModels(CommonCallbacks.CompletionCallback callback) {
        this.mGduCommunication3.getTargetDetectModels((code, bean) -> {
            if (callback != null) {
                if (code == 0) {
                    callback.onResult((GDUError)null);
                } else {
                    callback.onResult(GDUError.COMMON_TIMEOUT);
                }
            }

        });
    }

    public void setOnTargetDetectModelsListener(OnTargetDetectModelListener listener) {
        this.targetDetectModelListener = listener;
    }
}
