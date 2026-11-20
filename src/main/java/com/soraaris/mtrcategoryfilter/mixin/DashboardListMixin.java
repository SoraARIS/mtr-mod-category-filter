package com.soraaris.mtrcategoryfilter.mixin;

import com.soraaris.mtrcategoryfilter.TagSearchItem;
import com.soraaris.mtrcategoryfilter.TagSearch;
import org.mtr.mapping.mapper.TextFieldWidgetExtension;
import org.mtr.mod.screen.DashboardList;
import org.mtr.mod.screen.DashboardListItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DashboardList.class, remap = false)
public abstract class DashboardListMixin {
	@Shadow(remap = false)
	@Final
	private TextFieldWidgetExtension textFieldSearch;

	@Redirect(
			method = "tick",
			at = @At(
					value = "INVOKE",
					target = "Lorg/mtr/mod/screen/DashboardListItem;getName(Z)Ljava/lang/String;",
					remap = false
			),
			remap = false
	)
	private String mtrCategoryFilter$searchTagsWhenRequested(DashboardListItem item, boolean formatted) {
		if (item instanceof TagSearchItem tagged) {
			final String query = textFieldSearch.getText2().toLowerCase(java.util.Locale.ROOT);
			if (query.startsWith("tag:")) {
				return TagSearch.searchableTags(tagged.mtrCategoryFilter$getTags());
			}
			if (query.startsWith("pack:")) {
				return TagSearch.searchablePack(tagged.mtrCategoryFilter$getPackName());
			}
		}
		return item.getName(formatted);
	}
}
