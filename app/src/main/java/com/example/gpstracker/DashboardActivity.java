package com.example.gpstracker;

import static java.lang.Math.abs;
import static java.lang.Math.acos;
import static java.lang.Math.ceil;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

import android.Manifest;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.UUID;
import de.nitri.gauge.Gauge;

public class DashboardActivity extends FragmentActivity implements AdapterView.OnItemClickListener, OnMapReadyCallback{

    private static final String TAG = "DashboardActivity: ";
    private static final int NUMBER_OF_DATA =14 ;
    // one min
    private static final long TIMER_INITIAL_VALUE =12000 ;
    private static boolean TIMERCALLED = true;
    private static boolean GLOSACALLED = false;
    public BluetoothConnectionService mBluetoothConnection;
    private RelativeLayout secMain;
    private RelativeLayout main;
    private TextView trafficlightstate;
    private TextView timer_tv;
    private CountDownTimer countDownTimer;
    private boolean timerIsRunning;
    private long timeLeftMS=TIMER_INITIAL_VALUE;
    BluetoothDevice mBTDevice;
    PolylineOptions options,amboptions;
    Marker changeMarker,ambchangeMarker,trafficMarker;
    Polyline currentPolyline,ambcurrentPolyline;
    TextView brakeValue;
    TextView throttleValue;
    MarkerOptions place1,place2,ambplace2,ambplace1;
    Gauge gauge;
    private TextView speedRecommendation;
    private LineChart lineChart;
    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    public DeviceListAdapter mDeviceListAdapter;
    ListView lvNewDevices;
    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothAdapter mBluetoothAdapter;
    boolean firstCoordinates=true;
    boolean firstAmbCoordinates=true;
    int newTrafficCoordinate=0;
    ImageView brake_icon;
    ImageView warning_icon;
    ImageView traffic_icon;
    ImageView exl_icon;
    ImageView siren_icon;
    ImageView cam_icon;
    private double time=1;
    private static final int ERROR = 9001;
    private boolean permissionGranted = false;
    private GoogleMap mMap;
    private int newCoordinate=0;
    private double newLat;
    private double newLon;
    private int newAmbCoordinate=0;
    private double newAmbLat;
    private double newAmbLon,newTrafficLat,newTrafficLon;
    private LineDataSet lineDataSet;
    private LineData lineData;
    private double VMAX=25*(1000/(60.0*60.0));
    private double dist=-1;
    private TextView send;
    private  Button btnONOFF;
    private  Button startConnect;
    private Button discover;


    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;

                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    /**
     * Broadcast Receiver for changes made to bluetooth states such as:
     * 1) Discoverability mode on/off or expire.
     */
    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {

                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    //Device is in Discoverable Mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Enabled.");
                        break;
                    //Device not in discoverable mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver2: Connecting....");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver2: Connected.");
                        break;
                }

            }
        }
    };

    private final BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);
                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                lvNewDevices.setAdapter(mDeviceListAdapter);
            }
        }
    };


    /**
     * Broadcast Receiver that detects bond state changes (Pairing status changes)
     */
    private BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    //inside BroadcastReceiver4
                    mBTDevice = mDevice;
                }
                //case2: creating a bone
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        init_ui();
        mBTDevices = new ArrayList<>();

        //permissions for bluetooth
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PackageManager.PERMISSION_GRANTED);
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PackageManager.PERMISSION_GRANTED);

        // speed analysis
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(false);
        ArrayList<Entry>yvalue=new ArrayList<>();
        yvalue.add(new Entry(0,0));
        lineDataSet=new LineDataSet(yvalue, "Vehicle Speed");
        lineDataSet.setFillAlpha(110);
        lineDataSet.setColor(Color.RED);
        lineDataSet.setLineWidth(3f);
        lineDataSet.setValueTextColor(Color.BLACK);
        lineData=new LineData();
        lineData.addDataSet(lineDataSet);
        lineChart.setData(lineData);

        // used for GLOSA Algorithm
        updateTimer(false);

        // reception of msg
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("IncomingMessage"));

        //Broadcasts when bond state changes (ie:pairing)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4, filter);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.i(TAG, "Bluetooth not supported");
            // Show proper message here
            finish();
        }
        getlocationpermission();


        lvNewDevices.setOnItemClickListener((AdapterView.OnItemClickListener) DashboardActivity.this);
        btnONOFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: enabling/disabling bluetooth.");
                enableDisableBT();

            }
        });

        discover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnEnableDisable_Discoverable();
                btnDiscover();
            }
        });


        startConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startConnection();

            }
        });


        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Toast.makeText(DashboardActivity.this, "Welcome", Toast.LENGTH_LONG).show();
                main.setVisibility(View.INVISIBLE);
                secMain.setVisibility(View.VISIBLE);

            }
        });

        // for google map
        options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
        place2 = new MarkerOptions();
        amboptions = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
        ambplace2 = new MarkerOptions();

    }

    void updateSpeedAnalysis(float time,float speed)
    {
        if(time<=2) speed=0;

        LineData data = lineChart.getData();
        data.addEntry(new Entry(time,speed),0);
        data.notifyDataChanged();
        lineChart.notifyDataSetChanged();
       // lineChart.setVisibleXRangeMaximum(10);
        lineChart.moveViewToX(data.getEntryCount());

    }


    private void changePosition(double lat, double lon, boolean first,int view) {

        if(lat<30 ||lon<31)
            return;
        if(lat>31 ||lon>32)
            return;

        if(view==0) {
            if (first) {
                Log.d(TAG, "--------------------------------------view 0");
                options.add(new LatLng(lat, lon));
                currentPolyline = mMap.addPolyline(options);
                place1 = new MarkerOptions().position(new LatLng(lat, lon)).title("start Location");
                place1.icon(BitmapDescriptorFactory.fromResource(R.drawable.carmap));
                Location targetLocation = new Location("");
                targetLocation.setLatitude(lat);
                targetLocation.setLongitude(lon);
                place1.rotation(targetLocation.getBearing());
               changeMarker= mMap.addMarker(place1);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 17f));

            } else {
                place2.position(new LatLng(lat, lon)).title(" New Location");
                place2.icon(BitmapDescriptorFactory.fromResource(R.drawable.carmap));
                Location targetLocation = new Location("");
                targetLocation.setLatitude(lat);
                targetLocation.setLongitude(lon);
                place2.rotation(targetLocation.getBearing());
                if (changeMarker != null) changeMarker.remove();
                changeMarker = mMap.addMarker(place2);

                options.add(new LatLng(lat, lon));
                currentPolyline = mMap.addPolyline(options);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 17f));

            }
        }
        else if(view ==1){
            if (first) {
                Log.d(TAG, "-----------------------------------------------------view 1");

                Log.d(TAG, "---------------------------------------------------------------------");
                amboptions.add(new LatLng(lat, lon));
                ambcurrentPolyline = mMap.addPolyline(amboptions);
                ambplace1 = new MarkerOptions().position(new LatLng(lat, lon)).title("ambulance start Location");
                ambplace1.icon(BitmapDescriptorFactory.fromResource(R.drawable.mapamb));
                Location targetLocation = new Location("");
                targetLocation.setLatitude(lat);
                targetLocation.setLongitude(lon);
                ambplace1.rotation(targetLocation.getBearing());
                ambchangeMarker=mMap.addMarker(ambplace1);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 17f));
            } else {

                ambplace2.position(new LatLng(lat, lon)).title(" ambulance New Location");
                ambplace2.icon(BitmapDescriptorFactory.fromResource(R.drawable.mapamb));
                Location targetLocation = new Location("");
                targetLocation.setLatitude(lat);
                targetLocation.setLongitude(lon);
                ambplace2.rotation(targetLocation.getBearing());
                if (ambchangeMarker != null) ambchangeMarker.remove();
                ambchangeMarker = mMap.addMarker(ambplace2);

                amboptions.add(new LatLng(lat, lon));
                ambcurrentPolyline = mMap.addPolyline(amboptions);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 17f));

            }


        }

        else if(view==2){
            MarkerOptions place=new MarkerOptions().position(new LatLng(lat,lon)).title("traffic light Locarion");

            switch (Data.current_traffic_state) {
                case "1":
                    place.icon(BitmapDescriptorFactory.fromResource(R.drawable.redtraffic));
                    break;
                case "2":
                    place.icon(BitmapDescriptorFactory.fromResource(R.drawable.yellowtraffic));
                    break;
                case "3":
                    place.icon(BitmapDescriptorFactory.fromResource(R.drawable.greentraffic));
                    break;
                default:
                    place.icon(BitmapDescriptorFactory.fromResource(R.drawable.redtraffic));
            }
            if (trafficMarker != null) trafficMarker.remove();
            trafficMarker=mMap.addMarker(place);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 17f));

        }
    }

    public void getlocationpermission() {
        String[] p = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                permissionGranted = true;
                initMap();

            } else {
                ActivityCompat.requestPermissions(this, p, 1234);
            }


        } else {
            ActivityCompat.requestPermissions(this, p, 1234);
        }

    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(DashboardActivity.this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionGranted = false;
        if (requestCode == 1234) {
            if (grantResults.length > 0) {

                for (int grantResult : grantResults) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
                permissionGranted = true;
                //init map
                initMap();
            }
        }
    }

    boolean isServiceOk() {
        Log.d("DashboardActivity: ", "checking google");
        int av = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(DashboardActivity.this);
        if (av == ConnectionResult.SUCCESS) {
            Log.d("DashboardActivity: ", " google map working");
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(av)) {
            Log.d("DashboardActivity: ", " resolved error");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(DashboardActivity.this, av, ERROR);
            assert dialog != null;
            dialog.show();

        } else
            Toast.makeText(DashboardActivity.this, "Google Services not Supported", Toast.LENGTH_LONG).show();
        return false;

    }

    void init_ui() {

        brake_icon = findViewById(R.id.dash_brake_id);
        warning_icon = findViewById(R.id.warning_id);
        traffic_icon = findViewById(R.id.dash_traffic_id);
        exl_icon = findViewById(R.id.dash_exl_id);
        siren_icon = findViewById(R.id.dash_siren_id);
        cam_icon = findViewById(R.id.dash_new_cam_id);

        gauge = findViewById(R.id.gauge);
        timer_tv=findViewById(R.id.Current_Traffic_timer_id);
        throttleValue=findViewById(R.id.throttle_value);
        brakeValue=findViewById(R.id.brake_value);
        speedRecommendation=findViewById(R.id.speed_Recommendation_id);
        trafficlightstate=findViewById(R.id.Current_Traffic_Light_State_id);
        main = findViewById(R.id.bt_activity_id);
        send = findViewById(R.id.send);
        btnONOFF = (Button) findViewById(R.id.btnONOFF);
        startConnect = (Button) findViewById(R.id.startConnect);
        discover = (Button) findViewById(R.id.discover);
        lineChart=(LineChart)findViewById(R.id.lineChartId);
        lvNewDevices = (ListView) findViewById(R.id.lvNewDevices);
        secMain = findViewById(R.id.dashboard_activity_id);

    }


    BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String txt = intent.getStringExtra("msg");
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(txt);
            Log.d(TAG, "GET " + txt);
            if(!txt.isEmpty()) {

                int i=0;
                int startIndex=0;
                int endIndex=0;
                while(i<NUMBER_OF_DATA && txt.contains("/") )
                {
                    if(i==0)
                        startIndex=txt.indexOf("/");

                    endIndex=txt.indexOf(" ", startIndex+1);
                    if(startIndex+1<endIndex )
                    {String value=txt.substring(startIndex+1,endIndex);

                        if(!value.contains("/")&&!value.contains("("))
                        {
                            try{
                            Double.parseDouble(value);

                            }
                            catch (Exception e){
                                Log.d(TAG,"Error, Number");
                            }
                            showValue(i,value);

                        }
                    }
                    startIndex=endIndex;
                    i++;
                }

            }
        }
    };

    void startStopTimer(){
        if(timerIsRunning){

            countDownTimer.cancel();
            timerIsRunning=false;
        }
        else{

            countDownTimer=new CountDownTimer(timeLeftMS,1000) {
                @Override
                public void onTick(long millisUntilFinished) {

                    timeLeftMS=millisUntilFinished;
                    updateTimer(false);

                }

                @Override
                public void onFinish() {

                }
            }.start();
            timerIsRunning=true;

        }
    }


    void glosaAlgorithm(){
        Log.d("glosa DISTANCE",dist+"");

        if(dist>0)
        {
            double speed_mps=Double.parseDouble(Data.speed)*(1000/(60.0*60.0));
            Log.d("glosa DISTANCE",dist+"");
            double arrivalTime=(dist/speed_mps)*1000;
            Log.d("glosa speed_mps",speed_mps+"");

            double deceleration=(-0.005*(speed_mps*speed_mps))+(0.154*speed_mps)+0.493;

            double time_adr=(((dist-(((speed_mps*speed_mps)-(0.36*VMAX*VMAX))/2.0*deceleration))/(0.6*VMAX))+(((speed_mps)-(0.6*VMAX))/deceleration))*1000;

            Log.d("glosa arrival time",arrivalTime+"");

            Log.d("glosa time left ms",timeLeftMS+"");

            if(arrivalTime>timeLeftMS)
            {
                // keep current speed
                String msg="Speed Recommendation: "+Double.parseDouble(Data.speed)+"km/h";
                speedRecommendation.setTextColor(Color.GREEN);
                speedRecommendation.setText(msg);

            }
            else if(arrivalTime<timeLeftMS && timeLeftMS<time_adr){

                //speed advice= dist/time_adr
                double speed_advice=(dist/(time_adr/1000.0))/(1000/(60.0*60.0));
                String msg="Speed Recommendation: "+speed_advice+"km/h";
                speedRecommendation.setTextColor(Color.WHITE);
                speedRecommendation.setText(msg);


            }
            else if(timeLeftMS>=time_adr)
            {

                // stop
                String msg="You Need to Stop";
                speedRecommendation.setTextColor(Color.RED);
                speedRecommendation.setText(msg);
            }


        }

    }

    void updateTimer(boolean reset){
        if(reset)
            timeLeftMS=TIMER_INITIAL_VALUE;
        int min=(int)timeLeftMS/60000;
        int sec=(int)timeLeftMS%60000/1000;
        String timeLeftText;
        timeLeftText=""+min;
        timeLeftText+=":";
        if(sec<10)
            timeLeftText+="0";
        timeLeftText+=sec;
        timer_tv.setText(timeLeftText);
    }

    public void showValue(int key, String value){
        switch (key) {
            case 0:
                Data.brake = value;
                brakeValue.setText(value);
                if (Double.parseDouble(value)==0)
                    brake_icon.setColorFilter(getResources().getColor(R.color.green));
                else if (Double.parseDouble(value)>0)
                    brake_icon.setColorFilter(getResources().getColor(R.color.red));

                newCoordinate=0;
                break;
            case 1:
                Data.exlight = value;
                if (Double.parseDouble(value)==0)
                    exl_icon.setColorFilter(getResources().getColor(R.color.green));
                else if (Double.parseDouble(value)>0)
                    exl_icon.setColorFilter(getResources().getColor(R.color.red));
                newCoordinate=0;
                break;
            case 2:
                Data.sirin = value;
                if (Double.parseDouble(value)==0)
                    siren_icon.setColorFilter(getResources().getColor(R.color.green));
                else if (Double.parseDouble(value)>0)
                    siren_icon.setColorFilter(getResources().getColor(R.color.red));
                newCoordinate=0;
                break;
            case 3:
                Data.warning = value;
                if (Double.parseDouble(value)==0)
                    warning_icon.setColorFilter(getResources().getColor(R.color.green));
                else if (Double.parseDouble(value)>0)
                    warning_icon.setColorFilter(getResources().getColor(R.color.red));
                newCoordinate=0;
                break;
            case 4:
                Data.speed = value;
                double v = Double.parseDouble(value);
                if (v <= 110 && v >= 0)
                {
                    gauge.moveToValue((float) v);

                    /*if(velocity!=v )
                    {
                        velocity=v;*/
                        updateSpeedAnalysis((float)time,(float) v);
                        time=time+1;
                   // }

                }
                newCoordinate=0;
                break;
            case 8:
                trafficlightstate.setTextColor(Color.RED);
                trafficlightstate.setText("Traffic light is RED");

                if(Double.parseDouble(value)==0){
                    traffic_icon.setColorFilter(getResources().getColor(R.color.white));

                }
                else if(Double.parseDouble(value)==1){
                    traffic_icon.setColorFilter(getResources().getColor(R.color.red));
                    Data.current_traffic_state = "1";
                     if(!GLOSACALLED)
                     {
                         if(timerIsRunning)
                             // stop timer
                             startStopTimer();
                         // reset timer
                         updateTimer(true);
                         // start timer
                         startStopTimer();
                         glosaAlgorithm();
                         GLOSACALLED=true;
                     }

                }

                else if(Double.parseDouble(value)==2){
                    Data.current_traffic_state = "3";
                    trafficlightstate.setTextColor(Color.GREEN);
                    trafficlightstate.setText("Traffic light is GREEN");
                    traffic_icon.setColorFilter(getResources().getColor(R.color.green));
                    speedRecommendation.setTextColor(Color.WHITE);

                    String msg="Speed Recommendation: "+25+"km/h";
                    speedRecommendation.setText(msg);
                    if(TIMERCALLED)
                    {
                    if(timerIsRunning)
                        // stop timer
                        startStopTimer();
                    // reset timer
                    updateTimer(true);
                    // start timer
                    startStopTimer();

                    TIMERCALLED=false;
                    }
                }

                newCoordinate=0;
                break;

            case 5:
                newCoordinate = 1;
                newLat=Double.parseDouble(value);
                break;
            case 6:
                newLon=Double.parseDouble(value);
                if(newCoordinate==1){


                    changePosition(newLat,newLon,firstCoordinates,0);
                    if(firstCoordinates)
                        firstCoordinates=false;
                    newCoordinate=0;
                }
                break;

            case 7:
                throttleValue.setText(value);
                break;
            case 9:

                // ambulance lat
                newAmbCoordinate=1;
                newAmbLat=Double.parseDouble(value);

                break;
            case 10:
                // ambulance lon
                newAmbLon=Double.parseDouble(value);
                if(newAmbCoordinate==1){


                    changePosition(newAmbLat,newAmbLon,firstAmbCoordinates,1);
                    if(firstAmbCoordinates)
                        firstAmbCoordinates=false;
                    newAmbCoordinate=0;
                }
                break;
            case 11:
                // traffic light lat
                newTrafficCoordinate=1;
                newTrafficLat=Double.parseDouble(value);
                break;
            case 12:
                // traffic ligh lon
                newTrafficLon=Double.parseDouble(value);
                if(newTrafficCoordinate==1){


                    changePosition(newTrafficLat,newTrafficLon,false,2);
                    calculateDistance(newTrafficLat,newTrafficLon,newLat,newLon);

                    newTrafficCoordinate=0;
                }
                break;
            case 13:
           /*   *//*
              * traffic jam is decreasing
             * *//*
                if(Double.parseDouble(value)== 18 && newAccidentFlag>10 && f1==1)
                {
                    f1=0;
                    dialog.setContentView(R.layout.trafficjamend_dialog);

                    dialog.show();
                    Handler handler=new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();

                        }
                    },2000);
                }
                else if(Double.parseDouble(value)== 20 && newAccidentFlag>20 && f2==1){

                    f2=0;
                    *//*
                     *  accident just happened
                     * *//*
                    dialog.setContentView(R.layout.accident_dialog);

                    dialog.show();
                    Handler handler=new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();

                        }
                    },2000);

                }
                else if(Double.parseDouble(value)== 30 && newAccidentFlag>30&&f3==1){

                    f3=0;
                    *//* Roadworks  *//*
                    dialog.setContentView(R.layout.roadwork_dialog);


                    dialog.show();
                    Handler handler=new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();

                        }
                    },2000);
                    newAccidentFlag=35;
                }
                else if(Double.parseDouble(value)== 94 && newAccidentFlag>45 && f4==1){
                    *//*Vehicle breakdown*//*

                    f4=0;
                    dialog.setContentView(R.layout.brokendown_dialog);

                    dialog.show();
                    Handler handler=new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();

                        }
                    },2000);
                }
                newAccidentFlag+=1;*/

                break;



        }
    }

    private void calculateDistance(double newTrafficLat, double newTrafficLon, double newLat, double newLon) {
    /*    double theta = newLon - newTrafficLon;
         dist = Math.sin(deg2rad(newLat))
                * Math.sin(deg2rad(newTrafficLat))
                + Math.cos(deg2rad(newLat))
                * Math.cos(deg2rad(newTrafficLat))
                * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        dist = ceil(1609.344*dist);*/

        dist=8;

        /* Changing type of arguments to be able to convert to degrees. */
/*        if(lat1 != 0 && lon1 != 0)
        {

            double theta = lon1 - lon2;
             dist = sin(deg2rad(lat1))
                    * sin(deg2rad(lat2))
                    + cos(deg2rad(lat1))
                    * cos(deg2rad(lat2))
                    * cos(deg2rad(theta));
            dist = acos(dist);
            dist = rad2deg(dist);
            dist = dist * 60 * 1.1515;
            dist = ceil(1609.344*dist);
        }*/
    }
    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    public void startConnection() {
        startBTConnection(mBTDevice, MY_UUID_INSECURE);
    }

    /**
     * starting chat service method
     */
    public void startBTConnection(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection.");

        if (mBluetoothConnection != null)
        {
            mBluetoothConnection.startClient(device, uuid);
            Toast.makeText(DashboardActivity.this, "Welcome", Toast.LENGTH_LONG).show();
            main.setVisibility(View.INVISIBLE);
            secMain.setVisibility(View.VISIBLE);


        }

    }


    public void enableDisableBT() {
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "enableDisableBT: Does not have BT capabilities.");
        } else if (!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enableDisableBT: enabling BT.");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        } else if (mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enableDisableBT: disabling BT.");
            mBluetoothAdapter.disable();

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }

    }


    public void btnEnableDisable_Discoverable() {

        Log.d(TAG, "btnEnableDisable_Discoverable: Making device discoverable for 300 seconds.");

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);

        IntentFilter intentFilter = new IntentFilter(mBluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(mBroadcastReceiver2, intentFilter);

    }

    public void btnDiscover() {

        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");

        if (mBluetoothAdapter != null && mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "btnDiscover: Canceling discovery.");

            //check BT permissions in manifest
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        } else if (mBluetoothAdapter != null && !mBluetoothAdapter.isDiscovering()) {

            //check BT permissions in manifest
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }
    }

    /**
     * This method is required for all devices running API23+
     * Android must programmatically check the permissions for bluetooth. Putting the proper permissions
     * in the manifest is not enough.
     *
     * NOTE: This will only execute on versions > LOLLIPOP because it is not needed otherwise.
     */
    private void checkBTPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int permissionCheck = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            }
            if (permissionCheck != 0) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
                }
            }
        } else {
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        //first cancel discovery because its very memory intensive.
        mBluetoothAdapter.cancelDiscovery();

        Log.d(TAG, "onItemClick: You Clicked on a device.");
        String deviceName = mBTDevices.get(i).getName();
        String deviceAddress = mBTDevices.get(i).getAddress();

        Log.d(TAG, "onItemClick: deviceName = " + deviceName);
        Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);

        //create the bond.
        //NOTE: Requires API 17+? I think this is JellyBean
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Log.d(TAG, "Trying to pair with " + deviceName);
            mBTDevices.get(i).createBond();

            mBTDevice = mBTDevices.get(i);
            mBluetoothConnection = new BluetoothConnectionService(DashboardActivity.this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver4);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setBuildingsEnabled(false);
        /*if(permissionGranted){

        }*/
    }


}