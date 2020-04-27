package javax.microedition.m3g;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Vector;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.lcdui.Image;

public class Image2D extends Object3D {
	public static final int ALPHA = 96;
	public static final int LUMINANCE = 97;
	public static final int LUMINANCE_ALPHA = 98;
	public static final int RGB = 99;
	public static final int RGBA = 100;

	private int format;
	private boolean isMutable;
	private int width;
	private int height;
	private ByteBuffer pixels;
	private int[] id = new int[]{0};
	private boolean dirty = true;
	static Vector recycledTextures = new Vector();

	@Override
	public void finalize() {
		if (id[0] != 0)
			recycledTextures.add(this);
	}

	void releaseTexture(GL10 gl) {
		if (id[0] != 0) {
			gl.glDeleteTextures(1, id, 0);
			id[0] = 0;
		}
	}

	public Image2D(int format, int width, int height) {
		this.isMutable = true;
		this.format = format;
		this.width = width;
		this.height = height;

		int bpp = getBytesPerPixel();

		pixels = ByteBuffer.allocateDirect(width * height * bpp).order(ByteOrder.nativeOrder());
		for (int i = 0; i < width * height; ++i) {
			for (int c = 0; c < bpp; ++c) {
				pixels.put((byte) 0xFF);
			}
		}
		pixels.flip();
	}

	public Image2D(int format, int width, int height, byte[] image) {
		this.isMutable = false;
		this.format = format;
		this.width = width;
		this.height = height;

		int bpp = getBytesPerPixel();

		if (image.length < width * height * bpp)
			throw new IllegalArgumentException("image.length != width*height");

		pixels = ByteBuffer.allocateDirect(width * height * bpp).order(ByteOrder.nativeOrder());
		pixels.put(image, 0, width * height * bpp);
		pixels.flip();
	}

	public Image2D(int format, int width, int height, byte[] image, byte[] palette) {
		this.isMutable = false;
		this.format = format;
		this.width = width;
		this.height = height;

		if (image.length < width * height)
			throw new IllegalArgumentException("image.length != width*height");

		int bytesPerPixel = getBytesPerPixel();
		pixels = ByteBuffer.allocateDirect(width * height * bytesPerPixel).order(ByteOrder.nativeOrder());
		for (int i = 0; i < width * height; ++i) {
			for (int c = 0; c < bytesPerPixel; ++c) {
				int index = ((int) image[i] & 0xFF) * bytesPerPixel + c;
				pixels.put(palette[index]);
			}
		}
		pixels.flip();
	}

	public Image2D(int format, Object image) {
		this.isMutable = false;
		this.format = format;

		if (image instanceof Image) {
			loadFromImage((Image) image);
		} else {
			throw new IllegalArgumentException("Unrecognized image object.");
		}
	}

	@Override
	Object3D duplicateImpl() {
		Image2D copy = new Image2D(format, width, height);
		pixels.rewind();
		copy.pixels.put(pixels);
		copy.pixels.flip();
		copy.isMutable = isMutable;
		return copy;
	}

	public void set(int px, int py, int wid, int hei, byte[] image) {
		int bpp = getBytesPerPixel();
		if (px == 0 && py == 0 && wid == this.width && hei == this.height) {
			pixels.rewind();
			pixels.put(image, 0, wid * hei * bpp);
		} else {
			for (int y = 0; y < hei; y++) {
				pixels.position((y + py) * this.width * bpp + px * bpp);
				pixels.put(image, y * wid * bpp, wid * bpp);
			}
		}
		pixels.rewind();
		dirty = true;
	}

	private void loadFromImage(Image image) {
		this.width = image.getWidth();
		this.height = image.getHeight();

		if (width == -1 || height == -1)
			throw new IllegalArgumentException("Failed to get width/height.");

		int[] packedPixels = new int[width * height];
		image.getRGB(packedPixels, 0, width, 0, 0, width, height);

		int bpp = getBytesPerPixel();
		pixels = ByteBuffer.allocateDirect(packedPixels.length * bpp).order(ByteOrder.nativeOrder());

		for (int row = 0; row < height; ++row) {
			for (int col = 0; col < width; ++col) {
				int packedPixel = packedPixels[row * width + col];
				if (bpp == 1)
					pixels.put((byte) ((packedPixel >> 24) & 0xFF));
				else if (bpp == 2) {
					// TODO
				} else if (bpp >= 3) {
					pixels.put((byte) ((packedPixel >> 16) & 0xFF));
					pixels.put((byte) ((packedPixel >> 8) & 0xFF));
					pixels.put((byte) ((packedPixel >> 0) & 0xFF));
					if (bpp >= 4)
						pixels.put((byte) ((packedPixel >> 24) & 0xFF));
				}
			}
		}
		pixels.flip();

	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getWidth() {
		return width;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getHeight() {
		return height;
	}

	public boolean isMutable() {
		return isMutable;
	}

	public int getFormat() {
		return format;
	}

	ByteBuffer getPixels() {
		return pixels;
	}

	int getBytesPerPixel() {
		switch (format) {
			case ALPHA:
				return 1;
			case LUMINANCE:
				return 1;
			case LUMINANCE_ALPHA:
				return 2;
			case RGB:
				return 3;
			case RGBA:
				return 4;
			default:
				throw new RuntimeException("Invalid format on image");
		}
	}

	int getGLFormat() {
		switch (format) {
			case ALPHA:
				return GL10.GL_ALPHA;
			case LUMINANCE:
				return GL10.GL_LUMINANCE;
			case LUMINANCE_ALPHA:
				return GL10.GL_LUMINANCE_ALPHA;
			case RGB:
				return GL10.GL_RGB;
			case RGBA:
				return GL10.GL_RGBA;
			default:
				throw new RuntimeException("Invalid format on image");
		}
	}

	void setupGL(GL10 gl) {
		if (id[0] == 0) {
			gl.glGenTextures(1, id, 0);
			dirty = true;
		}
		gl.glBindTexture(GL10.GL_TEXTURE_2D, id[0]);
		if (dirty) {
			gl.glTexImage2D(GL10.GL_TEXTURE_2D, 0, getGLFormat(), getWidth(), getHeight(), 0, getGLFormat(), GL10.GL_UNSIGNED_BYTE, getPixels());
			dirty = false;
		}
	}
}
