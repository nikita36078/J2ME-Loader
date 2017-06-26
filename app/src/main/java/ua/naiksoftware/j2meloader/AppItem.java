package ua.naiksoftware.j2meloader;

import java.io.Serializable;

/**
 * @author Naik
 */
public class AppItem implements Serializable {

	private String imagePath;
	private String title;
	private String author;
	private String version;
	private String path;

	public AppItem(String imagePath_, String title_, String author_, String version_) {
		imagePath = imagePath_;
		title = title_;
		author = author_;
		version = version_;
	}

	public void setPath(String p) {
		path = p;
		setImagePath();
	}

	public String getPath() {
		return path;
	}

	public void setTitle(String title_) {
		title = title_;
	}

	public String getTitle() {
		return title;
	}

	public void setImagePath() {
		String resString = "/res";
		imagePath = imagePath.replace(" ", "");
		if (!imagePath.contains("/")) {
			resString += "/";
		}
		imagePath = path + resString + imagePath;
	}

	public String getImagePath() {
		return imagePath;
	}

	public String getAuthor() {
		return author;
	}

	public String getVersion() {
		return version;
	}

}
