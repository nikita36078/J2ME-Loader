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

public class KeyframeSequence extends Object3D
{
    //------------------------------------------------------------------
    // Static data
    //------------------------------------------------------------------

    public static final int LINEAR = 176;
    public static final int SLERP = 177;
    public static final int SPLINE = 178;
    public static final int SQUAD = 179;
    public static final int STEP = 180;

    public static final int CONSTANT = 192;
    public static final int LOOP = 193;

    //------------------------------------------------------------------
    // Constructor
    //------------------------------------------------------------------

    public KeyframeSequence(int numKeyframes,
                            int numComponents,
                            int interpolation)
    {
        super(_ctor(Interface.getHandle(),
                    numKeyframes,
                    numComponents,
                    interpolation));
    }

    /**
     */
    KeyframeSequence(int handle)
    {
        super(handle);
    }

    //------------------------------------------------------------------
    // Public methods
    //------------------------------------------------------------------

    public void setKeyframe(int index, int time, float[] value)
    {
        _setKeyframe(handle, index, time, value);
    }

    public void setValidRange(int first, int last)
    {
        _setValidRange(handle, first, last);
    }

    public void setDuration(int duration)
    {
        _setDuration(handle, duration);
    }

    public int getDuration()
    {
        return _getDuration(handle);
    }

    /**
     */
    public void setRepeatMode(int mode)
    {
        _setRepeatMode(handle, mode);
    }

    /**
     */
    public int getRepeatMode()
    {
        return _getRepeatMode(handle);
    }

    // M3G 1.1 Maintenance release getters

    public int getComponentCount()
    {
        return _getComponentCount(handle);
    }

    public int getInterpolationType()
    {
        return _getInterpolationType(handle);
    }

    public int getKeyframe(int index, float[] value)
    {
        return _getKeyframe(handle, index, value);
    }

    public int getKeyframeCount()
    {
        return _getKeyframeCount(handle);
    }

    public int getValidRangeFirst()
    {
        return _getValidRangeFirst(handle);
    }

    public int getValidRangeLast()
    {
        return _getValidRangeLast(handle);
    }


    //------------------------------------------------------------------
    // Private methods
    //------------------------------------------------------------------

    private native static int _ctor(int hInterface,
                                    int numKeyframes,
                                    int numComponents,
                                    int interpolation);
    private native static void _setValidRange(int handle, int first, int last);
    private native static void _setKeyframe(int handle, int index, int time, float[] value);
    private native static void _setDuration(int handle, int duration);
    private native static int _getDuration(int handle);
    private native static void _setRepeatMode(int handle, int mode);
    private native static int _getRepeatMode(int handle);

    // M3G 1.1 Maintenance release getters
    private native static int _getComponentCount(int handle);
    private native static int _getInterpolationType(int handle);
    private native static int _getKeyframe(int handle, int index, float[] value);
    private native static int _getKeyframeCount(int handle);
    private native static int _getValidRangeFirst(int handle);
    private native static int _getValidRangeLast(int handle);
}
