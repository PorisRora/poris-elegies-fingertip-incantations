package com.poris.elegies.fingertip.client.hud;

import com.google.gson.GsonBuilder;
import com.poris.elegies.fingertip.Fingertip;
import net.neoforged.fml.loading.FMLPaths;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Local reusable layout only. World-specific item bindings never enter this file. */
public final class HudLayout {
    public static final HudLayout INSTANCE = load();
    public boolean enabled = true;
    public com.poris.elegies.fingertip.client.input.InputPreferences input = new com.poris.elegies.fingertip.client.input.InputPreferences();
    public boolean takeOverHotbar = false;
    public boolean customLayoutInitialized;
    public String displayMode = "all";
    public String lastVisibleDisplayMode = "all";
    public boolean hideModFunctions;
    /** Worn items show the durability left as a percentage, in the HUD and every inventory (DurabilityPercent, 2026-10-30). */
    public boolean durabilityPercent = true;
    public List<String> nativeModAllowlist = new ArrayList<>();
    /** Single keys allowed natively, by key mapping name, apart from whole mods (2026-10-28, Iron's spell wheel switch). */
    public List<String> nativeKeyAllowlist = new ArrayList<>();
    /** Mod HUD layers shown although the Fingertip HUD is up: those of a layer switch that is on (0.1.4-beta, Iron's spell bar). */
    public List<String> shownModLayers = new ArrayList<>();
    public boolean snapToSlots = true;
    /** A {@link Theme} id. */
    public String skin = Theme.CLASSIC.id;
    public int gap = HudArrangement.DEFAULT_GAP;
    public List<Slot> slots = defaults();
    /** One rigid native row; custom 1-9 and shared 10+ are in {@link #slots}, as layout files keep them. */
    public List<Slot> nativeHotbar = defaults();
    /** The off-hand frame's ID: past the HUD slots (ids below Fingertip.SLOT_COUNT), so it never owns a key, a binding or inventory. */
    public static final int OFFHAND = Fingertip.SLOT_COUNT;
    /** The native row: nine frames {@link #NATIVE_PITCH} apart, {@link #NATIVE_WIDTH} wide in all. */
    private static final int NATIVE_PITCH = 24, NATIVE_WIDTH = 8 * NATIVE_PITCH + HudArrangement.CELL;
    /**
     * The off-hand frame, one position for both modes. It shows the real off hand and can be moved on its own, but is
     * never deleted, arranged or bound (2026-10-26, user). Null in layouts saved before preview19.7, and for one read
     * without an anchor (1.0.0's, since 1.2.0): {@link #offhand()} then puts it beside the HUD, where it used to be drawn.
     */
    public Slot offhand;
    /** Null in a new layout, which then saves the registered default keys. Slot keys are owned by this file only. */
    public com.google.gson.JsonObject shortcuts;
    /**
     * A frame's place for 指尖定位法 ({@link HudPlacement}): the 九宫点 its group keeps to ({@code anchor}) and its offset
     * from it ({@code dx}, {@code dy}). Since 1.2.0 that is all a layout stores (2026-11-01):
     * 1.0.0's offsets from the bottom centre are neither read nor written, and a frame read without an anchor gets the
     * default place for its id.
     */
    public static final class Slot {
        public int id;
        public String anchor;
        public int dx, dy;
        transient HudLayout owner;
        /** Where the last placement put this frame, and which placement that was. */
        transient int placedX, placedY;
        transient long placedIn = -1;
        /** Put down by {@link #add}, {@link #place} or the first off-hand place and not recorded yet: no earlier point to keep. */
        transient boolean fresh;
        /** A frame following the bottom middle at {@code dx}, {@code dy}. */
        public Slot(int id, int dx, int dy) { this.id = id; fromBottom(dx, dy); }
        /** Follows the bottom middle at {@code dx}, {@code dy}. */
        public void fromBottom(int dx, int dy) { anchor = HudPlacement.Point9.BOTTOM.id; this.dx = dx; this.dy = dy; }
        /** The default place for its id: nine to a row above the bottom middle, the first row where the hotbar is. */
        void home() { fromBottom(-NATIVE_WIDTH / 2 + id % 9 * NATIVE_PITCH, -22 - id / 9 * NATIVE_PITCH); }
        /** Where this frame is drawn on a width × height screen. */
        public int screenX(int width, int height) { return owner != null && owner.placed(this, width, height) ? placedX : point().x(width) + dx; }
        public int screenY(int width, int height) { return owner != null && owner.placed(this, width, height) ? placedY : point().y(height) + dy; }
        HudPlacement.Point9 point() { return HudPlacement.Point9.from(anchor); }
        /** The stored place for 指尖定位法. */
        HudPlacement.Frame frame() { return new HudPlacement.Frame(point(), dx, dy); }
        /** Records a place 指尖定位法 worked out. */
        void set(HudPlacement.Frame frame) { fresh = false; anchor = frame.point().id; dx = frame.dx(); dy = frame.dy(); }
        /** Takes {@code other}'s stored place, {@code shift} pixels to the right. */
        void placeLike(Slot other, int shift) { anchor = other.anchor; dx = other.dx + shift; dy = other.dy; }
    }
    private static List<Slot> defaults() {
        var slots = new ArrayList<Slot>();
        for (int i = 0; i < 9; i++) slots.add(new Slot(i, -NATIVE_WIDTH / 2 + i * NATIVE_PITCH, -22));
        return slots;
    }
    public Slot add(int width, int height) {
        if (takeOverHotbar) customLayoutInitialized = true;
        for (int i = takeOverHotbar ? 0 : 9; i < Fingertip.SLOT_COUNT; i++) {
            int id = i;
            if (slots.stream().noneMatch(slot -> slot.id == id)) {
                // Search free space using rendered bounds, not the list length (deleting a slot changes that): row by row
                // upward from the HUD's lower-left corner, left to right in each, as before 1.1.0.
                int pitch = HudArrangement.CELL + gap;
                int columns = Math.max(1, Math.min(9, (width - 8 + gap) / pitch));
                var visible = activeSlots();
                var box = HudArrangement.bounds(HudArrangement.positions(visible, width, height));
                var occupied = HudArrangement.positions(layoutSlots(), width, height);   // keeps clear of the off hand too
                int left = visible.isEmpty() ? (width - (columns * pitch - gap)) / 2 : box.left();
                left = Math.clamp(left, 4, Math.max(4, width - (columns * pitch - gap) - 4));
                int bottom = visible.isEmpty() ? height - 2 : box.bottom();
                var candidates = new ArrayList<HudPlacement.Rect>();
                for (int row = 0; row < Math.max(1, height / pitch); row++)
                    for (int column = 0; column < columns; column++)
                        candidates.add(HudPlacement.Rect.cell(left + column * pitch, bottom - HudArrangement.CELL - row * pitch));
                var spot = HudPlacement.firstFree(candidates, HudPlacement.Rect.cells(occupied), gap, width, height);
                var point = spot != null ? new HudArrangement.Point(spot.x(), spot.y())
                        : HudArrangement.auxiliary(HudArrangement.positions(visible, width, height), occupied, 1, width, height);
                var slot = new Slot(id, -106, -46);
                adopt(slot);
                slots.add(slot);
                if (point != null) { slot.fresh = true; moveTo(List.of(slot), List.of(point), HudPlacement.Edit.DROP, width, height); }
                return slot;
            }
        }
        return null;
    }
    /** Every frame's cell on a width × height screen, for the controls that keep clear of them. */
    List<HudPlacement.Rect> frameRects(int width, int height) {
        return HudPlacement.Rect.cells(HudArrangement.positions(layoutSlots(), width, height));
    }
    /** Asked for every slot's key every tick, so a plain loop rather than a stream. */
    public boolean visible(int id) {
        if (!takeOverHotbar && id >= 0 && id < 9) return true;
        for (var slot : slots) if (slot.id == id) return true;
        return false;
    }
    public List<Slot> activeSlots() {
        if (takeOverHotbar) return slots;
        var active = new ArrayList<>(nativeHotbar);
        for (var slot : slots) if (slot.id >= 9) active.add(slot);
        return active;
    }
    /** Every frame the layout editor can move: the active slots and the off-hand frame. */
    public List<Slot> layoutSlots() {
        var frames = new ArrayList<>(activeSlots());
        frames.add(offhand());
        return frames;
    }
    /**
     * The off-hand frame. The first time a layout without one is used, it goes where earlier versions drew it next to
     * the HUD (left of it for a right-handed player) and is saved there; so is a 1.0.0 one, read without an anchor.
     */
    public Slot offhand() {
        if (offhand == null) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            int width = mc.getWindow().getGuiScaledWidth(), height = mc.getWindow().getGuiScaledHeight();
            int side = mc.player != null && mc.player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT ? 1 : -1;
            var place = HudArrangement.auxiliary(HudArrangement.positions(activeSlots(), width, height), side, width, height);
            var frame = new Slot(OFFHAND, -130, -22);
            adopt(frame);
            offhand = frame;
            if (place != null) { frame.fresh = true; moveTo(List.of(frame), List.of(place), HudPlacement.Edit.DROP, width, height); }
            save();
        }
        return offhand;
    }
    public void setHotbarTakeover(boolean takeover) {
        if (takeover && !customLayoutInitialized) {
            for (var slot : slots) if (slot.id < 9) {
                slot.placeLike(nativeHotbar.get(slot.id), 0);
            }
            customLayoutInitialized = true;
        }
        takeOverHotbar = takeover;
        com.poris.elegies.fingertip.client.input.ShortcutKeyRules.changed();   // number keys switch between native and paused
    }
    public void setDisplayMode(HudVisibility.Mode mode) {
        var current = HudVisibility.Mode.from(displayMode);
        if (mode != HudVisibility.Mode.HIDDEN) lastVisibleDisplayMode = mode.id;
        else if (current != HudVisibility.Mode.HIDDEN) lastVisibleDisplayMode = current.id;
        displayMode = mode.id;
    }
    public void toggleDisplay() {
        var restore = HudVisibility.Mode.from(lastVisibleDisplayMode);
        setDisplayMode(HudVisibility.Mode.from(displayMode) == HudVisibility.Mode.HIDDEN
                ? restore == HudVisibility.Mode.HIDDEN ? HudVisibility.Mode.ALL : restore
                : HudVisibility.Mode.HIDDEN);
    }
    /** Removing slots re-anchors nothing: what is left of a group keeps its point, so it still moves as one. */
    public void remove(java.util.Set<Integer> ids) {
        if (takeOverHotbar) customLayoutInitialized = true;
        slots.removeIf(slot -> ids.contains(slot.id) && (takeOverHotbar || slot.id >= 9));
    }

    // ---- 指尖定位法: positions for the current screen ----
    private transient long placement = -1, placementStamp;
    private transient int placedWidth = -1, placedHeight = -1;
    /** Frames held where a drag has them, drawn there until the drag ends ({@link #moveTo}) or is dropped. */
    private transient Map<Slot, HudArrangement.Point> preview = Map.of();
    private transient int previewVersion;

    /** The frames placement covers: the active slots and, once it exists, the off-hand frame. */
    private List<Slot> frames() {
        var frames = activeSlots();
        if (offhand == null) return frames;
        if (frames == slots) frames = new ArrayList<>(slots);
        frames.add(offhand);
        return frames;
    }
    private long stamp(List<Slot> frames) {
        long stamp = takeOverHotbar ? 7 : 11;
        stamp = stamp * 31 + previewVersion;
        for (var slot : frames) {
            stamp = stamp * 31 + System.identityHashCode(slot);
            stamp = stamp * 31 + slot.dx; stamp = stamp * 31 + slot.dy;
            stamp = stamp * 31 + (slot.anchor == null ? 0 : slot.anchor.hashCode());
        }
        return stamp;
    }
    boolean placed(Slot slot, int width, int height) {
        ensurePlaced(width, height);
        return slot.placedIn == placement;
    }
    /**
     * Works out where every frame goes on a width × height screen, unless nothing changed since the last time; the HUD
     * asks once a frame and then reads {@code placedX}/{@code placedY} directly.
     */
    void ensurePlaced(int width, int height) {
        var frames = frames();
        long stamp = stamp(frames);
        if (placement >= 0 && stamp == placementStamp && width == placedWidth && height == placedHeight) return;
        for (var slot : frames) adopt(slot);
        var stored = new ArrayList<HudPlacement.Frame>(frames.size());
        var held = new java.util.HashMap<Integer, HudArrangement.Point>();
        for (int i = 0; i < frames.size(); i++) {
            var slot = frames.get(i);
            stored.add(slot.frame());
            var point = preview.get(slot);
            if (point != null) held.put(i, point);
        }
        var result = HudPlacement.resolve(stored, width, height, held);
        placement++;
        for (int i = 0; i < frames.size(); i++) {
            var slot = frames.get(i);
            slot.placedX = result.x()[i]; slot.placedY = result.y()[i]; slot.placedIn = placement;
        }
        placementStamp = stamp; placedWidth = width; placedHeight = height;
    }
    private void adopt(Slot slot) { if (slot.owner == null) slot.owner = this; }
    /**
     * Puts frames at screen points on a width × height screen and records them by {@link HudPlacement#commit}'s rules,
     * {@code edit} saying how they got there; the one way positions are written (editor drags and nudges, arranging, a
     * new slot, the first off-hand place). Callers that moved nothing do not call it.
     */
    void moveTo(List<Slot> moved, List<HudArrangement.Point> points, HudPlacement.Edit edit, int width, int height) {
        preview = Map.of(); previewVersion++;
        ensurePlaced(width, height);
        var frames = frames();
        var current = new ArrayList<HudArrangement.Point>(frames.size());
        var before = new ArrayList<HudArrangement.Point>(frames.size());
        var changed = new java.util.HashSet<Integer>();
        var previous = new ArrayList<HudPlacement.Frame>(frames.size());
        for (int i = 0; i < frames.size(); i++) {
            var slot = frames.get(i);
            previous.add(slot.fresh ? new HudPlacement.Frame(null, 0, 0) : slot.frame());
            var was = new HudArrangement.Point(slot.placedX, slot.placedY);
            before.add(was);
            int k = indexOf(moved, slot);
            if (k >= 0) { current.add(points.get(k)); changed.add(i); }
            else current.add(was);
        }
        record(frames, current, before, previous, changed, edit, width, height);
    }
    /**
     * Puts {@code moved} down at screen points ({@code xs}, {@code ys}) on a width × height screen as the editor puts down
     * new frames, 指尖定位法 picking their groups and points: for a layout arranged in code, such as the probes'.
     */
    public void place(List<Slot> moved, int[] xs, int[] ys, int width, int height) {
        var points = new ArrayList<HudArrangement.Point>(moved.size());
        for (int i = 0; i < moved.size(); i++) {
            adopt(moved.get(i)); moved.get(i).fresh = true;
            points.add(new HudArrangement.Point(xs[i], ys[i]));
        }
        moveTo(moved, points, HudPlacement.Edit.DROP, width, height);
    }
    private static int indexOf(List<Slot> list, Slot slot) {
        for (int i = 0; i < list.size(); i++) if (list.get(i) == slot) return i;
        return -1;
    }
    private void record(List<Slot> frames, List<HudArrangement.Point> points, List<HudArrangement.Point> before,
                        List<HudPlacement.Frame> previous, java.util.Set<Integer> changed, HudPlacement.Edit edit,
                        int width, int height) {
        HudPlacement.commit(points, before, previous, changed, edit, width, height).forEach((i, frame) ->
                frames.get(i).set(frame));
        normalizeNativeRow();
    }
    /** Shows the dragged frames at {@code points} without storing anything. */
    void preview(List<Slot> moving, List<HudArrangement.Point> points) {
        var held = new IdentityHashMap<Slot, HudArrangement.Point>();
        for (int i = 0; i < moving.size(); i++) held.put(moving.get(i), points.get(i));
        preview = held; previewVersion++;
    }
    void clearPreview() { if (!preview.isEmpty()) { preview = Map.of(); previewVersion++; } }

    private static Path file() { return FMLPaths.CONFIGDIR.get().resolve(Fingertip.MOD_ID).resolve("layout.json"); }
    private static HudLayout load() {
        if (!Files.exists(file())) return new HudLayout();
        try (var reader = Files.newBufferedReader(file())) {
            var json = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            var shortcutData = json.remove("shortcuts");
            var displayData = json.remove("displayMode");
            var lastVisibleData = json.remove("lastVisibleDisplayMode");
            var hiddenFunctions = json.remove("hideModFunctions");
            var whitelist = json.remove("nativeModAllowlist");
            var inputData = json.remove("input");
            var layout = new GsonBuilder().create().fromJson(json, HudLayout.class);
            if (layout == null || layout.slots == null) throw new IllegalArgumentException("Missing slots");
            if (displayData != null && displayData.isJsonPrimitive() && displayData.getAsJsonPrimitive().isString())
                layout.displayMode = HudVisibility.Mode.from(displayData.getAsString()).id;
            var lastVisible = lastVisibleData != null && lastVisibleData.isJsonPrimitive()
                    && lastVisibleData.getAsJsonPrimitive().isString()
                    ? HudVisibility.Mode.from(lastVisibleData.getAsString()) : HudVisibility.Mode.ALL;
            layout.lastVisibleDisplayMode = lastVisible == HudVisibility.Mode.HIDDEN ? "all" : lastVisible.id;
            // A visible mode is authoritative; hidden layouts retain the mode saved before hiding.
            layout.setDisplayMode(HudVisibility.Mode.from(layout.displayMode));
            layout.hideModFunctions = hiddenFunctions != null && hiddenFunctions.isJsonPrimitive()
                    && hiddenFunctions.getAsJsonPrimitive().isBoolean() && hiddenFunctions.getAsBoolean();
            if (whitelist != null && whitelist.isJsonArray()) for (var entry : whitelist.getAsJsonArray()) {
                if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) continue;
                String id = entry.getAsString();
                if (com.poris.elegies.fingertip.client.input.ModKeyWhitelist.validId(id) && !layout.nativeModAllowlist.contains(id))
                    layout.nativeModAllowlist.add(id);
            }
            if (layout.nativeKeyAllowlist == null) layout.nativeKeyAllowlist = new ArrayList<>();
            layout.nativeKeyAllowlist.removeIf(name -> name == null || name.isBlank() || name.length() > 128);
            if (layout.shownModLayers == null) layout.shownModLayers = new ArrayList<>();
            layout.shownModLayers.removeIf(name -> name == null || net.minecraft.resources.ResourceLocation.tryParse(name) == null);
            if (shortcutData != null) {
                layout.shortcuts = shortcutData.isJsonObject() ? shortcutData.getAsJsonObject() : new com.google.gson.JsonObject();
                if (!shortcutData.isJsonObject()) Fingertip.LOGGER.warn("Invalid HUD shortcut section cleared; positions and inventory bindings are preserved");
            }
            layout.skin = Theme.from(layout.skin).id;   // an unknown theme becomes classic
            layout.gap = Math.clamp(layout.gap, 0, 8) / 2 * 2;
            var ids = new java.util.HashSet<Integer>();
            layout.slots.removeIf(slot -> slot == null || slot.id < 0 || slot.id >= Fingertip.SLOT_COUNT || !ids.add(slot.id));
            var nativeFrames = defaults();
            ids.clear();
            if (layout.nativeHotbar != null) for (var slot : layout.nativeHotbar)
                if (slot != null && slot.id >= 0 && slot.id < 9 && slot.point() != null && ids.add(slot.id)) nativeFrames.set(slot.id, slot);
            layout.nativeHotbar = nativeFrames;
            // 1.2.0 reads only 指尖定位法's places (): a frame without an anchor, as 1.0.0 saved them,
            // goes to its default place, and an off-hand frame without one is placed again beside the HUD.
            for (var slot : layout.slots) if (slot.point() == null) slot.home();
            if (layout.offhand != null) { layout.offhand.id = OFFHAND; if (layout.offhand.point() == null) layout.offhand = null; }
            if (inputData != null && inputData.isJsonObject()) {
                try { layout.input = new GsonBuilder().create().fromJson(inputData, com.poris.elegies.fingertip.client.input.InputPreferences.class); }
                catch (RuntimeException error) { Fingertip.LOGGER.warn("Invalid input preferences reset; HUD positions and bindings are preserved", error); }
            }
            if (layout.input == null) layout.input = new com.poris.elegies.fingertip.client.input.InputPreferences();
            layout.input.normalize();
            layout.normalizeNativeRow();
            return layout;
        } catch (IOException | RuntimeException error) {
            Fingertip.LOGGER.warn("Could not load Fingertip layout; keeping original file and using defaults", error);
            return new HudLayout();
        }
    }
    public void save() {
        normalizeNativeRow();
        try {
            Files.createDirectories(file().getParent());
            var temporary = file().resolveSibling("layout.json.tmp");
            Files.writeString(temporary, new GsonBuilder().setPrettyPrinting().create().toJson(this));
            try { Files.move(temporary, file(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException error) { Files.move(temporary, file(), StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException error) { Fingertip.LOGGER.error("Could not save Fingertip layout", error); }
    }
    /** The native row is rigid: the other eight follow the first. */
    private void normalizeNativeRow() {
        var first = nativeHotbar.getFirst();
        for (int i = 1; i < 9; i++) nativeHotbar.get(i).placeLike(first, i * NATIVE_PITCH);
    }
}
