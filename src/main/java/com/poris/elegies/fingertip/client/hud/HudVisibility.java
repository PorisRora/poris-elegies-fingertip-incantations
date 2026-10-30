package com.poris.elegies.fingertip.client.hud;

import com.poris.elegies.fingertip.api.ActionCatalogInfo;
import com.poris.elegies.fingertip.api.FingertipApi;
import com.poris.elegies.fingertip.binding.HudBinding;
import com.poris.elegies.fingertip.client.ClientSession;
import com.poris.elegies.fingertip.client.input.HotbarControl;

/** Presentation only: never changes binding ownership, key dispatch or editor hit targets. */
public final class HudVisibility {
    public enum Mode {
        ALL("all"), NONEMPTY("nonempty"), HIDDEN("hidden");
        public final String id;
        Mode(String id) { this.id = id; }
        public Mode next() { return values()[(ordinal() + 1) % values().length]; }
        public static Mode from(String id) {
            for (var mode : values()) if (mode.id.equals(id)) return mode;
            return ALL;
        }
    }
    public static Mode mode() { return Mode.from(HudLayout.INSTANCE.displayMode); }
    public static boolean includes(int id) {
        var binding = ClientSession.binding(id);
        boolean function = binding.kind() == HudBinding.Kind.MAPPING;
        if (binding.kind() == HudBinding.Kind.ACTION) {
            try {
                function = FingertipApi.find(binding.action()).map(a -> a.catalogInfo().kind() != ActionCatalogInfo.Kind.SKILL).orElse(true);
            } catch (RuntimeException | LinkageError error) { FingertipApi.report(binding.action(), error); function = true; }
        }
        return includes(mode(), HudLayout.INSTANCE.hideModFunctions, HotbarControl.nativeSlot(id),
                binding.kind() == HudBinding.Kind.EMPTY, function);
    }
    static boolean includes(Mode mode, boolean hideFunctions, boolean nativeSlot, boolean empty, boolean function) {
        if (mode == Mode.HIDDEN) return false;
        if (nativeSlot) return true; // Keep the mixed-mode native row intact.
        return !(hideFunctions && function) && (mode != Mode.NONEMPTY || !empty);
    }
    private HudVisibility() {}
}
