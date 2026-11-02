package com.poris.elegies.fingertip.server;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
public interface PlayerData {
    CompoundTag fingertip$data();
    static CompoundTag of(ServerPlayer player) { return ((PlayerData)player).fingertip$data(); }
}
