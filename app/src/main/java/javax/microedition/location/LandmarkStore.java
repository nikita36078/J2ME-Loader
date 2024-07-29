/*
 * Copyright 2023 Arman Jussupgaliyev
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
package javax.microedition.location;

import java.io.IOException;
import java.util.Enumeration;

public class LandmarkStore {
	public static LandmarkStore getInstance(String paramString) {
		return null;
	}

	public static void createLandmarkStore(String paramString) throws IOException, LandmarkException {
	}

	public static void deleteLandmarkStore(String paramString) throws IOException, LandmarkException {
	}

	public static String[] listLandmarkStores() throws IOException {
		return null;
	}

	public void addLandmark(Landmark paramLandmark, String paramString) throws IOException {
	}

	public Enumeration getLandmarks(String paramString1, String paramString2) throws IOException {
		return null;
	}

	public Enumeration getLandmarks() throws IOException {
		return null;
	}

	public Enumeration getLandmarks(String paramString, double paramDouble1, double paramDouble2, double paramDouble3,
			double paramDouble4) throws IOException {
		return null;
	}

	public void removeLandmarkFromCategory(Landmark paramLandmark, String paramString) throws IOException {
	}

	public void updateLandmark(Landmark paramLandmark) throws LandmarkException, IOException {
	}

	public void deleteLandmark(Landmark paramLandmark) throws LandmarkException, IOException {
	}

	public Enumeration getCategories() {
		return null;
	}

	public void addCategory(String paramString) throws IOException, LandmarkException {
	}

	public void deleteCategory(String paramString) throws IOException, LandmarkException {
	}

	private LandmarkStore(String paramString) {
	}
}
