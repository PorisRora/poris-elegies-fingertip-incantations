package com.poris.elegies.fingertip.compat.irons;

import com.poris.elegies.fingertip.Fingertip;
import com.poris.elegies.fingertip.api.ActionCatalogInfo;
import com.poris.elegies.fingertip.api.ActionState;
import com.poris.elegies.fingertip.api.FingertipAction;
import com.poris.elegies.fingertip.api.FingertipApi;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.SelectionOption;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Every registered spell is an action. It is listed while it is in the current spell list (the equipped spell book,
 * then spell items in the hands, in Iron's order), at the level it has there. A frame's lookups all read one snapshot
 * of Iron's client data, taken once per client tick.
 */
final class IronsSpellsClient {
    private static final Snapshot NO_PLAYER = new Snapshot(null, 0, Map.of(), Map.of());
    private static List<FingertipAction> actions;
    private static Snapshot snapshot = NO_PLAYER;

    static void register() {
        FingertipApi.registerProvider(Fingertip.id("irons_spells"), IronsSpellsClient::actions);
        // The group's line: what the spells come from, the spell book first, as Iron's lists them (0.1.5-beta).
        FingertipApi.registerSources(IronsSpells.MOD_ID, () -> List.copyOf(snapshot().sources().values()));
        // With nothing that gives spells, the group says so (2026-10-30).
        FingertipApi.registerEmptyHint(IronsSpells.MOD_ID, () -> snapshot().sources().isEmpty()
                ? Component.translatable("poris_elegies_fingertip_incantations.irons.no_spell_book") : null);
        // Iron's spell bar, hidden while the HUD is up unless switched on: the HUD shows the spells, and the bar sat under
        // the slots (2026-10-29, "只管术法栏"). Iron's spell wheel keys are no longer
        // switched here (2026-10-28 to 0.1.4-beta): like any mod's keys, the mod whitelist gives them back to Iron's.
        FingertipApi.registerLayerSwitch(new FingertipApi.LayerSwitch(IronsSpells.MOD_ID,
                Component.translatable("poris_elegies_fingertip_incantations.irons.spell_bar"),
                Component.translatable("poris_elegies_fingertip_incantations.irons.spell_bar_hint"),
                List.of(ResourceLocation.fromNamespaceAndPath(IronsSpells.MOD_ID, "spell_bar"))));
    }

    /** Built on first use, once Iron's registry is complete; Iron's empty "none" spell is not a skill. */
    private static List<FingertipAction> actions() {
        if (actions == null) {
            var scroll = new ItemStack(BuiltInRegistries.ITEM.getOptional(
                    ResourceLocation.fromNamespaceAndPath(IronsSpells.MOD_ID, "scroll")).orElse(Items.PAPER));
            actions = SpellRegistry.REGISTRY.stream().filter(spell -> spell != SpellRegistry.none())
                    .<FingertipAction>map(spell -> new SpellAction(spell, IronsSpells.actionId(spell.getSpellResource()), scroll, summons(spell)))
                    .toList();
        }
        return actions;
    }

    /**
     * A summon is a toggle on the HUD (2026-10-29): its recast carries Iron's summoned-entity data, and Iron's ends that
     * recast itself when the summons are all gone or their time runs out. Read from Iron's data, not the spell's name
     * ("Summon Ender Chest" only opens the chest; "Raise Hell" recasts without summoning), so addon summons that use
     * Iron's summon system count too.
     */
    private static boolean summons(AbstractSpell spell) {
        try { return spell.getEmptyCastData() instanceof SummonedEntitiesCastData; }
        catch (RuntimeException | LinkageError error) { return false; }
    }

    /**
     * The first copy of each spell in the list, and what the spells come from, by Iron's slot name in Iron's order: the
     * spell book, then armour and hands that carry spells. Other curio slots go unnamed; Fingertip does not compile
     * against Curios.
     */
    private record Snapshot(LocalPlayer player, int tick, Map<ResourceLocation, SelectionOption> equipped, Map<String, Component> sources) {}
    private static Snapshot snapshot() {
        var player = Minecraft.getInstance().player;
        if (player == null) return snapshot = NO_PLAYER;
        if (snapshot.player() != player || snapshot.tick() != player.tickCount) {
            var equipped = new HashMap<ResourceLocation, SelectionOption>();
            var sources = new LinkedHashMap<String, Component>();
            for (var option : ClientMagicData.getSpellSelectionManager().getAllSpells()) {
                if (option.spellData == null || option.spellData == SpellData.EMPTY) continue;
                equipped.putIfAbsent(option.spellData.getSpell().getSpellResource(), option);
                // Every slot that carries a spell, so a staff holding the book's spells is named too.
                sources.computeIfAbsent(option.slot, slot -> source(player, slot));
            }
            snapshot = new Snapshot(player, player.tickCount, equipped, sources);
        }
        return snapshot;
    }
    /** The item in one of Iron's spell slots: "spellbook", or an equipment slot such as "mainhand"; null otherwise. */
    private static Component source(LocalPlayer player, String slot) {
        ItemStack stack;
        if ("spellbook".equals(slot)) stack = Utils.getPlayerSpellbookStack(player);
        else {
            try { stack = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.byName(slot)); }
            catch (IllegalArgumentException notEquipment) { return null; }
        }
        return stack == null || stack.isEmpty() ? null : stack.getHoverName();
    }

    private record SpellAction(AbstractSpell spell, ResourceLocation id, ItemStack icon, boolean summon) implements FingertipAction {
        @Override public Component name() { return spell.getDisplayName(Minecraft.getInstance().player); }
        @Override public ResourceLocation iconTexture() { return spell.getSpellIconResource(); }
        /** School, level and mana cost. */
        @Override public String category() {
            int level = shownLevel();
            return Component.translatable("poris_elegies_fingertip_incantations.irons.spell_info", spell.getSchoolType().getDisplayName(), level,
                    spell.getManaCost(level)).getString();
        }
        /** Iron's own info lines, as its spell tooltips show them: damage, radius, duration and so on (0.2.2-beta). */
        @Override public List<Component> details() {
            var player = Minecraft.getInstance().player;
            return player == null ? List.of() : List.copyOf(spell.getUniqueInfo(shownLevel(), player));
        }
        /** The level the spell has in the list, or its lowest level. */
        private int shownLevel() {
            var option = snapshot().equipped().get(spell.getSpellResource());
            return option == null ? spell.getMinLevel() : level(option);
        }
        @Override public ActionCatalogInfo catalogInfo() {
            var current = snapshot();
            var option = current.equipped().get(spell.getSpellResource());
            Component source = option == null ? null : current.sources().get(option.slot);
            return new ActionCatalogInfo(ActionCatalogInfo.Kind.SKILL, option != null, source);
        }
        /** Iron's own cast check, so the slot dims or shades where Iron's would refuse; the server checks again. */
        @Override public ActionState state() {
            var player = Minecraft.getInstance().player;
            if (player == null || player.isSpectator() || !player.isAlive()) return blocked("poris_elegies_fingertip_incantations.irons.not_playing");
            var option = snapshot().equipped().get(spell.getSpellResource());
            if (option == null) return blocked("poris_elegies_fingertip_incantations.irons.not_equipped");
            if (!spell.isEnabled()) return blocked("poris_elegies_fingertip_incantations.irons.disabled");
            if (spell.requiresLearning() && !spell.isLearned(player)) return blocked("poris_elegies_fingertip_incantations.irons.unlearned");
            // Inside a recast window: a summon is on (pressing again dismisses it), any other spell has presses left.
            // The window's time left is Iron's to show (its recast overlay), so the count draws no refill bar.
            var recast = ClientMagicData.getRecasts().getRecastInstance(spell.getSpellId());
            if (recast != null && recast.getRemainingRecasts() > 0)
                return summon
                        ? new ActionState(true, 0, false, Component.translatable("poris_elegies_fingertip_incantations.irons.summoned"), 0,
                                ActionState.Toggle.ON, null)
                        : new ActionState(true, 0, false, Component.translatable("poris_elegies_fingertip_incantations.irons.recast",
                                recast.getRemainingRecasts()), 0, ActionState.Toggle.NONE,
                                new ActionState.Charges(recast.getRemainingRecasts(), recast.getTotalRecasts()));
            int level = level(option), cost = spell.getManaCost(level), mana = ClientMagicData.getPlayerMana();
            // Creative play skips mana and cooldowns, as under Iron's default server settings.
            boolean creative = player.isCreative();
            boolean free = creative || !option.getCastSource().consumesMana();
            boolean lacks = !free && mana < cost;
            float cooldown = creative || !option.getCastSource().respectsCooldown() ? 0 : ClientMagicData.getCooldownPercent(spell);
            Component reason = lacks ? Component.translatable("poris_elegies_fingertip_incantations.irons.mana", cost, mana)
                    : cooldown > 0 ? Component.translatable("poris_elegies_fingertip_incantations.irons.cooldown", seconds(spell))
                    : free ? Component.translatable("poris_elegies_fingertip_incantations.irons.ready_free", level)
                    : Component.translatable("poris_elegies_fingertip_incantations.irons.ready", level, cost);
            return new ActionState(true, cooldown, lacks, reason, cooldown > 0 ? remaining(spell) : 0,
                    summon ? ActionState.Toggle.OFF : ActionState.Toggle.NONE, null);
        }
        @Override public void press() { FingertipApi.sendToServer(id, true); }
        private int level(SelectionOption option) {
            return spell.getLevelFor(option.spellData.getLevel(), Minecraft.getInstance().player);
        }
        private static ActionState blocked(String key) { return new ActionState(false, 0, false, Component.translatable(key)); }
    }
    private static String seconds(AbstractSpell spell) {
        return String.format(Locale.ROOT, "%.1f", remaining(spell));
    }
    private static float remaining(AbstractSpell spell) {
        var cooldown = ClientMagicData.getCooldowns().getSpellCooldowns().get(spell.getSpellId());
        return cooldown == null ? 0 : cooldown.getCooldownRemaining() / 20f;
    }
    private IronsSpellsClient() {}
}
