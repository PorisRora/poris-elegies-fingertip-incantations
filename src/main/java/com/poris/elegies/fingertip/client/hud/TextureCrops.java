package com.poris.elegies.fingertip.client.hud;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.poris.elegies.fingertip.Fingertip;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The key glyphs and the classic and Poris slot frames ship cropped to the parts the code reads (2026-10-29, user:
 * "Crop those"): the texture atlas generator copies each such rectangle, pixel for pixel with a 2 px margin,
 * into a smaller image of the same name and records in textures/gui/crops.json where each block went. The code keeps
 * the coordinates of the original atlases; this moves them into the shipped image. Images not listed pass through as they
 * are. Buttons, panels, wheel rims and the Starlight frames stay whole: their sampling falls exactly between two source
 * pixels at some GUI scales, and the side picked would change with the image's size.
 */
final class TextureCrops {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PREFIX = "textures/gui/";
    /** Where a rectangle of an original lies in the shipped image, and that image's size. */
    record Area(int u, int v, int atlasWidth, int atlasHeight) {}
    private record Block(int x, int y, int width, int height, int newX, int newY) {}
    private record Crop(int originalWidth, int originalHeight, int width, int height, List<Block> blocks) {}
    private static final Map<String, Crop> CROPS = load();
    private static final Set<String> MISSES = ConcurrentHashMap.newKeySet();

    /** The rectangle (u, v, width, height) of the original texture, whose size was originalWidth x originalHeight. */
    static Area area(ResourceLocation texture, int u, int v, int width, int height, int originalWidth, int originalHeight) {
        var crop = CROPS.get(texture.getPath().startsWith(PREFIX) ? texture.getPath().substring(PREFIX.length()) : "");
        if (crop == null) return new Area(u, v, originalWidth, originalHeight);
        if (crop.originalWidth == originalWidth && crop.originalHeight == originalHeight)
            for (var b : crop.blocks)
                if (b.x <= u && b.y <= v && u + width <= b.x + b.width && v + height <= b.y + b.height)
                    return new Area(b.newX + u - b.x, b.newY + v - b.y, crop.width, crop.height);
        if (MISSES.add(texture + " " + u + "," + v + " " + width + "x" + height))
            LOGGER.error("No cropped block of {} holds {},{} {}x{} (of {}x{}); it draws from the wrong place",
                    texture, u, v, width, height, originalWidth, originalHeight);
        return new Area(u, v, originalWidth, originalHeight);
    }
    /** Rectangles asked for that no block holds; empty unless the code and crops.json disagree. */
    static Set<String> misses() { return Set.copyOf(MISSES); }
    private static Map<String, Crop> load() {
        var crops = new HashMap<String, Crop>();
        try (var in = TextureCrops.class.getResourceAsStream("/assets/" + Fingertip.MOD_ID + "/" + PREFIX + "crops.json")) {
            if (in == null) { LOGGER.error("{}crops.json is missing; cropped images will draw from the wrong place", PREFIX); return Map.of(); }
            var json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : json.getAsJsonObject("textures").entrySet()) {
                JsonObject value = entry.getValue().getAsJsonObject();
                var original = value.getAsJsonArray("original"); var size = value.getAsJsonArray("size");
                var blocks = new ArrayList<Block>();
                for (var b : value.getAsJsonArray("blocks")) {
                    var n = b.getAsJsonArray();
                    blocks.add(new Block(n.get(0).getAsInt(), n.get(1).getAsInt(), n.get(2).getAsInt(), n.get(3).getAsInt(),
                            n.get(4).getAsInt(), n.get(5).getAsInt()));
                }
                crops.put(entry.getKey(), new Crop(original.get(0).getAsInt(), original.get(1).getAsInt(),
                        size.get(0).getAsInt(), size.get(1).getAsInt(), List.copyOf(blocks)));
            }
        } catch (Exception error) {
            LOGGER.error("Cannot read {}crops.json; cropped images will draw from the wrong place", PREFIX, error);
        }
        return Map.copyOf(crops);
    }
    private TextureCrops() {}
}
