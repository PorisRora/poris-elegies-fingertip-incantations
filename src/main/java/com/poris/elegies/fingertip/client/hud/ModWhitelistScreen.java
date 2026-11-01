package com.poris.elegies.fingertip.client.hud;

import com.poris.elegies.fingertip.client.input.ModKeyWhitelist;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

/** Explicit native-key exceptions, kept separate from HUD visibility and item/skill binding. */
public final class ModWhitelistScreen extends Screen {
    private final Screen parent;
    private final List<Button> rows = new ArrayList<>();
    private List<ModKeyWhitelist.Group> groups = List.of(), filtered = List.of();
    private EditBox search;
    private Button previous, next;
    private int x, y, w, h, page, capacity;
    private String query = "";
    private Component error, notice;

    public ModWhitelistScreen(Screen parent) { super(Component.translatable("poris_elegies_fingertip_incantations.whitelist")); this.parent = parent; }
    @Override protected void init() {
        w = Math.min(440, width - 16); h = Math.min(360, height - 16);
        x = (width - w) / 2; y = (height - h) / 2;
        capacity = Math.max(1, (h - 142) / 24);
        groups = ModKeyWhitelist.groups(); rows.clear();
        search = addRenderableWidget(new EditBox(font, x + 12, y + 59, w - 24, 18, Component.translatable("poris_elegies_fingertip_incantations.whitelist_search")));
        search.setMaxLength(128); search.setValue(query);
        search.setHint(Component.translatable("poris_elegies_fingertip_incantations.whitelist_search"));
        search.setResponder(value -> { query = value; page = 0; rebuildRows(); });
        previous = addRenderableWidget(EditorTheme.button(x + 12, y + h - 28, 30, 18, Component.literal("‹"), b -> { page--; rebuildRows(); }));
        next = addRenderableWidget(EditorTheme.button(x + 92, y + h - 28, 30, 18, Component.literal("›"), b -> { page++; rebuildRows(); }));
        addRenderableWidget(EditorTheme.button(x + w - 72, y + h - 28, 60, 18, Component.translatable("gui.done"), b -> onClose()));
        rebuildRows();
    }
    private void rebuildRows() {
        rows.forEach(this::removeWidget); rows.clear();
        filtered = groups.stream().filter(g -> ListSearch.matches(query, g.id() + " " + g.name())).toList();
        page = ListSearch.clampPage(page, ListSearch.pages(filtered.size(), capacity), previous, next);
        for (int i = page * capacity; i < Math.min(filtered.size(), (page + 1) * capacity); i++) {
            var group = filtered.get(i);
            var button = EditorTheme.button(x + 12, y + 85 + (i - page * capacity) * 24, w - 24, 20,
                    label(group), b -> {
                        error = ModKeyWhitelist.set(group.id(), !ModKeyWhitelist.contains(group.id()));
                        notice = error == null && ModKeyWhitelist.contains(group.id()) ? ModKeyWhitelist.paused(group.id()) : null;
                        b.setMessage(label(group));
                    }, () -> ModKeyWhitelist.contains(group.id()));
            var tooltip = Component.literal(group.name() + " (" + group.id() + ")");
            group.keys().stream().limit(8).forEach(k -> tooltip.append("\n").append(Component.translatable(k.getName()))
                    .append(": ").append(k.getTranslatedKeyMessage()));
            tooltip.append("\n").append(Component.translatable("poris_elegies_fingertip_incantations.whitelist_limits"));
            button.setTooltip(EditorTheme.tip(tooltip));
            rows.add(addRenderableWidget(button));
        }
    }
    private Component label(ModKeyWhitelist.Group group) {
        return Component.translatable(ModKeyWhitelist.contains(group.id()) ? "poris_elegies_fingertip_incantations.whitelist_allowed" : "poris_elegies_fingertip_incantations.whitelist_managed", group.name());
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, 0x8010131C); EditorTheme.panel(g, x, y, w, h, true);
    }
    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        g.drawString(font, title, x + 12, y + 12, EditorTheme.accent(), false);
        TextFit.drawLines(g, font, Component.translatable("poris_elegies_fingertip_incantations.whitelist_hint"), x + 12, y + 29, w - 24,
                TextFit.linesIn(font, 28, 9), 9, EditorTheme.MUTED, false);
        g.drawCenteredString(font, (page + 1) + "/" + ListSearch.pages(filtered.size(), capacity), x + 67, y + h - 23, EditorTheme.TEXT);
        if (filtered.isEmpty()) g.drawString(font, Component.translatable("poris_elegies_fingertip_incantations.whitelist_no_results"), x + 12, y + 87, EditorTheme.MUTED, false);
        if (error != null) TextFit.drawLines(g, font, error, x + 12, y + h - 53, w - 24, 2, 9, ThemePalette.ERROR, false);
        else if (notice != null) TextFit.drawLines(g, font, notice, x + 12, y + h - 53, w - 24, 2, 9, EditorTheme.accent(), false);
    }
    @Override public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (!EditorGeometry.inside(mx, my, x, y + 82, w, capacity * 24)) return super.mouseScrolled(mx, my, dx, dy);
        page -= (int)Math.signum(dy); rebuildRows(); return true;
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
}
