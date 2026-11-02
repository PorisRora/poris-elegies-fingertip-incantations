package com.poris.elegies.fingertip;

import com.mojang.logging.LogUtils;
import com.poris.elegies.fingertip.network.FingertipNetwork;
import com.poris.elegies.fingertip.server.BindingService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import java.util.UUID;
import java.util.function.Supplier;

public final class Fingertip implements ModInitializer {
    public static final String MOD_ID = "poris_elegies_fingertip_incantations";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final int SLOT_COUNT = 64;
    private static final DataComponentType<UUID> IDENTITY_TYPE = DataComponentType.<UUID>builder()
            .persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC).build();
    public static final Supplier<DataComponentType<UUID>> IDENTITY = () -> IDENTITY_TYPE;
    @Override public void onInitialize() {
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id("equipment_identity"), IDENTITY_TYPE);
        FingertipNetwork.register();
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> BindingService.login(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> BindingService.logout(handler.player));
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> BindingService.clone(oldPlayer, newPlayer));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> BindingService.respawn(newPlayer));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> BindingService.changedDimension(player));
    }
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MOD_ID, path); }
}
