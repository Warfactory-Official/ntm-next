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

package com.hbm.blockentity.machine.storage;

import com.hbm.blockentity.security.LockableBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.FileCabinetMenu;
import com.hbm.sound.ModSounds;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jspecify.annotations.Nullable;

public class FileCabinetBlockEntity extends LockableBlockEntity implements MenuProvider {

    public static final int SLOT_COUNT = 8;

    private static final float DRAWER_MAX = 0.8F;
    private static final float DRAWER_HALF = 0.4F;
    private static final float OPEN_SPEED = 1F / 16F;
    private static final float CLOSE_SPEED = 1F / 25F;
    private static final int UPPER_DELAY = 10;
    private static final long UNIT_TIMER = 1L << 4;
    private static final long UNIT_VIEWERS = 1L << 5;

    private int timer;
    private int viewers;
    public float lowerDrawer;
    public float prevLowerDrawer;
    public float upperDrawer;
    public float prevUpperDrawer;

    public FileCabinetBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FILE_CABINET.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public void tickServer() {
        if (viewers > 0) {
            if (timer < UPPER_DELAY) hbm$syncSet$FileCabinetBlockEntity$timer((int) (timer + 1));
            tickViewed();
        } else if (timer != 0) {
            hbm$syncSet$FileCabinetBlockEntity$timer(0);
        }
        moveDrawers();
        networkPackNT(25);
    }

    protected void tickViewed() {}

    @Override
    public void tickClient() {
        moveDrawers();
    }

    private void moveDrawers() {
        prevLowerDrawer = lowerDrawer;
        prevUpperDrawer = upperDrawer;
        if (viewers > 0) {
            if (lowerDrawer == 0F && upperDrawer == 0F) {
                cue(ModSounds.CRATE_OPEN.get(), 0.8F, false);
            } else {
                if (upperDrawer + OPEN_SPEED >= DRAWER_MAX && lowerDrawer < DRAWER_MAX) {
                    cue(ModSounds.CRATE_OPEN.get(), 0.5F, true);
                }
                if (lowerDrawer + OPEN_SPEED >= DRAWER_MAX && lowerDrawer < DRAWER_MAX) {
                    cue(ModSounds.CRATE_OPEN.get(), 0.5F, true);
                }
            }
            lowerDrawer += OPEN_SPEED;
            if (timer >= UPPER_DELAY) upperDrawer += OPEN_SPEED;
        } else if (lowerDrawer > 0F) {
            if (upperDrawer != lowerDrawer
                    && upperDrawer >= DRAWER_HALF
                    && upperDrawer - CLOSE_SPEED < DRAWER_HALF) {
                cue(ModSounds.CRATE_CLOSE.get(), 0.8F, false);
            }
            if (lowerDrawer >= DRAWER_HALF && lowerDrawer - CLOSE_SPEED < DRAWER_HALF) {
                cue(ModSounds.CRATE_CLOSE.get(), 0.8F, false);
            }
            lowerDrawer -= CLOSE_SPEED;
            upperDrawer -= CLOSE_SPEED;
        }
        lowerDrawer = Math.clamp(lowerDrawer, 0F, DRAWER_MAX);
        upperDrawer = Math.clamp(upperDrawer, 0F, DRAWER_MAX);
    }

    private void cue(SoundEvent sound, float volume, boolean clunk) {
        if (!level.isClientSide()) {
            level.gameEvent(null, GameEvent.BLOCK_ACTIVATE, worldPosition);
            return;
        }
        float pitch = clunk ? 0.7F + 0.1F * level.getRandom().nextFloat() : 1F;
        level.playLocalSound(
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D,
                sound,
                SoundSource.BLOCKS,
                volume,
                pitch,
                false);
    }

    @Override
    public void startOpen(ContainerUser user) {
        hbm$syncSet$FileCabinetBlockEntity$viewers((int) (viewers + 1));
    }

    @Override
    public void stopOpen(ContainerUser user) {
        hbm$syncSet$FileCabinetBlockEntity$viewers((int) (viewers - 1));
    }

    @Override
    protected double interactionRangeSq() {
        return 8D * 8D;
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(
            int containerId, Inventory inventory, Player player) {
        if (!player.isSpectator() && !canAccess(player)) return null;
        return new FileCabinetMenu(containerId, inventory, this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.fileCabinet");
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | UNIT_TIMER | UNIT_VIEWERS;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 4 -> output.writeInt(timer);
            case 5 -> output.writeInt(viewers);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 4 -> hbm$syncSet$FileCabinetBlockEntity$timer(input.readInt());
            case 5 -> hbm$syncSet$FileCabinetBlockEntity$viewers(input.readInt());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$FileCabinetBlockEntity$timer(int next) {
        if (this.timer != next) {
            this.timer = next;
            if ((1L << 4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 4);
        }
    }

    public final void hbm$syncSet$FileCabinetBlockEntity$viewers(int next) {
        if (this.viewers != next) {
            this.viewers = next;
            if ((1L << 5) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 5);
        }
    }
}
