package com.poris.elegies.fingertip.client.hud;

import com.poris.elegies.fingertip.Fingertip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;

/**
 * Durability left as a percentage on worn items (2026-10-30, user: "有耐久的物品要不要加个显示百分比 不过背包的也要显示百分比了
 * 可以显示设置加个开关？"). A NeoForge item decorator, so every screen that draws item decorations shows it, the inventory
 * and containers alike, and the HUD too (HudRenderer runs each item's decorators in its last pass). Only on items
 * vanilla counts as worn: damageable and damaged, so nothing on unbreakable or unworn items (),
 * and nothing for a bar an item uses for something else, such as a bundle's fullness. It takes the durability bar's place, small and centred under the
 * icon (): ItemBarMixin and HudRenderer leave the bar out
 * where {@link #shows} holds. Display settings switch it ({@link HudLayout#durabilityPercent}); switched off, the bar is back.
 */
@EventBusSubscriber(modid = Fingertip.MOD_ID, value = Dist.CLIENT)
public final class DurabilityPercent implements IItemDecorator {
    private static final DurabilityPercent INSTANCE = new DurabilityPercent();

    // Components can give any item durability, so every item gets the decorator; it returns at once for the rest.
    @SubscribeEvent public static void register(RegisterItemDecorationsEvent event) {
        for (var item : BuiltInRegistries.ITEM) event.register(item, INSTANCE);
    }

    /** Whether the stack shows its percent, in place of the durability bar. */
    public static boolean shows(ItemStack stack) {
        return HudLayout.INSTANCE.durabilityPercent && stack.isDamageableItem() && stack.isDamaged();
    }

    @Override public boolean render(GuiGraphics g, Font font, ItemStack stack, int x, int y) {
        if (!shows(stack)) return false;
        int max = stack.getMaxDamage(), damage = stack.getDamageValue();
        String text = Durability.percent(damage, max) + "%";
        // The durability bar's own colour, green to red.
        int color = Mth.hsvToRgb(Math.max(0, (max - damage) / (float)max) / 3, 1, 1);
        // About 0.6, at a whole number of screen pixels per font pixel (3 at GUI scale 5).
        float scale = GuiPixels.scale(.6F);
        // Outlined like the charges, not shadowed: a shadow in the bar's colour ran into the digits, so 4 read as 9. Centred
        // on the icon, the digits' foot where the bar's was.
        float left = GuiPixels.snap(x + 8 - (font.width(text) - 1) * scale / 2);
        float top = GuiPixels.snap(y + 15 - 7 * scale);
        g.pose().pushPose();
        try {
            g.pose().translate(left, top, 200);
            g.pose().scale(scale, scale, 1);
            GuiPixels.outlined(g, font, text, 0, 0, 0xFF000000 | color, GuiPixels.OUTLINE);
        } finally { g.pose().popPose(); }
        return false;
    }
}
