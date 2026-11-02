package com.poris.elegies.fingertip.client;

import com.poris.elegies.fingertip.client.hud.HudEditorScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Optional configuration entry; Mod Menu loads this entrypoint only when installed. */
public final class FingertipModMenu implements ModMenuApi {
    @Override public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new HudEditorScreen(parent, -1);
    }
}
