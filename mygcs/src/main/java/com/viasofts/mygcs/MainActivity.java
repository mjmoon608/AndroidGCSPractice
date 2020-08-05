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

    private LatLong dronePosition;
    private Marker droneMarker = new Marker();

    private static final int DEFAULT_UDP_PORT = 14550;
    private static final int DEFAULT_USB_BAUD_RATE = 57600;

    //private Spinner modeSelector;

    Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnConnect = (Button)findViewById(R.id.btnConnect);
        takeOffAltitudeET = (EditText)findViewById(R.id.inputAltitude);

        final Context context = getApplicationContext();
        this.controlTower = new ControlTower(context);
        this.drone = new Drone(context);


        this.modeSelector = (Spinner)findViewById(R.id.modeSelect);
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
        MapFragment mapFragment = (MapFragment)fm.findFragmentById(R.id.map);
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
//                    alertUser("Taking off...");
                    AlertDialog.Builder oDialog = new AlertDialog.Builder(getApplicationContext(),
                            android.R.style.Theme_DeviceDefault_Light_Dialog);

                    oDialog.setMessage("지정한 이륙 고도까지 기체가 상승합니다.\n안전거리를 유지하세요.")
                            .setTitle("일반 Dialog")
                            .setPositiveButton("아니오", new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    Log.i("Dialog", "취소");
                                    Toast.makeText(getApplicationContext(), "취소", Toast.LENGTH_LONG).show();
                                    onError(123);
                                }
                            })
                            .setNeutralButton("예", new DialogInterface.OnClickListener()
                            {
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    finish();
                                }
                            })
                            .setCancelable(false) // 백버튼으로 팝업창이 닫히지 않도록 한다.
                            .show();
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
                public void onError(int executionError) { alertUser("Unable to arm vehicle.");}

                @Override
                public void onTimeout() {
                    alertUser("Arming operation timed out.");
                }
            });
        }
    }

    private void updateArmButton() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        Button armButton = (Button) findViewById(R.id.btnArmTakeOff);

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

    private void updateSatCnt(){
        TextView satelliteTextView = (TextView)findViewById(R.id.satelliteValueTextView);
        Gps droneSatCnt = this.drone.getAttribute(AttributeType.GPS);
        satelliteTextView.setText(String.format("%d", droneSatCnt.getSatellitesCount()) + "개");
        //위성의 갯수가 9개 이하일때 Arm 버튼 등 뜨지 않게 하고 만약 Take Off 상태일 때 떨어지면 자동으로 Land 할 수 있도록.
    }

    private void updateDronePosition(){
        Gps loadGps = this.drone.getAttribute(AttributeType.GPS);
        this.dronePosition = loadGps.getPosition();

        if(dronePosition != null){
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

            locationOverlay.setAnchor(new PointF(0.5f,1));


        }



//        Log.d("position test", dronePosition.getLatitude()+"");

    }


    private void updateYaw() {
        TextView yawTextView = (TextView) findViewById(R.id.yawValueTextView);
        Attitude droneYaw = this.drone.getAttribute(AttributeType.ATTITUDE);
        if (droneYaw.getYaw() < 0){
            yawTextView.setText(String.format("%3.1f", droneYaw.getYaw()*(-1)) + "deg");
        } else {
            yawTextView.setText(String.format("%3.1f", droneYaw.getYaw()) + "deg");
        }

    }

    private void updateBattery() {
        TextView batteryTextView = (TextView) findViewById(R.id.voltageValueTextView);
        Battery droneBattery = this.drone.getAttribute(AttributeType.BATTERY);
        batteryTextView.setText(String.format("%3.1f", droneBattery.getBatteryVoltage()) + "V");

    }
    protected void updateAltitude() {
        TextView altitudeTextView = (TextView) findViewById(R.id.altitudeValueTextView);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        if ( droneAltitude.getAltitude() < 0){
            altitudeTextView.setText(String.format("%3.1f", droneAltitude.getAltitude()*(-1)) + "m");
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
        if (soloState == null){
            alertUser("Unable to retrieve the solo state.");
        }
        else {
            alertUser("Solo state is up to date.");
        }
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }

    @Override
    public void onLinkStateUpdated(@NonNull LinkConnectionStatus connectionStatus) {
        switch(connectionStatus.getStatusCode()){
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

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.mMap = naverMap;
        mMap.setMapType(NaverMap.MapType.Satellite);

        UiSettings uiSettings = mMap.getUiSettings();
        uiSettings.setScaleBarEnabled(false);
        uiSettings.setZoomControlEnabled(false);



    }
}
