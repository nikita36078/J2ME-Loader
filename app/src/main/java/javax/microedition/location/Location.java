package javax.microedition.location;

public class Location {
	public static final int MTE_SATELLITE = 1;
	public static final int MTE_TIMEDIFFERENCE = 2;
	public static final int MTE_TIMEOFARRIVAL = 4;
	public static final int MTE_CELLID = 8;
	public static final int MTE_SHORTRANGE = 16;
	public static final int MTE_ANGLEOFARRIVAL = 32;
	public static final int MTY_TERMINALBASED = 65536;
	public static final int MTY_NETWORKBASED = 131072;
	public static final int MTA_ASSISTED = 262144;
	public static final int MTA_UNASSISTED = 524288;
	private android.location.Location androidLocation;
	private int locationMethod;

	protected Location(android.location.Location androidLocation, int method) {
		this.androidLocation = androidLocation;
		this.locationMethod = method;
	}

	public boolean isValid() {
		return false;
	}

	public long getTimestamp() {
		return androidLocation.getTime();
	}

	public QualifiedCoordinates getQualifiedCoordinates() {
		return new QualifiedCoordinates(androidLocation.getLatitude(), androidLocation.getLongitude(), (float) androidLocation.getAltitude(), androidLocation.getAccuracy(), androidLocation.getAccuracy());
	}

	public float getSpeed() {
		if(!androidLocation.hasSpeed()) {
			return Float.NaN;
		}
		return androidLocation.getSpeed();
	}

	public float getCourse() {
		if(!androidLocation.hasBearing()) {
			return Float.NaN;
		}
		return androidLocation.getBearing();
	}

	public int getLocationMethod() {
		return locationMethod;
	}

	public AddressInfo getAddressInfo() {
		return null;
	}

	public String getExtraInfo(String mimetype) {
		return null;
	}
}
