package com.poris.elegies.fingertip.mixin.client;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiPlayerGameMode.class)
public interface GameModeAccess { @Accessor("carriedIndex") void fingertip$carriedIndex(int index); }
