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

public class VertexArray extends Object3D
{
    //------------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------------

    public VertexArray(int numVertices, int numComponents, int componentSize)
    {
        super(createHandle(numVertices,
                           numComponents,
                           componentSize));
    }

    /**
     */
    VertexArray(int handle)
    {
        super(handle);
    }

    //------------------------------------------------------------------
    // Public methods
    //------------------------------------------------------------------

    public void set(int startIndex, int length, short[] values)
    {
        _setShort(handle, startIndex, length, values);
    }

    public void set(int startIndex, int length, byte[] values)
    {
        _setByte(handle, startIndex, length, values);
    }

    // M3G 1.1 Maintenance release getters
    public void get(int firstVertex, int numVertices, byte[] values)
    {
        _getByte(handle, firstVertex, numVertices, values);
    }

    public void get(int firstVertex, int numVertices, short[] values)
    {
        _getShort(handle, firstVertex, numVertices, values);
    }

    public int getComponentCount()
    {
        return _getComponentCount(handle);
    }

    public int getComponentType()
    {
        return _getComponentType(handle);
    }

    public int getVertexCount()
    {
        return _getVertexCount(handle);
    }

    //------------------------------------------------------------------
    // Private methods
    //------------------------------------------------------------------

    private static int createHandle(int numVertices, int numComponents, int componentSize)
    {
        Platform.heuristicGC();
        return  _ctor(Interface.getHandle(),
                      numVertices,
                      numComponents,
                      componentSize);
    }

    // Native methods
    private native static int _ctor(int hInterface,
                                    int numVertices,
                                    int numComponents,
                                    int componentSize);
    private native static void _setByte(int handle,
                                        int first,
                                        int count,
                                        byte[] src);
    private native static void _setShort(int handle,
                                         int first,
                                         int count,
                                         short[] src);

    // M3G 1.1 Maintenance release getters
    private native static void _getByte(int handle,
                                        int firstVertex,
                                        int numVertices,
                                        byte[] values);
    private native static void _getShort(int handle,
                                         int firstVertex,
                                         int numVertices,
                                         short[] values);
    private native static int _getComponentCount(int handle);
    private native static int _getComponentType(int handle);
    private native static int _getVertexCount(int handle);
}
