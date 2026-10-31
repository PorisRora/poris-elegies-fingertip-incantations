# 1.0.0 · First stable release

Fingertip Incantations brings customizable item, keybind, and skill slots, Mixed/Fingertip hotbar modes, a radial menu, and three visual themes to Minecraft.

This release adopts the 0.2.8-beta gameplay and interface baseline with the final drag-swap feedback fix. Network protocol remains 3.

## Changes since 0.2.4-beta

- Cleaner item/skill tooltips and shorter key-takeover text.
- Drag-swap confirmation stays in the bottom banner without marking either slot; editor selection follows the dragged binding to its destination.
- Item tooltips support the item-aware NeoForge path, including mod-provided visual components.
- Clearer key capture with persistent Backspace/Esc instructions and click-outside cancellation.
- During key capture, only the Delete toolbar button remains active; the Delete key can still be assigned as a shortcut.
- Blank canvas deselects editor slots; editing uses an outer outline while held-item highlighting remains visible.
- Removing, clearing, or rebinding the held Fingertip slot returns to an empty hand in either mode.

## Requirements and updating

- Minecraft Java Edition **1.21.1** and **NeoForge 21.1.181 or later** for that game version.
- Install on both the client and server for multiplayer.
- Iron's Spells 'n Spellbooks is optional; supported versions start at **1.21-3.4.4**. Install its own required dependencies as well.
- Close the game, remove the old Fingertip JAR, and install `poris-elegies-fingertip-incantations-1.0.0.jar`. Keep only one Fingertip JAR in `mods`.
- The `-sources.jar` is for development and must not be installed as a second mod.

Integration authors: the unused `InventorySources.name(SourceAddress)` helper from the public beta has been removed. Update callers to obtain a known provider's display name from `InventorySource.name()`.

Source code uses MIT; logo and artwork have a separate All Rights Reserved statement.

---

# 1.0.0 · 首个正式版

以 0.2.8-beta 的玩法与界面加上最终互换提示修正为正式版基线，网络协议仍为 3。互换时只显示底部提示，不在任一格子上打标记；编辑器选中会跟随拖动的内容到目标格。

相较公开的 0.2.4-beta，本版精简物品／技能悬停与按键接管文字，物品提示改用支持第三方视觉组件的 NeoForge 接口；优化录键提示、框外点击取消与空白画布取消选择。录键时工具栏只保留「删除」可用，Delete 键本身仍可录为快捷键。编辑器用外框表示编辑选择，同时保留手持格的选中样式。删除、清空或改绑当前手持指尖格后，两种模式都会回到空手。

需要 Minecraft 1.21.1、对应的 NeoForge 21.1.181 或更新版本。多人游戏需双端安装。Iron's 为可选联动，最低支持 1.21-3.4.4，另需满足它自己的前置依赖。

更新时关闭游戏，移出旧指尖 JAR，放入 `poris-elegies-fingertip-incantations-1.0.0.jar`，同一 `mods` 文件夹只留一版。源码包 `-sources.jar` 不放进 `mods`。

适配开发者注意：已移除公开测试版未使用的 `InventorySources.name(SourceAddress)` 辅助方法；原调用方需要改为从已知来源的 `InventorySource.name()` 获取名称。外部来源可参与查找与计数，编辑器直接浏览、绑定外部容器尚未实现。

源码 MIT；Logo／美术保留权利。
