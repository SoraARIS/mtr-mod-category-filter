package com.soraaris.mtrcategoryfilter;

import org.junit.jupiter.api.Test;
import org.mtr.core.data.TransportMode;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class VehicleResourceIndexTest {
	@Test
	void skipsLookupWhenTransportModeIsMissing() {
		final AtomicBoolean resolverCalled = new AtomicBoolean();

		assertNull(VehicleResourceIndex.resolve(null, 0, (mode, index, consumer) -> resolverCalled.set(true)));
		assertFalse(resolverCalled.get());
	}

	@Test
	void returnsTheResourceForAValidModeAndIndex() {
		assertEquals("vehicle", VehicleResourceIndex.resolve(TransportMode.TRAIN, 2, (mode, index, consumer) -> {
			assertEquals(TransportMode.TRAIN, mode);
			assertEquals(2, index);
			consumer.accept("vehicle");
		}));
	}

	@Test
	void returnsNullWhenTheIndexHasNoResource() {
		assertNull(VehicleResourceIndex.resolve(TransportMode.TRAIN, 20, (mode, index, consumer) -> {
		}));
		assertNull(VehicleResourceIndex.resolve(TransportMode.TRAIN, -1, (mode, index, consumer) -> {
			throw new AssertionError("Negative indexes must not be resolved");
		}));
	}
}
