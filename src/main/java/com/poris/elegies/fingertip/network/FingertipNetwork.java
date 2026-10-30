package com.poris.elegies.fingertip.network;

import com.poris.elegies.fingertip.Fingertip;
import com.poris.elegies.fingertip.api.inventory.SourceAddress;
import com.poris.elegies.fingertip.binding.HudBinding;
import com.poris.elegies.fingertip.server.ActionRelay;
import com.poris.elegies.fingertip.server.BindingService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class FingertipNetwork {
    /**
     * Request operations. Protocol 2 (0.1.2-beta, 2026-10-29, user: "我们指尖的其他也能尽可能贴近原版吗") dropped CLEAR (vanilla's own
     * hotbar slot packet ends a HUD choice), RECONCILE (the server checks the hand as it handles vanilla's inventory
     * packets) and folded CONTROL and REAL_SLOT into SETTINGS, sent once on joining and on each change, as vanilla
     * sends its client information. The numbers left keep their values. Protocol 3 (0.1.5-beta) adds SWAP: slots hud and
     * source trade their bindings in one step.
     */
    public static final int SYNC = 0, BIND_ITEM = 1, SELECT = 2, BIND_ACTION = 4, UNBIND = 5, BIND_HOTBAR = 6, SETTINGS = 7, TOGGLE_SELECT = 9,
            SWAP = 10;
    /** SETTINGS flags in Request.source: the HUD takes over the hotbar, "use real slot", and the mode itself changed. */
    public static final int TAKEOVER = 1, REAL_SLOT = 2, MODE_CHANGED = 4;
    /** SELECT and TOGGLE_SELECT with this in Request.source carry the hand the client already predicted in address. */
    public static final int PREDICTED = 1;
    public static Consumer<Profile> profileReceiver = ignored -> {};
    public static Consumer<BindingUpdate> bindingReceiver = ignored -> {};
    public static Consumer<Hand> handReceiver = ignored -> {};

    public record Request(int sequence, int operation, int hud, int source, String action, ItemStack expected, SourceAddress address) implements CustomPacketPayload {
        public Request(int sequence, int operation, int hud, int source, String action, ItemStack expected) {
            this(sequence, operation, hud, source, action, expected,
                    operation == BIND_ITEM || operation == BIND_HOTBAR ? SourceAddress.legacy(source) : null);
        }
        public static final Type<Request> TYPE = new Type<>(Fingertip.id("request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = new StreamCodec<>() {
            public Request decode(RegistryFriendlyByteBuf b) {
                return new Request(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readUtf(256), ItemStack.OPTIONAL_STREAM_CODEC.decode(b), readAddress(b));
            }
            public void encode(RegistryFriendlyByteBuf b, Request v) {
                b.writeVarInt(v.sequence); b.writeVarInt(v.operation); b.writeVarInt(v.hud); b.writeVarInt(v.source);
                b.writeUtf(v.action, 256); ItemStack.OPTIONAL_STREAM_CODEC.encode(b, v.expected); writeAddress(b, v.address);
            }
        };
        public Type<Request> type() { return TYPE; }
    }
    public record Profile(List<HudBinding> bindings) implements CustomPacketPayload {
        public static final Type<Profile> TYPE = new Type<>(Fingertip.id("profile"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Profile> CODEC = new StreamCodec<>() {
            public Profile decode(RegistryFriendlyByteBuf b) {
                var entries = new ArrayList<HudBinding>(Fingertip.SLOT_COUNT);
                for (int i = 0; i < Fingertip.SLOT_COUNT; i++) {
                    var tag = b.readNbt();
                    entries.add(tag == null ? HudBinding.empty() : HudBinding.load(tag, b.registryAccess()));
                }
                return new Profile(List.copyOf(entries));
            }
            public void encode(RegistryFriendlyByteBuf b, Profile v) {
                for (int i = 0; i < Fingertip.SLOT_COUNT; i++) b.writeNbt(v.bindings.get(i).save(b.registryAccess()));
            }
        };
        public Type<Profile> type() { return TYPE; }
    }
    /** One HUD slot's binding, sent instead of the whole table when only that slot changed (protocol 2). */
    public record BindingUpdate(int slot, HudBinding binding) implements CustomPacketPayload {
        public static final Type<BindingUpdate> TYPE = new Type<>(Fingertip.id("binding"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BindingUpdate> CODEC = new StreamCodec<>() {
            public BindingUpdate decode(RegistryFriendlyByteBuf b) {
                int slot = b.readVarInt();
                var tag = b.readNbt();
                return new BindingUpdate(slot, tag == null ? HudBinding.empty() : HudBinding.load(tag, b.registryAccess()));
            }
            public void encode(RegistryFriendlyByteBuf b, BindingUpdate v) {
                b.writeVarInt(v.slot); b.writeNbt(v.binding.save(b.registryAccess()));
            }
        };
        public Type<BindingUpdate> type() { return TYPE; }
    }
    /**
     * The hand as the server holds it. It carries no vanilla slot number (protocol 2): that number is vanilla's own and
     * travels in vanilla's slot packets only, or a late reply would set it back over a newer vanilla choice.
     */
    public record Hand(int sequence, boolean controlled, SourceAddress address, int hud, boolean lost, String message) implements CustomPacketPayload {
        public static final Type<Hand> TYPE = new Type<>(Fingertip.id("hand"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Hand> CODEC = new StreamCodec<>() {
            public Hand decode(RegistryFriendlyByteBuf b) {
                return new Hand(b.readVarInt(), b.readBoolean(), readAddress(b), b.readVarInt(), b.readBoolean(), b.readUtf(256));
            }
            public void encode(RegistryFriendlyByteBuf b, Hand v) {
                b.writeVarInt(v.sequence); b.writeBoolean(v.controlled); writeAddress(b, v.address); b.writeVarInt(v.hud);
                b.writeBoolean(v.lost); b.writeUtf(v.message, 256);
            }
        };
        public Type<Hand> type() { return TYPE; }
    }
    /** A mod action's press or release relayed by FingertipApi.sendToServer (2026-10-28). */
    public record ActionInput(ResourceLocation action, boolean pressed) implements CustomPacketPayload {
        public static final Type<ActionInput> TYPE = new Type<>(Fingertip.id("action_input"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ActionInput> CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, ActionInput::action, ByteBufCodecs.BOOL, ActionInput::pressed, ActionInput::new);
        public Type<ActionInput> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        // Counted again from 1 at 0.1-beta (2026-10-28, user: "以当前版本为基线 定成1吧"); raise it whenever a packet changes.
        // 2 at 0.1.2-beta: SETTINGS, BindingUpdate, predicted selections. 3 at 0.1.5-beta: SWAP.
        var registrar = event.registrar("3");
        registrar.playToServer(Request.TYPE, Request.CODEC, (request, context) -> {
            if (context.player() instanceof ServerPlayer player) BindingService.request(player, request);
        });
        registrar.playToClient(Profile.TYPE, Profile.CODEC, (packet, context) -> profileReceiver.accept(packet));
        registrar.playToClient(BindingUpdate.TYPE, BindingUpdate.CODEC, (packet, context) -> bindingReceiver.accept(packet));
        registrar.playToClient(Hand.TYPE, Hand.CODEC, (packet, context) -> handReceiver.accept(packet));
        registrar.playToServer(ActionInput.TYPE, ActionInput.CODEC, (input, context) -> {
            if (context.player() instanceof ServerPlayer player) ActionRelay.dispatch(player, input.action(), input.pressed());
        });
    }
    private static SourceAddress readAddress(RegistryFriendlyByteBuf b) {
        return b.readBoolean() ? new SourceAddress(b.readUtf(256), b.readVarInt()) : null;
    }
    private static void writeAddress(RegistryFriendlyByteBuf b, SourceAddress address) {
        b.writeBoolean(address != null);
        if (address != null) { b.writeUtf(address.provider(), 256); b.writeVarInt(address.slot()); }
    }
    private FingertipNetwork() {}
}
