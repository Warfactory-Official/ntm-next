// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

public final class ItemRodOfDiscord extends Item {

    public ItemRodOfDiscord(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        Vec3 from = player.getEyePosition(1.0F);
        Vec3 to = from.add(player.getViewVector(1.0F).scale(100.0D));
        BlockHitResult hit =
                level.clip(
                        new ClipContext(
                                from,
                                to,
                                ClipContext.Block.OUTLINE,
                                ClipContext.Fluid.NONE,
                                player));

        BlockHitResult blockHit =
                hit.getType() == HitResult.Type.MISS
                        ? Shapes.block().clip(from, to, BlockPos.containing(to))
                        : hit;
        if (blockHit == null) return super.use(level, player, hand);

        if (level.isClientSide()) {
            for (int i = 0; i < 32; ++i) {
                level.addParticle(
                        ParticleTypes.PORTAL,
                        player.getX(),
                        player.getY() + player.getRandom().nextDouble() * 2.0D,
                        player.getZ(),
                        player.getRandom().nextGaussian(),
                        0.0D,
                        player.getRandom().nextGaussian());
            }
        } else if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.stopRiding();
            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);

            Direction direction = blockHit.getDirection();
            Vec3 location = blockHit.getLocation();
            serverPlayer.teleportTo(
                    location.x + direction.getStepX(),
                    location.y + direction.getStepY() - 1.0D,
                    location.z + direction.getStepZ());

            level.playSound(
                    null,
                    serverPlayer.getX(),
                    serverPlayer.getY(),
                    serverPlayer.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
            serverPlayer.resetFallDistance();
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(
                Component.translatable("desc.item.rodOfDiscord.iVeSeenThe")
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
        adder.accept(
                Component.translatable("desc.item.rodOfDiscord.itSNotAs")
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
        adder.accept(Component.empty());
        adder.accept(
                Component.translatable("desc.item.rodOfDiscord.rodOfDiscordIs")
                        .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
        adder.accept(
                Component.translatable("desc.item.rodOfDiscord.imagineGettingCoiledBy")
                        .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
        adder.accept(Component.empty());
        adder.accept(
                Component.translatable("desc.item.rodOfDiscord.ohYouMeanThe")
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
        adder.accept(
                Component.translatable("desc.item.rodOfDiscord.idkAboutThatThing")
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
    }
}
