package com.poris.elegies.fingertip.client.hud;

/**
 * The three looks: classic, Starlight (星辉) and Poris. The id is what layout.json saves and what names each theme's
 * lang keys and images. An unknown id reads as classic; since 1.2.0 so does "magic", Starlight's id until 0.2-beta.
 */
enum Theme {
    CLASSIC("classic"), STARLIGHT("starlight"), PORIS("poris");
    private static final Theme[] ALL = values();
    final String id;
    Theme(String id) { this.id = id; }

    /** The saved theme, {@link HudLayout#skin}; read on every draw, since the setting can change at any time. */
    static Theme current() { return from(HudLayout.INSTANCE.skin); }
    /** The theme of a saved id; an unknown or missing one is classic. */
    static Theme from(String id) {
        for (var theme : ALL) if (theme.id.equals(id)) return theme;
        return CLASSIC;
    }
    /** The style button's cycle: classic, Starlight, Poris, then classic again. */
    Theme next() { return ALL[(ordinal() + 1) % ALL.length]; }
    /** This theme's one of three. */
    <T> T pick(T classic, T starlight, T poris) {
        return switch (this) { case CLASSIC -> classic; case STARLIGHT -> starlight; case PORIS -> poris; };
    }
}
