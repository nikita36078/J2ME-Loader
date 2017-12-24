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

public class SkinnedMesh extends Mesh
{
    //------------------------------------------------------------------
    // Instance data
    //------------------------------------------------------------------

    private Group skeleton;

    static private IndexBuffer[] tempTrianglesArray;
    static private Appearance[]  tempAppearanceArray;

    static private IndexBuffer tempTriangles;
    static private Appearance  tempAppearance;


    //------------------------------------------------------------------
    // Constructor(s)
    //------------------------------------------------------------------

    public SkinnedMesh(VertexBuffer vertices,
                       IndexBuffer[] triangles,
                       Appearance[] appearances,
                       Group skeleton)
    {
        super(createHandle(vertices, triangles, appearances, skeleton));
        skeleton.setParent(this);
        this.skeleton = skeleton;
    }

    public SkinnedMesh(VertexBuffer vertices,
                       IndexBuffer triangles,
                       Appearance appearance,
                       Group skeleton)
    {
        super(createHandle(vertices, triangles, appearance, skeleton));
        skeleton.setParent(this);
        this.skeleton = skeleton;
    }

    /**
     */
    SkinnedMesh(int handle)
    {
        super(handle);
        skeleton = (Group) getInstance(_getSkeleton(handle));
    }

    //------------------------------------------------------------------
    // Public methods
    //------------------------------------------------------------------

    public void addTransform(Node bone,
                             int weight,
                             int firstVertex,
                             int numVertices)
    {
        _addTransform(handle,
                      bone != null ? bone.handle : 0,
                      weight,
                      firstVertex,
                      numVertices);
    }

    public Group getSkeleton()
    {
        return skeleton;
    }

    // M3G 1.1 Maintenance release getters

    public void getBoneTransform(Node bone, Transform transform)
    {
        _getBoneTransform(handle, bone.handle, transform.matrix);
    }

    public int getBoneVertices(Node bone, int[] indices, float[] weights)
    {
        return _getBoneVertices(handle, bone.handle, indices, weights);
    }

    //------------------------------------------------------------------
    // Private methods
    //------------------------------------------------------------------

    static int createHandle(VertexBuffer vertices,
                            IndexBuffer[] triangles,
                            Appearance[] appearances,
                            Group skeleton)
    {

        tempTrianglesArray = triangles;
        tempAppearanceArray = appearances;

        verifyParams(vertices, triangles, appearances);

        if (skeleton == null)
        {
            throw new NullPointerException();
        }

        if (skeleton.getParent() != null || skeleton instanceof World)
        {
            throw new IllegalArgumentException();
        }

        int[] hTri = new int[triangles.length];
        int[] hApp = new int[triangles.length];
        for (int i = 0; i < triangles.length; i++)
        {
            hTri[i] = triangles[i].handle;
            if (appearances != null && i < appearances.length)
            {
                hApp[i] = appearances[i] != null ? appearances[i].handle : 0;
            }
        }
        int ret=  _ctor(Interface.getHandle(),
                        vertices.handle,
                        hTri,
                        hApp,
                        skeleton.handle);

        tempTrianglesArray = triangles;
        tempAppearanceArray = appearances;

        return ret;
    }

    static int createHandle(VertexBuffer vertices,
                            IndexBuffer triangles,
                            Appearance appearance,
                            Group skeleton)
    {

        tempTriangles  = triangles;
        tempAppearance = appearance;

        verifyParams(vertices, triangles);

        if (skeleton == null)
        {
            throw new NullPointerException();
        }
        if (skeleton.getParent() != null || skeleton instanceof World)
        {
            throw new IllegalArgumentException();
        }
        int[] hTri = {triangles.handle};
        int[] hApp = {appearance != null ? appearance.handle : 0};
        int ret = _ctor(Interface.getHandle(),
                        vertices.handle,
                        hTri,
                        hApp,
                        skeleton.handle);

        tempTriangles  = null;
        tempAppearance = null;

        return ret;
    }

    // Native methods
    private native static int _ctor(int hInstance,
                                    int hVertices,
                                    int[] hTriangles,
                                    int[] hAppearances,
                                    int hSkeleton);
    private native static void _addTransform(int handle,
            int hBone,
            int weight,
            int firstVertex,
            int numVertices);
    private native static int _getSkeleton(int handle);

    // M3G 1.1 Maintenance release getters
    private native static void _getBoneTransform(int handle,
            int hBone,
            byte[] transform);
    private native static int _getBoneVertices(int handle,
            int hBone,
            int[] indices,
            float[] weights);

}
