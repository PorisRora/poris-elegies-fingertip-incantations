package com.poris.elegies.fingertip.client.input;
import net.minecraft.client.KeyMapping;

import com.poris.elegies.fingertip.client.input.KeyState;
/** Scopes a targeted key lease. Fabric mods consume the mapping's click/held state; raw hardware input needs an adapter. */
public final class RawKeyRelay {
    private record Context(KeyMapping target, KeyModifier modifier) {}
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();
    public static boolean active() { return CURRENT.get() != null; }
    public static boolean targets(Object mapping) { return active() && CURRENT.get().target() == mapping; }
    public static boolean modifier(KeyModifier modifier) { return active() && CURRENT.get().modifier() == modifier; }
    public record Stroke(KeyMapping target, KeyModifier modifier) {
        public void run(Runnable action) {
            Context previous = CURRENT.get();
            CURRENT.set(new Context(target, modifier));
            try { action.run(); }
            finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
        }
    }
    public static Stroke stroke(KeyMapping target) { return new Stroke(target, KeyState.modifier(target)); }
    private RawKeyRelay() {}
}
