/*
 * J2ME Loader
 * Copyright (C) 2017 Nikita Shakarun
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package javax.microedition.sensor;

public interface ChannelInfo {
	public static final int TYPE_DOUBLE = 1;
	public static final int TYPE_INT = 2;
	public static final int TYPE_OBJECT = 4;

	float getAccuracy();

	int getDataType();

	MeasurementRange[] getMeasurementRanges();

	String getName();

	int getScale();

	Unit getUnit();
}
