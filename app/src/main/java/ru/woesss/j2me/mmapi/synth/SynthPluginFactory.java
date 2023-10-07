/*
 * Copyright 2023 Yury Kharchenko
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

package ru.woesss.j2me.mmapi.synth;

import android.util.Log;

import java.util.List;

import javax.microedition.shell.MicroLoader;
import javax.microedition.util.ContextHolder;

import ru.playsoftware.j2meloader.R;
import ru.woesss.j2me.mmapi.Plugin;
import ru.woesss.j2me.mmapi.synth.eas.LibEAS;
import ru.woesss.j2me.mmapi.synth.tsf.LibTSF;

public class SynthPluginFactory {
	private static final String TAG = SynthPluginFactory.class.getSimpleName();

	public static void loadPlugins(List<Plugin> plugins) {
		String soundBank = MicroLoader.getSoundBank();
		if (soundBank == null) {
			plugins.add(new MIDIDevicePlugin());
			return;
		}
		try {
			plugins.add(new SynthPlugin(new LibEAS(soundBank)));
		} catch (Throwable e) {
			Log.w(TAG, "create EAS plugin failed", e);
			plugins.add(new MIDIDevicePlugin());
		}
		try {
			plugins.add(new SynthPlugin(new LibTSF(soundBank)));
		} catch (Throwable e) {
			Log.w(TAG, "create TSF plugin failed", e);
		}
		if (plugins.isEmpty()) {
			ContextHolder.getActivity().toast(R.string.msg_unsupported_soundbank);
		}
	}
}
