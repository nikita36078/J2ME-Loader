/*
 *  Copyright 2021 Yury Kharchenko
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

package javax.microedition.pim;

import java.util.Enumeration;

public class RepeatRule {
	public static final int FREQUENCY = 0;
	public static final int DAY_IN_MONTH = 1;
	public static final int DAY_IN_WEEK = 2;
	public static final int DAY_IN_YEAR = 4;
	public static final int MONTH_IN_YEAR = 8;
	public static final int WEEK_IN_MONTH = 16;
	public static final int COUNT = 32;
	public static final int END = 64;
	public static final int INTERVAL = 128;
	public static final int DAILY = 16;
	public static final int WEEKLY = 17;
	public static final int MONTHLY = 18;
	public static final int YEARLY = 19;
	public static final int FIRST = 1;
	public static final int SECOND = 2;
	public static final int THIRD = 4;
	public static final int FOURTH = 8;
	public static final int FIFTH = 16;
	public static final int LAST = 32;
	public static final int SECONDLAST = 64;
	public static final int THIRDLAST = 128;
	public static final int FOURTHLAST = 256;
	public static final int FIFTHLAST = 512;
	public static final int SATURDAY = 1024;
	public static final int FRIDAY = 2048;
	public static final int THURSDAY = 4096;
	public static final int WEDNESDAY = 8192;
	public static final int TUESDAY = 16384;
	public static final int MONDAY = 32768;
	public static final int SUNDAY = 65536;
	public static final int JANUARY = 131072;
	public static final int FEBRUARY = 262144;
	public static final int MARCH = 524288;
	public static final int APRIL = 1048576;
	public static final int MAY = 2097152;
	public static final int JUNE = 4194304;
	public static final int JULY = 8388608;
	public static final int AUGUST = 16777216;
	public static final int SEPTEMBER = 33554432;
	public static final int OCTOBER = 67108864;
	public static final int NOVEMBER = 134217728;
	public static final int DECEMBER = 268435456;

	public RepeatRule() {
	}

	public Enumeration dates(long startDate, long subsetBeginning, long subsetEnding) {
		return null;
	}

	public void addExceptDate(long date) {
	}

	public void removeExceptDate(long date) {
	}

	public Enumeration getExceptDates() {
		return null;
	}

	public int getInt(int field) {
		return 0;
	}

	public void setInt(int field, int value) {
	}

	public long getDate(int field) {
		return 0;
	}

	public void setDate(int field, long value) {
	}

	public int[] getFields() {
		return null;
	}

	public boolean equals(Object obj) {
		return false;
	}
}
