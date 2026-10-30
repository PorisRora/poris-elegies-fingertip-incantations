package com.poris.elegies.fingertip.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.poris.elegies.fingertip.Fingertip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Generated image regions with visual item centers calibrated independently of corner ornaments. The coordinates are
 * those of the original images; TextureCrops places them in the cropped images the jar ships for some of them.
 */
final class UiSkin {
    static final Sprite SLOT = new Sprite(classicSlots(92, 93, 483, 443),
            plain("slot_starlight", 440, 432), plain("slot_poris", 498, 463));
    // Every frame of a theme keeps the same size and alpha, so a slot taken in hand changes look and never moves.
    // Classic: its atlas, and the selected frame, a recoloured copy.
    // Starlight and Poris: the selected frame set, redrawn with their own
    // ornaments. Fingertip slots pair set B's plain frame with set A's selected one, natives use set A's pair (user: "那B+A拿着
    // 吧！"): A's own Fingertip pair, and B's, changed only ornaments a few pixels wide, unreadable at 20 GUI px.
    static final Sprite SLOT_SELECTED = new Sprite(selected("slot_classic", 483, 443),
            selected("slot_starlight", 440, 432), selected("slot_poris", 498, 463));
    /** The editor's hovered frame: the atlas cell for classic; the selected frame, which the old hover cells resembled, otherwise. */
    static final Sprite SLOT_HOVER = new Sprite(classicSlots(92, 716, 483, 441),
            selected("slot_starlight", 440, 432), selected("slot_poris", 498, 463));
    /**
     * The inventory's entry button: the Fingertip logo without a frame, the same in every theme (2026-10-30, user: "而且这个
     * 入口用指尖logo是不是更好"), and with a gold edge when hovered; no disabled face ("指着可以留着 停用都删了吧"). Built by
     * the icon atlas generator at 70 px, one texel per screen pixel at GUI scale 5.
     */
    static final Region ENTRY = entry(0), ENTRY_HOVER = entry(70);
    static final Sprite NATIVE = new Sprite(wideTile("native-slots", 209, 161, 617, 557),
            plain("native_starlight", 424, 412), plain("native_poris", 628, 556));
    static final Sprite NATIVE_SELECTED = new Sprite(selected("native_classic", 617, 557),
            selected("native_starlight", 424, 412), selected("native_poris", 628, 556));
    /**
     * Each frame's rim inside the icon square, drawn over the icon (2026-10-30, user: "这个图标压在下面是不是更合适一点"):
     * made by tools/derive-fingertip-rims.java, bright texels only, so the icon shows through the frame's dark ground.
     */
    private static final Rims RIM = new Rims(new Rim("plain_slot_classic", 483, 443), new Rim("plain_slot_starlight", 440, 432), new Rim("plain_slot_poris", 498, 463));
    private static final Rims RIM_SELECTED = new Rims(new Rim("selected_slot_classic", 483, 443), new Rim("selected_slot_starlight", 440, 432), new Rim("selected_slot_poris", 498, 463));
    private static final Rims RIM_NATIVE = new Rims(new Rim("plain_native_classic", 617, 557), new Rim("plain_native_starlight", 424, 412), new Rim("plain_native_poris", 628, 556));
    private static final Rims RIM_NATIVE_SELECTED = new Rims(new Rim("selected_native_classic", 617, 557), new Rim("selected_native_starlight", 424, 412), new Rim("selected_native_poris", 628, 556));


    static boolean poris() { return Theme.current() == Theme.PORIS; }
    /** The rim of the frame {@link #slotFrame} gives, above the icon. */
    static void rim(GuiGraphics g, boolean nativeSlot, boolean selected, int size) {
        (nativeSlot ? (selected ? RIM_NATIVE_SELECTED : RIM_NATIVE) : (selected ? RIM_SELECTED : RIM)).current().draw(g, size);
    }
    /** Each theme has four base faces; item availability and cooldown are separate overlays. */
    static Sprite slotFrame(boolean nativeSlot, boolean selected) {
        return nativeSlot ? (selected ? NATIVE_SELECTED : NATIVE) : (selected ? SLOT_SELECTED : SLOT);
    }
    /** Measured inner centers: Poris native 422 vs crop 435; custom 313 vs crop 316.5. */
    static float itemOffsetY(boolean nativeSlot, int frameSize) {
        return poris() ? (nativeSlot ? -13F / 556 : -3.5F / 463) * frameSize : 0;
    }
    static void item(GuiGraphics g, ItemStack stack, int x, int y, int frameSize, boolean nativeSlot) {
        g.pose().pushPose();
        try {
            g.pose().translate(0, itemOffsetY(nativeSlot, frameSize), 0);
            g.renderItem(stack, x, y);
        } finally { g.pose().popPose(); }
    }
    /** A function icon sits where the item would, with the same per-theme frame offset. */
    static void sprite(GuiGraphics g, ActionIcons.Icon sprite, int x, int y, int frameSize, boolean nativeSlot) {
        g.pose().pushPose();
        try {
            g.pose().translate(0, itemOffsetY(nativeSlot, frameSize), 0);
            sprite.draw(g, x, y);
        } finally { g.pose().popPose(); }
    }
    private static Region plain(String name, int w, int h) {
        return region(Fingertip.id("textures/gui/themes/plain/frame_plain_" + name + ".png"), 0, 0, w, h, w, h);
    }
    private static Region selected(String name, int w, int h) {
        return region(Fingertip.id("textures/gui/themes/selected/frame_selected_" + name + ".png"), 0, 0, w, h, w, h);
    }
    private static Region entry(int v) {
        return region(Fingertip.id("textures/gui/icons/fingertip_entry.png"), 0, v, 70, 70, 70, 140);
    }
    private static Region wideTile(String file, int u, int v, int w, int h) {
        return region(Fingertip.id("textures/gui/themes/" + file + ".png"), u, v, w, h, 1774, 887);
    }
    /** A cell of the classic slot atlas (astral_v3/slots.png, its first art set). */
    private static Region classicSlots(int u, int v, int w, int h) {
        return region(Fingertip.id("textures/gui/astral_v3/slots.png"), u, v, w, h, 1254, 1254);
    }
    private static Region region(ResourceLocation texture, int u, int v, int w, int h, int aw, int ah) {
        var at = TextureCrops.area(texture, u, v, w, h, aw, ah);
        return new Region(texture, at.u(), at.v(), w, h, at.atlasWidth(), at.atlasHeight());
    }

    private record Rims(Rim classic, Rim starlight, Rim poris) {
        Rim current() { return Theme.current().pick(classic, starlight, poris); }
    }
    /**
     * A rim image covers the icon square, 2..18 of the frame's 20, cropped at whole texels of a frame width by height; these
     * texture coordinates put each texel exactly where the frame beneath has it.
     */
    private record Rim(ResourceLocation texture, int frameWidth, int frameHeight) {
        Rim(String name, int frameWidth, int frameHeight) {
            this(Fingertip.id("textures/gui/themes/rims/rim_" + name + ".png"), frameWidth, frameHeight);
        }
        void draw(GuiGraphics g, int size) {
            int x0 = Math.floorDiv(frameWidth, 10), y0 = Math.floorDiv(frameHeight, 10);
            int width = (frameWidth * 9 + 9) / 10 - x0, height = (frameHeight * 9 + 9) / 10 - y0;
            float u1 = (frameWidth / 10F - x0) / width, u2 = (frameWidth * .9F - x0) / width;
            float v1 = (frameHeight / 10F - y0) / height, v2 = (frameHeight * .9F - y0) / height;
            RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
            BlitBatch.addUv(g, texture, size / 10F, size / 10F, size * .9F, size * .9F, u1, v1, u2, v2);
        }
    }
    record Sprite(Region classic, Region starlight, Region poris) {
        void square(GuiGraphics g, int x, int y, int size) { Theme.current().pick(classic, starlight, poris).square(g, x, y, size); }
    }
    /** A rectangle of the shipped image, where TextureCrops put it. */
    record Region(ResourceLocation texture, int u, int v, int width, int height, int atlasWidth, int atlasHeight) {
        /** Square HUD frames use one draw, preserving the existing 20px hit area and 16px item origin. */
        void square(GuiGraphics g, int x, int y, int size) {
            if (size <= 0) return;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            try {
                if (!BlitBatch.add(g, texture, x, y, size, size, u, v, width, height, atlasWidth, atlasHeight))
                    g.blit(texture, x, y, size, size, u, v, width, height, atlasWidth, atlasHeight);
            } finally { RenderSystem.disableBlend(); }
        }
    }
    private UiSkin() {}
}
