package com.soraaris.mtrcategoryfilter;

import com.google.gson.JsonObject;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class LegacyResourceMetadata {
	private LegacyResourceMetadata() {
	}

	public static boolean supportsNamespace(String namespace) {
		return namespace.equals("mtr-nte") || namespace.equals("mtrsteamloco");
	}

	public static Set<String> resourceIds(String path, JsonObject definition) {
		final Set<String> resourceIds = new LinkedHashSet<>();
		if (definition.has("model")) {
			final int lastSlash = path.lastIndexOf('/');
			final String filename = path.substring(lastSlash + 1);
			final int extension = filename.lastIndexOf('.');
			if (extension > 0) {
				resourceIds.add(filename.substring(0, extension));
			}
		} else {
			definition.keySet().stream()
					.filter(key -> !key.trim().isEmpty())
					.forEach(resourceIds::add);
		}
		return resourceIds;
	}
}
