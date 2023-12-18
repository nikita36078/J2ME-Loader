/*
 * Copyright 2020 Yury Kharchenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.woesss.j2me.micro3d;

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
		private final float[] matrix;
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
			if (this.frame == frame) {
				return;
			}
			this.frame = frame;
			float kgf = frame / 65536f;
			switch (type) {
				case 2: {
					float[] arr = new float[3];

					// translate
					translate.get(kgf, arr);
					matrix[mtxOffset +  3] = arr[0];
					matrix[mtxOffset +  7] = arr[1];
					matrix[mtxOffset + 11] = arr[2];

					// rotate
					rotate.get(kgf, arr);
					rotate(arr[0], arr[1], arr[2]);

					// roll
					final float r = roll.get(kgf);
					roll(r);

					// scale
					scale.get(kgf, arr);
					float x = arr[0];
					float y = arr[1];
					float z = arr[2];
					matrix[mtxOffset     ] *= x;
					matrix[mtxOffset +  1] *= y;
					matrix[mtxOffset +  2] *= z;
					matrix[mtxOffset +  4] *= x;
					matrix[mtxOffset +  5] *= y;
					matrix[mtxOffset +  6] *= z;
					matrix[mtxOffset +  8] *= x;
					matrix[mtxOffset +  9] *= y;
					matrix[mtxOffset + 10] *= z;
					break;
				}
				case 3: {
					float[] arr = translate.values[0].clone();

					// translate (for all frames)
					matrix[mtxOffset +  3] = arr[0];
					matrix[mtxOffset +  7] = arr[1];
					matrix[mtxOffset + 11] = arr[2];

					// rotate
					rotate.get(kgf, arr);
					rotate(arr[0], arr[1], arr[2]);

					// roll (for all frames)
					final float r = roll.values[0];
					roll(r);
					break;
				}
				case 4: {
					float[] arr = new float[3];

					// rotate
					rotate.get(kgf, arr);
					rotate(arr[0], arr[1], arr[2]);

					// roll
					final float r = roll.get(kgf);
					roll(r);
					break;
				}
				case 5: {
					float[] arr = new float[3];

					// rotate
					rotate.get(kgf, arr);
					rotate(arr[0], arr[1], arr[2]);
					break;
				}
				case 6: {
					float[] arr = new float[3];

					// translate
					translate.get(kgf, arr);
					matrix[mtxOffset +  3] = arr[0];
					matrix[mtxOffset +  7] = arr[1];
					matrix[mtxOffset + 11] = arr[2];

					// rotate
					rotate.get(kgf, arr);
					rotate(arr[0], arr[1], arr[2]);

					// roll
					final float r = roll.get(kgf);
					roll(r);
					break;
				}
			}
		}

		/**
		 * Rotate matrix to new z-axis
		 *
		 * @param x X coord of new z-axis
		 * @param y Y coord of new z-axis
		 * @param z Y coord of new z-axis
		 */
		private void rotate(float x, float y, float z) {
			// normalize direction vector
			float rld = 1.0f / (float) Math.sqrt(x * x + y * y + z * z);
			x *= rld;
			y *= rld;
			z *= rld;

			float xx = x * x;
			float yy = y * y;
			if (xx > 0.0f || yy > 0.0f) {
				float a = (1.0f - z) / (yy + xx);
				float b = a * -(x * y);
				matrix[mtxOffset     ] = z + yy * a;
				matrix[mtxOffset +  1] = b;
				matrix[mtxOffset +  2] = x;
				matrix[mtxOffset +  4] = b;
				matrix[mtxOffset +  5] = z + xx * a;
				matrix[mtxOffset +  6] = y;
				matrix[mtxOffset +  8] = -x;
				matrix[mtxOffset +  9] = -y;
			} else {
				matrix[mtxOffset     ] = 1.0f;
				matrix[mtxOffset +  1] = 0.0f;
				matrix[mtxOffset +  2] = 0.0f;
				matrix[mtxOffset +  4] = 0.0f;
				matrix[mtxOffset +  5] = z;
				matrix[mtxOffset +  6] = 0.0f;
				matrix[mtxOffset +  8] = 0.0f;
				matrix[mtxOffset +  9] = 0.0f;
			}
			matrix[mtxOffset + 10] = z;
		}

		/**
		 * @param angle rotate angle in radians
		 */
		private void roll(float angle) {
			float s = (float) Math.sin(angle);
			float c = (float) Math.cos(angle);

			float m00 = matrix[mtxOffset];
			float m01 = matrix[mtxOffset + 1];
			float m10 = matrix[mtxOffset + 4];
			float m11 = matrix[mtxOffset + 5];
			float m20 = matrix[mtxOffset + 8];
			float m21 = matrix[mtxOffset + 9];

			matrix[mtxOffset    ] = m00 * c + m01 * s;
			matrix[mtxOffset + 1] = m01 * c - m00 * s;
			matrix[mtxOffset + 4] = m10 * c + m11 * s;
			matrix[mtxOffset + 5] = m11 * c - m10 * s;
			matrix[mtxOffset + 8] = m20 * c + m21 * s;
			matrix[mtxOffset + 9] = m21 * c - m20 * s;
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
