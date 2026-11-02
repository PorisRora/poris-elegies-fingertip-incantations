package com.poris.elegies.fingertip.api;

import net.minecraft.network.chat.Component;

/**
 * Catalog visibility is separate from action lifetime: hidden skills must still resolve existing HUD bindings.
 * {@code status} names where this skill comes from, such as the item that gives it; the skill's card shows it on hover
 * (2026-10-28: renamed from context). Until 0.1.5-beta the skill tab also took the first one as the whole mod's line;
 * that line is now the mod's own list, {@link FingertipApi#registerSources}.
 */
public record ActionCatalogInfo(Kind kind, boolean visible, Component status) {
    public enum Kind { FUNCTION, SKILL }
    public static final ActionCatalogInfo FUNCTION = new ActionCatalogInfo(Kind.FUNCTION, true, Component.empty());
    public ActionCatalogInfo {
        java.util.Objects.requireNonNull(kind);
        status = status == null ? Component.empty() : status;
    }
}
