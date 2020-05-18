/*
 * Copyright 2020 Nikita Shakarun
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

package com.motorola.funlight;

public class FunLight {
	public static int BLACK = 0x00000000;
	public static int BLANK = 0;
	public static int BLUE = 0x000000FF;
	public static int CYAN = 0x0000FFFF;
	public static int GREEN = 0x0000FF00;
	public static int IGNORED = 2;
	public static int MAGENTA = 0x00FF00FF;
	public static int OFF = 0x00000000;
	public static int ON = 0x00FFFFFF;
	public static int QUEUED = 1;
	public static int RED = 0x00FF0000;
	public static int SUCCESS = 0;
	public static int WHITE = 0x00FFFFFF;
	public static int YELLOW = 0x00FFFF00;

	public static int getControl() {
		return QUEUED;
	}

	public static Region getRegion(int ID) {
		return new BlankRegion();
	}

	public static Region[] getRegions() {
		return null;
	}

	public static int[] getRegionsIDs() {
		return null;
	}

	public static void releaseControl() {
	}

	public static int setColor(byte red, byte green, byte blue) {
		return QUEUED;
	}

	public static int setColor(int color) {
		return QUEUED;
	}

	static class BlankRegion implements Region {
		@Override
		public int getColor() {
			return QUEUED;
		}

		@Override
		public int getControl() {
			return QUEUED;
		}

		@Override
		public int getID() {
			return QUEUED;
		}

		@Override
		public void releaseControl() {
		}

		@Override
		public int setColor(byte red, byte green, byte blue) {
			return QUEUED;
		}

		@Override
		public int setColor(int color) {
			return QUEUED;
		}
	}
}
