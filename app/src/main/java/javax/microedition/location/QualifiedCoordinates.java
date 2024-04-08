/*
 * Copyright 2023 Arman Jussupgaliyev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
