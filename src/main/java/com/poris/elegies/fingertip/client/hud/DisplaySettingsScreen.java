package com.poris.elegies.fingertip.client.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.BooleanSupplier;

/** Appearance controls over the player's actual HUD. No sample items, projected layout or binding editor. */
public final class DisplaySettingsScreen extends Screen {
    private final Screen parent;
    private boolean panelVisible = true;
    private DisplaySettingsGeometry ui;

    public DisplaySettingsScreen(Screen parent) {
        super(tr("title"));
        this.parent = parent;
    }

    private static Component tr(String key, Object... args) { return Component.translatable("poris_elegies_fingertip_incantations.display." + key, args); }

    @Override protected void init() {
        ui = new DisplaySettingsGeometry(width, height, HudLayout.INSTANCE.frameRects(width, height));
        if (!panelVisible) {
            button(ui.restore, tr("return_settings"), b -> togglePanel())
                    .setTooltip(EditorTheme.tip(tr("return_settings_hint")));
            return;
        }
        button(ui.done, tr("done"), b -> onClose());
        var themes = Theme.values();
        for (int i = 0; i < themes.length; i++) {
            var theme = themes[i];
            option(ui.theme(i), tr("theme_" + theme.id), b -> {
                HudLayout.INSTANCE.skin = theme.id;
                HudLayout.INSTANCE.save();
                ThemeTextures.releaseOtherThemes();
            }, () -> Theme.current() == theme).setTooltip(EditorTheme.tip(tr("theme_" + theme.id + "_hint")));
        }
        var modes = HudVisibility.Mode.values();
        for (int i = 0; i < modes.length; i++) {
            var mode = modes[i];
            option(ui.range(i), tr("range_" + mode.id), b -> {
                HudLayout.INSTANCE.setDisplayMode(mode);
                HudLayout.INSTANCE.save();
            }, () -> HudVisibility.mode() == mode).setTooltip(EditorTheme.tip(tr("mode_" + mode.id + "_hint")));
        }
        button(ui.functions(), functionsLabel(), b -> {
            HudLayout.INSTANCE.hideModFunctions = !HudLayout.INSTANCE.hideModFunctions;
            HudLayout.INSTANCE.save();
            b.setMessage(functionsLabel());
        }).setTooltip(EditorTheme.tip(tr("functions_hint")));
        button(ui.durability(), durabilityLabel(), b -> {
            HudLayout.INSTANCE.durabilityPercent = !HudLayout.INSTANCE.durabilityPercent;
            HudLayout.INSTANCE.save();
            b.setMessage(durabilityLabel());
        }).setTooltip(EditorTheme.tip(tr("durability_hint")));
    }

    private Button button(HudPlacement.Rect r, Component text, Button.OnPress action) {
        return addRenderableWidget(EditorTheme.button(r.x(), r.y(), r.width(), r.height(), text, action));
    }

    /** Segmented display options use the same material states as every other themed button. */
    private Button option(HudPlacement.Rect r, Component text, Button.OnPress action, BooleanSupplier selected) {
        return addRenderableWidget(EditorTheme.button(r.x(), r.y(), r.width(), r.height(), text, action, selected));
    }

    private Component functionsLabel() { return tr(HudLayout.INSTANCE.hideModFunctions ? "functions_hidden" : "functions_shown"); }
    private Component durabilityLabel() { return tr(HudLayout.INSTANCE.durabilityPercent ? "durability_shown" : "durability_hidden"); }

    @Override public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        if (panelVisible) EditorTheme.panel(g, ui.panel.x(), ui.panel.y(), ui.panel.width(), ui.panel.height(), true);
    }

    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        // Draw at gameplay positions and scale. Only the Z layer moves behind the floating controls.
        g.pose().pushPose();
        g.pose().translate(0, 0, -400);
        try {
            for (var slot : HudLayout.INSTANCE.activeSlots()) if (HudVisibility.includes(slot.id))
                HudRenderer.renderAt(g, slot.id, slot.screenX(width, height), slot.screenY(width, height));
        } finally { g.pose().popPose(); g.setColor(1, 1, 1, 1); }
        g.flush();
        super.render(g, mx, my, partial);
        if (panelVisible) {
            TextFit.draw(g, font, title, ui.panel.x() + 12, ui.panel.y() + 12, ui.panel.width() - 24, EditorTheme.accent(), false, TextFit.Align.LEFT);
            g.drawString(font, tr("theme_label"), ui.panel.x() + 12, ui.panel.y() + 29, EditorTheme.MUTED, false);
            g.drawString(font, tr("range_label"), ui.panel.x() + 12, ui.panel.y() + 69, EditorTheme.MUTED, false);
            boolean hidden = HudVisibility.mode() == HudVisibility.Mode.HIDDEN;
            var hint = ui.hint(hidden ? 2 : 1);
            TextFit.draw(g, font, tr("panel_hide_hint"), hint.x(), hint.y(), hint.width(), EditorTheme.MUTED, false, TextFit.Align.LEFT);
            if (hidden) TextFit.draw(g, font, tr("panel_hidden_hint"), hint.x(), hint.y() + 10, hint.width(), EditorTheme.MUTED, false, TextFit.Align.LEFT);
        }
        // Screen.renderWithTooltip draws all deferred tooltips afterwards, outside any widget clipping.
        g.flush();
    }

    private void togglePanel() { panelVisible = !panelVisible; rebuildWidgets(); }

    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_TAB) { togglePanel(); return true; }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { HudLayout.INSTANCE.save(); minecraft.setScreen(parent); }
}
