package javax.bluetooth;

import android.bluetooth.BluetoothSocket;

class J2MEServiceRecord implements ServiceRecord {
	private RemoteDevice dev;
	private UUID uuid;
	private boolean skipAfterWrite;

	public J2MEServiceRecord(RemoteDevice dev, UUID uuid, boolean skipAfterWrite) {
		this.dev = dev;
		this.uuid = uuid;
		this.skipAfterWrite = skipAfterWrite;
	}

	public RemoteDevice getHostDevice() {
		return dev;
	}

	public String getConnectionURL(int requiredSecurity, boolean mustBeMaster) {
		StringBuilder sb = new StringBuilder("btspp://");
		if (dev != null)
			sb.append(dev.getBluetoothAddress());
		else
			sb.append("localhost");
		sb.append(":");
		sb.append(uuid.toString());

		switch (requiredSecurity) {
			case NOAUTHENTICATE_NOENCRYPT:
				sb.append(";authenticate=false;encrypt=false");
				break;
			case AUTHENTICATE_NOENCRYPT:
				sb.append(";authenticate=true;encrypt=false");
				break;
			case AUTHENTICATE_ENCRYPT:
				sb.append(";authenticate=true;encrypt=true");
				break;
			default:
				throw new IllegalArgumentException();
		}

		if (mustBeMaster)
			sb.append(";master=true");
		else
			sb.append(";master=false");

		if (skipAfterWrite)
			sb.append(";skipAfterWrite=true");

		return sb.toString();
	}

	public boolean setAttributeValue(int attrID, DataElement attrValue) {
		if (attrID == 0)
			throw new IllegalArgumentException("attrID is ServiceRecordHandle (0x0000)");
		if (attrValue == null)
			return false;
		// Android does not support this, return success
		return true;
	}

	public DataElement getAttributeValue(int attrID) {
		// Fake service name
		if (attrID == 0x100)
			return new DataElement(DataElement.STRING, "SOMENAME");
		return null;
	}

	public int[] getAttributeIDs() {
		return new int[0];
	}

	public void setDeviceServiceClasses(int classes) {
		return;
	}

	public boolean populateRecord(int[] attrIDs) {
		if (attrIDs == null)
			throw new NullPointerException();
		// Android does not support this, return success
		return true;
	}

}
