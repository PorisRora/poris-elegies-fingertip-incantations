![珀璃丝追忆录：指尖咒文](https://raw.githubusercontent.com/PorisRora/poris-elegies-fingertip-incantations/v0.2.3-beta/docs/images/logo-horizontal.png)

# 珀璃丝追忆录：指尖咒文

指尖咒文（Poris Elegies: Fingertip Incantations）是一个提供自定义快捷栏与操作轮盘的模组。玩家可以将背包物品、原版动作、模组按键和已适配的模组技能绑定到屏幕上的格子，设置快捷键并调整布局。

以下介绍以 **0.2.3-beta** 为准，适用于 **Minecraft Java 版 1.21.1／NeoForge**，多人游戏需要客户端和服务端均安装。

物品格引用背包里已有的物品。按下剑对应的快捷键会将其拿到主手，物品仍留在原来的库存位置；攻击、使用、蓄力和放置继续使用正常操作。

## 快捷栏与布局

编辑器右侧的内容库分为「物品」「原生」「模组」「技能」四页。将其中的物品或动作拖到格子上即可绑定；拖到另一格可以交换绑定，拖到空白处并确认后可以清空。

![编辑器：左侧为屏幕上的格子，右侧为物品、原生、模组和技能内容库](https://raw.githubusercontent.com/PorisRora/poris-elegies-fingertip-incantations/v0.2.3-beta/docs/images/zh-CN/editor-skills.webp)

快捷栏提供两种模式：

- **混合模式**：保留原版九格，额外增加指尖格。原版九格作为一组移动，数字键和鼠标滚轮继续用于原版选格。
- **指尖模式**：原来的九格也使用指尖绑定，可以改绑、移动或删除，通过各格自己的快捷键选择。

布局编辑支持对齐、吸附、间距调整，以及横排、竖排和网格排列。界面提供普通、星辉、Poris 三套主题，并可设置格子显示范围、模组功能图标和剩余耐久百分比。

| 指尖格配置 | 显示设置 |
| --- | --- |
| ![添加格子、调整排列与间距、吸附对齐](https://raw.githubusercontent.com/PorisRora/poris-elegies-fingertip-incantations/v0.2.3-beta/docs/images/zh-CN/layout-editor.webp) | ![选择主题、格子显示范围、模组功能图标和耐久百分比](https://raw.githubusercontent.com/PorisRora/poris-elegies-fingertip-incantations/v0.2.3-beta/docs/images/zh-CN/display-settings.webp) |

## 装备认这一件，补给认这一种

工具、武器等装备绑定会记住选中的具体物品，包括其耐久与物品数据。食物、方块等补给按种类和物品数据匹配，一叠用完后继续使用下一叠匹配物品。

按物品格的快捷键可以拿到主手，再按同一个键收起为空手。格子是已有物品的快捷入口，不增加背包容量；选中物品不会自动攻击、进食、施法或放置。

![悬停物品格可查看物品说明、库存位置、数量和快捷键](https://raw.githubusercontent.com/PorisRora/poris-elegies-fingertip-incantations/v0.2.3-beta/docs/images/zh-CN/item-tooltip.webp)

## 轮盘和模组按键

轮盘可以打开背包、切换视角、交换副手、丢弃物品、打开编辑器，也可以执行分配给扇区的其他动作。

默认**按住鼠标中键，指向功能，松开执行**；也可以改成点击式。

| 模组按键 | 轮盘配置 |
| --- | --- |
| ![将模组按键绑定到屏幕格子](https://raw.githubusercontent.com/PorisRora/poris-elegies-fingertip-incantations/v0.2.3-beta/docs/images/zh-CN/mod-keybinds.webp) | ![选择轮盘扇区并分配原生或模组功能](https://raw.githubusercontent.com/PorisRora/poris-elegies-fingertip-incantations/v0.2.3-beta/docs/images/zh-CN/radial-setup.webp) |

原版动作和支持的模组按键可以绑定到格子。**HUD 开启时，模组功能按键由指尖接管。** 没有绑定到格子、也没有被放行的模组功能键不会按原方式触发；需要保留某个模组的原快捷键时，可在「按键接管 → 模组白名单」里打开该模组。移动、攻击、使用等基础控制保留。

| 按键接管 | 模组白名单 |
| --- | --- |
| ![配置可选按键接管和使用真实格](https://raw.githubusercontent.com/PorisRora/poris-elegies-fingertip-incantations/v0.2.3-beta/docs/images/zh-CN/key-takeover.webp) | ![允许指定模组保留自己的原快捷键](https://raw.githubusercontent.com/PorisRora/poris-elegies-fingertip-incantations/v0.2.3-beta/docs/images/zh-CN/mod-allowlist.webp) |

## 已适配技能

内置 Iron's Spells 'n Spellbooks 适配，可用法术出现在「技能」页。技能格能显示适配方提供的冷却、开关状态、充能和再施放次数；悬停可查看法术自己的伤害、范围、持续时间等说明。

![技能格显示施放状态、等级、资源状态和法术自己的数值说明](https://raw.githubusercontent.com/PorisRora/poris-elegies-fingertip-incantations/v0.2.3-beta/docs/images/zh-CN/skill-tooltip.webp)

能否释放、消耗多少资源和实际效果仍由技能所属模组处理。其他模组可以通过[公开接口](https://github.com/PorisRora/poris-elegies-fingertip-incantations/blob/v0.2.3-beta/docs/INTEGRATION.md)添加技能或物品来源适配。

## 开始使用

1. 将模组装入 Minecraft 实例；多人游戏的服务端和参与玩家都需要安装。
2. 打开背包，点击右上角附近的指尖 Logo。
3. 绑定物品或动作，配置快捷键并整理布局。
4. 打开「按键接管 → 模组白名单」，放行需要保留原始快捷键的模组。
5. 返回游戏，用格子的快捷键或中键轮盘操作。

## 环境与兼容

- Minecraft Java 版 **1.21.1**。
- NeoForge **21.1.181 及以上**，需对应 Minecraft 1.21.1。
- 客户端和服务端均需安装，单人使用内置服务端。
- 可以独立使用，Iron's Spells 是可选联动。
- 内置适配支持 Iron's Spells **1.21.1-3.15.5 及以上**。

当前物品来源为原版玩家背包。第三方背包容器需要物品来源适配；使用私有物理输入的模组可能需要专门兼容。「按键接管」里的「使用真实格」是全局开关，默认开启，右键使用时会让其他模组读取物品实际所在的库存格；出现兼容问题时可关闭。

指尖模式下滚轮不切格，需要使用格子快捷键；希望沿用原版选格方式时可使用混合模式。

## 相关链接与反馈

- [中文使用指南](https://github.com/PorisRora/poris-elegies-fingertip-incantations/blob/v0.2.3-beta/docs/USER_GUIDE.zh-CN.md)
- [GitHub 源码](https://github.com/PorisRora/poris-elegies-fingertip-incantations)
- [0.2.3-beta 发行页面](https://github.com/PorisRora/poris-elegies-fingertip-incantations/releases/tag/v0.2.3-beta)
- [问题与建议](https://github.com/PorisRora/poris-elegies-fingertip-incantations/issues)

反馈问题时请提供模组和 NeoForge 版本、单人或多人环境、相关模组与复现步骤。

## 许可

源码、构建脚本、配置数据、界面翻译和技术文档采用 [**MIT 许可**](https://github.com/PorisRora/poris-elegies-fingertip-incantations/blob/v0.2.3-beta/LICENSE)。Logo 和美术素材**保留所有权利**，按[独立素材声明](https://github.com/PorisRora/poris-elegies-fingertip-incantations/blob/v0.2.3-beta/ASSET_LICENSE.md)允许将未经修改的官方发行包加入整合包。
