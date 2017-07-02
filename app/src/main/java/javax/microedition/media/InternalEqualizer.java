package javax.microedition.media;

import javax.microedition.amms.control.audioeffect.EqualizerControl;

public class InternalEqualizer implements EqualizerControl {

	@Override
	public String[] getPresetNames() {
		return new String[0];
	}

	@Override
	public void setPreset(String preset) {

	}

	@Override
	public String getPreset() {
		return null;
	}

	@Override
	public void setEnabled(boolean enable) {

	}

	@Override
	public boolean isEnabled() {
		return false;
	}

	@Override
	public int getNumberOfBands() {
		return 0;
	}

	@Override
	public int getBand(int frequency) {
		return 0;
	}

	@Override
	public int getCenterFreq(int band) {
		return 0;
	}

	@Override
	public int getMinBandLevel() {
		return 0;
	}

	@Override
	public int getMaxBandLevel() {
		return 0;
	}

	@Override
	public void setBandLevel(int level, int band) {

	}

	@Override
	public int getBandLevel(int band) {
		return 0;
	}
}