package com.poris.elegies.fingertip.client.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * 指尖排字法's rules on their own (see {@link TextFit}, which draws with them): fitting one line, cutting, and wrapping
 * with the break rules. No Minecraft types, so they can be tested on their own.
 */
final class TextLines {
    static final String ELLIPSIS = "…";
    static final float LEAST = .6F;
    /** One line as drawn: its text, the scale it is drawn at on top of the current one, and whether text was cut. */
    record Line(String text, float scale, boolean cut) {}
    /** Wrapped lines, and whether text was left out. */
    record Wrapped(List<String> lines, boolean cut) {}

    // A break after these ends a phrase: preferred.
    private static final String PHRASE_END = "·，。；：、！？,;:!?";
    // Never at the start of a line.
    private static final String NO_START = "·，。、；：！？）」』】》〉,.;:!?)%";
    // Never at the end of a line.
    private static final String NO_END = "（「『【《〈(";

    /**
     * One line no wider than {@code room} GUI pixels. {@code full} is the screen pixels on each font pixel at scale 1;
     * the largest smaller whole number of them that fits is used, down to {@link #LEAST}, and then the text is cut.
     */
    static Line line(String text, int room, int full, ToIntFunction<String> width) {
        int w = width.applyAsInt(text);
        if (w <= room) return new Line(text, 1, false);
        int least = Math.max(1, (int)Math.ceil(full * LEAST - 1e-6));
        for (int k = full - 1; k >= least; k--)
            if ((long)w * k <= (long)room * full) return new Line(text, (float)k / full, false);
        int scaledRoom = (int)Math.floor(room * (double)full / least);
        return new Line(cut(text, scaledRoom, width), (float)least / full, true);
    }
    /** {@code text}, or as much of it as fits in {@code room} followed by "…". */
    static String cut(String text, int room, ToIntFunction<String> width) {
        return width.applyAsInt(text) <= room ? text : ellipsized(text, room, width);
    }
    /** The longest start of {@code text} that fits in {@code room} with "…" after it (just "…" or nothing when tight). */
    static String ellipsized(String text, int room, ToIntFunction<String> width) {
        int ellipsis = width.applyAsInt(ELLIPSIS);
        if (ellipsis > room) return "";
        int lo = 0, hi = text.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (width.applyAsInt(text.substring(0, whole(text, mid))) + ellipsis <= room) lo = mid; else hi = mid - 1;
        }
        return text.substring(0, whole(text, lo)).stripTrailing() + ELLIPSIS;
    }
    /** {@code end}, moved back off the middle of a surrogate pair. */
    private static int whole(String text, int end) {
        return end > 0 && end < text.length() && Character.isLowSurrogate(text.charAt(end)) && Character.isHighSurrogate(text.charAt(end - 1)) ? end - 1 : end;
    }

    /** At most {@code maxLines} lines of {@code room}; "\n" starts a new one. */
    static Wrapped wrap(String text, int room, int maxLines, ToIntFunction<String> width) {
        var out = new ArrayList<String>();
        if (maxLines <= 0) return new Wrapped(out, !text.isBlank());
        String[] paragraphs = text.split("\n", -1);
        for (int p = 0; p < paragraphs.length; p++) {
            String rest = paragraphs[p].strip();
            do {
                if (out.size() == maxLines - 1) {
                    boolean more = false;
                    for (int q = p + 1; q < paragraphs.length; q++) more |= !paragraphs[q].isBlank();
                    if (!more && width.applyAsInt(rest) <= room) { out.add(rest); return new Wrapped(out, false); }
                    out.add(ellipsized(rest, room, width)); return new Wrapped(out, true);
                }
                int end = breakAt(rest, room, width);
                out.add(rest.substring(0, end).stripTrailing());
                rest = rest.substring(end).stripLeading();
            } while (!rest.isEmpty());
        }
        return new Wrapped(out, false);
    }
    /** Where the first line of {@code text} ends. */
    static int breakAt(String text, int room, ToIntFunction<String> width) {
        if (width.applyAsInt(text) <= room) return text.length();
        int lo = 1, hi = text.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (width.applyAsInt(text.substring(0, whole(text, mid))) <= room) lo = mid; else hi = mid - 1;
        }
        int fit = Math.max(1, whole(text, lo)), phrase = -1, word = -1;
        for (int i = 1; i <= fit && i < text.length(); i++) {
            int kind = opportunity(text, i);
            if (kind == 2) phrase = i; else if (kind == 1) word = i;
        }
        // A phrase end wins unless it would leave the line under a third full.
        if (phrase > 0 && width.applyAsInt(text.substring(0, phrase)) * 3 >= room) return phrase;
        if (word > 0) return word;
        if (phrase > 0) return phrase;
        return fit;
    }
    /** 2: a phrase ends before index {@code i}; 1: a word or a Chinese character does; 0: no break. */
    private static int opportunity(String text, int i) {
        char before = text.charAt(i - 1), after = text.charAt(i);
        char earlier = i >= 2 ? text.charAt(i - 2) : 0;
        if (NO_START.indexOf(after) >= 0 || NO_END.indexOf(before) >= 0) return 0;
        if (PHRASE_END.indexOf(before) >= 0 || before == ' ' && PHRASE_END.indexOf(earlier) >= 0) return 2;
        if (before == ' ') return Character.isDigit(earlier) || Character.isDigit(after) ? 0 : 1;
        return cjk(before) && cjk(after) ? 1 : 0;
    }
    private static boolean cjk(char c) {
        var script = Character.UnicodeScript.of(c);
        return script == Character.UnicodeScript.HAN || script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.KATAKANA || script == Character.UnicodeScript.HANGUL
                || c >= 0x3000 && c <= 0x303F || c >= 0xFF00 && c <= 0xFFEF;
    }
    private TextLines() {}
}
