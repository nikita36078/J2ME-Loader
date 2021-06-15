/*
 * (c) 2006 by SONiVOX
 *
 * This implementation of Manager delagates all call to
 * the implementation class com.sonivox.mmapi.ManagerImpl
 */

package javax.microedition.media;

import java.io.IOException;
import java.io.InputStream;
import com.sonivox.mmapi.ManagerEAS;

import javax.microedition.media.protocol.*;

/**
 * <code>Manager</code> is the access point for obtaining system dependent
 * resources such as <code>Players</code> for multimedia processing.
 * <p>
 *
 * A <a href="Player.html"<code>Player</code></a> is an object used to
 * control and render media that is specific to the <a
 * href="#content-type">content type</a> of the data.
 * <p>
 * <code>Manager</code> provides access to an implementation specific
 * mechanism for constructing <code>Players</code>.
 * <p>
 * For convenience, <code>Manager</code> also provides a simplified method to
 * generate simple tones.
 *
 * <h2>Simple Tone Generation</h2>
 *
 * <blockquote> The <a href="#playTone(int, int, int)"> <code>playTone</code></a>
 * function is defined to generate tones. Given the note and duration, the
 * function will produce the specified tone. </blockquote>
 *
 * <h2>Creating Players</h2>
 * <blockquote>
 *
 * <code>Manager</code> provides three methods to create a <code>Player</code>
 * for playing back media:
 * <ul>
 * <li> Create from a media locator.
 * <li> Create from a <code>DataSource</code>.
 * <li> Create from an <code>InputStream</code>.
 * </ul>
 * The <code>Player</code> returned can be used to control the presentation of
 * the media.
 * <p>
 *
 * The simplest way to create a <code>Player</code> is from a <a
 * href="#media-locator">locator in the URI syntax</a>. Given a locator, <a
 * href="#createPlayer(java.lang.String)"> <code>createPlayer</code></a> will
 * create a <code>Player</code> suitable to handle the media identified by the
 * locator.
 * <p>
 * Users can also implement a custom <code>DataSource</code> to handle an
 * application-defined protocol. The custom <code>DataSource</code> can be
 * used to create a <code>Player</code> by using the <a
 * href="#createPlayer(javax.microedition.media.protocol.DataSource)">
 * <code>createPlayer</code></a> method.
 * <p>
 * A third version of <a href="#createPlayer(java.io.InputStream,
 * java.lang.String)"> <code>createPlayer</code></a> creates a
 * <code>Player</code> from an <code>InputStream</code>. This can be used
 * to interface with other Java API's which use <code>InputStreams</code> such
 * as the java.io package. It should be noted that <code>InputStream</code>
 * does not provide the necessary random seeking functionality. So a
 * <code>Player</code> created from an <code>InputStream</code> may not
 * support random seeking (ala <code>Player.setMediaTime</code>).
 * </blockquote>
 *
 * <h2>System Time Base</h2>
 *
 * <blockquote> All <code>Players</code> need a <code>TimeBase</code>. Many
 * use a system-wide <code>TimeBase</code>, often based on a time-of-day
 * clock. <code>Manager</code> provides access to the system
 * <code>TimeBase</code> through <a href="#getSystemTimeBase()">
 * <code>getSystemTimeBase</code></a>. </blockquote>
 *
 * <a name="content-type"></a>
 * <h2>Content Types</h2>
 * <blockquote> Content types identify the type of media data. They are defined
 * to be the registered MIME types (<a href=
 * "http://www.iana.org/assignments/media-types/">
 * http://www.iana.org/assignments/media-types/</a>); plus some user-defined
 * types that generally follow the MIME syntax (<a
 * href="ftp://ftp.isi.edu/in-notes/rfc2045.txt">RFC 2045</a>, <a
 * href="ftp://ftp.isi.edu/in-notes/rfc2046.txt">RFC 2046</a>).
 * <p>
 * For example, here are a few common content types:
 * <ol>
 * <li>Wave audio files: <code>audio/x-wav</code>
 * <li>AU audio files: <code>audio/basic</code>
 * <li>MP3 audio files: <code>audio/mpeg</code>
 * <li>MIDI files: <code>audio/midi</code>
 * <li>Tone sequences: <code>audio/x-tone-seq</code>
 * <li>MPEG video files: <code>video/mpeg</code>
 * </ol>
 * </blockquote>
 *
 * <a name="delivery-protocol"></a>
 * <h2>Data Delivery Protocol</h2>
 * <blockquote> A data delivery protocol specifies how media data is delivered
 * to the media processing systems. Some common protocols are: local file, disk
 * I/O, HTTP, RTP streaming, live media capture etc.
 * <p>
 * <a href="#media-locator">Media locators</a> are used to identify the
 * delivery protocol (as well as the identifier/name of the media).
 * </blockquote>
 *
 * <a name="media-locator"></a>
 * <h2>Media Locator</h2>
 * <blockquote> <a name="media-protocol"></a> Media locators are specified in
 * <a href="http://www.ietf.org/rfc/rfc2396.txt">URI syntax</a> which is
 * defined in the form:
 * <p>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;scheme&gt;:&lt;scheme-specific-part&gt;
 * <p>
 * The "scheme" part of the locator string identifies the name of the protocol
 * being used to deliver the data.
 * <p>
 * Some media specific locator syntax are defined below:
 *
 * <a name="live-capture"></a>
 * <h3>1. Locators for Live-media Capture</h3>
 * The locators for capturing live media are defined by the following syntax in
 * <a href="http://www.ietf.org/rfc/rfc2234">Augmented BNF</a> notations:
 * <p>
 *
 * <pre>
 *      &quot;capture://&quot; device [ &quot;?&quot; media_encodings ]
 * </pre>
 *
 * &nbsp;&nbsp;To identify the type or the specific name of the device:
 * <p>
 *
 * <pre>
 *      device       = &quot;audio&quot; / &quot;video&quot; / &quot;audio_video&quot; / dev_name
 *      dev_name     = alphanumeric
 *      alphanumeric = 1*( ALPHA / DIGIT )
 * </pre>
 *
 * The syntax for specifying the media encodings are defined in <a
 * href="Manager.html#media_encodings">Media Encoding Strings</a>.<br>
 * <br>
 * &nbsp;&nbsp;&nbsp;&nbsp;Examples: <br>
 *
 * <pre>
 *      capture://audio  (default audio)
 *      capture://audio?encoding=pcm   (default audio in PCM format)
 *      capture://devmic0?encoding=pcm&amp;rate=11025&amp;bits=16&amp;channels=1
 *              (audio from a specific device--devmic0)
 * <br>
 *      capture://video  (default video)
 *      capture://video?encoding=gray8&amp;width=160&amp;height=120
 *      capture://devcam0?encoding=rgb888&amp;width=160&amp;height=120&amp;fps=7
 * <br>
 *      capture://mydev?myattr=123   (custom device with custom param)
 * </pre>
 *
 * <h3>2. Locators for RTP streaming</h3>
 * <a href="http://www.ietf.org/rfc/rfc1889.txt">RTP</a> is a public standard
 * for streaming media. The locator syntax for specifying RTP sessions is:
 *
 * <pre>
 *      &quot;rtp://&quot; address [ &quot;:&quot; port ] [ &quot;/&quot; type ]
 * </pre>
 *
 * &nbsp;&nbsp;&nbsp;&nbsp;where:
 *
 * <pre>
 *      address and port defines the RTP session.  The
 *      address and port usage is similar to the host and port
 *      usage as defined in the &lt;a href=&quot;http://www.ietf.org/rfc/rfc2396.txt&quot;&gt;URI syntax&lt;/a&gt;.
 * <br>
 * type = &quot;audio&quot; / &quot;video&quot; / &quot;text&quot;
 * </pre>
 *
 * <br>
 * &nbsp;&nbsp;&nbsp;&nbsp;Example: <br>
 *
 * <pre>
 *      rtp://224.1.2.3:12344/audio
 * </pre>
 *
 * <h3>3. Locators for Radio Tuner</h3>
 * To create a <code>Player</code> to tune into a radio program, the following
 * locator syntax is used:
 * <p>
 *
 * <pre>
 *      &quot;capture://radio&quot; [ &quot;?&quot; tuner_params ]
 * </pre>
 *
 * &nbsp;&nbsp;&nbsp;&nbsp;where:
 *
 * <pre>
 *      tuner_params = tuner_param *( &quot;&amp;&quot; tuner_param )
 *      tuner_param  = &quot;f=&quot; freq /
 *                     &quot;mod=&quot; modulation /
 *                     &quot;st=&quot; stereo_mode /
 *                     &quot;id=&quot; program_id /
 *                     &quot;preset=&quot; preset
 *      freq         = megahertz /
 *                     kilohertz /
 *                     hertz
 *      megahertz    = pos_integer &quot;M&quot; /
 *                     pos_integer &quot;.&quot; pos_integer &quot;M&quot;
 *      kilohertz    = pos_integer &quot;k&quot; /
 *                     pos_integer &quot;.&quot; pos_integer &quot;k&quot;
 *      hertz        = pos_integer
 *      modulation   = &quot;fm&quot; / &quot;am&quot;
 *      stereo_mode  = &quot;mono&quot; / &quot;stereo&quot; / &quot;auto&quot;
 *      program_id   = alpanumeric ; identifies an FM channel by its
 *                                   program service name (PS) delivered
 *                                   via Radio Data System (RDS)**.
 *      preset       = pos_integer ; predefined tuning number
 * </pre> ** The RDS specification is available from <a
 * href="http://bsonline.techindex.co.uk">http://bsonline.techindex.co.uk</a>,
 * id BSEN 50067:1998.<br>
 * <br>
 * &nbsp;&nbsp;&nbsp;&nbsp;Examples: <br>
 *
 * <pre>
 *      capture://radio?f=91.9M&amp;st=auto
 *                (91.9 MHz with automatic stereo setting)
 *      capture://radio?f=558k&amp;mod=am
 *                (558 kHz with amplitude modulation)
 *      capture://radio?id=yleq
 *                (FM channel that has &quot;yleq&quot; as its program service name
 *                 delivered via Radia Data System)
 * </pre>
 *
 * </blockquote>
 * <p>
 *
 * <a name="media_encodings"></a>
 * <h2>Media Encoding Strings</h2>
 * <blockquote> There are a few places where media encodings are specified as
 * strings, e.g. in the capture media locator. Sections A to E define the
 * encoding syntax. <a href="#encodings_rules">Section F</a> defines the rules
 * for how they should be handled.
 * <p>
 *
 * &nbsp;&nbsp;A. Describing media encodings:
 * <p>
 *
 * <pre>
 * media_encodings = audio_encodings / video_encodings / mixed_encodings
 * 		/ custom_encodings
 * </pre>
 *
 * <a name="audio_encodings"></a> &nbsp;&nbsp;B. Describing the audio
 * encodings:
 * <p>
 *
 * <pre>
 *      audio_encodings = audio_enc_param *( &quot;&amp;&quot; audio_param )
 *      audio_enc_param = &quot;encoding=&quot; audio_enc
 *      audio_enc       = &quot;pcm&quot; / &quot;ulaw&quot; / &quot;gsm&quot; / content_type
 *      audio_param     = &quot;rate=&quot; rate /
 *                        &quot;bits=&quot; bits /
 *                        &quot;channels=&quot; channels /
 *                        &quot;endian=&quot; endian /
 *                        &quot;signed=&quot; signed /
 *                        &quot;type=&quot; audio_type
 *      rate            = &quot;96000&quot; / &quot;48000&quot; / &quot;44100&quot; /
 *                        &quot;22050&quot; / &quot;16000&quot; / &quot;11025&quot; /
 *                        &quot;8000&quot; / other_rate
 *      other_rate      = pos_integer
 *      bits            = &quot;8&quot; / &quot;16&quot; / &quot;24&quot; / other_bits
 *      other_bits      = pos_integer
 *      channels        = pos_integer
 *      endian          = &quot;little&quot; / &quot;big&quot;
 *      signed          = &quot;signed&quot; / &quot;unsigned&quot;
 *      audio_type      = bitrate_variable / other_type
 *      other_type      = alphanumeric
 *      pos_integer     = 1*DIGIT
 *
 *    and
 *      &lt;a href=&quot;#content-type&quot;&gt;content type&lt;/a&gt; is given in the MIME syntax.
 * </pre>
 *
 * <br>
 * &nbsp;&nbsp;&nbsp;&nbsp;Example: <br>
 *
 * <pre>
 *      encoding=pcm&amp;rate=11025&amp;bits=16&amp;channels=1
 * </pre>
 *
 * <a name="video_encodings"></a> &nbsp;&nbsp;C. Describing the video or image
 * encodings:
 * <p>
 *
 * <pre>
 *      video_encodings   = video_enc_param *( &quot;&amp;&quot; video_param )
 *      video_enc_param   = &quot;encoding=&quot; video_enc
 *      video_enc         = &quot;gray8&quot; / &quot;rgb888&quot; / &quot;bgr888&quot; /
 *                          &quot;rgb565&quot; / &quot;rgb555&quot; / &quot;yuv444&quot; /
 *                          &quot;yuv422&quot; / &quot;yuv420&quot; / &quot;jpeg&quot; / &quot;png&quot; /
 *                          content_type
 *      video_param       = &quot;width=&quot; width /
 *                          &quot;height=&quot; height /
 *                          &quot;fps=&quot; fps /
 *                          &quot;colors=&quot; colors /
 *                          &quot;progressive=&quot; progressive /
 *                          &quot;interlaced=&quot; interlaced /
 *                          &quot;type=&quot; video_type
 *      width             = pos_integer
 *      height            = pos_integer
 *      fps               = pos_number
 *      quality           = pos_integer
 *      colors            = &quot;p&quot; colors_in_palette /
 *                        = &quot;rgb&quot; r_bits g_bits b_bits /
 *                        = &quot;gray&quot; gray_bits
 *      colors_in_palette = pos_integer
 *      r_bits            = pos_integer
 *      g_bits            = pos_integer
 *      b_bits            = pos_integer
 *      gray_bits         = pos_integer
 *      progressive       = boolean
 *      video_type        = jfif / exif / other_type
 *      other_type        = alphanumeric
 *      interlaced        = boolean
 *      pos_number        = 1*DIGIT [ &quot;.&quot; 1*DIGIT ]
 *      boolean           = &quot;true&quot; / &quot;false&quot;
 *
 *    and
 *      &lt;a href=&quot;#content-type&quot;&gt;content type&lt;/a&gt; is given in the MIME syntax.
 * </pre>
 *
 * <br>
 * &nbsp;&nbsp;&nbsp;&nbsp;Examples: <br>
 *
 * <pre>
 *      encoding=gray8&amp;width=160&amp;height=120
 *      encoding=jpeg&amp;quality=80&amp;progressive=true&amp;type=jfif
 *          (progressive JPEG with quality 80 in jfif format)
 *      encoding=jpeg&amp;type=exif
 *          (JPEG in exif format)
 *      encoding=png&amp;colors=rgb888
 *          (24 bits/pixel PNG)
 *      encoding=rgb888
 *          (raw 24-bit rgb image)
 *      encoding=rgb&amp;colors=rgb888
 *          (raw 24-bit rgb image)
 * </pre>
 *
 * <a name="mixed_params"></a> &nbsp;&nbsp;D. Describing mixed audio and video
 * encodings:
 * <p>
 *
 * <pre>
 *      mixed_encodings = audio_encodings &quot;&amp;&quot; video_encodings
 * </pre>
 *
 * <br>
 * &nbsp;&nbsp;&nbsp;&nbsp;Example: <br>
 *
 * <pre>
 *      encoding=pcm&amp;encoding=gray8&amp;width=160&amp;height=160
 * </pre>
 *
 * &nbsp;&nbsp;E. Describing custom media encodings:
 * <p>
 *
 * <pre>
 *      custom_encodings = custom_enc_param *( &quot;&amp;&quot; param )
 *      custom_enc_param = &quot;encoding=&quot; value
 *      param            = key &quot;=&quot; value
 *      key              = alphanumeric
 *      value            = alphanumeric
 * </pre>
 *
 * <a name="encodings_rules"></a> &nbsp;&nbsp;F. Rules for handling the
 * encodings strings:
 * <p>
 * <ul>
 * <li> If a given parameter is a custom parameter and is not recognizable by
 * the implementation, the parameter is treated as an illegal parameter and the
 * method must throw an appropriate Exception to denote that.
 * <li> If the value for a given parameter is incorrect because it is
 * syntactically wrong or illegal (e.g. out of range), the method must throw an
 * appropriate Exception to denote that.
 * </ul>
 *
 * @see DataSource
 * @see Player
 * @see TimeBase
 */

public final class Manager {

	/**
	 * The locator to create a tone <code>Player</code> to play back tone
	 * sequences. For example,
	 *
	 * <pre>
	 * try {
	 * 	Player p = Manager.createPlayer(Manager.TONE_DEVICE_LOCATOR);
	 * 	p.realize();
	 * 	ToneControl tc = (ToneControl) p.getControl(&quot;ToneControl&quot;);
	 * 	tc.setSequence(mySequence);
	 * 	p.start();
	 * } catch (IOException ioe) {
	 * } catch (MediaException me) {
	 * }
	 * </pre>
	 *
	 * If a tone sequence is not set on the tone <code>Player</code> via its
	 * <code>ToneControl</code>, the <code>Player</code> does not carry any
	 * sequence. <code>getDuration</code> returns 0 for this
	 * <code>Player</code>.
	 * <p>
	 * The content type of the <code>Player</code> created from this locator
	 * is <code>audio/x-tone-seq</code>.
	 * <p>
	 * A <code>Player</code> for this locator may not be supported for all
	 * implementations.
	 * <p>
	 * Value "device://tone" is assigned to <code>TONE_DEVICE_LOCATOR</code>.
	 */
	final public static String TONE_DEVICE_LOCATOR = "device://tone";

	/**
	 * The locator to create a MIDI <code>Player</code> which gives access to
	 * the MIDI device by making
	 * {@link javax.microedition.media.control.MIDIControl MIDIControl}
	 * available. For example,
	 *
	 * <pre>
	 * try {
	 * 	Player p = Manager.createPlayer(Manager.MIDI_DEVICE_LOCATOR);
	 * 	p.prefetch(); // opens the MIDI device
	 * 	MIDIControl m = (MIDIControl) p.getControl(&quot;MIDIControl&quot;);
	 * } catch (IOException ioe) {
	 * } catch (MediaException me) {
	 * }
	 * </pre>
	 *
	 * The MIDI <code>Player</code> returned does not carry any media data.
	 * <code>getDuration</code> returns 0 for this <code>Player</code>.
	 * <p>
	 * The content type of the <code>Player</code> created from this locator
	 * is <code>audio/midi</code>.
	 * <p>
	 * A <code>Player</code> for this locator may not be supported for all
	 * implementations.
	 * <p>
	 * Value "device://midi" is assigned to <code>MIDI_DEVICE_LOCATOR</code>.
	 */
	final public static String MIDI_DEVICE_LOCATOR = "device://midi";

	/**
	 * prevent instanciation
	 */
	private Manager() {
	}

	/**
	 * Return the list of supported content types for the given protocol.
	 * <p>
	 * See <a href="#content-type">content types</a> for the syntax of the
	 * content types returned. See <a href="#media-protocol">protocol name</a>
	 * for the syntax of the protocol used.
	 * <p>
	 * For example, if the given <code>protocol</code> is <code>"http"</code>,
	 * then the supported content types that can be played back with the
	 * <code>http</code> protocol will be returned.
	 * <p>
	 * If <code>null</code> is passed in as the <code>protocol</code>, all
	 * the supported content types for this implementation will be returned. The
	 * returned array must be non-empty.
	 * <p>
	 * If the given <code>protocol</code> is an invalid or unsupported
	 * protocol, then an empty array will be returned.
	 *
	 * @param protocol
	 *            The input protocol for the supported content types.
	 * @return The list of supported content types for the given protocol.
	 */
	public static String[] getSupportedContentTypes(String protocol) {
		return ManagerEAS.getSupportedContentTypes(protocol);
	}

	/**
	 * Return the list of supported protocols given the content type. The
	 * protocols are returned as strings which identify what locators can be
	 * used for creating <code>Player</code>'s.
	 * <p>
	 * See <a href="#media-protocol">protocol name</a> for the syntax of the
	 * protocols returned. See <a href="#content-type">content types</a> for
	 * the syntax of the content type used.
	 * <p>
	 * For example, if the given <code>content_type</code> is
	 * <code>"audio/x-wav"</code>, then the supported protocols that can be
	 * used to play back <code>audio/x-wav</code> will be returned.
	 * <p>
	 * If <code>null</code> is passed in as the <code>content_type</code>,
	 * all the supported protocols for this implementation will be returned. The
	 * returned array must be non-empty.
	 * <p>
	 * If the given <code>content_type</code> is an invalid or unsupported
	 * content type, then an empty array will be returned.
	 *
	 * @param content_type
	 *            The content type for the supported protocols.
	 * @return The list of supported protocols for the given content type.
	 */
	public static String[] getSupportedProtocols(String content_type) {
		return ManagerEAS.getSupportedProtocols(content_type);
	}

	/**
	 * Create a <code>Player</code> from an input locator.
	 *
	 * @param locator
	 *            A locator string in URI syntax that describes the media
	 *            content.
	 * @return A new <code>Player</code>.
	 * @exception IllegalArgumentException
	 *                Thrown if <code>locator</code> is <code>null</code>.
	 * @exception MediaException
	 *                Thrown if a <code>Player</code> cannot be created for
	 *                the given locator.
	 * @exception IOException
	 *                Thrown if there was a problem connecting with the source
	 *                pointed to by the <code>locator</code>.
	 * @exception SecurityException
	 *                Thrown if the caller does not have security permission to
	 *                create the <code>Player</code>.
	 */
	public static Player createPlayer(String locator) throws IOException,
			MediaException {
		return ManagerEAS.createPlayer(locator);
	}

	/**
	 * Create a <code>Player</code> to play back media from an
	 * <code>InputStream</code>.
	 * <p>
	 * The <code>type</code> argument specifies the content-type of the input
	 * media. If <code>null</code> is given, <code>Manager</code> will
	 * attempt to determine the type. However, since determining the media type
	 * is non-trivial for some media types, it may not be feasible in some
	 * cases. The <code>Manager</code> may throw a <code>MediaException</code>
	 * to indicate that.
	 *
	 * @param stream
	 *            The <code>InputStream</code> that delivers the input media.
	 * @param type
	 *            The <code>ContentType</code> of the media.
	 * @return A new <code>Player</code>.
	 * @exception IllegalArgumentException
	 *                Thrown if <code>stream</code> is <code>null</code>.
	 * @exception MediaException
	 *                Thrown if a <code>Player</code> cannot be created for
	 *                the given stream and type.
	 * @exception IOException
	 *                Thrown if there was a problem reading data from the
	 *                <code>InputStream</code>.
	 * @exception SecurityException
	 *                Thrown if the caller does not have security permission to
	 *                create the <code>Player</code>.
	 */
	public static Player createPlayer(InputStream stream, String type)
			throws IOException, MediaException {
		return ManagerEAS.createPlayer(stream, type);
	}

	/**
	 * Create a <code>Player</code> for a <code>DataSource</code>.
	 *
	 * @param source
	 *            The <CODE>DataSource</CODE> that provides the media content.
	 * @return A new <code>Player</code>.
	 * @exception IllegalArgumentException
	 *                Thrown if <code>source</code> is <code>null</code>.
	 * @exception MediaException
	 *                Thrown if a <code>Player</code> cannot be created for
	 *                the given <code>DataSource</code>.
	 * @exception IOException
	 *                Thrown if there was a problem connecting with the source.
	 * @exception SecurityException
	 *                Thrown if the caller does not have security permission to
	 *                create the <code>Player</code>.
	 */
	public static Player createPlayer(DataSource source) throws IOException,
			MediaException {

		return ManagerEAS.createPlayer(source);
	}

	/**
	 * Play back a tone as specified by a note and its duration. A note is given
	 * in the range of 0 to 127 inclusive. The frequency of the note can be
	 * calculated from the following formula:
	 *
	 * <pre>
	 *      SEMITONE_CONST = 17.31234049066755 = 1/(ln(2&circ;(1/12)))
	 *      note = ln(freq/8.176)*SEMITONE_CONST
	 *      The musical note A = MIDI note 69 (0x45) = 440 Hz.
	 * </pre>
	 *
	 * This call is a non-blocking call. Notice that this method may utilize CPU
	 * resources significantly on devices that don't have hardware support for
	 * tone generation.
	 *
	 * @param note
	 *            Defines the tone of the note as specified by the above
	 *            formula.
	 * @param duration
	 *            The duration of the tone in milli-seconds. Duration must be
	 *            positive.
	 * @param volume
	 *            Audio volume range from 0 to 100. 100 represents the maximum
	 *            volume at the current hardware level. Setting the volume to a
	 *            value less than 0 will set the volume to 0. Setting the volume
	 *            to greater than 100 will set the volume to 100.
	 *
	 * @exception IllegalArgumentException
	 *                Thrown if the given note or duration is out of range.
	 * @exception MediaException
	 *                Thrown if the tone cannot be played due to a
	 *                device-related problem.
	 */
	static public void playTone(int note, int duration, int volume)
			throws MediaException {
		ManagerEAS.playTone(note, duration, volume);
	}

	/**
	 * Get the time-base object for the system.
	 *
	 * @return The system time base.
	 */
	public static TimeBase getSystemTimeBase() {
		return ManagerEAS.getSystemTimeBase();
	}
}

