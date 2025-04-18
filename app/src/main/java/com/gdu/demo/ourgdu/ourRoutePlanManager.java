package com.gdu.demo.ourgdu;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.gdu.api.RoutePlanning.CreatPaseXmlTool;
import com.gdu.api.RoutePlanning.EnumPointAction;
import com.gdu.api.RoutePlanning.EnumRoutePlanningErrStatus;
import com.gdu.api.RoutePlanning.EnumRoutePlanningOrder;
import com.gdu.api.RoutePlanning.EnumRoutePlanningRunningStatus;
import com.gdu.api.RoutePlanning.EnumRoutePlanningTaskStatus;
import com.gdu.api.RoutePlanning.KmlParseTool;
import com.gdu.api.RoutePlanning.OnRouteCmdListener;
import com.gdu.api.RoutePlanning.OnRoutePlanListener;
import com.gdu.api.RoutePlanning.RouteCommonParamBean;
import com.gdu.api.RoutePlanning.RoutePlanBean;
import com.gdu.api.RoutePlanning.RoutePlanPoint;
import com.gdu.api.Util.ToolManager;
import com.gdu.config.ConnStateEnum;
import com.gdu.config.GduConfig;
import com.gdu.config.GlobalVariable;
import com.gdu.drone.DroneException;
import com.gdu.drone.FlyingState;
import com.gdu.sdk.filetransfer.ErrorStatus;
import com.gdu.sdk.filetransfer.UploadFileTool;
import com.gdu.sdk.filetransfer.UploadListener;
import com.gdu.socket.GduCommunication3;
import com.gdu.socket.GduFrame3;
import com.gdu.socket.SocketCallBack3;
import com.gdu.util.ByteUtilsLowBefore;
import com.gdu.util.MD5Util;
import com.gdu.util.MathUtils;
import com.gdu.util.TimeUtil;
import com.gdu.util.compress.CompressUtil;
import com.gdu.util.logs.RonLog;
import com.gdu.util.logs.RonLog2File;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class ourRoutePlanManager {
    private final String PLAN_XML = "routePlan.xml";
    private final String PLAN_XML_PATH;
    private final String PLAN_XML_DIR;
    private final String REMOTE_DIR;
    private final String FILE_TYPE;
    private InputStream in;
    private PrintWriter pw;
    private CreatPaseXmlTool creatPaseXmlTool;
    private KmlParseTool mKmlParseTool;
    private UploadFileTool mUploadFileTool;
    private String mCurrentTaskXmlPath;
    private String mCurrentTaskNum;
    private int mLastReturnNum;
    private boolean isUploadFinished;
    private boolean isTaskFinished;
    private EnumRoutePlanningRunningStatus mCurrentRouteStatus;
    private EnumRoutePlanningRunningStatus mLastReturnRouteStatus;
    private boolean isSendStopWaypoint;
    private OnRoutePlanListener onRoutePlanListener;
    private GduCommunication3 gduCommunication;
    private SocketCallBack3 runningStatus;
    private OnRouteCmdListener onRouteCmdListener;
    private Handler handler;
    private byte upgradeState;
    private final int OK;
    private final int FAILED;
    private final int UPLOAD_PLAN_XML;
    private final int CHECK_JSON_STATE;
    private final int UPLOAD_SUCCESS;
    private final int UPLOAD_FAILED;
    private final int TAKE_OFF_SEND_SUCCESS;
    private final int TAKE_OFF_SEND_FAILED;
    private final int FLY_RADIUS_ERROR;
    private final int COURSE_OVERLAP_TOO_BIG;
    private final int COURSE_OVERLAP_TOO_SMALL;
    private final int MAX_FLY_ANGLE_TOO_SMALL;
    private SocketCallBack3 sdroneFWUpdateCallback;

    public void setOnRoutePlanListener(OnRoutePlanListener onRoutePlanListener) {
        this.onRoutePlanListener = onRoutePlanListener;
    }

    public ourRoutePlanManager(GduCommunication3 gduCommunication) {
        this.PLAN_XML_PATH = GduConfig.BaseDirectory + "/RoutePlan/" + "routePlan.xml";
        this.PLAN_XML_DIR = GduConfig.BaseDirectory + "/RoutePlan/";
        this.REMOTE_DIR = "/home/gdu-tech/file/mission/";
        this.FILE_TYPE = "file/mcu";
        this.in = null;
        this.runningStatus = new SocketCallBack3() {
            public void callBack(int code, GduFrame3 bean) {
                Log.d("testPlane","测试加载任务");
                if (bean != null && bean.frameContent != null && bean.frameContent.length >= 17) {
                    if (com.gdu.demo.ourgdu.ourRoutePlanManager.this.onRoutePlanListener != null) {
                        byte[] taskNum = new byte[14];
                        int index = 0;
                        System.arraycopy(bean.frameContent, 0, taskNum, 0, 14);
                        index += 14;
                        String taskName = new String(taskNum);
                        com.gdu.demo.ourgdu.ourRoutePlanManager.this.mCurrentTaskNum = taskName;
                        EnumRoutePlanningRunningStatus status = EnumRoutePlanningRunningStatus.RUNNING;
                        byte st = bean.frameContent[index];
                        ++index;
                        switch (st) {
                            case 2:
                                status = EnumRoutePlanningRunningStatus.RUNNING;
                                com.gdu.demo.ourgdu.ourRoutePlanManager.this.isTaskFinished = false;
                                break;
                            case 3:
                                if (com.gdu.demo.ourgdu.ourRoutePlanManager.this.isSendStopWaypoint) {
                                    status = EnumRoutePlanningRunningStatus.FINISH;
                                } else {
                                    status = EnumRoutePlanningRunningStatus.PAUSE;
                                }
                                break;
                            case 4:
                                status = EnumRoutePlanningRunningStatus.FINISH;
                                break;
                            case 5:
                                status = EnumRoutePlanningRunningStatus.EXITBYUSER;
                                break;
                            case 6:
                                status = EnumRoutePlanningRunningStatus.FAIL;
                                break;
                            case 7:
                                status = EnumRoutePlanningRunningStatus.FINISH;
                        }

                        com.gdu.demo.ourgdu.ourRoutePlanManager.this.mCurrentRouteStatus = status;
                        short currentNum = 0;
                        if (bean.frameContent.length > 3) {
                            currentNum = ByteUtilsLowBefore.byte2short(bean.frameContent, index);
                            --currentNum;
                            if (currentNum < 0) {
                                currentNum = 0;
                            }

                            index += 2;
                        }

                        RonLog.LogD(new String[]{"test runningStatus callBack() taskName " + taskName + "; status = " + st + "; currentNum = " + currentNum});
                        EnumRoutePlanningTaskStatus taskStatus = EnumRoutePlanningTaskStatus.TASK_NONE;
                        if (bean.frameContent.length > 4) {
                            switch (bean.frameContent[index]) {
                                case 1:
                                    taskStatus = EnumRoutePlanningTaskStatus.TASK_HOVERING;
                                    break;
                                default:
                                    taskStatus = EnumRoutePlanningTaskStatus.TASK_NONE;
                            }
                        }

                        if (status == EnumRoutePlanningRunningStatus.FINISH && !com.gdu.demo.ourgdu.ourRoutePlanManager.this.isTaskFinished) {
                            if (com.gdu.demo.ourgdu.ourRoutePlanManager.this.mLastReturnNum != currentNum) {
                                com.gdu.demo.ourgdu.ourRoutePlanManager.this.mLastReturnNum = currentNum;
                                if (currentNum != 0 && !com.gdu.demo.ourgdu.ourRoutePlanManager.this.isTaskFinished) {
                                    com.gdu.demo.ourgdu.ourRoutePlanManager.this.onRoutePlanListener.onRoutePlanningRunningStatus(EnumRoutePlanningRunningStatus.ARRIVE_POINT, currentNum, taskStatus);
                                }
                            }

                            com.gdu.demo.ourgdu.ourRoutePlanManager.this.isTaskFinished = true;
                            com.gdu.demo.ourgdu.ourRoutePlanManager.this.onRoutePlanListener.onRoutePlanningRunningStatus(status, currentNum, taskStatus);
                        } else if (com.gdu.demo.ourgdu.ourRoutePlanManager.this.mLastReturnNum != currentNum) {
                            com.gdu.demo.ourgdu.ourRoutePlanManager.this.mLastReturnNum = currentNum;
                            if (currentNum != 0 && !com.gdu.demo.ourgdu.ourRoutePlanManager.this.isTaskFinished) {
                                com.gdu.demo.ourgdu.ourRoutePlanManager.this.onRoutePlanListener.onRoutePlanningRunningStatus(EnumRoutePlanningRunningStatus.ARRIVE_POINT, currentNum, taskStatus);
                            }
                        } else if (com.gdu.demo.ourgdu.ourRoutePlanManager.this.mCurrentRouteStatus != EnumRoutePlanningRunningStatus.FINISH) {
                            if (com.gdu.demo.ourgdu.ourRoutePlanManager.this.mCurrentRouteStatus != EnumRoutePlanningRunningStatus.RUNNING) {
                                if (com.gdu.demo.ourgdu.ourRoutePlanManager.this.mLastReturnRouteStatus != com.gdu.demo.ourgdu.ourRoutePlanManager.this.mCurrentRouteStatus) {
                                    com.gdu.demo.ourgdu.ourRoutePlanManager.this.onRoutePlanListener.onRoutePlanningRunningStatus(com.gdu.demo.ourgdu.ourRoutePlanManager.this.mCurrentRouteStatus, currentNum, taskStatus);
                                }
                            } else {
                                com.gdu.demo.ourgdu.ourRoutePlanManager.this.onRoutePlanListener.onRoutePlanningRunningStatus(com.gdu.demo.ourgdu.ourRoutePlanManager.this.mCurrentRouteStatus, currentNum, taskStatus);
                            }

                            com.gdu.demo.ourgdu.ourRoutePlanManager.this.mLastReturnRouteStatus = com.gdu.demo.ourgdu.ourRoutePlanManager.this.mCurrentRouteStatus;
                        }

                        if (status != EnumRoutePlanningRunningStatus.FINISH && status != EnumRoutePlanningRunningStatus.EXITBYUSER) {
                            if (status == EnumRoutePlanningRunningStatus.FAIL) {
                                com.gdu.demo.ourgdu.ourRoutePlanManager.this.sendTaskStartCmd(EnumRoutePlanningOrder.TASK_END, taskName, com.gdu.demo.ourgdu.ourRoutePlanManager.this.onRouteCmdListener);
                                com.gdu.demo.ourgdu.ourRoutePlanManager.this.onRoutePlanListener.OnRoutePlanningExit(EnumRoutePlanningErrStatus.DroneExecuteFail);
                            }
                        } else {
                            com.gdu.demo.ourgdu.ourRoutePlanManager.this.sendTaskStartCmd(EnumRoutePlanningOrder.TASK_END, taskName,com.gdu.demo.ourgdu.ourRoutePlanManager.this.onRouteCmdListener);
                            com.gdu.demo.ourgdu.ourRoutePlanManager.this.onRoutePlanListener.OnRoutePlanningExit(EnumRoutePlanningErrStatus.RoutePlanningPerform);
                        }
                    }

                }
            }
        };
        this.onRouteCmdListener = new OnRouteCmdListener() {
            public void setSuccess(boolean isSuccess, Object object) {
            }
        };
        this.handler = new Handler(new Handler.Callback() {
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                    case 4:
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    default:
                        break;
                    case 2:
                    case 6:
                        com.gdu.demo.ourgdu.ourRoutePlanManager.this.isUploadFinished = false;
                        com.gdu.demo.ourgdu.ourRoutePlanManager.this.routePlanningErr(EnumRoutePlanningErrStatus.sendRoutePlanning2DroneFail);
                        break;
                    case 3:
                        Log.d("handleMessage", "handleMessage: 3");
                        com.gdu.demo.ourgdu.ourRoutePlanManager.this.UploadFileToPlane((Boolean)msg.obj);
                        break;
                    case 5:
                        Log.d("handleMessage", "handleMessage: 5");
                        if (com.gdu.demo.ourgdu.ourRoutePlanManager.this.onRoutePlanListener != null) {
                            com.gdu.demo.ourgdu.ourRoutePlanManager.this.isUploadFinished = true;
                            com.gdu.demo.ourgdu.ourRoutePlanManager.this.onRoutePlanListener.sendRoutePlane2DroneSuccess(true);
                        }
                }

                return true;
            }
        });
        this.upgradeState = -1;
        this.OK = 1;
        this.FAILED = 2;
        this.UPLOAD_PLAN_XML = 3;
        this.CHECK_JSON_STATE = 4;
        this.UPLOAD_SUCCESS = 5;
        this.UPLOAD_FAILED = 6;
        this.TAKE_OFF_SEND_SUCCESS = 7;
        this.TAKE_OFF_SEND_FAILED = 8;
        this.FLY_RADIUS_ERROR = 9;
        this.COURSE_OVERLAP_TOO_BIG = 10;
        this.COURSE_OVERLAP_TOO_SMALL = 11;
        this.MAX_FLY_ANGLE_TOO_SMALL = 12;
        this.sdroneFWUpdateCallback = new SocketCallBack3() {
            public void callBack(int code, GduFrame3 bean) {
                if (com.gdu.demo.ourgdu.ourRoutePlanManager.this.onRoutePlanListener != null) {
                    com.gdu.demo.ourgdu.ourRoutePlanManager.this.onRoutePlanListener.onMsgLog("上传文件的反馈结果:" + bean.frameContent[0] + "," + bean.frameContent[3]);
                }

                com.gdu.demo.ourgdu.ourRoutePlanManager.this.upgradeState = bean.frameContent[0];
                com.gdu.demo.ourgdu.ourRoutePlanManager.this.mUploadFileTool.callback(bean);
            }
        };
        this.creatPaseXmlTool = new CreatPaseXmlTool();
        this.mKmlParseTool = new KmlParseTool();
        this.gduCommunication = gduCommunication;
        this.mCurrentRouteStatus = EnumRoutePlanningRunningStatus.FINISH;
        this.mUploadFileTool = new UploadFileTool(new UploadListener() {
            public void error(ErrorStatus status) {
                RonLog.LogD(new String[]{"mUploadFileTool error()"});
                com.gdu.demo.ourgdu.ourRoutePlanManager.this.handler.sendEmptyMessage(6);
            }

            public void uploadProgress(float progress) {
                Log.d("uploadProgress: ", "mUploadFileTool uploadProgress() progress = " + progress);
//                RonLog.LogD(new String[]{"mUploadFileTool uploadProgress() progress = " + progress});
                if (com.gdu.demo.ourgdu.ourRoutePlanManager.this.onRoutePlanListener != null) {
                    com.gdu.demo.ourgdu.ourRoutePlanManager.this.onRoutePlanListener.progressOfSendRoutePlanning2Drone(progress);
                }

            }

            public void success() {
                Log.d("uploadProgress: ", "mUploadFileTool uploadProgress() progress = success");
                RonLog.LogD(new String[]{"mUploadFileTool success()"});
                if (com.gdu.demo.ourgdu.ourRoutePlanManager.this.onRoutePlanListener != null) {
                    com.gdu.demo.ourgdu.ourRoutePlanManager.this.onRoutePlanListener.sendRoutePlane2DroneSuccess(true);
                }

            }
        });
    }

    public RoutePlanBean getPlanBeanFromXML() {
        try {
            File xmlFile = new File(this.PLAN_XML_PATH);
            this.in = new FileInputStream(xmlFile);
            RoutePlanBean result = this.getPlanBeanFromXML(this.PLAN_XML_PATH);
            return result;
        } catch (IOException var3) {
            IOException e = var3;
            e.printStackTrace();
            return null;
        }
    }

    public RoutePlanBean getPlanBeanFromXML(String filePath) {
        try {
            RoutePlanBean routePlanBean = null;
            if (filePath.endsWith(".xml")) {
                routePlanBean = this.creatPaseXmlTool.parseXMLWithPull(filePath);
            }

            return routePlanBean;
        } catch (Exception var3) {
            Exception e = var3;
            e.printStackTrace();
            return null;
        }
    }

    private boolean checkConnStatus() {
        if (GlobalVariable.connStateEnum != ConnStateEnum.Conn_Sucess) {
            this.routePlanningErr(EnumRoutePlanningErrStatus.ConnDroneErr);
            return false;
        } else {
            return true;
        }
    }

    private void routePlanningErr(EnumRoutePlanningErrStatus status) {
        if (this.onRoutePlanListener != null) {
            switch (status) {
                case createRoutePlanningFail:
                    this.onRoutePlanListener.createRoutePlanSuccess(false, "2");
                    break;
                case sendRoutePlanning2DroneFail:
                    this.onRoutePlanListener.sendRoutePlane2DroneSuccess(false);
            }

            this.onRoutePlanListener.OnRoutePlanningExit(status);
        }

    }

    public void updateSeniorPlanning2Drone(List<RoutePlanPoint> wayPointsList) {
        if (this.routeNumCheck(wayPointsList)) {
            if (this.statusCheck()) {
                this.isUploadFinished = false;
                this.updateSeniorPlanning2Drone(wayPointsList, 1, 2, 1);
            }
        }
    }

    public boolean routeNumCheck(List<RoutePlanPoint> wayPointsList) {
        if (wayPointsList == null) {
            if (this.onRoutePlanListener != null) {
                this.onRoutePlanListener.OnRoutePlanningExit(EnumRoutePlanningErrStatus.createRoutePlanningFail);
            }

            return false;
        } else {
            return true;
        }
    }

    public boolean statusCheck() {
        if (this.mCurrentRouteStatus != EnumRoutePlanningRunningStatus.RUNNING && this.mCurrentRouteStatus != EnumRoutePlanningRunningStatus.ARRIVE_POINT) {
            return true;
        } else {
            if (this.onRoutePlanListener != null) {
                this.onRoutePlanListener.createRoutePlanSuccess(false, "1");
            }

            return false;
        }
    }

    public void updateSeniorPlanning2Drone(List<RoutePlanPoint> wayPointsList, int altitudeMode) {
        if (this.routeNumCheck(wayPointsList)) {
            if (this.statusCheck()) {
                this.isUploadFinished = false;
                this.updateSeniorPlanning2Drone(wayPointsList, 1, 2, altitudeMode);
            }
        }
    }

    public void uploadSeniorPlanningKML2Drone(String path) {
        RoutePlanBean routePlanBean = this.mKmlParseTool.parseKMLWithPull(path);
        this.updateSeniorPlanning2Drone(routePlanBean);
    }

    private boolean checkRoutePlan(RoutePlanBean routePlanBean) {
        if (routePlanBean == null) {
            return false;
        } else if (routePlanBean.getProject() == null) {
            return false;
        } else if (routePlanBean.getWayPoint() == null) {
            return false;
        } else if (routePlanBean.getTask() == null) {
            return false;
        } else if (routePlanBean.getWayPoint().getWaypointsBean() == null) {
            return false;
        } else if (routePlanBean.getWayPoint().getWaypointsBean().getPlacemark() == null) {
            return false;
        } else {
            Iterator var2 = routePlanBean.getWayPoint().getWaypointsBean().getPlacemark().iterator();

            RoutePlanBean.PlacemarkBean bean;
            do {
                if (!var2.hasNext()) {
                    return true;
                }

                bean = (RoutePlanBean.PlacemarkBean)var2.next();
                bean.getActionCount();
            } while(bean.getActionsbean() != null && bean.getActionsbean().getSubAction() != null);

            return false;
        }
    }

    public void updateSeniorPlanning2Drone(RoutePlanBean routePlanBean) {
        this.mLastReturnNum = -1;
        if (this.onRoutePlanListener != null) {
            this.onRoutePlanListener.beginCreateRoutePlan();
        }

        if (this.gduCommunication != null) {
            Log.d("updateSeniorPlanning2Drone", "updateSeniorPlanning2Drone: True");
            this.gduCommunication.addCycleACKCB(17694730, this.runningStatus);
        }

        boolean isSuccess = this.checkRoutePlan(routePlanBean);
        if (!isSuccess) {
            this.routePlanningErr(EnumRoutePlanningErrStatus.createRoutePlanningFail);
        } else {
            Log.d("PlanTask", "PlanTask: True");
            (new com.gdu.demo.ourgdu.ourRoutePlanManager.PlanTask()).execute(new RoutePlanBean[]{routePlanBean});
        }
    }

    public void uploadXML2Drone(String name) {
        if (this.statusCheck()) {
            this.isUploadFinished = false;
            this.mLastReturnNum = -1;
            if (this.onRoutePlanListener != null) {
                this.onRoutePlanListener.beginCreateRoutePlan();
            }

            if (this.gduCommunication != null) {
                this.gduCommunication.addCycleACKCB(17694730, this.runningStatus);
            }

            (new com.gdu.demo.ourgdu.ourRoutePlanManager.PlanXMLTask()).execute(new String[]{name});
        }
    }

    public void updateSeniorPlanning2Drone(List<RoutePlanPoint> wayPointsList, int lostOrder, int endOrder, int altitudeMode) {
        RouteCommonParamBean routeCommonParamBean = new RouteCommonParamBean();
        routeCommonParamBean.setAltitudeMode(altitudeMode);
        routeCommonParamBean.setEndOrder(endOrder);
        routeCommonParamBean.setLostOrder(lostOrder);
        routeCommonParamBean.setTurnType(0);
        this.updateSeniorPlanning2Drone(wayPointsList, routeCommonParamBean);
    }

    public void updateSeniorPlanning2Drone(List<RoutePlanPoint> wayPointsList, RouteCommonParamBean routeCommonParamBean) {
        RoutePlanBean result = new RoutePlanBean();
        RoutePlanBean.TaskBean taskBeanDefault = new RoutePlanBean.TaskBean();
        result.setTask(taskBeanDefault);
        List<RoutePlanBean.PlacemarkBean> placemarkList = new ArrayList();
        int length = wayPointsList.size();

        for(int i = 0; i < length; ++i) {
            RoutePlanPoint point = (RoutePlanPoint)wayPointsList.get(i);
            point.turningPoint = 1;
            this.speedControl(point);
            RoutePlanBean.PlacemarkBean placemarkBean = new RoutePlanBean.PlacemarkBean(i + 1, point.longitude + "", point.latitude + "", String.valueOf(point.height), point.gimbalAngle, point.speed, point.turningPoint, (int)point.verticalUpSpeed, (int)point.verticalDownSpeed, point.droneHeadAngle, point.getWpType());
            RoutePlanBean.actionsBean act = new RoutePlanBean.actionsBean();
            if (point.actions == null) {
                point.actions = new ArrayList();
            }

            act.setSubAction(point.actions);
            placemarkBean.setActionsbean(act);
            placemarkList.add(placemarkBean);
        }

        float shootPhotoTimeInterval = 0.0F;
        float shootPhotoDistanceInterval = 0.0F;
        Iterator var13 = wayPointsList.iterator();

        while(var13.hasNext()) {
            RoutePlanPoint routePlanPoint = (RoutePlanPoint)var13.next();
            if (routePlanPoint.shootPhotoTimeInterval > 0.0F) {
                shootPhotoTimeInterval = routePlanPoint.shootPhotoTimeInterval;
                break;
            }

            if (routePlanPoint.shootPhotoDistanceInterval > 0.0F) {
                shootPhotoDistanceInterval = routePlanPoint.shootPhotoDistanceInterval;
                break;
            }
        }

        if (shootPhotoTimeInterval > 0.0F) {
            result.getProject().setTimerAction(1);
            result.getProject().setTimerValue((int)shootPhotoTimeInterval);
        } else if (shootPhotoDistanceInterval > 0.0F) {
            result.getProject().setTimerAction(2);
            result.getProject().setTimerValue((int)shootPhotoDistanceInterval);
        } else {
            result.getProject().setTimerAction(0);
        }

        result.getWayPoint().getWaypointsBean().setPlacemark(placemarkList);
        result.getProject().setEndOrder(routeCommonParamBean.getEndOrder());
        result.getProject().setAirNums(wayPointsList.size());
        result.getProject().setReturnHome(routeCommonParamBean.getLostOrder());
        result.getProject().setAltitudeMode(routeCommonParamBean.getAltitudeMode());
        result.getProject().setTurnType(routeCommonParamBean.getTurnType());
        result.getProject().setPath(TimeUtil.getCurrentTimeStr());
        result.getProject().setProjectName(TimeUtil.getCurrentTimeStr());
        result.getProject().setStartDirection(1);
        this.updateSeniorPlanning2Drone(result);
    }

    private void speedControl(RoutePlanPoint point) {
        if (point.speed < 1.0) {
            point.speed = 1.0;
        } else if (point.speed > 18.0) {
            point.speed = 18.0;
        }

        if (point.verticalDownSpeed < 1.0F) {
            point.verticalDownSpeed = 1.0F;
        } else if (point.verticalDownSpeed > 3.0F) {
            point.verticalDownSpeed = 3.0F;
        }

        if (point.verticalUpSpeed < 1.0F) {
            point.verticalUpSpeed = 1.0F;
        } else if (point.verticalUpSpeed > 5.0F) {
            point.verticalUpSpeed = 5.0F;
        }

    }

    private List<RoutePlanBean.subActionBean> actionAddDelay(List<RoutePlanBean.subActionBean> subActionBeans) {
        List<RoutePlanBean.subActionBean> actionBeans = new ArrayList();
        Iterator var3 = subActionBeans.iterator();

        while(var3.hasNext()) {
            RoutePlanBean.subActionBean subActionBean = (RoutePlanBean.subActionBean)var3.next();
            actionBeans.add(subActionBean);
            if (subActionBean.getName().equals("gimbalAngle")) {
                actionBeans.add(new RoutePlanBean.subActionBean(EnumPointAction.Delay, "1"));
            }

            if (subActionBean.getName().equals("rotation")) {
                actionBeans.add(new RoutePlanBean.subActionBean(EnumPointAction.Delay, "2"));
            }

            if (subActionBean.getName().equals("takePhoto")) {
                actionBeans.add(new RoutePlanBean.subActionBean(EnumPointAction.Delay, "2"));
            }
        }

        return actionBeans;
    }

    public void updateSeniorPlanning2Drone(List<RoutePlanPoint> wayPointsList, int lostOrder, int endOrder) {
        if (this.routeNumCheck(wayPointsList)) {
            if (this.statusCheck()) {
                this.updateSeniorPlanning2Drone(wayPointsList, lostOrder, endOrder, 0);
            }
        }
    }

    private List<RoutePlanPoint> calculateRoute(List<RoutePlanPoint> gduRoutePlanPoints) {
        List<RoutePlanPoint> routePlanPoints = new ArrayList();
        int length = gduRoutePlanPoints.size();

        for(int i = 0; i < gduRoutePlanPoints.size() - 1; ++i) {
            RoutePlanPoint gduRoutePlanPoint = (RoutePlanPoint)gduRoutePlanPoints.get(i);
            RoutePlanPoint nextGduRoutePlanPoint = (RoutePlanPoint)gduRoutePlanPoints.get(i + 1);
            routePlanPoints.add(gduRoutePlanPoint);
            if (gduRoutePlanPoint.shootPhotoDistanceInterval != 0.0F || gduRoutePlanPoint.shootPhotoTimeInterval != 0.0F) {
                double distance;
                if (gduRoutePlanPoint.shootPhotoDistanceInterval != 0.0F) {
                    distance = (double)gduRoutePlanPoint.shootPhotoDistanceInterval;
                } else {
                    distance = gduRoutePlanPoint.speed * (double)gduRoutePlanPoint.shootPhotoTimeInterval;
                }

                List<RoutePlanPoint> subPointList = this.getSubRoutePlanBeanByDistance(gduRoutePlanPoint, nextGduRoutePlanPoint, distance);
                if (subPointList != null && subPointList.size() > 0) {
                    routePlanPoints.addAll(subPointList);
                }
            }
        }

        routePlanPoints.add((RoutePlanPoint)gduRoutePlanPoints.get(length - 1));
        return routePlanPoints;
    }

    private List<RoutePlanPoint> getSubRoutePlanBeanByDistance(RoutePlanPoint planPoint1, RoutePlanPoint planPoint2, double distance) {
        double allDistance = MathUtils.distanceOfTwoPoints(planPoint1.latitude, planPoint1.longitude, planPoint2.latitude, planPoint2.longitude);
        double angle = MathUtils.computeAzimuth(planPoint1.latitude, planPoint1.longitude, planPoint2.latitude, planPoint2.longitude);
        if (allDistance < distance) {
            return null;
        } else {
            double startLat = planPoint1.latitude;
            double startLng = planPoint1.longitude;
            List<RoutePlanPoint> routePlanBeans = new ArrayList();
            int num = (int)(allDistance / distance);
            if (allDistance % distance < 7.0) {
                --num;
            }

            for(int i = 1; i <= num; ++i) {
                RoutePlanPoint routePlanPoint = new RoutePlanPoint();
                if (planPoint1.actions != null && planPoint1.actions.size() > 0) {
                    routePlanPoint.actions = this.copySubAction(planPoint1.actions);
                }

                routePlanPoint.gimbalAngle = planPoint1.gimbalAngle;
                routePlanPoint.height = planPoint1.height;
                routePlanPoint.speed = planPoint1.speed;
                double[] tempLatLng = MathUtils.computerThatLonLat(startLng, startLat, angle, distance * (double)i);
                routePlanPoint.latitude = tempLatLng[0];
                routePlanPoint.longitude = tempLatLng[1];
                routePlanBeans.add(routePlanPoint);
            }

            return routePlanBeans;
        }
    }

    private List<RoutePlanBean.subActionBean> copySubAction(List<RoutePlanBean.subActionBean> tempSubActionBeans) {
        List<RoutePlanBean.subActionBean> subActionBeans = new ArrayList();
        Iterator var3 = tempSubActionBeans.iterator();

        while(var3.hasNext()) {
            RoutePlanBean.subActionBean tempSubActionBean = (RoutePlanBean.subActionBean)var3.next();
            RoutePlanBean.subActionBean subActionBean = new RoutePlanBean.subActionBean();
            subActionBean.setName(tempSubActionBean.name);
            subActionBean.setPram(tempSubActionBean.getPram());
            subActionBeans.add(subActionBean);
        }

        return subActionBeans;
    }

    private String savePlan(RoutePlanBean planBean) {
        String taskNum = this.generateTaskNum();
        planBean.getProject().setProjectName(taskNum);
        String taskXmlPath = this.PLAN_XML_DIR + taskNum + ".xml";
        File xmlFile = new File(taskXmlPath);
        if (!xmlFile.getParentFile().exists()) {
            xmlFile.getParentFile().mkdirs();
        }

        Object var6;
        try {
            xmlFile.createNewFile();
            this.pw = new PrintWriter(taskXmlPath, "utf-8");
            this.pw.printf("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            return taskNum;
        } catch (IOException var10) {
            IOException e = var10;
            e.printStackTrace();
            var6 = null;
        } finally {
            this.pw.close();
        }

        return (String)var6;
    }

    private String savePlanNew(RoutePlanBean planBean) {
        String path = this.generateTaskNum();
        String taskXmlPath = this.PLAN_XML_DIR + path + ".xml";
        File xmlFile = new File(taskXmlPath);
        if (!xmlFile.getParentFile().exists()) {
            xmlFile.getParentFile().mkdirs();
        }

        try {
            this.creatPaseXmlTool.creatXmlWithPull(taskXmlPath, planBean);
            return path;
        } catch (Exception var10) {
            Exception e = var10;
            e.printStackTrace();
            this.routePlanningErr(EnumRoutePlanningErrStatus.createRoutePlanningFail);
            Object var6 = null;
            return (String)var6;
        } finally {
            ;
        }
    }

    public void updateTask2Drone(String taskNum) {
        if (this.checkConnStatus()) {
            String xmlPath = this.PLAN_XML_DIR + taskNum + ".gz";
            File xmlFile = new File(xmlPath);
            if (!xmlFile.exists()) {
                this.routePlanningErr(EnumRoutePlanningErrStatus.RoutePlanningNotExit);
            } else {
                Log.d("updateTask2Drone", "updateTask2Drone: "+xmlPath);
                this.mCurrentTaskXmlPath = xmlPath;
                this.mCurrentTaskNum = taskNum;
                if (this.onRoutePlanListener != null) {
                    this.onRoutePlanListener.beginSendRoutePlan2Drone();
                }

                this.uploadPlanXml();
            }
        }
    }

    public void uploadTask2Drone(String taskName) {
        this.updateTask2Drone(taskName);
    }

    private void uploadPlanXml() {
        this.gduCommunication.addCycleACKCB(17891329, this.sdroneFWUpdateCallback);
        this.gduCommunication.addCycleACKCB(17891328, this.sdroneFWUpdateCallback);
        this.gduCommunication.addCycleACKCB(17891335, this.sdroneFWUpdateCallback);
        this.gduCommunication.addCycleACKCB(17891336, this.sdroneFWUpdateCallback);
        this.gduCommunication.addCycleACKCB(17891337, this.sdroneFWUpdateCallback);
        this.upgradeState = -1;
        this.handler.obtainMessage(3, true).sendToTarget();
    }

    public void takeOffAndStartTask(final OnRouteCmdListener onRouteCmdListener) {
        if (!this.flyCheck(onRouteCmdListener)) {
            RonLog2File.getSingle().saveData("test flyCheck");
        } else {
            if (this.getFlyingState() == FlyingState.Hovering) {
                this.sendTaskStartCmd(EnumRoutePlanningOrder.TASK_BEGIN, onRouteCmdListener);
            } else {
                this.gduCommunication.oneKeyFly(new SocketCallBack3() {
                    public void callBack(int code, GduFrame3 bean) {
                        RonLog2File.getSingle().saveData("test oneKeyFly code" + code);
                        if (code == 0 && onRouteCmdListener != null) {
                            com.gdu.demo.ourgdu.ourRoutePlanManager.this.doTask(onRouteCmdListener);
                        } else {
                            onRouteCmdListener.setSuccess(false, code);
                        }

                    }
                });
            }

        }
    }

    private boolean flyCheck(OnRouteCmdListener onRouteCmdListener) {
        RonLog2File.getSingle().saveData("test flyCheck 111");
        if (!this.isUploadFinished) {
            onRouteCmdListener.setSuccess(false, 530);
            return false;
        } else {
            RonLog2File.getSingle().saveData("test flyCheck 222");
            if (GlobalVariable.satellite_drone < 12) {
                onRouteCmdListener.setSuccess(false, 518);
                return false;
            } else {
                RonLog2File.getSingle().saveData("test flyCheck 333");
                if (GlobalVariable.DroneFlyMode == 1 && GlobalVariable.flyMode == 1) {
                    RonLog2File.getSingle().saveData("test flyCheck 444");
                    DroneException droneException = ToolManager.getDroneException();
                    if (droneException == null) {
                        RonLog2File.getSingle().saveData("test flyCheck 5555");
                        onRouteCmdListener.setSuccess(false, 4097);
                        return false;
                    } else if (!droneException.IMU_EXCEPTION && !droneException.BAROMETER_EXCEPTION && !droneException.MAGNETOMETER_EXCEPTION && !droneException.VISION_SYS_EXCEPTION && !droneException.SMART_BATTERY_SYS_EXCEPTION && !droneException.GPS_EXCEPTION && !droneException.OPTICAL_EXCEPTION && !droneException.ULTRASONIC_EXCEPTION && !droneException.GIMBAL_SYS_EXCEPTION) {
                        return true;
                    } else {
                        RonLog2File.getSingle().saveData("test flyCheck 6666");
                        onRouteCmdListener.setSuccess(false, 4097);
                        return false;
                    }
                } else {
                    onRouteCmdListener.setSuccess(false, 529);
                    return false;
                }
            }
        }
    }

    private void doTask(final OnRouteCmdListener onRouteCmdListener) {
        this.handler.postDelayed(new Runnable() {
            public void run() {
                RonLog2File.getSingle().saveData("test doTask");
                if (com.gdu.demo.ourgdu.ourRoutePlanManager.this.getFlyingState() != FlyingState.Hovering && com.gdu.demo.ourgdu.ourRoutePlanManager.this.getFlyingState() != FlyingState.Takeoff) {
                    onRouteCmdListener.setSuccess(false, 531);
                } else {
                    com.gdu.demo.ourgdu.ourRoutePlanManager.this.sendTaskStartCmd(EnumRoutePlanningOrder.TASK_BEGIN, onRouteCmdListener);
                }

            }
        }, 10000L);
    }

    private FlyingState getFlyingState() {
        FlyingState flyingState = FlyingState.Ground;
        switch (GlobalVariable.droneFlyState) {
            case 0:
                flyingState = FlyingState.Fling;
                break;
            case 1:
                flyingState = FlyingState.Ground;
                break;
            case 2:
                flyingState = FlyingState.Takeoff;
                break;
            case 3:
                flyingState = FlyingState.Landing;
                break;
            case 4:
                flyingState = FlyingState.Hovering;
        }

        return flyingState;
    }

    public void sendTaskStartCmd(EnumRoutePlanningOrder cmd, OnRouteCmdListener onRouteCmdListener) {
        if (this.mCurrentTaskNum == null && cmd.order_index == EnumRoutePlanningOrder.TASK_END.order_index) {
            this.mCurrentTaskNum = "00000000000000";
        }

        if (this.mCurrentTaskNum != null && !"".equals(this.mCurrentTaskNum)) {
            if (cmd == EnumRoutePlanningOrder.TASK_END) {
                this.isSendStopWaypoint = true;
            } else {
                Log.d("sendTaskStartCmd", "sendTaskStartCmd: ");
                this.isSendStopWaypoint = false;
            }

            String log = "RouteTask  " + this.mCurrentTaskNum + "," + cmd.order_index;
            RonLog2File.getSingle().saveData(log);
        }
    }

    public void sendTaskStartCmd(EnumRoutePlanningOrder cmd, String taskNum, OnRouteCmdListener onRouteCmdListener) {
        if (this.mCurrentTaskNum == null && cmd.order_index == EnumRoutePlanningOrder.TASK_END.order_index) {
            this.mCurrentTaskNum = taskNum;
        }

        if (this.mCurrentTaskNum != null && !"".equals(this.mCurrentTaskNum)) {
            if (cmd == EnumRoutePlanningOrder.TASK_END) {
                this.isSendStopWaypoint = true;
            } else {
                this.isSendStopWaypoint = false;
            }

        }
    }

    private void UploadFileToPlane(boolean isJson) {
        File xmlFile = new File(this.mCurrentTaskXmlPath);
        String xmlMd5 = MD5Util.getFileMD5(xmlFile);
        this.mUploadFileTool.startUpload(this.mCurrentTaskXmlPath, this.mCurrentTaskNum + ".gz", "/home/gdu-tech/file/mission/" + this.mCurrentTaskNum + ".gz", "file/mcu", true);
        RonLog2File.getSingle().saveData("md5=" + xmlMd5);
    }

    private String generateTaskNum() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String taskNum = formatter.format(currentTime);
        return taskNum;
    }

    public void removeListener() {
        this.onRoutePlanListener = null;
    }

    class PlanTask extends AsyncTask<RoutePlanBean, Integer, String> {
        PlanTask() {
        }

        protected String doInBackground(RoutePlanBean... routePlanBeans) {
            String taskNum = com.gdu.demo.ourgdu.ourRoutePlanManager.this.savePlanNew(routePlanBeans[0]);
            boolean isSuccess = false;
            if (!TextUtils.isEmpty(taskNum)) {
                String xmlPath = com.gdu.demo.ourgdu.ourRoutePlanManager.this.PLAN_XML_DIR + taskNum + ".xml";
                CompressUtil compressUtil = new CompressUtil();
                isSuccess = compressUtil.compress2Gzip(xmlPath, (String)null);
            }

            if (!isSuccess) {
                com.gdu.demo.ourgdu.ourRoutePlanManager.this.routePlanningErr(EnumRoutePlanningErrStatus.createRoutePlanningFail);
                return null;
            } else {
                if (com.gdu.demo.ourgdu.ourRoutePlanManager.this.onRoutePlanListener != null) {
                    com.gdu.demo.ourgdu.ourRoutePlanManager.this.onRoutePlanListener.createRoutePlanSuccess(true, taskNum);
                }

                return taskNum;
            }
        }

        protected void onPostExecute(String taskNum) {
            super.onPostExecute(taskNum);
            if (!TextUtils.isEmpty(taskNum)) {
                com.gdu.demo.ourgdu.ourRoutePlanManager.this.updateTask2Drone(taskNum);
            }
        }
    }

    class PlanXMLTask extends AsyncTask<String, Integer, String> {
        PlanXMLTask() {
        }

        protected String doInBackground(String... taskNums) {
            boolean isSuccess = false;
            String taskNum = taskNums[0];
            if (!TextUtils.isEmpty(taskNum)) {
                String xmlPath = com.gdu.demo.ourgdu.ourRoutePlanManager.this.PLAN_XML_DIR + taskNum + ".xml";
                CompressUtil compressUtil = new CompressUtil();
                isSuccess = compressUtil.compress2Gzip(xmlPath, (String)null);
            }

            if (!isSuccess) {
                com.gdu.demo.ourgdu.ourRoutePlanManager.this.routePlanningErr(EnumRoutePlanningErrStatus.createRoutePlanningFail);
                return null;
            } else {
                if (com.gdu.demo.ourgdu.ourRoutePlanManager.this.onRoutePlanListener != null) {
                    com.gdu.demo.ourgdu.ourRoutePlanManager.this.onRoutePlanListener.createRoutePlanSuccess(true, taskNum);
                }

                return taskNum;
            }
        }

        protected void onPostExecute(String taskNum) {
            super.onPostExecute(taskNum);
            if (!TextUtils.isEmpty(taskNum)) {
                com.gdu.demo.ourgdu.ourRoutePlanManager.this.updateTask2Drone(taskNum);
            }
        }
    }
}

