package com.soraaris.mtrcategoryfilter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyResourceMetadataTest {
	@Test
	void recognizesShinkansenPackNamespaceAndRailDefinitionPath() {
		final JsonObject rail = JsonParser.parseString("""
				{
				  "name": "Shinkansen Embankment (cobblestone)",
				  "model": "mtrsteamloco:rails/embankment_cobble/embankment_cobble.obj",
				  "repeatInterval": 1
				}
				""").getAsJsonObject();

		assertTrue(LegacyResourceMetadata.supportsNamespace("mtrsteamloco"));
		assertEquals(Set.of("embankment_cobble"), resourceIds("rails/embankment_cobble.json", rail));
	}

	@Test
	void extractsEyeCandyIdFromJsonSubdirectoryAndModelSchema() {
		final JsonObject object = JsonParser.parseString("""
				{
				  "name": "Shinkansen Bridge (Tied-arch) [Center]",
				  "model": "mtrsteamloco:eyecandies/shinkansen_bridge_round/shinkansen_bridge_round_center.obj",
				  "textureId": "mtrsteamloco:eyecandies/shinkansen_bridge_round/concrete.png",
				  "scale": [1, 1, 1],
				  "mirror": [true, true, true]
				}
				""").getAsJsonObject();

		assertEquals(
				Set.of("shinkansen_bridge_round_center"),
				resourceIds("eyecandies/json/shinkansen_bridge_round_center.json", object)
		);
	}

	@Test
	void retainsLegacyGroupedDefinitionIds() {
		final JsonObject groupedDefinitions = JsonParser.parseString("""
				{
				  "track_a": {"model": "mtr-nte:rails/track_a.obj"},
				  "track_b": {"model": "mtr-nte:rails/track_b.obj"}
				}
				""").getAsJsonObject();

		assertEquals(Set.of("track_a", "track_b"), resourceIds("rails/tracks.json", groupedDefinitions));
		assertTrue(LegacyResourceMetadata.supportsNamespace("mtr-nte"));
	}

	private static Set<String> resourceIds(String path, JsonObject definition) {
		final Set<String> ids = new HashSet<>();
		LegacyResourceMetadata.forEachResourceId(path, definition, ids::add);
		return ids;
	}
}
