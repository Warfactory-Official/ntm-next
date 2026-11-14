/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.items.tool;

import com.hbm.blocks.ModBlocks;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.BatteryItem;
import com.hbm.sound.EntitySounds;
import com.hbm.util.NumberFormatter;
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

public class AnchorRemoteItem extends BatteryItem {
    private static final long JUMP_COST = 10_000;
    private static final float ARRIVAL_BLAST = 2F;
    private static final int PORTAL_PARTICLES = 32;

    public AnchorRemoteItem(Properties props) {
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
        EntitySounds.play(
                player,
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
        removeCharge(stack, JUMP_COST);
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult refuse(ServerLevel level, Player player) {
        EntitySounds.play(
                player,
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
                        NumberFormatter.formatCompact(getCharge(stack))
                                + "/"
                                + NumberFormatter.formatCompact(maxCharge)
                                + "HE"));
        adder.accept(
                Component.translatable(
                        "desc.shared.chargeRate", NumberFormatter.formatCompact(chargeRate)));
    }
}
