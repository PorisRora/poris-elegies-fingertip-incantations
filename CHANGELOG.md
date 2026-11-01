# Changelog

## 1.2.1

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

## 1.0.0

### First stable release

- Promote the 0.2.8-beta gameplay and interface baseline, with the final drag-swap feedback fix, to 1.0.0; network protocol remains 3.
- Keep drag-swap feedback in the bottom banner without marking either slot, and follow the dragged binding with the editor selection.
- Keep Minecraft 1.21.1, NeoForge 21.1.181+, and optional Iron's Spells 1.21-3.4.4+ support.

### Changes since the public 0.2.4-beta

- Simplify item and skill tooltips: combine the slot number and shortcut, remove redundant inventory-position/count text, and show offhand or missing-item status when relevant.
- Render item tooltips through NeoForge's item-aware tooltip path so mod-provided components, such as socket graphics, can appear normally.
- Clarify key capture, keep Backspace/Esc instructions visible beside validation errors, and cancel capture with a left click outside the box.
- While capturing a key, disable the layout toolbar except for Delete; retain Delete as an assignable key during capture.
- Clear editor selection when clicking blank canvas, use an outer selection outline, and preserve the held item's gameplay highlight.
- Return to an empty hand when the held Fingertip slot is removed, cleared, or rebound in either mode.
- Shorten key-takeover settings and tooltips; remove unused code and translation keys.

### Integration migration

- Remove the unused public-beta helper `InventorySources.name(SourceAddress)`. Integrations that called it need to use their provider's `InventorySource.name()` instead.
- External inventory sources participate in lookup and counting; direct editor browsing/binding of external inventories is not implemented yet.

## 0.2.4-beta

- Lower the optional Iron's Spells 'n Spellbooks requirement to 1.21-3.4.4 for Minecraft 1.21.1.
- Keep the NeoForge minimum at 21.1.181.
- Verify the minimum combination across skill actions, key takeover, the radial menu, and dedicated-server loading.
- Update the English and Chinese documentation. Runtime code, artwork, configuration, and protocol 3 are unchanged.

## 0.2.3-beta

### First public beta

- Customizable slots for inventory items, registered keybinds, and supported skills.
- Mixed and Fingertip hotbar modes.
- Inventory-backed equipment selection and matching-stack supply continuation.
- Configurable radial menu and key-takeover options.
- Classic, Starlight, and Poris themes, layout tools, HUD visibility, and optional durability percentages.
- Built-in Iron's Spells 'n Spellbooks integration, including supported skill states and hover details.
- Skill-provider and external-inventory integration interfaces.
- MIT-licensed source code with separately reserved artwork.

### UI refinements

- Updated the Fingertip emblem and inventory entry across all three themes.
- Kept the inventory entry free of a surrounding frame, with a gold hover outline.
- Positioned wheel-configuration hover hints at the cursor and retained descriptions for reserved sectors.
