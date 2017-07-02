package javax.microedition.media;

import android.media.audiofx.Equalizer;

import javax.microedition.amms.control.audioeffect.EqualizerControl;

public class InternalEqualizer implements EqualizerControl {
	protected Equalizer equalizer;
	protected String[] presets;

	public InternalEqualizer(int audioSession) {
		equalizer = new Equalizer(0, audioSession);
	}

	public String[] getPresetNames() {
		if (presets == null) {
			presets = new String[equalizer.getNumberOfPresets()];

			for (short i = 0; i < presets.length; i++) {
				presets[i] = equalizer.getPresetName(i);
			}
		}

		return presets;
	}

	public void setPreset(String preset) {
		if (presets == null) {
			getPresetNames();
		}

		for (short i = 0; i < presets.length; i++) {
			if (presets[i].equals(preset)) {
				equalizer.usePreset(i);
				break;
			}
		}
	}

	public String getPreset() {
		if (presets == null) {
			getPresetNames();
		}

		try {
			return presets[equalizer.getCurrentPreset()];
		} catch (Exception e) {
			return null;
		}
	}

	public void setEnabled(boolean enable) {
		equalizer.setEnabled(enable);
	}

	public boolean isEnabled() {
		return equalizer.getEnabled();
	}

	public int getNumberOfBands() {
		return equalizer.getNumberOfBands();
	}

	public int getBand(int frequency) {
		return equalizer.getBand(frequency);
	}

	public int getCenterFreq(int band) {
		return equalizer.getCenterFreq((short) band);
	}

	public int getMinBandLevel() {
		return equalizer.getBandLevelRange()[0];
	}

	public int getMaxBandLevel() {
		return equalizer.getBandLevelRange()[1];
	}

	public void setBandLevel(int level, int band) {
		equalizer.setBandLevel((short) band, (short) level);
	}

	public int getBandLevel(int band) {
		return equalizer.getBandLevel((short) band);
	}
}