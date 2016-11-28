package com.group3.smartshop;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.vision.text.Text;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/*
 * Helper class to store useful info about a certain marker. Should be put into a hashMap by
 * put(marker, MarkerInfo)
 */
class MarkerInfo {
    public String imgUrl = "";
    public String webUrl = "";
    public int reviewCount = -1;
    public double rating = -1;
    MarkerInfo(String image, String web, int reviewNums, double rating) {
        this.imgUrl = image;
        this.webUrl = web;
        this.reviewCount = reviewNums;
        this.rating = rating;
    }
}
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {



    /*
     * Adapter for InfoWindow
     */
    class SmartInfoWindowAdapter implements GoogleMap.InfoWindowAdapter{
        private final View smartContentsView;
        SmartInfoWindowAdapter(){
            smartContentsView = getLayoutInflater().inflate(R.layout.googlemaps_infowindow, null);
        }

        @Override
        public View getInfoContents(Marker marker){
            TextView windowTitle = (TextView) smartContentsView.findViewById(R.id.infowindowtitle);
            windowTitle.setText(marker.getTitle());
            TextView windowDescription =
                    (TextView) smartContentsView.findViewById(R.id.infowindowdescription);
            windowDescription.setText(marker.getSnippet());
            ImageView windowImage = (ImageView) smartContentsView.findViewById(R.id.infowindowimg);

            String imgUrl = markerImages.get(marker).imgUrl;
            if (imgUrl != null && imgUrl != "") {
                Picasso.with(getApplicationContext())
                        .load(imgUrl)
                        .into(windowImage, new InfoWindowRefresher(marker));
            }

            TextView windowRating = (TextView)
                    smartContentsView.findViewById(R.id.infowindow_rating);
            double ratingDouble = markerImages.get(marker).rating;
            String rating = "Rating Score: " + ratingDouble;


            windowRating.setText(rating);

            TextView windowRatingCount =
                    (TextView)smartContentsView.findViewById(R.id.infowindow_numofratings);

            String numOfRating = "Total number of reviews: " +
                    Integer.toString(markerImages.get(marker).reviewCount);
            windowRatingCount.setText(numOfRating);


            return smartContentsView;
        }

        @Override
        public View getInfoWindow(Marker marker){
            return null;
        }
    }

    /*
     * class that handles infoWindow callback
     *
     */
    private class InfoWindowRefresher implements com.squareup.picasso.Callback {
        private Marker markerToRefresh = null;

        public InfoWindowRefresher(Marker marker){
            this.markerToRefresh = marker;
        }

        @Override
        public void onSuccess() {
            if (markerToRefresh != null && markerToRefresh.isInfoWindowShown()) {
                markerToRefresh.hideInfoWindow();
                markerToRefresh.showInfoWindow();
            }
        }

        @Override
        public void onError(){
            Log.e(getClass().getSimpleName(), "Error loading thumbnail!");
        }

    }

    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 1;
    public static final String URL_TO_VIEW = "group3.CSE110smartshop.URL_TO_VIEW";
    public static final String BASE_URL = "https://api.yelp.com/v3/";
    public static final String YELP_TOKEN =
            "Bearer z-wkW_0ij8grCGOBsXW2jivBrYrsGThT4FyWcyQYtmcHh7sYTDMfXu_ypDbY0vFWDOZ7uIRZ" +
                    "KzxYLTctx8cdkjIlBhBqoaiMKyF2n7quen_6BEG_pBgrnMfwi6YWWHYx";
    private String search_term;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private double mLat, mLgn;
    private TextView mLatitudeText,mLongitudeText;
    private List<Business> businesses;
    private HashMap<Marker, MarkerInfo> markerImages;




    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    private void startNewWebView(String url) {
        if (!url.isEmpty()) {
            Intent webViewIntent = new Intent(this, InfoWindowWebActivity.class);
            webViewIntent.putExtra(URL_TO_VIEW, url);

            startActivity(webViewIntent);

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        search_term = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        markerImages = new HashMap<Marker, MarkerInfo>();



        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

        } else {
            // Show rationale and request permission.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSIONS_REQUEST_FINE_LOCATION);

                // PERMISSIONS_REQUEST_FINE_LOCATION is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }

        }
        // To check if the apiclient has been initialized
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }




        setContentView(R.layout.activity_maps);

    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            if (mLastLocation != null) {
                //mLatitudeText.setText(String.valueOf(mLastLocation.getLatitude()));
                //mLongitudeText.setText(String.valueOf(mLastLocation.getLongitude()));

                mLat = mLastLocation.getLatitude();
                mLgn = mLastLocation.getLongitude();
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
                mapFragment.getMapAsync(this);
            }
        } else {
            // Show rationale and request permission.
        }

    }

    @Override
    public void onConnectionSuspended(int i){

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult){

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        System.out.println("my location in onMapReady: " + mLat + ", " + mLgn);
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setInfoWindowAdapter(new SmartInfoWindowAdapter());
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                String url = markerImages.get(marker).webUrl;
                startNewWebView(url);
            }
        });

        LatLng myLl = new LatLng(mLat,mLgn);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            // Show rationale and request permission.
        }

        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        YelpApiEndPointInterface apiService = retrofit.create(YelpApiEndPointInterface.class);
        Call<YelpParser> call =
                apiService.getBusinesses(YELP_TOKEN, search_term, mLat, mLgn);
        call.enqueue(new Callback<YelpParser>() {
            @Override
            public void onResponse(Call<YelpParser> call, Response<YelpParser> response) {
                if (response.body() == null) {
                    System.out.println("body is null");
                }
                businesses = response.body().getBusinesses();

                for (int i = 0; i < businesses.size(); i++) {
                    LatLng ll = new LatLng(businesses.get(i).getCoordinates().getLatitude(),
                            businesses.get(i).getCoordinates().getLongitude());
                    Marker marker;
                    if (i == 0) {
                        marker = mMap.addMarker(new MarkerOptions()
                                .position(ll)
                                .title(businesses.get(i).
                                        getName())
                                .snippet(businesses.get(i).
                                        getDistance().intValue() + " meters away from you")
                                .icon(BitmapDescriptorFactory.
                                        defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        );

                    } else {
                        marker = mMap.addMarker(new MarkerOptions()
                                .position(ll)
                                .title(businesses.get(i).
                                        getName())
                                .snippet(businesses.get(i).
                                        getDistance().intValue() + " meters away from you")
                        );
                    }
                    if (marker != null) {
                        MarkerInfo markerInfo = new MarkerInfo(businesses.get(i).getImageUrl(),
                                businesses.get(i).getUrl(),
                                businesses.get(i).getReviewCount(),
                                businesses.get(i).getRating());
                        markerImages.put(marker, markerInfo);
                    }
                }
            }

            @Override
            public void onFailure(Call<YelpParser> call, Throwable t) {
                System.out.println("businesses not grabbed");
                System.out.println(t.toString());
            }

        });
        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(myLl));
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_FINE_LOCATION) {
            if (permissions.length == 1 &&
                    permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                // Permission was denied. Display an error message.
            }
        }

    }





}
