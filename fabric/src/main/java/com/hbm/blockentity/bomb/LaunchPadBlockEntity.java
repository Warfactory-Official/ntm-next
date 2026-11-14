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

package com.hbm.blockentity.bomb;

import com.hbm.blockentity.ordnance.launch.LaunchPlatformBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.entity.missile.Missile;
import com.hbm.inventory.container.LaunchPadMenu;
import com.hbm.particle.HbmParticles;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class LaunchPadBlockEntity extends LaunchPlatformBlockEntity {
    private static final int LAUNCH_OFFSET = 1;
    private static final int RELOAD_TICKS = 100;

    private int reloadDelay;

    public LaunchPadBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAUNCH1.get(), pos, state);
    }

    @Override
    public void tickServer() {
        int prevReloadDelay = reloadDelay;
        if (reloadDelay > 0) hbm$syncSet$LaunchPadBlockEntity$reloadDelay((int) (reloadDelay - 1));
        if (!acceptsMissile(getItem(SLOT_MISSILE)) || !hasPropellant())
            hbm$syncSet$LaunchPadBlockEntity$reloadDelay(RELOAD_TICKS);
        if (reloadDelay != prevReloadDelay) markChanged();

        if (!hasPropellant() || !acceptsMissile(getItem(SLOT_MISSILE))) {
            hbm$syncSet$LaunchPlatformBlockEntity$statusCode(MISSING);
        } else {
            hbm$syncSet$LaunchPlatformBlockEntity$statusCode(reloadDelay > 0 ? LOADING : READY);
        }

        advancePlatform();
    }

    @Override
    public void tickClient() {
        if (level.getEntitiesOfClass(
                        Missile.class,
                        new AABB(
                                worldPosition.getX() - .5D,
                                worldPosition.getY(),
                                worldPosition.getZ() - .5D,
                                worldPosition.getX() + 1.5D,
                                worldPosition.getY() + 10D,
                                worldPosition.getZ() + 1.5D))
                .isEmpty()) return;
        Direction facing = MultiblockCoreBlock.coreFacing(getBlockState());
        for (int i = 0; i < 15; i++) {
            Direction direction = facing;
            if (level.getRandom().nextBoolean()) direction = direction.getOpposite();
            if (level.getRandom().nextBoolean()) direction = direction.getClockWise();
            double motionX = level.getRandom().nextGaussian() * .15D + .75D;
            double motionZ = level.getRandom().nextGaussian() * .15D + .75D;
            level.addParticle(
                    HbmParticles.LAUNCH_SMOKE.get(),
                    true,
                    false,
                    worldPosition.getX() + .5D,
                    worldPosition.getY() + .25D,
                    worldPosition.getZ() + .5D,
                    motionX * direction.getStepX(),
                    0D,
                    motionZ * direction.getStepZ());
        }
    }

    @Override
    public boolean mechanismReady() {
        return reloadDelay <= 0;
    }

    @Override
    protected double launchHeight() {
        return LAUNCH_OFFSET;
    }

    @Override
    protected void onMissileLaunched() {
        hbm$syncSet$LaunchPadBlockEntity$reloadDelay(RELOAD_TICKS);
    }

    public int reloadDelay() {
        return reloadDelay;
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 1L << 5;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit == 5) output.writeInt(reloadDelay);
        else super.writeSyncUnit(unit, output);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit == 5) hbm$syncSet$LaunchPadBlockEntity$reloadDelay(input.readInt());
        else super.readSyncUnit(unit, input);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$LaunchPadBlockEntity$reloadDelay(input.getIntOr("reloadDelay", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("reloadDelay", reloadDelay);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new LaunchPadMenu(containerId, inventory, this);
    }

    public final void hbm$syncSet$LaunchPadBlockEntity$reloadDelay(int next) {
        if (this.reloadDelay != next) {
            this.reloadDelay = next;
            if ((1L << 5) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 5);
        }
    }
}
