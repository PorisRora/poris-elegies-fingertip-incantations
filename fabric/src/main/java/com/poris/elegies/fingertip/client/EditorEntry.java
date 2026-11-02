package com.poris.elegies.fingertip.client;

import com.poris.elegies.fingertip.Fingertip;
import com.poris.elegies.fingertip.client.hud.HudEditorScreen;
import com.poris.elegies.fingertip.client.hud.HudEntryButton;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;

public final class EditorEntry {
    private record CarriedClick(net.minecraft.client.gui.screens.Screen screen, int button) {}
    private static CarriedClick carriedClick;

    public static void inventory(net.minecraft.client.gui.screens.Screen screen) {
        if (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen || screen instanceof ControlsScreen)
            net.fabricmc.fabric.api.client.screen.v1.Screens.getButtons(screen).add(new HudEntryButton(screen));
    }
    public static void position(net.minecraft.client.gui.screens.Screen screen) {
        for (var child : screen.children())
            if (child instanceof HudEntryButton button) button.position();
    }

    /** Only our visible button owns this hit; recipe overlays still receive every other click. */
    public static boolean click(net.minecraft.client.gui.screens.Screen screen, double mouseX, double mouseY, int buttonId) {
        carriedClick = null;
        for (var child : screen.children()) {
            if (child instanceof HudEntryButton button) {
                button.position();
                double x = mouseX, y = mouseY;
                if (button.visible && buttonId <= 1 && buttonId >= 0
                        && x >= button.getX() && x < button.getX() + button.getWidth()
                        && y >= button.getY() && y < button.getY() + button.getHeight()
                        && screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> container
                        && !container.getMenu().getCarried().isEmpty()) {
                    // This hit belongs to our entry. Other overlays may swallow it
                    // before the container receives its normal click pair.
                    // Invoke the real screen methods so creative/survival and right-click semantics survive.
                    carriedClick = new CarriedClick(container, buttonId);
                    container.mouseClicked(x, y, buttonId);
                    return false;
                }
                return !button.mouseClicked(mouseX, mouseY, buttonId);
            }
        }
        return true;
    }

    public static boolean release(net.minecraft.client.gui.screens.Screen screen, double mouseX, double mouseY, int buttonId) {
        var click = carriedClick;
        if (click == null || click.button() != buttonId) return true;
        carriedClick = null;
        if (click.screen() == screen)
            click.screen().mouseReleased(mouseX, mouseY, buttonId);
        return false;
    }

    public static void opening() { carriedClick = null; }

    private EditorEntry() {}
}
