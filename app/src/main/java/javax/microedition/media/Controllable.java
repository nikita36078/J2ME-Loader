/*
 * %W% %E%
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package javax.microedition.media;

/**
 * <code>Controllable</code> provides an interface for
 * obtaining the <code>Control</code>s from an object
 * like a <code>Player</code>.
 * It provides methods to query all the supported <code>Control</code>s
 * and to obtain a particular <code>Control</code> based on its class
 * name.
 **/
public interface Controllable {

    /**
     * Obtain the collection of <code>Control</code>s
     * from the object that implements this interface.
     * <p>
     * Since a single object can implement multiple
     * <code>Control</code> interfaces, it's necessary
     * to check each object against different <code>Control</code>
     * types.  For example:
     * <p>
     * <code>
     *   Controllable controllable;<br>
     *   &nbsp;&nbsp;&nbsp;&nbsp;:<br>
     *   Control cs[];<br>
     *   cs = controllable.getControls();<br>
     *   for (int i = 0; i < cs.length; i++) {<br>
     *   &nbsp;&nbsp;&nbsp;&nbsp;if (cs[i] instanceof ControlTypeA)<br>
     *   &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;doSomethingA();<br>
     *   &nbsp;&nbsp;&nbsp;&nbsp;if (cs[i] instanceof ControlTypeB)<br>
     *   &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;doSomethingB();<br>
     *   &nbsp;&nbsp;&nbsp;&nbsp;// etc. <br>
     *   }<br>
     * </code>
     * <p>
     * The list of <code>Control</code> objects returned
     * will not contain any duplicates.  And the list will not
     * change over time.
     * <p>
     * If no <code>Control</code> is supported, a zero length
     * array is returned.
     *
     * @return the collection of <code>Control</code> objects.
     * @exception IllegalStateException Thrown if <code>getControls</code>
     * is called in a wrong state.
     * See
     * <a href="Player.html#controls"><code>Player</code></a>
     * and
     * <a href="protocol/DataSource.html#controls"><code>DataSource</code></a>
     * for more details.
     */
    Control[] getControls();

    /**
     * Obtain the object that implements the specified
     * <code>Control</code> interface.
     * <p>
     * If the specified <code>Control</code> interface is not supported
     * then <code>null</code> is returned.
     * <p>
     * If the <code>Controllable</code> supports multiple objects that
     * implement the same specified <code>Control</code> interface, only
     * one of them will be returned.  To obtain all the
     * <code>Control</code>'s of that type, use the <code>getControls</code>
     * method and check the list for the requested type.
     *
     * @param controlType the class name of the <code>Control</code>.
     * The class name
     * should be given either as the fully-qualified name of the class;
     * or if the package of the class is not given, the package
     * <code>javax.microedition.media.control</code> is assumed.
     *
     * @return the object that implements the control,
     * or <code>null</code>.
     * @exception IllegalArgumentException Thrown if <code>controlType</code>
     * is <code>null</code>.
     * @exception IllegalStateException Thrown if <code>getControl</code>
     * is called in a wrong state.
     * See
     * <a href="Player.html#controls"><code>Player</code></a>
     * and
     * <a href="protocol/DataSource.html#controls"><code>DataSource</code></a>
     * for more details.
     */
    Control getControl(String controlType);

}
