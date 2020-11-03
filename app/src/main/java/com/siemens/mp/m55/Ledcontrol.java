/*
 *  Copyright 2020 Yury Kharchenko
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

package com.siemens.mp.m55;

public class Ledcontrol {
	public static final int LED_BOTTOM = 1;
	public static final int LED_TOP = 0;
	public static final int P_BEAT = 3;
	public static final int P_CONSTANLY_LITEUP = 11;
	public static final int P_ETERNITY = 6;
	public static final int P_IDLE = 12;
	public static final int P_LIGHTHOUSE = 1;
	public static final int P_LIMELIGHT = 8;
	public static final int P_NORMAL_BLINKING = 9;
	public static final int P_PULSATING = 10;
	public static final int P_RUNWAY = 4;
	public static final int P_SPEED = 0;
	public static final int P_STROBO = 7;
	public static final int P_TRANCE = 5;
	public static final int P_WAVE = 2;

	public static void switchON(int led){}
	public static void playPattern(int pattern){}
	public static void stopPattern(){}
	public static void switchOFF(int led){}
}
