# Changelog

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
