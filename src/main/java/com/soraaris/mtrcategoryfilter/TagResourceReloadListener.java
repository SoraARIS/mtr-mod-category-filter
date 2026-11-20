package com.soraaris.mtrcategoryfilter;

import com.google.gson.JsonParseException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class TagResourceReloadListener implements SimpleSynchronousResourceReloadListener {
	public static final String MOD_ID = "mtr_category_filter";
	private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static volatile Snapshot snapshot = new Snapshot(Collections.emptyMap(), Collections.emptyMap());
	private static volatile LocalTagOverrides localOverrides;

	@Override
	public Identifier getFabricId() {
		return new Identifier(MOD_ID, "resource_tags");
	}

	@Override
	public void reload(ResourceManager manager) {
		final Map<ResourceKey, Set<String>> next = new HashMap<>();
		final Map<ResourceKey, String> nextPackByResource = new HashMap<>();
		final Map<String, Set<String>> packTagsByName = new HashMap<>();
		manager.findResources("packs", id -> id.getNamespace().equals(MOD_ID) && id.getPath().endsWith(".json"))
				.forEach((id, resource) -> {
					try (Reader reader = resource.getReader()) {
						final TagMetadata metadata = TagMetadata.parse(readAll(reader));
						packTagsByName.computeIfAbsent(normalize(metadata.packId()), ignored -> new HashSet<>()).addAll(metadata.packTags());
						metadata.resources().forEach((kind, resources) -> resources.forEach((resourceId, resourceTags) -> {
							final Set<String> merged = next.computeIfAbsent(new ResourceKey(kind, resourceId), ignored -> new HashSet<>());
							merged.addAll(metadata.packTags());
							merged.addAll(resourceTags);
						}));
					} catch (IOException | JsonParseException | IllegalArgumentException exception) {
						LOGGER.error("Ignoring invalid MTR category metadata at {}", id, exception);
					}
				});

		collectPackNames(manager, new Identifier("mtr", "mtr_custom_resources.json"), nextPackByResource);
		collectPackNames(manager, new Identifier("mtr", "mtr_custom_resources_pending_migration.json"), nextPackByResource);
		collectLegacyPackNames(manager, "rails", "rail", nextPackByResource);
		collectLegacyPackNames(manager, "eyecandies", "object", nextPackByResource);
		nextPackByResource.forEach((key, packName) -> {
			final Set<String> packTags = packTagsByName.get(normalize(packName));
			if (packTags != null && !packTags.isEmpty()) {
				next.computeIfAbsent(key, ignored -> new HashSet<>()).addAll(packTags);
			}
		});

		final Map<ResourceKey, Set<String>> immutable = new HashMap<>();
		next.forEach((key, tags) -> immutable.put(key, Collections.unmodifiableSet(new HashSet<>(tags))));
		snapshot = new Snapshot(Collections.unmodifiableMap(immutable), Collections.unmodifiableMap(nextPackByResource));
	}

	public static Set<String> tagsFor(String kind, String resourceId) {
		final Set<String> loadedTags = snapshot.tagsByResource.getOrDefault(new ResourceKey(kind, resourceId), Collections.emptySet());
		final LocalTagOverrides overrides = localOverrides;
		if (overrides == null) {
			return loadedTags;
		}
		final Set<String> localTags = overrides.tagsFor(kind, resourceId);
		if (localTags.isEmpty()) {
			return loadedTags;
		}
		final Set<String> combined = new HashSet<>(loadedTags);
		combined.addAll(localTags);
		return Collections.unmodifiableSet(combined);
	}

	public static void setLocalOverrides(LocalTagOverrides overrides) {
		localOverrides = overrides;
	}

	public static String packNameFor(String kind, String resourceId) {
		return snapshot.packByResource.getOrDefault(new ResourceKey(kind, resourceId), "");
	}

	private static void collectPackNames(ResourceManager manager, Identifier id, Map<ResourceKey, String> packNames) {
		for (final Resource resource : manager.getAllResources(id)) {
			try (Reader reader = resource.getReader()) {
				final JsonElement rootElement = JsonParser.parseReader(reader);
				if (!rootElement.isJsonObject()) {
					continue;
				}
				final JsonObject root = rootElement.getAsJsonObject();
				final String packName = resource.getResourcePackName();
				collectArrayResources(root, "vehicles", "vehicle", packName, packNames);
				collectArrayResources(root, "objects", "object", packName, packNames);
				collectArrayResources(root, "rails", "rail", packName, packNames);
				collectLegacyVehicles(root, packName, packNames);
			} catch (IOException | JsonParseException | IllegalStateException exception) {
				LOGGER.warn("Unable to read MTR resource pack origins from {}", id, exception);
			}
		}
	}

	private static void collectArrayResources(JsonObject root, String arrayName, String kind, String packName, Map<ResourceKey, String> packNames) {
		if (!root.has(arrayName) || !root.get(arrayName).isJsonArray()) {
			return;
		}
		for (final JsonElement element : root.getAsJsonArray(arrayName)) {
			if (element.isJsonObject() && element.getAsJsonObject().has("id")) {
				final JsonElement id = element.getAsJsonObject().get("id");
				if (id.isJsonPrimitive() && id.getAsJsonPrimitive().isString() && !id.getAsString().isBlank()) {
					packNames.put(new ResourceKey(kind, id.getAsString()), packName);
				}
			}
		}
	}

	private static void collectLegacyVehicles(JsonObject root, String packName, Map<ResourceKey, String> packNames) {
		if (root.has("custom_trains") && root.get("custom_trains").isJsonObject()) {
			root.getAsJsonObject("custom_trains").keySet().forEach(id -> packNames.put(new ResourceKey("vehicle", id), packName));
		}
	}

	private static void collectLegacyPackNames(ResourceManager manager, String directory, String kind, Map<ResourceKey, String> packNames) {
		manager.findResources(directory, id -> id.getNamespace().equals("mtr-nte") && id.getPath().endsWith(".json"))
				.keySet()
				.forEach(id -> {
					for (final Resource resource : manager.getAllResources(id)) {
						try (Reader reader = resource.getReader()) {
							final JsonElement rootElement = JsonParser.parseReader(reader);
							if (!rootElement.isJsonObject()) {
								continue;
							}
							final JsonObject root = rootElement.getAsJsonObject();
							final String packName = resource.getResourcePackName();
							if (root.has("model")) {
								final String path = id.getPath();
								final String resourceId = path.substring(path.lastIndexOf('/') + 1, path.length() - ".json".length());
								packNames.put(new ResourceKey(kind, resourceId), packName);
							} else {
								root.keySet().forEach(resourceId -> packNames.put(new ResourceKey(kind, resourceId), packName));
							}
						} catch (IOException | JsonParseException | IllegalStateException exception) {
							LOGGER.warn("Unable to read MTR resource pack origins from {}", id, exception);
						}
					}
				});
	}

	private static String readAll(Reader reader) throws IOException {
		final StringBuilder content = new StringBuilder();
		final char[] buffer = new char[2048];
		int length;
		while ((length = reader.read(buffer)) != -1) {
			content.append(buffer, 0, length);
		}
		return content.toString();
	}

	private static String normalize(String value) {
		return value.trim().toLowerCase(Locale.ROOT);
	}

	private record ResourceKey(String kind, String id) {
		private ResourceKey {
			kind = kind.toLowerCase(Locale.ROOT);
			id = id.toLowerCase(Locale.ROOT);
		}
	}

	private record Snapshot(Map<ResourceKey, Set<String>> tagsByResource, Map<ResourceKey, String> packByResource) {
	}
}
