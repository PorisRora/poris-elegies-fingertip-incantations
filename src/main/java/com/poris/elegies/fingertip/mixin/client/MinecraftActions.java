package com.poris.elegies.fingertip.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Calls the vanilla action itself, without synthesizing a physical key shared by other mods. */
@Mixin(Minecraft.class)
public interface MinecraftActions {
    @Invoker("pickBlock") void fingertip$pickBlock();
    @Invoker("openChatScreen") void fingertip$openChat(String initial);
}
