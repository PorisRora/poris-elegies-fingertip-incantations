package com.poris.elegies.fingertip.binding;

import com.poris.elegies.fingertip.Fingertip;
import com.poris.elegies.fingertip.api.inventory.*;
import com.poris.elegies.fingertip.core.SourceResolver;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import java.util.UUID;

/**
 * What a HUD slot binds: nothing, equipment, a supply, an action or a key mapping (named ItemBinding before 2026-10-28;
 * saved data unchanged). Live inventory locations belong exclusively to the hand state.
 */
public record HudBinding(Kind kind, UUID identity, ItemStack template, String action) {
    public enum Kind { EMPTY, EQUIPMENT, SUPPLY, ACTION, MAPPING }
    public static HudBinding empty() { return new HudBinding(Kind.EMPTY, null, ItemStack.EMPTY, ""); }
    public boolean isItem() { return kind == Kind.EQUIPMENT || kind == Kind.SUPPLY; }

    /**
     * 指尖分类法 (the Fingertip sorting rule, named by the user on 2026-10-28): "this one" or "this kind", decided when the
     * slot is bound; 指尖检索法 then decides where to find it. A single item tracks the very item
     * when it has durability (unbreakable ones too) or data a fresh one lacks: enchantments, a name, contents, writing.
     * One exactly like a fresh one (a boat, a minecart, a totem, a saddle) binds its kind, so another takes over once
     * it is placed or spent; before preview19.29 those were equipment too and their slot went missing for good.
     * An item made to hold data of its own is the one item even when new (preview19.31): a book and quill turns into a
     * written book when signed and only its identity follows it there, as the external source smoke test found; the
     * same goes for bundles, shulker boxes and modded items carrying energy or spells.
     */
    public static boolean equipment(ItemStack stack) {
        return single(stack) && (stack.has(DataComponents.MAX_DAMAGE) || holdsData(stack.getItem()) || !plain(stack)
                || Equipable.get(stack) != null || stack.getEquipmentSlot() != null
                || !stack.getItem().canFitInsideContainerItems() || storesThings(stack));
    }
    // Something worn (vanilla's Equipable, or a slot declared through NeoForge's getEquipmentSlot) is the one item even
    // without durability (preview19.32): right-click wearing it hands back the piece worn before, and only "this one"
    // follows that, as with the armour stand. Found in Kaleidoscope Cookery's farmer clothes and straw hats. So is an
    // item vanilla's rule keeps out of container items (canFitInsideContainerItems): a container itself, like
    // Kaleidoscope's fruit basket (2026-10-28, user: "原版的物品接口是不是也可以补一下").
    /**
     * Whether the item declares NeoForge's standard storage: an inventory, energy or fluid (preview19.32, user: "这个也算
     * 通用解法吧"). A new Sophisticated backpack carries no data until first used, so three of them bound to F1-F3
     * all opened the first one; it declares an inventory from the start. Asking may let the item set itself up, as it
     * would on first use, so this is only asked when binding, never while looking items up.
     */
    private static boolean storesThings(ItemStack stack) {
        return stack.getCapability(Capabilities.ItemHandler.ITEM) != null || stack.getCapability(Capabilities.EnergyStorage.ITEM) != null
                || stack.getCapability(Capabilities.FluidHandler.ITEM) != null;
    }
    /** Whether the item's own defaults carry data beyond what every item has (writing, contents, energy, spells). */
    private static boolean holdsData(Item item) {
        for (var type : item.components().keySet()) if (!DataComponents.COMMON_ITEM_COMPONENTS.has(type)) return true;
        return false;
    }
    /** A non-stacking item that is neither food, a potion nor a bucket: what a slot can follow from hand to hand. */
    public static boolean single(ItemStack stack) {
        Item item = stack.getItem();
        return stack.getMaxStackSize() == 1 && !stack.has(DataComponents.FOOD)
                && !(item instanceof PotionItem) && !(item instanceof MilkBucketItem)
                && !(item instanceof BucketItem) && !(item instanceof SolidBucketItem);
    }
    /** Exactly a fresh one of its item; Fingertip's own identity mark does not count. */
    public static boolean plain(ItemStack stack) {
        for (var entry : stack.getComponentsPatch().entrySet())
            if (entry.getKey() != Fingertip.IDENTITY.get()) return false;
        return true;
    }
    /**
     * A supply of one kind of single item: any of that item counts, whatever its data becomes in use (a backpack given
     * its id when first opened, a charged tool), so the slot never loses it over that.
     */
    public boolean byKind() { return kind == Kind.SUPPLY && single(template) && plain(template) && !holdsData(template.getItem()); }
    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (kind == Kind.EQUIPMENT) return identity != null && identity.equals(stack.get(Fingertip.IDENTITY.get()));
        if (kind != Kind.SUPPLY) return false;
        return byKind() ? stack.is(template.getItem()) : ItemStack.isSameItemSameComponents(template, stack);
    }
    public SourceResolver.Result findSource(Player player) {
        return findSource(player, null);
    }
    /**
     * Both sides verify real stock with the same 指尖检索法: the held stack while it is still valid, otherwise the first
     * match from the lowest number. Pass heldSource only for the currently selected HUD entry.
     */
    public SourceResolver.Result findSource(Player player, SourceAddress heldSource) {
        if (!isItem() || kind == Kind.EQUIPMENT && identity == null) return SourceResolver.Result.MISSING;
        if (heldSource != null && InventorySources.selectable(player, heldSource) && matches(InventorySources.read(player, heldSource)))
            return new SourceResolver.Result(SourceResolver.Status.READY, heldSource);
        var search = new SourceResolver();
        InventorySources.visitWhile(player, (address, access, stack) -> !matches(stack) || search.accept(address, access));
        return search.result();
    }
    public CompoundTag save(HolderLookup.Provider lookup) {
        var tag = new CompoundTag();
        tag.putString("kind", kind.name());
        if (identity != null) tag.putUUID("identity", identity);
        tag.putString("action", action);
        if (!template.isEmpty()) tag.put("template", template.save(lookup));
        return tag;
    }
    public static HudBinding load(CompoundTag tag, HolderLookup.Provider lookup) {
        try {
            Kind kind = Kind.valueOf(tag.getString("kind"));
            ItemStack template = ItemStack.parseOptional(lookup, tag.getCompound("template"));
            // A slot saved before preview19.29 as equipment that is now a kind (a bound boat or totem) reads as one.
            if (kind == Kind.EQUIPMENT && !template.isEmpty() && !equipment(template)) {
                var kindTemplate = template.copyWithCount(1); kindTemplate.remove(Fingertip.IDENTITY.get());
                return new HudBinding(Kind.SUPPLY, null, kindTemplate, tag.getString("action"));
            }
            return new HudBinding(kind, tag.hasUUID("identity") ? tag.getUUID("identity") : null,
                    template.copyWithCount(1), tag.getString("action"));
        } catch (IllegalArgumentException error) { return empty(); }
    }
}
