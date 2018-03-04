/*
 * Copyright 2018 Nikita Shakarun
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

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.util.ContextHolder;

public class Texture {
	protected boolean isModel;

	public Texture(byte[] b, boolean isForModel) {
		if (b == null) {
			throw new RuntimeException();
		}
		this.isModel = isForModel;
	}

	public Texture(String name, boolean isForModel) throws IOException {
		if (name == null) {
			throw new NullPointerException();
		}
		InputStream is = ContextHolder.getResourceAsStream(null, name);
		if (is == null) {
			throw new IOException();
		}
		this.isModel = isForModel;
	}

	public final void dispose() {
	}
}
