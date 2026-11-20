package com.soraaris.mtrcategoryfilter;

import org.mtr.core.data.TransportMode;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class VehicleResourceIndex {
	private VehicleResourceIndex() {
	}

	public static <T> T resolve(TransportMode mode, int index, Resolver<T> resolver) {
		if (mode == null || index < 0) {
			return null;
		}
		final AtomicReference<T> result = new AtomicReference<>();
		resolver.resolve(mode, index, result::set);
		return result.get();
	}

	@FunctionalInterface
	public interface Resolver<T> {
		void resolve(TransportMode mode, int index, Consumer<T> consumer);
	}
}
