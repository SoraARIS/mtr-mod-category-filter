# MTR Category Filter

A client-side Fabric mod for Minecraft 1.20.1 and MTR 4.0.5. It adds searchable custom tags to MTR's vehicle, eye-candy object, and rail-style selectors without changing the displayed resource names or the existing name search.

## Resource-pack metadata

Add one JSON document per pack at `assets/mtr_category_filter/packs/<filename>.json`. Use a unique filename. `pack.id` must exactly match the pack name shown by MTR/Minecraft; its tags apply to every supported MTR resource identified as originating from that pack. Per-resource tags are additive.

```json
{
  "schemaVersion": 1,
  "pack": {
    "id": "Example Transit Pack",
    "tags": ["city", "regional"]
  },
  "resources": {
    "vehicle": {
      "mtr:commuter_train": ["electric", "express"]
    },
    "object": {
      "mtr:station_sign": ["platform"]
    },
    "rail": {
      "mtr:concrete_slab": ["modern"]
    }
  }
}
```

Resource keys are MTR resource IDs, separated into `vehicle`, `object`, and `rail` groups. The `resources` object is optional: pack tags work without listing individual IDs. MTR 4.0.5 does not expose source-pack identity through the selector API, so this mod traces the originating pack names from MTR's resource JSON during resource reload. Resources with no matching metadata remain available under their pack name, and those fallback pack names are sorted A-Z in the filter choices. Tags from multiple metadata documents referring to the same MTR resource are merged.

After enabling or editing a pack, reload resource packs. In vehicle, eye-candy object, or rail-style selection, open the **Filters** control to expand or fold the choices, select a tag or fallback pack name, and page through the list when needed. Selecting the same choice again clears the filter. The existing list search field also accepts `tag:<tag>` or `pack:<name>` as quick filters; normal text continues to search resource names. Filtering affects available resources without hiding the currently selected resources.

## Compatibility

The selector hooks are targeted to MTR's `DashboardListSelectorScreen` and `DashboardList` implementation and are validated against the public MTR Fabric 4.0.5 build for Minecraft 1.20.1. The affected selectors are vehicle selection, eye-candy object selection, and rail-style selection. Fallback pack names are recovered from MTR's current and legacy custom-resource JSON formats. This is a client-side mod; both the mod and MTR must be installed on the client. A later MTR release may require updating the mixin targets if its selector internals change.

MTR is a runtime dependency and is not bundled. The mixins interact with MTR's public selector/resource classes; no MTR implementation code is copied into this project.

## Build and test

With JDK 17 or newer, run `./gradlew test build`. The Gradle build resolves Minecraft/Fabric dependencies and the exact MTR 4.0.5+1.20.1 artifact from public repositories.
