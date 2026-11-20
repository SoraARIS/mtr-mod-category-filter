package com.soraaris.mtrcategoryfilter;

public final class FilterChoiceWindow {
	private static final int NARROW_SCREEN_WIDTH = 720;
	public static final int MAX_BUTTONS = 5;
	public static final int NARROW_BUTTONS = 3;

	private FilterChoiceWindow() {
	}

	public static int visibleLimit(int screenWidth) {
		return screenWidth < NARROW_SCREEN_WIDTH ? NARROW_BUTTONS : MAX_BUTTONS;
	}

	public static int buttonCount(int totalChoices, int screenWidth) {
		return Math.min(Math.max(0, totalChoices), visibleLimit(screenWidth));
	}

	public static int maxButtonCount(int screenWidth) {
		return visibleLimit(screenWidth);
	}

	public static int maxOffset(int totalChoices, int buttonCount) {
		return Math.max(0, totalChoices - buttonCount);
	}

	public static int clampOffset(int offset, int totalChoices, int buttonCount) {
		return Math.max(0, Math.min(offset, maxOffset(totalChoices, buttonCount)));
	}

	public static int choiceIndex(int offset, int buttonSlot, int totalChoices, int buttonCount) {
		final int index = offset + buttonSlot;
		return buttonSlot >= 0 && buttonSlot < buttonCount && index < totalChoices ? index : -1;
	}

	public static int scroll(int offset, int totalChoices, int buttonCount, double wheelAmount) {
		return clampOffset(offset - (int) Math.signum(wheelAmount), totalChoices, buttonCount);
	}
}
