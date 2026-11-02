<p align="center">
  <img src="docs/images/logo-horizontal.png" width="860" alt="Poris Elegies: Fingertip Incantations">
</p>

<p align="center"><strong>Keep the items, controls, and skills you use most within reach.</strong></p>
<p align="center">English · <a href="README.zh-CN.md">简体中文</a></p>

**Fingertip Incantations** is a customizable RPG-style hotbar. Equipment, supplies, vanilla actions, mod keybinds, and supported mod skills can all be bound to visible slots on the screen.

The slots point to your existing items. Selecting a sword equips that sword without moving it into a different inventory slot. Attacking, using items, placing blocks, and charging an action still use your normal controls.

| Requirement | Supported |
| --- | --- |
| Version | **1.2.1** |
| Minecraft | Java Edition **1.21.1** |
| Loader | **NeoForge 21.1.181+** or **Fabric Loader 0.16.14+**; download the matching edition |
| Required mod (Fabric) | **Fabric API 0.106.0+1.21.1 or later for Minecraft 1.21.1** |
| Installation | Client and server; single-player uses its integrated server |
| Optional integration (NeoForge only) | Iron's Spells 'n Spellbooks **1.21-3.4.4 or later** |
| License | Source code: **MIT** · Logo and artwork: **All Rights Reserved** |

[Download for NeoForge](https://github.com/PorisRora/poris-elegies-fingertip-incantations/releases/tag/v1.2.1) · [Download for Fabric](https://github.com/PorisRora/poris-elegies-fingertip-incantations/releases/tag/fabric-v1.2.1)

## What's new in 1.2.1

The **Fabric edition is now available**, with the same slot editor, layouts, action wheel, item bindings, themes, and standard mod keybind support. It does not include the NeoForge edition's Iron's Spells adapter.

**Updating from 1.0.0:** Slot positions reset to the default rows and need arranging again. Item and action bindings, shortcut keys, and other settings are retained. Layouts saved by 1.2.1 cannot be read by 1.0.0.

- More stable slot placement across window sizes, with screen anchors and clear grouping rules.
- A group dropped within 8 GUI pixels of one screen edge follows that edge. Dragging directly away from the edge restores anchoring by screen region; arrow-key nudges keep their existing rules.
- Arrow-key nudges keep groups separate; resizing or pressing Esc cancels an unfinished slot drag.
- Consistent text fitting, revised English and Chinese labels, and shared status colors across themes.
- Improved the existing editor size selection and small-window text layout, with clearer hints and better-fitting labels.

See the [changelog](CHANGELOG.md) for details. Screenshots below illustrate the core controls; some labels and panel sizes differ in 1.2.1.

## Make the hotbar your own

Create and arrange slots around the screen. Bind items or actions by dragging them from the editor, swap two bindings by dragging one onto another, and clear a binding by dragging it into empty space. Alignment, snapping, and group arrangement help keep the layout tidy.

![Drag to swap: drop one slot's content on another and release; the two swap and keep their keys](docs/images/en/drag-swap.webp)

Choose between two modes:

- **Mixed:** keep the vanilla nine-slot hotbar and add Fingertip slots alongside it. The vanilla row moves as a group and keeps number-key and mouse-wheel selection.
- **Fingertip:** use Fingertip bindings for the original nine positions as well. Slots can be moved, rebound, or removed, and selection uses their assigned shortcuts.

The **Classic**, **Starlight**, and **Poris** themes share the same controls. Choose which slots remain visible and whether to show item durability percentages.

| Slots | Display |
| --- | --- |
| ![Slots: add and delete slots, arrange in a row, column or grid, gap, snapping, canvas](docs/images/en/layout-editor.webp) | ![Display: style, which slots show, mod action slots, durability percentage](docs/images/en/display-settings.webp) |

## Keep equipment and supplies close

Equipment bindings remember the particular item you chose, including tools or weapons with their own durability and data. Supply bindings, such as food or blocks, match by item type and data, and continue with another matching stack when the current stack is consumed.

Press a bound item's key to equip it. Press the same slot's key again to put it away and use an empty hand. Your items remain in the inventory; the HUD does not add storage slots.

![Hovering an item slot: the item's own tooltip, slot number, shortcut, and relevant status](docs/images/en/item-tooltip.webp)

## Put controls on the screen

Bind vanilla actions and supported mod keybinds to visible slots. The configurable action wheel also provides quick access to actions such as opening the inventory, switching perspective, swapping hands, dropping items, and opening the editor.

By default, **hold the middle mouse button, point at an action, and release**. A click-based mode is available too.

| Mod keybinds on slots | Wheel setup |
| --- | --- |
| ![Mods page: other mods' keybinds can be dragged straight onto a slot](docs/images/en/mod-keybinds.webp) | ![Wheel setup: pick a position, then a vanilla or mod function](docs/images/en/radial-setup.webp) |

**Key takeover changes how mod shortcuts work while the HUD is enabled.** Unbound mod shortcuts are not forwarded unless their mod is turned on in the **Mod whitelist**, opened from the **Key takeover** page. Movement, attack, use, and other basic controls remain available.

| Key takeover | Mod whitelist |
| --- | --- |
| ![Key takeover: F1–F9, Q, E, T and Use real slot](docs/images/en/key-takeover.webp) | ![Mod whitelist: mods switched on keep their own keys](docs/images/en/mod-allowlist.webp) |

## See supported skills at a glance

The NeoForge edition's built-in Iron's Spells 'n Spellbooks integration adds available spells to the **Skills** page. Skill slots can display cooldowns, active states, charges, and recast counts when the integration provides them. Hovering can show the skill's own information, such as damage, range, or duration.

![The Skills page and a skill tooltip: supported spells grouped by mod; hovering shows readiness, level, mana, and damage](docs/images/en/skill-tooltip.webp)

The Iron's Spells screenshot above shows the NeoForge integration. Fabric skill entries require a separate provider.

The spell's own mod still handles casting rules, resource costs, and gameplay effects. Fingertip supplies the interface and forwards the action. Other mods can add integrations through the public API.

## Start playing

1. Install the JAR for your loader. On Fabric, also install Fabric API. Multiplayer requires the same edition on the server and participating clients.
2. Open your inventory and click the **Fingertip Incantations** logo near the upper-right corner.
3. Drag items or actions to slots, assign their shortcuts, and arrange the layout.
4. Check **Key takeover**, and in its **Mod whitelist** switch on any mods whose original shortcuts you want to keep.
5. Return to the world. Use your slot keys or open the action wheel with the middle mouse button.

See the [user guide](docs/USER_GUIDE.md) for mode differences, key conflicts, controls, and compatibility options.

## Compatibility

The NeoForge edition needs no additional library mod. The Fabric edition requires **Fabric API 0.106.0+1.21.1 or later for Minecraft 1.21.1**. Iron's Spells integration is optional and available only in the NeoForge edition. The Fabric edition retains the public skill and inventory-source APIs.

**Fabric compatibility note:** Inventory Profiles Next's automatic refill can interfere with returning an item from the offhand in multiplayer, especially with latency. Disable its automatic refill if this occurs; this interaction is not fixed in 1.2.1.

| Situation | Current behavior |
| --- | --- |
| Items in the vanilla player inventory | Supported as shortcut sources |
| A mod exposes standard Minecraft keybinds | Its functions can be assigned through the editor |
| An item needs its real inventory slot during right-click use | **Use real slot** in Key takeover exposes that slot during use (one global switch, on by default) |
| A mod reads physical input through its own system | May require a dedicated integration |
| Items inside a third-party backpack or container | Requires an inventory-source integration; containers are not automatically scanned |
| A skill from another mod | Requires a skill provider; arbitrary spells are not detected automatically |

In Fingertip mode, the mouse wheel does not select slots. Use their assigned keys, or use Mixed mode for vanilla hotbar selection.

## Development and feedback

- [Build from source](docs/BUILDING.md)
- [Skill and inventory integrations](docs/INTEGRATION.md) · [Fabric specifics](fabric/docs/INTEGRATION.md)
- [Changelog](CHANGELOG.md)
- [Source license](LICENSE) and [artwork rights](ASSET_LICENSE.md)

Bug reports are welcome through the repository's Issues tab. Include the mod, Minecraft, and loader versions, whether the issue occurs in single-player or multiplayer, relevant mods, and steps to reproduce it.

## License

The source code, build scripts, configuration data, translations, and technical documentation are licensed under the **MIT License**.

The logo, textures, and other artwork remain **All Rights Reserved**. The [asset rights statement](ASSET_LICENSE.md) permits local builds and redistribution of unmodified official releases in modpacks. Third-party components retain their own licenses; see [third-party notices](THIRD_PARTY_NOTICES.md).
