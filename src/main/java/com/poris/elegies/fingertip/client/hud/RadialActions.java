package com.poris.elegies.fingertip.client.hud;

import com.poris.elegies.fingertip.client.ClientSession;
import com.poris.elegies.fingertip.api.FingertipApi;
import com.poris.elegies.fingertip.client.input.HotbarControl;
import com.poris.elegies.fingertip.client.input.ShortcutKeys;
import com.poris.elegies.fingertip.client.input.WheelActionPulse;
import com.poris.elegies.fingertip.client.input.WheelActionRef;
import com.poris.elegies.fingertip.mixin.client.KeyMappingAccess;
import com.poris.elegies.fingertip.mixin.client.MinecraftActions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class RadialActions {
    public static Component label(String id) {
        var ref = WheelActionRef.parse(id).orElse(null);
        if (ref == null) return Component.literal(id == null ? "?" : id);
        if (ref.kind() == WheelActionRef.Kind.NATIVE) return Component.translatable("poris_elegies_fingertip_incantations.wheel." + id);
        if (ref.kind() == WheelActionRef.Kind.KEY) return Component.translatable(ref.target());
        try { return FingertipApi.find(ref.target()).map(action -> action.name().copy()).orElseGet(() -> Component.literal(ref.target())); }
        catch (RuntimeException | LinkageError error) { FingertipApi.report(ref.target(), error); return Component.literal(ref.target()); }
    }
    public static ItemStack icon(String id) {
        var ref = WheelActionRef.parse(id).orElse(null);
        if (ref != null && ref.kind() == WheelActionRef.Kind.KEY)
            return KeyMappingAccess.fingertip$all().containsKey(ref.target()) ? ActionIcons.nativeKey(ref.target()) : new ItemStack(Items.BARRIER);
        if (ref != null && ref.kind() == WheelActionRef.Kind.ACTION) {
            try { return FingertipApi.find(ref.target()).map(action -> action.icon().copy()).orElseGet(() -> new ItemStack(Items.BARRIER)); }
            catch (RuntimeException | LinkageError error) { FingertipApi.report(ref.target(), error); return new ItemStack(Items.BARRIER); }
        }
        return new ItemStack(switch (id) {
            case "inventory" -> Items.CHEST;
            case "chat" -> Items.WRITABLE_BOOK;
            case "swap" -> Items.SHIELD;
            case "perspective" -> Items.ENDER_EYE;
            case "editor" -> Items.COMPASS;
            case "hud" -> Items.ITEM_FRAME;
            case "drop", "drop_stack" -> Items.HOPPER;
            case "pick" -> Items.GRASS_BLOCK;
            case "screenshot" -> Items.SPYGLASS;
            default -> Items.BARRIER;
        });
    }
    /** the bundled function icon for a native action or vanilla key, or a mod action's own texture; null draws {@link #icon}. */
    static ActionIcons.Icon sprite(String id) {
        var ref = WheelActionRef.parse(id).orElse(null);
        if (ref == null) return null;
        if (ref.kind() == WheelActionRef.Kind.ACTION) return FingertipApi.find(ref.target()).map(ActionIcons::of).orElse(null);
        if (ref.kind() == WheelActionRef.Kind.KEY)
            return KeyMappingAccess.fingertip$all().containsKey(ref.target()) ? ActionIcons.key(ref.target()) : null;
        return ActionIcons.wheel(id);
    }
    public static boolean available(String id) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isAlive() || mc.gameMode == null) return false;
        var ref = WheelActionRef.parse(id).orElse(null);
        if (ref == null) return false;
        if (ref.kind() == WheelActionRef.Kind.KEY) {
            var mapping = KeyMappingAccess.fingertip$all().get(ref.target());
            // The wheel itself is a Screen. Evaluate conflict context only after it closes in dispatch.
            return mapping != null && !mapping.getName().startsWith(ShortcutKeys.PREFIX);
        }
        if (ref.kind() == WheelActionRef.Kind.ACTION) {
            try {
                var action = FingertipApi.find(ref.target()).orElse(null);
                if (action == null) return false;
                var state = action.state();
                return state.available() && !state.lacksResource() && state.cooldown() <= 0;
            } catch (RuntimeException | LinkageError error) { FingertipApi.report(ref.target(), error); return false; }
        }
        return switch (id) {
            case "drop", "drop_stack" -> !ClientSession.inputBlocked() && !mc.player.isSpectator() && !mc.player.getMainHandItem().isEmpty();
            case "swap" -> !ClientSession.inputBlocked() && !mc.player.isSpectator();
            // 2026-10-27 (user): pick block would take the hand from a HUD slot and move the block into the hotbar.
            // It lights up only in Mixed mode with a vanilla hotbar slot chosen.
            case "pick" -> HotbarControl.allowsPickBlock() && !mc.player.isSpectator();
            case "none" -> false;
            default -> true;
        };
    }
    /** The two drops run with the wheel still open, so they can be clicked again (2026-10-28, user). */
    public static boolean repeatable(String id) { return "drop".equals(id) || "drop_stack".equals(id); }
    /** A drop clicked on the open wheel. */
    static void repeat(String id) {
        var mc = Minecraft.getInstance();
        if (repeatable(id) && mc.isWindowActive() && available(id) && mc.player.drop("drop_stack".equals(id)))
            mc.player.swing(InteractionHand.MAIN_HAND);
    }
    public static void execute(String id) {
        var mc = Minecraft.getInstance();
        if (mc.screen != null || !mc.isWindowActive()) return;
        if ("pick".equals(id) && !HotbarControl.allowsPickBlock()) { HotbarControl.refusePickBlock(); return; }
        var ref = WheelActionRef.parse(id).orElse(null);
        if (ref == null) return;
        // A skill is sent whatever its state shows, as from a HUD slot; its owner decides (2026-10-29). The wheel still
        // grays it out and says so in the hint.
        if (ref.kind() != WheelActionRef.Kind.ACTION && !available(id)) return;
        if (ref.kind() != WheelActionRef.Kind.NATIVE) { WheelActionPulse.enqueue(ref); return; }
        switch (id) {
            case "inventory" -> {
                if (mc.gameMode.isServerControlledInventory()) mc.player.sendOpenInventory();
                else { mc.getTutorial().onOpenInventory(); mc.setScreen(new InventoryScreen(mc.player)); }
            }
            case "chat" -> ((MinecraftActions)mc).fingertip$openChat("");
            case "swap" -> mc.getConnection().send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
            case "drop" -> { if (mc.player.drop(false)) mc.player.swing(InteractionHand.MAIN_HAND); }
            // The whole held stack, as Ctrl+Q (2026-10-28, user: "轮盘那边是不是应该加一个丢全部的功能"); the hand goes on
            // with the next stack like any Q that empties one.
            case "drop_stack" -> { if (mc.player.drop(true)) mc.player.swing(InteractionHand.MAIN_HAND); }
            case "perspective" -> {
                var old = mc.options.getCameraType(); mc.options.setCameraType(old.cycle());
                if (old.isFirstPerson() != mc.options.getCameraType().isFirstPerson())
                    mc.gameRenderer.checkEntityPostEffect(mc.options.getCameraType().isFirstPerson() ? mc.getCameraEntity() : null);
                mc.levelRenderer.needsUpdate();
            }
            case "editor" -> mc.setScreen(new HudEditorScreen(null, -1));
            case "hud" -> {
                HudLayout.INSTANCE.toggleDisplay();
                HudLayout.INSTANCE.save();
            }
            // Only with a vanilla hotbar slot chosen (see available). HEAD/TAIL hooks send CLEAR with the actual selected slot.
            case "pick" -> ((MinecraftActions)mc).fingertip$pickBlock();
            case "screenshot" -> Screenshot.grab(mc.gameDirectory, mc.getMainRenderTarget(),
                    message -> mc.execute(() -> mc.gui.getChat().addMessage(message)));
            default -> { }
        }
    }
    private RadialActions() {}
}
