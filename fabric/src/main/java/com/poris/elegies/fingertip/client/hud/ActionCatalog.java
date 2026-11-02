package com.poris.elegies.fingertip.client.hud;

import com.poris.elegies.fingertip.api.FingertipApi;
import com.poris.elegies.fingertip.api.ActionState;
import com.poris.elegies.fingertip.api.ActionCatalogInfo;
import com.poris.elegies.fingertip.api.FingertipAction;
import com.poris.elegies.fingertip.client.input.ModKeyWhitelist;
import com.poris.elegies.fingertip.client.input.ShortcutKeys;
import com.poris.elegies.fingertip.mixin.client.KeyMappingAccess;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

final class ActionCatalog {
    /**
     * {@code sprite}, when present, is drawn instead of {@code icon}: the bundled function icon for a native key, or a mod API
     * action's own texture.
     */
    record Entry(String id, Component name, Component category, ItemStack icon,
                 String owner, Component ownerName, FingertipAction action, ActionCatalogInfo catalog,
                 ActionIcons.Icon sprite) {
        Entry {
            java.util.Objects.requireNonNull(id);
            java.util.Objects.requireNonNull(name);
            java.util.Objects.requireNonNull(category);
            java.util.Objects.requireNonNull(icon);
            java.util.Objects.requireNonNull(owner);
            java.util.Objects.requireNonNull(ownerName);
            java.util.Objects.requireNonNull(catalog);
        }
        ActionState state() {
            if (action == null) return ActionState.READY;
            try { return java.util.Objects.requireNonNull(action.state()); }
            catch (RuntimeException | LinkageError error) {
                FingertipApi.report(id, error);
                return new ActionState(false, 0, false, Component.translatable("poris_elegies_fingertip_incantations.unavailable"));
            }
        }
        List<Component> tooltip() {
            var lines = new ArrayList<>(List.of(TooltipStyle.title(name), TooltipStyle.muted(ownerName), TooltipStyle.muted(category)));
            if (action == null) {
                lines.add(TooltipStyle.muted(Component.translatable("poris_elegies_fingertip_incantations.mapping_compatibility")));
                return lines;
            }
            // Where this skill comes from, among the sources its mod lists (0.1.5-beta).
            if (!catalog.status().getString().isBlank())
                lines.add(TooltipStyle.muted(Component.translatable("poris_elegies_fingertip_incantations.skill_source", catalog.status())));
            var state = state();
            lines.add(TooltipStyle.state(state.reason(), state));
            details(action).forEach(line -> lines.add(TooltipStyle.body(line)));
            return lines;
        }
        boolean matches(String query) {
            return ListSearch.matches(query, name.getString() + " " + category.getString() + " " + id + " " + owner + " " + ownerName.getString());
        }
    }
    /** An action's own detail lines ({@link FingertipAction#details}), for hover text only; a failing mod shows none. */
    static List<Component> details(FingertipAction action) {
        try {
            var lines = action.details();
            return lines == null ? List.of() : lines.stream().filter(java.util.Objects::nonNull).toList();
        } catch (RuntimeException | LinkageError error) {
            FingertipApi.report(String.valueOf(action.id()), error);
            return List.of();
        }
    }
    // Key mappings barely change in a session, so their entries are built on first use and kept; only a language
    // change (names and sort order) or a different key count rebuilds them. Mod API actions such as skills change as
    // the items that give them change, so they are listed fresh on every call. Render-thread only.
    private static List<Entry> keyEntries = List.of();
    private static String keyLanguage;
    private static int keyCount = -1;
    private static java.util.Map<String, String> ownerNames;

    static List<Entry> entries() {
        var entries = new ArrayList<>(actionEntries());
        entries.addAll(keyEntries());
        return entries;
    }
    private static String ownerName(String owner) {
        if (ownerNames == null) {
            var names = new java.util.HashMap<String, String>();
            net.fabricmc.loader.api.FabricLoader.getInstance().getAllMods().stream().map(net.fabricmc.loader.api.ModContainer::getMetadata).toList().forEach(mod -> names.put(mod.getId(), mod.getName()));
            ownerNames = names;
        }
        return shortName(ownerNames.getOrDefault(owner, owner));
    }
    /**
     * Names written "中文 | English", as our own packs are, show the half for the client's language: the full name
     * did not fit a group header (2026-10-28, user's screenshot of the skills tab).
     */
    private static String shortName(String name) {
        int bar = name.indexOf(" | ");
        if (bar <= 0) return name;
        boolean chinese = net.minecraft.client.Minecraft.getInstance().getLanguageManager().getSelected().startsWith("zh");
        return chinese ? name.substring(0, bar) : name.substring(bar + 3);
    }
    private static List<Entry> actionEntries() {
        var entries = new ArrayList<Entry>();
        for (var action : FingertipApi.actions()) {
            try {
                String owner = action.id().getNamespace();
                entries.add(new Entry(action.id().toString(), action.name(), Component.literal(action.category()), action.icon(),
                        owner, Component.literal(ownerName(owner)), action, action.catalogInfo(), ActionIcons.of(action)));
            }
            catch (RuntimeException | LinkageError error) { FingertipApi.report("catalog:" + action.getClass().getName(), error); }
        }
        return entries;
    }
    private static List<Entry> keyEntries() {
        String language = Minecraft.getInstance().getLanguageManager().getSelected();
        var mappings = KeyMappingAccess.fingertip$all();
        if (mappings.size() == keyCount && language.equals(keyLanguage)) return keyEntries;
        // Each name is translated once for sorting; owners come from the input router's per-key cache.
        record Named(KeyMapping mapping, String title) {}
        var entries = new ArrayList<Entry>();
        mappings.values().stream()
                .filter(mapping -> !mapping.getName().startsWith(ShortcutKeys.PREFIX))
                .map(mapping -> new Named(mapping, Component.translatable(mapping.getName()).getString()))
                .sorted(java.util.Comparator.comparing(Named::title))
                .map(Named::mapping)
                .forEach(mapping -> {
                    String owner = ModKeyWhitelist.owner(mapping);
                    Component ownerName = owner.startsWith("category:")
                            ? Component.translatable("poris_elegies_fingertip_incantations.unknown_owner", Component.translatable(mapping.getCategory()))
                            : Component.literal(ownerName(owner));
                    entries.add(new Entry("mapping:" + mapping.getName(), Component.translatable(mapping.getName()),
                            Component.translatable(mapping.getCategory()), ActionIcons.nativeKey(mapping.getName()), owner, ownerName, null,
                            ActionCatalogInfo.FUNCTION, ActionIcons.key(mapping.getName())));
                });
        keyEntries = List.copyOf(entries); keyLanguage = language; keyCount = mappings.size();
        return keyEntries;
    }
    /**
     * True when two catalogs would show the same cards: the same entries in order with unchanged kind, visibility,
     * status, name and icon. Cooldowns and availability are read live while drawing, so they never need a rebuild.
     */
    static boolean sameListing(List<Entry> a, List<Entry> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            Entry x = a.get(i), y = b.get(i);
            if (x == y) continue;
            if (!x.id().equals(y.id()) || x.catalog().kind() != y.catalog().kind() || x.catalog().visible() != y.catalog().visible()
                    || !x.catalog().status().getString().equals(y.catalog().status().getString())
                    || !x.name().getString().equals(y.name().getString()) || !ItemStack.matches(x.icon(), y.icon())
                    || !java.util.Objects.equals(x.sprite(), y.sprite())) return false;
        }
        return true;
    }
    private ActionCatalog() {}
}
