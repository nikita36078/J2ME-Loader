package javax.microedition.location;

public class Orientation {
	private float azimuth;
	private float pitch;
	private float roll;
	private boolean isMagnetic;

	public Orientation(float azimuth, boolean isMagnetic, float pitch, float roll) {
		this.azimuth = azimuth;
		this.isMagnetic = isMagnetic;
		this.pitch = pitch;
		this.roll = roll;
	}

	public float getCompassAzimuth() {
		return this.azimuth;
	}

	public boolean isOrientationMagnetic() {
		return this.isMagnetic;
	}

	public float getPitch() {
		return this.pitch;
	}

	public float getRoll() {
		return this.roll;
	}

	public static Orientation getOrientation() throws LocationException {
		throw new LocationException();
	}
}
