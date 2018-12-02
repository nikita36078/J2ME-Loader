package javax.microedition.m3g;

class Constants {
	public static final float EPSILON = 0.0000001f;
	public static final float DEG2RAD = 0.017453292519943295769236907684886f;
	public static final float RAD2DEG = 57.295779513082320876798154814105f;
}

class Tools {
	public static boolean isPowerOfTwo(int x) {
		return (x & (x - 1)) == 0;
	}
}

class Vector3 {
	public float x;
	public float y;
	public float z;

	public Vector3() {
		x = y = z = 0;
	}

	public Vector3(float[] vec) {
		if (vec == null)
			throw new NullPointerException();
		if (vec.length < 3)
			throw new IllegalArgumentException();

		x = vec[0];
		y = vec[1];
		z = vec[2];
	}

	public Vector3(float vx, float vy, float vz) {
		x = vx;
		y = vy;
		z = vz;
	}

	public Vector3(Vector3 vec) {
		x = vec.x;
		y = vec.y;
		z = vec.z;
	}

	public Vector3(QVec4 q) {
		x = q.x;
		y = q.y;
		z = q.z;
	}

	public void logQuat(QVec4 quat) {
		float sinTheta = (float) Math.sqrt(QVec4.norm3(quat));

		if (sinTheta > Constants.EPSILON) {
			float s = (float) (Math.atan2(sinTheta, quat.w) / sinTheta);
			x = s * quat.x;
			y = s * quat.y;
			z = s * quat.z;
		} else
			x = y = z = 0.0f;
	}

	public void assign(Vector3 other) {
		x = other.x;
		y = other.y;
		z = other.z;
	}

	public void setVec3(float val) {
		x = val;
		y = val;
		z = val;
	}

	public void setVec3(float[] vec) {
		if (vec == null)
			throw new NullPointerException();
		if (vec.length < 3)
			throw new IllegalArgumentException();

		x = vec[0];
		y = vec[1];
		z = vec[2];
	}

	public void setVec3(float vx, float vy, float vz) {
		x = vx;
		y = vy;
		z = vz;
	}

	public void logDiffQuat(QVec4 from, QVec4 to) {
		QVec4 temp = new QVec4();
		temp.x = -from.x;
		temp.y = -from.y;
		temp.z = -from.z;
		temp.w = from.w;
		temp.mulQuat(to);
		this.logQuat(temp);
	}

	public void addVec3(Vector3 other) {
		x += other.x;
		y += other.y;
		z += other.z;
	}

	public void subVec3(Vector3 other) {
		x -= other.x;
		y -= other.y;
		z -= other.z;
	}

	public static void scale3(float[] v, float s) {
		v[0] *= s;
		v[1] *= s;
		v[2] *= s;
	}

	public void scaleVec3(float s) {
		x *= s;
		y *= s;
		z *= s;
	}

	public static void lerp(int size, float[] vec, float s, float[] start, float[] end) {
		float sCompl = 1.f - s;
		for (int i = 0; i < size; i++)
			vec[i] = (sCompl * start[i]) + (s * end[i]);
	}

	public float lengthVec3() {
		return (float) Math.sqrt(x * x + y * y + z * z);
	}

	public static float norm3(float[] v) {
		return (v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
	}

	public void normalizeVec3() {
		float norm = norm3(new float[]{x, y, z});
		if (norm > Constants.EPSILON) {
			norm = (float) (1.0d / Math.sqrt(norm));
			scaleVec3(norm);
		} else {
			x = y = z = 0;
		}
	}

	public void cross(Vector3 a, Vector3 b) {
		x = a.y * b.z - a.z * b.y;
		y = a.z * b.x - a.x * b.z;
		z = a.x * b.y - a.y * b.x;
	}

	public static float dot3(Vector3 a, Vector3 b) {
		return a.x * b.x + a.y * b.y + a.z * b.z;
	}

	public static boolean intersectTriangle(Vector3 orig, Vector3 dir, Vector3 vert0, Vector3 vert1, Vector3 vert2, Vector3 tuv, int cullMode) {
		Vector3 edge1 = new Vector3();
		Vector3 edge2 = new Vector3();
		Vector3 tvec = new Vector3();
		Vector3 pvec = new Vector3();
		Vector3 qvec = new Vector3();

		edge1.assign(vert1);
		edge2.assign(vert2);
		edge1.subVec3(vert0);
		edge2.subVec3(vert0);

		pvec.cross(dir, edge2);
		float det = dot3(edge1, pvec);

		if (cullMode == 0 && det <= 0) return false;
		if (cullMode == 1 && det >= 0) return false;

		if (det > -Constants.EPSILON && det < Constants.EPSILON)
			return false;
		float inv_det = (float) (1.0d / det);

		tvec.assign(orig);
		tvec.subVec3(vert0);

		tuv.y = inv_det * dot3(tvec, pvec);
		if (tuv.y < 0.0f || tuv.y > 1.0f)
			return false;

		qvec.cross(tvec, edge1);

		tuv.z = inv_det * dot3(dir, qvec);
		if (tuv.z < 0.0f || (tuv.y + tuv.z) > 1.0f)
			return false;

		tuv.x = inv_det * dot3(edge2, qvec);

		return true;
	}
}

class QVec4 {
	public static final int[] Vec4_X_AXIS = new int[]{1, 0, 0, 0};
	public static final int[] Vec4_Y_AXIS = new int[]{0, 1, 0, 0};
	public static final int[] Vec4_Z_AXIS = new int[]{0, 0, 1, 0};
	public static final int[] Vec4_ORIGIN = new int[]{0, 0, 0, 1};

	public float x;
	public float y;
	public float z;
	public float w;

	public QVec4() {
		x = y = z = w = 0;
	}

	public QVec4(float[] q) {
		if (q == null)
			throw new NullPointerException();
		if (q.length < 4)
			throw new IllegalArgumentException();

		x = q[0];
		y = q[1];
		z = q[2];
		w = q[3];
	}

	public QVec4(QVec4 q) {
		x = q.x;
		y = q.y;
		z = q.z;
		w = q.w;
	}

	public QVec4(float qx, float qy, float qz, float qw) {
		x = qx;
		y = qy;
		z = qz;
		w = qw;
	}

	public void setQuat(float[] vec) {
		if (vec.length < 4)
			throw new IllegalArgumentException();

		x = vec[0];
		y = vec[1];
		z = vec[2];
		w = vec[3];
	}

	public void setVec4(float x, float y, float z, float w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}

	public void assign(QVec4 other) {
		x = other.x;
		y = other.y;
		z = other.z;
		w = other.w;
	}

	public void mulQuat(QVec4 other) {
		QVec4 q = new QVec4();
		q.assign(other);
		w = q.w * other.w - q.x * other.x - q.y * other.y - q.z * other.z;
		x = q.w * other.x + q.x * other.w + q.y * other.z - q.z * other.y;
		y = q.w * other.y - q.x * other.z + q.y * other.w + q.z * other.x;
		z = q.w * other.z + q.x * other.y - q.y * other.x + q.z * other.w;
	}

	public static float norm3(QVec4 quat) {
		return (quat.x * quat.x + quat.y * quat.y + quat.z * quat.z);
	}

	public void expQuat(Vector3 qExp) {
		float theta = (float) (Math.sqrt(qExp.x * qExp.x + qExp.y * qExp.y + qExp.z * qExp.z));

		if (theta > Constants.EPSILON) {
			float s = (float) (Math.sin(theta) * (1.0d / (double) theta));
			x = qExp.x * s;
			y = qExp.y * s;
			z = qExp.z * s;
			w = (float) Math.cos(theta);
		} else {
			x = y = z = 0.0f;
			w = 1.0f;
		}
	}

	public static float dot4(QVec4 a, QVec4 b) {
		return a.x * b.x + a.y * b.y + a.z * b.z + a.w * b.w;
	}

	public void slerpQuat(float s, QVec4 q0, QVec4 q1) {
		float s0, s1;
		float cosTheta = QVec4.dot4(q0, q1);
		float oneMinusS = 1.0f - s;

		if (cosTheta > (Constants.EPSILON - 1.0f)) {
			if (cosTheta < (1.0f - Constants.EPSILON)) {
				float theta = (float) Math.acos((double) cosTheta);
				float sinTheta = (float) Math.sin(theta);
				s0 = (float) (Math.sin(oneMinusS * theta) / sinTheta);
				s1 = (float) (Math.sin(s * theta) / sinTheta);
			} else {
				s0 = oneMinusS;
				s1 = s;
			}
			x = s0 * q0.x + s1 * q1.x;
			y = s0 * q0.y + s1 * q1.y;
			z = s0 * q0.z + s1 * q1.z;
			w = s0 * q0.w + s1 * q1.w;
		} else {
			x = -q0.y;
			y = q0.x;
			z = -q0.w;
			w = q0.z;

			s0 = (float) Math.sin(oneMinusS * (Math.PI / 2));
			s1 = (float) Math.sin(s * (Math.PI / 2));

			x = s0 * q0.x + s1 * x;
			y = s0 * q0.y + s1 * y;
			z = s0 * q0.z + s1 * z;
		}
	}

	public void scaleVec4(float s) {
		x *= s;
		y *= s;
		z *= s;
		w *= s;
	}

	public float norm4() {
		return (x * x + y * y + z * z + w * w);
	}

	public static float norm4(float[] v) {
		return v[0] * v[0] + v[1] * v[1] + v[2] * v[2] + v[3] * v[3];
	}

	public void identityQuat() {
		x = y = z = 0.0f;
		w = 1.0f;
	}

	public void normalizeQuat() {
		float norm = (x * x + y * y + z * z + w * w);

		if (norm > Constants.EPSILON) {
			norm = (float) (1.0d / Math.sqrt(norm));
			scaleVec4(norm);
		} else
			identityQuat();
	}

	public void setAngleAxis(float angle, float ax, float ay, float az) {
		setAngleAxisRad(angle * Constants.DEG2RAD, ax, ay, az);
	}

	public void setAngleAxisRad(float angleRad, float ax, float ay, float az) {
		if (angleRad != 0) {
			float halfAngle = angleRad / 2;
			float s = (float) Math.sin(halfAngle);

			float sqrNorm = ax * ax + ay * ay + az * az;
			if (sqrNorm < 0.995f || sqrNorm > 1.005f) {
				if (sqrNorm > Constants.EPSILON) {
					float ooNorm = (float) (1.0d / Math.sqrt(sqrNorm));
					ax *= ooNorm;
					ay *= ooNorm;
					az *= ooNorm;
				} else
					ax = ay = az = 0.0f;
			}

			x = ax * s;
			y = ay * s;
			z = az * s;
			w = (float) Math.cos(halfAngle);
		} else
			identityQuat();
	}

	public float getAngleAxis(Vector3 axis) {
		float x = this.x;
		float y = this.y;
		float z = this.z;

		float sinTheta = (float) (Math.sqrt(x * x + y * y + z * z));

		if (sinTheta > Constants.EPSILON) {
			float ooSinTheta = (float) (1.0d / sinTheta);
			axis.x = x * ooSinTheta;
			axis.y = y * ooSinTheta;
			axis.z = z * ooSinTheta;
		} else {
			axis.x = axis.y = 0.0f;
			axis.z = 1.0f;
		}
		return (float) (2.0f * Constants.RAD2DEG * Math.acos(this.w));
	}

	public void setQuatRotation(Vector3 from, Vector3 to) {
		float cosAngle = Vector3.dot3(from, to);

		if (cosAngle > (1.0f - Constants.EPSILON)) {
			identityQuat();
			return;
		} else if (cosAngle > (1.0e-3f - 1.0f)) {
			Vector3 axis = new Vector3();
			axis.cross(from, to);
			setAngleAxisRad((float) (Math.acos(cosAngle)), axis.x, axis.y, axis.z);
		} else {
			Vector3 axis = new Vector3();
			Vector3 temp = new Vector3();

			axis.x = axis.y = axis.z = 0.0f;
			if (Math.abs(from.z) < (1.0f - Constants.EPSILON))
				axis.z = 1.0f;
			else
				axis.y = 1.0f;

			float s = Vector3.dot3(axis, from);
			temp.assign(from);
			temp.scaleVec3(s);
			axis.subVec3(temp);

			setAngleAxis(180.f, axis.x, axis.y, axis.z);
		}
	}

	public void addVec4(QVec4 other) {
		x += other.x;
		y += other.y;
		z += other.z;
		w += other.w;
	}

	public void subVec4(QVec4 other) {
		x -= other.x;
		y -= other.y;
		z -= other.z;
		w -= other.w;
	}

	public static void scale4(float[] v, float s) {
		v[0] *= s;
		v[1] *= s;
		v[2] *= s;
		v[3] *= s;
	}

	public void normalizeVec4() {
		float norm = norm4();

		if (norm > Constants.EPSILON) {
			norm = (float) (1.0d / Math.sqrt(norm));
			scaleVec4(norm);
		} else {
			x = 0;
			y = 0;
			z = 0;
			w = 0;
		}
	}
}

class AABB {
	float[] min = new float[3];
	float[] max = new float[3];

	void transformAABB(Matrix mtx) {
		float[] boxMin = new float[3];
		float[] boxMax = new float[3];
		float[] newMin = new float[3];
		float[] newMax = new float[3];

		if (!mtx.complete)
			mtx.fillClassifiedMatrix();

		for (int i = 0; i < 3; i++) {
			boxMin[i] = min[i];
			boxMax[i] = max[i];
			newMin[i] = newMax[i] = mtx.elem[i + 12];
		}

		for (int row = 0; row < 3; row++)
			for (int col = 0; col < 3; col++) {
				float a = mtx.elem[row + col * 4] * boxMin[col];
				float b = mtx.elem[row + col * 4] * boxMax[col];

				if (a < b) {
					newMin[row] += a;
					newMax[row] += b;
				} else {
					newMin[row] += b;
					newMax[row] += a;
				}
			}

		for (int i = 0; i < 3; i++) {
			min[i] = newMin[i];
			max[i] = newMax[i];
		}
	}

	public static boolean intersectBox(Vector3 orig, Vector3 dir, AABB box) {
		float tnear = -3.402e+38f;
		float tfar = 3.402e+38f;
		float t1, t2, temp;

		if (dir.x != 0) {
			t1 = (box.min[0] - orig.x) / dir.x;
			t2 = (box.max[0] - orig.x) / dir.x;

			if (t1 > t2) {
				temp = t1;
				t1 = t2;
				t2 = temp;
			}

			if (t1 > tnear) tnear = t1;
			if (t2 < tfar) tfar = t2;

			if (tnear > tfar) return false;
			if (tfar < 0) return false;
		} else if (orig.x > box.max[0] || orig.x < box.min[0]) return false;

		if (dir.y != 0) {
			t1 = (box.min[1] - orig.y) / dir.y;
			t2 = (box.max[1] - orig.y) / dir.y;

			if (t1 > t2) {
				temp = t1;
				t1 = t2;
				t2 = temp;
			}

			if (t1 > tnear) tnear = t1;
			if (t2 < tfar) tfar = t2;

			if (tnear > tfar) return false;
			if (tfar < 0) return false;
		} else if (orig.y > box.max[1] || orig.y < box.min[1]) return false;

		if (dir.z != 0) {
			t1 = (box.min[2] - orig.z) / dir.z;
			t2 = (box.max[2] - orig.z) / dir.z;

			if (t1 > t2) {
				temp = t1;
				t1 = t2;
				t2 = temp;
			}

			if (t1 > tnear) tnear = t1;
			if (t2 < tfar) tfar = t2;

			if (tnear > tfar) return false;
			if (tfar < 0) return false;
		} else if (orig.z > box.max[2] || orig.y < box.min[2]) return false;

		return true;
	}

	public void assign(AABB other) {
		System.arraycopy(other.min, 0, min, 0, 3);
		System.arraycopy(other.max, 0, max, 0, 3);
	}

	public void fitAABB(AABB a, AABB b) {
		if (a != null && b != null)
			for (int i = 0; i < 3; i++) {
				min[i] = Math.min(a.min[i], b.min[i]);
				max[i] = Math.max(a.max[i], b.max[i]);
			}
		else if (a != null) {
			assign(a);
		} else if (b != null) {
			assign(b);
		}
	}
}

class Matrix {
	public static final byte ZERO = 0;
	public static final byte ONE = 1;
	public static final byte MINUS_ONE = 2;
	public static final byte ANY = 3;

	public static final int IDENTITY = 0x40100401;
	public static final int FRUSTUM = 0x30BF0C03;
	public static final int PERSPECTIVE = 0x30B00C03;
	public static final int ORTHO = 0x7F300C03;
	public static final int PARALLEL = 0x70300C03;
	public static final int SCALING_ROTATION = 0x403F3F3F;
	public static final int SCALING = 0x40300C03;
	public static final int TRANSLATION = 0x7F100401;
	public static final int X_ROTATION = 0x403C3C01;
	public static final int Y_ROTATION = 0x40330433;
	public static final int Z_ROTATION = 0x40100F0F;
	public static final int W_UNITY = 0x7F3F3F3F;
	public static final int GENERIC = 0xFFFFFFFF;

	public static final int TRANSLATION_PART = 0x3F000000;
	public static final int SCALE_PART = 0x00300C03;
	public static final int SCALE_ROTATION_PART = 0x003F3F3F;

	public float[] elem = new float[16];
	public byte[] mask = new byte[16];
	public boolean classified = false;
	public boolean complete = false;

	public Matrix() {
		for (int i = 0; i < 16; i++) {
			elem[i] = 0.0f;
			mask[i] = 0;
		}
	}

	public static byte[] getByteMask(int mask) {
		//System.out.println("getByteMask");
		byte[] maskArr = new byte[16];
		//System.out.println(Integer.toHexString(mask));
		for (int i = 0; i < 16; i++, mask >>>= 2) {
			maskArr[i] = (byte) (mask & 3);
			//System.out.println(Integer.toHexString((int)maskArr[i]));
		}
		return maskArr;
	}

	public static int getIntMask(byte[] mask) {
		//System.out.println("getIntMask");
		int ret = 0;
		for (int i = 0; i < 16; i++, ret <<= 2) {
			ret |= mask[i];
			//System.out.println(Integer.toHexString((int)mask[i]));
		}
		//System.out.println(Integer.toHexString(ret));
		return ret;
	}

	public boolean compareMask(byte[] mask) {
		for (int i = 0; i < 16; i++)
			if (this.mask[i] != mask[i]) return false;
		return true;
	}

	public void copyMatrix(Matrix src) {
		System.arraycopy(src.elem, 0, elem, 0, 16);
		System.arraycopy(src.mask, 0, mask, 0, 16);
		classified = src.classified;
		complete = src.complete;
	}

	public byte elementClass(float x) {
		if (x == 0)
			return ZERO;
		else if (x == 1)
			return ONE;
		else if (x == -1)
			return MINUS_ONE;
		return ANY;
	}

	public void classify() {
		for (int i = 0; i < 16; i++)
			mask[i] = elementClass(elem[i]);
		classified = true;
	}

	public void classifyAs(byte[] mask) {
		this.mask = mask;
		classified = true;
		complete = false;
	}

	public void subClassify() {
		for (int i = 0; i < 16; i++)
			if (mask[i] == ANY)
				mask[i] = elementClass(elem[i]);
	}

	public void fillClassifiedMatrix() {
		for (int i = 0; i < 16; i++)
			switch (mask[i]) {
				case ZERO:
					elem[i] = 0.0f;
					break;
				case ONE:
					elem[i] = 1.0f;
					break;
				case MINUS_ONE:
					elem[i] = -1.0f;
					break;
				default:
					break;
			}
		complete = true;
	}

	public float classifiedMadd(byte amask, float pa, byte bmask, float pb, float c) {
		if (amask == ZERO || bmask == ZERO)
			return c;

		switch (amask) {
			case ANY:
				if (bmask == ONE) return pa + c;
				if (bmask == MINUS_ONE) return c - pa;
				return pa * pb + c;
			case ONE:
				if (bmask == ONE) return 1.f + c;
				if (bmask == MINUS_ONE) return c - 1.f;
				return pb + c;
			case MINUS_ONE:
				if (bmask == ONE) return c - 1.f;
				if (bmask == MINUS_ONE) return 1.f + c;
				return c - pb;
			default:
				return 0.0f;
		}
	}

	public void genericMatrixProduct(Matrix left, Matrix right) {
		if (!left.complete)
			left.fillClassifiedMatrix();
		if (!right.complete)
			right.fillClassifiedMatrix();

		for (int row = 0; row < 4; row++)
			for (int col = 0; col < 4; col++) {
				float a = 0;
				for (int k = 0; k < 4; k++)
					a += left.elem[row + k * 4] * right.elem[k + col * 4];
				this.elem[row + col * 4] = a;
			}
		this.complete = true;
		this.classified = false;
	}

	public boolean isWUnity() {
		if (classified)
			return (mask[3] == ZERO) &&
					(mask[7] == ZERO) &&
					(mask[11] == ZERO) &&
					(mask[15] == ONE);
		else
			return (elem[3] == 0.0f) &&
					(elem[7] == 0.0f) &&
					(elem[11] == 0.0f) &&
					(elem[15] == 1.0f);
	}

	public void getMatrixColumn(int col, QVec4 dst) {
		if (!complete)
			fillClassifiedMatrix();
		dst.x = elem[col * 4];
		dst.y = elem[1 + col * 4];
		dst.z = elem[2 + col * 4];
		dst.w = elem[3 + col * 4];
	}

	public void getMatrixColumns(float[] dst) {
		if (!complete)
			fillClassifiedMatrix();
		System.arraycopy(elem, 0, dst, 0, 16);
	}

	public void getMatrixRow(int row, QVec4 dst) {
		if (!complete)
			fillClassifiedMatrix();
		dst.x = elem[row];
		dst.y = elem[row + 4];
		dst.z = elem[row + 8];
		dst.w = elem[row + 12];
	}

	public void getMatrixRows(float[] dst) {
		if (!complete)
			fillClassifiedMatrix();

		for (int i = 0, row = 0; row < 4; row++) {
			dst[i++] = elem[row];
			dst[i++] = elem[4 + row];
			dst[i++] = elem[8 + row];
			dst[i++] = elem[12 + row];
		}
	}

	public void setMatrixColumns(float[] src) {
		System.arraycopy(src, 0, elem, 0, elem.length);
		classified = false;
		complete = true;
	}

	public void setMatrixRows(float[] src) {
		for (int i = 0, row = 0; row < 4; row++) {
			elem[row] = src[i++];
			elem[row + 4] = src[i++];
			elem[row + 8] = src[i++];
			elem[row + 12] = src[i++];
		}
		classified = false;
		complete = true;
	}

	public void identityMatrix() {
		classifyAs(getByteMask(IDENTITY));
	}

	public boolean invertMatrix() {
		float[] tmp = new float[12];
		float[] src = new float[16];
		int msk = getIntMask(mask);

		if (!classified)
			classify();

		if (msk == IDENTITY)
			return true;

		if (!complete)
			fillClassifiedMatrix();

		if ((msk | (0x3F << 24)) == TRANSLATION) {
			elem[12] = -elem[12];
			elem[13] = -elem[13];
			elem[14] = -elem[14];
			mask = getByteMask(TRANSLATION);
			return true;
		}

		if ((msk | 0x300C03) == SCALING) {
			msk = getIntMask(mask);
			if ((msk & 3) == 0 || (msk & (3 << 10)) == 0 || (msk & (3 << 20)) == 0)
				return false;

			elem[0] = (float) (1.0d / elem[0]);
			elem[5] = (float) (1.0d / elem[5]);
			elem[10] = (float) (1.0d / elem[10]);
			return true;
		}

		float[] matrix = elem;

		for (int i = 0; i < 4; i++) {
			src[i] = matrix[i * 4];
			src[i + 4] = matrix[i * 4 + 1];
			src[i + 8] = matrix[i * 4 + 2];
			src[i + 12] = matrix[i * 4 + 3];
		}

		/* calculate pairs for first 8 elements (cofactors) */
		tmp[0] = src[10] * src[15];
		tmp[1] = src[11] * src[14];
		tmp[2] = src[9] * src[15];
		tmp[3] = src[11] * src[13];
		tmp[4] = src[9] * src[14];
		tmp[5] = src[10] * src[13];
		tmp[6] = src[8] * src[15];
		tmp[7] = src[11] * src[12];
		tmp[8] = src[8] * src[14];
		tmp[9] = src[10] * src[12];
		tmp[10] = src[8] * src[13];
		tmp[11] = src[9] * src[12];

		/* calculate first 8 elements (cofactors) */
		matrix[0] = tmp[0] * src[5] + tmp[3] * src[6] + tmp[4] * src[7];
		matrix[0] -= tmp[1] * src[5] + tmp[2] * src[6] + tmp[5] * src[7];
		matrix[1] = tmp[1] * src[4] + tmp[6] * src[6] + tmp[9] * src[7];
		matrix[1] -= tmp[0] * src[4] + tmp[7] * src[6] + tmp[8] * src[7];
		matrix[2] = tmp[2] * src[4] + tmp[7] * src[5] + tmp[10] * src[7];
		matrix[2] -= tmp[3] * src[4] + tmp[6] * src[5] + tmp[11] * src[7];
		matrix[3] = tmp[5] * src[4] + tmp[8] * src[5] + tmp[11] * src[6];
		matrix[3] -= tmp[4] * src[4] + tmp[9] * src[5] + tmp[10] * src[6];
		matrix[4] = tmp[1] * src[1] + tmp[2] * src[2] + tmp[5] * src[3];
		matrix[4] -= tmp[0] * src[1] + tmp[3] * src[2] + tmp[4] * src[3];
		matrix[5] = tmp[0] * src[0] + tmp[7] * src[2] + tmp[8] * src[3];
		matrix[5] -= tmp[1] * src[0] + tmp[6] * src[2] + tmp[9] * src[3];
		matrix[6] = tmp[3] * src[0] + tmp[6] * src[1] + tmp[11] * src[3];
		matrix[6] -= tmp[2] * src[0] + tmp[7] * src[1] + tmp[10] * src[3];
		matrix[7] = tmp[4] * src[0] + tmp[9] * src[1] + tmp[10] * src[2];
		matrix[7] -= tmp[5] * src[0] + tmp[8] * src[1] + tmp[11] * src[2];

		/* calculate pairs for second 8 elements (cofactors) */
		tmp[0] = src[2] * src[7];
		tmp[1] = src[3] * src[6];
		tmp[2] = src[1] * src[7];
		tmp[3] = src[3] * src[5];
		tmp[4] = src[1] * src[6];
		tmp[5] = src[2] * src[5];
		tmp[6] = src[0] * src[7];
		tmp[7] = src[3] * src[4];
		tmp[8] = src[0] * src[6];
		tmp[9] = src[2] * src[4];
		tmp[10] = src[0] * src[5];
		tmp[11] = src[1] * src[4];

		/* calculate second 8 elements (cofactors) */
		matrix[8] = tmp[0] * src[13] + tmp[3] * src[14] + tmp[4] * src[15];
		matrix[8] -= tmp[1] * src[13] + tmp[2] * src[14] + tmp[5] * src[15];
		matrix[9] = tmp[1] * src[12] + tmp[6] * src[14] + tmp[9] * src[15];
		matrix[9] -= tmp[0] * src[12] + tmp[7] * src[14] + tmp[8] * src[15];
		matrix[10] = tmp[2] * src[12] + tmp[7] * src[13] + tmp[10] * src[15];
		matrix[10] -= tmp[3] * src[12] + tmp[6] * src[13] + tmp[11] * src[15];
		matrix[11] = tmp[5] * src[12] + tmp[8] * src[13] + tmp[11] * src[14];
		matrix[11] -= tmp[4] * src[12] + tmp[9] * src[13] + tmp[10] * src[14];
		matrix[12] = tmp[2] * src[10] + tmp[5] * src[11] + tmp[1] * src[9];
		matrix[12] -= tmp[4] * src[11] + tmp[0] * src[9] + tmp[3] * src[10];
		matrix[13] = tmp[8] * src[11] + tmp[0] * src[8] + tmp[7] * src[10];
		matrix[13] -= tmp[6] * src[10] + tmp[9] * src[11] + tmp[1] * src[8];
		matrix[14] = tmp[6] * src[9] + tmp[11] * src[11] + tmp[3] * src[8];
		matrix[14] -= tmp[10] * src[11] + tmp[2] * src[8] + tmp[7] * src[9];
		matrix[15] = tmp[10] * src[10] + tmp[4] * src[8] + tmp[9] * src[9];
		matrix[15] -= tmp[8] * src[9] + tmp[11] * src[10] + tmp[5] * src[8];

		/* calculate determinant */
		float det = src[0] * matrix[0] + src[1] * matrix[1] + src[2] * matrix[2] + src[3] * matrix[3];

		if (det == 0.0f)
			return false;

		det = (float) (1.0d / det);

		for (int i = 0; i < 16; i++)
			matrix[i] *= det;

		classified = false;
		return true;
	}

	public boolean matrixInverse(Matrix other) {
		if (!other.classified)
			other.classify();

		copyMatrix(other);
		return invertMatrix();
	}

	public void matrixTranspose(Matrix other) {
		if (!other.complete)
			other.fillClassifiedMatrix();

		for (int i = 0; i < 4; i++) {
			elem[i] = other.elem[i * 4];
			elem[i + 4] = other.elem[i * 4 + 1];
			elem[i + 8] = other.elem[i * 4 + 2];
			elem[i + 12] = other.elem[i * 4 + 3];
		}
		classified = false;
		complete = true;
	}

	public boolean inverseTranspose(Matrix other) {
		Matrix tmp = new Matrix();
		if (!tmp.matrixInverse(other))
			return false;
		matrixTranspose(tmp);
		return true;
	}

	public void transformVec4(QVec4 vec) {
		if (!classified)
			classify();

		int type = getIntMask(mask);

		if (type == IDENTITY)
			return;
		else {
			int n = isWUnity() ? 3 : 4;

			if (!complete)
				fillClassifiedMatrix();

			QVec4 v = new QVec4();
			v.assign(vec);

			for (int i = 0; i < n; i++) {
				float d = v.x * elem[i];
				d += v.y * elem[i + 4];
				d += v.z * elem[i + 8];
				d += v.w * elem[i + 12];
				switch (i) {
					case 0:
						vec.x = d;
						break;
					case 1:
						vec.y = d;
						break;
					case 2:
						vec.z = d;
						break;
					case 3:
						vec.w = d;
						break;
				}
			}
		}
	}


	public void matrixProduct(Matrix left, Matrix right) {
		if (!left.classified)
			left.classify();
		int lmask = getIntMask(left.mask);
		if (lmask == IDENTITY) {
			copyMatrix(right);
			return;
		}

		if (!right.classified)
			right.classify();
		int rmask = getIntMask(right.mask);
		if (rmask == IDENTITY) {
			copyMatrix(left);
			return;
		}

		if (left.isWUnity() && right.isWUnity()) {
			if ((lmask & ~TRANSLATION_PART) == IDENTITY) {
				if (lmask != TRANSLATION && !left.complete)
					left.fillClassifiedMatrix();
				if (rmask != TRANSLATION && !right.complete)
					right.fillClassifiedMatrix();

				copyMatrix(right);

				elem[12] += left.elem[12];
				elem[13] += left.elem[13];
				elem[14] += left.elem[14];

				mask = getByteMask((getIntMask(mask) | TRANSLATION_PART));
				return;
			}

			if ((rmask & ~TRANSLATION_PART) == IDENTITY) {
				if (lmask != TRANSLATION && !left.complete)
					left.fillClassifiedMatrix();
				if (rmask != TRANSLATION && !right.complete)
					right.fillClassifiedMatrix();

				copyMatrix(left);

				QVec4 tvec = new QVec4();
				right.getMatrixColumn(3, tvec);
				transformVec4(tvec);

				elem[12] = tvec.x;
				elem[13] = tvec.y;
				elem[14] = tvec.z;

				mask = getByteMask((getIntMask(mask) | TRANSLATION_PART));
				return;
			}
		}

		genericMatrixProduct(left, right);
	}

	public void scalingMatrix(float sx, float sy, float sz) {
		elem[0] = sx;
		elem[5] = sy;
		elem[10] = sz;
		classifyAs(getByteMask(SCALING));
		subClassify();
	}

	public void translationMatrix(float tx, float ty, float tz) {
		elem[12] = tx;
		elem[13] = ty;
		elem[14] = tz;
		classifyAs(getByteMask(TRANSLATION));
		subClassify();
	}

	public void postMultiplyMatrix(Matrix other) {
		Matrix temp = new Matrix();
		temp.copyMatrix(this);
		matrixProduct(temp, other);
	}

	public void preMultiplyMatrix(Matrix other) {
		Matrix temp = new Matrix();
		temp.copyMatrix(this);
		matrixProduct(other, temp);
	}

	public void quatMatrix(QVec4 quat) {
		float qx = quat.x;
		float qy = quat.y;
		float qz = quat.z;
		float qw = quat.w;

		if (qx == 0 && qy == 0 && qz == 0) {
			identityMatrix();
			return;
		}
		int type = SCALING_ROTATION;
		if (qz == 0 && qy == 0)
			type = X_ROTATION;
		else if (qz == 0 && qx == 0)
			type = Y_ROTATION;
		else if (qx == 0 && qy == 0)
			type = Z_ROTATION;

		classifyAs(getByteMask(type));

		float wx, wy, wz, xx, yy, yz, xy, xz, zz;

		xx = qx * qx;
		xy = qx * qy;
		xz = qx * qz;
		yy = qy * qy;
		yz = qy * qz;
		zz = qz * qz;
		wx = qw * qx;
		wy = qw * qy;
		wz = qw * qz;

		if (type != X_ROTATION) {
			elem[0] = 1.f - 2 * (yy + zz);
			elem[4] = 2 * (xy - wz);
			elem[8] = 2 * (xz + wy);
		}

		if (type != Y_ROTATION) {
			elem[1] = 2 * (xy + wz);
			elem[5] = 1.f - 2 * (xx + zz);
			elem[9] = 2 * (yz - wx);
		}

		if (type != Z_ROTATION) {
			elem[2] = 2 * (xz - wy);
			elem[6] = 2 * (yz + wx);
			elem[10] = 1.f - 2 * (xx + yy);
		}

		subClassify();
	}

	public void postRotateMatrixQuat(QVec4 quat) {
		Matrix temp = new Matrix();
		temp.quatMatrix(quat);
		postMultiplyMatrix(temp);
	}

	public void postRotateMatrix(float angle, float ax, float ay, float az) {
		QVec4 q = new QVec4();
		q.setAngleAxis(angle, ax, ay, az);
		postRotateMatrixQuat(q);
	}

	public void postScaleMatrix(float sx, float sy, float sz) {
		Matrix temp = new Matrix();
		temp.scalingMatrix(sx, sy, sz);
		postMultiplyMatrix(temp);
	}

	public void postTranslateMatrix(float tx, float ty, float tz) {
		Matrix temp = new Matrix();
		temp.translationMatrix(tx, ty, tz);
		postMultiplyMatrix(temp);
	}

	public void preRotateMatrixQuat(QVec4 quat) {
		Matrix temp = new Matrix();
		temp.quatMatrix(quat);
		preMultiplyMatrix(temp);
	}

	public void preRotateMatrix(float angle, float ax, float ay, float az) {
		QVec4 q = new QVec4();
		q.setAngleAxis(angle, ax, ay, az);
		preRotateMatrixQuat(q);
	}

	public void preScaleMatrix(float sx, float sy, float sz) {
		Matrix temp = new Matrix();
		temp.scalingMatrix(sx, sy, sz);
		preMultiplyMatrix(temp);
	}

	public void preTranslateMatrix(float tx, float ty, float tz) {
		Matrix temp = new Matrix();
		temp.translationMatrix(tx, ty, tz);
		preMultiplyMatrix(temp);
	}

	public void rotateMatrix(float angle, float ax, float ay, float az) {
		postRotateMatrix(angle, ax, ay, az);
	}

	public void rotateMatrixQuat(QVec4 quat) {
		postRotateMatrixQuat(quat);
	}

	public void translateMatrix(float tx, float ty, float tz) {
		postTranslateMatrix(tx, ty, tz);
	}

	public void scaleMatrix(float sx, float sy, float sz) {
		postScaleMatrix(sx, sy, sz);
	}

	public void mulMatrix(Matrix other) {
		postMultiplyMatrix(other);
	}

	public void leftMulMatrix(Matrix other) {
		preMultiplyMatrix(other);
	}

	public void rightMulMatrix(Matrix other) {
		postMultiplyMatrix(other);
	}
}

class ColConv {
	public static final int ALPHA_MASK = 0xFF000000;

	public static int alpha1f(float a) {
		return (int) (a * 255.f);
	}

	public static int color3f(float r, float g, float b) {
		return (((int) (r * 255.f)) << 16) | (((int) (g * 255.f)) << 8) | (int) (b * 255.f) | ALPHA_MASK;
	}

	public static int color4f(float r, float g, float b, float a) {
		return (((int) (r * 255.f)) << 16) | (((int) (g * 255.f)) << 8) | (int) (b * 255.f) | (((int) (a * 255.f)) << 24);
	}

	public static float[] floatColor(int argb, float intensity) {
		float[] rgba = new float[4];
		float oneOver255 = (float) (1.0001d / 255.0d);

		rgba[0] = (float) ((argb >>> 16) & 0xFF);
		rgba[1] = (float) ((argb >>> 8) & 0xFF);
		rgba[2] = (float) ((argb) & 0xFF);
		rgba[3] = (float) ((argb >>> 24) & 0xFF);

		QVec4.scale4(rgba, oneOver255 * intensity);
		return rgba;
	}
}

class Color {
	public float a = 0.0f;
	public float r = 0.0f;
	public float g = 0.0f;
	public float b = 0.0f;

	public Color(float a, float r, float g, float b) {
		this.a = a;
		this.r = r;
		this.g = g;
		this.b = b;
	}

	public Color(int color) {
		this.a = ((float) ((color >> 24) & 0xFF)) / 255.0f;
		this.r = ((float) ((color >> 16) & 0xFF)) / 255.0f;
		this.g = ((float) ((color >> 8) & 0xFF)) / 255.0f;
		this.b = ((float) (color & 0xFF)) / 255.0f;
	}

	public float[] toRGBAArray() {
		return new float[]{r, g, b, a};
	}

	public static float[] intToFloatArray(int color) {
		Color c = new Color(color);
		return c.toRGBAArray();
	}
}

