package com.poris.elegies.fingertip.client.hud;

import com.poris.elegies.fingertip.client.input.KeyState;

import com.mojang.blaze3d.platform.InputConstants;
import com.poris.elegies.fingertip.client.input.OptionalKeyPolicy;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.LinkedHashSet;

/** Less frequent controls live here instead of competing with binding and placement on the canvas. */
public final class InputSettingsScreen extends Screen {
    private final Screen parent;
    private int x, y, w, h, top;
    public InputSettingsScreen(Screen parent) { super(tr("poris_elegies_fingertip_incantations.settings_keys")); this.parent = parent; }
    private static Component tr(String key, Object... args) { return Component.translatable(key, args); }
    private Button button(int x, int y, int w, Component text, Button.OnPress action) {
        return addRenderableWidget(EditorTheme.button(x, y, w, 18, text, action));
    }
    private Button toggle(int x, int y, int w, Component text, Button.OnPress action, java.util.function.BooleanSupplier selected) {
        return addRenderableWidget(EditorTheme.button(x, y, w, 18, text, action, selected));
    }
    private Component state(String name, boolean on) { return tr(name).copy().append(tr(on ? "poris_elegies_fingertip_incantations.switch_on" : "poris_elegies_fingertip_incantations.switch_off")); }
    private void changed() { OptionalKeyPolicy.changed(); rebuildWidgets(); }
    /** Notes (E, F3) go above the list of what the key already does; lit/dim is explained once, on the F1–F9 switch (10-31). */
    private Tooltip keyHint(int code, String... notes) {
        var names = new LinkedHashSet<String>();
        // F1/F3 are hard-coded by Minecraft; other entries reflect the player's real mappings.
        if (code == 290) names.add(tr("poris_elegies_fingertip_incantations.native_f1").getString());
        if (code == 292) names.add(tr("poris_elegies_fingertip_incantations.native_f3").getString());
        for (var mapping : minecraft.options.keyMappings) {
            if (KeyState.key(mapping).getType() == InputConstants.Type.KEYSYM && KeyState.key(mapping).getValue() == code)
                names.add(Component.translatable(mapping.getName()).getString());
        }
        var message = Component.empty();
        for (var note : notes) message.append(tr(note)).append("\n");
        if (notes.length > 0) message.append("\n");
        message.append(tr("poris_elegies_fingertip_incantations.key_occupants"));
        if (names.isEmpty()) message.append("\n").append(tr("poris_elegies_fingertip_incantations.key_unassigned"));
        else {
            int count = 0;
            for (var name : names) { if (count++ == 6) break; message.append("\n• " + name); }
            if (names.size() > 6) message.append("\n").append(tr("poris_elegies_fingertip_incantations.key_more", names.size() - 6));
        }
        return EditorTheme.tip(message);
    }
    @Override protected void init() {
        w = Math.min(480, width - 24); h = Math.min(260, height - 16);
        x = (width - w) / 2; y = (height - h) / 2; top = y + 36;
        button(x + w - 76, y + h - 28, 64, tr("gui.done"), b -> onClose());
        button(x + 12, y + h - 28, Math.min(148, w - 100), tr("poris_elegies_fingertip_incantations.whitelist"),
                b -> minecraft.setScreen(new ModWhitelistScreen(this)));
        var input = OptionalKeyPolicy.settings();
        toggle(x + 12, top, w - 24, state("poris_elegies_fingertip_incantations.take_functions", input.functionKeys), b -> { input.functionKeys = !input.functionKeys; changed(); }, () -> input.functionKeys)
                .setTooltip(EditorTheme.tip(tr("poris_elegies_fingertip_incantations.take_functions_hint")));
        for (int i = 0; i < 9; i++) {
            final int bit = 1 << i;
            int left = edge(i, 9, 2);
            var key = addRenderableWidget(EditorTheme.button(left, top + 25, edge(i + 1, 9, 2) - 2 - left, 18, Component.literal("F" + (i + 1)),
                    b -> { input.functionMask ^= bit; changed(); }, () -> input.functionKeys && (input.functionMask & bit) != 0));
            key.active = input.functionKeys;
            key.setTooltip(i == 2 ? keyHint(292, "poris_elegies_fingertip_incantations.take_f3_hint") : keyHint(290 + i));
        }
        toggle(edge(0, 3, 6), top + 54, edge(1, 3, 6) - 6 - edge(0, 3, 6), state("poris_elegies_fingertip_incantations.take_q", input.takeQ), b -> { input.takeQ = !input.takeQ; changed(); }, () -> input.takeQ).setTooltip(keyHint(81));
        toggle(edge(1, 3, 6), top + 54, edge(2, 3, 6) - 6 - edge(1, 3, 6), state("poris_elegies_fingertip_incantations.take_e", input.takeE), b -> { input.takeE = !input.takeE; changed(); }, () -> input.takeE).setTooltip(keyHint(69, "poris_elegies_fingertip_incantations.take_e_hint"));
        toggle(edge(2, 3, 6), top + 54, edge(3, 3, 6) - 6 - edge(2, 3, 6), state("poris_elegies_fingertip_incantations.take_t", input.takeT), b -> { input.takeT = !input.takeT; changed(); }, () -> input.takeT).setTooltip(keyHint(84));
        // 2026-10-27 (user): the escape hatch for the right-click "real slot" compatibility, in case a mod breaks on it.
        toggle(x + 12, top + 83, w - 24, state("poris_elegies_fingertip_incantations.use_real_slot", input.useRealSlot),
                b -> { input.useRealSlot = !input.useRealSlot; changed(); }, () -> input.useRealSlot)
                .setTooltip(EditorTheme.tip(tr("poris_elegies_fingertip_incantations.use_real_slot_hint")));
    }
    /**
     * Left edge of the i-th of {@code count} buttons sharing the full row with {@code gap} between them; edge(count) - gap
     * is the row's right end. Spreading the remainder keeps the F1–F9 row as wide as the rows above and below it
     * (2026-10-27: it was 8 px short at the user's 2560x1369, GUI scale 5).
     */
    private int edge(int i, int count, int gap) { return x + 12 + i * (w - 24 + gap) / count; }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, 0x99100F19);
        EditorTheme.panel(g, x, y, w, h, true);
    }
    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        g.drawString(font, title, x + 14, y + 12, EditorTheme.accent(), false);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { HudLayout.INSTANCE.save(); minecraft.setScreen(parent); }
}
