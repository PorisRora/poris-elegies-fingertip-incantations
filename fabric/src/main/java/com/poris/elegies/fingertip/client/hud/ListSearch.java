package com.poris.elegies.fingertip.client.hud;

import net.minecraft.client.gui.components.Button;
import java.util.Locale;

/** The search boxes of the editor's lists, and the page arrows of the lists shown a page at a time. */
final class ListSearch {
    /** Every word of {@code query} appears in {@code text}, ignoring case; a blank query matches everything. */
    static boolean matches(String query, String text) {
        String haystack = text.toLowerCase(Locale.ROOT);
        for (String word : query.strip().toLowerCase(Locale.ROOT).split("\\s+")) if (!haystack.contains(word)) return false;
        return true;
    }
    /** Pages of {@code capacity} items each, at least one. */
    static int pages(int items, int capacity) { return Math.max(1, (items + capacity - 1) / capacity); }
    /** {@code page} kept within {@code pages}; each arrow is active only when there is a page that way. */
    static int clampPage(int page, int pages, Button previous, Button next) {
        page = Math.clamp(page, 0, pages - 1);
        previous.active = page > 0; next.active = page + 1 < pages;
        return page;
    }
    private ListSearch() {}
}
