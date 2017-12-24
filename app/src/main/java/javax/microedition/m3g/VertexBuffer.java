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

public class VertexBuffer extends Object3D
{
    //------------------------------------------------------------------
    // Instance data
    //------------------------------------------------------------------

    private VertexArray   positions;
    private VertexArray   normals;
    private VertexArray   colors;
    private VertexArray[] texCoords;

    //------------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------------

    public VertexBuffer()
    {
        super(_ctor(Interface.getHandle()));
    }

    /**
     */
    VertexBuffer(int handle)
    {
        super(handle);

        positions = (VertexArray) getInstance(_getArray(handle, Defs.GET_POSITIONS, null));
        normals   = (VertexArray) getInstance(_getArray(handle, Defs.GET_NORMALS, null));
        colors    = (VertexArray) getInstance(_getArray(handle, Defs.GET_COLORS, null));

        texCoords = new VertexArray[Defs.NUM_TEXTURE_UNITS];
        for (int i = 0; i < Defs.NUM_TEXTURE_UNITS; ++i)
        {
            texCoords[i] =
                (VertexArray) getInstance(_getArray(handle, Defs.GET_TEXCOORDS0 + i, null));
        }
    }

    //------------------------------------------------------------------
    // Public methods
    //------------------------------------------------------------------

    public int getVertexCount()
    {
        return _getVertexCount(handle);
    }

    public void setPositions(VertexArray positions, float scale, float[] bias)
    {
        _setVertices(handle,
                     (positions != null) ? positions.handle : 0,
                     scale,
                     bias);
        this.positions = positions;
    }

    public void setTexCoords(int index, VertexArray texCoords, float scale, float[] bias)
    {
        _setTexCoords(handle,
                      index,
                      texCoords != null ? texCoords.handle : 0,
                      scale,
                      bias);

        if (this.texCoords == null)
        {
            this.texCoords = new VertexArray[Defs.NUM_TEXTURE_UNITS];
        }
        this.texCoords[index] = texCoords;
    }

    public void setNormals(VertexArray normals)
    {
        _setNormals(handle, normals != null ? normals.handle : 0);
        this.normals = normals;
    }

    public void setColors(VertexArray colors)
    {
        _setColors(handle, colors != null ? colors.handle : 0);
        this.colors = colors;
    }

    public VertexArray getPositions(float[] scaleBias)
    {
        /* Get scale and bias with native getter */
        _getArray(handle, Defs.GET_POSITIONS, scaleBias);
        return positions;
    }

    public VertexArray getTexCoords(int index, float[] scaleBias)
    {
        /* Index has to be checked here due to the native getter input params */
        if (index < 0 || index >= Defs.NUM_TEXTURE_UNITS)
        {
            throw new IndexOutOfBoundsException();
        }

        /* Get scale and bias with native getter */
        _getArray(handle, Defs.GET_TEXCOORDS0 + index, scaleBias);
        return texCoords != null ? texCoords[index] : null;
    }

    public VertexArray getNormals()
    {
        return normals;
    }

    public VertexArray getColors()
    {
        return colors;
    }

    public void setDefaultColor(int ARGB)
    {
        _setDefaultColor(handle, ARGB);
    }

    public int getDefaultColor()
    {
        return _getDefaultColor(handle);
    }

    //------------------------------------------------------------------
    // Private methods
    //------------------------------------------------------------------

    // Native methods
    private static native int _ctor(int hInterface);
    private static native void _setColors(int hBuffer, int hArray);
    private static native void _setNormals(int hBuffer, int hArray);
    private static native void _setTexCoords(int hBuffer, int unit, int hArray, float scale, float[] bias);
    private static native void _setVertices(int hBuffer, int hArray, float scale, float[] bias);
    private static native void _setDefaultColor(int hBuffer, int ARGB);
    private static native int  _getDefaultColor(int hBuffer);
    private static native int  _getArray(int hBuffer, int which, float[] scaleBias);
    private static native int  _getVertexCount(int hBuffer);
}
