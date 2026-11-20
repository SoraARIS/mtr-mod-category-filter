package com.soraaris.mtrcategoryfilter;

import org.junit.jupiter.api.Test;
import com.google.gson.JsonParseException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TagMetadataTest {
	@Test
	void combinesPackAndResourceTagsForTheDeclaredResource() {
		final TagMetadata metadata = TagMetadata.parse("""
				{
				  "schemaVersion": 1,
				  "pack": {"id": "metro-pack", "tags": ["city", "regional"]},
				  "resources": {
				    "vehicle": {"mtr:commuter": ["electric", "regional"]},
				    "object": {"mtr:station_sign": ["platform"]}
				  }
				}
				""");

		assertEquals("metro-pack", metadata.packId());
		assertEquals(3, metadata.tagsFor("vehicle", "MTR:COMMUTER").size());
		assertTrue(metadata.tagsFor("vehicle", "mtr:commuter").containsAll(List.of("city", "regional", "electric")));
		assertTrue(metadata.tagsFor("rail", "mtr:default").isEmpty());
	}

	@Test
	void parsesArbitraryTagsAndCreatesSearchTokens() {
		final TagMetadata metadata = TagMetadata.parse("""
				{
				  "schemaVersion": 1,
				  "pack": {"id": "custom", "tags": []},
				  "resources": {"rail": {"mtr:old-line": ["pre-1950", "commuter rail"]}}
				}
				""");

		assertTrue(TagSearch.matches(metadata.tagsFor("rail", "mtr:old-line"), "TAG:commuter rail"));
		assertTrue(TagSearch.hasTag(metadata.tagsFor("rail", "mtr:old-line"), "PRE-1950"));
		assertTrue(!TagSearch.hasTag(metadata.tagsFor("rail", "mtr:old-line"), "pre"));
		assertTrue(TagSearch.matchesPack("Metro Addon", "PACK:metro addon"));
		assertTrue(TagSearch.hasPack("Metro Addon", "metro addon"));
		assertTrue(!TagSearch.hasPack("Metro Addon", "Metro"));
		assertEquals(List.of("Alpha", "mTR", "zulu"), List.copyOf(TagSearch.alphabetical(List.of("zulu", "mTR", "Alpha"))));
		assertEquals("tag:pre-1950 tag:commuter rail", TagSearch.searchableTags(metadata.tagsFor("rail", "mtr:old-line")));
	}

	@Test
	void rejectsUnknownSchemaVersionsAndInvalidTagShapes() {
		assertThrows(JsonParseException.class, () -> TagMetadata.parse("""
				{"schemaVersion": 2, "pack": {"id": "x", "tags": []}}
				"""));
		assertThrows(JsonParseException.class, () -> TagMetadata.parse("""
				{"schemaVersion": 1, "pack": {"id": "x", "tags": ["valid", 4]}}
				"""));
	}
}
