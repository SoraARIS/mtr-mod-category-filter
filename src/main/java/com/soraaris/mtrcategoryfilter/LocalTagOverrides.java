package com.soraaris.mtrcategoryfilter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class LocalTagOverrides {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String PACK_ID = "Local Overrides";
	private static final List<String> KINDS = List.of("vehicle", "object", "rail");

	private final Path file;
	private final Map<String, Map<String, Set<String>>> tagsByResource;

	private LocalTagOverrides(Path file, Map<String, Map<String, Set<String>>> tagsByResource) {
		this.file = file;
		this.tagsByResource = tagsByResource;
	}

	public static LocalTagOverrides load(Path file) throws IOException {
		if (!Files.exists(file)) {
			return new LocalTagOverrides(file, new LinkedHashMap<>());
		}

		try {
			final TagMetadata metadata = TagMetadata.parse(Files.readString(file, StandardCharsets.UTF_8));
			if (!metadata.packTags().isEmpty()) {
				throw new JsonParseException("Local overrides cannot declare pack tags");
			}
			final Map<String, Map<String, Set<String>>> resources = new LinkedHashMap<>();
			metadata.resources().forEach((kind, byId) -> {
				final Map<String, Set<String>> copiedResources = new LinkedHashMap<>();
				byId.forEach((id, tags) -> copiedResources.put(id, new LinkedHashSet<>(tags)));
				resources.put(kind, copiedResources);
			});
			return new LocalTagOverrides(file, resources);
		} catch (JsonParseException | IllegalArgumentException exception) {
			throw new IOException("Invalid local tag override file: " + file, exception);
		}
	}

	public synchronized Set<String> tagsFor(String kind, String resourceId) {
		final Map<String, Set<String>> byId = tagsByResource.get(normalizeKind(kind));
		if (byId == null) {
			return Collections.emptySet();
		}
		final Set<String> tags = byId.get(normalize(resourceId));
		return tags == null ? Collections.emptySet() : Collections.unmodifiableSet(new LinkedHashSet<>(tags));
	}

	public synchronized boolean addTag(String kind, String resourceId, String tag) throws IOException {
		final String normalizedKind = normalizeKind(kind);
		final String normalizedId = normalize(validate(resourceId, "Resource ID"));
		final String normalizedTag = validate(tag, "Tag");
		final Map<String, Set<String>> byId = tagsByResource.computeIfAbsent(normalizedKind, ignored -> new LinkedHashMap<>());
		final Set<String> tags = byId.computeIfAbsent(normalizedId, ignored -> new LinkedHashSet<>());
		if (!tags.add(normalizedTag)) {
			return false;
		}
		try {
			write(file);
		} catch (IOException exception) {
			tags.remove(normalizedTag);
			if (tags.isEmpty()) {
				byId.remove(normalizedId);
			}
			if (byId.isEmpty()) {
				tagsByResource.remove(normalizedKind);
			}
			throw exception;
		}
		return true;
	}

	public synchronized boolean removeTag(String kind, String resourceId, String tag) throws IOException {
		final String normalizedKind = normalizeKind(kind);
		final String normalizedId = normalize(validate(resourceId, "Resource ID"));
		final String normalizedTag = validate(tag, "Tag");
		final Map<String, Set<String>> byId = tagsByResource.get(normalizedKind);
		final Set<String> tags = byId == null ? null : byId.get(normalizedId);
		if (tags == null || !tags.remove(normalizedTag)) {
			return false;
		}
		if (tags.isEmpty()) {
			byId.remove(normalizedId);
		}
		if (byId.isEmpty()) {
			tagsByResource.remove(normalizedKind);
		}
		try {
			write(file);
		} catch (IOException exception) {
			tagsByResource.computeIfAbsent(normalizedKind, ignored -> new LinkedHashMap<>())
					.computeIfAbsent(normalizedId, ignored -> new LinkedHashSet<>())
					.add(normalizedTag);
			throw exception;
		}
		return true;
	}

	public synchronized void export(Path destination) throws IOException {
		write(destination);
	}

	private void write(Path destination) throws IOException {
		final Path absolute = destination.toAbsolutePath();
		Files.createDirectories(absolute.getParent());
		final Path temporary = Files.createTempFile(absolute.getParent(), "local-tags-", ".json.tmp");
		try {
			Files.writeString(temporary, GSON.toJson(toJson()), StandardCharsets.UTF_8);
			try {
				Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException exception) {
				Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

	private JsonObject toJson() {
		final JsonObject root = new JsonObject();
		root.addProperty("schemaVersion", 1);
		final JsonObject pack = new JsonObject();
		pack.addProperty("id", PACK_ID);
		pack.add("tags", new JsonArray());
		root.add("pack", pack);

		final JsonObject resources = new JsonObject();
		for (final String kind : KINDS) {
			final Map<String, Set<String>> byId = tagsByResource.get(kind);
			if (byId == null || byId.isEmpty()) {
				continue;
			}
			final JsonObject resourceTags = new JsonObject();
			byId.forEach((id, tags) -> {
				final JsonArray tagArray = new JsonArray();
				tags.forEach(tagArray::add);
				resourceTags.add(id, tagArray);
			});
			resources.add(kind, resourceTags);
		}
		root.add("resources", resources);
		return root;
	}

	private static String normalizeKind(String kind) {
		final String normalized = kind.toLowerCase(Locale.ROOT);
		if (!KINDS.contains(normalized)) {
			throw new IllegalArgumentException("Unsupported resource kind: " + kind);
		}
		return normalized;
	}

	private static String validate(String value, String name) {
		final String normalized = value.trim();
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException(name + " cannot be blank");
		}
		return normalized;
	}

	private static String normalize(String value) {
		return value.toLowerCase(Locale.ROOT);
	}
}
