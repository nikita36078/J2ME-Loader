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
* Description: Image implementation
*
*/


/*!
 * \internal
 * \file
 * \brief Image implementation
 *
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

#include "m3g_image.h"
#include "m3g_texture.h"

/* Declare prototypes for some of the helper functions called from the
 * platform-dependent code included below */

static M3Gint  m3gBytesPerPixel(M3GPixelFormat format);
static M3Gint  m3gGetNumMipmapLevels(M3Gint w, M3Gint h);
static void m3gDownsample(M3GPixelFormat format, const M3Gubyte *srcPixels, M3Gint *pw, M3Gint *ph, M3Gubyte *dstPixels);
static void m3gFreeImageData(Image *img);

/* Include platform-dependent functionality */
#include "m3g_image.inl"

/* Size of the buffer used in pixel format conversions; this should be
 * an even number */
#define SPAN_BUFFER_SIZE 32

M3G_CT_ASSERT((SPAN_BUFFER_SIZE & 1) == 0);

/*----------------------------------------------------------------------
 * Private functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Destroys this Image object.
 *
 * \param obj Image object
 */
static void m3gDestroyImage(Object *obj)
{
    Image *image = (Image*)obj;
    Interface *m3g = M3G_INTERFACE(image);
    M3G_VALIDATE_OBJECT(image);

    if (!image->copyOf) {
        m3gFreeObject(m3g, image->data);
        m3gFreeObject(m3g, image->mipData);
    }
    M3G_ASSIGN_REF(image->copyOf, NULL);
    
    if (image->powerOfTwo != image) {
        M3G_ASSIGN_REF(image->powerOfTwo, NULL);
    }

#   if !defined(M3G_NGL_TEXTURE_API)
    if (image->texObject) {
        m3gDeleteGLTextures(m3g, 1, &image->texObject);
    }
    if (image->large != NULL) {
        m3gDestroyLargeImage(image);
    }
#   endif

    m3gDestroyObject(obj);
}

/*--------------------------------------------------------------------*/

#define RED(argb)   (((argb) >> 16) & 0xFFu)
#define GREEN(argb) (((argb) >>  8) & 0xFFu)
#define BLUE(argb)  ( (argb)        & 0xFFu)
#define ALPHA(argb) (((argb) >> 24) & 0xFFu)

#define RGBSUM(argb) (0x4CB2u * RED(argb) +     \
                      0x9691u * GREEN(argb) +   \
                      0x1D3Eu * BLUE(argb))

/*!
 * \internal \brief ARGB -> A
 */
static void convertARGBToA8(const M3Guint *src, M3Gsizei count, M3Gubyte *dst)
{
    while (count--) {
        *dst++ = (M3Gubyte) ALPHA(*src++);
    }
}

/*!
 * \internal \brief ARGB -> L
 */
static void convertARGBToL8(const M3Guint *src, M3Gsizei count, M3Gubyte *dst)
{
    while (count--) {
        M3Guint argb = *src++;
        M3Guint sum = RGBSUM(argb);
        *dst++ = (M3Gubyte)(sum >> 16);
    }
}

/*!
 * \internal \brief ARGB -> LA */
static void convertARGBToLA4(const M3Guint *src, M3Gsizei count, M3Gubyte *dst)
{
    while (count--) {
        M3Guint argb = *src++;
        M3Guint sum = RGBSUM(argb);
        *dst++ = (M3Gubyte)(((sum >> 16) & 0xF0) | ((argb >> 28) & 0x0F));
    }
}

/*!
 * \internal \brief ARGB -> LA8
 */
static void convertARGBToLA8(const M3Guint *src, M3Gsizei count, M3Gubyte *dst)
{
    while (count--) {
        M3Guint argb = *src++;
        M3Guint sum = RGBSUM(argb);
        *dst++ = (M3Gubyte)(sum >> 16);       /* L */
        *dst++ = (M3Gubyte) ALPHA(argb);
    }
}

/*!
 * \internal \brief ARGB -> RGB
 */
static void convertARGBToRGB8(const M3Guint *src, M3Gsizei count, M3Gubyte *dst)
{
    while (count--) {
        M3Guint argb = *src++;
        *dst++ = (M3Gubyte) RED(argb);
        *dst++ = (M3Gubyte) GREEN(argb);
        *dst++ = (M3Gubyte) BLUE(argb);
    }
}

/*!
 * \internal \brief ARGB -> RGB565
 */
static void convertARGBToRGB565(const M3Guint *src, M3Gsizei count, M3Gubyte *dst)
{
    while (count--) {
        M3Guint argb = *src++;
        *(M3Gushort*)dst = (M3Gushort)(((argb >> 8) & 0xF800u)|
                                       ((argb >> 5) & 0x07E0u)|
                                       ((argb >> 3) & 0x001Fu));
        dst += 2;
    }
}

/*!
 * \internal \brief ARGB -> RGBA
 */
static void convertARGBToRGBA8(const M3Guint *src, M3Gsizei count, M3Gubyte *dst)
{
    while (count--) {
        M3Guint argb = *src++;
        *dst++ = (M3Gubyte) RED(argb);
        *dst++ = (M3Gubyte) GREEN(argb);
        *dst++ = (M3Gubyte) BLUE(argb);
        *dst++ = (M3Gubyte) ALPHA(argb);
    }
}

/*!
 * \internal \brief ARGB -> BGRA
 */
static void convertARGBToBGRA8(const M3Guint *src, M3Gsizei count, M3Gubyte *dst)
{
    while (count--) {
        M3Guint argb = *src++;
        *dst++ = (M3Gubyte) BLUE(argb);
        *dst++ = (M3Gubyte) GREEN(argb);
        *dst++ = (M3Gubyte) RED(argb);
        *dst++ = (M3Gubyte) ALPHA(argb);
    }
}

#undef RED
#undef GREEN
#undef BLUE
#undef ALPHA

/*!
 * \internal
 * \brief Converts a span of ARGB pixels to another format
 *
 * \param src   source pixels
 * \param count pixel count
 * \param dstFormat destination format
 * \param dst destination pixels
 */
static void convertFromARGB(const M3Guint *src,
                            M3Gsizei count,
                            M3GPixelFormat dstFormat,
                            M3Gubyte *dst)
{
    switch (dstFormat) {
    case M3G_L8:
        convertARGBToL8(src, count, dst);
        break;
    case M3G_A8:
        convertARGBToA8(src, count, dst);
        break;
    case M3G_LA4:
        convertARGBToLA4(src, count, dst);
        break;
    case M3G_LA8:
        convertARGBToLA8(src, count, dst);
        break;
    case M3G_RGB8:
        convertARGBToRGB8(src, count, dst);
        break;
    case M3G_RGB565:
        convertARGBToRGB565(src, count, dst);
        break;
    case M3G_RGBA8:
    case M3G_RGB8_32:
        convertARGBToRGBA8(src, count, dst);
        break;
    case M3G_BGRA8:
    case M3G_BGR8_32:
        convertARGBToBGRA8(src, count, dst);
        break;
    default:
        M3G_ASSERT(M3G_FALSE);  /* conversion not supported */
    }
}

/*--------------------------------------------------------------------*/

/*!
 * \internal \brief A8 -> ARGB
 */
static void convertA8ToARGB(const M3Gubyte *src, M3Gsizei count, M3Guint *dst)
{
    while (count--) {
        M3Guint argb = M3G_RGB_MASK;
        argb |= ((M3Guint) *src++) << 24;
        *dst++ = argb;
    }
}

/*!
 * \internal \brief L8 -> ARGB
 */
static void convertL8ToARGB(const M3Gubyte *src, M3Gsizei count, M3Guint *dst)
{
    while (count--) {
        M3Guint argb = *src++;
        argb |= (argb << 8) | (argb << 16);
        argb |= M3G_ALPHA_MASK;
        *dst++ = argb;
    }
}

/*!
 * \internal \brief LA8 -> ARGB
 */
static void convertLA8ToARGB(const M3Gubyte *src, M3Gsizei count, M3Guint *dst)
{
    while (count--) {
        M3Guint argb = *src++;
        argb |= (argb << 8) | (argb << 16);
        argb |= ((M3Guint) *src++) << 24;
        *dst++ = argb;
    }
}

/*!
 * \internal \brief RGB8 -> ARGB
 */
static void convertRGB8ToARGB(const M3Gubyte *src, M3Gsizei count, M3Guint *dst)
{
    while (count--) {
        M3Guint argb = M3G_ALPHA_MASK;
        argb |= ((M3Guint)(*src++)) << 16;
        argb |= ((M3Guint)(*src++)) <<  8;
        argb |=  (M3Guint)(*src++);
        *dst++ = argb;
    }
}

/*!
 * \internal \brief RGB565 -> ARGB
 */
static void convertRGB565ToARGB(const M3Gubyte *src, M3Gsizei count, M3Guint *dst)
{
    while (count--) {
        M3Guint argb = M3G_ALPHA_MASK;
        const M3Guint rgb565 = *(const M3Gushort*)src;
        argb |= ((rgb565 & 0xF800u) << 8)|((rgb565 & 0xE000u) << 3);
        argb |= ((rgb565 & 0x07E0u) << 5)|((rgb565 & 0x0600u) >> 1);
        argb |= ((rgb565 & 0x001Fu) << 3)|((rgb565 & 0x001Cu) >> 2);
        *dst++ = argb;
        src += 2;
    }
}

/*!
 * \internal \brief RGBA8 -> ARGB
 */
static void convertRGBA8ToARGB(const M3Gubyte *src, M3Gsizei count, M3Guint *dst)
{
    while (count--) {
        M3Guint argb;
        argb  = ((M3Guint)(*src++)) << 16;
        argb |= ((M3Guint)(*src++)) <<  8;
        argb |=  (M3Guint)(*src++);
        argb |= ((M3Guint)(*src++)) << 24;
        *dst++ = argb;
    }
}

/*!
 * \internal \brief BGRA8 -> ARGB
 */
static void convertBGRA8ToARGB(const M3Gubyte *src, M3Gsizei count, M3Guint *dst)
{
    while (count--) {
        M3Guint argb;
        argb  =  (M3Guint)(*src++);
        argb |= ((M3Guint)(*src++)) <<  8;
        argb |= ((M3Guint)(*src++)) << 16;
        argb |= ((M3Guint)(*src++)) << 24;
        *dst++ = argb;
    }
}

/*!
 * \internal
 * \brief Converts a span of pixels to ARGB
 *
 * \param srcFormat source format
 * \param src   source pixels
 * \param count pixel count
 * \param dst destination pixels
 */
static void convertToARGB(M3GPixelFormat srcFormat,
                          const M3Gubyte *src,
                          M3Gsizei count,
                          M3Guint *dst)
{
    switch (srcFormat) {
    case M3G_A8:
        convertA8ToARGB(src, count, dst);
        break;
    case M3G_L8:
        convertL8ToARGB(src, count, dst);
        break;
    case M3G_LA8:
        convertLA8ToARGB(src, count, dst);
        break;
    case M3G_RGB8:
        convertRGB8ToARGB(src, count, dst);
        break;
    case M3G_RGB565:
        convertRGB565ToARGB(src, count, dst);
        break;
    case M3G_RGBA8:
    case M3G_RGB8_32:
        convertRGBA8ToARGB(src, count, dst);
        break;
    case M3G_BGRA8:
    case M3G_BGR8_32:
        convertBGRA8ToARGB(src, count, dst);
        break;
    default:
        M3G_ASSERT(M3G_FALSE);  /* conversion not supported */
    }
}

/*!
 * \internal
 * \brief Fast path for BGRA-to-RGBA conversion
 */
#if defined (M3G_HW_ARMV6)
__asm void fastConvertBGRAToRGBA(const M3Gubyte *src, M3Gsizei srcStride,
                                 M3Gsizei width, M3Gsizei height,
                                 M3Gubyte *dst)
{
// r0 = *src
// r1 = srcStride
// r2 = width
// r3 = height
// sp[0] = *dst

		CODE32

		CMP		r3, #0				// if height = 0, do nothing
		BXEQ	lr

  		STMFD	sp!, {r4-r12, lr} 

		LDR		r12, [sp, #(10*4)]
		SUB		r1, r1, r2, LSL #2
		MOV		r14, r2

_fastConvertBGRAToRGBA_outerLoop
		MOVS	r2, r14, ASR #2			// amount of 4x32 bit writes
		BEQ		_fastConvertBGRAToRGBA_tail

_fastConvertBGRAToRGBA_innerLoop

		LDMIA	r0!, {r4-r7}		// AARRGGBB
		SUBS	r2, #1
		REV		r4, r4				// BBGGRRAA
		REV		r5, r5
		REV		r6, r6				
		REV		r7, r7
		MOV		r8, r4, ROR #8		// AABBGGRR
		MOV		r9, r5, ROR #8
		MOV		r10, r6, ROR #8		
		MOV		r11, r7, ROR #8
		STMIA	r12!, {r8-r11}
		BNE		_fastConvertBGRAToRGBA_innerLoop

_fastConvertBGRAToRGBA_tail
		MOV 	r2, r14, ASR #2
		SUBS	r2, r14, r2, LSL #2		// number of remaining writes in the tail

_fastConvertBGRAToRGBA_tail_loop

		LDRNE	r4, [r0], #4
		REVNE	r4, r4
		MOVNE	r8, r4, ROR #8
		STRNE	r8, [r12], #4
		SUBNES	r2, #1
		BNE		_fastConvertBGRAToRGBA_tail_loop

		SUBS	r3, #1
		ADD		r0, r0, r1
		BNE		_fastConvertBGRAToRGBA_outerLoop

		LDMFD	sp!, {r4-r12, lr}
		BX		lr

}
#else /* #if defined (M3G_HW_ARMV6) */
static void fastConvertBGRAToRGBA(const M3Gubyte *src, M3Gsizei srcStride,
                                  M3Gsizei width, M3Gsizei height,
                                  M3Gubyte *dst)
{
    unsigned int pixel, pixel2;
    unsigned int temp;
    unsigned int mask = 0x00ff00ff;
    int spanwidth = (width >> 1) - 1;
    int x, y;
    unsigned int *d = (unsigned int *)dst;

    M3G_ASSERT(width > 2);
    
    for (y = 0; y < height; ++y) {
        unsigned int *s = (unsigned int *)(src + y*srcStride);

        pixel = *s++;

        for (x = 0; x < spanwidth; ++x) {
            pixel2 = *s++;

            temp   = pixel & mask;          /* 00RR00BB */
            pixel  = pixel - temp;          /* AA00GG00 */
            pixel  = pixel | (temp << 16);  /* AABBGG00 */
            *d++   = pixel | (temp >> 16);  /* AABBGGRR */

            pixel = *s++;

            temp   = pixel2 & mask;          /* 00RR00BB */
            pixel2 = pixel2 - temp;          /* AA00GG00 */
            pixel2 = pixel2 | (temp << 16);  /* AABBGG00 */
            *d++   = pixel2 | (temp >> 16);  /* AABBGGRR */
        }
        
        pixel2 = *s++;
        temp   = pixel & mask;          /* 00RR00BB */
        pixel  = pixel - temp;          /* AA00GG00 */
        pixel  = pixel | (temp << 16);  /* AABBGG00 */
        *d++   = pixel | (temp >> 16);  /* AABBGGRR */

        temp   = pixel2 & mask;          /* 00RR00BB */
        pixel2 = pixel2 - temp;          /* AA00GG00 */
        pixel2 = pixel2 | (temp << 16);  /* AABBGG00 */
        *d++   = pixel2 | (temp >> 16);  /* AABBGGRR */
    }
}
#endif /* #if defined (M3G_HW_ARMV6) */

/*--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Maps a logical image format to an internal pixel format
 *
 * \param imgFormat logical image format
 * \param paletted  paletted flag
 * \return the internal image pixel format
 */
static M3GPixelFormat getInternalFormat(M3GImageFormat imgFormat,
                                        M3Gbool paletted)
{
    if (paletted) {
        switch (imgFormat) {
        case M3G_RGB:
#           if defined(M3G_NGL_TEXTURE_API)
            return M3G_PALETTE8_RGB8_32;
#           else
            return M3G_PALETTE8_RGB8;
#           endif
        case M3G_RGBA:
            return M3G_PALETTE8_RGBA8;
        default:
            M3G_ASSERT(M3G_FALSE);
            return (M3GPixelFormat)0;
        }
    }
    else {
        M3GPixelFormat format = m3gPixelFormat(imgFormat);
        
#       if defined(M3G_NGL_TEXTURE_API)
        if (format == M3G_RGB8) {
            return (M3G_USE_16BIT_TEXTURES) ? M3G_RGB565 : M3G_RGB8_32;
        }
        if (format == M3G_LA8) {
            return M3G_LA4;
        }
#       endif
        
        return format;
    }
}

/*!
 * \internal
 * \brief Gets the correct pixel format for setting data to an image
 */
static M3GPixelFormat m3gInputDataFormat(const Image *img)
{
    /* Any of the paletted formats will do for a paletted image, as
     * they all have 8-bit indices; we pick PALETTE8_RGBA8 here */
    
    if (img->flags & M3G_PALETTED) {
        return M3G_PALETTE8_RGBA8;
    }
    
    return m3gPixelFormat(img->format);
}


/*!
 * \internal
 * \brief Returns log2(resolution)+1. Assumes that resolution is power of two.
 *
 * \param w width in pixels
 * \param h height in pixels
 * \return number of needed mipmap levels
 */
static M3Gint m3gGetNumMipmapLevels(M3Gint w, M3Gint h)
{
    M3Gint res = (w > h) ? w : h;
    M3Gint levels = 0;
    while (res > 0) {
        ++levels;
        res >>= 1;
    };
    return levels;
}

/*!
 * \internal
 * \brief Downsamples an image to half the original size
 *
 *
 * \param format    pixel format
 * \param srcPixels source pixels
 * \param pw        pointer to width
 * \param ph        pointer to height
 * \param dstPixels destination pixels
 */
static void m3gDownsample(M3GPixelFormat format,
                          const M3Gubyte *srcPixels,
                          M3Gint *pw, M3Gint *ph,
                          M3Gubyte *dstPixels)
{
    M3Gint i, j, bpp, pixStride, lineStride;
    M3Gint w = *pw, h = *ph;
    M3Gubyte *dst;
    M3Guint temp[2][SPAN_BUFFER_SIZE/2];

    M3G_ASSERT_PTR(srcPixels);
    M3G_ASSERT(w >= 1 && h >= 1);

    bpp = m3gBytesPerPixel(format);
    lineStride = (h > 1) ? w * bpp : 0;
    pixStride = (w > 1) ? bpp : 0;

    dst = dstPixels;

    /* Iterate over buffer-sized blocks in the image */
    
    for (j = 0; j < h; j += 2) {
        for (i = 0; i < w; i += SPAN_BUFFER_SIZE/2) {
            
            /* Fill the buffer from the source image */
            
            const M3Gubyte *src = srcPixels + (j*lineStride + i*pixStride);
            M3Gint c = SPAN_BUFFER_SIZE/2;
            if (w - i < c) {
                c = w - i;
            }
            convertToARGB(format, src, c, &temp[0][0]);
            convertToARGB(format, src + lineStride, c, &temp[1][0]);
            if (w == 1) {
                temp[0][1] = temp[0][0];
                temp[1][1] = temp[1][0];
            }
            
            /* Average the pixels in the buffer */
            {
#               define AG_MASK 0xFF00FF00u
#               define RB_MASK 0x00FF00FFu
                
                M3Gint k;
                for (k = 0; k < c; k += 2) {
                    M3Guint ag, rb;

                    /* Add two components in parallel */
                    
                    ag =  ((temp[0][k] & AG_MASK) >> 8)
                        + ((temp[1][k] & AG_MASK) >> 8)
                        + ((temp[0][k+1] & AG_MASK) >> 8)
                        + ((temp[1][k+1] & AG_MASK) >> 8);
                        
                    rb =  (temp[0][k] & RB_MASK)
                        + (temp[1][k] & RB_MASK)
                        + (temp[0][k+1] & RB_MASK)
                        + (temp[1][k+1] & RB_MASK);

                    /* Shift to divide by 4, adding ½ for rounding */
                    
                    temp[0][k>>1] = ((((ag + 0x00020002u) << 6) & AG_MASK) |
                                     (((rb + 0x00020002u) >> 2) & RB_MASK));
                }
                
#               undef AG_MASK
#               undef RB_MASK
            }

            /* Write result to the output buffer */

            convertFromARGB(&temp[0][0], c>>1, format, dst);
            dst += (c>>1) * bpp;
        }
    }

    /* Return output width and height */
    
    if (w > 1) {
        *pw = (w >> 1);
    }
    if (h > 1) {
        *ph = (h >> 1);
    }
}

/*!
 * \internal
 * \brief Returns the OpenGL minification filter corresponding to M3G
 * filtering flags
 */
static GLenum m3gGetGLMinFilter(M3Genum levelFilter, M3Genum imageFilter) 
{
    static const GLenum minFilter[3][2] = {
        GL_LINEAR, GL_NEAREST,
        GL_LINEAR_MIPMAP_LINEAR, GL_NEAREST_MIPMAP_LINEAR,
        GL_LINEAR_MIPMAP_NEAREST, GL_NEAREST_MIPMAP_NEAREST
    };

    return minFilter[levelFilter - M3G_FILTER_BASE_LEVEL][imageFilter - M3G_FILTER_LINEAR];
}

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Converts an internal ARGB color to four GLfixed components
 */
static void m3gGLColor(M3Guint argb, GLfixed *dst)
{
    GLfixed r, g, b, a;
        
    r = (GLfixed)((argb & 0x00FF0000u) >> 16);
    g = (GLfixed)((argb & 0x0000FF00u) >>  8);
    b = (GLfixed)( argb & 0x000000FFu       );
    a = (GLfixed)((argb & 0xFF000000u) >> 24);

    dst[0] = ((r << 8) | r) + (r >> 7);
    dst[1] = ((g << 8) | g) + (g >> 7);
    dst[2] = ((b << 8) | b) + (b >> 7);
    dst[3] = ((a << 8) | a) + (a >> 7);
}

/*!
 * \internal
 * \brief Binds an image into the current texture unit and sets up
 * texture filtering
 */
static void m3gBindTextureImage(Image *img, M3Genum levelFilter, M3Genum imageFilter)
{
    M3G_ASSERT_GL;
    
    /* We have no mipmap generation for paletted images, so disable
     * mipmapping in that case */
    
    if (m3gIsInternallyPaletted(img)) {
        levelFilter = M3G_FILTER_BASE_LEVEL;
    }

    /* Bind the OpenGL texture object, generating mipmaps if
     * required */
    
    m3gBindTextureObject(img, levelFilter != M3G_FILTER_BASE_LEVEL);

    /* Set up OpenGL texture filtering according to our flags */

    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER,
                    (imageFilter == M3G_FILTER_LINEAR) ? GL_LINEAR : GL_NEAREST);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER,
                    m3gGetGLMinFilter(levelFilter, imageFilter));
    
    M3G_ASSERT_GL;
}

/*!
 * \internal
 * \brief Maps a logical image format to the matching default pixel
 * format
 * 
 * \param imgFormat logical image format
 * \return a one-byte-per-pixel pixel format
 */
static M3GPixelFormat m3gPixelFormat(M3GImageFormat imgFormat)
{
    switch (imgFormat) {
    case M3G_ALPHA:
        return M3G_A8;
    case M3G_LUMINANCE:
        return M3G_L8;
    case M3G_LUMINANCE_ALPHA:
        return M3G_LA8;
    case M3G_RGB:
        return M3G_RGB8;
    case M3G_RGBA:
        return M3G_RGBA8;
    default:
        M3G_ASSERT(M3G_FALSE);
        return M3G_NO_FORMAT;
    }
}

/*!
 * \internal
 * \brief Returns the number of bytes per pixel in a given pixel format
 *
 * \param format pixel format
 * \return bytes per pixel
 */
static M3Gint m3gBytesPerPixel(M3GPixelFormat format)
{
    switch (format) {
    case M3G_L8:
    case M3G_A8:
    case M3G_LA4:
    case M3G_PALETTE8_RGB8:
    case M3G_PALETTE8_RGB8_32:
    case M3G_PALETTE8_RGBA8:
        return 1;
    case M3G_RGB4:
    case M3G_RGB565:
    case M3G_RGBA4:
    case M3G_RGB5A1:
    case M3G_LA8:
        return 2;
    case M3G_RGB8:
        return 3;
    case M3G_RGBA8:
    case M3G_BGRA8:
    case M3G_ARGB8:
    case M3G_BGR8_32:
    case M3G_RGB8_32:
        return 4;
    default:
        M3G_ASSERT(M3G_FALSE);
        return 0;
    }
}

/*!
 * \internal
 * \brief Converts pixels between formats
 *
 * \note Only a limited subset of source and destination formats may
 * be supported; see the \c convert functions in m3g_image.c
 *
 * \param srcFormat source format
 * \param src       source pixels
 * \param dstFormat destination format
 * \param dst       destination pixels
 * \param count     pixel count
 */
static void m3gConvertPixels(M3GPixelFormat srcFormat, const M3Gubyte *src,
                             M3GPixelFormat dstFormat, M3Gubyte *dst,
                             M3Gsizei count)
{
    M3Guint temp[SPAN_BUFFER_SIZE];
    const char endianTest[4] = { 1, 0, 0, 0 };

    M3Guint srcBpp = m3gBytesPerPixel(srcFormat);
    M3Guint dstBpp = m3gBytesPerPixel(dstFormat);
    M3G_ASSERT(srcBpp > 0 && dstBpp > 0);

    while (count > 0) {
        M3Gsizei n = count;

        /* Check the source and destination formats to avoid 
           the intermediate ARGB format conversion. */
        if (((srcFormat == M3G_RGBA8 && (dstFormat == M3G_BGRA8 || dstFormat == M3G_BGR8_32))
            || (dstFormat == M3G_RGBA8 && (srcFormat == M3G_BGRA8 || srcFormat == M3G_BGR8_32))) 
            && (n > 2) && ((*(const int *)endianTest) == 1)) {
            /* use fast path for RGBA<->BGRA conversion */
            fastConvertBGRAToRGBA(src, n * srcBpp, n, 1, dst);
        } else if (srcFormat == M3G_ARGB8 && dstFormat != M3G_ARGB8) {
            convertFromARGB((M3Guint*)src, n, dstFormat, dst);
        } else if (srcFormat != M3G_ARGB8 && dstFormat == M3G_ARGB8) {
            convertToARGB(srcFormat, src, n, (M3Guint*)dst);
        } else {
            /* no luck, do the conversion via ARGB (source format -> ARGB -> destination format) */
            n = (count < SPAN_BUFFER_SIZE) ? count : SPAN_BUFFER_SIZE;
            convertToARGB(srcFormat, src, n, temp);
            convertFromARGB(temp, n, dstFormat, dst);
        }
        count -= n;
        src += n * srcBpp;
        dst += n * dstBpp;
    }
}

/*!
 * \internal
 * \brief Copies image data. The source image is copied to
 * the destination image.
 *
 * \param dst destination image
 * \param src source image
 */
static void m3gCopyImagePixels(Image *dst,
                               const Image *src)
{
    const M3Gubyte *pSrc;
    M3Gubyte *pDst;
    M3Gint bpp;

    /* Check inputs (debug only!) */
    M3G_VALIDATE_OBJECT(dst);
    M3G_VALIDATE_OBJECT(src);

    M3G_ASSERT(src->internalFormat == dst->internalFormat);
    M3G_ASSERT(src->format == dst->format);

    M3G_ASSERT(src->paletteBytes == dst->paletteBytes);
    
    /* Compute source and destination pixel data pointers */
    pSrc = (M3Gubyte *)m3gMapObject(M3G_INTERFACE(src), src->data);
    pDst = (M3Gubyte *)m3gMapObject(M3G_INTERFACE(dst), dst->data);

    bpp = m3gBytesPerPixel(src->internalFormat);

    if (src->paletteBytes > 0) {
        m3gCopy(pDst, pSrc, src->paletteBytes);
        pDst += dst->paletteBytes;
        pSrc += src->paletteBytes;
    }

    /* Do a straight copy if the sizes match, or resample if not */
    if (src->width == dst->width && src->height == dst->height ) {
        m3gCopy(pDst, pSrc, src->width * src->height * bpp);
    }
    else {
        /* Adder values as 8.8 fixed point */
        M3Gint xAdd, yAdd;
        M3Gint x, y;

        xAdd = (256 * src->width) / dst->width;
        yAdd = (256 * src->height) / dst->height;

        for (y = 0; y < dst->height; y++) {
            for (x = 0; x < dst->width; x++) {
                m3gCopy(pDst, pSrc + bpp * (((xAdd * x) >> 8) + ((yAdd * y) >> 8) * src->width), bpp);
                pDst += bpp;
            }
        }
    }

    m3gUnmapObject(M3G_INTERFACE(dst), dst->data);
    m3gUnmapObject(M3G_INTERFACE(src), src->data);

    m3gInvalidateImage(dst);
}

/*!
 * \internal
 * \brief Invalidates any cached data for this image
 *
 * Used when rendering to the image.
 *
 * \param img Image object
 */
static void m3gInvalidateImage(Image *img)
{
    M3G_VALIDATE_OBJECT(img);
    img->dirty = M3G_TRUE;
    
#   if !defined(M3G_NGL_TEXTURE_API)
    if (img->large) {
        img->large->dirty = M3G_TRUE;
    }
#   endif /*M3G_NGL_TEXTURE_API*/

    if (img->powerOfTwo != img) {
        img->powerOfTwoDirty = M3G_TRUE;
    }
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param originalObj original Image object
 * \param cloneObj pointer to cloned Image object
 * \param pairs array for all object-duplicate pairs
 * \param numPairs number of pairs
 */
static M3Gbool m3gImageDuplicate(const Object *originalObj,
                                 Object **cloneObj,
                                 Object **pairs,
                                 M3Gint *numPairs)
{
    Image *original = (Image *)originalObj;
    Image *clone;

    /* If the original image still has its pixel data, make a full
     * copy -- this is wasteful for immutable images, but the shame's
     * on the user in that case */
    
    if (original->data) {
        clone = (Image*) m3gCreateImage(originalObj->interface,
                                        original->format,
                                        original->width,
                                        original->height,
                                        original->flags);
    }
    else {

        /* Otherwise, just point to the original and use its data
         * buffers */
        
        clone = (Image*) m3gAlloc(M3G_INTERFACE(original), sizeof(*clone));
        *clone = *original;
        M3G_ASSIGN_REF(clone->copyOf, original);
    }
    
    *cloneObj = (Object *)clone;
    if (*cloneObj == NULL) {
        return M3G_FALSE;
    }

    if (m3gObjectDuplicate(originalObj, cloneObj, pairs, numPairs)) {
        /* Copy image contents */
        if (original->data) {
            m3gCopyImagePixels(clone, original);
        }
        return M3G_TRUE;
    }
    else {
        return M3G_FALSE;
    }
}

/*!
 * \internal
 *
 * \brief Frees the pixel data associated with this image; used for
 * optimizing memory usage after copying the data to a secondary
 * location
 */
static void m3gFreeImageData(Image *img)
{
    M3G_ASSERT(img->format == M3G_RGB || img->format == M3G_LUMINANCE);
#   if !defined(M3G_NGL_TEXTURE_API)
    M3G_ASSERT(!img->mipmapsDirty);
#   endif
    M3G_ASSERT(!img->powerOfTwoDirty);
    M3G_ASSERT(img->powerOfTwo != NULL);
    M3G_ASSERT(!img->pinned);
    
    M3G_LOG1(M3G_LOG_IMAGES, "Freeing copy of image 0x%08X\n",
             (unsigned) img);

    if (!img->copyOf) {
        m3gFreeObject(M3G_INTERFACE(img), img->data);
        img->data = 0;
        m3gFreeObject(M3G_INTERFACE(img), img->mipData);
        img->mipData = 0;
    }
    M3G_ASSIGN_REF(img->copyOf, NULL);
}

/*!
 * \internal
 * \brief Returns a power-of-two variant of an image
 *
 * This is used for sprites and background images.
 */
static Image *m3gGetPowerOfTwoImage(Image *img)
{
    M3G_VALIDATE_OBJECT(img);
    
    /* Create a power-of-two variant of the image if one doesn't exist
     * already */
    
    if (img->powerOfTwo == NULL) {

        M3Gint width, height;
        M3Gbitmask flags;
        Image *potImage;
        
        M3G_ASSERT(!m3gIsPowerOfTwo(img->width) ||
                   !m3gIsPowerOfTwo(img->height));
        
        /* Choose new image size to allow a maximum shrinkage of 25%;
         * this is to weed out pathological cases of quadruple memory
         * usage because an image is one pixel too wide */

        width  = m3gNextPowerOfTwo((img->width * 3) >> 2);
        height = m3gNextPowerOfTwo((img->height * 3) >> 2);
        
        width  = M3G_MIN(width, M3G_MAX_TEXTURE_DIMENSION);
        height = M3G_MIN(height, M3G_MAX_TEXTURE_DIMENSION);
        
        flags = img->flags & (~M3G_RENDERING_TARGET);
        
        potImage = m3gCreateImage(M3G_INTERFACE(img),
                                  img->format,
                                  width, height,
                                  flags);
        if (!potImage) {
            return NULL; /* automatic out-of-memory */
        }

        M3G_ASSIGN_REF(img->powerOfTwo, potImage);
        img->powerOfTwoDirty = M3G_TRUE;
    }

    /* Update POT image data if necessary */
    
    if (img->powerOfTwoDirty) {
        m3gCopyImagePixels(img->powerOfTwo, img);
        img->powerOfTwoDirty = M3G_FALSE;

        /* Get rid of the original at this point if we can */
        
        if (!img->pinned) {
            m3gFreeImageData(img);
        }
    }

    return img->powerOfTwo;
}

/*!
 * \internal
 * \brief Gets image alpha at x, y.
 *
 * \param image Image object
 * \param x x-coordinate
 * \param y y-coordinate
 * \return alpha value
 *
 */
static M3Gint m3gGetAlpha(Image *image, M3Gint x, M3Gint y)
{
    M3Gint alpha = 255;
    M3Gint bpp = m3gBytesPerPixel(image->internalFormat);
    M3Guint data = 0;
    M3Gubyte *pixels;

    /* Quick exit for non-alpha formats */
    
    if (image->format == M3G_RGB || image->format == M3G_LUMINANCE) {
        return alpha;
    }

    /* For other formats, we have to sample the image data */

    if (!image->data) {
        Image *potImage = image->powerOfTwo;
        M3G_ASSERT(potImage != image);
        return m3gGetAlpha(potImage,
                           (x * image->width) / potImage->width,
                           (y * image->height) / potImage->height);
    }
    
    pixels = ((M3Gubyte *)m3gMapObject(M3G_INTERFACE(image), image->data));

    if (image->paletteBytes == 0) {
        if (bpp == 1) {
            data = pixels[x + y * image->width];
        }
        else if (bpp == 2) {
            data = ((M3Gushort *)pixels)[x + y * image->width];
        }
        else {
            data = ((M3Guint *)pixels)[x + y * image->width];
        }
    }
    else {
        M3Guint *palette;
        palette = (M3Guint *)pixels;
        pixels += image->paletteBytes;

        data = palette[pixels[x + y * image->width]];
    }

    m3gUnmapObject(M3G_INTERFACE(image), image->data);

    switch (image->internalFormat) {

    case M3G_A8:
        alpha = data;
        break;
    case M3G_LA8:
        alpha = data >> 8;
        break;
    case M3G_RGBA8:
        alpha = data >> 24;
        break;
    default:
        /* Should never be here!! */
        M3G_ASSERT(M3G_FALSE);
    }

    return alpha;
}

/*!
 * \internal
 * \brief Computes the scanline stride of an image
 */
static M3Gsizei m3gGetImageStride(const Image *img)
{
    M3G_VALIDATE_OBJECT(img);
    return img->width * m3gBytesPerPixel(img->internalFormat);
}

/*----------------------------------------------------------------------
 * Virtual function table
 *--------------------------------------------------------------------*/

static const ObjectVFTable m3gvf_Image = {
    m3gObjectApplyAnimation,
    m3gObjectIsCompatible,
    m3gObjectUpdateProperty,
    m3gObjectDoGetReferences,
    m3gObjectFindID,
    m3gImageDuplicate,
    m3gDestroyImage
};


/*----------------------------------------------------------------------
 * Public API functions
 *--------------------------------------------------------------------*/

/*!
 * \brief Creates a new Image
 *
 * \param interface     M3G interface
 * \param srcFormat     source format
 * \param width         width in pixels
 * \param height        height in pixels
 * \param flags         creation flags; a combination of
 *                      M3G_DYNAMIC, M3G_STATIC,
 *                      M3G_RENDERING_TARGET, and M3G_PALETTED
 * \retval Image new Image object
 * \retval NULL Image creating failed
 */
M3G_API M3GImage m3gCreateImage(/*@dependent@*/ M3GInterface interface,
                                M3GImageFormat srcFormat,
                                M3Gint width, M3Gint height,
                                M3Gbitmask flags)
{
    Interface *m3g = (Interface *) interface;
    M3G_VALIDATE_INTERFACE(m3g);
    
    /* Check errors */
    
    if (width <= 0 || height <= 0) {
        m3gRaiseError(m3g, M3G_INVALID_VALUE);
        return NULL;
    }

    if (!m3gInRange(srcFormat, M3G_ALPHA, M3G_RGBA)) {
        m3gRaiseError(m3g, M3G_INVALID_ENUM);
        return NULL;
    }

    /* Parameters OK; allocate and initialize the object */

    {
        Image *img = m3gAllocZ(m3g, sizeof(Image));
        if (img == NULL) {
            return NULL;
        }

        /* Clean up and set flags */

        M3G_LOG3(M3G_LOG_IMAGES, "Image 0x%08X is %d x %d",
                 (unsigned) img, width, height);
        
        flags |= M3G_DYNAMIC;   /* the default */
        
        if (flags & M3G_STATIC) {
            M3G_LOG(M3G_LOG_IMAGES, ", immutable");
            flags &= ~M3G_DYNAMIC;
        }
        if (flags & M3G_RENDERING_TARGET) {
            M3G_LOG(M3G_LOG_IMAGES, ", rendertarget");
            flags |= M3G_DYNAMIC;
        }
        if (flags & M3G_PALETTED) {
            M3G_LOG(M3G_LOG_IMAGES, ", paletted");
        }
        img->flags = flags;

        M3G_LOG(M3G_LOG_IMAGES, "\n");
        
        {
            /* Allocate pixel & palette data; the palette is stored at
             * the beginning of the pixel data chunk */

            M3Gbool paletted = ((img->flags & M3G_PALETTED) != 0)
                && m3gSupportedPaletteFormat(srcFormat);
            M3GPixelFormat internalFormat = getInternalFormat(srcFormat,
                                                              paletted);
            M3Guint bpp = m3gBytesPerPixel(internalFormat);
            M3Guint pixelBytes = width * height * bpp;

            if ((img->flags & M3G_PALETTED) != 0 && !paletted) {
                M3G_LOG(M3G_LOG_WARNINGS|M3G_LOG_IMAGES,
                        "Warning: Unsupported paletted format\n");
            }
                
            /* The palette will always have 256 elements and one byte
             * per color component (except padded 32-bit for NGL) */
            
            if (paletted) {
                img->paletteBytes =
#                   if defined(M3G_NGL_TEXTURE_API)
                    256 * 4;
#                   else
                    256 * m3gBytesPerPixel(m3gPixelFormat(srcFormat));
#                   endif
            }

            /* Set up the rest of the image parameters */
            
            img->width = width;
            img->height = height;
            img->format = srcFormat;
            img->internalFormat = internalFormat;
            img->glFormat = m3gGetGLFormat(internalFormat);

            M3G_LOG1(M3G_LOG_IMAGES, "Image data %d bytes\n",
                     pixelBytes + img->paletteBytes);
            
            /* Allocate the image memory */
            
            img->data = m3gAllocObject(m3g, pixelBytes + img->paletteBytes);
            if (img->data == 0) {
                m3gFree(m3g, img);
                return NULL;
            }

#ifdef M3G_ENABLE_GLES_RESOURCE_HANDLING
            /* If GLES resource freeing (see function m3gFreeGLESResources) 
               is enabled, the GL texture might get deleted at any point, so
			   a copy of the texture data has to be always kept in memory. */
            img->pinned = M3G_TRUE;
#else           
            /* Lock the image data in memory if the image is dynamic,
             * or the format has alpha information; otherwise, we'll
             * be able to get rid of an extra copy when generating a
             * power-of-two version or uploading to OpenGL */
            
            if ((img->flags & M3G_DYNAMIC) != 0
                || (img->format != M3G_RGB &&
                    img->format != M3G_LUMINANCE)) {
                img->pinned = M3G_TRUE;
            }
#endif
            /* If the image can be used as a rendering target, clear
             * to opaque white by default */
            
            if ((img->flags & M3G_RENDERING_TARGET) != 0) {
                M3Gubyte *pixels = ((M3Gubyte *)m3gMapObject(m3g, img->data))
                    + img->paletteBytes;
                m3gFill(pixels, (size_t) pixelBytes, -1); 
                m3gUnmapObject(m3g, img->data);
            }

            /* Check for "special" images that can't be used as
             * textures without some extra trickery */

            if (!m3gIsPowerOfTwo((M3Guint) width) ||
                !m3gIsPowerOfTwo((M3Guint) height)) {
                img->special |= IMG_NPOT;
            }
            else {
                img->powerOfTwo = img;
            }
            
            if (width > M3G_MAX_TEXTURE_DIMENSION ||
                height > M3G_MAX_TEXTURE_DIMENSION) {
                img->special |= IMG_LARGE;
            }
        }

        /* Call base class constructor (can not fail) and return */
        m3gInitObject(&img->object, m3g, M3G_CLASS_IMAGE);

        M3G_VALIDATE_OBJECT(img);
        return (M3GImage) img;
    }
}

/*!
 * \brief Prevents further modifications to an image
 *
 * Essentially, this changes the default M3G_DYNAMIC flag to
 * M3G_STATIC; this allows the implementation to make memory and
 * performance optimizations not possible for dynamically modified
 * images.
 */
M3G_API void m3gCommitImage(M3GImage hImage)
{
    Image *image = (Image *) hImage;
    M3Gbitmask flags;
    M3G_VALIDATE_OBJECT(image);
    
    flags = image->flags;
    flags &= ~(M3G_DYNAMIC|M3G_RENDERING_TARGET);
    flags |= M3G_STATIC;
    
    image->flags = flags;

#ifndef M3G_ENABLE_GLES_RESOURCE_HANDLING
    /* If the image format has no alpha information, we can discard
     * the image data under suitable conditions */
    
    if (image->format == M3G_RGB || image->format == M3G_LUMINANCE) {
        image->pinned = M3G_FALSE;
    }
#endif    
    M3G_LOG1(M3G_LOG_IMAGES, "Image 0x%08X made immutable\n",
             (unsigned) image);
}

/*!
 * \brief Check if image is mutable.
 * 
 * \param hImage Image object
 * \retval M3G_TRUE image is mutable
 * \retval M3G_FALSE image is immutable
 */
M3G_API M3Gbool m3gIsMutable(M3GImage hImage)
{
    Image *image = (Image *) hImage;
    M3G_VALIDATE_OBJECT(image);
    return ((image->flags & M3G_DYNAMIC) != 0);
}

/*!
 * \brief Gets image format as JSR-184 constant
 * 
 * \param hImage Image object
 * \return JSR-184 format
 */
M3G_API M3GImageFormat m3gGetFormat(M3GImage hImage)
{
    Image *image = (Image *) hImage;
    M3G_VALIDATE_OBJECT(image);
    return image->format;
}

/*!
 * \brief Gets image width
 * 
 * \param hImage Image object
 * \return width in pixels
 */
M3G_API M3Gint m3gGetWidth(M3GImage hImage)
{
    Image *image = (Image *) hImage;
    M3G_VALIDATE_OBJECT(image);
    return image->width;
}

/*!
 * \brief Gets image height
 * 
 * \param hImage Image object
 * \return height in pixels
 */
M3G_API M3Gint m3gGetHeight(M3GImage hImage)
{
    Image *image = (Image *) hImage;
    M3G_VALIDATE_OBJECT(image);
    return image->height;
}

/*!
 * \brief Converts a rectangle of pixels of src to dst as srcFormat to
 * dstFormat conversion requires.
 *
 * \param srcFormat source format
 * \param src       source pixels
 * \param srcStride source stride
 * \param width     width in pixels
 * \param height    height in pixels
 * \param dstFormat destination format
 * \param dst       destination pixels
 * \param dstStride destination stride
 */
static void m3gConvertPixelRect(
    M3GPixelFormat srcFormat, const M3Gubyte *src, M3Gsizei srcStride,
    M3Gsizei width, M3Gsizei height,
    M3GPixelFormat dstFormat, M3Gubyte *dst, M3Gsizei dstStride)
{
    /* Detect any fast path cases */
    
    if ((srcFormat == M3G_BGRA8 || srcFormat == M3G_BGR8_32)
        && dstFormat == M3G_RGBA8) {
        if (width > 2 && dstStride == width*4) {

            const char endianTest[4] = { 1, 0, 0, 0 };
            if ((*(const int *)endianTest) == 1) {
                fastConvertBGRAToRGBA(src, srcStride, width, height, dst);
            }
            return;
        }
    }

    /* No luck, do the generic conversion */
        
    while (height-- > 0) {
        m3gConvertPixels(srcFormat, src, dstFormat, dst, width);
        src += srcStride;
        dst += dstStride;
    }
}

/*!
 * \brief Sets the pixel data for an image
 * 
 * \param hImage Image object
 * \param srcPixels source pixels
 */
M3G_API void m3gSetImage(M3GImage hImage, const void *srcPixels)
{
    Image *img = (Image *) hImage;
    M3G_VALIDATE_OBJECT(img);

    {
        M3Gsizei bpp = m3gBytesPerPixel(m3gInputDataFormat(img));
        m3gSetSubImage(hImage,
                       0, 0, img->width, img->height,
                       img->width * img->height * bpp, srcPixels);
    }
}

/*!
 * \brief Reads pixel data from an image
 *
 * \param hImage Image object
 * \param pixels output buffer for pixels
 */
M3G_API void m3gGetImageARGB(M3GImage hImage, M3Guint *pixels)
{
    Interface *m3g;
    const Image *img = (const Image *) hImage;
    M3G_VALIDATE_OBJECT(img);
    m3g = M3G_INTERFACE(img);
    
    if (!pixels) {
        m3gRaiseError(m3g, M3G_NULL_POINTER);
        return;
    }
    
    if (img->data) {
        const M3Gubyte *src = (const M3Gubyte*) m3gMapObject(m3g, img->data);
        convertToARGB(img->internalFormat, src,
                      img->width * img->height,
                      pixels);
        m3gUnmapObject(m3g, img->data);
    }
}

/*!
 * \brief Sets the palette for an image
 * 
 * \param hImage Image object
 * \param paletteLength length of the palette
 * \param srcPalette palette data
 */
M3G_API void m3gSetImagePalette(M3GImage hImage,
                                M3Gint paletteLength,
                                const void *srcPalette)
{
    Interface *m3g;
    Image *img = (Image *) hImage;
    M3G_VALIDATE_OBJECT(img);
    m3g = M3G_INTERFACE(img);

    /* Check for errors */

    if (img->data == 0 || (img->flags & M3G_STATIC) != 0
            || (img->flags & M3G_PALETTED) == 0) {
        M3G_ASSERT(!(img->flags & M3G_DYNAMIC));
        m3gRaiseError(m3g, M3G_INVALID_OPERATION);
        return;
    }
    if (srcPalette == NULL) {
        m3gRaiseError(m3g, M3G_NULL_POINTER);
        return;
    }
    if (!m3gInRange(paletteLength, 0, 256)) {
        m3gRaiseError(m3g, M3G_INVALID_VALUE);
        return;
    }

    /*
     * Copy the palette data into the allocated palette (for natively
     * supported paletted formats), or remap the existing image data
     * using the supplied palette entries (for non-native formats)
     *
     * NOTE the latter is a one-time operation!
     */
    if (img->paletteBytes > 0) {
        M3Gubyte *palette = (M3Gubyte *)m3gMapObject(m3g, img->data);
#       if defined(M3G_NGL_TEXTURE_API)
        m3gConvertPixels(m3gPixelFormat(img->format), srcPalette,
                         M3G_RGBA8, palette,
                         paletteLength);
#       else
        M3Gsizei bpp = m3gBytesPerPixel(m3gPixelFormat(img->format));
        m3gCopy(palette, srcPalette, (size_t) paletteLength * bpp);
#       endif
    }
    else {
        M3Gint count = img->width * img->height;
        M3Gubyte *pixel = (M3Gubyte*)m3gMapObject(m3g, img->data);
        const M3Gubyte *bytePalette = (const M3Gubyte *) srcPalette;

        /* We need to treat the input and internal formats as
         * separate, as the internal storage may be padded to more
         * bytes than there are color components */

        M3GPixelFormat paletteFormat = m3gPixelFormat(img->format);
        const int numComponents = m3gBytesPerPixel(paletteFormat);
        M3GPixelFormat imgFormat = img->internalFormat;
        const int imgBpp = m3gBytesPerPixel(imgFormat);

        /* In most cases we can just copy the corresponding palette
         * entry on top of each pixel based on the pixel intensity (R
         * or L component), but special formats require a more
         * complicated conversion.  We just use the (slow) general
         * conversion routine, as it already incorporates support for
         * all formats. */
        
        if (imgBpp >= numComponents) {
            while (count--) {
                int offset = (*pixel) * numComponents;
                int c;
                for (c = 0; c < numComponents; ++c) {
                    *pixel++ = bytePalette[offset + c];
                }
                while (c++ < imgBpp) { /* padding for e.g. 24-bit RGB */
                    *pixel++ = 0xFF;
                }
            }
        }
        else {
            while (count--) {
                int offset = (*pixel) * numComponents;
                m3gConvertPixels(paletteFormat, &bytePalette[offset],
                                 imgFormat, pixel,
                                 1);
                pixel += imgBpp;
            }
        }
    }
    m3gUnmapObject(m3g, img->data);
    m3gInvalidateImage(img);
}

/*!
 * \brief Sets a scanline of an image
 * 
 * \param hImage Image object
 * \param line scanline
 * \param trueAlpha M3G_TRUE if the source image has an alpha channel,
 *                  M3G_FALSE if it should come from the RGB values;
 *                  this only matters for alpha-only destination images
 * \param pixels souce pixels
 */
M3G_API void m3gSetImageScanline(M3GImage hImage,
                                 M3Gint line,
                                 M3Gbool trueAlpha,
                                 const M3Guint *pixels)
{
    Image *img = (Image *) hImage;
    M3G_VALIDATE_OBJECT(img);

    if (img->data == 0 || (img->flags & M3G_STATIC) != 0
            || img->paletteBytes != 0) {
        m3gRaiseError(M3G_INTERFACE(img), M3G_INVALID_OPERATION);
        return;
    }
    
    {
        Interface *m3g = M3G_INTERFACE(img);
        M3Gint stride = img->width * m3gBytesPerPixel(img->internalFormat);
        M3Gubyte *dst = ((M3Gubyte *) m3gMapObject(m3g, img->data))
            + img->paletteBytes;

#ifdef M3G_NGL_TEXTURE_API
        /* For RGB images without alpha channel, source alpha is
         * forced to 0xff. */

        if (img->format == M3G_RGB) {
            M3Gint i;
            M3Guint argb, *dst;

            dst = (M3Guint *) pixels;

            for (i = 0; i < img->width; i++) {
                argb = *dst | 0xff000000;
                *dst++ = argb;
            }
        }
#endif

        /* Note that an alpha-only destination format is faked for
         * luminance if the source contained no true alpha data; alpha
         * is then inferred from the RGB values instead */
        
        convertFromARGB(pixels,
                        img->width,
                        (img->internalFormat == M3G_A8 && !trueAlpha) ? M3G_L8 : img->internalFormat,
                        dst + line * stride);

        m3gUnmapObject(m3g, img->data);
        m3gInvalidateImage(img);
    }
}

/*!
 * \brief Sets a rectangular subregion of an image
 * 
 * \param hImage Image object
 * \param x x-coordinate in destination image
 * \param y y-coordinate in destination image
 * \param width width of source pixels
 * \param height height of source pixels
 * \param length length of source data, in bytes
 * \param pixels source pixels
 */
M3G_API void m3gSetSubImage(M3GImage hImage,
                            M3Gint x, M3Gint y,
                            M3Gint width, M3Gint height,
                            M3Gint length, const void *pixels)
{
    Interface *m3g;
    Image *img = (Image *) hImage;

    M3GPixelFormat srcFormat;
    M3Gsizei srcBpp;

    M3G_VALIDATE_OBJECT(img);
    m3g = M3G_INTERFACE(img);

    /* Check for errors */

    if (img->data == 0 || (img->flags & M3G_STATIC) != 0) {
        M3G_ASSERT(!(img->flags & M3G_DYNAMIC));
        m3gRaiseError(m3g, M3G_INVALID_OPERATION);
        return;
    }
    if (pixels == NULL) {
        m3gRaiseError(m3g, M3G_INVALID_VALUE);
        return;
    }
    if (x < 0 || y < 0 || width <= 0 || height <= 0
            || x+width > img->width || y+height > img->height) {
        m3gRaiseError(m3g, M3G_INVALID_VALUE);
        return;
    }
    
    srcFormat = m3gInputDataFormat(img);
    srcBpp = m3gBytesPerPixel(srcFormat);
    
    if (length < width * height * srcBpp) {
        m3gRaiseError(m3g, M3G_INVALID_VALUE);
        return;
    }    

    /* Copy the image data, doing a conversion if the input format
     * does not match the internal storage format */
    {
        const M3Gubyte *srcPixels = (const M3Gubyte*) pixels;
        M3Gsizei srcStride = width * srcBpp;

        M3GPixelFormat dstFormat = img->internalFormat;
        M3Gsizei dstBpp = m3gBytesPerPixel(dstFormat);
        M3Gsizei dstStride = img->width * dstBpp;
        M3Gubyte *dstPixels =
            ((M3Gubyte *)m3gMapObject(m3g, img->data))
            + img->paletteBytes
            + y * dstStride + x * dstBpp;
        
        M3Gint numLines = height, numPixels = width;
        M3Gbool paletted = (img->flags & M3G_PALETTED) != 0;
        
        /* Optimize the copy for full image width */
        
        if (width == img->width) {
            numLines = 1;
            numPixels = width * height;
        }
        
        /* Copy a scanline at a time, converting as necessary */
        
        while (numLines-- > 0) {
            
            /* Matching pixel formats are just copied without
             * conversion, and all internally supported paletted
             * formats match each other physically, so they can be
             * copied as well */
        
            if (dstFormat == srcFormat || img->paletteBytes > 0) {
                m3gCopy(dstPixels, srcPixels, numPixels * dstBpp);
            }
            else {
                if (!paletted) {

                    /* Ordinary conversion into an internal format
                     * that is encoded differently from the external
                     * format; can not be a paletted image */

                    M3G_ASSERT((img->flags & M3G_PALETTED) == 0);
                    m3gConvertPixels(srcFormat, srcPixels,
                                     dstFormat, dstPixels,
                                     numPixels);
                }
                else {
                    M3G_ASSERT(!m3gSupportedPaletteFormat(img->format));
                    
                    /* Palette indices for one-byte-per-pixel formats
                     * are just copied in and mapped to actual values
                     * later; multibyte paletted formats require a
                     * conversion into LA, RGB, or RGBA format
                     * intensity levels temporarily before remapping
                     * to actual colors in m3gSetImagePalette */
                    
                    if (dstBpp == 1) {
                        m3gCopy(dstPixels, srcPixels, numPixels);
                    }
                    else {
                        m3gConvertPixels(M3G_L8, srcPixels,
                                         dstFormat, dstPixels,
                                         numPixels);
                    }
                }
            }
            
            srcPixels += srcStride;
            dstPixels += dstStride;
        }

        /* Release the image data and invalidate mipmap levels */
        
        m3gUnmapObject(m3g, img->data);
        m3gInvalidateImage(img);
    }
    M3G_VALIDATE_OBJECT(img);
}

#undef SPAN_BUFFER_SIZE

