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

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.IndustrialTurbineBlockEntity;
import com.hbm.blockentity.machine.TurbineBaseBlockEntity;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.trait.CoolableTrait;
import com.hbm.sound.ModSounds;
import com.hbm.util.NumberFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class IndustrialTurbineBlock extends MultiblockCoreBlock
        implements TickingEntityBlock, CapabilityBlock, LookOverlayProvider {
    private static final String[] TURBINE_SPINNER = {"\u2596 ", "\u2598 ", " \u2598", " \u2596"};

    public IndustrialTurbineBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed()
                .powerOut()
                .fluidIn()
                .fluidOut()
                .fe()
                .faces(
                        IndustrialTurbineBlockEntity.class,
                        IndustrialTurbineBlockEntity::acceptsFace)
                .fluidFaces(
                        IndustrialTurbineBlockEntity.class,
                        (machine, face) ->
                                face.fluid() == null
                                        || machine.acceptsFluid(face.fluid(), face.side()));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IndustrialTurbineBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState state, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;

        var lever = core.relative(state.getValue(FACING), 3).above();
        if (!hit.getBlockPos().equals(lever)) return InteractionResult.PASS;
        if (level.getBlockEntity(core) instanceof IndustrialTurbineBlockEntity machine) {
            return pullLever(level, hit.getBlockPos(), player, machine);
        }
        return InteractionResult.PASS;
    }

    static InteractionResult pullLever(
            Level level, BlockPos clicked, Player player, TurbineBaseBlockEntity machine) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (!machine.operational) {
            level.playSound(
                    null, clicked, ModSounds.CHUNGUS_LEVER.get(), SoundSource.BLOCKS, 1.5F, 1F);
            machine.onLeverPull();
        } else {
            player.sendSystemMessage(
                    Component.translatable("desc.block.industrialTurbine.cannotChangeCompressor")
                            .withStyle(ChatFormatting.RED));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay card) {
        if (!(level.getBlockEntity(pos) instanceof IndustrialTurbineBlockEntity machine)) return;

        var input = machine.tanks[0];
        var output = machine.tanks[1];
        var inputType = input.getTankType();
        var cooling = NTMFluidProperties.getTrait(inputType, CoolableTrait.class);
        var outputType = NTMFluids.NONE;
        if (cooling != null) outputType = cooling.product();

        var inputText =
                Component.translatable(
                        "overlay.hbm.tank",
                        NTMFluidProperties.getDisplayName(inputType),
                        NumberFormatter.formatGrouped(input.getFill()),
                        NumberFormatter.formatGrouped(input.getMaxFill()));
        var outputText =
                Component.translatable(
                        "overlay.hbm.tank",
                        NTMFluidProperties.getDisplayName(outputType),
                        NumberFormatter.formatGrouped(output.getFill()),
                        NumberFormatter.formatGrouped(output.getMaxFill()));

        int frame = 0;
        if (machine.powerBuffer > 0) frame = (int) ((level.getGameTime() / 4) % 4);
        int red = (int) (0xFF - 0xFF * machine.spin);
        int green = (int) (0xFF * machine.spin);
        int spinColor = red << 16 | green << 8;
        var energyText =
                Component.translatable(
                                "unit.energy", NumberFormatter.formatCompact(machine.powerBuffer))
                        .append(" (")
                        .withStyle(ChatFormatting.WHITE);
        var powerText =
                Component.empty()
                        .append(energyText)
                        .append(TURBINE_SPINNER[frame])
                        .append(
                                Component.translatable(
                                        "unit.percent", Math.round(machine.spin * 100)))
                        .append(Component.literal(")").withStyle(ChatFormatting.WHITE));

        card.title(getName());
        card.row(LookOverlay.inflow(inputText));
        card.row(LookOverlay.outflow(outputText));
        card.row(LookOverlay.outflow(powerText), spinColor);
    }
}
