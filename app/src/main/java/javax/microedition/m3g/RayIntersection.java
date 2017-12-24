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

public class RayIntersection
{
    private Node intersected = null;
    private float distance = 0.f;
    private int submeshIndex = 0;
    private float[] textureS = new float[Defs.NUM_TEXTURE_UNITS];
    private float[] textureT = new float[Defs.NUM_TEXTURE_UNITS];
    private float[] normal = new float[3];
    private float[] ray = new float[6];

    public RayIntersection()
    {
        normal[0] = 0.f;
        normal[1] = 0.f;
        normal[2] = 1.f;

        ray[0] = 0.f;
        ray[1] = 0.f;
        ray[2] = 0.f;
        ray[3] = 0.f;
        ray[4] = 0.f;
        ray[5] = 1.f;
    }

    public Node getIntersected()
    {
        return intersected;
    }

    public float getDistance()
    {
        return distance;
    }

    public int getSubmeshIndex()
    {
        return submeshIndex;
    }

    public float getTextureS(int index)
    {
        if (index < 0 || index >= textureS.length)
        {
            throw new IndexOutOfBoundsException();
        }

        return textureS[index];
    }

    public float getTextureT(int index)
    {
        if (index < 0 || index >= textureT.length)
        {
            throw new IndexOutOfBoundsException();
        }

        return textureT[index];
    }

    public float getNormalX()
    {
        return normal[0];
    }

    public float getNormalY()
    {
        return normal[1];
    }

    public float getNormalZ()
    {
        return normal[2];
    }

    public void getRay(float[] ray)
    {
        if (ray.length < 6)
        {
            throw new IllegalArgumentException();
        }

        ray[0] = this.ray[0];
        ray[1] = this.ray[1];
        ray[2] = this.ray[2];
        ray[3] = this.ray[3];
        ray[4] = this.ray[4];
        ray[5] = this.ray[5];
    }

    //------------------------------------------------------------------
    // Private methods
    //------------------------------------------------------------------

    static float[] createResult()
    {
        return new float[1 + 1 + 2 * Defs.NUM_TEXTURE_UNITS + 3 + 6];
    }

    void fill(int hIntersected, float[] result)
    {
        intersected = (Node)Object3D.getInstance(hIntersected);
        distance = result[0];
        submeshIndex = (int)result[1];
        textureS[0] = result[2];
        textureS[1] = result[3];
        textureT[0] = result[4];
        textureT[1] = result[5];
        normal[0] = result[6];
        normal[1] = result[7];
        normal[2] = result[8];
        ray[0] = result[9];
        ray[1] = result[10];
        ray[2] = result[11];
        ray[3] = result[12];
        ray[4] = result[13];
        ray[5] = result[14];
    }
}
