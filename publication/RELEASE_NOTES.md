# 1.2.1

**Layouts saved by 1.0.0 are not read.** When you update from 1.0.0, slots go back to their default places (rows of
nine above the hotbar) and need arranging again; bound items, keys, actions and settings are kept. 1.0.0 cannot read
layouts saved by 1.2.1.

### Slot placement

- Slots keep their spacing in groups that follow one of nine screen anchors: the corners, edge midpoints, or center.
- Layouts that do not fit remain at their saved positions. Slots can extend off screen or overlap; adjust them in the
  editor when needed.
- Adding or dropping slots within 8 GUI pixels of an existing group joins that group and uses its anchor. The nearest
  group is chosen, or the topmost when groups overlap. Arrow-key nudges do not merge groups, groups moved together
  retain their separate grouping, and untouched slots are not rewritten.
- A group keeps its anchor while its center remains in the same screen region; crossing into another region can change
  the anchor. Joining an existing group uses that group's anchor.
- A group put down within 8 GUI pixels of one screen edge, and not of the opposite one, follows that edge even if its
  other end runs off screen, so a long row against the left edge stays there when the window narrows. Dragged away from
  that edge, it follows the region it is in again. Arrow-key nudges never apply this rule.
- Arrow keys move one pixel, or ten with Shift. Partly off-screen groups move back in gradually without jumping
  sideways; an edge that is already outside the screen cannot be pushed farther out. Oversized selections no longer
  cause movement errors.
- Resizing the window or pressing Esc during a slot drag cancels that drag instead of saving its preview.
- Small floating controls look for clear space where possible.

### Text and colors

- Single-line text that does not fit shrinks, then ends with an ellipsis if necessary; hovering reveals truncated text.
  Multi-line text has more consistent wrapping, including improved handling of Chinese text, numbers, and punctuation.
- The editor uses the same wording at both sizes. English labels are shorter and consistent, including **Slots**,
  **Display**, **Wheel setup**, and **Hide slots**; Chinese instructions have also been corrected.
- Status colors for readiness, cooldown, progress, missing resources, errors, paused actions, recasts, and totals are
  consistent across all three themes.

### Editor

- The larger editor layout is used when it fits, with a compact layout for smaller GUI sizes. Both follow Minecraft's
  GUI scale.
- The item-page hint aligns with its cells. The display panel uses a consistent width, the wheel setup hint is always
  visible, and wheel action cards use the larger size when space allows.

### Compatibility

- Requirements remain Minecraft 1.21.1 and NeoForge 21.1.181 or later. Optional Iron's Spells support still starts at
  1.21-3.4.4.
- Network protocol remains 3. Server-side behavior and the integration APIs are unchanged from 1.0.0.
- Layout files: see the note at the top of this section.

### Installation

- Close the game, remove the previous Fingertip JAR, and install `poris-elegies-fingertip-incantations-1.2.1.jar`. Keep one Fingertip version in `mods`.
- Install the mod on both client and server; matching versions are recommended for multiplayer.
- The `-sources.jar` is for developers and must not be placed in `mods`. Source code is MIT; the logo and artwork are All Rights Reserved.

---

## 1.2.1 相较 1.0.0 的变化

**从 1.0.0 升级请注意：格子位置会恢复为默认排列，需要重新摆放。** 物品与动作绑定、快捷键和其他设置保留。1.1.x 的布局仍能读取；1.0.0 无法读取 1.2.1 保存的布局。

- **定位与分组**：格组跟随屏幕九宫锚点，保持组内间距。放不下时按保存的位置显示，允许出屏或重叠，由玩家调整。
- **进组规则明确**：添加或拖放到已有组 8 个界面像素以内时加入该组，沿用目标组的锚点；多个目标选最近的，重叠时选最上层。方向键微调不合组，一起拖动的多个组保持各自分组，未操作的格子不会被重新保存。
- 贴着屏幕一侧（0–8 个界面像素）放下的组跟随该边，另一端允许溢出；直接拖离后按所在区域重新定位，方向键微调沿用原规则。
- **移动与取消更稳定**：方向键一次移动 1 像素，Shift 加速到 10 像素；出屏的格子逐步往里挪，不再突然跳回，已出屏的方向不能继续往外推。超长选区不再因移动报错；拖动中改变窗口大小或按 Esc 会取消本次拖动。
- **文字显示统一**：单行文字放不下时先缩小，再省略并提供悬停全文；多行文字改善换行，尤其是中文、数字和标点。大小编辑器使用相同文案，中英文按钮名称与说明同步修正。
- **状态颜色统一**：能用、冷却、进行中、缺资源、出错、暂停、再按一次和合计，在三套主题中使用一致的含义色。
- **编辑器排版调整**：空间充足时使用大号布局，否则使用紧凑布局。物品页说明与格子对齐，显示设置面板宽度统一，轮盘配置提示始终显示。
- **兼容信息**：网络协议仍为 3，服务端行为与适配接口保持不变。

### 安装与更新

需要 Minecraft 1.21.1 与 NeoForge 21.1.181 或更新版本；多人游戏需双端安装，建议两端使用相同版本。Iron's Spells 为可选联动，最低支持 1.21-3.4.4，同时需要满足其自身前置要求。

关闭游戏，移出旧指尖 JAR，放入 `poris-elegies-fingertip-incantations-1.2.1.jar`，同一 mods 文件夹只留一版。源码包 `-sources.jar` 供开发使用，不放进 mods。源码 MIT；Logo 与美术素材保留权利。
