// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.client.ClientPlayerAccess;
import com.hbm.items.IAnimatedItem;
import com.hbm.items.armor.ArmorSuitEffects;
import com.hbm.items.armor.ModArmorItem;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.AnimationEnums;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.HbmAnimations.Animation;
import com.hbm.render.anim.HbmAnimations;
import com.hbm.util.GameTime;
import io.netty.buffer.ByteBuf;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

public final class HbmAnimationPayload extends ThreadedPayload {

    public static final Type<HbmAnimationPayload> TYPE = new Type<>(Library.id("gun_anim"));
    public static final StreamCodec<ByteBuf, HbmAnimationPayload> STREAM_CODEC =
            streamCodec(HbmAnimationPayload::decode);

    private final int type;
    private final int receiverIndex;
    private final int itemIndex;

    public HbmAnimationPayload(int type, int receiverIndex, int itemIndex) {
        this.type = type;
        this.receiverIndex = receiverIndex;
        this.itemIndex = itemIndex;
    }

    private static HbmAnimationPayload decode(ByteBuf buf) {
        return new HbmAnimationPayload(
                ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf));
    }

    public static void handleClient(HbmAnimationPayload payload, IPayloadHandlerContext ctx) {
        try {
            Player player = ClientPlayerAccess.player();
            ItemStack stack = player.getMainHandItem();
            int slot = player.getInventory().getSelectedSlot();

            if (stack.isEmpty()) return;

            if (stack.getItem() instanceof ItemGunBaseNT) {
                handleSedna(
                        player,
                        stack,
                        slot,
                        GunAnimation.values()[payload.type],
                        payload.receiverIndex,
                        payload.itemIndex);
            } else if (stack.getItem() instanceof IAnimatedItem item) {
                handleAnimatedItem(
                        slot,
                        stack,
                        item.getAnimation(
                                AnimationEnums.ToolAnimation.values()[payload.type], stack));
            }
        } catch (Exception x) {
        }
    }

    private static void handleAnimatedItem(
            int slot, ItemStack stack, @Nullable BusAnimation animation) {
        if (animation == null) return;
        HbmAnimations.hotbar[slot][0] =
                new Animation(
                        stack.getItem().getDescriptionId(), GameTime.millis(), animation, false);
    }

    public static void handleSedna(
            Player player,
            ItemStack stack,
            int slot,
            GunAnimation type,
            int receiverIndex,
            int gunIndex) {
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        GunConfig config = gun.getConfig(stack, gunIndex);

        if (type == GunAnimation.CYCLE) {
            if (gunIndex < gun.lastShot.length)
                gun.lastShot[gunIndex] = GameTime.millis(player.level());
            gun.shotRand = player.level().getRandom().nextDouble();

            Receiver[] receivers = config.getReceivers(stack);
            if (receiverIndex >= 0 && receiverIndex < receivers.length) {
                Receiver rec = receivers[receiverIndex];
                BiConsumer<ItemStack, LambdaContext> onRecoil = rec.getRecoil(stack);
                if (onRecoil != null)
                    onRecoil.accept(
                            stack,
                            new LambdaContext(
                                    config, player, player.getInventory(), receiverIndex));
            }
        }

        BiFunction<ItemStack, GunAnimation, BusAnimation> anims = config.getAnims(stack);
        BusAnimation animation = anims.apply(stack, type);

        if (animation == null
                && (type == GunAnimation.ALT_CYCLE || type == GunAnimation.CYCLE_EMPTY)) {
            animation = anims.apply(stack, GunAnimation.CYCLE);
        }

        if (animation != null) {

            boolean isReloadAnimation =
                    type == GunAnimation.RELOAD || type == GunAnimation.RELOAD_CYCLE;
            if (isReloadAnimation
                    && ArmorSuitEffects.hasFullSet(player, ModArmorItem.Suit.TRENCHMASTER)) {
                animation.setTimeMult(0.5D);
            }
            HbmAnimations.hotbar[slot][gunIndex] =
                    new Animation(
                            stack.getItem().getDescriptionId(),
                            GameTime.millis(),
                            animation,
                            isReloadAnimation && config.getReloadAnimSequential(stack));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufCodecs.VAR_INT.encode(buf, type);
        ByteBufCodecs.VAR_INT.encode(buf, receiverIndex);
        ByteBufCodecs.VAR_INT.encode(buf, itemIndex);
    }

    @Override
    public @NotNull Type<HbmAnimationPayload> type() {
        return TYPE;
    }
}
