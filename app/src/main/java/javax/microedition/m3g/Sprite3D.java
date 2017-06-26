package javax.microedition.m3g;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Hashtable;

import javax.microedition.khronos.opengles.GL10;

public class Sprite3D extends Node {
	private static Hashtable textures = new Hashtable();

	private int cropX = 0;
	private int cropY = 0;
	private int cropWidth;
	private int cropHeight;

	private boolean scaled = false;
	private Appearance appearance;
	private Image2D image;
	private Texture2D texture;

	private FloatBuffer vertexBuffer;
	private FloatBuffer textureBuffer;
	private float[] vertexArray = {1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f, 1.0f, -1.0f, 0.0f, -1.0f, -1.0f, 0.0f};
	private float[] textureArray;

	public Sprite3D(boolean scaled, Image2D image, Appearance appearance) {
		setImage(image);
		setAppearance(appearance);

		vertexBuffer = ByteBuffer.allocateDirect(4 * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		textureBuffer = ByteBuffer.allocateDirect(4 * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		textureArray = new float[4 * 2];

		cropWidth = image.getWidth();
		cropHeight = image.getHeight();
	}

	Object3D duplicateImpl() {
		Sprite3D copy = new Sprite3D(scaled, image, appearance);
		duplicate((Node) copy);
		copy.texture = texture;
		return copy;
	}

	public void setAppearance(Appearance appearance) {
		this.appearance = appearance;
	}

	public Appearance getAppearance() {
		return appearance;
	}

	public void setImage(Image2D image) {
		this.image = image;
		texture = (Texture2D) textures.get(image);

		if (texture == null) {
			texture = new Texture2D(image);
			texture.setFiltering(Texture2D.FILTER_LINEAR, Texture2D.FILTER_LINEAR);
			texture.setWrapping(Texture2D.WRAP_CLAMP, Texture2D.WRAP_CLAMP);
			texture.setBlending(Texture2D.FUNC_REPLACE);

			// cache texture
			textures.put(image, texture);
		}
	}

	public Image2D getImage() {
		return image;
	}

	public boolean isScaled() {
		return scaled;
	}

	public int getCropX() {
		return cropX;
	}

	public int getCropY() {
		return cropY;
	}

	public int getCropWidth() {
		return cropWidth;
	}

	public int getCropHeight() {
		return cropHeight;
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

	void render(GL10 gl, Transform t) {
/*
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glPushMatrix();
		//gl.glLoadIdentity();
		t.multGL(gl);
		
		// get current modelview matrix
		float[] m = new float[16];
		((GL11)gl).glGetFloatv(GL11.GL_MODELVIEW_MATRIX, m, 0);
		
		float[] m = new float[16];
		t.get(m);

		// get up and right vector, used to create a camera-facing quad
		//Vector3 up = new Vector3(m[4], m[5], m[6]);
		
		Vector3 up = new Vector3(m[1], m[5], m[9]);
		up.normalize();
		//Vector3 right = new Vector3(m[0], m[1], m[2]);
		Vector3 right = new Vector3(m[0], m[4], m[8]);
		right.normalize();

		float size = 1;
		Vector3 rightPlusUp = new Vector3(right);
		rightPlusUp.add(up);
		rightPlusUp.multiply(size);
		Vector3 rightMinusUp = new Vector3(right);
		rightMinusUp.subtract(up);
		rightMinusUp.multiply(size);

		Vector3 topLeft = new Vector3(rightMinusUp);
		topLeft.multiply(-1);

		Vector3 topRight = new Vector3(rightPlusUp);

		Vector3 bottomLeft = new Vector3(rightPlusUp);
		bottomLeft.multiply(-1);

		Vector3 bottomRight = new Vector3(rightMinusUp);

		Graphics3D.getInstance().setAppearance(getAppearance());
		Graphics3D.getInstance().disableTextureUnits();
		gl.glActiveTexture(GL10.GL_TEXTURE0);
		gl.glClientActiveTexture(GL10.GL_TEXTURE0);
		texture.setupGL(gl, new float[] { 1, 0, 0, 0 });
		
		int w = image.getWidth();
		int h = image.getHeight(); 
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
		
		// Top right
		vertexArray[0] = topRight.x;  
		vertexArray[1] = topRight.y;
		vertexArray[2] = topRight.z;
		// Top left
		vertexArray[3] = topLeft.x;
		vertexArray[4] = topLeft.x;
		vertexArray[5] = topLeft.x;
		// Bottom Right
		vertexArray[6] = bottomRight.x;
		vertexArray[7] = bottomRight.x;
		vertexArray[8] = bottomRight.x;
		// Bottom Left
		vertexArray[9] = bottomLeft.x;
		vertexArray[10] = bottomLeft.x;
		vertexArray[11] = bottomLeft.x;
		vertexBuffer.put(vertexArray);
		vertexBuffer.flip();

		// Draw the background
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);

		// Draw sprite
		/*
		gl.glBegin(GL10.GL_QUADS);

		gl.glTexCoord2f(0, 0);
		gl.glVertex3f(topLeft.x, topLeft.y, topLeft.z); // Top Left

		gl.glTexCoord2f(0, 1);
		gl.glVertex3f(bottomLeft.x, bottomLeft.y, bottomLeft.z); // Bottom Left

		gl.glTexCoord2f(1, 1);
		gl.glVertex3f(bottomRight.x, bottomRight.y, bottomRight.z); // Bottom Right

		gl.glTexCoord2f(1, 0);
		gl.glVertex3f(topRight.x, topRight.y, topRight.z); // Top Right

		gl.glEnd();/

		gl.glPopMatrix();

		gl.glDisable(GL10.GL_TEXTURE_2D);
		gl.glDepthMask(true);*/
	}

	boolean isCompatible(AnimationTrack track) {
		switch (track.getTargetProperty()) {
			case AnimationTrack.CROP:
				return true;
			default:
				return super.isCompatible(track);
		}
	}
}
