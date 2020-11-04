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

class Defs {
	static boolean supportDithering = false;
	static boolean supportTrueColor = false;
	static boolean supportAntialiasing = true;
	static boolean supportMipmapping = true;
	static boolean supportPerspectiveCorrection = true;
	static boolean supportLocalCameraLighting = false;

	static int MAX_LIGHTS = 8;
	static int MAX_TEXTURE_DIMENSION = 4096;
	static int MAX_TRANSFORMS_PER_VERTEX = 4;
	static int MAX_VIEWPORT_WIDTH = 4096;
	static int MAX_VIEWPORT_HEIGHT = 4096;
	static int MAX_VIEWPORT_DIMENSION = 4096;
	static int NUM_TEXTURE_UNITS = 2;

	/* Constants used in various setters/getters */

	/* VertexBuffer */
	static final int GET_POSITIONS = 0;
	static final int GET_NORMALS = 1;
	static final int GET_COLORS = 2;
	static final int GET_TEXCOORDS0 = 3;
	/*               GET_TEXCOORDS1    = 4 */
	/* Sprite and Background */
	static final int GET_CROPX = 0;
	static final int GET_CROPY = 1;
	static final int GET_CROPWIDTH = 2;
	static final int GET_CROPHEIGHT = 3;
	/* Background */
	static final int GET_MODEX = 0;
	static final int GET_MODEY = 1;
	static final int SETGET_COLORCLEAR = 0;
	static final int SETGET_DEPTHCLEAR = 1;
	/* Fog */
	static final int GET_NEAR = 0;
	static final int GET_FAR = 1;
	/* Node */
	static final int SETGET_RENDERING = 0;
	static final int SETGET_PICKING = 1;
	/* Light */
	static final int GET_CONSTANT = 0;
	static final int GET_LINEAR = 1;
	static final int GET_QUADRATIC = 2;
}
