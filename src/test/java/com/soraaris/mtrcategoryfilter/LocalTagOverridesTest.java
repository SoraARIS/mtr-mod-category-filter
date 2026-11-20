package com.soraaris.mtrcategoryfilter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
	void rejectsInvalidKinds() throws IOException {
		final LocalTagOverrides overrides = LocalTagOverrides.load(temporaryDirectory.resolve("local-tags.json"));
		assertThrows(IllegalArgumentException.class, () -> overrides.addTag("formation", "mtr:train", "commuter"));
		assertThrows(IllegalArgumentException.class, () -> overrides.addTag("vehicle", "mtr:train", " "));
	}

	@Test
	void loadsMalformedFileNonfatallyAndPreservesItBeforeSaving() throws IOException {
		final Path invalidFile = temporaryDirectory.resolve("local-tags.json");
		final String invalidJson = """
				{"schemaVersion":1,"pack":{"id":"local","tags":["not-local"]}}
				""";
		Files.writeString(invalidFile, invalidJson);

		final LocalTagOverrides overrides = assertDoesNotThrow(() -> LocalTagOverrides.load(invalidFile));
		assertTrue(overrides.tagsFor("object", "mtr:sign").isEmpty());

		final Path backup;
		try (var files = Files.list(temporaryDirectory)) {
			backup = files.filter(path -> path.getFileName().toString().startsWith("local-tags-invalid-"))
					.findFirst()
					.orElseThrow();
		}
		assertEquals(invalidJson, Files.readString(backup));

		overrides.addTag("object", "mtr:sign", "platform");
		assertEquals(invalidJson, Files.readString(backup));
		assertEquals(List.of("platform"), List.copyOf(TagMetadata.parse(Files.readString(invalidFile))
				.resources().get("object").get("mtr:sign")));
	}
}
