package com.poris.elegies.fingertip.diagnostics;

import com.poris.elegies.fingertip.Fingertip;
import com.poris.elegies.fingertip.binding.HudBinding;
import com.poris.elegies.fingertip.hand.HandAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayDeque;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Read-only, bounded history per inventory and logical side. Never repairs or sends inventory packets.
 * <p>
 * Off unless the game starts with -Dfingertip.handTrace=true (0.2-beta, 2026-10-30): always on, it wrote up to 120 INFO
 * lines a minute per player and side, 68 in twelve minutes of the user's own game. Off, every entry point returns at
 * once, before any event text is built or the inventory is read.
 */
public final class HandDiagnostics {
    /** Read once: the JVM property fingertip.handTrace. */
    public static final boolean ENABLED = Boolean.getBoolean("fingertip.handTrace");
    private static final int HISTORY_LIMIT = 40, MAX_REPORTS = 6;
    private final ArrayDeque<String> history = new ArrayDeque<>();
    private int emptyTicks, reports;
    private int eventWindowTick, visibleEvents;
    private boolean failed;

    public static String stack(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()) + "x" + stack.getCount()
                + "@" + Integer.toHexString(System.identityHashCode(stack));
    }
    public static void event(Player player, String event, Supplier<String> details) {
        if (!ENABLED) return;
        var trace = HandAccess.of(player.getInventory()).diagnostics();
        if (trace.failed) return;
        try {
            String line = trace.header(player) + " event=" + event + " " + details.get() + " "
                    + HandAccess.of(player.getInventory()).diagnosticState(player.getInventory());
            if (trace.history.size() == HISTORY_LIMIT) trace.history.removeFirst();
            trace.history.addLast(line);
            if (player.tickCount < trace.eventWindowTick || player.tickCount - trace.eventWindowTick >= 1200) {
                trace.eventWindowTick = player.tickCount; trace.visibleEvents = 0;
            }
            // Launchers may filter DEBUG even in debug.log. A trace that was asked for stays at the default
            // level, bounded to 120 events per 1200 player ticks; the in-memory history continues.
            if (trace.visibleEvents++ < 120) Fingertip.LOGGER.info("[Fingertip hand] {}", line);
            else Fingertip.LOGGER.debug("[Fingertip hand] {}", line);
        } catch (RuntimeException error) { trace.fail(error); }
    }
    /** Only captures the caller when the selected source actually receives a different object. */
    public static void sourceWrite(Player player, int slot, ItemStack replacement) {
        if (!ENABLED || player.level().isClientSide) return;
        var hand = HandAccess.of(player.getInventory());
        var address = hand.address();
        if (!hand.extended() || address == null || !address.vanilla() || address.slot() != slot
                || player.getInventory().getItem(slot) == replacement) return;
        event(player, "source-write", () -> "slot=" + slot + " replacement=" + stack(replacement) + " caller="
                + StackWalker.getInstance().walk(frames -> frames
                    .filter(f -> !f.getClassName().startsWith(HandDiagnostics.class.getPackageName()))
                    .limit(7).map(StackWalker.StackFrame::toString).collect(Collectors.joining(" <- "))));
    }
    /** A symptom report, not a claim that the source was invalidated. No work on normal held frames. */
    public static void observeEmpty(Player player, int hud, HudBinding binding, boolean expected,
                                    Supplier<String> context) {
        if (!ENABLED) return;
        var hand = HandAccess.of(player.getInventory());
        var trace = hand.diagnostics();
        if (trace.failed) return;
        try {
            if (!expected || hud < 0 || !binding.isItem() || !hand.extended() || !hand.read(player.getInventory()).isEmpty()) {
                trace.emptyTicks = 0; return;
            }
            trace.emptyTicks++;
            // Acknowledge normal prediction/latency before reporting. At most once per 200 ticks,
            // and six history dumps per player inventory; no per-frame logging or disk files.
            if (trace.emptyTicks < 20 || (trace.emptyTicks - 20) % 200 != 0 || trace.reports >= MAX_REPORTS) return;
            var result = binding.findSource(player);
            if (!result.ready()) return;
            trace.reports++;
            event(player, "empty-with-matches", () -> "hud=" + hud + " binding=" + stack(binding.template())
                    + " next=" + result.address() + " emptyTicks=" + trace.emptyTicks
                    + " " + context.get());
            Fingertip.LOGGER.warn("[Fingertip hand] {} symptom=empty-with-matches report={} history:\n{}",
                    trace.header(player), trace.reports, String.join("\n", trace.history));
        } catch (RuntimeException error) { trace.fail(error); }
    }
    private String header(Player player) {
        return "side=" + (player.level().isClientSide ? "client" : "server") + " entity=" + player.getId()
                + " tick=" + player.tickCount + " gameTime=" + player.level().getGameTime();
    }
    private void fail(RuntimeException error) {
        failed = true; history.clear();
        Fingertip.LOGGER.warn("[Fingertip hand] Diagnostics disabled after a read failure; item handling unchanged", error);
    }
}
