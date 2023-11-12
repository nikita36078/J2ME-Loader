/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2018 Nikita Shakarun
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
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.AppCompatSpinner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import javax.microedition.lcdui.event.SimpleEvent;
import javax.microedition.lcdui.list.CompoundSpinnerAdapter;
import javax.microedition.util.ContextHolder;

public class ChoiceGroup extends Item implements Choice {
	private final ArrayList<String> strings = new ArrayList<>();
	private final ArrayList<Image> images = new ArrayList<>();
	private final ArrayList<CompoundButton> buttons = new ArrayList<>();
	private final ArrayList<Boolean> selected = new ArrayList<>();

	private Spinner spinner;
	private CompoundSpinnerAdapter adapter;
	private LinearLayout buttongroup;

	private final int choiceType;
	private int selectedIndex = -1;
	private int fitPolicy;

	private final SimpleEvent msgSetSelection = new SimpleEvent() {
		@Override
		public void process() {
			spinner.setSelection(selectedIndex);
		}
	};

	private final SimpleEvent msgDeleteAll = new SimpleEvent() {
		@Override
		public void process() {
			if (buttongroup != null) {
				buttons.clear();
				buttongroup.removeAllViews();
			} else if (spinner != null) {
				adapter.deleteAll();
			}
		}
	};

	private class RadioListener implements RadioGroup.OnCheckedChangeListener {
		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			selectedIndex = checkedId;
			notifyStateChanged();
		}
	}

	private class CheckListener implements CompoundButton.OnCheckedChangeListener {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			int index = buttonView.getId();

			synchronized (selected) {
				if (index >= 0 && index < selected.size()) {
					selected.set(index, isChecked);
				}
			}

			if (choiceType == MULTIPLE) {
				notifyStateChanged();
			}
		}
	}

	private class SpinnerListener implements AdapterView.OnItemSelectedListener {
		@Override
		public void onItemSelected(AdapterView parent, View view, int position, long id) {
			// prevent onItemSelected call after initializing
			if (!spinnerInitialized) {
				spinner.setSelection(selectedIndex);
				spinnerInitialized = true;
				return;
			}
			synchronized (selected) {
				if (selectedIndex >= 0 && selectedIndex < selected.size()) {
					selected.set(selectedIndex, Boolean.FALSE);
				}

				if (position >= 0 && position < selected.size()) {
					selected.set(position, Boolean.TRUE);
				}
			}

			selectedIndex = position;
			notifyStateChanged();
		}

		@Override
		public void onNothingSelected(AdapterView parent) {
			synchronized (selected) {
				if (selectedIndex >= 0 && selectedIndex < selected.size()) {
					selected.set(selectedIndex, Boolean.FALSE);
				}
			}

			selectedIndex = -1;
			notifyStateChanged();
		}
	}

	private final RadioListener radiolistener = new RadioListener();
	private final CheckListener checklistener = new CheckListener();
	private final SpinnerListener spinnerListener = new SpinnerListener();
	private boolean spinnerInitialized;

	public ChoiceGroup(String label, int choiceType) {
		switch (choiceType) {
			case POPUP:
			case EXCLUSIVE:
			case MULTIPLE:
				this.choiceType = choiceType;
				break;

			default:
				throw new IllegalArgumentException("choice type " + choiceType + " is not supported");
		}

		fitPolicy = Choice.TEXT_WRAP_DEFAULT;
		setLabel(label);
	}

	public ChoiceGroup(String label, int choiceType, String[] stringElements, Image[] imageElements) {
		this(label, choiceType);

		if (stringElements != null && imageElements != null && imageElements.length != stringElements.length) {
			throw new IllegalArgumentException("string and image arrays have different length");
		}

		if (stringElements != null) {
			strings.addAll(Arrays.asList(stringElements));
		}

		if (imageElements != null) {
			images.addAll(Arrays.asList(imageElements));
		}

		int size = Math.max(strings.size(), images.size());

		if (size > 0) {
			selected.addAll(Collections.nCopies(size, Boolean.FALSE));

			if (strings.size() == 0) {
				strings.addAll(Collections.nCopies(size, null));
			}

			if (images.size() == 0) {
				images.addAll(Collections.nCopies(size, null));
			}
			if (choiceType != MULTIPLE) {
				selectedIndex = 0;
				selected.set(0, true);
			}
		}
	}

	@Override
	public int append(String stringPart, Image imagePart) {
		synchronized (selected) {
			int index = selected.size();
			boolean select = index == 0 && choiceType != MULTIPLE;

			strings.add(stringPart);
			images.add(imagePart);
			selected.add(select);

			if (select) {
				selectedIndex = index;
				selected.set(index, true);
			}
			ViewHandler.postEvent(new SimpleEvent() {
				@Override
				public void process() {
					if (buttongroup != null) {
						addButton(index, stringPart, imagePart, select);
					} else if (spinner != null) {
						adapter.add(stringPart, imagePart);

						if (select) {
							spinner.setSelection(index);
						}
					}
				}
			});

			return index;
		}
	}

	@Override
	public void delete(int elementNum) {
		synchronized (selected) {
			strings.remove(elementNum);
			images.remove(elementNum);
			selected.remove(elementNum);

			if (selected.size() == 0) {
				selectedIndex = -1;
			}

			ViewHandler.postEvent(new SimpleEvent() {
				@Override
				public void process() {
					if (buttongroup != null) {
						buttons.remove(elementNum);
						buttongroup.removeViewAt(elementNum);

						updateButtonIDs(elementNum);
					} else if (spinner != null) {
						adapter.delete(elementNum);
					}
				}
			});
		}
	}

	@Override
	public void deleteAll() {
		synchronized (selected) {
			strings.clear();
			images.clear();
			selected.clear();

			selectedIndex = -1;

			ViewHandler.postEvent(msgDeleteAll);
		}
	}

	@Override
	public Image getImage(int elementNum) {
		return images.get(elementNum);
	}

	@Override
	public int getSelectedFlags(boolean[] selectedArray) {
		synchronized (selected) {
			if (selectedArray.length < selected.size()) {
				throw new IllegalArgumentException("return array is too short");
			}

			int index = 0;
			int selectedCount = 0;

			for (Boolean flag : selected) {
				if (flag) {
					selectedCount++;
				}

				selectedArray[index++] = flag;
			}

			while (index < selectedArray.length) {
				selectedArray[index++] = false;
			}

			return selectedCount;
		}
	}

	@Override
	public int getSelectedIndex() {
		return selectedIndex;
	}

	@Override
	public String getString(int elementNum) {
		return strings.get(elementNum);
	}

	@Override
	public void insert(int elementNum, String stringPart, Image imagePart) {
		synchronized (selected) {
			boolean select = selected.size() == 0 && choiceType != MULTIPLE;

			strings.add(elementNum, stringPart);
			images.add(elementNum, imagePart);
			selected.add(elementNum, select);

			if (select) {
				selectedIndex = elementNum;
			}

			ViewHandler.postEvent(new SimpleEvent() {
				@Override
				public void process() {
					if (buttongroup != null) {
						addButton(elementNum, stringPart, imagePart, select);
					} else if (spinner != null) {
						adapter.insert(elementNum, stringPart, imagePart);

						if (select) {
							spinner.setSelection(elementNum);
						}
					}
				}
			});
		}
	}

	@Override
	public boolean isSelected(int elementNum) {
		synchronized (selected) {
			return selected.get(elementNum);
		}
	}

	@Override
	public void set(int elementNum, String stringPart, Image imagePart) {
		synchronized (selected) {
			strings.set(elementNum, stringPart);
			images.set(elementNum, imagePart);
			ViewHandler.postEvent(new SimpleEvent() {
				@Override
				public void process() {
					if (buttongroup != null) {
						CompoundButton button = buttons.get(elementNum);

						button.setText(stringPart);

						if (imagePart != null) {
							button.setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(imagePart.getBitmap()), null, null, null);
						} else {
							button.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
						}

						button.setCompoundDrawablePadding(button.getPaddingLeft());
					} else if (adapter != null) {
						adapter.set(elementNum, stringPart, imagePart);
					}
				}
			});
		}
	}

	@Override
	public void setSelectedFlags(boolean[] selectedArray) {
		if (choiceType == EXCLUSIVE || choiceType == POPUP) {
			for (int i = 0; i < selectedArray.length; i++) {
				if (selectedArray[i]) {
					setSelectedIndex(i, true);
					return;
				}
			}
		}

		synchronized (selected) {
			if (selectedArray.length < selected.size()) {
				throw new IllegalArgumentException("array is too short");
			}

			int size = selected.size();

			if (buttongroup != null) {
				for (int i = 0; i < size; i++) {
					selected.set(i, selectedArray[i]);
					buttons.get(i).setChecked(selectedArray[i]);
				}
			} else {
				for (int i = 0; i < size; i++) {
					selected.set(i, selectedArray[i]);
				}
			}
		}
	}

	@Override
	public void setSelectedIndex(int elementNum, boolean flag) {
		synchronized (selected) {
			selected.set(elementNum, flag);

			if (flag) {
				selectedIndex = elementNum;
			}

			if (buttongroup != null) {
				buttons.get(elementNum).setChecked(flag);
			} else if (spinner != null) {
				if (flag) {
					ViewHandler.postEvent(msgSetSelection);
				}
			}
		}
	}

	@Override
	public void setFont(int elementNum, Font font) {
	}

	@Override
	public Font getFont(int elementNum) {
		return Font.getDefaultFont();
	}

	@Override
	public void setFitPolicy(int fitPolicy) {
		this.fitPolicy = fitPolicy;
	}

	@Override
	public int getFitPolicy() {
		return fitPolicy;
	}

	@Override
	public int size() {
		synchronized (selected) {
			return selected.size();
		}
	}

	@Override
	public View getItemContentView() {
		Context context = ContextHolder.getActivity();

		switch (choiceType) {
			case EXCLUSIVE:
				if (buttongroup == null) {
					buttongroup = new RadioGroup(context);
					initButtonGroup();

					((RadioGroup) buttongroup).setOnCheckedChangeListener(radiolistener);
				}

				return buttongroup;

			case MULTIPLE:
				if (buttongroup == null) {
					buttongroup = new LinearLayout(context);
					initButtonGroup();
				}

				return buttongroup;

			case POPUP:
				if (spinner == null) {
					adapter = new CompoundSpinnerAdapter();

					spinner = new AppCompatSpinner(context);
					spinner.setAdapter(adapter);

					int size = selected.size();

					for (int i = 0; i < size; i++) {
						adapter.add(strings.get(i), images.get(i));
					}

					if (selectedIndex >= 0 && selectedIndex < selected.size()) {
						spinner.setSelection(selectedIndex);
					}

					spinnerInitialized = false;
					spinner.setOnItemSelectedListener(spinnerListener);
				}

				return spinner;

			default:
				throw new InternalError();
		}
	}

	@Override
	public void clearItemContentView() {
		buttongroup = null;
		buttons.clear();

		spinner = null;
		adapter = null;
	}

	private void addButton(int index, String stringPart, Image imagePart, boolean checked) {
		Context context = ContextHolder.getActivity();

		if (buttongroup instanceof RadioGroup) {
			addButton(new RadioButton(context), index, stringPart, imagePart, checked);
		} else {
			addButton(new CheckBox(context), index, stringPart, imagePart, checked);
		}
	}

	private void addButton(CompoundButton button, int index, String stringPart, Image imagePart, boolean checked) {
		int w = LinearLayout.LayoutParams.MATCH_PARENT;
		int h = LinearLayout.LayoutParams.WRAP_CONTENT;
		button.setLayoutParams(new LinearLayout.LayoutParams(w, h));
		button.setText(stringPart);

		if (imagePart != null) {
			button.setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(imagePart.getBitmap()), null, null, null);
		}

		button.setCompoundDrawablePadding(button.getPaddingLeft());

		button.setId(index);
		button.setChecked(checked);
		button.setOnCheckedChangeListener(checklistener);

		if (index == buttons.size()) {
			buttons.add(button);
		} else {
			buttons.add(index++, button);

			if (buttons.get(index).getId() != index) {
				updateButtonIDs(index);
			}
		}

		buttongroup.addView(button);
	}

	private void updateButtonIDs(int fromIndex) {
		int size = buttons.size();

		for (int i = fromIndex; i < size; i++) {
			buttons.get(i).setId(i);
		}
	}

	private void initButtonGroup() {
		buttongroup.setOrientation(LinearLayout.VERTICAL);

		Context context = ContextHolder.getActivity();
		int size = selected.size();

		if (buttongroup instanceof RadioGroup) {
			for (int i = 0; i < size; i++) {
				addButton(new AppCompatRadioButton(context), i, strings.get(i), images.get(i), selected.get(i));
			}
		} else {
			for (int i = 0; i < size; i++) {
				addButton(new AppCompatCheckBox(context), i, strings.get(i), images.get(i), selected.get(i));
			}
		}
	}
}