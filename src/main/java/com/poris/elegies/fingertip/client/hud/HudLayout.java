package com.poris.elegies.fingertip.client.hud;

import com.google.gson.GsonBuilder;
import com.poris.elegies.fingertip.Fingertip;
import net.neoforged.fml.loading.FMLPaths;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

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
    /** One rigid native row; custom 1-9 and shared 10+ remain in slots for legacy compatibility. */
    public List<Slot> nativeHotbar = nativeDefaults();
    /** The off-hand frame's ID: past the HUD slots (ids below Fingertip.SLOT_COUNT), so it never owns a key, a binding or inventory. */
    public static final int OFFHAND = Fingertip.SLOT_COUNT;
    /** The native row: nine frames {@link #NATIVE_PITCH} apart, {@link #NATIVE_WIDTH} wide in all. */
    private static final int NATIVE_PITCH = 24, NATIVE_WIDTH = 8 * NATIVE_PITCH + HudArrangement.CELL;
    /**
     * The off-hand frame, one position for both modes. It shows the real off hand and can be moved on its own, but is
     * never deleted, arranged or bound (2026-10-26, user). Null in layouts saved before preview19.7: {@link #offhand()}
     * then puts it where it used to be drawn, beside the HUD.
     */
    public Slot offhand;
    /** Null in a new layout, which then saves the registered default keys. Slot keys are owned by this file only. */
    public com.google.gson.JsonObject shortcuts;
    public static final class Slot {
        public int id, x, y;
        private transient boolean nativeFrame;
        public Slot(int id, int x, int y) { this.id = id; this.x = x; this.y = y; }
        public int screenX(int width) {
            return nativeFrame ? Math.clamp(width / 2 + x - id * NATIVE_PITCH, 0, Math.max(0, width - NATIVE_WIDTH)) + id * NATIVE_PITCH
                    : Math.clamp(width / 2 + x, 0, Math.max(0, width - HudArrangement.CELL));
        }
        public int screenY(int height) { return Math.clamp(height + y, 0, Math.max(0, height - HudArrangement.CELL)); }
    }
    private static List<Slot> defaults() {
        var slots = new ArrayList<Slot>();
        for (int i = 0; i < 9; i++) slots.add(new Slot(i, -NATIVE_WIDTH / 2 + i * NATIVE_PITCH, -22));
        return slots;
    }
    private static List<Slot> nativeDefaults() {
        var slots = defaults(); slots.forEach(slot -> slot.nativeFrame = true); return slots;
    }
    public Slot add(int width, int height) {
        if (takeOverHotbar) customLayoutInitialized = true;
        for (int i = takeOverHotbar ? 0 : 9; i < Fingertip.SLOT_COUNT; i++) {
            int id = i;
            if (slots.stream().noneMatch(slot -> slot.id == id)) {
                var slot = new Slot(id, -106, -46);
                // Search free space using rendered bounds, not the list length (deleting a slot changes that).
                int pitch = HudArrangement.CELL + gap;
                int columns = Math.max(1, Math.min(9, (width - 8 + gap) / pitch));
                var visible = activeSlots();
                var box = HudArrangement.bounds(HudArrangement.positions(visible, width, height));
                var occupied = HudArrangement.positions(layoutSlots(), width, height);   // keeps clear of the off hand too
                int left = visible.isEmpty() ? (width - (columns * pitch - gap)) / 2 : box.left();
                left = Math.clamp(left, 4, Math.max(4, width - (columns * pitch - gap) - 4));
                int bottom = visible.isEmpty() ? height - 2 : box.bottom();
                boolean found = false;
                for (int row = 0; row < Math.max(1, height / pitch) && !found; row++) {
                    for (int column = 0; column < columns; column++) {
                        int x = left + column * pitch;
                        int y = bottom - HudArrangement.CELL - row * pitch;
                        if (HudArrangement.free(new HudArrangement.Point(x, y), occupied, gap, width, height)) {
                            slot.x = x - width / 2; slot.y = y - height; found = true; break;
                        }
                    }
                }
                if (!found) {
                    var free = HudArrangement.auxiliary(HudArrangement.positions(visible, width, height), occupied, 1, width, height);
                    if (free != null) { slot.x = free.x() - width / 2; slot.y = free.y() - height; }
                }
                slots.add(slot); return slot;
            }
        }
        return null;
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
     * the HUD (left of it for a right-handed player) and is saved there, so upgrading moves nothing.
     */
    public Slot offhand() {
        if (offhand == null) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            int width = mc.getWindow().getGuiScaledWidth(), height = mc.getWindow().getGuiScaledHeight();
            int side = mc.player != null && mc.player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT ? 1 : -1;
            var place = HudArrangement.auxiliary(HudArrangement.positions(activeSlots(), width, height), side, width, height);
            offhand = place == null ? new Slot(OFFHAND, -130, -22) : new Slot(OFFHAND, place.x() - width / 2, place.y() - height);
            save();
        }
        return offhand;
    }
    public void setHotbarTakeover(boolean takeover) {
        if (takeover && !customLayoutInitialized) {
            for (var slot : slots) if (slot.id < 9) {
                var initial = nativeHotbar.get(slot.id); slot.x = initial.x; slot.y = initial.y;
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
    public void remove(java.util.Set<Integer> ids) {
        if (takeOverHotbar) customLayoutInitialized = true;
        slots.removeIf(slot -> ids.contains(slot.id) && (takeOverHotbar || slot.id >= 9));
    }
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
            // Existing custom layouts (including intentional deletions) must never be initialized again.
            if (!json.has("customLayoutInitialized")) layout.customLayoutInitialized = true;
            // Starlight was saved as "magic" before 0.2-beta; an unknown theme becomes classic.
            layout.skin = Theme.from("magic".equals(layout.skin) ? Theme.STARLIGHT.id : layout.skin).id;
            layout.gap = Math.clamp(layout.gap, 0, 8) / 2 * 2;
            var ids = new java.util.HashSet<Integer>();
            layout.slots.removeIf(slot -> slot == null || slot.id < 0 || slot.id >= Fingertip.SLOT_COUNT || !ids.add(slot.id));
            var nativeFrames = defaults();
            ids.clear();
            if (layout.nativeHotbar != null) for (var slot : layout.nativeHotbar)
                if (slot != null && slot.id >= 0 && slot.id < 9 && ids.add(slot.id)) nativeFrames.set(slot.id, slot);
            layout.nativeHotbar = nativeFrames;
            nativeFrames.forEach(slot -> slot.nativeFrame = true);
            if (layout.offhand != null) layout.offhand.id = OFFHAND;
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
    private void normalizeNativeRow() {
        var first = nativeHotbar.getFirst();
        for (int i = 1; i < 9; i++) { nativeHotbar.get(i).x = first.x + i * NATIVE_PITCH; nativeHotbar.get(i).y = first.y; }
    }
}
