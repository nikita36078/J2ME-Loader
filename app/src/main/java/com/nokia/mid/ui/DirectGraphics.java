package com.nokia.mid.ui;

import javax.microedition.lcdui.Image;

public interface DirectGraphics {
    public static final int FLIP_HORIZONTAL = 8192;
    public static final int FLIP_VERTICAL = 16384;
    public static final int ROTATE_180 = 180;
    public static final int ROTATE_270 = 270;
    public static final int ROTATE_90 = 90;
    public static final int TYPE_BYTE_1_GRAY = 1;
    public static final int TYPE_BYTE_1_GRAY_VERTICAL = -1;
    public static final int TYPE_BYTE_2_GRAY = 2;
    public static final int TYPE_BYTE_332_RGB = 332;
    public static final int TYPE_BYTE_4_GRAY = 4;
    public static final int TYPE_BYTE_8_GRAY = 8;
    public static final int TYPE_INT_8888_ARGB = 8888;
    public static final int TYPE_INT_888_RGB = 888;
    public static final int TYPE_USHORT_1555_ARGB = 1555;
    public static final int TYPE_USHORT_4444_ARGB = 4444;
    public static final int TYPE_USHORT_444_RGB = 444;
    public static final int TYPE_USHORT_555_RGB = 555;
    public static final int TYPE_USHORT_565_RGB = 565;

    void drawImage(Image image, int i, int i2, int i3, int i4);

    void drawPixels(byte[] bArr, byte[] bArr2, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8);

    void drawPixels(int[] iArr, boolean z, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8);

    void drawPixels(short[] sArr, boolean z, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8);

    void drawPolygon(int[] iArr, int i, int[] iArr2, int i2, int i3, int i4);

    void drawTriangle(int i, int i2, int i3, int i4, int i5, int i6, int i7);

    void fillPolygon(int[] iArr, int i, int[] iArr2, int i2, int i3, int i4);

    void fillTriangle(int i, int i2, int i3, int i4, int i5, int i6, int i7);

    int getAlphaComponent();

    int getNativePixelFormat();

    void getPixels(byte[] bArr, byte[] bArr2, int i, int i2, int i3, int i4, int i5, int i6, int i7);

    void getPixels(int[] iArr, int i, int i2, int i3, int i4, int i5, int i6, int i7);

    void getPixels(short[] sArr, int i, int i2, int i3, int i4, int i5, int i6, int i7);

    void setARGBColor(int i);
}
