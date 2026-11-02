package com.poris.elegies.fingertip.client.input;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
public enum KeyModifier {
    NONE, SHIFT, CONTROL, ALT;
    public boolean active() {
        return switch (this) {
            case NONE -> true;
            case SHIFT -> Screen.hasShiftDown();
            case CONTROL -> Screen.hasControlDown();
            case ALT -> Screen.hasAltDown();
        };
    }
    public static boolean isKeyCodeModifier(InputConstants.Key key) {
        if (key.getType() != InputConstants.Type.KEYSYM) return false;
        int code = key.getValue();
        return code >= GLFW.GLFW_KEY_LEFT_SHIFT && code <= GLFW.GLFW_KEY_RIGHT_SUPER;
    }
}
