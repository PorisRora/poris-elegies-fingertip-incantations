package com.poris.elegies.fingertip.api;

import com.poris.elegies.fingertip.Fingertip;
import com.poris.elegies.fingertip.network.FingertipNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

/**
 * Register providers from client setup. No dependency on Minecraft.getInstance() in the public registry. A provider
 * that throws, or that was built against another version of the mod it adapts (a LinkageError), is reported once and
 * its actions show as unavailable; the game keeps running. Contract: docs/INTEGRATION.md.
 */
public final class FingertipApi {
    private static final Map<ResourceLocation, ActionProvider> PROVIDERS = new LinkedHashMap<>();
    private static final Map<String, java.util.function.Supplier<? extends List<Component>>> SOURCES = new LinkedHashMap<>();
    private static final Map<String, java.util.function.Supplier<? extends Component>> EMPTY_HINTS = new LinkedHashMap<>();
    // The integrated server's thread reports server handler failures here too.
    private static final Set<String> REPORTED = java.util.concurrent.ConcurrentHashMap.newKeySet();
    // The HUD looks actions up every frame for every skill slot. Providers are asked once and the result is kept, with
    // an ID index, until invalidate(); Fingertip calls that each client tick (2026-10-26, preview19.6).
    private static List<FingertipAction> actions;
    private static Map<String, FingertipAction> byId = Map.of();
    public static void registerProvider(ResourceLocation id, ActionProvider provider) {
        if (PROVIDERS.putIfAbsent(Objects.requireNonNull(id), Objects.requireNonNull(provider)) != null)
            throw new IllegalArgumentException("Duplicate Fingertip provider " + id);
        invalidate();
    }
    public static List<FingertipAction> actions() {
        if (actions != null) return actions;
        var found = new LinkedHashMap<ResourceLocation, FingertipAction>();
        PROVIDERS.forEach((id, provider) -> {
            try {
                for (FingertipAction action : provider.actions()) if (action != null)
                    found.putIfAbsent(Objects.requireNonNull(action.id(), "Action ID"), action);
            } catch (RuntimeException | LinkageError error) { report(id.toString(), error); }
        });
        var index = new HashMap<String, FingertipAction>();
        found.forEach((id, action) -> index.put(id.toString(), action));
        byId = index;
        return actions = List.copyOf(found.values());
    }
    public static Optional<FingertipAction> find(String id) {
        actions();
        return Optional.ofNullable(byId.get(id));
    }
    /**
     * What gives a skill mod's skills right now, by name; several items can, such as a worn item, a held tool and an
     * accessory. The skill tab shows the names under the mod's name, joined, and one per line on hover; with none, no
     * line (2026-10-29, user: "这里应该做接口接受消息吧", "这里应该注册物品名 没有的话就不显示吧"). Before, Fingertip took the
     * first status among the mod's skills, and one skill's reason spoke for the whole mod. {@code owner} is the
     * mod ID before the colon of its action IDs, which the tab groups by. Asked about once a second while the editor is
     * open. Client only; since 0.1.5-beta.
     */
    public static void registerSources(String owner, java.util.function.Supplier<? extends List<Component>> names) {
        if (SOURCES.putIfAbsent(Objects.requireNonNull(owner), Objects.requireNonNull(names)) != null)
            throw new IllegalArgumentException("Duplicate Fingertip skill sources for " + owner);
    }
    /** The names a skill mod gives for its sources, blank ones left out; empty if it registered none or the call failed. */
    public static List<Component> sources(String owner) {
        var supplier = SOURCES.get(owner);
        if (supplier == null) return List.of();
        try {
            var names = supplier.get();
            if (names == null) return List.of();
            return names.stream().filter(name -> name != null && !name.getString().isBlank()).map(name -> (Component)name).toList();
        } catch (RuntimeException | LinkageError error) { report(owner + " sources", error); return List.of(); }
    }
    /**
     * The line under a skill mod's name while it gives no skill, saying why, such as "no spell book equipped"; null or
     * blank shows nothing, and so does a mod that registers none. Fingertip itself writes no such hint: only the mod knows
     * what its skills need (2026-10-30, user: "有铁魔法的时候显示就行 有魔法书的时候就列出来魔法", "未装备魔法书",
     * "什么都没装的时候就不显示提示"). Same {@code owner} and timing as {@link #registerSources}. Client only; since 0.2.1-beta.
     */
    public static void registerEmptyHint(String owner, java.util.function.Supplier<? extends Component> hint) {
        if (EMPTY_HINTS.putIfAbsent(Objects.requireNonNull(owner), Objects.requireNonNull(hint)) != null)
            throw new IllegalArgumentException("Duplicate Fingertip empty hint for " + owner);
    }
    public static Optional<Component> emptyHint(String owner) {
        var supplier = EMPTY_HINTS.get(owner);
        if (supplier == null) return Optional.empty();
        try {
            Component hint = supplier.get();
            return hint == null || hint.getString().isBlank() ? Optional.empty() : Optional.of(hint);
        } catch (RuntimeException | LinkageError error) { report(owner + " empty hint", error); return Optional.empty(); }
    }
    /** Providers are asked again on the next lookup. Fingertip calls this at the start of every client tick. */
    public static void invalidate() { actions = null; }
    /**
     * Client only: passes a press or release of {@code action} to the server handler registered, with
     * {@link FingertipServerApi#registerHandler}, for the mod ID before its colon; for mods with no packet of their own
     * (2026-10-28).
     */
    public static void sendToServer(ResourceLocation action, boolean pressed) {
        PacketDistributor.sendToServer(new FingertipNetwork.ActionInput(action, pressed));
    }
    /** A switch on a skill mod's group in the editor's skill list; off by default, client only. */
    public sealed interface ModSwitch permits KeySwitch, LayerSwitch {
        String owner(); Component label(); Component hint();
    }
    /**
     * Lets some of the mod's own keys work natively, by key mapping name, while the rest stay on the HUD (2026-10-28, user:
     * Iron's spell wheel). A single key counts as allowed only while a registered switch names it.
     */
    public record KeySwitch(String owner, Component label, Component hint, List<String> keys) implements ModSwitch {}
    /**
     * Shows the mod's own HUD layers, by the names it registered them under, while the Fingertip HUD is up; off, Fingertip
     * hides them. For a bar of the skills the HUD already shows (2026-10-29, user, of Iron's spell bar: "而且不是施法轮盘！是
     * 这个UI", "只管术法栏"). With the Fingertip HUD off they always show. Since 0.1.4-beta.
     */
    public record LayerSwitch(String owner, Component label, Component hint, List<ResourceLocation> layers) implements ModSwitch {
        public LayerSwitch { layers = List.copyOf(layers); }
    }
    private static final List<ModSwitch> SWITCHES = new ArrayList<>();
    public static void registerKeySwitch(KeySwitch keySwitch) { SWITCHES.add(Objects.requireNonNull(keySwitch)); }
    public static void registerLayerSwitch(LayerSwitch layerSwitch) { SWITCHES.add(Objects.requireNonNull(layerSwitch)); }
    /** Every switch, in the order registered. */
    public static List<ModSwitch> switches() { return List.copyOf(SWITCHES); }
    public static List<KeySwitch> keySwitches() {
        var keys = new ArrayList<KeySwitch>();
        for (var modSwitch : SWITCHES) if (modSwitch instanceof KeySwitch keySwitch) keys.add(keySwitch);
        return List.copyOf(keys);
    }
    public static void report(String owner, Throwable error) {
        if (REPORTED.add(owner)) Fingertip.LOGGER.error("Fingertip provider failed: {}", owner, error);
    }
    private FingertipApi() {}
}
