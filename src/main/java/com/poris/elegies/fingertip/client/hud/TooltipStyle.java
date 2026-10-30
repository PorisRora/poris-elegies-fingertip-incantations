package com.poris.elegies.fingertip.client.hud;

import com.poris.elegies.fingertip.api.ActionState;
import net.minecraft.network.chat.Component;

/**
 * Hover text colours (2026-10-30, user: "感觉有点凌乱 或许可以用颜色标注不同的内容"): the name in the theme's accent, a
 * state line by what it means, a mod's own numbers in body text, and labels, keys and hints muted. A colour the mod set
 * on its own text wins.
 */
final class TooltipStyle {
    static final int READY = 0xFF8FD694, WAITING = 0xFFF2B05E, NO_RESOURCE = 0xFF8CB4FF, BLOCKED = 0xFFE8857A;

    static Component title(Component text) { return paint(text, EditorTheme.accent()); }
    static Component body(Component text) { return paint(text, EditorTheme.TEXT); }
    static Component muted(Component text) { return paint(text, EditorTheme.MUTED); }
    static Component warning(Component text) { return paint(text, BLOCKED); }
    static Component state(Component text, ActionState state) {
        return state(text, state.available() && !state.lacksResource(), state.available() && state.lacksResource(), state.cooldown());
    }
    /** Red when it cannot be used, blue when short of its resource, amber while cooling down, green when ready. */
    static Component state(Component text, boolean available, boolean lacksResource, float cooldown) {
        return paint(text, lacksResource ? NO_RESOURCE : !available ? BLOCKED : cooldown > 0 ? WAITING : READY);
    }
    private static Component paint(Component text, int argb) {
        return text == null || text.getStyle().getColor() != null ? text : text.copy().withColor(argb & 0xFFFFFF);
    }
    private TooltipStyle() {}
}
