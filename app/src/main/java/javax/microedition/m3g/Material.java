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
	private float shininess = 0.0f;
	private boolean isVertexColorTrackingEnabled = false;

	public Material() {
	}

	@Override
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

	@Override
	void updateProperty(int property, float[] value) {
		switch (property) {
			case AnimationTrack.ALPHA:
				diffuseColor = (diffuseColor | 0xFF000000) & (ColConv.alpha1f(value[0]) << 24);
				break;
			case AnimationTrack.AMBIENT_COLOR:
				ambientColor = ColConv.color3f(value[0], value[1], value[2]);
				break;
			case AnimationTrack.DIFFUSE_COLOR:
				diffuseColor = (diffuseColor | 0x00FFFFFF) & (ColConv.color3f(value[0], value[1], value[2]));
				break;
			case AnimationTrack.EMISSIVE_COLOR:
				emissiveColor = (ColConv.color3f(value[0], value[1], value[2]) & 0x00FFFFFF);
				break;
			case AnimationTrack.SHININESS:
				shininess = Math.max(0.f, Math.min(128.f, value[0]));
				break;
			case AnimationTrack.SPECULAR_COLOR:
				specularColor = ColConv.color3f(value[0], value[1], value[2]);
				break;
			default:
				super.updateProperty(property, value);
		}
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
		switch (target) {
			case AMBIENT:
				return ambientColor;
			case DIFFUSE:
				return diffuseColor;
			case EMISSIVE:
				return emissiveColor;
			case SPECULAR:
				return specularColor;
			default:
				throw new IllegalArgumentException("Invalid color target");
		}
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

	void setupGL(GL10 gl) {
		if (isVertexColorTrackingEnabled)
			gl.glEnable(GL10.GL_COLOR_MATERIAL);
		else {
			gl.glDisable(GL10.GL_COLOR_MATERIAL);

			gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT, Color.intToFloatArray(ambientColor), 0);
			gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_DIFFUSE, Color.intToFloatArray(diffuseColor), 0);
		}
		gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_EMISSION, Color.intToFloatArray(emissiveColor), 0);
		gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR, Color.intToFloatArray(specularColor), 0);
		gl.glMaterialf(GL10.GL_FRONT_AND_BACK, GL10.GL_SHININESS, shininess);

		gl.glEnable(GL10.GL_LIGHTING);
	}

	@Override
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
