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

import ru.woesss.j2me.micro3d.FigureImpl;

public class Figure extends Object3D {
	final FigureImpl impl;
	ActionTable actionTable;
	int actionIndex;
	int frameIndex;
	int pattern;

	private Figure(byte[] data, int offset, int length) throws IOException {
		impl = new FigureImpl(data, offset, length);
	}

	private Figure(String name) throws IOException {
		impl = new FigureImpl(name);
	}

	public static Figure createFigure(byte[] data, int offset, int length) throws IOException {
		return new Figure(data, offset, length);
	}

	public static Figure createFigure(String name) throws IOException {
		return new Figure(name);
	}

	public int getActionIndex() {
		return actionIndex;
	}

	public ActionTable getActionTable() {
		return actionTable;
	}

	public int getFrameIndex() {
		return frameIndex;
	}

	public int getNumberOfPatterns() {
		return impl.getNumPattern();
	}

	public int getPattern() {
		return pattern;
	}

	public void setActionTable(ActionTable actionTable) {
		if (actionTable == null) {
			actionIndex = 0;
			frameIndex = 0;
		}
		this.actionTable = actionTable;
	}

	public void setPattern(int pattern) {
		this.pattern = pattern;
	}

	public void setPosture(int actionIndex, int frameIndex) {
		if (actionTable == null || actionIndex < 0 || actionIndex >= actionTable.getNumberOfActions()) {
			throw new IllegalArgumentException();
		}
		this.actionIndex = actionIndex;
		this.frameIndex = frameIndex;
		pattern = actionTable.impl.getPattern(actionIndex, frameIndex, pattern);
	}
}
