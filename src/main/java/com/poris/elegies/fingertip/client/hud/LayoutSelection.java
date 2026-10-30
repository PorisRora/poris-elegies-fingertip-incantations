package com.poris.elegies.fingertip.client.hud;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Selection is transient UI state: changing it never changes bindings or physical slot positions. */
final class LayoutSelection {
    final Set<Integer> ids = new LinkedHashSet<>();
    private Set<Integer> linked = Set.of();
    private Set<Integer> before = Set.of();
    private Set<Integer> initial = Set.of();
    private double startX, startY, endX, endY;
    private boolean box;
    void linked(Set<Integer> group) { linked = Set.copyOf(group); expandLinked(); }
    void single(int id) { ids.clear(); if (id >= 0) ids.add(id); expandLinked(); }
    void all(List<HudLayout.Slot> slots) { ids.clear(); slots.forEach(s -> ids.add(s.id)); }
    void toggle(int id) {
        if (linked.contains(id)) { if (ids.contains(id)) ids.removeAll(linked); else ids.addAll(linked); }
        else if (!ids.remove(id)) ids.add(id);
    }
    private void expandLinked() { if (ids.stream().anyMatch(linked::contains)) ids.addAll(linked); }
    boolean boxing() { return box; }
    void begin(double x, double y, boolean additive) {
        initial = Set.copyOf(ids);
        before = additive ? Set.copyOf(ids) : Set.of();
        ids.clear(); ids.addAll(before); startX = endX = x; startY = endY = y; box = true;
    }
    void move(double x, double y, List<HudLayout.Slot> slots, int width, int height) {
        endX = Math.clamp(x, 0, width); endY = Math.clamp(y, 0, height);
        var b = bounds(); ids.clear(); ids.addAll(before);
        for (var slot : slots) {
            int sx = slot.screenX(width), sy = slot.screenY(height);
            if (sx < b.right() && sx + 20 > b.left() && sy < b.bottom() && sy + 20 > b.top()) ids.add(slot.id);
        }
        expandLinked();
    }
    HudArrangement.Bounds bounds() {
        return new HudArrangement.Bounds((int)Math.min(startX, endX), (int)Math.min(startY, endY),
                (int)Math.max(startX, endX), (int)Math.max(startY, endY));
    }
    void end() { box = false; }
    void cancel() { if (box) { ids.clear(); ids.addAll(initial); } box = false; }
    List<HudLayout.Slot> slots(List<HudLayout.Slot> all) { return all.stream().filter(s -> ids.contains(s.id)).toList(); }
}
