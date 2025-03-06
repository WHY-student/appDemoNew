package com.gdu.demo.ourgdu;
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import android.util.Log;
import com.gdu.common.error.GDUError;
import com.gdu.drone.TargetMode;
import com.gdu.sdk.util.CommonCallbacks;
import com.gdu.sdk.vision.OnTargetDetectListener;
import com.gdu.sdk.vision.OnTargetTrackListener;
import com.gdu.socket.GduCommunication3;
import com.gdu.socket.GduFrame3;
import com.gdu.socket.GduSocketManager;
import com.gdu.socket.SocketCallBack3;
import com.gdu.util.ByteUtilsLowBefore;
import com.gdu.util.RectUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ourGDUVision {
    private GduCommunication3 mGduCommunication3 = GduSocketManager.getInstance().getGduCommunication();
    private OnTargetDetectListener targetDetectListener;
    private OnTargetTrackListener targetTrackListener;
    private final List<TargetMode> mTargetModeList = new ArrayList();
    private final SocketCallBack3 targetDetectCallBack = new SocketCallBack3() {
        public void callBack(int code, GduFrame3 bean) {
            if (bean != null && bean.frameContent != null) {
                try {
                    ourGDUVision.this.getDetectTargetNew(bean.frameContent);
                } catch (Exception var4) {
                    Exception e = var4;
                    Log.i("获取监测目标出错", e.toString());
                }
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

    private final SocketCallBack3 ourDetectCallBack = new SocketCallBack3() { // from class: com.gdu.sdk.vision.GDUVision.1
        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r0v2, types: [byte[]] */
        /* JADX WARN: Type inference failed for: r0v3, types: [java.lang.Exception] */
        /* JADX WARN: Type inference failed for: r0v8, types: [com.gdu.sdk.vision.GDUVision] */
        @Override // com.gdu.gdusocket.SocketCallBack3
        public void callBack(int b, GduFrame3 gduFrame3) {
            if (gduFrame3 == null || gduFrame3.frameContent == null) {
                return;
            }

            try {
                ourGDUVision.this.ourGetDetectTargetNew(gduFrame3.frameContent);
            } catch (Exception exception) {
                Log.i("获取监测目标出错", exception.toString());
            }
        }
    };


    private TargetMode ourParseTargetModeNew(byte[] var1) {
        TargetMode var10000 = new TargetMode();
        //byte[] var10007 = var1;
        //byte[] var10008 = var1;
        //byte[] var10009 = var1;
//        byte[] var10010 = var1;

        short var7 = ByteUtilsLowBefore.byte2short(var1, 0);
        short var8 = ByteUtilsLowBefore.byte2short(var1, 2);
        short var2 = ByteUtilsLowBefore.byte2short(var1, 4);
        short var3 = ByteUtilsLowBefore.byte2short(var1, 6);
        byte var4 = var1[8];

        byte id = var1[9];
        int var5 =ByteUtilsLowBefore.byte2Int(var1,10);
        //byte var4 = var10008[8];
        //byte var5 = (byte)(var10007[9] & 1 & 255);
        var10000.setHeight(var3);
        var10000.setWidth(var2);
        var10000.setLeftX(var7);
        var10000.setLeftY(var8);
        var10000.setTargetConfidence((short)var4);
        var10000.setFlawType(id);
        var10000.setId(var5);
        return var10000;
    }

    private void ourGetDetectTargetNew(byte[] var1) {
        //int var2;

        if(var1.length>38){
//            int startIndex=38;//待修改，可以根据解析的帧xiu
            byte objectLength = var1[36];
            CopyOnWriteArrayList boxList = new CopyOnWriteArrayList();

            for(int index = 0; index < objectLength; ++index) {
//                byte[] var7 = Arrays.copyOfRange(targetBox,var6*14,var6*14+14);
                byte[] boxBytes = new byte[14];

                for(int var8 = 0; var8 < 14; ++var8) {
                    boxBytes[var8] = var1[index * 14 + var8 + 38];
                }

                boxList.add(this.ourParseTargetModeNew(boxBytes));
            }


            List var10;
//        byte nowBox = 0;
            if ((var10 = this.mTargetModeList) != null) {

                var10.clear();
                this.mTargetModeList.addAll(boxList);
            }

            OnTargetDetectListener var9;
            if ((var9 = this.targetDetectListener) != null) {
                var9.onTargetDetecting(boxList);
            }

        }
    }

    public ourGDUVision() {
        this.mGduCommunication3.addCycleACKCB(42205240, this.ourDetectCallBack);

//        this.mGduCommunication3.addCycleACKCB(16908309, this.targetDetectCallBack);
//        this.mGduCommunication3.addCycleACKCB(49283093, this.targetDetectCallBack);
//        this.mGduCommunication3.addCycleACKCB(33555224, this.targetDetectCallBack);
//        this.mGduCommunication3.addCycleACKCB(16908304, this.videoTrackCallback);
//        this.mGduCommunication3.addCycleACKCB(49283088, this.videoTrackCallback);
//        this.mGduCommunication3.addCycleACKCB(16908305, this.multipleTargetTrackCb);
//        this.mGduCommunication3.addCycleACKCB(49283089, this.multipleTargetTrackCb);
    }

    public void setOnTargetDetectListener(OnTargetDetectListener listener) {
        this.targetDetectListener = listener;
    }

    public void setOnTargetTrackListener(OnTargetTrackListener listener) {
        this.targetTrackListener = listener;
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
        this.mGduCommunication3.targetDetect((byte)1, (short)0, (short)0, (short)0, (short)0, (byte)0, (byte)0, (code, bean) -> {
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
}
