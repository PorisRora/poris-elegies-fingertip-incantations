package com.poris.elegies.fingertip.client.hud;

import net.minecraft.world.item.ItemStack;
import java.util.List;

/** A drag captures an item, an action ID or the starting positions of a HUD group. */
final class EditorDrag {
    int inventory = -1;
    int fromHud = -1;
    String action;
    ItemStack icon = ItemStack.EMPTY;
    ActionIcons.Icon sprite;
    HudLayout.Slot slot;
    boolean moved, canvas;
    Integer guideX, guideY;
    private double originX, originY;
    private List<HudLayout.Slot> moving = List.of();
    private List<HudArrangement.Point> startPositions = List.of(), next = List.of();
    private HudLayout layout;

    boolean active() { return inventory >= 0 || fromHud >= 0 || action != null || slot != null; }
    void content(int id, ItemStack stack, ActionIcons.Icon sprite, double x, double y) {
        clear(); fromHud = id; icon = stack.copy(); this.sprite = sprite; originX = x; originY = y;
    }

    void item(int index, ItemStack stack, double x, double y) {
        clear(); inventory = index; icon = stack.copy(); originX = x; originY = y;
    }
    void action(String id, ItemStack stack, ActionIcons.Icon sprite, double x, double y) {
        clear(); action = id; icon = stack.copy(); this.sprite = sprite; originX = x; originY = y;
    }

    void layout(HudLayout.Slot target, List<HudLayout.Slot> targets, double x, double y, int width, int height) {
        clear(); slot = target;
        originX = x; originY = y; moving = List.copyOf(targets);
        startPositions = next = HudArrangement.positions(moving, width, height);
    }

    void move(double x, double y, int width, int height, HudLayout layout) {
        if (slot != null) {
            moved |= (x - originX) * (x - originX) + (y - originY) * (y - originY) >= 9;
            if (!moved) return;
            var next = HudArrangement.translate(startPositions, (int)Math.round(x - originX),
                    (int)Math.round(y - originY), width, height);
            guideX = guideY = null;
            if (layout.snapToSlots) {
                var fixed = HudArrangement.positions(layout.layoutSlots().stream()
                        .filter(s -> !moving.contains(s)).toList(), width, height);
                var snap = HudSnap.align(next, fixed, layout.gap, width, height);
                next = snap.points(); guideX = snap.guideX(); guideY = snap.guideY();
            }
            // Shown where it is held; stored once, when the drag ends (finish), so a group it only passes by keeps its point.
            this.layout = layout; this.next = next;
            layout.preview(moving, next);
        } else if (inventory >= 0 || fromHud >= 0 || action != null) {
            moved |= (x - originX) * (x - originX) + (y - originY) * (y - originY) >= 9;
        }
    }

    boolean stillMatches(ItemStack current) {
        return !current.isEmpty() && ItemStack.isSameItemSameComponents(icon, current);
    }

    /** Ends the drag; a layout drag stores the frames where they were let go, as put down (指尖定位法), and saves. */
    void finish(int width, int height) {
        // Dragged back to where it started: nothing to store, however far it went on the way.
        if (layout != null && !next.equals(startPositions)) { layout.moveTo(moving, next, HudPlacement.Edit.DROP, width, height); layout.save(); }
        clear();
    }

    void clear() {
        if (layout != null) layout.clearPreview();
        layout = null; next = List.of();
        inventory = fromHud = -1; action = null; icon = ItemStack.EMPTY; sprite = null; slot = null; moved = false; canvas = false;
        moving = List.of(); startPositions = List.of();
        guideX = guideY = null;
    }
}
