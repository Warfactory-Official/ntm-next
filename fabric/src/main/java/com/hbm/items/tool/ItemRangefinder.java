// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.items.ModDataComponents;
import com.hbm.packet.toclient.PlayerInformPayload;
import com.hbm.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ItemRangefinder extends Item {

    private static final double RANGE = 300D;

    public ItemRangefinder(Properties properties) {
        super(properties);
    }

    public static boolean isPolarized(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.POLARIZED.get(), false);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer))
            return InteractionResult.SUCCESS;

        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(RANGE));
        BlockHitResult hit =
                level.clip(
                        new ClipContext(
                                start,
                                end,
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                player));

        if (hit.getType() == HitResult.Type.BLOCK) {
            double dist = start.distanceTo(hit.getLocation());
            MutableComponent message =
                    Component.translatable(
                            "desc.item.rangefinder.distance",
                            String.valueOf(((int) (dist * 10D)) / 10D));
            if (isPolarized(player.getItemInHand(hand)))
                message.withStyle(ChatFormatting.LIGHT_PURPLE);
            Services.NETWORK.sendTo(
                    new PlayerInformPayload(message, PlayerInformPayload.ID_DETONATOR, 5_000),
                    serverPlayer);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getName(ItemStack stack) {
        Component name = super.getName(stack);
        return isPolarized(stack) ? name.copy().withStyle(ChatFormatting.LIGHT_PURPLE) : name;
    }
}
