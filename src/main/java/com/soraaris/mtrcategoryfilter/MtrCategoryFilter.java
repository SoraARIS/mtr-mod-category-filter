package com.soraaris.mtrcategoryfilter;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;

import java.nio.file.Path;

public final class MtrCategoryFilter implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		final Path configDirectory = FabricLoader.getInstance().getConfigDir().resolve(TagResourceReloadListener.MOD_ID);
		final LocalTagOverrides localOverrides = LocalTagOverrides.load(configDirectory.resolve("local-tags.json"));
		TagResourceReloadListener.setLocalOverrides(localOverrides);
		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new TagResourceReloadListener());
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				ResourceTagCommands.register(dispatcher, localOverrides, configDirectory.resolve("export").resolve("packs").resolve("local-tags.json"))
		);
	}
}
