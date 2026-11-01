package com.poris.elegies.fingertip.client.hud;

import org.junit.jupiter.api.Test;
import java.util.function.ToIntFunction;
import static org.junit.jupiter.api.Assertions.*;

class TextLinesTest {
    /** A stand-in font: Chinese 9 px, space 4, "…" 9, anything else 6. */
    private static final ToIntFunction<String> WIDTH = text -> text.codePoints().map(c ->
            c == ' ' ? 4 : c == '…' ? 9 : Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN || c >= 0xFF00 ? 9 : 6).sum();

    @Test void oneLineShrinksByWholeScreenPixelsThenCuts() {
        String text = "abcdefghijklmnopqrst";   // 120 px
        assertEquals(new TextLines.Line(text, 1, false), TextLines.line(text, 120, 5, WIDTH));
        var shrunk = TextLines.line(text, 100, 5, WIDTH);
        assertEquals(.8F, shrunk.scale(), 1e-6); assertFalse(shrunk.cut());
        var cut = TextLines.line(text, 70, 5, WIDTH);
        assertEquals(.6F, cut.scale(), 1e-6); assertTrue(cut.cut());
        assertTrue(cut.text().endsWith("…") && WIDTH.applyAsInt(cut.text()) * .6F <= 70);
        // At GUI scale 2 the only whole-pixel step under 1 is 0.5, below 60 %: no shrinking, just the cut.
        var two = TextLines.line(text, 100, 2, WIDTH);
        assertEquals(1F, two.scale(), 1e-6); assertTrue(two.cut()); assertTrue(WIDTH.applyAsInt(two.text()) <= 100);
    }

    @Test void numbersAndTheWordsAroundThemStayTogether() {
        String text = "第 12 格已经在用这个键";
        var wrapped = TextLines.wrap(text, 50, 3, WIDTH);
        assertFalse(wrapped.cut());
        assertTrue(wrapped.lines().getFirst().startsWith("第 12 格"), wrapped.lines().toString());
        assertEquals(text.replace(" ", ""), String.join("", wrapped.lines()).replace(" ", ""));
        for (var line : wrapped.lines()) assertTrue(WIDTH.applyAsInt(line) <= 50, line);
    }

    @Test void phrasesEndAtTheSeparatorBeforeAWordBreak() {
        var wrapped = TextLines.wrap("Backspace 清除 · Esc 取消", 110, 2, WIDTH);
        assertEquals(2, wrapped.lines().size());
        assertEquals("Backspace 清除 ·", wrapped.lines().get(0));
        assertEquals("Esc 取消", wrapped.lines().get(1));
    }

    @Test void theLastLineEndsWithAnEllipsisWhenTextIsLeft() {
        String text = "这个键留给基础操作，请换一个别的键试试看吧，一定可以找到合适的";
        var two = TextLines.wrap(text, 60, 2, WIDTH);
        assertEquals(2, two.lines().size()); assertTrue(two.cut());
        assertTrue(two.lines().get(1).endsWith("…"));
        assertTrue(WIDTH.applyAsInt(two.lines().get(1)) <= 60);
        // A new line in the text starts a new line; more paragraphs than lines end the last one with "…".
        assertEquals(java.util.List.of("ab", "cd"), TextLines.wrap("ab\ncd", 60, 2, WIDTH).lines());
        var more = TextLines.wrap("ab\ncd\nef", 60, 2, WIDTH);
        assertEquals("cd…", more.lines().get(1)); assertTrue(more.cut());
    }

    @Test void punctuationNeverStartsALine() {
        var wrapped = TextLines.wrap("一二三四五，六七八", 45, 3, WIDTH);
        for (var line : wrapped.lines()) assertFalse(line.startsWith("，"), wrapped.lines().toString());
    }

    @Test void cutKeepsAsMuchAsFits() {
        assertEquals("abc", TextLines.cut("abc", 18, WIDTH));
        assertEquals("ab…", TextLines.cut("abcdef", 21, WIDTH));
        assertEquals("", TextLines.cut("abcdef", 5, WIDTH));
    }
}
