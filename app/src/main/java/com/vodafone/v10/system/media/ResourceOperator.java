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

package com.vodafone.v10.system.media;

public interface ResourceOperator {
	int getIndexOfResource(int id);

	int getResourceCount();

	int getResourceID(int index);

	String getResourceName(int id);

	String[] getResourceNames();

	int getResourceType();

	void setResource(MediaPlayer player, int index);

	void setResourceByID(MediaPlayer player, int id);

	void setResourceByTitle(MediaPlayer player, String name);
}