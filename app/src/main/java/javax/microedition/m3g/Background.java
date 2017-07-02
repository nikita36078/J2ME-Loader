package javax.microedition.m3g;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class Background extends Object3D {

	public static final int BORDER = 32;
	public static final int REPEAT = 33;
	private int backgroundColor = 0x00000000; // Default color is black
	private Image2D backgroundImage = null;
	private int backgroundImageModeX = BORDER;
	private int backgroundImageModeY = BORDER;
	private int cropX;
	private int cropY;
	private int cropWidth;
	private int cropHeight;
	private boolean colorClearEnabled = true; // Default
	private boolean depthClearEnabled = true; // Default
	private Texture2D backgroundTexture = null;

	private FloatBuffer vertexBuffer;
	private FloatBuffer textureBuffer;
	// top right, top left, bottom right, bottom left coordinates
	private float[] vertexArray = {1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f, 1.0f, -1.0f, 0.0f, -1.0f, -1.0f, 0.0f};
	private float[] textureArray;

	public Background() {
		// 4 elements, 3 coordinates per element, float type
		vertexBuffer = ByteBuffer.allocateDirect(4 * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		vertexBuffer.put(vertexArray);
		vertexBuffer.flip();
		// 4 elements, 2 coordinates per element, float type
		textureBuffer = ByteBuffer.allocateDirect(4 * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		textureArray = new float[4 * 2];
	}

	Object3D duplicateImpl() {
		Background copy = new Background();
		copy.backgroundColor = backgroundColor;
		copy.backgroundImage = backgroundImage;
		copy.backgroundImageModeX = backgroundImageModeX;
		copy.backgroundImageModeY = backgroundImageModeY;
		copy.cropX = cropX;
		copy.cropY = cropY;
		copy.cropWidth = cropWidth;
		copy.cropHeight = cropHeight;
		copy.colorClearEnabled = colorClearEnabled;
		copy.depthClearEnabled = depthClearEnabled;
		copy.backgroundTexture = backgroundTexture;
		return copy;
	}

	@Override
	int doGetReferences(Object3D[] references) {
		int num = super.doGetReferences(references);
		if (backgroundImage != null) {
			if (references != null)
				references[num] = backgroundImage;
			num++;
		}
		return num;
	}

	@Override
	Object3D findID(int userID) {
		Object3D found = super.findID(userID);

		if ((found == null) && (backgroundImage != null))
			found = backgroundImage.findID(userID);
		return found;
	}

	@Override
	void updateProperty(int property, float[] value) {
		switch (property) {
			case AnimationTrack.ALPHA:
				backgroundColor = (backgroundColor | 0xFF000000) & (ColConv.alpha1f(value[0]) << 24);
				break;
			case AnimationTrack.COLOR:
				backgroundColor = (backgroundColor | 0x00FFFFFF) & (ColConv.color3f(value[0], value[1], value[2]));
				break;
			case AnimationTrack.CROP:
				int x = (int)value[0];
				int y = (int)value[1];
				int width = cropWidth;
				int height = cropHeight;
				if (value.length > 2) {
					width = (value[2] < 0) ? 0 : (int)value[2];
					height = (value[3] < 0) ? 0 : (int)value[3];
				}
				setCrop(x, y, width, height);
				break;
			default:
				super.updateProperty(property, value);
		}
	}

	public int getColor() {
		return this.backgroundColor;
	}

	public void setColor(int color) {
		this.backgroundColor = color;
	}

	public int getCropX() {
		return this.cropX;
	}

	public int getCropY() {
		return this.cropY;
	}

	public int getCropWidth() {
		return this.cropWidth;
	}

	public int getCropHeight() {
		return this.cropHeight;
	}

	public void setCrop(int x, int y, int width, int height) {
		if ((width < 0) || (height < 0)) {
			throw new IllegalArgumentException("Width and height must be positive or zero");
		}
		this.cropX = x;
		this.cropY = y;
		this.cropWidth = width;
		this.cropHeight = height;
	}

	public void setColorClearEnable(boolean enable) {
		this.colorClearEnabled = enable;
	}

	public boolean isColorClearEnabled() {
		return this.colorClearEnabled;
	}

	public void setDepthClearEnable(boolean enable) {
		this.depthClearEnabled = enable;
	}

	public boolean isDepthClearEnabled() {
		return this.depthClearEnabled;
	}

	public void setImageMode(int modeX, int modeY) {
		if (((modeX != BORDER) && (modeX != REPEAT)) || ((modeY != BORDER) && (modeY != REPEAT))) {
			throw new IllegalArgumentException("Bad image mode");
		}

		this.backgroundImageModeX = modeX;
		this.backgroundImageModeY = modeY;
	}

	public int getImageModeX() {
		return this.backgroundImageModeX;
	}

	public int getImageModeY() {
		return this.backgroundImageModeY;
	}

	public void setImage(Image2D image) {
		if ((image != null) && (image.getFormat() != Image2D.RGB) && (image.getFormat() != Image2D.RGBA)) {
			throw new IllegalArgumentException("Image format must be RGB or RGBA");
		}
		this.backgroundImage = image;

		if (image != null) {
			backgroundTexture = new Texture2D(backgroundImage);
			backgroundTexture.setFiltering(Texture2D.FILTER_LINEAR, Texture2D.FILTER_LINEAR);
			backgroundTexture.setWrapping(Texture2D.WRAP_CLAMP, Texture2D.WRAP_CLAMP);
			backgroundTexture.setBlending(Texture2D.FUNC_REPLACE);
		} else {
			backgroundTexture = null;
		}
	}

	public Image2D getImage() {
		return backgroundImage;
	}

	void setupGL(GL10 gl) {

		// Clear the buffers
		Color c = new Color(backgroundColor);
		gl.glClearColor(c.r, c.g, c.b, c.a);

		int clearBits = 0;
		if (isColorClearEnabled())
			clearBits |= GL10.GL_COLOR_BUFFER_BIT;
		if (isDepthClearEnabled())
			clearBits |= GL10.GL_DEPTH_BUFFER_BIT;

		if (clearBits != 0)
			gl.glClear(clearBits);

		// Draw the background image if any
		if (backgroundImage != null) {

			int w = Graphics3D.getInstance().getViewportWidth();
			int h = Graphics3D.getInstance().getViewportHeight();

			gl.glMatrixMode(GL10.GL_PROJECTION);
			gl.glPushMatrix();
			gl.glLoadIdentity();
			gl.glOrthof(-1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f);
			gl.glMatrixMode(GL10.GL_MODELVIEW);
			gl.glPushMatrix();
			gl.glLoadIdentity();

			gl.glColorMask(true, true, true, true);
			gl.glDepthMask(false);
			gl.glDisable(GL10.GL_LIGHTING);
			gl.glDisable(GL10.GL_CULL_FACE);
			gl.glDisable(GL10.GL_BLEND);

			Graphics3D.getInstance().disableTextureUnits();

			gl.glActiveTexture(GL10.GL_TEXTURE0);
			gl.glClientActiveTexture(GL10.GL_TEXTURE0);
			backgroundTexture.setupGL(gl, new float[]{1, 0, 0, 0});

			// Calculate crop

			if (cropWidth <= 0)
				cropWidth = w;
			if (cropHeight <= 0)
				cropHeight = h;

			float u0 = (float) cropX / (float) w;
			float u1 = u0 + (float) cropWidth / (float) w;
			float v0 = (float) cropY / (float) h;
			float v1 = v0 + (float) cropHeight / (float) h;

			// Set texture coordinates
			// Top right
			textureArray[0] = u1;
			textureArray[1] = v0;
			// Top left
			textureArray[2] = u0;
			textureArray[3] = v0;
			// Bottom Right
			textureArray[4] = u1;
			textureArray[5] = v1;
			// Bottom Left
			textureArray[6] = u0;
			textureArray[7] = v1;
			textureBuffer.put(textureArray);
			textureBuffer.flip();

			// Draw the background
			gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
			gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer);
			gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);

			// Remove local context
			gl.glMatrixMode(GL10.GL_PROJECTION);
			gl.glPopMatrix();
			gl.glMatrixMode(GL10.GL_MODELVIEW);
			gl.glPopMatrix();
			gl.glDisable(GL10.GL_TEXTURE_2D);
			gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		}
	}

	boolean isCompatible(AnimationTrack track) {
		switch (track.getTargetProperty()) {
			case AnimationTrack.ALPHA:
			case AnimationTrack.COLOR:
			case AnimationTrack.CROP:
				return true;
			default:
				return super.isCompatible(track);
		}
	}

}
