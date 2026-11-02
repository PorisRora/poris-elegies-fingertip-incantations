package com.poris.elegies.fingertip.mixin.client;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Map;

@Mixin(KeyMapping.class)
public interface KeyMappingAccess {
    @Accessor("ALL") static Map<String, KeyMapping> fingertip$all() { throw new AssertionError(); }
    @Accessor("key") com.mojang.blaze3d.platform.InputConstants.Key fingertip$key();
    @Accessor("clickCount") int fingertip$clickCount();
    @Accessor("clickCount") void fingertip$clickCount(int count);
    @Accessor("isDown") void fingertip$down(boolean down);
}
