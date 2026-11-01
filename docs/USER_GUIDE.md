# User guide

[Back to README](../README.md) · [简体中文](USER_GUIDE.zh-CN.md)

## Installation

Use Minecraft Java Edition 1.21.1 with NeoForge 21.1.181 or later for that game version. Put the official Fingertip JAR in the instance's `mods` directory.

For multiplayer, install Fingertip on the server and participating clients. Use matching versions when setting up a server. Single-player already includes an integrated server.

Iron's Spells 'n Spellbooks is optional. If you install it, also install the dependencies required by its own download page.

## First layout

Open the inventory and click the Fingertip logo near the upper-right corner. The editor's item and action pages supply the things you can bind.

Drag an item or action to a slot. Drag one bound slot onto another to swap them, or drag its binding to clear space and confirm the prompt to clear it. Open **Slots** for layout editing: drag to position slots, or click a single Fingertip slot and press the shortcut you want to assign. Alignment and group-arrangement controls help organize the layout.

The HUD can show all slots, only bound slots, or be hidden. Classic, Starlight, and Poris change the visual theme without changing the controls.

While the key capture box is open, the layout toolbar is disabled except for **Delete**. Click Delete to remove the selected slot after confirmation. The Delete key itself can be assigned as a shortcut while capture is open; Backspace clears the shortcut and Esc cancels. A left click outside the box cancels capture; clicking blank canvas also clears the editor selection.

The editor uses an outer selection outline. The slot for the item currently held keeps its normal gameplay highlight.

## Slot placement and window changes

Slots use nine screen anchors and keep their spacing within groups. When the window is too small, slots remain at their saved positions and may extend off screen or overlap. Restore the window size or use the editor to rearrange them.

Adding or dropping slots within 8 GUI pixels of an existing group joins it and uses its anchor. Arrow keys only nudge the selected slots; they do not merge groups. Groups moved together keep their own grouping, while an explicit row, column, or grid arrangement groups the selected slots. An ungrouped move keeps its anchor within the same screen region and can select a new anchor when its center crosses into another region.

Use the arrow keys for one-pixel moves, or hold Shift for ten pixels. A partly off-screen group moves back in gradually; it is not snapped across an untouched axis. Press Esc during a slot drag to cancel it. Resizing the window also cancels an unfinished drag.

**Updating from 1.0.0:** Slot positions reset to the default rows and need arranging again. Item and action bindings, shortcut keys, and other settings are retained. Layouts saved by 1.1.x still load; 1.0.0 cannot read layouts saved by 1.2.1.

- A group dropped within 8 GUI pixels of one screen edge follows that edge. Dragging directly away from the edge restores anchoring by screen region; arrow-key nudges keep their existing rules.

## Choose a mode

| Mode | Vanilla nine slots | Additional slots | Selection |
| --- | --- | --- | --- |
| Mixed | Stay vanilla and move as a row | Fingertip bindings | Vanilla keys/wheel for vanilla slots; assigned keys for Fingertip slots |
| Fingertip | Become editable Fingertip slots | Fingertip bindings | Assigned slot keys; the mouse wheel does not select slots |

Turning **Enabled** off restores vanilla display and controls. This is separate from a display option that merely hides slots.

## What an item shortcut does

An item shortcut equips the referenced item. It does not automatically attack, eat, drink, cast, or place it. Use the normal attack/use controls after selection.

Press the selected slot's key again to put the item away. The binding remains available. Removing, clearing, or rebinding the held Fingertip slot also returns to an empty hand in either mode.

Equipment bindings remember a particular item, rather than picking any tool of the same type. Supply bindings find matching items and their data, ignoring stack count. Different potion effects or other differing item data do not get mixed together.

When a supply stack is used up, the mod continues with another matching stack. If none remains, the slot shows the missing item and selection ends. Once you have matching supplies again, press the slot to select them.

The default source is the player's main inventory, including the vanilla hotbar. Chests, backpack containers, armor slots, and the cursor are not automatically searched for replenishment.

## Key takeover

While the HUD is enabled, mod function keybinds are routed through Fingertip slots. A mod's original unbound shortcuts do not trigger unless you turn that mod on in **Key takeover → Mod whitelist**.

The basic movement, attack/use, inventory, chat, and other reserved controls stay available. Optional settings can take over some additional controls. The editor prevents configurations that would remove the required way back to the inventory.

To retain another mod's original shortcuts, allow that mod through. Slots that conflict with reserved controls, allowed-through controls, or another slot can be paused; their key and layout are retained. Hover the slot to read the reason and choose a nonconflicting shortcut.

Holding **Esc for three seconds** temporarily disables Fingertip until the game is restarted.

## Action wheel

The default is hold-and-release: hold the middle mouse button, point at the action, then release. You can also choose click-based operation.

Open **Wheel setup** to select a sector and assign an action. The inventory and editor sectors are reserved so those interfaces remain reachable.

## Supported skills

A skill integration supplies the icon, name, state, and action. The Skills page groups entries by their supplying mod and can show the items providing the skills.

Cooldowns, active-state indicators, charges, and recast counts appear when the integration supplies them. Hover information can include the skill's own damage, radius, or duration.

The owning mod still validates the action and performs the gameplay. Fingertip's displayed state is information, not an alternative authority over whether a skill can be used.

## Compatibility options

- **Use real slot:** a global switch in Key takeover, enabled by default. During right-click use of an item held through a Fingertip slot, other mods see the inventory slot the item is really in. It only affects that right-click; turn it off if a mod misbehaves.
- **Custom input handling:** mods that bypass Minecraft's normal keybind handling may need an integration.
- **External inventories:** third-party backpacks need an inventory-source integration. Fingertip does not automatically scan container contents.
- **Other skill mods:** support requires a skill provider; the existence of a spell item alone does not provide an integration.

## Reporting a problem

Include the Fingertip version, Minecraft and NeoForge versions, single-player or server environment, selected hotbar mode, relevant mods, exact actions that reproduce the issue, and a useful log or screenshot. For a key issue, include the key and whether the other mod is allowed through.
