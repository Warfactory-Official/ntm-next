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
import com.hbm.api.redstoneoverradio.RadioControlHandler;
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.blockentity.machine.albion.AcceleratorComponent;
import com.hbm.blockentity.machine.albion.CooledAcceleratorBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.capability.port.CapabilityPortProvider;
import com.hbm.capability.port.ItemPort;
import com.hbm.capability.port.ItemPortCache;
import com.hbm.inventory.container.PASourceMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import static com.hbm.api.redstoneoverradio.RadioCommandParser.FUN;
import static com.hbm.api.redstoneoverradio.RadioCommandParser.VAL;

public class PASourceBlockEntity extends CooledAcceleratorBlockEntity
        implements MenuProvider, CapabilityPortProvider, RadioValueProvider, RadioControlHandler {

    public static final String[] ROR = {
        VAL + "status", VAL + "momentum", VAL + "defocus",
        VAL + "temperature", VAL + "pfmcold", VAL + "pfm",
        FUN + "cancel"
    };

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_INPUT_1 = 1;
    public static final int SLOT_INPUT_2 = 2;
    public static final int SLOT_CONTAINER_1 = 3;
    public static final int SLOT_CONTAINER_2 = 4;
    public static final int SLOT_COUNT = 5;

    public static final long usage = 100_000;
    private static final int[] SLOTS_DEFAULT = {SLOT_CONTAINER_1, SLOT_CONTAINER_2};
    private static final int[] SLOTS_RED = {SLOT_INPUT_1, SLOT_CONTAINER_1, SLOT_CONTAINER_2};
    private static final int[] SLOTS_YELLOW = {SLOT_INPUT_2, SLOT_CONTAINER_1, SLOT_CONTAINER_2};
    public @Nullable Particle particle;
    public PAState state;

    {
        hbm$syncSet$PASourceBlockEntity$state(PAState.IDLE);
    }

    public int lastSpeed;
    public int debugSpeed;

    public PASourceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PA_SOURCE.get(), pos, state, SLOT_COUNT);
    }

    private static ItemStack remainderOf(ItemStack stack) {
        ItemStackTemplate remainder = stack.getCraftingRemainder();
        return remainder == null ? ItemStack.EMPTY : remainder.create();
    }

    public void updateState(PAState state) {
        this.hbm$syncSet$PASourceBlockEntity$state(state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.paSource");
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public long getMaxPower() {
        return 10_000_000;
    }

    @Override
    public void tickServer() {
        hbm$syncSet$CooledAcceleratorBlockEntity$power(
                (long)
                        (power
                                + (ItemEnergyTransfer.extract(
                                        this, SLOT_BATTERY, getMaxPower() - power, false))));

        int steps = 1;
        if (particle != null) steps = 1 + Mth.clamp(particle.momentum / 1_000, 0, 9);

        for (int i = 0; i < steps; i++) {
            if (particle != null) {
                this.hbm$syncSet$PASourceBlockEntity$state(PAState.RUNNING);
                steppy();
                this.hbm$syncSet$PASourceBlockEntity$debugSpeed(particle.momentum);
                if (particle.invalid) this.particle = null;
            } else if (this.power >= usage
                    && !inventory.get(SLOT_INPUT_1).isEmpty()
                    && !inventory.get(SLOT_INPUT_2).isEmpty()) {
                tryRun();
                break;
            }
        }

        super.tickServer();
    }

    public void steppy() {
        Particle p = this.particle;
        if (p == null) return;

        if (!MultiblockCoreBlock.canReadWithoutLoading(level, p.pos)) {
            this.hbm$syncSet$PASourceBlockEntity$state(PAState.PAUSE_UNLOADED);
            return;
        }
        markChanged();

        BlockPos core = componentCore(level.getBlockState(p.pos), p.pos);
        if (core == null) {
            p.crash(PAState.CRASH_DERAIL);
            return;
        }
        BlockEntity tile = level.getBlockEntity(core);
        if (!(tile instanceof AcceleratorComponent pa)) {
            p.crash(PAState.CRASH_DERAIL);
            return;
        }
        if (pa.canAcceptParticle(p, p.dir, p.pos)) {
            pa.onParticleEnter(p, p.dir);
            BlockPos exit = pa.getParticleExit(p);
            if (exit != null) p.move(exit);
        } else {
            p.crash(PAState.CRASH_CANNOT_ENTER);
        }
    }

    private @Nullable BlockPos componentCore(BlockState state, BlockPos pos) {
        if (MultiblockSurface.foldedCore(state) != null) return pos.immutable();
        if (MultiblockSurface.isFoldedCell(state) && level instanceof ServerLevel server) {
            return MultiblockSurface.indexedCore(server, pos);
        }
        return null;
    }

    public void tryRun() {
        if (!isCool()) return;

        ItemStack in1 = inventory.get(SLOT_INPUT_1);
        ItemStack in2 = inventory.get(SLOT_INPUT_2);
        ItemStack container1 = remainderOf(in1);
        ItemStack container2 = remainderOf(in2);

        if (!container1.isEmpty() && !inventory.get(SLOT_CONTAINER_1).isEmpty()) return;
        if (!container2.isEmpty() && !inventory.get(SLOT_CONTAINER_2).isEmpty()) return;

        if (!container1.isEmpty()) inventory.set(SLOT_CONTAINER_1, container1);
        if (!container2.isEmpty()) inventory.set(SLOT_CONTAINER_2, container2);

        this.hbm$syncSet$CooledAcceleratorBlockEntity$power((long) (this.power - (usage)));
        Direction dir = MultiblockCoreBlock.coreFacing(getBlockState());
        Direction rot = dir.getCounterClockWise();
        this.particle = new Particle(this, worldPosition.relative(rot, 5), rot, in1, in2);
        inventory.set(SLOT_INPUT_1, ItemStack.EMPTY);
        inventory.set(SLOT_INPUT_2, ItemStack.EMPTY);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_BATTERY -> EnergyItem.matches(stack);
            case SLOT_INPUT_1, SLOT_INPUT_2 -> true;
            default -> false;
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == SLOT_INPUT_1 || slot == SLOT_INPUT_2;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_CONTAINER_1 || slot == SLOT_CONTAINER_2;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS_DEFAULT;
    }

    private final ItemPortCache portCache =
            new ItemPortCache(
                    (slot, stack) ->
                            (slot == SLOT_INPUT_1 || slot == SLOT_INPUT_2)
                                    && canPlaceItem(slot, stack),
                    (slot, stack) -> slot == SLOT_CONTAINER_1 || slot == SLOT_CONTAINER_2);

    @Override
    public @Nullable ItemPort itemAccess(BlockPos cell, Direction side) {
        Direction dir = MultiblockCoreBlock.coreFacing(getBlockState());
        Direction rot = dir.getClockWise();

        int[] slots = SLOTS_DEFAULT;
        if (cell.equals(worldPosition.relative(dir).relative(rot, -2))
                || cell.equals(worldPosition.relative(dir, -1).relative(rot, 2))) {
            slots = SLOTS_YELLOW;
        } else if (cell.equals(worldPosition.relative(dir, -1).relative(rot, -2))
                || cell.equals(worldPosition.relative(dir).relative(rot, 2))) {
            slots = SLOTS_RED;
        }

        return portCache.forSlots(slots);
    }

    public void cancelParticle() {
        particle = null;
        hbm$syncSet$PASourceBlockEntity$state(PAState.IDLE);
        setChanged();
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String getRadioValue(String name) {
        if ((VAL + "status").equals(name)) return state.toString();
        if ((VAL + "momentum").equals(name)) return Integer.toString(lastSpeed);
        if ((VAL + "defocus").equals(name))
            return particle == null ? "0" : Integer.toString(particle.defocus);
        if ((VAL + "temperature").equals(name)) return Integer.toString((int) temperature);
        if ((VAL + "pfmcold").equals(name)) return Integer.toString(coolantTanks[0].getFill());
        if ((VAL + "pfm").equals(name)) return Integer.toString(coolantTanks[1].getFill());
        return null;
    }

    @Override
    public void runRadioFunction(String name, String[] params) {
        if ((FUN + "cancel").equals(name)) cancelParticle();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new PASourceMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        if (particle != null) {
            ValueOutput tag = output.child("particle");
            tag.store("pos", BlockPos.CODEC, particle.pos);
            tag.putByte("dir", (byte) particle.dir.get3DDataValue());
            tag.putInt("momentum", particle.momentum);
            tag.putInt("defocus", particle.defocus);
            tag.putInt("dist", particle.distanceTraveled);
            tag.store("input1", ItemStack.OPTIONAL_CODEC, particle.input1);
            tag.store("input2", ItemStack.OPTIONAL_CODEC, particle.input2);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.particle =
                input.child("particle")
                        .map(
                                tag -> {
                                    BlockPos pos =
                                            tag.read("pos", BlockPos.CODEC).orElse(worldPosition);
                                    Direction dir =
                                            Direction.VALUES[tag.getByteOr("dir", (byte) 0)];
                                    Particle p =
                                            new Particle(
                                                    this,
                                                    pos,
                                                    dir,
                                                    tag.read("input1", ItemStack.OPTIONAL_CODEC)
                                                            .orElse(ItemStack.EMPTY),
                                                    tag.read("input2", ItemStack.OPTIONAL_CODEC)
                                                            .orElse(ItemStack.EMPTY));
                                    p.momentum = tag.getIntOr("momentum", 0);
                                    p.defocus = tag.getIntOr("defocus", 0);
                                    p.distanceTraveled = tag.getIntOr("dist", 0);
                                    return p;
                                })
                        .orElse(null);
    }

    public enum PAState {
        IDLE(0x8080ff),
        RUNNING(0xffff00),
        SUCCESS(0x00ff00),
        PAUSE_UNLOADED(0x808080),
        CRASH_DEFOCUS(0xff0000),
        CRASH_DERAIL(0xff0000),
        CRASH_CANNOT_ENTER(0xff0000),
        CRASH_NOCOOL(0xff0000),
        CRASH_NOPOWER(0xff0000),
        CRASH_NOCOIL(0xff0000),
        CRASH_OVERSPEED(0xff0000),
        CRASH_UNDERSPEED(0xff0000),
        CRASH_NORECIPE(0xff0000);

        public static final PAState[] VALUES = values();

        public final int color;

        PAState(int color) {
            this.color = color;
        }
    }

    public static class Particle {

        public static final int maxDefocus = 1000;
        private final PASourceBlockEntity source;
        public BlockPos pos;
        public Direction dir;
        public int momentum;
        public int defocus;
        public int distanceTraveled;
        public boolean invalid = false;

        public ItemStack input1;
        public ItemStack input2;

        public Particle(
                PASourceBlockEntity source,
                BlockPos pos,
                Direction dir,
                ItemStack input1,
                ItemStack input2) {
            this.source = source;
            this.pos = pos;
            this.dir = dir;
            this.input1 = input1;
            this.input2 = input2;
        }

        public void crash(PAState state) {
            this.invalid = true;
            this.source.updateState(state);
        }

        public void move(BlockPos pos) {
            this.pos = pos;
            this.source.hbm$syncSet$PASourceBlockEntity$lastSpeed(this.momentum);
        }

        public void addDistance(int dist) {
            this.distanceTraveled += dist;
        }

        public void resetDistance() {
            this.distanceTraveled = 0;
        }

        public void defocus(int amount) {
            this.defocus += amount;
            if (this.defocus > maxDefocus) this.crash(PAState.CRASH_DEFOCUS);
        }

        public void focus(int amount) {
            this.defocus -= amount;
            if (this.defocus < 0) this.defocus = 0;
        }
    }

    private void writeState(ByteBuf output) {
        output.writeByte(state.ordinal());
    }

    private void readState(ByteBuf input) {
        hbm$syncSet$PASourceBlockEntity$state(PAState.VALUES[input.readByte()]);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x38L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 3 -> output.writeInt(this.debugSpeed);
            case 4 -> writeState(output);
            case 5 -> output.writeInt(this.lastSpeed);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 3 -> this.hbm$syncSet$PASourceBlockEntity$debugSpeed(input.readInt());
            case 4 -> readState(input);
            case 5 -> this.hbm$syncSet$PASourceBlockEntity$lastSpeed(input.readInt());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$PASourceBlockEntity$state(PAState next) {
        if (this.state != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.state);
            this.state = next;
            if (bound) bindSyncValue(next, 3, 1L << 4);
            if ((1L << 4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 4);
        }
    }

    public final void hbm$syncSet$PASourceBlockEntity$lastSpeed(int next) {
        if (this.lastSpeed != next) {
            this.lastSpeed = next;
            if ((1L << 5) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 5);
        }
    }

    public final void hbm$syncSet$PASourceBlockEntity$debugSpeed(int next) {
        if (this.debugSpeed != next) {
            this.debugSpeed = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(PASourceBlockEntity.class, "state", 3, 1L << 4);
    }
}
