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

import javax.microedition.media.Control;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;

public class GlobalManager {
	private static Spectator spectator = new Spectator();

	public static Control[] getControls() {
		return null;
	}

	public static Control getControl(String aControlType) {
		return null;
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

	public static MediaProcessor createMediaProcessor(String aContentType) throws MediaException {
		return null;
	}

	public static String[] getSupportedMediaProcessorInputTypes() {
		return new String[0];
	}
}
