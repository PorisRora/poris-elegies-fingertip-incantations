package com.poris.elegies.fingertip.client.hud;

import com.poris.elegies.fingertip.Fingertip;
import com.poris.elegies.fingertip.api.FingertipApi;
import net.minecraft.resources.ResourceLocation;
import java.util.HashSet;
import java.util.Set;

/**
 * Skill mods' own HUD layers under a layer switch ({@link FingertipApi.LayerSwitch}) stay hidden while the Fingertip HUD
 * is up, unless the switch is on (2026-10-29, user: Iron's spell bar showed under the slots, "而且不是施法轮盘！是这个UI",
 * "只管术法栏"). Shown layers are saved in layout.json's shownModLayers. Worked out once a client tick and on each change,
 * so each layer costs one lookup.
 */
public final class SwitchedLayers {
    private static Set<ResourceLocation> hidden = Set.of();

    public static boolean hides(ResourceLocation layer) { return hidden.contains(layer); }
    public static boolean shown(FingertipApi.LayerSwitch layerSwitch) {
        for (var layer : layerSwitch.layers()) if (!HudLayout.INSTANCE.shownModLayers.contains(layer.toString())) return false;
        return true;
    }
    public static void set(FingertipApi.LayerSwitch layerSwitch, boolean shown) {
        var list = HudLayout.INSTANCE.shownModLayers;
        for (var layer : layerSwitch.layers()) {
            String name = layer.toString();
            if (!shown) list.remove(name);
            else if (!list.contains(name)) list.add(name);
        }
        HudLayout.INSTANCE.save();
        update();
    }
    public static void tick() { update(); }
    private static void update() {
        Set<ResourceLocation> next = new HashSet<>();
        if (HudRenderer.replacesHotbar())
            for (var modSwitch : FingertipApi.switches())
                if (modSwitch instanceof FingertipApi.LayerSwitch layerSwitch && !shown(layerSwitch)) next.addAll(layerSwitch.layers());
        hidden = next.isEmpty() ? Set.of() : Set.copyOf(next);
    }

    private SwitchedLayers() {}
}
