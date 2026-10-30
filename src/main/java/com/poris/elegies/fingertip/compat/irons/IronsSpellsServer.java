package com.poris.elegies.fingertip.compat.irons;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * A slot's key names a spell; the server looks it up in the player's spell list as it is now and casts it as Iron's
 * quick cast key for that position would. Iron's quick cast builds the same list, so the position stays right.
 */
final class IronsSpellsServer {
    static void handle(ServerPlayer player, ResourceLocation action, boolean pressed) {
        var spell = IronsSpells.spellOf(action);
        if (!pressed || spell == null || player.isSpectator() || !player.isAlive()) return;
        var options = new SpellSelectionManager(player).getAllSpells();
        // The first copy, as the client lists it: the spell book's spells come before spell items in the hands.
        for (int index = 0; index < options.size(); index++) {
            SpellData data = options.get(index).spellData;
            if (data != null && data != SpellData.EMPTY && spell.equals(data.getSpell().getSpellResource())) {
                Utils.serverSideInitiateQuickCast(player, index);
                return;
            }
        }
        player.displayClientMessage(Component.translatable("poris_elegies_fingertip_incantations.irons.not_equipped"), true);
    }
    private IronsSpellsServer() {}
}
