package com.poris.elegies.fingertip.client.input;

import com.poris.elegies.fingertip.client.input.KeyState;
import com.mojang.blaze3d.platform.InputConstants;
import com.poris.elegies.fingertip.Fingertip;
import com.poris.elegies.fingertip.client.hud.HudLayout;
import com.poris.elegies.fingertip.core.ActionOwners;
import com.poris.elegies.fingertip.mixin.client.KeyMappingAccess;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import java.util.*;

/**
 * Native shortcuts explicitly allowed per mod, or one key at a time (a skill mod's switch, such as Iron's spell wheel).
 * Ownership uses the same namespace rules as the action catalog.
 */
public final class ModKeyWhitelist {
    private static final Map<KeyMapping, String> OWNERS = new WeakHashMap<>();
    private static Set<String> modIds;
    public record Group(String id, String name, List<KeyMapping> keys) {}
    public static boolean validId(String id) {
        return id != null && id.matches("[a-z][a-z0-9_]{1,63}")
                && !id.equals("minecraft") && !id.equals("fabricloader") && !id.startsWith("fabric-") && !id.equals(Fingertip.MOD_ID);
    }
    public static String owner(KeyMapping key) {
        if (modIds == null) {
            modIds = new HashSet<>(); net.fabricmc.loader.api.FabricLoader.getInstance().getAllMods().stream().map(net.fabricmc.loader.api.ModContainer::getMetadata).toList().forEach(m -> modIds.add(m.getId()));
        }
        return OWNERS.computeIfAbsent(key, k -> ActionOwners.resolve(k.getName(), k.getCategory(), modIds));
    }
    public static List<Group> groups() {
        var groups = new ArrayList<Group>();
        for (var mod : net.fabricmc.loader.api.FabricLoader.getInstance().getAllMods().stream().map(net.fabricmc.loader.api.ModContainer::getMetadata).toList()) {
            if (!validId(mod.getId())) continue;
            var keys = KeyMappingAccess.fingertip$all().values().stream()
                    .filter(k -> !k.getName().startsWith(ShortcutKeys.PREFIX) && owner(k).equals(mod.getId())).toList();
            if (!keys.isEmpty()) groups.add(new Group(mod.getId(), mod.getName(), keys));
        }
        groups.sort(Comparator.comparing(Group::name, String.CASE_INSENSITIVE_ORDER));
        return groups;
    }
    public static boolean contains(String id) { return HudLayout.INSTANCE.nativeModAllowlist.contains(id); }
    /**
     * A single key allowed natively, and still named by a registered key switch: a key whose switch went away (Iron's spell
     * wheel keys since 0.1.4-beta, now the mod whitelist's) is back on the HUD, with no setting left that nothing shows.
     */
    public static boolean keyAllowed(String name) {
        if (!HudLayout.INSTANCE.nativeKeyAllowlist.contains(name)) return false;
        for (var keySwitch : com.poris.elegies.fingertip.api.FingertipApi.keySwitches()) if (keySwitch.keys().contains(name)) return true;
        return false;
    }
    /** A skill mod's key switch is on while all its keys work natively. */
    public static boolean switchOn(com.poris.elegies.fingertip.api.FingertipApi.KeySwitch keySwitch) {
        for (var key : keySwitch.keys()) if (!keyAllowed(key)) return false;
        return true;
    }
    private static boolean none() { return HudLayout.INSTANCE.nativeModAllowlist.isEmpty() && HudLayout.INSTANCE.nativeKeyAllowlist.isEmpty(); }
    private static boolean allowed(KeyMapping key) {
        return !none() && !key.getName().startsWith(ShortcutKeys.PREFIX)
                && (contains(owner(key)) || keyAllowed(key.getName()));
    }
    /** Reserve the physical key including modifier variants, so changing a modifier cannot cause double dispatch. */
    public static KeyMapping reservedBy(InputConstants.Key key) {
        if (key.equals(InputConstants.UNKNOWN) || none()) return null;
        return KeyMappingAccess.fingertip$all().values().stream()
                .filter(k -> KeyState.key(k).equals(key) && allowed(k)).findFirst().orElse(null);
    }
    public static List<KeyMapping> matching(InputConstants.Key key) {
        if (none() || NativeInputPolicy.basicKey(key)
                || NativeInputPolicy.systemKey(key) || HotbarControl.preservesKey(key)) return List.of();
        var matches = KeyMappingAccess.fingertip$all().values().stream().filter(k -> KeyState.key(k).equals(key)).filter(ModKeyWhitelist::allowed)
                .filter(k -> KeyState.matches(k)).toList();
        // A blocked mod's modifier binding must not steal an allowed mod's plain key.
        boolean modified = matches.stream().anyMatch(k -> KeyState.modifier(k) != com.poris.elegies.fingertip.client.input.KeyModifier.NONE);
        return modified ? matches.stream().filter(k -> KeyState.modifier(k) != com.poris.elegies.fingertip.client.input.KeyModifier.NONE).toList() : matches;
    }
    /** HUD slots paused because this whitelisted mod uses their keys, worded for the whitelist screen; null if none. */
    public static Component paused(String owner) {
        var slots = new ArrayList<String>();
        for (int id = 0; id < ShortcutKeys.SLOTS.length; id++) {
            var by = ShortcutKeyRules.conflict(id) == null ? null : reservedBy(KeyState.key(ShortcutKeys.SLOTS[id]));
            if (by != null && owner(by).equals(owner)) slots.add(String.valueOf(id + 1));
        }
        return slots.isEmpty() ? null : Component.translatable("poris_elegies_fingertip_incantations.whitelist_paused_slots",
                String.join(Component.translatable("poris_elegies_fingertip_incantations.list_separator").getString(), slots));
    }
    /**
     * Enabling a mod whose key a HUD slot uses no longer refuses: that slot is paused, keeps its key and resumes when
     * the mod leaves the whitelist (2026-10-26).
     */
    public static Component set(String owner, boolean enabled) {
        if (!validId(owner)) return Component.translatable("poris_elegies_fingertip_incantations.whitelist_invalid");
        PhysicalInputRouter.releaseWhitelisted();
        if (enabled) { if (!contains(owner)) HudLayout.INSTANCE.nativeModAllowlist.add(owner); }
        else HudLayout.INSTANCE.nativeModAllowlist.remove(owner);
        HudLayout.INSTANCE.save();
        ShortcutKeyRules.changed();
        return null;
    }
    /** Lets these keys work natively, or hands them back to the HUD; a slot on the same key pauses meanwhile, as above. */
    public static void setKeys(List<String> names, boolean enabled) {
        PhysicalInputRouter.releaseWhitelisted();
        var allowed = HudLayout.INSTANCE.nativeKeyAllowlist;
        for (var name : names) { if (enabled) { if (!allowed.contains(name)) allowed.add(name); } else allowed.remove(name); }
        HudLayout.INSTANCE.save();
        ShortcutKeyRules.changed();
    }
    private ModKeyWhitelist() {}
}
