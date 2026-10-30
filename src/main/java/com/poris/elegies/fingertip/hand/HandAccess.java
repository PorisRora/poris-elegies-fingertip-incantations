package com.poris.elegies.fingertip.hand;

import net.minecraft.world.entity.player.Inventory;

public interface HandAccess {
    HandSource fingertip$hand();
    static HandSource of(Inventory inventory) { return ((HandAccess) inventory).fingertip$hand(); }
}
