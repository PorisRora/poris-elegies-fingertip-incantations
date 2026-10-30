package com.poris.elegies.fingertip.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/**
 * A targeted event for listeners which use registered KeyMappings instead of consumeClick().
 * Never broadcast the target's physical key: unrelated mods can share it. The private sentinel
 * matches exactly one mapping, only while dispatching, including listeners comparing getKey().
 * The saved/physical key is unchanged. The sentinel is never sent to GLFW or KeyboardHandler.
 * Code which polls physical hardware or compares hard-coded key codes needs an explicit adapter.
 */
public final class RawKeyRelay {
    private static final int VIRTUAL = -2;
    private record Context(KeyMapping target, KeyModifier modifier, boolean dispatching) {}
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

    public static boolean active() { return CURRENT.get() != null; }
    public static boolean keyboardEvent(int key, int scan) { return active() && key == VIRTUAL && scan == VIRTUAL; }
    public static boolean mouseEvent(int button) { return active() && button == VIRTUAL; }
    public static boolean virtualKey(InputConstants.Key key) { return active() && key.getValue() == VIRTUAL; }
    public static boolean targets(Object mapping) { return active() && CURRENT.get().target() == mapping; }
    public static boolean modifier(KeyModifier modifier) { return active() && CURRENT.get().modifier() == modifier; }

    /** Only the active event target exposes the sentinel; holding a lease alone never remaps it. */
    public static InputConstants.Key eventKey(Object mapping, InputConstants.Key physical) {
        Context context = CURRENT.get();
        return context != null && context.dispatching() && context.target() == mapping
                ? physical.getType().getOrCreate(VIRTUAL) : physical;
    }

    public record Stroke(KeyMapping target, KeyModifier modifier, boolean mouse) {
        public void run(Runnable action) { run(action, false); }
        private void run(Runnable action, boolean dispatching) {
            Context previous = CURRENT.get();
            CURRENT.set(new Context(target, modifier, dispatching));
            try { action.run(); }
            finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
        }
        public void send(int action) {
            run(() -> {
                int modifiers = switch (modifier) {
                    case SHIFT -> GLFW.GLFW_MOD_SHIFT;
                    case CONTROL -> GLFW.GLFW_MOD_CONTROL;
                    case ALT -> GLFW.GLFW_MOD_ALT;
                    case NONE -> 0;
                };
                if (mouse) {
                    var event = NeoForge.EVENT_BUS.post(new InputEvent.MouseButton.Pre(VIRTUAL, action, modifiers));
                    if (!event.isCanceled()) NeoForge.EVENT_BUS.post(new InputEvent.MouseButton.Post(VIRTUAL, action, modifiers));
                } else NeoForge.EVENT_BUS.post(new InputEvent.Key(VIRTUAL, VIRTUAL, action, modifiers));
            }, true);
        }
    }
    public static Stroke stroke(KeyMapping target) {
        return new Stroke(target, target.getKeyModifier(), target.getKey().getType() == InputConstants.Type.MOUSE);
    }
    private RawKeyRelay() {}
}
