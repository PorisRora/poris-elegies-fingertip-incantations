package com.poris.elegies.fingertip.client.hud;

import com.poris.elegies.fingertip.client.input.HotbarControl;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Both editor surfaces share one mode switch and the same explanation. */
final class HotbarModeButton {
    static Component label() {
        return Component.translatable(HudLayout.INSTANCE.takeOverHotbar ? "poris_elegies_fingertip_incantations.hotbar_custom" : "poris_elegies_fingertip_incantations.hotbar_native");
    }
    static Button create(int x, int y, int width, Runnable changed) {
        return create(x, y, width, 18, changed);
    }
    static Button create(int x, int y, int width, int height, Runnable changed) {
        var button = EditorTheme.button(x, y, width, height, label(width), b -> {
            HotbarControl.toggle(); b.setMessage(label(width)); changed.run();
        });
        button.setTooltip(Tooltip.create(Component.translatable("poris_elegies_fingertip_incantations.hotbar_mode_hint")));
        return button;
    }
    private static Component label(int width) {
        return width >= 70 ? label() : Component.translatable(HudLayout.INSTANCE.takeOverHotbar
                ? "poris_elegies_fingertip_incantations.mode_custom_short" : "poris_elegies_fingertip_incantations.mode_native_short");
    }
    private HotbarModeButton() {}
}
