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

import com.mascotcapsule.micro3d.v3.figure.FigureImpl;
import com.mascotcapsule.micro3d.v3.figure.Mesh;
import com.mascotcapsule.micro3d.v3.figure.Renderable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.util.ContextHolder;

@SuppressWarnings({"unused", "WeakerAccess"})
public class Figure implements Renderable {
	public FigureImpl figure;
	private Texture[] myTextureArray;
	private Texture myTextureNow;

	public Figure(byte[] b) {
		if (b == null) {
			throw new NullPointerException();
		}
		try {
			figure = new FigureImpl(new ByteArrayInputStream(b));
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Loading error");
			throw new RuntimeException(e);
		}
	}

	public Figure(String name) throws IOException {
		if (name == null) {
			throw new NullPointerException();
		}
		InputStream is = ContextHolder.getResourceAsStream(null, name);
		if (is == null) {
			throw new IOException();
		}
		try {
			figure = new FigureImpl(is);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Loading error");
			throw new RuntimeException(e);
		}
	}

	public final void dispose() {
	}

	public final void setPosture(ActionTable act, int action, int frame) {
		if (act == null) {
			throw new NullPointerException();
		} else if (action < 0 || action >= act.getNumActions()) {
			throw new IllegalArgumentException();
		}
	}

	public final Texture getTexture() {
		return this.myTextureNow;
	}

	public final void setTexture(Texture t) {
		if (t == null) {
			throw new NullPointerException();
		} else if (t.isModel) {
			this.myTextureArray = new Texture[1];
			this.myTextureArray[0] = t;
			this.myTextureNow = t;
		} else {
			throw new IllegalArgumentException();
		}
	}

	public final void setTexture(Texture[] t) {
		if (t == null || t.length == 0) {
			throw new NullPointerException();
		} else {
			int i = 0;
			while (i < t.length) {
				if (t[i] == null) {
					throw new NullPointerException();
				} else if (t[i].isModel) {
					i++;
				} else {
					throw new IllegalArgumentException();
				}
			}
			this.myTextureArray = t;
			this.myTextureNow = null;
		}
	}

	public final int getNumTextures() {
		if (this.myTextureArray == null) {
			return 0;
		} else {
			return this.myTextureArray.length;
		}
	}

	public final void selectTexture(int idx) {
		if (idx < 0 || idx >= getNumTextures()) {
			throw new IllegalArgumentException();
		} else {
			this.myTextureNow = this.myTextureArray[idx];
		}
	}

	public final int getNumPattern() {
		return figure.getNumPattern();
	}

	public final void setPattern(int idx) {
	}

	@Override
	public ArrayList<Mesh> getMeshesT() {
		return figure.meshesT;
	}

	@Override
	public ArrayList<Mesh> getMeshesF() {
		return figure.meshesF;
	}

	@Override
	public FloatBuffer getVboPolyT() {
		return figure.vboPolyT;
	}

	@Override
	public FloatBuffer getVboPolyF() {
		return figure.vboPolyF;
	}

	@Override
	public int getNumPolyT() {
		return figure.numPolyT;
	}

	@Override
	public int getNumPolyF() {
		return figure.numPolyF;
	}

	@Override
	public Texture getTextureById(int idx) {
		return myTextureArray[idx];
	}
}
