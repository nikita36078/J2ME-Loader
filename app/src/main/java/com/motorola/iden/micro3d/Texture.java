/*
 *  Copyright 2022 Yury Kharchenko
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

package com.motorola.iden.micro3d;

import java.io.IOException;

import ru.woesss.j2me.micro3d.TextureImpl;

public class Texture {
	TextureImpl impl;

	private boolean isSphereTexture;

	public Texture(byte[] data, int offset, int length, boolean sphereTexture) throws IOException {
		impl = new TextureImpl(data, offset, length);
		isSphereTexture = sphereTexture;
	}

	private Texture(String name, boolean sphereTexture) throws IOException {
		impl = new TextureImpl(name);
		isSphereTexture = sphereTexture;
	}

	Texture(boolean mutable) {
		if (mutable) {
			impl = new TextureImpl();
		}
	}

	private static Texture createTexture(byte[] data, int offset, int length, boolean sphereTexture)
			throws IOException {
		return new Texture(data, offset, length, sphereTexture);
	}

	public static Texture createTexture(String name, boolean sphereTexture)
			throws IOException {
		return new Texture(name, sphereTexture);
	}

	public boolean isSphereTexture() {
		return isSphereTexture;
	}

	boolean isMutable() {
		return impl != null && impl.isMutable();
	}
}
