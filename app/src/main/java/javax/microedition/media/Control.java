/*
 * %W% %E%
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package javax.microedition.media;


/**
 * A <code>Control</code> object is used to control some media
 * processing functions.  The set of
 * operations are usually functionally related.  Thus a <code>Control</code>
 * object provides a logical grouping of media processing functions.
 * <p>
 * <code>Control</code>s are obtained from <code>Controllable</code>.
 * The <code>Player</code> interface extends <code>Controllable</code>.
 * Therefore a <code>Player</code> implementation can use the
 * <code>Control</code> interface
 * to extend its media processing functions.  For example,
 * a <code>Player</code> can expose a <code>VolumeControl</code> to allow
 * the volume level to be set.
 * <p>
 * Multiple <code>Control</code>s can be implemented by the same object.
 * For example, an object can implement both <code>VolumeControl</code>
 * and <code>ToneControl</code>.  In this case, the object can be
 * used for controlling both the volume and tone generation.
 * <p>
 * The <code>javax.microedition.media.control</code> package specifies
 * a set of pre-defined <code>Control</code>s.
 *
 * @see Controllable
 * @see Player
 */
public interface Control {
}
