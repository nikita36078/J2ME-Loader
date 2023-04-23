package javax.microedition.location;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import javax.microedition.shell.MicroActivity;
import javax.microedition.util.ContextHolder;

public class LocationProviderImpl extends LocationProvider implements android.location.LocationListener {
    private String provider;
    private Criteria criteria;
    private LocationListener listener;

    public static final Object permissionLock = new Object();
    public static boolean permissionResult;
    private Object locationLock = new Object();
    private Location location;

    public LocationProviderImpl(Criteria criteria) {
        super();
        this.criteria = criteria;
        this.provider = gpsProviderEnabled ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;
    }

    @Override
    public Location getLocation(int timeout) throws LocationException {
        LocationProviderImpl.requestLocationPermission();
        ContextHolder.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                locationManager.requestSingleUpdate(provider, LocationProviderImpl.this, null);
            }
        });
        location = null;
        try {
            synchronized (locationLock) {
                locationLock.wait(timeout * 1000);
            }
        } catch (InterruptedException e) {
        }
        if(location != null) {
            return location;
        } else {
            throw new LocationException("timed out");
        }
        //return getLastKnownLocation();
    }

    @Override
    public void setLocationListener(LocationListener listener, final int interval, int timeout, int maxAge) {
        LocationProviderImpl.requestLocationPermission();
        if(this.listener == null) {
            ContextHolder.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    locationManager.requestLocationUpdates(provider, interval < 0 ? 30000 : interval * 1000, 10F, LocationProviderImpl.this);
                }
            });
        }
        this.listener = listener;
    }

    @Override
    public int getState() {
        gpsProviderEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        networkProviderEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if(!gpsProviderEnabled && !networkProviderEnabled) {
            return LocationProvider.OUT_OF_SERVICE;
        }
        return gpsState;
    }

    @Override
    public void reset() {
        locationManager.removeUpdates(this);
    }

    @Override
    public void onLocationChanged(@NonNull android.location.Location location) {
        Location location1 = new Location(location, gpsProviderEnabled ? Location.MTE_SATELLITE : Location.MTY_NETWORKBASED);
        if(listener != null) {
            listener.locationUpdated(this, location1);
        }
        this.location = location1;
        synchronized (locationLock) {
            locationLock.notify();
        }
    }

    protected static void requestLocationPermission() {
        if(ContextCompat.checkSelfPermission(ContextHolder.getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ContextHolder.getActivity(), new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            try {
                synchronized (LocationProviderImpl.permissionLock) {
                    permissionLock.wait();
                }
            } catch (InterruptedException e) {
                return;
            }
            if(!permissionResult) {
                throw new SecurityException();
            }
        }
    }

    public
}
