package javax.microedition.m3g;

import javax.microedition.khronos.opengles.GL10;

public class Material extends Object3D {
	public static final int AMBIENT = 1024;
	public static final int DIFFUSE = 2048;
	public static final int EMISSIVE = 4096;
	public static final int SPECULAR = 8192;

	private int ambientColor = 0x00333333;
	private int diffuseColor = 0xFFCCCCCC;
	private int emissiveColor = 0;
	private int specularColor = 0;
	private float shininess = 10.0f;
	private boolean isVertexColorTrackingEnabled = false;

	public Material() {
	}

	Object3D duplicateImpl() {
		Material copy = new Material();
		copy.ambientColor = ambientColor;
		copy.diffuseColor = diffuseColor;
		copy.emissiveColor = emissiveColor;
		copy.specularColor = specularColor;
		copy.shininess = shininess;
		copy.isVertexColorTrackingEnabled = isVertexColorTrackingEnabled;
		return copy;
	}

	public void setColor(int target, int color) {
		if ((target & AMBIENT) != 0)
			this.ambientColor = color;
		if ((target & DIFFUSE) != 0)
			this.diffuseColor = color;
		if ((target & EMISSIVE) != 0)
			this.emissiveColor = color;
		if ((target & SPECULAR) != 0)
			this.specularColor = color;
	}

	public int getColor(int target) {
		if (target == AMBIENT)
			return ambientColor;
		else if (target == DIFFUSE)
			return diffuseColor;
		else if (target == EMISSIVE)
			return emissiveColor;
		else if (target == SPECULAR)
			return specularColor;
		throw new IllegalArgumentException("Invalid color target");
	}

	public void setShininess(float shininess) {
		this.shininess = shininess;
	}

	public float getShininess() {
		return shininess;
	}

	public void setVertexColorTrackingEnable(boolean isVertexColorTrackingEnabled) {
		this.isVertexColorTrackingEnabled = isVertexColorTrackingEnabled;
	}

	public boolean isVertexColorTrackingEnabled() {
		return isVertexColorTrackingEnabled;
	}

	void setupGL(GL10 gl, int lightTarget) {
		gl.glEnable(GL10.GL_LIGHTING);
		gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_EMISSION, Color.intToFloatArray(emissiveColor), 0);
		gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT, Color.intToFloatArray(ambientColor), 0);
		gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_DIFFUSE, Color.intToFloatArray(diffuseColor), 0);
		gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR, Color.intToFloatArray(specularColor), 0);
		gl.glMaterialf(GL10.GL_FRONT_AND_BACK, GL10.GL_SHININESS, shininess);
	}

	boolean isCompatible(AnimationTrack track) {
		switch (track.getTargetProperty()) {
			case AnimationTrack.ALPHA:
			case AnimationTrack.AMBIENT_COLOR:
			case AnimationTrack.DIFFUSE_COLOR:
			case AnimationTrack.EMISSIVE_COLOR:
			case AnimationTrack.SHININESS:
			case AnimationTrack.SPECULAR_COLOR:
				return true;
			default:
				return super.isCompatible(track);
		}
	}
}
