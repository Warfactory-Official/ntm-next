/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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
import com.hbm.blocks.network.BaseConveyorBlock;
import com.hbm.blocks.network.ConveyorBend;
import com.hbm.blocks.network.ConveyorBendableBlock;
import com.hbm.client.ModifierKeys;
import com.hbm.interfaces.ItemLookOverlayProvider;
import com.hbm.interfaces.LookOverlay;
import com.hbm.items.ModDataComponents;
import com.hbm.items.tool.ConveyorRoute;
import com.hbm.util.TooltipStyle;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class ConveyorWandItem extends Item implements ItemLookOverlayProvider {
    private static final int BREAK_DEPTH = 32;

    public final ConveyorType type;

    public ConveyorWandItem(Properties properties, ConveyorType type) {
        super(properties);
        this.type = type;
    }

    public static ConveyorType getType(ItemStack stack) {
        return ((ConveyorWandItem) stack.getItem()).type;
    }

    public static Block getConveyorBlock(ConveyorType type) {
        return type.block.get();
    }

    public static boolean hasSnakesAndLadders(ConveyorType type) {
        return type == ConveyorType.REGULAR;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        if (!ModifierKeys.detailsHeld()) {
            adder.accept(TooltipStyle.holdDetails());
            return;
        }

        for (Component line :
                TooltipStyle.lines(
                        Component.translatableWithFallback("item.hbm.conveyor_wand.desc", ""))) {
            adder.accept(line.copy().withStyle(ChatFormatting.YELLOW));
        }
        if (hasSnakesAndLadders(getType(stack))) {
            adder.accept(
                    Component.translatable("item.hbm.conveyor_wand.vertical.desc")
                            .withStyle(ChatFormatting.AQUA));
        }
    }

    @Override
    public void buildLookOverlay(
            Level level, BlockPos pos, ItemStack stack, Player player, LookOverlay info) {
        if (!player.isShiftKeyDown() || !player.getAbilities().instabuild) return;

        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BaseConveyorBlock)) return;

        info.title(state.getBlock().getName());
        info.row(Component.translatable("overlay.hbm.break_conveyor_line"));
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND) stack.remove(ModDataComponents.CONVEYOR_RUN.get());
    }

    @Override
    public boolean canDestroyBlock(
            ItemStack stack, BlockState state, Level level, BlockPos pos, LivingEntity user) {
        if (!(level instanceof ServerLevel server) || !(user instanceof Player player)) return true;
        if (!player.isShiftKeyDown() || !player.getAbilities().instabuild) return true;
        if (!(state.getBlock() instanceof BaseConveyorBlock belt)) return true;

        breakExtra(server, player, pos.relative(belt.getInputDirection(level, pos)), BREAK_DEPTH);
        breakExtra(server, player, pos.relative(belt.getOutputDirection(level, pos)), BREAK_DEPTH);
        return true;
    }

    private static void breakExtra(ServerLevel level, Player player, BlockPos pos, int depth) {
        if (--depth <= 0) return;

        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BaseConveyorBlock belt)) return;

        Direction input = belt.getInputDirection(level, pos);
        Direction output = belt.getOutputDirection(level, pos);

        level.destroyBlock(pos, false, player);

        breakExtra(level, player, pos.relative(input), depth);
        breakExtra(level, player, pos.relative(output), depth);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;

        Level level = ctx.getLevel();
        ItemStack stack = ctx.getItemInHand();
        BlockPos pos = ctx.getClickedPos();
        Direction side = ctx.getClickedFace();
        ConveyorRunData run = stack.get(ModDataComponents.CONVEYOR_RUN.get());

        if (player.isShiftKeyDown() && run == null) {
            if (level instanceof ServerLevel server) placeSingle(server, player, stack, pos, side);
            return InteractionResult.SUCCESS;
        }

        if (level.getBlockState(pos).getBlock() instanceof ConveyorBendableBlock bendable) {
            Direction moveDir =
                    run != null
                            ? bendable.getInputDirection(level, pos)
                            : bendable.getOutputDirection(level, pos);
            if (level.getBlockState(pos.relative(moveDir)).canBeReplaced()) side = moveDir;
        }

        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;

        if (run == null) {
            stack.set(
                    ModDataComponents.CONVEYOR_RUN.get(),
                    new ConveyorRunData(pos, side, budget(player, stack)));
            return InteractionResult.SUCCESS;
        }

        buildRun(server, player, stack, run, pos, side);
        stack.remove(ModDataComponents.CONVEYOR_RUN.get());
        return InteractionResult.SUCCESS;
    }

    private void placeSingle(
            ServerLevel level, Player player, ItemStack stack, BlockPos pos, Direction side) {
        ConveyorType type = getType(stack);
        BlockState onState = level.getBlockState(pos);
        Block onBlock = onState.getBlock();

        if (hasSnakesAndLadders(type)
                && onBlock == ModBlocks.CONVEYOR.get()
                && onState.getValue(ConveyorBendableBlock.BEND) == ConveyorBend.STRAIGHT) {
            if (side == Direction.UP) onBlock = ModBlocks.CONVEYOR_LIFT.get();
            else if (side == Direction.DOWN) onBlock = ModBlocks.CONVEYOR_CHUTE.get();

            if (onBlock != ModBlocks.CONVEYOR.get()) {
                level.setBlock(
                        pos,
                        onBlock.defaultBlockState()
                                .setValue(
                                        BaseConveyorBlock.FACING,
                                        onState.getValue(BaseConveyorBlock.FACING)),
                        Block.UPDATE_ALL);
            }
        }

        Block toPlace = getConveyorBlock(type);
        if (hasSnakesAndLadders(type)) {
            if (onBlock == ModBlocks.CONVEYOR_LIFT.get() && side == Direction.UP) toPlace = onBlock;
            if (onBlock == ModBlocks.CONVEYOR_CHUTE.get() && side == Direction.DOWN)
                toPlace = onBlock;
        }

        BlockPos target = pos.relative(side);
        if (!level.getBlockState(target).canBeReplaced()) return;

        BlockState placed =
                toPlace.defaultBlockState()
                        .setValue(BaseConveyorBlock.FACING, player.getDirection());
        level.setBlock(target, placed, Block.UPDATE_ALL);
        placeSound(level, target, placed);
        stack.consume(1, player);
    }

    private static void placeSound(ServerLevel level, BlockPos pos, BlockState state) {
        SoundType sound = state.getSoundType();
        level.playSound(
                null,
                pos,
                sound.getPlaceSound(),
                SoundSource.BLOCKS,
                (sound.getVolume() + 1F) / 2F,
                sound.getPitch() * 0.8F);
    }

    private static int budget(Player player, ItemStack stack) {
        if (player.getAbilities().instabuild) return 256;

        int count = 0;
        for (ItemStack carried : player.getInventory().getNonEquipmentItems()) {
            if (carried.getItem() == stack.getItem() && getType(carried) == getType(stack)) {
                count += carried.getCount();
            }
        }
        return count;
    }

    private void buildRun(
            ServerLevel level,
            Player player,
            ItemStack stack,
            ConveyorRunData run,
            BlockPos end,
            Direction endSide) {
        ConveyorType type = getType(stack);

        int planned =
                ConveyorRoute.construct(
                        level,
                        null,
                        type,
                        player,
                        run.anchor(),
                        run.side(),
                        end,
                        endSide,
                        run.budget());

        if (planned <= 0) {
            player.sendSystemMessage(
                    Component.literal(
                            planned == 0
                                    ? "Not enough conveyors, build cancelled"
                                    : "Conveyor obstructed, build cancelled"));
            return;
        }

        int spend =
                ConveyorRoute.construct(
                        level,
                        ConveyorRoute.into(level),
                        type,
                        player,
                        run.anchor(),
                        run.side(),
                        end,
                        endSide,
                        run.budget());
        placeSound(level, end.relative(endSide), getConveyorBlock(type).defaultBlockState());

        if (!player.getAbilities().instabuild) {
            for (ItemStack carried : player.getInventory().getNonEquipmentItems()) {
                if (spend <= 0) break;
                if (carried.getItem() != stack.getItem() || getType(carried) != type) continue;
                int removing = Math.min(spend, carried.getCount());
                carried.shrink(removing);
                spend -= removing;
            }
            player.inventoryMenu.broadcastChanges();
        }

        player.sendSystemMessage(Component.translatable("desc.item.conveyorWand.conveyorBuilt"));
    }

    public enum ConveyorType {
        REGULAR(() -> ModBlocks.CONVEYOR.get()),
        EXPRESS(() -> ModBlocks.CONVEYOR_EXPRESS.get()),
        DOUBLE(() -> ModBlocks.CONVEYOR_DOUBLE.get()),
        TRIPLE(() -> ModBlocks.CONVEYOR_TRIPLE.get());

        public static final ConveyorType[] VALUES = values();

        public final String id = name().toLowerCase(Locale.ROOT);
        private final Supplier<Block> block;

        ConveyorType(Supplier<Block> block) {
            this.block = block;
        }
    }
}
