package javax.microedition.m3g;

import javax.microedition.khronos.opengles.GL10;

public class Transform {
	Matrix mtx;

	public Transform() {
		mtx = new Matrix();

		mtx.identityMatrix();
	}

	public Transform(Transform transform) {
		mtx = new Matrix();

		mtx.copyMatrix(transform.mtx);
	}

	public void get(float[] matrix) {
		if (matrix == null)
			throw new NullPointerException("matrix can not be null");
		if (matrix.length < 16)
			throw new IllegalArgumentException("matrix must be of length 16");

		mtx.getMatrixRows(matrix);
	}

	public void invert() {
		if (!mtx.invertMatrix()) {
			throw new ArithmeticException("matrix can not be inverted");
		}
	}

	public void set(float[] matrix) {
		if (matrix == null)
			throw new NullPointerException("matrix can not be null");
		if (matrix.length < 16)
			throw new IllegalArgumentException("matrix must be of length 16");

		mtx.setMatrixRows(matrix);
	}

	public void set(Transform transform) {
		if (transform == null)
			throw new NullPointerException("transform can not be null");

		mtx.copyMatrix(transform.mtx);
	}

	public void setIdentity() {
		mtx.identityMatrix();
	}

	public void transform(float[] vectors) {
		if (vectors == null)
			throw new NullPointerException("vectors can not be null");
		if ((vectors.length % 4) != 0)
			throw new IllegalArgumentException("Number of elements in vector array must be a multiple of 4");

		QVec4 vec = new QVec4();

		for (int i = 0; i < vectors.length; i += 4) {
			vec.setVec4(vectors[i], vectors[i + 1], vectors[i + 2], vectors[i + 3]);
			mtx.transformVec4(vec);
			vectors[i] = vec.x;
			vectors[i + 1] = vec.y;
			vectors[i + 2] = vec.z;
			vectors[i + 3] = vec.w;
		}
	}

	public void transform(VertexArray in, float[] out, boolean W) {
		if (in == null)
			throw new NullPointerException("in can not be null");
		if (out == null)
			throw new NullPointerException("out can not be null");
		if (out.length < in.getVertexCount() * 4)
			throw new IllegalArgumentException("Number of elements in out array must be at least vertexCount*4");

		int cc = in.getComponentCount();
		int vc = in.getVertexCount();
		QVec4 vec = new QVec4();

		if (in.getComponentType() == 1) {
			byte[] values = new byte[vc * cc];
			in.get(0, vc, values);
			for (int i = 0, j = 0; i < vc * cc; i += cc, j += 4) {
				vec.x = values[i];
				vec.y = (cc >= 2 ? (float) values[i + 1] : 0.0f);
				vec.z = (cc >= 3 ? (float) values[i + 2] : 0.0f);
				vec.w = (cc >= 4 ? (float) values[i + 3] : (W ? 1 : 0));

				mtx.transformVec4(vec);

				out[j] = vec.x;
				out[j + 1] = vec.y;
				out[j + 2] = vec.z;
				out[j + 3] = vec.w;
			}
		} else {
			short[] values = new short[vc * cc];
			in.get(0, vc, values);
			for (int i = 0, j = 0; i < vc * cc; i += cc, j += 4) {
				vec.x = values[i];
				vec.y = (cc >= 2 ? (float) values[i + 1] : 0.0f);
				vec.z = (cc >= 3 ? (float) values[i + 2] : 0.0f);
				vec.w = (cc >= 4 ? (float) values[i + 3] : (W ? 1 : 0));

				mtx.transformVec4(vec);

				out[j] = vec.x;
				out[j + 1] = vec.y;
				out[j + 2] = vec.z;
				out[j + 3] = vec.w;
			}
		}
	}

	public void transpose() {
		Matrix tpos = new Matrix();
		tpos.matrixTranspose(mtx);
		mtx.copyMatrix(tpos);
	}

	public void postMultiply(Transform transform) {
		Matrix temp = new Matrix();
		temp.matrixProduct(this.mtx, transform.mtx);
		mtx.copyMatrix(temp);
	}

	public void postRotate(float angle, float ax, float ay, float az) {
		if (ax == 0 && ay == 0 && az == 0 && angle != 0)
			throw new IllegalArgumentException();
		mtx.postRotateMatrix(angle, ax, ay, az);
	}

	public void postRotateQuat(float qx, float qy, float qz, float qw) {
		QVec4 quat = new QVec4(qx, qy, qz, qw);
		quat.normalizeQuat();
		mtx.postRotateMatrixQuat(quat);
	}

	public void postScale(float sx, float sy, float sz) {
		mtx.postScaleMatrix(sx, sy, sz);
	}

	public void postTranslate(float tx, float ty, float tz) {
		mtx.postTranslateMatrix(tx, ty, tz);
	}

	void setGL(GL10 gl) {
		float[] cols = new float[16];
		mtx.getMatrixColumns(cols);
		gl.glLoadMatrixf(cols, 0);
	}

	public void multGL(GL10 gl) {
		float[] cols = new float[16];
		mtx.getMatrixColumns(cols);
		gl.glMultMatrixf(cols, 0);
	}

	private void transpose(Matrix other) {
		other.matrixTranspose(mtx);
	}

	public String toString() {
		String ret = "{";
		for (int i = 0; i < 16; ++i) {
			if ((i % 4) == 0 && i > 0)
				ret += "\n ";
			ret += mtx.elem[i] + ", ";
		}
		return ret + "}";
	}
}
