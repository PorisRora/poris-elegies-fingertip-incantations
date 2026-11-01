package com.poris.elegies.fingertip.client.hud;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.poris.elegies.fingertip.Fingertip;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

/** Original generated PNGs plus measured UVs: no per-frame image work or font rasterization. */
@EventBusSubscriber(modid = Fingertip.MOD_ID, value = Dist.CLIENT)
final class ShortcutGlyphs {
    private static final String DIRECTORY = "textures/gui/keyglyphs/";
    private static final Face EMPTY = new Face(Map.of());
    private static Map<Theme, Face> faces = Map.of();

    @SubscribeEvent
    public static void register(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener)ShortcutGlyphs::reload);
    }

    private static void reload(ResourceManager resources) {
        var next = new HashMap<Theme, Face>();
        for (var theme : Theme.values()) {
            try {
                next.put(theme, read(resources, theme.id));
            } catch (IOException | RuntimeException error) {
                Fingertip.LOGGER.error("Could not load {} shortcut glyphs; retaining the last valid face", theme.id, error);
                next.put(theme, faces.getOrDefault(theme, EMPTY));
            }
        }
        faces = Map.copyOf(next);
    }

    static Face current() { return faces.getOrDefault(Theme.current(), EMPTY); }

    private static Face read(ResourceManager resources, String style) throws IOException {
        try (var reader = resources.openAsReader(Fingertip.id(DIRECTORY + style + ".json"))) {
            var json = JsonParser.parseReader(reader).getAsJsonObject();
            var atlases = json.getAsJsonArray("atlases");
            var glyphs = new HashMap<Character, Glyph>();
            for (var entry : json.getAsJsonObject("glyphs").entrySet()) {
                var value = entry.getValue().getAsJsonObject();
                var atlas = atlases.get(value.get("atlas").getAsInt()).getAsJsonObject();
                var texture = Fingertip.id(DIRECTORY + atlas.get("file").getAsString());
                int x = value.get("x").getAsInt(), y = value.get("y").getAsInt();
                int w = value.get("w").getAsInt(), h = value.get("h").getAsInt();
                int aw = atlas.get("width").getAsInt(), ah = atlas.get("height").getAsInt();
                float height = value.get("height").getAsFloat(), top = value.get("top").getAsFloat();
                if (entry.getKey().length() != 1 || x < 0 || y < 0 || w <= 0 || h <= 0 || x + w > aw || y + h > ah
                        || !Float.isFinite(height) || height <= 0 || height > 1 || !Float.isFinite(top) || top < 0 || top > 1)
                    throw new IOException("Invalid glyph region: " + entry.getKey());
                if (resources.getResource(texture).isEmpty()) throw new IOException("Missing glyph atlas: " + texture);
                // The json keeps the original atlas's coordinates; TextureCrops places the glyph in the cropped image.
                var at = TextureCrops.area(texture, x, y, w, h, aw, ah);
                glyphs.put(entry.getKey().charAt(0), new Glyph(texture, at.u(), at.v(), w, h, at.atlasWidth(), at.atlasHeight(), height, top));
            }
            for (char c : "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+-=/\\.,;'`[]↑↓←→".toCharArray())
                if (!glyphs.containsKey(c)) throw new IOException("Missing glyph: " + c);
            return new Face(Map.copyOf(glyphs));
        }
    }

    record Face(Map<Character, Glyph> glyphs) {
        private static final float GAP = .08F;
        boolean supports(String text) { return text.chars().allMatch(c -> glyphs.containsKey((char)c)); }
        float width(String text) {
            if (!supports(text)) return Minecraft.getInstance().font.width(text) / 8F;
            float width = 0;
            for (int i = 0; i < text.length(); i++) width += glyphs.get(text.charAt(i)).advance() + (i == 0 ? 0 : GAP);
            return width;
        }
        void draw(GuiGraphics graphics, String text, float x, float y, float height) {
            if (!supports(text)) {
                // A broken external resource pack must not make a bound shortcut unidentified.
                graphics.pose().pushPose();
                graphics.pose().translate(x, y, 0);
                graphics.pose().scale(height / 8F, height / 8F, 1);
                graphics.drawString(Minecraft.getInstance().font, text, 0, 0, EditorTheme.TEXT, false);
                graphics.pose().popPose();
                return;
            }
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            try {
                for (int i = 0; i < text.length(); i++) {
                    var glyph = glyphs.get(text.charAt(i));
                    graphics.pose().pushPose();
                    graphics.pose().translate(x, y + glyph.top * height, 0);
                    float scale = height * glyph.height / glyph.h;
                    graphics.pose().scale(scale, scale, 1);
                    if (!BlitBatch.add(graphics, glyph.texture, 0, 0, glyph.w, glyph.h, glyph.x, glyph.y, glyph.w, glyph.h, glyph.aw, glyph.ah))
                        graphics.blit(glyph.texture, 0, 0, glyph.w, glyph.h, glyph.x, glyph.y, glyph.w, glyph.h, glyph.aw, glyph.ah);
                    graphics.pose().popPose();
                    x += (glyph.advance() + GAP) * height;
                }
            } finally { RenderSystem.disableBlend(); }
        }
    }

    record Glyph(ResourceLocation texture, int x, int y, int w, int h, int aw, int ah, float height, float top) {
        float advance() { return w * height / h; }
    }

    private ShortcutGlyphs() {}
}
