package com.soraaris.mtrcategoryfilter;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import org.mtr.core.data.TransportMode;
import org.mtr.mod.client.CustomResourceLoader;
import org.mtr.mod.resource.ObjectResource;
import org.mtr.mod.resource.RailResource;
import org.mtr.mod.resource.VehicleResource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public final class ResourceTagCommands {
	private static final List<String> KINDS = List.of("vehicle", "object", "rail");

	private ResourceTagCommands() {
	}

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, LocalTagOverrides overrides, Path exportPath) {
		final SuggestionProvider<FabricClientCommandSource> kindSuggestions = (context, builder) -> {
			final String prefix = builder.getRemaining().toLowerCase(Locale.ROOT);
			KINDS.stream().filter(kind -> kind.startsWith(prefix)).forEach(builder::suggest);
			return builder.buildFuture();
		};
		final SuggestionProvider<FabricClientCommandSource> resourceSuggestions = (context, builder) -> {
			final String kind = StringArgumentType.getString(context, "kind");
			scannedIds(kind).stream().filter(id -> id.toLowerCase(Locale.ROOT).startsWith(builder.getRemaining().toLowerCase(Locale.ROOT)))
					.forEach(builder::suggest);
			return builder.buildFuture();
		};

		dispatcher.register(ClientCommandManager.literal("mtrtag")
				.then(ClientCommandManager.literal("add")
						.then(ClientCommandManager.argument("kind", StringArgumentType.word()).suggests(kindSuggestions)
								.then(ClientCommandManager.argument("resource", StringArgumentType.word()).suggests(resourceSuggestions)
										.then(ClientCommandManager.argument("tag", StringArgumentType.greedyString())
												.executes(context -> update(context, overrides, true))))))
				.then(ClientCommandManager.literal("remove")
						.then(ClientCommandManager.argument("kind", StringArgumentType.word()).suggests(kindSuggestions)
								.then(ClientCommandManager.argument("resource", StringArgumentType.word()).suggests(resourceSuggestions)
										.then(ClientCommandManager.argument("tag", StringArgumentType.greedyString())
												.executes(context -> update(context, overrides, false))))))
				.then(ClientCommandManager.literal("export")
						.executes(context -> export(context, overrides, exportPath))));
	}

	private static int update(CommandContext<FabricClientCommandSource> context, LocalTagOverrides overrides, boolean add) {
		final String kind = StringArgumentType.getString(context, "kind");
		final String resourceId = StringArgumentType.getString(context, "resource");
		final String tag = StringArgumentType.getString(context, "tag");
		if (!KINDS.contains(kind.toLowerCase(Locale.ROOT))) {
			context.getSource().sendError(Text.literal("Resource kind must be vehicle, object, or rail."));
			return 0;
		}
		if (scannedIds(kind).stream().noneMatch(id -> id.equalsIgnoreCase(resourceId))) {
			context.getSource().sendError(Text.literal("Unknown scanned " + kind + " resource ID: " + resourceId));
			return 0;
		}

		try {
			final boolean changed = add
					? overrides.addTag(kind, resourceId, tag)
					: overrides.removeTag(kind, resourceId, tag);
			if (!changed) {
				context.getSource().sendFeedback(Text.literal(add ? "Tag already exists." : "Tag was not present."));
				return 0;
			}
			context.getSource().sendFeedback(Text.literal((add ? "Added " : "Removed ") + tag + " " + (add ? "to " : "from ") + kind + " " + resourceId + "."));
			return 1;
		} catch (IOException | IllegalArgumentException exception) {
			context.getSource().sendError(Text.literal("Unable to update local tags: " + exception.getMessage()));
			return 0;
		}
	}

	private static int export(CommandContext<FabricClientCommandSource> context, LocalTagOverrides overrides, Path exportPath) {
		try {
			overrides.export(exportPath);
			context.getSource().sendFeedback(Text.literal("Exported tag metadata to " + exportPath.toAbsolutePath() + "."));
			return 1;
		} catch (IOException exception) {
			context.getSource().sendError(Text.literal("Unable to export tag metadata: " + exception.getMessage()));
			return 0;
		}
	}

	private static Set<String> scannedIds(String kind) {
		final Set<String> ids = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		switch (kind.toLowerCase(Locale.ROOT)) {
			case "vehicle" -> {
				for (final TransportMode mode : TransportMode.values()) {
					CustomResourceLoader.iterateVehicles(mode, (VehicleResource vehicle) -> addId(ids, vehicle.getId()));
				}
			}
			case "object" -> CustomResourceLoader.getObjects().forEach((ObjectResource resource) -> addId(ids, resource.getId()));
			case "rail" -> CustomResourceLoader.getRails().forEach((RailResource resource) -> addId(ids, resource.getId()));
			default -> {
				return Collections.emptySet();
			}
		}
		return Collections.unmodifiableSet(ids);
	}

	private static void addId(Set<String> ids, String id) {
		if (id != null && !id.isBlank()) {
			ids.add(id);
		}
	}
}
