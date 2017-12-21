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

public class TriangleStripArray extends IndexBuffer
{
    //------------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------------

    public TriangleStripArray(int firstIndex, final int[] stripLengths)
    {
        super(_createImplicit(Interface.getHandle(),
                              firstIndex,
                              stripLengths));
    }

    public TriangleStripArray(final int[] indices, final int[] stripLengths)
    {
        super(_createExplicit(Interface.getHandle(), indices, stripLengths));
    }

    TriangleStripArray(int handle)
    {
        super(handle);
    }

    // M3G 1.1 Maintenance release getters

    public int getIndexCount()
    {
        return _getIndexCount(handle);
    }

    public void getIndices(int[] indices)
    {
        if (indices.length < _getIndexCount(handle))
        {
            throw new IllegalArgumentException();
        }
        _getIndices(handle, indices);
    }

    //------------------------------------------------------------------
    // Private methods
    //------------------------------------------------------------------

    // Native methods
    private native static int _createImplicit(int hInterface,
            int first,
            final int[] lengths);
    private native static int _createExplicit(int hInterface,
            final int[] indices,
            final int[] lengths);

    // M3G 1.1 Maintenance release getters
    private native static int _getIndexCount(int handle);
    private native static void _getIndices(int handle, int[] indices);

}
