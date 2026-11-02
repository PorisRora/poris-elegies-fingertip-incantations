package com.poris.elegies.fingertip.mixin.client;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccess {
    @Accessor("leftPos") int fingertip$left();
    @Accessor("topPos") int fingertip$top();
    @Accessor("imageWidth") int fingertip$width();
}
