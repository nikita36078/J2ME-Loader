package com.nokia.mid.ui;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class DirectGraphicsImp implements DirectGraphics {
    private Graphics graphics;
    private int x_b;

	private static final short WRAP_CLAMP = 240;

    public DirectGraphicsImp(Graphics graphics) {
        this.graphics = graphics;
    }

    private static int x_a(byte[] bArr, byte[] bArr2, int i, int i2) {
        int i3 = x_a(bArr[i], i2) ? 0 : 16777215;
        int i4 = (bArr2 == null || x_a(bArr2[i], i2)) ? -16777216 : 0;
        return i3 | i4;
    }

    private static boolean x_a(byte b, int i) {
        return (((byte) (1 << i)) & b) != 0;
    }

    public void drawImage(Image image, int i, int i2, int i3, int i4) {
        if (image == null) {
            throw new NullPointerException();
        }
        int i5;
        switch (i4) {
            case 90:
                i5 = 5;
                break;
            case 180:
                i5 = 3;
                break;
            case 270:
                i5 = 6;
                break;
            case 8192:
                i5 = 2;
                break;
            case 16384:
                i5 = 1;
                break;
            default:
                i5 = 0;
                break;
        }
        this.graphics.drawRegion(image, 0, 0, image.getWidth(), image.getHeight(), i5, i, i2, i3);
    }

    public void drawPixels(byte[] bArr, byte[] bArr2, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        if (bArr == null) {
            throw new NullPointerException();
        } else if (i5 < 0 || i6 < 0) {
            throw new IllegalArgumentException();
        } else {
            Graphics graphics = this.graphics;
            int i9;
            int i10;
            int i11;
            int i12;
            int x_a;
            if (i8 == 1) {
                i9 = 7;
                i10 = 0;
                while (i10 < i6) {
                    i11 = (i10 * i2) + i;
                    i12 = i9;
                    for (i9 = 0; i9 < i5; i9++) {
                        x_a = x_a(bArr, bArr2, (i11 + i9) / 8, i12);
                        if ((x_a >>> 24) != 0) {
                            if (graphics.getColor() != x_a) {
                                graphics.setColor(x_a);
                            }
                            graphics.drawLine(i9 + i3, i10 + i4, i9 + i3, i10 + i4);
                        }
                        i12--;
                        if (i12 < 0) {
                            i12 = 7;
                        }
                    }
                    i10++;
                    i9 = i12;
                }
            } else if (i8 == -1) {
                i10 = i / i2;
                i9 = i % i2;
                i12 = 0;
                for (i11 = 0; i11 < i6; i11++) {
                    x_a = (((i10 + i11) / 8) * i2) + i9;
                    for (int i13 = 0; i13 < i5; i13++) {
                        int x_a2 = x_a(bArr, bArr2, x_a + i13, i12);
                        if (graphics.getColor() != x_a2) {
                            graphics.setColor(x_a2);
                        }
                        if ((x_a2 >>> 24) != 0) {
                            graphics.drawLine(i13 + i3, i11 + i4, i13 + i3, i11 + i4);
                        }
                    }
                    i12++;
                    if (i12 > 7) {
                        i12 = 0;
                    }
                }
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    public void drawPixels(int[] iArr, boolean z, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        throw new IllegalArgumentException("TODO drawPixels(int pix[], boolean transparency, int off, int scanlen, int x, int y, int width, int height, int manipulation, int format)");
    }

    public void drawPixels(short[] sArr, boolean z, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        if (i8 != 4444) {
            throw new IllegalArgumentException("Illegal format: " + i8);
        }
        Graphics graphics = this.graphics;
        for (int i9 = 0; i9 < i6; i9++) {
            for (int i10 = 0; i10 < i5; i10++) {
                short s = sArr[(i + i10) + (i9 * i2)];
                int i11 = ((s & 15) * 15) | ((((((61440 & s) >>> 12) * 15) << 24) | ((((s & 3840) >>> 8) * 15) << 16)) | ((((s & WRAP_CLAMP) >>> 4) * 15) << 8));
                if (((-16777216 & i11) == 0 ? 1 : null) == null) {
                    graphics.setColor(i11);
                    graphics.drawLine(i3 + i10, i4 + i9, i3 + i10, i4 + i9);
                }
            }
        }
    }

    public void drawPolygon(int[] iArr, int i, int[] iArr2, int i2, int i3, int i4) {
        setARGBColor(i4);
        this.graphics.drawPolygon(iArr, i, iArr2, i2, i3);
    }

    public void drawTriangle(int i, int i2, int i3, int i4, int i5, int i6, int i7) {
        setARGBColor(i7);
        this.graphics.drawTriangle(i, i2, i3, i4, i5, i6);
    }

    public void fillPolygon(int[] iArr, int i, int[] iArr2, int i2, int i3, int i4) {
        setARGBColor(i4);
        this.graphics.fillPolygon(iArr, i, iArr2, i2, i3);
    }

    public void fillTriangle(int i, int i2, int i3, int i4, int i5, int i6, int i7) {
        this.graphics.setARGBColor(i7);
        this.graphics.fillTriangle(i, i2, i3, i4, i5, i6);
    }

    public int getAlphaComponent() {
        return this.x_b;
    }

    public int getNativePixelFormat() {
        return 1;
    }

    public void getPixels(byte[] bArr, byte[] bArr2, int i, int i2, int i3, int i4, int i5, int i6, int i7) {
        throw new IllegalArgumentException("public void getPixels(byte pix[], byte alpha[], int offset, int scanlen, int x, int y, int width, int height, int format)");
    }

    public void getPixels(int[] iArr, int i, int i2, int i3, int i4, int i5, int i6, int i7) {
        throw new IllegalArgumentException("!!!public void getPixels(int pix[], int offset, int scanlen, int x, int y, int width, int height, int format");
    }

    public void getPixels(short[] sArr, int i, int i2, int i3, int i4, int i5, int i6, int i7) {
        switch (i7) {
            case DirectGraphics.TYPE_USHORT_444_RGB /*444*/:
            case DirectGraphics.TYPE_USHORT_4444_ARGB /*4444*/:
                return;
            default:
                throw new IllegalArgumentException("Illegal format: " + i7);
        }
    }

    public void setARGBColor(int i) {
        this.x_b = i >>> 24;
        this.graphics.setARGBColor(i);
    }
}
