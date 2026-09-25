// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.blocks.ModBlocks;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.ItemBattery;
import com.hbm.util.BobMathUtil;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ItemAnchorRemote extends ItemBattery {

    private static final long JUMP_COST = 10_000;
    private static final float ARRIVAL_BLAST = 2F;
    private static final int PORTAL_PARTICLES = 32;

    public ItemAnchorRemote(Properties props) {
        super(props, 1_000_000, JUMP_COST, 0);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        if (!level.getBlockState(pos).is(ModBlocks.TELEANCHOR.get())) return InteractionResult.PASS;

        ctx.getItemInHand().set(ModDataComponents.ANCHOR_POS.get(), pos.asLong());
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown() || !(level instanceof ServerLevel sl))
            return InteractionResult.PASS;

        ItemStack stack = player.getItemInHand(hand);
        Long packed = stack.get(ModDataComponents.ANCHOR_POS.get());
        if (packed == null || getCharge(stack) < JUMP_COST) return refuse(sl, player);

        BlockPos target = BlockPos.of(packed);

        sl.getChunk(target);
        BlockState state = sl.getBlockState(target);
        if (!state.is(ModBlocks.TELEANCHOR.get())) return refuse(sl, player);

        player.stopRiding();
        sl.explode(
                player,
                target.getX() + 0.5,
                target.getY() + 1 + player.getBbHeight() / 2,
                target.getZ() + 0.5,
                ARRIVAL_BLAST,
                Level.ExplosionInteraction.NONE);
        sl.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
        player.teleportTo(target.getX() + 0.5, target.getY() + 1, target.getZ() + 0.5);
        player.fallDistance = 0.0F;

        for (int i = 0; i < PORTAL_PARTICLES; i++) {
            sl.sendParticles(
                    ParticleTypes.PORTAL,
                    player.getX(),
                    player.getY() + player.getRandom().nextDouble() * 2.0D,
                    player.getZ(),
                    1,
                    player.getRandom().nextGaussian(),
                    0.0D,
                    player.getRandom().nextGaussian(),
                    1.0D);
        }
        dischargeBattery(stack, JUMP_COST);
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult refuse(ServerLevel level, Player player) {
        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS,
                0.25F,
                0.75F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(
                Component.translatable(
                        "desc.shared.energyStored",
                        BobMathUtil.getShortNumber(getCharge(stack))
                                + "/"
                                + BobMathUtil.getShortNumber(maxCharge)
                                + "HE"));
        adder.accept(
                Component.translatable(
                        "desc.shared.chargeRate", BobMathUtil.getShortNumber(chargeRate)));
    }
}
