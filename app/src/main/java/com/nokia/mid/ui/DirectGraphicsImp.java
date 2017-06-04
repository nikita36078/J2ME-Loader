package com.nokia.mid.ui;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.Sprite;

public class DirectGraphicsImp implements DirectGraphics {

    private Graphics graphics;
    private int alphaComponent;

    public DirectGraphicsImp(Graphics graphics) {
        this.graphics = graphics;
    }

    private static int doAlpha(byte[] pix, byte[] alpha, int pos, int shift) {
        int p;
        int a;
        if (isBitSet(pix[pos], shift))
            p = 0;
        else
            p = 0x00FFFFFF;
        if (alpha == null || isBitSet(alpha[pos], shift))
            a = 0xFF000000;
        else
            a = 0;
        return p | a;
    }

    private static boolean isBitSet(byte b, int pos) {
        return ((b & (byte) (1 << pos)) != 0);
    }

    public void drawImage(Image img, int x, int y, int anchor, int manipulation) {
        if (img == null) {
            throw new NullPointerException();
        }
        int transform;
        switch (manipulation) {
            case FLIP_HORIZONTAL:
                transform = Sprite.TRANS_MIRROR_ROT180;
                break;
            case FLIP_VERTICAL:
                transform = Sprite.TRANS_MIRROR;
                break;
            case ROTATE_90:
                transform = Sprite.TRANS_ROT90;
                break;
            case ROTATE_180:
                transform = Sprite.TRANS_ROT180;
                break;
            case ROTATE_270:
                transform = Sprite.TRANS_ROT270;
                break;
            default:
                transform = -1;
        }
        if (anchor >= 64 || transform == -1) {
            throw new IllegalArgumentException();
        } else {
            graphics.drawRegion(
                    img,
                    x + graphics.getTranslateX(), y + graphics.getTranslateY(),
                    img.getWidth(), img.getHeight(),
                    transform,
                    x + graphics.getTranslateX(), y + graphics.getTranslateY(), anchor);
            return;
        }
    }

    public void drawPixels(byte[] pix, byte[] alpha, int off, int scanlen, int x, int y, int width, int height, int manipulation, int format) {
        if (pix == null) {
            throw new NullPointerException();
        }
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException();
        }

        Graphics g = graphics;
        int c;

        if (format == TYPE_BYTE_1_GRAY) {

            int b = 7;

            for (int yj = 0; yj < height; yj++) {
                int line = off + yj * scanlen;
                int ypos = yj * width;
                for (int xj = 0; xj < width; xj++) {
                    c = doAlpha(pix, alpha, (line + xj) / 8, b);
                    if ((c >> 24 & 0xff) != 0)//alpha
                    {
                        if (g.getColor() != c) g.setColor(c);
                        g.drawLine(xj + x, yj + y, xj + x, yj + y);
                    }
                    b--;
                    if (b < 0) b = 7;
                }
            }
        } else if (format == TYPE_BYTE_1_GRAY_VERTICAL) {
            int ods = off / scanlen;
            int oms = off % scanlen;
            int b = 0;
            for (int yj = 0; yj < height; yj++) {
                int ypos = yj * width;
                int tmp = (ods + yj) / 8 * scanlen + oms;
                for (int xj = 0; xj < width; xj++) {
                    c = doAlpha(pix, alpha, tmp + xj, b);
                    if (g.getColor() != c) g.setColor(c);
                    if ((c >> 24 & 0xff) != 0) //alpha
                        g.drawLine(xj + x, yj + y, xj + x, yj + y);
                }
                b++;
                if (b > 7) b = 0;
            }
        } else
            throw new IllegalArgumentException();
    }

    public void drawPixels(int[] iArr, boolean z, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        throw new IllegalArgumentException("TODO drawPixels(int pix[], boolean transparency, int off, int scanlen, int x, int y, int width, int height, int manipulation, int format)");
    }

    public void drawPixels(short pix[], boolean trans, int off, int scanlen, int x, int y, int width, int height, int manipulation, int format) {
        if (format != TYPE_USHORT_4444_ARGB) {
            throw new IllegalArgumentException("Illegal format: " + format);
        }
        Graphics g = graphics;
        for (int iy = 0; iy < height; iy++) {
            for (int ix = 0; ix < width; ix++) {
                int c = toARGB(pix[off + ix + iy * scanlen], TYPE_USHORT_4444_ARGB);
                if (!isTransparent(c)) {
                    g.setColor(c);
                    g.drawLine(x + ix, y + iy, x + ix, y + iy);
                }
            }
        }
    }

    private static boolean isTransparent(int s) {
        return (s & 0xFF000000) == 0;
    }

    private static int toARGB(int s, int type) {
        switch (type) {
            case TYPE_USHORT_4444_ARGB: {
                int a = ((s) & 0xF000) >>> 12;
                int r = ((s) & 0x0F00) >>> 8;
                int g = ((s) & 0x00F0) >>> 4;
                int b = ((s) & 0x000F);
                s = ((a * 15) << 24) | ((r * 15) << 16) | ((g * 15) << 8) | (b * 15);
                break;
            }
            case TYPE_USHORT_444_RGB: {
                int r = ((s) & 0x0F00) >>> 8;
                int g = ((s) & 0x00F0) >>> 4;
                int b = ((s) & 0x000F);
                s = ((r * 15) << 16) | ((g * 15) << 8) | (b * 15);
                break;
            }
        }
        return s;
    }

    public void drawPolygon(int[] iArr, int i, int[] iArr2, int i2, int i3, int i4) {
        setARGBColor(i4);
        graphics.drawPolygon(iArr, i, iArr2, i2, i3);
    }

    public void drawTriangle(int i, int i2, int i3, int i4, int i5, int i6, int i7) {
        setARGBColor(i7);
        graphics.drawTriangle(i, i2, i3, i4, i5, i6);
    }

    public void fillPolygon(int[] iArr, int i, int[] iArr2, int i2, int i3, int i4) {
        setARGBColor(i4);
        graphics.fillPolygon(iArr, i, iArr2, i2, i3);
    }

    public void fillTriangle(int i, int i2, int i3, int i4, int i5, int i6, int i7) {
        graphics.setARGBColor(i7);
        graphics.fillTriangle(i, i2, i3, i4, i5, i6);
    }

    public int getAlphaComponent() {
        return alphaComponent;
    }

    public int getNativePixelFormat() {
        return TYPE_BYTE_1_GRAY;
    }

    public void getPixels(byte[] bArr, byte[] bArr2, int i, int i2, int i3, int i4, int i5, int i6, int i7) {
        throw new IllegalArgumentException("public void getPixels(byte pix[], byte alpha[], int offset, int scanlen, int x, int y, int width, int height, int format)");
    }

    public void getPixels(int[] iArr, int i, int i2, int i3, int i4, int i5, int i6, int i7) {
        throw new IllegalArgumentException("!!!public void getPixels(int pix[], int offset, int scanlen, int x, int y, int width, int height, int format");
    }

    public void getPixels(short[] sArr, int i, int i2, int i3, int i4, int i5, int i6, int i7) {
        switch (i7) {
            case DirectGraphics.TYPE_USHORT_444_RGB:
            case DirectGraphics.TYPE_USHORT_4444_ARGB:
                return;
            default:
                throw new IllegalArgumentException("Illegal format: " + i7);
        }
    }

    public void setARGBColor(int argb) {
        alphaComponent = (argb >> 24 & 0xff);
        graphics.setColor(argb & 0xffffff);
    }
}
