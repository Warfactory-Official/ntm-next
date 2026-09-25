// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toserver;

import com.hbm.entity.train.EntityRailCarBase;
import com.hbm.extprop.HbmPlayerProps;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.items.IKeybindReceiver;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import com.hbm.packet.toclient.PlayerInformPayload;
import com.hbm.platform.Services;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class KeybindPayload extends ThreadedPayload {

    public static final Type<KeybindPayload> TYPE = new Type<>(Library.id("keybind"));
    public static final StreamCodec<ByteBuf, KeybindPayload> STREAM_CODEC =
            streamCodec(KeybindPayload::decode);

    private static final int ID_HUD = 7;
    private static final int ID_MAGNET = 6;
    private static final int ID_JETPACK = 5;

    private final int key;
    private final boolean pressed;

    public KeybindPayload(EnumKeybind key, boolean pressed) {
        this(key.ordinal(), pressed);
    }

    private KeybindPayload(int key, boolean pressed) {
        this.key = key;
        this.pressed = pressed;
    }

    private static KeybindPayload decode(ByteBuf buf) {
        return new KeybindPayload(
                ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.BOOL.decode(buf));
    }

    public static void handleServer(KeybindPayload payload, IPayloadHandlerContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        if (payload.key < 0 || payload.key >= EnumKeybind.VALUES.length) return;
        EnumKeybind key = EnumKeybind.VALUES[payload.key];

        HbmPlayerProps props = HbmPlayerProps.getData(player);

        if (payload.pressed && !props.getKeyPressed(key) && key == EnumKeybind.TOGGLE_HEAD) {
            props.enableHUD = !props.enableHUD;
            Services.NETWORK.sendTo(
                    new PlayerInformPayload(
                            toggled(props.enableHUD, "chat.toggle.hudOn", "chat.toggle.hudOff"),
                            ID_HUD,
                            1_000),
                    player);
        }
        if (payload.pressed && !props.getKeyPressed(key) && key == EnumKeybind.TOGGLE_JETPACK) {
            props.enableBackpack = !props.enableBackpack;
            Services.NETWORK.sendTo(
                    new PlayerInformPayload(
                            toggled(
                                    props.enableBackpack,
                                    "chat.toggle.jetpackOn",
                                    "chat.toggle.jetpackOff"),
                            ID_JETPACK,
                            1_000),
                    player);
        }
        if (payload.pressed && !props.getKeyPressed(key) && key == EnumKeybind.TOGGLE_MAGNET) {
            props.enableMagnet = !props.enableMagnet;
            Services.NETWORK.sendTo(
                    new PlayerInformPayload(
                            toggled(
                                    props.enableMagnet,
                                    "chat.toggle.magnetOn",
                                    "chat.toggle.magnetOff"),
                            ID_MAGNET,
                            1_000),
                    player);
        }

        if (payload.pressed
                && !props.getKeyPressed(key)
                && key == EnumKeybind.TRAIN
                && player.getVehicle() instanceof EntityRailCarBase car
                && car instanceof MenuProvider menu) {
            player.openMenu(menu);
        }
        props.setKeyPressed(key, payload.pressed);

        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty() && held.getItem() instanceof IKeybindReceiver rec) {
            if (rec.canHandleKeybind(player, held, key))
                rec.handleKeybind(player, held, key, payload.pressed);
        }
    }

    private static Component toggled(boolean on, String onKey, String offKey) {
        return Component.translatable(on ? onKey : offKey)
                .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufCodecs.VAR_INT.encode(buf, key);
        ByteBufCodecs.BOOL.encode(buf, pressed);
    }

    @Override
    public @NotNull Type<KeybindPayload> type() {
        return TYPE;
    }
}
