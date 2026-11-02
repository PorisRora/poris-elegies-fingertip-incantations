package com.poris.elegies.fingertip.client.input;
import com.mojang.blaze3d.platform.InputConstants;
import com.poris.elegies.fingertip.mixin.client.KeyMappingAccess;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Fabric key state and Fingertip's own saved modifiers, without changing other mods' bindings. */
public final class KeyState {
    private static final Map<KeyMapping, KeyModifier> MODIFIERS = new IdentityHashMap<>();
    public static InputConstants.Key key(KeyMapping mapping) { return ((KeyMappingAccess)mapping).fingertip$key(); }
    public static KeyModifier modifier(KeyMapping mapping) { return MODIFIERS.getOrDefault(mapping, KeyModifier.NONE); }
    public static void bind(KeyMapping mapping, KeyModifier modifier, InputConstants.Key key) {
        if (modifier == KeyModifier.NONE || key.equals(InputConstants.UNKNOWN)) MODIFIERS.remove(mapping);
        else MODIFIERS.put(mapping, modifier);
        mapping.setKey(key);
    }
    public static boolean active(KeyMapping mapping) { return Minecraft.getInstance().screen == null; }
    public static boolean matches(KeyMapping mapping) { return active(mapping) && modifier(mapping).active(); }
    public static List<KeyMapping> boundTo(InputConstants.Key key) {
        return KeyMappingAccess.fingertip$all().values().stream().filter(k -> key(k).equals(key)).toList();
    }
    public static List<KeyMapping> candidates(InputConstants.Key key) {
        var matches = boundTo(key).stream().filter(KeyState::matches).toList();
        boolean modified = matches.stream().anyMatch(k -> modifier(k) != KeyModifier.NONE);
        return modified ? matches.stream().filter(k -> modifier(k) != KeyModifier.NONE).toList() : matches;
    }
    private KeyState() {}
}
