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
* Description: Group interface
*
*/

#ifndef __M3G_GROUP_H__
#define __M3G_GROUP_H__

/*!
 * \internal
 * \file
 * \brief Group interface
 */

#include "m3g_gl.h"
#include "m3g_node.h"

typedef M3Gushort GroupDescendantCount;

struct M3GGroupImpl
{
	Node node;

	Node *firstChild;
    
    AABB *bbox;
    GroupDescendantCount numRenderables, numNonCullables;
};

/*----------------------------------------------------------------------
 * Virtual functions
 *--------------------------------------------------------------------*/
 
static void m3gDestroyGroup(Object *obj);
static M3Gbool m3gGroupAlign(Node *self, const Node *refNode);
static M3Gbool m3gGroupSetupRender(Node *self,
                                   const Node *caller,
                                   SetupRenderState *s,
                                   RenderQueue *renderQueue);
static M3Gint m3gGroupApplyAnimation(Object *self, M3Gint time);
static M3Gbool m3gGroupRayIntersect(	Node *self,
								M3Gint mask,
								M3Gfloat *ray,
								RayIntersection *ri,
								Matrix *toGroup);
static M3Gint m3gGroupDoGetReferences(Object *self, Object **references);
static M3Gint m3gGroupGetBBox(Node *self, AABB *bbox);
static Object *m3gGroupFindID(Object *self, M3Gint userID);
static M3Gbool m3gGroupDuplicate(const Object *original, Object **clone, Object **pairs, M3Gint *numPairs);
static M3Gbool m3gGroupValidate(Node *self, M3Gbitmask stateBits, M3Gint scope);
static void m3gGroupUpdateDuplicateReferences(Node *self, Object **pairs, M3Gint numPairs);

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

static void m3gInitGroup(Interface *m3g,
                         Group *group,
                         M3GClass classID);

#endif /*__M3G_GROUP_H__*/
