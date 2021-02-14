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

package javax.microedition.amms;

import java.util.HashMap;

import javax.microedition.amms.control.audioeffect.EqualizerControl;
import javax.microedition.media.Control;
import javax.microedition.media.InternalEqualizer;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;

public class GlobalManager {
	private static Spectator spectator = new Spectator();
	private static HashMap<String, Control> controls = new HashMap<>();

	static {
		InternalEqualizer equalizer = new InternalEqualizer();
		controls.put(EqualizerControl.class.getName(), equalizer);
	}

	public static Control[] getControls() {
		return controls.values().toArray(new Control[0]);
	}

	public static Control getControl(String controlType) {
		return controls.get(controlType);
	}

	public static EffectModule createEffectModule() throws MediaException {
		return new EffectModuleImpl();
	}

	public static SoundSource3D createSoundSource3D() throws MediaException {
		return new SoundSource3DImpl();
	}

	public static String[] getSupportedSoundSource3DPlayerTypes() {
		return Manager.getSupportedContentTypes(null);
	}

	public static Spectator getSpectator() throws MediaException {
		return spectator;
	}

	public static MediaProcessor createMediaProcessor(String contentType) throws MediaException {
		return null;
	}

	public static String[] getSupportedMediaProcessorInputTypes() {
		return new String[0];
	}
}
