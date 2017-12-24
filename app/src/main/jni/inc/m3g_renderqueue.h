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
* Description: Rendering queue interface
*
*/

#ifndef __M3G_RENDERQUEUE_H__
#define __M3G_RENDERQUEUE_H__

/*!
 * \internal
 * \file
 * \brief Rendering queue interface
 */

#include "m3g_appearance.h"
#include "m3g_lightmanager.h"
#include "m3g_node.h"

typedef struct RenderBucketImpl RenderBucket;
typedef struct RenderItemImpl   RenderItem;

/*!
 * \internal
 * \brief RenderQueue object
 */
struct RenderQueueImpl {
    const Node *root;
    
    M3Gint scope;
    
    const Camera *camera;
    LightManager *lightManager;

	RenderBucket *buckets[1 << M3G_RENDERQUEUE_BUCKET_BITS];
    RenderItem *freeItems;

    M3Gint minBucket, maxBucket;
};

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

static RenderQueue *m3gCreateRenderQueue(Interface *m3g);
static void m3gDestroyRenderQueue(Interface *m3g, RenderQueue *rq);
static M3Gbool m3gInsertDrawable(Interface *m3g,
                                 RenderQueue *rq,
                                 Node *node,
                                 const Matrix *toCamera,
                                 M3Gint subMeshIndex,
                                 M3Guint sortKey);
static void m3gClearRenderQueue(RenderQueue *rq);
static void m3gCommit(RenderQueue *rq, RenderContext *ctx);

#endif /*__M3G_RENDERQUEUE_H__*/
