//package com.gdu.demo;
//
//import android.app.Activity;
//import android.content.Context;
//import android.content.res.AssetManager;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.graphics.Color;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.AdapterView;
//import android.widget.ArrayAdapter;
//import android.widget.Spinner;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.Nullable;
//
//import com.amap.api.maps.AMap;
//import com.amap.api.maps.CameraUpdateFactory;
//import com.amap.api.maps.CoordinateConverter;
//import com.amap.api.maps.LocationSource;
//import com.amap.api.maps.MapView;
//import com.amap.api.maps.model.BitmapDescriptorFactory;
//import com.amap.api.maps.model.LatLng;
//import com.amap.api.maps.model.Marker;
//import com.amap.api.maps.model.MarkerOptions;
//import com.amap.api.maps.model.PolylineOptions;
//import com.gdu.common.error.GDUError;
//import com.gdu.common.mission.waypoint.Waypoint;
//import com.gdu.common.mission.waypoint.WaypointAction;
//import com.gdu.common.mission.waypoint.WaypointActionType;
//import com.gdu.common.mission.waypoint.WaypointMission;
//import com.gdu.common.mission.waypoint.WaypointMissionExecutionEvent;
//import com.gdu.common.mission.waypoint.WaypointMissionFinishedAction;
//import com.gdu.common.mission.waypoint.WaypointMissionHeadingMode;
//import com.gdu.common.mission.waypoint.WaypointMissionState;
//import com.gdu.common.mission.waypoint.WaypointMissionUploadEvent;
//import com.gdu.demo.ourgdu.ourGDUAircraft;
////import com.gdu.demo.ourgdu.ourMissionControl;
////import com.gdu.demo.ourgdu.ourWaypointMissionOperator;
//import com.gdu.demo.ourgdu.ourMissionControl;
//import com.gdu.demo.ourgdu.ourWaypointMissionOperator;
//import com.gdu.drone.LocationCoordinate2D;
//import com.gdu.drone.LocationCoordinate3D;
//import com.gdu.rtk.PositioningSolution;
//import com.gdu.sdk.base.BaseProduct;
//import com.gdu.sdk.camera.GDUCamera;
//import com.gdu.sdk.camera.SystemState;
//import com.gdu.sdk.flightcontroller.FlightControllerState;
//import com.gdu.sdk.flightcontroller.GDUFlightController;
//import com.gdu.sdk.mission.MissionControl;
//import com.gdu.sdk.mission.waypoint.WaypointMissionOperator;
//import com.gdu.sdk.mission.waypoint.WaypointMissionOperatorListener;
//import com.gdu.sdk.products.GDUAircraft;
//import com.gdu.sdk.simulator.InitializationData;
//import com.gdu.sdk.util.CommonCallbacks;
//import com.gdu.socket.GduCommunication3;
//import com.gdu.socket.GduFrame3;
//import com.gdu.socket.GduSocketManager;
//import com.gdu.socket.SocketCallBack3;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.ArrayList;
//import java.util.List;
//
//import javax.xml.parsers.DocumentBuilder;
//import javax.xml.parsers.DocumentBuilderFactory;
//import org.w3c.dom.Document;
//import org.w3c.dom.Element;
//import org.w3c.dom.Node;
//import org.w3c.dom.NodeList;
//import org.xml.sax.InputSource;
//
///**
// * 航点任务测试
// */
//public class WaypointMissionOperatorActivity extends Activity implements LocationSource , View.OnClickListener {
//
//    private static final double HORIZONTAL_DISTANCE = 30;
//    private static final double VERTICAL_DISTANCE = 30;
//    private static final double ONE_METER_OFFSET = 0.00000899322;
//
//    private TextView flyInfoView;
//    private MapView mMapView;
//    private AMap aMap;
//    private Marker mPlaneMarker;
//    private MarkerOptions mPlaneMarkerOptions;
//    private CoordinateConverter coordinateConverter;
//    private Context mContext;
//    private TextView mMissionInfoTextView;
//
//
//    private GDUFlightController mGDUFlightController;
//    private ourWaypointMissionOperator waypointMissionOperator = null;
//    private WaypointMission mission = null;
//    private WaypointMissionOperatorListener listener;
//    private GDUCamera mGDUCamera;
//
//    private Spinner spinner;
//
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        mContext = this;
//        setContentView(R.layout.activity_waypoint_mission);
//        flyInfoView  =(TextView) findViewById(R.id.fly_info_textview);
//        mMapView = findViewById(R.id.map);
//        mMissionInfoTextView = findViewById(R.id.mission_info_textview);
//        spinner = (Spinner) findViewById(R.id.task_list);
//        initMap(savedInstanceState);
//        initData();
//        initListener();
//
//
//    }
//
//    private void initData() {
//        BaseProduct product = SdkDemoApplication.getProductInstance();
//        if (product == null || !product.isConnected()) {
//            return;
//        } else {
//            mGDUFlightController = ((ourGDUAircraft) product).getFlightController();
//            mGDUFlightController.setStateCallback(new FlightControllerState.Callback() {
//                @Override
//                public void onUpdate(FlightControllerState flightControllerState) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            if (mPlaneMarker != null) {
//                                LocationCoordinate3D locationCoordinate3D  = flightControllerState.getAircraftLocation();
//                                LatLng latLng = new LatLng(locationCoordinate3D.getLatitude(), locationCoordinate3D.getLongitude());
//                                coordinateConverter.coord(latLng);
//                                mPlaneMarker.setPosition(coordinateConverter.convert());
//                            }
//                            flyInfoView.setText(flightControllerState.getString());
//                        }
//                    });
//                }
//            });
//            waypointMissionOperator = getWaypointMissionOperator();
//            setUpListener();
//
//            mGDUCamera = (GDUCamera) ((ourGDUAircraft) SdkDemoApplication.getProductInstance()).getCamera();
//
//        }
//        try{
//            AssetManager assetManager=getAssets();
//            String[] xml_arr = assetManager.list("waypoints");
//            // 然后的话创建一个我们的一个数组适配器并且的话这个数组适配器使我们的字符串类型的
//            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,xml_arr);
//            // 设置我们的数组下拉时的选项的样式
//            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//            // 将我们的适配器和我们的下拉列表框关联起来
//            spinner.setAdapter(adapter);
//            spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {//选择item的选择点击监听事件
//                public void onItemSelected(AdapterView<?> arg0, View arg1,
//                                           int arg2, long arg3) {
//                    // TODO Auto-generated method stub
//                    // 将所选mySpinner 的值带入myTextView 中
//                    cleanWaypointMission();
//                    mission = createWaypointMission(adapter.getItem(arg2));
//                    addPolyline(mission);
//                    waypointMissionOperator.loadMission(mission);
//                }
//
//                public void onNothingSelected(AdapterView<?> arg0) {
//                    // TODO Auto-generated method stub
//                }
//            });
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//    }
//
//    private ourWaypointMissionOperator getWaypointMissionOperator() {
//        if (null == waypointMissionOperator) {
//            if (null != ourMissionControl.getInstance()) {
//                return ourMissionControl.getInstance().getWaypointMissionOperator();
//            }
//        }
//        return waypointMissionOperator;
//    }
//
//    private void setUpListener() {
//        listener = new WaypointMissionOperatorListener() {
//
//            @Override
//            public void onUploadUpdate(WaypointMissionUploadEvent waypointMissionUploadEvent) {
//                // Example of Upload Listener
////                if (waypointMissionUploadEvent.getProgress() != null
////                        && waypointMissionUploadEvent.getProgress().isSummaryUploaded
////                        && waypointMissionUploadEvent.getProgress().uploadedWaypointIndex == (WAYPOINT_COUNT - 1)) {
////                    toast("Mission is uploaded successfully");
////                }
//                show("Mission is uploaded successfully Progress " + waypointMissionUploadEvent.getProgress());
////                updateWaypointMissionState();
//            }
//
//            @Override
//            public void onExecutionUpdate(WaypointMissionExecutionEvent waypointMissionExecutionEvent) {
//                // Example of Execution Listener
//                show(waypointMissionExecutionEvent.getCurrentState().getName()
//                        + (waypointMissionExecutionEvent.getProgress() == null
//                        ? ""
//                        : waypointMissionExecutionEvent.getProgress().targetWaypointIndex));
////                updateWaypointMissionState();
//            }
//
//            @Override
//            public void onExecutionStart() {
//                toast("Mission started");
////                updateWaypointMissionState();
//            }
//
//            @Override
//            public void onExecutionFinish(GDUError djiError) {
////                show("Mission finished");
//                toast("Mission finished");
////                updateWaypointMissionState();
//            }
//        };
//
//        if (waypointMissionOperator != null && listener != null) {
//            // Example of adding listeners
//            waypointMissionOperator.addListener(listener);
//        }
//    }
//
//
//    private void initListener() {
//        if (mGDUCamera != null) {
//            mGDUCamera.setSystemStateCallback(new SystemState.Callback() {
//                @Override
//                public void onUpdate(SystemState systemState) {
//                    StringBuilder sb = new StringBuilder();
//                    sb.append(" isPhotoStored ");
//                    sb.append(systemState.isPhotoStored());
//                    sb.append(" hasError ");
//                    sb.append(systemState.isHasError());
//                    sb.append(" isRecording ");
//                    sb.append(systemState.isRecording());
//                    sb.append(" mode ");
//                    sb.append(systemState.getMode());
//                    sb.append(" time ");
//                    sb.append(systemState.getCurrentVideoRecordingTimeInSeconds());
//                    if (systemState.isPhotoStored()) {
//                        toast(sb.toString());
//                    }
//                }
//            });
//        }
//    }
//
//    int index = 0;
//    private void addPolyline(WaypointMission waypointMission){
//        if(index == 0){
//            index++;
//            addPolyline(waypointMission.getWaypointList(), Color.argb(255, 1, 1, 1), 10);
//        } else {
//            addPolyline(waypointMission.getWaypointList(), Color.argb(255, 255, 1, 1), 10);
//        }
//
//    }
//
//    private void addPolyline(List<Waypoint> planPoints, int color, int width){
//        List<LatLng> latLngs = new ArrayList<>();
//        if (planPoints == null) {
//            return;
//        }
//        for (Waypoint waypoint : planPoints) {
//            LocationCoordinate2D locationCoordinate2D = waypoint.getCoordinate();
//            LatLng latLng = new LatLng(locationCoordinate2D.getLatitude(), locationCoordinate2D.getLongitude());
//            coordinateConverter.coord(latLng);
//            latLngs.add(coordinateConverter.convert());
//        }
//        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngs.get(0), 19));
//        mPlaneMarkerOptions = new MarkerOptions();
//        mPlaneMarkerOptions.position(latLngs.get(0));
//
//        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
//                R.mipmap.icon_plane).copy(Bitmap.Config.ARGB_8888, true);
//        mPlaneMarkerOptions.anchor(0.5f, 0.5f);
//        mPlaneMarkerOptions.icon(BitmapDescriptorFactory.fromBitmap(bitmap));
//        mPlaneMarker = aMap.addMarker(mPlaneMarkerOptions);
//        aMap.addPolyline(new PolylineOptions().addAll(latLngs).width(width).color(color));
//    }
//
//    private void initMap(Bundle savedInstanceState) {
//        mMapView.onCreate(savedInstanceState);
//        aMap = mMapView.getMap();
//        aMap.setMapType(AMap.MAP_TYPE_SATELLITE);
//        coordinateConverter = new CoordinateConverter(this);
//        coordinateConverter.from(CoordinateConverter.CoordType.GPS);
//    }
//
//    private void startSimulator() {
//        if (null != mGDUFlightController) {
//            LocationCoordinate3D locationCoordinate3D = new LocationCoordinate3D(30.5102977554885,114.4093863896088, 10);
//            InitializationData initializationData = new InitializationData(locationCoordinate3D, (short) 90, PositioningSolution.FIXED_POINT, (byte) 30);
//            mGDUFlightController.getSimulator().start(initializationData, new CommonCallbacks.CompletionCallback() {
//                @Override
//                public void onResult(GDUError gduError) {
//                    if (gduError == null) {
//                        toast("模拟飞行开启");
//                    }
//                }
//            });
//        }
//    }
//
//
//    public void onClick(View view){
//        switch (view.getId()){
//            case R.id.simulator_button:
//                startSimulator();
//                break;
//            case R.id.load_waypoint_button:
//                cleanWaypointMission();
//                mission = createWaypointMission("已知类别海面目标.xml");
//                toast(""+mission.getWaypointList().size());
//                addPolyline(mission);
//                waypointMissionOperator.loadMission(mission);
//                break;
//            case R.id.upload_waypoint_button:
//                waypointMissionOperator.uploadMission(new CommonCallbacks.CompletionCallback() {
//                    @Override
//                    public void onResult(GDUError error) {
//                        if (error == null) {
//                            toast("上传航迹发送成功");
//                        } else {
//                            toast("上传航迹发送失败");
//                        }
//                    }
//                });
//                break;
//            case R.id.start_waypoint_button:
//                if (waypointMissionOperator.getCurrentState() == WaypointMissionState.READY_TO_EXECUTE) {
//                    waypointMissionOperator.startMission(new CommonCallbacks.CompletionCallback() {
//                        @Override
//                        public void onResult(GDUError error) {
//                            if (error == null) {
//                                toast("开始航迹成功");
//                            } else {
//                                toast("开始航迹失败");
//                            }
//                        }
//                    });
//                }
//                break;
//            case R.id.resume_waypoint_button:
//                waypointMissionOperator.resumeMission(new CommonCallbacks.CompletionCallback() {
//                    @Override
//                    public void onResult(GDUError error) {
//                        if (error == null) {
//                            toast("继续航迹成功");
//                        } else {
//                            toast("继续航迹失败");
//                        }
//                    }
//                });
//                break;
//            case R.id.pause_waypoint_button:
//                    waypointMissionOperator.pauseMission(new CommonCallbacks.CompletionCallback() {
//                        @Override
//                        public void onResult(GDUError error) {
//                            if (error == null) {
//                                toast("暂停航迹成功");
//                            } else {
//                                toast("暂停航迹失败");
//                            }
//                        }
//                    });
//                break;
//            case R.id.stop_waypoint_button:
//                waypointMissionOperator.stopMission(new CommonCallbacks.CompletionCallback() {
//                    @Override
//                    public void onResult(GDUError error) {
//                        if (error == null) {
//                            toast("停止航迹成功");
//                        } else {
//                            toast("停止航迹失败");
//                        }
//                    }
//                });
//                break;
//        }
//    }
//    public void cleanWaypointMission(){
//        aMap.clear(true);
//    }
//
//    // 用Element方式
//    public static ArrayList<String> element(NodeList list) {
//        ArrayList<String> waypoints_position = new ArrayList<>();
//        for (int i = 0; i < list.getLength(); i++) {
//            Element element = (Element) list.item(i);
//            NodeList childNodes = element.getElementsByTagName("coordinates");
//            String result = childNodes.item(0).getNodeValue();
//            waypoints_position.add(result);
//        }
//        return waypoints_position;
//    }
//    private WaypointMission createWaypointMission(String xml_name) {
//        // 读取xml文件
////        NodeList sList;
////        try {
////            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
////            DocumentBuilder builder = factory.newDocumentBuilder();
////            AssetManager assetManager=getAssets();
////            InputStream stream=assetManager.open("waypoints/" + xml_name);
////
////            Document d = builder.parse(stream);
////            sList = d.getElementsByTagName("Placemark");
////        } catch (Exception e) {
////            throw new RuntimeException(e);
////        }
////
////        WaypointMission waypointMission = new WaypointMission();
////        waypointMission.setAutoFlightSpeed(10f);
////        waypointMission.setMaxFlightSpeed(10f);
////        waypointMission.setResponseLostActionOnRCSignalLost(false);
////        waypointMission.setFinishedAction(WaypointMissionFinishedAction.GO_HOME);
////        waypointMission.setHeadingMode(WaypointMissionHeadingMode.AUTO);
////        waypointMission.setGimbalPitchRotationEnabled(true);
////
////        List<Waypoint> waypointList = new ArrayList<>();
////        Waypoint waypoint;
////        float gimbal_pitch;
////        float gimbal_rotate;
////        for (int i = 0; i < sList.getLength(); i++) {
////            Element element = (Element) sList.item(i);
////            NodeList childNodes = element.getElementsByTagName("coordinates");
////            String waypoint_position = childNodes.item(0).getTextContent();
////            String s[] = waypoint_position.split(",");
////            double Longitude = Double.parseDouble(s[0]);
////            double Latitude = Double.parseDouble(s[1]);
////            float Altitude = Float.parseFloat(s[2]);
////            waypoint =  new Waypoint(Latitude, Longitude, Altitude);
////            waypoint.setSpeed(Integer.parseInt(element.getElementsByTagName("speed").item(0).getTextContent()));
////            gimbal_pitch = Float.parseFloat(element.getElementsByTagName("gimbalAngle").item(0).getTextContent());
////            gimbal_rotate=Float.parseFloat(element.getElementsByTagName("droneHeadAngle").item(0).getTextContent());
////            waypoint.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT,(int)gimbal_rotate));
//////            waypoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));//拍照比较耗时
////            waypoint.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, (int) gimbal_pitch));
//////            waypoint.addAction(new WaypointAction(WaypointActionType.STAY, 30));
////            waypoint.setGimbalPitch(gimbal_pitch);
////            waypointList.add(waypoint);
////        }
//
//        WaypointMission waypointMission = new WaypointMission();
////        double baseLatitude = 30.471033;
////        double baseLongitude = 114.4280014;
//        double baseLatitude = 30.510297755488512;
//        double baseLongitude = 114.40938773071838;
//
////
////
////        final float baseAltitude = 30.0f;
//        waypointMission.setAutoFlightSpeed(5f);
//        waypointMission.setMaxFlightSpeed(10f);
//        waypointMission.setResponseLostActionOnRCSignalLost(false);
//        waypointMission.setFinishedAction(WaypointMissionFinishedAction.GO_HOME);
//        waypointMission.setHeadingMode(WaypointMissionHeadingMode.AUTO);
////      builder.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY);  waypointMission.setGotoFirstWaypointMode(WaypointMissionFlightPathMode.NORMAL);
////      builder.setPointOfInterest(new LocationCoordinate2D(15, 15));
////      builder.headingMode(WaypointMissionHeadingMode.TOWARD_POINT_OF_INTEREST);
//        waypointMission.setGimbalPitchRotationEnabled(true);
//
//        List<Waypoint> waypointList = new ArrayList<>();
//
//
//
//        final float baseAltitude = 50.0f;
//
//
//        // Waypoint 0: (0,0)
//
//        Waypoint waypoint0 = new Waypoint(baseLatitude, baseLongitude, baseAltitude);
////        waypoint0.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT,720));
////        waypoint0.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
////        waypoint0.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -90));
////        waypoint0.addAction(new WaypointAction(WaypointActionType.STAY,1));
//        waypoint0.setSpeed(5);
//        waypoint0.setGimbalPitch(-90);
//        waypointList.add(waypoint0);
//
//
//        // Waypoint 1: (0,30)
//        baseLongitude=114.40937030114404;
//        baseLatitude=30.510077128267262;
//        Waypoint waypoint1 = new Waypoint(baseLatitude, baseLongitude , baseAltitude);
////        waypoint1.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, 720));
////        waypoint1.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -45));
//        waypoint1.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 1));
////        waypoint1.addAction(new WaypointAction(WaypointActionType.STAY,1));
//        waypoint1.setSpeed(10);
//        waypoint1.setGimbalPitch(-90);
//        waypointList.add(waypoint1);
//
//
//        // Waypoint 2: (30,30)
//        baseLongitude=114.410590424279;
//        baseLatitude=30.509974739031247;
//        Waypoint waypoint2 = new Waypoint(baseLatitude , baseLongitude , baseAltitude);
////        waypoint2.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, 720));
//        waypoint2.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
////        waypoint2.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -90));
////        waypoint1.addAction(new WaypointAction(WaypointActionType.STAY,1));
//        waypoint2.setSpeed(10);
//        waypoint2.setGimbalPitch(-90);
//        waypointList.add(waypoint2);
//
//        // Waypoint 3: (30,0)
//        baseLongitude=114.41067625395226;
//        baseLatitude=30.5108653410641;
//        Waypoint waypoint3 = new Waypoint(baseLatitude, baseLongitude, baseAltitude);
////        waypoint3.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, 720));
//        waypoint3.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
////        waypoint3.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -90));
////        waypoint1.addAction(new WaypointAction(WaypointActionType.STAY,1));
//        waypoint3.setSpeed(10);
//        waypoint3.setGimbalPitch(-90);
//        waypointList.add(waypoint3);
//
//        // Waypoint 4: (30,0)
//        baseLongitude=114.40948968094305;
//        baseLatitude=30.510970045329092;
//        Waypoint waypoint4 = new Waypoint(baseLatitude, baseLongitude, baseAltitude);
////        waypoint4.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, 720));
//        waypoint4.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
////        waypoint4.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -90));
////        waypoint1.addAction(new WaypointAction(WaypointActionType.STAY,1));
//        waypoint4.setSpeed(10);
//        waypoint4.setGimbalPitch(-90);
//        waypointList.add(waypoint4);
//
//        // Waypoint 4: (30,0)
//        baseLongitude=114.4093863896088;
//        baseLatitude=30.51028504911789;
//        Waypoint waypoint5 = new Waypoint(baseLatitude, baseLongitude, baseAltitude);
////        waypoint5.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, 720));
//        waypoint5.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
////        waypoint5.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -90));
//        waypoint5.setSpeed(10);
//        waypoint5.setGimbalPitch(-90);
//        waypointList.add(waypoint5);
//
//       waypointMission.setWaypointCount(waypointList.size());
//        waypointMission.setWaypointList(waypointList);
//        return waypointMission;
//    }
//
//    private int calculateTurnAngle() {
//        return Math.round((float)Math.toDegrees(Math.atan(VERTICAL_DISTANCE/ HORIZONTAL_DISTANCE)));
//    }
//
//
//    public void toast(final String toast){
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
////                mVideoPicTextView.setText(toast);
//                Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    public void show(final String toast){
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                mMissionInfoTextView.setText(toast);
////                Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    @Override
//    public void activate(OnLocationChangedListener onLocationChangedListener) {
//
//    }
//
//    @Override
//    public void deactivate() {
//
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        mMapView.onResume();
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        mMapView.onPause();
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        mMapView.onDestroy();
//    }
//}
package com.gdu.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.CoordinateConverter;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.PolylineOptions;
import com.gdu.common.error.GDUError;
import com.gdu.common.mission.waypoint.Waypoint;
import com.gdu.common.mission.waypoint.WaypointAction;
import com.gdu.common.mission.waypoint.WaypointActionType;
import com.gdu.common.mission.waypoint.WaypointMission;
import com.gdu.common.mission.waypoint.WaypointMissionExecutionEvent;
import com.gdu.common.mission.waypoint.WaypointMissionFinishedAction;
import com.gdu.common.mission.waypoint.WaypointMissionHeadingMode;
import com.gdu.common.mission.waypoint.WaypointMissionState;
import com.gdu.common.mission.waypoint.WaypointMissionUploadEvent;
import com.gdu.config.GlobalVariable;
import com.gdu.demo.ourgdu.ourGDUAircraft;
//import com.gdu.demo.ourgdu.ourMissionControl;
//import com.gdu.demo.ourgdu.ourWaypointMissionOperator;
import com.gdu.drone.LocationCoordinate2D;
import com.gdu.drone.LocationCoordinate3D;
import com.gdu.rtk.PositioningSolution;
import com.gdu.sdk.base.BaseProduct;
import com.gdu.sdk.camera.GDUCamera;
import com.gdu.sdk.camera.SystemState;
import com.gdu.sdk.flightcontroller.FlightControllerState;
import com.gdu.sdk.flightcontroller.GDUFlightController;
import com.gdu.sdk.mission.MissionControl;
import com.gdu.sdk.mission.waypoint.WaypointMissionOperator;
import com.gdu.sdk.mission.waypoint.WaypointMissionOperatorListener;
import com.gdu.sdk.products.GDUAircraft;
import com.gdu.sdk.simulator.InitializationData;
import com.gdu.sdk.util.CommonCallbacks;
import com.gdu.socket.GduCommunication3;
import com.gdu.socket.GduFrame3;
import com.gdu.socket.GduSocketManager;
import com.gdu.socket.SocketCallBack3;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * 航点任务测试
 */
public class WaypointMissionOperatorActivity extends Activity implements LocationSource , View.OnClickListener {

    private static final double HORIZONTAL_DISTANCE = 30;
    private static final double VERTICAL_DISTANCE = 30;
    private static final double ONE_METER_OFFSET = 0.00000899322;

    private static int select_mission_index = 0;
    private static boolean is_mission = false;

//    private TextView flyInfoView;
    private MapView mMapView;
    private AMap aMap;
    private Marker mPlaneMarker;
    private MarkerOptions mPlaneMarkerOptions;
    private CoordinateConverter coordinateConverter;
    private Context mContext;
    private TextView mMissionInfoTextView;
    private Spinner spinner;


    private GDUFlightController mGDUFlightController;
    private WaypointMissionOperator waypointMissionOperator = null;
    private WaypointMission mission = null;
    private WaypointMissionOperatorListener listener;
    private GDUCamera mGDUCamera;
    private Button btnBackToFlight;

    private static final List<WaypointMissionState> validMissionStartStates = Arrays.asList(
            WaypointMissionState.READY_TO_EXECUTE,
            WaypointMissionState.EXECUTING,
            WaypointMissionState.EXECUTION_PAUSED
    );

    private boolean is_init_spinner = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_waypoint_mission);
//        flyInfoView  =(TextView) findViewById(R.id.fly_info_textview);
        mMapView = findViewById(R.id.map);
        mMissionInfoTextView = findViewById(R.id.mission_info_textview);
        btnBackToFlight = findViewById(R.id.btn_back_to_flight);
        spinner = (Spinner) findViewById(R.id.task_list);
        initMap(savedInstanceState);
        initData();
        initListener();
    }

    private void initData() {
        BaseProduct product = SdkDemoApplication.getProductInstance();
        if (product == null || !product.isConnected()) {
            return;
        } else {
            mGDUFlightController = ((ourGDUAircraft) product).getFlightController();
            mGDUFlightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(FlightControllerState flightControllerState) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mPlaneMarker != null) {
                                LocationCoordinate3D locationCoordinate3D  = flightControllerState.getAircraftLocation();
                                LatLng latLng = new LatLng(locationCoordinate3D.getLatitude(), locationCoordinate3D.getLongitude());
                                coordinateConverter.coord(latLng);
                                mPlaneMarker.setPosition(coordinateConverter.convert());
                            }
//                            flyInfoView.setText(flightControllerState.getString());
                        }
                    });
                }
            });
            waypointMissionOperator = getWaypointMissionOperator();
            setUpListener();

            mGDUCamera = (GDUCamera) ((ourGDUAircraft) SdkDemoApplication.getProductInstance()).getCamera();

        }
        try{
            AssetManager assetManager=getAssets();
            String[] xml_arr = assetManager.list("waypoints");
            String[] select_xml_arr;

            if(xml_arr != null){
                select_xml_arr = new String[xml_arr.length + 1];

                // 设置第一个元素为空字符串
                select_xml_arr[0] = "请选择任务";

                // 复制原数组内容到新数组
                System.arraycopy(xml_arr, 0, select_xml_arr, 1, xml_arr.length);
            }else {
                select_xml_arr = new String[]{"请选择任务"};
            }

            // 然后的话创建一个我们的一个数组适配器并且的话这个数组适配器使我们的字符串类型的
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, select_xml_arr);
            // 设置我们的数组下拉时的选项的样式
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            // 将我们的适配器和我们的下拉列表框关联起来
            spinner.setAdapter(adapter);
            spinner.setSelection(WaypointMissionOperatorActivity.select_mission_index);
            spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {//选择item的选择点击监听事件
                public void onItemSelected(AdapterView<?> arg0, View arg1,
                                           int arg2, long arg3) {
                    // TODO Auto-generated method stub
                    // 将所选mySpinner 的值带入myTextView 中
                    cleanWaypointMission();
                    WaypointMissionOperatorActivity.select_mission_index = arg2;
                    if(is_init_spinner){
                        if(arg2!=0){
                            mission = createWaypointMission(adapter.getItem(arg2));
                            addPolyline(mission);
                        }
                        // 初始化的时候不应该重新加载任务
                        is_init_spinner = false;
                        return;
                    }
                    if(arg2!=0){
                        mission = createWaypointMission(adapter.getItem(arg2));
                        addPolyline(mission);
                        if(!GlobalVariable.isWaypointDoing) {
                            waypointMissionOperator.loadMission(mission);
                            toast("航线加载完成");
                            waypointMissionOperator.uploadMission(new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(GDUError error) {
                                    if (error == null) {
                                        toast("开始上传任务");
                                    } else {
                                        toast("上传航迹发送失败");
                                    }
                                }
                            });
                        }
                    }
                }

                public void onNothingSelected(AdapterView<?> arg0) {

                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private WaypointMissionOperator getWaypointMissionOperator() {
        if (null == waypointMissionOperator) {
            if (null != MissionControl.getInstance()) {
                return MissionControl.getInstance().getWaypointMissionOperator();
            }
        }
        return waypointMissionOperator;
    }

    private void setUpListener() {
        listener = new WaypointMissionOperatorListener() {

            @Override
            public void onUploadUpdate(WaypointMissionUploadEvent waypointMissionUploadEvent) {
                // Example of Upload Listener
//                if (waypointMissionUploadEvent.getProgress() != null
//                        && waypointMissionUploadEvent.getProgress().isSummaryUploaded
//                        && waypointMissionUploadEvent.getProgress().uploadedWaypointIndex == (WAYPOINT_COUNT - 1)) {
//                    toast("Mission is uploaded successfully");
//                }
//                show("Mission is uploaded successfully Progress " + waypointMissionUploadEvent.getProgress());
                if(waypointMissionUploadEvent.getProgress() <100){
                    toast("任务上传中，进度：" + waypointMissionUploadEvent.getProgress() + "%");
                }else {
                    toast("任务上传完成");
                }

//                updateWaypointMissionState();
            }

            @Override
            public void onExecutionUpdate(WaypointMissionExecutionEvent waypointMissionExecutionEvent) {
                // Example of Execution Listener
                show(waypointMissionExecutionEvent.getCurrentState().getName()
                        + (waypointMissionExecutionEvent.getProgress() == null
                        ? ""
                        : waypointMissionExecutionEvent.getProgress().targetWaypointIndex));
//                updateWaypointMissionState();
            }

            @Override
            public void onExecutionStart() {
                toast("Mission started");
//                updateWaypointMissionState();
            }

            @Override
            public void onExecutionFinish(GDUError djiError) {
//                show("Mission finished");
                toast("Mission finished");
//                updateWaypointMissionState();
            }
        };

        if (waypointMissionOperator != null && listener != null) {
            // Example of adding listeners
            waypointMissionOperator.addListener(listener);
        }
    }


    private void initListener() {
        if (mGDUCamera != null) {
            mGDUCamera.setSystemStateCallback(new SystemState.Callback() {
                @Override
                public void onUpdate(SystemState systemState) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(" isPhotoStored ");
                    sb.append(systemState.isPhotoStored());
                    sb.append(" hasError ");
                    sb.append(systemState.isHasError());
                    sb.append(" isRecording ");
                    sb.append(systemState.isRecording());
                    sb.append(" mode ");
                    sb.append(systemState.getMode());
                    sb.append(" time ");
                    sb.append(systemState.getCurrentVideoRecordingTimeInSeconds());
                    if (systemState.isPhotoStored()) {
                        toast(sb.toString());
                    }
                }
            });
        }
    }

    int index = 0;
    private void addPolyline(WaypointMission waypointMission){
        if(index == 0){
            index++;
            addPolyline(waypointMission.getWaypointList(), Color.argb(255, 1, 1, 1), 10);
        } else {
            addPolyline(waypointMission.getWaypointList(), Color.argb(255, 255, 1, 1), 10);
        }

    }

    private void addPolyline(List<Waypoint> planPoints, int color, int width){
        List<LatLng> latLngs = new ArrayList<>();
        if (planPoints == null) {
            return;
        }
        for (Waypoint waypoint : planPoints) {
            LocationCoordinate2D locationCoordinate2D = waypoint.getCoordinate();
            LatLng latLng = new LatLng(locationCoordinate2D.getLatitude(), locationCoordinate2D.getLongitude());
            coordinateConverter.coord(latLng);
            latLngs.add(coordinateConverter.convert());
        }
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngs.get(0), 19));
        mPlaneMarkerOptions = new MarkerOptions();
        mPlaneMarkerOptions.position(latLngs.get(0));

        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                R.mipmap.icon_plane).copy(Bitmap.Config.ARGB_8888, true);
        mPlaneMarkerOptions.anchor(0.5f, 0.5f);
        mPlaneMarkerOptions.icon(BitmapDescriptorFactory.fromBitmap(bitmap));
        mPlaneMarker = aMap.addMarker(mPlaneMarkerOptions);
        aMap.addPolyline(new PolylineOptions().addAll(latLngs).width(width).color(color));
    }

    private void initMap(Bundle savedInstanceState) {
        mMapView.onCreate(savedInstanceState);
        aMap = mMapView.getMap();
        aMap.setMapType(AMap.MAP_TYPE_SATELLITE);
        coordinateConverter = new CoordinateConverter(this);
        coordinateConverter.from(CoordinateConverter.CoordType.GPS);
    }

    private void startSimulator() {
        if (null != mGDUFlightController) {
            LocationCoordinate3D locationCoordinate3D = new LocationCoordinate3D(30.515442262332098,114.43098599032749, 10);
            InitializationData initializationData = new InitializationData(locationCoordinate3D, (short) 90, PositioningSolution.FIXED_POINT, (byte) 30);
            mGDUFlightController.getSimulator().start(initializationData, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(GDUError gduError) {
                    if (gduError == null) {
                        toast("模拟飞行开启");
                    }
                }
            });
        }
    }
    public void cleanWaypointMission(){
        aMap.clear(true);
    }


    public void onClick(View view){
        switch (view.getId()){
            case R.id.simulator_button:
                startSimulator();
                break;
            case R.id.get_mission:
                toast(waypointMissionOperator.getCurrentState().getName());
                break;

//            case R.id.load_waypoint_button:
////                cleanWaypointMission();
//                mission = createWaypointMission("已知类别海面目标.xml");
////                toast(""+mission.getWaypointList().size());
//                addPolyline(mission);
//                waypointMissionOperator.loadMission(mission);
//                break;
//            case R.id.upload_waypoint_button:
//                waypointMissionOperator.uploadMission(new CommonCallbacks.CompletionCallback() {
//                    @Override
//                    public void onResult(GDUError error) {
//                        if (error == null) {
//                            toast("上传航迹发送成功");
//                        } else {
//                            toast("上传航迹发送失败");
//                        }
//                    }
//                });
//                break;
            case R.id.start_waypoint_button:
                if(GlobalVariable.isWaypointDoing){
                    toast("任务状态不符，当前状态："+waypointMissionOperator.getCurrentState().getName());
                    break;
                }
                if (validMissionStartStates.contains(waypointMissionOperator.getCurrentState())) {
                    waypointMissionOperator.startMission(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(GDUError error) {
                            if (error == null) {
                                toast("开始航迹成功");
                            } else {

                                toast("开始航迹失败"+error);
                            }
                        }
                    });
                }else {
                    toast("任务状态不符，当前状态："+waypointMissionOperator.getCurrentState().getName());
                }
                break;
            case R.id.resume_waypoint_button:
                waypointMissionOperator.resumeMission(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(GDUError error) {
                        if (error == null) {
                            toast("继续航迹成功");
                        } else {
                            toast("继续航迹失败");
                        }
                    }
                });
                break;
            case R.id.pause_waypoint_button:
                waypointMissionOperator.pauseMission(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(GDUError error) {
                        if (error == null) {
                            toast("暂停航迹成功");
                        } else {
                            toast("暂停航迹失败");
                        }
                    }
                });
                break;
            case R.id.stop_waypoint_button:
                waypointMissionOperator.stopMission(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(GDUError error) {
                        if (error == null) {
                            toast("停止航迹成功");
                        } else {
                            toast("停止航迹失败");
                        }
                    }
                });
                break;
            case R.id.btn_back_to_flight:
                if(getIntent().getStringExtra("source_class")!=null){
                    finish();
                }else{
                    Intent intent = new Intent(WaypointMissionOperatorActivity.this, FlightActivity.class);
                    intent.putExtra("source_class", WaypointMissionOperatorActivity.class.getName());
                    startActivity(intent);
                }
                break;
        }
    }
    private WaypointMission createWaypointMission(String xml_name) {
//        // 读取xml文件
        NodeList sList;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            AssetManager assetManager=getAssets();
            InputStream stream=assetManager.open("waypoints/" + xml_name);

            Document d = builder.parse(stream);
            sList = d.getElementsByTagName("Placemark");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        WaypointMission waypointMission = new WaypointMission();
        waypointMission.setAutoFlightSpeed(3f);
        waypointMission.setMaxFlightSpeed(10f);
        waypointMission.setResponseLostActionOnRCSignalLost(false);
        waypointMission.setFinishedAction(WaypointMissionFinishedAction.GO_HOME);
        waypointMission.setHeadingMode(WaypointMissionHeadingMode.AUTO);
        waypointMission.setGimbalPitchRotationEnabled(true);

        List<Waypoint> waypointList = new ArrayList<>();
        Waypoint waypoint;
        float gimbal_pitch;
        float gimbal_rotate;
        for (int i = 0; i < sList.getLength(); i++) {
            Element element = (Element) sList.item(i);
            NodeList childNodes = element.getElementsByTagName("coordinates");
            String waypoint_position = childNodes.item(0).getTextContent();
            String s[] = waypoint_position.split(",");
            double Longitude = Double.parseDouble(s[0]);
            double Latitude = Double.parseDouble(s[1]);
            float Altitude = Float.parseFloat(s[2]);
//            float Altitude=10f;
            waypoint =  new Waypoint(Latitude, Longitude, Altitude);
            waypoint.setSpeed(0.5);
//            waypoint.setSpeed(Integer.parseInt(element.getElementsByTagName("speed").item(0).getTextContent()));
            gimbal_pitch = Float.parseFloat(element.getElementsByTagName("gimbalAngle").item(0).getTextContent());
            gimbal_rotate=Float.parseFloat(element.getElementsByTagName("droneHeadAngle").item(0).getTextContent());
            waypoint.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT,(int)gimbal_rotate));
//            waypoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));//拍照比较耗时
            waypoint.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, (int) gimbal_pitch));
//            waypoint.setSpeed(3);
//            waypoint.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT,720));
//            waypoint.addAction(new WaypointAction(WaypointActionType.STAY, 30));
            waypoint.setGimbalPitch(-90);
            waypointList.add(waypoint);
//            show("高度"+Altitude+"经度"+Latitude+"维度"+Latitude+"云台角度"+gimbal_pitch+"旋转角度"+gimbal_rotate);
        }
//
//        WaypointMission waypointMission = new WaypointMission();
////        double baseLatitude = 30.471033;
////        double baseLongitude = 114.4280014;
//        double baseLatitude = 30.510297755488512;
//        double baseLongitude = 114.40938773071838;
//
////
////
////        final float baseAltitude = 30.0f;
//        waypointMission.setAutoFlightSpeed(5f);
//        waypointMission.setMaxFlightSpeed(10f);
//        waypointMission.setResponseLostActionOnRCSignalLost(false);
//        waypointMission.setFinishedAction(WaypointMissionFinishedAction.GO_HOME);
//        waypointMission.setHeadingMode(WaypointMissionHeadingMode.AUTO);
////      builder.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY);  waypointMission.setGotoFirstWaypointMode(WaypointMissionFlightPathMode.NORMAL);
////      builder.setPointOfInterest(new LocationCoordinate2D(15, 15));
////      builder.headingMode(WaypointMissionHeadingMode.TOWARD_POINT_OF_INTEREST);
//        waypointMission.setGimbalPitchRotationEnabled(true);
//
//        List<Waypoint> waypointList = new ArrayList<>();
//
//
//
//        final float baseAltitude = 50.0f;
//
//
//        // Waypoint 0: (0,0)
//
//        Waypoint waypoint0 = new Waypoint(baseLatitude, baseLongitude, baseAltitude);
////        waypoint0.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT,720));
////        waypoint0.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
////        waypoint0.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -90));
////        waypoint0.addAction(new WaypointAction(WaypointActionType.STAY,1));
//        waypoint0.setSpeed(5);
//        waypoint0.setGimbalPitch(-90);
//        waypointList.add(waypoint0);
//
//
//        // Waypoint 1: (0,30)
//        baseLongitude=114.40937030114404;
//        baseLatitude=30.510077128267262;
//        Waypoint waypoint1 = new Waypoint(baseLatitude, baseLongitude , baseAltitude);
////        waypoint1.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, 720));
////        waypoint1.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -45));
//        waypoint1.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 1));
////        waypoint1.addAction(new WaypointAction(WaypointActionType.STAY,1));
//        waypoint1.setSpeed(10);
//        waypoint1.setGimbalPitch(-90);
//        waypointList.add(waypoint1);
//
//
//        // Waypoint 2: (30,30)
//        baseLongitude=114.410590424279;
//        baseLatitude=30.509974739031247;
//        Waypoint waypoint2 = new Waypoint(baseLatitude , baseLongitude , baseAltitude);
////        waypoint2.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, 720));
//        waypoint2.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
////        waypoint2.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -90));
////        waypoint1.addAction(new WaypointAction(WaypointActionType.STAY,1));
//        waypoint2.setSpeed(10);
//        waypoint2.setGimbalPitch(-90);
//        waypointList.add(waypoint2);
//
//        // Waypoint 3: (30,0)
//        baseLongitude=114.41067625395226;
//        baseLatitude=30.5108653410641;
//        Waypoint waypoint3 = new Waypoint(baseLatitude, baseLongitude, baseAltitude);
////        waypoint3.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, 720));
//        waypoint3.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
////        waypoint3.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -90));
////        waypoint1.addAction(new WaypointAction(WaypointActionType.STAY,1));
//        waypoint3.setSpeed(10);
//        waypoint3.setGimbalPitch(-90);
//        waypointList.add(waypoint3);
//
//        // Waypoint 4: (30,0)
//        baseLongitude=114.40948968094305;
//        baseLatitude=30.510970045329092;
//        Waypoint waypoint4 = new Waypoint(baseLatitude, baseLongitude, baseAltitude);
////        waypoint4.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, 720));
//        waypoint4.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
////        waypoint4.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -90));
////        waypoint1.addAction(new WaypointAction(WaypointActionType.STAY,1));
//        waypoint4.setSpeed(10);
//        waypoint4.setGimbalPitch(-90);
//        waypointList.add(waypoint4);
//
//        // Waypoint 4: (30,0)
//        baseLongitude=114.4093863896088;
//        baseLatitude=30.51028504911789;
//        Waypoint waypoint5 = new Waypoint(baseLatitude, baseLongitude, baseAltitude);
////        waypoint5.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, 720));
//        waypoint5.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
////        waypoint5.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -90));
//        waypoint5.setSpeed(10);
//        waypoint5.setGimbalPitch(-90);
//        waypointList.add(waypoint5);
//
        waypointMission.setWaypointCount(waypointList.size());
        waypointMission.setWaypointList(waypointList);
//        toast(""+waypointList.size());
        return waypointMission;
    }

//    private WaypointMission createWaypointMission;() {
//        WaypointMission waypointMission = new WaypointMission();
////        double baseLatitude = 30.471033;
////        double baseLongitude = 114.4280014;
//        double baseLatitude = 30.510297755488512;
//        double baseLongitude = 114.40938773071838;
//
////
////
////        final float baseAltitude = 30.0f;
//        waypointMission.setAutoFlightSpeed(5f);
//        waypointMission.setMaxFlightSpeed(10f);
//        waypointMission.setResponseLostActionOnRCSignalLost(false);
//        waypointMission.setFinishedAction(WaypointMissionFinishedAction.GO_HOME);
//        waypointMission.setHeadingMode(WaypointMissionHeadingMode.AUTO);
////      builder.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY);  waypointMission.setGotoFirstWaypointMode(WaypointMissionFlightPathMode.NORMAL);
////      builder.setPointOfInterest(new LocationCoordinate2D(15, 15));
////      builder.headingMode(WaypointMissionHeadingMode.TOWARD_POINT_OF_INTEREST);
//        waypointMission.setGimbalPitchRotationEnabled(true);
//
//        List<Waypoint> waypointList = new ArrayList<>();
//
//
//
//        final float baseAltitude = 50.0f;
//
//
//        // Waypoint 0: (0,0)
//
//        Waypoint waypoint0 = new Waypoint(baseLatitude, baseLongitude, baseAltitude);
//        waypoint0.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT,720));
////        waypoint0.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
//        waypoint0.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -90));
////        waypoint0.addAction(new WaypointAction(WaypointActionType.STAY,1));
//        waypoint0.setSpeed(5);
//        waypoint0.setGimbalPitch(-90);
//        waypointList.add(waypoint0);
//
//
//        // Waypoint 1: (0,30)
//        baseLongitude=114.40937030114404;
//        baseLatitude=30.510077128267262;
//        Waypoint waypoint1 = new Waypoint(baseLatitude, baseLongitude , baseAltitude);
//        waypoint1.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, 720));
//        waypoint1.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -45));
////        waypoint1.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 1));
////        waypoint1.addAction(new WaypointAction(WaypointActionType.STAY,1));
//        waypoint1.setSpeed(10);
//        waypoint1.setGimbalPitch(-90);
//        waypointList.add(waypoint1);
//
//
//        // Waypoint 2: (30,30)
//        baseLongitude=114.410590424279;
//        baseLatitude=30.509974739031247;
//        Waypoint waypoint2 = new Waypoint(baseLatitude , baseLongitude , baseAltitude);
////        waypoint2.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, 720));
//        waypoint2.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
////        waypoint2.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -90));
////        waypoint1.addAction(new WaypointAction(WaypointActionType.STAY,1));
//        waypoint2.setSpeed(10);
//        waypoint2.setGimbalPitch(-90);
//        waypointList.add(waypoint2);
//
//        // Waypoint 3: (30,0)
//        baseLongitude=114.41067625395226;
//        baseLatitude=30.5108653410641;
//        Waypoint waypoint3 = new Waypoint(baseLatitude, baseLongitude, baseAltitude);
////        waypoint3.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, 720));
//        waypoint3.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
////        waypoint3.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -90));
////        waypoint1.addAction(new WaypointAction(WaypointActionType.STAY,1));
//        waypoint3.setSpeed(10);
//        waypoint3.setGimbalPitch(-90);
//        waypointList.add(waypoint3);
//
//        // Waypoint 4: (30,0)
//        baseLongitude=114.40948968094305;
//        baseLatitude=30.510970045329092;
//        Waypoint waypoint4 = new Waypoint(baseLatitude, baseLongitude, baseAltitude);
////        waypoint4.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, 720));
//        waypoint4.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
////        waypoint4.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -90));
//        waypoint4.addAction(new WaypointAction(WaypointActionType.STAY,1));
//        waypoint4.setSpeed(10);
//        waypoint4.setGimbalPitch(-90);
//        waypointList.add(waypoint4);
//
//        // Waypoint 4: (30,0)
//        baseLongitude=114.4093863896088;
//        baseLatitude=30.51028504911789;
//        Waypoint waypoint5 = new Waypoint(baseLatitude, baseLongitude, baseAltitude);
////        waypoint5.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, 720));
//        waypoint5.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
////        waypoint5.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -90));
//        waypoint5.setSpeed(10);
//        waypoint5.setGimbalPitch(-90);
//        waypointList.add(waypoint5);
//
//        // Waypoint 0: (0,0)
////        Waypoint waypoint0 = new Waypoint(baseLatitude, baseLongitude, baseAltitude);
////        waypoint0.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT,0 + calculateTurnAngle()));
////        waypoint0.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
////        waypoint0.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -90));
////        waypoint0.setSpeed(5);
////        waypoint0.setGimbalPitch(-90);
////        waypointList.add(waypoint0);
////
////        // Waypoint 1: (0,30)
////        Waypoint waypoint1 = new Waypoint(baseLatitude, baseLongitude + HORIZONTAL_DISTANCE * ONE_METER_OFFSET, baseAltitude);
////        waypoint1.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, 0 - calculateTurnAngle()));
////        waypoint1.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
////        waypoint1.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -45));
////        waypoint1.setSpeed(5);
////        waypoint1.setGimbalPitch(-45);
////        waypointList.add(waypoint1);
////
////        // Waypoint 2: (30,30)
////        Waypoint waypoint2 = new Waypoint(baseLatitude + VERTICAL_DISTANCE * ONE_METER_OFFSET, baseLongitude + HORIZONTAL_DISTANCE * ONE_METER_OFFSET, baseAltitude);
////        waypoint2.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, -180 + calculateTurnAngle()));
////        waypoint2.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
////        waypoint2.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -90));
////        waypoint2.setSpeed(5);
////        waypoint2.setGimbalPitch(-90);
////        waypointList.add(waypoint2);
////
////        // Waypoint 3: (30,0)
////        Waypoint waypoint3 = new Waypoint(baseLatitude + VERTICAL_DISTANCE * ONE_METER_OFFSET, baseLongitude, baseAltitude);
////        waypoint3.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, 180 - calculateTurnAngle()));
////        waypoint3.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
////        waypoint3.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, 0));
////        waypoint3.setSpeed(5);
////        waypoint3.setGimbalPitch(0);
////        waypointList.add(waypoint3);
//
//        waypointMission.setWaypointCount(waypointList.size());
//        waypointMission.setWaypointList(waypointList);
//        return waypointMission;
//    }


//    private WaypointMission createWaypointMission() {
//        WaypointMission waypointMission = new WaypointMission();
//        double baseLatitude = 30.471033;
//        double baseLongitude = 114.4280014;
//
//
//        final float baseAltitude = 30.0f;
//        waypointMission.setAutoFlightSpeed(5f);
//        waypointMission.setMaxFlightSpeed(10f);
//        waypointMission.setResponseLostActionOnRCSignalLost(false);
//        waypointMission.setFinishedAction(WaypointMissionFinishedAction.GO_HOME);
//        waypointMission.setHeadingMode(WaypointMissionHeadingMode.AUTO);
////      builder.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY);  waypointMission.setGotoFirstWaypointMode(WaypointMissionFlightPathMode.NORMAL);
////      builder.setPointOfInterest(new LocationCoordinate2D(15, 15));
////      builder.headingMode(WaypointMissionHeadingMode.TOWARD_POINT_OF_INTEREST);
//        waypointMission.setGimbalPitchRotationEnabled(true);
//
//        List<Waypoint> waypointList = new ArrayList<>();
//
//        // Waypoint 0: (0,0)
//        Waypoint waypoint0 = new Waypoint(baseLatitude, baseLongitude, baseAltitude);
////        waypoint0.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT,0 + calculateTurnAngle()));
////        waypoint0.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
//        waypoint0.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -90));
//        waypoint0.setSpeed(5);
//        waypoint0.setGimbalPitch(-90);
//        waypointList.add(waypoint0);
//
//        // Waypoint 1: (0,30)
//        Waypoint waypoint1 = new Waypoint(baseLatitude, baseLongitude + HORIZONTAL_DISTANCE * ONE_METER_OFFSET, baseAltitude);
//        waypoint1.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, 0 - calculateTurnAngle()));
//        waypoint1.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
//        waypoint1.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -45));
//        waypoint1.setSpeed(5);
//        waypoint1.setGimbalPitch(-45);
//        waypointList.add(waypoint1);
//
//        // Waypoint 2: (30,30)
//        Waypoint waypoint2 = new Waypoint(baseLatitude + VERTICAL_DISTANCE * ONE_METER_OFFSET, baseLongitude + HORIZONTAL_DISTANCE * ONE_METER_OFFSET, baseAltitude);
//        waypoint2.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, -180 + calculateTurnAngle()));
//        waypoint2.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
//        waypoint2.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -90));
//        waypoint2.setSpeed(5);
//        waypoint2.setGimbalPitch(-90);
//        waypointList.add(waypoint2);
//
//        // Waypoint 3: (30,0)
//        Waypoint waypoint3 = new Waypoint(baseLatitude + VERTICAL_DISTANCE * ONE_METER_OFFSET, baseLongitude, baseAltitude);
//        waypoint3.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, 180 - calculateTurnAngle()));
//        waypoint3.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
//        waypoint3.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, 0));
//        waypoint3.setSpeed(5);
//        waypoint3.setGimbalPitch(0);
//        waypointList.add(waypoint3);
//
//        waypointMission.setWaypointCount(waypointList.size());
//        waypointMission.setWaypointList(waypointList);
//        return waypointMission;
//    }

    private int calculateTurnAngle() {
        return Math.round((float)Math.toDegrees(Math.atan(VERTICAL_DISTANCE/ HORIZONTAL_DISTANCE)));
    }


    public void toast(final String toast){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                mVideoPicTextView.setText(toast);
                Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void show(final String toast){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMissionInfoTextView.setText(toast);
//                Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {

    }

    @Override
    public void deactivate() {

    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }
}