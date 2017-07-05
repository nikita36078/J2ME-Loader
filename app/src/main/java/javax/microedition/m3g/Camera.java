package javax.microedition.m3g;

public class Camera extends Node {
	public static final int GENERIC = 48;
	public static final int PARALLEL = 49;
	public static final int PERSPECTIVE = 50;

	private int projectionType = GENERIC;
	private float fovy;
	private float aspectRatio;
	private float near;
	private float far;
	private Transform transform = new Transform();
	private boolean zeroViewVolume;

	public Camera() {
	}

	Object3D duplicateImpl() {
		Camera copy = new Camera();
		super.duplicate((Node) copy);
		copy.projectionType = projectionType;
		copy.fovy = fovy;
		copy.aspectRatio = aspectRatio;
		copy.near = near;
		copy.far = far;
		copy.transform = transform;
		return copy;
	}

	@Override
	void updateProperty(int property, float[] value) {
		switch (property) {
			case AnimationTrack.FAR_DISTANCE:
				far = (projectionType == PERSPECTIVE) ? Math.max(0.f, value[0]) : value[0];
				break;
			case AnimationTrack.FIELD_OF_VIEW:
				fovy = (projectionType == PERSPECTIVE) ? Math.max(0.f, Math.min(180.f, value[0])) : Math.max(0, value[0]);
				break;
			case AnimationTrack.NEAR_DISTANCE:
				near = (projectionType == PERSPECTIVE) ? Math.max(0.f, value[0]) : value[0];
				break;
			default:
				super.updateProperty(property, value);
		}

		validateProjectionMatrix();
	}

	public void setParallel(float fovy, float aspectRatio, float near, float far) {
		this.projectionType = PARALLEL;
		this.fovy = fovy;
		this.aspectRatio = aspectRatio;
		this.near = near;
		this.far = far;

		validateProjectionMatrix();
	}

	public void setPerspective(float fovy, float aspectRatio, float near, float far) {
		this.projectionType = PERSPECTIVE;
		this.fovy = fovy;
		this.aspectRatio = aspectRatio;
		this.near = near;
		this.far = far;

		validateProjectionMatrix();
	}

	private void validateProjectionMatrix() {
		if (projectionType != GENERIC) {
			float[] m = new float[16];

			float clipNear = near;
			float clipFar = far;

			if (projectionType == PERSPECTIVE) {
				float height = (float) Math.tan(Constants.DEG2RAD * 0.5f * fovy);

				m[0] = (float) (1.0d / (aspectRatio * height));
				m[1] = m[2] = m[3] = 0.f;

				m[4] = 0.f;
				m[5] = (float) (1.0d / height);
				m[6] = m[7] = 0.f;

				m[8] = m[9] = 0.f;
				m[10] = (-clipNear - clipFar) / (clipFar - clipNear);
				m[11] = -1.f;

				m[12] = m[13] = 0.f;
				m[14] = (((-2.f * clipFar) * clipNear) / (clipFar - clipNear));
				m[15] = 0.f;
			} else if (projectionType == PARALLEL) {
				float height = fovy;

				m[0] = (float) (2.d / (aspectRatio * height));
				m[1] = m[2] = m[3] = 0.f;

				m[4] = 0.f;
				m[5] = (float) (2.d / height);
				m[6] = m[7] = 0;

				m[8] = m[9] = 0;
				m[10] = (float) (-2.f / (clipFar - clipNear));
				m[11] = 0.f;

				m[12] = m[13] = 0.f;
				m[14] = (-clipNear - clipFar) / (clipFar - clipNear);
				m[15] = 1.f;
			}
			transform.mtx.setMatrixColumns(m);
		}

		Matrix im = new Matrix();
		if (im.matrixInverse(transform.mtx))
			zeroViewVolume = false;
		else
			zeroViewVolume = true;
	}

	public void setGeneric(Transform transform) {
		this.projectionType = GENERIC;
		//this.transform.set(transform);
		this.transform.mtx.copyMatrix(transform.mtx);

		validateProjectionMatrix();
	}

	public int getProjection(Transform transform) {
		if (transform != null)
			transform.mtx.copyMatrix(this.transform.mtx);
		return projectionType;
	}

	public int getProjection(float[] params) {
		if (params != null) {
			if (params.length < 4)
				throw new IllegalArgumentException("Params");

			params[0] = fovy;
			params[1] = aspectRatio;
			params[2] = near;
			params[3] = far;
		}
		return projectionType;
	}

	boolean isCompatible(AnimationTrack track) {
		switch (track.getTargetProperty()) {
			case AnimationTrack.FAR_DISTANCE:
			case AnimationTrack.FIELD_OF_VIEW:
			case AnimationTrack.NEAR_DISTANCE:
				return true;
			default:
				return super.isCompatible(track);
		}
	}
}
