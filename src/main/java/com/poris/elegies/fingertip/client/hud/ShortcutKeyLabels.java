package com.poris.elegies.fingertip.client.hud;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/** Compact, layout-independent legends; tooltips still show the full localized binding. */
final class ShortcutKeyLabels {
    static net.minecraft.network.chat.Component description(int id) {
        return net.minecraft.network.chat.Component.translatable(
                com.poris.elegies.fingertip.client.input.HotbarControl.shortcutPaused(id)
                        ? "poris_elegies_fingertip_incantations.key_value_paused" : "poris_elegies_fingertip_incantations.key_value",
                com.poris.elegies.fingertip.client.input.HotbarControl.displayKey(id).getTranslatedKeyMessage());
    }

    static String name(KeyMapping mapping) {
        return name(mapping.getKey(), mapping.getKeyModifier());
    }

    static String name(com.poris.elegies.fingertip.client.input.ShortcutKeyRules.KeyChoice choice) {
        return name(choice.key(), choice.modifier());
    }

    static String name(InputConstants.Key key, KeyModifier modifier) {
        String prefix = modifier == KeyModifier.CONTROL ? "C+"
                : modifier == KeyModifier.ALT ? "A+" : modifier == KeyModifier.SHIFT ? "S+" : "";
        return prefix + primary(key);
    }

    static String primary(InputConstants.Key key) {
        int code = key.getValue();
        if (key.getType() == InputConstants.Type.MOUSE) return "M" + (code + 1);
        if (key.getType() == InputConstants.Type.SCANCODE) return "SC" + code;
        if (code >= GLFW.GLFW_KEY_A && code <= GLFW.GLFW_KEY_Z || code >= GLFW.GLFW_KEY_0 && code <= GLFW.GLFW_KEY_9)
            return Character.toString((char)code);
        if (code >= GLFW.GLFW_KEY_F1 && code <= GLFW.GLFW_KEY_F25) return "F" + (code - GLFW.GLFW_KEY_F1 + 1);
        if (code >= GLFW.GLFW_KEY_KP_0 && code <= GLFW.GLFW_KEY_KP_9) return "N" + (code - GLFW.GLFW_KEY_KP_0);
        return switch (code) {
            case GLFW.GLFW_KEY_SPACE -> "SPC";
            case GLFW.GLFW_KEY_ESCAPE -> "ESC";
            case GLFW.GLFW_KEY_ENTER -> "ENT";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_BACKSPACE -> "BK";
            case GLFW.GLFW_KEY_INSERT -> "INS";
            case GLFW.GLFW_KEY_DELETE -> "DEL";
            case GLFW.GLFW_KEY_PAGE_UP -> "PU";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "PD";
            case GLFW.GLFW_KEY_HOME -> "HM";
            case GLFW.GLFW_KEY_END -> "END";
            case GLFW.GLFW_KEY_UP -> "↑";
            case GLFW.GLFW_KEY_DOWN -> "↓";
            case GLFW.GLFW_KEY_LEFT -> "←";
            case GLFW.GLFW_KEY_RIGHT -> "→";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "CAP";
            case GLFW.GLFW_KEY_SCROLL_LOCK -> "SCR";
            case GLFW.GLFW_KEY_NUM_LOCK -> "NUM";
            case GLFW.GLFW_KEY_PRINT_SCREEN -> "PRT";
            case GLFW.GLFW_KEY_PAUSE -> "PAU";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSH";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSH";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCT";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCT";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LAL";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RAL";
            case GLFW.GLFW_KEY_LEFT_SUPER -> "LWN";
            case GLFW.GLFW_KEY_RIGHT_SUPER -> "RWN";
            case GLFW.GLFW_KEY_MENU -> "MENU";
            case GLFW.GLFW_KEY_WORLD_1 -> "W1";
            case GLFW.GLFW_KEY_WORLD_2 -> "W2";
            case GLFW.GLFW_KEY_KP_DECIMAL -> "N.";
            case GLFW.GLFW_KEY_KP_DIVIDE -> "N/";
            case GLFW.GLFW_KEY_KP_MULTIPLY -> "NX";
            case GLFW.GLFW_KEY_KP_SUBTRACT -> "N-";
            case GLFW.GLFW_KEY_KP_ADD -> "N+";
            case GLFW.GLFW_KEY_KP_ENTER -> "NEN";
            case GLFW.GLFW_KEY_KP_EQUAL -> "N=";
            case GLFW.GLFW_KEY_APOSTROPHE, GLFW.GLFW_KEY_COMMA, GLFW.GLFW_KEY_MINUS,
                 GLFW.GLFW_KEY_PERIOD, GLFW.GLFW_KEY_SLASH, GLFW.GLFW_KEY_SEMICOLON,
                 GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_LEFT_BRACKET, GLFW.GLFW_KEY_BACKSLASH,
                 GLFW.GLFW_KEY_RIGHT_BRACKET, GLFW.GLFW_KEY_GRAVE_ACCENT -> Character.toString((char)code);
            default -> "K" + code;
        };
    }

    private ShortcutKeyLabels() {}
}
