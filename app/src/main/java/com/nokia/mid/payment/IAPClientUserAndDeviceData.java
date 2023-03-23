package com.nokia.mid.payment;

/**
 * Class for holding User and Device Data related information.
 * This class contains valid data if the status returned in the userAndDeviceDataReceived() callback has value OK.
 * This provides get methods to retrieve each of the  User and Device data related information.
 */
public class IAPClientUserAndDeviceData extends Object {

	/**
	 * Returns the Nokia User Account information in hashed form.
	 */
	public String getAccount() {
		return "dummy_account";
	}

	/**
	 * Returns the device IMEI code in hashed form.
	 */
	public String getImei() {
		return "dummy_imei";
	}

	/**
	 * Returns the subscriber's IMSI code in hashed form.
	 */
	public String getImsi() {
		return "dummy_imsi";
	}

	/**
	 * Returns the home network country according to the SIM card in the device.
	 */
	public String getCountry() {
		return "dummy_country";
	}

	/**
	 * Returns the device language according to the device settings.
	 */
	public String getLanguage() {
		return "dummy_language";
	}

	/**
	 * Returns the name of the device in use.
	 */
	public String getDeviceModel() {
		return "dummy_device_model";
	}
}
