package com.soraaris.mtrcategoryfilter;

import com.google.gson.JsonParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalTagOverridesTest {
	@TempDir
	Path temporaryDirectory;

	@Test
	void persistsAndExportsResourceTagsUsingMetadataSchema() throws IOException {
		final Path localFile = temporaryDirectory.resolve("local-tags.json");
		final Path exportFile = temporaryDirectory.resolve("export/packs/local-tags.json");
		final LocalTagOverrides overrides = LocalTagOverrides.load(localFile);

		assertTrue(overrides.addTag("vehicle", "mtr:commuter", "regional express"));
		assertTrue(overrides.addTag("rail", "mtr:concrete", "modern"));
		assertFalse(overrides.addTag("vehicle", "MTR:COMMUTER", "regional express"));
		assertEquals(List.of("regional express"), List.copyOf(overrides.tagsFor("vehicle", "MTR:COMMUTER")));

		overrides.export(exportFile);
		final TagMetadata exported = TagMetadata.parse(Files.readString(exportFile));
		assertEquals(List.of("regional express"), List.copyOf(exported.resources().get("vehicle").get("mtr:commuter")));
		assertEquals(List.of("modern"), List.copyOf(exported.resources().get("rail").get("mtr:concrete")));
		assertTrue(exported.packTags().isEmpty());

		final LocalTagOverrides reloaded = LocalTagOverrides.load(localFile);
		assertEquals(overrides.tagsFor("vehicle", "mtr:commuter"), reloaded.tagsFor("vehicle", "mtr:commuter"));
	}

	@Test
	void removesTagsAndCleansUpResourcesAndKinds() throws IOException {
		final LocalTagOverrides overrides = LocalTagOverrides.load(temporaryDirectory.resolve("local-tags.json"));
		overrides.addTag("object", "mtr:sign", "platform");

		assertTrue(overrides.removeTag("object", "mtr:sign", "platform"));
		assertFalse(overrides.removeTag("object", "mtr:sign", "platform"));
		assertTrue(overrides.tagsFor("object", "mtr:sign").isEmpty());
	}

	@Test
	void rejectsInvalidKindsAndMalformedLocalFiles() throws IOException {
		final LocalTagOverrides overrides = LocalTagOverrides.load(temporaryDirectory.resolve("local-tags.json"));
		assertThrows(IllegalArgumentException.class, () -> overrides.addTag("formation", "mtr:train", "commuter"));
		assertThrows(IllegalArgumentException.class, () -> overrides.addTag("vehicle", "mtr:train", " "));

		final Path invalidFile = temporaryDirectory.resolve("invalid.json");
		Files.writeString(invalidFile, """
				{"schemaVersion":1,"pack":{"id":"local","tags":["not-local"]}}
				""");
		assertThrows(IOException.class, () -> LocalTagOverrides.load(invalidFile));
	}
}
