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
* Description: M3G core API public interface
*
*/

#ifndef __M3G_CORE_H__
#define __M3G_CORE_H__

#if defined(__cplusplus)
extern "C" {
#endif

/*!
 * \file
 * \brief Public interface to the M3G core library
 *
 * This can be overlaid by an additional layer providing the actual
 * API for different languages; for the JSR184 API, this is the Java
 * binding layer.
 */

#if (__ARMCC_VERSION >= 220000)
#   if defined(M3G_BUILD_DLL)
#       define M3G_API __declspec(dllexport)
#   else
#       define M3G_API __declspec(dllimport)
#   endif
#else
#   if defined(M3G_BUILD_DLL)
#       define M3G_API __declspec(dllexport)
#   else
#       define M3G_API
#   endif
#endif
    
/*----------------------------------------------------------------------
 * Enumerated constants
 *--------------------------------------------------------------------*/

/*javax.microedition.m3g.AnimationTrack*/
#define M3G_ANIM_ALPHA          256
#define M3G_ANIM_AMBIENT_COLOR  257
#define M3G_ANIM_COLOR          258
#define M3G_ANIM_CROP           259
#define M3G_ANIM_DENSITY        260
#define M3G_ANIM_DIFFUSE_COLOR  261
#define M3G_ANIM_EMISSIVE_COLOR 262
#define M3G_ANIM_FAR_DISTANCE   263
#define M3G_ANIM_FIELD_OF_VIEW  264
#define M3G_ANIM_INTENSITY      265
#define M3G_ANIM_MORPH_WEIGHTS  266
#define M3G_ANIM_NEAR_DISTANCE  267
#define M3G_ANIM_ORIENTATION    268
#define M3G_ANIM_PICKABILITY    269
#define M3G_ANIM_SCALE          270
#define M3G_ANIM_SHININESS      271
#define M3G_ANIM_SPECULAR_COLOR 272
#define M3G_ANIM_SPOT_ANGLE     273
#define M3G_ANIM_SPOT_EXPONENT  274
#define M3G_ANIM_TRANSLATION    275
#define M3G_ANIM_VISIBILITY     276

/*javax.microedition.m3g.Background*/
#define M3G_BORDER              32
#define M3G_REPEAT              33

/*javax.microedition.m3g.Camera*/
#define M3G_GENERIC             48
#define M3G_PARALLEL            49
#define M3G_PERSPECTIVE         50

/*javax.microedition.m3g.CompositingMode*/
#define M3G_ALPHA_BLEND         64
#define M3G_ALPHA_ADD           65
#define M3G_MODULATE            66
#define M3G_MODULATE_X2         67
#define M3G_REPLACE             68

/*javax.microedition.m3g.Fog*/
#define M3G_EXPONENTIAL_FOG     80
#define M3G_LINEAR_FOG          81

/*javax.microedition.m3g.Graphics3D*/
#define M3G_ANTIALIAS_BIT       2
#define M3G_DITHER_BIT          4
#define M3G_TRUECOLOR_BIT       8
#define M3G_OVERWRITE_BIT       16

/*javax.microedition.m3g.Image2D*/
typedef enum 
{
    M3G_ALPHA           = 96,
    M3G_LUMINANCE       = 97,
    M3G_LUMINANCE_ALPHA = 98,
    M3G_RGB             = 99,
    M3G_RGBA            = 100
} M3GImageFormat;

/*javax.microedition.m3g.KeyframeSequence*/
#define M3G_LINEAR              176
#define M3G_SLERP               177
#define M3G_SPLINE              178
#define M3G_SQUAD               179
#define M3G_STEP                180
#define M3G_CONSTANT            192
#define M3G_LOOP                193

/*javax.microedition.m3g.Light*/
#define M3G_AMBIENT             128
#define M3G_DIRECTIONAL         129
#define M3G_OMNI                130
#define M3G_SPOT                131

/*javax.microedition.m3g.Material*/
#define M3G_AMBIENT_BIT         1024
#define M3G_DIFFUSE_BIT         2048
#define M3G_EMISSIVE_BIT        4096
#define M3G_SPECULAR_BIT        8192

/*javax.microedition.m3g.Node*/
#define M3G_NONE                144
#define M3G_ORIGIN              145
#define M3G_X_AXIS              146
#define M3G_Y_AXIS              147
#define M3G_Z_AXIS              148

/*javax.microedition.m3g.Object3D*/
typedef enum {    
    M3G_CLASS_ANIMATION_CONTROLLER = 1,
    M3G_CLASS_ANIMATION_TRACK      = 2,
    M3G_CLASS_APPEARANCE           = 3,
    M3G_CLASS_BACKGROUND           = 4,
    M3G_CLASS_CAMERA               = 5,
    M3G_CLASS_COMPOSITING_MODE     = 6,
    M3G_CLASS_FOG                  = 7,
    M3G_CLASS_GROUP                = 8,
    M3G_CLASS_IMAGE                = 9,
    M3G_CLASS_INDEX_BUFFER         = 10,
    M3G_CLASS_KEYFRAME_SEQUENCE    = 11,
    M3G_CLASS_LIGHT                = 12,
    M3G_CLASS_LOADER               = 13,
    M3G_CLASS_MATERIAL             = 14,
    M3G_CLASS_MESH                 = 15,
    M3G_CLASS_MORPHING_MESH        = 16,
    M3G_CLASS_POLYGON_MODE         = 17,
    M3G_CLASS_RENDER_CONTEXT       = 18,
    M3G_CLASS_SKINNED_MESH         = 19,
    M3G_CLASS_SPRITE               = 20,
    M3G_CLASS_TEXTURE              = 21,
    M3G_CLASS_VERTEX_ARRAY         = 22,
    M3G_CLASS_VERTEX_BUFFER        = 23,
    M3G_CLASS_WORLD                = 24,
    /* extra enumeration used for abstract classes */
    M3G_ABSTRACT_CLASS             = 0
} M3GClass;
    
/*javax.microedition.m3g.PolygonMode*/
#define M3G_CULL_BACK           160
#define M3G_CULL_FRONT          161
#define M3G_CULL_NONE           162
#define M3G_SHADE_FLAT          164
#define M3G_SHADE_SMOOTH        165
#define M3G_WINDING_CCW         168
#define M3G_WINDING_CW          169

/*javax.microedition.m3g.Texture2D*/
#define M3G_FILTER_BASE_LEVEL   208
#define M3G_FILTER_LINEAR       209
#define M3G_FILTER_NEAREST      210
#define M3G_FUNC_ADD            224
#define M3G_FUNC_BLEND          225
#define M3G_FUNC_DECAL          226
#define M3G_FUNC_MODULATE       227
#define M3G_FUNC_REPLACE        228
#define M3G_WRAP_CLAMP          240
#define M3G_WRAP_REPEAT         241

/*javax.microedition.m3g.Defs*/
#define M3G_GET_POSITIONS       0
#define M3G_GET_NORMALS         1
#define M3G_GET_COLORS          2
#define M3G_GET_TEXCOORDS0      3
#define M3G_GET_CROPX           0
#define M3G_GET_CROPY           1
#define M3G_GET_CROPWIDTH       2
#define M3G_GET_CROPHEIGHT      3
#define M3G_GET_MODEX           0
#define M3G_GET_MODEY           1
#define M3G_SETGET_COLORCLEAR   0
#define M3G_SETGET_DEPTHCLEAR   1
#define M3G_GET_NEAR            0
#define M3G_GET_FAR             1
#define M3G_SETGET_RENDERING    0
#define M3G_SETGET_PICKING      1
#define M3G_GET_CONSTANT        0
#define M3G_GET_LINEAR          1
#define M3G_GET_QUADRATIC       2

/*--------------------------------------------------------------------*/
/* Object usage flag bits
 *
 * Currently only used for Image
 */

/*! \brief Specifies that an object can be written to at any time */
#define M3G_DYNAMIC             0x0001
/*! \brief Specifies that an object can only be written to prior to
 *  its first use (default) */
#define M3G_STATIC              0x0002
/*! \brief Specifies that an Image can be rendered to (implies
 *  M3G_DYNAMIC) */
#define M3G_RENDERING_TARGET    0x0004
/*!
 * \brief Specifies that an Image is paletted
 */
#define M3G_PALETTED            0x0008


/*--------------------------------------------------------------------*/
/* Buffer bit constants */
#define M3G_COLOR_BUFFER_BIT        0x0001
#define M3G_DEPTH_BUFFER_BIT        0x0002
#define M3G_STENCIL_BUFFER_BIT      0x0004
#define M3G_MULTISAMPLE_BUFFER_BIT  0x0008

/*--------------------------------------------------------------------*/
/* Pixel (output/texture) formats
 */
typedef enum {
    M3G_L8,
    M3G_A8,
    M3G_LA8,
    M3G_LA4,
    M3G_RGB8,
    M3G_RGB8_32,
    M3G_BGR8_32,
    M3G_RGB4,
    M3G_RGB565,
    M3G_RGBA8,
    M3G_BGRA8,
    M3G_ARGB8,
    M3G_RGBA4,
    M3G_RGB5A1,
    M3G_PALETTE8_RGB8,
    M3G_PALETTE8_RGB8_32,
    M3G_PALETTE8_RGBA8,
    M3G_NO_FORMAT
} M3GPixelFormat;

/* Errors */
typedef enum {
    M3G_NO_ERROR = 0x00,
    M3G_INVALID_VALUE,
    M3G_INVALID_ENUM,
    M3G_INVALID_OPERATION,
    M3G_INVALID_OBJECT,
    M3G_INVALID_INDEX,
    M3G_OUT_OF_MEMORY,
    M3G_NULL_POINTER,
    M3G_ARITHMETIC_ERROR,
    M3G_IO_ERROR
} M3GError;

/* Supported types of rendering primitives
 *
 * These match GL enums directly.
 */
typedef enum {
    M3G_TRIANGLE_STRIPS = 0x05
    /*M3G_TRIANGLES,
      M3G_TRIANGLE_FANS*/
} M3Gprimitive;

/*--------------------------------------------------------------------*/
/*! \brief Supported input data types
 *
 * These can be converted to/from OpengGL type enums by adding 0x1400.
 */
typedef enum {
    M3G_BYTE    = 0x00,
    M3G_UBYTE,
    M3G_SHORT,
    M3G_USHORT,
    M3G_INT,
    M3G_UINT,
    M3G_FLOAT
} M3Gdatatype;

/*--------------------------------------------------------------------*/
/*! \brief Profiling statistics */
typedef enum {
    M3G_STAT_CULLING_TESTS,
    M3G_STAT_FLOPS,
    M3G_STAT_MEMORY_ALLOCS,
    M3G_STAT_MEMORY_LOCKS,
    M3G_STAT_RENDER_NODES,
    M3G_STAT_RENDER_NODES_CULLED,
    M3G_STAT_RENDER_NODES_DRAWN,
    M3G_STAT_TCACHE_COMPOSITE_COLLISIONS,
    M3G_STAT_TCACHE_COMPOSITE_HITS,
    M3G_STAT_TCACHE_COMPOSITE_INSERTS,
    M3G_STAT_TCACHE_COMPOSITE_MISSES,
    M3G_STAT_TCACHE_PATH_COLLISIONS,
    M3G_STAT_TCACHE_PATH_FLUSHES,
    M3G_STAT_TCACHE_PATH_HITS,
    M3G_STAT_TCACHE_PATH_INSERTS,
    M3G_STAT_TCACHE_PATH_MISSES,
    M3G_PROFILE_ALIGN,
    M3G_PROFILE_ANIM,
    M3G_PROFILE_BINDTORELEASE,
    M3G_PROFILE_COMMIT,
    M3G_PROFILE_LOADER_DECODE,
    M3G_PROFILE_MORPH,
    M3G_PROFILE_NGL_DRAW,
    M3G_PROFILE_PICK,
    M3G_PROFILE_RELEASETOBIND,
    M3G_PROFILE_SETUP,
    M3G_PROFILE_SETUP_SORT,
    M3G_PROFILE_SETUP_TRANSFORMS,
    M3G_PROFILE_SKIN,
    M3G_PROFILE_TCACHE,
    M3G_PROFILE_TRANSFORM_INVERT,
    M3G_PROFILE_TRANSFORM_TO,
    M3G_PROFILE_VALIDATE,
    M3G_PROFILE_VFC_TEST,
    M3G_PROFILE_VFC_UPDATE,
    /*----------------*/
    M3G_STAT_CUMULATIVE, /* the rest are not cleared automatically */
    /*----------------*/
    M3G_STAT_BOUNDING_BOXES,
    M3G_STAT_MEMORY_ALLOCATED,
    M3G_STAT_MEMORY_MALLOC_BYTES,
    M3G_STAT_MEMORY_MALLOC_PEAK,
    M3G_STAT_MEMORY_OBJECT_BYTES,
    M3G_STAT_MEMORY_OBJECT_PEAK,
    M3G_STAT_MEMORY_PEAK,
    M3G_STAT_OBJECTS,
    M3G_STAT_RENDERABLES,
    M3G_STAT_RENDERQUEUE_SIZE,
    M3G_STAT_TCACHE_COMPOSITE_LOAD,
    M3G_STAT_TCACHE_PATH_LOAD,
    /*----------------*/
    M3G_STAT_MAX
} M3Gstatistic;

#define MIN_PROFILE_STAT    M3G_PROFILE_ALIGN

/*----------------------------------------------------------------------
 * Portable numeric types
 *--------------------------------------------------------------------*/

typedef int             M3Gint;     /*!< \brief 32-bit signed integer   */
typedef short           M3Gshort;   /*!< \brief 16-bit signed integer   */
typedef signed char     M3Gbyte;    /*!< \brief  8-bit signed integer   */
typedef unsigned        M3Guint;    /*!< \brief 32-bit unsigned integer */
typedef unsigned short  M3Gushort;  /*!< \brief 16-bit unsigned integer */
typedef unsigned char   M3Gubyte;   /*!< \brief  8-bit unsigned integer */
typedef float           M3Gfloat;   /*!< \brief 32-bit IEEE float */

typedef M3Gint   M3Genum;
typedef M3Guint  M3Gbitmask;
typedef M3Guint  M3Gbool;

typedef M3Gint  M3Gsizei;

/* Boolean values */
#define M3G_TRUE        ((M3Gbool)1)
#define M3G_FALSE       ((M3Gbool)0)

/*----------------------------------------------------------------------
 * Object handle types
 *--------------------------------------------------------------------*/

typedef struct M3GInterfaceImpl *M3GInterface;
typedef struct M3GLoaderImpl *M3GLoader;
typedef struct M3GObjectImpl *M3GObject;
    typedef struct M3GAnimationControllerImpl *M3GAnimationController;
    typedef struct M3GAnimationTrackImpl *M3GAnimationTrack;
    typedef struct M3GAppearanceImpl *M3GAppearance;
    typedef struct M3GBackgroundImpl *M3GBackground;
    typedef struct M3GCompositingModeImpl *M3GCompositingMode;
    typedef struct M3GFogImpl *M3GFog;
    typedef struct M3GImageImpl *M3GImage;
    typedef struct M3GIndexBufferImpl *M3GIndexBuffer;
    typedef struct M3GKeyframeSequenceImpl *M3GKeyframeSequence;
    typedef struct M3GMaterialImpl *M3GMaterial;
    typedef struct M3GTransformableImpl *M3GTransformable;
        typedef struct M3GNodeImpl *M3GNode;
            typedef struct M3GCameraImpl *M3GCamera;
            typedef struct M3GGroupImpl *M3GGroup;
                typedef struct M3GWorldImpl *M3GWorld;
            typedef struct M3GLightImpl *M3GLight;
            typedef struct M3GMeshImpl *M3GMesh;
                typedef struct M3GMorphingMeshImpl *M3GMorphingMesh;
                typedef struct M3GSkinnedMeshImpl *M3GSkinnedMesh;
            typedef struct M3GSpriteImpl *M3GSprite;
        typedef struct M3GTextureImpl *M3GTexture;
    typedef struct M3GPolygonModeImpl *M3GPolygonMode;
    typedef struct M3GRenderContextImpl *M3GRenderContext;
    typedef struct M3GVertexArrayImpl *M3GVertexArray;
    typedef struct M3GVertexBufferImpl *M3GVertexBuffer;

typedef /*@abstract@*/ M3Gint M3GMemObject;

/*----------------------------------------------------------------------
 * Abstracted OpenGL rendering target types
 *--------------------------------------------------------------------*/

typedef /*@abstract@*/ M3Guint M3GNativeBitmap;
typedef /*@abstract@*/ M3Guint M3GNativeWindow;
typedef /*@abstract@*/ M3Guint M3GEGLSurface;

/*----------------------------------------------------------------------
 * Math API
 *--------------------------------------------------------------------*/

/* -------- Data types -------- */

/*!
 * \brief An optimized 4x4 matrix class.
 *
 * \note This is an abstract type, and only declared here so that it
 * can be instantiated on the stack.  The internal format may change
 * in the future.
 */
typedef struct
{
    /* If you change this struct be sure to check the
     * structure size against the array size in Transform.java!! */
    M3Gfloat elem[16];
    M3Guint mask;
    M3Guint classified  : 1;
    M3Guint complete    : 1;
} M3GMatrix;

/*!
 * \brief A 3-vector class
 */
typedef struct {
    M3Gfloat x, y, z;
} M3GVec3;

/*!
 * \brief A 4-vector class
 */
typedef struct {
    M3Gfloat x, y, z, w;
} M3GVec4;

/*!
 * \brief A quaternion class
 */
typedef struct {
    M3Gfloat x, y, z, w;
} M3GQuat;

/*!
 * \brief A rectangle
 */
typedef struct {
	M3Gint x;
	M3Gint y;
	M3Gint width;
	M3Gint height;
} M3GRectangle;

/* -------- Interpolation -------- */

M3G_API void m3gLerp    (M3Gint size,
                         M3Gfloat *vec,
                         M3Gfloat s,
                         const M3Gfloat *start, const M3Gfloat *end);
M3G_API void m3gHermite (M3Gint size,
                         M3Gfloat *vec,
                         M3Gfloat s,
                         const M3Gfloat *start, const M3Gfloat *end,
                         const M3Gfloat *tStart, const M3Gfloat *tEnd);
M3G_API void m3gSlerpQuat(M3GQuat *quat,
                          M3Gfloat s,
                          const M3GQuat *q0, const M3GQuat *q1);
M3G_API void m3gSquadQuat(M3GQuat *quat,
                          M3Gfloat s,
                          const M3GQuat *q0, const M3GQuat *a,
                          const M3GQuat *b, const M3GQuat *q1);

/* -------- Matrix, vector & quaternion operations -------- */

/* Matrix */

/* shorthand names for the OpenGL style functions */
#define m3gRotateMatrix         m3gPostRotateMatrix
#define m3gRotateMatrixQuat     m3gPostRotateMatrixQuat
#define m3gTranslateMatrix      m3gPostTranslateMatrix
#define m3gScaleMatrix          m3gPostScaleMatrix
#define m3gMulMatrix            m3gPostMultiplyMatrix

/* ...and second names for the matrix-matrix multipliers */
#define m3gLeftMulMatrix        m3gPreMultiplyMatrix
#define m3gRightMulMatrix       m3gPostMultiplyMatrix

M3G_API void    m3gCopyMatrix           (M3GMatrix *dst, const M3GMatrix *src);
M3G_API void    m3gGetMatrixColumn      (const M3GMatrix *mtx, M3Gint col, M3GVec4 *dst);
M3G_API void    m3gGetMatrixColumns     (const M3GMatrix *mtx, M3Gfloat *dst);
M3G_API void    m3gGetMatrixRow         (const M3GMatrix *mtx, M3Gint row, M3GVec4 *dst);
M3G_API void    m3gGetMatrixRows        (const M3GMatrix *mtx, M3Gfloat *dst);
M3G_API void    m3gIdentityMatrix       (M3GMatrix *mtx);
M3G_API M3Gbool m3gInvertMatrix         (M3GMatrix *mtx);
M3G_API M3Gbool m3gMatrixInverse        (M3GMatrix *mtx, const M3GMatrix *other);
M3G_API void    m3gMatrixTranspose      (M3GMatrix *mtx, const M3GMatrix *other);
M3G_API M3Gbool m3gInverseTranspose     (M3GMatrix *mtx, const M3GMatrix *other);
M3G_API void    m3gMatrixProduct        (M3GMatrix *dst, const M3GMatrix *left, const M3GMatrix *right);
M3G_API void    m3gPostMultiplyMatrix   (M3GMatrix *mtx, const M3GMatrix *other);
M3G_API void    m3gPostRotateMatrix     (M3GMatrix *mtx, M3Gfloat angle, M3Gfloat ax, M3Gfloat ay, M3Gfloat az);
M3G_API void    m3gPostRotateMatrixQuat (M3GMatrix *mtx, const M3GQuat *quat);
M3G_API void    m3gPostScaleMatrix      (M3GMatrix *mtx, M3Gfloat sx, M3Gfloat sy, M3Gfloat sz);
M3G_API void    m3gPostTranslateMatrix  (M3GMatrix *mtx, M3Gfloat tx, M3Gfloat ty, M3Gfloat tz);
M3G_API void    m3gPreMultiplyMatrix    (M3GMatrix *mtx, const M3GMatrix *other);
M3G_API void    m3gPreRotateMatrix      (M3GMatrix *mtx, M3Gfloat angle, M3Gfloat ax, M3Gfloat ay, M3Gfloat az);
M3G_API void    m3gPreRotateMatrixQuat  (M3GMatrix *mtx, const M3GQuat *quat);
M3G_API void    m3gPreScaleMatrix       (M3GMatrix *mtx, M3Gfloat sx, M3Gfloat sy, M3Gfloat sz);
M3G_API void    m3gPreTranslateMatrix   (M3GMatrix *mtx, M3Gfloat tx, M3Gfloat ty, M3Gfloat tz);
M3G_API void    m3gScalingMatrix        (M3GMatrix *mtx, const M3Gfloat sx, const M3Gfloat sy, const M3Gfloat sz);
M3G_API void    m3gSetMatrixColumns     (M3GMatrix *mtx, const M3Gfloat *src);
M3G_API void    m3gSetMatrixRows        (M3GMatrix *mtx, const M3Gfloat *src);
M3G_API void    m3gTranslationMatrix    (M3GMatrix *mtx, const M3Gfloat tx, const M3Gfloat ty, const M3Gfloat tz);

/* Vec3/4 */

M3G_API void    m3gAddVec3         (M3GVec3 *vec, const M3GVec3 *other);
M3G_API void    m3gAddVec4         (M3GVec4 *vec, const M3GVec4 *other);
M3G_API void    m3gCross           (M3GVec3 *dst, const M3GVec3 *a, const M3GVec3 *b);
M3G_API M3Gfloat m3gDot3           (const M3GVec3 *a, const M3GVec3 *b);
M3G_API M3Gfloat m3gDot4           (const M3GVec4 *a, const M3GVec4 *b);
M3G_API M3Gfloat m3gLengthVec3     (const M3GVec3 *vec);
M3G_API void    m3gNormalizeVec3   (M3GVec3 *vec);
M3G_API void    m3gNormalizeVec4   (M3GVec4 *vec);
M3G_API void    m3gScaleVec3       (M3GVec3 *vec, const M3Gfloat s);
M3G_API void    m3gScaleVec4       (M3GVec4 *vec, const M3Gfloat s);
M3G_API void    m3gSetVec3         (M3GVec3 *v, M3Gfloat x, M3Gfloat y, M3Gfloat z);
M3G_API void    m3gSetVec4         (M3GVec4 *v, M3Gfloat x, M3Gfloat y, M3Gfloat z, M3Gfloat w);
M3G_API void    m3gSubVec3         (M3GVec3 *vec, const M3GVec3 *other);
M3G_API void    m3gSubVec4         (M3GVec4 *vec, const M3GVec4 *other);
M3G_API void    m3gTransformVec4   (const M3GMatrix *mtx, M3GVec4 *vec);

/* Quat */

M3G_API void    m3gGetAngleAxis    (const M3GQuat *quat,
                                    M3Gfloat *angle, M3GVec3 *axis);
M3G_API void    m3gIdentityQuat    (M3GQuat *quat);
M3G_API void	m3gMulQuat		   (M3GQuat *quat, const M3GQuat *other);
M3G_API void    m3gNormalizeQuat   (M3GQuat *quat);
M3G_API void    m3gQuatMatrix      (M3GMatrix *mtx, const M3GQuat *quat);
M3G_API void    m3gSetAngleAxis    (M3GQuat *quat,
                                    M3Gfloat angle,
                                    M3Gfloat ax, M3Gfloat ay, M3Gfloat az);
M3G_API void    m3gSetAngleAxisRad (M3GQuat *quat,
                                    M3Gfloat angleRad,
                                    M3Gfloat ax, M3Gfloat ay, M3Gfloat az);
M3G_API void    m3gSetQuat         (M3GQuat *quat, const M3Gfloat *vec);
M3G_API void	m3gSetQuatRotation (M3GQuat *quat, const M3GVec3 *from, const M3GVec3 *to);

/*----------------------------------------------------------------------
 * Interface callback types
 *--------------------------------------------------------------------*/

typedef /*@only@*//*@null@*/ void* (m3gMallocFunc) (M3Guint bytes);

typedef void    (m3gFreeFunc)   (/*@only@*//*@null@*//*@out@*/ void *ptr);

typedef M3GMemObject (m3gObjectAllocator)    (M3Guint bytes);
typedef /*@dependent@*/ void*   (m3gObjectResolver)     (M3GMemObject handle);
typedef void    (m3gObjectDeallocator)(M3GMemObject handle);
    
typedef void    (m3gErrorHandler)(M3Genum errorCode, M3GInterface hInterface);

typedef void*   (m3gBeginRenderFunc)(M3Guint userTarget);
typedef void    (m3gEndRenderFunc)(M3Guint userTarget);
typedef void    (m3gReleaseTargetFunc)(M3Guint userTarget);

/*!
 * \brief M3G interface initialization structure
 *
 *
 */
typedef struct {
    /*@shared@*/ m3gMallocFunc          *mallocFunc;
    /*@shared@*/ m3gFreeFunc            *freeFunc;
    /*@shared@*/ m3gObjectAllocator     *objAllocFunc;
    /*@shared@*/ m3gObjectResolver      *objResolveFunc;
    /*@shared@*/ m3gObjectDeallocator   *objFreeFunc;
    /*@shared@*/ m3gErrorHandler        *errorFunc;
    /*@shared@*/ m3gBeginRenderFunc     *beginRenderFunc;
    /*@shared@*/ m3gEndRenderFunc       *endRenderFunc;
    /*@shared@*/ m3gReleaseTargetFunc   *releaseTargetFunc;
    /*@shared@*/ void                   *userContext;
} M3Gparams;

/*----------------------------------------------------------------------
 * API functions
 *--------------------------------------------------------------------*/

/* -------- AnimationTrack -------- */
    
M3G_API M3GAnimationTrack       m3gCreateAnimationTrack (M3GInterface hInterface, M3GKeyframeSequence hSequence, M3Gint property);
M3G_API void                    m3gSetController        (M3GAnimationTrack hTrack, M3GAnimationController hController);
M3G_API M3GAnimationController  m3gGetController        (M3GAnimationTrack hTrack);
M3G_API M3GKeyframeSequence     m3gGetSequence          (M3GAnimationTrack hTrack);
M3G_API M3Gint                  m3gGetTargetProperty    (M3GAnimationTrack hTrack);


/* -------- AnimationController -------- */

M3G_API M3GAnimationController m3gCreateAnimationController(M3GInterface hInterface);
M3G_API void            m3gSetActiveInterval        (M3GAnimationController hController, M3Gint worldTimeMin, M3Gint worldTimeMax);
M3G_API M3Gint          m3gGetActiveIntervalStart   (M3GAnimationController hController);
M3G_API M3Gint          m3gGetActiveIntervalEnd     (M3GAnimationController hController);
M3G_API void            m3gSetSpeed                 (M3GAnimationController hController, M3Gfloat factor, M3Gint worldTime);
M3G_API M3Gfloat        m3gGetSpeed                 (M3GAnimationController hController);
M3G_API void            m3gSetPosition              (M3GAnimationController hController, M3Gfloat sequenceTime, M3Gint worldTime);
M3G_API M3Gfloat        m3gGetPosition              (M3GAnimationController hController, M3Gint worldTime);
M3G_API void            m3gSetWeight                (M3GAnimationController hController, M3Gfloat weight);
M3G_API M3Gfloat        m3gGetWeight                (M3GAnimationController hController);
M3G_API M3Gint          m3gGetRefWorldTime          (M3GAnimationController hController);
    
/* -------- Appearance -------- */
    
M3G_API M3GAppearance   m3gCreateAppearance     (M3GInterface hInterface);
M3G_API M3GCompositingMode m3gGetCompositingMode(M3GAppearance hApp);
M3G_API M3GFog          m3gGetFog               (M3GAppearance hApp);
M3G_API M3Gint          m3gGetLayer             (M3GAppearance hApp);
M3G_API M3GMaterial     m3gGetMaterial          (M3GAppearance hApp);
M3G_API M3GPolygonMode  m3gGetPolygonMode       (M3GAppearance hApp);
M3G_API M3GTexture      m3gGetTexture           (M3GAppearance hApp, M3Gint unit);
M3G_API void            m3gSetCompositingMode   (M3GAppearance hApp, M3GCompositingMode hMode);
M3G_API void            m3gSetFog               (M3GAppearance hApp, M3GFog hFog);
M3G_API void            m3gSetLayer             (M3GAppearance hApp, M3Gint layer);
M3G_API void            m3gSetPolygonMode       (M3GAppearance hApp, M3GPolygonMode hMode);
M3G_API void            m3gSetMaterial          (M3GAppearance hApp, M3GMaterial hMaterial);
M3G_API void            m3gSetTexture           (M3GAppearance hAppearance, M3Gint unit, M3GTexture hTexture);

    
/* -------- Background -------- */
    
M3G_API M3GBackground   m3gCreateBackground     (M3GInterface hInterface);
M3G_API void            m3gSetBgColor           (M3GBackground handle, M3Guint ARGB);
M3G_API void            m3gSetBgMode            (M3GBackground handle, M3Gint modeX, M3Gint modeY);
M3G_API void            m3gSetBgCrop            (M3GBackground handle, M3Gint cropX, M3Gint cropY, M3Gint width, M3Gint height);
M3G_API void            m3gSetBgImage           (M3GBackground handle, M3GImage hImage);
M3G_API M3GImage        m3gGetBgImage           (M3GBackground handle);
M3G_API M3Guint         m3gGetBgColor           (M3GBackground handle);
M3G_API M3Gint          m3gGetBgMode            (M3GBackground handle, M3Gint which);
M3G_API M3Gint          m3gGetBgCrop            (M3GBackground handle, M3Gint which);
M3G_API void            m3gSetBgEnable          (M3GBackground handle, M3Gint which, M3Gbool enable);
M3G_API M3Gbool         m3gIsBgEnabled          (M3GBackground handle, M3Gint which);

    
/* -------- Camera -------- */
    
M3G_API M3GCamera       m3gCreateCamera         (M3GInterface hInterface);
M3G_API void            m3gSetParallel          (M3GCamera handle, M3Gfloat height, M3Gfloat aspectRatio, M3Gfloat clipNear, M3Gfloat clipFar);
M3G_API void            m3gSetPerspective       (M3GCamera handle, M3Gfloat fovy, M3Gfloat aspectRatio, M3Gfloat clipNear, M3Gfloat clipFar);
M3G_API void            m3gSetProjectionMatrix  (M3GCamera handle, const M3GMatrix *transform);
M3G_API M3Gint          m3gGetProjectionAsMatrix(M3GCamera handle, M3GMatrix *transform);
M3G_API M3Gint          m3gGetProjectionAsParams(M3GCamera handle, M3Gfloat *params);

    
/* -------- CompositingMode -------- */
    
M3G_API M3GCompositingMode m3gCreateCompositingMode(M3GInterface m3g);
    
M3G_API void            m3gSetBlending          (M3GCompositingMode compositingMode, M3Genum mode);
M3G_API M3Genum         m3gGetBlending          (M3GCompositingMode compositingMode);
M3G_API void            m3gSetAlphaThreshold    (M3GCompositingMode compositingMode, M3Gfloat threshold);
M3G_API M3Gfloat        m3gGetAlphaThreshold    (M3GCompositingMode compositingMode);
M3G_API void            m3gEnableDepthTest      (M3GCompositingMode compositingMode, M3Gbool enable);
M3G_API void            m3gEnableDepthWrite     (M3GCompositingMode compositingMode, M3Gbool enable);
M3G_API void            m3gEnableColorWrite     (M3GCompositingMode compositingMode, M3Gbool enable);
M3G_API void            m3gSetDepthOffset       (M3GCompositingMode compositingMode, M3Gfloat factor, M3Gfloat units);
M3G_API M3Gfloat        m3gGetDepthOffsetFactor (M3GCompositingMode compositingMode);
M3G_API M3Gfloat        m3gGetDepthOffsetUnits  (M3GCompositingMode compositingMode);
M3G_API M3Gbool         m3gIsAlphaWriteEnabled  (M3GCompositingMode handle);
M3G_API M3Gbool         m3gIsColorWriteEnabled  (M3GCompositingMode handle);
M3G_API M3Gbool         m3gIsDepthTestEnabled   (M3GCompositingMode handle);
M3G_API M3Gbool         m3gIsDepthWriteEnabled  (M3GCompositingMode handle);
M3G_API void            m3gSetAlphaWriteEnable  (M3GCompositingMode handle, M3Gbool enable);

    
/* -------- Fog -------- */

M3G_API M3GFog          m3gCreateFog    (M3GInterface hInterface);
M3G_API void            m3gSetFogMode   (M3GFog handle, M3Gint mode);
M3G_API M3Gint          m3gGetFogMode   (M3GFog handle);
M3G_API void            m3gSetFogLinear (M3GFog handle, M3Gfloat fogNear, M3Gfloat fogFar);
M3G_API M3Gfloat        m3gGetFogDistance(M3GFog handle, M3Genum which);
M3G_API void            m3gSetFogDensity(M3GFog handle, M3Gfloat density);
M3G_API M3Gfloat        m3gGetFogDensity(M3GFog handle);
M3G_API void            m3gSetFogColor  (M3GFog handle, M3Guint rgb);
M3G_API M3Guint         m3gGetFogColor  (M3GFog handle);
    
    
/* -------- Group -------- */

M3G_API M3GGroup        m3gCreateGroup  (M3GInterface m3g);
M3G_API void            m3gAddChild     (M3GGroup handle, M3GNode hNode);
M3G_API void            m3gRemoveChild  (M3GGroup handle, M3GNode hNode);
M3G_API M3GNode         m3gPick3D       (M3GGroup handle, M3Gint mask, M3Gfloat *ray, M3Gfloat *result);
M3G_API M3GNode         m3gPick2D       (M3GGroup handle, M3Gint mask, M3Gfloat x, M3Gfloat y, M3GCamera hCamera, M3Gfloat *result);
M3G_API M3GNode         m3gGetChild     (M3GGroup handle, M3Gint idx);
M3G_API M3Gint          m3gGetChildCount(M3GGroup handle);

    
/* -------- Image -------- */
    
M3G_API M3GImage        m3gCreateImage      (M3GInterface m3g, M3GImageFormat format, M3Gint width, M3Gint height, M3Gbitmask flags);
M3G_API void            m3gSetImage         (M3GImage hImage, const void *pixels);
M3G_API void            m3gSetImagePalette  (M3GImage hImage, M3Gint paletteLength, const void *palette);
M3G_API void            m3gSetImageScanline (M3GImage hImage, M3Gint line, M3Gbool trueAlpha, const M3Guint *pixels);
M3G_API void            m3gSetSubImage      (M3GImage hImage, M3Gint x, M3Gint y, M3Gint width, M3Gint height, M3Gint length, const void *pixels);
M3G_API M3Gbool         m3gIsMutable        (M3GImage hImage);
M3G_API M3GImageFormat  m3gGetFormat        (M3GImage hImage);
M3G_API M3Gint          m3gGetWidth         (M3GImage hImage);
M3G_API M3Gint          m3gGetHeight        (M3GImage hImage);
M3G_API void            m3gGetImageARGB     (M3GImage hImage, M3Guint *pixels);
M3G_API void            m3gCommitImage      (M3GImage hImage);
    
/* -------- IndexBuffer -------- */
    
M3G_API M3GIndexBuffer  m3gCreateStripBuffer            (M3GInterface m3g, M3Gprimitive primitive, M3Gsizei stripCount, const M3Gsizei *stripLengths, M3Gdatatype type, M3Gsizei indicesCount, const void *stripIndices);
M3G_API M3GIndexBuffer  m3gCreateImplicitStripBuffer    (M3GInterface hInterface, M3Gsizei stripCount, const M3Gsizei *stripLengths, M3Gint firstIndex);
M3G_API M3Gint          m3gGetBatchCount(M3GIndexBuffer buffer);
M3G_API M3Gbool         m3gGetBatchIndices(M3GIndexBuffer buffer, M3Gint batchIndex, M3Gint *indices);
M3G_API M3Gint          m3gGetBatchSize(M3GIndexBuffer buffer, M3Gint batchIndex);
M3G_API M3Gprimitive    m3gGetPrimitive(M3GIndexBuffer buffer);
    
/* -------- Interface -------- */

M3G_API M3GInterface    m3gCreateInterface  (const M3Gparams *params);
M3G_API void            m3gDeleteInterface  (M3GInterface m3g);
M3G_API void            m3gGarbageCollect   (M3GInterface hInterface);    
M3G_API M3Genum         m3gGetError         (M3GInterface m3g);
M3G_API M3Gint          m3gGetStatistic     (M3GInterface hInterface,
                                             M3Gstatistic stat);
M3G_API void *          m3gGetUserContext   (M3GInterface m3g);
M3G_API M3Gbool         m3gIsAntialiasingSupported(M3GInterface interface);

/* -------- KeyframeSequence -------- */

M3G_API M3GKeyframeSequence m3gCreateKeyframeSequence(M3GInterface hInterface, M3Gint numKeyframes, M3Gint numComponents, M3Gint interpolation);
M3G_API void            m3gSetValidRange(M3GKeyframeSequence handle, M3Gint first, M3Gint last);
M3G_API void            m3gSetKeyframe  (M3GKeyframeSequence handle, M3Gint ind, M3Gint time, M3Gint valueSize, const M3Gfloat *value);
M3G_API void            m3gSetDuration  (M3GKeyframeSequence handle, M3Gint duration);
M3G_API M3Gint          m3gGetDuration  (M3GKeyframeSequence handle);
M3G_API void            m3gSetRepeatMode(M3GKeyframeSequence handle, M3Genum mode);
M3G_API M3Genum         m3gGetRepeatMode(M3GKeyframeSequence handle);
M3G_API M3Gint          m3gGetComponentCount(M3GKeyframeSequence handle);
M3G_API M3Gint          m3gGetInterpolationType(M3GKeyframeSequence handle);
M3G_API M3Gint          m3gGetKeyframe  (M3GKeyframeSequence handle, M3Gint frameIndex, M3Gfloat *value);
M3G_API M3Gint          m3gGetKeyframeCount(M3GKeyframeSequence handle);
M3G_API void            m3gGetValidRange(M3GKeyframeSequence handle, M3Gint *first, M3Gint *last);
    
/* -------- Light -------- */
    
M3G_API M3GLight        m3gCreateLight      (M3GInterface hInterface);
M3G_API void            m3gSetIntensity     (M3GLight handle, M3Gfloat intensity);
M3G_API void            m3gSetLightColor    (M3GLight handle, M3Guint rgb);
M3G_API void            m3gSetLightMode     (M3GLight handle, M3Genum mode);
M3G_API void            m3gSetSpotAngle     (M3GLight handle, M3Gfloat angle);
M3G_API void            m3gSetSpotExponent  (M3GLight handle, M3Gfloat exponent);
M3G_API void            m3gSetAttenuation   (M3GLight handle, M3Gfloat constant, M3Gfloat linear, M3Gfloat quadratic);
M3G_API M3Gfloat        m3gGetIntensity     (M3GLight handle);
M3G_API M3Guint         m3gGetLightColor    (M3GLight handle);
M3G_API M3Gint          m3gGetLightMode     (M3GLight handle);
M3G_API M3Gfloat        m3gGetSpotAngle     (M3GLight handle);
M3G_API M3Gfloat        m3gGetSpotExponent  (M3GLight handle);
M3G_API M3Gfloat        m3gGetAttenuation   (M3GLight handle, M3Genum type);
M3G_API M3Gint          m3gGetScopeMask     (M3GLight handle);

    
/* -------- Loader -------- */

M3G_API M3GLoader       m3gCreateLoader(M3GInterface m3g);
M3G_API M3Gsizei        m3gDecodeData(M3GLoader loader, M3Gsizei bytes, const M3Gubyte *data);
M3G_API M3Gint          m3gGetLoadedObjects(M3GLoader loader, M3GObject *buffer);
M3G_API void            m3gImportObjects(M3GLoader loader, M3Gint n, M3GObject *refs);
M3G_API M3Gint          m3gGetObjectsWithUserParameters(M3GLoader loader, M3GObject *objects);
M3G_API M3Gint          m3gGetNumUserParameters(M3GLoader loader, M3Gint object);
M3G_API M3Gint          m3gGetUserParameter(M3GLoader loader, M3Gint object, M3Gint index, M3Gbyte *buffer);
M3G_API void            m3gSetConstraints(M3GLoader loader, M3Gint triConstraint);

/* -------- Material -------- */
    
M3G_API M3GMaterial     m3gCreateMaterial               (M3GInterface m3g);
M3G_API void            m3gSetColor                     (M3GMaterial material, M3Genum target, M3Guint ARGB);
M3G_API M3Guint         m3gGetColor                     (M3GMaterial material, M3Genum target);
M3G_API void            m3gSetShininess                 (M3GMaterial material, M3Gfloat shininess);
M3G_API M3Gfloat        m3gGetShininess                 (M3GMaterial material);
M3G_API void            m3gSetVertexColorTrackingEnable (M3GMaterial material, M3Gbool enable);
M3G_API M3Gbool         m3gIsVertexColorTrackingEnabled (M3GMaterial material);

    
/* -------- Mesh -------- */

M3G_API M3GMesh         m3gCreateMesh       (M3GInterface hInterface, M3GVertexBuffer hVertices, M3GIndexBuffer *hTriangles, M3GAppearance *hAppearances, M3Gint trianglePatchCount);
M3G_API void            m3gSetAppearance    (M3GMesh handle, M3Gint appearanceIndex, M3GAppearance hAppearance);
M3G_API M3GAppearance   m3gGetAppearance    (M3GMesh handle, M3Gint idx);
M3G_API M3GIndexBuffer  m3gGetIndexBuffer   (M3GMesh handle, M3Gint idx);
M3G_API M3GVertexBuffer m3gGetVertexBuffer  (M3GMesh handle);
M3G_API M3Gint          m3gGetSubmeshCount  (M3GMesh handle);
    
    
/* -------- MorphingMesh -------- */

M3G_API M3GMorphingMesh m3gCreateMorphingMesh  (M3GInterface hInterface, M3GVertexBuffer hVertices, M3GVertexBuffer *hTargets, M3GIndexBuffer *hTriangles, M3GAppearance *hAppearances, M3Gint trianglePatchCount, M3Gint targetCount);
M3G_API void            m3gSetWeights          (M3GMorphingMesh handle, M3Gfloat *weights, M3Gint numWeights);
M3G_API void            m3gGetWeights          (M3GMorphingMesh handle, M3Gfloat *weights, M3Gint numWeights);
M3G_API M3GVertexBuffer m3gGetMorphTarget      (M3GMorphingMesh handle, M3Gint idx);
M3G_API M3Gint          m3gGetMorphTargetCount  (M3GMorphingMesh handle);

    
/* -------- Node -------- */

M3G_API void            m3gAlignNode        (M3GNode hNode, M3GNode hReference);
M3G_API M3Gbool         m3gGetTransformTo   (M3GNode handle, M3GNode hTarget, M3GMatrix *transform);
M3G_API void            m3gSetAlignment     (M3GNode handle, M3GNode hZReference, M3Gint zTarget, M3GNode hYReference, M3Gint yTarget);
M3G_API void            m3gSetAlphaFactor   (M3GNode handle, M3Gfloat alphaFactor);
M3G_API M3Gfloat        m3gGetAlphaFactor   (M3GNode handle);
M3G_API void            m3gEnable           (M3GNode handle, M3Gint which, M3Gbool enable);
M3G_API M3Gint          m3gIsEnabled        (M3GNode handle, M3Gint which);
M3G_API void            m3gSetScope         (M3GNode handle, M3Gint id);
M3G_API M3Gint          m3gGetScope         (M3GNode handle);
M3G_API M3GNode         m3gGetParent        (M3GNode handle);
M3G_API M3GNode         m3gGetZRef          (M3GNode handle);
M3G_API M3GNode         m3gGetYRef          (M3GNode handle);
M3G_API M3Gint          m3gGetSubtreeSize   (M3GNode handle);
M3G_API M3Gint          m3gGetAlignmentTarget(M3GNode handle, M3Gint axis);
    
/* -------- Object -------- */
    
M3G_API void            m3gAddRef               (M3GObject hObject);
M3G_API void            m3gDeleteObject         (M3GObject hObject);
M3G_API void            m3gDeleteRef            (M3GObject hObject);
M3G_API M3GClass        m3gGetClass             (M3GObject hObject);
M3G_API M3Gint          m3gAddAnimationTrack    (M3GObject hObject, M3GAnimationTrack hAnimationTrack);
M3G_API void            m3gRemoveAnimationTrack (M3GObject hObject, M3GAnimationTrack hAnimationTrack);
M3G_API M3Gint          m3gGetAnimationTrackCount(M3GObject hObject);
M3G_API M3Gint          m3gAnimate              (M3GObject hObject, M3Gint time);
M3G_API void            m3gSetUserID            (M3GObject hObject, M3Gint userID);
M3G_API M3Gint          m3gGetUserID            (M3GObject hObject);
M3G_API M3GAnimationTrack m3gGetAnimationTrack  (M3GObject hObject, M3Gint idx);
M3G_API M3GObject       m3gDuplicate            (M3GObject hObject, M3GObject *hReferences);
M3G_API M3GInterface    m3gGetObjectInterface   (M3GObject hObject);
M3G_API M3Gint          m3gGetReferences        (M3GObject hObject, M3GObject *references, M3Gint length);
M3G_API M3GObject       m3gFind                 (M3GObject hObject, M3Gint userID);

#define m3gGetInterface(obj) m3gGetObjectInterface((M3GObject)(obj))
    
/* -------- PolygonMode -------- */
    
M3G_API M3GPolygonMode  m3gCreatePolygonMode    (M3GInterface m3g);
M3G_API void            m3gSetCulling           (M3GPolygonMode handle, M3Gint mode);
M3G_API M3Gint          m3gGetCulling           (M3GPolygonMode handle);
M3G_API void            m3gSetWinding           (M3GPolygonMode handle, M3Gint mode);
M3G_API M3Gint          m3gGetWinding           (M3GPolygonMode handle);
M3G_API void            m3gSetShading           (M3GPolygonMode handle, M3Gint mode);
M3G_API M3Gint          m3gGetShading           (M3GPolygonMode handle);
M3G_API void            m3gSetTwoSidedLightingEnable(M3GPolygonMode handle, M3Gbool enable);
M3G_API M3Gbool         m3gIsTwoSidedLightingEnabled(M3GPolygonMode handle);
M3G_API void            m3gSetLocalCameraLightingEnable(M3GPolygonMode polygonMode, M3Gbool enable);
M3G_API void            m3gSetPerspectiveCorrectionEnable(M3GPolygonMode polygonMode, M3Gbool enable);
M3G_API M3Gbool         m3gIsLocalCameraLightingEnabled(M3GPolygonMode handle);
M3G_API M3Gbool         m3gIsPerspectiveCorrectionEnabled(M3GPolygonMode handle);
    
/* -------- RenderContext -------- */

M3G_API M3GRenderContext m3gCreateContext(M3GInterface m3g);
    
M3G_API M3Gint  m3gAddLight             (M3GRenderContext ctx, M3GLight light, const M3GMatrix *transform);
M3G_API void    m3gBindBitmapTarget     (M3GRenderContext ctx, M3GNativeBitmap hBitmap);
M3G_API void    m3gBindImageTarget      (M3GRenderContext ctx, M3GImage hImage);
M3G_API void    m3gBindEGLSurfaceTarget (M3GRenderContext context, M3GEGLSurface surface);
M3G_API void    m3gBindMemoryTarget     (M3GRenderContext context, void *pixels, M3Guint width, M3Guint height, M3GPixelFormat format, M3Guint stride, M3Guint userContext);
M3G_API void    m3gBindWindowTarget     (M3GRenderContext ctx, M3GNativeWindow hWindow);
M3G_API void    m3gClear                (M3GRenderContext context, M3GBackground hBackground);
M3G_API void    m3gClearLights          (M3GRenderContext context);
M3G_API M3Guint m3gGetUserHandle        (M3GRenderContext hCtx);
M3G_API void    m3gInvalidateBitmapTarget(M3GRenderContext ctx, M3GNativeBitmap hBitmap);
M3G_API void    m3gInvalidateWindowTarget(M3GRenderContext ctx, M3GNativeWindow hWindow);
M3G_API void    m3gInvalidateMemoryTarget(M3GRenderContext ctx, void *pixels);
M3G_API void    m3gReleaseTarget        (M3GRenderContext ctx);
M3G_API void    m3gRender               (M3GRenderContext ctx, M3GVertexBuffer hVertices, M3GIndexBuffer hIndices, M3GAppearance hAppearance, const M3GMatrix *transform, M3Gfloat alphaFactor, M3Gint scope);
M3G_API void    m3gRenderWorld          (M3GRenderContext context, M3GWorld hWorld);
M3G_API void    m3gRenderNode           (M3GRenderContext context, M3GNode hNode, const M3GMatrix *transform);
M3G_API M3Gbool m3gSetRenderBuffers     (M3GRenderContext hCtx, M3Gbitmask bufferBits);
M3G_API M3Gbool m3gSetRenderHints       (M3GRenderContext hCtx, M3Gbitmask hintBits);
M3G_API void    m3gSetCamera            (M3GRenderContext context, M3GCamera hCamera, M3GMatrix *transform);
M3G_API void    m3gSetDepthRange        (M3GRenderContext hCtx, M3Gfloat depthNear, M3Gfloat depthFar);
M3G_API void    m3gSetLight             (M3GRenderContext context, M3Gint lightIndex, M3GLight hLight, const M3GMatrix *transform);
M3G_API void    m3gSetClipRect          (M3GRenderContext ctx, M3Gint x, M3Gint y, M3Gint width, M3Gint height);
M3G_API void    m3gSetDisplayArea(M3GRenderContext hCtx, M3Gint width, M3Gint height);
M3G_API void    m3gSetViewport          (M3GRenderContext ctx, M3Gint x, M3Gint y, M3Gint width, M3Gint height);
M3G_API void    m3gGetViewport          (M3GRenderContext hCtx, M3Gint *x, M3Gint *y, M3Gint *width, M3Gint *height);
M3G_API void    m3gGetDepthRange        (M3GRenderContext hCtx, M3Gfloat *depthNear, M3Gfloat *depthFar);
M3G_API void    m3gGetViewTransform     (M3GRenderContext hCtx, M3GMatrix *transform);
M3G_API M3GLight m3gGetLightTransform   (M3GRenderContext hCtx, M3Gint lightIndex, M3GMatrix *transform);
M3G_API M3Gsizei m3gGetLightCount       (M3GRenderContext hCtx);
M3G_API M3GCamera m3gGetCamera          (M3GRenderContext hCtx);
M3G_API void	m3gSetAlphaWrite		(M3GRenderContext ctx, M3Gbool enable);
M3G_API M3Gbool	m3gGetAlphaWrite		(M3GRenderContext ctx);
M3G_API void    m3gFreeGLESResources    (M3GRenderContext ctx);

/* -------- SkinnedMesh -------- */

M3G_API M3GSkinnedMesh  m3gCreateSkinnedMesh    (M3GInterface hInterface, M3GVertexBuffer hVertices, M3GIndexBuffer *hTriangles, M3GAppearance *hAppearances, M3Gint trianglePatchCount, M3GGroup hSkeleton);
M3G_API void            m3gAddTransform         (M3GSkinnedMesh handle, M3GNode hBone, M3Gint weight, M3Gint firstVertex, M3Gint numVertices);
M3G_API M3GGroup        m3gGetSkeleton          (M3GSkinnedMesh handle);
M3G_API void            m3gGetBoneTransform     (M3GSkinnedMesh handle, M3GNode hBone, M3GMatrix *transform);
M3G_API M3Gint          m3gGetBoneVertices      (M3GSkinnedMesh handle, M3GNode hBone, M3Gint *indices, M3Gfloat *weights);
    
    
/* -------- Sprite -------- */

M3G_API M3GSprite       m3gCreateSprite         (M3GInterface hInterface, M3Gbool scaled, M3GImage hImage, M3GAppearance hAppearance);
M3G_API M3Gbool         m3gIsScaledSprite       (M3GSprite handle);
M3G_API void            m3gSetSpriteAppearance  (M3GSprite handle, M3GAppearance hAppearance);
M3G_API M3Gbool         m3gSetSpriteImage       (M3GSprite handle, M3GImage hImage);
M3G_API void            m3gSetCrop              (M3GSprite handle, M3Gint cropX, M3Gint cropY, M3Gint width, M3Gint height);
M3G_API M3Gint          m3gGetCrop              (M3GSprite handle, M3Gint which);
M3G_API M3GAppearance   m3gGetSpriteAppearance  (M3GSprite handle);
M3G_API M3GImage        m3gGetSpriteImage       (M3GSprite handle);

    
/* -------- Texture -------- */
    
M3G_API M3GTexture      m3gCreateTexture        (M3GInterface m3g, M3GImage hImage);
M3G_API M3GImage        m3gGetTextureImage      (M3GTexture hTexture);
M3G_API void            m3gSetTextureImage      (M3GTexture hTexture, M3GImage hImage);
M3G_API void            m3gSetTextureTransform  (M3GTexture hTexture, M3GMatrix *mtx);
M3G_API void            m3gSetFiltering         (M3GTexture hTexture, M3Genum levelFilter, M3Genum imageFilter);
M3G_API void            m3gSetWrapping          (M3GTexture hTexture, M3Genum wrapS, M3Genum wrapT);
M3G_API int             m3gGetWrappingS         (M3GTexture hTexture);
M3G_API int             m3gGetWrappingT         (M3GTexture hTexture);
M3G_API void            m3gTextureSetBlending   (M3GTexture hTexture, M3Genum func);
M3G_API int             m3gTextureGetBlending   (M3GTexture hTexture);
M3G_API void            m3gSetBlendColor        (M3GTexture hTexture, M3Guint RGB);
M3G_API M3Guint         m3gGetBlendColor        (M3GTexture hTexture);
M3G_API void            m3gTransformSetTransform(M3GTexture hTexture, const M3GMatrix *transform);
M3G_API void            m3gTransformGetTransform(M3GTexture hTexture, M3GMatrix *transform);
M3G_API void            m3gGetFiltering         (M3GTexture hTexture, M3Gint *levelFilter, M3Gint *imageFilter);

/* -------- Transformable -------- */

M3G_API void    m3gSetOrientation       (M3GTransformable handle, M3Gfloat angle, M3Gfloat ax, M3Gfloat ay, M3Gfloat az);
M3G_API void    m3gPostRotate           (M3GTransformable handle, M3Gfloat angle, M3Gfloat ax, M3Gfloat ay, M3Gfloat az);
M3G_API void    m3gPreRotate            (M3GTransformable handle, M3Gfloat angle, M3Gfloat ax, M3Gfloat ay, M3Gfloat az);
M3G_API void    m3gGetOrientation       (M3GTransformable handle, M3Gfloat *angleAxis);
M3G_API void    m3gSetScale             (M3GTransformable handle, M3Gfloat sx, M3Gfloat sy, M3Gfloat sz);
M3G_API void    m3gScale                (M3GTransformable handle, M3Gfloat sx, M3Gfloat sy, M3Gfloat sz);
M3G_API void    m3gGetScale             (M3GTransformable handle, M3Gfloat *scale);
M3G_API void    m3gSetTranslation       (M3GTransformable handle, M3Gfloat tx, M3Gfloat ty, M3Gfloat tz);
M3G_API void    m3gTranslate            (M3GTransformable handle, M3Gfloat tx, M3Gfloat ty, M3Gfloat tz);
M3G_API void    m3gGetTranslation       (M3GTransformable handle, M3Gfloat *translation);
M3G_API void    m3gSetTransform         (M3GTransformable handle, const M3GMatrix *transform);
M3G_API void    m3gGetTransform         (M3GTransformable handle, M3GMatrix *transform);
M3G_API void    m3gGetCompositeTransform(M3GTransformable handle, M3GMatrix *transform);
    
/* -------- VertexArray -------- */
    
M3G_API M3GVertexArray  m3gCreateVertexArray    (M3GInterface m3g, M3Gsizei count, M3Gint size, M3Gdatatype type);
M3G_API void            m3gGetVertexArrayParams (M3GVertexArray handle, M3Gsizei *count, M3Gint *size, M3Gdatatype *type, M3Gsizei *stride);
M3G_API void *          m3gMapVertexArray       (M3GVertexArray handle);
M3G_API const void *    m3gMapVertexArrayReadOnly(M3GVertexArray handle);
M3G_API void            m3gSetVertexArrayElements(M3GVertexArray handle, M3Gint first, M3Gsizei count, M3Gsizei srcLength, M3Gdatatype type, const void *src);
M3G_API void            m3gTransformArray       (M3GVertexArray handle, M3GMatrix *transform, M3Gfloat *out, M3Gint outLength, M3Gbool w);
M3G_API void            m3gUnmapVertexArray     (M3GVertexArray handle);
M3G_API void            m3gGetVertexArrayElements(M3GVertexArray handle, M3Gint first, M3Gsizei count, M3Gsizei dstLength, M3Gdatatype type, void *dst);
    
/* -------- VertexBuffer -------- */
    
M3G_API M3GVertexBuffer m3gCreateVertexBuffer   (M3GInterface m3g);
M3G_API M3Gint          m3gGetVertexCount       (M3GVertexBuffer hBuffer);
M3G_API M3GVertexArray  m3gGetVertexArray       (M3GVertexBuffer handle, M3Gint which, M3Gfloat *scaleBias, M3Gint sbLength);
M3G_API void            m3gSetColorArray        (M3GVertexBuffer hBuffer, M3GVertexArray hArray);
M3G_API void            m3gSetNormalArray       (M3GVertexBuffer hBuffer, M3GVertexArray hArray);
M3G_API void            m3gSetTexCoordArray     (M3GVertexBuffer hBuffer, M3Gint unit, M3GVertexArray hArray, M3Gfloat scale, M3Gfloat *bias, M3Gint biasLength);
M3G_API void            m3gSetVertexDefaultColor(M3GVertexBuffer hBuffer, M3Guint ARGB);
M3G_API M3Guint         m3gGetVertexDefaultColor(M3GVertexBuffer hBuffer);
M3G_API void            m3gSetVertexArray       (M3GVertexBuffer hBuffer, M3GVertexArray hArray, M3Gfloat scale, M3Gfloat *bias, M3Gint biasLength);
    
/* -------- World -------- */
    
M3G_API M3GWorld        m3gCreateWorld          (M3GInterface hInterface);
M3G_API void            m3gSetActiveCamera      (M3GWorld handle, M3GCamera hCamera);
M3G_API void            m3gSetBackground        (M3GWorld handle, M3GBackground hBackground);
M3G_API M3GBackground   m3gGetBackground        (M3GWorld handle);
M3G_API M3GCamera       m3gGetActiveCamera      (M3GWorld handle);

#if defined(__cplusplus)
} /* extern "C" */
#endif

#endif /*__M3G_CORE_H__*/
