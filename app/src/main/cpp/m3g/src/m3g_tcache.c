/*
* Copyright (c) 2005 Nokia Corporation and/or its subsidiary(-ies).
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
* Description: Transformation cache implementation
*
*/


/*!
 * \internal
 * \file
 * \brief Transformation cache implementation
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

#include "m3g_tcache.h"

/* NOTE size MUST be a power of two! */
#define TCACHE_COMPOSITES 128
#define TCACHE_PATHS 128

/*----------------------------------------------------------------------
 * Data structures
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Transformation path cache element
 */
typedef struct
{
    M3Gfloat elem[12];  /* NOTE the bottom row is always 0 0 0 1 */
    M3Guint mask;
    M3Guint classified  : 1;
    M3Guint complete    : 1;
    
    const Node *from, *to;
} TCachePath;

M3G_CT_ASSERT2(sizeof(TCachePath) == 64);

/*!
 * \internal
 * \brief Transformation cache data implementation
 */
struct TCacheImpl
{
    Interface *m3g;

    TCachePath paths[TCACHE_PATHS];
    Matrix composites[TCACHE_COMPOSITES];
    const Transformable *compositeObjs[TCACHE_COMPOSITES];
    
    M3Gbool pathsInvalid;
};

/*----------------------------------------------------------------------
 * Private functions
 *--------------------------------------------------------------------*/

static M3G_INLINE M3Gint m3gTransformableHash(const Transformable *t)
{
    M3Guint a = (M3Guint) t;
    M3Guint b = (M3Guint) t;
    
    a += (a >> 3) + (a >> 9) + (a >> 17);
    b  = (b >> 16) | (b << 16);
    b += (b >> 5) + (b >> 10) + (b >> 20);
    return (M3Gint)(a ^ b);
}

static M3Gint m3gPathHash(const Node *from, const Node *to)
{
    M3Guint a = (M3Guint) from;
    M3Guint b = (M3Guint) to;

    a += (a >> 3) + (a >> 9) + (a >> 17);
    b  = (b >> 16) | (b << 16);
    b += (b >> 5) + (b >> 10) + (b >> 20);
    return (M3Gint)(a ^ b);
}

static M3G_INLINE M3Gint m3gGetTransformableSlot(const TCache *tc, const Transformable *t)
{
    M3G_UNREF(tc);
    return m3gTransformableHash(t) & (TCACHE_COMPOSITES - 1);
}

static M3G_INLINE M3Gint m3gGetPathSlot(const TCache *tc, const Node *from, const Node *to)
{
    M3G_UNREF(tc);
    return m3gPathHash(from, to) & (TCACHE_PATHS - 1);
}
    
/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief
 */
static TCache* m3gCreateTransformCache(Interface *m3g)
{
    TCache *cache = m3gAllocZ(m3g, sizeof(TCache));
    if (cache) {
        cache->m3g = m3g;
        cache->pathsInvalid = M3G_TRUE;
    }
    return cache;
}

/*!
 * \internal
 * \brief
 */
static void m3gDeleteTransformCache(TCache *cache)
{
    if (cache) {
        m3gFree(cache->m3g, cache);
    }
}

/*!
 * \internal
 * \brief
 */
static void m3gCacheComposite(TCache *cache, const Transformable *t, const Matrix *m)
{
    M3Gint idx = m3gGetTransformableSlot(cache, t);

    /* If the matrix being added already exists in the cache, the
     * cache is being used sub-optimally */
    
    M3G_ASSERT(cache->compositeObjs[idx] != t);
    
    /* Just overwrite any existing entries, but keep track of
     * collisions for profiling */
    
    if (cache->compositeObjs[idx]) {
        m3gIncStat(cache->m3g, M3G_STAT_TCACHE_COMPOSITE_COLLISIONS, 1);
    }
    else {
        m3gIncStat(cache->m3g, M3G_STAT_TCACHE_COMPOSITE_LOAD, 1);
    }
    cache->composites[idx] = *m;
    cache->compositeObjs[idx] = t;
    
    m3gIncStat(cache->m3g, M3G_STAT_TCACHE_COMPOSITE_INSERTS, 1);
}

/*!
 * \internal
 * \brief
 */
static void m3gCachePath(TCache *cache, const Node *from, const Node *to, const Matrix *m)
{
    M3G_ASSERT(m3gIsWUnity(m));
    /* These two asserts are not errors in a strict sense, but imply
     * sub-optimal cache use */
    M3G_ASSERT(from || to);
    M3G_ASSERT(from != to);
    
    M3G_BEGIN_PROFILE(cache->m3g, M3G_PROFILE_TCACHE);
    
    /* If the cache has been invalidated, wipe it clean before
     * inserting anything */
    
    if (cache->pathsInvalid) {
        m3gIncStat(cache->m3g, M3G_STAT_TCACHE_PATH_FLUSHES, 1);
        m3gZero(cache->paths, TCACHE_PATHS * sizeof(cache->paths[0]));
        cache->pathsInvalid = M3G_FALSE;
    }

    /* Hash to the cache, then just overwrite anything previously
     * there */
    {
        M3Gint idx = m3gGetPathSlot(cache, from, to);
        TCachePath *c = &cache->paths[idx];

        /* If this assert is hit, the path being added already exists
         * in the cache; this is not an error per se, but implies
         * sub-optimal cache usage */
        M3G_ASSERT(c->from != from || c->to != to);
        
        /* Overwrite the matrix data */
        {
            const M3Gfloat *src = &m->elem[0];
            M3Gfloat *dst = &c->elem[0];
            int row, col;
            for (col = 0; col < 4; ++col) {
                for (row = 0; row < 3; ++row) {
                    *dst++ = *src++;
                }
                ++src;
            }
            c->mask = m->mask;
            c->classified = m->classified;
            c->complete = m->complete;
        }

        /* Register collisions for bookkeeping */

        if (c->from || c->to) {
            m3gIncStat(cache->m3g, M3G_STAT_TCACHE_PATH_COLLISIONS, 1);
        }
        else {
            m3gIncStat(cache->m3g, M3G_STAT_TCACHE_PATH_LOAD, 1);
        }
        
        c->from = from;
        c->to = to;
    }
    M3G_END_PROFILE(cache->m3g, M3G_PROFILE_TCACHE);
    
    m3gIncStat(cache->m3g, M3G_STAT_TCACHE_PATH_INSERTS, 1);
}

/*!
 * \internal
 * \brief
 */
static M3Gbool m3gGetCachedComposite(const TCache *cache, const Transformable *t, Matrix *m)
{
    M3Gint idx = m3gGetTransformableSlot(cache, t);
    if (cache->compositeObjs[idx] == t) {
        *m = cache->composites[idx];
        m3gIncStat(cache->m3g, M3G_STAT_TCACHE_COMPOSITE_HITS, 1);
        return M3G_TRUE;
    }
    m3gIncStat(cache->m3g, M3G_STAT_TCACHE_COMPOSITE_MISSES, 1);
    return M3G_FALSE;
}

/*!
 * \internal
 * \brief
 */
static M3Gbool m3gGetCachedPath(const TCache *cache, const Node *from, const Node *to, Matrix *m)
{
    M3G_BEGIN_PROFILE(cache->m3g, M3G_PROFILE_TCACHE);
    
    if (!cache->pathsInvalid) {

        /* Hash to the respective cache slot */
        
        M3Gint idx = m3gGetPathSlot(cache, from, to);
        const TCachePath *c = &cache->paths[idx];

        /* If it matches, copy to the output matrix */
        
        if (c->from == from && c->to == to) {
            const M3Gfloat *src = &c->elem[0];
            M3Gfloat *dst = &m->elem[0];
            int col;
            for (col = 0; col < 4; ++col) {
                *dst++ = *src++;
                *dst++ = *src++;
                *dst++ = *src++;
                *dst++ = 0.0f;
            }
            m->elem[15] = 1.0f;
            m->mask = c->mask;
            m->classified = c->classified;
            m->complete = c->complete;

            M3G_END_PROFILE(cache->m3g, M3G_PROFILE_TCACHE);
    
            m3gIncStat(cache->m3g, M3G_STAT_TCACHE_PATH_HITS, 1);
            return M3G_TRUE;
        }
    }
    M3G_END_PROFILE(cache->m3g, M3G_PROFILE_TCACHE);
    
    m3gIncStat(cache->m3g, M3G_STAT_TCACHE_PATH_MISSES, 1);
    return M3G_FALSE;
}

/*!
 * \internal
 * \brief
 */
static void m3gInvalidateCachedPaths(TCache *cache, const Node *n)
{
    M3G_UNREF(n);
    m3gResetStat(cache->m3g, M3G_STAT_TCACHE_PATH_LOAD);
    cache->pathsInvalid = M3G_TRUE;
}

/*!
 * \internal
 * \brief
 */
static void m3gInvalidateCachedTransforms(TCache *cache, const Transformable *t)
{
    /* Look for a composite entry and wipe if found */
    {
        M3Gint idx = m3gGetTransformableSlot(cache, t);
        if (cache->compositeObjs[idx] == t) {
            cache->compositeObjs[idx] = NULL;
            m3gIncStat(cache->m3g, M3G_STAT_TCACHE_COMPOSITE_LOAD, -1);
        }
    }
    
    /* NOTE We just cast the pointer to a Node below -- it's only used
     * for hashing, and every object will still be unique regardless
     * of the class */
    
    m3gInvalidateCachedPaths(cache, (const Node*) t);
}
