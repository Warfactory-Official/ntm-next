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

package com.hbm.blockentity.machine.pile;

import com.hbm.api.fluidmk2.PlainFluidReceiver;
import com.hbm.blockentity.machine.pile.PileCoreBlockEntity.PileChannel;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.pile.PileBlock;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class PileVentBlockEntity extends PileDeviceBaseBlockEntity
        implements FluidTankAccess, PlainFluidReceiver, SyncUnitSchema {
    public final FluidTankNTM compair = new FluidTankNTM(NTMFluids.AIR, 4_000).withPressure(1);
    public boolean active;
    public float fan;
    public float lastFan;

    public PileVentBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PILE_VENT.get(), pos, state, 0);
    }

    @Override
    public void tickServer() {
        hbm$syncSet$PileVentBlockEntity$active(false);
        PileChannel channel = attachedChannel(PileBlock.Role.AIR_IN);
        if (channel != null) {
            int fill = Math.min(compair.getFill(), PileChannel.MAX_AIR - channel.air);
            channel.air += fill;
            compair.setFill(compair.getFill() - fill);
            hbm$syncSet$PileVentBlockEntity$active(fill > 0);
            if (active) {
                setChanged();
                channel.coreChanged();
            }
        }
        networkPackNT(35);
    }

    @Override
    public void tickClient() {
        lastFan = fan;
        if (active) {
            fan += 45F;
            if (level.getRandom().nextInt(20) == 0)
                level.addParticle(
                        ParticleTypes.CLOUD,
                        worldPosition.getX() + 0.5D,
                        worldPosition.getY() + 1D,
                        worldPosition.getZ() + 0.5D,
                        0D,
                        0.05D,
                        0D);
        }
        if (fan >= 360F) {
            lastFan -= 360F;
            fan -= 360F;
        }
    }

    @Override
    public boolean canReceiveFluid(Fluid type) {
        return compair.joins(type);
    }

    @Override
    public void forEachTank(Consumer<FluidTankNTM> visitor) {
        visitor.accept(compair);
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (!compair.accepts(type) || pressure != compair.getPressure()) return 0L;
        return compair.getMaxFill() - compair.getFill();
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (!compair.accepts(type) || pressure != compair.getPressure()) return amount;
        int accepted = compair.fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
        if (accepted > 0) setChanged();
        return amount - accepted;
    }

    @Override
    public int receivingPressureMask(Fluid type) {
        return 1 << compair.getPressure();
    }

    @Override
    public int tankCount() {
        return 1;
    }

    @Override
    public FluidTankNTM tank(int index) {
        if (index != 0) throw new IndexOutOfBoundsException(index);
        return compair;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("tank").ifPresent(compair::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        compair.serialize(output.child("tank"));
    }

    @Override
    public long syncUnitMask() {
        return 7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> compair.packetSerialize(output);
            case 1 -> output.writeBoolean(active);
            case 2 -> output.writeInt(channelNumber);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> compair.packetDeserialize(input);
            case 1 -> hbm$syncSet$PileVentBlockEntity$active(input.readBoolean());
            case 2 -> hbm$syncSet$PileDeviceBaseBlockEntity$channelNumber(input.readInt());
            default -> throw new IllegalArgumentException();
        }
    }

    public final void hbm$syncSet$PileVentBlockEntity$active(boolean next) {
        if (this.active != next) {
            this.active = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(PileVentBlockEntity.class, "compair", 3, 1L);
    }
}
