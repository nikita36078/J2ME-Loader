/*
* Copyright (c) 2003 Nokia Corporation and/or its subsidiary(-ies).
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

import java.util.Vector;

public class Group extends Node
{
    //------------------------------------------------------------------
    // Instance data
    //------------------------------------------------------------------

    // The child links are duplicated on the Java side for the same
    // reason as the other node->node references; see Node.java

    Vector children;

    //------------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------------

    public Group()
    {
        super(_ctor(Interface.getHandle()));
    }

    Group(int handle)
    {
        super(handle);
        int n = _getChildCount(handle);
        while (n-- > 0)
        {
            linkChild((Node) getInstance(_getChild(handle, n)));
        }
    }

    //------------------------------------------------------------------
    // Public methods
    //------------------------------------------------------------------

    public void addChild(Node child)
    {
        _addChild(handle, child != null ? child.handle : 0);
        if (child != null)
        {
            linkChild(child);
        }
    }

    public void removeChild(Node child)
    {
        if (child != null)
        {
            _removeChild(handle, child.handle);
            detachChild(child);
        }
    }

    public int getChildCount()
    {
        return _getChildCount(handle);
    }

    public Node getChild(int index)
    {

        /* Instead of trying to match the indexing of children on the
         * native side, we just call the native getter. This may have
         * some performance penalty, but likely not enough to make it
         * worth the extra maintenance burden of duplicating the
         * native ordering here. */

        return (Node) getInstance(_getChild(handle, index));
    }

    public boolean pick(int mask,
                        float ox, float oy, float oz,
                        float dx, float dy, float dz,
                        RayIntersection ri)
    {
        float[] result = RayIntersection.createResult();
        float[] ray = {ox, oy, oz, dx, dy, dz};
        int hIntersected;

        hIntersected = _pick3D(handle, mask, ray, result);

        if (hIntersected != 0)
        {
            if (ri != null)
            {
                ri.fill(hIntersected, result);
            }
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean pick(int mask, float x, float y, Camera camera, RayIntersection ri)
    {
        float[] result = RayIntersection.createResult();
        int hIntersected;

        hIntersected = _pick2D(handle, mask, x, y, camera != null ? camera.handle : 0, result);

        if (hIntersected != 0)
        {
            if (ri != null)
            {
                ri.fill(hIntersected, result);
            }
            return true;
        }
        else
        {
            return false;
        }
    }

    //------------------------------------------------------------------
    // Private methods
    //------------------------------------------------------------------

    /**
     * Adds a Java-side child link in this Group.
     */
    private void linkChild(Node child)
    {
        if (child == null)
        {
            throw new Error(); // DEBUG
        }
        if (children == null)
        {
            children = new Vector();
        }
        children.addElement(child);
        child.setParent(this);
    }

    /**
     * Removes a Java-side child link from this Group.
     */
    private void detachChild(Node child)
    {
        if (children != null)
        {
            if (children.removeElement(child))
            {
                /* If no children remain, we delete the array to free some
                 * memory. If a Group is frequently cleared and
                 * re-populated, this should be covered by the free list
                 * used by most VM implementations without causing
                 * significant performance degradation. */
                if (children.isEmpty())
                {
                    children = null;
                }

                child.setParent(null);
            }
        }
    }

    // Native methods
    private static native int _ctor(int hInterface);
    private static native void _addChild(int handle, int hNode);
    private static native void _removeChild(int handle, int hNode);
    private static native int _getChildCount(int handle);
    private static native int _getChild(int handle, int index);
    private static native int _pick3D(int handle, int mask, float[] ray, float[] result);
    private static native int _pick2D(int handle, int mask, float x, float y, int hCamera, float[] result);
}
