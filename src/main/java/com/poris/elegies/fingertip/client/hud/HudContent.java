package com.poris.elegies.fingertip.client.hud;

import com.poris.elegies.fingertip.api.ActionState;
import com.poris.elegies.fingertip.api.FingertipApi;
import com.poris.elegies.fingertip.api.inventory.*;
import com.poris.elegies.fingertip.binding.HudBinding;
import com.poris.elegies.fingertip.client.ClientSession;
import com.poris.elegies.fingertip.client.inventory.InventoryFrame;
import com.poris.elegies.fingertip.client.input.HotbarControl;
import com.poris.elegies.fingertip.core.SourceResolver;
import com.poris.elegies.fingertip.hand.HandAccess;
import com.poris.elegies.fingertip.mixin.client.KeyMappingAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Read-only view of a binding, resolved for the HUD without ever acquiring or moving an item. A native key binding also
 * carries the bundled function icon in {@code sprite}, and a mod action its own texture, drawn instead of {@code icon}. A skill
 * also carries its toggle and charges (Magic Action, 2026-10-29); other slots have NONE and null, which draw nothing.
 */
public record HudContent(ItemStack icon, Component name, boolean available, Selection selection, boolean selected, int total,
                         float cooldown, Component detail, boolean missingItem, ActionIcons.Icon sprite,
                         SourceAddress source, SourceResolver.Status sourceStatus, boolean lacksResource,
                         float cooldownSeconds, ActionState.Toggle toggle, ActionState.Charges charges) {
    public enum Selection { NONE, HELD, PENDING, RESELECT }
    /** Item and native slots: no resource of their own and no cooldown time to show. */
    public HudContent(ItemStack icon, Component name, boolean available, Selection selection, boolean selected, int total,
                      float cooldown, Component detail, boolean missingItem, ActionIcons.Icon sprite,
                      SourceAddress source, SourceResolver.Status sourceStatus) {
        this(icon, name, available, selection, selected, total, cooldown, detail, missingItem, sprite, source, sourceStatus,
                false, 0, ActionState.Toggle.NONE, null);
    }
    public HudContent {
        java.util.Objects.requireNonNull(icon);
        toggle = toggle == null ? ActionState.Toggle.NONE : toggle;
    }
    public HudContent(ItemStack icon, Component name, boolean available, Selection selection, boolean selected, int total,
                      float cooldown, Component detail, boolean missingItem, ActionIcons.Icon sprite) {
        this(icon, name, available, selection, selected, total, cooldown, detail, missingItem, sprite, null, null);
    }
    public HudContent(ItemStack icon, Component name, boolean available, Selection selection, boolean selected, int total,
                      float cooldown, Component detail, boolean missingItem) {
        this(icon, name, available, selection, selected, total, cooldown, detail, missingItem, null);
    }

    /** Item names and source descriptions are needed only by hover/inspection, not ordinary drawing. */
    @Override public Component name() {
        return name != null ? name : icon.isEmpty() ? Component.translatable("poris_elegies_fingertip_incantations.empty") : icon.getHoverName();
    }
    @Override public Component detail() {
        if (detail != null) return detail;
        if (selection == Selection.PENDING) return Component.translatable("poris_elegies_fingertip_incantations.selection_pending");
        if (selection == Selection.RESELECT) return Component.translatable("poris_elegies_fingertip_incantations.selection_again");
        // 2026-10-31 (user: "在背包XX格....很冗杂"): no source slot or stack count; the slot already draws the count.
        // Only an item that is not in the backpack gets a line.
        if (sourceStatus == SourceResolver.Status.MISSING) return Component.translatable("poris_elegies_fingertip_incantations.missing");
        if (sourceStatus == SourceResolver.Status.OFFHAND) return Component.translatable("poris_elegies_fingertip_incantations.in_offhand");
        return Component.empty();
    }
    public static HudContent forFrame(int id) {
        var player = Minecraft.getInstance().player;
        HudBinding binding = ClientSession.binding(id);
        if (player == null) return empty();
        if (HotbarControl.nativeSlot(id)) {
            var inventory = player.getInventory();
            var icon = inventory.getItem(id);
            boolean selected = !HandAccess.of(inventory).extended() && inventory.selected == id;
            return new HudContent(icon, icon.isEmpty() ? Component.translatable("poris_elegies_fingertip_incantations.native_slot", id + 1) : icon.getHoverName(),
                    !icon.isEmpty(), selected ? Selection.HELD : Selection.NONE, selected, icon.getCount(),
                    icon.isEmpty() ? 0 : player.getCooldowns().getCooldownPercent(icon.getItem(), 0),
                    Component.translatable("poris_elegies_fingertip_incantations.native_slot_hint"), false);
        }
        if (binding.isItem()) {
            var hand = HandAccess.of(player.getInventory());
            var row = InventoryFrame.index().find(binding);
            var held = hand.extended() && ClientSession.selectedHud() == id ? hand.address() : null;
            var match = row.source(held);
            var status = row.status(); int total = row.displayTotal();
            SourceAddress source = match == null ? null : match.address();
            ItemStack icon = match == null ? binding.template() : match.stack();
            // 2026-10-27 (user): an item only in the off hand is not dimmed, because this slot's key takes it back.
            boolean inOffhand = status == SourceResolver.Status.OFFHAND && binding.matches(player.getOffhandItem());
            if (inOffhand) icon = player.getOffhandItem();
            boolean available = status == SourceResolver.Status.READY || inOffhand;
            // The current local choice, predicted or confirmed (the reply corrects it); missing stock is never selected.
            boolean selected = available && hand.extended() && ClientSession.selectedHud() == id;
            Selection selection = ClientSession.waitingHud() == id ? Selection.PENDING
                    : selected && available
                    && !hand.lost() && hand.at(player.getInventory(), source) ? Selection.HELD
                    : available && ClientSession.selectedHud() == id && hand.lost() ? Selection.RESELECT : Selection.NONE;
            float cooldown = icon.isEmpty() ? 0 : player.getCooldowns().getCooldownPercent(icon.getItem(), 0);
            return new HudContent(icon, null, available, selection, selected, total, cooldown, null,
                    status == SourceResolver.Status.MISSING, null, source, status);
        }
        if (binding.kind() == HudBinding.Kind.MAPPING) {
            boolean found = KeyMappingAccess.fingertip$all().containsKey(binding.action());
            return new HudContent(ActionIcons.nativeKey(binding.action()), Component.translatable(binding.action()), found, Selection.NONE, false, 0, 0,
                    Component.translatable(found ? "poris_elegies_fingertip_incantations.native_action" : "poris_elegies_fingertip_incantations.unavailable"), false,
                    found ? ActionIcons.key(binding.action()) : null);
        }
        if (binding.kind() == HudBinding.Kind.ACTION) {
            try {
                var action = FingertipApi.find(binding.action()).orElse(null);
                if (action != null) {
                    var state = action.state();
                    return new HudContent(action.icon(), action.name(), state.available() && !state.lacksResource(), Selection.NONE, false, 0,
                            state.cooldown(), state.reason(), false, ActionIcons.of(action), null, null,
                            state.available() && state.lacksResource(), state.cooldownSeconds(), state.toggle(), state.charges());
                }
            } catch (RuntimeException | LinkageError error) { FingertipApi.report(binding.action(), error); }
            return new HudContent(new ItemStack(Items.BARRIER), Component.literal(binding.action()), false, Selection.NONE, false, 0, 0,
                    Component.translatable("poris_elegies_fingertip_incantations.unavailable"), false);
        }
        return new HudContent(ItemStack.EMPTY, Component.translatable("poris_elegies_fingertip_incantations.unbound"), false, Selection.NONE, false, 0, 0,
                Component.translatable("poris_elegies_fingertip_incantations.empty_selection_hint"), false);
    }
    /** The bound action's own detail lines, for hover text only: read on demand, never per frame (0.2.2-beta). */
    static java.util.List<Component> details(int id) {
        var binding = ClientSession.binding(id);
        if (HotbarControl.nativeSlot(id) || binding.kind() != HudBinding.Kind.ACTION) return java.util.List.of();
        try { return FingertipApi.find(binding.action()).map(ActionCatalog::details).orElse(java.util.List.of()); }
        catch (RuntimeException | LinkageError error) { FingertipApi.report(binding.action(), error); return java.util.List.of(); }
    }
    private static HudContent empty() { return new HudContent(ItemStack.EMPTY, Component.translatable("poris_elegies_fingertip_incantations.unbound"), false, Selection.NONE, false, 0, 0, Component.empty(), false); }
}
