package com.poris.elegies.fingertip.core;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ActionOwnersTest {
    private final List<String> mods = List.of("minecraft", "neoforge", "example_spells", "iris", "sophisticatedbackpacks");
    @Test void mapsNamespacesFromEitherKeyOrCategory() {
        assertEquals("example_spells", ActionOwners.resolve("key.example_spells.cast", "key.categories.gameplay", mods));
        assertEquals("sophisticatedbackpacks", ActionOwners.resolve("key.open_backpack", "key.sophisticatedbackpacks.category", mods));
        assertEquals("iris", ActionOwners.resolve("iris.keybind.toggleShaders", "iris.keybinds", mods));
    }
    @Test void nativeAndUnknownKeysStaySeparate() {
        assertEquals("minecraft", ActionOwners.resolve("key.jump", "key.categories.movement", mods));
        assertEquals("minecraft", ActionOwners.resolve("key.hotbar.9", "key.categories.inventory", mods));
        assertEquals("category:key.categories.gameplay", ActionOwners.resolve("key.unknown.skill", "key.categories.gameplay", mods));
    }
    @Test void noSubstringGuessingOrAmbiguousShortNamespace() {
        assertEquals("category:custom", ActionOwners.resolve("key.myirisaddon.cast", "custom", mods));
        assertEquals("example_spells", ActionOwners.resolve("key.example_spells.cast", "custom", List.of("example", "example_spells")));
    }
}
