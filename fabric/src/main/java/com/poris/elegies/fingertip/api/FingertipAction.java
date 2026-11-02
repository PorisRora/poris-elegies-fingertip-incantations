package com.poris.elegies.fingertip.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/**
 * Client-side presentation and input adapter. Gameplay requests MUST still be validated by the owning mod's server.
 * The integration contract, with the built-in Iron's Spells adapter as an example, is in
 * docs/INTEGRATION.md.
 */
public interface FingertipAction {
    ResourceLocation id();
    Component name();
    ItemStack icon();
    /**
     * Optional square texture, drawn scaled to 16 x 16 in place of {@link #icon()}: a spell icon from the owning mod's
     * own assets, for example. Null draws the item; {@link #icon()} stays required (2026-10-28).
     */
    default ResourceLocation iconTexture() { return null; }
    default String category() { return id().getNamespace(); }
    default ActionState state() { return ActionState.READY; }
    default ActionCatalogInfo catalogInfo() { return ActionCatalogInfo.FUNCTION; }
    /**
     * Optional lines on what the action does, in the owning mod's words: a spell's damage, radius or duration, say. Read
     * only for hover text, under the state line, never per frame; empty shows nothing. Since 0.2.2-beta.
     */
    default List<Component> details() { return List.of(); }
    void press();
    default void release() {}
}
