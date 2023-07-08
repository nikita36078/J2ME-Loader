package javax.microedition.location;

public class QualifiedCoordinates extends Coordinates {
	float horizontalAccuracy = Float.NaN;
	float verticalAccuracy = Float.NaN;

	public QualifiedCoordinates(double latitude, double longitude, float altitude, float horizontalAccuracy,
			float verticalAccuracy) {
		super(latitude, longitude, altitude);
		setHorizontalAccuracy(horizontalAccuracy);
		setVerticalAccuracy(verticalAccuracy);
	}

	QualifiedCoordinates() {
		this(0.0D, 0.0D, Float.NaN, Float.NaN, Float.NaN);
	}

	QualifiedCoordinates(QualifiedCoordinates other) {
		this(other.latitude, other.longitude, other.altitude, other.horizontalAccuracy, other.verticalAccuracy);
	}

	public float getHorizontalAccuracy() {
		return this.horizontalAccuracy;
	}

	public float getVerticalAccuracy() {
		return this.verticalAccuracy;
	}

	public void setHorizontalAccuracy(float horizontalAccuracy) {
		if ((Float.isNaN(horizontalAccuracy)) || (horizontalAccuracy >= 0.0F)) {
			this.horizontalAccuracy = horizontalAccuracy;
		} else {
			throw new IllegalArgumentException();
		}
	}

	public void setVerticalAccuracy(float verticalAccuracy) {
		if ((Float.isNaN(verticalAccuracy)) || (verticalAccuracy >= 0.0F)) {
			this.verticalAccuracy = verticalAccuracy;
		} else {
			throw new IllegalArgumentException();
		}
	}

	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if (!super.equals(other)) {
			return false;
		}
		if (!(other instanceof QualifiedCoordinates)) {
			return false;
		}
		QualifiedCoordinates o = (QualifiedCoordinates) other;
		if (Float.floatToIntBits(getHorizontalAccuracy()) != Float.floatToIntBits(o.getHorizontalAccuracy())) {
			return false;
		}
		if (Float.floatToIntBits(getVerticalAccuracy()) != Float.floatToIntBits(o.getVerticalAccuracy())) {
			return false;
		}
		return true;
	}

	public int hashCode() {
		int result = 17;
		result = 37 * result + Float.floatToIntBits(getHorizontalAccuracy());
		result = 37 * result + Float.floatToIntBits(getVerticalAccuracy());
		result = 37 * result + super.hashCode();
		return result;
	}

	protected QualifiedCoordinates clone() {
		QualifiedCoordinates clone = new QualifiedCoordinates(this);
		return clone;
	}
}
