package javax.microedition.m3g;

import javax.microedition.khronos.opengles.GL10;

public class PolygonMode extends Object3D {

	public static final int CULL_BACK = 160;
	public static final int CULL_FRONT = 161;
	public static final int CULL_NONE = 162;
	public static final int SHADE_FLAT = 164;
	public static final int SHADE_SMOOTH = 165;
	public static final int WINDING_CCW = 168;
	public static final int WINDING_CW = 169;

	private int culling = CULL_BACK;
	private int shading = SHADE_SMOOTH;
	private int winding = WINDING_CCW;
	private boolean twoSidedLightingEnabled = false;
	private boolean localCameraLightingEnabled = false;
	private boolean perspectiveCorrectionEnabled = true;

	@Override
	Object3D duplicateImpl() {
		PolygonMode copy = new PolygonMode();
		copy.culling = culling;
		copy.shading = shading;
		copy.winding = winding;
		copy.twoSidedLightingEnabled = twoSidedLightingEnabled;
		copy.localCameraLightingEnabled = localCameraLightingEnabled;
		copy.perspectiveCorrectionEnabled = perspectiveCorrectionEnabled;
		return copy;
	}

	public void setCulling(int culling) {
		this.culling = culling;
	}

	public int getCulling() {
		return culling;
	}

	public void setShading(int shading) {
		this.shading = shading;
	}

	public int getShading() {
		return shading;
	}

	public void setWinding(int winding) {
		this.winding = winding;
	}

	public int getWinding() {
		return winding;
	}

	public void setTwoSidedLightingEnable(boolean enable) {
		this.twoSidedLightingEnabled = enable;
	}

	public boolean isTwoSidedLightingEnabled() {
		return twoSidedLightingEnabled;
	}

	public void setLocalCameraLightingEnable(boolean enable) {
		this.localCameraLightingEnabled = enable;
	}

	public boolean isLocalCameraLightingEnabled() {
		return localCameraLightingEnabled;
	}

	public void setPerspectiveCorrectionEnable(boolean enable) {
		this.perspectiveCorrectionEnabled = enable;
	}

	public boolean isPerspectiveCorrectionEnabled() {
		return perspectiveCorrectionEnabled;
	}

	void setupGL(GL10 gl) {
		// Setup culling
		if (culling == CULL_NONE)
			gl.glDisable(GL10.GL_CULL_FACE);
		else {
			gl.glCullFace((culling == CULL_BACK) ? GL10.GL_BACK : GL10.GL_FRONT);
			gl.glEnable(GL10.GL_CULL_FACE);
		}

		// Setup two sided lighting
		gl.glLightModelf(GL10.GL_LIGHT_MODEL_TWO_SIDE, twoSidedLightingEnabled ? GL10.GL_TRUE : GL10.GL_FALSE);

		// Setup shading
		gl.glShadeModel((shading == SHADE_FLAT) ? GL10.GL_FLAT : GL10.GL_SMOOTH);

		// Setup winding
		gl.glFrontFace((winding == WINDING_CW) ? GL10.GL_CW : GL10.GL_CCW);

		// Setup perspective correction
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, perspectiveCorrectionEnabled ? GL10.GL_NICEST : GL10.GL_FASTEST);
	}

	int getLightTarget() {
		if (isTwoSidedLightingEnabled())
			return GL10.GL_FRONT_AND_BACK;
		else
			return GL10.GL_FRONT;
	}
}
