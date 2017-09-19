package javax.microedition.media.control;

import javax.microedition.media.Control;

public interface RateControl extends Control {
	public int getMaxRate();

	public int getMinRate();

	public int getRate();

	public int setRate(int millirate);
}
