package javax.microedition.m3g;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

public class Sprite3D extends Node {
	private Appearance appearance;
	private Image2D image;

	private static final int FLIPX = 1;
	private static final int FLIPY = 2;
	private int flip;

	private int width;
	private int height;

	private int cropX = 0;
	private int cropY = 0;
	private int cropWidth;
	private int cropHeight;

	private boolean scaled = false;

	public Sprite3D(boolean scaled, Image2D image, Appearance appearance) {
		setAppearance(appearance);

		hasRenderables = true;
		this.scaled = scaled;
		this.image = image;
		setImage(image);
	}

	@Override
	Object3D duplicateImpl() {
		Sprite3D copy = new Sprite3D(scaled, image, appearance);
		super.duplicate(copy);
		copy.cropX = cropX;
		copy.cropY = cropY;
		copy.cropWidth = cropWidth;
		copy.cropHeight = cropHeight;
		copy.flip = flip;
		return copy;
	}

	@Override
	int doGetReferences(Object3D[] references) {
		int num = super.doGetReferences(references);
		if (image != null) {
			if (references != null)
				references[num] = image;
			num++;
		}
		if (appearance != null) {
			if (references != null)
				references[num] = appearance;
			num++;
		}
		return num;
	}

	@Override
	Object3D findID(int userID) {
		Object3D found = super.findID(userID);

		if ((found == null) && (image != null))
			found = image.findID(userID);
		if ((found == null) && (appearance != null))
			found = appearance.findID(userID);
		return found;
	}

	@Override
	int applyAnimation(int time) {
		int minValidity = super.applyAnimation(time);

		if (minValidity > 0) {
			Object3D app = appearance;

			if (app != null) {
				int validity = app.applyAnimation(time);
				minValidity = Math.min(validity, minValidity);
			}
		}
		return minValidity;
	}

	@Override
	void updateProperty(int property, float[] value) {
		switch (property) {
			case AnimationTrack.CROP:
				if (value.length > 2) {
					setCrop((int) value[0], (int) value[1], (int) Math.max(-Graphics3D.getMaxTextureSize(), Math.min(Graphics3D.getMaxTextureSize(), value[2])),
							(int) Math.max(-Graphics3D.getMaxTextureSize(), Math.min(Graphics3D.getMaxTextureSize(), value[3])));
				} else {
					setCrop((int) value[0], (int) value[1], getCropWidth(), getCropHeight());
				}
			default:
				super.updateProperty(property, value);
		}
	}

	private boolean getSpriteCoordinates(Graphics3D ctx, Camera cam, Transform toCamera, int[] vert, short[] texvert, QVec4 eyeSpace, short adjust) {
		QVec4 o = new QVec4(0, 0, 0, 1);
		QVec4 x = new QVec4(0.5f, 0, 0, 1);
		QVec4 y = new QVec4(0, 0.5f, 0, 1);

		int rIntX = cropX, rIntY = cropY;
		int rIntW = Math.min(width, cropX + cropWidth) - rIntX;
		int rIntH = Math.min(height, cropY + cropHeight) - rIntY;
		if (rIntW < 0 || rIntH < 0)
			return false;

		toCamera.mtx.transformVec4(o);
		toCamera.mtx.transformVec4(x);
		toCamera.mtx.transformVec4(y);

		QVec4 ot = new QVec4(o);

		o.scaleVec4((float) (1.0d / o.w));
		x.scaleVec4((float) (1.0d / x.w));
		y.scaleVec4((float) (1.0d / y.w));

		if (eyeSpace != null) {
			eyeSpace.x = o.x;
			eyeSpace.y = o.y;
			eyeSpace.z = o.z;
		}

		x.subVec4(o);
		y.subVec4(o);

		x.x = ot.x + new Vector3(x).lengthVec3();
		x.y = ot.y;
		x.z = ot.z;
		x.w = ot.w;

		y.y = ot.y + new Vector3(y).lengthVec3();
		y.x = ot.x;
		y.z = ot.z;
		y.w = ot.w;

		Transform projMatrix = new Transform();
		cam.getProjection(projMatrix);
		projMatrix.mtx.transformVec4(ot);
		projMatrix.mtx.transformVec4(x);
		projMatrix.mtx.transformVec4(y);

		ot.scaleVec4((float) (1.0d / ot.w));
		x.scaleVec4((float) (1.0d / x.w));
		y.scaleVec4((float) (1.0d / y.w));

		x.subVec4(ot);
		y.subVec4(ot);

		x.x = new Vector3(x).lengthVec3();
		y.y = new Vector3(y).lengthVec3();

		if (!scaled) {
			int[] viewport;
			if (ctx != null)
				viewport = new int[]{ctx.getViewportX(), ctx.getViewportY(), ctx.getViewportWidth(), ctx.getViewportHeight()};
			else
				viewport = new int[]{0, 0, 256, 256};

			x.x = rIntW / viewport[2];
			y.y = rIntH / viewport[3];

			ot.x = (ot.x - ((2 * cropX + cropWidth - 2 * rIntX - rIntW) / viewport[2]));

			ot.y = (ot.y + ((2 * cropY + cropHeight - 2 * rIntY - rIntH) / viewport[3]));
		} else {
			x.x /= cropWidth;
			y.y /= cropHeight;


			ot.x = (ot.x - ((float) (2 * cropX + cropWidth - 2 * rIntX - rIntW) * x.x));

			ot.y = (ot.y + ((float) (2 * cropY + cropHeight - 2 * rIntY - rIntH) * y.y));

			x.x = (x.x * (float) rIntW);
			y.y = (y.y * (float) rIntH);
		}

		vert[0 * 3 + 0] = (int) (65536 * (ot.x - x.x));
		vert[0 * 3 + 1] = (int) ((65536 * (ot.y + y.y)) + 0.5f);
		vert[0 * 3 + 2] = (int) (65536 * ot.z);

		vert[1 * 3 + 0] = vert[0 * 3 + 0];
		vert[1 * 3 + 1] = (int) (65536 * (ot.y - y.y));
		vert[1 * 3 + 2] = vert[0 * 3 + 2];

		vert[2 * 3 + 0] = (int) ((65536 * (ot.x * x.x)) + 0.5f);
		vert[2 * 3 + 1] = vert[0 * 3 + 1];
		vert[2 * 3 + 2] = vert[0 * 3 + 2];

		vert[3 * 3 + 0] = vert[2 * 3 + 0];
		vert[3 * 3 + 1] = vert[1 * 3 + 1];
		vert[3 * 3 + 2] = vert[0 * 3 + 2];

		if ((flip & FLIPX) == 0) {
			texvert[0 * 2 + 0] = (short) rIntX;
			texvert[1 * 2 + 0] = (short) rIntX;
			texvert[2 * 2 + 0] = (short) (rIntX + rIntW - adjust);
			texvert[3 * 2 + 0] = (short) (rIntX + rIntW - adjust);
		} else {
			texvert[0 * 2 + 0] = (short) (rIntX + rIntW - adjust);
			texvert[1 * 2 + 0] = (short) (rIntX + rIntW - adjust);
			texvert[2 * 2 + 0] = (short) rIntX;
			texvert[3 * 2 + 0] = (short) rIntX;
		}

		if ((flip & FLIPY) == 0) {
			texvert[0 * 2 + 1] = (short) rIntY;
			texvert[1 * 2 + 1] = (short) (rIntY + rIntH - adjust);
			texvert[2 * 2 + 1] = (short) rIntY;
			texvert[3 * 2 + 1] = (short) (rIntY + rIntH - adjust);
		} else {
			texvert[0 * 2 + 1] = (short) (rIntY + rIntH - adjust);
			texvert[1 * 2 + 1] = (short) rIntY;
			texvert[2 * 2 + 1] = (short) (rIntY + rIntH - adjust);
			texvert[3 * 2 + 1] = (short) rIntY;
		}

		return true;
	}


	public void setAppearance(Appearance appearance) {
		this.appearance = appearance;
	}

	public Appearance getAppearance() {
		return appearance;
	}

	public void setImage(Image2D image) {
		this.image = image;

		width = image.getWidth();
		height = image.getHeight();

		cropX = 0;
		cropY = 0;
		cropWidth = image.getWidth();
		cropHeight = image.getHeight();

		flip = 0;
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
		return ((flip & FLIPX) != 0) ? -cropWidth : cropWidth;
	}

	public int getCropHeight() {
		return ((flip & FLIPY) != 0) ? -cropHeight : cropHeight;
	}

	public void setCrop(int x, int y, int width, int height) {
		this.cropX = x;
		this.cropY = y;

		if (width < 0) {
			this.cropWidth = -width;
			flip |= FLIPX;
		} else {
			this.cropWidth = width;
			flip &= ~FLIPX;
		}

		if (height < 0) {
			this.cropHeight = -height;
			flip |= FLIPY;
		} else {
			this.cropHeight = height;
			flip &= ~FLIPY;
		}
	}

	void render(GL10 gl, Graphics3D ctx) {
		short[] texvert = new short[8];
		int[] vert = new int[12];
		QVec4 eyeSpace = new QVec4();
		Transform toCamera = new Transform();
		Camera curCamera = ctx.getCamera(toCamera);

		if (!getSpriteCoordinates(ctx, curCamera, toCamera, vert, texvert, eyeSpace, (short) 0))
			return;

		Appearance app = new Appearance();
		app.setupGL(gl);
		for (int i = 0; i < ctx.getTextureUnitCount(); i++) {
			gl.glClientActiveTexture(GL10.GL_TEXTURE0 + i);
			gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			gl.glActiveTexture(GL10.GL_TEXTURE0 + i);
			gl.glDisable(GL10.GL_TEXTURE_2D);
		}

		gl.glClientActiveTexture(GL10.GL_TEXTURE0);
		gl.glActiveTexture(GL10.GL_TEXTURE0);
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		ShortBuffer texvertbuf = ByteBuffer.allocateDirect(texvert.length * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
		texvertbuf.position(0);
		texvertbuf.put(texvert);
		gl.glTexCoordPointer(2, GL10.GL_SHORT, 0, texvertbuf);
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE);

		image.setupGL(gl);

		gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
		gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

		gl.glMatrixMode(GL10.GL_TEXTURE);
		gl.glLoadIdentity();
		gl.glScalef((float) (1.0d / image.getWidth()), (float) (1.0d / image.getHeight()), 1.f);
		gl.glMatrixMode(GL10.GL_MODELVIEW);

		app.setFog(appearance.getFog());
		app.setCompositingMode(appearance.getCompositingMode());

		gl.glColor4x(1 << 16, 1 << 16, 1 << 16, 1 << 16);

		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		IntBuffer vertbuf = ByteBuffer.allocateDirect(vert.length * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
		vertbuf.position(0);
		vertbuf.put(vert);
		gl.glVertexPointer(3, GL10.GL_FIXED, 0, vertbuf);

		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();

		float[] transform = new float[16];
		float scaleW[] = new float[]{0.f, 0.f, 0.f, 0.f, 0.f, 0.f, 0.f, 0.f, 0.f, 0.f, 0.f, 0.f, 0.f, 0.f, 0.f, 0.f};
		Matrix invProjMatrix = new Matrix();
		Transform projMatrix = new Transform();
		curCamera.getProjection(projMatrix);

		invProjMatrix.matrixInverse(projMatrix.mtx);
		invProjMatrix.getMatrixColumns(transform);

		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glMultMatrixf(transform, 0);
		scaleW[0] = scaleW[5] = scaleW[10] = scaleW[15] = eyeSpace.w;
		gl.glMultMatrixf(scaleW, 0);

		gl.glMatrixMode(GL10.GL_PROJECTION);
		projMatrix.mtx.getMatrixColumns(transform);
		gl.glLoadMatrixf(transform, 0);

		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);

		gl.glBindTexture(GL10.GL_TEXTURE_2D, 0);

		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glPopMatrix();
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glPopMatrix();
	}

	@Override
	boolean isCompatible(AnimationTrack track) {
		switch (track.getTargetProperty()) {
			case AnimationTrack.CROP:
				return true;
			default:
				return super.isCompatible(track);
		}
	}
}
