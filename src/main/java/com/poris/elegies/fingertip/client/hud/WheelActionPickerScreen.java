package com.poris.elegies.fingertip.client.hud;

import com.poris.elegies.fingertip.api.ActionCatalogInfo;
import com.poris.elegies.fingertip.client.input.InputPreferences;
import com.poris.elegies.fingertip.client.input.OptionalKeyPolicy;
import com.poris.elegies.fingertip.client.input.WheelActionRef;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/** Choosing a card changes only the selected wheel sector, then returns to the wheel. */
public final class WheelActionPickerScreen extends Screen {
    private static final int CARD_GAP = 6;
    private record Choice(String id, Component label, ItemStack icon, ActionIcons.Icon sprite, Component owner, String ownerId,
                          Component hint) {}
    private record Row(String group, Component label, Choice choice, int count) {}
    private final WheelConfigurationScreen parent;
    private final int sector;
    private final Set<String> expanded = new HashSet<>();
    private final List<Button> rows = new ArrayList<>();
    private List<Choice> choices = List.of();
    private List<Row> visibleRows = List.of();
    private List<List<Row>> bands = List.of();
    private EditBox search;
    private Button previous, next;
    private boolean mods;
    private String query = "";
    private int page, capacity, columns, cardWidth, cardHeight, rowPitch, x, y, w, h, rowY;

    public WheelActionPickerScreen(WheelConfigurationScreen parent, int sector) {
        super(Component.translatable("poris_elegies_fingertip_incantations.wheel.picker_title", sector + 1));
        this.parent = parent; this.sector = sector;
    }
    @Override protected void init() {
        w = Math.min(620, width - 24); h = Math.min(344, height - 16);
        x = (width - w) / 2; y = (height - h) / 2;
        // 36 high from 270 high up, 30 below (1.1.6, with the editor's normal size; 1.1.5 had 30 everywhere).
        cardHeight = height < 270 ? 30 : 36; rowPitch = cardHeight + CARD_GAP;
        rowY = y + 79; capacity = Math.max(1, (h - 112) / rowPitch);
        columns = w >= 480 ? 3 : 2;
        cardWidth = (w - 24 - CARD_GAP * (columns - 1)) / columns;
        int tw = (w - 32) / 2;
        addRenderableWidget(EditorTheme.button(x + 12, y + 31, tw, 20, tr("native_functions"), b -> selectCategory(false), () -> !mods));
        addRenderableWidget(EditorTheme.button(x + 20 + tw, y + 31, tw, 20, tr("mod_functions"), b -> selectCategory(true), () -> mods));
        search = addRenderableWidget(new EditBox(font, x + 14, y + 58, w - 28, 16, tr("function_search")));
        search.setMaxLength(80); search.setHint(tr("function_search")); search.setValue(query);
        search.setResponder(value -> { query = value; page = 0; rebuildRows(); });
        addRenderableWidget(EditorTheme.button(x + 12, y + h - 27, 56, 19, tr("clear"), b -> confirmClear()));
        previous = addRenderableWidget(EditorTheme.button(x + w / 2 - 40, y + h - 27, 22, 19, Component.literal("‹"), b -> { page--; rebuildRows(); }));
        next = addRenderableWidget(EditorTheme.button(x + w / 2 + 18, y + h - 27, 22, 19, Component.literal("›"), b -> { page++; rebuildRows(); }));
        addRenderableWidget(EditorTheme.button(x + w - 68, y + h - 27, 56, 19, tr("back_to_wheel"), b -> onClose()));
        loadChoices(); rebuildRows();
    }
    private static Component tr(String key, Object... args) { return Component.translatable("poris_elegies_fingertip_incantations.wheel." + key, args); }
    private void selectCategory(boolean mods) { this.mods = mods; query = ""; page = 0; rebuildWidgets(); }
    private void loadChoices() {
        if (!mods) {
            choices = InputPreferences.ACTIONS.stream().filter(id -> !id.equals("none"))
                    .map(id -> new Choice(id, RadialActions.label(id), RadialActions.icon(id), RadialActions.sprite(id), Component.literal("Minecraft"), "minecraft", tr("native_choice_hint"))).toList();
            return;
        }
        choices = ActionCatalog.entries().stream()
                .filter(e -> e.catalog().kind() == ActionCatalogInfo.Kind.FUNCTION && e.catalog().visible() && !e.owner().equals("minecraft"))
                .sorted(Comparator.comparing((ActionCatalog.Entry e) -> e.ownerName().getString()).thenComparing(e -> e.name().getString()))
                .map(e -> new Choice(WheelActionRef.fromCatalogId(e.id()), e.name(), e.icon(), e.sprite(), e.ownerName(), e.owner(),
                        e.action() == null ? Component.translatable("poris_elegies_fingertip_incantations.mapping_compatibility") : e.category())).toList();
    }
    private void rebuildRows() {
        rows.forEach(this::removeWidget); rows.clear();
        boolean searching = !query.isBlank();
        var filtered = choices.stream().filter(c -> ListSearch.matches(query,
                c.label().getString() + " " + c.owner().getString() + " " + c.ownerId() + " " + c.id())).toList();
        var result = new ArrayList<Row>();
        if (!mods) filtered.forEach(c -> result.add(new Row("", c.label(), c, 0)));
        else {
            var groups = new LinkedHashMap<String, List<Choice>>();
            filtered.forEach(c -> groups.computeIfAbsent(c.ownerId(), ignored -> new ArrayList<>()).add(c));
            for (var group : groups.entrySet()) {
                result.add(new Row(group.getKey(), group.getValue().getFirst().owner(), null, group.getValue().size()));
                if (searching || expanded.contains(group.getKey()))
                    group.getValue().forEach(c -> result.add(new Row(group.getKey(), c.label(), c, 0)));
            }
        }
        visibleRows = List.copyOf(result);
        var grouped = new ArrayList<List<Row>>();
        var band = new ArrayList<Row>();
        for (Row row : visibleRows) {
            if (row.choice() == null) {
                if (!band.isEmpty()) { grouped.add(List.copyOf(band)); band.clear(); }
                grouped.add(List.of(row));
            } else {
                band.add(row);
                if (band.size() == columns) { grouped.add(List.copyOf(band)); band.clear(); }
            }
        }
        if (!band.isEmpty()) grouped.add(List.copyOf(band));
        bands = List.copyOf(grouped);
        page = ListSearch.clampPage(page, pageCount(), previous, next);
        for (int i = page * capacity; i < Math.min(bands.size(), (page + 1) * capacity); i++) {
            var line = bands.get(i); int py = rowY + (i - page * capacity) * rowPitch;
            for (int column = 0; column < line.size(); column++) {
                Row row = line.get(column); Button b;
                if (row.choice() == null) {
                    String marker = !searching && !expanded.contains(row.group()) ? "+ " : "− ";
                    b = EditorTheme.button(x + 12, py, w - 24, cardHeight,
                            Component.literal(marker).append(row.label()).append("  ·  " + row.count()),
                            ignored -> { if (!expanded.remove(row.group())) expanded.add(row.group()); rebuildRows(); }, () -> expanded.contains(row.group()));
                } else b = choiceButton(row.choice(), x + 12 + column * (cardWidth + CARD_GAP), py);
                rows.add(addRenderableWidget(b));
            }
        }
    }
    private int pageCount() { return ListSearch.pages(bands.size(), capacity); }
    private Button choiceButton(Choice choice, int px, int py) {
        var b = new Button(px, py, cardWidth, cardHeight, choice.label(), ignored -> assign(choice.id()), java.util.function.Supplier::get) {
            @Override protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
                boolean hover = EditorTheme.hoverOrKeyboardFocus(this);
                boolean selected = OptionalKeyPolicy.settings().wheel.get(sector).equals(choice.id());
                int inset = EditorTheme.buttonFace(g, getX(), getY(), getWidth(), getHeight(), active, hover, selected);
                ActionIcons.draw(g, choice.sprite(), choice.icon(), getX() + inset + 2, getY() + (getHeight() - 16) / 2);
                int textWidth = getWidth() - inset * 2 - 26;
                // Two lines at most, the second ending with "…" when the name is longer (指尖排字法); the hover text has it whole.
                var lines = TextLines.wrap(choice.label().getString(), textWidth, 2, font::width).lines();
                int lineCount = lines.size();
                int textY = getY() + (getHeight() - lineCount * font.lineHeight) / 2 + 1;
                for (int line = 0; line < lineCount; line++)
                    g.drawString(font, lines.get(line), getX() + inset + 24, textY + line * font.lineHeight,
                            hover || selected ? EditorTheme.accent() : EditorTheme.TEXT, false);
            }
        };
        b.setTooltip(Tooltip.create(choice.label().copy().append("\n").append(choice.owner()).append("\n").append(choice.hint()).append("\n").append(tr("pulse_hint"))));
        return b;
    }
    private void assign(String id) {
        var input = OptionalKeyPolicy.settings();
        if (input.lockedWheelSlot(sector)) return;
        input.wheel.set(sector, id); OptionalKeyPolicy.changed(); parent.assigned(sector, id); minecraft.setScreen(parent);
    }
    private void confirmClear() {
        minecraft.setScreen(new ThemedConfirmScreen(confirmed -> {
            if (confirmed) assign("none"); else minecraft.setScreen(this);
        }, tr("clear_confirm", sector + 1), tr("clear_detail")));
    }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, 0x99100F19); EditorTheme.panel(g, x, y, w, h, true);
    }
    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        g.drawString(font, title, x + 14, y + 12, EditorTheme.accent(), false);
        if (visibleRows.isEmpty()) TextFit.drawLines(g, font, Component.translatable("poris_elegies_fingertip_incantations.no_matches"), x + 16, rowY + 6,
                w - 32, Math.max(1, TextFit.linesIn(font, capacity * rowPitch - 6, 9)), 9, EditorTheme.MUTED, false);
        int pages = pageCount();
        g.drawCenteredString(font, (page + 1) + "/" + pages, x + w / 2, y + h - 21, EditorTheme.MUTED);
    }
    @Override public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (!EditorGeometry.inside(mx, my, x, rowY, w, capacity * rowPitch)) return super.mouseScrolled(mx, my, dx, dy);
        page -= (int)Math.signum(dy); rebuildRows(); return true;
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { minecraft.setScreen(parent); }
}
