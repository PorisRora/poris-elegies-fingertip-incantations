# 概述

咒文不止写在书页上。

也藏在每一次伸手、每一次抬起指尖的瞬间。

指尖咒文提供可自由编排的快捷栏和操作轮盘，将背包物品、原版动作、模组按键与已适配技能放进屏幕上的格子。

## 1.2.1 更新

现已提供 **Fabric 版**，保留指尖格编辑、布局、轮盘、物品绑定、主题与标准模组按键支持；暂不包含 NeoForge 版的 Iron's Spells 适配。

改善格子定位、分组与窗口缩放时的操作；方向键微调不合组，拖动中改变窗口大小或按 Esc 可取消拖动。统一文字适配、状态颜色与中英文说明，改进原有大小两档编辑器的选择条件与小窗口提示排版。

**从 1.0.0 升级请注意：格子位置会恢复为默认排列，需要重新摆放。** 物品与动作绑定、快捷键和其他设置保留。1.0.0 无法读取 1.2.1 保存的布局。

- 贴着屏幕一侧（0–8 个界面像素）放下的组跟随该边，另一端允许溢出；直接拖离后按所在区域重新定位，方向键微调沿用原规则。

截图沿用原有功能展示，部分按钮名称与面板尺寸已有调整。

## 主要功能

- 自由编排快捷栏，可调整格子的位置和快捷键，支持对齐、吸附、间距调整，以及横排、竖排和网格排列。
- 提供混合、指尖两种模式，可保留原版九格并添加指尖格，也可将原来的九格一起交给指尖管理。
- 拖放绑定物品或动作，拖到另一格交换绑定，拖到空白处并确认后清空。
- 装备记住绑定的那一件物品，补给按种类和物品数据匹配，一叠用完后接续下一叠匹配物品。
- 可配置操作轮盘，默认按住鼠标中键打开、指向后松开执行，也可改成点击式。
- NeoForge 版内置 Iron's Spells 'n Spellbooks 适配，可将可用法术放入格子，并显示适配方提供的冷却、充能、开关状态与技能说明。
- 提供普通、星辉、Poris 三套主题，可设置格子显示范围、模组功能指尖格和耐久百分比。
- 界面支持简体中文和英文，随游戏语言切换。

## 安装与使用

当前版本：1.2.1。

支持 Minecraft Java 版 1.21.1，使用 NeoForge 21.1.181+ 或 Fabric Loader 0.16.14+，请选择对应加载器的 JAR。

Fabric 必需前置：Fabric API 0.106.0+1.21.1 或更新的 1.21.1 版。

客户端与服务端均需安装，多人游戏建议使用相同版本。将模组 JAR 放入实例的 mods 文件夹，更新时移出旧版文件并完整重启游戏。

打开背包，点击右上角附近的指尖咒文 Logo，即可进入编辑器。将内容库中的物品或动作拖到格子上绑定，再进入「指尖格配置」调整位置和快捷键。

HUD 开启时会接管模组功能按键。需要保留其他模组的原快捷键时，请在「按键接管 → 模组白名单」里打开对应模组。

# 快捷栏模式

| 模式 | 原版九格 | 选格方式 |
| --- | --- | --- |
| 混合模式 | 保留原版行为，作为一组移动，旁边可添加指尖格 | 数字键与滚轮选择原版格；指尖格使用各自快捷键 |
| 指尖模式 | 原来的九格也使用指尖绑定，可改绑、移动或删除 | 使用各格自己的快捷键，滚轮不切格 |

物品格引用背包里已有的物品，不增加背包容量。按下物品格的快捷键会将对应物品拿到主手，再按同一个键收起为空手。

选中物品不会自动攻击、进食、施法或放置，仍需使用正常的攻击与使用操作。物品格与技能格的行为不同，已适配的技能格会将施放请求交给技能所属模组处理。

关闭编辑器顶部的启用开关，会恢复原版界面与控制；仅在显示设置中隐藏格子，不会停用指尖。

# 操作

| 操作 | 方法 |
| --- | --- |
| 打开编辑器 | 打开背包，点击右上角附近的指尖 Logo |
| 绑定物品或动作 | 从内容库拖到屏幕上的格子 |
| 交换绑定 | 将已有绑定拖到另一格 |
| 清空绑定 | 将绑定拖到空白处，并确认提示 |
| 调整格子位置 | 进入「指尖格配置」，拖动格子或使用排列、对齐功能 |
| 设置快捷键 | 在「指尖格配置」中单击一个指尖格，再录入按键 |
| 拿出或收起物品 | 按物品格的快捷键；再次按当前格的键收起为空手 |
| 使用轮盘 | 默认按住鼠标中键，指向功能后松开；可在轮盘配置中改为点击式 |
| 保留某个模组的原快捷键 | 在「按键接管 → 模组白名单」中打开对应模组 |
| 临时停用指尖 | 长按 Esc 三秒，直到重新启动游戏前保持停用 |

快捷键与基础控制、白名单模组按键或其他格子冲突时，格子可能暂停，但绑定和布局会保留。悬停格子可以查看原因，再更换按键。

# 兼容与联动

NeoForge 版无需额外函数库模组。Fabric 版的必需前置为 **Fabric API 0.106.0+1.21.1 或更新的 1.21.1 版**。Iron's Spells 联动仅在 NeoForge 版内置；Fabric 版保留技能与物品来源接口。 NeoForge 的 Iron's Spells 适配支持 1.21-3.4.4 及以上版本，联动模组自身的依赖仍需满足。

**Fabric 兼容提示：** Inventory Profiles Next 的自动补货可能干扰多人游戏中的副手物品归还，延迟较高时更容易出现。遇到时请关闭它的自动补货；1.2.1 尚未修复这一兼容问题。

技能能否施放、消耗多少资源和产生什么效果，由技能所属模组决定。指尖负责显示与操作转发，其他技能模组需要通过接口接入，不会自动识别任意法术。

默认物品来源为玩家主背包和原版快捷栏。箱子、第三方背包容器不会自动成为补给来源，第三方容器需要物品来源适配。

使用 Minecraft 标准按键系统的模组功能可以在编辑器中绑定；使用私有物理输入处理的功能需要额外适配。

「使用真实格」位于「按键接管」中，是默认开启的全局开关。右键使用通过指尖格拿着的物品时，让其他模组读取该物品实际所在的库存格；只影响这次右键，出现兼容问题时可关闭。

# 许可与链接

源码采用 MIT 许可，Logo 与美术素材保留所有权利。素材声明允许将未经修改的官方发行包加入整合包，并保留许可与来源信息。

- [GitHub 源码](https://github.com/PorisRora/poris-elegies-fingertip-incantations)
- [NeoForge 1.2.1](https://github.com/PorisRora/poris-elegies-fingertip-incantations/releases/tag/v1.2.1) · [Fabric 1.2.1](https://github.com/PorisRora/poris-elegies-fingertip-incantations/releases/tag/fabric-v1.2.1)
- [中文使用指南](https://github.com/PorisRora/poris-elegies-fingertip-incantations/blob/main/docs/USER_GUIDE.zh-CN.md)
- [问题反馈](https://github.com/PorisRora/poris-elegies-fingertip-incantations/issues)
- [MIT 许可](https://github.com/PorisRora/poris-elegies-fingertip-incantations/blob/v1.2.1/LICENSE)与[素材权利声明](https://github.com/PorisRora/poris-elegies-fingertip-incantations/blob/v1.2.1/ASSET_LICENSE.md)

# 画廊

拖动互换：把一格的内容拖到另一格上，松开就两格互换，按键不变。

![拖动互换](https://raw.githubusercontent.com/PorisRora/poris-elegies-fingertip-incantations/v1.0.0/docs/images/zh-CN/drag-swap.webp)

指尖格配置：添加和删除格子、横排竖排、网格、间距和吸附对齐。

![指尖格配置](https://raw.githubusercontent.com/PorisRora/poris-elegies-fingertip-incantations/v1.0.0/docs/images/zh-CN/layout-editor.webp)

显示设置：主题、格子显示范围、模组功能指尖格和耐久百分比。

![显示设置](https://raw.githubusercontent.com/PorisRora/poris-elegies-fingertip-incantations/v1.0.0/docs/images/zh-CN/display-settings.webp)

物品悬停说明：物品自己的说明、格号和快捷键。

![物品悬停说明](https://raw.githubusercontent.com/PorisRora/poris-elegies-fingertip-incantations/v1.0.0/docs/images/zh-CN/item-tooltip.webp)

将模组功能按键绑定到格子。

![模组功能按键](https://raw.githubusercontent.com/PorisRora/poris-elegies-fingertip-incantations/v1.0.0/docs/images/zh-CN/mod-keybinds.webp)

轮盘配置：为扇区选择原生或模组功能。

![轮盘配置](https://raw.githubusercontent.com/PorisRora/poris-elegies-fingertip-incantations/v1.0.0/docs/images/zh-CN/radial-setup.webp)

按键接管与「使用真实格」开关。

![按键接管](https://raw.githubusercontent.com/PorisRora/poris-elegies-fingertip-incantations/v1.0.0/docs/images/zh-CN/key-takeover.webp)

模组白名单：允许指定模组保留自己的快捷键。

![模组白名单](https://raw.githubusercontent.com/PorisRora/poris-elegies-fingertip-incantations/v1.0.0/docs/images/zh-CN/mod-allowlist.webp)

技能页与技能悬停：已适配的法术按模组分组，悬停显示施放状态、等级、资源状态和法术数值。

![技能页与技能悬停](https://raw.githubusercontent.com/PorisRora/poris-elegies-fingertip-incantations/v1.0.0/docs/images/zh-CN/skill-tooltip.webp)
