/*
 *  Copyright 2020 Yury Kharchenko
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.mascotcapsule.micro3d.v3;

import static com.mascotcapsule.micro3d.v3.Utils.IDENTITY_AFFINE;

import android.util.SparseIntArray;

class Action {
	final int keyframes;
	final Bone[] boneActions;
	final float[] matrices;
	SparseIntArray dynamic;

	Action(int keyframes, int numBones) {
		this.keyframes = keyframes;
		this.boneActions = new Bone[numBones];
		this.matrices = new float[numBones * 12];
	}

	static final class Bone {
		private final int type;
		private final int mtxOffset;
		final float[] matrix;
		RollAnim roll;
		Animation rotate;
		Animation scale;
		Animation translate;
		private int frame = -1;

		Bone(int type, int mtxOffset, float[] matrix) {
			this.type = type;
			this.mtxOffset = mtxOffset;
			this.matrix = matrix;
		}

		void setFrame(int frame) {
			if (this.frame == frame) return;
			this.frame = frame;
			float kgf = frame / 65536f;
			final float[] m = matrix;
			switch (type) {
				case 2: {
					System.arraycopy(IDENTITY_AFFINE, 0, m, mtxOffset, 12);
					float[] arr = new float[3];

					// translate
					translate.get(kgf, arr);
					m[mtxOffset +  3] = arr[0];
					m[mtxOffset +  7] = arr[1];
					m[mtxOffset + 11] = arr[2];

					// rotate
					rotate.get(kgf, arr);
					rotate(m, arr[0], arr[1], arr[2]);

					// roll
					final float r = roll.get(kgf);
					roll(m, r);

					// scale
					scale.get(kgf, arr);
					float x = arr[0];
					float y = arr[1];
					float z = arr[2];
					m[mtxOffset     ] *= x;
					m[mtxOffset +  1] *= y;
					m[mtxOffset +  2] *= z;
					m[mtxOffset +  4] *= x;
					m[mtxOffset +  5] *= y;
					m[mtxOffset +  6] *= z;
					m[mtxOffset +  8] *= x;
					m[mtxOffset +  9] *= y;
					m[mtxOffset + 10] *= z;
					break;
				}
				case 3: {
					System.arraycopy(IDENTITY_AFFINE, 0, m, mtxOffset, 12);
					float[] arr = translate.values[0].clone();

					// translate (for all frames)
					m[mtxOffset +  3] = arr[0];
					m[mtxOffset +  7] = arr[1];
					m[mtxOffset + 11] = arr[2];

					// rotate
					rotate.get(kgf, arr);
					rotate(m, arr[0], arr[1], arr[2]);

					// roll (for all frames)
					final float r = roll.values[0];
					roll(m, r);
					break;
				}
				case 4: {
					System.arraycopy(IDENTITY_AFFINE, 0, m, mtxOffset, 12);
					float[] arr = new float[3];

					// rotate
					rotate.get(kgf, arr);
					rotate(m, arr[0], arr[1], arr[2]);

					// roll
					final float r = roll.get(kgf);
					roll(m, r);
					break;
				}
				case 5: {
					System.arraycopy(IDENTITY_AFFINE, 0, m, mtxOffset, 12);
					float[] arr = new float[3];

					// rotate
					rotate.get(kgf, arr);
					rotate(m, arr[0], arr[1], arr[2]);
					break;
				}
				case 6: {
					System.arraycopy(IDENTITY_AFFINE, 0, m, mtxOffset, 12);
					float[] arr = new float[3];

					// translate
					translate.get(kgf, arr);
					m[mtxOffset +  3] = arr[0];
					m[mtxOffset +  7] = arr[1];
					m[mtxOffset + 11] = arr[2];

					// rotate
					rotate.get(kgf, arr);
					rotate(m, arr[0], arr[1], arr[2]);

					// roll
					final float r = roll.get(kgf);
					roll(m, r);
					break;
				}
			}
		}

		/**
		 * Rotate matrix to new z-axis
		 *
		 * @param m destination matrix
		 * @param x X coord of new z-axis
		 * @param y Y coord of new z-axis
		 * @param z Y coord of new z-axis
		 */
		private void rotate(float[] m, float x, float y, float z) {
			if (x == 0.0f && y == 0.0f) {
				if (z < 0.0f) {// reverse (rotate 180 degrees around x-axis)
					m[mtxOffset + 5] = -1.0f;
					m[mtxOffset + 10] = -1.0f;
				} // else identity (no rotate)
				return;
			}
			// normalize direction vector
			float rld = 1.0f / (float) Math.sqrt(x * x + y * y + z * z);
			x *= rld;
			y *= rld;
			z *= rld;

			// compute rotate axis R = Z x Z' (x means "cross product")
			float rx = -y; // 0*z - 1*y
			float ry = x;  // 1*x - 0*z
			// rz = 0.0f   // 0*y - 0*x (inlined)

			// and normalize R
			float rls = 1.0f / (float) Math.sqrt(rx * rx + ry * ry);
			rx *= rls;
			ry *= rls;

			// cos = z (inlined)
			// compute sin from cos
			float sin = (float) Math.sqrt(1.0f - z * z);
			if (1.0f == rx && 0.0f == ry) {
				m[mtxOffset +  5] = z;
				m[mtxOffset +  6] = -sin;
				m[mtxOffset +  9] = sin;
				m[mtxOffset + 10] = z;
			} else if (0.0f == rx && 1.0f == ry) {
				m[mtxOffset] = z;
				m[mtxOffset +  2] = sin;
				m[mtxOffset +  8] = -sin;
				m[mtxOffset + 10] = z;
			} else {
				float nc = 1.0f - z;
				float xy = rx * ry;
				float xs = rx * sin;
				float ys = ry * sin;
				m[mtxOffset] = rx * rx * nc + z;
				m[mtxOffset +  1] = xy * nc;
				m[mtxOffset +  2] = ys;
				m[mtxOffset +  4] = xy * nc;
				m[mtxOffset +  5] = ry * ry * nc + z;
				m[mtxOffset +  6] = -xs;
				m[mtxOffset +  8] = -ys;
				m[mtxOffset +  9] = xs;
				m[mtxOffset + 10] = z;
			}
		}

		/**
		 * @param m     dest matrix
		 * @param angle rotate angle in radians
		 */
		private void roll(float[] m, float angle) {
			if (angle == 0.0f)
				return;
			float s = (float) Math.sin(angle);
			float c = (float) Math.cos(angle);
			float m00 = m[mtxOffset];
			float m10 = m[mtxOffset + 4];
			float m20 = m[mtxOffset + 8];
			float m01 = m[mtxOffset + 1];
			float m11 = m[mtxOffset + 5];
			float m21 = m[mtxOffset + 9];
			m[mtxOffset    ] = m00 * c + m01 * s;
			m[mtxOffset + 4] = m10 * c + m11 * s;
			m[mtxOffset + 8] = m20 * c + m21 * s;
			m[mtxOffset + 1] = m01 * c - m00 * s;
			m[mtxOffset + 5] = m11 * c - m10 * s;
			m[mtxOffset + 9] = m21 * c - m20 * s;
		}
	}

	static final class Animation {
		private final int[] keys;
		final float[][] values;

		Animation(int count) {
			keys = new int[count];
			values = new float[count][3];
		}

		void set(int idx, int kf, float x, float y, float z) {
			keys[idx] = kf;
			values[idx][0] = x;
			values[idx][1] = y;
			values[idx][2] = z;
		}

		void get(float kgf, float[] arr) {
			final int max = keys.length - 1;
			if (kgf >= keys[max]) {
				float[] value = values[max];
				arr[0] = value[0];
				arr[1] = value[1];
				arr[2] = value[2];
				return;
			}
			for (int i = max - 1; i >= 0; i--) {
				final int prevKey = keys[i];
				if (prevKey > kgf) {
					continue;
				}
				final float[] prevVal = values[i];
				float x = prevVal[0];
				float y = prevVal[1];
				float z = prevVal[2];
				if (prevKey == kgf) {
					arr[0] = x;
					arr[1] = y;
					arr[2] = z;
					return;
				}
				int nextKey = keys[i + 1];
				float[] nextValue = values[i + 1];
				float delta = (kgf - prevKey) / (nextKey - prevKey);
				arr[0] = x + (nextValue[0] - x) * delta;
				arr[1] = y + (nextValue[1] - y) * delta;
				arr[2] = z + (nextValue[2] - z) * delta;
				return;
			}
		}
	}

	static final class RollAnim {
		private final int[] keys;
		final float[] values;

		RollAnim(int count) {
			keys = new int[count];
			values = new float[count];
		}

		void set(int idx, int kf, float v) {
			keys[idx] = kf;
			values[idx] = v;
		}

		float get(float kgf) {
			final int max = keys.length - 1;
			if (kgf >= keys[max]) {
				return values[max];
			}
			for (int i = max - 1; i >= 0; i--) {
				final int key = keys[i];
				if (key > kgf) {
					continue;
				}
				float value = values[i];
				if (key == kgf) {
					return value;
				}
				int nextKey = keys[i + 1];
				float nextValue = values[i + 1];
				return value + (nextValue - value) / (nextKey - key) * (kgf - key);
			}
			return 0;
		}
	}
}
