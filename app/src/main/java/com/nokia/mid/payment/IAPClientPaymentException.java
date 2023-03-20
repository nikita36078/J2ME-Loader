package com.nokia.mid.payment;

/**
 * The <code>IAPClientPaymentException</code> is thrown when any unexpected system error occurs.
 */
public class IAPClientPaymentException extends Exception {

	/**
	 * Constructs a <code>IAPClientPaymentException</code> with no detail message.
	 */
	public IAPClientPaymentException() {
	}

	/**
	 * Constructs a <code>IAPClientPaymentException</code> with the specified detail message.
	 *
	 * @param s the detail message
	 */
	public IAPClientPaymentException(String s) {
		super(s);
	}
}
