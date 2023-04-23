package javax.microedition.location;

public abstract interface LocationListener {
	public abstract void locationUpdated(LocationProvider paramLocationProvider, Location paramLocation);

	public abstract void providerStateChanged(LocationProvider paramLocationProvider, int paramInt);
}
