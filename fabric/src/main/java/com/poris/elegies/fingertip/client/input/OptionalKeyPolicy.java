package com.poris.elegies.fingertip.client.input;

import com.poris.elegies.fingertip.client.input.KeyState;
import com.mojang.blaze3d.platform.InputConstants;
import com.poris.elegies.fingertip.client.hud.HudLayout;
import com.poris.elegies.fingertip.mixin.client.KeyMappingAccess;
import org.lwjgl.glfw.GLFW;

/** Physical keys may be reclaimed; turning a switch off pauses their HUD bindings without deleting them. */
public final class OptionalKeyPolicy {
    public static InputPreferences settings() { return HudLayout.INSTANCE.input; }
    public static boolean optional(InputConstants.Key key) {
        return key.getType() == InputConstants.Type.KEYSYM && InputPreferences.optional(key.getValue());
    }
    public static boolean claimed(InputConstants.Key key) {
        return optional(key) && settings().claims(key.getValue());
    }
    public static boolean passthrough(InputConstants.Key key) { return optional(key) && !claimed(key); }
    public static boolean wheelKey(InputConstants.Key key) {
        return key.getType() == InputConstants.Type.MOUSE && key.getValue() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && settings().wheelEnabled;
    }
    public static boolean paused(InputConstants.Key key) { return passthrough(key) || wheelKey(key); }
    public static void changed() {
        ShortcutKeyRules.changed();
        InputRouter.releaseAll();
        for (var mapping : KeyMappingAccess.fingertip$all().values()) {
            if (!optional(KeyState.key(mapping)) && KeyState.key(mapping).getType() != InputConstants.Type.MOUSE) continue;
            var access = (KeyMappingAccess)mapping;
            access.fingertip$down(false); access.fingertip$clickCount(0);
        }
        settings().normalize(); HudLayout.INSTANCE.save();
    }
    private OptionalKeyPolicy() {}
}
