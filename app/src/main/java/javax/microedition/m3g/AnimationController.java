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

public class AnimationController extends Object3D
{
    //------------------------------------------------------------------
    // Constructor
    //------------------------------------------------------------------

    public AnimationController()
    {
        super(_ctor(Interface.getHandle()));
    }

    /**
     */
    AnimationController(int handle)
    {
        super(handle);
    }

    //------------------------------------------------------------------
    // Public methods
    //------------------------------------------------------------------

    public void setActiveInterval(int worldTimeMin, int worldTimeMax)
    {
        _setActiveInterval(handle, worldTimeMin, worldTimeMax);
    }

    public int getActiveIntervalStart()
    {
        return _getActiveIntervalStart(handle);
    }

    public int getActiveIntervalEnd()
    {
        return _getActiveIntervalEnd(handle);
    }

    public void setSpeed(float factor, int worldTime)
    {
        _setSpeed(handle, factor, worldTime);
    }

    public float getSpeed()
    {
        return _getSpeed(handle);
    }

    public void setPosition(float time, int worldTime)
    {
        _setPosition(handle, time, worldTime);
    }

    public float getPosition(int worldTime)
    {
        return _getPosition(handle, worldTime);
    }

    public void setWeight(float weight)
    {
        _setWeight(handle, weight);
    }

    public float getWeight()
    {
        return _getWeight(handle);
    }

    // M3G maintenance version 1.1
    public int getRefWorldTime()
    {
        return _getRefWorldTime(handle);
    }


    // Native methods
    private native static int _ctor(int hInterface);
    private native static void _setActiveInterval(int handle, int worldTimeMin, int worldTimeMax);
    private native static int _getActiveIntervalStart(int handle);
    private native static int _getActiveIntervalEnd(int handle);
    private native static void _setSpeed(int handle, float factor, int worldTime);
    private native static float _getSpeed(int handle);
    private native static void _setPosition(int handle, float time, int worldTime);
    private native static float _getPosition(int handle, int worldTime);
    private native static void _setWeight(int handle, float weight);
    private native static float _getWeight(int handle);

    // M3G maintenance version 1.1
    private native static int _getRefWorldTime(int handle);

}
