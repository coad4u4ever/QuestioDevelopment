package com.questio.projects.questiodevelopment.sections;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.questio.projects.questiodevelopment.DatabaseHelper;
import com.questio.projects.questiodevelopment.MainActivity;
import com.questio.projects.questiodevelopment.questaction.QuestBrowsing;
import com.questio.projects.questiodevelopment.R;
import com.questio.projects.questiodevelopment.adapters.PlaceListAdapter;
import com.questio.projects.questiodevelopment.models.PlaceObject;
import com.questio.projects.questiodevelopment.questaction.QuestZoning;

import org.w3c.dom.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import AndroidGoogleDirectionAndPlaceLibrary.GoogleDirection;


public class SectionQuestmap extends Fragment implements LocationListener, GoogleMap.OnCameraChangeListener {
    public static final String LOG_TAG = SectionQuestmap.class.getSimpleName();
    Context mContext;
    DatabaseHelper databaseHelper;
    Boolean isGPSEnabled;
    Boolean isNetworkEnabled;
    Boolean canGetLocation;
    final long MIN_TIME_BW_UPDATES = 10000;
    final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 0;
    GoogleMap googleMap;
    LocationManager locationManager;
    Location location;
    Geocoder myLocation;
    ProgressDialog prgDialog;
    Cursor cursor;
    Marker mMarker;
    MapView mMapView;
    View sectionView;
    TextView tv_place_detail;
    TextView tv_place_lat;
    TextView tv_place_lng;
    String currentPlace = "";
    double currentLat = 0;
    double currentLng = 0;
    ListView mListView;
    ArrayList<PlaceObject> placeListForDistance;
    PlaceListAdapter mPlaceListAdapter;
    PlaceObject po;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        po = new PlaceObject(mContext);
        databaseHelper = new DatabaseHelper(mContext);
        placeListForDistance = po.getAllPlaceArrayList();
        location = getLocation();
        setHasOptionsMenu(true);
        prgDialog = new ProgressDialog(mContext);
        prgDialog.setMessage("Sync place data, please wait...");
        prgDialog.setCancelable(false);
        myLocation = new Geocoder(mContext, Locale.getDefault());
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        sectionView = inflater.inflate(R.layout.section_questmap, container, false);
        tv_place_detail = (TextView) sectionView.findViewById(R.id.tv_place_detail);
        tv_place_lat = (TextView) sectionView.findViewById(R.id.tv_place_lat);
        tv_place_lng = (TextView) sectionView.findViewById(R.id.tv_place_lng);
        mMapView = (MapView) sectionView.findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume();


        try {
            MapsInitializer.initialize(mContext.getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
        googleMap = mMapView.getMap();
        LatLng coordinate = new LatLng(currentLat, currentLng);
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(coordinate, 16));
        googleMap.setMyLocationEnabled(true);
        googleMap.setOnCameraChangeListener(this);

        cursor = po.getAllPlacesCursor();

        mPlaceListAdapter = new PlaceListAdapter(mContext, cursor, 0);
        mListView = (ListView) sectionView.findViewById(R.id.listview_place);
        mListView.setAdapter(mPlaceListAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView tvLat = (TextView) view.findViewById(R.id.placeLat);
                TextView tvLng = (TextView) view.findViewById(R.id.placeLng);
                LatLng fromPosition = new LatLng(currentLat, currentLng);
                LatLng toPosition = new LatLng(Double.parseDouble(tvLat.getText().toString()), Double.parseDouble(tvLng.getText().toString()));
                googleMap.clear();
                googleMap.addMarker(new MarkerOptions().position(toPosition).title("Destination"));

                GoogleDirection gd = new GoogleDirection(mContext);
                gd.request(fromPosition, toPosition, GoogleDirection.MODE_DRIVING);
                gd.setOnDirectionResponseListener(new GoogleDirection.OnDirectionResponseListener() {
                    public void onResponse(String status, Document doc, GoogleDirection gd) {
                        googleMap.addPolyline(gd.getPolyline(doc, 3, Color.BLUE));
                    }
                });
            }
        });

        return sectionView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_questmap, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sit_location:
                currentLat = 13.652623;
                currentLng = 100.493673;
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLat, currentLng), 16.0f));
                if (!placeListForDistance.isEmpty()) {
                    for (PlaceObject po : placeListForDistance) {
                        isEnterQuestMap(currentLat, currentLng, po);
                    }
                }
                return true;

            case R.id.action_lib_location:
                currentLat = 13.653077;
                currentLng = 100.493956;
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLat, currentLng), 16.0f));
                if (!placeListForDistance.isEmpty()) {
                    for (PlaceObject po : placeListForDistance) {
                        isEnterQuestMap(currentLat, currentLng, po);
                    }
                }
                return true;

            case R.id.action_sync_data:
                po.updatePlaceSQLite();
                return true;
            case R.id.action_delect_all_data:
                po.delectAllPlace();
                return true;
            case R.id.action_qrcode_scan:
                ((MainActivity) mContext).launchQRScanner(sectionView);
                return true;
            case R.id.action_enter_zone0:
                Intent intent = new Intent(mContext, QuestZoning.class);
//                intent.putExtra("p", p);
                startActivity(intent);
                return true;
            default:
                break;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
//        float fixZoomMin = 16.0f;
//        if (cameraPosition.zoom  < fixZoomMin) {
//            googleMap.animateCamera(CameraUpdateFactory.zoomTo(fixZoomMin));
//        }
    }

    // This method call everytime when player's location change.
    @Override
    public void onLocationChanged(Location location) {
        Log.d(LOG_TAG, "onLocationChanged: LocationChanged called!");
        List<Address> myList;
        currentLat = location.getLatitude();
        currentLng = location.getLongitude();
        if (!placeListForDistance.isEmpty()) {
            for (PlaceObject po : placeListForDistance) {
                isEnterQuestMap(currentLat, currentLng, po);
            }
        }

        try {
            myList = myLocation.getFromLocation(currentLat, currentLng, 1);
            if (myList != null) {
                Address address = myList.get(0);
                currentPlace = address.getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mMarker != null) {
            mMarker.remove();
        }
        LatLng coordinate = new LatLng(currentLat, currentLng);
        mMarker = googleMap.addMarker(new MarkerOptions().position(coordinate).title("คุณอยู่นี่").snippet("ชื่อตัวละคร").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        tv_place_detail.setText("" + currentPlace);
        tv_place_lat.setText("" + currentLat);
        tv_place_lng.setText("" + currentLng);
        Log.d(LOG_TAG, coordinate + "");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


    public void isEnterQuestMap(double currentLat, double currentLng, final PlaceObject p) {

        float[] results = new float[1];
        Location.distanceBetween(currentLat, currentLng,
                p.getPlaceLatitude(), p.getPlaceLongitude(), results);
        if (results[0] <= p.getPlaceRadius()) {

            new AlertDialog.Builder(mContext)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setTitle("เข้าสู่ " + p.getPlaceName() + "!")
                    .setMessage("ยืนยันการเข้าสู่สถานที่แห่งนี้หรือไม่ครับ")
                    .setPositiveButton("ยืนยัน!", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(mContext, QuestBrowsing.class);
                              intent.putExtra("p", p);
                            startActivity(intent);
                        }

                    })
                    .setNegativeButton("ไม่", null)
                    .show();
        }
    }
    public Location getLocation() {
        try {
            locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
            // getting GPS status
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            // getting network status
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
            } else {
                this.canGetLocation = true;
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    Log.d(LOG_TAG, "getLocation(): Network Enabled");
                    if (locationManager != null) {
                        location = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            currentLat = location.getLatitude();
                            currentLng = location.getLongitude();
                        }
                    }
                }
                // if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {
                    if (location == null) {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                        Log.d(LOG_TAG, "getLocation(): GPS Enabled");
                        if (locationManager != null) {
                            location = locationManager
                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                currentLat = location.getLatitude();
                                currentLng = location.getLongitude();
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return location;
    }


}





