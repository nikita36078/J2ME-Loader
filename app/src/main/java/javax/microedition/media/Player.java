/*
 * %W% %E%
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package javax.microedition.media;

//import javax.microedition.media.protocol.DataSource;
//import java.io.IOException;


/**
 * <code>Player</code> controls the rendering of time based media data.
 * It provides the methods to manage the <code>Player</code>'s life
 * cycle, controls the playback progress, obtains the presentation
 * components, controls and provides the means to synchronize
 * with other <code>Players</code>.
 *
 * <h2>Simple Playback</h2>
 *
 * <blockquote>
 * A <code>Player</code> can be created from one of the
 * <code>Manager</code>'s <code>createPlayer</code> methods.
 * After the <code>Player</code> is created,
 * calling <code>start</code> will start the playback as soon as possible.
 * The method will return when the
 * playback is started.  The playback will continue in the background
 * and will stop automatically when the end of media is reached.
 * <p>
 * <a href="#example">Simple playback example</a> illustrates this.
 * </blockquote>
 *
 * <h2>Player Life Cycle</h2>
 *
 * <blockquote>
 * A <code>Player</code> has five states:
 * <a href="#unrealizedState"><i>UNREALIZED</i></a>,
 * <a href="#realizedState"><i>REALIZED</i></a>,
 * <a href="#prefetchedState"><i>PREFETCHED</i></a>,
 * <a href="#startedState"><i>STARTED</i></a>,
 * <a href="#closedState"><i>CLOSED</i></a>.
 * <p>
 *
 * The purpose of these life-cycle states is to provide
 * programmatic control over potentially time-consuming operations.
 * For example, when a <code>Player</code> is first constructed, it's in
 * the <i>UNREALIZED</i> state.
 * Transitioned from <I>UNREALIZED</I> to <I>REALIZED</I>, the
 * <code>Player</code>
 * performs the communication necessary to locate all of the resources
 * it needs to function (such as communicating with a server
 * or a file system).
 * The <code>realize</code> method allows an application to initiate this
 * potentially time-consuming process at an
 * appropriate time.
 * <p>
 *
 * Typically, a <code>Player</code> moves from the <i>UNREALIZED</i> state
 * to the <i>REALIZED</i> state, then to the <i>PREFETCHED</i> state,
 * and finally on to the <i>STARTED</i> state.
 * <p>
 *
 * A <code>Player</code> stops when it reaches the
 * end of media; when
 * its stop time is reached;
 * or when the <code>stop</code> method is invoked.
 * When that happens, the <code>Player</code> moves from the
 * <i>STARTED</i> state
 * back to the <i>PREFETCHED</i> state.
 * It is then ready to repeat the cycle.
 * <p>
 *
 * To use a <code>Player</code>, you must set up parameters to
 * manage its movement through these life-cycle states and then
 * move it through the states using the <code>Player</code>'s
 * state transition methods.
 *
 * </blockquote>
 * <p>
 *
 * <h2>Player States</h2>
 *
 * <blockquote>
 * This section describes the semantics of each of the <code>Player</code>
 * states.
 *
 * <a name="unrealizedState"></a>
 * <h3>UNREALIZED State</h3>
 * <blockquote>
 * A <code>Player</code> starts in
 * the <i>UNREALIZED</i> state.
 * An unrealized <code>Player</code>
 * does not have enough information to acquire all the resources it needs
 * to function.
 * <p>
 * The following methods must not be used
 * when the <code>Player</code> is in
 * the <i>UNREALIZED</i> state.
 * <ul>
 * <li> <CODE>getContentType</CODE>
 * <li> <CODE>setTimeBase</CODE>
 * <li> <CODE>getTimeBase</CODE>
 * <li> <CODE>setMediaTime</CODE>
 * <li> <CODE>getControls</CODE>
 * <li> <CODE>getControl</CODE>
 * </ul>
 *
 * An <code>IllegalStateException</code> will be thrown.
 * <p>
 * The <a href="#realize()"><code>realize</code></a> method transitions
 * the <code>Player</code> from the <i>UNREALIZED</i> state to the
 * <i>REALIZED</i> state.
 * </blockquote>
 *
 * <a name="realizedState"></a>
 * <h3>REALIZED State</h3>
 *
 * <blockquote>
 * A <code>Player</code> is in the <i>REALIZED</i> state when it
 * has obtained
 * the information required to acquire the media resources.
 * Realizing a <code>Player</code> can be a resource and
 * time consuming process.
 * The <code>Player</code> may have to communicate with
 * a server, read a file, or interact with a set of objects.
 * <p>
 *
 * Although a realized <code>Player</code> does not have
 * to acquire any resources, it
 * is likely to have acquired all of the resources it needs except
 * those that imply exclusive use of a
 * scarce system resource, such as an audio device.
 * <p>
 *
 * Normally, a <code>Player</code> moves from the <i>UNREALIZED</i> state
 * to the <i>REALIZED</i> state.
 * After <a href="#realize()"><code>realize</code></a> has been
 * invoked on a <code>Player</code>,
 * the only way it can return
 * to the <i>UNREALIZED</i> state is if
 * <a href="#deallocate()"><code>deallocate</code></a> is
 * invoked before <code>realize</code> is completed.
 * Once a <code>Player</code> reaches the <i>REALIZED</i> state, it
 * never returns to the <i>UNREALIZED</i> state.  It remains in one of four
 * states: <i>REALIZED</i>, <i>PREFETCHED</i>, <i>STARTED</i> or
 * <i>CLOSED</i>.
 * </blockquote>
 *
 * <a name="prefetchedState"></a>
 * <h3>PREFETCHED State</h3>
 *
 * <blockquote>
 * Once realized, a <code>Player</code> may still need to
 * perform a number of time-consuming tasks before it is ready to be started.
 * For example, it may need to acquire scarce or exclusive resources,
 * fill buffers with media data, or perform other start-up processing.
 * Calling
 * <a href="#prefetch()"><code>prefetch</code></a>
 * on the <code>Player</code> carries
 * out these tasks.
 * <p>
 *
 * Once a <code>Player</code> is in the <i>PREFETCHED</i> state, it may
 * be started.
 * Prefetching reduces the startup latency of a <code>Player</code>
 * to the minimum possible value.
 * <p>
 *
 * When a started <code>Player</code> stops,
 * it returns
 * to the <i>PREFETCHED</i> state.
 * </blockquote>
 * <p>
 *
 * <a name="startedState"></a>
 * <h3>STARTED State</h3>
 *
 * <blockquote>
 * Once prefetched, a <code>Player</code> can enter the
 * <i>STARTED</i> state by calling the
 * <a href="#start()"><code>start</code></a> method.
 * A <I>STARTED</I>&nbsp;<CODE>Player</CODE>&nbsp;
 * means the <code>Player</code> is running and processing data.
 * A <code>Player</code> returns to the <i>PREFETCHED</i>
 * state when it stops, because the
 * <a href="#stop()"><code>stop</code></a> method was invoked,
 * it has reached the end of the media, or its stop time.
 * <p>
 *
 * When the <code>Player</code> moves from the <i>PREFETCHED</i>
 * to the <i>STARTED</i> state, it posts a <code>STARTED</code> event.
 * When it moves from the <i>STARTED</i> state to the
 * <i>PREFETCHED</i> state,
 * it posts a <code>STOPPED</code>, <code>END_OF_MEDIA</code> or
 * <code>STOPPED_AT_TIME</code> event depending on the reason it
 * stopped.
 * <p>
 *
 * The following methods must not be used
 * when the <code>Player</code> is in the <i>STARTED</i> state:
 * <ul>
 * <li> <CODE>setTimeBase</CODE>
 * <li> <CODE>setLoopCount</CODE>
 * </ul>
 *
 * An <code>IllegalStateException</code> will be thrown.
 *
 * </blockquote>
 *
 * <a name="closedState"></a>
 * <h3>CLOSED state</h3>
 *
 * <blockquote>
 * Calling <code>close</code> on the <code>Player</code>
 * puts it in the <i>CLOSED</i> state.  In the <i>CLOSED</i>
 * state, the <code>Player</code> has
 * released most of its resources and must not
 * be used again.
 * </blockquote>
 *
 * The <code>Player</code>'s five states and the state transition
 * methods are summarized in the following diagram:
 * <p>
 * <blockquote>
 * <img src="states.gif" width="492" height="183">
 * </blockquote>
 *
 * </blockquote>
 * <p>
 *
 * <a name="CE">
 * <h2>Player Events</h2></a>
 *
 * <blockquote>
 * <code>Player</code> events asynchronously deliver
 * information about the <code>Player</code>'s state changes
 * and other relevant information from the <code>Player</code>'s
 * <code>Control</code>s.
 * <p>
 *
 * To receive events, an object must implement the
 * <code>PlayerListener</code> interface and use the
 * <code>addPlayerListener</code> method to register its
 * interest in a <code>Player</code>'s events.
 * All <code>Player</code> events are posted to each
 * registered listener.
 * <p>
 *
 * The events are guaranteed to be delivered in the order
 * that the actions representing the events occur.
 * For example, if a <code>Player</code>
 * stops shortly after it starts because it is playing back
 * a very short media file, the <code>STARTED</code> event
 * must always
 * preceed the <code>END_OF_MEDIA</code> event.
 * <p>
 *
 * An <code>ERROR</code> event may be sent any time
 * an irrecoverable error has occured.  When that happens, the
 * <code>Player</code> is in the <i>CLOSED</i> state.
 * <p>
 *
 * The <code>Player</code> event mechanism is extensible and
 * some <code>Players</code> define events other than
 * the ones described here.  For a list of pre-defined player
 * events, check the <code>PlayerListener</code> interface.
 * </blockquote>
 *
 * <h3>Managing the Resources Used by a Player</h3>
 *
 * <blockquote>
 * The <a href="#prefetch()"><code>prefetch</code></a>
 * method is used to acquire scarce or exclusive resources
 * such as the audio device.
 * Conversely, the <a href="#deallocate()"><code>deallocate</code></a>
 * method is used to release the scarce or exclusive
 * resources.  By using these two methods, an application can
 * programmatically manage the <code>Player</code>'s resources.
 * <p>
 * For example, in an implementation with an exclusive audio device, to
 * alternate the audio playback of multiple <code>Player</code>s,
 * an application can selectively deallocate and prefetch individual
 * <code>Player</code>s.
 *
 * </blockquote>
 * <p>
 *
 * <h2>Player's TimeBase</h2>
 *
 * <blockquote>
 * The <code>TimeBase</code> of a <code>Player</code> provides the
 * basic measure of time for the <code>Player</code> to synchronize
 * its media playback.  Each <code>Player</code> must provide one
 * default <code>TimeBase</code>.  The <code>getTimeBase</code>
 * method can be used to retrieve that.
 * <p>
 *
 * Setting a different <code>TimeBase</code> on a
 * <code>Player</code> instructs the <code>Player</code> to synchronize
 * its playback rate according to the given <code>TimeBase</code>.
 * <p>
 *
 * Two <code>Player</code>s can be synchronized by getting the
 * <code>TimeBase</code> from one <code>Player</code> and setting
 * that on the second <code>Player</code>.
 * <p>
 *
 * However, not all <code>Player</code>s support using a different
 * <code>TimeBase</code> other than its own.  In such cases,
 * a <code>MediaException</code> will be thrown when
 * <code>setTimeBase</code> is called.
 * </blockquote>
 * <p>
 *
 * <a name="controls"></a>
 * <h2>Player's Controls</h2>
 * <blockquote>
 * <code>Player</code> implements <code>Controllable</code> which
 * provides extra controls via some type-specific <code>Control</code>
 * interfaces.  <code>getControl</code> and <code>getControls</code>
 * cannot be called when the <code>Player</code> is in the
 * <i>UNREALIZED</i> or <i>CLOSED</i> state.
 * An <code>IllegalStateException</code> will be thrown.
 * </blockquote>
 * <p>
 *
 * <a name="example"></a>
 * <h2>Simple Playback Example</h2>
 *
 * <blockquote>
 * <pre>
 * try {
 *     Player p = Manager.createPlayer("http://abc.mpg");
 *     p.realize();
 *     VideoControl vc;
 *     if ((vc = (VideoControl)p.getControl("VideoControl")) != null)
 *         add((Component)vc.initDisplayMode(vc.USE_GUI_PRIMITIVE, null));
 *     p.start();
 * } catch (MediaException pe) {
 * } catch (IOException ioe) {
 * }
 * </pre>
 * </blockquote>
 *
 */

public interface Player extends Controllable {

    /**
     * The state of the <code>Player</code> indicating that it has
     * not acquired the required information and resources to function.
     * <p>
     * Value 100 is assigned to <code>UNREALIZED</code>.
     */
    int UNREALIZED = 100;

    /**
     * The state of the <code>Player</code> indicating that it has
     * acquired the required information but not the resources to function.
     * <p>
     * Value 200 is assigned to <code>REALIZED</code>.
     */
    int REALIZED = 200;

    /**
     * The state of the <code>Player</code> indicating that it has
     * acquired all the resources to begin playing.
     * <p>
     * Value 300 is assigned to <code>PREFETCHED</code>.
     */
    int PREFETCHED = 300;

    /**
     * The state of the <code>Player</code> indicating that the
     * <code>Player</code> has already started.
     * <p>
     * Value 400 is assigned to <code>STARTED</code>.
     */
    int STARTED = 400;

    /**
     * The state of the <code>Player</code> indicating that the
     * <code>Player</code> is closed.
     * <p>
     * Value 0 is assigned to <code>CLOSED</code>.
     */
    int CLOSED = 0;

    /**
     * The returned value indicating that the requested time is unknown.
     * <p>
     * Value -1 is assigned to <code>TIME_UNKNOWN</code>.
     */
    long TIME_UNKNOWN = -1;

    /**
     * Constructs portions of the <code>Player</code> without
     * acquiring the scarce and exclusive resources.
     * This may include examining media data and may
     * take some time to complete.
     * <p>
     * When <code>realize</code> completes successfully,
     * the <code>Player</code> is in the
     * <i>REALIZED</i> state.
     * <p>
     * If <code>realize</code> is called when the <code>Player</code> is in
     * the <i>REALIZED</i>, <i>PREFETCHTED</i> or <i>STARTED</i> state,
     * the request will be ignored.
     *
     * @exception IllegalStateException Thrown if the <code>Player</code>
     * is in the <i>CLOSED</i> state.
     * @exception MediaException Thrown if the <code>Player</code> cannot
     * be realized.
     * @exception SecurityException Thrown if the caller does not
     * have security permission to realize the <code>Player</code>.
     *
     */
    void realize() throws MediaException;

    /**
     * Acquires the scarce and exclusive resources
     * and processes as much data as necessary
     * to reduce the start latency.
     * <p>
     * When <code>prefetch</code> completes successfully,
     * the <code>Player</code> is in
     * the <i>PREFETCHED</i> state.
     * <p>
     * If <code>prefetch</code> is called when the <code>Player</code>
     * is in the <i>UNREALIZED</i> state,
     * it will implicitly call <code>realize</code>.
     * <p>
     * If <code>prefetch</code> is called when the <code>Player</code>
     * is already in the <i>PREFETCHED</i> state, the <code>Player</code>
     * may still process data necessary to reduce the start
     * latency.  This is to guarantee that start latency can
     * be maintained at a minimum.
     * <p>
     * If <code>prefetch</code> is called when the <code>Player</code>
     * is in the <i>STARTED</i> state,
     * the request will be ignored.
     * <p>
     * If the <code>Player</code> cannot obtain all
     * of the resources it needs, it throws a <code>MediaException</code>.
     * When that happens, the <code>Player</code> will not be able to
     * start.  However, <code>prefetch</code> may be called again when
     * the needed resource is later released perhaps by another
     * <code>Player</code> or application.
     *
     * @exception IllegalStateException Thrown if the <code>Player</code>
     * is in the <i>CLOSED</i> state.
     * @exception MediaException Thrown if the <code>Player</code> cannot
     * be prefetched.
     * @exception SecurityException Thrown if the caller does not
     * have security permission to prefetch the <code>Player</code>.
     *
     */
    void prefetch() throws MediaException;

    /**
     * Starts the <code>Player</code> as soon as possible.
     * If the <code>Player</code> was previously stopped
     * by calling <code>stop</code> or reaching a preset
     * stop time, it will resume playback
     * from where it was previously stopped.  If the
     * <code>Player</code> has reached the end of media,
     * calling <code>start</code> will automatically
     * start the playback from the start of the media.
     * <p>
     * When <code>start</code> returns successfully,
     * the <code>Player</code> must have been started and
     * a <code>STARTED</code> event will
     * be delivered to the registered <code>PlayerListener</code>s.
     * However, the <code>Player</code> is not guaranteed to be in
     * the <i>STARTED</i> state.  The <code>Player</code> may have
     * already stopped (in the <i>PREFETCHED</i> state) because
     * the media has 0 or a very short duration.
     * <p>
     * If <code>start</code> is called when the <code>Player</code>
     * is in the <i>UNREALIZED</i> or <i>REALIZED</i> state,
     * it will implicitly call <code>prefetch</code>.
     * <p>
     * If <code>start</code> is called when the <code>Player</code>
     * is in the <i>STARTED</i> state,
     * the request will be ignored.
     *
     * @exception IllegalStateException Thrown if the <code>Player</code>
     * is in the <i>CLOSED</i> state.
     * @exception MediaException Thrown if the <code>Player</code> cannot
     * be started.
     * @exception SecurityException Thrown if the caller does not
     * have security permission to start the <code>Player</code>.
     */
    void start() throws MediaException;

    /**
     * Stops the <code>Player</code>.  It will pause the playback at
     * the current media time.
     * <p>
     * When <code>stop</code> returns, the <code>Player</code> is in the
     * <i>PREFETCHED</i> state.
     * A <code>STOPPED</code> event will be delivered to the registered
     * <code>PlayerListener</code>s.
     * <p>
     * If <code>stop</code> is called on
     * a stopped <code>Player</code>, the request is ignored.
     *
     * @exception IllegalStateException Thrown if the <code>Player</code>
     * is in the <i>CLOSED</i> state.
     * @exception MediaException Thrown if the <code>Player</code>
     * cannot be stopped.
     */
    void stop() throws MediaException;

    /**
     * Release the scarce or exclusive
     * resources like the audio device acquired by the <code>Player</code>.
     * <p>
     * When <code>deallocate</code> returns, the <code>Player</code>
     * is in the <i>UNREALIZED</i> or <i>REALIZED</i> state.
     * <p>
     * If the <code>Player</code> is blocked at
     * the <code>realize</code> call while realizing, calling
     * <code>deallocate</code> unblocks the <code>realize</code> call and
     * returns the <code>Player</code> to the <i>UNREALIZED</i> state.
     * Otherwise, calling <code>deallocate</code> returns the
     * <code>Player</code> to  the <i>REALIZED</i> state.
     * <p>
     * If <code>deallocate</code> is called when the <code>Player</code>
     * is in the <i>UNREALIZED</i> or <i>REALIZED</i>
     * state, the request is ignored.
     * <p>
     * If the <code>Player</code> is <code>STARTED</code>
     * when <code>deallocate</code> is called, <code>deallocate</code>
     * will implicitly call <code>stop</code> on the <code>Player</code>.
     *
     * @exception IllegalStateException Thrown if the <code>Player</code>
     * is in the <i>CLOSED</i> state.
     */
    void deallocate();

    /**
     * Close the <code>Player</code> and release its resources.
     * <p>
     * When the method returns, the <code>Player</code> is in the
     * <i>CLOSED</i> state and can no longer be used.
     * A <code>CLOSED</code> event will be delivered to the registered
     * <code>PlayerListener</code>s.
     * <p>
     * If <code>close</code> is called on a closed <code>Player</code>
     * the request is ignored.
     */
    void close();

    /**
     * Sets the <code>TimeBase</code> for this <code>Player</code>.
     * <p>
     * A <code>Player</code> has a default <code>TimeBase</code> that
     * is determined by the implementation.
     * To reset a <code>Player</code> to its default
     * <code>TimeBase</code>, call <code>setTimeBase(null)</code>.
     *
     * @param master The new <CODE>TimeBase</CODE> or
     * <CODE>null</CODE> to reset the <code>Player</code>
     * to its default <code>TimeBase</code>.
     * @exception IllegalStateException Thrown if the <code>Player</code>
     * is in the <i>UNREALIZED</i>, <i>STARTED</i> or <i>CLOSED</i> state.
     * @exception MediaException Thrown if
     * the specified <code>TimeBase</code> cannot be set on the
     * <code>Player</code>.
     * @see #getTimeBase
     */
    void setTimeBase(TimeBase master)
	throws MediaException;

    /**
     * Gets the <code>TimeBase</code> that this <code>Player</code> is using.
     * @return The <code>TimeBase</code> that this <code>Player</code> is using.
     * @see #setTimeBase
     *
     * @exception IllegalStateException Thrown if the <code>Player</code>
     * is in the <i>UNREALIZED</i> or <i>CLOSED</i> state.
     */
    TimeBase getTimeBase();

    /**
     * Sets the <code>Player</code>'s&nbsp;<i>media time</i>.
     * <p>
     * For some media types, setting the media time may not be very
     * accurate.  The returned value will indicate the
     * actual media time set.
     * <p>
     * Setting the media time to negative values will effectively
     * set the media time to zero.  Setting the media time to
     * beyond the duration of the media will set the time to
     * the end of media.
     * <p>
     * There are some media types that cannot support the setting
     * of media time.  Calling <code>setMediaTime</code> will throw
     * a <code>MediaException</code> in those cases.
     *
     * @param now The new media time in microseconds.
     * @return The actual media time set in microseconds.
     * @exception IllegalStateException Thrown if the <code>Player</code>
     * is in the <i>UNREALIZED</i> or <i>CLOSED</i> state.
     * @exception MediaException Thrown if the media time
     * cannot be set.
     * @see #getMediaTime
     */
    long setMediaTime(long now) throws MediaException;

    /**
     * Gets this <code>Player</code>'s current <i>media time</i>.
     * <p>
     * <code>getMediaTime</code> may return <code>TIME_UNKNOWN</code> to
     * indicate that the media time cannot be determined.
     * However, once <code>getMediaTime</code> returns a known time
     * (time not equals to <code>TIME_UNKNOWN</code>), subsequent calls
     * to <code>getMediaTime</code> must not return
     * <code>TIME_UNKNOWN</code>.
     *
     * @return The current <i>media time</i> in microseconds or
     * <code>TIME_UNKNOWN</code>.
     * @exception IllegalStateException Thrown if the <code>Player</code>
     * is in the <i>CLOSED</i> state.
     * @see #setMediaTime
     */
    long getMediaTime();

    /**
     * Gets the current state of this <code>Player</code>.
     * The possible states are: <i>UNREALIZED</i>,
     * <i>REALIZED</i>, <i>PREFETCHED</i>, <i>STARTED</i>, <i>CLOSED</i>.
     *
     * @return The <code>Player</code>'s current state.
     */
    int getState();

    /**
     * Get the duration of the media.
     * The value returned is the media's duration
     * when played at the default rate.
     * <br>
     * If the duration cannot be determined (for example, the
     * <code>Player</code> is presenting live
     * media)  <CODE>getDuration</CODE> returns <CODE>TIME_UNKNOWN</CODE>.
     *
     * @return The duration in microseconds or <code>TIME_UNKNOWN</code>.
     * @exception IllegalStateException Thrown if the <code>Player</code>
     * is in the <i>CLOSED</i> state.
     */
    long getDuration();

    /**
     * Get the content type of the media that's
     * being played back by this <code>Player</code>.
     * <p>
     * See <a href="Manager.html#content-type">content type</a>
     * for the syntax of the content type returned.
     *
     * @return The content type being played back by this
     * <code>Player</code>.
     * @exception IllegalStateException Thrown if the <code>Player</code>
     * is in the <i>UNREALIZED</i> or <i>CLOSED</i> state.
     */
    String getContentType();

    /**
     * Set the number of times the <code>Player</code> will loop
     * and play the content.
     * <p>
     * By default, the loop count is one.  That is, once started,
     * the <code>Player</code> will start playing from the current
     * media time to the end of media once.
     * <p>
     * If the loop count is set to N where N is bigger than one,
     * starting the <code>Player</code> will start playing the
     * content from the current media time to the end of media.
     * It will then loop back to the beginning of the content
     * (media time zero) and play till the end of the media.
     * The number of times it will loop to the beginning and
     * play to the end of media will be N-1.
     * <p>
     * Setting the loop count to 0 is invalid.  An
     * <code>IllegalArgumentException</code> will be thrown.
     * <p>
     * Setting the loop count to -1 will loop and play the content
     * indefinitely.
     * <p>
     * If the <code>Player</code> is stopped before the preset loop
     * count is reached either because <code>stop</code> is called or
     * a preset stop time (set with the <code>StopTimeControl</code>)
     * is reached, calling <code>start</code> again will
     * resume the looping playback from where it was stopped until it
     * fully reaches the preset loop count.
     * <p>
     * An <i>END_OF_MEDIA</i> event will be posted
     * every time the <code>Player</code> reaches the end of media.
     * If the <code>Player</code> loops back to the beginning and
     * starts playing again because it has not completed the loop
     * count, a <i>STARTED</i> event will be posted.
     *
     * @param count indicates the number of times the content will be
     * played.  1 is the default.  0 is invalid.  -1 indicates looping
     * indefintely.
     * @exception IllegalArgumentException Thrown if the given
     * count is invalid.
     * @exception IllegalStateException Thrown if the
     * <code>Player</code> is in the <i>STARTED</i>
     * or <i>CLOSED</i> state.
     */
    void setLoopCount(int count);

    /**
     * Add a player listener for this player.
     *
     * @param playerListener the listener to add.
     * If <code>null</code> is used, the request will be ignored.
     * @exception IllegalStateException Thrown if the <code>Player</code>
     * is in the <i>CLOSED</i> state.
     * @see #removePlayerListener
     */
    void addPlayerListener(PlayerListener playerListener);

    /**
     * Remove a player listener for this player.
     *
     * @param playerListener the listener to remove.
     * If <code>null</code> is used or the given
     * <code>playerListener</code> is not a listener for this
     * <code>Player</code>, the request will be ignored.
     * @exception IllegalStateException Thrown if the <code>Player</code>
     * is in the <i>CLOSED</i> state.
     * @see #addPlayerListener
     */
    void removePlayerListener(PlayerListener playerListener);
}
