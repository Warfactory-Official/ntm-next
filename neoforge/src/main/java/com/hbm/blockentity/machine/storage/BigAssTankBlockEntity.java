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

package com.hbm.blockentity.machine.storage;

import com.hbm.api.environment.Environment;
import com.hbm.blockentity.Tiltable;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.config.BalanceConfig;
import com.hbm.data.MachineData;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.trait.FluidTraitSimple;
import com.hbm.sound.ModSounds;
import com.hbm.util.TickPhase;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class BigAssTankBlockEntity extends FluidBarrelBlockEntity implements Tiltable {

    public static final int CAPACITY = 16_000_000;
    private static final long RATE_FLOOR = 50_000L;
    private static final int PORT_REACH = 6;
    private static final int FLOOR_PROBES = 16;

    private int comparator;

    public BigAssTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIGASSTANK.get(), pos, state, CAPACITY);
    }

    @Override
    public void tickServer() {
        tickTilt(Policy.UNAVOIDABLE, true);
        super.tickServer();
        if (isRemoved()) return;
        int now = getComparatorPower();
        if (now != comparator) {
            comparator = now;
            setChanged();
            ((MultiblockCoreBlock) getBlockState().getBlock())
                    .updateComparatorOutput((ServerLevel) level, worldPosition);
        }
    }

    @Override
    protected boolean leaning() {
        return isTilted();
    }

    @Override
    public int getSupportCheckCount() {
        return FLOOR_PROBES;
    }

    @Override
    public @Nullable BlockPos getSupportCheckPosition(int index) {
        return floorGrid7(index);
    }

    @Override
    protected long receiveRate() {
        return getMode().receives() && !isTilted()
                ? Math.max(RATE_FLOOR, (tank.getMaxFill() - tank.getFill()) / 100L)
                : 0L;
    }

    @Override
    protected long provideRate() {
        TransferMode mode = getMode();
        return mode == TransferMode.BUFFER || mode == TransferMode.SEND && !isTilted()
                ? Math.max(RATE_FLOOR, tank.getFill() / 100L)
                : 0L;
    }

    @Override
    protected void checkHazards() {
        if (!NTMFluidProperties.hasTrait(tank.getTankType(), FluidTraitSimple.FT_Amat.class))
            return;
        level.destroyBlock(worldPosition, false);
        level.explode(
                null,
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D,
                10F,
                true,
                Level.ExplosionInteraction.BLOCK);
    }

    @Override
    protected boolean hasArms() {
        return false;
    }

    @Override
    protected void joinFluidGraph(ServerLevel server, Fluid fluid) {
        FluidBufferCells.join(
                server,
                worldPosition,
                FluidBufferCells.endPorts(worldPosition, getBlockState(), PORT_REACH),
                fluid);
    }

    @Override
    protected void leaveFluidGraph(ServerLevel server) {
        FluidBufferCells.leave(
                server, FluidBufferCells.endPorts(worldPosition, getBlockState(), PORT_REACH));
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.bigAssTank");
    }

    private int hbm$tiltChecked;

    private int hbm$tiltValid;

    public boolean isTilted() {
        return getBlockState().getValue(Tiltable.TILT);
    }

    public void tickTilt(Tiltable.Policy policy, boolean extraHeavy) {
        Level level = getLevel();
        boolean doesTilt =
                (policy == Tiltable.Policy.UNAVOIDABLE
                                ? Environment.unavoidableGravity(level)
                                : MachineData.ENABLE_MACHINE_GRAVITY.get()
                                        || BalanceConfig.enable528MachineGravity)
                        && !Environment.orbital(level);

        if (!doesTilt || getSupportCheckCount() <= 0) {
            hbm$settle(false);
            return;
        }
        if (!TickPhase.every(this, SharedConstants.TICKS_PER_SECOND)) return;

        if (hbm$tiltChecked >= getSupportCheckCount()) {
            boolean tilted = hbm$tiltValid < hbm$tiltChecked * 0.95;
            if (tilted && !isTilted()) {
                SoundEvent impact = ModSounds.METAL_IMPACT.get();
                level.playSound(null, getBlockPos(), impact, SoundSource.BLOCKS, 3F, 1F);
            }
            hbm$settle(tilted);
            hbm$tiltChecked = 0;
            hbm$tiltValid = 0;
        }

        BlockPos pos = getSupportCheckPosition(hbm$tiltChecked);
        if (pos == null) return;

        BlockState ground = level.getBlockState(pos);
        hbm$tiltChecked++;

        if (extraHeavy) {
            if (!ground.isSolidRender() || ground.isSignalSource()) return;
            if (ground.getBlock() instanceof FallingBlock
                    || ground.is(BlockTags.MINEABLE_WITH_SHOVEL)
                    || ground.is(BlockTags.WOOL)) return;
            if (ground.getBlock().getExplosionResistance() < Blocks.STONE.getExplosionResistance())
                return;
            hbm$tiltValid++;
            return;
        }
        if (!ground.isFaceSturdy(level, pos, Direction.UP)) return;
        if (ground.getBlock() instanceof FallingBlock) return;
        if (ground.is(ModBlocks.DIRT_DEAD.get())
                || ground.is(ModBlocks.DIRT_OILY.get())
                || ground.is(ModBlocks.STONE_CRACKED.get())) return;
        hbm$tiltValid++;
    }

    private void hbm$settle(boolean tilted) {
        BlockState state = getBlockState();
        if (state.getValue(Tiltable.TILT) == tilted) return;
        BlockState settled = state.setValue(Tiltable.TILT, tilted);
        getLevel()
                .setBlock(
                        getBlockPos(),
                        settled,
                        Block.UPDATE_CLIENTS
                                | Block.UPDATE_KNOWN_SHAPE
                                | Block.UPDATE_SKIP_ON_PLACE);
    }
}
