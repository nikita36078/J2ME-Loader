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

package javax.microedition.amms.control.tuner;

import java.util.Date;

import javax.microedition.media.MediaException;

public interface RDSControl extends javax.microedition.media.Control {
	public String RDS_NEW_DATA = "RDS_NEW_DATA";
	public String RDS_NEW_ALARM = "RDS_ALARM";
	public String RADIO_CHANGED = "radio_changed";

	public boolean isRDSSignal();

	public String getPS();

	public String getRT();

	public short getPTY();

	public String getPTYString(boolean longer);

	public short getPI();

	public int[] getFreqsByPTY(short PTY);

	public int[][] getFreqsByTA(boolean TA);

	public String[] getPSByPTY(short PTY);

	public String[] getPSByTA(boolean TA);

	public Date getCT();

	public boolean getTA();

	public boolean getTP();

	void setAutomaticSwitching(boolean automatic) throws MediaException;

	public boolean getAutomaticSwitching();

	public void setAutomaticTA(boolean automatic) throws MediaException;

	public boolean getAutomaticTA();
}
