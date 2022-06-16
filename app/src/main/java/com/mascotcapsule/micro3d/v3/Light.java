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

package com.mascotcapsule.micro3d.v3;

@SuppressWarnings("unused")
public class Light {

	private Vector3D mDirVector;
	private int mDirIntensity;
	private int mAmbIntensity;

	public Light() {
		mDirVector = new Vector3D(0, 0, 4096);
		mDirIntensity = 4096;
		mAmbIntensity = 0;
	}

	public Light(Vector3D dir, int dirIntensity, int ambIntensity) {
		if (dir == null) {
			throw new NullPointerException();
		}
		mDirVector = dir;
		mDirIntensity = dirIntensity;
		mAmbIntensity = ambIntensity;
	}

	Light(Light src) {
		mDirVector = new Vector3D(src.mDirVector);
		mDirIntensity = src.mDirIntensity;
		mAmbIntensity = src.mAmbIntensity;
	}

	@Deprecated
	public final int getDirIntensity() {
		return mDirIntensity;
	}

	@Deprecated
	public final void setDirIntensity(int p) {
		mDirIntensity = p;
	}

	public final int getParallelLightIntensity() {
		return mDirIntensity;
	}

	public final void setParallelLightIntensity(int p) {
		mDirIntensity = p;
	}

	@Deprecated
	public final int getAmbIntensity() {
		return mAmbIntensity;
	}

	@Deprecated
	public final void setAmbIntensity(int p) {
		mAmbIntensity = p;
	}

	public final int getAmbientIntensity() {
		return mAmbIntensity;
	}

	public final void setAmbientIntensity(int p) {
		mAmbIntensity = p;
	}

	@Deprecated
	public Vector3D getDirection() {
		return mDirVector;
	}

	@Deprecated
	public final void setDirection(Vector3D v) {
		if (v == null) {
			throw new NullPointerException();
		}
		mDirVector = v;
	}

	public final Vector3D getParallelLightDirection() {
		return mDirVector;
	}

	public final void setParallelLightDirection(Vector3D v) {
		if (v == null) {
			throw new NullPointerException();
		}
		mDirVector = v;
	}

	void set(Light src) {
		mDirVector.set(src.mDirVector);
		mDirIntensity = src.mDirIntensity;
		mAmbIntensity = src.mAmbIntensity;
	}
}
