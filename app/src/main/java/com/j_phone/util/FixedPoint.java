/*
 * Copyright 2024 Yury Kharchenko
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

package com.j_phone.util;

public class FixedPoint {
	private double value;

	public FixedPoint() {
	}

	public FixedPoint(int value) {
		this(value / 65536.0);
	}

	public FixedPoint(double value) {
		this.value = value;
	}

	private static FixedPoint getPI() {
		return new FixedPoint(Math.PI);
	}

	public static FixedPoint getMaximum() {
		return new FixedPoint(Double.MAX_VALUE);
	}

	public static FixedPoint getMinimum() {
		return new FixedPoint(Double.MIN_VALUE);
	}

	public FixedPoint acos(FixedPoint v) {
		value = Math.acos(v.value);
		return this;
	}

	public FixedPoint add(FixedPoint n) {
		value += n.value;
		return this;
	}

	public FixedPoint add(int n) {
		value += n / 65536.0;
		return this;
	}

	public FixedPoint asin(FixedPoint v) {
		value = Math.asin(v.value);
		return this;
	}

	public FixedPoint atan(FixedPoint v) {
		value = Math.atan(v.value);
		return this;
	}

	public FixedPoint clone() {
		return new FixedPoint(value);
	}

	public FixedPoint cos(FixedPoint r) {
		value = Math.cos(r.value);
		return this;
	}

	public FixedPoint divide(FixedPoint n) {
		value /= n.value;
		return this;
	}

	public FixedPoint divide(int n) {
		value /= n / 65536.0;
		return this;
	}

	public int getDecimal() {
		int v = (int) ((value - (int) value) * 65536.0);
		if (value < 0.0) {
			return -(v | 0xffff);
		} else {
			return (v & 0xffff);
		}
	}

	public int getInteger() {
		return (int) value;
	}

	public FixedPoint inverse() {
		value = 1 / value;
		return this;
	}

	public boolean isInfinite() {
		return Double.isInfinite(value);
	}

	public FixedPoint multiply(FixedPoint n) {
		value *= n.value;
		return this;
	}

	public FixedPoint multiply(int n) {
		value *= n / 65536.0;
		return this;
	}

	public FixedPoint pow() {
		value = Math.pow(value, 2);
		return this;
	}

	public void setValue(int value) {
		this.value = value / 65536.0;
	}

	public FixedPoint sin(FixedPoint r) {
		value = Math.sin(r.value);
		return this;
	}

	public FixedPoint sqrt() {
		value = Math.sqrt(value);
		return this;
	}

	public FixedPoint subtract(FixedPoint n) {
		value -= n.value;
		return this;
	}

	public FixedPoint subtract(int n) {
		value -= n / 65536.0;
		return this;
	}

	public FixedPoint tan(FixedPoint r) {
		value = Math.tan(r.value);
		return this;
	}
}
