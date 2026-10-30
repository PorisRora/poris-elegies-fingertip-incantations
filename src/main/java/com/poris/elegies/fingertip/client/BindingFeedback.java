package com.poris.elegies.fingertip.client;

import com.poris.elegies.fingertip.Fingertip;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import java.util.HashMap;
import java.util.Map;

/** Binding feedback follows the server acknowledgement, independently of hand-selection replies. */
public final class BindingFeedback {
    public enum State { WAITING, SUCCESS, ERROR }
    public enum Operation { BIND, ADD, CLEAR, REMOVE, SWAP }
    public record Notice(int hud, State state, Component text, long time) {}
    /** {@code other} is the second slot of a swap, else -1. */
    private record Pending(int hud, int other, Operation operation) {}
    /** Replies that never came must not pile up: past this many waiting, all of them are forgotten. */
    private static final int MAX_PENDING = 64;
    private static final Map<Integer, Pending> pending = new HashMap<>();
    private static Notice latest;
    private static int latestSequence;
    public static void sent(int sequence, int hud, Operation operation) { sent(sequence, hud, -1, operation); }
    public static void sent(int sequence, int hud, int other, Operation operation) {
        if (pending.size() > MAX_PENDING) pending.clear();
        pending.put(sequence, new Pending(hud, other, operation));
        latestSequence = sequence;
        String key = switch (operation) {
            case BIND -> "poris_elegies_fingertip_incantations.binding_wait";
            case ADD -> "poris_elegies_fingertip_incantations.add_wait";
            case CLEAR -> "poris_elegies_fingertip_incantations.clear_wait";
            case REMOVE -> "poris_elegies_fingertip_incantations.remove_wait";
            case SWAP -> "poris_elegies_fingertip_incantations.swap_wait";
        };
        latest = new Notice(hud, State.WAITING, Component.translatable(key, hud + 1, other + 1), Util.getMillis());
    }
    public static void received(int sequence, String error) {
        Pending request = pending.remove(sequence);
        if (request == null || request.hud() < 0 || request.hud() >= Fingertip.SLOT_COUNT || sequence < latestSequence) return;
        int hud = request.hud();
        boolean success = error.isBlank();
        var binding = ClientSession.binding(hud);
        Component name = binding.isItem() ? binding.template().getHoverName() : switch (binding.kind()) {
            case MAPPING -> Component.translatable(binding.action());
            case ACTION -> actionName(binding.action());
            default -> Component.translatable("poris_elegies_fingertip_incantations.unbound");
        };
        Component done = switch (request.operation()) {
            case BIND -> Component.translatable("poris_elegies_fingertip_incantations.binding_done", hud + 1, name);
            case ADD -> Component.translatable("poris_elegies_fingertip_incantations.add_done", hud + 1);
            case CLEAR -> Component.translatable("poris_elegies_fingertip_incantations.clear_done", hud + 1);
            case REMOVE -> Component.translatable("poris_elegies_fingertip_incantations.remove_done", hud + 1);
            case SWAP -> Component.translatable("poris_elegies_fingertip_incantations.swap_done", hud + 1, request.other() + 1);
        };
        latest = new Notice(hud, success ? State.SUCCESS : State.ERROR,
                success ? done : Component.translatable(error), Util.getMillis());
        if (success && Minecraft.getInstance().screen != null)
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.35F, .3F));
    }
    public static void hint(int hud, String key) { hint(hud, Component.translatable(key)); }
    public static void hint(int hud, Component text) { latest = new Notice(hud, State.ERROR, text, Util.getMillis()); }
    public static Notice latest() {
        if (latest != null && Util.getMillis() - latest.time() > (latest.state() == State.WAITING ? 15000 : 3500)) latest = null;
        return latest;
    }
    private static Component actionName(String id) {
        try { return com.poris.elegies.fingertip.api.FingertipApi.find(id).map(a -> a.name()).orElse(Component.literal(id)); }
        catch (RuntimeException ignored) { return Component.literal(id); }
    }
    public static void reset() { pending.clear(); latest = null; latestSequence = 0; }
    private BindingFeedback() {}
}
