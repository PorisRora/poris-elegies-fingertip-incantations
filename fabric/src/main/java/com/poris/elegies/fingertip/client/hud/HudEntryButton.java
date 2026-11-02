package com.poris.elegies.fingertip.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

/**
 * The Fingertip logo in the inventory panel's top-right corner (UiSkin.ENTRY), survival and creative: above the crafting result, or above the
 * creative scroll bar. It used to sit outside the panel's right edge, where the potion effects list covered it
 * (2026-10-28). Plain and hovered only, no disabled face (2026-10-30, user:
 * "指着可以留着 停用都删了吧"): with a stack on the cursor it opens the editor all the same, after closing the inventory as
 * E does, so the server puts the stack back; switching screens alone left it hidden on the cursor.
 */
public final class HudEntryButton extends Button {
    private final Screen parent;
    public HudEntryButton(Screen parent) {
        super(0, 0, 14, 14, Component.translatable("poris_elegies_fingertip_incantations.editor"), b -> open(parent), DEFAULT_NARRATION);
        this.parent = parent;
        setTooltip(Tooltip.create(Component.translatable("poris_elegies_fingertip_incantations.entry_hint")));
        position();
    }
    private static void open(Screen parent) {
        var mc = Minecraft.getInstance();
        if (mc.player != null && parent instanceof AbstractContainerScreen<?> container && !container.getMenu().getCarried().isEmpty())
            mc.player.closeContainer();
        mc.setScreen(new HudEditorScreen(parent, -1));
    }
    public void position() {
        visible = true;
        if (parent instanceof AbstractContainerScreen<?> container) {
            boolean creative = parent instanceof CreativeModeInventoryScreen;
            var area = (com.poris.elegies.fingertip.mixin.client.ContainerScreenAccess)container;
            setX(area.fingertip$left() + area.fingertip$width() - getWidth() - (creative ? 4 : 5));
            setY(area.fingertip$top() + (creative ? 3 : 5));
            // Narrow recipe mode replaces the inventory panel, so its entry hides too.
            if (parent instanceof InventoryScreen inventory && parent.width < 379
                    && inventory.getRecipeBookComponent().isVisible()) visible = false;
        } else {
            setX(Math.max(4, parent.width - 22)); setY(6);
        }
    }
    @Override protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
        position();
        if (!visible) return;
        (EditorTheme.hoverOrKeyboardFocus(this) ? UiSkin.ENTRY_HOVER : UiSkin.ENTRY).square(g, getX(), getY(), 14);
    }
}
