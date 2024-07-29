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
