/*----------------------------------------------------------------------------
 *
 * File:
 * eas_mmapi_midp.h
 *
 * Contents and purpose:
 * MIDP specific: implementation of functions that are required
 * by Sun's MIDP reference implementation. This file mainly serves
 * the purpose of making MIDP compilable without Sun's audio
 * implementation.
 *
 * NOTE 1:
 * Do not use this file if you do not use Sun's MIDP implementation
 * or if MIDP's vibrator control is implemented elsewhere.
 *
 * NOTE 2:
 * This is an incomplete implementation:
 * startVibrate takes a "samples" argument, for the duration
 * of the vibration. This is ignored in this implementation.
 *
 * Copyright 2006 Sonic Network Inc.
 *
 *----------------------------------------------------------------------------
 * Revision Control:
 *   $Revision: 560 $
 *   $Date: 2007-02-02 14:34:18 -0800 (Fri, 02 Feb 2007) $
 *----------------------------------------------------------------------------
*/

#include "eas_mmapi_config.h"
#include "eas_mmapi_types.h"
#include "eas_mmapi.h"
/* declared in midp2.0fcs\src\share\native\vibrate.h */
//#include <vibrate.h>
#include "../sonivox/host_src/eas_host.h"

/*----------------------------------------------------------------------------
 * startVibrate()
 *
 * Start the vibrator for the specified time in samples.
 * This function is declared and called by the MIDP implementation.
 *
 * @param samples - the duration of the vibration in samples,
 *                  at 8000Hz sample rate
 * @return 1 on success, 0 on error
 *
 *----------------------------------------------------------------------------
*/
int startVibrate(int samples) {
	EAS_RESULT res;

	res = EAS_HWVibrate(0, EAS_TRUE);

	/* TODO: a way to stop the vibration after the specified amount of time */

	/* return 1 for success, 0 otherwise */
	return (res == EAS_SUCCESS)?1:0;
}

/*----------------------------------------------------------------------------
 * stopVibrate()
 *
 * Stop the vibrator (if it is still active).
 * This function is declared and called by the MIDP implementation.
 *
 *----------------------------------------------------------------------------
*/
void stopVibrate() {
	EAS_HWVibrate(0, EAS_FALSE);
}

