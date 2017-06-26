package javax.microedition.m3g;

import javax.microedition.khronos.opengles.GL10;

public class Texture2D extends Transformable {

	public static final int FILTER_BASE_LEVEL = 208;
	public static final int FILTER_LINEAR = 209;
	public static final int FILTER_NEAREST = 210;
	public static final int FUNC_ADD = 224;
	public static final int FUNC_BLEND = 225;
	public static final int FUNC_DECAL = 226;
	public static final int FUNC_MODULATE = 227;
	public static final int FUNC_REPLACE = 228;
	public static final int WRAP_CLAMP = 240;
	public static final int WRAP_REPEAT = 241;

	private Image2D image;
	private int blendColor = 0;
	private int blending = FUNC_MODULATE;
	private int wrappingS = WRAP_REPEAT;
	private int wrappingT = WRAP_REPEAT;
	private int levelFilter = FILTER_BASE_LEVEL;
	private int imageFilter = FILTER_NEAREST;

	private boolean textureInitialized = false;
	private int[] id = {0};

	public Texture2D(Image2D image) {
		setImage(image);
	}

	Object3D duplicateImpl() {
		Texture2D copy = new Texture2D(image);
		duplicate((Transformable) copy);
		copy.blendColor = blendColor;
		copy.blending = blending;
		copy.wrappingS = wrappingS;
		copy.wrappingT = wrappingT;
		copy.levelFilter = levelFilter;
		copy.imageFilter = imageFilter;
		return copy;
	}

	public void setBlendColor(int blendColor) {
		this.blendColor = blendColor & 0xFFFFFF;
	}

	public int getBlendColor() {
		return blendColor;
	}

	public void setBlending(int blending) {
		switch (blending) {
			case FUNC_REPLACE:
			case FUNC_MODULATE:
			case FUNC_DECAL:
			case FUNC_BLEND:
			case FUNC_ADD:
				this.blending = blending;
				break;
			default:
				throw new IllegalArgumentException("Bad blending function");
		}
	}

	public int getBlending() {
		return blending;
	}

	public void setImage(Image2D image) {
		if (image == null) {
			throw new NullPointerException();
		}

		int w = image.getWidth();
		int h = image.getHeight();
		if ((!Tools.isPowerOfTwo(w)) || (!Tools.isPowerOfTwo(h))) {
			throw new IllegalArgumentException("Width and height of image must be a positive power of two");
		}

		int maxSize = Graphics3D.getInstance().getMaxTextureSize();
		if ((w > maxSize) || (h > maxSize)) {
			throw new IllegalArgumentException("Width or height of image exceeds the implementation defined maximum");
		}

		this.image = image;
	}

	public Image2D getImage() {
		return image;
	}

	public void setFiltering(int levelFilter, int imageFilter) {
		if ((levelFilter != FILTER_BASE_LEVEL) && (levelFilter != FILTER_NEAREST) && (levelFilter != FILTER_LINEAR)) {
			throw new IllegalArgumentException("Bad levelFilter argument");
		}
		if ((imageFilter != FILTER_NEAREST) && (imageFilter != FILTER_LINEAR)) {
			throw new IllegalArgumentException("Bad imageFilter argument");
		}
		this.levelFilter = levelFilter;
		this.imageFilter = imageFilter;
	}

	public int getImageFilter() {
		return imageFilter;
	}

	public int getLevelFilter() {
		return levelFilter;
	}

	public void setWrapping(int wrappingS, int wrappingT) {
		if ((wrappingS != WRAP_CLAMP) && (wrappingS != WRAP_REPEAT)) {
			throw new IllegalArgumentException("Bad wrapping mode");
		}
		if ((wrappingT != WRAP_CLAMP) && (wrappingT != WRAP_REPEAT)) {
			throw new IllegalArgumentException("Bad wrapping mode");
		}
		this.wrappingS = wrappingS;
		this.wrappingT = wrappingT;
	}

	public int getWrappingS() {
		return wrappingS;
	}

	public int getWrappingT() {
		return wrappingT;
	}

	void setupGL(GL10 gl, float[] scaleBias) {

		gl.glEnable(GL10.GL_TEXTURE_2D);

		if (!textureInitialized) {
			gl.glGenTextures(1, id, 0);
			gl.glBindTexture(GL10.GL_TEXTURE_2D, id[0]);
			gl.glTexImage2D(GL10.GL_TEXTURE_2D, 0, image.getGLFormat(), image.getWidth(), image.getHeight(), 0, image
					.getGLFormat(), GL10.GL_UNSIGNED_BYTE, image.getPixels());
			textureInitialized = true;
		} else {
			gl.glBindTexture(GL10.GL_TEXTURE_2D, id[0]);
		}

		// Set filtering.
		gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, getGLFilter()); // Linear Filtering
		gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, getGLFilter()); // Linear Filtering

		// Set wrap mode
		gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, getGLWrap(this.wrappingS));
		gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, getGLWrap(this.wrappingT));

		// Set blendmode
		gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, getGLBlend());
		gl.glTexEnvfv(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_COLOR, Color.intToFloatArray(blendColor), 0);

		// set texture scale
		Transform t = new Transform();
		getCompositeTransform(t);

		gl.glMatrixMode(GL10.GL_TEXTURE);
		t.setGL(gl);
		gl.glTranslatef(scaleBias[1], scaleBias[2], scaleBias[3]);
		gl.glScalef(scaleBias[0], scaleBias[0], scaleBias[0]);
		gl.glMatrixMode(GL10.GL_MODELVIEW);
	}

	private int getGLFilter() {
		if (imageFilter == FILTER_NEAREST) {
			return GL10.GL_NEAREST;
		} else { // imageFilter == FILTER_LINEAR
			return GL10.GL_LINEAR;
		}
	}

	private int getGLWrap(int wrap) {
		switch (wrap) {
			case Texture2D.WRAP_CLAMP:
				return GL10.GL_CLAMP_TO_EDGE;
			default:
				return GL10.GL_REPEAT;
		}
	}

	private int getGLBlend() {
		switch (blending) {
			case FUNC_ADD:
				return GL10.GL_ADD;
			case FUNC_MODULATE:
				return GL10.GL_MODULATE;
			case FUNC_BLEND:
				return GL10.GL_BLEND;
			case FUNC_REPLACE:
				return GL10.GL_REPLACE;
			default:
				return GL10.GL_DECAL;
		}
	}

	boolean isCompatible(AnimationTrack track) {
		switch (track.getTargetProperty()) {
			case AnimationTrack.COLOR:
				return true;
			default:
				return super.isCompatible(track);
		}
	}
}
