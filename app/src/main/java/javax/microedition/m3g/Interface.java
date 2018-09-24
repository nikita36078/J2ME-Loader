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

package javax.microedition.m3g;

import java.lang.ref.WeakReference;
import java.util.Hashtable;

/**
 * M3G interface object. An interface is automatically created for
 * each MIDlet using the 3D API to keep track of Java-side object
 * lifetimes etc.
 */
class Interface {
	//------------------------------------------------------------------
	// Static data
	//------------------------------------------------------------------

	// Common class enumeration for Java and native code

	private static final int ANIMATION_CONTROLLER = 0x01;
	private static final int ANIMATION_TRACK = 0x02;
	private static final int APPEARANCE = 0x03;
	private static final int BACKGROUND = 0x04;
	private static final int CAMERA = 0x05;
	private static final int COMPOSITING_MODE = 0x06;
	private static final int FOG = 0x07;
	private static final int GROUP = 0x08;
	private static final int IMAGE_2D = 0x09;
	private static final int INDEX_BUFFER = 0x0A;
	private static final int KEYFRAME_SEQUENCE = 0x0B;
	private static final int LIGHT = 0x0C;
	private static final int LOADER = 0x0D;
	private static final int MATERIAL = 0x0E;
	private static final int MESH = 0x0F;
	private static final int MORPHING_MESH = 0x10;
	private static final int POLYGON_MODE = 0x11;
	private static final int RENDER_CONTEXT = 0x12;
	private static final int SKINNED_MESH = 0x13;
	private static final int SPRITE_3D = 0x14;
	private static final int TEXTURE_2D = 0x15;
	private static final int VERTEX_ARRAY = 0x16;
	private static final int VERTEX_BUFFER = 0x17;
	private static final int WORLD = 0x18;

	// Once created, the interface singleton currently remains in
	// memory until VM exit.  By using a WeakReference here, with hard
	// references stored in each object, it could be GC'd when no more
	// objects exist, but that probably isn't worth the extra memory
	// overhead.

	//private static Hashtable s_instances = new Hashtable();
	private static Interface instance = null;

	//------------------------------------------------------------------
	// Instance data
	//------------------------------------------------------------------

	/**
	 * Handle of the native interface object.
	 */
	private long handle;

	/**
	 * Global handle-to-Object3D map used to both find the Java
	 * counterparts of objects returned from the native methods, and
	 * keep certain objects from being garbage collected.
	 */
	private final Hashtable liveObjects = new Hashtable();

	/**
	 * Flag for shutdown signal
	 */
	private boolean iShutdown = false;

	/**
	 * Flag for native peer init state
	 */
	private boolean iNativeInitialized = false;


	//#ifdef RD_JAVA_OMJ
	@Override
	protected void finalize() {
		doFinalize();
	}
//#endif // RD_JAVA_OMJ

	//------------------------------------------------------------------
	// Constructors
	//------------------------------------------------------------------

	private Interface() {

		// Contruct native peer
		initNativePeer();

//#ifdef RD_JAVA_OMJ
//#else // RD_JAVA_OMJ
//        Platform.registerFinalizer(this);
//#endif // RD_JAVA_OMJ
	}

	//------------------------------------------------------------------
	// Package methods
	//------------------------------------------------------------------

	/**
	 * Returns the M3G interface instance for the current MIDlet.
	 */
	static final Interface getInstance() {
		if (instance == null) {
			instance = new Interface();
		}
		return instance;
	}

	/**
	 * Returns the native handle of the current Interface instance.
	 */
	static final long getHandle() {
		getInstance().integrityCheck();
		return getInstance().handle;
	}

	/**
	 * Registers an Object3D with this interface. The object is added
	 * to the global handle-to-object map, and the native finalization
	 * callback is set up. The handle of the object must already be
	 * set at this point!
	 */
	static final void register(Object3D obj) {
		getInstance().liveObjects.put(new Long(obj.handle),
				new WeakReference(obj));
	}

	static final void register(Loader obj) {
		getInstance().liveObjects.put(new Long(obj.handle),
				new WeakReference(obj));
	}

	/**
	 * Finds an Object3D in the global handle-to-object map. Also
	 * removes dead objects (that is, null references) from the map
	 * upon encountering them.
	 */
	static final Object3D findObject(long handle) {
		Interface self = getInstance();
		Long iHandle = new Long(handle);
		Object ref = self.liveObjects.get(iHandle);

		if (ref != null) {
			Object3D obj = (Object3D) ((WeakReference) ref).get();
			if (obj == null) {
				self.liveObjects.remove(iHandle);
			}
			return obj;
		} else {
			return null;
		}
	}

	/**
	 * Returns the Java object representing a native object, or
	 * creates a new proxy/peer if one doesn't exist yet.
	 */
	static final Object3D getObjectInstance(long handle) {

		// A zero handle equals null

		if (handle == 0) {
			return null;
		}

		// Then try to find an existing Java representative for the
		// object

		Object3D obj = findObject(handle);
		if (obj != null) {
			return obj;
		}

		// Not found, create a new Java object. Note that only
		// non-abstract classes can possibly be returned.

		switch (_getClassID(handle)) {
			case ANIMATION_CONTROLLER:
				return new AnimationController(handle);
			case ANIMATION_TRACK:
				return new AnimationTrack(handle);
			case APPEARANCE:
				return new Appearance(handle);
			case BACKGROUND:
				return new Background(handle);
			case CAMERA:
				return new Camera(handle);
			case COMPOSITING_MODE:
				return new CompositingMode(handle);
			case FOG:
				return new Fog(handle);
			case GROUP:
				return new Group(handle);
			case IMAGE_2D:
				return new Image2D(handle);
			case INDEX_BUFFER:
				return new TriangleStripArray(handle);
			case KEYFRAME_SEQUENCE:
				return new KeyframeSequence(handle);
			case LIGHT:
				return new Light(handle);
			//case LOADER:
			case MATERIAL:
				return new Material(handle);
			case MESH:
				return new Mesh(handle);
			case MORPHING_MESH:
				return new MorphingMesh(handle);
			case POLYGON_MODE:
				return new PolygonMode(handle);
			//case RENDER_CONTEXT:
			case SKINNED_MESH:
				return new SkinnedMesh(handle);
			case SPRITE_3D:
				return new Sprite3D(handle);
			case TEXTURE_2D:
				return new Texture2D(handle);
			case VERTEX_ARRAY:
				return new VertexArray(handle);
			case VERTEX_BUFFER:
				return new VertexBuffer(handle);
			case WORLD:
				return new World(handle);
			default:
				throw new Error();
		}
	}

	/**
	 * Forces removal of an object from the handle-to-object map.
	 */
	static final void deregister(Object3D obj, Interface self) {
		self.liveObjects.remove(new Long(obj.handle));
		if (self.liveObjects.isEmpty() && self.iShutdown) {
			self.registeredFinalize();
		}
	}

	/**
	 * Forces removal of an object from the handle-to-object map.
	 */
	static final void deregister(Loader obj, Interface self) {
		self.liveObjects.remove(new Long(obj.handle));
		if (self.liveObjects.isEmpty() && self.iShutdown) {
			self.registeredFinalize();
		}
	}

	/**
	 * Sets shutdown indication flag. Actual native
	 * cleanup occurs when liveObjects count is zero
	 */
	void signalShutdown() {
		iShutdown = true;
	}

	/**
	 * Gets the state of this interface
	 *
	 * @return true if interface is fully constructed, otherwise false
	 */
	boolean isFullyInitialized() {
		return iNativeInitialized;
	}

	//------------------------------------------------------------------
	// Private methods
	//------------------------------------------------------------------

	/**
	 * Checks the status of the native interface
	 */
	private void integrityCheck() {
		if (!iNativeInitialized) {
			// If native interface cannot be initialized we cannot recover from it
			if (!initNativePeer()) {
				throw new Error("UI thread not available");
			}
		}
	}

	/**
	 * Initializes native peer
	 *
	 * @return true if native interface was succesfully inialized otherwise false
	 */
	private boolean initNativePeer() {
		if (iNativeInitialized) {
			return true;
		}
		if (Platform.uiThreadAvailable()) {
			Platform.executeInUIThread(
					new M3gRunnable() {
						@Override
						public void doRun() {
							handle = _ctor();
						}
					});
			iNativeInitialized = true;
			return true;
		} else {
			return false;
		}
	}


	//#ifdef RD_JAVA_OMJ
	private void doFinalize() {
		registeredFinalize();
	}
//#endif // RD_JAVA_OMJ

	// Native finalization hook, for Symbian only
	final private void registeredFinalize() {
		if (Interface.instance != null) {
			Platform.executeInUIThread(
					new M3gRunnable() {
						@Override
						public void doRun() {
							Platform.finalizeInterface(handle);
						}
					});
			Interface.instance = null;
		}
	}

	// Native constructor
	private static native long _ctor();

	// Native class ID resolver
	private static native int _getClassID(long hObject);
}
