/*
* Copyright (c) 2009 Nokia Corporation and/or its subsidiary(-ies).
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
#include "javax_microedition_m3g_Interface.h"


/*!
 * \brief Error handler for the Java interface
 *
 * Converts M3G errors to exceptions and throws them automatically.
 */
static void errorHandler(M3Genum errorCode, M3GInterface /*m3g*/)
{
    CSynchronization::InstanceL()->SetErrorCode(errorCode);
}

/*
static int createInterface(M3Gparams* aCs)
{
    return ((unsigned) m3gCreateInterface(aCs));
}
*/

/*
 * Must be executed in UI thread
 */
JNIEXPORT jlong JNICALL Java_javax_microedition_m3g_Interface__1ctor(JNIEnv* aEnv, jclass)
{
    /*EGLDisplay oldDisplay = eglGetCurrentDisplay();
    EGLSurface oldDrawSurface = eglGetCurrentSurface(EGL_DRAW);
    EGLSurface oldReadSurface = eglGetCurrentSurface(EGL_READ);
    EGLContext oldContext = eglGetCurrentContext();
    EGLenum oldAPI = eglQueryAPI();*/
#if 0
	EGLint config_attribs[] = {
		EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
		EGL_RED_SIZE, 8,
		EGL_GREEN_SIZE, 8,
		EGL_BLUE_SIZE, 8,
		EGL_ALPHA_SIZE, 8,
		EGL_DEPTH_SIZE, 8,
		EGL_STENCIL_SIZE, EGL_DONT_CARE,
		EGL_NONE };
	EGLDisplay display;
	EGLConfig config;
	EGLint num_configs;
	EGLContext context;
	EGLint pbuffer_attribs[] = {
		EGL_WIDTH, 1,
		EGL_HEIGHT, 1,
		EGL_NONE };
	EGLSurface surface;

       	display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
	eglInitialize(display, NULL, NULL);
	//eglBindAPI(EGL_OPENGL_ES_API);
	eglChooseConfig(display, config_attribs, &config, 1, &num_configs);
	context = eglCreateContext(display, config, EGL_NO_CONTEXT, NULL);
	surface = eglCreatePbufferSurface(display, config, pbuffer_attribs);
	//eglMakeCurrent(display, surface, surface, context);

    eglMakeCurrent( display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT );
    //eglBindAPI( EGL_OPENGL_ES_API );
#endif
    
    M3Gparams cs;
    memset(&cs, 0, sizeof(cs));
    cs.mallocFunc = malloc;
    cs.freeFunc   = free;
    cs.errorFunc = errorHandler;

    M3G_DO_LOCK
    /* Call to the Eventserver side */
    //CJavaM3GEventSource* eventSource = JavaUnhand<CJavaM3GEventSource>(aEventSourceHandle);
    //jlong handle = eventSource->Execute(&createInterface, &cs);
    jlong handle = (jlong)m3gCreateInterface(&cs);
    M3G_DO_UNLOCK(aEnv);
    
    //eglMakeCurrent( EGL_DEFAULT_DISPLAY, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT );
    //eglBindAPI( oldAPI );
    //eglMakeCurrent( display, surface, surface, context );
    return handle;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Interface__1getClassID
(JNIEnv* aEnv, jclass, jlong aHObject)
{
    M3G_DO_LOCK
    jint handle = m3gGetClass((M3GObject)aHObject);
    M3G_DO_UNLOCK(aEnv);
    return handle;
}
