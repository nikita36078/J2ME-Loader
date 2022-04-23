/*
 * Copyright 2022 Nikita Shakarun
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

package javax.microedition.lcdui;

import android.content.Context;
import android.os.Build;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.camera.view.PreviewView;

import javax.microedition.media.CameraController;
import javax.microedition.util.ContextHolder;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class VideoItem extends Item {
	private PreviewView view;
	private final CameraController controller;

	public VideoItem(CameraController controller) {
		this.controller = controller;
	}

	@Override
	protected View getItemContentView() {
		if (view == null) {
			Context context = ContextHolder.getAppContext();
			view = new PreviewView(context);
			controller.setUp(view);
		}
		return view;
	}

	@Override
	protected void clearItemContentView() {
		view = null;
	}
}
