package javax.microedition.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import javax.microedition.util.ContextHolder;

public class LocationProviderImpl extends LocationProvider implements android.location.LocationListener, GpsStatus.NmeaListener, Runnable {
    private final Thread thread;
    private String provider;
    private LocationListener listener;
    public static final Object permissionLock = new Object();
    public static boolean permissionResult;
    private Object timeoutLocationLock = new Object();
    private Object listenerLocationLock = new Object();
    private Location location;
    private String[] nmea = new String[7];
    private String currentSatellite = "GP";
    private String nmeaSequence = "";
    private int timeout;
    private boolean gotLocation;

    public LocationProviderImpl() {

        this.provider = gpsProviderEnabled ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;
        ContextHolder.getActivity().runOnUiThread(() -> locationManager.addNmeaListener((GpsStatus.NmeaListener) this));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ContextHolder.getActivity().runOnUiThread(() -> locationManager.addNmeaListener(new OnNmeaMessageListener() {
                @Override
                public void onNmeaMessage(String message, long timestamp) {
                    parseNmeaMessage(message, timestamp);
                }
            }));
        }
        thread = new Thread(this, "LocationProvider Thread");
    }

    private void parseNmeaMessage(String message, long timestamp) {
        for (String s1: message.split("\n")) {
            String[] s2 = s1.split(",");
            String satellite = s2[0].substring(1, 3);
            String sentence = s2[0].substring(3, 6);
            if (sentence.equals("GSA")) {
                if (!satellite.equals(currentSatellite)) {
                    nmeaSequence = message;
                    currentSatellite = satellite;
                } else {
                    nmeaSequence += (nmeaSequence.length() == 0 ? "" : "\n") + message;
                }
            } else if (satellite.equals(currentSatellite) || sentence.equals("GSV")) {
                nmeaSequence += (nmeaSequence.length() == 0 ? "" : "\n") + message;
            }
        }
    }

    @Override
    public Location getLocation(int timeout) throws LocationException {
        LocationProviderImpl.requestLocationPermission();
        if (timeout <= 0 && timeout != -1) {
            throw new IllegalArgumentException("Timeout value is invalid");
        }
        if (getState() == LocationProvider.OUT_OF_SERVICE) {
            throw new LocationException("All positioning methods are disabled");
        }
        ContextHolder.getActivity().runOnUiThread(() -> locationManager.requestSingleUpdate(provider, this, null));
        location = null;
        try {
            synchronized (timeoutLocationLock) {
                if (timeout > 0) {
                    timeoutLocationLock.wait(timeout * 1000);
                } else {
                    timeoutLocationLock.wait();
                }
            }
        } catch (InterruptedException e) {
        }
        if (location != null) {
            return location;
        }
        throw new LocationException("Location request timed out");
    }

    @Override
    public void setLocationListener(LocationListener listener, final int interval, int timeout, int maxAge) {
        if(listener == null) {
            reset();
            return;
        }
        if (interval < -1) {
            throw new IllegalArgumentException("interval");
        }
        if (interval > 0) {
            if (timeout > interval) {
                throw new IllegalArgumentException("timeout > interval");
            }
            if (timeout < 1 && timeout != -1) {
                throw new IllegalArgumentException("timeout");
            }
            if (maxAge < 1 && maxAge != -1) {
                throw new IllegalArgumentException("maxAge");
            }
            if (maxAge > interval) {
                throw new IllegalArgumentException("maxAge > interval");
            }
        }
        this.timeout = timeout * 1000;
        LocationProviderImpl.requestLocationPermission();
        if (this.listener != null) {
            reset();
        } else {
            thread.start();
        }
        this.listener = listener;
        ContextHolder.getActivity().runOnUiThread(() ->
                locationManager.requestLocationUpdates(provider, interval < 0 ? 2000 : interval * 1000L, 0, this));
    }

    @Override
    public int getState() {
        gpsProviderEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        networkProviderEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!gpsProviderEnabled && !networkProviderEnabled) {
            return LocationProvider.OUT_OF_SERVICE;
        }
        return LocationProvider.AVAILABLE;
    }

    @Override
    public void reset() {
        ContextHolder.getActivity().runOnUiThread(() -> locationManager.removeUpdates(this));
    }

    @Override
    public void onLocationChanged(@NonNull android.location.Location location) {
        Location location1 = new Location(location, getLocationMethod(), nmeaSequence);
        nmeaSequence = "";
        if (listener != null) {
            listener.locationUpdated(this, location1);
        }
        this.location = location1;
        gotLocation = true;
        synchronized (timeoutLocationLock) {
            timeoutLocationLock.notify();
        }
        synchronized (listenerLocationLock) {
            listenerLocationLock.notify();
        }
    }

    private int getLocationMethod() {
        return gpsProviderEnabled ? Location.MTE_SATELLITE : Location.MTY_NETWORKBASED;
    }

    @Override
    public void onProviderEnabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            gpsProviderEnabled = true;
        } else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
            networkProviderEnabled = true;
        }
        if (listener != null) {
            listener.providerStateChanged(this, !gpsProviderEnabled && !networkProviderEnabled ?
                    LocationProvider.OUT_OF_SERVICE : LocationProvider.AVAILABLE);
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            gpsProviderEnabled = false;
        } else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
            networkProviderEnabled = false;
        }
        if (listener != null) {
            listener.providerStateChanged(this, !gpsProviderEnabled && !networkProviderEnabled ?
                    LocationProvider.OUT_OF_SERVICE : LocationProvider.AVAILABLE);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        if(status == LocationProvider.AVAILABLE) {
            onProviderEnabled(provider);
        } else {
            onProviderDisabled(provider);
        }
    }

    protected static void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(ContextHolder.getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ContextHolder.getActivity(), new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            try {
                synchronized (LocationProviderImpl.permissionLock) {
                    permissionLock.wait();
                }
            } catch (InterruptedException e) {
                return;
            }
            if (!permissionResult) {
                throw new SecurityException();
            }
        }
    }

    @SuppressLint("WrongConstant")
    public boolean meetsCriteria(Criteria criteria) {
        if (criteria == null) {
            return true;
        }
        android.location.Criteria androidCriteria = new android.location.Criteria();
        androidCriteria.setAltitudeRequired(criteria.isAltitudeRequired());
        androidCriteria.setCostAllowed(criteria.isAllowedToCost());
        androidCriteria.setPowerRequirement(criteria.getPreferredPowerConsumption());
        androidCriteria.setSpeedRequired(criteria.isSpeedAndCourseRequired());
        androidCriteria.setBearingRequired(criteria.isSpeedAndCourseRequired());
        return locationManager.getProvider(provider).meetsCriteria(androidCriteria);
    }

    @Override
    public void onNmeaReceived(long timestamp, String nmea) {
        parseNmeaMessage(nmea, timestamp);
    }

    @Override
    public void run() {
        try {
            while (true) {
                synchronized (listenerLocationLock) {
                    if (timeout > 0) {
                        listenerLocationLock.wait(timeout);
                    } else {
                        listenerLocationLock.wait();
                    }
                }
                if (!gotLocation && listener != null) {
                    listener.locationUpdated(this, new Location(null, getLocationMethod(), null));
                }
                gotLocation = false;
            }
        } catch (Exception e) {
        }
    }

}
