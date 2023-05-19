package javax.microedition.location;

public abstract interface LocationListener {
	public abstract void locationUpdated(LocationProvider locationProvider, Location location);

	public abstract void providerStateChanged(LocationProvider locationProvider, int newState);
}
