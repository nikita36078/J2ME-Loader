package javax.microedition.m3g;

public abstract class Transformable extends Object3D {
	QVec4 orientation = new QVec4();
	private Transform transform = new Transform();
	float sx = 1, sy = 1, sz = 1;
	float tx = 0, ty = 0, tz = 0;
	Matrix matrix;

	void duplicate(Transformable copy) {
		copy.tx = tx; copy.ty = ty; copy.tz = tz;
		copy.sx = sx; copy.sy = sy; copy.sz = sz;
		copy.orientation = new QVec4(orientation.x, orientation.y, orientation.z, orientation.w);
		copy.transform = new Transform(transform);
		if (matrix != null) {
			copy.matrix = new Matrix();
			copy.matrix.copyMatrix(matrix);
		}
	}

	void invalidateTransformable() {
		if (!(this instanceof Texture2D))
			if (((Node)this).parent != null && (((Node)this).hasRenderables || ((Node)this).hasBones))
				((Node)this).parent.invalidateNode(new boolean[]{false,false});
	}

	@Override
	void updateProperty(int property, float[] value) {
		boolean invalidate = true;
		switch (property) {
			case AnimationTrack.ORIENTATION:
				orientation.setQuat(value);
				orientation.normalizeQuat();
				break;
			case AnimationTrack.TRANSLATION:
				tx = value[0];
				ty = value[1];
				tz = value[2];
				break;
			case AnimationTrack.SCALE:
				sx = value[0];
				sy = value[1];
				sz = value[2];
				break;
			default:
				super.updateProperty(property, value);
				invalidate = false;
		}
		if (invalidate)
			invalidateTransformable();
	}

	boolean getInverseCompositeTransform(Matrix transform) {
		transform.scalingMatrix(sx, sy, sz);
		if (matrix != null)
			transform.mulMatrix(matrix);

		boolean ok = transform.invertMatrix();
		if (!ok)
			return false;

		QVec4 temp = new QVec4(orientation);
		temp.w = -temp.w;
		transform.rotateMatrixQuat(temp);

		transform.translateMatrix(-tx, -ty, -tz);
		return true;
	}

	public void getCompositeTransform(Matrix mtx) {
		mtx.identityMatrix();
		mtx.translateMatrix(tx, ty, tz);
		mtx.rotateMatrixQuat(orientation);
		mtx.scaleMatrix(sx, sy, sz);
		if (matrix != null)
			mtx.mulMatrix(matrix);
	}

	public void getCompositeTransform(Transform transform) {
		if (transform == null)
			throw new NullPointerException("transform can not be null");

		transform.mtx.identityMatrix();
		transform.mtx.translateMatrix(tx, ty, tz);
		transform.mtx.rotateMatrixQuat(orientation);
		transform.mtx.scaleMatrix(sx, sy, sz);

		if (matrix != null)
			transform.mtx.mulMatrix(matrix);
	}

	public void setOrientation(float angle, float ax, float ay, float az) {
		if (angle != 0 && ax == 0 && ay == 0 && az == 0)
			throw new IllegalArgumentException();

		orientation.setAngleAxis(angle, ax, ay, az);
		invalidateTransformable();
	}

	public void getTransform(Transform transform) {
		if (transform == null)
			throw new NullPointerException("transform can not be null");

		if (this.matrix != null)
			transform.mtx.copyMatrix(matrix);
		else
			transform.mtx.identityMatrix();
	}

	public void postRotate(float angle, float ax, float ay, float az) {
		QVec4 rotate = new QVec4();

		if (angle != 0 && ax == 0 && ay == 0 && az == 0)
			throw new IllegalArgumentException();

		rotate.setAngleAxis(angle, ax, ay, az);
		orientation.mulQuat(rotate);
		invalidateTransformable();
	}

	public void preRotate(float angle, float ax, float ay, float az) {
		QVec4 rotate = new QVec4();

		if (angle != 0 && ax == 0 && ay == 0 && az == 0)
			throw new IllegalArgumentException();

		rotate.setAngleAxis(angle, ax, ay, az);
		rotate.mulQuat(orientation);
		orientation.assign(rotate);
	}

	public void getOrientation(float[] angleAxis) {
		Vector3 vec = new Vector3(angleAxis[1], angleAxis[2], angleAxis[3]);
		angleAxis[0] = orientation.getAngleAxis(vec);
		angleAxis[1] = vec.x;
		angleAxis[2] = vec.y;
		angleAxis[3] = vec.z;
	}

	public void setScale(float sx, float sy, float sz) {
		this.sx = sx;
		this.sy = sy;
		this.sz = sz;
		invalidateTransformable();
	}

	public void scale(float sx, float sy, float sz) {
		this.sx *= sx;
		this.sy *= sy;
		this.sz *= sz;
		invalidateTransformable();
	}

	public void getScale(float[] scale) {
		scale[0] = this.sx;
		scale[1] = this.sy;
		scale[2] = this.sz;
	}

	public void setTranslation(float tx, float ty, float tz) {
		this.tx = tx;
		this.ty = ty;
		this.tz = tz;
		invalidateTransformable();
	}

	public void translate(float tx, float ty, float tz) {
		this.tx += tx;
		this.ty += ty;
		this.tz += tz;
		invalidateTransformable();
	}

	public void getTranslation(float[] translation) {
		translation[0] = tx;
		translation[1] = ty;
		translation[2] = tz;
	}

	public void setTransform(Transform transform) {
		if (transform == null)
			throw new NullPointerException("transform can not be null");

		this.transform.set(transform);
		if (transform != null) {
			if (!(this instanceof Texture2D) && !transform.mtx.isWUnity())
				throw new IllegalArgumentException();
			if (matrix == null)
				matrix = new Matrix();
			matrix.copyMatrix(transform.mtx);
		} else if (matrix != null)
			matrix.identityMatrix();

		invalidateTransformable();
	}
	
	boolean isCompatible(AnimationTrack track) {
		switch (track.getTargetProperty()) {
		case AnimationTrack.ORIENTATION:
		case AnimationTrack.SCALE:
		case AnimationTrack.TRANSLATION:
		    return true;
		default:
		    return super.isCompatible(track);
		}
	}
	
}
