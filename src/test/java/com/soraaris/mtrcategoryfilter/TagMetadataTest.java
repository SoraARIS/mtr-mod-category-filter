package com.soraaris.mtrcategoryfilter;

import org.junit.jupiter.api.Test;
import com.google.gson.JsonParseException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TagMetadataTest {
	@Test
	void filterChoiceWindowLimitsVisibleChoicesAndClampsScrolling() {
		assertEquals(3, FilterChoiceWindow.visibleLimit(600));
		assertEquals(5, FilterChoiceWindow.visibleLimit(854));
		assertEquals(3, FilterChoiceWindow.buttonCount(100, 600));
		assertEquals(5, FilterChoiceWindow.buttonCount(100, 854));
		assertEquals(0, FilterChoiceWindow.buttonCount(0, 854));
		assertEquals(0, FilterChoiceWindow.maxOffset(3, 5));
		assertEquals(7, FilterChoiceWindow.maxOffset(10, 3));
		assertEquals(0, FilterChoiceWindow.clampOffset(-1, 10, 3));
		assertEquals(7, FilterChoiceWindow.clampOffset(9, 10, 3));
		assertEquals(4, FilterChoiceWindow.clampOffset(4, 10, 3));
		assertEquals(5, FilterChoiceWindow.scroll(4, 10, 3, -1));
		assertEquals(3, FilterChoiceWindow.scroll(4, 10, 3, 1));
		assertEquals(7, FilterChoiceWindow.scroll(7, 10, 3, -1));
		assertEquals(3, FilterChoiceWindow.page(0, 10, 3, 1));
		assertEquals(0, FilterChoiceWindow.page(3, 10, 3, -1));
		assertEquals(7, FilterChoiceWindow.page(6, 10, 3, 1));
		assertEquals(7, FilterChoiceWindow.page(7, 10, 3, 1));
		assertEquals(0, FilterChoiceWindow.page(0, 3, 3, 1));
		assertEquals(7, FilterChoiceWindow.choiceIndex(7, 0, 10, 3));
		assertEquals(9, FilterChoiceWindow.choiceIndex(7, 2, 10, 3));
		assertEquals(-1, FilterChoiceWindow.choiceIndex(7, 3, 10, 3));
		assertEquals(-1, FilterChoiceWindow.choiceIndex(8, 2, 10, 3));
		assertEquals(-1, FilterChoiceWindow.choiceIndex(0, -1, 10, 3));
	}

	@Test
	void missingResourceMetadataSharesOneImmutableEmptyValue() {
		final TagResourceReloadListener.ResourceInfo first = TagResourceReloadListener.metadataFor("object", "missing");
		final TagResourceReloadListener.ResourceInfo second = TagResourceReloadListener.metadataFor("object", "missing");

		assertSame(first, second);
		assertTrue(first.tags().isEmpty());
		assertEquals("", first.packName());
		assertThrows(UnsupportedOperationException.class, () -> first.tags().add("mutable"));
	}

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
