package com.poris.elegies.fingertip.client.hud;

import com.poris.elegies.fingertip.api.ActionState;
import net.minecraft.network.chat.Component;

/**
 * Hover text colours (2026-10-30): the name in the theme's accent, a
 * state line by what it means, a mod's own numbers in body text, and labels, keys and hints muted. A colour the mod set
 * on its own text wins. The state colours are 指尖配色法's (ThemePalette).
 */
final class TooltipStyle {

    static Component title(Component text) { return paint(text, EditorTheme.accent()); }
    static Component body(Component text) { return paint(text, EditorTheme.TEXT); }
    static Component muted(Component text) { return paint(text, EditorTheme.MUTED); }
    static Component warning(Component text) { return paint(text, ThemePalette.ERROR); }
    static Component state(Component text, ActionState state) {
        return state(text, state.available() && !state.lacksResource(), state.available() && state.lacksResource(), state.cooldown());
    }
    /** Red when it cannot be used, blue when short of its resource, amber while cooling down, green when ready. */
    static Component state(Component text, boolean available, boolean lacksResource, float cooldown) {
        return paint(text, lacksResource ? ThemePalette.NO_RESOURCE : !available ? ThemePalette.ERROR
                : cooldown > 0 ? ThemePalette.COOLDOWN : ThemePalette.READY);
    }
    private static Component paint(Component text, int argb) {
        return text == null || text.getStyle().getColor() != null ? text : text.copy().withColor(argb & 0xFFFFFF);
    }
    private TooltipStyle() {}
}
