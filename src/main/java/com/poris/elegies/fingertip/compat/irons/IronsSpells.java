package com.poris.elegies.fingertip.compat.irons;

import com.poris.elegies.fingertip.api.FingertipServerApi;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Iron's Spells 'n Spellbooks on the Fingertip HUD (2026-10-28, user): the spells in the player's current spell list
 * are skills, bound to a slot by spell rather than by list position, so reordering the spell book or holding a staff
 * never casts the wrong one. Written against Fingertip's public API only, as the example adapter of
 * docs/INTEGRATION.md.
 * <p>
 * Loaded only when Iron's is installed. Iron's owns all gameplay: the server casts through Iron's own quick cast, which
 * checks mana, cooldown and everything else. Iron's is All Rights Reserved, so it is only compiled against, never
 * bundled, and its spell icons are drawn from its own assets. This class names no Iron's type, so Fingertip can load it.
 */
public final class IronsSpells {
    public static final String MOD_ID = "irons_spellbooks";
    private static final String PREFIX = "spell/";

    public static void init(IEventBus modBus) {
        FingertipServerApi.registerHandler(MOD_ID, IronsSpellsServer::handle);
        if (FMLEnvironment.dist.isClient()) modBus.addListener(FMLClientSetupEvent.class, event -> IronsSpellsClient.register());
    }
    /**
     * irons_spellbooks:spell/&lt;the spell's mod ID&gt;/&lt;spell path&gt;: under Iron's mod ID, so spells added by Iron's
     * addons share its group and its server handler.
     */
    public static ResourceLocation actionId(ResourceLocation spell) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, PREFIX + spell.getNamespace() + "/" + spell.getPath());
    }
    /** The spell an action ID names; null for any other ID. */
    public static ResourceLocation spellOf(ResourceLocation action) {
        if (!action.getNamespace().equals(MOD_ID) || !action.getPath().startsWith(PREFIX)) return null;
        String rest = action.getPath().substring(PREFIX.length());
        int slash = rest.indexOf('/');
        return slash <= 0 ? null : ResourceLocation.tryBuild(rest.substring(0, slash), rest.substring(slash + 1));
    }
    private IronsSpells() {}
}
