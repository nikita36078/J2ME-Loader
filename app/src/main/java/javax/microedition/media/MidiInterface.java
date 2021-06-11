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

package javax.microedition.media;

import org.billthefarmer.mididriver.MidiDriver;

public class MidiInterface {
	private static MidiDriver driver;

	public static synchronized MidiDriver getDriver() {
		if (driver == null) {
			driver = MidiDriver.getInstance();
			driver.start();
		}
		return driver;
	}
}
