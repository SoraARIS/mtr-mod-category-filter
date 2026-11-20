package com.soraaris.mtrcategoryfilter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class TagMetadata {
	private final String packId;
	private final Set<String> packTags;
	private final Map<String, Map<String, Set<String>>> resources;

	private TagMetadata(String packId, Set<String> packTags, Map<String, Map<String, Set<String>>> resources) {
		this.packId = packId;
		this.packTags = Collections.unmodifiableSet(packTags);
		this.resources = Collections.unmodifiableMap(resources);
	}

	public static TagMetadata parse(String json) {
		final JsonElement rootElement = JsonParser.parseString(json);
		if (!rootElement.isJsonObject()) {
			throw new JsonParseException("Tag metadata root must be an object");
		}

		final JsonObject root = rootElement.getAsJsonObject();
		if (!root.has("schemaVersion") || !root.get("schemaVersion").isJsonPrimitive() || !root.getAsJsonPrimitive("schemaVersion").isNumber() || root.get("schemaVersion").getAsInt() != 1) {
			throw new JsonParseException("Unsupported or missing schemaVersion; expected 1");
		}
		if (!root.has("pack") || !root.get("pack").isJsonObject()) {
			throw new JsonParseException("Tag metadata must contain a pack object");
		}

		final JsonObject pack = root.getAsJsonObject("pack");
		final String packId = requiredString(pack, "id", "pack.id");
		final Set<String> packTags = readTags(pack.get("tags"), "pack.tags");
		final Map<String, Map<String, Set<String>>> resources = new LinkedHashMap<>();

		if (root.has("resources")) {
			if (!root.get("resources").isJsonObject()) {
				throw new JsonParseException("resources must be an object");
			}
			for (final Map.Entry<String, JsonElement> kindEntry : root.getAsJsonObject("resources").entrySet()) {
				final String kind = kindEntry.getKey().toLowerCase(Locale.ROOT);
				if (!kind.equals("vehicle") && !kind.equals("object") && !kind.equals("rail")) {
					throw new JsonParseException("Unsupported resource kind: " + kindEntry.getKey());
				}
				if (!kindEntry.getValue().isJsonObject()) {
					throw new JsonParseException("resources." + kind + " must be an object");
				}
				final Map<String, Set<String>> byId = new LinkedHashMap<>();
				for (final Map.Entry<String, JsonElement> resourceEntry : kindEntry.getValue().getAsJsonObject().entrySet()) {
					if (resourceEntry.getKey().isBlank()) {
						throw new JsonParseException("Resource ids cannot be blank");
					}
					byId.put(normalize(resourceEntry.getKey()), readTags(resourceEntry.getValue(), "resources." + kind + "." + resourceEntry.getKey()));
				}
				resources.put(kind, Collections.unmodifiableMap(byId));
			}
		}

		return new TagMetadata(packId, packTags, resources);
	}

	public String packId() {
		return packId;
	}

	public Set<String> packTags() {
		return packTags;
	}

	public Map<String, Map<String, Set<String>>> resources() {
		return resources;
	}

	public Set<String> tagsFor(String kind, String resourceId) {
		final Map<String, Set<String>> byId = resources.get(kind.toLowerCase(Locale.ROOT));
		if (byId == null || !byId.containsKey(normalize(resourceId))) {
			return Collections.emptySet();
		}
		final Set<String> tags = new LinkedHashSet<>(packTags);
		tags.addAll(byId.get(normalize(resourceId)));
		return Collections.unmodifiableSet(tags);
	}

	private static String requiredString(JsonObject object, String key, String path) {
		if (!object.has(key) || !object.get(key).isJsonPrimitive() || !object.get(key).getAsJsonPrimitive().isString()) {
			throw new JsonParseException(path + " must be a non-empty string");
		}
		final String value = object.get(key).getAsString().trim();
		if (value.isEmpty()) {
			throw new JsonParseException(path + " must be a non-empty string");
		}
		return value;
	}

	private static Set<String> readTags(JsonElement element, String path) {
		if (element == null || !element.isJsonArray()) {
			throw new JsonParseException(path + " must be an array of strings");
		}
		final Set<String> tags = new LinkedHashSet<>();
		final JsonArray array = element.getAsJsonArray();
		for (final JsonElement tagElement : array) {
			if (!tagElement.isJsonPrimitive() || !tagElement.getAsJsonPrimitive().isString()) {
				throw new JsonParseException(path + " must contain only strings");
			}
			final String tag = tagElement.getAsString().trim();
			if (!tag.isEmpty()) {
				tags.add(tag);
			}
		}
		return tags;
	}

	private static String normalize(String value) {
		return value.toLowerCase(Locale.ROOT);
	}
}
