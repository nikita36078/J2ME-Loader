/*
 *  Copyright 2022 Yury Kharchenko
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.jblend.graphics.j3d;

public class Util3D {

	public static int sqrt(int x) {
		if (x == 0) return 0;
		if (x < 0) {
			throw new IllegalArgumentException("Negative arg=" + x);
		}
		return (int) Math.round(Math.sqrt(x));
	}

	public static int sin(int a) {
		double radian = a * Math.PI / 2048;
		return (int) Math.round(Math.sin(radian) * 4096);
	}

	public static int cos(int a) {
		return sin(a + 1024);
	}
}