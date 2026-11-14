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

import com.hbm.api.fluidmk2.FluidPipeGraph;
import com.hbm.api.fluidmk2.PipeData;
import com.hbm.blocks.network.FluidPipeAnchorBlock;
import com.hbm.blocks.network.FluidPipeBlock;
import com.hbm.inventory.material.MaterialShape;
import com.hbm.items.ModDataComponents;
import com.hbm.sound.EntitySounds;
import com.hbm.uninos.graph.LevelNodeGraph;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

public class WrenchItem extends Item {

    public WrenchItem(Properties props) {
        super(props);
    }

    public static Properties properties() {
        return SwordAbilityItem.swordProperties(
                        ToolTier.STEEL, 6.0F, 0, MaterialShape.INGOT.tag("steel"))
                .component(DataComponents.WEAPON, new Weapon(0));
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        Vec3 look = attacker.getLookAngle();
        target.push(look.x * 0.5D, look.y * 0.5D, look.z * 0.5D);
        EntitySounds.play(
                target,
                target.getX(),
                target.getY(),
                target.getZ(),
                SoundEvents.ANVIL_LAND,
                target.getSoundSource(),
                3.0F,
                0.75F);
    }

    private static String connect(ServerLevel level, BlockPos first, BlockPos second) {
        if (first.equals(second)) return "Pipe error - Cannot connect to the same pipe anchor";

        long firstKey = first.asLong();
        long secondKey = second.asLong();
        PipeData firstData = FluidPipeGraph.dataAt(level, firstKey);
        PipeData secondData = FluidPipeGraph.dataAt(level, secondKey);
        if (firstData == null || secondData == null) return "Pipe error";

        Fluid firstFluid = firstData.fluid();
        Fluid secondFluid = secondData.fluid();
        if (firstFluid == Fluids.EMPTY && secondFluid != Fluids.EMPTY) {
            FluidPipeBlock.retypeNode(level, firstKey, secondFluid);
            firstFluid = secondFluid;
        }
        if (secondFluid == Fluids.EMPTY && firstFluid != Fluids.EMPTY) {
            FluidPipeBlock.retypeNode(level, secondKey, firstFluid);
            secondFluid = firstFluid;
        }
        if (firstFluid != secondFluid) return "Pipe error - Pipe anchor fluid types do not match";

        Vec3 delta = Vec3.atCenterOf(second).subtract(Vec3.atCenterOf(first));
        if (delta.length() > FluidPipeAnchorBlock.MAX_PIPE_LENGTH)
            return "Pipe error - Pipe anchor is too far away";

        LevelNodeGraph<PipeData> graph = FluidPipeGraph.get(level, firstFluid);
        graph.addRemoteLink(firstKey, secondKey);
        return "Pipe end";
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown()) return InteractionResult.PASS;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockState(pos).getBlock() instanceof FluidPipeAnchorBlock))
            return InteractionResult.PASS;
        ItemStack stack = context.getItemInHand();

        Long stored = stack.get(ModDataComponents.WRENCH_TARGET.get());
        if (stored == null) {
            stack.set(ModDataComponents.WRENCH_TARGET.get(), pos.asLong());
            if (!level.isClientSide())
                player.sendSystemMessage(Component.translatable("desc.item.wrench.pipeStart"));
            return InteractionResult.SUCCESS;
        }

        if (level.isClientSide()) return InteractionResult.SUCCESS;
        ServerLevel sl = (ServerLevel) level;
        BlockPos first = BlockPos.of(stored);
        stack.remove(ModDataComponents.WRENCH_TARGET.get());

        if (!(sl.getBlockState(first).getBlock() instanceof FluidPipeAnchorBlock)) {
            player.sendSystemMessage(Component.translatable("desc.item.wrench.pipeError"));
            return InteractionResult.SUCCESS;
        }
        player.sendSystemMessage(Component.literal(connect(sl, first, pos)));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        Long stored = stack.get(ModDataComponents.WRENCH_TARGET.get());
        if (stored != null) {
            BlockPos pos = BlockPos.of(stored);
            adder.accept(Component.translatable("desc.item.wrench.pipeStartX", pos.getX()));
            adder.accept(Component.translatable("desc.item.wrench.pipeStartY", pos.getY()));
            adder.accept(Component.translatable("desc.item.wrench.pipeStartZ", pos.getZ()));
        } else {
            adder.accept(Component.translatable("desc.item.wrench.rightClickPipeAnchors"));
        }
    }
}
