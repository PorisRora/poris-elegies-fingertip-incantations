package com.poris.elegies.fingertip;

import com.mojang.logging.LogUtils;
import com.poris.elegies.fingertip.compat.irons.IronsSpells;
import com.poris.elegies.fingertip.network.FingertipNetwork;
import com.poris.elegies.fingertip.server.BindingService;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import java.util.UUID;

@Mod(Fingertip.MOD_ID)
public final class Fingertip {
    public static final String MOD_ID = "poris_elegies_fingertip_incantations";
    public static final Logger LOGGER = LogUtils.getLogger();
    /** Fingertip slots per player, ids 0 to 63: bindings, keys and requests all count to this. */
    public static final int SLOT_COUNT = 64;
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MOD_ID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> IDENTITY = COMPONENTS.register(
            "equipment_identity", () -> DataComponentType.<UUID>builder().persistent(UUIDUtil.CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC).build());

    public Fingertip(IEventBus bus) {
        COMPONENTS.register(bus);
        bus.addListener(FingertipNetwork::register);
        NeoForge.EVENT_BUS.register(BindingService.class);
        // Built-in adapters for other mods' skills, loaded only with that mod (2026-10-28).
        if (ModList.get().isLoaded(IronsSpells.MOD_ID)) IronsSpells.init(bus);
    }
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MOD_ID, path); }
}
