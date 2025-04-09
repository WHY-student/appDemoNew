package com.gdu.demo.ourgdu;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//
import com.gdu.sdk.mission.followme.FollowMeMissionOperator;
import com.gdu.sdk.mission.hotpoint.HotpointMissionOperator;
import com.gdu.sdk.mission.waypoint.WaypointMissionOperator;
import com.gdu.socket.GduSocketManager;

public class ourMissionControl {
    private static com.gdu.demo.ourgdu.ourMissionControl instance;
    private GduSocketManager mGduSocketManager = GduSocketManager.getInstance();
    private HotpointMissionOperator mHotpointMissionOperator;
    private FollowMeMissionOperator mFollowMeMissionOperator;
    private ourWaypointMissionOperator mWaypointMissionOperator;

    public static synchronized com.gdu.demo.ourgdu.ourMissionControl getInstance() {
        if (instance == null) {
            Class var0 = com.gdu.sdk.mission.MissionControl.class;
            synchronized(com.gdu.sdk.mission.MissionControl.class) {
                if (instance == null) {
                    instance = new com.gdu.demo.ourgdu.ourMissionControl();
                }
            }
        }

        return instance;
    }

    private ourMissionControl() {
    }

    public ourWaypointMissionOperator getWaypointMissionOperator() {
        if (this.mWaypointMissionOperator == null) {
            this.mWaypointMissionOperator = new ourWaypointMissionOperator();
            this.mWaypointMissionOperator.initCommunication(this.mGduSocketManager.getGduCommunication());
        }

        return this.mWaypointMissionOperator;
    }

    public HotpointMissionOperator getHotpointMissionOperator() {
        if (this.mHotpointMissionOperator == null) {
            this.mHotpointMissionOperator = new HotpointMissionOperator();
            this.mHotpointMissionOperator.initCommunication(this.mGduSocketManager.getGduCommunication());
        }

        return this.mHotpointMissionOperator;
    }

    public FollowMeMissionOperator getFollowMeMissionOperator() {
        if (this.mFollowMeMissionOperator == null) {
            this.mFollowMeMissionOperator = new FollowMeMissionOperator();
            this.mFollowMeMissionOperator.initCommunication(this.mGduSocketManager.getGduCommunication());
        }

        return this.mFollowMeMissionOperator;
    }

    private static enum TimelineState {
        START,
        START_NEXT,
        PAUSE,
        RESUME,
        STOP,
        FINISH;

        private TimelineState() {
        }
    }
}
