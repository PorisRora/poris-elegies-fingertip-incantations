package com.poris.elegies.fingertip.client.hud;

import com.poris.elegies.fingertip.client.input.InputPreferences;
import com.poris.elegies.fingertip.client.input.OptionalKeyPolicy;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

/** Pick a physical sector first, then choose its function on a dedicated page. */
public final class WheelConfigurationScreen extends Screen {
    private final Screen parent;
    private WheelConfigurationGeometry ui;
    private int selectedSector = -1;
    private Component feedback;

    public WheelConfigurationScreen(Screen parent) {
        super(Component.translatable("poris_elegies_fingertip_incantations.settings_wheel")); this.parent = parent;
    }
    @Override protected void init() {
        ui = new WheelConfigurationGeometry(width, height);
        var input = OptionalKeyPolicy.settings();
        for (int i = 0; i < InputPreferences.WHEEL_SLOTS; i++) {
            final int sector = i;
            addRenderableWidget(new WheelSectorButton(ui.wheel, sector, sectorLabel(sector), b -> selectSector(sector)));
        }
        var toggle = addRenderableWidget(EditorTheme.button(ui.toggleX, ui.footerY, ui.toggleWidth, WheelConfigurationGeometry.BUTTON_HEIGHT,
                input.takeE ? Component.translatable("poris_elegies_fingertip_incantations.wheel_required")
                        : Component.translatable("poris_elegies_fingertip_incantations.wheel_enabled").append(Component.translatable(input.wheelEnabled ? "poris_elegies_fingertip_incantations.switch_on" : "poris_elegies_fingertip_incantations.switch_off")),
                b -> { if (!input.takeE) { input.wheelEnabled = !input.wheelEnabled; OptionalKeyPolicy.changed(); rebuildWidgets(); } },
                () -> input.wheelEnabled));
        toggle.active = !input.takeE;
        toggle.setTooltip(EditorTheme.tip(Component.translatable(input.takeE ? "poris_elegies_fingertip_incantations.wheel_required_hint" : "poris_elegies_fingertip_incantations.wheel_setting_hint")));
        var mode = addRenderableWidget(EditorTheme.button(ui.modeX, ui.footerY, WheelConfigurationGeometry.MODE_WIDTH, WheelConfigurationGeometry.BUTTON_HEIGHT,
                Component.translatable(input.wheelDrag ? "poris_elegies_fingertip_incantations.wheel.mode_drag" : "poris_elegies_fingertip_incantations.wheel.mode_click"),
                b -> { input.wheelDrag = !input.wheelDrag; OptionalKeyPolicy.changed(); rebuildWidgets(); }));
        mode.setTooltip(EditorTheme.tip(Component.translatable("poris_elegies_fingertip_incantations.wheel.mode_hint")));
        addRenderableWidget(EditorTheme.button(ui.doneX, ui.footerY, WheelConfigurationGeometry.DONE_WIDTH, WheelConfigurationGeometry.BUTTON_HEIGHT,
                Component.translatable("gui.done"), b -> onClose()));
    }
    private void selectSector(int sector) {
        selectedSector = sector;
        if (OptionalKeyPolicy.settings().lockedWheelSlot(sector)) { feedback = fixedHint(sector); return; }
        feedback = null;
        minecraft.setScreen(new WheelActionPickerScreen(this, sector));
    }
    private static Component sectorLabel(int sector) {
        return Component.translatable("poris_elegies_fingertip_incantations.wheel.slot", sector + 1).append(" · ")
                .append(RadialActions.label(OptionalKeyPolicy.settings().wheel.get(sector)));
    }
    private static Component fixedHint(int sector) {
        return Component.translatable(sector == InputPreferences.EDITOR_SLOT ? "poris_elegies_fingertip_incantations.wheel.editor_fixed" : "poris_elegies_fingertip_incantations.wheel.inventory_fixed");
    }
    void assigned(int sector, String id) {
        selectedSector = sector;
        feedback = Component.translatable("poris_elegies_fingertip_incantations.wheel.assigned", sector + 1, RadialActions.label(id));
    }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, 0x99100F19);
    }
    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        g.drawCenteredString(font, title, width / 2, 12, EditorTheme.accent());
        int hover = ui.wheel.atPreview(mx, my);
        int shown = hover >= 0 ? hover : getFocused() instanceof WheelSectorButton sector ? sector.sector() : selectedSector;
        RadialPainter.draw(g, ui.wheel, OptionalKeyPolicy.settings().wheel, shown, true);
        Component center = shown >= 0 ? Component.translatable("poris_elegies_fingertip_incantations.wheel.slot", shown + 1)
                : Component.translatable("poris_elegies_fingertip_incantations.wheel.choose_sector");
        g.drawCenteredString(font, center, ui.wheel.cx(), ui.wheel.cy() - 4, EditorTheme.accent());
        Component hint = feedback != null ? feedback : Component.translatable("poris_elegies_fingertip_incantations.wheel.configure_hint");
        TextFit.draw(g, font, hint, 16, 29, width - 32, EditorTheme.MUTED, true, TextFit.Align.CENTER);
        // The hovered sector's text, at the mouse (the sector buttons carry no widget tooltip, see WheelSectorButton).
        if (hover >= 0) {
            var lines = new java.util.ArrayList<Component>(List.of(TooltipStyle.title(sectorLabel(hover))));
            if (OptionalKeyPolicy.settings().lockedWheelSlot(hover)) lines.add(TooltipStyle.muted(fixedHint(hover)));
            setTooltipForNextRenderPass(lines.stream().map(Component::getVisualOrderText).toList());
        }
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { minecraft.setScreen(parent); }
}
