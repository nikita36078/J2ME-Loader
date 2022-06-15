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

public class Figure extends Object3D {

	public static Figure createFigure(byte[] data, int offset, int length)
			throws java.io.IOException {
		return null;
	}

	public static Figure createFigure(java.lang.String name) throws java.io.IOException {
		return null;
	}

	public int getActionIndex() {
		return 0;
	}

	public ActionTable getActionTable() {
		return null;
	}

	public int getFrameIndex() {
		return 0;
	}

	public int getNumberOfPatterns() {
		return 0;
	}

	public int getPattern() {
		return 0;
	}

	public void setActionTable(ActionTable actionTable) {
	}

	public void setPattern(int pattern) {
	}

	public void setPosture(int actionIndex, int frameIndex) {
	}
}
