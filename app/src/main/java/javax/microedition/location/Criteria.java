package javax.microedition.location;

public class Criteria {
	public static final int NO_REQUIREMENT = 0;
	public static final int POWER_USAGE_LOW = 1;
	public static final int POWER_USAGE_MEDIUM = 2;
	public static final int POWER_USAGE_HIGH = 3;
	private int horizontalAccuracy = 0;
	private int verticalAccuracy = 0;
	private int maxResponseTime = 0;
	private int powerConsumption = 0;
	private boolean costAllowed = true;
	private boolean speedRequired = false;
	private boolean altitudeRequired = false;
	private boolean addressInfoRequired = false;

	public int getPreferredPowerConsumption() {
		return this.powerConsumption;
	}

	public boolean isAllowedToCost() {
		return this.costAllowed;
	}

	public int getVerticalAccuracy() {
		return this.verticalAccuracy;
	}

	public int getHorizontalAccuracy() {
		return this.horizontalAccuracy;
	}

	public int getPreferredResponseTime() {
		return this.maxResponseTime;
	}

	public boolean isSpeedAndCourseRequired() {
		return this.speedRequired;
	}

	public boolean isAltitudeRequired() {
		return this.altitudeRequired;
	}

	public boolean isAddressInfoRequired() {
		return this.addressInfoRequired;
	}

	public void setHorizontalAccuracy(int accuracy) {
		this.horizontalAccuracy = accuracy;
	}

	public void setVerticalAccuracy(int accuracy) {
		this.verticalAccuracy = accuracy;
	}

	public void setPreferredResponseTime(int time) {
		this.maxResponseTime = time;
	}

	public void setPreferredPowerConsumption(int level) {
		this.powerConsumption = level;
	}

	public void setCostAllowed(boolean costAllowed) {
		this.costAllowed = costAllowed;
	}

	public void setSpeedAndCourseRequired(boolean speedAndCourseRequired) {
		this.speedRequired = speedAndCourseRequired;
	}

	public void setAltitudeRequired(boolean altitudeRequired) {
		this.altitudeRequired = altitudeRequired;
	}

	public void setAddressInfoRequired(boolean addressInfoRequired) {
		this.addressInfoRequired = addressInfoRequired;
	}
}
