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
* Description: Internal math function implementations
*
*/


/*!
 * \internal
 * \file
 * \brief Internal math function implementations
 *
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

#include "m3g_defs.h"
#include "m3g_memory.h"

#if defined(M3G_SOFT_FLOAT)
#include <math.h>
#endif

/*----------------------------------------------------------------------
 * Private types and definitions
 *--------------------------------------------------------------------*/

/* Enumerated common matrix classifications */
#define MC_IDENTITY             0x40100401 // 01000000 00010000 00000100 00000001
#define MC_FRUSTUM              0x30BF0C03 // 00110000 10111111 00001100 00000011
#define MC_PERSPECTIVE          0x30B00C03 // 00110000 10110000 00001100 00000011
#define MC_ORTHO                0x7F300C03 // 01111111 00110000 00001100 00000011
#define MC_PARALLEL             0x70300C03 // 01110000 00110000 00001100 00000011
#define MC_SCALING_ROTATION     0x403F3F3F // 01000000 00111111 00111111 00111111
#define MC_SCALING              0x40300C03 // 01000000 00110000 00001100 00000011
#define MC_TRANSLATION          0x7F100401 // 01111111 00010000 00000100 00000001
#define MC_X_ROTATION           0x403C3C01 // 01000000 00111100 00111100 00000001
#define MC_Y_ROTATION           0x40330433 // 01000000 00110011 00000100 00110011
#define MC_Z_ROTATION           0x40100F0F // 01000000 00010000 00001111 00001111
#define MC_W_UNITY              0x7F3F3F3F // 01111111 00111111 00111111 00111111
#define MC_GENERIC              0xFFFFFFFF

/* Partial masks for individual matrix components */
#define MC_TRANSLATION_PART     0x3F000000 // 00111111 00000000 00000000 00000000
#define MC_SCALE_PART           0x00300C03 // 00000000 00110000 00001100 00000011
#define MC_SCALE_ROTATION_PART  0x003F3F3F // 00000000 00111111 00111111 00111111

/* Matrix element classification masks */
#define ELEM_ZERO       0x00
#define ELEM_ONE        0x01
#define ELEM_MINUS_ONE  0x02
#define ELEM_ANY        0x03

/*!
 * \internal
 * \brief calculates the offset of a 4x4 matrix element in a linear
 * array
 *
 * \notes The current convention is column-major, as in OpenGL ES
 */
#define MELEM(row, col) ((row) + ((col) << 2))

/*!
 * \internal
 * \brief Macro for accessing 4x4 float matrix elements
 *
 * \param mtx pointer to the first element of the matrix
 * \param row matrix row
 * \param col matrix column
 */
#define M44F(mtx, row, col)     ((mtx)->elem[MELEM((row), (col))])

/*--------------------------------------------------------------------*/

/*----------------------------------------------------------------------
 * Private functions
 *--------------------------------------------------------------------*/


/*!
 * \internal
 * \brief ARM VFPv2 implementation of 4x4 matrix multiplication.
 *
 * \param dst	multiplication result
 * \param left	left-hand matrix
 * \param right right-hand matrix
 */
#if defined(M3G_HW_FLOAT_VFPV2)

__asm void _m3gGenericMatrixProduct(Matrix *dst,
                                    const Matrix *left, const Matrix *right)
{
// r0 = *dst
// r1 = *left
// r2 = *right

		CODE32

// save the VFP registers and set the vector STRIDE = 1, LENGTH = 4

		FSTMFDD	sp!, {d8-d15}

		FMRX	r3, FPSCR		 
		BIC		r12, r3, #0x00370000
		ORR		r12, #0x00030000
		FMXR	FPSCR, r12

// left = [a0  a1  a2  a3	right = [b0  b1  b2  b3
//	 	   a4  a5  a6  a7		     b4  b5  b6  b7
//		   a8  a9 a10 a11		     b8  b9 b10 b11
//		  a12 a13 a14 a15]		    b12 b13 b14 b15]
//
// dst = [a0*b0+a4*b1+a8*b2+a12*b3  a1*b0+a5*b1+a9*b2+a13*b3  ..
//		  a0*b4+a4*b5+a8*b6+a12*b7  a1*b4+a5*b5+a9*b6+a13*b7  ..
//					.							.
//					.							.

		FLDMIAS	r1!, {s8-s23}		// load the left matrix [a0-a15] to registers s8-s23
		FLDMIAS	r2!, {s0-s7}		// load [b0-b7] of right matrix to registers s0-s7
		FMULS	s24, s8, s0			// [s24-s27]  = [a0*b0  a1*b0  a2*b0  a3*b0]
		FMULS	s28, s8, s4			// [s28-s31]  = [a0*b4  a1*b4  a2*b4  a3*b4]
		FMACS	s24, s12, s1		// [s24-s27] += [a4*b1  a5*b1  a6*b1  a7*b1]
		FMACS	s28, s12, s5		// [s28-s31] += [a4*b5  a5*b5  a6*b5  a7*b5]
		FMACS	s24, s16, s2		// [s24-s27] += [a8*b2  a9*b2  a10*b2 a11*b2]
		FMACS	s28, s16, s6		// [s28-s31] += [a8*b6  a9*b6  a10*b6 a11*b6]
		FMACS	s24, s20, s3		// [s24-s27] += [a12*b3 a13*b3 a14*b3 a15*b3]
		FMACS	s28, s20, s7		// [s28-s31] += [a12*b7 a13*b37a14*b7 a15*b7]
		FLDMIAS	r2!, {s0-s7}		// load [b8-b15]
		FSTMIAS	r0!, {s24-s31}		// write [dst0-dst7]
		FMULS	s24, s8, s0			
		FMULS	s28, s8, s4			
		FMACS	s24, s12, s1		
		FMACS	s28, s12, s5		
		FMACS	s24, s16, s2		
		FMACS	s28, s16, s6		
		FMACS	s24, s20, s3		
		FMACS	s28, s20, s7		
		FSTMIAS	r0!, {s24-s31}

// Restore the VFP registers and return.

		FMXR	FPSCR, r3

		FLDMFDD	sp!, {d8-d15}	

		BX		lr

}
#endif /* #if defined(M3G_HW_FLOAT_VFPV2) */


/*------------------ Elementary float ------------------*/

#if defined(M3G_SOFT_FLOAT)

#if defined (M3G_BUILD_ARM)

/*!
 * \internal
 * \brief Floating point multiplication implementation for ARM.
 */
__asm M3Gfloat m3gMul(const M3Gfloat a,
                      const M3Gfloat b)
{
    /**
     * Extract the exponents of the multiplicands and add them
     * together. Flush to zero if either exponent or their sum
     * is zero.
     */

    mov     r12, #0xff;
    ands    r2, r0, r12, lsl #23;   // exit if e1 == 0
    andnes  r3, r1, r12, lsl #23;   // exit if e2 == 0
    subne   r2, r2, #(127 << 23);
    addnes  r12, r2, r3;            // exit if e1+e2-127 <= 0
    movle   r0, #0;
    bxle    lr;

    /**
     * Determine the sign of the result. Note that the exponent
     * may have overflowed to the sign bit, and thus the result
     * may be an arbitrary negative value when it really should
     * be +Inf or -Inf.
     */

    teq     r0, r1;
    orrmi   r12, r12, #0x80000000;

    /**
     * Multiply the mantissas. First shift the mantissas up to
     * unsigned 1.31 fixed point, adding the leading "1" bit at
     * the same time, and finally do a 32x32 -> 64 bit unsigned
     * multiplication. The result is in unsigned 2.62 fixed point,
     * representing the interval [1.0, 4.0).
     */

    mov     r2, #0x80000000;
    orr     r0, r2, r0, lsl #8;
    orr     r1, r2, r1, lsl #8;
    umulls  r2, r3, r0, r1;

    /**
     * If the highest bit of the 64-bit result is set, then the
     * mantissa lies in [2.0, 4.0) and needs to be renormalized.
     * That is, the mantissa is shifted one bit to the right and
     * the exponent correspondingly increased by 1. Note that we
     * lose the leading "1" bit from the mantissa by adding it up
     * with the exponent.
     */

    subpl   r12, r12, #(1 << 23);    // no overflow: exponent -= 1
    addpl   r0, r12, r3, lsr #7;     // no overflow: exponent += 1
    addmi   r0, r12, r3, lsr #8;     // overflow: exponent += 1
    bx      lr;
}

/*!
 * \internal
 * \brief Floating point addition implementation for ARM.
 */
__asm M3Gfloat m3gAdd(const M3Gfloat a,
                      const M3Gfloat b)
{
    /**
     * If the operands have opposite signs then this is not really
     * an addition but a subtraction. Subtraction is much slower,
     * so we have a separate code path for it, rather than trying
     * to save space by handling both in the same place.
     */
    
    teq     r0, r1;
    bmi     _m3gSub;

    /**
     * Sort the operands such that the larger operand is in R0 and
     * the smaller in R1. The sign bits do not affect the ordering,
     * since they are known to be equal.
     */
    
    subs    r2, r0, r1;
    submi   r0, r0, r2;
    addmi   r1, r1, r2;

    /**
     * Extract the exponent of the smaller operand into R2 and compute
     * the difference between the larger and smaller exponent into R3.
     * (Note that the sign bits cancel out in the subtraction.) The
     * exponent delta tells how many bits the mantissa of the smaller
     * operand must be shifted to the right in order to bring the
     * operands into equal scale.
     */

    mov     r2, r1, lsr #23;
    rsb     r3, r2, r0, lsr #23;

    /**
     * Check if the exponent delta is bigger than 23 bits, or if the
     * smaller exponent is zero. In either case, exit the routine and
     * return the larger operand (which is already in R0). Note that
     * this means that subnormals are treated as zero.
     */

    rsbs    r12, r3, #23;               // N set, V clear if R3 > 23
    tstpl   r2, #0xff;                  // execute only if R3 <= 23
    bxle    lr;                         // exit if Z set or N != V

    /**
     * Extract the mantissas and shift them up to unsigned 1.31 fixed
     * point, inserting the implied leading "1" bit at the same time.
     * Finally, align the decimal points and add up the mantissas.
     */
    
    mov     r12, #0x80000000;
    orr     r0, r12, r0, lsl #8;
    orr     r1, r12, r1, lsl #8;
    adds    r0, r0, r1, lsr r3;

    /**
     * Compute the final exponent by adding up the smaller exponent
     * (R2), the exponent delta (R3), and the possible overflow bit
     * (carry flag). Note that in case of overflow, the leading "1"
     * has ended up in the carry flag and thus needs not be explicitly
     * discarded. Finally, put the mantissa together with the sign and
     * exponent.
     */

    adc     r2, r2, r3;                 // r2 = smallExp + deltaExp + overflow
    movcc   r0, r0, lsl #1;             // no overflow: discard leading 1
    mov     r0, r0, lsr #9;
    orr     r0, r0, r2, lsl #23;
    bx      lr;
    
_m3gSub

    /**
     * Sort the operands such that the one with larger magnitude is
     * in R0 and has the correct sign (the sign of the final result),
     * while the smaller operand is in R1 with an inverted sign bit.
     */
    
    eor     r1, r1, #0x80000000;
    subs    r2, r0, r1;
    eormi   r2, r2, #0x80000000;
    submi   r0, r0, r2;
    addmi   r1, r1, r2;

    /**
     * Extract the exponent of the smaller operand into R2 and compute
     * the difference between the larger and smaller exponent into R3.
     * (Note that the sign bits cancel out in the subtraction.) The
     * exponent delta tells how many bits the mantissa of the smaller
     * operand must be shifted to the right in order to bring the
     * operands into equal scale.
     */

    mov     r2, r1, lsr #23;
    rsbs    r3, r2, r0, lsr #23;

    /**
     * Check if the exponent delta is bigger than 31 bits, or if the
     * smaller exponent is zero. In either case, exit the routine and
     * return the larger operand (which is already in R0). Note that
     * this means that subnormals are treated as zero.
     */

    rsbs    r12, r3, #31;               // N set, V clear if R3 > 31
    tstpl   r2, #0xff;                  // execute only if R3 <= 31
    bxle    lr;                         // exit if Z set or N != V

    /**
     * Extract the mantissas and shift them up to unsigned 1.31 fixed
     * point, inserting the implied leading "1" bit at the same time.
     * Then align the decimal points and subtract the smaller mantissa
     * from the larger one.
     */
    
    mov     r12, #0x80000000;
    orr     r0, r12, r0, lsl #8;
    orr     r1, r12, r1, lsl #8;
    subs    r0, r0, r1, lsr r3;

    /**
     * We split the range of possible results into three categories:
     * 
     *   1. [1.0, 2.0) ==> no renormalization needed (highest bit set)
     *   2. [0.5, 1.0) ==> only one left-shift needed
     *   3. (0.0, 0.5) ==> multiple left-shifts needed
     *   4. zero       ==> just return
     *   
     * Cases 1 and 2 are handled in the main code path. Cases 3 and 4
     * are less common by far, so we branch to a separate code fragment
     * for those.
     */
    
    movpls  r0, r0, lsl #1;             // Cases 2,3,4: shift left
    bpl     _m3gSubRenormalize;         // Cases 3 & 4: branch out
    
    /**
     * Now we have normalized the mantissa such that the highest bit
     * is set. Here we only need to adjust the exponent, if necessary,
     * and put the pieces together. Note that we lose the leading "1"
     * bit from the mantissa by adding it up with the exponent. We can
     * also do proper rounding (towards nearest) instead of truncation
     * (towards zero) at no extra cost!
     */

    sbc     r3, r3, #1;                 // deltaExp -= 1 (Case 1) or 2 (Case 2)
    add     r2, r2, r3;                 // resultExp = smallExp + deltaExp
    movs    r0, r0, lsr #8;             // shift mantissa, keep leading "1"
    adc     r0, r0, r2, lsl #23;        // resultExp += 1, mantissa += carry
    bx      lr;

    /**
     * Separate code path for cases 3 and 4 (see above). The mantissa
     * has already been shifted up by one, but the exponent has not
     * been correspondingly decreased. We also know that the highest
     * bit is still not set, and that the carry flag is clear.
     */

_m3gSubRenormalize
    bxeq    lr;
    subcc   r3, r3, #2;
    movccs  r0, r0, lsl #2;

    /**
     * If the carry flag is still not set, i.e. there were more than
     * two leading zeros in the mantissa initially, loop until we
     * find the highest set bit.
     */
    
_m3gSubRenormalizeLoop
    subcc   r3, r3, #1;
    movccs  r0, r0, lsl #1;
    bcc     _m3gSubRenormalizeLoop;

    /**
     * Now the leading "1" is in the carry flag, so we can just add up
     * the exponent and mantissa as usual, doing proper rounding at
     * the same time. However, cases where the exponent goes negative
     * (that is, underflows) must be detected and flushed to zero.
     */
    
    add     r3, r2, r3;
    movs    r0, r0, lsr #9;
    adc     r0, r0, r3, lsl #23;
    teq     r0, r2, lsl #23;
    movmi   r0, #0;
    bx      lr;
}

#else /* M3G_BUILD_ARM */

/*!
 * \internal
 * \brief Floating point addition implementation
 *
 */
static M3G_INLINE M3Gfloat m3gFloatAdd(const M3Guint aBits,
                                       const M3Guint bBits)
{
    M3Guint large, small, signMask;
    
    /* Early exits for underflow cases */

    large = (M3Gint)(aBits & ~SIGN_MASK);
    if (large <= 0x00800000) {
        return INT_AS_FLOAT(bBits);
    }
    small = (M3Gint)(bBits & ~SIGN_MASK);
    if (small <= 0x00800000) {
        return INT_AS_FLOAT(aBits);
    }

    /* Swap the numbers so that "large" really is larger; the unsigned
     * (or de-signed) bitmasks for floats are nicely monotonous, so we
     * can compare directly.  Also store the sign of the larger number
     * for future reference. */

    if (small > large) {
        M3Gint temp = small;
        small = large;
        large = temp;
        signMask = (bBits & SIGN_MASK);
    }
    else {
        signMask = (aBits & SIGN_MASK);
    }
    
    {
        M3Guint res, overflow;
        M3Guint resExp, expDelta;
        
        /* Store the larger exponent as our candidate result exponent,
         * and compute the difference between the exponents */

        resExp = (large >> 23);
        expDelta = resExp - (small >> 23);

        /* Take an early exit if the change would be insignificant;
         * this also guards against odd results from shifting by more
         * than 31 (undefined in C) */

        if (expDelta >= 24) {
            res = large | signMask;
            return INT_AS_FLOAT(res);
        }

        /* Convert the mantissas into shifted integers, and shift the
         * smaller number to the same scale with the larger one. */

        large = (large & MANTISSA_MASK) | LEADING_ONE;
        small = (small & MANTISSA_MASK) | LEADING_ONE;
        small >>= expDelta;
        M3G_ASSERT(large >= small);
        
        /* Check whether we're really adding or subtracting the
         * smaller number, and branch to slightly different routines
         * respectively */

        if (((aBits ^ bBits) & SIGN_MASK) == 0) {

            /* Matching signs; just add the numbers and check for
             * overflow, shifting to compensate as necessary. */

            res = large + small;
            
            overflow = (res >> 24);
            res >>= overflow;
            resExp += overflow;
        }
        else {

            /* Different signs, so let's subtract the smaller value;
             * also check that we're not subtracting a number from
             * itself (so we don't have to normalize a zero below) */
            
            if (small == large) {
                return 0.0f; /* x - x = 0 */
            }

            res = (large << 8) - (small << 8);

            /* Renormalize the number by shifting until we've got the
             * high bit in place */

            while ((res >> 24) == 0) {
                res <<= 8;
                resExp -= 8;
            }
            while ((res >> 31) == 0) {
                res <<= 1;
                --resExp;
            }
            res >>= 8;
        }

        /* Flush to zero in case of over/underflow of the exponent */

        if (resExp >= 255) {
            return 0.0f;
        }
        
        /* Compose the final number into "res"; note that we pull in
         * the sign of the original larger number, which should still
         * be valid */

        res &= MANTISSA_MASK;
        res |= (resExp << 23);
        res |= signMask;

        return INT_AS_FLOAT(res);
    }
}

/*!
 * \internal
 * \brief Floating point multiplication implementation
 */
static M3G_INLINE M3Gfloat m3gFloatMul(const M3Guint aBits,
                                       const M3Guint bBits)
{
    M3Guint a, b;

    /* Early exits for underflow and multiplication by zero */

    a = (aBits & ~SIGN_MASK);
    if (a <= 0x00800000) {
        return 0.0f;
    }
    
    b = (bBits & ~SIGN_MASK);
    if (b <= 0x00800000) {
        return 0.0f;
    }
    
    {
        M3Guint res, exponent, overflow;
        
        /* Compute the exponent of the result, assuming the mantissas
         * don't overflow; then mask out the original exponents */
        
        exponent = (a >> 23) + (b >> 23) - 127;
        a &= MANTISSA_MASK;
        b &= MANTISSA_MASK;

        /* Compute the new mantissa from:
         *
         *   (1 + a)(1 + b) = ab + a + b + 1
         *   
         * First shift the mantissas from 0.23 down to 0.16 for the
         * multiplication, then shift back to 0.23 for adding in the
         * "a + b + 1" part of the equation.  */
        
        res = (a >> 7) * (b >> 7);              /* "ab" at 0.32 */
        res = (res >> 9) + a + b + LEADING_ONE;

        /* Add the leading one, then normalize the result by checking
         * the overflow bit and dividing by two if necessary */

        overflow = (res >> 24);
        res >>= overflow;
        exponent += overflow;

        /* Flush to zero in case of over/underflow of the exponent */

        if (exponent >= 255) {
            return 0.0f;
        }

        /* Compose the final number into "res" */

        res &= MANTISSA_MASK;
        res |= (exponent << 23);
        res |= (aBits ^ bBits) & SIGN_MASK;

        return INT_AS_FLOAT(res);
    }
}

#endif /* M3G_BUILD_ARM */

/*!
 * \internal
 * \brief Computes the signed fractional part of a floating point
 * number
 *
 * \param x  floating point value
 * \return x signed fraction of x in ]-1, 1[
 */
static M3Gfloat m3gFrac(M3Gfloat x)
{
    /* Quick exit for -1 < x < 1 */
    
    if (m3gAbs(x) < 1.0f) {
        return x;
    }

    /* Shift the mantissa to the proper place, mask out the integer
     * part, and renormalize */
    {
        M3Guint ix = FLOAT_AS_UINT(x);
        M3Gint expo = ((ix >> 23) & 0xFF) - 127;
        M3G_ASSERT(expo >= 0);

        /* The decimal part will always be zero for large values with
         * exponents over 24 */
        
        if (expo >= 24) {
            return 0.f;
        }
        else {
            
            /* Shift the integer part out of the mantissa and see what
             * we have left */
            
            M3Guint base = (ix & MANTISSA_MASK) | LEADING_ONE;
            base = (base << expo) & MANTISSA_MASK;

            /* Quick exit (and guard against infinite looping) for
             * zero */

            if (base == 0) {
                return 0.f;
            }

            /* We now have an exponent of 0 (i.e. no shifting), but
             * must renormalize to get a set bit in place of the
             * hidden (implicit one) bit */
            
            expo = 0;
            
            while ((base >> 19) == 0) {
                base <<= 4;
                expo -= 4;
            }
            while ((base >> 23) == 0) {
                base <<= 1;
                --expo;
            }

            /* Compose the final number */

            ix =
                (base & MANTISSA_MASK) |
                ((expo + 127) << 23) |
                (ix & SIGN_MASK);
            return INT_AS_FLOAT(ix);
        }
    }
}

#endif /* M3G_SOFT_FLOAT */

#if defined(M3G_DEBUG)
/*!
 * \internal
 * \brief Checks for NaN or infinity in a floating point input
 */
static void m3gValidateFloats(int n, float *p)
{
    while (n-- > 0) {
        M3G_ASSERT(EXPONENT(*p) < 120);
        ++p;
    }
}
#else
#   define m3gValidateFloats(n, p)
#endif
    
/*------------------ Trigonometry and exp ----------*/


#if defined(M3G_SOFT_FLOAT)
/*!
 * \internal
 * \brief Sine for the first quadrant
 *
 * \param x floating point value in [0, PI/2]
 * \return sine of \c x
 */
static M3Gfloat m3gSinFirstQuadrant(M3Gfloat x)
{
    M3Guint bits = FLOAT_AS_UINT(x);
    
    if (bits <= 0x3ba3d70au)    /* 0.005f */
        return x;
    else {
        static const M3Gfloat sinTermLut[4] = {
            -1.0f / (2*3),
            -1.0f / (4*5),
            -1.0f / (6*7),
            -1.0f / (8*9)
        };

        M3Gfloat xx = m3gSquare(x);
        M3Gfloat sinx = x;
        int i;

        for (i = 0; i < 4; ++i) {
            x    = m3gMul(x, m3gMul(xx, sinTermLut[i]));
            sinx = m3gAdd(sinx, x);
        }

        return sinx;
    }
}
#endif /* M3G_SOFT_FLOAT */

#if defined(M3G_SOFT_FLOAT)
/*!
 * \internal
 * \brief Computes sine for the first period
 *
 * \param x floating point value in [0, 2PI]
 * \return sine of x
 */
static M3Gfloat m3gSinFirstPeriod(const M3Gfloat x)
{
    M3G_ASSERT(x >= 0 && x <= TWO_PI);
    
    if (x >= PI) {
        return m3gNegate(m3gSinFirstQuadrant(x >= ONE_AND_HALF_PI ?
                                             m3gSub(TWO_PI, x) :
                                             m3gSub(x, PI)));
    }
    return m3gSinFirstQuadrant((x >= HALF_PI) ? m3gSub(PI, x) : x);
}
#endif /* M3G_SOFT_FLOAT */

/*------------- Float vs. int conversion helpers -------------*/

/*!
 * \internal
 * \brief Scales and clamps a float to unsigned byte range
 */
static M3G_INLINE M3Gint m3gFloatToByte(const M3Gfloat a)
{
    return m3gRoundToInt(m3gMul(255.f, m3gClampFloat(a, 0.f, 1.f)));
}

/*------------------ Vector helpers ------------------*/

/*!
 * \internal
 * \brief Computes the norm of a floating-point 3-vector
 */
static M3G_INLINE M3Gfloat m3gNorm3(const M3Gfloat *v)
{
    return m3gAdd(m3gAdd(m3gSquare(v[0]), m3gSquare(v[1])),
                  m3gSquare(v[2]));
}

/*!
 * \internal
 * \brief Computes the norm of a floating-point 4-vector
 */
static M3G_INLINE M3Gfloat m3gNorm4(const M3Gfloat *v)
{
    return m3gAdd(m3gAdd(m3gSquare(v[0]), m3gSquare(v[1])),
                  m3gAdd(m3gSquare(v[2]), m3gSquare(v[3])));
}

/*!
 * \internal
 * \brief Scales a floating-point 3-vector
 */
static M3G_INLINE void m3gScale3(M3Gfloat *v, M3Gfloat s)
{
    v[0] = m3gMul(v[0], s);
    v[1] = m3gMul(v[1], s);
    v[2] = m3gMul(v[2], s);
}

/*!
 * \internal
 * \brief Scales a floating-point 4-vector
 */
static M3G_INLINE void m3gScale4(M3Gfloat *v, M3Gfloat s)
{
    v[0] = m3gMul(v[0], s);
    v[1] = m3gMul(v[1], s);
    v[2] = m3gMul(v[2], s);
    v[3] = m3gMul(v[3], s);
}


/*------------------ Matrices ------------------*/

/*!
 * \internal
 */
static M3G_INLINE M3Gbool m3gIsClassified(const Matrix *mtx)
{
    M3G_ASSERT(mtx != NULL);
    return (M3Gbool) mtx->classified;
}

/*!
 * \internal
 * \brief Returns a classification for a single floating point number
 */
static M3G_INLINE M3Guint m3gElementClass(const M3Gfloat x)
{
    if (IS_ZERO(x)) {
        return ELEM_ZERO;
    }
    else if (IS_ONE(x)) {
        return ELEM_ONE;
    }
    else if (IS_MINUS_ONE(x)) {
        return ELEM_MINUS_ONE;
    }
    return ELEM_ANY;
}

/*!
 * \internal
 * \brief Computes the classification mask of a matrix
 *
 * The mask is constructed from two bits per elements, with the lowest
 * two bits corresponding to the first element in the \c elem array of
 * the matrix.
 */
static void m3gClassify(Matrix *mtx)
{
    M3Guint mask = 0;
    const M3Gfloat *p;
    int i;

    M3G_ASSERT(mtx != NULL);
    M3G_ASSERT(!m3gIsClassified(mtx));

    p = mtx->elem;
    for (i = 0; i < 16; ++i) {
        M3Gfloat elem = *p++;
        mask |= (m3gElementClass(elem) << (i*2));
    }
    mtx->mask = mask;
    mtx->classified = M3G_TRUE;
}

/*!
 * \internal
 * \brief Sets matrix classification directly
 */
static M3G_INLINE void m3gClassifyAs(M3Guint mask, Matrix *mtx)
{
    M3G_ASSERT(mtx != NULL);
    mtx->mask = mask;
    mtx->classified = M3G_TRUE;
    mtx->complete = M3G_FALSE;
}

/*!
 * \internal
 * \brief Attempts to classify a matrix more precisely
 *
 * Tries to classify all "free" elements of a matrix into one of the
 * predefined constants to improve precision and performance in
 * subsequent calculations.
 */
static void m3gSubClassify(Matrix *mtx)
{
    M3G_ASSERT_PTR(mtx);
    M3G_ASSERT(m3gIsClassified(mtx));
    {
        const M3Gfloat *p = mtx->elem;
        M3Guint inMask = mtx->mask;
        M3Guint outMask = inMask;
        int i;

        for (i = 0; i < 16; ++i, inMask >>= 2) {
            M3Gfloat elem = *p++;
            if ((inMask & 0x03) == ELEM_ANY) {
                outMask &= ~(0x03u << (i*2));
                outMask |= (m3gElementClass(elem) << (i*2));
            }
        }
        mtx->mask = outMask;
    }
}

/*!
 * \internal
 * \brief Fills in the implicit values for a classified matrix
 */
static void m3gFillClassifiedMatrix(Matrix *mtx)
{
    int i;
    M3Guint mask;
    M3Gfloat *p;

    M3G_ASSERT(mtx != NULL);
    M3G_ASSERT(mtx->classified);
    M3G_ASSERT(!mtx->complete);

    mask = mtx->mask;
    p = mtx->elem;

    for (i = 0; i < 16; ++i, mask >>= 2) {
        unsigned elem = (mask & 0x03);
        switch (elem) {
        case ELEM_ZERO:         *p++ =  0.0f; break;
        case ELEM_ONE:          *p++ =  1.0f; break;
        case ELEM_MINUS_ONE:    *p++ = -1.0f; break;
        default:                ++p;
        }
    }
    mtx->complete = M3G_TRUE;
}


#if !defined(M3G_HW_FLOAT)
/*!
 * \internal
 * \brief Performs one multiply-add of classified matrix elements
 *
 * \param amask element class of the first multiplicand
 * \param a     float value of the first multiplicand
 * \param bmask element class of the second multiplicand
 * \param b     float value of the second multiplicand
 * \param c     float value to add
 * \return a * b + c
 * 
 * \notes inline, as only called from the matrix product function
 */
static M3G_INLINE M3Gfloat m3gClassifiedMadd(
    const M3Gbitmask amask, const M3Gfloat *pa,
    const M3Gbitmask bmask, const M3Gfloat *pb,
    const M3Gfloat c)
{
    /* Check for zero product to reduce the switch cases below */
    
    if (amask == ELEM_ZERO || bmask == ELEM_ZERO) {
        return c;    
    }

    /* Branch based on the classification of a */
    
    switch (amask) {
        
    case ELEM_ANY:
        if (bmask == ELEM_ONE) {
            return m3gAdd(*pa, c);      /*  a * 1 + c  =  a + c  */
        }
        if (bmask == ELEM_MINUS_ONE) {
            return m3gSub(c, *pa);      /*  a * -1 + c = -a + c  */
        }
        return m3gMadd(*pa, *pb, c);    /*  a * b + c            */
        
    case ELEM_ONE:
        if (bmask == ELEM_ONE) {
            return m3gAdd(c, 1.f);      /*  1 * 1 + c  = 1 + c   */
        }
        if (bmask == ELEM_MINUS_ONE) {
            return m3gSub(c, 1.f);      /*  1 * -1 + c = -1 + c  */
        }
        return m3gAdd(*pb, c);          /*  1 * b + c  =  b + c  */
        
    case ELEM_MINUS_ONE:
        if (bmask == ELEM_ONE) {
            return m3gSub(c, 1.f);      /* -1 * 1 + c  = -1 + c  */
        }
        if (bmask == ELEM_MINUS_ONE) {
            return m3gAdd(c, 1.f);      /* -1 * -1 + c =  1 + c  */
        }
        return m3gSub(c, *pb);          /* -1 * b + c  = -b + c  */
        
    default:
        M3G_ASSERT(M3G_FALSE);
        return 0.0f;
    }
}
#endif /*!defined(M3G_HW_FLOAT)*/

/*!
 * \internal
 * \brief Computes a generic 4x4 matrix product
 */
static void m3gGenericMatrixProduct(Matrix *dst,
                                    const Matrix *left, const Matrix *right)
{
    M3G_ASSERT(dst != NULL && left != NULL && right != NULL);

    {
#       if defined(M3G_HW_FLOAT)
        if (!left->complete) {
            m3gFillClassifiedMatrix((Matrix*)left);
        }
        if (!right->complete) {
            m3gFillClassifiedMatrix((Matrix*)right);
        }
#       else
        const unsigned lmask = left->mask;
        const unsigned rmask = right->mask;
#       endif
        
#if defined(M3G_HW_FLOAT_VFPV2)
		_m3gGenericMatrixProduct(dst, left, right);
#else	
        {
            int row;

            for (row = 0; row < 4; ++row) {
                int col;
                for (col = 0; col < 4; ++col) {
                    int k;
                    M3Gfloat a = 0;
                    for (k = 0; k < 4; ++k) {
                        M3Gint lidx = MELEM(row, k);
                        M3Gint ridx = MELEM(k, col);
#                       if defined(M3G_HW_FLOAT)
                        a = m3gMadd(left->elem[lidx], right->elem[ridx], a);
#                       else
                        a = m3gClassifiedMadd((lmask >> (2 * lidx)) & 3,
                                              &left->elem[lidx],
                                              (rmask >> (2 * ridx)) & 3,
                                              &right->elem[ridx],
                                              a);
#                       endif /*!M3G_HW_FLOAT*/
                    }
                    M44F(dst, row, col) = a;
                }
            }
        }
#endif /*!M3G_HW_FLOAT_VFPV2*/
    }
    dst->complete = M3G_TRUE;
    dst->classified = M3G_FALSE;
}

/*!
 * \internal
 * \brief Converts a float vector to 16-bit integers
 *
 * \param size   vector length
 * \param vec    pointer to float vector
 * \param scale  scale of output values, as the number of bits to
 *               shift left to get actual values
 * \param outVec output value vector
 */
static void m3gFloatVecToShort(M3Gint size, const M3Gfloat *vec,
                               M3Gint scale, M3Gshort *outVec)
{
    const M3Guint *vecInt = (const M3Guint*) vec;
    M3Gint i;
    
    for (i = 0; i < size; ++i) {
        M3Guint a = vecInt[i];
        if ((a & ~SIGN_MASK) < (1 << 23)) {
            *outVec++ = 0;
        }
        else {
            M3Gint shift =
                scale - ((M3Gint)((vecInt[i] >> 23) & 0xFFu) - (127 + 23));
            M3G_ASSERT(shift > 8); /* or the high bits will overflow */
            
            if (shift > 23) {
                *outVec++ = 0;
            }
            else {
                M3Gint out =
                    (M3Gint) (((a & MANTISSA_MASK) | LEADING_ONE) >> shift);
                if (a >> 31) {
                    out = -out;
                }
                M3G_ASSERT(m3gInRange(out, -32767, 32767));
                *outVec++ = (M3Gshort) out;
            }
        }
    }
}

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*--------------------------------------------------------------------*/

#if defined(M3G_SOFT_FLOAT)

#if !defined (M3G_BUILD_ARM)

static M3Gfloat m3gAdd(const M3Gfloat a, const M3Gfloat b)
{
    return m3gFloatAdd(FLOAT_AS_UINT(a), FLOAT_AS_UINT(b));
}

static M3Gfloat m3gMul(const M3Gfloat a, const M3Gfloat b)
{
    return m3gFloatMul(FLOAT_AS_UINT(a), FLOAT_AS_UINT(b));
}

#endif /* M3G_BUILD_ARM */

/*!
 * \internal
 * \brief Computes the reciprocal of the square root
 *
 * \param x a floating point value
 * \return 1 / square root of \c x
 */
static M3Gfloat m3gRcpSqrt(const M3Gfloat x)
{
    /* Approximation followed by Newton-Raphson iteration a'la
     * "Floating-point tricks" by J. Blinn, but we iterate several
     * times to improve precision */
    
    M3Gint i = (M3G_FLOAT_ONE + (M3G_FLOAT_ONE >> 1))
        - (FLOAT_AS_UINT(x) >> 1);
    M3Gfloat y = INT_AS_FLOAT(i);
    for (i = 0; i < 3; ++i) {
        y = m3gMul(y, m3gSub(1.5f, m3gHalf(m3gMul(x, m3gSquare(y)))));
    }
    return y;
}

/*!
 * \internal
 * \brief Computes the square root
 *
 * \param x a floating point value
 * \return square root of \c x
 */
static M3Gfloat m3gSqrt(const M3Gfloat x)
{
    /* Approximation followed by Newton-Raphson iteration a'la
     * "Floating-point tricks" by J. Blinn, but we iterate several
     * times to improve precision */

    M3Gint i = (FLOAT_AS_UINT(x) >> 1) + (M3G_FLOAT_ONE >> 1);
    M3Gfloat y = INT_AS_FLOAT(i);
    for (i = 0; i < 2; ++i) {
        y = m3gDiv(m3gAdd(m3gSquare(y), x), m3gDouble(y));
    }
    return y;
}

#endif /* M3G_SOFT_FLOAT */

/*--------------------------------------------------------------------*/

#if defined(M3G_SOFT_FLOAT)
/*!
 * \internal
 */
static M3Gfloat m3gArcCos(const M3Gfloat x)
{
    return (M3Gfloat) acos(x);
}

/*!
 * \internal
 */
static M3Gfloat m3gArcTan(const M3Gfloat y, const M3Gfloat x)
{
    return (M3Gfloat) atan2(y, x);
}

/*!
 * \internal
 */
static M3Gfloat m3gCos(const M3Gfloat x)
{
    return m3gSin(m3gAdd(x, HALF_PI));
}

/*!
 * \internal
 * \brief
 */
static M3Gfloat m3gSin(const M3Gfloat x)
{
    M3Gfloat f = x;
    
    /* If x is greater than two pi, do a modulo operation to bring it
     * back in range for the internal sine function */
    
    if (m3gAbs(f) >= TWO_PI) {
        f = m3gMul (f, (1.f / TWO_PI));
        f = m3gFrac(f);
        f = m3gMul (f, TWO_PI);
    }

    /* Compute the result, negating both the input value and the
     * result if x was negative */
    {
        M3Guint i = FLOAT_AS_UINT(f);
        M3Guint neg = (i & SIGN_MASK);
        i ^= neg;
        f = m3gSinFirstPeriod(INT_AS_FLOAT(i));
        i = FLOAT_AS_UINT(f) ^ neg;
        return INT_AS_FLOAT(i);
    }
}

/*!
 * \internal
 */
static M3Gfloat m3gTan(const M3Gfloat x)
{
    return (M3Gfloat) tan(x);
}

/*!
 * \internal
 */
static M3Gfloat m3gExp(const M3Gfloat a)
{
    return (M3Gfloat) exp(a);
}
#endif /* M3G_SOFT_FLOAT */

/*!
 * \brief Checks whether the bottom row of a matrix is 0 0 0 1
 */
static M3Gbool m3gIsWUnity(const Matrix *mtx)
{
    M3G_ASSERT_PTR(mtx);

    if (!m3gIsClassified(mtx)) {
        return (IS_ZERO(M44F(mtx, 3, 0)) &&
                IS_ZERO(M44F(mtx, 3, 1)) &&
                IS_ZERO(M44F(mtx, 3, 2)) &&
                IS_ONE (M44F(mtx, 3, 3)));
    }
    else {
        return ((mtx->mask & 0xC0C0C0C0) == (ELEM_ONE << 30));
    }
}

/*!
 * \brief Makes a quaternion by exponentiating a quaternion logarithm
 */
static void m3gExpQuat(Quat *quat, const Vec3 *qExp)
{
    M3Gfloat theta;

    M3G_ASSERT_PTR(quat);
    M3G_ASSERT_PTR(qExp);

    theta = m3gSqrt(m3gAdd(m3gAdd(m3gSquare(qExp->x),
                                  m3gSquare(qExp->y)),
                           m3gSquare(qExp->z)));

    if (theta > EPSILON) {
        M3Gfloat s = m3gMul(m3gSin(theta), m3gRcp(theta));
        quat->x = m3gMul(qExp->x, s);
        quat->y = m3gMul(qExp->y, s);
        quat->z = m3gMul(qExp->z, s);
        quat->w = m3gCos(theta);
    }
    else {
        quat->x = quat->y = quat->z = 0.0f;
        quat->w = 1.0f;
    }
}

/*!
 * \brief Natural logarithm of a unit quaternion.
 */
static void m3gLogQuat(Vec3 *qLog, const Quat *quat)
{
    M3Gfloat sinTheta = m3gSqrt(m3gNorm3((const float *) quat));

    if (sinTheta > EPSILON) {
        M3Gfloat s = m3gArcTan(sinTheta, quat->w) / sinTheta;
        qLog->x = m3gMul(s, quat->x);
        qLog->y = m3gMul(s, quat->y);
        qLog->z = m3gMul(s, quat->z);
    }
    else {
        qLog->x = qLog->y = qLog->z = 0.0f;
    }
}

/*!
 * \brief Make quaternion the "logarithmic difference" between two
 * other quaternions.
 */
static void m3gLogDiffQuat(Vec3 *logDiff,
                           const Quat *from, const Quat *to)
{
    Quat temp;
    temp.x = m3gNegate(from->x);
    temp.y = m3gNegate(from->y);
    temp.z = m3gNegate(from->z);
    temp.w =           from->w;
    m3gMulQuat(&temp, to);
    m3gLogQuat(logDiff, &temp);
}

/*!
 * \brief Rounds a float to the nearest integer
 *
 * Overflows are clamped to the maximum or minimum representable
 * value.
 */
static M3Gint m3gRoundToInt(const M3Gfloat a)
{
    M3Guint base = FLOAT_AS_UINT(a);
    M3Gint signMask, expo;

    /* Decompose the number into sign, exponent, and base number */
    
    signMask = ((M3Gint) base >> 31);   /* -> 0 or 0xFFFFFFFF */
    expo = (M3Gint)((base >> 23) & 0xFF) - 127;
    
    /* First check for large values and return either the negative or
     * the positive maximum integer in case of overflow.  The overflow
     * check can be made on the exponent alone, as large floats are
     * spaced several integer values apart so that nothing will
     * overflow because of rounding later on */
    
    if (expo >= 31) {
        return (M3Gint)((1U << 31) - 1 + (((M3Guint) signMask) & 1));
    }

    /* Also check for underflow to avoid problems with shifting by
     * more than 31 */

    if (expo < -1) {
        return 0;
    }
    
    /* Mask out the sign and exponent bits, shift the base number so
     * that the lowest bit corresponds to one half, then add one
     * (half) and shift to round to the closest integer. */

    base = (base | LEADING_ONE) << 8;   /* shift mantissa to 1.31 */
    base =  base >> (30 - expo);        /* integer value as 31.1 */
    base = (base + 1) >> 1;             /* round to nearest 32.0 */
    
    /* Factor in the sign (negate if originally negative) and
     * return */

    return ((M3Gint) base ^ signMask) - signMask;
}

/*!
 * \brief Calculates ray-triangle intersection.
 *
 * http://www.acm.org/jgt/papers/MollerTrumbore97
 */
static M3Gbool m3gIntersectTriangle(const Vec3 *orig, const Vec3 *dir,
                                    const Vec3 *vert0, const Vec3 *vert1, const Vec3 *vert2,
                                    Vec3 *tuv, M3Gint cullMode)
{
    Vec3 edge1, edge2, tvec, pvec, qvec;
    M3Gfloat det,inv_det;

    /* find vectors for two edges sharing vert0 */
    edge1 = *vert1;
    edge2 = *vert2;
    m3gSubVec3(&edge1, vert0);
    m3gSubVec3(&edge2, vert0);

    /* begin calculating determinant - also used to calculate U parameter */
    m3gCross(&pvec, dir, &edge2);

    /* if determinant is near zero, ray lies in plane of triangle */
    det = m3gDot3(&edge1, &pvec);

    if (cullMode == 0 && det <= 0) return M3G_FALSE;
    if (cullMode == 1 && det >= 0) return M3G_FALSE;

    if (det > -EPSILON && det < EPSILON)
        return M3G_FALSE;
    inv_det = m3gRcp(det);

    /* calculate distance from vert0 to ray origin */
    tvec = *orig;
    m3gSubVec3(&tvec, vert0);

    /* calculate U parameter and test bounds */
    tuv->y = m3gMul(m3gDot3(&tvec, &pvec), inv_det);
    if (tuv->y < 0.0f || tuv->y > 1.0f)
        return M3G_FALSE;

    /* prepare to test V parameter */
    m3gCross(&qvec, &tvec, &edge1);

    /* calculate V parameter and test bounds */
    tuv->z = m3gMul(m3gDot3(dir, &qvec), inv_det);
    if (tuv->z < 0.0f || m3gAdd(tuv->y, tuv->z) > 1.0f)
        return M3G_FALSE;

    /* calculate t, ray intersects triangle */
    tuv->x = m3gMul(m3gDot3(&edge2, &qvec), inv_det);

    return M3G_TRUE;
}

/*!
 * \brief Calculates ray-box intersection.
 *
 * http://www.siggraph.org/education/materials/HyperGraph/raytrace/rtinter3.htm
 */

#define XO	orig->x
#define YO	orig->y
#define ZO	orig->z
#define XD	dir->x
#define YD	dir->y
#define ZD	dir->z

#define XL	box->min[0]
#define YL	box->min[1]
#define ZL	box->min[2]
#define XH	box->max[0]
#define YH	box->max[1]
#define ZH	box->max[2]

/*!
 * \internal
 * \brief Ray - bounding box intersection
 *
 */
static M3Gbool m3gIntersectBox(const Vec3 *orig, const Vec3 *dir, const AABB *box)
{
	M3Gfloat tnear = M3G_MIN_NEGATIVE_FLOAT;
	M3Gfloat tfar  = M3G_MAX_POSITIVE_FLOAT;
	M3Gfloat t1, t2, temp;

	/* X slab */
	if(XD != 0) {
		t1 = m3gSub(XL, XO) / XD;
		t2 = m3gSub(XH, XO) / XD;

		if(t1 > t2) {
			temp = t1;
			t1 = t2;
			t2 = temp;
		}

		if(t1 > tnear) tnear = t1;
		if(t2 < tfar) tfar = t2;

		if(tnear > tfar) return M3G_FALSE;
		if(tfar < 0) return M3G_FALSE;
	}
	else {
		if(XO > XH || XO < XL) return M3G_FALSE;
	}

	/* Y slab */
	if(YD != 0) {
		t1 = m3gSub(YL, YO) / YD;
		t2 = m3gSub(YH, YO) / YD;

		if(t1 > t2) {
			temp = t1;
			t1 = t2;
			t2 = temp;
		}

		if(t1 > tnear) tnear = t1;
		if(t2 < tfar) tfar = t2;

		if(tnear > tfar) return M3G_FALSE;
		if(tfar < 0) return M3G_FALSE;
	}
	else {
		if(YO > YH || YO < YL) return M3G_FALSE;
	}

	/* Z slab */
	if(ZD != 0) {
		t1 = m3gSub(ZL, ZO) / ZD;
		t2 = m3gSub(ZH, ZO) / ZD;

		if(t1 > t2) {
			temp = t1;
			t1 = t2;
			t2 = temp;
		}

		if(t1 > tnear) tnear = t1;
		if(t2 < tfar) tfar = t2;

		if(tnear > tfar) return M3G_FALSE;
		if(tfar < 0) return M3G_FALSE;
	}
	else {
		if(ZO > ZH || ZO < ZL) return M3G_FALSE;
	}

	return M3G_TRUE;
}

/*!
 * \brief Calculates the intersection of two rectangles. Always fills
 * the intersection result.
 *
 * \param dst   result of the intersection
 * \param r1    rectangle 1
 * \param r2    rectangle 2
 */
static M3Gbool m3gIntersectRectangle(M3GRectangle *dst, M3GRectangle *r1, M3GRectangle *r2)
{
    M3Gbool intersects = M3G_TRUE;
    M3Gint min, max;

    max = (r1->x) >= (r2->x) ? (r1->x) : (r2->x);
    min = (r1->x + r1->width) <= (r2->x + r2->width) ? (r1->x + r1->width) : (r2->x + r2->width);
    if ((min - max) < 0) intersects = M3G_FALSE;
    dst->x = max;
    dst->width = min - max;

    max = (r1->y) >= (r2->y) ? (r1->y) : (r2->y);
    min = (r1->y + r1->height) <= (r2->y + r2->height) ? (r1->y + r1->height) : (r2->y + r2->height);
    if ((min - max) < 0) intersects = M3G_FALSE;
    dst->y = max;
    dst->height = min - max;

    return intersects;
}

/*-------- float-to-int color conversions --------*/

static M3Guint m3gAlpha1f(M3Gfloat a)
{
    M3Guint alpha = (M3Guint) m3gFloatToByte(a);
    return (alpha << 24) | M3G_RGB_MASK;
}

static M3Guint m3gColor3f(M3Gfloat r, M3Gfloat g, M3Gfloat b)
{
    return ((M3Guint) m3gFloatToByte(r) << 16)
        |  ((M3Guint) m3gFloatToByte(g) <<  8)
        |   (M3Guint) m3gFloatToByte(b)
        |   M3G_ALPHA_MASK;
}

static M3Guint m3gColor4f(M3Gfloat r, M3Gfloat g, M3Gfloat b, M3Gfloat a)
{
    return ((M3Guint) m3gFloatToByte(r) << 16)
        |  ((M3Guint) m3gFloatToByte(g) <<  8)
        |   (M3Guint) m3gFloatToByte(b)
        |  ((M3Guint) m3gFloatToByte(a) << 24);
}

static void m3gFloatColor(M3Gint argb, M3Gfloat intensity, M3Gfloat *rgba)
{
    /* NOTE we intentionally aim a bit high here -- some GL
     * implementations may round down instead of closest */
    
    const M3Gfloat oneOver255 = (M3Gfloat)(1.0001 / 255.0);
    
	rgba[0] = (M3Gfloat)((argb >> 16) & 0xFF);
	rgba[1] = (M3Gfloat)((argb >>  8) & 0xFF);
	rgba[2] = (M3Gfloat)((argb      ) & 0xFF);
	rgba[3] = (M3Gfloat)((argb >> 24) & 0xFF);
    
    m3gScale4(rgba, m3gMul(oneOver255, intensity));
}

/*!
 * \brief Converts the 3x3 submatrix of a matrix into fixed point
 *
 * The input matrix must be an affine one, i.e. the bottom row must be
 * 0 0 0 1.  The output matrix is guaranteed to be such that it can be
 * multiplied with a 16-bit 3-vector without overflowing, while using
 * the 32-bit range optimally.
 *
 * \param mtx  the original matrix
 * \param elem 9 shorts to hold the fixed point base numbers
 * \return floating point exponent for the values in \c elem
 *         (number of bits to shift left for actual values)
 */
static M3Gint m3gGetFixedPoint3x3Basis(const Matrix *mtx, M3Gshort *elem)
{
    M3Gint outExp;
    M3Gint row, col;
    const M3Guint *m;
    
    if (!mtx->complete) {
        m3gFillClassifiedMatrix((Matrix*) mtx);
    }
    m = (const M3Guint*) mtx->elem;
    
    /* First, find the maximum exponent value in the whole matrix */

    outExp = 0;
    for (col = 0; col < 3; ++col) {
        for (row = 0; row < 3; ++row) {
            M3Gint element = (M3Gint)(m[MELEM(row, col)] & ~SIGN_MASK);
            outExp = M3G_MAX(outExp, element);
        }
    }
    outExp >>= 23;

    /* Our candidate exponent is the maximum found plus 9, which is
     * guaranteed to shift the maximum unsigned 24-bit mantissa (which
     * always will have the high bit set) down to the signed 16-bit
     * range */

    outExp += 9;
    
    /* Now proceed to sum each row and see what's the actual smallest
     * exponent we can safely use without overflowing in a 16+16
     * matrix-vector multiplication; this will win us one bit
     * (doubling the precision) compared to the conservative approach
     * of just shifting everything down by 10 bits */

    for (row = 0; row < 3; ++row) {

        /* Sum the absolute values on this row */
            
        M3Gint rowSum = 0;
        for (col = 0; col < 3; ++col) {
            M3Gint a = (M3Gint)(m[MELEM(row, col)] & ~SIGN_MASK);
            M3Gint shift = outExp - (a >> 23);
            M3G_ASSERT(shift < 265);
                
            if (shift < 24) {
                rowSum += ((a & MANTISSA_MASK) | LEADING_ONE) >> shift;
            }
        }

        /* Now we have a 26-bit sum of the absolute values on this
         * row, and shift that down until we fit the target range of
         * [0, 65535].  Note that this still leaves *exactly* enough
         * space for adding in an arbitrary 16-bit translation vector
         * after multiplying with the matrix! */
            
        while (rowSum >= (1 << 16)) {
            rowSum >>= 1;
            ++outExp;
        }
    }

    /* De-bias the exponent, but add in an extra 23 to account for the
     * decimal bits in the floating point mantissa values we started
     * with (we're returning the exponent as "bits to shift left to
     * get integers", so we're off by 23 from IEEE notation) */
    
    outExp = (outExp - 127) - 23;
    
    /* Finally, shift all the matrix elements to our final output
     * precision */
    
    for (col = 0; col < 3; ++col) {
        m3gFloatVecToShort(3, mtx->elem + MELEM(0, col), outExp, elem);
        elem += 3;
    }
    return outExp;
}

/*!
 * \brief Gets the translation component of a matrix as fixed point
 *
 * \param mtx  the matrix
 * \param elem 3 shorts to write the vector into
 * \return floating point exponent for the values in \c elem
 *         (number of bits to shift left for actual values)
 */
static M3Gint m3gGetFixedPointTranslation(const Matrix *mtx, M3Gshort *elem)
{
    const M3Guint *m;
    
    M3G_ASSERT(m3gIsWUnity(mtx));
    if (!mtx->complete) {
        m3gFillClassifiedMatrix((Matrix*) mtx);
    }
    m = (const M3Guint*) &mtx->elem[MELEM(0, 3)];

    /* Find the maximum exponent, then scale down by 9 bits from that
     * to shift the unsigned 24-bit mantissas to the signed 16-bit
     * range */
    {
        M3Gint outExp;
        M3Guint maxElem = m[0] & ~SIGN_MASK;
        maxElem = M3G_MAX(maxElem, m[1] & ~SIGN_MASK);
        maxElem = M3G_MAX(maxElem, m[2] & ~SIGN_MASK);
        
        outExp = (M3Gint)(maxElem >> 23) - (127 + 23) + 9;
        m3gFloatVecToShort(3, mtx->elem + MELEM(0, 3), outExp, elem);
        return outExp;
    }
}

/*!
 * \internal
 * \brief Compute a bounding box enclosing two other boxes
 *
 * \param box   box to fit
 * \param a     first box to enclose or NULL
 * \param b     second box to enclose or NULL
 * 
 * \note If both input boxes are NULL, the box is not modified.
 */
static void m3gFitAABB(AABB *box, const AABB *a, const AABB *b)
{
    int i;

    M3G_ASSERT_PTR(box);
    
    if (a) {
        m3gValidateAABB(a);
    }
    if (b) {
        m3gValidateAABB(b);
    }

    if (a && b) {
        for (i = 0; i < 3; ++i) {
            box->min[i] = M3G_MIN(a->min[i], b->min[i]);
            box->max[i] = M3G_MAX(a->max[i], b->max[i]);
        }
    }
    else if (a) {
        *box = *a;
    }
    else if (b) {
        *box = *b;
    }
}

/*
 * \internal
 * \brief Transform an axis-aligned bounding box with a matrix
 *
 * This results in a box that encloses the transformed original box.
 * 
 * Based on "Transforming Axis-Aligned Bounding Boxes" by Jim Arvo
 * from Graphics Gems.
 * 
 * \note The bottom row of the matrix is ignored in the transformation.
 */
static void m3gTransformAABB(AABB *box, const Matrix *mtx)
{
    M3Gfloat boxMin[3], boxMax[3];
    M3Gfloat newMin[3], newMax[3];

    m3gValidateAABB(box);
    
    if (!mtx->complete) {
        m3gFillClassifiedMatrix((Matrix*) mtx);
    }

    /* Get the original minimum and maximum extents as floats, and add
     * the translation as the base for the transformed box */
    {
        int i;
        for (i = 0; i < 3; ++i) {
            boxMin[i] = box->min[i];
            boxMax[i] = box->max[i];
            newMin[i] = newMax[i] = M44F(mtx, i, 3);
        }
    }

    /* Transform into the new minimum and maximum coordinates using
     * the upper left 3x3 part of the matrix */
    {
        M3Gint row, col;
        
        for (row = 0; row < 3; ++row) {
            for (col = 0; col < 3; ++col) {
                M3Gfloat a = m3gMul(M44F(mtx, row, col), boxMin[col]);
                M3Gfloat b = m3gMul(M44F(mtx, row, col), boxMax[col]);
                
                if (a < b) { 
                    newMin[row] = m3gAdd(newMin[row], a);
                    newMax[row] = m3gAdd(newMax[row], b);
                }
                else { 
                    newMin[row] = m3gAdd(newMin[row], b);
                    newMax[row] = m3gAdd(newMax[row], a);
                }
            }
        }
    }

    /* Write back into the bounding box */
    {
        int i;
        for (i = 0; i < 3; ++i) {
            box->min[i] = newMin[i];
            box->max[i] = newMax[i];
        }
    }
    
    m3gValidateAABB(box);
}

#if defined(M3G_DEBUG)
/*!
 * \brief
 */
static void m3gValidateAABB(const AABB *aabb)
{
    m3gValidateFloats(6, (float*) aabb);
}
#endif

/*----------------------------------------------------------------------
 * Public functions
 *--------------------------------------------------------------------*/

/*!
 * \brief Linear interpolation of vectors
 *
 * \param size     number of components
 * \param vec      output vector
 * \param s        interpolation factor
 * \param start    initial value
 * \param end      target value
 */
#if defined(M3G_HW_FLOAT_VFPV2)

__weak __asm void m3gLerp(M3Gint size,
				   M3Gfloat *vec,
				   M3Gfloat s,
				   const M3Gfloat *start, const M3Gfloat *end)
{
// r0 = size
// r1 = *vec
// r2 = s
// r3 = *start
// sp[0] = *end

		EXPORT	m3gLerp[DYNAMIC]
		CODE32
/*
    M3Gfloat sCompl = 1.0 - s;
    for (i = 0; i < size; ++i) {
        vec[i] = sCompl*start[i] + s*end[i];
    }
*/
//
// if size = 0, return
//
		CMP		r0, #0
		BXEQ	lr

		FMSR	s0, r2
		MOV		r2, r3
		LDR		r3, [sp]

		FLDS	s1,=1.0
		STMFD	sp!, {r4-r5}
		FSUBS	s2, s1, s0			// sCompl = 1 - s

		FMRX	r4, FPSCR
		CMP		r0, #4
		BGT		_m3gLerp_over4Loop

//
// if 1 < size <= 4
//
// set vector STRIDE = 1, LENGTH = 4
		BIC		r12, r4, #0x00370000
		ORR		r12, #(3<<16)
		FMXR	FPSCR, r12

		FLDMIAS	r2!, {s4-s7}		// load the start[i] values
		FLDMIAS	r3!, {s8-s11}		// load the end[i] values
		FMULS	s12, s4, s2			// [s12-s15]  = [sCompl*start[0] .. sCompl*start[3]]
		FMACS	s12, s8, s0			// [s12-s15] += [    	s*end[0] ..        s*end[3]]
		CMP		r0, #1
		FSTS	s12, [r1]
		FSTSGT	s13, [r1, #4]
		CMP		r0, #3
		FSTSGE	s14, [r1, #8]
		FSTSGT	s15, [r1, #12]

		FMXR	FPSCR, r4

		LDMFD	sp!, {r4-r5}

		BX		lr
//
// if size > 4, interpolate 8 values in one loop
//
_m3gLerp_over4Loop

		FSTMFDD	sp!, {d8-d9}
		MOVS	r5, r0, ASR #3			// size/8
		SUB		r0, r0, r5, LSL #3		// tail length

// set vector STRIDE = 1, LENGTH = 8
		BIC		r12, r4, #0x00370000
		ORR		r12, #(7<<16)
		FMXR	FPSCR, r12


_m3gLerp_alignedLoop

		FLDMIASNE	r2!, {s8-s15}		// load the start[i] values
		FLDMIASNE	r3!, {s16-s23}		// load the end[i] values
		FMULSNE		s24, s8, s2			// [s16-s23]  = [ sCompl*start[0] sCompl*start[1] .. sCompl*start[7]]
		FMACSNE		s24, s16, s0		// [s16-s23] += [		 s*end[0]        s*end[1] ..		s*end[7]]
		FSTMIASNE	r1!, {s24-s31}
		SUBSNE		r5, #1

		BNE			_m3gLerp_alignedLoop

// process the 0-7 remaining values in the tail

		CMP			r0, #1
		FLDMIASGE	r2!, {s8-s15}		
		FLDMIASGE	r3!, {s16-s23}		
		FMULSGE		s24, s8, s2			
		FMACSGE		s24, s16, s0		
		FSTSGE		s24, [r1]
		FSTSGT		s25, [r1, #4]
		CMP			r0, #3
		FSTSGE		s26, [r1, #8]
		FSTSGT		s27, [r1, #12]
		CMP			r0, #5
		FSTSGE		s28, [r1, #16]
		FSTSGT		s29, [r1, #20]
		CMP			r0, #7
		FSTSEQ		s30, [r1, #24]

		FMXR	FPSCR, r4

		FLDMFDD	sp!, {d8-d9}
		LDMFD	sp!, {r4-r5}

		BX		lr

}
#else /* #if defined(M3G_HW_FLOAT_VFPV2) */

M3G_API void m3gLerp(M3Gint size,
             M3Gfloat *vec,
             M3Gfloat s,
             const M3Gfloat *start, const M3Gfloat *end)
{
    int i;

    M3Gfloat sCompl = m3gSub(1.f, s);
    for (i = 0; i < size; ++i) {
        vec[i] = m3gAdd(m3gMul(sCompl, start[i]), m3gMul(s, end[i]));
    }
}
#endif /* #if defined(M3G_HW_FLOAT_VFPV2) */

/*!
 * \brief Hermite spline interpolation of vectors
 *
 * \param size      number of components
 * \param vec       output vector
 * \param s         interpolation factor
 * \param start     start value vector
 * \param end       end value vector
 * \param tStart    start tangent vector
 * \param tEnd      end tangent vector
 */
M3G_API void m3gHermite(M3Gint size,
                        M3Gfloat *vec,
                        M3Gfloat s,
                        const M3Gfloat *start, const M3Gfloat *end,
                        const M3Gfloat *tStart, const M3Gfloat *tEnd)
{
    M3Gfloat s2 = m3gSquare(s);
    M3Gfloat s3 = m3gMul(s2, s);
    int i;
    
    for (i = 0; i < size; ++i) {
        vec[i] =
            m3gMadd(start[i],
                    m3gAdd(m3gSub(m3gDouble(s3), m3gMul(3.f, s2)), 1.f),
                    m3gMadd(end[i],
                            m3gSub(m3gMul(3.f, s2), m3gDouble(s3)),
                            m3gMadd(tStart[i],
                                    m3gAdd(m3gSub(s3, m3gDouble(s2)), s),
                                    m3gMul(tEnd[i],
                                           m3gSub(s3, s2)))));

    }
    
    /*  vec = ( 2*s3 - 3*s2 + 1) * start
            + (-2*s3 + 3*s2    ) * end
            + (   s3 - 2*s2 + s) * tStart
            + (   s3 -   s2    ) * tEnd;    */
}

/*--------------------------------------------------------------------*/

/*!
 * \brief Sets a matrix to a copy of another matrix
 */
M3G_API void m3gCopyMatrix(Matrix *dst, const Matrix *src)
{
    M3G_ASSERT(dst != NULL && src != NULL);
    *dst = *src;
}

/*!
 * \brief Vector addition
 */
M3G_API void m3gAddVec3(Vec3 *vec, const Vec3 *other)
{
    vec->x = m3gAdd(vec->x, other->x);
    vec->y = m3gAdd(vec->y, other->y);
    vec->z = m3gAdd(vec->z, other->z);
}

/*!
 * \brief Vector addition
 */
M3G_API void m3gAddVec4(Vec4 *vec, const Vec4 *other)
{
    vec->x = m3gAdd(vec->x, other->x);
    vec->y = m3gAdd(vec->y, other->y);
    vec->z = m3gAdd(vec->z, other->z);
    vec->w = m3gAdd(vec->w, other->w);
}

/*!
 * \brief Cross product of two 3D vectors expressed as 4D vectors
 */
M3G_API void m3gCross(Vec3 *dst, const Vec3 *a, const Vec3 *b)
{
    dst->x = m3gSub(m3gMul(a->y, b->z), m3gMul(a->z, b->y));
    dst->y = m3gSub(m3gMul(a->z, b->x), m3gMul(a->x, b->z));
    dst->z = m3gSub(m3gMul(a->x, b->y), m3gMul(a->y, b->x));
}

/*!
 * \brief Dot product of two vectors
 */
M3G_API M3Gfloat m3gDot3(const Vec3 *a, const Vec3 *b)
{
    M3Gfloat d;
    d = m3gMul(a->x, b->x);
    d = m3gMadd(a->y, b->y, d);
    d = m3gMadd(a->z, b->z, d);
    return d;
}

/*!
 * \brief Dot product of two vectors
 */
M3G_API M3Gfloat m3gDot4(const Vec4 *a, const Vec4 *b)
{
    M3Gfloat d;
    d = m3gMul(a->x, b->x);
    d = m3gMadd(a->y, b->y, d);
    d = m3gMadd(a->z, b->z, d);
    d = m3gMadd(a->w, b->w, d);
    return d;
}

/*!
 * \brief
 */
M3G_API void m3gSetVec3(Vec3 *v, M3Gfloat x, M3Gfloat y, M3Gfloat z)
{
    M3G_ASSERT_PTR(v);
    v->x = x;
    v->y = y;
    v->z = z;
}

/*!
 * \brief
 */
M3G_API void m3gSetVec4(Vec4 *v, M3Gfloat x, M3Gfloat y, M3Gfloat z, M3Gfloat w)
{
    M3G_ASSERT_PTR(v);
    v->x = x;
    v->y = y;
    v->z = z;
    v->w = w;
}

/*!
 * \brief Vector subtraction
 */
M3G_API void m3gSubVec3(Vec3 *vec, const Vec3 *other)
{
    vec->x = m3gSub(vec->x, other->x);
    vec->y = m3gSub(vec->y, other->y);
    vec->z = m3gSub(vec->z, other->z);
}

/*!
 * \brief Vector subtraction
 */
M3G_API void m3gSubVec4(Vec4 *vec, const Vec4 *other)
{
    vec->x = m3gSub(vec->x, other->x);
    vec->y = m3gSub(vec->y, other->y);
    vec->z = m3gSub(vec->z, other->z);
    vec->w = m3gSub(vec->w, other->w);
}

/*!
 * \brief Vector length
 */
M3G_API M3Gfloat m3gLengthVec3(const Vec3 *vec)
{
    return m3gSqrt(m3gAdd(m3gAdd(m3gSquare(vec->x),
                                 m3gSquare(vec->y)),
                          m3gSquare(vec->z)));
}

/*!
 * \brief Vector scaling
 */
M3G_API void m3gScaleVec3(Vec3 *vec, const M3Gfloat s)
{
    vec->x = m3gMul(vec->x, s);
    vec->y = m3gMul(vec->y, s);
    vec->z = m3gMul(vec->z, s);
}

/*!
 * \brief Vector scaling
 */
M3G_API void m3gScaleVec4(Vec4 *vec, const M3Gfloat s)
{
    vec->x = m3gMul(vec->x, s);
    vec->y = m3gMul(vec->y, s);
    vec->z = m3gMul(vec->z, s);
    vec->w = m3gMul(vec->w, s);
}

/*!
 * \brief Returns an angle-axis representation for a quaternion
 *
 * \note There are many, and this is not guaranteed to return a
 * particular one
 */
M3G_API void m3gGetAngleAxis(const Quat *quat, M3Gfloat *angle, Vec3 *axis)
{
    M3Gfloat x, y, z, sinTheta;

    M3G_ASSERT_PTR(quat);
    M3G_ASSERT_PTR(angle);
    M3G_ASSERT_PTR(axis);

    x = quat->x;
    y = quat->y;
    z = quat->z;

    sinTheta = m3gSqrt(m3gAdd(m3gAdd(m3gSquare(x), m3gSquare(y)),
                              m3gSquare(z)));

    if (sinTheta > EPSILON) {
        M3Gfloat ooSinTheta = m3gRcp(sinTheta);
        axis->x = m3gMul(x, ooSinTheta);
        axis->y = m3gMul(y, ooSinTheta);
        axis->z = m3gMul(z, ooSinTheta);
    }
    else {
        /* return a valid axis even for no rotation */
        axis->x = axis->y = 0.0f;
        axis->z = 1.0f;
    }
    *angle = m3gMul(2.0f * RAD2DEG, m3gArcCos(quat->w));
}

/*!
 * \brief Gets a single matrix column
 */
M3G_API void m3gGetMatrixColumn(const Matrix *mtx, M3Gint col, Vec4 *dst)
{
    if ((col & ~3) == 0) {
        if (!mtx->complete) {
            m3gFillClassifiedMatrix((Matrix*)mtx);
        }
        dst->x = M44F(mtx, 0, col);
        dst->y = M44F(mtx, 1, col);
        dst->z = M44F(mtx, 2, col);
        dst->w = M44F(mtx, 3, col);
    }
    else {
        M3G_ASSERT(M3G_FALSE);
    }
}

/*!
 * \brief Returns the floating point values of a matrix as consecutive
 * columns
 */
M3G_API void m3gGetMatrixColumns(const Matrix *mtx, M3Gfloat *dst)
{
    M3G_ASSERT(mtx != NULL && dst != NULL);

    if (!mtx->complete) {
        m3gFillClassifiedMatrix((Matrix*)mtx);
    }
    m3gCopy(dst, mtx->elem, sizeof(mtx->elem));
}

/*!
 * \brief Gets a single matrix row
 */
M3G_API void m3gGetMatrixRow(const Matrix *mtx, M3Gint row, Vec4 *dst)
{
    if ((row & ~3) == 0) {
        if (!mtx->complete) {
            m3gFillClassifiedMatrix((Matrix*)mtx);
        }
        dst->x = M44F(mtx, row, 0);
        dst->y = M44F(mtx, row, 1);
        dst->z = M44F(mtx, row, 2);
        dst->w = M44F(mtx, row, 3);
    }
    else {
        M3G_ASSERT(M3G_FALSE);
    }
}

/*!
 * \brief Returns the floating point values of a matrix as consecutive
 * rows
 */
M3G_API void m3gGetMatrixRows(const Matrix *mtx, M3Gfloat *dst)
{
    M3G_ASSERT(mtx != NULL && dst != NULL);

    if (!mtx->complete) {
        m3gFillClassifiedMatrix((Matrix*)mtx);
    }
    {
        int row;
        for (row = 0; row < 4; ++row) {
            *dst++ = mtx->elem[ 0 + row];
            *dst++ = mtx->elem[ 4 + row];
            *dst++ = mtx->elem[ 8 + row];
            *dst++ = mtx->elem[12 + row];
        }
    }
}

/*!
 * \brief Sets a matrix to identity
 */
M3G_API void m3gIdentityMatrix(Matrix *mtx)
{
    M3G_ASSERT(mtx != NULL);
    m3gClassifyAs(MC_IDENTITY, mtx);
}

/*!
 * \brief Sets a quaternion to identity
 */
M3G_API void m3gIdentityQuat(Quat *quat)
{
    M3G_ASSERT(quat != NULL);
    quat->x = quat->y = quat->z = 0.0f;
    quat->w = 1.0f;
}

/*!
 * \brief Inverts a matrix
 */
M3G_API M3Gbool m3gInvertMatrix(Matrix *mtx)
{
    M3Gfloat *matrix;
    M3Gint i;
    M3Gfloat tmp[12];
    M3Gfloat src[16];
    M3Gfloat det;

    M3G_ASSERT(mtx != NULL);

    if (!m3gIsClassified(mtx)) {
        m3gClassify(mtx);
    }

    /* Quick exit for identity */
    
    if (mtx->mask == MC_IDENTITY) {
        return M3G_TRUE;
    }

    /* Look for other common cases; these require that we have valid
     * values in all the elements, so fill in the values first */
    {
        M3Guint mask = mtx->mask;
        
        if (!mtx->complete) {
            m3gFillClassifiedMatrix(mtx);
        }
        
        if ((mask | (0x3F << 24)) == MC_TRANSLATION) {
            M44F(mtx, 0, 3) = m3gNegate(M44F(mtx, 0, 3));
            M44F(mtx, 1, 3) = m3gNegate(M44F(mtx, 1, 3));
            M44F(mtx, 2, 3) = m3gNegate(M44F(mtx, 2, 3));
            mtx->mask = MC_TRANSLATION;
            return M3G_TRUE;
        }
        if ((mask | 0x300C03) == MC_SCALING) {
            if ((mask &  3       ) == 0 ||
                (mask & (3 << 10)) == 0 ||
                (mask & (3 << 20)) == 0) {
                return M3G_FALSE; /* zero scale for at least one axis */
            }
            M44F(mtx, 0, 0) = m3gRcp(M44F(mtx, 0, 0));
            M44F(mtx, 1, 1) = m3gRcp(M44F(mtx, 1, 1));
            M44F(mtx, 2, 2) = m3gRcp(M44F(mtx, 2, 2));
            return M3G_TRUE;
        }
    }

    /* Do a full 4x4 inversion as a last resort */
        
	matrix = mtx->elem;

    /* transpose matrix */
    for (i = 0; i < 4; i++) {
        src[i] = matrix[i*4];
        src[i+4] = matrix[i*4+1];
        src[i+8] = matrix[i*4+2];
        src[i+12] = matrix[i*4+3];
    }

    /* calculate pairs for first 8 elements (cofactors) */
    tmp[0] = src[10]*src[15];
    tmp[1] = src[11]*src[14];
    tmp[2] = src[9]*src[15];
    tmp[3] = src[11]*src[13];
    tmp[4] = src[9]*src[14];
    tmp[5] = src[10]*src[13];
    tmp[6] = src[8]*src[15];
    tmp[7] = src[11]*src[12];
    tmp[8] = src[8]*src[14];
    tmp[9] = src[10]*src[12];
    tmp[10] = src[8]*src[13];
    tmp[11] = src[9]*src[12];

    /* calculate first 8 elements (cofactors) */
    matrix[0] = tmp[0]*src[5] + tmp[3]*src[6] + tmp[4]*src[7];
    matrix[0] -= tmp[1]*src[5] + tmp[2]*src[6] + tmp[5]*src[7];
    matrix[1] = tmp[1]*src[4] + tmp[6]*src[6] + tmp[9]*src[7];
    matrix[1] -= tmp[0]*src[4] + tmp[7]*src[6] + tmp[8]*src[7];
    matrix[2] = tmp[2]*src[4] + tmp[7]*src[5] + tmp[10]*src[7];
    matrix[2] -= tmp[3]*src[4] + tmp[6]*src[5] + tmp[11]*src[7];
    matrix[3] = tmp[5]*src[4] + tmp[8]*src[5] + tmp[11]*src[6];
    matrix[3] -= tmp[4]*src[4] + tmp[9]*src[5] + tmp[10]*src[6];
    matrix[4] = tmp[1]*src[1] + tmp[2]*src[2] + tmp[5]*src[3];
    matrix[4] -= tmp[0]*src[1] + tmp[3]*src[2] + tmp[4]*src[3];
    matrix[5] = tmp[0]*src[0] + tmp[7]*src[2] + tmp[8]*src[3];
    matrix[5] -= tmp[1]*src[0] + tmp[6]*src[2] + tmp[9]*src[3];
    matrix[6] = tmp[3]*src[0] + tmp[6]*src[1] + tmp[11]*src[3];
    matrix[6] -= tmp[2]*src[0] + tmp[7]*src[1] + tmp[10]*src[3];
    matrix[7] = tmp[4]*src[0] + tmp[9]*src[1] + tmp[10]*src[2];
    matrix[7] -= tmp[5]*src[0] + tmp[8]*src[1] + tmp[11]*src[2];

    /* calculate pairs for second 8 elements (cofactors) */
    tmp[0] = src[2]*src[7];
    tmp[1] = src[3]*src[6];
    tmp[2] = src[1]*src[7];
    tmp[3] = src[3]*src[5];
    tmp[4] = src[1]*src[6];
    tmp[5] = src[2]*src[5];
    tmp[6] = src[0]*src[7];
    tmp[7] = src[3]*src[4];
    tmp[8] = src[0]*src[6];
    tmp[9] = src[2]*src[4];
    tmp[10] = src[0]*src[5];
    tmp[11] = src[1]*src[4];

    /* calculate second 8 elements (cofactors) */
    matrix[8] = tmp[0]*src[13] + tmp[3]*src[14] + tmp[4]*src[15];
    matrix[8] -= tmp[1]*src[13] + tmp[2]*src[14] + tmp[5]*src[15];
    matrix[9] = tmp[1]*src[12] + tmp[6]*src[14] + tmp[9]*src[15];
    matrix[9] -= tmp[0]*src[12] + tmp[7]*src[14] + tmp[8]*src[15];
    matrix[10] = tmp[2]*src[12] + tmp[7]*src[13] + tmp[10]*src[15];
    matrix[10] -= tmp[3]*src[12] + tmp[6]*src[13] + tmp[11]*src[15];
    matrix[11] = tmp[5]*src[12] + tmp[8]*src[13] + tmp[11]*src[14];
    matrix[11] -= tmp[4]*src[12] + tmp[9]*src[13] + tmp[10]*src[14];
    matrix[12] = tmp[2]*src[10] + tmp[5]*src[11] + tmp[1]*src[9];
    matrix[12] -= tmp[4]*src[11] + tmp[0]*src[9] + tmp[3]*src[10];
    matrix[13] = tmp[8]*src[11] + tmp[0]*src[8] + tmp[7]*src[10];
    matrix[13] -= tmp[6]*src[10] + tmp[9]*src[11] + tmp[1]*src[8];
    matrix[14] = tmp[6]*src[9] + tmp[11]*src[11] + tmp[3]*src[8];
    matrix[14] -= tmp[10]*src[11] + tmp[2]*src[8] + tmp[7]*src[9];
    matrix[15] = tmp[10]*src[10] + tmp[4]*src[8] + tmp[9]*src[9];
    matrix[15] -= tmp[8]*src[9] + tmp[11]*src[10] + tmp[5]*src[8];

    /* calculate determinant */
    det = src[0]*matrix[0]+src[1]*matrix[1]+src[2]*matrix[2]+src[3]*matrix[3];

    /* matrix has no inverse */
    if (det == 0.0f) {
        return M3G_FALSE;
    }

    /* calculate matrix inverse */
    det = 1/det;
    for (i = 0; i < 16; i++) {
        matrix[i] *= det;
    }

    mtx->classified = M3G_FALSE;
	return M3G_TRUE;
}

/*!
 * \brief Sets a matrix to the inverse of another matrix
 */
M3G_API M3Gbool m3gMatrixInverse(Matrix *mtx, const Matrix *other)
{
    M3G_ASSERT(mtx != NULL && other != NULL);

    if (!m3gIsClassified(other)) {
        m3gClassify((Matrix*)other);
    }

	m3gCopyMatrix(mtx, other);
	return m3gInvertMatrix(mtx);
}

/*!
 * \brief Sets a matrix to the transpose of another matrix
 */
M3G_API void m3gMatrixTranspose(Matrix *mtx, const Matrix *other)
{
    M3Gbyte i;
    M3G_ASSERT(mtx != NULL && other != NULL);

    if (!other->complete) {
        m3gFillClassifiedMatrix((Matrix *)other);
    }

    for (i = 0; i < 4; i++) {
        mtx->elem[i] = other->elem[i*4];
        mtx->elem[i+4] = other->elem[i*4+1];
        mtx->elem[i+8] = other->elem[i*4+2];
        mtx->elem[i+12] = other->elem[i*4+3];
    }
    mtx->classified = M3G_FALSE;
    mtx->complete = M3G_TRUE;
}

M3G_API M3Gbool m3gInverseTranspose(Matrix *mtx, const Matrix *other)
{
    Matrix temp;
    if (!m3gMatrixInverse(&temp, other)) {
        return M3G_FALSE;
    }
    m3gMatrixTranspose(mtx, &temp);
    return M3G_TRUE;
}

/*!
 * \brief Sets a matrix to the product of two other matrices
 *
 * \note \c dst can not be either of \c left or \c right; if it is,
 * the results are undefined
 */
M3G_API void m3gMatrixProduct(Matrix *dst, const Matrix *left, const Matrix *right)
{
    M3G_ASSERT_PTR(dst);
    M3G_ASSERT_PTR(left);
    M3G_ASSERT_PTR(right);
    M3G_ASSERT(dst != left && dst != right);

    /* Classify input matrices and take early exits for identities */

    if (!m3gIsClassified(left)) {
        m3gClassify((Matrix*)left);
    }
    if (left->mask == MC_IDENTITY) {
        m3gCopyMatrix(dst, right);
        return;
    }

    if (!m3gIsClassified(right)) {
        m3gClassify((Matrix*)right);
    }
    if (right->mask == MC_IDENTITY) {
        m3gCopyMatrix(dst, left);
        return;
    }

    /* Special quick paths for 3x4 matrices */
    
    if (m3gIsWUnity(left) && m3gIsWUnity(right)) {

        /* Translation? */
        
        if ((left->mask & ~MC_TRANSLATION_PART) == MC_IDENTITY) {
            
            if (left->mask != MC_TRANSLATION && !left->complete) {
                m3gFillClassifiedMatrix((Matrix*)left);
            }
            if (right->mask != MC_TRANSLATION && !right->complete) {
                m3gFillClassifiedMatrix((Matrix*)right);
            }

            m3gCopyMatrix(dst, right);
            
            M44F(dst, 0, 3) = m3gAdd(M44F(left, 0, 3), M44F(dst, 0, 3));
            M44F(dst, 1, 3) = m3gAdd(M44F(left, 1, 3), M44F(dst, 1, 3));
            M44F(dst, 2, 3) = m3gAdd(M44F(left, 2, 3), M44F(dst, 2, 3));
            
            dst->mask |= MC_TRANSLATION_PART;
            return;
        }

        if ((right->mask & ~MC_TRANSLATION_PART) == MC_IDENTITY) {
            Vec4 tvec;

            if (left->mask != MC_TRANSLATION && !left->complete) {
                m3gFillClassifiedMatrix((Matrix*)left);
            }
            if (right->mask != MC_TRANSLATION && !right->complete) {
                m3gFillClassifiedMatrix((Matrix*)right);
            }

            m3gCopyMatrix(dst, left);
            
            m3gGetMatrixColumn(right, 3, &tvec);
            m3gTransformVec4(dst, &tvec);
            
            M44F(dst, 0, 3) = tvec.x;
            M44F(dst, 1, 3) = tvec.y;
            M44F(dst, 2, 3) = tvec.z;
            
            dst->mask |= MC_TRANSLATION_PART;
            return;
        }

    }
        
    /* Compute product and set output classification */

    m3gGenericMatrixProduct(dst, left, right);
}

/*!
 * \brief Normalizes a quaternion
 */
M3G_API void m3gNormalizeQuat(Quat *q)
{
    M3Gfloat norm;
    M3G_ASSERT_PTR(q);
    
    norm = m3gNorm4(&q->x);
    
    if (norm > EPSILON) {
        norm = m3gRcpSqrt(norm);
        m3gScale4(&q->x, norm);
    }
    else {
        m3gIdentityQuat(q);
    }
}

/*!
 * \brief Normalizes a three-vector
 */
M3G_API void m3gNormalizeVec3(Vec3 *v)
{
    M3Gfloat norm;
    M3G_ASSERT_PTR(v);
    
    norm = m3gNorm3(&v->x);
    
    if (norm > EPSILON) {
        norm = m3gRcpSqrt(norm);
        m3gScale3(&v->x, norm);
    }
    else {
        m3gZero(v, sizeof(Vec3));
    }
}

/*!
 * \brief Normalizes a four-vector
 */
M3G_API void m3gNormalizeVec4(Vec4 *v)
{
    M3Gfloat norm;
    M3G_ASSERT_PTR(v);
    
    norm = m3gNorm4(&v->x);
    
    if (norm > EPSILON) {
        norm = m3gRcpSqrt(norm);
        m3gScale4(&v->x, norm);
    }
    else {
        m3gZero(v, sizeof(Vec4));
    }
}

/*!
 * \brief Multiplies a matrix from the right with another matrix
 */
M3G_API void m3gPostMultiplyMatrix(Matrix *mtx, const Matrix *other)
{
    Matrix temp;
    M3G_ASSERT_PTR(mtx);
    M3G_ASSERT_PTR(other);

    m3gCopyMatrix(&temp, mtx);
    m3gMatrixProduct(mtx, &temp, other);
}

/*!
 * \brief Multiplies a matrix from the left with another matrix
 */
M3G_API void m3gPreMultiplyMatrix(Matrix *mtx, const Matrix *other)
{
    Matrix temp;
    M3G_ASSERT_PTR(mtx);
    M3G_ASSERT_PTR(other);

    m3gCopyMatrix(&temp, mtx);
    m3gMatrixProduct(mtx, other, &temp);
}

/*!
 * \brief Multiplies a matrix with a rotation matrix.
 */
M3G_API void m3gPostRotateMatrix(Matrix *mtx,
                         M3Gfloat angle,
                         M3Gfloat ax, M3Gfloat ay, M3Gfloat az)
{
    Quat q;
    m3gSetAngleAxis(&q, angle, ax, ay, az);
    m3gPostRotateMatrixQuat(mtx, &q);
}

/*!
 * \brief Multiplies a matrix with a translation matrix.
 */
M3G_API void m3gPostRotateMatrixQuat(Matrix *mtx, const Quat *quat)
{
    Matrix temp;
    m3gQuatMatrix(&temp, quat);
    m3gPostMultiplyMatrix(mtx, &temp);
}

/*!
 * \brief Multiplies a matrix with a scale matrix.
 */
M3G_API void m3gPostScaleMatrix(Matrix *mtx, M3Gfloat sx, M3Gfloat sy, M3Gfloat sz)
{
    Matrix temp;
    m3gScalingMatrix(&temp, sx, sy, sz);
    m3gPostMultiplyMatrix(mtx, &temp);
}

/*!
 * \brief Multiplies a matrix with a translation (matrix).
 */
M3G_API void m3gPostTranslateMatrix(Matrix *mtx,
                            M3Gfloat tx, M3Gfloat ty, M3Gfloat tz)
{
    Matrix temp;
    m3gTranslationMatrix(&temp, tx, ty, tz);
    m3gPostMultiplyMatrix(mtx, &temp);
}

/*!
 * \brief Multiplies a matrix with a rotation matrix
 */
M3G_API void m3gPreRotateMatrix(Matrix *mtx,
                        M3Gfloat angle,
                        M3Gfloat ax, M3Gfloat ay, M3Gfloat az)
{
    Quat q;
    m3gSetAngleAxis(&q, angle, ax, ay, az);
    m3gPreRotateMatrixQuat(mtx, &q);
}

/*!
 * \brief Multiplies a matrix with a quaternion rotation
 */
M3G_API void m3gPreRotateMatrixQuat(Matrix *mtx, const Quat *quat)
{
    Matrix temp;
    m3gQuatMatrix(&temp, quat);
    m3gPreMultiplyMatrix(mtx, &temp);
}

/*!
 * \brief Multiplies a matrix with a scale matrix.
 */
M3G_API void m3gPreScaleMatrix(Matrix *mtx, M3Gfloat sx, M3Gfloat sy, M3Gfloat sz)
{
    Matrix temp;
    m3gScalingMatrix(&temp, sx, sy, sz);
    m3gPreMultiplyMatrix(mtx, &temp);
}

/*!
 * \brief Multiplies a matrix with a translation (matrix).
 */
M3G_API void m3gPreTranslateMatrix(Matrix *mtx,
                           M3Gfloat tx, M3Gfloat ty, M3Gfloat tz)
{
    Matrix temp;
    m3gTranslationMatrix(&temp, tx, ty, tz);
    m3gPreMultiplyMatrix(mtx, &temp);
}

/*!
 * \brief Converts a quaternion into a matrix
 *
 * The output is a matrix effecting the same rotation as the
 * quaternion passed as input
 */
M3G_API void m3gQuatMatrix(Matrix *mtx, const Quat *quat)
{
    M3Gfloat qx = quat->x;
    M3Gfloat qy = quat->y;
    M3Gfloat qz = quat->z;
    M3Gfloat qw = quat->w;

    /* Quick exit for identity rotations */

    if (IS_ZERO(qx) && IS_ZERO(qy) && IS_ZERO(qz)) {
        m3gIdentityMatrix(mtx);
        return;
    }

    {
        /* Determine the rough type of the output matrix */

        M3Guint type = MC_SCALING_ROTATION;
        if (IS_ZERO(qz) && IS_ZERO(qy)) {
            type = MC_X_ROTATION;
        }
        else if (IS_ZERO(qz) && IS_ZERO(qx)) {
            type = MC_Y_ROTATION;
        }
        else if (IS_ZERO(qx) && IS_ZERO(qy)) {
            type = MC_Z_ROTATION;
        }
        m3gClassifyAs(type, mtx);

        /* Generate the non-constant parts of the matrix */
        {
            M3Gfloat wx, wy, wz, xx, yy, yz, xy, xz, zz;

            xx = m3gMul(qx, qx);
            xy = m3gMul(qx, qy);
            xz = m3gMul(qx, qz);
            yy = m3gMul(qy, qy);
            yz = m3gMul(qy, qz);
            zz = m3gMul(qz, qz);
            wx = m3gMul(qw, qx);
            wy = m3gMul(qw, qy);
            wz = m3gMul(qw, qz);

            if (type != MC_X_ROTATION) {
                M44F(mtx, 0, 0) = m3gSub(1.f, m3gDouble(m3gAdd(yy, zz)));
                M44F(mtx, 0, 1) =             m3gDouble(m3gSub(xy, wz));
                M44F(mtx, 0, 2) =             m3gDouble(m3gAdd(xz, wy));
            }

            if (type != MC_Y_ROTATION) {
                M44F(mtx, 1, 0) =             m3gDouble(m3gAdd(xy, wz));
                M44F(mtx, 1, 1) = m3gSub(1.f, m3gDouble(m3gAdd(xx, zz)));
                M44F(mtx, 1, 2) =             m3gDouble(m3gSub(yz, wx));
            }

            if (type != MC_Z_ROTATION) {
                M44F(mtx, 2, 0) =             m3gDouble(m3gSub(xz, wy));
                M44F(mtx, 2, 1) =             m3gDouble(m3gAdd(yz, wx));
                M44F(mtx, 2, 2) = m3gSub(1.f, m3gDouble(m3gAdd(xx, yy)));
            }
        }
        m3gSubClassify(mtx);
    }
}

/*!
 * \brief Generates a scaling matrix
 */
M3G_API void m3gScalingMatrix(
    Matrix *mtx,
    const M3Gfloat sx, const M3Gfloat sy, const M3Gfloat sz)
{
    M3G_ASSERT_PTR(mtx);
    M44F(mtx, 0, 0) = sx;
    M44F(mtx, 1, 1) = sy;
    M44F(mtx, 2, 2) = sz;
    m3gClassifyAs(MC_SCALING, mtx);
    m3gSubClassify(mtx);
}

/*!
 * \brief Sets a quaternion to represent an angle-axis rotation
 */
M3G_API void m3gSetAngleAxis(Quat *quat,
                     M3Gfloat angle,
                     M3Gfloat ax, M3Gfloat ay, M3Gfloat az)
{
    m3gSetAngleAxisRad(quat, m3gMul(angle, M3G_DEG2RAD), ax, ay, az);
}

/*!
 * \brief Sets a quaternion to represent an angle-axis rotation
 */
M3G_API void m3gSetAngleAxisRad(Quat *quat,
                        M3Gfloat angleRad,
                        M3Gfloat ax, M3Gfloat ay, M3Gfloat az)
{
    M3G_ASSERT_PTR(quat);
    
    if (!IS_ZERO(angleRad)) {
        M3Gfloat s;
        M3Gfloat halfAngle = m3gHalf(angleRad);

        s = m3gSin(halfAngle);
        
        {
            M3Gfloat sqrNorm = m3gMadd(ax, ax, m3gMadd(ay, ay, m3gMul(az, az)));
            if (sqrNorm < 0.995f || sqrNorm > 1.005f) {
                if (sqrNorm > EPSILON) {
                    M3Gfloat ooNorm = m3gRcpSqrt(sqrNorm);
                    ax = m3gMul(ax, ooNorm);
                    ay = m3gMul(ay, ooNorm);
                    az = m3gMul(az, ooNorm);
                }
                else {
                    ax = ay = az = 0.0f;
                }
            }
        }

        quat->x = m3gMul(s, ax);
        quat->y = m3gMul(s, ay);
        quat->z = m3gMul(s, az);
        quat->w = m3gCos(halfAngle);
    }
    else {
        m3gIdentityQuat(quat);
    }
}

/*!
 * \brief Quaternion multiplication.
 */
M3G_API void m3gMulQuat(Quat *quat, const Quat *other)
{
    Quat q;
    q = *quat;

    quat->w = m3gMul(q.w, other->w) - m3gMul(q.x, other->x) - m3gMul(q.y, other->y) - m3gMul(q.z, other->z);
    quat->x = m3gMul(q.w, other->x) + m3gMul(q.x, other->w) + m3gMul(q.y, other->z) - m3gMul(q.z, other->y);
    quat->y = m3gMul(q.w, other->y) - m3gMul(q.x, other->z) + m3gMul(q.y, other->w) + m3gMul(q.z, other->x);
    quat->z = m3gMul(q.w, other->z) + m3gMul(q.x, other->y) - m3gMul(q.y, other->x) + m3gMul(q.z, other->w);
}

/*!
 * \brief Makes this quaternion represent the rotation from one 3D
 * vector to another
 */
M3G_API void m3gSetQuatRotation(Quat *quat,
                                const Vec3 *from, const Vec3 *to)
{
    M3Gfloat cosAngle;

    M3G_ASSERT_PTR(quat);
    M3G_ASSERT_PTR(from);
    M3G_ASSERT_PTR(to);

    cosAngle = m3gDot3(from, to);

    if (cosAngle > (1.0f - EPSILON)) {  /* zero angle */
        m3gIdentityQuat(quat);
        return;
    }
    else if (cosAngle > (1.0e-3f - 1.0f)) { /* normal case */
        Vec3 axis;
        m3gCross(&axis, from, to);
        m3gSetAngleAxisRad(quat, m3gArcCos(cosAngle), axis.x, axis.y, axis.z);
    }
    else {

        /* Opposite vectors; must generate an arbitrary perpendicular
         * vector and use that as the rotation axis. Here, we try the
         * Z axis first, and if that seems too parallel to the
         * vectors, project the Y axis instead: Z is the only good
         * choice for Z-constrained rotations, and Y by definition
         * must be perpendicular to that. */

        Vec3 axis, temp;
        M3Gfloat s;

        axis.x = axis.y = axis.z = 0.0f;
        if (m3gAbs(from->z) < (1.0f - EPSILON)) {
            axis.z = 1.0f;
        }
        else {
            axis.y = 1.0f;
        }

        s = m3gDot3(&axis, from);
        temp = *from;
        m3gScaleVec3(&temp, s);
        m3gSubVec3(&axis, &temp);

        m3gSetAngleAxis(quat, 180.f, axis.x, axis.y, axis.z);
    }
}

/*!
 * \brief Sets the values of a matrix
 *
 */
M3G_API void m3gSetMatrixColumns(Matrix *mtx, const M3Gfloat *src)
{
    M3G_ASSERT(mtx != NULL && src != NULL);

    m3gCopy(mtx->elem, src, sizeof(mtx->elem));
    mtx->classified = M3G_FALSE;
    mtx->complete = M3G_TRUE;
}

/*!
 * \brief Sets the values of a matrix
 *
 */
M3G_API void m3gSetMatrixRows(Matrix *mtx, const M3Gfloat *src)
{
    M3G_ASSERT(mtx != NULL && src != NULL);
    {
        int row;
        for (row = 0; row < 4; ++row) {
            mtx->elem[ 0 + row] = *src++;
            mtx->elem[ 4 + row] = *src++;
            mtx->elem[ 8 + row] = *src++;
            mtx->elem[12 + row] = *src++;
        }
    }
    mtx->classified = M3G_FALSE;
    mtx->complete = M3G_TRUE;
}

/*!
 * \brief Transforms a 4-vector with a matrix
 */
#if defined(M3G_HW_FLOAT_VFPV2)

__asm void _m3gTransformVec4(const Matrix *mtx, Vec4 *vec, M3Gint n)
{

		CODE32

		FSTMFDD	sp!, {d8-d11}

		FMRX	r3, FPSCR		 
		BIC		r12, r3, #0x00370000
		ORR		r12, #(3<<16)
		FMXR	FPSCR, r12
		CMP		r2, #4

		FLDMIAS	r0, {s4-s19}		// [mtx0  mtx1 ..]
		FLDMIAS	r1, {s0-s3}			// [vec.x  vec.y  vec.z  vec.w]
		FMULS	s20, s4, s0			// [s20-s23]  = [v.x*mtx0  v.x*mtx1  v.x*mtx2  v.x*mtx3 ]
		FMACS	s20, s8, s1			// [s20-s23] += [v.y*mtx4  v.y*mtx5  v.y*mtx6  v.y*mtx7 ]
		FMACS	s20, s12, s2		// [s20-s23] += [v.z*mtx8  v.z*mtx9  v.z*mtx10 v.z*mtx11]
		FMACS	s20, s16, s3		// [s20-s23] += [v.w*mtx12 v.w*mtx13 v.w*mtx14 v.w*mtx15]
		FSTMIAS		r1!, {s20-s22}
		FSTMIASEQ	r1, {s23}

		FMXR	FPSCR, r3
		FLDMFDD	sp!, {d8-d11}

		BX		lr
}
#endif /* #if defined(M3G_HW_FLOAT_VFPV2) */

M3G_API void m3gTransformVec4(const Matrix *mtx, Vec4 *vec)
{
    M3Guint type;
    M3G_ASSERT(mtx != NULL && vec != NULL);

    if (!m3gIsClassified(mtx)) {
        m3gClassify((Matrix*)mtx);
    }

    type = mtx->mask;

    if (type == MC_IDENTITY) {
        return;
    }
    else {
        int n = m3gIsWUnity(mtx) ? 3 : 4;

        if (!mtx->complete) {
            m3gFillClassifiedMatrix((Matrix*)mtx);
        }
#if	defined(M3G_HW_FLOAT_VFPV2)
		_m3gTransformVec4(mtx, vec, n);
#else
        {
            Vec4 v = *vec;
            int i;

            for (i = 0; i < n; ++i) {
                M3Gfloat d = m3gMul(M44F(mtx, i, 0), v.x);
                d = m3gMadd(M44F(mtx, i, 1), v.y, d);
                d = m3gMadd(M44F(mtx, i, 2), v.z, d);
                d = m3gMadd(M44F(mtx, i, 3), v.w, d);
                (&vec->x)[i] = d;
            }
        }
#endif
    }
}

/*!
 * \brief Generates a translation matrix
 */
M3G_API void m3gTranslationMatrix(
    Matrix *mtx,
    const M3Gfloat tx, const M3Gfloat ty, const M3Gfloat tz)
{
    M3G_ASSERT_PTR(mtx);
    M44F(mtx, 0, 3) = tx;
    M44F(mtx, 1, 3) = ty;
    M44F(mtx, 2, 3) = tz;
    m3gClassifyAs(MC_TRANSLATION, mtx);
    m3gSubClassify(mtx);
}

/*!
 * \brief Float vector assignment.
 */
M3G_API void m3gSetQuat(Quat *quat, const M3Gfloat *vec)
{
    quat->x = vec[0];
    quat->y = vec[1];
    quat->z = vec[2];
    quat->w = vec[3];
}

/*!
 * \brief Slerp between quaternions q0 and q1, storing the result in quat.
 */
M3G_API void m3gSlerpQuat(Quat *quat,
                  M3Gfloat s,
                  const Quat *q0, const Quat *q1)
{
    M3Gfloat s0, s1;
    M3Gfloat cosTheta = m3gDot4((const Vec4 *)q0, (const Vec4 *)q1);
    M3Gfloat oneMinusS = m3gSub(1.0f, s);

    if (cosTheta > EPSILON - 1.0f) {
        if (cosTheta < 1.0f - EPSILON) {
            M3Gfloat theta    = m3gArcCos(cosTheta);
            M3Gfloat sinTheta = m3gSin(theta);
            s0 = m3gSin(m3gMul(oneMinusS, theta)) / sinTheta;
            s1 = m3gSin(m3gMul(s, theta)) / sinTheta;
        }
        else {
            /* For quaternions very close to each other, use plain
               linear interpolation for numerical stability. */
            s0 = oneMinusS;
            s1 = s;
        }
        quat->x = m3gMadd(s0, q0->x, m3gMul(s1, q1->x));
        quat->y = m3gMadd(s0, q0->y, m3gMul(s1, q1->y));
        quat->z = m3gMadd(s0, q0->z, m3gMul(s1, q1->z));
        quat->w = m3gMadd(s0, q0->w, m3gMul(s1, q1->w));
    }
    else {
        /* Slerp is undefined if the two quaternions are (nearly)
           opposite, so we just pick an arbitrary plane of
           rotation here. */

        quat->x = -q0->y;
        quat->y =  q0->x;
        quat->z = -q0->w;
        quat->w =  q0->z;

        s0 = m3gSin(m3gMul(oneMinusS, HALF_PI));
        s1 = m3gSin(m3gMul(s, HALF_PI));

        quat->x = m3gMadd(s0, q0->x, m3gMul(s1, quat->x));
        quat->y = m3gMadd(s0, q0->y, m3gMul(s1, quat->y));
        quat->z = m3gMadd(s0, q0->z, m3gMul(s1, quat->z));
    }
}

/*!
 * \brief Interpolate quaternions using spline, or "squad" interpolation.
 */
M3G_API void m3gSquadQuat(Quat *quat,
                  M3Gfloat s,
                  const Quat *q0, const Quat *a, const Quat *b, const Quat *q1)
{

    Quat temp0;
    Quat temp1;
    m3gSlerpQuat(&temp0, s, q0, q1);
    m3gSlerpQuat(&temp1, s, a, b);

    m3gSlerpQuat(quat, m3gDouble(m3gMul(s, m3gSub(1.0f, s))), &temp0, &temp1);
}



