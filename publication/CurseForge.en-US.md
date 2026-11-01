![Poris Elegies: Fingertip Incantations](https://raw.githubusercontent.com/PorisRora/poris-elegies-fingertip-incantations/v1.0.0/docs/images/logo-horizontal.png)

# Poris Elegies: Fingertip Incantations

**Keep the items, controls, and skills you use most within reach.**

**Minecraft 1.21.1 · NeoForge · Client and server**

Fingertip Incantations adds a customizable RPG-style hotbar and a configurable action wheel. Bind an item or action to a visible slot, choose its shortcut, and place it where it feels comfortable.

Items stay in your existing inventory. Pressing a sword's slot equips that sword without moving it into a different storage slot; attack and use controls still work as usual.

## What's new in 1.2.1

**Updating from 1.0.0:** Slot positions reset to the default rows and need arranging again. Item and action bindings, shortcut keys, and other settings are retained. Layouts saved by 1.1.x still load; 1.0.0 cannot read layouts saved by 1.2.1.

- More stable slot placement across window sizes, with screen anchors and clear grouping rules.
- A group dropped within 8 GUI pixels of one screen edge follows that edge. Dragging directly away from the edge restores anchoring by screen region; arrow-key nudges keep their existing rules.
- Arrow-key nudges keep groups separate; resizing or pressing Esc cancels an unfinished slot drag.
- Consistent text fitting, revised English and Chinese labels, and shared status colors across themes.
- A larger editor where space allows, with a compact layout for smaller GUI sizes.

See the [changelog](https://github.com/PorisRora/poris-elegies-fingertip-incantations/blob/v1.2.1/CHANGELOG.md) for details. Screenshots below illustrate the core controls; some labels and panel sizes differ in 1.2.1.

## Build your own layout

Drag items and actions into slots, swap bindings by dragging one onto another, or clear them by dragging into empty space. Move and align the slots with snapping and group-arrangement tools.

![Drag one slot onto another and release to swap them; their keys stay](https://raw.githubusercontent.com/PorisRora/poris-elegies-fingertip-incantations/v1.0.0/docs/images/en/drag-swap.webp)

- **Mixed mode:** keep the vanilla nine-slot hotbar alongside extra Fingertip slots. The vanilla row moves as a group and keeps number-key and mouse-wheel selection.
- **Fingertip mode:** make the original nine positions editable Fingertip slots as well. Move, rebind, or remove them, and select slots using their assigned shortcuts.

The **Classic**, **Starlight**, and **Poris** themes provide different looks with the same controls. HUD visibility and optional durability percentages can be configured separately.

| Slots | Display |
| --- | --- |
| ![Add, delete, and arrange slots, adjust spacing, and enable snapping](https://raw.githubusercontent.com/PorisRora/poris-elegies-fingertip-incantations/v1.0.0/docs/images/en/layout-editor.webp) | ![Choose a theme, slot visibility, mod action slots, and durability percentages](https://raw.githubusercontent.com/PorisRora/poris-elegies-fingertip-incantations/v1.0.0/docs/images/en/display-settings.webp) |

## Equipment and supplies behave differently

Equipment bindings remember the particular item you chose, including its durability and data. Supplies such as food or blocks match by item type and data, and continue with another matching stack when the current stack is consumed.

Press a bound item key to equip it; press that same slot again to put it away. Slots are shortcuts to your existing items and do not add storage capacity. Selecting an item does not automatically attack, eat, cast, or place it.

![An item slot shows the item's own tooltip, slot number, shortcut, and relevant status](https://raw.githubusercontent.com/PorisRora/poris-elegies-fingertip-incantations/v1.0.0/docs/images/en/item-tooltip.webp)

## Keep common actions within reach

The action wheel can open your inventory, switch perspective, swap hands, drop items, open the editor, and run other assigned actions.

By default, **hold the middle mouse button, point at an action, and release**. Click-based operation is also available.

| Mod keybinds | Wheel setup |
| --- | --- |
| ![Bind a mod's keybind to a visible slot](https://raw.githubusercontent.com/PorisRora/poris-elegies-fingertip-incantations/v1.0.0/docs/images/en/mod-keybinds.webp) | ![Choose a wheel sector and assign a vanilla or mod action](https://raw.githubusercontent.com/PorisRora/poris-elegies-fingertip-incantations/v1.0.0/docs/images/en/radial-setup.webp) |

Vanilla actions and supported mod keybinds can be bound to visible slots. **While the HUD is enabled, mod function keys are routed through Fingertip.** Unbound mod shortcuts do not trigger unless their mod is turned on in **Key takeover → Mod whitelist**. Use that page when you want to retain a mod's original shortcuts. Basic movement, attack, use, and other reserved controls remain available.

| Key takeover | Mod whitelist |
| --- | --- |
| ![Configure optional key takeover and Use real slot](https://raw.githubusercontent.com/PorisRora/poris-elegies-fingertip-incantations/v1.0.0/docs/images/en/key-takeover.webp) | ![Allow selected mods to retain their original shortcuts](https://raw.githubusercontent.com/PorisRora/poris-elegies-fingertip-incantations/v1.0.0/docs/images/en/mod-allowlist.webp) |

## Supported mod skills

Built-in integration with **Iron's Spells 'n Spellbooks** makes available spells appear on the Skills page. Skill slots can show cooldowns, active states, charges, and recast counts when supplied by the integration.

Hovering can display the spell's own information, including damage, radius, or duration. Casting rules, resource costs, and gameplay remain controlled by the spell's own mod.

![The Skills page groups supported spells by mod; a skill slot shows its readiness, level, resource state, and spell details](https://raw.githubusercontent.com/PorisRora/poris-elegies-fingertip-incantations/v1.0.0/docs/images/en/skill-tooltip.webp)

Other mods can add skill providers and inventory-source integrations through the [public API](https://github.com/PorisRora/poris-elegies-fingertip-incantations/blob/v1.2.1/docs/INTEGRATION.md).

## Getting started

1. Install Fingertip in your Minecraft instance. For multiplayer, install it on the server and participating clients.
2. Open your inventory and click the Fingertip logo near the upper-right corner.
3. Bind items or actions, assign shortcuts, and arrange the layout.
4. Open Key takeover → Mod whitelist and turn on the mods whose original shortcuts you want to keep.
5. Return to the world and use the slot keys or the action wheel.

## Requirements and compatibility

- **Minecraft Java Edition 1.21.1**
- **NeoForge 21.1.181 or later** for Minecraft 1.21.1
- Required on **both client and server** for multiplayer
- Works independently; Iron's Spells is optional
- Built-in Iron's Spells integration supports **1.21-3.4.4 or later**

Item shortcuts use the vanilla player inventory. Third-party backpack contents require an inventory-source integration. Mods with their own physical-input handling may need additional compatibility support.

**Use real slot** in Key takeover is a global switch, enabled by default. During right-click use, it lets other mods see the held item's real inventory slot; turn it off if a mod misbehaves. In Fingertip mode, selection uses assigned slot keys; the mouse wheel does not select slots.

## Guides and feedback

- [User guide](https://github.com/PorisRora/poris-elegies-fingertip-incantations/blob/v1.2.1/docs/USER_GUIDE.md)
- [Source code](https://github.com/PorisRora/poris-elegies-fingertip-incantations)
- [Report a bug or suggest a feature](https://github.com/PorisRora/poris-elegies-fingertip-incantations/issues)
- [中文介绍](https://github.com/PorisRora/poris-elegies-fingertip-incantations/blob/v1.2.1/README.zh-CN.md)

For bug reports, include your mod and NeoForge versions, single-player or multiplayer environment, relevant mods, and steps to reproduce the issue.

## License

Source code, build scripts, configuration data, translations, and technical documentation use the [**MIT License**](https://github.com/PorisRora/poris-elegies-fingertip-incantations/blob/v1.2.1/LICENSE). The logo and artwork are **All Rights Reserved** under a [separate asset statement](https://github.com/PorisRora/poris-elegies-fingertip-incantations/blob/v1.2.1/ASSET_LICENSE.md). Unmodified official releases may be included in modpacks under that statement.
