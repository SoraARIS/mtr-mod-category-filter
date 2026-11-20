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
	private static final ResourceInfo EMPTY_RESOURCE_INFO = new ResourceInfo(Collections.emptySet(), "");
	private static volatile Snapshot snapshot = new Snapshot(Collections.emptyMap());
	private static volatile LocalTagOverrides localOverrides;

	@Override
	public Identifier getFabricId() {
		return new Identifier(MOD_ID, "resource_tags");
	}

	@Override
	public void reload(ResourceManager manager) {
		final Map<ResourceKey, MutableResourceInfo> resources = new HashMap<>();
		final Map<String, Set<String>> packTagsByName = new HashMap<>();
		manager.findResources("packs", id -> id.getNamespace().equals(MOD_ID) && id.getPath().endsWith(".json"))
				.forEach((id, resource) -> {
					try (Reader reader = resource.getReader()) {
						final TagMetadata metadata = TagMetadata.parse(readAll(reader));
						packTagsByName.computeIfAbsent(normalize(metadata.packId()), ignored -> new HashSet<>()).addAll(metadata.packTags());
						metadata.resources().forEach((kind, resourceEntries) -> resourceEntries.forEach((resourceId, resourceTags) -> {
							final MutableResourceInfo info = resources.computeIfAbsent(new ResourceKey(kind, resourceId), ignored -> new MutableResourceInfo());
							info.tags.addAll(metadata.packTags());
							info.tags.addAll(resourceTags);
						}));
					} catch (IOException | JsonParseException | IllegalArgumentException exception) {
						LOGGER.error("Ignoring invalid MTR category metadata at {}", id, exception);
					}
				});

		collectPackNames(manager, new Identifier("mtr", "mtr_custom_resources.json"), resources);
		collectPackNames(manager, new Identifier("mtr", "mtr_custom_resources_pending_migration.json"), resources);
		collectLegacyPackNames(manager, "rails", "rail", resources);
		collectLegacyPackNames(manager, "eyecandies", "object", resources);
		resources.values().forEach(info -> {
			final Set<String> packTags = packTagsByName.get(normalize(info.packName));
			if (packTags != null && !packTags.isEmpty()) {
				info.tags.addAll(packTags);
			}
		});

		final Map<ResourceKey, ResourceInfo> immutable = new HashMap<>(resources.size());
		resources.forEach((key, info) -> immutable.put(
				key,
				new ResourceInfo(info.tags.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(info.tags), info.packName)
		));
		snapshot = new Snapshot(Collections.unmodifiableMap(immutable));
	}

	public static ResourceInfo metadataFor(String kind, String resourceId) {
		final ResourceInfo loadedInfo = snapshot.resources.getOrDefault(new ResourceKey(kind, resourceId), EMPTY_RESOURCE_INFO);
		final LocalTagOverrides overrides = localOverrides;
		if (overrides == null) {
			return loadedInfo;
		}
		final Set<String> localTags = overrides.tagsFor(kind, resourceId);
		if (localTags.isEmpty()) {
			return loadedInfo;
		}
		final Set<String> combined = new HashSet<>(loadedInfo.tags());
		combined.addAll(localTags);
		return new ResourceInfo(Collections.unmodifiableSet(combined), loadedInfo.packName());
	}

	public static void setLocalOverrides(LocalTagOverrides overrides) {
		localOverrides = overrides;
	}

	public static Set<String> tagsFor(String kind, String resourceId) {
		return metadataFor(kind, resourceId).tags();
	}

	public static String packNameFor(String kind, String resourceId) {
		return metadataFor(kind, resourceId).packName();
	}

	private static void collectPackNames(ResourceManager manager, Identifier id, Map<ResourceKey, MutableResourceInfo> resources) {
		for (final Resource resource : manager.getAllResources(id)) {
			try (Reader reader = resource.getReader()) {
				final JsonElement rootElement = JsonParser.parseReader(reader);
				if (!rootElement.isJsonObject()) {
					continue;
				}
				final JsonObject root = rootElement.getAsJsonObject();
				final String packName = resource.getResourcePackName();
				collectArrayResources(root, "vehicles", "vehicle", packName, resources);
				collectArrayResources(root, "objects", "object", packName, resources);
				collectArrayResources(root, "rails", "rail", packName, resources);
				collectLegacyVehicles(root, packName, resources);
			} catch (IOException | JsonParseException | IllegalStateException exception) {
				LOGGER.warn("Unable to read MTR resource pack origins from {}", id, exception);
			}
		}
	}

	private static void collectArrayResources(JsonObject root, String arrayName, String kind, String packName, Map<ResourceKey, MutableResourceInfo> resources) {
		if (!root.has(arrayName) || !root.get(arrayName).isJsonArray()) {
			return;
		}
		for (final JsonElement element : root.getAsJsonArray(arrayName)) {
			if (element.isJsonObject() && element.getAsJsonObject().has("id")) {
				final JsonElement id = element.getAsJsonObject().get("id");
				if (id.isJsonPrimitive() && id.getAsJsonPrimitive().isString() && !id.getAsString().isBlank()) {
					setPackName(resources, kind, id.getAsString(), packName);
				}
			}
		}
	}

	private static void collectLegacyVehicles(JsonObject root, String packName, Map<ResourceKey, MutableResourceInfo> resources) {
		if (root.has("custom_trains") && root.get("custom_trains").isJsonObject()) {
			root.getAsJsonObject("custom_trains").keySet().forEach(id -> setPackName(resources, "vehicle", id, packName));
		}
	}

	private static void collectLegacyPackNames(ResourceManager manager, String directory, String kind, Map<ResourceKey, MutableResourceInfo> resources) {
		manager.findResources(directory, id -> LegacyResourceMetadata.supportsNamespace(id.getNamespace()) && id.getPath().endsWith(".json"))
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
							LegacyResourceMetadata.forEachResourceId(
									id.getPath(),
									root,
									resourceId -> setPackName(resources, kind, resourceId, packName)
							);
						} catch (IOException | JsonParseException | IllegalStateException exception) {
							LOGGER.warn("Unable to read MTR resource pack origins from {}", id, exception);
						}
					}
				});
	}

	private static void setPackName(Map<ResourceKey, MutableResourceInfo> resources, String kind, String resourceId, String packName) {
		resources.computeIfAbsent(new ResourceKey(kind, resourceId), ignored -> new MutableResourceInfo()).packName = packName;
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

	public record ResourceInfo(Set<String> tags, String packName) {
	}

	private static final class MutableResourceInfo {
		private final Set<String> tags = new HashSet<>();
		private String packName = "";
	}

	private record Snapshot(Map<ResourceKey, ResourceInfo> resources) {
	}
}
