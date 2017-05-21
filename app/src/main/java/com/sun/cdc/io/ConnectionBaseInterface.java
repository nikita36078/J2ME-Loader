/**
 *  MicroEmulator
 *
 *  @version $Id$
 */
package com.sun.cdc.io;

import javax.microedition.io.Connection;

public interface ConnectionBaseInterface {

	Connection openPrim(String name, int mode, boolean timeouts);

}
