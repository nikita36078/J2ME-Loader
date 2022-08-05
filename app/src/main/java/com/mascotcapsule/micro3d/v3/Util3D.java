/*
 * Copyright 2020 Yury Kharchenko
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

package com.mascotcapsule.micro3d.v3;

@SuppressWarnings({"unused", "WeakerAccess"})
public class Util3D {

	static final String TAG = "micro3d";

	public static int sqrt(int p) {
		if (p == 0) return 0;
		double a;
		if (p < 0) {
			if (p > 0xfffd0002) return 0xffff;
			a = p & 0xffffffffL;
		} else {
			a = p;
		}
		return (int) Math.round(Math.sqrt(a));
	}

	public static int sin(int p) {
		double radian = p * Math.PI / 2048;
		return (int) Math.round(Math.sin(radian) * 4096);
	}

	public static int cos(int p) {
		return sin(p + 1024);
	}
}
