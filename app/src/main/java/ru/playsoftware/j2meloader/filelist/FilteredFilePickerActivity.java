package ru.playsoftware.j2meloader.filelist;

import android.support.annotation.Nullable;

import com.nononsenseapps.filepicker.AbstractFilePickerActivity;
import com.nononsenseapps.filepicker.AbstractFilePickerFragment;

import java.io.File;

public class FilteredFilePickerActivity extends AbstractFilePickerActivity<File> {

	private FilteredFilePickerFragment currentFragment;

	@Override
	protected AbstractFilePickerFragment<File> getFragment(@Nullable String startPath, int mode, boolean allowMultiple,
														   boolean allowCreateDir, boolean allowExistingFile, boolean singleClick) {
		// Only the fragment in this line needs to be changed
		currentFragment = new FilteredFilePickerFragment();
		currentFragment.setArgs(startPath, mode, allowMultiple, allowCreateDir, allowExistingFile, singleClick);
		return currentFragment;
	}

	@Override
	public void onBackPressed() {
		if (currentFragment.isBackTop()) {
			super.onBackPressed();
		} else {
			currentFragment.goUp();
		}
	}
}
