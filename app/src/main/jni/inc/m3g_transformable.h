/*
* Copyright (c) 2003 Nokia Corporation and/or its subsidiary(-ies).
* All rights reserved.
* This component and the accompanying materials are made available
* under the terms of the License "Eclipse Public License v1.0"
* which accompanies this distribution, and is available
* at the URL "http://www.eclipse.org/legal/epl-v10.html".
*
* Initial Contributors:
* Nokia Corporation - initial contribution.
*
* Contributors:
*
* Description: Transformable interface
*
*/

#ifndef __M3G_TRANSFORMABLE_H__
#define __M3G_TRANSFORMABLE_H__

/*!
 * \internal
 * \file
 * \brief Transformable interface
 */

#include "m3g_object.h"

/*----------------------------------------------------------------------
 * Class definition
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Transformable class virtual functions
 *
 * \note This is currently empty, but may find use in the future, so
 * we're retaining it for the time being.
 */
typedef struct
{
	ObjectVFTable object;
} TransformableVFTable;

/*!
 * \internal
 * \brief Transformable object instance data
 */
struct M3GTransformableImpl
{
    Object object;
    
	Quat orientation;
	M3Gfloat sx, sy, sz;
	M3Gfloat tx, ty, tz;
	Matrix *matrix;
};

/* Sanity check; if this assert fires, check the compiler padding
 * settings */

M3G_CT_ASSERT(sizeof(Transformable) == sizeof(Object) + 44);


/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

static void     m3gInitTransformable    (Transformable *tf, Interface *m3g, M3GClass classID);
static void     m3gDestroyTransformable (Object *obj);

static M3Gbool  m3gGetInverseCompositeTransform (M3GTransformable handle,
                                                 M3GMatrix *transform);
static M3Gbool  m3gTransformableIsCompatible    (M3Gint property);
static void     m3gTransformableUpdateProperty  (Object *self, M3Gint property, M3Gint valueSize, const M3Gfloat *value);
static M3Gbool  m3gTransformableDuplicate       (const Object *original, Object **clone, Object **pairs, M3Gint *numPairs);

static void m3gInvalidateTransformable(Transformable *self);

#endif /*__M3G_TRANSFORMABLE_H__*/
