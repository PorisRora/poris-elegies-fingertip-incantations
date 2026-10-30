package com.poris.elegies.fingertip.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.poris.elegies.fingertip.Fingertip;
import com.poris.elegies.fingertip.api.FingertipAction;
import com.poris.elegies.fingertip.api.FingertipApi;
import com.poris.elegies.fingertip.mixin.client.KeyMappingAccess;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Native actions and every vanilla key draw the bundled function icons (textures/gui/icons/function_icons.png, made by
 * the icon atlas generator; the vanilla keys and drop-stack since 2026-10-29); a vanilla key without one
 * would keep a recognisable item, other mods' keys get the generic mod icon. Mod API actions supply their own item or
 * texture.
 */
final class ActionIcons {
    private static final ResourceLocation ATLAS = Fingertip.id("textures/gui/icons/function_icons.png");
    private static final ResourceLocation SMALL_ATLAS = Fingertip.id("textures/gui/icons/function_icons_small.png");
    /** Each drawn size has its own reduction, one texel per screen pixel at the user's GUI scale 5: 16 px -> 80, 12 px -> 60. */
    private static final int CELL = 80, SMALL_CELL = 60, COLUMNS = 4;
    private static final int ROWS = (Sprite.values().length + COLUMNS - 1) / COLUMNS;
    static final int SMALL = 12;
    private static final Set<String> VANILLA_CATEGORIES = Set.of("key.categories.movement", "key.categories.gameplay",
            "key.categories.inventory", "key.categories.creative", "key.categories.multiplayer", "key.categories.ui",
            "key.categories.misc");

    /** Whatever is drawn in an item's 16 px square instead of the item: a function icon or a mod action's own texture. */
    interface Icon { void draw(GuiGraphics g, int x, int y); }

    /** In the atlas order of ORDER in the icon atlas generator, four per row. */
    enum Sprite implements Icon {
        FINGERTIP, SLOTS, DISPLAY, KEYS, WHEEL, INVENTORY, CHAT, SCREENSHOT,
        PERSPECTIVE, SWAP_HANDS, DROP, PICK_BLOCK, TOGGLE_HUD, EMPTY, EDITOR, MOD_FUNCTION,
        // The additional native-action icons, appended after the first sixteen.
        DROP_STACK, MOVE_FORWARD, MOVE_BACK, MOVE_LEFT, MOVE_RIGHT, JUMP, SNEAK, SPRINT, ATTACK, USE,
        HOTBAR_SLOT, SAVE_TOOLBAR, LOAD_TOOLBAR, PLAYER_LIST, SOCIAL, SPECTATOR_OUTLINES, SMOOTH_CAMERA,
        FULLSCREEN, ADVANCEMENTS;
        @Override public void draw(GuiGraphics g, int x, int y) { blit(g, ATLAS, CELL, 16, x, y); }
        /** The 12 px icon for toolbar buttons and titles. */
        void drawSmall(GuiGraphics g, int x, int y) { blit(g, SMALL_ATLAS, SMALL_CELL, SMALL, x, y); }
        private void blit(GuiGraphics g, ResourceLocation atlas, int cell, int size, int x, int y) {
            RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
            try {
                g.blit(atlas, x, y, size, size, ordinal() % COLUMNS * cell, ordinal() / COLUMNS * cell, cell, cell,
                        cell * COLUMNS, cell * ROWS);
            } finally { RenderSystem.disableBlend(); }
        }
    }
    /** {@link FingertipAction#iconTexture()}: the whole texture, whatever its size, in 16 px. */
    record Texture(ResourceLocation location) implements Icon {
        @Override public void draw(GuiGraphics g, int x, int y) {
            RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
            try { g.blit(location, x, y, 16, 16, 0, 0, 1, 1, 1, 1); }
            finally { RenderSystem.disableBlend(); }
        }
    }
    /** A mod API action's texture icon, or null to draw its item; a provider that throws here keeps its item. */
    static Icon of(FingertipAction action) {
        try {
            var texture = action.iconTexture();
            return texture == null ? null : new Texture(texture);
        } catch (RuntimeException | LinkageError error) {
            FingertipApi.report("icon:" + action.getClass().getName(), error);
            return null;
        }
    }

    /**
     * A hotbar key's icon: the empty slot frame with its number over it (the frame excludes its number, added in
     * 2026-10-28). A record, so the editor's change check sees two of the same number as equal.
     */
    record HotbarSlot(int number) implements Icon {
        @Override public void draw(GuiGraphics g, int x, int y) {
            Sprite.HOTBAR_SLOT.draw(g, x, y);
            var font = Minecraft.getInstance().font;
            String text = Integer.toString(number);
            g.drawString(font, text, x + 8 - font.width(text) / 2, y + 4, 0xFFF1E3BC, true);
        }
    }
    /** The wheel's own actions, by their InputPreferences id. */
    static Icon wheel(String id) {
        return switch (id) {
            case "inventory" -> Sprite.INVENTORY;
            case "chat" -> Sprite.CHAT;
            case "swap" -> Sprite.SWAP_HANDS;
            case "perspective" -> Sprite.PERSPECTIVE;
            case "editor" -> Sprite.FINGERTIP;
            case "hud" -> Sprite.TOGGLE_HUD;
            case "drop" -> Sprite.DROP;
            case "drop_stack" -> Sprite.DROP_STACK;
            case "pick" -> Sprite.PICK_BLOCK;
            case "screenshot" -> Sprite.SCREENSHOT;
            case "none" -> Sprite.EMPTY;
            default -> null;
        };
    }
    /** A key mapping's icon; null keeps the item from {@link #nativeKey}. */
    static Icon key(String mapping) {
        if (mapping.startsWith("key.hotbar.")) {
            try { return new HotbarSlot(Integer.parseInt(mapping.substring("key.hotbar.".length()))); }
            catch (NumberFormatException ignored) { }
        }
        var sprite = switch (mapping) {
            case "key.forward" -> Sprite.MOVE_FORWARD;
            case "key.back" -> Sprite.MOVE_BACK;
            case "key.left" -> Sprite.MOVE_LEFT;
            case "key.right" -> Sprite.MOVE_RIGHT;
            case "key.jump" -> Sprite.JUMP;
            case "key.sneak" -> Sprite.SNEAK;
            case "key.sprint" -> Sprite.SPRINT;
            case "key.attack" -> Sprite.ATTACK;
            case "key.use" -> Sprite.USE;
            case "key.inventory" -> Sprite.INVENTORY;
            case "key.chat", "key.command" -> Sprite.CHAT;
            case "key.screenshot" -> Sprite.SCREENSHOT;
            case "key.togglePerspective" -> Sprite.PERSPECTIVE;
            case "key.swapOffhand" -> Sprite.SWAP_HANDS;
            case "key.drop" -> Sprite.DROP;
            case "key.pickItem" -> Sprite.PICK_BLOCK;
            case "key.saveToolbarActivator" -> Sprite.SAVE_TOOLBAR;
            case "key.loadToolbarActivator" -> Sprite.LOAD_TOOLBAR;
            case "key.playerlist" -> Sprite.PLAYER_LIST;
            case "key.socialInteractions" -> Sprite.SOCIAL;
            case "key.spectatorOutlines" -> Sprite.SPECTATOR_OUTLINES;
            case "key.smoothCamera" -> Sprite.SMOOTH_CAMERA;
            case "key.fullscreen" -> Sprite.FULLSCREEN;
            case "key.advancements" -> Sprite.ADVANCEMENTS;
            default -> null;
        };
        if (sprite != null) return sprite;
        var found = KeyMappingAccess.fingertip$all().get(mapping);
        return found == null || VANILLA_CATEGORIES.contains(found.getCategory()) ? null : Sprite.MOD_FUNCTION;
    }
    static ItemStack nativeKey(String id) {
        return new ItemStack(switch (id) {
            case "key.jump" -> Items.FEATHER;
            case "key.sprint" -> Items.RABBIT_FOOT;
            case "key.sneak" -> Items.LEATHER_BOOTS;
            case "key.inventory" -> Items.CHEST;
            case "key.swapOffhand" -> Items.SHIELD;
            case "key.drop" -> Items.HOPPER;
            case "key.chat", "key.command" -> Items.WRITABLE_BOOK;
            case "key.advancements" -> Items.KNOWLEDGE_BOOK;
            case "key.attack" -> Items.IRON_SWORD;
            case "key.use" -> Items.ENDER_PEARL;
            case "key.pickItem" -> Items.ITEM_FRAME;
            case "key.playerlist" -> Items.PLAYER_HEAD;
            default -> Items.COMPASS;
        });
    }
    /** Draws the sprite when there is one, else the item. */
    static void draw(GuiGraphics g, Icon sprite, ItemStack stack, int x, int y) {
        if (sprite != null) sprite.draw(g, x, y);
        else if (!stack.isEmpty()) g.renderItem(stack, x, y);
    }
    /**
     * Like {@link #draw}, but a dimmed sprite is darkened itself: the shade box callers lay over items would show as a
     * square around its transparent edge. The empty-slot ring is already faint and stays as drawn. Returns whether a
     * sprite was drawn, so the caller shades only items.
     */
    static boolean draw(GuiGraphics g, Icon sprite, ItemStack stack, int x, int y, boolean dim) {
        if (sprite == null) { draw(g, null, stack, x, y); return false; }
        if (dim && sprite != Sprite.EMPTY) g.setColor(.5F, .5F, .55F, 1F);
        try { sprite.draw(g, x, y); }
        finally { g.setColor(1, 1, 1, 1); }
        return true;
    }
    private ActionIcons() {}
}
