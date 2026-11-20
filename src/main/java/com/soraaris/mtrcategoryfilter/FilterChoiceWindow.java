package com.soraaris.mtrcategoryfilter;

public final class FilterChoiceWindow {
	private static final int NARROW_SCREEN_WIDTH = 720;

	private FilterChoiceWindow() {
	}

	public static int visibleLimit(int screenWidth) {
		return screenWidth < NARROW_SCREEN_WIDTH ? 3 : 5;
	}

	public static int maxOffset(int totalChoices, int visibleLimit) {
		return Math.max(0, totalChoices - visibleLimit);
	}

	public static int clampOffset(int offset, int totalChoices, int visibleLimit) {
		return Math.max(0, Math.min(offset, maxOffset(totalChoices, visibleLimit)));
	}

	public static int scroll(int offset, int totalChoices, int visibleLimit, double wheelAmount) {
		return clampOffset(offset - (int) Math.signum(wheelAmount), totalChoices, visibleLimit);
	}
}
