# Integration guide

Fingertip exposes a client-side action interface and an inventory-source interface. The source declarations are the reference for exact signatures.

## Skills and actions

Use [FingertipApi](../src/main/java/com/poris/elegies/fingertip/api/FingertipApi.java) to register an action provider. Each action implements [FingertipAction](../src/main/java/com/poris/elegies/fingertip/api/FingertipAction.java).

| Member | Responsibility |
| --- | --- |
| `id()` | Stable namespaced identity used by saved bindings |
| `name()`, `icon()` | Display name and fallback item icon |
| `iconTexture()` | Optional square texture from the owning mod |
| `press()`, `release()` | Forward the player's input to the owning mod |
| `state()` | Lightweight display state, including cooldown, availability, active state, or charges |
| `catalogInfo()` | Catalog presentation |
| `details()` | Optional hover-only descriptions or values |

[ActionState](../src/main/java/com/poris/elegies/fingertip/api/ActionState.java) represents what the slot displays. Cache expensive state calculations; rendering may read it frequently. `details()` is read for hover information rather than every rendered frame.

The owning mod remains responsible for validating gameplay requests on its server. Fingertip forwards an input; a displayed cooldown or resource state is not a replacement for server validation.

Provider registration can also supply the names of items contributing skills and a meaningful empty-state hint. Key-switch and HUD-layer hooks allow integrations to expose relevant control options without duplicating gameplay logic.

The [built-in Iron's Spells adapter](../src/main/java/com/poris/elegies/fingertip/compat/irons/) is a working example. It reads that mod's own spell state and descriptions.

## External inventories

Implement [InventorySource](../src/main/java/com/poris/elegies/fingertip/api/inventory/InventorySource.java), then register it with [InventorySources](../src/main/java/com/poris/elegies/fingertip/api/inventory/InventorySources.java).

A provider owns its real inventory, persistence, and synchronization. Fingertip reads and operates on the source it exposes.

- Use a stable provider ID and valid provider-local slot indices.
- Expose only inventory the player is currently permitted to access.
- Return the actual mutable stack at a valid slot, rather than a detached copy.
- Keep logical client and server state separate; client reads use the provider's synchronized mirror.
- Report writes and synchronize changes through the provider contract.
- After a custom inventory-edit transaction, notify `InventorySources.committedEdit(serverPlayer)` so held-item state can be reconciled.
- Do not expose the same underlying stack twice as different writable sources.

Register an integration only when its dependencies are present. Preserve saved action IDs and provider IDs across updates so existing player bindings remain meaningful.

## Distribution

Declare Fingertip as a dependency appropriate to the integration. Do not bundle another copy of Fingertip or its reserved artwork. The API source is covered by MIT; game and branding assets have separate terms.
