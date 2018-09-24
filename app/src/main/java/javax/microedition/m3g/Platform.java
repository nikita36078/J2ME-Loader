/*
 * Copyright (c) 2009 Nokia Corporation and/or its subsidiary(-ies).
 * All rights reserved.
 * This component and the accompanying materials are made available
 * under the terms of "Eclipse Public License v1.0"
 * which accompanies this distribution, and is available
 * at the URL "http://www.eclipse.org/legal/epl-v10.html".
 *
 * Initial Contributors:
 * Nokia Corporation - initial contribution.
 *
 * Contributors:
 *
 * Description:
 *
 */
/**
 * \file
 * \brief Target platform dependent Java module for Symbian.
 */

package javax.microedition.m3g;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * Implements platform-dependent functionality. At the moment, this
 * includes native finalization and some helper methods for
 * synchronizing 2D and 3D rendering.
 */
class Platform {
	/**
	 * eSWT display for ui thread access
	 */
	private static boolean libraryLoaded = false;

	//------------------------------------------------------------------
	// Package private methods
	//------------------------------------------------------------------

	/**
	 * Executes given runnable in UI thread if caller thread is not UI thread
	 */
	static void executeInUIThread(M3gRunnable obj) {
		if (!uiThreadAvailable()) {
			throw new Error("Ui thread not available");
		}

		if (obj != null) {
			// In this case we are in UI thread so just execute directly
			obj.run();
			// Check if any exceptions occured in execution
			// and throw forward in caller thread
			obj.checkAndThrow();
		}
	}

	/**
	 * Check the UI thread / toolkit init status and store display if it is available
	 *
	 * @return true if either lcdui or eswt toolkit is initialized and ui thread is accessible
	 * otherwise false
	 */
	static boolean uiThreadAvailable() {
		{
			// UI thread is available, so load native library if not already loaded
			if (!libraryLoaded) {
				System.loadLibrary("javam3g");
				libraryLoaded = true;
			}
			return true;
		}
	}

	/**
	 * Registers an Object3D in the global handle-to-object map. The
	 * handle of the object must already be set at this point!
	 */
	static final void registerFinalizer(Object3D obj) {
		//heuristicGC();
	}

	/**
	 * Registers a Graphics3D object (not derived from Object3D) for
	 * finalization.
	 */
	static final void registerFinalizer(Graphics3D g3d) {
		//heuristicGC();
	}

	/**
	 * Registers an Interface object for finalization
	 */
	static final void registerFinalizer(Interface m3g) {
	}

	/**
	 * Registers a Loader object for finalization
	 */
	static final void registerFinalizer(Loader loader) {
	}

	/**
	 * Flushes all pending rendering to a Graphics context and blocks
	 * until finished
	 */
	static final void sync(Graphics g) {
		//ToolkitInvoker invoker = ToolkitInvoker.getToolkitInvoker();
		//invoker.toolkitSync(invoker.getToolkit());
	}

	/**
	 * Flushes all pending rendering to an Image object
	 */
	static final void sync(Image img) {
		//ToolkitInvoker invoker = ToolkitInvoker.getToolkitInvoker();
		//invoker.toolkitSync(invoker.getToolkit());
	}

	/**
	 * Finalizes the native peer of an interface
	 */
	static final native void finalizeInterface(long handle);

	/**
	 * Finalizes the native peer of an object
	 * JCF: added this wrapper method so we could pass the toolkit handle to the native method.
	 */
	static final void finalizeObject(long handle) {
		try {
			final long finalHandle = handle;
			executeInUIThread(
					new M3gRunnable() {
						@Override
						void doRun() {
							_finalizeObject(finalHandle);
						}
					});
		} catch (Exception e) {
			// do nothing
		}
	}

	/**
	 * Finalizes the native peer of an object associated with
	 * given Interface instance
	 */
	static final void finalizeObject(long handle, Interface aInterface) {
		try {
			final long finalHandle = handle;
			executeInUIThread(
					new M3gRunnable() {
						@Override
						public void doRun() {
							_finalizeObject(finalHandle);
						}
					});
		} catch (Exception e) {
			// do nothing
		}
	}


	//------------------------------------------------------------------
	// Private methods
	//------------------------------------------------------------------

	/**
	 * Trigger GC if minimum free memory limit has been exceeded in the native side
	 */
	static final void heuristicGC() {
	}

	private static final native void _finalizeObject(long handle);
}

