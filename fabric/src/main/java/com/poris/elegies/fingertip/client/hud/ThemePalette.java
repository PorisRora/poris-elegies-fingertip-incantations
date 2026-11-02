package com.poris.elegies.fingertip.client.hud;

/**
 * Semantic colors shared by generated chrome, lists, drag feedback and radial geometry. {@code orbitNear} and
 * {@code orbitFar} are the running light's tail on a skill that is on, from just behind its white head to the end
 * (2026-10-29): warm gold for classic, ice blue for starlight, champagne
 * gold into rose red for Poris; the last two deepened on 10-30 so they show against their own frames.
 */
record ThemePalette(int accent, int border, int surface, int raised, int hover, int selected, int orbitNear, int orbitFar) {
    private static final ThemePalette CLASSIC = new ThemePalette(0xFFE1BE7D, 0xFF665C49, 0xFF181B20, 0xFF25292F, 0xFF373A3E, 0xFF4A4438,
            0xFFEEDAB5, 0xFFE1BE7D);
    private static final ThemePalette STARLIGHT = new ThemePalette(0xFF9DE5FA, 0xFF41677E, 0xFF101C2E, 0xFF1D3047, 0xFF294760, 0xFF345A73,
            0xFFA6EAFF, 0xFF2E9CE6);
    private static final ThemePalette PORIS = new ThemePalette(0xFFF0C995, 0xFF76504C, 0xFF20151C, 0xFF35202A, 0xFF4B2838, 0xFF663344,
            0xFFFFCF86, 0xFFE52A55);
    static ThemePalette of(Theme theme) { return theme.pick(CLASSIC, STARLIGHT, PORIS); }

    // 指尖配色法 (2026-11-01, named with 指尖定位法 and 指尖排字法): one colour per meaning, the same in every theme, so a
    // state reads alike after a theme change; the hover-text colours are the standard. Text, muted text and the theme's
    // accent stay in EditorTheme; overlays that darken or tint a whole slot keep their own shades.
    /** Ready, or done. */
    static final int READY = 0xFF8FD694;
    /** Cooling down. */
    static final int COOLDOWN = 0xFFF2B05E;
    /** Under way: waiting for the server, or the next charge coming back. */
    static final int PENDING = 0xFFC5B9EC;
    /** Short of what it costs, mana and the like. */
    static final int NO_RESOURCE = 0xFF8CB4FF;
    /** Failed, or cannot be used. */
    static final int ERROR = 0xFFE8857A;
    /** Paused, switched off or used up. */
    static final int PAUSED = 0xFF918A9B;
    /** Press again: a recast window or the "↻" (gold, 2026-10-29). */
    static final int AGAIN = 0xFFF2C86B;
    /** An item's Σ total over the whole inventory. */
    static final int TOTAL = 0xFFE1CDEB;
}
