/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.blocks.machine;

import com.hbm.api.conveyor.ConveyorBelt;
import com.hbm.blockentity.processing.ConveyorStampBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.interfaces.ToolInteractionHandler;
import com.hbm.interfaces.ToolType;
import com.hbm.items.machine.StampItem;
import com.hbm.sound.ModSounds;
import com.hbm.util.NumberFormatter;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ConveyorPressBlock extends MultiblockCoreBlock
        implements ConveyorBelt,
                LookOverlayProvider,
                ToolInteractionHandler,
                TickingEntityBlock,
                CapabilityBlock {
    public static final MapCodec<ConveyorPressBlock> CODEC = simpleCodec(ConveyorPressBlock::new);

    private static final int RED = 0xFF5555;

    public ConveyorPressBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.CONVEYOR_PRESS)
                .powerIn()
                .fe()
                .faces(ConveyorStampBlockEntity.class, (be, side) -> side != Direction.DOWN);
    }

    private @Nullable ConveyorStampBlockEntity pressUnder(Level level, BlockPos beltPos) {
        return level.getBlockEntity(beltPos.below()) instanceof ConveyorStampBlockEntity press
                ? press
                : null;
    }

    @Override
    public boolean canItemStay(Level level, BlockPos pos, Vec3 itemPos) {
        return pressUnder(level, pos) != null;
    }

    public Direction getTravelDirection(Level level, BlockPos pos, Vec3 itemPos) {
        BlockPos core = pos.below();
        return level.getBlockState(core).getValue(FACING).getClockWise();
    }

    @Override
    public Vec3 getTravelPosition(Level level, BlockPos pos, Vec3 itemPos, double speed) {
        Direction dir = getTravelDirection(level, pos, itemPos);
        Vec3 clamped = clamp(pos, itemPos);
        Vec3 snap = snap(level, pos, clamped, dir);

        Vec3 dest =
                snap.subtract(
                        dir.getStepX() * speed, dir.getStepY() * speed, dir.getStepZ() * speed);
        Vec3 motion = dest.subtract(clamped);
        double len = motion.length();
        return clamped.add(motion.x / len * speed, motion.y / len * speed, motion.z / len * speed);
    }

    @Override
    public Vec3 getSnappingPosition(Level level, BlockPos pos, Vec3 itemPos) {
        return snap(level, pos, clamp(pos, itemPos), getTravelDirection(level, pos, itemPos));
    }

    private static Vec3 snap(Level level, BlockPos pos, Vec3 clamped, Direction dir) {
        double posX = dir.getStepX() != 0 ? clamped.x : pos.getX() + 0.5;
        double posZ = dir.getStepZ() != 0 ? clamped.z : pos.getZ() + 0.5;
        return new Vec3(posX, pos.getY() + 0.25, posZ);
    }

    private static Vec3 clamp(BlockPos pos, Vec3 itemPos) {
        return new Vec3(
                Mth.clamp(itemPos.x, pos.getX(), pos.getX() + 1D),
                itemPos.y,
                Mth.clamp(itemPos.z, pos.getZ(), pos.getZ() + 1D));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConveyorStampBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(core) instanceof ConveyorStampBlockEntity press)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof StampItem)
                || !press.getItem(ConveyorStampBlockEntity.SLOT_STAMP).isEmpty()) {
            return InteractionResult.PASS;
        }

        ItemStack stamp = held.copyWithCount(1);
        press.setItem(ConveyorStampBlockEntity.SLOT_STAMP, stamp);
        held.consume(1, player);
        level.playSound(null, core, ModSounds.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        press.markChanged();
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean onToolUse(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;

        BlockPos core = coreOf(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof ConveyorStampBlockEntity press))
            return false;

        ItemStack stamp = press.getItem(ConveyorStampBlockEntity.SLOT_STAMP);
        if (stamp.isEmpty()) return false;

        player.getInventory().placeItemBackInInventory(stamp.copy());
        press.setItem(ConveyorStampBlockEntity.SLOT_STAMP, ItemStack.EMPTY);
        press.markChanged();
        return true;
    }

    private static @Nullable BlockPos coreOf(Level level, BlockPos pos) {
        FoldedOwner owner = ownerOf(level, pos);
        if (owner != null) return owner.pos();
        return level.getBlockEntity(pos) instanceof ConveyorStampBlockEntity ? pos : null;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        BlockPos core = coreOf(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof ConveyorStampBlockEntity press))
            return;

        info.title(getName());
        info.row(
                Component.translatable(
                        "overlay.hbm.charge_each",
                        NumberFormatter.formatCompact(press.power),
                        NumberFormatter.formatCompact(ConveyorStampBlockEntity.MAX_POWER)));

        ItemStack stamp = press.syncStack;
        info.row(
                Component.translatable(
                        "overlay.hbm.installed_stamp",
                        stamp.isEmpty()
                                ? Component.translatable("overlay.hbm.installed_stamp.none")
                                : stamp.getHoverName()),
                stamp.isEmpty() ? RED : 0xFFFFFF);
    }
}
