package ru.playsoftware.j2meloader.filelist;

import android.support.annotation.NonNull;

import com.nononsenseapps.filepicker.FilePickerFragment;

import java.io.File;

public class FilteredFilePickerFragment extends FilePickerFragment {

	// File extension to filter on
	private static final String EXTENSION = ".jar";

	private String getExtension(@NonNull File file) {
		String path = file.getPath();
		int i = path.lastIndexOf(".");
		if (i < 0) {
			return null;
		} else {
			return path.substring(i);
		}
	}

	@Override
	protected boolean isItemVisible(final File file) {
		return true;
	}

	public File getBackTop() {
		return getPath(getArguments().getString(KEY_START_PATH, "/"));
	}

	public boolean isBackTop() {
		return 0 == compareFiles(mCurrentPath, getBackTop()) ||
				0 == compareFiles(mCurrentPath, new File("/"));
	}
}