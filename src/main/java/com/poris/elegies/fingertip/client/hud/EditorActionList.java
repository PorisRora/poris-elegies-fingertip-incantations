package com.poris.elegies.fingertip.client.hud;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import com.poris.elegies.fingertip.api.ActionCatalogInfo;
import com.poris.elegies.fingertip.api.FingertipApi;
import com.poris.elegies.fingertip.client.input.ModKeyWhitelist;
import java.util.*;

/**
 * A searchable accordion of mods with compact action cards. Headers and gutters never bind a slot.
 * <p>
 * Skills (2026-10-28): with one skill mod the list has no header and its status (what gives its skills now, or why
 * nothing is usable) is the line above the cards. With several, each mod gets a header with its status on a second
 * line, shown even while none of its skills is usable, so a mod with nothing usable still says why. The skill tab opens
 * and closes its groups apart from the Mods tab, where a mod's key mappings start closed.
 */
final class EditorActionList {
    /** Which cards the list shows: skills, vanilla's key mappings, or other mods' functions and keys. */
    enum Source { SKILLS, VANILLA, MODS }
    /** A card name is broken into lines when the list is laid out, not on every frame. */
    private record NameLine(FormattedCharSequence text, int width) {}
    /** {@code group} keys the open/closed state; {@code note} is a skill mod header's status line, else null. */
    private record Row(String group, Component title, int count, ActionCatalog.Entry entry,
                       int x, int top, int width, int height, List<NameLine> lines, Component note) {}
    /** One skill mod and the names of what gives its skills now ({@link FingertipApi#registerSources}). */
    record Provider(String owner, Component name, List<Component> sources) {
        /** The names on one line, as the mod's card shows them. */
        Component status() {
            var line = Component.empty();
            for (int i = 0; i < sources.size(); i++) {
                if (i > 0) line.append(Component.translatable("poris_elegies_fingertip_incantations.list_separator"));
                line.append(sources.get(i));
            }
            return line;
        }
        /** The card's second line: what gives the skills, or, with nothing, the mod's own hint; null for neither. */
        Component note() {
            return !sources.isEmpty() ? status() : FingertipApi.emptyHint(owner).orElse(null);
        }
    }
    private record Group(String key, Component name, Component note, List<ActionCatalog.Entry> entries) {
        boolean keysOnly() { return !entries.isEmpty() && entries.stream().noneMatch(e -> e.action() != null); }
    }
    final EditBox search;
    private final EditorGeometry ui;
    private final Font font;
    private final boolean skills;
    private final Source source;
    private final ActionCardGrid cardGrid;
    private List<Provider> providers = List.of();
    /** Where each key switch was drawn this frame, for clicks and hints. */
    private record SwitchHit(int x0, int y0, int x1, int y1, FingertipApi.ModSwitch modSwitch) {}
    private final List<SwitchHit> switchHits = new ArrayList<>();
    private final Set<String> collapsed = new HashSet<>();
    private final Set<String> knownGroups = new HashSet<>();
    private List<ActionCatalog.Entry> all;
    private List<ActionCatalog.Entry> filtered;
    private List<Row> display = List.of();
    private int offset;
    private int contentHeight;
    private boolean scrolling;
    private double scrollGrab;

    /** {@code catalog} is the screen's current {@link ActionCatalog#entries()}, so one rebuild builds it once. */
    EditorActionList(EditorGeometry ui, Font font, EditorActionList previous, Source source, List<ActionCatalog.Entry> catalog) {
        this.ui = ui; this.font = font; this.skills = source == Source.SKILLS; this.source = source;
        cardGrid = ActionCardGrid.fit(ui.rightX + 2, ui.rightWidth - 14);
        if (previous != null) {
            collapsed.addAll(previous.collapsed); knownGroups.addAll(previous.knownGroups);
        }
        all = catalog;
        // Each tab says what its search finds (2026-10-30, user: "这里文案是不是得优化一下").
        var hint = Component.translatable("poris_elegies_fingertip_incantations.search_" + source.name().toLowerCase(java.util.Locale.ROOT));
        search = new EditBox(font, ui.rightX + 5, ui.listY + 1, ui.rightWidth - 10, 16, hint);
        search.setMaxLength(80); search.setHint(hint);
        search.setResponder(value -> { offset = 0; rebuild(); });
        search.setValue(previous == null || previous.source != source ? "" : previous.search.getValue());
        rebuild();
        if (previous != null && previous.source == source) offset = Math.clamp(previous.offset, 0, maxOffset());
    }
    /** Once a second: lay the list out again only when a skill appeared, vanished or changed its label, or a mod's status changed. */
    void refresh() {
        if (scrolling) return;
        var next = ActionCatalog.entries();
        if (ActionCatalog.sameListing(all, next) && sameStatuses(providers, providers(next))) return;
        all = next; rebuild();
    }
    private static boolean sameStatuses(List<Provider> a, List<Provider> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++)
            if (!a.get(i).owner().equals(b.get(i).owner()) || !a.get(i).status().getString().equals(b.get(i).status().getString())) return false;
        return true;
    }
    private void rebuild() {
        boolean searching = !search.getValue().isBlank();
        providers = providers(all);
        var mode = skills ? ActionCatalogInfo.Kind.SKILL : ActionCatalogInfo.Kind.FUNCTION;
        var section = all.stream().filter(e -> e.catalog().kind() == mode)
                .filter(e -> source != Source.VANILLA || e.owner().equals("minecraft"))
                .filter(e -> source != Source.MODS || !e.owner().equals("minecraft")).toList();
        var listed = section.stream().filter(e -> e.catalog().visible()).toList();
        filtered = listed.stream().filter(e -> e.matches(search.getValue())).toList();
        // Every skill mod is one card, however many there are: its name, then what gives its skills or its own hint
        // (2026-10-30, user: "只有一个iron应该也有个卡片吧 这样就不用单独做只有一个的了", "卡片就是 mod 提供法术的物品名").
        // There used to be a status line above a lone mod's skills instead.
        var notes = new HashMap<String, Component>();
        if (skills) providers.forEach(provider -> { var note = provider.note(); if (note != null) notes.put(provider.owner(), note); });
        var groups = new LinkedHashMap<String, Group>();
        if (skills && !searching) providers.forEach(provider -> groups.put(provider.owner(),
                new Group(key(provider.owner()), provider.name(), notes.get(provider.owner()), new ArrayList<>())));
        filtered.forEach(entry -> groups.computeIfAbsent(entry.owner(), owner -> new Group(key(owner), entry.ownerName(),
                notes.get(owner), new ArrayList<>())).entries().add(entry));
        var ordered = new ArrayList<>(groups.values());
        ordered.sort(Comparator.comparing(Group::keysOnly).thenComparing(group -> group.name().getString()));
        var result = new ArrayList<Row>();
        int top = 0;
        for (var group : ordered) {
            var entries = group.entries();
            if (knownGroups.add(group.key()) && source != Source.VANILLA && group.keysOnly()) collapsed.add(group.key());
            boolean expanded = searching || !collapsed.contains(group.key());
            int height = group.note() == null ? 18 : 29;
            result.add(new Row(group.key(), group.name(), entries.size(), null,
                    cardGrid.x(), top, cardGrid.width(), height, List.of(), group.note()));
            top += height + (expanded && !entries.isEmpty() ? 4 : 0);
            if (expanded && !entries.isEmpty()) {
                for (int i = 0; i < entries.size(); i++) {
                    var entry = entries.get(i);
                    result.add(new Row(group.key(), entry.name(), 0, entry,
                            cardGrid.cardX(i % cardGrid.columns()), top + (i / cardGrid.columns()) * cardGrid.rowPitch(),
                            cardGrid.cardWidth(), cardGrid.cardHeight(), nameLines(entry.name(), cardGrid.cardWidth(), cardGrid.cardHeight()), null));
                }
                int count = (entries.size() + cardGrid.columns() - 1) / cardGrid.columns();
                top += count * cardGrid.rowPitch() - cardGrid.gap();
            }
            top += 6;
        }
        contentHeight = Math.max(0, top - 6);
        display = List.copyOf(result); offset = Math.clamp(offset, 0, maxOffset());
    }
    private String key(String owner) { return skills ? "skills/" + owner : owner; }
    /**
     * Every skill mod once, ordered by name, with the names of what gives its skills, as the mod lists them. A mod that
     * lists none gets no line. Until 0.1.5-beta the line was the first status among the mod's skills, hidden ones
     * included, so one hidden skill's requirement stood over the skills that were usable (2026-10-29, user's
     * screenshot): one skill's message must not speak for the whole mod.
     */
    static List<Provider> providers(List<ActionCatalog.Entry> catalog) {
        var names = new LinkedHashMap<String, Component>();
        for (var entry : catalog) if (entry.catalog().kind() == ActionCatalogInfo.Kind.SKILL) names.putIfAbsent(entry.owner(), entry.ownerName());
        var found = new ArrayList<Provider>();
        names.forEach((owner, name) -> found.add(new Provider(owner, name, FingertipApi.sources(owner))));
        return found.stream().sorted(Comparator.comparing((Provider provider) -> provider.name().getString())).toList();
    }
    private int rowsY() { return ui.listY + 23; }
    private int viewportHeight() { return Math.max(1, ui.bodyBottom - rowsY()); }
    private int maxOffset() { return Math.max(0, contentHeight - viewportHeight()); }
    private int thumbHeight() { return Math.min(viewportHeight(), Math.max(8, viewportHeight() * viewportHeight() / Math.max(1, contentHeight))); }
    boolean beginScroll(double x, double y) {
        if (maxOffset() == 0 || !EditorGeometry.inside(x, y,
                ui.rightX + ui.rightWidth - 8, rowsY(), 8, viewportHeight())) return false;
        int thumbY = rowsY() + (viewportHeight() - thumbHeight()) * offset / maxOffset();
        scrollGrab = y >= thumbY && y < thumbY + thumbHeight() ? y - thumbY : thumbHeight() / 2.0;
        scrolling = true; dragScroll(y); return true;
    }
    boolean dragScroll(double y) {
        if (!scrolling) return false;
        double progress = (y - rowsY() - scrollGrab) / Math.max(1, viewportHeight() - thumbHeight());
        offset = (int)Math.round(Math.clamp(progress, 0, 1) * maxOffset());
        return true;
    }
    void endScroll() { scrolling = false; }
    private Row rowAt(double x, double y) {
        if (!EditorGeometry.inside(x, y, cardGrid.x(), rowsY(), cardGrid.width(), viewportHeight())) return null;
        double contentY = y - rowsY() + offset;
        for (var row : display)
            if (EditorGeometry.inside(x, contentY, row.x(), row.top(), row.width(), row.height())) return row;
        return null;
    }
    /** A skill mod's switches, in the order registered: keys it takes back, layers of its own it shows. */
    private static List<FingertipApi.ModSwitch> switchesOf(String owner) {
        if (owner == null) return List.of();
        var list = new ArrayList<FingertipApi.ModSwitch>();
        for (var modSwitch : FingertipApi.switches()) if (modSwitch.owner().equals(owner)) list.add(modSwitch);
        return list;
    }
    private static boolean on(FingertipApi.ModSwitch modSwitch) {
        return switch (modSwitch) {
            case FingertipApi.KeySwitch keySwitch -> ModKeyWhitelist.switchOn(keySwitch);
            case FingertipApi.LayerSwitch layerSwitch -> SwitchedLayers.shown(layerSwitch);
        };
    }
    private static Component switchText(FingertipApi.ModSwitch modSwitch) {
        return modSwitch.label().copy().append(Component.translatable(on(modSwitch)
                ? "poris_elegies_fingertip_incantations.switch_on" : "poris_elegies_fingertip_incantations.switch_off"));
    }
    private int switchWidth(FingertipApi.ModSwitch modSwitch) { return font.width(switchText(modSwitch)) + 8; }
    /** A small switch right-aligned at {@code right}, text top at {@code y}; returns the width it took. */
    private int drawSwitch(GuiGraphics g, FingertipApi.ModSwitch modSwitch, int right, int y, int mx, int my) {
        var text = switchText(modSwitch);
        int width = switchWidth(modSwitch), x = right - width;
        boolean hover = EditorGeometry.inside(mx, my, x, y - 2, width, 12);
        g.fill(x, y - 2, right, y + 10, hover ? EditorTheme.palette().hover() : EditorTheme.palette().surface());
        g.drawString(font, text, x + 4, y, on(modSwitch) ? EditorTheme.accent() : EditorTheme.MUTED, false);
        switchHits.add(new SwitchHit(x, y - 2, right, y + 10, modSwitch));
        return width;
    }
    /** A mod's switches side by side, the last one against {@code right}; returns the width taken with a 4 px gap each. */
    private int drawSwitches(GuiGraphics g, List<FingertipApi.ModSwitch> switches, int right, int y, int mx, int my) {
        int used = 0;
        for (int i = switches.size() - 1; i >= 0; i--) used += drawSwitch(g, switches.get(i), right - used, y, mx, my) + 4;
        return used;
    }
    private boolean overSwitches(List<FingertipApi.ModSwitch> switches, int right, int y, int mx, int my) {
        int width = 0;
        for (var modSwitch : switches) width += switchWidth(modSwitch) + 4;
        return width > 0 && EditorGeometry.inside(mx, my, right - width + 4, y - 2, width - 4, 12);
    }
    private SwitchHit switchAt(double x, double y) {
        for (var hit : switchHits) if (x >= hit.x0() && x < hit.x1() && y >= hit.y0() && y < hit.y1()) return hit;
        return null;
    }
    /** A click on a skill mod's switch flips it (2026-10-28: Iron's spell wheel keys; 10-29: Iron's spell bar). */
    boolean toggleSwitch(double x, double y) {
        var hit = switchAt(x, y);
        if (hit == null) return false;
        boolean on = on(hit.modSwitch());
        switch (hit.modSwitch()) {
            case FingertipApi.KeySwitch keySwitch -> ModKeyWhitelist.setKeys(keySwitch.keys(), !on);
            case FingertipApi.LayerSwitch layerSwitch -> SwitchedLayers.set(layerSwitch, !on);
        }
        return true;
    }
    boolean toggleGroup(double x, double y) {
        Row row = rowAt(x, y);
        if (row == null || row.entry() != null) return false;
        // Search exposes every match without discarding the user's normal collapsed groups.
        if (search.getValue().isBlank() && row.count() > 0) {
            if (!collapsed.remove(row.group())) collapsed.add(row.group());
            rebuild();
        }
        return true;
    }
    ActionCatalog.Entry at(double x, double y) {
        Row row = rowAt(x, y); return row == null ? null : row.entry();
    }
    List<Component> tooltipAt(double x, double y) {
        var hit = switchAt(x, y);
        if (hit != null) return List.of(TooltipStyle.title(switchText(hit.modSwitch())), TooltipStyle.muted(hit.modSwitch().hint()));
        Row row = rowAt(x, y);
        if (row == null) return List.of();
        if (row.entry() != null) {
            var lines = new ArrayList<>(row.entry().tooltip());
            lines.add(TooltipStyle.muted(Component.translatable("poris_elegies_fingertip_incantations.bind_action_hint"))); return lines;
        }
        var lines = new ArrayList<Component>(List.of(TooltipStyle.title(row.title())));
        // A skill mod's header lists what gives its skills one per line; the row shows them joined and may cut them.
        var provider = skills && row.group().startsWith("skills/") ? providers.stream()
                .filter(p -> p.owner().equals(row.group().substring(7))).findFirst().orElse(null) : null;
        if (provider != null && !provider.sources().isEmpty()) provider.sources().forEach(source -> lines.add(TooltipStyle.body(source)));
        else if (row.note() != null) lines.add(TooltipStyle.body(row.note()));
        String hint = !search.getValue().isBlank() ? "poris_elegies_fingertip_incantations.mod_group_search" : skills ? "poris_elegies_fingertip_incantations.skill_group_hint" : "poris_elegies_fingertip_incantations.mod_group_hint";
        if (row.count() > 0) lines.add(TooltipStyle.muted(Component.translatable(hint, row.count())));
        return lines;
    }
    boolean scroll(double x, double y, double direction) {
        if (!EditorGeometry.inside(x, y, ui.rightX, ui.listY, ui.rightWidth, ui.bodyBottom - ui.listY)) return false;
        offset = Math.clamp(offset - (int)Math.signum(direction) * (cardGrid.rowPitch() / 2), 0, maxOffset()); return true;
    }
    void render(GuiGraphics g, int mx, int my, String selectedId, String draggedId) {
        switchHits.clear();
        g.enableScissor(cardGrid.x(), rowsY(), cardGrid.x() + cardGrid.width(), rowsY() + viewportHeight());
        Row hovered = rowAt(mx, my);
        for (Row row : display) {
            int y = rowsY() + row.top() - offset;
            if (y + row.height() <= rowsY() || y >= rowsY() + viewportHeight()) continue;
            boolean hover = hovered == row;
            if (row.entry() == null) {
                String count = Integer.toString(row.count());
                // A skill mod's switches go on its status line, or beside the count when it has none.
                var switches = skills && row.group().startsWith("skills/") ? switchesOf(row.group().substring(7)) : List.<FingertipApi.ModSwitch>of();
                int switchRight = row.note() == null ? row.x() + row.width() - 9 - font.width(count) : row.x() + row.width() - 5;
                int switchY = row.note() == null ? y + 5 : y + 16;
                // On the switch only the switch lights up, not its mod's row as well.
                boolean onSwitch = overSwitches(switches, switchRight, switchY, mx, my);
                g.fill(row.x(), y, row.x() + row.width(), y + row.height(), hover && !onSwitch ? EditorTheme.palette().hover() : EditorTheme.palette().raised());
                String marker = search.getValue().isBlank() && collapsed.contains(row.group()) ? "+" : "-";
                // Drawn once, on the status line or, without one, the title line; only that line makes room for them.
                int switchesWidth = drawSwitches(g, switches, switchRight, switchY, mx, my);
                int onTitle = row.note() == null ? switchesWidth : 0;
                if (row.count() > 0) g.drawString(font, marker, row.x() + 4, y + 5, EditorTheme.accent(), false);
                g.drawString(font, font.substrByWidth(row.title(), row.width() - 28 - font.width(count) - onTitle).getString(),
                        row.x() + 14, y + 5, EditorTheme.accent(), false);
                g.drawString(font, count, row.x() + row.width() - 5 - font.width(count), y + 5, EditorTheme.MUTED, false);
                if (row.note() != null) g.drawString(font, font.substrByWidth(row.note(), row.width() - 19 - switchesWidth).getString(),
                        row.x() + 14, y + 16, EditorTheme.TEXT, false);
                continue;
            }
            var entry = row.entry(); var state = entry.state();
            boolean available = state.available() && !state.lacksResource();
            EditorTheme.buttonFace(g, row.x(), y, row.width(), row.height(), available, hover, entry.id().equals(selectedId));
            int iconX = row.x() + (row.width() - 16) / 2;
            boolean dim = !available || entry.id().equals(draggedId);
            if (!ActionIcons.draw(g, entry.sprite(), entry.icon(), iconX, y + 6, dim) && dim) {
                g.pose().pushPose(); g.pose().translate(0, 0, 220);
                g.fill(iconX, y + 6, iconX + 16, y + 22, 0x99504A59);
                g.pose().popPose();
            }
            var lines = row.lines();
            for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                var line = lines.get(lineIndex);
                int textY = y + (lines.size() == 1 ? 30 : 26) + lineIndex * font.lineHeight;
                g.drawString(font, line.text(), row.x() + (row.width() - line.width()) / 2, textY,
                        !available ? EditorTheme.MUTED : hover || entry.id().equals(selectedId) ? EditorTheme.accent() : EditorTheme.TEXT, false);
            }
        }
        // A search with no result says so. A skill mod that gives nothing yet says why on its own card, if it registered a
        // hint (FingertipApi.registerEmptyHint); Fingertip adds no line of its own (2026-10-30, user: "其实这里不需要提示").
        if (filtered.isEmpty() && !(skills && search.getValue().isBlank())) {
            int lineY = rowsY() + 5 - offset;
            for (var line : font.split(Component.translatable("poris_elegies_fingertip_incantations.no_matches"), ui.rightWidth - 14)) {
                if (lineY + font.lineHeight > ui.bodyBottom) break;
                g.drawString(font, line, ui.rightX + 7, lineY, EditorTheme.MUTED, false); lineY += font.lineHeight + 2;
            }
        }
        g.disableScissor();
        if (maxOffset() > 0) {
            int track = viewportHeight(), thumb = thumbHeight();
            int y = rowsY() + (track - thumb) * offset / maxOffset();
            g.fill(ui.rightX + ui.rightWidth - 4, rowsY(), ui.rightX + ui.rightWidth - 2, rowsY() + track, EditorTheme.palette().surface());
            g.fill(ui.rightX + ui.rightWidth - 4, y, ui.rightX + ui.rightWidth - 2, y + thumb, EditorTheme.palette().border());
        }
    }
    /** At most two centred lines, the second cut with an ellipsis; the inset matches the card face drawn under it. */
    private List<NameLine> nameLines(Component name, int width, int height) {
        int textWidth = Math.max(10, width - Math.max(6, EditorTheme.textInset(width, height)) * 2);
        var split = font.split(name, textWidth);
        var lines = new ArrayList<NameLine>(2);
        for (int index = 0; index < Math.min(2, split.size()); index++) {
            var text = index == 1 && split.size() > 2 ? ellipsis(split.get(index), textWidth) : split.get(index);
            lines.add(new NameLine(text, font.width(text)));
        }
        return List.copyOf(lines);
    }
    private FormattedCharSequence ellipsis(FormattedCharSequence line, int maxWidth) {
        var text = new StringBuilder();
        line.accept((index, style, codePoint) -> { text.appendCodePoint(codePoint); return true; });
        return FormattedCharSequence.forward(GuiPixels.cut(font, text.toString(), Math.max(1, maxWidth - font.width(GuiPixels.ELLIPSIS))), Style.EMPTY);
    }
}
