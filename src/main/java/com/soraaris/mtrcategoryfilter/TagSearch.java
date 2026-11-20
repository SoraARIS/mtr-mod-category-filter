package com.soraaris.mtrcategoryfilter;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public final class TagSearch {
	private TagSearch() {
	}

	public static String searchableTags(Collection<String> tags) {
		return tags.stream()
				.map(tag -> "tag:" + tag.toLowerCase(Locale.ROOT))
				.collect(Collectors.joining(" "));
	}

	public static String searchablePack(String packName) {
		return packName.isEmpty() ? "" : "pack:" + packName.toLowerCase(Locale.ROOT);
	}

	public static boolean matches(Collection<String> tags, String query) {
		final String normalizedQuery = query.toLowerCase(Locale.ROOT);
		return searchableTags(tags).contains(normalizedQuery);
	}

	public static boolean matchesPack(String packName, String query) {
		return searchablePack(packName).contains(query.toLowerCase(Locale.ROOT));
	}

	public static boolean hasPack(String packName, String selectedPack) {
		return packName.equalsIgnoreCase(selectedPack);
	}

	public static boolean hasTag(Collection<String> tags, String selectedTag) {
		return tags.stream().anyMatch(tag -> tag.equalsIgnoreCase(selectedTag));
	}

	public static Set<String> alphabetical(Collection<String> values) {
		final Set<String> sorted = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		sorted.addAll(values);
		return Collections.unmodifiableSet(sorted);
	}
}
