package com.poris.elegies.fingertip.client.hud;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

class HudPlacementTest {
    private static final int BASE_W = 640, BASE_H = 346;
    private static int heightFor(int width) { return Math.max(240, Math.round(width * .54F)); }

    /** The user's layout of 2026-11-01 as placed full screen: the native row and off hand, and two rows at the left edge. */
    private static List<HudArrangement.Point> userLayout() {
        var points = new ArrayList<HudArrangement.Point>();
        for (int i = 0; i < 9; i++) points.add(new HudArrangement.Point(210 + 24 * i, 325));
        points.add(new HudArrangement.Point(424, 325));
        for (int j = 0; j < 9; j++) points.add(new HudArrangement.Point(4 + 26 * j, 274));
        for (int j = 0; j < 7; j++) points.add(new HudArrangement.Point(4 + 26 * j, 248));
        return points;
    }
    /** Frames never placed before: no earlier point. */
    private static List<HudPlacement.Frame> fresh(int count) {
        return IntStream.range(0, count).mapToObj(i -> new HudPlacement.Frame(null, 0, 0)).toList();
    }
    private static Set<Integer> all(int n) { return IntStream.range(0, n).boxed().collect(Collectors.toSet()); }
    /** A first record of frames never placed, where they stand. */
    private static List<HudPlacement.Frame> place(List<HudArrangement.Point> points, int width, int height) {
        var frames = HudPlacement.commit(points, points, fresh(points.size()), all(points.size()), HudPlacement.Edit.DROP, width, height);
        assertEquals(points.size(), frames.size());
        return IntStream.range(0, points.size()).mapToObj(frames::get).toList();
    }
    private static List<HudArrangement.Point> positions(HudPlacement.Result result) {
        return IntStream.range(0, result.x().length).mapToObj(i -> new HudArrangement.Point(result.x()[i], result.y()[i])).toList();
    }
    private static List<HudPlacement.Frame> applied(List<HudPlacement.Frame> stored, Map<Integer, HudPlacement.Frame> changes) {
        return IntStream.range(0, stored.size()).mapToObj(i -> changes.getOrDefault(i, stored.get(i))).toList();
    }
    private static void assertNoOverlap(HudPlacement.Result result, int width, int height) {
        int n = result.x().length;
        for (int i = 0; i < n; i++) {
            assertTrue(result.x()[i] >= 0 && result.y()[i] >= 0 && result.x()[i] + 20 <= width && result.y()[i] + 20 <= height,
                    "frame " + i + " off screen at " + width + "x" + height);
            for (int j = i + 1; j < n; j++)
                assertFalse(HudPlacement.Rect.cell(result.x()[i], result.y()[i]).near(HudPlacement.Rect.cell(result.x()[j], result.y()[j]), 0),
                        "frames " + i + " and " + j + " overlap at " + width + "x" + height);
        }
    }

    @Test void userLayoutStaysWholeFromFullScreenDownToTheSmallestWindow() {
        var frames = place(userLayout(), BASE_W, BASE_H);
        assertEquals(HudPlacement.Point9.BOTTOM, frames.get(0).point(), "the native row keeps to the bottom centre");
        assertEquals(HudPlacement.Point9.BOTTOM, frames.get(9).point(), "the off hand, 2 px from the row, goes with it");
        assertEquals(HudPlacement.Point9.BOTTOM_LEFT, frames.get(10).point(), "the two rows at the left keep to the lower left");
        for (int width : new int[]{800, 640, 600, 534, 491, 427, 320}) {
            int height = heightFor(width);
            var result = HudPlacement.resolve(frames, width, height, Map.of());
            assertNoOverlap(result, width, height);
            assertEquals(4, result.x()[10], "the left rows stay 4 px from the left edge at " + width);
            assertEquals(height - 72, result.y()[10]);
            assertEquals(26, result.x()[11] - result.x()[10], "gaps inside a group never change");
            assertEquals(width / 2 - 110, result.x()[0], "the native row stays centred under vanilla's bars");
            assertEquals(height - 21, result.y()[0]);
        }
    }

    @Test void eachGroupTakesThePointOfItsThird() {
        var points = List.of(new HudArrangement.Point(4, 4), new HudArrangement.Point(310, 4), new HudArrangement.Point(616, 4),
                new HudArrangement.Point(4, 160), new HudArrangement.Point(310, 160), new HudArrangement.Point(616, 160),
                new HudArrangement.Point(4, 320), new HudArrangement.Point(310, 320), new HudArrangement.Point(616, 320));
        var frames = place(points, BASE_W, BASE_H);
        var expected = HudPlacement.Point9.values();
        for (int i = 0; i < 9; i++) assertEquals(expected[i], frames.get(i).point(), "frame " + i);
        // Stored offsets reproduce the place they were taken from on that screen.
        var result = HudPlacement.resolve(frames, BASE_W, BASE_H, Map.of());
        for (int i = 0; i < 9; i++) { assertEquals(points.get(i).x(), result.x()[i]); assertEquals(points.get(i).y(), result.y()[i]); }
    }

    @Test void whatDoesNotFitIsShownAsItIs() {
        // 1.1.4 (2026-11-01): a group past the edge is not pushed back in, and a frame over
        // another is not moved away; the player sorts it out.
        var frames = new ArrayList<HudPlacement.Frame>();
        for (int i = 0; i < 4; i++) frames.add(new HudPlacement.Frame(HudPlacement.Point9.BOTTOM, -200 + 26 * i, -60));
        var result = HudPlacement.resolve(frames, 320, 240, Map.of());
        for (int i = 0; i < 4; i++) { assertEquals(160 - 200 + 26 * i, result.x()[i]); assertEquals(180, result.y()[i]); }
        var nativeFrame = new HudPlacement.Frame(HudPlacement.Point9.BOTTOM, -110, -21);
        var corner = new HudPlacement.Frame(HudPlacement.Point9.BOTTOM_RIGHT, -260, -21);
        var both = HudPlacement.resolve(List.of(nativeFrame, corner), 320, 240, Map.of());
        assertEquals(60, both.x()[1], "the corner frame keeps its distance to its corner, over the native row");
        assertEquals(219, both.y()[1]);
    }

    @Test void aFramePutNextToAGroupJoinsItAndNothingElseIsStored() {
        var stored = place(List.of(new HudArrangement.Point(4, 300), new HudArrangement.Point(30, 300),
                new HudArrangement.Point(600, 300), new HudArrangement.Point(616, 4)), BASE_W, BASE_H);
        // A new frame 6 px right of the left pair takes its point; the pair and the others are not stored again (1.1.6).
        var points = new ArrayList<>(positions(HudPlacement.resolve(stored, BASE_W, BASE_H, Map.of())));
        points.add(new HudArrangement.Point(56, 300));
        var previous = new ArrayList<>(stored); previous.add(new HudPlacement.Frame(null, 0, 0));
        var frames = HudPlacement.commit(points, points, previous, Set.of(4), HudPlacement.Edit.DROP, BASE_W, BASE_H);
        assertEquals(Set.of(4), frames.keySet());
        assertEquals(HudPlacement.Point9.BOTTOM_LEFT, frames.get(4).point());
        assertEquals(56, frames.get(4).dx());
        // Out in the open it is a group of its own, on the point of the third it is in.
        points.set(4, new HudArrangement.Point(300, 150));
        assertEquals(HudPlacement.Point9.CENTER, HudPlacement.commit(points, points, previous, Set.of(4), HudPlacement.Edit.DROP,
                BASE_W, BASE_H).get(4).point());
    }

    /** Regression case (2026-11-01): the native row from (534,325) and a 20-slot row from (800,325), laid out 1280x346. */
    private static List<HudArrangement.Point> nativeAndLongRow() {
        var points = new ArrayList<HudArrangement.Point>();
        for (int i = 0; i < 9; i++) points.add(new HudArrangement.Point(534 + 24 * i, 325));
        for (int i = 0; i < 20; i++) points.add(new HudArrangement.Point(800 + 24 * i, 325));
        return points;
    }

    @Test void aNudgeInASmallWindowKeepsWhatItDidNotMove() {
        // In 320x240 the long row lies over the native row; nudged up 1 there, it is x 800 again in the big window and one
        // pixel higher, and the native row it lay over is left alone (it used to take the native row's point: x 324).
        var stored = place(nativeAndLongRow(), 1280, 346);
        assertEquals(HudPlacement.Point9.BOTTOM_RIGHT, stored.get(9).point());
        var small = positions(HudPlacement.resolve(stored, 320, 240, Map.of()));
        var row = IntStream.range(9, 29).boxed().collect(Collectors.toSet());
        var after = new ArrayList<>(small);
        for (int i : row) after.set(i, new HudArrangement.Point(small.get(i).x(), small.get(i).y() - 1));
        var changes = HudPlacement.commit(after, small, stored, row, HudPlacement.Edit.NUDGE, 320, 240);
        assertEquals(row, changes.keySet(), "the native row is not drawn in");
        var back = HudPlacement.resolve(applied(stored, changes), 1280, 346, Map.of());
        assertEquals(800, back.x()[9], "x as laid out");
        assertEquals(324, back.y()[9], "one pixel up");
        assertEquals(534, back.x()[0]); assertEquals(325, back.y()[0]);
    }

    @Test void anEditThatMovedNothingChangesNothing() {
        // An arrow key toward the edge the row is already past leaves it where it is; the editor then stores nothing, and
        // a record of it would change nothing either.
        var stored = place(nativeAndLongRow(), 1280, 346);
        var small = positions(HudPlacement.resolve(stored, 320, 240, Map.of()));
        var rowPoints = small.subList(9, 29);
        assertEquals(rowPoints, HudArrangement.translate(rowPoints, -1, 0, 320, 240));
        var row = IntStream.range(9, 29).boxed().collect(Collectors.toSet());
        var changes = HudPlacement.commit(small, small, stored, row, HudPlacement.Edit.NUDGE, 320, 240);
        for (int i : row) assertEquals(stored.get(i), changes.get(i), "frame " + i);
    }

    /** Moves the frames in {@code ids} on a 320x240 screen as the editor's arrow keys do, and records it. */
    private static List<HudPlacement.Frame> nudge(List<HudPlacement.Frame> stored, List<HudArrangement.Point> shown,
                                                  Set<Integer> ids, int dx, int dy) {
        var sorted = ids.stream().sorted().toList();
        var moved = HudArrangement.translate(sorted.stream().map(shown::get).toList(), dx, dy, 320, 240);
        var after = new ArrayList<>(shown);
        for (int k = 0; k < sorted.size(); k++) after.set(sorted.get(k), moved.get(k));
        return applied(stored, HudPlacement.commit(after, shown, stored, ids, HudPlacement.Edit.NUDGE, 320, 240));
    }

    @Test void aRowPastTheEdgeOfASmallWindowMovesThereAsUsual() {
        // Regression in 1.1.4 (2026-11-01): the native row from (534,325) and four slots from (440,280), laid out
        // 1280x346, the four keeping to the bottom centre. In 320x240 they start at x -40, past the left edge.
        var points = new ArrayList<HudArrangement.Point>();
        for (int i = 0; i < 9; i++) points.add(new HudArrangement.Point(534 + 24 * i, 325));
        for (int i = 0; i < 4; i++) points.add(new HudArrangement.Point(440 + 24 * i, 280));
        var stored = place(points, 1280, 346);
        assertEquals(HudPlacement.Point9.BOTTOM, stored.get(9).point());
        var small = positions(HudPlacement.resolve(stored, 320, 240, Map.of()));
        assertEquals(new HudArrangement.Point(-40, 174), small.get(9));
        var row = IntStream.range(9, 13).boxed().collect(Collectors.toSet());
        // Up only: back in the big window it is where it was laid out across, one pixel higher (it went to x 0).
        var back = HudPlacement.resolve(nudge(stored, small, row, 0, -1), 1280, 346, Map.of());
        assertEquals(440, back.x()[9]); assertEquals(279, back.y()[9]);
        // Right, one back in: still a bottom-centre row, one further right in the big window too (1.1.5; it took the
        // small window's left point and landed at x -39).
        var right = nudge(stored, small, row, 1, 0);
        assertEquals(HudPlacement.Point9.BOTTOM, right.get(9).point());
        back = HudPlacement.resolve(right, 1280, 346, Map.of());
        assertEquals(441, back.x()[9]); assertEquals(280, back.y()[9]);
        assertEquals(534, back.x()[0], "the native row is left alone");
    }

    @Test void groupsAWindowPutTogetherStayApartUntilThePlayerJoinsThem() {
        var points = List.of(new HudArrangement.Point(4, 300), new HudArrangement.Point(30, 300),
                new HudArrangement.Point(590, 300), new HudArrangement.Point(616, 300));
        var stored = place(points, BASE_W, BASE_H);
        assertEquals(HudPlacement.Point9.BOTTOM_LEFT, stored.get(0).point());
        assertEquals(HudPlacement.Point9.BOTTOM_RIGHT, stored.get(2).point());
        // 60 wide, the right group lies over the left one; nudging the left one does not make them one group.
        var narrow = positions(HudPlacement.resolve(stored, 60, 240, Map.of()));
        var after = new ArrayList<>(narrow);
        for (int i : new int[]{0, 1}) after.set(i, new HudArrangement.Point(narrow.get(i).x(), narrow.get(i).y() - 2));
        var changes = HudPlacement.commit(after, narrow, stored, Set.of(0, 1), HudPlacement.Edit.NUDGE, 60, 240);
        assertEquals(Set.of(0, 1), changes.keySet(), "the right group is not drawn in");
        assertEquals(HudPlacement.Point9.BOTTOM_LEFT, changes.get(0).point());
        // Dragged from the right group to 6 px beside the left one, a frame joins the group it was put against and takes
        // its point; that group and the rest of the right one are not stored again.
        var big = positions(HudPlacement.resolve(stored, BASE_W, BASE_H, Map.of()));
        var dragged = new ArrayList<>(big);
        dragged.set(2, new HudArrangement.Point(56, 300));
        var joined = HudPlacement.commit(dragged, big, stored, Set.of(2), HudPlacement.Edit.DROP, BASE_W, BASE_H);
        assertEquals(Set.of(2), joined.keySet());
        assertEquals(stored.get(0).point(), joined.get(2).point());
    }

    @Test void anAxisNobodyMovedKeepsItsPoint() {
        // A right-hand pair in a window so narrow its centre falls in the middle third: moved only up, it stays a right
        // pair, back where it was laid out across.
        var points = List.of(new HudArrangement.Point(560, 300), new HudArrangement.Point(586, 300));
        var stored = place(points, BASE_W, BASE_H);
        assertEquals(HudPlacement.Point9.BOTTOM_RIGHT, stored.getFirst().point());
        var narrow = positions(HudPlacement.resolve(stored, 120, 240, Map.of()));
        var after = narrow.stream().map(p -> new HudArrangement.Point(p.x(), p.y() - 5)).toList();
        var changes = HudPlacement.commit(after, narrow, stored, Set.of(0, 1), HudPlacement.Edit.DROP, 120, 240);
        assertEquals(HudPlacement.Point9.BOTTOM_RIGHT, changes.get(0).point());
        var back = HudPlacement.resolve(applied(stored, changes), BASE_W, BASE_H, Map.of());
        assertEquals(560, back.x()[0]); assertEquals(295, back.y()[0]);
    }

    @Test void aGroupLongerThanTheScreenKeepsItsSideWhenMovedTheOtherWay() {
        // Laid out at the left of the 640-wide window; 320 wide the row runs past the right edge. Moved up there (the
        // editor allows only that), it stays a left group with its offset.
        var points = new ArrayList<HudArrangement.Point>();
        for (int i = 0; i < 15; i++) points.add(new HudArrangement.Point(4 + 24 * i, 300));
        var stored = place(points, BASE_W, BASE_H);
        assertEquals(HudPlacement.Point9.BOTTOM_LEFT, stored.getFirst().point());
        var small = positions(HudPlacement.resolve(stored, 320, 240, Map.of()));
        assertEquals(4, small.getFirst().x(), "shown past the right edge, not pulled in");
        var moved = small.stream().map(p -> new HudArrangement.Point(p.x(), p.y() - 30)).toList();
        var changes = HudPlacement.commit(moved, small, stored, all(15), HudPlacement.Edit.DROP, 320, 240);
        assertEquals(HudPlacement.Point9.BOTTOM_LEFT, changes.get(0).point(), "still a left group");
        assertEquals(4, changes.get(0).dx(), "with the offset it had");
        assertEquals(24, changes.get(1).dx() - changes.get(0).dx(), "and its gaps");
        var back = HudPlacement.resolve(applied(stored, changes), BASE_W, BASE_H, Map.of());
        assertEquals(4, back.x()[0], "back at the left edge of the big window");
        assertEquals(270, back.y()[0], "and the move up kept");
    }

    /** Regression in 1.1.5 (2026-11-01): the native row from (534,325), A five from (182,280), B two from (1180,280), 1280x346. */
    private static List<HudPlacement.Frame> nativeAndTwoRows() {
        var points = new ArrayList<HudArrangement.Point>();
        for (int i = 0; i < 9; i++) points.add(new HudArrangement.Point(534 + 24 * i, 325));
        for (int i = 0; i < 5; i++) points.add(new HudArrangement.Point(182 + 24 * i, 280));
        for (int i = 0; i < 2; i++) points.add(new HudArrangement.Point(1180 + 24 * i, 280));
        return place(points, 1280, 346);
    }
    private static final Set<Integer> ROW_A = Set.of(9, 10, 11, 12, 13);

    @Test void aRowNudgedOverAnotherGroupInASmallWindowKeepsItsPoint() {
        // In 320x240 A (lower left) lies over B (lower right). Shift+Left moves it 10 and one more pair of frames touches;
        // arrow keys never join groups (1.1.6), so A keeps its point and is 10 further left in the big window too (it was
        // joined to B and landed at x 1132).
        var stored = nativeAndTwoRows();
        assertEquals(HudPlacement.Point9.BOTTOM_LEFT, stored.get(9).point());
        assertEquals(HudPlacement.Point9.BOTTOM_RIGHT, stored.get(14).point());
        var small = positions(HudPlacement.resolve(stored, 320, 240, Map.of()));
        assertEquals(new HudArrangement.Point(220, 174), small.get(14), "B over A");
        var after = nudge(stored, small, ROW_A, -10, 0);
        assertEquals(HudPlacement.Point9.BOTTOM_LEFT, after.get(9).point());
        var back = HudPlacement.resolve(after, 1280, 346, Map.of());
        assertEquals(172, back.x()[9]); assertEquals(1180, back.x()[14]); assertEquals(534, back.x()[0]);
    }

    @Test void aRowDroppedOnAnotherGroupJoinsIt() {
        // The same 10 to the left with the mouse: A is put down on B, so it joins B and takes its point, and B is not stored
        // again. In the big window A lies over B as it did when it was put down ().
        var stored = nativeAndTwoRows();
        var small = positions(HudPlacement.resolve(stored, 320, 240, Map.of()));
        var after = new ArrayList<>(small);
        for (int i : ROW_A) after.set(i, new HudArrangement.Point(small.get(i).x() - 10, small.get(i).y()));
        var changes = HudPlacement.commit(after, small, stored, ROW_A, HudPlacement.Edit.DROP, 320, 240);
        assertEquals(ROW_A, changes.keySet());
        assertEquals(HudPlacement.Point9.BOTTOM_RIGHT, changes.get(9).point());
        var back = HudPlacement.resolve(applied(stored, changes), 1280, 346, Map.of());
        assertEquals(1132, back.x()[9]); assertEquals(1180, back.x()[14]);
    }

    @Test void draggingEverythingInASmallWindowJoinsNothing() {
        // All selected and dragged up 2 in 320x240, where A and B lie over each other: frames dragged together keep their
        // groups, and no frame that stayed is there to land on.
        var stored = nativeAndTwoRows();
        var small = positions(HudPlacement.resolve(stored, 320, 240, Map.of()));
        var after = small.stream().map(p -> new HudArrangement.Point(p.x(), p.y() - 2)).toList();
        var changes = HudPlacement.commit(after, small, stored, all(16), HudPlacement.Edit.DROP, 320, 240);
        for (int i = 0; i < 16; i++) assertEquals(stored.get(i).point(), changes.get(i).point(), "frame " + i);
        var back = HudPlacement.resolve(applied(stored, changes), 1280, 346, Map.of());
        assertEquals(182, back.x()[9]); assertEquals(278, back.y()[9]); assertEquals(1180, back.x()[14]);
    }

    @Test void aFramePutDownBetweenGroupsJoinsTheNearestOrTheOneOnTop() {
        // Two pairs 32 apart in 320x240, the left on the lower-left point, the right on the lower-right one; then a new frame.
        var stored = List.of(new HudPlacement.Frame(HudPlacement.Point9.BOTTOM_LEFT, 100, -40),
                new HudPlacement.Frame(HudPlacement.Point9.BOTTOM_LEFT, 124, -40),
                new HudPlacement.Frame(HudPlacement.Point9.BOTTOM_RIGHT, -144, -40),
                new HudPlacement.Frame(HudPlacement.Point9.BOTTOM_RIGHT, -120, -40),
                new HudPlacement.Frame(null, 0, 0));
        var points = new ArrayList<>(positions(HudPlacement.resolve(stored.subList(0, 4), 320, 240, Map.of())));
        points.add(new HudArrangement.Point(148, 200));   // 4 from the left pair, 8 from the right one
        assertEquals(HudPlacement.Point9.BOTTOM_LEFT,
                HudPlacement.commit(points, points, stored, Set.of(4), HudPlacement.Edit.DROP, 320, 240).get(4).point());
        // A small window lays the right pair over the left one; a frame put down on both joins the one drawn last.
        var squeezed = List.of(new HudArrangement.Point(100, 200), new HudArrangement.Point(124, 200),
                new HudArrangement.Point(120, 200), new HudArrangement.Point(144, 200), new HudArrangement.Point(130, 200));
        assertEquals(HudPlacement.Point9.BOTTOM_RIGHT,
                HudPlacement.commit(squeezed, squeezed, stored, Set.of(4), HudPlacement.Edit.DROP, 320, 240).get(4).point());
    }

    @Test void framesLinedUpBecomeOneGroup() {
        // The two pairs of a small window lined up by the toolbar: one group, on the point of the third the line is in.
        var stored = List.of(new HudPlacement.Frame(HudPlacement.Point9.BOTTOM_LEFT, 100, -40),
                new HudPlacement.Frame(HudPlacement.Point9.BOTTOM_LEFT, 124, -40),
                new HudPlacement.Frame(HudPlacement.Point9.BOTTOM_RIGHT, -200, -60),
                new HudPlacement.Frame(HudPlacement.Point9.BOTTOM_RIGHT, -176, -60));
        var before = positions(HudPlacement.resolve(stored, 320, 240, Map.of()));
        var line = List.of(new HudArrangement.Point(100, 200), new HudArrangement.Point(124, 200),
                new HudArrangement.Point(148, 200), new HudArrangement.Point(172, 200));
        var changes = HudPlacement.commit(line, before, stored, all(4), HudPlacement.Edit.ARRANGE, 320, 240);
        for (int i = 0; i < 4; i++) assertEquals(HudPlacement.Point9.BOTTOM, changes.get(i).point(), "frame " + i);
        // Put there by a drag instead, the two pairs would keep their own points.
        var dragged = HudPlacement.commit(line, before, stored, all(4), HudPlacement.Edit.DROP, 320, 240);
        assertNotEquals(dragged.get(0).point(), dragged.get(2).point());
    }

    @Test void partsOfTwoGroupsDraggedToTheTopFollowTheTopCorners() {
        // 2026-11-01, user: three of a lower-left group and three of a lower-right one dragged together to the top. Each part
        // is a group of its own on the top corner of its side; the frames not dragged stay as they were.
        var points = new ArrayList<HudArrangement.Point>();
        for (int i = 0; i < 5; i++) points.add(new HudArrangement.Point(4 + 24 * i, 300));
        for (int i = 0; i < 5; i++) points.add(new HudArrangement.Point(520 + 24 * i, 300));
        var stored = place(points, BASE_W, BASE_H);
        assertEquals(HudPlacement.Point9.BOTTOM_LEFT, stored.get(0).point());
        assertEquals(HudPlacement.Point9.BOTTOM_RIGHT, stored.get(9).point());
        var dragged = Set.of(0, 1, 2, 7, 8, 9);
        var after = new ArrayList<>(points);
        for (int i : dragged) after.set(i, new HudArrangement.Point(points.get(i).x(), 4));
        var changes = HudPlacement.commit(after, points, stored, dragged, HudPlacement.Edit.DROP, BASE_W, BASE_H);
        assertEquals(dragged, changes.keySet());
        assertEquals(HudPlacement.Point9.TOP_LEFT, changes.get(0).point());
        assertEquals(HudPlacement.Point9.TOP_RIGHT, changes.get(9).point());
        var small = HudPlacement.resolve(applied(stored, changes), 320, 240, Map.of());
        assertEquals(4, small.x()[0]); assertEquals(4, small.y()[0]);
        assertEquals(248, small.x()[7]); assertEquals(4, small.y()[7]);
        assertEquals(76, small.x()[3]); assertEquals(194, small.y()[3], "the rest of the left group stays at the lower left");
    }

    /** {@code count} new frames in a row from {@code x0}, {@code pitch} apart, at {@code y}, put down together on width x height. */
    private static List<HudPlacement.Frame> row(int count, int x0, int pitch, int y, int width, int height) {
        var points = IntStream.range(0, count).mapToObj(i -> new HudArrangement.Point(x0 + pitch * i, y)).toList();
        return place(points, width, height);
    }

    @Test void aLongRowPutAgainstAnEdgeFollowsThatEdge() {
        // 1.2.0 (2026-11-01): 17 slots from 4 px off the left edge, 436 wide, their centre in the
        // middle third; put down against the left edge they keep to it, and 480 wide they still start 4 px in (following
        // the middle they started at -76).
        var stored = row(17, 4, 26, 300, BASE_W, BASE_H);
        assertEquals(HudPlacement.Point9.BOTTOM_LEFT, stored.getFirst().point());
        var narrow = HudPlacement.resolve(stored, 480, 270, Map.of());
        assertEquals(4, narrow.x()[0]); assertEquals(4 + 26 * 16, narrow.x()[16]);
        // The same against the top edge, and a column against the right one.
        assertEquals(HudPlacement.Point9.TOP_LEFT, row(17, 4, 26, 3, BASE_W, BASE_H).getFirst().point());
        var column = place(IntStream.range(0, 12).mapToObj(i -> new HudArrangement.Point(616, 30 + 24 * i)).toList(), BASE_W, BASE_H);
        assertEquals(HudPlacement.Point9.RIGHT, column.getFirst().point(), "against the right edge, its middle in the middle third");
    }

    @Test void aRowAgainstBothEdgesOrPastOneKeepsToItsThird() {
        // As wide as the screen, 2 px from both edges: its third, the middle.
        assertEquals(HudPlacement.Point9.BOTTOM, row(29, 2, 22, 300, BASE_W, BASE_H).getFirst().point());
        // Past the left edge it is not against it: 19 from x -30 have their centre in the middle third and follow the middle.
        assertEquals(HudPlacement.Point9.BOTTOM, row(19, -30, 26, 300, BASE_W, BASE_H).getFirst().point());
        // Not quite against it, 9 px in: the third decides.
        assertEquals(HudPlacement.Point9.BOTTOM, row(17, 9, 26, 300, BASE_W, BASE_H).getFirst().point());
    }

    @Test void arrowKeysNeverMakeAGroupFollowAnEdge() {
        // A middle row shown 2 px from the left edge of a small window and nudged up keeps its point (1.1.5): the edge
        // rule is for groups put down there.
        var stored = new ArrayList<HudPlacement.Frame>();
        for (int i = 0; i < 9; i++) stored.add(new HudPlacement.Frame(HudPlacement.Point9.BOTTOM, -158 + 24 * i, -60));
        var small = positions(HudPlacement.resolve(stored, 320, 240, Map.of()));
        assertEquals(2, small.getFirst().x());
        var after = nudge(stored, small, IntStream.range(0, 9).boxed().collect(Collectors.toSet()), 0, -1);
        assertEquals(HudPlacement.Point9.BOTTOM, after.getFirst().point());
        // Dragged there instead, it follows the left edge.
        var dragged = small.stream().map(p -> new HudArrangement.Point(p.x(), p.y() - 1)).toList();
        var changes = HudPlacement.commit(dragged, small, stored, all(9), HudPlacement.Edit.DROP, 320, 240);
        assertEquals(HudPlacement.Point9.BOTTOM_LEFT, changes.get(0).point());
    }

    @Test void aGroupDraggedOffTheEdgeItFollowedLetsGoOfIt() {
        // review of 1.2.0 (2026-11-01): a row put against the left edge follows it; dragged to the middle it kept
        // that point, its box still in the middle third, so 480 wide it stayed 102 px from the left edge and ran 58 px past
        // the right one. Taken off the edge it was against, the third decides again.
        var stored = row(17, 4, 26, 300, BASE_W, BASE_H);
        assertEquals(HudPlacement.Point9.BOTTOM_LEFT, stored.getFirst().point());
        var shown = positions(HudPlacement.resolve(stored, BASE_W, BASE_H, Map.of()));
        var centred = shown.stream().map(p -> new HudArrangement.Point(p.x() + 98, p.y())).toList();
        var changes = HudPlacement.commit(centred, shown, stored, all(17), HudPlacement.Edit.DROP, BASE_W, BASE_H);
        assertEquals(HudPlacement.Point9.BOTTOM, changes.get(0).point());
        var dropped = IntStream.range(0, 17).mapToObj(changes::get).toList();
        assertEquals(22, HudPlacement.resolve(dropped, 480, 270, Map.of()).x()[0], "centred in the narrower window too");
        // Moved off it by the arrow keys instead, it keeps the left edge (1.1.5).
        var right10 = shown.stream().map(p -> new HudArrangement.Point(p.x() + 10, p.y())).toList();
        var nudged = HudPlacement.commit(right10, shown, stored, all(17), HudPlacement.Edit.NUDGE, BASE_W, BASE_H);
        assertEquals(HudPlacement.Point9.BOTTOM_LEFT, nudged.get(0).point());
    }

    @Test void aRowAgainstOneEdgeFollowsItThoughItRunsPastTheOther() {
        // review of 1.2.0: only the side against the edge counts (). 17 slots from x 4 in a
        // 320-wide window run 120 px past the right edge and still follow the left one.
        assertEquals(HudPlacement.Point9.BOTTOM_LEFT, row(17, 4, 26, 180, 320, 240).getFirst().point());
    }

    @Test void heldFramesAreDrawnWhereTheyAreHeld() {
        var frames = List.of(new HudPlacement.Frame(HudPlacement.Point9.BOTTOM, -10, -22),
                new HudPlacement.Frame(HudPlacement.Point9.BOTTOM, 16, -22));
        var result = HudPlacement.resolve(frames, 320, 240, Map.of(1, new HudArrangement.Point(150, 218)));
        assertEquals(150, result.x()[1]); assertEquals(218, result.y()[1]);
        assertEquals(150, result.x()[0], "a dragged frame over another moves nothing");
        assertEquals(218, result.y()[0]);
    }

    @Test void smallControlsTakeTheFirstFreeCandidate() {
        var avoid = List.of(HudPlacement.Rect.cell(300, 10));
        var candidates = List.of(new HudPlacement.Rect(290, 6, 30, 18), new HudPlacement.Rect(6, 6, 30, 18), new HudPlacement.Rect(6, 30, 30, 18));
        assertEquals(candidates.get(1), HudPlacement.firstFree(candidates, avoid, 0, 320, 240));
        assertNull(HudPlacement.firstFree(List.of(candidates.getFirst()), avoid, 0, 320, 240));
        assertEquals(candidates.getFirst(), HudPlacement.bestSpot(List.of(candidates.getFirst()), avoid, 0, 320, 240));
        var offScreen = new HudPlacement.Rect(310, 6, 30, 18);
        assertEquals(candidates.get(1), HudPlacement.firstFree(List.of(offScreen, candidates.get(1)), List.of(), 0, 320, 240));
    }
}
