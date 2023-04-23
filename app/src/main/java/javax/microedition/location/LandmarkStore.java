package javax.microedition.location;

import java.io.IOException;
import java.util.Enumeration;

public class LandmarkStore {
	public static LandmarkStore getInstance(String paramString) {
		LandmarkStore localLandmarkStore = null;
		return localLandmarkStore;
	}

	public static void createLandmarkStore(String paramString) throws IOException, LandmarkException {
	}

	public static void deleteLandmarkStore(String paramString) throws IOException, LandmarkException {
	}

	public static String[] listLandmarkStores() throws IOException {
		String[] arrayOfString = null;
		return arrayOfString;
	}

	public void addLandmark(Landmark paramLandmark, String paramString) throws IOException {
	}

	public Enumeration getLandmarks(String paramString1, String paramString2) throws IOException {
		Enumeration localEnumeration = null;
		return localEnumeration;
	}

	public Enumeration getLandmarks() throws IOException {
		Enumeration localEnumeration = null;
		return localEnumeration;
	}

	public Enumeration getLandmarks(String paramString, double paramDouble1, double paramDouble2, double paramDouble3,
			double paramDouble4) throws IOException {
		Enumeration localEnumeration = null;
		return localEnumeration;
	}

	public void removeLandmarkFromCategory(Landmark paramLandmark, String paramString) throws IOException {
	}

	public void updateLandmark(Landmark paramLandmark) throws LandmarkException, IOException {
	}

	public void deleteLandmark(Landmark paramLandmark) throws LandmarkException, IOException {
	}

	public Enumeration getCategories() {
		Enumeration localEnumeration = null;
		return localEnumeration;
	}

	public void addCategory(String paramString) throws IOException, LandmarkException {
	}

	public void deleteCategory(String paramString) throws IOException, LandmarkException {
	}

	private LandmarkStore(String paramString) {
	}
}
