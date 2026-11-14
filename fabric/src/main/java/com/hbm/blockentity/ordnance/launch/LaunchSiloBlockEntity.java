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

package com.hbm.blockentity.ordnance.launch;

import com.hbm.api.item.TargetDesignator;
import com.hbm.blockentity.ChunkPublication;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.entity.ModEntities;
import com.hbm.entity.missile.Missile;
import com.hbm.interfaces.TriggerResult;
import com.hbm.inventory.container.LaunchPadRustedMenu;
import com.hbm.items.ModItems;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.particle.HbmParticles;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public final class LaunchSiloBlockEntity extends MachineBlockEntity
        implements SyncUnitSchema, MenuProvider {
    public static final int RECOVERY_SLOT = 0;
    public static final int CODE_SLOT = 1;
    public static final int KEY_SLOT = 2;
    public static final int DESIGNATOR_SLOT = 3;
    public static final int SLOT_COUNT = 4;

    private static final int PAYLOAD_UNIT = 0;
    private static final long PAYLOAD_MASK = 1L << PAYLOAD_UNIT;
    private static final Codec<List<BlockPos>> SIGNAL_CELLS_CODEC = BlockPos.CODEC.listOf();

    private boolean payloadArmed;
    private int poweredCellCount;
    private final LongOpenHashSet poweredCells = new LongOpenHashSet(4);
    private final AABB exhaustBounds;

    public LaunchSiloBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAUNCHPAD_RUSTED.get(), pos, state, SLOT_COUNT);
        exhaustBounds =
                new AABB(
                        pos.getX() - 0.5D,
                        pos.getY(),
                        pos.getZ() - 0.5D,
                        pos.getX() + 1.5D,
                        pos.getY() + 10D,
                        pos.getZ() + 1.5D);
    }

    public boolean isPayloadArmed() {
        return payloadArmed;
    }

    public void armPayload() {
        if (payloadArmed) return;
        hbm$syncSet$LaunchSiloBlockEntity$payloadArmed(true);
        setChanged();
    }

    public TriggerResult tryLaunch() {
        if (!payloadArmed) return TriggerResult.MISSING_COMPONENT;
        var code = getItem(CODE_SLOT);
        var key = getItem(KEY_SLOT);
        var designatorStack = getItem(DESIGNATOR_SLOT);
        if (!code.is(ModItems.LAUNCH_CODE.get()) || !key.is(ModItems.LAUNCH_KEY.get())) {
            return TriggerResult.MISSING_COMPONENT;
        }
        if (!(designatorStack.getItem() instanceof TargetDesignator designator)
                || !designator.isReady(designatorStack)) {
            return TriggerResult.MISSING_COMPONENT;
        }

        int targetX = designator.getTargetX(designatorStack);
        int targetZ = designator.getTargetZ(designatorStack);
        float launchX = worldPosition.getX() + 0.5F;
        float launchY = worldPosition.getY() + 1F;
        float launchZ = worldPosition.getZ() + 0.5F;
        if (!Missile.canReach(launchX, launchZ, targetX, targetZ)) {
            return TriggerResult.MISSING_COMPONENT;
        }

        var missile =
                ModEntities.MISSILE_DOOMSDAY_RUSTED.get().create(level, EntitySpawnReason.EVENT);
        missile.launch(launchX, launchY, launchZ, targetX, targetZ);
        level.addFreshEntity(missile);
        WorldSounds.machine(
                level,
                null,
                worldPosition,
                ModSounds.MISSILE_TAKE_OFF.get(),
                SoundSource.BLOCKS,
                2F,
                1F);
        hbm$syncSet$LaunchSiloBlockEntity$payloadArmed(false);
        code.shrink(1);
        setChanged();
        networkPackNT(250);
        return TriggerResult.LAUNCHED;
    }

    public void recoverPayload() {
        if (!payloadArmed || !getItem(RECOVERY_SLOT).isEmpty()) return;
        inventory.set(RECOVERY_SLOT, new ItemStack(ModItems.MISSILE_DOOMSDAY_RUSTED.get()));
        hbm$syncSet$LaunchSiloBlockEntity$payloadArmed(false);
        setChanged();
        networkPackNT(250);
    }

    public void refreshSignalCell(BlockPos cell) {
        boolean wasPowered = poweredCellCount > 0;
        boolean changed = recordSignal(cell.asLong(), level.hasNeighborSignal(cell));
        if (changed) finishSignalRefresh(wasPowered);
    }

    @Override
    public void onGraphLoad(ServerLevel server) {
        var signals = ChunkPublication.signals(server, worldPosition);
        var cell = new BlockPos.MutableBlockPos();
        boolean wasPowered = poweredCellCount > 0;
        boolean changed = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                cell.set(
                        worldPosition.getX() + dx, worldPosition.getY(), worldPosition.getZ() + dz);
                boolean cellChanged = recordSignal(cell.asLong(), signals.hasNeighborSignal(cell));
                changed = changed || cellChanged;
            }
        }
        if (changed) finishSignalRefresh(wasPowered);
    }

    private boolean recordSignal(long cell, boolean powered) {
        if (powered) return poweredCells.add(cell);
        return poweredCells.remove(cell);
    }

    private void finishSignalRefresh(boolean wasPowered) {
        poweredCellCount = poweredCells.isEmpty() ? -1 : poweredCells.size();
        setChanged();
        if (!wasPowered && poweredCellCount > 0) tryLaunch();
    }

    @Override
    public void tickClient() {
        if (level.getEntitiesOfClass(Missile.class, exhaustBounds).isEmpty()) return;
        var random = level.getRandom();
        var padFacing = MultiblockCoreBlock.coreFacing(getBlockState());
        double x = worldPosition.getX() + 0.5D;
        double y = worldPosition.getY() + 0.25D;
        double z = worldPosition.getZ() + 0.5D;
        for (int particle = 0; particle < 15; particle++) {
            var direction = padFacing;
            if (random.nextBoolean()) direction = direction.getOpposite();
            if (random.nextBoolean()) direction = direction.getClockWise();
            float xScale = (float) (random.nextGaussian() * 0.15F + 0.75D);
            float zScale = (float) (random.nextGaussian() * 0.15F + 0.75D);
            float motionX = direction.getStepX() * xScale;
            float motionZ = direction.getStepZ() * zScale;
            level.addParticle(
                    HbmParticles.LAUNCH_SMOKE.get(), true, false, x, y, z, motionX, 0D, motionZ);
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case CODE_SLOT -> stack.is(ModItems.LAUNCH_CODE.get());
            case KEY_SLOT -> stack.is(ModItems.LAUNCH_KEY.get());
            case DESIGNATOR_SLOT -> stack.getItem() instanceof TargetDesignator;
            default -> false;
        };
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new LaunchPadRustedMenu(containerId, playerInventory, this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.launchPadRusted");
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$LaunchSiloBlockEntity$payloadArmed(input.getBooleanOr("missileLoaded", false));
        poweredCellCount = input.getIntOr("redstonePower", 0);
        poweredCells.clear();
        for (var cell : input.read("activatedBlocks", SIGNAL_CELLS_CODEC).orElse(List.of())) {
            poweredCells.add(cell.asLong());
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("missileLoaded", payloadArmed);
        output.putInt("redstonePower", poweredCellCount);
        var savedCells = new ArrayList<BlockPos>(poweredCells.size());
        for (var cells = poweredCells.iterator(); cells.hasNext(); ) {
            savedCells.add(BlockPos.of(cells.nextLong()));
        }
        output.store("activatedBlocks", SIGNAL_CELLS_CODEC, savedCells);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | PAYLOAD_MASK;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit == PAYLOAD_UNIT) {
            output.writeBoolean(payloadArmed);
        } else {
            super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit == PAYLOAD_UNIT) {
            hbm$syncSet$LaunchSiloBlockEntity$payloadArmed(input.readBoolean());
        } else {
            super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$LaunchSiloBlockEntity$payloadArmed(boolean next) {
        if (this.payloadArmed != next) {
            this.payloadArmed = next;
            if ((PAYLOAD_MASK) == 0L) syncChanged(3);
            else syncUnitsChanged(3, PAYLOAD_MASK);
        }
    }
}
