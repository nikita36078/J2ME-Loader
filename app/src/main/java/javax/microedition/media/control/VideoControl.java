/*
 * %W% %E%
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package javax.microedition.media.control;

import javax.microedition.media.MediaException;


/**
 * <code>VideoControl</code> controls the display of video.
 * A <code>Player</code> which supports the playback of video
 * must provide a <code>VideoControl</code> via its
 * <code>getControl</code> and <code>getControls</code>
 * method.
 *
 */
public interface VideoControl extends GUIControl {

    /**
     * This defines a mode on how the video is displayed.
     * It is used in conjunction with
     * <a href="#initDisplayMode(int, java.lang.Object)">
     * <code>initDisplayMode</code></a>.
     * <p>
     * <code>USE_DIRECT_VIDEO</code> mode can only be used
     * on platforms with LCDUI support.
     * <p>
     * When <code>USE_DIRECT_VIDEO</code> is specified for
     * <code>initDisplayMode</code>, the <code>arg</code>
     * argument must not be null and must be a
     * <code>javax.microedition.lcdui.Canvas</code> or a subclass of it.
     * In this mode, the video is directly
     * rendered onto the canvas.  The region where the video
     * is rendered can be set by the <code>setDisplayLocation</code>
     * method.  By default, the location is (0, 0).
     * Drawing any graphics or rendering other video at the same
     * region on the canvas may not be supported.
     * <p>
     * <code>initDisplayMode</code>
     * returns <code>null</code> in this mode.
     * <p>
     * Here is one sample usage scenario:
     * <pre>
     * <code>
     *   javax.microedition.lcdui.Canvas canvas;
     *   // canvas must be created before being used in the following code.
     *
     *   try {
     *       Player p = Manager.createPlayer("http://mymachine/abc.mpg");
     *       p.realize();
     *       VideoControl vc;
     *       if ((vc = (VideoControl)p.getControl("VideoControl")) != null) {
     *           vc.initDisplayMode(VideoControl.USE_DIRECT_VIDEO, canvas);
     *           vc.setVisible(true);
     *       }
     *       p.start();
     *   } catch (MediaException pe) {
     *   } catch (IOException ioe) {
     *   }
     * </code>
     * </pre>
     * <p>
     * Value 1 is assigned to <code>USE_DIRECT_VIDEO</code>.
     */
    int USE_DIRECT_VIDEO = 1;


    /**
     * Initialize the mode on how the video is displayed.
     * This method must be called before video can be displayed.
     * <p>
     * Two modes are defined:
     * <ul>
     * <li> <a href="GUIControl.html#USE_GUI_PRIMITIVE"><code>USE_GUI_PRIMITIVE</code></a> (inherited from GUIControl)
     * <li> <a href="#USE_DIRECT_VIDEO"><code>USE_DIRECT_VIDEO</code></a>
     * </ul>
     * On platforms with LCDUI support, both modes must be supported.
     *
     * @param mode The video mode that determines how video is
     * displayed.  It can be <code>USE_GUI_PRIMITIVE</code>,
     * <code>USE_DIRECT_VIDEO</code> or an implementation-
     * specific mode.
     *
     * @param arg The exact semantics of this argument is
     * specified in the respective mode definitions.
     *
     * @return The exact semantics and type of the object returned
     * are specified in the respective mode definitions.
     *
     * @exception IllegalStateException Thrown if
     * <code>initDisplayMode</code> is called again after it has
     * previously been called successfully.
     *
     * @exception IllegalArgumentException Thrown if
     * the <code>mode</code> or <code>arg</code>
     * argument is invalid.   <code>mode</code> must be
     * <code>USE_GUI_PRIMITIVE</code>,
     * <code>USE_DIRECT_VIDEO</code>, or a custom mode
     * supported by this implementation.
     * <code>arg</code> must conform to the
     * conditions defined by the
     * respective mode definitions.
     * Refer to the mode definitions for the required type
     * of <code>arg</code>.
     */
    Object initDisplayMode(int mode, Object arg);

    /**
     * Set the location of the video with respect to
     * the canvas where the video is displayed.
     * <p>
     * This method only works when the <code>USE_DIRECT_VIDEO</code>
     * mode is set.  In <code>USE_GUI_PRIMITIVE</code> mode,
     * this call will be ignored.
     * <p>
     * The location is specified in pixel values relative to the
     * upper left hand corner of the GUI object.
     * <p>
     * By default, video appears at location (0,0).
     * <p>
     * The location can be given in negative values or can be
     * greater than the actual size of the canvas.  When
     * that happens, the video should be clipped to the
     * boundaries of the canvas.
     *
     * @param x The x coordinate (in pixels) of the video location.
     * @param y The y coordinate (in pixels) of the video location.
     * @exception IllegalStateException Thrown if
     * <code>initDisplayMode</code> has not been called.
     */
    void setDisplayLocation(int x, int y);

    /**
     * Return the X-coordinate of the video with respect
     * to the GUI object where the video is displayed.
     * The coordinate is specified in pixel values relative to the
     * upper left hand corner of the GUI object.
     * <p>
     * The return value is undefined if <code>initDisplayMode</code>
     * has not been called.
     *
     * @return the X-coordinate of the video.
     */
    int getDisplayX();

    /**
     * Return the Y-coordinate of the video with respective
     * to the GUI object where the video is displayed.
     * The coordinate is specified in pixel values relative to the
     * upper left hand corner of the GUI object.
     * <p>
     * The return value is undefined if <code>initDisplayMode</code>
     * has not been called.
     *
     * @return the Y-coordinate of the video.
     */
    int getDisplayY();

    /**
     * Show or hide the video.
     * <p>
     * If <code>USE_GUI_PRIMITIVE</code> is set, the video by
     * default is shown when the GUI primitive is displayed.
     * If <code>USE_DIRECT_VIDEO</code> is set, the video by
     * default is not shown when the canvas is displayed
     * until <code>setVisible(true)</code> is called.  If
     * the canvas is removed from the screen, the video
     * will not be displayed.
     *
     * @param visible Show the video if true, hide it otherwise.
     * @exception IllegalStateException Thrown if
     * <code>initDisplayMode</code> has not been called.
     */
    void setVisible(boolean visible);

    /**
     * Resize the video image.
     * <p>
     * If the video mode is set to <code>USE_DIRECT_VIDEO</code>,
     * setting the size of the video will not
     * affect the size of the GUI object that the video is
     * displayed.
     * If the video is scaled to beyond the size of the
     * GUI object, the video will be clipped.
     * <p>
     * If the video mode is set to <code>USE_GUI_PRIMITIVE</code>,
     * Scaling the video will also scale the GUI object.
     * <p>
     * The actual scaling algorithm is left up
     * to the underlying implementation.  If
     * the dimensions of the requested display size are smaller than
     * the dimensions of the video clip, some implementations may
     * choose to merely clip the video while other implementations
     * may resize the video.
     * <p>
     * If the dimensions of the requested display size are bigger than
     * the dimensions of the video clip, some implementations may
     * resize the video while others may leave the video clip in the
     * original size and just enlarge the display region.  It is
     * left up to the implementation
     * where the video clip is placed in the display region in this
     * instance (i.e., it can be in the center of the window or
     * in a corner of the window).
     *
     * @param width Desired width (in pixels) of the display window
     * @param height Desired height (in pixels) of the display window
     * @exception IllegalArgumentException Thrown if the
     * given <code>width</code> and <code>height</code> are
     * non-positive values.
     * @exception IllegalStateException Thrown if
     * <code>initDisplayMode</code> has not been called.
     * @exception MediaException Thrown if resizing is not supported or
     * the operation failed due to hardware or software limitations.
     */
    void setDisplaySize(int width, int height)
	throws MediaException;

    /**
     * Set the size of the render region for the video clip to be
     * fullscreen. It is left up to the underlying implementation how
     * fullscreen mode is
     * implemented and what actual dimensions constitutes fullscreen.
     * This is useful when the application does not know the actual
     * width and height dimensions
     * that are needed to make setDisplaySize(width, height) go to
     * fullscreen mode.
     * For example, on a device with a 400 pixel wide by 200 pixel
     * high screen, a video
     * clip that is 50 pixels wide by 100 pixels high in fullscreen
     * mode may be
     * 100 pixels wide by 200 pixels high if the underlying implementation
     * wants
     * to preserve the aspect ratio.  In this case, an exception
     * is not thrown.
     * @param fullScreenMode Indicates whether or not to render
     * in full screen mode
     * @exception MediaException Thrown if resizing to full screen size
     * is not supported.
     * @exception IllegalStateException Thrown if
     * <code>initDisplayMode</code> has not been called.
     */
    void setDisplayFullScreen(boolean fullScreenMode) throws MediaException;

    /**
     * Return the width of the source video.  The
     * width must be a positive number.
     * @return the width of the source video
     */
    int getSourceWidth();

    /**
     * Return the height of the source video.  The
     * height must be a positive number.
     * @return the height of the source video
     */
    int getSourceHeight();

    /**
     * Return the actual width of the current render video.
     *
     * @return width of the display video
     * @exception IllegalStateException Thrown if
     * <code>initDisplayMode</code> has not been called.
     */
    int getDisplayWidth();

    /**
     * Return the actual height of the current render video.
     *
     * @return height of the display video
     * @exception IllegalStateException Thrown if
     * <code>initDisplayMode</code> has not been called.
     */
    int getDisplayHeight();

    /**
     * Get a snapshot of the displayed content. Features and format
     * of the captured image are specified by <code>imageType</code>.
     * Supported formats can be queried from <code>System.getProperty</code>
     * with
     * <a href="../../../../overview-summary.html#video.snapshot.encodings">
     * <code>video.snapshot.encodings</code></a> as the key.
     * The first format in the supported list is the default capture format.
     *
     * @param imageType Format and resolution of the returned image.
     * If <code>null</code> is given, the default capture format is used.
     * @return image as a byte array in required format.
     * @exception IllegalStateException Thrown if
     * <code>initDisplayMode</code> has not been called.
     * @exception MediaException Thrown if the requested format is
     * not supported or the <code>Player</code> does not support
     * snapshots.
     * @exception SecurityException Thrown if the caller does
     * not have the security permission to take the snapshot.
     */
    byte[] getSnapshot(String imageType) throws MediaException;
}

