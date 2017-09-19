package javax.microedition.media.control;

import javax.microedition.media.Control;

public interface StopTimeControl extends Control {
	public static long RESET = Long.MAX_VALUE;

	public long getStopTime();
	
	public void setStopTime(long stopTime);
}
