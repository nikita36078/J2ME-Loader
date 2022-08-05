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
* Description: RenderQueue implementation
*
*/


/*!
 * \internal
 * \file
 * \brief RenderQueue implementation.
 *
 * Render queue holds
 * meshes and sprites that are added to queue in rendering
 * setup. After setup, queue is committed and contents are
 * rendered. Queue grows dynamically, its initial size is
 * defined in m3g_defs.h.
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

#include "m3g_array.h"

/*----------------------------------------------------------------------
 * Private data structures
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief RenderQueue entry object
 */
struct RenderItemImpl
{
	Node *node;
	Matrix toCamera;
    
    M3Gint subMeshIndex;
	M3Guint sortKey;
};

/*!
 * \internal
 * \brief Drawable queue
 */
struct RenderBucketImpl
{
    PointerArray items;
};


/* Sanity checking... */
M3G_CT_ASSERT(M3G_RENDERQUEUE_BUCKET_BITS >= M3G_APPEARANCE_HARD_SORT_BITS);


/*----------------------------------------------------------------------
 * Private functions
 *--------------------------------------------------------------------*/

static M3G_INLINE RenderBucket *m3gGetRenderBucket(RenderQueue *rq,
                                                   M3Guint sortKey,
                                                   Interface *m3g)
{
    int i = (int)(sortKey >> (32 - M3G_RENDERQUEUE_BUCKET_BITS));
    RenderBucket *bucket = rq->buckets[i];
    
    if (!bucket) {
        bucket = m3gAllocZ(m3g, sizeof(RenderBucket));
        /* array already initialized by zero-allocation; see m3g_array.h */
        rq->buckets[i] = bucket;
    }

    if (i < rq->minBucket) {
        rq->minBucket = i;
    }
    if (i > rq->maxBucket) {
        rq->maxBucket = i;
    }
    
    return bucket;
}

static void m3gDestroyRenderBucket(RenderBucket *bucket, Interface *m3g)
{
    if (bucket) {
        int i;
        PointerArray *items = &bucket->items;
        for (i = 0; i < m3gArraySize(items); ++i) {
            m3gFree(m3g, m3gGetArrayElement(items, i));
        }
        m3gDestroyArray(items, m3g);
    }
    m3gFree(m3g, bucket);
}

static RenderItem *m3gGetRenderItem(RenderQueue *rq, Interface *m3g)
{
    RenderItem *item = rq->freeItems;
    if (!item) {
        m3gIncStat(m3g, M3G_STAT_RENDERQUEUE_SIZE, 1);
        item = m3gAlloc(m3g, sizeof(RenderItem));
    }
    else {
        M3G_VALIDATE_MEMBLOCK(item);
        rq->freeItems = *(RenderItem**)item; /* move to next */
    }
    return item;
}

static void m3gRecycleRenderItem(RenderQueue *rq, RenderItem *item)
{
    if (item) {
        *(RenderItem**)item = rq->freeItems; /* store "next" pointer */
        rq->freeItems = item;
    }
}

static M3Gbool m3gInsertRenderItem(RenderBucket *bucket,
                                   RenderItem *item,
                                   Interface *m3g)
{
    PointerArray *items = &bucket->items;
    int idx = m3gArraySize(items);
    
    /* Do a binary search for the sorting key of the item */
    {
        M3Guint key = item->sortKey;
        int low = 0;
        int high = idx;
        const RenderItem *cmp;

        idx >>= 1;
        
        while (low < high) {
            cmp = (const RenderItem*) m3gGetArrayElement(items, idx);
            
            if (cmp->sortKey < key) {
                low = idx + 1;
            }
            else if (cmp->sortKey > key) {
                high = idx;
            }
            else {
                break;
            }            
            idx = (low + high) >> 1;
        }
    }

    /* Now that we know where to insert, insert; out of memory here
     * returns < 0 */

    return (m3gArrayInsert(items, idx, item, m3g) >= 0);
}


/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Creates a new render queue.
 *
 * \param m3g   M3G interface object
 * \return      RenderQueue object
 */
static RenderQueue *m3gCreateRenderQueue(Interface *m3g)
{
    RenderQueue *rq;
    
    rq = m3gAllocZ(m3g, (M3Gsizei) sizeof(RenderQueue));
    return rq;
}

/*!
 * \internal
 * \brief Destroys a render queue and frees
 * all allocated memory.
 *
 * \param m3g   M3G interface object
 * \param rq    RenderQueue
 */
static void m3gDestroyRenderQueue(Interface *m3g, RenderQueue *rq)
{
    if (rq) {
        int i;
        for (i = 0; i < (1 << M3G_RENDERQUEUE_BUCKET_BITS); ++i) {
            RenderBucket *bucket = rq->buckets[i];
            m3gDestroyRenderBucket(bucket, m3g);
        }
        while (rq->freeItems) {
            RenderItem *item = rq->freeItems;
            rq->freeItems = *(RenderItem**)item;
            m3gFree(m3g, item);
        }
    }
    m3gFree(m3g, rq);
}

/*!
 * \internal
 * \brief Inserts a mesh to the queue. May allocate
 * memory if queue size is exceeded.
 *
 * \param m3g           M3G interface object
 * \param rq            RenderQueue
 * \param mesh          Mesh object
 * \param subMeshIndex  submesh index inside mesh
 * \param sortKey       sorting key
 */
static M3Gbool m3gInsertDrawable(Interface *m3g,
                                 RenderQueue *rq,
                                 Node *node,
                                 const Matrix *toCamera,
                                 M3Gint subMeshIndex,
                                 M3Guint sortKey)
{
    RenderItem *item;
    RenderBucket *bucket;
    
    item = m3gGetRenderItem(rq, m3g);
    if (!item) {
        goto OutOfMemory;
    }

    bucket = m3gGetRenderBucket(rq, sortKey, m3g);
    if (!bucket) {
        goto OutOfMemory;
    }

    item->node         = node;
    item->toCamera     = *toCamera;
    item->subMeshIndex = subMeshIndex;
    item->sortKey      = (sortKey << M3G_RENDERQUEUE_BUCKET_BITS);

    M3G_BEGIN_PROFILE(m3g, M3G_PROFILE_SETUP_SORT);
    if (!m3gInsertRenderItem(bucket, item, m3g)) {
        goto OutOfMemory;
    }
    M3G_END_PROFILE(m3g, M3G_PROFILE_SETUP_SORT);
    return M3G_TRUE;

OutOfMemory:        
    m3gRecycleRenderItem(rq, item);
    return M3G_FALSE;
}

/*!
 * \internal
 * \brief Clears the queue.
 *
 * \param rq            RenderQueue
 */
static void m3gClearRenderQueue(RenderQueue *rq)
{
    rq->root = NULL;
    rq->lightManager = NULL;
    rq->minBucket = (1 << M3G_RENDERQUEUE_BUCKET_BITS);
    rq->maxBucket = 0;
}

/*!
 * \internal
 * \brief Commits the queue by rendering its contents.
 *
 * \param rq            RenderQueue
 * \param ctx           RenderContext object
 */
static void m3gCommit(RenderQueue *rq, RenderContext *ctx)
{
    M3Gint b;

    for (b = rq->minBucket; b <= rq->maxBucket; ++b) {
        if (rq->buckets[b]) {
            PointerArray *items = &rq->buckets[b]->items;
            int n = m3gArraySize(items);
            int i;
            for (i = 0; i < n; ++i) {
                RenderItem *item = (RenderItem*) m3gGetArrayElement(items, i);
                M3G_VFUNC(Node, item->node, doRender)(
                    item->node, ctx, &item->toCamera, item->subMeshIndex);
                m3gRecycleRenderItem(rq, item);
            }
            m3gClearArray(items);
            m3gIncStat(M3G_INTERFACE(ctx), M3G_STAT_RENDER_NODES_DRAWN, n);
        }
    }
}

