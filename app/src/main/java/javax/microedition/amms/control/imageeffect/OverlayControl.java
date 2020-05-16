/*
 * Copyright 2020 Nikita Shakarun
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

package javax.microedition.amms.control.imageeffect;

import javax.microedition.amms.control.EffectControl;

public interface OverlayControl extends EffectControl {
	public int insertImage(Object image, int x, int y, int order) throws IllegalArgumentException;

	public int insertImage(Object image, int x, int y, int order, int transparentColor) throws IllegalArgumentException;

	public void removeImage(Object image);

	public Object getImage(int order);

	public int numberOfImages();

	public void clear();
}
