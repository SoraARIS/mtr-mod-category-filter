package com.soraaris.mtrcategoryfilter.mixin;

import com.soraaris.mtrcategoryfilter.TagResourceReloadListener;
import com.soraaris.mtrcategoryfilter.TagSearch;
import com.soraaris.mtrcategoryfilter.TagSearchItem;
import org.mtr.core.data.Siding;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongCollection;
import org.mtr.mapping.holder.Text;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.client.CustomResourceLoader;
import org.mtr.mod.resource.ObjectResource;
import org.mtr.mod.resource.RailResource;
import org.mtr.mod.resource.VehicleResource;
import org.mtr.mod.screen.DashboardList;
import org.mtr.mod.screen.DashboardListItem;
import org.mtr.mod.screen.DashboardListSelectorScreen;
import org.mtr.mod.screen.EyeCandyObjectSelectionScreen;
import org.mtr.mod.screen.RailStyleSelectorScreen;
import org.mtr.mod.screen.VehicleSelectorScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Mixin(value = DashboardListSelectorScreen.class, remap = false)
public abstract class DashboardListSelectorScreenMixin {
	private static final int MTR_CATEGORY_FILTER_MAX_TAG_ROWS = 2;
	private static final int MTR_CATEGORY_FILTER_TAG_BUTTON_WIDTH = 110;

	@Shadow(remap = false)
	@Final
	protected DashboardList availableList;

	@Shadow(remap = false)
	@Final
	protected DashboardList selectedList;

	@Shadow(remap = false)
	@Final
	protected ObjectImmutableList<DashboardListItem> allData;

	@Shadow(remap = false)
	@Final
	protected LongCollection selectedIds;

	@Shadow(remap = false)
	@Final
	private boolean canRepeat;

	@Unique
	private String mtrCategoryFilter$kind;

	@Unique
	private ObjectArrayList<FilterChoice> mtrCategoryFilter$choices = new ObjectArrayList<>();

	@Unique
	private String mtrCategoryFilter$selectedTag = "";

	@Unique
	private String mtrCategoryFilter$selectedPack = "";

	@Unique
	private boolean mtrCategoryFilter$expanded;

	@Unique
	private int mtrCategoryFilter$page;

	@Unique
	private int mtrCategoryFilter$baseListHeight;

	@Unique
	private ButtonWidgetExtension mtrCategoryFilter$toggleButton;

	@Unique
	private ButtonWidgetExtension mtrCategoryFilter$previousPageButton;

	@Unique
	private ButtonWidgetExtension mtrCategoryFilter$nextPageButton;

	@Unique
	private ObjectArrayList<ButtonWidgetExtension> mtrCategoryFilter$tagButtons = new ObjectArrayList<>();

	@Inject(method = "init2", at = @At(value = "TAIL", remap = false), remap = false)
	private void mtrCategoryFilter$initializeTagControls(CallbackInfo callbackInfo) {
		mtrCategoryFilter$kind = mtrCategoryFilter$kindFor((Object) this);
		if (mtrCategoryFilter$kind == null) {
			return;
		}

		mtrCategoryFilter$choices = mtrCategoryFilter$collectChoices((Object) this, mtrCategoryFilter$kind);
		mtrCategoryFilter$choices.sort(Comparator
				.comparing((FilterChoice choice) -> choice.kind)
				.thenComparing(choice -> choice.value.toLowerCase(Locale.ROOT)));
		mtrCategoryFilter$baseListHeight = availableList.height;
		mtrCategoryFilter$tagButtons = new ObjectArrayList<>();
		final DashboardListSelectorScreen screen = (DashboardListSelectorScreen) (Object) this;
		final int columns = Math.max(1, screen.width / MTR_CATEGORY_FILTER_TAG_BUTTON_WIDTH);
		final int toggleY = screen.height - screen.SQUARE_SIZE * 3;

		mtrCategoryFilter$toggleButton = new ButtonWidgetExtension(
				screen.width / 2 - 90, toggleY, 180, screen.SQUARE_SIZE,
				"", button -> {
					mtrCategoryFilter$expanded = !mtrCategoryFilter$expanded;
					mtrCategoryFilter$refreshTagControls();
				}
		);
		mtrCategoryFilter$previousPageButton = new ButtonWidgetExtension(
				screen.width - screen.SQUARE_SIZE * 2, toggleY, screen.SQUARE_SIZE, screen.SQUARE_SIZE,
				"<", button -> {
					mtrCategoryFilter$page = Math.max(0, mtrCategoryFilter$page - 1);
					mtrCategoryFilter$refreshTagControls();
				}
		);
		mtrCategoryFilter$nextPageButton = new ButtonWidgetExtension(
				screen.width - screen.SQUARE_SIZE, toggleY, screen.SQUARE_SIZE, screen.SQUARE_SIZE,
				">", button -> {
					mtrCategoryFilter$page = Math.min(mtrCategoryFilter$page + 1, mtrCategoryFilter$pageCount() - 1);
					mtrCategoryFilter$refreshTagControls();
				}
		);
		screen.addChild(new ClickableWidget(mtrCategoryFilter$toggleButton));
		screen.addChild(new ClickableWidget(mtrCategoryFilter$previousPageButton));
		screen.addChild(new ClickableWidget(mtrCategoryFilter$nextPageButton));

		for (final FilterChoice choice : mtrCategoryFilter$choices) {
			final ButtonWidgetExtension tagButton = new ButtonWidgetExtension(
					0, 0, 0, screen.SQUARE_SIZE, choice.kind + ":" + choice.value,
					button -> mtrCategoryFilter$selectChoice(choice)
			);
			mtrCategoryFilter$tagButtons.add(tagButton);
			screen.addChild(new ClickableWidget(tagButton));
		}
		mtrCategoryFilter$refreshTagControls();
	}

	@ModifyArg(
			method = "updateList",
			at = @At(
					value = "INVOKE",
					target = "Lorg/mtr/mod/screen/DashboardList;setData(Lorg/mtr/libraries/it/unimi/dsi/fastutil/objects/ObjectArrayList;ZZZZZZ)V",
					ordinal = 0,
					remap = false
			),
			index = 0,
			remap = false
	)
	private ObjectArrayList<DashboardListItem> mtrCategoryFilter$tagAvailableItems(ObjectArrayList<DashboardListItem> items) {
		return mtrCategoryFilter$attachTags(items, true);
	}

	@ModifyArg(
			method = "updateList",
			at = @At(
					value = "INVOKE",
					target = "Lorg/mtr/mod/screen/DashboardList;setData(Lorg/mtr/libraries/it/unimi/dsi/fastutil/objects/ObjectArrayList;ZZZZZZ)V",
					ordinal = 1,
					remap = false
			),
			index = 0,
			remap = false
	)
	private ObjectArrayList<DashboardListItem> mtrCategoryFilter$tagSelectedItems(ObjectArrayList<DashboardListItem> items) {
		return mtrCategoryFilter$attachTags(items, false);
	}

	private ObjectArrayList<DashboardListItem> mtrCategoryFilter$attachTags(ObjectArrayList<DashboardListItem> items, boolean filterBySelectedTag) {
		final Object screen = this;
		final String kind = mtrCategoryFilter$kindFor(screen);
		if (kind == null) {
			return items;
		}

		final ObjectImmutableList<ObjectResource> objects = kind.equals("object") ? CustomResourceLoader.getObjects() : null;
		final ObjectImmutableList<RailResource> rails = kind.equals("rail") ? CustomResourceLoader.getRails() : null;
		final ObjectArrayList<DashboardListItem> tagged = new ObjectArrayList<>(items.size());
		for (final DashboardListItem item : items) {
			final String resourceId = mtrCategoryFilter$resourceId(screen, kind, item, objects, rails);
			if (resourceId == null) {
				tagged.add(item);
				continue;
			}
			final Set<String> tags = TagResourceReloadListener.tagsFor(kind, resourceId);
			final String packName = TagResourceReloadListener.packNameFor(kind, resourceId);
			if (filterBySelectedTag) {
				if (!mtrCategoryFilter$selectedTag.isEmpty() && !TagSearch.hasTag(tags, mtrCategoryFilter$selectedTag)) {
					continue;
				}
				if (!mtrCategoryFilter$selectedPack.isEmpty() && !TagSearch.hasPack(packName, mtrCategoryFilter$selectedPack)) {
					continue;
				}
			}
			tagged.add(tags.isEmpty() && packName.isEmpty() ? item : new TaggedListItem(item, tags, packName));
		}
		return tagged;
	}

	private static String mtrCategoryFilter$kindFor(Object screen) {
		if (screen instanceof VehicleSelectorScreen) {
			return "vehicle";
		}
		if (screen instanceof EyeCandyObjectSelectionScreen) {
			return "object";
		}
		if (screen instanceof RailStyleSelectorScreen) {
			return "rail";
		}
		return null;
	}

	private String mtrCategoryFilter$resourceId(
			Object screen,
			String kind,
			DashboardListItem item,
			ObjectImmutableList<ObjectResource> objects,
			ObjectImmutableList<RailResource> rails
	) {
		if (item.id < 0 || item.id > Integer.MAX_VALUE) {
			return null;
		}
		final int index = (int) item.id;
		if (kind.equals("object")) {
			return index < objects.size() ? objects.get(index).getId() : null;
		}
		if (kind.equals("rail")) {
			return index < rails.size() ? rails.get(index).getId() : null;
		}

		final Siding siding = ((VehicleSelectorScreenAccessor) screen).mtrCategoryFilter$getSiding();
		final String[] resourceId = {null};
		CustomResourceLoader.getVehicleByIndex(siding.getTransportMode(), index, (VehicleResource vehicle) -> resourceId[0] = vehicle.getId());
		return resourceId[0];
	}

	@Unique
	private ObjectArrayList<FilterChoice> mtrCategoryFilter$collectChoices(Object screen, String kind) {
		final Set<String> tags = new HashSet<>();
		final Set<String> packs = new HashSet<>();
		final ObjectImmutableList<ObjectResource> objects = kind.equals("object") ? CustomResourceLoader.getObjects() : null;
		final ObjectImmutableList<RailResource> rails = kind.equals("rail") ? CustomResourceLoader.getRails() : null;
		for (final DashboardListItem item : allData) {
			final String resourceId = mtrCategoryFilter$resourceId(screen, kind, item, objects, rails);
			if (resourceId == null) {
				continue;
			}
			final Set<String> resourceTags = TagResourceReloadListener.tagsFor(kind, resourceId);
			if (resourceTags.isEmpty()) {
				final String packName = TagResourceReloadListener.packNameFor(kind, resourceId);
				if (!packName.isEmpty()) {
					packs.add(packName);
				}
			} else {
				tags.addAll(resourceTags);
			}
		}

		final ObjectArrayList<FilterChoice> choices = new ObjectArrayList<>();
		TagSearch.alphabetical(tags).forEach(tag -> choices.add(new FilterChoice("tag", tag)));
		TagSearch.alphabetical(packs).forEach(pack -> choices.add(new FilterChoice("pack", pack)));
		return choices;
	}

	@Unique
	private void mtrCategoryFilter$selectChoice(FilterChoice choice) {
		if (choice.kind.equals("tag")) {
			mtrCategoryFilter$selectedTag = mtrCategoryFilter$selectedTag.equalsIgnoreCase(choice.value) ? "" : choice.value;
			mtrCategoryFilter$selectedPack = "";
		} else {
			mtrCategoryFilter$selectedPack = mtrCategoryFilter$selectedPack.equalsIgnoreCase(choice.value) ? "" : choice.value;
			mtrCategoryFilter$selectedTag = "";
		}
		mtrCategoryFilter$refreshAvailableList();
		mtrCategoryFilter$refreshTagControls();
	}

	@Unique
	private void mtrCategoryFilter$refreshAvailableList() {
		final ObjectArrayList<DashboardListItem> availableData = new ObjectArrayList<>();
		for (final DashboardListItem item : allData) {
			if (canRepeat || !selectedIds.contains(item.id)) {
				availableData.add(item);
			}
		}
		availableList.setData(mtrCategoryFilter$attachTags(availableData, true), false, false, false, false, true, false);
	}

	@Unique
	private int mtrCategoryFilter$pageCount() {
		final DashboardListSelectorScreen screen = (DashboardListSelectorScreen) (Object) this;
		final int columns = Math.max(1, screen.width / MTR_CATEGORY_FILTER_TAG_BUTTON_WIDTH);
		final int pageCapacity = columns * MTR_CATEGORY_FILTER_MAX_TAG_ROWS;
		return Math.max(1, (mtrCategoryFilter$choices.size() + pageCapacity - 1) / pageCapacity);
	}

	@Unique
	private void mtrCategoryFilter$refreshTagControls() {
		if (mtrCategoryFilter$toggleButton == null) {
			return;
		}
		final DashboardListSelectorScreen screen = (DashboardListSelectorScreen) (Object) this;
		final int columns = Math.max(1, screen.width / MTR_CATEGORY_FILTER_TAG_BUTTON_WIDTH);
		final int pageCapacity = columns * MTR_CATEGORY_FILTER_MAX_TAG_ROWS;
		final int pageCount = mtrCategoryFilter$pageCount();
		mtrCategoryFilter$page = Math.min(mtrCategoryFilter$page, pageCount - 1);
		final String filterLabel = !mtrCategoryFilter$selectedTag.isEmpty()
				? "tag:" + mtrCategoryFilter$selectedTag
				: !mtrCategoryFilter$selectedPack.isEmpty() ? "pack:" + mtrCategoryFilter$selectedPack : "all";
		mtrCategoryFilter$toggleButton.setMessage2(new Text(TextHelper.literal("Filters: " + filterLabel + (mtrCategoryFilter$expanded ? " [-]" : " [+]")).data));
		mtrCategoryFilter$previousPageButton.visible = mtrCategoryFilter$expanded && mtrCategoryFilter$page > 0;
		mtrCategoryFilter$nextPageButton.visible = mtrCategoryFilter$expanded && mtrCategoryFilter$page < pageCount - 1;

		final int startIndex = mtrCategoryFilter$page * pageCapacity;
		final int pageTagCount = Math.min(pageCapacity, Math.max(0, mtrCategoryFilter$choices.size() - startIndex));
		final int rows = pageTagCount == 0 ? 0 : (pageTagCount + columns - 1) / columns;
		final int panelHeight = mtrCategoryFilter$expanded ? rows * screen.SQUARE_SIZE : 0;
		availableList.height = Math.max(screen.SQUARE_SIZE, mtrCategoryFilter$baseListHeight - panelHeight);
		selectedList.height = Math.max(screen.SQUARE_SIZE, mtrCategoryFilter$baseListHeight - panelHeight);
		final int toggleY = screen.height - screen.SQUARE_SIZE * 3;
		final int buttonWidth = Math.max(MTR_CATEGORY_FILTER_TAG_BUTTON_WIDTH, screen.width / columns);
		final int panelStartY = toggleY - panelHeight;

		for (int index = 0; index < mtrCategoryFilter$tagButtons.size(); index++) {
			final ButtonWidgetExtension button = mtrCategoryFilter$tagButtons.get(index);
			final int pageIndex = index - startIndex;
			final boolean visible = mtrCategoryFilter$expanded && pageIndex >= 0 && pageIndex < pageTagCount;
			button.visible = visible;
			if (visible) {
				final int row = pageIndex / columns;
				final int column = pageIndex % columns;
				button.setX2(column * buttonWidth);
				button.setY2(panelStartY + row * screen.SQUARE_SIZE);
				button.setWidth2(buttonWidth);
			}
		}
	}

	private static final class TaggedListItem extends DashboardListItem implements TagSearchItem {
		private final Set<String> tags;
		private final String packName;

		private TaggedListItem(DashboardListItem delegate, Set<String> tags, String packName) {
			super(delegate.id, delegate.getName(false), delegate.getColor(false));
			this.tags = Collections.unmodifiableSet(tags);
			this.packName = packName;
		}

		@Override
		public Set<String> mtrCategoryFilter$getTags() {
			return tags;
		}

		@Override
		public String mtrCategoryFilter$getPackName() {
			return packName;
		}

		@Override
		public String getName(boolean formatted) {
			return super.getName(formatted);
		}

		@Override
		public int getColor(boolean formatted) {
			return super.getColor(formatted);
		}
	}

	private static final class FilterChoice {
		private final String kind;
		private final String value;

		private FilterChoice(String kind, String value) {
			this.kind = kind;
			this.value = value;
		}
	}
}
