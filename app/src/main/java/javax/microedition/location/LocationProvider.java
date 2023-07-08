package javax.microedition.location;

import android.content.Context;
import android.location.LocationManager;

import javax.microedition.util.ContextHolder;

public abstract class LocationProvider {
	public static final int AVAILABLE = 1;
	public static final int TEMPORARILY_UNAVAILABLE = 2;
	public static final int OUT_OF_SERVICE = 3;

	protected static LocationManager locationManager;
	protected static boolean gpsProviderEnabled;
	protected static boolean networkProviderEnabled;

	private static void initLocationManager() throws LocationException {
		LocationProviderImpl.requestLocationPermission();
		if (locationManager == null) {
			locationManager = (LocationManager) ContextHolder.getActivity().getSystemService(Context.LOCATION_SERVICE);
		}
		gpsProviderEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		networkProviderEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		if (!gpsProviderEnabled && !networkProviderEnabled) {
			throw new LocationException("All positioning methods are disabled");
		}
	}

	public static LocationProvider getInstance(Criteria criteria) throws LocationException {
		initLocationManager();
		LocationProviderImpl lp = new LocationProviderImpl();
		if (lp.meetsCriteria(criteria)) {
			return lp;
		}
		return null;
	}

	public abstract Location getLocation(int timeout) throws LocationException, InterruptedException;

	public abstract void setLocationListener(LocationListener listener, int interval, int timeout, int maxAge);

	public static Location getLastKnownLocation() {
		try {
			initLocationManager();
			android.location.Location androidLocation;
			if (gpsProviderEnabled) {
				androidLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			} else {
				androidLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			}
			return new Location(androidLocation, gpsProviderEnabled ? Location.MTE_SATELLITE : Location.MTY_NETWORKBASED, null);
		} catch (LocationException e) {
		}
		return null;
	}

	public abstract int getState();

	public abstract void reset();

	public static void addProximityListener(ProximityListener listener, Coordinates coordinates, float proximityRadius)
			throws LocationException {
		throw new LocationException();
	}

	public static void removeProximityListener(ProximityListener listener) {
	}
}
