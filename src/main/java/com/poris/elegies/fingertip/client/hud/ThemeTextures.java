package com.poris.elegies.fingertip.client.hud;

import com.poris.elegies.fingertip.Fingertip;
import net.minecraft.client.Minecraft;
import java.util.List;
import java.util.Map;

/**
 * Each theme's own images (UiSkin, ButtonArt, PanelArt, WheelArt, the key glyphs). Switching themes frees the other
 * themes' images, so one theme's set stays in video memory rather than all three until resources reload (2026-10-29,
 * user: "但切换主题后全部27张图会占到约150MB直到重载资源 这个呢"); switching back loads them again when first drawn.
 * The O glyph image, the function icons and the logo entry serve all three and stay.
 */
final class ThemeTextures {
    private static final Map<Theme, List<String>> BY_THEME = Map.of(
            Theme.CLASSIC, List.of("astral_v3/slots.png", "themes/native-slots.png", "themes/classic-complete-v1.png",
                    "themes/buttons/classic-refined-v1.png", "themes/wheel-rim-classic-v2.png",
                    "keyglyphs/classic-letters.png", "keyglyphs/classic-symbols.png",
                    "themes/selected/frame_selected_slot_classic.png", "themes/selected/frame_selected_native_classic.png",
                    "themes/rims/rim_plain_slot_classic.png", "themes/rims/rim_selected_slot_classic.png",
                    "themes/rims/rim_plain_native_classic.png", "themes/rims/rim_selected_native_classic.png"),
            Theme.STARLIGHT, List.of("themes/starlight-complete-v1.png",
                    "themes/buttons/starlight-refined-v1.png", "themes/wheel-rim-starlight-v2.png",
                    "keyglyphs/starlight-letters.png", "keyglyphs/starlight-symbols.png",
                    "themes/plain/frame_plain_slot_starlight.png", "themes/plain/frame_plain_native_starlight.png",
                    "themes/selected/frame_selected_slot_starlight.png", "themes/selected/frame_selected_native_starlight.png",
                    "themes/rims/rim_plain_slot_starlight.png", "themes/rims/rim_selected_slot_starlight.png",
                    "themes/rims/rim_plain_native_starlight.png", "themes/rims/rim_selected_native_starlight.png"),
            Theme.PORIS, List.of("themes/poris-complete-v1.png",
                    "themes/buttons/poris-refined-v1.png", "themes/wheel-rim-poris-v2.png",
                    "keyglyphs/poris-letters.png", "keyglyphs/poris-symbols.png",
                    "themes/plain/frame_plain_slot_poris.png", "themes/plain/frame_plain_native_poris.png",
                    "themes/selected/frame_selected_slot_poris.png", "themes/selected/frame_selected_native_poris.png",
                    "themes/rims/rim_plain_slot_poris.png", "themes/rims/rim_selected_slot_poris.png",
                    "themes/rims/rim_plain_native_poris.png", "themes/rims/rim_selected_native_poris.png"));

    /** Frees the images of every theme but the current one; called right after the theme is switched. */
    static void releaseOtherThemes() {
        var textures = Minecraft.getInstance().getTextureManager();
        var current = Theme.current();
        BY_THEME.forEach((theme, paths) -> {
            if (theme != current) for (String path : paths) textures.release(Fingertip.id("textures/gui/" + path));
        });
    }
    private ThemeTextures() {}
}
