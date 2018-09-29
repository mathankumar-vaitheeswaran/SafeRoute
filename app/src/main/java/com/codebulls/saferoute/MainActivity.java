package com.codebulls.saferoute;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback, Animation.AnimationListener, GoogleMap.OnPolylineClickListener, GoogleMap.OnPolygonClickListener {

    private static final int RC_HANDLE_LOCATION = 2;
    private static final PatternItem DOT = new Dot();
    // Create a stroke pattern of a gap followed by a dot.
    private static final int PATTERN_GAP_LENGTH_PX = 20;
    private static final PatternItem GAP = new Gap(PATTERN_GAP_LENGTH_PX);
    private static final List<PatternItem> PATTERN_POLYLINE_DOTTED = Arrays.asList(GAP, DOT);
    MapView mapView;
    Boolean mLocationPermissionGranted = false;
    String passName = "";
    String passAccess = "";
    String passPhone = "";
    Boolean isDiaster = false;

    private void requestLocationPermission() {

        final String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_LOCATION);
        } else {
            mLocationPermissionGranted = true;
        }

        /*final Activity thisActivity = this;
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_LOCATION);
            }
        };*/
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case RC_HANDLE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        //updateLocationUI();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Exit Area
        if (getIntent().getBooleanExtra("EXIT", false)) {
            finish();
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Inform about a Disaster!", Snackbar.LENGTH_LONG)
                        .setAction("Notify", new reportListener()).show();
            }
        });

        Bundle myBundle = getIntent().getExtras();
        if (myBundle != null) {
                passName = myBundle.getString("mAccessUser");
                passAccess = myBundle.getString("mAccessLevel");
                passPhone = myBundle.getString("mAccessPhone");
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View headerView = navigationView.getHeaderView(0);

        TextView navhdr_Name = (TextView) headerView.findViewById(R.id.navhdr_Name);
        TextView navhdr_Access = (TextView) headerView.findViewById(R.id.navhdr_Access);
        TextView navhdr_Phone = (TextView) headerView.findViewById(R.id.navhdr_Mobile);
        navhdr_Name.setText(passName);
        switch (passAccess) {
            case "1":
                navhdr_Access.setText("Admin");
                break;
            case "2":
                navhdr_Access.setText("Volunteer");
                break;
            case "3":
                navhdr_Access.setText("User");
                break;
            default:
                navhdr_Access.setText("Guest User");
                break;
        }
        navhdr_Phone.setText(passPhone);

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

    }

    public void onMapReady(GoogleMap mMap) {
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
            return;
        }
        mMap.setMyLocationEnabled(true);
        // Add a marker in Chennai, India and move the camera.
        LatLng chennai = new LatLng(13.067439, 80.237617);
        mMap.addMarker(new MarkerOptions().position(chennai).title("Chennai"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(chennai, 10));

        if (!isDiaster) {
            return;
        }

        LatLng safeZone1 = new LatLng(12.9532, 80.1416);
        mMap.addMarker(new MarkerOptions().position(safeZone1).title("Govt. Relief Center"));
        LatLng safeZone2 = new LatLng(13.07743, 80.21765);
        mMap.addMarker(new MarkerOptions().position(safeZone2).title("Govt. Emergency Helpdesk"));
        LatLng safeZone3 = new LatLng(13.17743, 80.22765);
        mMap.addMarker(new MarkerOptions().position(safeZone3).title("Food"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(chennai, 10));

        // Instantiates a new Polyline object and adds points to define a rectangle
        PolylineOptions rectOptions = new PolylineOptions()
                .add(new LatLng(12.9532, 80.1416))
                .add(new LatLng(13.07743, 80.21765));
        // Get back the mutable Polyline
        Polyline polyline = mMap.addPolyline(rectOptions);
        polyline.setTag("Safe Route1");

        PolylineOptions rectOptions2 = new PolylineOptions()
                .add(new LatLng(12.9532, 80.1416))
                .add(new LatLng(13.07743, 80.21765));
        // Get back the mutable Polyline
        Polyline polyline2 = mMap.addPolyline(rectOptions2);
        polyline2.setTag("Safe Route2");

        Polygon polygon2 = mMap.addPolygon(new PolygonOptions()
                .clickable(true)
                .add(
                        new LatLng(13.367439, 80.137617),
                        new LatLng(13.067439, 80.137617),
                        new LatLng(12.967439, 80.337617),
                        new LatLng(13.367439, 80.127617),
                        new LatLng(13.367439, 80.137617)));
        polygon2.setTag("Disaster Zone");

        // Set listeners for click events.
        mMap.setOnPolylineClickListener(this);
        mMap.setOnPolygonClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent_Transfer = new Intent(MainActivity.this, SettingsActivity.class);
            MainActivity.this.startActivity(intent_Transfer);
            return true;
        }
        if (id == R.id.action_help) {
            Intent intent_Transfer = new Intent(MainActivity.this, HelpActivity.class);
            MainActivity.this.startActivity(intent_Transfer);
            return true;
        }
        if (id == R.id.action_logout) {
            Intent intent_Transfer = new Intent(MainActivity.this, LoginActivity.class);
            MainActivity.this.startActivity(intent_Transfer);
            return true;
        }
        if (id == R.id.action_exit) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("EXIT", true);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_disaster) {
            Intent intent_Transfer = new Intent(MainActivity.this, ReportActivity.class);
            MainActivity.this.startActivity(intent_Transfer);
        } else if (id == R.id.nav_resource) {

        } else if (id == R.id.nav_route) {

        } else if (id == R.id.nav_alert) {
            Animation animBlink;
            animBlink = AnimationUtils.loadAnimation(this, R.anim.blink);
            animBlink.setAnimationListener(this);

            TextView lblAlert = (TextView) findViewById(R.id.lblAlert);
            lblAlert.setText("Disaster Alert!");
            lblAlert.startAnimation(animBlink);

            isDiaster = true;
            mapView.getMapAsync(this);

            Button btnFindRoute = findViewById(R.id.btnFindRoute);
            btnFindRoute.setEnabled(true);

            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            Menu menuNav=navigationView.getMenu();
            MenuItem nav_auth_disaster = menuNav.findItem(R.id.nav_auth_disaster);
            nav_auth_disaster.setEnabled(true);
            MenuItem nav_AI_news = menuNav.findItem(R.id.nav_AI_news);
            nav_AI_news.setEnabled(true);

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onAnimationStart(Animation animation) {

    }

    @Override
    public void onAnimationEnd(Animation animation) {

    }

    @Override
    public void onAnimationRepeat(Animation animation) {

    }

    @Override
    public void onPolylineClick(Polyline polyline) {
        // Flip from solid stroke to dotted stroke pattern.
        if ((polyline.getPattern() == null) || (!polyline.getPattern().contains(DOT))) {
            polyline.setPattern(PATTERN_POLYLINE_DOTTED);
        } else {
            // The default pattern is a solid stroke.
            polyline.setPattern(null);
        }

        Toast.makeText(this, polyline.getTag().toString(),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPolygonClick(Polygon polygon) {
        // Flip the values of the red, green, and blue components of the polygon's color.
        int color = polygon.getStrokeColor() ^ 0x00ffffff;
        polygon.setStrokeColor(color);
        color = polygon.getFillColor() ^ 0x00ffffff;
        polygon.setFillColor(color);

        Toast.makeText(this, polygon.getTag().toString(), Toast.LENGTH_SHORT).show();
    }

    private class reportListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent_Transfer = new Intent(MainActivity.this, ReportActivity.class);
            MainActivity.this.startActivity(intent_Transfer);
        }
    }
}
