/*
 *  Siemens API for MicroEmulator
 *  Copyright (C) 2003 Markus Heberling <markus@heberling.net>
 *
 *  It is licensed under the following two licenses as alternatives:
 *    1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 *    2. Apache License (the "AL") Version 2.0
 *
 *  You may not use this file except in compliance with at least one of
 *  the above two licenses.
 *
 *  You may obtain a copy of the LGPL at
 *      http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
 *
 *  You may obtain a copy of the AL at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the LGPL or the AL for the specific language governing permissions and
 *  limitations.
 */

package com.siemens.mp.game;

public class MelodyComposer extends com.siemens.mp.misc.NativeMem {
	public static final int TONE_C0 = 0;
	public static final int TONE_CIS0 = 1;
	public static final int TONE_D0 = 2;
	public static final int TONE_DIS0 = 3;
	public static final int TONE_E0 = 4;
	public static final int TONE_F0 = 5;
	public static final int TONE_FIS0 = 6;
	public static final int TONE_G0 = 7;
	public static final int TONE_GIS0 = 8;
	public static final int TONE_A0 = 9;
	public static final int TONE_AIS0 = 10;
	public static final int TONE_H0 = 11;
	public static final int TONE_C1 = 12;
	public static final int TONE_CIS1 = 13;
	public static final int TONE_D1 = 14;
	public static final int TONE_DIS1 = 15;
	public static final int TONE_E1 = 16;
	public static final int TONE_F1 = 17;
	public static final int TONE_FIS1 = 18;
	public static final int TONE_G1 = 19;
	public static final int TONE_GIS1 = 20;
	public static final int TONE_A1 = 21;
	public static final int TONE_AIS1 = 22;
	public static final int TONE_H1 = 23;
	public static final int TONE_C2 = 24;
	public static final int TONE_CIS2 = 25;
	public static final int TONE_D2 = 26;
	public static final int TONE_DIS2 = 27;
	public static final int TONE_E2 = 28;
	public static final int TONE_F2 = 29;
	public static final int TONE_FIS2 = 30;
	public static final int TONE_G2 = 31;
	public static final int TONE_GIS2 = 32;
	public static final int TONE_A2 = 33;
	public static final int TONE_AIS2 = 34;
	public static final int TONE_H2 = 35;
	public static final int TONE_C3 = 36;
	public static final int TONE_CIS3 = 37;
	public static final int TONE_D3 = 38;
	public static final int TONE_DIS3 = 39;
	public static final int TONE_E3 = 40;
	public static final int TONE_F3 = 41;
	public static final int TONE_FIS3 = 42;
	public static final int TONE_G3 = 43;
	public static final int TONE_GIS3 = 44;
	public static final int TONE_A3 = 45;
	public static final int TONE_AIS3 = 46;
	public static final int TONE_H3 = 47;
	public static final int TONE_C4 = 48;
	public static final int TONE_CIS4 = 49;
	public static final int TONE_D4 = 50;
	public static final int TONE_DIS4 = 51;
	public static final int TONE_E4 = 52;
	public static final int TONE_F4 = 53;
	public static final int TONE_FIS4 = 54;
	public static final int TONE_G4 = 55;
	public static final int TONE_GIS4 = 56;
	public static final int TONE_A4 = 57;
	public static final int TONE_PAUSE = 58;
	public static final int NO_TONE = 59;
	public static final int TONE_STOP = 60;
	public static final int TONE_REPEAT = 61;
	public static final int TONE_REPEV = 62;
	public static final int TONE_REPON = 63;
	public static final int TONE_MARK = 64;
	public static final int TONE_REPEAT_MARK = 65;
	public static final int TONE_REPEV_MARK = 66;
	public static final int TONE_REPON_MARK = 67;
	public static final int TONELENGTH_1_1 = 0;
	public static final int TONELENGTH_1_2 = 1;
	public static final int TONELENGTH_1_4 = 2;
	public static final int TONELENGTH_1_8 = 3;
	public static final int TONELENGTH_1_16 = 4;
	public static final int TONELENGTH_1_32 = 5;
	public static final int TONELENGTH_1_64 = 6;
	public static final int TONELENGTH_DOTTED_1_1 = 7;
	public static final int TONELENGTH_DOTTED_1_2 = 8;
	public static final int TONELENGTH_DOTTED_1_4 = 9;
	public static final int TONELENGTH_DOTTED_1_8 = 10;
	public static final int TONELENGTH_DOTTED_1_16 = 11;
	public static final int TONELENGTH_DOTTED_1_32 = 12;
	public static final int TONELENGTH_DOTTED_1_64 = 13;
	public static final int BPM = 60;


	public MelodyComposer() {
	}

	public MelodyComposer(int[] notes, int bpm) {
	}

	public void appendNote(int note, int lenght) {
	}

	public Melody getMelody() {
		return new Melody();
	}

	public int length() {
		return 0;
	}

	public static int maxLength() {
		return 0;
	}

	public void resetMelody() {
	}

	public void setBPM(int bpm) {
	}
}
