package com.group3.smartshop;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
//import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.group3.smartshop.MapsActivity.BASE_URL;
import static com.group3.smartshop.MapsActivity.YELP_TOKEN;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        SearchFragment.OnFragmentInteractionListener{


    private DrawerLayout mDrawer;
    private Toolbar toolbar;
    private NavigationView nvDrawer;
    private FirebaseAuth auth;
    private TextView name, email;
    private ArrayList<Business> recommendations;
    private List<Business> businesses;

    public final static String EXTRA_MESSAGE = "group3.CSE110smartshop.MESSAGE";


    @Override
    public void onFragmentInteraction(Uri uri){

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Get Firebase auth instance
        auth = FirebaseAuth.getInstance();

        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawer.addDrawerListener(toggle);
        toggle.syncState();

        nvDrawer = (NavigationView) findViewById(R.id.nav_view);
        nvDrawer.setNavigationItemSelectedListener(this);

        //get current user
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        View header = nvDrawer.getHeaderView(0);
        name = (TextView)header.findViewById(R.id.nav_userName);
        email = (TextView)header.findViewById(R.id.nav_email);
        name.setText(user.getDisplayName());
        email.setText(user.getEmail());


    }

    public void searchNearby(View view) {
        EditText editText = (EditText) findViewById(R.id.edit_message);
        String message = editText.getText().toString();
        if(!message.isEmpty()) {

            Intent intent = new Intent(this, MapsActivity.class);
            intent.putExtra(EXTRA_MESSAGE, message);

            startActivity(intent);
        }
    }

    public void searchOnline (View view){
        EditText editText = (EditText) findViewById(R.id.edit_message);
        String message = editText.getText().toString();
        if (!message.isEmpty()) {
            Intent intent = new Intent(this, OnlineSearchActivity.class);
            intent.putExtra(EXTRA_MESSAGE, message);

            startActivity(intent);
        }
    }

    public void profileButton (View view){

        Intent intent = new Intent (this, myProfileActivity.class);
        startActivity(intent);
    }

    public void recButton (View view){

        Intent intent = new Intent (this, Recommendation.class);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }




    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.search) {
            Fragment fragment = new SearchFragment();
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
        } else if (id == R.id.recommendations) {
            Intent intent = new Intent (this, Recommendation.class);
            startActivity(intent);
        }else if (id == R.id.nav_profile) {
            Intent intent = new Intent (this, myProfileActivity.class);
            startActivity(intent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}
