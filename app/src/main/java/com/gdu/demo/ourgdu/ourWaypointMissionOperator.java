package com.gdu.demo.ourgdu;


import com.gdu.api.RoutePlanning.EnumPointAction;
import com.gdu.api.RoutePlanning.EnumRoutePlanningErrStatus;
import com.gdu.api.RoutePlanning.EnumRoutePlanningOrder;
import com.gdu.api.RoutePlanning.EnumRoutePlanningRunningStatus;
import com.gdu.api.RoutePlanning.EnumRoutePlanningTaskStatus;
import com.gdu.api.RoutePlanning.OnRouteCmdListener;
import com.gdu.api.RoutePlanning.OnRoutePlanListener;
import com.gdu.api.RoutePlanning.RouteCommonParamBean;
import com.gdu.api.RoutePlanning.RoutePlanBean;
import com.gdu.api.RoutePlanning.RoutePlanManager;
import com.gdu.api.RoutePlanning.RoutePlanPoint;
import com.gdu.common.error.GDUError;
import com.gdu.common.mission.waypoint.Waypoint;
import com.gdu.common.mission.waypoint.WaypointAction;
import com.gdu.common.mission.waypoint.WaypointActionType;
import com.gdu.common.mission.waypoint.WaypointExecutionProgress;
import com.gdu.common.mission.waypoint.WaypointMission;
import com.gdu.common.mission.waypoint.WaypointMissionExecutionEvent;
import com.gdu.common.mission.waypoint.WaypointMissionState;
import com.gdu.common.mission.waypoint.WaypointMissionUploadEvent;
import com.gdu.config.ConnStateEnum;
import com.gdu.config.GlobalVariable;
import com.gdu.sdk.mission.waypoint.WaypointMissionOperatorListener;
import com.gdu.sdk.util.CommonCallbacks;
import com.gdu.socket.GduCommunication3;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ourWaypointMissionOperator {
    private List<RoutePlanPoint> mRoutePlanPoints;
    private ourRoutePlanManager mRoutePlanManager;
    private GduCommunication3 mGduCommunication3;
    private List<WaypointMissionOperatorListener> mOperatorListenerList;
    private int mTotalWaypointCount;
    private boolean isExecutionStarting;
    private WaypointMission mCurrentMission;
    private WaypointMissionState mCurrentMissionState;

    public ourWaypointMissionOperator() {
        this.mCurrentMissionState = WaypointMissionState.UNKNOWN;
        this.mOperatorListenerList = new ArrayList();
    }

    public void initCommunication(GduCommunication3 communication3) {
        try {
            this.mRoutePlanManager = new ourRoutePlanManager(communication3);
        } catch (Exception var3) {
        }

        this.initListener();
    }

    public void addListener(WaypointMissionOperatorListener operatorListener) {
        this.mOperatorListenerList.add(operatorListener);
    }

    public void removeListener(WaypointMissionOperatorListener operatorListener) {
        if (this.mOperatorListenerList.contains(operatorListener)) {
            this.mOperatorListenerList.remove(operatorListener);
        }

    }

    private void initListener() {
        this.mRoutePlanManager.setOnRoutePlanListener(new OnRoutePlanListener() {
            public void beginCreateRoutePlan() {
            }

            public void createRoutePlanSuccess(boolean isSuccess, String routePlanningNum) {
            }

            public void beginSendRoutePlan2Drone() {
            }

            public void progressOfSendRoutePlanning2Drone(float progress) {
                if (com.gdu.demo.ourgdu.ourWaypointMissionOperator.this.mOperatorListenerList != null) {
                    Iterator var2 = com.gdu.demo.ourgdu.ourWaypointMissionOperator.this.mOperatorListenerList.iterator();

                    while(var2.hasNext()) {
                        WaypointMissionOperatorListener waypointMissionOperatorListener = (WaypointMissionOperatorListener)var2.next();
                        WaypointMissionUploadEvent event = new WaypointMissionUploadEvent();
                        event.setProgress(progress);
                        waypointMissionOperatorListener.onUploadUpdate(event);
                    }
                }

            }

            public void sendRoutePlane2DroneSuccess(boolean isSuccess) {
                com.gdu.demo.ourgdu.ourWaypointMissionOperator.this.isExecutionStarting = false;
                GlobalVariable.isWaypointDoing = false;
                com.gdu.demo.ourgdu.ourWaypointMissionOperator.this.mCurrentMissionState = WaypointMissionState.READY_TO_EXECUTE;
            }

            public void OnRoutePlanningExit(EnumRoutePlanningErrStatus errStatus) {
                if (com.gdu.demo.ourgdu.ourWaypointMissionOperator.this.isExecutionStarting && com.gdu.demo.ourgdu.ourWaypointMissionOperator.this.mOperatorListenerList != null) {
                    Iterator var2 = com.gdu.demo.ourgdu.ourWaypointMissionOperator.this.mOperatorListenerList.iterator();

                    while(var2.hasNext()) {
                        WaypointMissionOperatorListener waypointMissionOperatorListener = (WaypointMissionOperatorListener)var2.next();
                        waypointMissionOperatorListener.onExecutionFinish(GDUError.COMMON_PARAM_INVALID);
                    }
                }

                com.gdu.demo.ourgdu.ourWaypointMissionOperator.this.isExecutionStarting = false;
                GlobalVariable.isWaypointDoing = false;
            }

            public void onRoutePlanningRunningStatus(EnumRoutePlanningRunningStatus status, short num, EnumRoutePlanningTaskStatus taskStatus) {
                if (com.gdu.demo.ourgdu.ourWaypointMissionOperator.this.mOperatorListenerList != null) {
                    Iterator var4 = com.gdu.demo.ourgdu.ourWaypointMissionOperator.this.mOperatorListenerList.iterator();

                    while(var4.hasNext()) {
                        WaypointMissionOperatorListener waypointMissionOperatorListener = (WaypointMissionOperatorListener)var4.next();
                        if (!com.gdu.demo.ourgdu.ourWaypointMissionOperator.this.isExecutionStarting) {
                            com.gdu.demo.ourgdu.ourWaypointMissionOperator.this.isExecutionStarting = true;
                            GlobalVariable.isWaypointDoing = true;
                            waypointMissionOperatorListener.onExecutionStart();
                        } else if (status == EnumRoutePlanningRunningStatus.FINISH) {
                            GlobalVariable.isWaypointDoing = false;
                            if (com.gdu.demo.ourgdu.ourWaypointMissionOperator.this.isExecutionStarting) {
                                waypointMissionOperatorListener.onExecutionFinish((GDUError)null);
                            }

                            com.gdu.demo.ourgdu.ourWaypointMissionOperator.this.isExecutionStarting = false;
                        } else {
                            WaypointMissionExecutionEvent event = new WaypointMissionExecutionEvent();
                            WaypointMissionState currentState = com.gdu.demo.ourgdu.ourWaypointMissionOperator.this.getMissionStateFromRunningStatus(status);
                            event.setCurrentState(currentState);
                            WaypointExecutionProgress progress = new WaypointExecutionProgress();
                            progress.totalWaypointCount = com.gdu.demo.ourgdu.ourWaypointMissionOperator.this.mTotalWaypointCount;
                            if (status == EnumRoutePlanningRunningStatus.ARRIVE_POINT) {
                                progress.isWaypointReached = true;
                            } else {
                                progress.isWaypointReached = false;
                            }

                            progress.targetWaypointIndex = num;
                            event.setProgress(progress);
                            com.gdu.demo.ourgdu.ourWaypointMissionOperator.this.mCurrentMissionState = WaypointMissionState.EXECUTING;
                            GlobalVariable.isWaypointDoing = true;
                            waypointMissionOperatorListener.onExecutionUpdate(event);
                        }
                    }
                }

            }

            public void onMsgLog(String msg) {
            }
        });
    }

    public GDUError loadMission(WaypointMission mission) {
        this.mCurrentMission = mission;
        this.mRoutePlanPoints = new ArrayList();
        this.mCurrentMissionState = WaypointMissionState.READY_TO_UPLOAD;
        return this.waypointMissionToRoutePlanBean(mission, this.mRoutePlanPoints);
    }

    public void uploadMission(CommonCallbacks.CompletionCallback completionCallback) {
        if (completionCallback != null) {
            if (GlobalVariable.connStateEnum == ConnStateEnum.Conn_Sucess) {
                completionCallback.onResult((GDUError)null);
                this.mCurrentMissionState = WaypointMissionState.UPLOADING;
                WaypointMission mission = this.mCurrentMission;
                RouteCommonParamBean commonParamBean = new RouteCommonParamBean();
                switch (mission.getFinishedAction()) {
                    case GO_HOME:
                        commonParamBean.setEndOrder(2);
                        break;
                    case AUTO_LAND:
                        commonParamBean.setEndOrder(1);
                        break;
                    case NO_ACTION:
                        commonParamBean.setEndOrder(0);
                }

                if (mission.isResponseLostActionOnRCSignalLost()) {
                    commonParamBean.setLostOrder(1);
                } else {
                    commonParamBean.setLostOrder(0);
                }

                commonParamBean.setAltitudeMode(0);
                this.mTotalWaypointCount = this.mRoutePlanPoints.size();
                this.mRoutePlanManager.updateSeniorPlanning2Drone(this.mRoutePlanPoints, commonParamBean);
            } else {
                completionCallback.onResult(GDUError.COMMON_DISCONNECTED);
            }
        }
    }

    public void startMission(final CommonCallbacks.CompletionCallback completionCallback) {
        this.mRoutePlanManager.sendTaskStartCmd(EnumRoutePlanningOrder.TASK_BEGIN, new OnRouteCmdListener() {
            public void setSuccess(boolean isSuccess, Object object) {
                if (isSuccess) {
                    completionCallback.onResult((GDUError)null);
                } else {
                    completionCallback.onResult(GDUError.COMMON_PARAM_INVALID);
                }

            }
        });
    }

    public void pauseMission(final CommonCallbacks.CompletionCallback completionCallback) {
        this.mRoutePlanManager.sendTaskStartCmd(EnumRoutePlanningOrder.TASK_PAUSE, new OnRouteCmdListener() {
            public void setSuccess(boolean isSuccess, Object object) {
                if (isSuccess) {
                    completionCallback.onResult((GDUError)null);
                } else {
                    completionCallback.onResult(GDUError.COMMON_PARAM_INVALID);
                }

            }
        });
    }

    public void resumeMission(final CommonCallbacks.CompletionCallback completionCallback) {
        this.mRoutePlanManager.sendTaskStartCmd(EnumRoutePlanningOrder.TASK_GOON, new OnRouteCmdListener() {
            public void setSuccess(boolean isSuccess, Object object) {
                if (isSuccess) {
                    completionCallback.onResult((GDUError)null);
                } else {
                    completionCallback.onResult(GDUError.COMMON_PARAM_INVALID);
                }

            }
        });
    }

    public void stopMission(final CommonCallbacks.CompletionCallback completionCallback) {
        this.mRoutePlanManager.sendTaskStartCmd(EnumRoutePlanningOrder.TASK_END, new OnRouteCmdListener() {
            public void setSuccess(boolean isSuccess, Object object) {
                if (isSuccess) {
                    completionCallback.onResult((GDUError)null);
                } else {
                    completionCallback.onResult(GDUError.COMMON_PARAM_INVALID);
                }

            }
        });
        GlobalVariable.isWaypointDoing = false;
    }

    public WaypointMissionState getCurrentState() {
        return this.mCurrentMissionState;
    }

    private WaypointMissionState getMissionStateFromRunningStatus(EnumRoutePlanningRunningStatus runningStatus) {
        WaypointMissionState missionState = WaypointMissionState.EXECUTING;
        switch (runningStatus) {
            case RUNNING:
                missionState = WaypointMissionState.EXECUTING;
                break;
            case PAUSE:
                missionState = WaypointMissionState.EXECUTION_PAUSED;
                break;
            case ARRIVE_POINT:
                missionState = WaypointMissionState.EXECUTING;
            case EXITBYUSER:
            case FINISH:
            case FAIL:
        }

        this.mCurrentMissionState = missionState;
        return missionState;
    }

    private GDUError waypointMissionToRoutePlanBean(WaypointMission mission, List<RoutePlanPoint> routePlanPoints) {
        if (mission == null) {
            return GDUError.COMMON_PARAM_INVALID;
        } else {
            float autoSpeed = mission.getAutoFlightSpeed();
            List<Waypoint> waypoints = mission.getWaypointList();
            RoutePlanPoint planBean;
            if (waypoints != null) {
                for(Iterator var5 = waypoints.iterator(); var5.hasNext(); routePlanPoints.add(planBean)) {
                    Waypoint waypoint = (Waypoint)var5.next();
                    planBean = new RoutePlanPoint();
                    if (waypoint.getCoordinate() == null) {
                        return GDUError.COMMON_PARAM_INVALID;
                    }

                    planBean.latitude = waypoint.getCoordinate().getLatitude();
                    planBean.longitude = waypoint.getCoordinate().getLongitude();
                    planBean.height = waypoint.getAltitude();
                    planBean.speed = (double)autoSpeed;
                    planBean.verticalUpSpeed = autoSpeed;
                    planBean.verticalDownSpeed = autoSpeed;
                    planBean.droneHeadAngle = 720.0F;
                    planBean.gimbalAngle = waypoint.getGimbalPitch();
                    List<RoutePlanBean.subActionBean> subActionBeans = this.getSubActionList(waypoint);
                    if (subActionBeans != null) {
                        planBean.actions = subActionBeans;
                    }
                }
            }

            this.mCurrentMissionState = WaypointMissionState.READY_TO_UPLOAD;
            return null;
        }
    }

    private float changeWaypointHeading(float heading) {
        if (heading != 720.0F && heading != 1080.0F) {
            if (heading < 0.0F) {
                heading += 360.0F;
            }

            return heading;
        } else {
            return 0.0F;
        }
    }

    private List<RoutePlanBean.subActionBean> getSubActionList(Waypoint waypoint) {
        List<WaypointAction> waypointActions = waypoint.getWaypointActions();
        List<RoutePlanBean.subActionBean> subActionBeans = null;
        if (waypointActions != null) {
            subActionBeans = new ArrayList();
            Iterator var4 = waypointActions.iterator();

            while(var4.hasNext()) {
                WaypointAction waypointAction = (WaypointAction)var4.next();
                RoutePlanBean.subActionBean subActionBean = null;
                if (waypointAction.actionType == WaypointActionType.STAY) {
                    subActionBean = new RoutePlanBean.subActionBean(EnumPointAction.Hover, waypointAction.actionParam + "");
                } else if (waypointAction.actionType == WaypointActionType.START_TAKE_PHOTO) {
                    subActionBean = new RoutePlanBean.subActionBean(EnumPointAction.TakePhoto, "-1");
                } else if (waypointAction.actionType == WaypointActionType.START_RECORD) {
                    subActionBean = new RoutePlanBean.subActionBean(EnumPointAction.startRecord, waypointAction.actionParam + "");
                } else if (waypointAction.actionType == WaypointActionType.STOP_RECORD) {
                    subActionBean = new RoutePlanBean.subActionBean(EnumPointAction.EndRecord, waypointAction.actionParam + "");
                } else if (waypointAction.actionType == WaypointActionType.GIMBAL_PITCH) {
                    subActionBean = new RoutePlanBean.subActionBean(EnumPointAction.GimbalAngle, waypointAction.actionParam + "");
                } else if (waypointAction.actionType == WaypointActionType.ROTATE_AIRCRAFT) {
                    subActionBean = new RoutePlanBean.subActionBean(EnumPointAction.Rotation, waypointAction.actionParam + "");
                } else if (waypointAction.actionType == WaypointActionType.GIMBAL_YWA) {
                    subActionBean = new RoutePlanBean.subActionBean(EnumPointAction.GimbalYawAngle, waypointAction.actionParam + "");
                }

                if (subActionBean != null) {
                    subActionBeans.add(subActionBean);
                }
            }
        }

        return subActionBeans;
    }
}
