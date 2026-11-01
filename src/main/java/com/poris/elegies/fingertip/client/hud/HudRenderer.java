package com.poris.elegies.fingertip.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.poris.elegies.fingertip.Fingertip;
import com.poris.elegies.fingertip.api.ActionState;
import com.poris.elegies.fingertip.client.ClientSession;
import com.poris.elegies.fingertip.client.input.InputRouter;
import com.poris.elegies.fingertip.client.input.HotbarControl;
import net.minecraft.Util;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ItemDecoratorHandler;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = Fingertip.MOD_ID, value = Dist.CLIENT)
public final class HudRenderer {
    @SubscribeEvent public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, Fingertip.id("shortcuts"), HudRenderer::render);
    }
    public static boolean replacesHotbar() {
        var mc = Minecraft.getInstance();
        return InputRouter.enabled() && mc.player != null && !mc.player.isSpectator();
    }
    private static void render(GuiGraphics g, DeltaTracker delta) {
        var mc = Minecraft.getInstance();
        if (!replacesHotbar() || mc.options.hideGui || HudVisibility.mode() == HudVisibility.Mode.HIDDEN
                || mc.screen instanceof HudEditorScreen
                || mc.screen instanceof DisplaySettingsScreen || mc.screen instanceof ModWhitelistScreen) return;
        // Pass by pass over the slots rather than slot by slot, so frames, shading, bars, labels and key glyphs each go
        // to the GPU together instead of one draw call per piece (2026-10-28). Each kind is still
        // drawn after the kinds beneath it, as within one slot before, so the picture is the same.
        // Positions are worked out once here (指尖定位法, cached until the screen or the layout changes); the passes read them.
        var layout = HudLayout.INSTANCE;
        var offhandSlot = layout.offhand();
        layout.ensurePlaced(g.guiWidth(), g.guiHeight());
        var slots = new java.util.ArrayList<HudLayout.Slot>();
        for (var slot : layout.activeSlots()) if (HudVisibility.includes(slot.id)) slots.add(slot);
        var views = new HudContent[slots.size()];
        for (int i = 0; i < views.length; i++) views[i] = HudContent.forFrame(slots.get(i).id);
        BlitBatch.run(() -> pass(g, slots, views, Pass.FRAMES));
        pass(g, slots, views, Pass.ICONS);
        g.drawManaged(() -> pass(g, slots, views, Pass.SHADING));
        BlitBatch.run(() -> pass(g, slots, views, Pass.RIM));
        pass(g, slots, views, Pass.ORBIT);
        g.drawManaged(() -> pass(g, slots, views, Pass.BARS));
        g.drawManaged(() -> pass(g, slots, views, Pass.LABELS));
        BlitBatch.run(() -> pass(g, slots, views, Pass.KEYS));
        pass(g, slots, views, Pass.LAST);
        int width = g.guiWidth(), height = g.guiHeight();
        // The off hand keeps its own saved place and shows only while it holds something (2026-10-26, user).
        int offhandX = offhandSlot.placedX, offhandY = offhandSlot.placedY;
        renderOffhand(g, offhandX, offhandY, false);
        if (mc.options.attackIndicator().get() != AttackIndicatorStatus.HOTBAR) return;
        float charge = mc.player.getAttackStrengthScale(0);
        if (charge >= 1) return;
        // Slot positions are gathered only while the bar is drawn. It still keeps clear of the off-hand place, so it
        // does not jump when an item goes into the off hand.
        int side = mc.player.getMainArm() == HumanoidArm.RIGHT ? -1 : 1;
        var offhandPoint = new HudArrangement.Point(offhandX, offhandY);
        var slotPoints = HudArrangement.positions(gameplaySlots(), width, height);
        var occupied = new java.util.ArrayList<>(slotPoints);
        occupied.add(offhandPoint);
        var anchor = slotPoints.isEmpty() ? java.util.List.of(offhandPoint) : slotPoints;
        var position = HudArrangement.auxiliary(anchor, occupied, -side, width, height);
        if (position == null) return;
        int ax = position.x() + (side < 0 ? 0 : 16), y = position.y();
        g.fill(ax, y, ax + 4, y + 20, 0xC0202028);
        g.fill(ax, y + (int)(20 * (1 - charge)), ax + 4, y + 20, 0xFFE6DBB1);
    }
    /**
     * The real off-hand item in its frame. Gameplay skips an empty hand; the layout editor draws the empty frame with
     * vanilla's faint shield outline so it can still be found and moved.
     */
    static void renderOffhand(GuiGraphics g, int x, int y, boolean editing) {
        var mc = Minecraft.getInstance();
        var item = mc.player == null ? ItemStack.EMPTY : mc.player.getOffhandItem();
        if (item.isEmpty() && !editing) return;
        frame(g, x, y, false, false, false);
        if (!item.isEmpty()) {
            UiSkin.item(g, item, x + 2, y + 2, 20, false);
            g.pose().pushPose(); g.pose().translate(x, y, 200);
            try { UiSkin.rim(g, false, false, 20); } finally { g.pose().popPose(); }
            g.renderItemDecorations(mc.font, item, x + 2, y + 2);
            return;
        }
        var shield = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
        g.pose().pushPose(); g.pose().translate(0, UiSkin.itemOffsetY(false, 20), 0);
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); g.setColor(1, 1, 1, .6F);
        try { g.blit(x + 2, y + 2, 0, 16, 16, shield); }
        finally { g.setColor(1, 1, 1, 1); RenderSystem.disableBlend(); g.pose().popPose(); }
    }
    /**
     * Every slot as in gameplay, the one in hand lit. The editor marks its own selection with the outline around the slot,
     * never with the selected frame, which only means "in hand" (2026-10-31).
     */
    public static void renderSlots(GuiGraphics g) {
        HudLayout.INSTANCE.ensurePlaced(g.guiWidth(), g.guiHeight());
        for (var slot : HudLayout.INSTANCE.activeSlots()) renderAt(g, slot.id, slot.placedX, slot.placedY);
    }
    /** Filtering never deletes bindings, changes coordinates or hides remembered missing items as empty. */
    public static java.util.List<HudLayout.Slot> gameplaySlots() {
        return HudLayout.INSTANCE.activeSlots().stream().filter(slot -> HudVisibility.includes(slot.id)).toList();
    }
    /** Frame, icon, overlays and labels always share one local origin, in both editor and gameplay. */
    public static void renderAt(GuiGraphics g, int id, int x, int y) {
        draw(g, id, HudContent.forFrame(id), x, y, Pass.ALL);
    }
    /** The gameplay HUD's passes, bottom to top; the editor draws each slot whole (ALL), in the same order. */
    private enum Pass { ALL, FRAMES, ICONS, SHADING, RIM, ORBIT, BARS, LABELS, KEYS, LAST }
    private static void pass(GuiGraphics g, java.util.List<HudLayout.Slot> slots, HudContent[] views, Pass pass) {
        for (int i = 0; i < views.length; i++) {
            var slot = slots.get(i);
            draw(g, slot.id, views[i], slot.placedX, slot.placedY, pass);
        }
    }
    private static void draw(GuiGraphics g, int id, HudContent view, int x, int y, Pass pass) {
        boolean all = pass == Pass.ALL;
        g.pose().pushPose();
        try {
            g.pose().translate(x, y, 0);
            g.setColor(1, 1, 1, 1);
            boolean nativeSlot = HotbarControl.nativeSlot(id);
            boolean paused = HotbarControl.shortcutPaused(id);
            // The slot in hand takes the selected frame, a recoloured copy of the plain one (2026-10-29: no lift, no
            // overlay). A skill that is on keeps the plain frame and a light runs round it (orbit).
            boolean on = view.toggle() == ActionState.Toggle.ON, lit = view.selected();
            if (all || pass == Pass.FRAMES) frame(g, 0, 0, nativeSlot, lit, paused);
            if (all || pass == Pass.ICONS) {
                if (view.sprite() != null) UiSkin.sprite(g, view.sprite(), 2, 2, 20, nativeSlot);
                else if (!view.icon().isEmpty()) UiSkin.item(g, view.icon(), 2, 2, 20, nativeSlot);
            }
            var charges = view.charges();
            if (all || pass == Pass.SHADING) {
                // With every charge spent, the next one coming back reads as a cooldown.
                float refill = charges != null && charges.current() == 0 && charges.progress() >= 0 ? 1 - charges.progress() : 0;
                shadeIcon(g, nativeSlot, !view.available() && !view.lacksResource(), paused, Math.max(view.cooldown(), refill),
                        view.lacksResource());
            }
            // The frame's rim over the icon, every slot, so icons sit inside their frames (2026-10-30, user: "这个图标压在下面
            // 是不是更合适一点"); one batch for all slots.
            if (all || pass == Pass.RIM) {
                float brightness = paused ? .45F : 1;
                g.pose().pushPose(); g.pose().translate(0, 0, 200); g.setColor(brightness, brightness, brightness, 1);
                try { UiSkin.rim(g, nativeSlot, lit, 20); }
                finally { g.setColor(1, 1, 1, 1); g.pose().popPose(); }
            }
            if (on && (all || pass == Pass.ORBIT)) {
                g.pose().pushPose(); g.pose().translate(0, 0, 200);
                try { orbit(g, paused); } finally { g.pose().popPose(); }
            }
            if (charges != null && (all || pass == Pass.BARS) && charges.current() > 0 && charges.current() < charges.max()
                    && charges.progress() >= 0) refillBar(g, charges.progress(), paused);
            boolean totalCount = view.total() > view.icon().getCount();
            if (!view.icon().isEmpty()) {
                if (all || pass == Pass.BARS) bar(g, view.icon(), paused);
                // One number area: the combined count replaces the stack count instead of colliding with it. Missing
                // equipment shows no 0: it is one particular item, already dimmed, not a count (2026-10-28, user).
                boolean equipment = ClientSession.binding(id).kind() == com.poris.elegies.fingertip.binding.HudBinding.Kind.EQUIPMENT;
                if (all || pass == Pass.LABELS)
                    count(g, view.icon(), totalCount ? "" : view.missingItem() && !equipment ? "0" : null, paused);
                if (all || pass == Pass.LAST) ItemDecoratorHandler.of(view.icon()).render(g, Minecraft.getInstance().font, view.icon(), 2, 2);
            }
            if (!all && pass != Pass.KEYS && pass != Pass.LABELS && pass != Pass.LAST) return;
            var key = HotbarControl.displayKey(id);
            if (all || pass == Pass.KEYS) ShortcutKeyBadge.drawLegend(g, key, paused);
            if (all || pass == Pass.LABELS) {
                boolean reselect = view.selection() == HudContent.Selection.RESELECT;
                boolean pending = view.selection() == HudContent.Selection.PENDING;
                if (totalCount) total(g, view.total(), paused ? ThemePalette.PAUSED : ThemePalette.TOTAL);
                if (view.cooldownSeconds() > 0) cooldownSeconds(g, view.cooldownSeconds(), paused);
                if (charges != null) charges(g, charges, paused);
                // Bound slots retain frame, icon and count cues; free corners can show unbound status.
                if (key.isUnbound() && !nativeSlot) {
                    if (pending) small(g, "…", 14, 1, ThemePalette.PENDING);
                    // Short of a resource already reads blue; "!" stays the mark of a slot that cannot be used.
                    else if (!view.available() && !view.lacksResource() && !view.icon().isEmpty()) small(g, "!", 15, 1, ThemePalette.ERROR);
                    else if (reselect) small(g, "↻", 14, 1, ThemePalette.AGAIN);
                }
            }
            if (all || pass == Pass.LAST) BindingPresentation.slotFeedback(g, id);
        } finally {
            g.pose().popPose();
            g.setColor(1, 1, 1, 1);
        }
    }
    /** A Fingertip slot's frame, plain or selected; availability is HudContent's and drawn over it. */
    public static void frame(GuiGraphics g, int x, int y, boolean selected) {
        frame(g, x, y, false, selected, false);
    }
    static void frame(GuiGraphics g, int x, int y, boolean nativeSlot, boolean selected, boolean paused) {
        float brightness = paused ? .45F : 1;
        g.setColor(brightness, brightness, brightness, 1);
        try { UiSkin.slotFrame(nativeSlot, selected).square(g, x, y, 20); }
        finally { g.setColor(1, 1, 1, 1); }
    }
    /**
     * A skill that is on: a light runs round its frame, a lap every 2 s, with a tail fading over one side (2026-10-29, user:
     * "能不能走边框转圈圈的那种效果", after the earlier static pattern: "效果有点差"). A GUI px wide, it runs along the rim, 1 GUI px in, in
     * whole screen pixels so it stays sharp at any GUI scale: white at the head, then the theme's tail colours
     * ({@link ThemePalette#orbitNear} to {@link ThemePalette#orbitFar}), so every theme has it without art. Clockwise from
     * the top left. One managed batch per slot.
     */
    private static void orbit(GuiGraphics g, boolean paused) {
        int gui = GuiPixels.guiScale();
        int side = 18 * gui, perimeter = side * 4, tail = side, thick = gui, half = thick / 2;
        int head = (int)(Util.getMillis() % 2000 * perimeter / 2000);
        var palette = EditorTheme.palette();
        int near = palette.orbitNear(), far = palette.orbitFar(), whiteHead = gui * 3;
        g.pose().pushPose(); g.pose().scale(1F / gui, 1F / gui, 1);
        try {
            g.drawManaged(() -> {
                for (int d = 0; d < tail; d++) {
                    int s = Math.floorMod(head - d, perimeter), edge = s / side, along = s % side;
                    float fade = 1 - d / (float)tail;
                    int alpha = Math.round(255 * fade * (paused ? .45F : 1));
                    int color = alpha << 24 | (d < whiteHead ? 0xFFFFFF : mix(near, far, (d - whiteHead) / (float)(tail - whiteHead)) & 0xFFFFFF);
                    // One screen pixel along the path, thick across it; the path is the square from 1 to 19 GUI px.
                    int x = gui + switch (edge) { case 0 -> along; case 1 -> side; case 2 -> side - along; default -> 0; };
                    int y = gui + switch (edge) { case 0 -> 0; case 1 -> along; case 2 -> side; default -> side - along; };
                    if (edge % 2 == 0) g.fill(x, y - half, x + 1, y - half + thick, color);
                    else g.fill(x - half, y, x - half + thick, y + 1, color);
                }
            });
        } finally { g.pose().popPose(); }
    }
    private static int mix(int from, int to, float t) {
        int r = Math.round((from >> 16 & 0xFF) + ((to >> 16 & 0xFF) - (from >> 16 & 0xFF)) * t);
        int gr = Math.round((from >> 8 & 0xFF) + ((to >> 8 & 0xFF) - (from >> 8 & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return 0xFF000000 | r << 16 | gr << 8 | b;
    }
    static void shadeIcon(GuiGraphics g, boolean nativeSlot, boolean missing, boolean paused, double cooldown) {
        shadeIcon(g, nativeSlot, missing, paused, cooldown, false);
    }
    /** Short of mana or another resource reads blue rather than the gray of a skill that cannot be used (2026-10-28). */
    static void shadeIcon(GuiGraphics g, boolean nativeSlot, boolean missing, boolean paused, double cooldown, boolean lacksResource) {
        // Tint the existing frame and shade only the inner item area, preserving transparent ornaments.
        g.pose().pushPose(); g.pose().translate(0, UiSkin.itemOffsetY(nativeSlot, 20), 200);
        try {
            if (missing || paused) g.fill(2, 2, 18, 18, missing ? 0xB810111A : 0x8810111A);
            else if (lacksResource) g.fill(2, 2, 18, 18, 0x90183A8C);
            int shaded = (int)Math.ceil(16 * Math.clamp(cooldown, 0, 1));
            if (shaded > 0) g.fill(2, 18 - shaded, 18, 18, 0xA0201D30);
        } finally { g.pose().popPose(); }
    }
    /** Native durability/count/decorator support, with cooldown drawn once by this HUD above the icon. */
    private static void decorations(GuiGraphics g, ItemStack stack, String countOverride) {
        decorations(g, stack, countOverride, false);
    }
    private static void decorations(GuiGraphics g, ItemStack stack, String countOverride, boolean paused) {
        bar(g, stack, paused); count(g, stack, countOverride, paused);
        ItemDecoratorHandler.of(stack).render(g, Minecraft.getInstance().font, stack, 2, 2);
    }
    private static void bar(GuiGraphics g, ItemStack stack, boolean paused) {
        if (!stack.isBarVisible() || DurabilityPercent.shows(stack)) return;
        g.pose().pushPose(); g.pose().translate(0, 0, 200);
        try {
            g.fill(RenderType.guiOverlay(), 4, 15, 17, 17, 0xFF000000);
            int barColor = paused ? (stack.getBarColor() & 0xFEFEFE) >> 1 : stack.getBarColor();
            g.fill(RenderType.guiOverlay(), 4, 15, 4 + stack.getBarWidth(), 16, barColor | 0xFF000000);
        } finally { g.pose().popPose(); }
    }
    private static void count(GuiGraphics g, ItemStack stack, String countOverride, boolean paused) {
        if (stack.getCount() == 1 && countOverride == null) return;
        var font = Minecraft.getInstance().font;
        String count = countOverride == null ? Integer.toString(stack.getCount()) : countOverride;
        g.pose().pushPose();
        try {
            g.pose().translate(18 - font.width(count) * .75F, 12, 200);
            g.pose().scale(.75F, .75F, 1);
            g.drawString(font, count, 0, 0, paused ? ThemePalette.PAUSED : 0xFFFFFFFF, true);
        } finally { g.pose().popPose(); }
    }
    /**
     * Presses left, where an item's count goes (a skill slot has none). Larger than item counts and outlined (2026-10-29,
     *): about 0.8, at a whole number of screen pixels per font pixel (4 at GUI scale 5). Gold
     * while a recast window is open, white for stored charges that refill ("再施放金色、充能白色"), gray at 0.
     */
    private static void charges(GuiGraphics g, ActionState.Charges charges, boolean paused) {
        var font = Minecraft.getInstance().font;
        String text = Integer.toString(charges.current());
        float scale = GuiPixels.scale(.8F);
        int color = paused || charges.current() == 0 ? ThemePalette.PAUSED : charges.progress() < 0 ? ThemePalette.AGAIN : 0xFFFFFFFF;
        g.pose().pushPose();
        try {
            g.pose().translate(18 - font.width(text) * scale, 18 - 7 * scale, 200);
            g.pose().scale(scale, scale, 1);
            GuiPixels.outlined(g, font, text, 0, 0, color, GuiPixels.OUTLINE);
        } finally { g.pose().popPose(); }
    }
    /** The next charge coming back while some are left: a thin bar where an item's durability goes. */
    private static void refillBar(GuiGraphics g, float progress, boolean paused) {
        g.pose().pushPose(); g.pose().translate(0, 0, 200);
        try {
            g.fill(RenderType.guiOverlay(), 4, 15, 17, 17, 0xFF000000);
            g.fill(RenderType.guiOverlay(), 4, 15, 4 + Math.round(13 * progress), 16, paused ? ThemePalette.PAUSED : ThemePalette.PENDING);
        } finally { g.pose().popPose(); }
    }
    /** Seconds left, centered over the icon: whole seconds, tenths under three. A dark outline keeps them legible on pale icons. */
    private static void cooldownSeconds(GuiGraphics g, float seconds, boolean paused) {
        var font = Minecraft.getInstance().font;
        String text = seconds < 3 ? String.format(java.util.Locale.ROOT, "%.1f", seconds) : String.valueOf((int)Math.ceil(seconds));
        int x = Math.round(10 - font.width(text) / 4F);
        g.pose().pushPose(); g.pose().translate(x, 8, 200); g.pose().scale(.5F, .5F, 1);
        try { GuiPixels.outlined(g, font, text, 0, 0, paused ? ThemePalette.PAUSED : 0xFFFFFFFF, GuiPixels.OUTLINE); }
        finally { g.pose().popPose(); }
    }
    /**
     * The combined count, a little larger than the other small labels (2026-10-28): 0.6
     * instead of 0.5, three screen pixels per font pixel at the user's GUI scale 5, so it stays even. Four digits fit.
     */
    private static void total(GuiGraphics g, int total, int color) {
        var font = Minecraft.getInstance().font;
        String text = "Σ" + total;
        float scale = .6F, x = 1.2F;
        text = TextLines.cut(text, (int)((19 - x) / scale) + 1, font::width);
        g.pose().pushPose(); g.pose().translate(x, 11.4F, 200); g.pose().scale(scale, scale, 1);
        g.drawString(font, text, 0, 0, color); g.pose().popPose();
    }
    /** Half-size text from (x, y) to the slot's right edge. */
    private static void small(GuiGraphics g, String text, int x, int y, int color) {
        var font = Minecraft.getInstance().font;
        text = TextLines.cut(text, (19 - x) * 2, font::width);
        g.pose().pushPose(); g.pose().translate(x, y, 200); g.pose().scale(.5F, .5F, 1);
        g.drawString(font, text, 0, 0, color); g.pose().popPose();
    }
    private HudRenderer() {}
}
