package com.cajama.malarialite.newreport;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Toast;
import com.cajama.malarialite.R;

/**
 * Created by GMGA on 7/28/13.
 */
public class GetLocation {//implements LocationListener {
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    private static final String GPS_PROVIDER = LocationManager.GPS_PROVIDER;
    private static final String NETWORK_PROVIDER = LocationManager.NETWORK_PROVIDER ;
    public static final String BROADCAST_ACTION_LOCATION = "com.cajama.malarialite.newreport.NewReportActivity";
    private Context myContext;
    private boolean isCancelDialogOpen;
    private boolean enabled;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location currentBestGPS;
    private Location currentBestNetwork;
    private Intent intentUpdate;
    private Handler handler = new Handler();

    public GetLocation(Context mContext){
        myContext = mContext;
        locationManager = (LocationManager) myContext.getSystemService(Context.LOCATION_SERVICE);
        currentBestGPS = locationManager.getLastKnownLocation(GPS_PROVIDER);
        currentBestNetwork = locationManager.getLastKnownLocation(NETWORK_PROVIDER);

        intentUpdate = new Intent(BROADCAST_ACTION_LOCATION);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location.getProvider().equalsIgnoreCase(GPS_PROVIDER)) {
                    if (isBetterLocation(location, currentBestGPS)) {
                        currentBestGPS = location;
                        System.out.println("GPS: " + location.getLatitude() + " " + location.getLongitude());
                        Location test = locationManager.getLastKnownLocation(GPS_PROVIDER);
                        System.out.println("TEST: " + test.getLatitude() + " " + test.getLongitude());
                    }
                }
                else {
                    if (isBetterLocation(location, currentBestNetwork)) {
                        currentBestNetwork = location;
                        System.out.println("Network: " + location.getLatitude() + " " + location.getLongitude());
                        Location test = locationManager.getLastKnownLocation(NETWORK_PROVIDER);
                        System.out.println("TEST: " + test.getLatitude() + " " + test.getLongitude());
                    }
                }
                handler.removeCallbacks(update);
                handler.postDelayed(update, 1000);
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {
                System.out.println(s + ": wew");
            }
        };

        checkGps();
    }

    public void checkGps() {
        enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!enabled) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(myContext);
            alertDialogBuilder
                    .setTitle("Warning (Babala)")
                    .setMessage("This action needs GPS Settings to be enabled.\n(Kailangan ang GPS Settings para sa aksyon na ito.)")
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int id) {
                            isCancelDialogOpen = false;
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            myContext.startActivity(intent);
                            System.out.println("activate gps settings");
                        }
                    });

            if (!isCancelDialogOpen) {
                isCancelDialogOpen = true;
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        }
        else {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 0, locationListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, locationListener);
        }
    }

    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }
        else if (location == null) {
            return false;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    public Location getLocation() {
        //locationManager.removeUpdates(locationListener);
        return (isBetterLocation(currentBestNetwork, currentBestGPS) ? currentBestNetwork : currentBestGPS);
    }

    public void removeUpdates() {
        System.out.println("remove updates");
        locationManager.removeUpdates(locationListener);
    }

    private Runnable update = new Runnable() {
        @Override
        public void run() {
            intentUpdate.putExtra("update", "update");
            myContext.sendBroadcast(intentUpdate);
        }
    };
}
