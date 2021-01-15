package javax.microedition.lcdui;

import android.util.SparseIntArray;

import androidx.collection.SparseArrayCompat;

import static javax.microedition.lcdui.Canvas.*;

public class KeyMapper {
	private static final int DEFAULT_LAYOUT = 0;
	private static final int SIEMENS_LAYOUT = 1;
	private static final int MOTOROLA_LAYOUT = 2;
	private static final int CUSTOM_LAYOUT = 3;

	private static final int SIEMENS_KEY_UP = -59;
	private static final int SIEMENS_KEY_DOWN = -60;
	private static final int SIEMENS_KEY_LEFT = -61;
	private static final int SIEMENS_KEY_RIGHT = -62;
	private static final int SIEMENS_KEY_SOFT_LEFT = -1;
	private static final int SIEMENS_KEY_SOFT_RIGHT = -4;
	private static final int MOTOROLA_KEY_UP = -1;
	private static final int MOTOROLA_KEY_DOWN = -6;
	private static final int MOTOROLA_KEY_LEFT = -2;
	private static final int MOTOROLA_KEY_RIGHT = -5;
	private static final int MOTOROLA_KEY_FIRE = -20;
	private static final int MOTOROLA_KEY_SOFT_LEFT = -21;
	private static final int MOTOROLA_KEY_SOFT_RIGHT = -22;

	private static final SparseArrayCompat<String> keyCodeToKeyName = new SparseArrayCompat<>();
	private static final SparseIntArray keyCodeToCustom = new SparseIntArray();
	private static final SparseIntArray keyCodeToGameAction = new SparseIntArray();
	private static final SparseIntArray gameActionToKeyCode = new SparseIntArray();
	private static SparseIntArray androidToMIDP;
	private static int layoutType;

	static {
		mapKeyCode(KEY_NUM0, 0, "0");
		mapKeyCode(KEY_NUM1, 0, "1");
		mapKeyCode(KEY_NUM2, UP, "2");
		mapKeyCode(KEY_NUM3, 0, "3");
		mapKeyCode(KEY_NUM4, LEFT, "4");
		mapKeyCode(KEY_NUM5, FIRE, "5");
		mapKeyCode(KEY_NUM6, RIGHT, "6");
		mapKeyCode(KEY_NUM7, GAME_A, "7");
		mapKeyCode(KEY_NUM8, DOWN, "8");
		mapKeyCode(KEY_NUM9, GAME_B, "9");
		mapKeyCode(KEY_STAR, GAME_C, "ASTERISK");
		mapKeyCode(KEY_POUND, GAME_D, "POUND");
		mapKeyCode(KEY_UP, UP, "UP");
		mapKeyCode(KEY_DOWN, DOWN, "DOWN");
		mapKeyCode(KEY_LEFT, LEFT, "LEFT");
		mapKeyCode(KEY_RIGHT, RIGHT, "RIGHT");
		mapKeyCode(KEY_FIRE, FIRE, "SELECT");
		mapKeyCode(KEY_SOFT_LEFT, 0, "SOFT1");
		mapKeyCode(KEY_SOFT_RIGHT, 0, "SOFT2");
		mapKeyCode(KEY_CLEAR, 0, "CLEAR");
		mapKeyCode(KEY_SEND, 0, "SEND");
		mapKeyCode(KEY_END, 0, "END");

		mapGameAction(UP, KEY_UP);
		mapGameAction(LEFT, KEY_LEFT);
		mapGameAction(RIGHT, KEY_RIGHT);
		mapGameAction(DOWN, KEY_DOWN);
		mapGameAction(FIRE, KEY_FIRE);
		mapGameAction(GAME_A, KEY_NUM7);
		mapGameAction(GAME_B, KEY_NUM9);
		mapGameAction(GAME_C, KEY_STAR);
		mapGameAction(GAME_D, KEY_POUND);
	}

	private static void remapKeys() {
		if (layoutType == SIEMENS_LAYOUT) {
			keyCodeToCustom.put(Canvas.KEY_LEFT, SIEMENS_KEY_LEFT);
			keyCodeToCustom.put(Canvas.KEY_RIGHT, SIEMENS_KEY_RIGHT);
			keyCodeToCustom.put(Canvas.KEY_UP, SIEMENS_KEY_UP);
			keyCodeToCustom.put(Canvas.KEY_DOWN, SIEMENS_KEY_DOWN);
			keyCodeToCustom.put(Canvas.KEY_SOFT_LEFT, SIEMENS_KEY_SOFT_LEFT);
			keyCodeToCustom.put(Canvas.KEY_SOFT_RIGHT, SIEMENS_KEY_SOFT_RIGHT);

			mapGameAction(Canvas.LEFT, SIEMENS_KEY_LEFT);
			mapGameAction(Canvas.RIGHT, SIEMENS_KEY_RIGHT);
			mapGameAction(Canvas.UP, SIEMENS_KEY_UP);
			mapGameAction(Canvas.DOWN, SIEMENS_KEY_DOWN);

			mapKeyCode(SIEMENS_KEY_UP, Canvas.UP, "UP");
			mapKeyCode(SIEMENS_KEY_DOWN, Canvas.DOWN, "DOWN");
			mapKeyCode(SIEMENS_KEY_LEFT, Canvas.LEFT, "LEFT");
			mapKeyCode(SIEMENS_KEY_RIGHT, Canvas.RIGHT, "RIGHT");
			mapKeyCode(SIEMENS_KEY_SOFT_LEFT, 0, "SOFT1");
			mapKeyCode(SIEMENS_KEY_SOFT_RIGHT, 0, "SOFT2");
		} else if (layoutType == MOTOROLA_LAYOUT) {
			keyCodeToCustom.put(Canvas.KEY_UP, MOTOROLA_KEY_UP);
			keyCodeToCustom.put(Canvas.KEY_DOWN, MOTOROLA_KEY_DOWN);
			keyCodeToCustom.put(Canvas.KEY_LEFT, MOTOROLA_KEY_LEFT);
			keyCodeToCustom.put(Canvas.KEY_RIGHT, MOTOROLA_KEY_RIGHT);
			keyCodeToCustom.put(Canvas.KEY_FIRE, MOTOROLA_KEY_FIRE);
			keyCodeToCustom.put(Canvas.KEY_SOFT_LEFT, MOTOROLA_KEY_SOFT_LEFT);
			keyCodeToCustom.put(Canvas.KEY_SOFT_RIGHT, MOTOROLA_KEY_SOFT_RIGHT);

			mapGameAction(Canvas.LEFT, MOTOROLA_KEY_LEFT);
			mapGameAction(Canvas.RIGHT, MOTOROLA_KEY_RIGHT);
			mapGameAction(Canvas.UP, MOTOROLA_KEY_UP);
			mapGameAction(Canvas.DOWN, MOTOROLA_KEY_DOWN);
			mapGameAction(Canvas.FIRE, MOTOROLA_KEY_FIRE);

			mapKeyCode(MOTOROLA_KEY_UP, Canvas.UP, "UP");
			mapKeyCode(MOTOROLA_KEY_DOWN, Canvas.DOWN, "DOWN");
			mapKeyCode(MOTOROLA_KEY_LEFT, Canvas.LEFT, "LEFT");
			mapKeyCode(MOTOROLA_KEY_RIGHT, Canvas.RIGHT, "RIGHT");
			mapKeyCode(MOTOROLA_KEY_FIRE, Canvas.FIRE, "SELECT");
			mapKeyCode(MOTOROLA_KEY_SOFT_LEFT, 0, "SOFT1");
			mapKeyCode(MOTOROLA_KEY_SOFT_RIGHT, 0, "SOFT2");
		}
	}

	private static void mapKeyCode(int midpKeyCode, int gameAction, String keyName) {
		keyCodeToGameAction.put(midpKeyCode, gameAction);
		keyCodeToKeyName.put(midpKeyCode, keyName);
	}

	private static void mapGameAction(int gameAction, int keyCode) {
		gameActionToKeyCode.put(gameAction, keyCode);
	}

	static int convertAndroidKeyCode(int keyCode) {
		return androidToMIDP.get(keyCode, Integer.MAX_VALUE);
	}

	public static int convertKeyCode(int keyCode) {
		if (layoutType == DEFAULT_LAYOUT) {
			return keyCode;
		}
		return keyCodeToCustom.get(keyCode, keyCode);
	}

	public static void setKeyMapping(int layoutType, SparseIntArray android, SparseIntArray custom) {
		KeyMapper.layoutType = layoutType;
		androidToMIDP = android;
		remapKeys();
	}

	static int getKeyCode(int gameAction) {
		return gameActionToKeyCode.get(gameAction, Integer.MAX_VALUE);
	}

	static int getGameAction(int keyCode) {
		return keyCodeToGameAction.get(keyCode, Integer.MAX_VALUE);
	}

	static String getKeyName(int keyCode) {
		return keyCodeToKeyName.get(keyCode);
	}
}
