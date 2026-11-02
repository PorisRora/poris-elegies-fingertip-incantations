package com.poris.elegies.fingertip.client.hud;

import com.poris.elegies.fingertip.client.input.KeyState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;

/** Small unbacked key legends sit inside the upper-left ornament, clear of the quantity. */
final class ShortcutKeyBadge {
    static void draw(GuiGraphics g, KeyMapping mapping, boolean selected, boolean waiting, boolean reselect, boolean available, boolean nativeSlot) {
        drawLegend(g, mapping, false);
    }

    static void drawLegend(GuiGraphics g, KeyMapping mapping, boolean paused) {
        drawLegend(g, new com.poris.elegies.fingertip.client.input.ShortcutKeyRules.KeyChoice(KeyState.key(mapping), KeyState.modifier(mapping)), paused);
    }

    static void drawLegend(GuiGraphics g, com.poris.elegies.fingertip.client.input.ShortcutKeyRules.KeyChoice choice, boolean paused) {
        if (choice.isUnbound()) return;
        String text = ShortcutKeyLabels.name(choice);
        var face = ShortcutGlyphs.current();
        float height = Math.min(4.5F, 14F / Math.max(1, face.width(text)));
        g.pose().pushPose();
        g.pose().translate(3, 2.5F, 250);
        // Keep only the generated glyph's own thin outline; there is no panel over the item.
        try {
            if (paused) g.setColor(.48F, .48F, .48F, 1);
            face.draw(g, text, 0, 0, height);
        } finally {
            g.setColor(1, 1, 1, 1);
            g.pose().popPose();
        }
    }

    private ShortcutKeyBadge() {}
}
