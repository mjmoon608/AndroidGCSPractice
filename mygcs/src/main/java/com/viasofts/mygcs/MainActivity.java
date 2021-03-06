package com.viasofts.mygcs;

//드론 연결하는 부분 추가하고 정보 받아오는 거 확인하면 됌.

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.LocationOverlay;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.android.client.utils.video.MediaCodecManager;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.companion.solo.SoloAttributes;
import com.o3dr.services.android.lib.drone.companion.solo.SoloState;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.connection.ConnectionType;
import com.o3dr.services.android.lib.drone.mission.item.command.YawCondition;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.GuidedState;
import com.o3dr.services.android.lib.drone.property.Home;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.SimpleCommandListener;

import org.droidplanner.services.android.impl.core.drone.variables.Camera;
import org.droidplanner.services.android.impl.core.gcs.follow.Follow;
import org.w3c.dom.Text;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, DroneListener, TowerListener, LinkListener {
    private NaverMap mMap;


    private static final String TAG = MainActivity.class.getSimpleName();

    private Drone drone;
    private int droneType = Type.TYPE_UNKNOWN;
    private ControlTower controlTower;
    private final Handler handler = new Handler();

    private Spinner modeSelector;

    private Button btnConnect;
    private EditText takeOffAltitudeET;
    private Button armButton;

    private LatLong dronePosition;
    private Marker droneMarker = new Marker();

    private static final int DEFAULT_UDP_PORT = 14550;
    private static final int DEFAULT_USB_BAUD_RATE = 57600;

    static LatLng mGuidedPoint; //가이드모드 목적지 저장
    static Marker mMarkerGuide = new com.naver.maps.map.overlay.Marker(); //GCS 위치 표시  마커 옵션
    static OverlayImage guideIcon = OverlayImage.fromResource(R.drawable.droneimg); // 가이드 모드 아이콘 설정

    private ArrayList<LatLng> guideArray = new ArrayList<>();
    private ArrayList<Marker> guideMarkers = new ArrayList<>();
    private int guideMarker_index = 0;

    private boolean isGoing = false; // 가이드모드로 목적지까지 이동중인지 아닌지 판단.

    //private Spinner modeSelector;

    Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnConnect = (Button) findViewById(R.id.btnConnect);
        takeOffAltitudeET = (EditText) findViewById(R.id.inputAltitude);
        armButton = (Button) findViewById(R.id.btnArmTakeOff);

        final Context context = getApplicationContext();
        this.controlTower = new ControlTower(context);
        this.drone = new Drone(context);


        this.modeSelector = (Spinner) findViewById(R.id.modeSelect);
        this.modeSelector.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onFlightModeSelected(view);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });


        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }

        mapFragment.getMapAsync(this);

/*
        this.modeSelector = (Spinner) findViewById(R.id.modeSelect);
        this.modeSelector.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onFlightModeSelected(view);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
 */
        mainHandler = new Handler(getApplicationContext().getMainLooper());


    }


    @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect(this);
        updateVehicleModesForType(this.droneType);
    }

    protected void updateVehicleModesForType(int droneType) {

        List<VehicleMode> vehicleModes = VehicleMode.getVehicleModePerDroneType(droneType);
        ArrayAdapter<VehicleMode> vehicleModeArrayAdapter = new ArrayAdapter<VehicleMode>(this, android.R.layout.simple_spinner_item, vehicleModes);
        vehicleModeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.modeSelector.setAdapter(vehicleModeArrayAdapter);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.drone.isConnected()) {
            this.drone.disconnect();
            //updateConnectedButton(false);
        }

        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
    }

    @Override
    public void onDroneEvent(String event, Bundle extras) {
        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                alertUser("Drone Connected");
//                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
                checkSoloState();
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("Drone Disconnected");
//                updateConnectedButton(this.drone.isConnected());
                updateArmButton();

                break;

            case AttributeEvent.STATE_UPDATED:

            case AttributeEvent.STATE_ARMING:
                updateArmButton();
                break;

            case AttributeEvent.TYPE_UPDATED:
                Type newDroneType = this.drone.getAttribute(AttributeType.TYPE);
                if (newDroneType.getDroneType() != this.droneType) {
                    this.droneType = newDroneType.getDroneType();
                    updateVehicleModesForType(this.droneType);

                }
                break;

            case AttributeEvent.STATE_VEHICLE_MODE:
                updateVehicleMode();
                break;

            case AttributeEvent.SPEED_UPDATED:
                updateSpeed();
                break;

            case AttributeEvent.ALTITUDE_UPDATED:
                updateAltitude();
                break;

            case AttributeEvent.HOME_UPDATED:
                updateDistanceFromHome();
                break;

            case AttributeEvent.BATTERY_UPDATED:
                updateBattery();
                break;

            case AttributeEvent.ATTITUDE_UPDATED:
                updateYaw();
                break;


            case AttributeEvent.GPS_POSITION:
                updateSatCnt();
                updateDronePosition();
                break;


            default:
                // Log.i("DRONE_EVENT", event); //Uncomment to see events from the drone
                break;
        }
    }


    public void onBtnConnectTap(View view) {
        if (this.drone.isConnected()) {
            this.drone.disconnect();
        } else {
            int selectedConnectionType = 1;

            ConnectionParameter connectionParams = selectedConnectionType == ConnectionType.TYPE_USB
                    ? ConnectionParameter.newUsbConnection(null)
                    : ConnectionParameter.newUdpConnection(null);

            this.drone.connect(connectionParams);
        }

    }

    public void onArmButtonTap(View view) {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        if (vehicleState.isFlying()) {
            // Land
            VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LAND, new SimpleCommandListener() {
                @Override
                public void onError(int executionError) {
                    alertUser("Unable to land the vehicle.");
                }

                @Override
                public void onTimeout() {
                    alertUser("Unable to land the vehicle.");
                }
            });
        } else if (vehicleState.isArmed()) {
            // Take off
            //FC에 동그란 LED가 녹색 불이 떠야 자동 이륙 가능.
            Double inputAltitude = Double.parseDouble(takeOffAltitudeET.getText().toString());
            ControlApi.getApi(this.drone).takeoff(inputAltitude, new AbstractCommandListener() {

                @Override
                public void onSuccess() {
                    alertUser("Taking off...");
//                    AlertDialog.Builder oDialog = new AlertDialog.Builder(getApplicationContext(),
//                            android.R.style.Theme_DeviceDefault_Light_Dialog);
//
//                    oDialog.setMessage("지정한 이륙 고도까지 기체가 상승합니다.\n안전거리를 유지하세요.")
//                            .setTitle("일반 Dialog")
//                            .setPositiveButton("아니오", new DialogInterface.OnClickListener()
//                            {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which)
//                                {
//                                    Log.i("Dialog", "취소");
//                                    Toast.makeText(getApplicationContext(), "취소", Toast.LENGTH_LONG).show();
//                                    onError(123);
//                                }
//                            })
//                            .setNeutralButton("예", new DialogInterface.OnClickListener()
//                            {
//                                public void onClick(DialogInterface dialog, int which)
//                                {
//                                    finish();
//                                }
//                            })
//                            .setCancelable(false) // 백버튼으로 팝업창이 닫히지 않도록 한다.
//                            .show();
                }

                @Override
                public void onError(int i) {
//                    alertUser("Unable to take off.");
                    alertUser("비행 오류");
                }

                @Override
                public void onTimeout() {
                    alertUser("Unable to take off.");
                }
            });
        } else if (!vehicleState.isConnected()) {
            // Connect
            alertUser("Connect to a drone first");
        } else {
            // Connected but not Armed
            VehicleApi.getApi(this.drone).arm(true, false, new SimpleCommandListener() {
                @Override
                public void onError(int executionError) {
                    alertUser("Unable to arm vehicle.");
                }

                @Override
                public void onTimeout() {
                    alertUser("Arming operation timed out.");
                }
            });
        }
    }

    private void updateArmButton() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);


        if (!this.drone.isConnected()) {
            armButton.setVisibility(View.INVISIBLE);
        } else {
            armButton.setVisibility(View.VISIBLE);
        }

        if (vehicleState.isFlying()) {
            // Land
            armButton.setText("LAND");
        } else if (vehicleState.isArmed()) {
            // Take off
            armButton.setText("TAKE OFF");
        } else if (vehicleState.isConnected()) {
            // Connected but not Armed
            armButton.setText("ARM");
        }
    }

    private void updateSatCnt() {
        TextView satelliteTextView = (TextView) findViewById(R.id.satelliteValueTextView);
        Gps droneSatCnt = this.drone.getAttribute(AttributeType.GPS);
        satelliteTextView.setText(String.format("%d", droneSatCnt.getSatellitesCount()) + "개");
        //위성의 갯수가 9개 이하일때 Arm 버튼 등 뜨지 않게 하고 만약 Take Off 상태일 때 떨어지면 자동으로 Land 할 수 있도록.
    }

    private void updateDronePosition() {
        Gps loadGps = this.drone.getAttribute(AttributeType.GPS);
        this.dronePosition = loadGps.getPosition();
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);


        if (dronePosition != null) {
            this.droneMarker.setPosition(new LatLng(dronePosition.getLatitude(), dronePosition.getLongitude()));
//            this.droneMarker.setMap(mMap);
            CameraUpdate cameraUpdate = CameraUpdate.scrollTo(new LatLng(dronePosition.getLatitude(), dronePosition.getLongitude()));
            mMap.moveCamera(cameraUpdate);

            LocationOverlay locationOverlay = mMap.getLocationOverlay();
            locationOverlay.setVisible(true);
            locationOverlay.setPosition(new LatLng(dronePosition.getLatitude(), dronePosition.getLongitude()));

            locationOverlay.setIcon(OverlayImage.fromResource(R.drawable.droneimg));
            locationOverlay.setIconWidth(40);
            locationOverlay.setIconHeight(40);

            locationOverlay.setAnchor(new PointF(0.5f, 1));
        }

        if (isGoing) {
            if (checkGoal(drone, new LatLng(dronePosition.getLatitude(), dronePosition.getLongitude()))) {
                try {
                    vehicleState.setVehicleMode(VehicleMode.COPTER_LOITER);
                }catch(Exception e){
                    armButton.performClick();
                }
                isGoing = false;
            }
        }


//        Log.d("position test", dronePosition.getLatitude()+"");

    }


    private void updateYaw() {
        TextView yawTextView = (TextView) findViewById(R.id.yawValueTextView);
        Attitude droneYaw = this.drone.getAttribute(AttributeType.ATTITUDE);
        double bearingYaw;
        if (droneYaw.getYaw() < 0) {
            yawTextView.setText(String.format("%3.1f", droneYaw.getYaw() * (-1)) + "deg");
            bearingYaw = Double.parseDouble(String.format("%3.1f", droneYaw.getYaw() * (-1)));
        } else {
            yawTextView.setText(String.format("%3.1f", droneYaw.getYaw()) + "deg");
            bearingYaw = Double.parseDouble(String.format("%3.1f", droneYaw.getYaw()));
        }
//        Log.d("Yaw Type: ", droneYaw.getYaw() + "");
        //드론 Yaw값으로 맵 이동하는 거 추가한건데 되는지 확인을 못해봄.
//        CameraPosition cameraPosition = new CameraPosition(
//                new LatLng(dronePosition.getLatitude(), dronePosition.getLongitude()), // 대상 지점
//                16, // 줌 레벨
//                0, // 기울임 각도
//                bearingYaw // 베어링 각도
//        );
//        mMap.setCameraPosition(cameraPosition);

    }

    private void updateBattery() {
        TextView batteryTextView = (TextView) findViewById(R.id.voltageValueTextView);
        Battery droneBattery = this.drone.getAttribute(AttributeType.BATTERY);
        batteryTextView.setText(String.format("%3.1f", droneBattery.getBatteryVoltage()) + "V");

    }

    protected void updateAltitude() {
        TextView altitudeTextView = (TextView) findViewById(R.id.altitudeValueTextView);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        if (droneAltitude.getAltitude() < 0) {
            altitudeTextView.setText(String.format("%3.1f", droneAltitude.getAltitude() * (-1)) + "m");
        } else {
            altitudeTextView.setText(String.format("%3.1f", droneAltitude.getAltitude()) + "m");
        }


    }

    protected void updateSpeed() {
        TextView speedTextView = (TextView) findViewById(R.id.speedValueTextView);
        Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
        speedTextView.setText(String.format("%3.1f", droneSpeed.getGroundSpeed()) + "m/s");
    }

    protected void updateDistanceFromHome() {
        TextView distanceTextView = (TextView) findViewById(R.id.distanceValueTextView);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        double vehicleAltitude = droneAltitude.getAltitude();
        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
        LatLong vehiclePosition = droneGps.getPosition();

        double distanceFromHome = 0;

        if (droneGps.isValid()) {
            LatLongAlt vehicle3DPosition = new LatLongAlt(vehiclePosition.getLatitude(), vehiclePosition.getLongitude(), vehicleAltitude);
            Home droneHome = this.drone.getAttribute(AttributeType.HOME);
            distanceFromHome = distanceBetweenPoints(droneHome.getCoordinate(), vehicle3DPosition);
        } else {
            distanceFromHome = 0;
        }

        distanceTextView.setText(String.format("%3.1f", distanceFromHome) + "m");
    }


    protected void updateVehicleMode() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        ArrayAdapter arrayAdapter = (ArrayAdapter) this.modeSelector.getAdapter();
        this.modeSelector.setSelection(arrayAdapter.getPosition(vehicleMode));
    }


    private void checkSoloState() {
        final SoloState soloState = drone.getAttribute(SoloAttributes.SOLO_STATE);
        if (soloState == null) {
            alertUser("Unable to retrieve the solo state.");
        } else {
            alertUser("Solo state is up to date.");
        }
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }

    @Override
    public void onLinkStateUpdated(@NonNull LinkConnectionStatus connectionStatus) {
        switch (connectionStatus.getStatusCode()) {
            case LinkConnectionStatus.FAILED:
                Bundle extras = connectionStatus.getExtras();
                String msg = null;
                if (extras != null) {
                    msg = extras.getString(LinkConnectionStatus.EXTRA_ERROR_MSG);
                }
                alertUser("Connection Failed:" + msg);
                break;
        }
    }

    @Override
    public void onTowerConnected() {
        alertUser("DroneKit-Android Connected");
        this.controlTower.registerDrone(this.drone, this.handler);
        this.drone.registerDroneListener(this);
        btnConnect.performClick();
    }

    @Override
    public void onTowerDisconnected() {
        alertUser("DroneKit-Android Interrupted");
    }

    // Helper methods
    // ==========================================================

    protected void alertUser(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, message);
    }

    private void runOnMainThread(Runnable runnable) {
        mainHandler.post(runnable);
    }

    protected double distanceBetweenPoints(LatLongAlt pointA, LatLongAlt pointB) {
        if (pointA == null || pointB == null) {
            return 0;
        }
        double dx = pointA.getLatitude() - pointB.getLatitude();
        double dy = pointA.getLongitude() - pointB.getLongitude();
        double dz = pointA.getAltitude() - pointB.getAltitude();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }


    public void onFlightModeSelected(View view) {
        VehicleMode vehicleMode = (VehicleMode) this.modeSelector.getSelectedItem();

        VehicleApi.getApi(this.drone).setVehicleMode(vehicleMode, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Vehicle mode change successful.");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Vehicle mode change failed: " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Vehicle mode change timed out.");
            }
        });
    }


    private void GuideModeDialog(final Drone drone, final LatLong point) {

        AlertDialog.Builder alt_bld = new AlertDialog.Builder(MainActivity.this);
        alt_bld.setMessage("확인하시면 가이드모드로 전환후 기체가 이동합니다.").setCancelable(false).setPositiveButton("확인", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
// Action for 'Yes' Button
                VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_GUIDED,
                        new AbstractCommandListener() {
                            @Override

                            public void onSuccess() {
                                isGoing = true;
                                //guideMode LatLng ArrayList 확인 후 계속 반복해야하는데 checkGoal도 수정해야할 듯
                                ControlApi.getApi(drone).goTo(point, true, null);
                            }

                            @Override

                            public void onError(int i) {
                                isGoing = false;
                                guideMarker_index = 0;
                                //guideArray, guideMarkers 삭제 추가
                                for(int j = 0 ; j<guideMarkers.size() ; j++){
                                    guideMarkers.get(j).setMap(null);
                                }
                                guideArray.clear();
                                guideMarkers.clear();
                            }

                            @Override
                            public void onTimeout() {
                                isGoing = false;
                                guideMarker_index = 0;
                                //guideArray, guideMarkers 삭제 추가
                                for(int j = 0 ; j<guideMarkers.size() ; j++){
                                    guideMarkers.get(j).setMap(null);
                                }
                                guideArray.clear();
                                guideMarkers.clear();
                            }
                        });
            }
        }).setNegativeButton("취소", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = alt_bld.create();
// Title for AlertDialog
        alert.setTitle("Title");
// Icon for AlertDialog
        alert.setIcon(R.drawable.droneimg);
        alert.show();
    }

    public static boolean checkGoal(final Drone drone, LatLng recentLatLng) {
        GuidedState guidedState = drone.getAttribute(AttributeType.GUIDED_STATE);
        LatLng target = new LatLng(guidedState.getCoordinate().getLatitude(),
                guidedState.getCoordinate().getLongitude());
        return target.distanceTo(recentLatLng) <= 1;
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.mMap = naverMap;
        mMap.setMapType(NaverMap.MapType.Satellite);


        UiSettings uiSettings = mMap.getUiSettings();
        uiSettings.setScaleBarEnabled(false);
        uiSettings.setZoomControlEnabled(false);

        mMap.setOnMapLongClickListener(new NaverMap.OnMapLongClickListener() {

            @Override
            public void onMapLongClick(@NonNull PointF pointF, @NonNull LatLng latLng) {
//                Marker goalMarker = new Marker();
//                goalMarker.setPosition(new LatLng(latLng.latitude, latLng.longitude));
//                goalMarker.setMap(mMap);
                Marker touchMarker = new Marker();

                guideArray.add(new LatLng(latLng.latitude, latLng.longitude));
                touchMarker.setPosition(new LatLng(latLng.latitude, latLng.longitude));
                guideMarkers.add(touchMarker);
                guideMarkers.get(guideMarker_index).setMap(mMap);


                GuideModeDialog(drone, new LatLong(latLng.latitude, latLng.longitude));
                guideMarker_index++;
            }
        });


    }


}

//롱 클릭을 한다 해당 좌표값을 배열에 넣는다. -> 추가 경로 입력할 것인지 다이얼로그로 여부 확인. ->(No) GuideMode실행한다. -> 배열에 들어있는 갯수만큼 GuideMode 반복 실행한다. 배열의 첫번째 뺴고 하면 될 둣.
//                                          ->(YES) 추가 경로가 있을 시 롱 클릭 이벤트로 배열에 계속 넣는다. -> 새로운 좌표 넣을 때마다 다이얼로그로 확인.
