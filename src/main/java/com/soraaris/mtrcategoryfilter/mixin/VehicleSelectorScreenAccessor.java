package com.soraaris.mtrcategoryfilter.mixin;

import org.mtr.core.data.Siding;
import org.mtr.mod.screen.VehicleSelectorScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = VehicleSelectorScreen.class, remap = false)
public interface VehicleSelectorScreenAccessor {
	@Accessor(value = "siding", remap = false)
	Siding mtrCategoryFilter$getSiding();
}
