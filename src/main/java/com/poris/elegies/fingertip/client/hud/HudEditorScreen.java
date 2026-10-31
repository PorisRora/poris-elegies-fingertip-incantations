package com.poris.elegies.fingertip.client.hud;

import com.poris.elegies.fingertip.api.inventory.*;

import com.mojang.blaze3d.platform.InputConstants;
import com.poris.elegies.fingertip.Fingertip;
import com.poris.elegies.fingertip.client.ClientSession;
import com.poris.elegies.fingertip.client.BindingFeedback;
import com.poris.elegies.fingertip.client.input.HotbarControl;
import com.poris.elegies.fingertip.client.input.OptionalKeyPolicy;
import com.poris.elegies.fingertip.client.input.ShortcutKeys;
import com.poris.elegies.fingertip.client.input.ShortcutKeyRules;
import com.poris.elegies.fingertip.client.input.ShortcutConfiguration;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Binding references and layout editing have separate surfaces; neither moves inventory stacks. */
public final class HudEditorScreen extends Screen {
    // An explicit positioner also keeps Modern UI's replacement tooltip renderer inside the viewport.
    private static final net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner TOOLTIP_POSITION =
            (w, h, mx, my, tw, th) -> {
                int x = mx + 12 + tw + 6 <= w ? mx + 12 : mx - tw - 12;
                return new org.joml.Vector2i(Math.clamp(x, 6, Math.max(6, w - tw - 6)),
                        Math.clamp(my - 12, 6, Math.max(6, h - th - 6)));
            };
    private final Screen parent;
    /** Ticks since the screen opened; the action list looks for new or changed skills once a second. */
    private int age;
    private int selected;
    private boolean layoutOnly;
    private boolean actions, skills, nativeActions, capture, positioning;
    private boolean hasSkills, canvasOnly;
    private final LayoutSelection selection = new LayoutSelection();
    private Component keyError;
    private final BooleanSupplier primaryButtonDown;
    private final EditorDrag drag = new EditorDrag();
    private EditorActionList actionList;
    private EditorGeometry ui;
    private LayoutToolbarGeometry toolbar;
    private Button addButton, positionButton, skillButton, removeSelectionButton;

    public HudEditorScreen(Screen parent, int selected) {
        this(parent, selected, () -> GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(),
                GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS);
    }
    // Package-private input seam lets the isolated client exercise GUI-held presses across real ticks.
    HudEditorScreen(Screen parent, int selected, BooleanSupplier primaryButtonDown) {
        super(tr("poris_elegies_fingertip_incantations.editor")); this.parent = parent; this.selected = selected; this.primaryButtonDown = primaryButtonDown;
    }
    static HudEditorScreen layout(Screen parent, int selected) {
        var screen = new HudEditorScreen(parent, selected);
        screen.layoutOnly = screen.positioning = true;
        screen.selection.single(selected);
        return screen;
    }
    private static Component tr(String key, Object... args) { return Component.translatable(key, args); }
    /** Why a slot's key is paused, naming the key when a takeover switch is off (2026-10-28, user: keep it short). */
    private static Component pausedReason(InputConstants.Key key) {
        if (OptionalKeyPolicy.wheelKey(key)) return tr("poris_elegies_fingertip_incantations.shortcut_paused_wheel");
        if (OptionalKeyPolicy.passthrough(key)) return tr("poris_elegies_fingertip_incantations.shortcut_paused_takeover", key.getDisplayName());
        return tr("poris_elegies_fingertip_incantations.shortcut_paused_hint");
    }
    private HudLayout layout() { return HudLayout.INSTANCE; }
    private Button button(String key, int x, int y, int w, Button.OnPress press) {
        return addRenderableWidget(EditorTheme.button(x, y, w, 18, tr(key), press));
    }
    private Button toolbarButton(int index, String key, ActionIcons.Sprite icon, Button.OnPress press) {
        return addRenderableWidget(EditorTheme.button(ui.toolbarX(index), ui.toolbarY,
                ui.toolbarWidth(), ui.toolbarButtonHeight, tr(key), icon, press));
    }
    @Override protected void init() {
        cancelDrag(); capture = false;
        ui = new EditorGeometry(width, height, layout().activeSlots().size());
        selection.linked(HudLayout.INSTANCE.takeOverHotbar ? java.util.Set.of()
                : java.util.stream.IntStream.range(0, 9).boxed().collect(java.util.stream.Collectors.toSet()));
        ensureSelection();
        toolbar = positioning ? new LayoutToolbarGeometry(width) : null;
        if (canvasOnly) {
            var p = canvasRestorePosition();
            button("poris_elegies_fingertip_incantations.canvas_return", p[0], p[1], 100, b -> toggleCanvas())
                    .setTooltip(Tooltip.create(tr("poris_elegies_fingertip_incantations.canvas_hint")));
            return;
        }
        if (positioning) {
            button("gui.done", toolbar.doneX(), toolbar.y + 9, LayoutToolbarGeometry.DONE_WIDTH, b -> finishPositioning());
            addButton = button("poris_elegies_fingertip_incantations.add_short", toolbar.groupX(0), toolbar.buttonY(0), 44, b -> addSlot());
            button("poris_elegies_fingertip_incantations.select_all", toolbar.groupX(0) + 48, toolbar.buttonY(0), 44,
                    b -> { cancelDrag(); selection.all(layout().layoutSlots()); });
            button("poris_elegies_fingertip_incantations.horizontal", toolbar.groupX(1), toolbar.buttonY(1), 40, b -> arrangeLine(true));
            button("poris_elegies_fingertip_incantations.vertical", toolbar.groupX(1) + 44, toolbar.buttonY(1), 40, b -> arrangeLine(false));
            var spacing = addRenderableWidget(EditorTheme.button(toolbar.groupX(2), toolbar.buttonY(2), 48, 18,
                    tr("poris_elegies_fingertip_incantations.spacing", layout().gap), b -> {
                        layout().gap = (layout().gap + 2) % 10;
                        b.setMessage(tr("poris_elegies_fingertip_incantations.spacing", layout().gap)); layout().save();
                    }));
            spacing.setTooltip(Tooltip.create(tr("poris_elegies_fingertip_incantations.spacing_hint")));
            button("poris_elegies_fingertip_incantations.arrange", toolbar.groupX(1) + 88, toolbar.buttonY(1), 40, b -> {
                cancelDrag();
                var slots = arrangeable();
                if (slots.isEmpty()) { BindingFeedback.hint(-1, "poris_elegies_fingertip_incantations.select_first"); return; }
                applyArrangement(slots, HudArrangement.arrange(HudArrangement.positions(slots, width, height),
                        layout().gap, width, height));
            }).setTooltip(Tooltip.create(tr("poris_elegies_fingertip_incantations.arrange_hint")));
            button("poris_elegies_fingertip_incantations.canvas_short", toolbar.groupX(3), toolbar.buttonY(3), 48, b -> toggleCanvas())
                    .setTooltip(Tooltip.create(tr("poris_elegies_fingertip_incantations.canvas_hint")));
            addRenderableWidget(EditorTheme.button(toolbar.groupX(2) + 52,
                    toolbar.buttonY(2), 80, 18, tr(layout().snapToSlots ? "poris_elegies_fingertip_incantations.snap_on" : "poris_elegies_fingertip_incantations.snap_off"), b -> {
                        cancelDrag();
                        layout().snapToSlots = !layout().snapToSlots; layout().save();
                        b.setMessage(tr(layout().snapToSlots ? "poris_elegies_fingertip_incantations.snap_on" : "poris_elegies_fingertip_incantations.snap_off"));
                    }, () -> layout().snapToSlots)).setTooltip(Tooltip.create(tr("poris_elegies_fingertip_incantations.snap_hint")));
            removeSelectionButton = button("poris_elegies_fingertip_incantations.remove_selection",
                    toolbar.groupX(3) + 52, toolbar.buttonY(3), 64,
                    b -> removeSlots(selection.slots(layout().activeSlots())));
            removeSelectionButton.setTooltip(Tooltip.create(tr("poris_elegies_fingertip_incantations.remove_selection_hint")));
            updateControls();
            return;
        }
        var enableLabel = tr(HudLayout.INSTANCE.enabled ? "poris_elegies_fingertip_incantations.enabled_short" : "poris_elegies_fingertip_incantations.disabled_short");
        addRenderableWidget(EditorTheme.button(ui.enableX(), ui.y + ui.headerInset, ui.enableWidth(), ui.toolbarButtonHeight,
                enableLabel, b -> { HotbarControl.setEnabled(!HudLayout.INSTANCE.enabled); rebuildWidgets(); },
                () -> HudLayout.INSTANCE.enabled)).setTooltip(Tooltip.create(tr("poris_elegies_fingertip_incantations.enable_hint")));
        addRenderableWidget(HotbarModeButton.create(ui.modeX(), ui.y + ui.headerInset,
                ui.modeWidth(), ui.toolbarButtonHeight, this::rebuildWidgets));
        addRenderableWidget(EditorTheme.button(ui.doneX(), ui.y + ui.headerInset,
                ui.doneWidth(), ui.toolbarButtonHeight, tr("gui.done"), b -> onClose()));
        positionButton = toolbarButton(0, ui.compact ? "poris_elegies_fingertip_incantations.slot_configuration_short" : "poris_elegies_fingertip_incantations.slot_configuration",
                ActionIcons.Sprite.SLOTS, b -> setPositioning(true));
        toolbarButton(1, "poris_elegies_fingertip_incantations.settings_display", ActionIcons.Sprite.DISPLAY,
                b -> minecraft.setScreen(new DisplaySettingsScreen(this)));
        toolbarButton(2, "poris_elegies_fingertip_incantations.settings_keys", ActionIcons.Sprite.KEYS,
                b -> minecraft.setScreen(new InputSettingsScreen(this)));
        toolbarButton(3, "poris_elegies_fingertip_incantations.settings_wheel", ActionIcons.Sprite.WHEEL,
                b -> minecraft.setScreen(new WheelConfigurationScreen(this))).setTooltip(Tooltip.create(tr("poris_elegies_fingertip_incantations.wheel.configure_hint")));
        skillButton = null;
        var catalog = ActionCatalog.entries();   // built once per rebuild and handed to the list below
        hasSkills = catalog.stream().anyMatch(e -> e.catalog().kind() == com.poris.elegies.fingertip.api.ActionCatalogInfo.Kind.SKILL);
        if (!hasSkills && skills) { skills = false; actions = false; }
        int tabCount = hasSkills ? 4 : 3;
        int tabWidth = (ui.rightWidth - (tabCount - 1) * 3) / tabCount;
        addRenderableWidget(EditorTheme.button(ui.rightX, ui.tabsY, tabWidth, 18, tr("poris_elegies_fingertip_incantations.items_short"),
                b -> { actions = false; capture = false; cancelDrag(); }, () -> !actions));
        addRenderableWidget(EditorTheme.button(ui.rightX + tabWidth + 3, ui.tabsY, tabWidth, 18, tr("poris_elegies_fingertip_incantations.vanilla_short"),
                b -> { nativeActions = true; selectActionPage(false); }, () -> actions && nativeActions && !skills));
        addRenderableWidget(EditorTheme.button(ui.rightX + (tabWidth + 3) * 2, ui.tabsY, tabWidth, 18, tr("poris_elegies_fingertip_incantations.mods_short"),
                b -> { nativeActions = false; selectActionPage(false); }, () -> actions && !nativeActions && !skills));
        if (hasSkills) skillButton = addRenderableWidget(EditorTheme.button(ui.rightX + (tabWidth + 3) * 3, ui.tabsY, tabWidth, 18, tr("poris_elegies_fingertip_incantations.skills"),
                b -> selectActionPage(true), () -> actions && skills));
        actionList = new EditorActionList(ui, font, actionList, skills ? EditorActionList.Source.SKILLS
                : nativeActions ? EditorActionList.Source.VANILLA : EditorActionList.Source.MODS, catalog);
        addRenderableWidget(actionList.search);
        updateControls();
    }
    private void setPositioning(boolean enabled) {
        if (!enabled) { finishPositioning(); return; }
        selection.single(selected);
        positioning = true; capture = false; cancelDrag(); rebuildWidgets();
    }
    private void finishPositioning() {
        cancelDrag(); positioning = false; capture = false; canvasOnly = false;
        layout().save(); ensureSelection();
        if (layoutOnly) minecraft.setScreen(parent); else rebuildWidgets();
    }
    private void addSlot() {
        var slot = newSlot(width, height);
        if (slot == null) return;
        selected = slot.id; selection.single(selected); rebuildWidgets();
    }
    /**
     * A new slot in free space, empty and without a key, for every page that adds one; null when all slots are in use.
     * Its key goes through ShortcutKeyRules.clear, so a press left over from the slot's last life is dropped too.
     */
    static HudLayout.Slot newSlot(int width, int height) {
        var slot = HudLayout.INSTANCE.add(width, height);
        if (slot == null) return null;
        ClientSession.unbind(slot.id, BindingFeedback.Operation.ADD); ShortcutKeyRules.clear(slot.id);
        KeyMapping.resetMapping(); ShortcutConfiguration.save();
        return slot;
    }
    private void toggleCanvas() { cancelDrag(); canvasOnly = !canvasOnly; rebuildWidgets(); }
    private int[] canvasRestorePosition() {
        for (int y = 6; y < height - 24; y += 24) for (int x : new int[]{width - 106, 6}) {
            final int px = x, py = y;
            if (layout().activeSlots().stream().noneMatch(s -> s.screenX(width) < px + 100 && s.screenX(width) + 20 > px
                    && s.screenY(height) < py + 18 && s.screenY(height) + 20 > py)) return new int[]{x, y};
        }
        return new int[]{width - 106, 6};
    }
    private void selectActionPage(boolean skills) {
        cancelDrag(); capture = false; actions = true;
        this.skills = skills; rebuildWidgets();
    }
    @Override public void tick() {
        if (++age % 20 == 0 && !positioning && !canvasOnly && !drag.active() && actionList != null) actionList.refresh();
        // MouseHandler.isLeftPressed only updates when no Screen is open: it cancels every GUI drag.
        // Poll GLFW's actual button state to keep held drags alive and recover consumed mouse-up events.
        if (!minecraft.isWindowActive() || !primaryButtonDown.getAsBoolean()) cancelDrag();
    }
    private void cancelDrag() {
        if (drag.layoutChanged) layout().save();
        drag.clear(); selection.cancel();
        if (actionList != null) actionList.endScroll();
    }
    /**
     * The binding page marks its slot with the same outline as the layout page; slots keep their gameplay frames, so the
     * one in hand stays lit (2026-10-31, user: "编辑器做好自己分内的事情就好了"). None after a click on blank canvas.
     */
    private void outlineSelected(GuiGraphics g) {
        g.pose().pushPose(); g.pose().translate(0, 0, 300);
        for (var slot : layout().activeSlots())
            if (slot.id == selected) SelectionFrame.slot(g, slot.screenX(width) - 1, slot.screenY(height) - 1, 22, 22);
        g.pose().popPose();
    }
    /** Blank canvas leaves the slot on both pages (2026-10-31, user: "鼠标点外面 指尖格的失焦选中"). */
    private void deselect() { selected = -1; selection.single(-1); }
    private void ensureSelection() {
        if (selected >= 0 && !layout().visible(selected))
            selected = layout().activeSlots().isEmpty() ? -1 : layout().activeSlots().getFirst().id;
    }
    /** {@code slots} come from the active slots, so the off-hand frame is never among them: it cannot be deleted. */
    private void removeSlots(List<HudLayout.Slot> slots) {
        if (slots.isEmpty() && selection.ids.contains(HudLayout.OFFHAND)) { BindingFeedback.hint(-1, "poris_elegies_fingertip_incantations.offhand_locked"); return; }
        if (!canBind() || slots.isEmpty() || drag.active() || selection.boxing()) return;
        var ids = slots.stream().filter(slot -> !HotbarControl.nativeSlot(slot.id)).map(slot -> slot.id)
                .collect(java.util.stream.Collectors.toSet());
        if (ids.isEmpty()) { BindingFeedback.hint(-1, "poris_elegies_fingertip_incantations.native_slot_locked"); return; }
        minecraft.setScreen(new ThemedConfirmScreen(confirmed -> {
            minecraft.setScreen(this);
            if (!confirmed || !canBind()) return;
            for (int id : ids) { ClientSession.unbind(id, BindingFeedback.Operation.REMOVE); ShortcutKeyRules.clear(id); }
            layout().remove(ids); selection.ids.removeAll(ids); ensureSelection();
            KeyMapping.resetMapping(); ShortcutConfiguration.save(); rebuildWidgets();
        }, tr("poris_elegies_fingertip_incantations.delete_slots_confirm", ids.size()), tr("poris_elegies_fingertip_incantations.delete_slots_detail")));
    }
    private void confirmClear(int id) {
        minecraft.setScreen(new ThemedConfirmScreen(confirmed -> {
            minecraft.setScreen(this);
            if (confirmed && canBind()) ClientSession.unbind(id);
        }, tr("poris_elegies_fingertip_incantations.clear_confirm", id + 1), tr("poris_elegies_fingertip_incantations.clear_confirm_detail")));
    }
    private boolean ready() { return ClientSession.ready() && minecraft.player != null; }
    private boolean canBind() { return ready() && minecraft.player.isAlive() && !minecraft.player.isSpectator(); }
    private void updateControls() {
        ensureSelection();
        if (canvasOnly) return;
        if (positioning) {
            // While the key box is open the toolbar goes dark, all but 删除: deleting the clicked slot is the one other thing
            // to do with it (2026-10-31, user: "点击以后上面全暗 删除所选亮", then "删除所选就可以改叫删除了"; the Delete key
            // stays a key a slot can take).
            for (var child : children())
                if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget && widget != removeSelectionButton) widget.active = !capture;
            if (!capture) addButton.active = canBind() && layout().activeSlots().size() < Fingertip.SLOT_COUNT;
            removeSelectionButton.active = canBind() && selection.ids.stream().anyMatch(id -> id != HudLayout.OFFHAND && !HotbarControl.nativeSlot(id))
                    && !selection.boxing() && !drag.active();
            return;
        }
        positionButton.active = ready();
        if (actionList == null) return;
        actionList.search.visible = actions;
        actionList.search.active = actions;
    }
    @Override public boolean isPauseScreen() { return false; }

    @Override public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        if (canvasOnly || positioning && drag.moved) return;
        if (drag.canvas) { g.fill(0, 0, width, height, 0x3010131C); return; }
        if (positioning) {
            EditorTheme.panel(g, toolbar.x, toolbar.y, toolbar.width, toolbar.height, true);
            return;
        }
        EditorTheme.panel(g, ui.x, ui.y, ui.width, ui.headerHeight, true);
        EditorTheme.panel(g, ui.rightX - ui.panelPad, ui.tabsY - ui.panelPad, ui.rightWidth + ui.panelPad * 2, ui.bodyBottom - ui.tabsY + ui.panelPad * 2, false);
    }
    // Minecraft draws a widget's tooltip after render() at depth 400, the depth this screen lifts its controls to, so
    // button labels showed through it. Keep the request and draw it last, in front of everything.
    private List<net.minecraft.util.FormattedCharSequence> widgetTooltip;
    private net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner widgetTooltipPosition;
    @Override public void setTooltipForNextRenderPass(List<net.minecraft.util.FormattedCharSequence> lines,
            net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner positioner, boolean override) {
        if (widgetTooltip == null || override) { widgetTooltip = lines; widgetTooltipPosition = positioner; }
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        widgetTooltip = null;
        renderEditor(g, mouseX, mouseY, partial);
        if (widgetTooltip == null) return;
        g.pose().pushPose(); g.pose().translate(0, 0, 500);
        g.renderTooltip(font, widgetTooltip, widgetTooltipPosition, mouseX, mouseY);
        g.pose().popPose(); widgetTooltip = null;
    }
    private void renderEditor(GuiGraphics g, int mouseX, int mouseY, float partial) {
        updateControls();
        if (canvasOnly && !positioning && !drag.canvas) {
            if (ready()) { HudRenderer.renderSlots(g); outlineSelected(g); }
            g.pose().pushPose(); g.pose().translate(0, 0, 400);
            super.render(g, mouseX, mouseY, partial); renderTooltip(g, mouseX, mouseY);
            g.pose().popPose(); return;
        }
        if (positioning) { renderPositioning(g, mouseX, mouseY, partial); return; }
        g.fill(0, 0, width, height, 0x4010131C);
        if (ready()) { HudRenderer.renderSlots(g); outlineSelected(g); }
        if (drag.canvas) {
            if (drag.fromHud >= 0) {
                // A slot's content: onto another slot they trade places, onto blank canvas it is cleared (2026-10-29,
                // user: "这个做个换位置？ 拖到空白处"; the "drag here to clear" box is gone).
                var target = slotAt(mouseX, mouseY);
                boolean swap = target != null && target.id != drag.fromHud && target.id != HudLayout.OFFHAND;
                if (swap && !HotbarControl.nativeSlot(target.id))
                    BindingPresentation.target(g, target.id, target.screenX(width), target.screenY(height), drag.icon, drag.sprite);
                BindingPresentation.banner(g, target == null ? tr("poris_elegies_fingertip_incantations.drag_clear", drag.fromHud + 1)
                        : !swap ? tr("poris_elegies_fingertip_incantations.drag_back")
                        : HotbarControl.nativeSlot(target.id) ? tr("poris_elegies_fingertip_incantations.native_slot_locked")
                        : tr("poris_elegies_fingertip_incantations.drag_swap", drag.fromHud + 1, target.id + 1), 9);
                renderDraggedIcon(g, mouseX, mouseY); return;
            }
            var target = slotAt(mouseX, mouseY);
            if (target != null) BindingPresentation.target(g, target.id, target.screenX(width), target.screenY(height), drag.icon, drag.sprite);
            BindingPresentation.banner(g, tr(target == null ? "poris_elegies_fingertip_incantations.drop_hint" : HotbarControl.nativeSlot(target.id)
                    ? "poris_elegies_fingertip_incantations.native_slot_locked" : "poris_elegies_fingertip_incantations.drop_target", target == null ? 0 : target.id + 1), 9);
            renderDraggedIcon(g, mouseX, mouseY); return;
        }
        g.pose().pushPose(); g.pose().translate(0, 0, 400);
        super.render(g, mouseX, mouseY, partial);
        // The star seal leads the title, as the layout page's icon leads its heading (2026-10-27, user).
        int sealRoom = ActionIcons.SMALL + EditorTheme.ICON_GAP;
        ActionIcons.Sprite.FINGERTIP.drawSmall(g, ui.x + ui.headerInset, ui.y + ui.headerInset + 2);
        g.drawString(font, font.plainSubstrByWidth(title.getString(), ui.titleWidth() - sealRoom),
                ui.x + ui.headerInset + sealRoom, ui.y + ui.headerInset + 4, EditorTheme.accent(), false);
        if (!ready()) {
            g.drawCenteredString(font, tr("poris_elegies_fingertip_incantations.enter_world"), width / 2, height / 2, EditorTheme.TEXT);
            g.pose().popPose(); return;
        }
        if (actions) {
            var binding = selected < 0 || HotbarControl.nativeSlot(selected)
                    ? com.poris.elegies.fingertip.binding.HudBinding.empty() : ClientSession.binding(selected);
            String id = binding.kind() == com.poris.elegies.fingertip.binding.HudBinding.Kind.MAPPING ? "mapping:" + binding.action() : binding.action();
            actionList.render(g, mouseX, mouseY, id, drag.moved ? drag.action : null);
        } else renderInventory(g, mouseX, mouseY);
        var notice = BindingFeedback.latest();
        var message = notice == null ? tr("poris_elegies_fingertip_incantations.real_canvas_hint") : notice.text();
        int feedbackY = feedbackY();
        if (feedbackY >= 0) g.drawCenteredString(font, font.plainSubstrByWidth(message.getString(), width - 20), width / 2, feedbackY,
                notice == null ? EditorTheme.MUTED : BindingPresentation.color(notice));
        renderTooltip(g, mouseX, mouseY);
        renderDraggedIcon(g, mouseX, mouseY);
        g.pose().popPose();
    }
    private int feedbackY() {
        int bottom = ui.bodyBottom + ui.panelPad + 4;
        for (int y = height - 12; y > bottom; y -= 12) {
            final int line = y;
            if (layout().activeSlots().stream().noneMatch(s -> s.screenY(height) - 6 < line + 10
                    && s.screenY(height) + 24 > line)) return line;
        }
        return -1;
    }
    private void renderPositioning(GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fill(0, 0, width, height, 0x1810131C);
        if (ready()) {
            HudRenderer.renderSlots(g);
            var offhand = layout().offhand();
            HudRenderer.renderOffhand(g, offhand.screenX(width), offhand.screenY(height), true);
            g.pose().pushPose(); g.pose().translate(0, 0, 300);
            SelectionFrame.guides(g, drag.guideX, drag.guideY, canvasOnly || drag.moved ? 0 : toolbar.canvasTop(), width, height);
            if (selection.boxing()) {
                var box = selection.bounds();
                SelectionFrame.marquee(g, box.left(), box.top(), box.width(), box.height());
            } else if (selection.ids.size() > 1) {
                var box = HudArrangement.bounds(HudArrangement.positions(selection.slots(layout().layoutSlots()), width, height));
                SelectionFrame.group(g, box.left() - 4, box.top() - 4, box.width() + 8, box.height() + 8);
            }
            for (var slot : selection.slots(layout().layoutSlots()))
                SelectionFrame.slot(g, slot.screenX(width) - 1, slot.screenY(height) - 1, 22, 22);
            g.pose().popPose();
        }
        if (drag.moved) return;
        if (canvasOnly) {
            g.pose().pushPose(); g.pose().translate(0, 0, 400);
            super.render(g, mouseX, mouseY, partial);
            if (capture) renderCapture(g, mouseX, mouseY, partial); else renderTooltip(g, mouseX, mouseY);
            g.pose().popPose(); return;
        }
        // Paint the toolbar above the canvas so slots cannot cover its controls.
        g.pose().pushPose(); g.pose().translate(0, 0, 350);
        super.render(g, mouseX, mouseY, partial);
        var heading = tr("poris_elegies_fingertip_incantations.layout_title");
        // The editor icon and the heading are centred as one group.
        int iconRoom = ActionIcons.SMALL + EditorTheme.ICON_GAP;
        int headingX = toolbar.x + (toolbar.width - font.width(heading) + iconRoom) / 2;
        ActionIcons.Sprite.EDITOR.drawSmall(g, headingX - iconRoom, toolbar.y + 12);
        g.drawString(font, heading, headingX, toolbar.y + 14, EditorTheme.accent(), false);
        int countX = toolbar.x + 12;
        g.drawString(font, font.plainSubstrByWidth(tr("poris_elegies_fingertip_incantations.selection_count", selection.ids.size()).getString(),
                Math.max(0, headingX - iconRoom - countX - 10)), countX, toolbar.y + 14, EditorTheme.MUTED, false);
        String[] groups = {"select", "arrange", "align", "manage"};
        for (int i = 0; i < groups.length; i++) {
            g.drawString(font, tr("poris_elegies_fingertip_incantations.layout_group_" + groups[i]), toolbar.groupX(i), toolbar.labelY(i), EditorTheme.MUTED, false);
            if (i > 0 && (!toolbar.compact || i % 2 == 1)) {
                int dividerX = toolbar.groupX(i) - 6;
                g.fill(dividerX, toolbar.labelY(i) + 1, dividerX + 1, toolbar.buttonY(i) + 16, 0x405F5264);
            }
        }
        if (capture) { renderCapture(g, mouseX, mouseY, partial); g.pose().popPose(); return; }
        g.pose().popPose();
        if (!ready()) g.drawCenteredString(font, tr("poris_elegies_fingertip_incantations.enter_world"), width / 2, toolbar.canvasTop(), EditorTheme.TEXT);
        BindingPresentation.feedback(g, height - 30);
        if (!drag.active() && !selection.boxing()
                && EditorGeometry.inside(mouseX, mouseY, headingX - 4, toolbar.y + 10, font.width(heading) + 8, 17))
            tooltip(g, List.of(tr("poris_elegies_fingertip_incantations.layout_help_hint")), mouseX, toolbar.canvasTop() + 12);
        else renderTooltip(g, mouseX, mouseY);
    }
    private void renderDraggedIcon(GuiGraphics g, int mouseX, int mouseY) {
        if ((drag.inventory < 0 && drag.fromHud < 0 && drag.action == null) || !drag.moved) return;
        g.pose().pushPose(); g.pose().translate(0, 0, 500);
        ActionIcons.draw(g, drag.sprite, drag.icon, mouseX + 13, mouseY - 10);
        g.pose().popPose();
    }
    private static final int CAPTURE_HEIGHT = 80;
    private int captureWidth() { return Math.min(380, width - 24); }
    private int captureY() { return Math.max(40, height / 2 - 40); }
    private void renderCapture(GuiGraphics g, int mouseX, int mouseY, float partial) {
        int w = captureWidth(), x = (width - w) / 2, y = captureY();
        g.fill(0, 0, width, height, 0x66100F19);
        // 删除 is drawn again above the shade: the one lit control on the dark toolbar (see updateControls).
        if (!canvasOnly && removeSelectionButton != null) removeSelectionButton.render(g, mouseX, mouseY, partial);
        EditorTheme.panel(g, x, y, w, CAPTURE_HEIGHT, true);
        g.drawString(font, tr("poris_elegies_fingertip_incantations.capture_slot", selected + 1), x + 12, y + 12, EditorTheme.accent(), false);
        // The prompt or the error takes two lines (what is wrong, then what to do); the controls sit in the bottom-left
        // corner, as far from the bottom as the title is from the top, so an error never hides Esc (2026-10-31, user).
        wrapped(g, keyError != null ? keyError : tr("poris_elegies_fingertip_incantations.capture_key"), x + 12, y + 31, w - 24, 20, EditorTheme.TEXT);
        g.drawString(font, tr("poris_elegies_fingertip_incantations.capture_controls"), x + 12, y + CAPTURE_HEIGHT - 12 - font.lineHeight, EditorTheme.MUTED, false);
    }
    private void renderInventory(GuiGraphics g, int mx, int my) {
        for (int index = 0; index < 36; index++) {
            int x = ui.inventoryX(index), y = ui.inventoryY(index);
            (ui.inventoryAt(mx, my) == index || drag.inventory == index ? UiSkin.SLOT_HOVER : UiSkin.SLOT)
                    .square(g, x, y, ui.inventoryCell);
            int iconSize = Math.min(16, ui.inventoryCell - 4), inset = (ui.inventoryCell - iconSize) / 2;
            float scale = iconSize / 16.0F;
            var stack = InventorySources.read(minecraft.player, SourceAddress.vanilla(index));
            g.pose().pushPose(); g.pose().translate(x + inset, y + inset, 0); g.pose().scale(scale, scale, 1);
            UiSkin.item(g, stack, 0, 0, Math.round(ui.inventoryCell / scale), false);
            g.renderItemDecorations(font, stack, 0, 0);
            if (drag.inventory == index && drag.moved) {
                g.pose().pushPose(); g.pose().translate(0, UiSkin.itemOffsetY(false, Math.round(ui.inventoryCell / scale)), 220);
                g.fill(0, 0, 16, 16, 0x7010131D); g.pose().popPose();
            }
            g.pose().popPose();
        }
        int hintY = ui.inventoryY(0) + ui.inventoryCell + 10;
        wrapped(g, tr(ui.compact ? "poris_elegies_fingertip_incantations.drag_out_hint" : "poris_elegies_fingertip_incantations.bind_hint"), ui.rightX + 8, hintY,
                ui.rightWidth - 16, ui.bodyBottom - hintY - 6, EditorTheme.MUTED);
    }
    private void wrapped(GuiGraphics g, Component text, int x, int y, int w, int h, int color) {
        int line = 0;
        for (var part : font.split(text, Math.max(1, w))) {
            if (line + font.lineHeight > h) break;
            g.drawString(font, part, x, y + line, color, false); line += font.lineHeight + 2;
        }
    }
    private void renderTooltip(GuiGraphics g, int mx, int my) {
        if (drag.active() || selection.boxing()) return;
        var slot = slotAt(mx, my);
        if (slot != null && slot.id == HudLayout.OFFHAND) {
            tooltip(g, List.of(TooltipStyle.title(tr("poris_elegies_fingertip_incantations.offhand_slot")),
                    TooltipStyle.muted(tr("poris_elegies_fingertip_incantations.offhand_hint"))), mx, my);
        } else if (slot != null) {
            var view = HudContent.forFrame(slot.id);
            boolean nativeSlot = HotbarControl.nativeSlot(slot.id);
            var binding = ClientSession.binding(slot.id);
            boolean action = !nativeSlot && binding.kind() == com.poris.elegies.fingertip.binding.HudBinding.Kind.ACTION;
            var lines = new java.util.ArrayList<Component>();
            // The key shares the heading line (2026-10-31 text pass); pauses and conflicts still get their own line below.
            lines.add(TooltipStyle.muted(tr(nativeSlot ? "poris_elegies_fingertip_incantations.native_slot" : "poris_elegies_fingertip_incantations.selected_slot", slot.id + 1)
                    .copy().append(" · ").append(ShortcutKeyLabels.description(slot.id))));
            // An item shows its own hover text, as in the inventory, other mods' lines included (2026-10-30, user:
            // "我们物品要不要直接展示它原来的tag"); Fingertip's own lines around it stay muted.
            boolean item = (nativeSlot || binding.isItem()) && !view.icon().isEmpty();
            if (item) lines.addAll(getTooltipFromItem(minecraft, view.icon()));
            else lines.add(TooltipStyle.title(view.name()));
            lines.add(action ? TooltipStyle.state(view.detail(), view.available(), view.lacksResource(), view.cooldown())
                    : view.missingItem() ? TooltipStyle.warning(view.detail()) : TooltipStyle.muted(view.detail()));
            HudContent.details(slot.id).forEach(line -> lines.add(TooltipStyle.body(line)));
            var conflict = com.poris.elegies.fingertip.client.input.ShortcutKeyRules.conflict(slot.id);
            if (conflict != null) lines.add(TooltipStyle.warning(tr("poris_elegies_fingertip_incantations.shortcut_conflict_paused", conflict)));
            else if (HotbarControl.shortcutPaused(slot.id)) lines.add(TooltipStyle.warning(pausedReason(ShortcutKeys.SLOTS[slot.id].getKey())));
            // An item goes through the same path as the inventory, with its stack and image, so other mods can still turn
            // their markers into pictures (Apotheosis sockets showed as "APOTH_SOCKET_MARKER", 2026-10-31).
            if (item) g.renderTooltip(font, lines.stream().filter(c -> !c.getString().isBlank()).toList(), view.icon().getTooltipImage(), view.icon(), mx, my);
            else tooltip(g, lines, mx, my);
        } else if (!canvasOnly && !positioning && !actions) {
            int index = ui.inventoryAt(mx, my);
            if (index >= 0 && !InventorySources.read(minecraft.player, SourceAddress.vanilla(index)).isEmpty())
                g.renderTooltip(font, InventorySources.read(minecraft.player, SourceAddress.vanilla(index)), mx, my);
        } else if (!canvasOnly && !positioning && actions && !actionList.tooltipAt(mx, my).isEmpty()) {
            tooltip(g, actionList.tooltipAt(mx, my), mx, my);
        }
    }
    private void tooltip(GuiGraphics g, List<Component> lines, int x, int y) {
        var wrapped = lines.stream().filter(c -> !c.getString().isBlank())
                .flatMap(c -> font.split(c, Math.min(240, width - 24)).stream()).toList();
        int capacity = Math.max(1, (height - 20) / 10);
        if (wrapped.size() > capacity) {
            var shortened = new java.util.ArrayList<>(wrapped.subList(0, capacity - 1));
            shortened.add(Component.literal("…").getVisualOrderText()); wrapped = shortened;
        }
        g.renderTooltip(font, wrapped, TOOLTIP_POSITION, x, y);
    }
    /** The binding page's header and content panel: clicks there belong to the panel, not to the canvas behind it. */
    private boolean onPanel(double x, double y) {
        return !canvasOnly && !positioning && (EditorGeometry.inside(x, y, ui.x, ui.y, ui.width, ui.headerHeight)
                || EditorGeometry.inside(x, y, ui.rightX - ui.panelPad, ui.tabsY - ui.panelPad,
                        ui.rightWidth + ui.panelPad * 2, ui.bodyBottom - ui.tabsY + ui.panelPad * 2));
    }
    private HudLayout.Slot slotAt(double x, double y) {
        if (!canvasOnly && positioning && toolbar.contains(x, y)) return null;
        if (!drag.canvas && onPanel(x, y)) return null;
        // Only the layout page can pick up the off-hand frame; binding pages never target it.
        for (var slot : (positioning ? layout().layoutSlots() : layout().activeSlots()).reversed())
            if (EditorGeometry.inside(x, y, slot.screenX(width), slot.screenY(height), 20, 20)) return slot;
        return null;
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (capture) {
            // 2026-10-31 (user): a left click outside the key box closes it like Esc instead of reporting the attack
            // key as reserved. Inside the box it is still recorded, for players who moved attack to another key.
            if (button != 0 || EditorGeometry.inside(x, y, (width - captureWidth()) / 2, captureY(), captureWidth(), CAPTURE_HEIGHT)) {
                setKey(InputConstants.Type.MOUSE.getOrCreate(button)); return true;
            }
            capture = false;
            // Only 删除 is lit while the box is open (updateControls), so only it takes this click: click a slot, then 删除
            // deletes it (2026-10-31, user: "我要删1个指尖格的时候怎么办", "点击以后上面全暗 删除所选亮").
            // The dark buttons only close the box, like any click outside it; the canvas view has no 删除 to take it.
            if (!canvasOnly && super.mouseClicked(x, y, button)) return true;
            // The same click also leaves the slot when it lands on blank canvas (2026-10-31, user: "样式还在选中").
            if (slotAt(x, y) == null && !onPanel(x, y) && !(positioning && !canvasOnly && toolbar.contains(x, y))) deselect();
            return true;
        }
        if (super.mouseClicked(x, y, button)) return true;
        if (positioning && !canvasOnly && toolbar.contains(x, y)) return true;
        if (!ready() || button != 0) return false;
        if (!canvasOnly && !positioning && actions && actionList.beginScroll(x, y)) return true;
        if (!canvasOnly && !positioning && actions && actionList.toggleSwitch(x, y)) return true;
        if (!canvasOnly && !positioning && actions && actionList.toggleGroup(x, y)) return true;
        var slot = slotAt(x, y);
        if (slot != null) {
            if (slot.id != HudLayout.OFFHAND) selected = slot.id;   // the off hand joins the selection, never the bound slot
            if (positioning) {
                if (hasShiftDown() || hasControlDown()) { selection.toggle(slot.id); return true; }
                if (!selection.ids.contains(slot.id)) selection.single(slot.id);
                drag.layout(slot, selection.slots(layout().layoutSlots()), x, y, width, height);
            } else if (canBind() && !HotbarControl.nativeSlot(slot.id)
                    && ClientSession.binding(slot.id).kind() != com.poris.elegies.fingertip.binding.HudBinding.Kind.EMPTY) {
                var binding = ClientSession.binding(slot.id);
                if (binding.isItem()) {
                    var source = ClientSession.findSource(slot.id, minecraft.player);
                    drag.content(slot.id, source.ready() ? InventorySources.read(minecraft.player, source.address())
                            : binding.template(), null, x, y);
                } else {
                    var content = HudContent.forFrame(slot.id);
                    drag.content(slot.id, content.icon(), content.sprite(), x, y);
                }
            }
            return true;
        }
        if (positioning) {
            if (canvasOnly || y >= toolbar.canvasTop()) { selection.begin(x, y, hasShiftDown()); return true; }
            return false;
        }
        // 2026-10-31 (user: "鼠标点外面 指尖格的失焦选中是不是应该做一些"): a click on blank canvas drops the selected
        // slot, as an empty box does on the layout page; picking content afterwards asks for a slot first.
        if (!onPanel(x, y)) { deselect(); return false; }
        if (!canBind() || canvasOnly) return false;
        if (actions && actionList.at(x, y) != null) {
            var entry = actionList.at(x, y);
            drag.action(entry.id(), entry.icon(), entry.sprite(), x, y); return true;
        }
        if (!actions) {
            int index = ui.inventoryAt(x, y);
            if (index >= 0 && !InventorySources.read(minecraft.player, SourceAddress.vanilla(index)).isEmpty()) {
                drag.item(index, InventorySources.read(minecraft.player, SourceAddress.vanilla(index)), x, y); return true;
            }
        }
        return false;
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (positioning && selection.boxing() && button == 0) {
            selection.move(x, y, layout().layoutSlots(), width, height); return true;
        }
        if (!canvasOnly && !positioning && actions && button == 0 && actionList.dragScroll(y)) return true;
        if (drag.active() && button == 0) {
            drag.move(x, y, width, height, layout());
            if (!positioning && drag.moved) drag.canvas = true;
            return true;
        }
        return super.mouseDragged(x, y, button, dx, dy);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        if (button != 0) return super.mouseReleased(x, y, button);
        if (actionList != null) actionList.endScroll();
        if (drag.fromHud >= 0) {
            int id = drag.fromHud; boolean moved = drag.moved;
            var target = moved ? slotAt(x, y) : null;
            cancelDrag();
            if (!moved || !canBind()) return true;
            if (target == null) confirmClear(id);   // blank canvas: clear, after asking
            else if (target.id != id && target.id != HudLayout.OFFHAND) {
                ClientSession.swap(id, target.id);
                // The selection follows the dragged content, as a dropped binding selects its slot and the hand follows
                // its item on the server (2026-10-31, user: "指尖格换位的勾选好像反了"). Vanilla slots refuse a swap.
                if (!HotbarControl.nativeSlot(target.id)) selected = target.id;
            }
            return true;
        }
        if (selection.boxing()) {
            selection.move(x, y, layout().layoutSlots(), width, height); selection.end();
            if (selection.ids.isEmpty()) selected = -1;   // an empty box is a click on blank canvas: back on the binding page nothing is lit
            return true;
        }
        if (drag.inventory >= 0 || drag.action != null) {
            var target = slotAt(x, y);
            boolean valid = drag.action != null ? ActionCatalog.entries().stream().anyMatch(e -> e.id().equals(drag.action) && e.catalog().visible())
                    : drag.stillMatches(InventorySources.read(minecraft.player, SourceAddress.vanilla(drag.inventory)));
            if (!valid) BindingFeedback.hint(selected, "poris_elegies_fingertip_incantations.drag_changed");
            else if (drag.moved && target == null) BindingFeedback.hint(-1, "poris_elegies_fingertip_incantations.drop_cancelled");
            else if (!drag.moved && selected < 0 && target == null) BindingFeedback.hint(-1, "poris_elegies_fingertip_incantations.select_first");
            else if (canBind()) {
                if (target != null) selected = target.id;
                if (drag.action != null) ClientSession.bindAction(selected, drag.action);
                else ClientSession.bindItem(selected, drag.inventory);
            }
            cancelDrag(); return true;
        }
        if (drag.slot != null) {
            // A click on one slot records its key; the off-hand frame has no key, so a click only selects it.
            boolean clicked = !drag.moved && drag.slot.id != HudLayout.OFFHAND && selection.ids.size() == 1 && !HotbarControl.nativeSlot(selected)
                    && !hasShiftDown() && !hasControlDown() && !hasAltDown();
            cancelDrag();
            if (clicked) { capture = true; keyError = null; }
            return true;
        }
        return super.mouseReleased(x, y, button);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (capture || positioning || canvasOnly) return false;
        if (actions && actionList.scroll(x, y, vertical)) return true;
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    /** True while the key dialog waits for a key; KeyboardMixin then hands it the screenshot and fullscreen keys too. */
    public boolean capturing() { return capture; }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (capture) {
            if (key == GLFW.GLFW_KEY_ESCAPE) { capture = false; return true; }
            if (KeyModifier.isKeyCodeModifier(InputConstants.getKey(key, scan))) return true;
            setKey(key == GLFW.GLFW_KEY_BACKSPACE ? InputConstants.UNKNOWN : InputConstants.getKey(key, scan)); return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE && (drag.active() || selection.boxing())) { cancelDrag(); return true; }
        if (key == GLFW.GLFW_KEY_TAB && !(getFocused() instanceof net.minecraft.client.gui.components.EditBox)) {
            toggleCanvas(); return true;
        }
        if (positioning && hasControlDown() && key == GLFW.GLFW_KEY_A) { selection.all(layout().layoutSlots()); return true; }
        if (positioning && key == GLFW.GLFW_KEY_DELETE) {
            removeSlots(selection.slots(layout().activeSlots())); return true;
        }
        if (positioning && key == GLFW.GLFW_KEY_ESCAPE) { setPositioning(false); return true; }
        if (positioning && !selection.ids.isEmpty() && key >= GLFW.GLFW_KEY_RIGHT && key <= GLFW.GLFW_KEY_UP) {
            cancelDrag();
            int step = hasShiftDown() ? 10 : 1;
            int dx = key == GLFW.GLFW_KEY_LEFT ? -step : key == GLFW.GLFW_KEY_RIGHT ? step : 0;
            int dy = key == GLFW.GLFW_KEY_UP ? -step : key == GLFW.GLFW_KEY_DOWN ? step : 0;
            var moving = selection.slots(layout().layoutSlots());
            HudArrangement.apply(moving, HudArrangement.translate(HudArrangement.positions(moving, width, height),
                    dx, dy, width, height), width, height);
            layout().save(); return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }
    private void arrangeLine(boolean horizontal) {
        cancelDrag();
        var slots = arrangeable().stream()
                .sorted(java.util.Comparator.comparingInt((HudLayout.Slot s) -> s.screenY(height)).thenComparingInt(s -> s.screenX(width))).toList();
        if (slots.isEmpty()) { BindingFeedback.hint(-1, "poris_elegies_fingertip_incantations.select_first"); return; }
        var points = HudArrangement.line(HudArrangement.positions(slots, width, height), layout().gap, width, height, horizontal);
        applyArrangement(slots, points);
    }
    private void applyArrangement(List<HudLayout.Slot> slots, List<HudArrangement.Point> points) {
        // Arranging never moves the off-hand frame, but must not stack slots on it either.
        var others = HudArrangement.positions(layout().layoutSlots().stream().filter(s -> !slots.contains(s)).toList(), width, height);
        if (points.isEmpty() || points.stream().anyMatch(p -> !HudArrangement.free(p, others, 0, width, height))) {
            BindingFeedback.hint(-1, "poris_elegies_fingertip_incantations.arrange_no_room"); return;
        }
        HudArrangement.apply(slots, points, width, height); layout().save();
    }
    private List<HudLayout.Slot> arrangeable() {
        return selection.slots(layout().activeSlots()).stream().filter(s -> !HotbarControl.nativeSlot(s.id)).toList();
    }
    private void setKey(InputConstants.Key key) {
        if (selected < 0 || HotbarControl.nativeSlot(selected)) { capture = false; return; }
        KeyModifier modifier = hasControlDown() ? KeyModifier.CONTROL : hasAltDown() ? KeyModifier.ALT : hasShiftDown() ? KeyModifier.SHIFT : KeyModifier.NONE;
        if (key.equals(InputConstants.UNKNOWN)) modifier = KeyModifier.NONE;
        keyError = ShortcutKeyRules.problem(selected, key, modifier);
        if (keyError != null) return;
        ShortcutKeyRules.clear(selected);
        ShortcutKeys.SLOTS[selected].setKeyModifierAndCode(modifier, key);
        KeyMapping.resetMapping(); ShortcutConfiguration.save(); capture = false;
        // A key Fingertip does not take is saved but paused; say so now rather than when it does nothing in game.
        if (OptionalKeyPolicy.wheelKey(key)) BindingFeedback.hint(selected, "poris_elegies_fingertip_incantations.key_saved_wheel");
        else if (OptionalKeyPolicy.passthrough(key))
            BindingFeedback.hint(selected, tr("poris_elegies_fingertip_incantations.key_saved_paused", key.getDisplayName()));
    }
    @Override public void onClose() {
        if (positioning) { finishPositioning(); return; }
        cancelDrag(); HudLayout.INSTANCE.save();
        minecraft.setScreen(parent);
    }
    @Override public void removed() {
        cancelDrag(); capture = false;
        HudLayout.INSTANCE.save(); super.removed();
    }
}
