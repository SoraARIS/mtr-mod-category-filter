package com.soraaris.mtrcategoryfilter.mixin;

import com.soraaris.mtrcategoryfilter.TagResourceReloadListener;
import com.soraaris.mtrcategoryfilter.TagSearch;
import com.soraaris.mtrcategoryfilter.TagSearchItem;
import com.soraaris.mtrcategoryfilter.FilterChoiceWindow;
import org.mtr.core.data.TransportMode;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.ListIterator;
import java.util.TreeSet;

@Mixin(value = DashboardListSelectorScreen.class, remap = false)
public abstract class DashboardListSelectorScreenMixin {
	private static final int MTR_CATEGORY_FILTER_PANEL_PADDING = 8;
	private static final int MTR_CATEGORY_FILTER_MAX_PANEL_WIDTH = 180;
	private static final int MTR_CATEGORY_FILTER_BOTTOM_MARGIN = 40;

	@Shadow(remap = false)
	@Final
	protected DashboardList availableList;

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
	private ObjectArrayList<FilterChoice> mtrCategoryFilter$choices;

	@Unique
	private String mtrCategoryFilter$selectedTag = "";

	@Unique
	private String mtrCategoryFilter$selectedPack = "";

	@Unique
	private boolean mtrCategoryFilter$expanded;

	@Unique
	private int mtrCategoryFilter$scrollOffset;

	@Unique
	private int mtrCategoryFilter$panelX;

	@Unique
	private int mtrCategoryFilter$panelWidth;

	@Unique
	private int mtrCategoryFilter$visibleLimit;

	@Unique
	private ObjectImmutableList<ObjectResource> mtrCategoryFilter$objects;

	@Unique
	private ObjectImmutableList<RailResource> mtrCategoryFilter$rails;

	@Unique
	private TransportMode mtrCategoryFilter$transportMode;

	@Unique
	private ButtonWidgetExtension mtrCategoryFilter$toggleButton;

	@Unique
	private ButtonWidgetExtension mtrCategoryFilter$previousPageButton;

	@Unique
	private ButtonWidgetExtension mtrCategoryFilter$nextPageButton;

	@Unique
	private ObjectArrayList<ButtonWidgetExtension> mtrCategoryFilter$tagButtons;

	@Inject(method = "init2", at = @At(value = "TAIL", remap = false), remap = false)
	private void mtrCategoryFilter$initializeTagControls(CallbackInfo callbackInfo) {
		mtrCategoryFilter$kind = mtrCategoryFilter$kindFor((Object) this);
		if (mtrCategoryFilter$kind == null) {
			return;
		}

		if (mtrCategoryFilter$kind.equals("object")) {
			mtrCategoryFilter$objects = CustomResourceLoader.getObjects();
		} else if (mtrCategoryFilter$kind.equals("rail")) {
			mtrCategoryFilter$rails = CustomResourceLoader.getRails();
		} else {
			mtrCategoryFilter$transportMode = ((VehicleSelectorScreenAccessor) (Object) this).mtrCategoryFilter$getSiding().getTransportMode();
		}
		mtrCategoryFilter$choices = mtrCategoryFilter$collectChoices(mtrCategoryFilter$kind);
		final DashboardListSelectorScreen screen = (DashboardListSelectorScreen) (Object) this;
		mtrCategoryFilter$panelX = MTR_CATEGORY_FILTER_PANEL_PADDING;
		mtrCategoryFilter$panelWidth = Math.max(0, Math.min(
				MTR_CATEGORY_FILTER_MAX_PANEL_WIDTH,
				availableList.x - MTR_CATEGORY_FILTER_PANEL_PADDING * 2
		));
		mtrCategoryFilter$visibleLimit = FilterChoiceWindow.buttonCount(mtrCategoryFilter$choices.size(), screen.width);
		final int toggleY = screen.height - MTR_CATEGORY_FILTER_BOTTOM_MARGIN;
		final int arrowWidth = Math.min(screen.SQUARE_SIZE, mtrCategoryFilter$panelWidth / 3);
		final int toggleWidth = mtrCategoryFilter$panelWidth - arrowWidth * 2;

		mtrCategoryFilter$toggleButton = new ButtonWidgetExtension(
				mtrCategoryFilter$panelX, toggleY, toggleWidth, screen.SQUARE_SIZE,
				"", button -> {
					mtrCategoryFilter$expanded = !mtrCategoryFilter$expanded;
					mtrCategoryFilter$refreshTagControls();
				}
		);
		screen.addChild(new ClickableWidget(mtrCategoryFilter$toggleButton));

		mtrCategoryFilter$previousPageButton = new ButtonWidgetExtension(
				mtrCategoryFilter$panelX + toggleWidth, toggleY, arrowWidth, screen.SQUARE_SIZE,
				"<", button -> mtrCategoryFilter$pageChoices(-1)
		);
		screen.addChild(new ClickableWidget(mtrCategoryFilter$previousPageButton));
		mtrCategoryFilter$nextPageButton = new ButtonWidgetExtension(
				mtrCategoryFilter$panelX + toggleWidth + arrowWidth, toggleY, arrowWidth, screen.SQUARE_SIZE,
				">", button -> mtrCategoryFilter$pageChoices(1)
		);
		screen.addChild(new ClickableWidget(mtrCategoryFilter$nextPageButton));

		mtrCategoryFilter$tagButtons = new ObjectArrayList<>(mtrCategoryFilter$visibleLimit);
		for (int slot = 0; slot < mtrCategoryFilter$visibleLimit; slot++) {
			final int slotIndex = slot;
			final ButtonWidgetExtension tagButton = new ButtonWidgetExtension(
					mtrCategoryFilter$panelX, 0, mtrCategoryFilter$panelWidth, screen.SQUARE_SIZE, "",
					button -> mtrCategoryFilter$selectChoiceAt(slotIndex)
			);
			mtrCategoryFilter$tagButtons.add(tagButton);
			screen.addChild(new ClickableWidget(tagButton));
		}
		mtrCategoryFilter$refreshTagControls();
	}

	@Inject(method = "mouseScrolled2", at = @At(value = "HEAD", remap = false), cancellable = true, remap = false)
	private void mtrCategoryFilter$scrollChoices(double mouseX, double mouseY, double amount, CallbackInfoReturnable<Boolean> callbackInfo) {
		if (!mtrCategoryFilter$expanded || mtrCategoryFilter$panelWidth <= 0 || amount == 0) {
			return;
		}
		final DashboardListSelectorScreen screen = (DashboardListSelectorScreen) (Object) this;
		final int choiceTop = mtrCategoryFilter$choiceTop();
		final int choiceBottom = choiceTop + mtrCategoryFilter$visibleLimit * screen.SQUARE_SIZE;
		if (mouseX < mtrCategoryFilter$panelX || mouseX >= mtrCategoryFilter$panelX + mtrCategoryFilter$panelWidth || mouseY < choiceTop || mouseY >= choiceBottom) {
			return;
		}

		mtrCategoryFilter$scrollOffset = FilterChoiceWindow.scroll(
				mtrCategoryFilter$scrollOffset,
				mtrCategoryFilter$choices.size(),
				mtrCategoryFilter$visibleLimit,
				amount
		);
		mtrCategoryFilter$refreshTagControls();
		callbackInfo.setReturnValue(true);
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
		final String kind = mtrCategoryFilter$kindFor(this);
		if (kind == null) {
			return items;
		}

		final ListIterator<DashboardListItem> iterator = items.listIterator();
		while (iterator.hasNext()) {
			final DashboardListItem item = iterator.next();
			final String resourceId = mtrCategoryFilter$resourceId(kind, item);
			if (resourceId == null) {
				continue;
			}
			final TagResourceReloadListener.ResourceInfo metadata = TagResourceReloadListener.metadataFor(kind, resourceId);
			final Set<String> tags = metadata.tags();
			final String packName = metadata.packName();
			if (filterBySelectedTag) {
				if (!mtrCategoryFilter$selectedTag.isEmpty() && !TagSearch.hasTag(tags, mtrCategoryFilter$selectedTag)) {
					iterator.remove();
					continue;
				}
				if (!mtrCategoryFilter$selectedPack.isEmpty() && !TagSearch.hasPack(packName, mtrCategoryFilter$selectedPack)) {
					iterator.remove();
					continue;
				}
			}
			if (!tags.isEmpty() || !packName.isEmpty()) {
				iterator.set(new TaggedListItem(item, tags, packName));
			}
		}
		return items;
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

	private String mtrCategoryFilter$resourceId(String kind, DashboardListItem item) {
		if (item.id < 0 || item.id > Integer.MAX_VALUE) {
			return null;
		}
		final int index = (int) item.id;
		if (kind.equals("object")) {
			return index < mtrCategoryFilter$objects.size() ? mtrCategoryFilter$objects.get(index).getId() : null;
		}
		if (kind.equals("rail")) {
			return index < mtrCategoryFilter$rails.size() ? mtrCategoryFilter$rails.get(index).getId() : null;
		}

		final VehicleResource[] vehicle = {null};
		CustomResourceLoader.getVehicleByIndex(mtrCategoryFilter$transportMode, index, resource -> vehicle[0] = resource);
		return vehicle[0] == null ? null : vehicle[0].getId();
	}

	@Unique
	private ObjectArrayList<FilterChoice> mtrCategoryFilter$collectChoices(String kind) {
		final Set<String> tags = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		final Set<String> packs = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		for (final DashboardListItem item : allData) {
			final String resourceId = mtrCategoryFilter$resourceId(kind, item);
			if (resourceId == null) {
				continue;
			}
			final TagResourceReloadListener.ResourceInfo metadata = TagResourceReloadListener.metadataFor(kind, resourceId);
			if (metadata.tags().isEmpty()) {
				if (!metadata.packName().isEmpty()) {
					packs.add(metadata.packName());
				}
			} else {
				tags.addAll(metadata.tags());
			}
		}

		final ObjectArrayList<FilterChoice> choices = new ObjectArrayList<>(tags.size() + packs.size());
		tags.forEach(tag -> choices.add(new FilterChoice("tag", tag)));
		packs.forEach(pack -> choices.add(new FilterChoice("pack", pack)));
		return choices;
	}

	@Unique
	private void mtrCategoryFilter$selectChoiceAt(int slot) {
		final int choiceIndex = FilterChoiceWindow.choiceIndex(
				mtrCategoryFilter$scrollOffset,
				slot,
				mtrCategoryFilter$choices.size(),
				mtrCategoryFilter$visibleLimit
		);
		if (choiceIndex < 0) {
			return;
		}
		mtrCategoryFilter$selectChoice(mtrCategoryFilter$choices.get(choiceIndex));
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
	private void mtrCategoryFilter$pageChoices(int direction) {
		mtrCategoryFilter$scrollOffset = FilterChoiceWindow.page(
				mtrCategoryFilter$scrollOffset,
				mtrCategoryFilter$choices.size(),
				mtrCategoryFilter$visibleLimit,
				direction
		);
		mtrCategoryFilter$refreshTagControls();
	}

	@Unique
	private void mtrCategoryFilter$refreshAvailableList() {
		final ObjectArrayList<DashboardListItem> availableData = new ObjectArrayList<>(allData.size());
		for (final DashboardListItem item : allData) {
			if (canRepeat || !selectedIds.contains(item.id)) {
				availableData.add(item);
			}
		}
		availableList.setData(mtrCategoryFilter$attachTags(availableData, true), false, false, false, false, true, false);
	}

	@Unique
	private void mtrCategoryFilter$refreshTagControls() {
		if (mtrCategoryFilter$toggleButton == null) {
			return;
		}
		final DashboardListSelectorScreen screen = (DashboardListSelectorScreen) (Object) this;
		mtrCategoryFilter$visibleLimit = FilterChoiceWindow.buttonCount(mtrCategoryFilter$choices.size(), screen.width);
		mtrCategoryFilter$scrollOffset = FilterChoiceWindow.clampOffset(
				mtrCategoryFilter$scrollOffset,
				mtrCategoryFilter$choices.size(),
				mtrCategoryFilter$visibleLimit
		);
		final String filterLabel = !mtrCategoryFilter$selectedTag.isEmpty()
				? "tag:" + mtrCategoryFilter$selectedTag
				: !mtrCategoryFilter$selectedPack.isEmpty() ? "pack:" + mtrCategoryFilter$selectedPack : "all";
		mtrCategoryFilter$toggleButton.setMessage2(new Text(TextHelper.literal("Filters: " + filterLabel + (mtrCategoryFilter$expanded ? " [-]" : " [+]")).data));
		final boolean hasMultiplePages = mtrCategoryFilter$choices.size() > mtrCategoryFilter$visibleLimit;
		mtrCategoryFilter$previousPageButton.visible = mtrCategoryFilter$expanded && hasMultiplePages && mtrCategoryFilter$scrollOffset > 0;
		mtrCategoryFilter$nextPageButton.visible = mtrCategoryFilter$expanded
				&& hasMultiplePages
				&& mtrCategoryFilter$scrollOffset < FilterChoiceWindow.maxOffset(
						mtrCategoryFilter$choices.size(),
						mtrCategoryFilter$visibleLimit
				);

		for (int slot = 0; slot < mtrCategoryFilter$tagButtons.size(); slot++) {
			final ButtonWidgetExtension button = mtrCategoryFilter$tagButtons.get(slot);
			final int choiceIndex = FilterChoiceWindow.choiceIndex(
					mtrCategoryFilter$scrollOffset,
					slot,
					mtrCategoryFilter$choices.size(),
					mtrCategoryFilter$visibleLimit
			);
			final boolean visible = mtrCategoryFilter$expanded && choiceIndex >= 0;
			button.visible = visible;
			if (visible) {
				final FilterChoice choice = mtrCategoryFilter$choices.get(choiceIndex);
				button.setMessage2(new Text(TextHelper.literal(choice.kind + ":" + choice.value).data));
				button.setX2(mtrCategoryFilter$panelX);
				button.setY2(mtrCategoryFilter$choiceTop() + slot * screen.SQUARE_SIZE);
				button.setWidth2(mtrCategoryFilter$panelWidth);
			}
		}
	}

	@Unique
	private int mtrCategoryFilter$choiceTop() {
		final DashboardListSelectorScreen screen = (DashboardListSelectorScreen) (Object) this;
		return screen.height - MTR_CATEGORY_FILTER_BOTTOM_MARGIN - mtrCategoryFilter$visibleLimit * screen.SQUARE_SIZE;
	}

	private static final class TaggedListItem extends DashboardListItem implements TagSearchItem {
		private final Set<String> tags;
		private final String packName;

		private TaggedListItem(DashboardListItem delegate, Set<String> tags, String packName) {
			super(delegate.id, delegate.getName(false), delegate.getColor(false));
			this.tags = tags;
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
