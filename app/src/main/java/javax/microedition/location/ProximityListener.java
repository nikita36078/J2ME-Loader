package javax.microedition.location;

public abstract interface ProximityListener {
	public abstract void proximityEvent(Coordinates coordinates, Location location);

	public abstract void monitoringStateChanged(boolean isMonitoringActive);
}
