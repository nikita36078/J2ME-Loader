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

public class AnimationTrack extends Object3D
{
    //------------------------------------------------------------------
    // Static data
    //------------------------------------------------------------------

    public static final int ALPHA               = 256;
    public static final int AMBIENT_COLOR       = 257;
    public static final int COLOR               = 258;
    public static final int CROP                = 259;
    public static final int DENSITY             = 260;
    public static final int DIFFUSE_COLOR       = 261;
    public static final int EMISSIVE_COLOR      = 262;
    public static final int FAR_DISTANCE        = 263;
    public static final int FIELD_OF_VIEW       = 264;
    public static final int INTENSITY           = 265;
    public static final int MORPH_WEIGHTS       = 266;
    public static final int NEAR_DISTANCE       = 267;
    public static final int ORIENTATION         = 268;
    public static final int PICKABILITY         = 269;
    public static final int SCALE               = 270;
    public static final int SHININESS           = 271;
    public static final int SPECULAR_COLOR      = 272;
    public static final int SPOT_ANGLE          = 273;
    public static final int SPOT_EXPONENT       = 274;
    public static final int TRANSLATION         = 275;
    public static final int VISIBILITY          = 276;

    //------------------------------------------------------------------
    // Instance data
    //------------------------------------------------------------------

    private AnimationController controller;
    private KeyframeSequence sequence;

    //------------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------------

    AnimationTrack(int handle)
    {
        super(handle);
        controller = (AnimationController)getInstance(_getController(handle));
        sequence = (KeyframeSequence)getInstance(_getSequence(handle));
    }

    public AnimationTrack(KeyframeSequence sequence, int property)
    {
        super(_ctor(Interface.getHandle(),
                    sequence != null ? sequence.handle : 0, property));
        this.sequence = sequence;
    }

    //------------------------------------------------------------------
    // Public methods
    //------------------------------------------------------------------

    public void setController(AnimationController controller)
    {
        _setController(handle, controller != null ? controller.handle : 0);
        this.controller = controller;
    }

    public AnimationController getController()
    {
        return controller;
    }

    public KeyframeSequence getKeyframeSequence()
    {
        return sequence;
    }

    public int getTargetProperty()
    {
        return _getTargetProperty(handle);
    }

    //------------------------------------------------------------------
    // Private methods
    //------------------------------------------------------------------

    private native static int _ctor(int hInterface,
                                    int hSequence,
                                    int property);
    private native static int _getController(int handle);
    private native static int _getSequence(int handle);
    private native static int _getTargetProperty(int handle);
    private native static void _setController(int handle, int hController);
}
