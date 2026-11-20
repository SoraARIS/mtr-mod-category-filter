[English](README.md) | [简体中文](README_zh-CN.md)

# MTR 类别筛选器

这是一个适用于 Minecraft 1.20.1 和 MTR 4.0.5 的 Fabric 客户端模组。它为 MTR 的车辆、装饰物和轨道样式选择器添加可搜索的自定义标签，不会更改资源显示名称或现有名称搜索功能。

## 资源包元数据

在 `assets/mtr_category_filter/packs/<filename>.json` 中为每个资源包添加一个 JSON 文档。文件名必须唯一。`pack.id` 必须与 MTR/Minecraft 中显示的资源包名称完全一致；该资源包的标签会应用于所有被识别为来自该资源包的受支持 MTR 资源。单个资源的标签会与资源包标签叠加。

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

资源键是 MTR 资源 ID，分别归入 `vehicle`、`object` 和 `rail`。`resources` 对象是可选的：即使不列出单个 ID，也可以使用资源包标签。MTR 4.0.5 不会通过选择器 API 提供资源来源信息，因此本模组会在资源重载时从 MTR 的资源 JSON 中追踪来源资源包名称。没有匹配元数据的资源仍会显示其资源包名称作为筛选选项；这些备用资源包名称按 A-Z 排序。多个元数据文档中指向同一 MTR 资源的标签会合并。

启用或编辑资源包后，请重载资源包。在车辆、装饰物或轨道样式选择界面中，打开左下角的 **Filters** 控件以展开或收起选项。面板最多同时显示五个选项（窄屏上显示三个）；可在选项上滚动，或使用底部一行的箭头翻页。第一页和最后一页会隐藏相应方向的箭头。再次选择当前选项即可清除筛选。现有列表搜索框也支持使用 `tag:<tag>` 或 `pack:<name>` 进行快速筛选；普通文本仍会搜索资源名称。筛选只影响可选资源，不会隐藏已选中的资源。

## 游戏内资源标签命令

以下客户端命令可为一个已扫描的资源添加或移除标签：

```text
/mtrtag add <vehicle|object|rail> <resource_id> <tag>
/mtrtag remove <vehicle|object|rail> <resource_id> <tag>
```

示例：

```text
/mtrtag add vehicle mtr:commuter_train regional express
/mtrtag add object mtr:station_sign platform
/mtrtag add rail mtr:concrete_slab modern
/mtrtag remove vehicle mtr:commuter_train regional express
```

请将示例 ID 替换为当前 MTR 安装中实际可用的 ID。在命令和资源类型后使用 Tab 补全，可从 MTR 已加载的车辆、装饰物或轨道资源中选择 ID；命令也会拒绝扫描器中不存在的 ID。最后一个参数是完整标签，因此标签可以包含空格，无需加引号。

更改会保存在 `config/mtr_category_filter/local-tags.json`，并立即应用于选择器的标签搜索和筛选。它们不会修改来源资源包。若要分享这些标签，请运行：

```text
/mtrtag export
```

这会以与资源包元数据相同的格式写入 `config/mtr_category_filter/export/packs/local-tags.json`。将该文件复制到资源包中的 `assets/mtr_category_filter/packs/local-tags.json`。导出文件会包含逐个标记的资源 ID，且可以包含来自任意资源包的资源。

`remove` 只能移除之前添加到本地覆盖项中的标签。资源包加载的元数据采用叠加方式，且没有移除或覆盖语法，因此即使移除了本地同名标签，其他元数据文件提供的该标签仍会生效。

## 兼容性

选择器挂钩针对 MTR 的 `DashboardListSelectorScreen` 和 `DashboardList` 实现，已根据 Minecraft 1.20.1 的公开 MTR Fabric Loader 4.0.5 构建版本进行验证。支持的 MTR 版本为 4.0.5；由于 mixin 依赖这些内部实现，其他 MTR 版本不受支持。受影响的选择器包括车辆、装饰物和轨道样式选择器。备用资源包名称会从 MTR 当前和旧版自定义资源 JSON 格式中恢复，包括 `mtrsteamloco:rails/*.json` 和 `mtrsteamloco:eyecandies/json/*.json`。这是一个客户端模组；客户端必须同时安装本模组和 MTR。

MTR 是运行时依赖，不会打包在本模组中。Mixin 使用 MTR 的公开选择器/资源类；本项目没有复制 MTR 的实现代码。

## 构建和测试

使用 JDK 17 或更高版本运行 `./gradlew test build`。Gradle 会从公开仓库解析 Minecraft、Fabric Loader/API 依赖，以及适用于 Minecraft 1.20.1 的确切 MTR 4.0.5 构建版本。
