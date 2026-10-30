package com.poris.elegies.fingertip.client;

import com.poris.elegies.fingertip.Fingertip;
import com.poris.elegies.fingertip.client.hud.HudEditorScreen;
import com.poris.elegies.fingertip.client.hud.HudEntryButton;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@EventBusSubscriber(modid = Fingertip.MOD_ID, value = Dist.CLIENT)
public final class EditorEntry {
    private record CarriedClick(net.minecraft.client.gui.screens.Screen screen, int button) {}
    private static CarriedClick carriedClick;
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        ModList.get().getModContainerById(Fingertip.MOD_ID).ifPresent(container ->
                container.registerExtensionPoint(IConfigScreenFactory.class, (ignored, parent) -> new HudEditorScreen(parent, -1)));
    }
    @SubscribeEvent public static void inventory(ScreenEvent.Init.Post event) {
        var screen = event.getScreen();
        if (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen || screen instanceof ControlsScreen)
            event.addListener(new HudEntryButton(screen));
    }
    @SubscribeEvent public static void position(ScreenEvent.Render.Pre event) {
        for (var child : event.getScreen().children())
            if (child instanceof HudEntryButton button) button.position();
    }

    /** Only our visible button owns this hit; recipe overlays still receive every other click. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void click(ScreenEvent.MouseButtonPressed.Pre event) {
        carriedClick = null;
        for (var child : event.getScreen().children()) {
            if (child instanceof HudEntryButton button) {
                button.position();
                double x = event.getMouseX(), y = event.getMouseY();
                if (button.visible && event.getButton() <= 1 && event.getButton() >= 0
                        && x >= button.getX() && x < button.getX() + button.getWidth()
                        && y >= button.getY() && y < button.getY() + button.getHeight()
                        && event.getScreen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> container
                        && !container.getMenu().getCarried().isEmpty()) {
                    // This hit belongs to our entry. Other overlays may swallow it
                    // before the container receives its normal click pair.
                    // Invoke the real screen methods so creative/survival and right-click semantics survive.
                    carriedClick = new CarriedClick(container, event.getButton());
                    container.mouseClicked(x, y, event.getButton());
                    event.setCanceled(true);
                    return;
                }
                if (button.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton()))
                    event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void release(ScreenEvent.MouseButtonReleased.Pre event) {
        var click = carriedClick;
        if (click == null || click.button() != event.getButton()) return;
        carriedClick = null;
        if (click.screen() == event.getScreen())
            click.screen().mouseReleased(event.getMouseX(), event.getMouseY(), event.getButton());
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void opening(ScreenEvent.Opening event) { carriedClick = null; }

    private EditorEntry() {}
}
