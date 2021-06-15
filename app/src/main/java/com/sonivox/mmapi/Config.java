package com.sonivox.mmapi;

/**
 * Configuration for SONiVOX's MMAPI implementation.
 */
class Config {

	/*
	 * Native configuration (need to synchronize with C flags)
	 */

	/**
	 * If true, then a native rendering thread is created within the native
	 * implementation of EAS.nInit() which will from then on call EAS_Render()
	 * regularly. If native rendering is used, EAS.nRender() need never be
	 * called.
	 * <p>
	 * If false, the EAS class will instantiate and run a Java thread which
	 * regularly calls EAS.nRender().
	 */
	public static final boolean NATIVE_RENDERING_THREAD = false;

	/*
	 * supported content types
	 */

	public static final boolean HAS_WAVE_PLAYBACK = true;

	public static final boolean HAS_MIDI_PLAYBACK = true;

	public static final boolean HAS_XMF_PLAYBACK = true;

	// .jts files
	public static final boolean HAS_TONE_PLAYBACK = true;

	// NOTE: magic content types (device:...) are selected by respective
	// controls

	/*
	 * supported protocols
	 */

	public static final boolean HAS_FILE = true;

	public static final boolean HAS_HTTP = true;

	/* capture://audio, requires WAVE_PLAYBACK, implies RecordControl for this player */
	public static final boolean HAS_WAVE_CAPTURE = true;

	/* supported controls */
	
	/** device://tone */
	public static final boolean HAS_DEVICE_TONECONTROL = true;

	/** device://midi */
	public static final boolean HAS_DEVICE_MIDICONTROL = true;

	/** for media file players */
	public static final boolean HAS_VOLUMECONTROL = true;

	/** for media file players */
	public static final boolean HAS_STOPTIMECONTROL = true;

	/** for MIDI and tone file players */
	public static final boolean HAS_MIDITONE_RATECONTROL = true;

	/* for MIDI and tone file players. Currently not implemented by EAS */
	public static final boolean HAS_MIDITONE_TEMPOCONTROL = false;

	/** for MIDI and tone file players */
	public static final boolean HAS_MIDITONE_PITCHCONTROL = true;

	/** for MIDI players */
	public static final boolean HAS_MIDI_MIDICONTROL = true;

	/** for media file players */
	public static final boolean HAS_METADATACONTROL = true;

	/** for wave file players */
	public static final boolean HAS_WAVE_RATECONTROL = true;

	/** for wave file players, and for wave capture */
	public static final boolean HAS_WAVE_RECORDCONTROL = true;
	
	/** if wave can report meta data */
	public static final boolean HAS_WAVE_METADATACONTROL = false;

	
	/*
	 * Hard-coded Settings
	 */

	/**
	 * The size, in bytes, of the streaming buffer. This is the number of bytes
	 * that are written at once to native.
	 */
	public final static int STREAM_BUFFER_SIZE = 2048;
	
	/**
	 * This is the number of STREAM BUFFERS that are written to native 
	 * at realize() or prefetch() for streaming media. This should be
	 * synched with MMAPI_STREAM_CIRCULAR_BUFFER_SIZE in eas_mmapi_config.h. 
	 */
	public final static int STREAM_BUFFER_PREFETCH_COUNT = 20;
	

	/**
	 * The MIDI bank used for tone generation (Manager.playTone).
	 * Set to -1 in order to not change bank.
	 */
	public final static int TONE_MIDI_BANK = -1;
	
	
	/**
	 * The MIDI program used for tone generation (Manager.playTone).
	 */
	public final static int TONE_MIDI_PROGRAM = 80; // program 80: square lead
	
	
	/**
	 * The minimum rate for RateControl in millirate.
	 * Corresponds to EAS' minimum MIN_PLAYBACK_RATE defined in eas.h: 
	 * (EAS_U32)(1L << 27) -> 0.5 
	 */
	public static final int RATE_MIN = 50000;

	/**
	 * The maximum rate for RateControl in millirate.
	 * Corresponds to EAS' maximum MAX_PLAYBACK_RATE defined in eas.h: 
	 * (EAS_U32)(1L << 29) -> 2.0 
	 */
	public static final int RATE_MAX = 200000;

	/**
	 * The minimum pitch transposition for PitchControl in milli-semitones.
	 * It seems that EAS only supports transposing up. 
	 */
	public static final int PITCH_MIN = -12000;

	/**
	 * The maximum pitch transposition for PitchControl in milli-semitones.
	 * Corresponds to EAS' maximum MAX_TRANSPOSE defined in eas.h. 
	 */
	public static final int PITCH_MAX = 12000;


}
