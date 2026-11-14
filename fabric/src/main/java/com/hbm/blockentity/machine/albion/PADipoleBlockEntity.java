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

package com.hbm.blockentity.machine.albion;

import com.hbm.api.energymk2.EnergyItem;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.redstoneoverradio.RadioCommandParser;
import com.hbm.api.redstoneoverradio.RadioControlHandler;
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.blockentity.machine.albion.AcceleratorComponent;
import com.hbm.blockentity.machine.albion.CooledAcceleratorBlockEntity;
import com.hbm.blockentity.machine.albion.PASourceBlockEntity.PAState;
import com.hbm.blockentity.machine.albion.PASourceBlockEntity.Particle;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.PADipoleMenu;
import com.hbm.items.machine.PACoilItem.EnumCoilType;
import com.hbm.items.machine.PACoilItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import static com.hbm.api.redstoneoverradio.RadioCommandParser.FUN;
import static com.hbm.api.redstoneoverradio.RadioCommandParser.NAME_SEP;
import static com.hbm.api.redstoneoverradio.RadioCommandParser.VAL;

public class PADipoleBlockEntity extends CooledAcceleratorBlockEntity
        implements MenuProvider, AcceleratorComponent, RadioControlHandler, RadioValueProvider {
    public static final String[] ROR =
            new String[] {
                VAL + "temperature",
                VAL + "pfmcold",
                VAL + "pfm",
                FUN + "setthreshold" + NAME_SEP + "threshold",
            };
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_COIL = 1;
    public static final int SLOT_COUNT = 2;

    public static final long usage = 100_000;

    public int dirLower;
    public int dirUpper;
    public int dirRedstone;
    public int threshold;
    private boolean redstonePowered;

    public PADipoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PA_DIPOLE.get(), pos, state, SLOT_COUNT);
    }

    public static Direction ditToDirection(int dir) {
        if (dir == 1) return Direction.EAST;
        if (dir == 2) return Direction.SOUTH;
        if (dir == 3) return Direction.WEST;
        return Direction.NORTH;
    }

    @Override
    public long getMaxPower() {
        return 2_500_000;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.paDipole");
    }

    @Override
    public boolean canAcceptParticle(Particle particle, Direction dir, BlockPos pos) {
        return worldPosition.getY() == pos.getY()
                && (worldPosition.getX() == pos.getX() || worldPosition.getZ() == pos.getZ());
    }

    @Override
    public void onParticleEnter(Particle particle, Direction dir) {
        @Nullable EnumCoilType type = PACoilItem.typeOf(inventory.get(SLOT_COIL));
        boolean isInline = dir == getExitDir(particle);
        @Nullable PAState fault =
                type == null
                        ? PAState.CRASH_NOCOIL
                        : type.diMax < particle.momentum && !isInline
                                ? PAState.CRASH_OVERSPEED
                                : null;
        if (!admit(particle, usage * bendPenalty(type, particle, isInline), fault)) return;
        if (isInline) particle.addDistance(3);
        else particle.resetDistance();
    }

    private static int bendPenalty(@Nullable EnumCoilType type, Particle particle, boolean inline) {
        if (type == null || inline) return 1;
        int mult = 1;
        if (type.diMin > particle.momentum) mult *= 10;
        if (type.diDistMin > particle.distanceTraveled) mult *= 10;
        return mult;
    }

    @Override
    public BlockPos getParticleExit(Particle particle) {
        particle.dir = getExitDir(particle);
        return worldPosition.relative(particle.dir, 2);
    }

    public Direction getExitDir(Particle particle) {
        int dit =
                particle.momentum < this.threshold
                        ? dirLower
                        : checkRedstone() ? dirRedstone : dirUpper;
        return ditToDirection(dit);
    }

    public boolean checkRedstone() {
        return redstonePowered;
    }

    public void refreshRedstone() {
        boolean powered = false;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos arm = worldPosition.relative(side);
            if (level.hasNeighborSignal(arm.above()) || level.hasNeighborSignal(arm.below())) {
                powered = true;
                break;
            }
        }
        if (redstonePowered != powered) markChanged();
        redstonePowered = powered;
    }

    @Override
    public void tickServer() {
        hbm$syncSet$CooledAcceleratorBlockEntity$power(
                (long)
                        (power
                                + (ItemEnergyTransfer.extract(
                                        this, SLOT_BATTERY, getMaxPower() - power, false))));
        super.tickServer();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_BATTERY -> EnergyItem.matches(stack);
            case SLOT_COIL -> stack.getItem() instanceof PACoilItem;
            default -> false;
        };
    }

    public void cycleDirection(DirectionControl direction) {
        switch (direction) {
            case LOWER -> hbm$syncSet$PADipoleBlockEntity$dirLower((dirLower + 1) % 4);
            case UPPER -> hbm$syncSet$PADipoleBlockEntity$dirUpper((dirUpper + 1) % 4);
            case REDSTONE -> hbm$syncSet$PADipoleBlockEntity$dirRedstone((dirRedstone + 1) % 4);
        }
        setChanged();
    }

    public void setThreshold(int threshold) {
        this.hbm$syncSet$PADipoleBlockEntity$threshold(Mth.clamp(threshold, 0, 999_999_999));
        setChanged();
    }

    public enum DirectionControl {
        LOWER,
        UPPER,
        REDSTONE
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String getRadioValue(String name) {
        if ((VAL + "temperature").equals(name)) return Integer.toString((int) temperature);
        if ((VAL + "pfmcold").equals(name)) return Integer.toString(coolantTanks[0].getFill());
        if ((VAL + "pfm").equals(name)) return Integer.toString(coolantTanks[1].getFill());
        return null;
    }

    @Override
    public void runRadioFunction(String name, String[] params) {
        if ((FUN + "setthreshold").equals(name) && params.length > 0) {
            hbm$syncSet$PADipoleBlockEntity$threshold(
                    RadioCommandParser.intParam(params[0], 0, 999_999_999));
            setChanged();
        }
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new PADipoleMenu(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        redstonePowered = input.getBooleanOr("redstone", false);
        hbm$syncSet$PADipoleBlockEntity$dirLower(input.getIntOr("dirLower", 0));
        hbm$syncSet$PADipoleBlockEntity$dirUpper(input.getIntOr("dirUpper", 0));
        hbm$syncSet$PADipoleBlockEntity$dirRedstone(input.getIntOr("dirRedstone", 0));
        hbm$syncSet$PADipoleBlockEntity$threshold(input.getIntOr("threshold", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("redstone", redstonePowered);
        output.putInt("dirLower", dirLower);
        output.putInt("dirUpper", dirUpper);
        output.putInt("dirRedstone", dirRedstone);
        output.putInt("threshold", threshold);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x78L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 3 -> output.writeInt(this.dirLower);
            case 4 -> output.writeInt(this.dirUpper);
            case 5 -> output.writeInt(this.dirRedstone);
            case 6 -> output.writeInt(this.threshold);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 3 -> this.hbm$syncSet$PADipoleBlockEntity$dirLower(input.readInt());
            case 4 -> this.hbm$syncSet$PADipoleBlockEntity$dirUpper(input.readInt());
            case 5 -> this.hbm$syncSet$PADipoleBlockEntity$dirRedstone(input.readInt());
            case 6 -> this.hbm$syncSet$PADipoleBlockEntity$threshold(input.readInt());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$PADipoleBlockEntity$dirLower(int next) {
        if (this.dirLower != next) {
            this.dirLower = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    public final void hbm$syncSet$PADipoleBlockEntity$dirUpper(int next) {
        if (this.dirUpper != next) {
            this.dirUpper = next;
            if ((1L << 4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 4);
        }
    }

    public final void hbm$syncSet$PADipoleBlockEntity$dirRedstone(int next) {
        if (this.dirRedstone != next) {
            this.dirRedstone = next;
            if ((1L << 5) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 5);
        }
    }

    public final void hbm$syncSet$PADipoleBlockEntity$threshold(int next) {
        if (this.threshold != next) {
            this.threshold = next;
            if ((1L << 6) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 6);
        }
    }
}
