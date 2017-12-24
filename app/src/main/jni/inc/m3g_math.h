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
* Description: Vector and matrix math functions and data types
*
*/

#ifndef __M3G_MATH_H__
#define __M3G_MATH_H__

/*!
 * \file
 * \brief Vector and matrix math functions and data types
 */

/*----------------------------------------------------------------------
 * Internal data types
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Axis-aligned bounding box
 */
typedef struct
{
    M3Gfloat min[3], max[3];
    /*
    M3Gbyte min[3], minExp;
    M3Gbyte max[3], maxExp;
    */
} AABB;

/*----------------------------------------------------------------------
 * Global constants
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Maximum positive float value
 */
#define M3G_MAX_POSITIVE_FLOAT  (3.402e+38f)

/*!
 * \internal
 * \brief Minimum negative float value
 */
#define M3G_MIN_NEGATIVE_FLOAT  (-3.402e+38f)

/*! 
 * \internal
 * \brief Degrees to radians multiplier
 */
#define M3G_DEG2RAD             (0.017453292519943295769236907684886f)

#define EPSILON         (1.0e-5f)
#define EPSILON_EXP     (-17)
#define RAD2DEG         (57.295779513082320876798154814105f)
#define PI              (3.14159265359f)
#define HALF_PI         (PI / 2.0f)
#define ONE_AND_HALF_PI (PI + HALF_PI)
#define TWO_PI          (2.f * PI)

/*! \internal \brief Extracts the bit pattern of a floating point number */
#define FLOAT_AS_UINT(x) (*(M3Guint*)&(x))

/*! \internal \brief Returns an integer bit pattern as float */
#define INT_AS_FLOAT(x) (*(M3Gfloat*)&(x))

/* IEEE floating point format */
#define MANTISSA_MASK   0x007FFFFFu
#define EXP_MASK        0x7F800000u
#define SIGN_MASK       0x80000000u

#define M3G_FLOAT_ONE   0x3F800000

/*! \internal \brief Extracts the exponent of a floating point number */
#define EXPONENT(x) (((M3Gint)(FLOAT_AS_UINT(x) & EXP_MASK) >> 23) - 127)

/*! \internal \brief Extracts the mantissa of a floating point number */
#define MANTISSA(x) (FLOAT_AS_UINT(x) & MANTISSA_MASK)

/*! \internal \brief Extracts the sign of a floating point number */
#define SIGN(x) (1 - ((FLOAT_AS_UINT(x) & SIGN_MASK) >> 30))

/*! \internal \brief Extracts just the sign bit of a floating point number */
#define SIGN_BIT(x) (FLOAT_AS_UINT(x) >> 31)

/* Useful constants */
#define LEADING_ONE (1 << 23)

/*! \internal \brief Checks the sign of a floating point number */
#define IS_NEGATIVE(x) ((FLOAT_AS_UINT(x) & SIGN_MASK) != 0)

/* Floating-point constant identification macros */
#   define IS_ZERO(x)      ((FLOAT_AS_UINT(x) & ~SIGN_MASK) <= 0x01000000)
#   define IS_ONE(x)       (((x) > 1.0f - EPSILON) && ((x) < 1.0f + EPSILON))
#   define IS_MINUS_ONE(x) (((x) > -1.0f - EPSILON) && ((x) < -1.0f + EPSILON))

/* Elementary vectors */
static const Vec4 Vec4_X_AXIS = {1, 0, 0, 0};
static const Vec4 Vec4_Y_AXIS = {0, 1, 0, 0};
static const Vec4 Vec4_Z_AXIS = {0, 0, 1, 0};
static const Vec4 Vec4_ORIGIN = {0, 0, 0, 1};

/*----------------------------------------------------------------------
 * Elementary floating-point math
 *--------------------------------------------------------------------*/

#if defined(M3G_SOFT_FLOAT)
static M3Gfloat m3gAdd(const M3Gfloat a, const M3Gfloat b);
static M3Gfloat m3gMul(const M3Gfloat a, const M3Gfloat b);
static M3Gfloat m3gRcpSqrt(const M3Gfloat x);
static M3Gfloat m3gSqrt(const M3Gfloat x);
static M3G_INLINE M3Gfloat m3gAbs(const M3Gfloat a)
{
    M3Guint temp = FLOAT_AS_UINT(a) & ~SIGN_MASK;
    return INT_AS_FLOAT(temp);
}
static M3G_INLINE M3Gfloat m3gDiv(const M3Gfloat a, const M3Gfloat b)
{
    return (a / b);
}
static M3G_INLINE M3Gfloat m3gDivif(const M3Gint a, const M3Gint b)
{
    return m3gDiv((M3Gfloat) a, (M3Gfloat) b);
}
static M3G_INLINE M3Gfloat m3gMadd(const M3Gfloat a, const M3Gfloat b, const M3Gfloat c)
{
    return m3gAdd(m3gMul(a, b), c);
}
static M3G_INLINE M3Gfloat m3gRcp(const M3Gfloat x)
{
    return (1.0f / x);
}
static M3G_INLINE M3Gfloat m3gSub(const M3Gfloat a, const M3Gfloat b)
{
    M3Guint bNeg = FLOAT_AS_UINT(b) ^ SIGN_MASK;
    return m3gAdd(a, INT_AS_FLOAT(bNeg));
}
#else
#   include <math.h>
#   define m3gAbs(a)            ((float)fabs(a))
#   define m3gAdd(a, b)         ((float)(a) + (float)(b))
#   define m3gMadd(a, b, c)     ((float)(a) * (float)(b) + (float)(c))
#   define m3gMul(a, b)         ((float)(a) * (float)(b))
#   define m3gDiv(a, b)         ((float)(a) / (float)(b))
#   define m3gDivif(a, b)       ((float)(a) / (float)(b))
#   define m3gRcp(x)            (1.0f / (float)(x))
#   define m3gRcpSqrt(x)        (1.0f / (float)sqrt(x))
#   define m3gSqrt(x)           ((float)sqrt(x))
#   define m3gSub(a, b)         ((float)(a) - (float)(b))
#endif /* M3G_SOFT_FLOAT */

/*----------------------------------------------------------------------
 * Trigonometric and exp functions
 *--------------------------------------------------------------------*/

#if defined(M3G_SOFT_FLOAT)
static M3Gfloat m3gArcCos(const M3Gfloat x);
static M3Gfloat m3gArcTan(const M3Gfloat y, const M3Gfloat x);
static M3Gfloat m3gCos(const M3Gfloat x);
static M3Gfloat m3gSin(const M3Gfloat x);
static M3Gfloat m3gTan(const M3Gfloat x);
static M3Gfloat m3gExp(const M3Gfloat a);
#else
#   define m3gArcCos(x)         ((float)acos(x))
#   define m3gArcTan(y, x)      ((float)atan2((y), (x)))
#   define m3gCos(x)            ((float)cos(x))
#   define m3gSin(x)            ((float)sin(x))
#   define m3gTan(x)            ((float)tan(x))
#   define m3gExp(x)            ((float)exp(x))
#endif

/*----------------------------------------------------------------------
 * Matrix and quaternion stuff
 *--------------------------------------------------------------------*/

static M3Gbool m3gIsWUnity        (const Matrix *mtx);

static void    m3gExpQuat         (Quat *quat, const Vec3 *qExp);
static void    m3gLogQuat         (Vec3 *qLog, const Quat *quat);
static void    m3gLogDiffQuat     (Vec3 *logDiff,
                                   const Quat *from, const Quat *to);
static M3Gint  m3gGetFixedPoint3x3Basis(const Matrix *mtx, M3Gshort *elem);
static M3Gint  m3gGetFixedPointTranslation(const Matrix *mtx, M3Gshort *elem);

/*----------------------------------------------------------------------
 * Bounding boxes
 *--------------------------------------------------------------------*/

static void m3gFitAABB(AABB *box, const AABB *a, const AABB *b);
static void m3gTransformAABB(AABB *box, const Matrix *mtx);
#if defined(M3G_DEBUG)
static void m3gValidateAABB(const AABB *aabb);
#else
#   define m3gValidateAABB(a)
#endif
                             
/*----------------------------------------------------------------------
 * Rounding and conversion
 *--------------------------------------------------------------------*/

static M3Gint  m3gRoundToInt(const M3Gfloat a);

static M3Guint m3gAlpha1f(M3Gfloat a);
/*static M3Guint m3gColor1f(M3Gfloat i);*/
static M3Guint m3gColor3f(M3Gfloat r, M3Gfloat g, M3Gfloat b);
static M3Guint m3gColor4f(M3Gfloat r, M3Gfloat g, M3Gfloat b, M3Gfloat a);
static void    m3gFloatColor(M3Gint argb, M3Gfloat intensity, M3Gfloat *rgba);

static M3Gbool m3gIntersectTriangle(const Vec3 *orig, const Vec3 *dir,
                                    const Vec3 *vert0, const Vec3 *vert1, const Vec3 *vert2,
                                    Vec3 *tuv, M3Gint cullMode);
static M3Gbool m3gIntersectBox(const Vec3 *orig, const Vec3 *dir, const AABB *box);
static M3Gbool m3gIntersectRectangle(M3GRectangle *dst, M3GRectangle *r1, M3GRectangle *r2);

/*----------------------------------------------------------------------
 * Inline functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Multiplies a floating point number by 0.5.
 *
 * \param x the number to multiply
 * \return 0.5 * \c x
 */
static M3G_INLINE M3Gfloat m3gHalf(M3Gfloat x)
{
    M3Guint bits = FLOAT_AS_UINT(x);
    M3Guint mask = 0xff;
    M3Gint exponent = bits & (mask << 23);
    bits ^= exponent;
    exponent = exponent - (1 << 23);
    if (exponent > 0) bits |= exponent;
    return INT_AS_FLOAT(bits);
}

/*!
 * \internal
 * \brief Multiplies a floating point number by two
 *
 * This does NOT handle overflows.
 * 
 * \param x the number to multiply
 * \return 2 * \c x
 */
static M3G_INLINE M3Gfloat m3gDouble(M3Gfloat x)
{
    M3Guint bits = FLOAT_AS_UINT(x) + (1 << 23);
    return INT_AS_FLOAT(bits);
}

/*!
 * \internal
 * \brief Computes the square of a floating point number
 *
 * \param x the input number
 * \return x * x
 */
static M3G_INLINE M3Gfloat m3gSquare(M3Gfloat x)
{
    return m3gMul(x, x);
}

/*!
 * \internal
 * \brief Negates a floating-point value
 */
static M3G_INLINE M3Gfloat m3gNegate(M3Gfloat x)
{
    M3Guint ix = FLOAT_AS_UINT(x) ^ SIGN_MASK;
    return INT_AS_FLOAT(ix);
}

#endif /*__M3G_MATH_H__*/
