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
* Description: Transformation cache interface
*
*/

#ifndef __M3G_TCACHE_H__
#define __M3G_TCACHE_H__

/*!
 * \internal
 * \file
 * \brief Transformation cache interface
 */

typedef struct TCacheImpl TCache;

#include "m3g_node.h"

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

static  TCache* m3gCreateTransformCache (Interface *m3g);
static  void    m3gDeleteTransformCache (TCache *cache);

static  void    m3gCacheComposite       (TCache *cache, const Transformable *t, const Matrix *m);
static  void    m3gCachePath            (TCache *cache, const Node *from, const Node *to, const Matrix *m);

static  M3Gbool m3gGetCachedComposite   (const TCache *cache, const Transformable *t, Matrix *m);
static  M3Gbool m3gGetCachedPath        (const TCache *cache, const Node *from, const Node *to, Matrix *m);

static  void m3gInvalidateCachedPaths(TCache *cache, const Node *n);
static  void m3gInvalidateCachedTransforms(TCache *cache, const Transformable *t);


#endif /*__M3G_TCACHE_H__*/
