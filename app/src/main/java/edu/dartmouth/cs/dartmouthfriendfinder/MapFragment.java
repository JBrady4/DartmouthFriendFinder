package edu.dartmouth.cs.dartmouthfriendfinder;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.internal.zzccy;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.quickblox.auth.Consts;
import com.quickblox.auth.session.QBSettings;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.QBRestChatService;
import com.quickblox.chat.QBSystemMessagesManager;
import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.chat.model.QBChatMessage;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.core.model.QBBaseCustomObject;
import com.quickblox.core.request.QBRequestGetBuilder;
import com.quickblox.customobjects.QBCustomObjects;
import com.quickblox.customobjects.model.QBCustomObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;

import edu.dartmouth.cs.dartmouthfriendfinder.Common.Common;

/**
 * Created by johnnybrady
 */

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "MapFragment";
    private GoogleMap mGoogleMap;
    private MapView mMapView;
    View mView;

    ListView chatDialogs;


   //variables for storing current user info
    private String user, workLocation;
    private int id;
    private ArrayList<QBChatDialog> chatDialogArrayList;


    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mFusedLocationProviderClient;



    private CameraPosition mCameraPosition;

    private final LatLng mDefaultLocation = new LatLng(40.7128, -74.0060);
    private static final int DEFAULT_ZOOM = 15;

    //handlers for permission
    private boolean mLocationPermissionGranted;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    // The geographical location where the device is currently located
    // (i.e. the last-known location retrieved by the Fused Location Provider)
    private Location mLastKnownLocation;


    // Keys for restoring fragment state
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";



    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.map, container, false);

        //getting current user info
        user = getArguments().getString("user");
        id = getArguments().getInt("id");
        workLocation = getArguments().getString("workLocation");



        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);

            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());


        return mView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mMapView = (MapView) mView.findViewById(R.id.map);
        if (mMapView != null) {
            mMapView.onCreate(null);
            mMapView.onResume();
            mMapView.getMapAsync(this);
        }

    }

    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mGoogleMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mGoogleMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }



    }

    @Override
    /**
     * Creating the Google Map
     */
    public void onMapReady(GoogleMap googleMap) {
        MapsInitializer.initialize(getContext());

        mGoogleMap = googleMap;
        mGoogleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                View v = getLayoutInflater().inflate(R.layout.custom_infowindow, null);
                TextView user = (TextView) v.findViewById(R.id.marker_user);
                TextView bio = (TextView) v.findViewById(R.id.marker_bio);


                user.setText(marker.getTitle());
                bio.setText(marker.getSnippet());


                return v;
            }


        });

        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        //if the user clicks a marker and then the info window,
        mGoogleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(final Marker marker) {

                //creating server request for getting all the ChatDialogs that the current user is in
                QBRequestGetBuilder requestGetBuilder = new QBRequestGetBuilder();


                QBRestChatService.getChatDialogs(null, requestGetBuilder).performAsync(new QBEntityCallback<ArrayList<QBChatDialog>>() {
                    @Override
                    public void onSuccess(ArrayList<QBChatDialog> qbChatDialogs, Bundle bundle) {

                        boolean userInChat = false;
                        boolean targetInChat = false;

                        //iterate through all server chat dialogs the current user is in
                        for (int i = 0; i < qbChatDialogs.size(); i++){
                            if(qbChatDialogs.get(i).getOccupants().size() <= 2) {
                                for (int j = 0; j < qbChatDialogs.get(i).getOccupants().size(); j++) {

//                                    Log.d("COMPARING", String.valueOf(qbChatDialogs.get(i).getOccupants().size()));
//                                    Log.d("COMPARING", qbChatDialogs.get(i).getOccupants().get(j).toString());

                                    if (qbChatDialogs.get(i).getOccupants().get(j).toString().equals(String.valueOf(id))) {
                                        userInChat = true;
                                    }
                                    if (qbChatDialogs.get(i).getOccupants().get(j).toString().equals(marker.getTag().toString())) {
                                        targetInChat = true;
                                    }

                                    //triggers if a chat dialog already exists between the current user and the clicked user
                                    if (userInChat == true && targetInChat == true) {
//                                        Log.d("COMPARING", "FOUND MATCH");
                                        Toast.makeText(getActivity().getApplicationContext(), "You have already created a chat with this user. See your messages!", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

//                                    Log.d("COMPARING", " user:" + userInChat + " target:" + targetInChat);
                                }
                            }
                        }

                        //otherwise, start the ShowUsersActivity to show only the targetted user
                        String userName = marker.getTitle();
                        Intent intent = new Intent (getActivity().getApplicationContext(), ShowUsersActivity.class);
                        intent.putExtra("userName", userName);
                        intent.putExtra("mMarkerClicked", true);
                        startActivity(intent);


                    }

                    @Override
                    public void onError(QBResponseException e) {

                    }
                });

            }
        });


        // Prompt the user for permissions
        getLocationPermission();

        // Turn on the My Location layer and the related control on the map
        updateLocation();


        // Get the current location of the device and set the position of the map
        getDeviceLocation();




    }


    /**
     * Prompts the user for permission to use the device location.
     */
    private void getLocationPermission() {

        if (ContextCompat.checkSelfPermission(getActivity().getBaseContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    private void onRequestPermissionResult(int requestCode, String permissions[], int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }

    }


    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private void getDeviceLocation() {
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(getActivity(), new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();

                            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));


                        } else {
//                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mGoogleMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }


    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocation() {
        if (mGoogleMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mGoogleMap.setMyLocationEnabled(true);
                mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);

                //retrieve all the users and update the map with their current and work locations
                getOtherQBUsersInfo();

                //update the current user's location in the associated UserInfo object
                updateQBUserInfo();
            } else {
                mGoogleMap.setMyLocationEnabled(false);
                mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }

    }

   /*
        Creates a new UserInfo object on the Quickblox server
    */
    private void createNewQBUserInfo(){

        //instantiate the new object
        QBCustomObject userInfo = new QBCustomObject();
        userInfo.setClassName("UserInfo");

        String lat = String.valueOf(mLastKnownLocation.getLatitude());
        String lng = String.valueOf(mLastKnownLocation.getLongitude());

        String loc = lng+","+lat;

        HashMap<String, Object> fields = new HashMap<>();
        fields.put("userCurrentLocation", loc);
        fields.put("userName", user);
        fields.put("userID", id);
        fields.put("userWorkLocation", workLocation);

        userInfo.setFields(fields);

        //create the object on the server
        QBCustomObjects.createObject(userInfo).performAsync(new QBEntityCallback<QBCustomObject>() {
            @Override
            public void onSuccess(QBCustomObject qbCustomObject, Bundle bundle) {
//                Toast.makeText(getActivity().getApplicationContext(), "Created New User and added Location", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(QBResponseException e) {
                Log.e("onSaveInstance", e.getMessage() );
            }
        });

    }

    /*
        Retrieves the user by their username.
        creates a new user location for the user if there isn't one existing in the database
    */
    private void updateQBUserInfo(){

        //server request for retrieving all objects where userName is the current user's userName
        //(should return only one UserInfo object)
        QBRequestGetBuilder requestBuilder = new QBRequestGetBuilder();
        requestBuilder.eq("userName", user);


        QBCustomObjects.getObjects("UserInfo", requestBuilder).performAsync(new QBEntityCallback<ArrayList<QBCustomObject>>() {
            @Override
            public void onSuccess(ArrayList<QBCustomObject> qbCustomObjects, Bundle bundle) {

                String lat = String.valueOf(mLastKnownLocation.getLatitude());
                String lng = String.valueOf(mLastKnownLocation.getLongitude());

                //String for updating the userLocation on the server
                String loc = lng+","+lat;

                //if there is not a UserInfo object associated with the current user,
                //create a new object on the server
                if(qbCustomObjects.size() == 0){
                    createNewQBUserInfo();
                }
                else {
                    for(QBCustomObject customObject : qbCustomObjects){

                        //modify the current users's location with loc
                        HashMap<String, Object> fields = new HashMap<>();
                        fields.put("userCurrentLocation", loc);

                        customObject.setFields(fields);

                        //make the changes on the server
                        QBCustomObjects.updateObject(customObject, null).performAsync(new QBEntityCallback<QBCustomObject>() {
                            @Override
                            public void onSuccess(QBCustomObject qbCustomObject, Bundle bundle) {
//                                Toast.makeText(getActivity().getApplicationContext(), "updated old user", Toast.LENGTH_LONG).show();

                            }

                            @Override
                            public void onError(QBResponseException e) {
                                Log.e("ERROR", e.getMessage() );
                            }


                        });
                    }
                }

            }

            @Override
            public void onError(QBResponseException e) {
                Log.e("ERROR", e.getMessage());

            }
        });
    }

    private void getOtherQBUsersInfo(){

        //server request
        QBRequestGetBuilder requestBuilder = new QBRequestGetBuilder();

        //retrieve all the UserInfo objects
        QBCustomObjects.getObjects("UserInfo", requestBuilder).performAsync(new QBEntityCallback<ArrayList<QBCustomObject>>() {
            @Override
            public void onSuccess(ArrayList<QBCustomObject> qbCustomObjects, Bundle bundle) {

                HashMap<String, Object> fields = new HashMap<>();
                Geocoder geocoder = new Geocoder(getContext());

                //for each UserInfo object retrieved
                for (QBCustomObject qbCustomObject : qbCustomObjects) {
                    fields = qbCustomObject.getFields();
                    ArrayList loc = (ArrayList) fields.get("userCurrentLocation");
                    String uName = (String) fields.get("userName");
                    Object id = fields.get("userID");
                    Object wloc = fields.get("userWorkLocation");

                    String bio;
                    if(fields.get("userBio").getClass().equals(String.class)){
                        bio = (String) fields.get("userBio");
                    }
                    else{
                        bio = "No bio available.";
                    }


                    Log.d("GETTING", wloc.getClass().toString());



                    double workLat = 0;
                    double workLng = 0;

                    //creating a Latitude and Longitude from the address of the userWorkLocation
                    if( wloc.getClass().equals(String.class)){
                        String addressString = (String) wloc;
                        try {
                            List<Address> address = geocoder.getFromLocationName(addressString, 1);
                            address = geocoder.getFromLocationName(addressString, 1);
                            workLat = address.get(0).getLatitude();
                            workLng = address.get(0).getLongitude();
                        } catch (IOException e) {
                            Log.e("ERROR", e.getMessage());
                        }
                    }


                    //LatLng for user location
                    LatLng ltlgLocation = new LatLng(Double.parseDouble((String) loc.get(1)),
                            Double.parseDouble((String) loc.get(0)));

                    //LatLng for user work location
                    LatLng wltlgLocation = new LatLng(workLat, workLng);

                    if(!user.equals(uName)) {

                        //create a red marker based on the user's location
                        mGoogleMap.addMarker(new MarkerOptions().position(ltlgLocation).title(uName).snippet(bio)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))).setTag(id);
                        if(workLat != 0 && workLng != 0) {
                            //if the user has a work location, create a green marker at the work location
                            mGoogleMap.addMarker(new MarkerOptions().position(wltlgLocation).title(uName).snippet(bio+" \n\nThis is my work location")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))).setTag(id);
                        }
                    }
                }



            }

            @Override
            public void onError(QBResponseException e) {
                Log.e("ERROR", e.getMessage());


            }
        });

    }


}
