# Fabric integration

The public packages remain `com.poris.elegies.fingertip.api` and `com.poris.elegies.fingertip.api.inventory`. Compile against the Fabric artifact for Minecraft 1.21.1. Names below use official Mojang mappings; Loom can remap the dependency for projects using Yarn.

Register client providers in your `ClientModInitializer` using `FingertipApi.registerProvider(providerId, provider)`. Each `FingertipAction` supplies its stable ID, translated name, icon, state and press/release behavior. Cooldown, charges, toggle state, tooltips, skill-source names and empty hints retain the existing API. A provider is responsible for listing only actions currently available to the player.

To use Fingertip's network relay, register `FingertipServerApi.registerHandler("your_mod_id", handler)` from your common `ModInitializer`. On the client, call `FingertipApi.sendToServer(actionId, pressed)`. The handler runs on the server thread. The action ID namespace selects the registered handler; the owning mod must validate knowledge, equipment, costs, cooldown and all gameplay effects. Client UI availability is not authorization.

Inventory integrations implement `InventorySource` and register through `InventorySources.register`. Keep provider IDs and slot addresses stable. The inventory owner synchronizes its own contents; Fingertip synchronizes only its binding and selected-source state.

`KeySwitch` still controls standard registered key bindings. A private raw keyboard or mouse hook does not become a supported skill automatically: expose a `FingertipAction` for it. Fabric key bindings are dispatched through their click and held states; the port does not broadcast a synthetic physical key to unrelated listeners.

`LayerSwitch` registration and saved preferences remain available. Fabric 1.21.1's HUD callback has no named-layer cancellation event, so a cooperating renderer must check `FingertipApi.shouldRenderLayer(layerId)` before drawing that layer. The method returns true when Fingertip is not suppressing the layer.

This build includes no Iron's Spells adapter. The integration API is usable without a built-in magic-mod integration.
