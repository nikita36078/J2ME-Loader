/*
* Copyright (c) 2005-2006 Nokia Corporation and/or its subsidiary(-ies).
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
* Description:  M3GCore function call synchronization for J9
*
*/

// INCLUDE FILES
#include "CSynchronization.hpp"

class M3gGlobals
{
public:
    M3gGlobals() : mSync(0) {}

public:
    CSynchronization* mSync;
};

#if defined(__WINSCW__)

#include <pls.h>
M3gGlobals* getM3gGlobals()
{
    // Access the PLS of this process.
    return Pls<M3gGlobals>(TUid::Uid(0x200211E2));
}

#else

static M3gGlobals* sGlobals = 0;

M3gGlobals* getM3gGlobals()
{
    if (sGlobals == 0)
    {
        sGlobals = new M3gGlobals();
    }
    return sGlobals;
}
#endif


// STATIC MEMBERS
/*static*/ //CSynchronization* CSynchronization::iSelf = NULL;

// -----------------------------------------------------------------------------
// CSynchronization::InstanceL
// -----------------------------------------------------------------------------
/*static*/ CSynchronization* CSynchronization::InstanceL()
{
    static M3gGlobals* globals = getM3gGlobals();
    if (!globals->mSync)
    {
        globals->mSync = CSynchronization::NewL();
    }
    return globals->mSync;
}

// -----------------------------------------------------------------------------
// CSynchronization::NewL
// -----------------------------------------------------------------------------
/*static*/ CSynchronization* CSynchronization::NewL()
{
    CSynchronization* self = new /*(ELeave)*/ CSynchronization();
    //CleanupStack::PushL(self);
    self->ConstructL();
    //CleanupStack::Pop();
    return self;
}

// -----------------------------------------------------------------------------
// CSynchronization::ConstructL
// -----------------------------------------------------------------------------
void CSynchronization::ConstructL()
{
    //iGuard.CreateLocal();
#ifdef DO_SOMETHING
    pthread_mutex_init(&iGuard, NULL);
#endif
}

// -----------------------------------------------------------------------------
// CSynchronization::CSynchronization
// -----------------------------------------------------------------------------
CSynchronization::CSynchronization() : iErrorCode(0)
{
}

// -----------------------------------------------------------------------------
// CSynchronization::~CSynchronization
// -----------------------------------------------------------------------------
CSynchronization::~CSynchronization()
{
    //iGuard.Close();
}

// -----------------------------------------------------------------------------
// CSynchronization::Lock
// -----------------------------------------------------------------------------
void CSynchronization::Lock()
{
    //iGuard.Wait();
#ifdef DO_SOMETHING
    pthread_mutex_lock(&iGuard);
#endif
    iErrorCode = 0;
}

// -----------------------------------------------------------------------------
// CSynchronization::Unlock
// -----------------------------------------------------------------------------
void CSynchronization::Unlock()
{
    iErrorCode = 0;
    //iGuard.Signal();
#ifdef DO_SOMETHING
    pthread_mutex_unlock(&iGuard);
#endif
}

// -----------------------------------------------------------------------------
// CSynchronization::SetErrorCode
// -----------------------------------------------------------------------------
void CSynchronization::SetErrorCode(int aCode)
{
    iErrorCode = aCode;
}

// -----------------------------------------------------------------------------
// CSynchronization::GetErrorCode
// -----------------------------------------------------------------------------
int CSynchronization::GetErrorCode()
{
    return iErrorCode;
}
