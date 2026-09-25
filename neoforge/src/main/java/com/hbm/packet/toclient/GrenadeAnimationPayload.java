// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.client.ClientPlayerAccess;
import com.hbm.items.weapon.grenade.ItemGrenadeShell;
import com.hbm.items.weapon.grenade.ItemGrenadeUniversal;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationKeyframe.IType;
import com.hbm.render.anim.BusAnimationSequence;
import com.hbm.render.anim.HbmAnimations.Animation;
import com.hbm.render.anim.HbmAnimations;
import com.hbm.util.GameTime;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class GrenadeAnimationPayload extends ThreadedPayload {

    public static final Type<GrenadeAnimationPayload> TYPE = new Type<>(Library.id("grenade_anim"));
    public static final StreamCodec<ByteBuf, GrenadeAnimationPayload> STREAM_CODEC =
            streamCodec(GrenadeAnimationPayload::decode);

    private final int shell;

    public GrenadeAnimationPayload(int shell) {
        this.shell = shell;
    }

    private static GrenadeAnimationPayload decode(ByteBuf buf) {
        return new GrenadeAnimationPayload(ByteBufCodecs.VAR_INT.decode(buf));
    }

    public static void handleClient(GrenadeAnimationPayload payload, IPayloadHandlerContext ctx) {
        try {
            Player player = ClientPlayerAccess.player();
            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof ItemGrenadeUniversal)) return;
            ItemGrenadeShell.EnumGrenadeShell shell =
                    ItemGrenadeShell.EnumGrenadeShell.values()[payload.shell];
            HbmAnimations.hotbar[player.getInventory().getSelectedSlot()][0] =
                    new Animation(
                            stack.getItem().getDescriptionId(),
                            GameTime.millis(player.level()),
                            animation(shell));
        } catch (Exception ignored) {
        }
    }

    private static BusAnimation animation(ItemGrenadeShell.EnumGrenadeShell shell) {
        return switch (shell) {
            case FRAG, TECH ->
                    new BusAnimation()
                            .addBus(
                                    "BODYMOVE",
                                    new BusAnimationSequence()
                                            .setPos(0, -5, 0)
                                            .addPos(0, -3, 0, 350)
                                            .addPos(0, 0, 0, 350, IType.SIN_DOWN))
                            .addBus(
                                    "BODYTURN",
                                    new BusAnimationSequence()
                                            .addPos(0, 0, 45, 350)
                                            .addPos(0, 0, -15, 350, IType.SIN_DOWN)
                                            .hold(200)
                                            .addPos(0, 0, -20, 100, IType.SIN_DOWN)
                                            .addPos(0, 0, 0, 500, IType.SIN_FULL))
                            .addBus(
                                    "RINGMOVE",
                                    new BusAnimationSequence()
                                            .hold(900)
                                            .addPos(0, 0, 1, 150)
                                            .addPos(0, -3, 3, 300))
                            .addBus(
                                    "RINGTURN",
                                    new BusAnimationSequence().hold(900).addPos(0, 0, 45, 300))
                            .addBus(
                                    "RENDERRING",
                                    new BusAnimationSequence()
                                            .setPos(1, 1, 1)
                                            .hold(1350)
                                            .setPos(0, 0, 0));
            case STICK ->
                    new BusAnimation()
                            .addBus(
                                    "BODYMOVE",
                                    new BusAnimationSequence()
                                            .setPos(0, -7, 0)
                                            .addPos(0, 3, 0, 750, IType.SIN_DOWN)
                                            .holdUntil(1900)
                                            .addPos(0, 0, 0, 250, IType.SIN_FULL))
                            .addBus(
                                    "BODYTURN",
                                    new BusAnimationSequence()
                                            .setPos(0, 0, 90)
                                            .addPos(0, 0, -45, 750, IType.SIN_DOWN)
                                            .holdUntil(1900)
                                            .addPos(0, 0, 0, 250, IType.SIN_FULL))
                            .addBus(
                                    "RINGMOVE",
                                    new BusAnimationSequence()
                                            .hold(800)
                                            .addPos(0, -0.25, 0, 200, IType.SIN_FULL)
                                            .hold(250)
                                            .addPos(0, -0.5, 0, 200, IType.SIN_FULL)
                                            .addPos(2, -5, 0, 350, IType.SIN_UP))
                            .addBus(
                                    "RINGTURN",
                                    new BusAnimationSequence()
                                            .hold(800)
                                            .addPos(0, 360, 0, 200, IType.SIN_FULL)
                                            .hold(250)
                                            .addPos(0, 360 * 2, 0, 200, IType.SIN_FULL))
                            .addBus(
                                    "RENDERRING",
                                    new BusAnimationSequence()
                                            .setPos(1, 1, 1)
                                            .hold(2100)
                                            .setPos(0, 0, 0));
            case NUKE ->
                    new BusAnimation()
                            .addBus(
                                    "BODYMOVE",
                                    new BusAnimationSequence()
                                            .setPos(0, -5, 0)
                                            .hold(250)
                                            .addPos(0, 0, 0, 850, IType.SIN_DOWN))
                            .addBus(
                                    "BODYTURN",
                                    new BusAnimationSequence()
                                            .setPos(0, 0, 90)
                                            .hold(250)
                                            .addPos(0, 0, -25, 850, IType.SIN_DOWN)
                                            .hold(200)
                                            .addPos(0, 0, -30, 100, IType.SIN_DOWN)
                                            .addPos(0, 0, 0, 750, IType.SIN_FULL))
                            .addBus(
                                    "RINGMOVE",
                                    new BusAnimationSequence()
                                            .hold(1300)
                                            .addPos(0, 0, 1, 150)
                                            .addPos(0, -3, 3, 300))
                            .addBus(
                                    "RINGTURN",
                                    new BusAnimationSequence().hold(1300).addPos(0, 0, 720, 500))
                            .addBus(
                                    "RENDERRING",
                                    new BusAnimationSequence()
                                            .setPos(1, 1, 1)
                                            .hold(1750)
                                            .setPos(0, 0, 0));
        };
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufCodecs.VAR_INT.encode(buf, shell);
    }

    @Override
    public @NotNull Type<GrenadeAnimationPayload> type() {
        return TYPE;
    }
}
