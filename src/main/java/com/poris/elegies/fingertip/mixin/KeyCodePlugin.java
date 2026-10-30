package com.poris.elegies.fingertip.mixin;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

/**
 * Decorates the final getter after accessor generation. A normal return injector is prepared
 * against the old MethodNode and is lost when another mixin replaces it with an accessor.
 * This uses only bytecode names: loading the plugin on a dedicated server loads no client classes.
 */
public final class KeyCodePlugin implements IMixinConfigPlugin {
    private static final String KEY = "Lcom/mojang/blaze3d/platform/InputConstants$Key;";
    private static final String RELAY = "com/poris/elegies/fingertip/client/input/RawKeyRelay";
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String target, String mixin) { return true; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {
        if (!mixin.equals("com.poris.elegies.fingertip.mixin.client.KeyMappingMixin")) return;
        for (var method : node.methods) {
            if (!method.name.equals("getKey") || !method.desc.equals("()" + KEY)) continue;
            for (var instruction : method.instructions)
                if (instruction instanceof MethodInsnNode call && call.owner.equals(RELAY) && call.name.equals("eventKey")) return;
            int returns = 0;
            for (var instruction : method.instructions.toArray()) {
                if (instruction.getOpcode() != Opcodes.ARETURN) continue;
                var hook = new InsnList();
                hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
                hook.add(new InsnNode(Opcodes.SWAP));
                hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, RELAY, "eventKey", "(Ljava/lang/Object;" + KEY + ")" + KEY, false));
                method.instructions.insertBefore(instruction, hook);
                returns++;
            }
            if (returns == 0) throw new IllegalStateException("KeyMapping.getKey has no object return");
            method.maxStack = Math.max(method.maxStack, 2);
            return;
        }
        throw new IllegalStateException("KeyMapping.getKey not found after mixin accessors");
    }
}
