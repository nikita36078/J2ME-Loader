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

public abstract class IndexBuffer extends Object3D
{
    /**
     * Only a package private constructor exists for this class.
     */
    IndexBuffer(int handle)
    {
        super(handle);
    }

    public abstract int getIndexCount();
    public abstract void getIndices(int[] indices);
}
