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

package com.hbm.blockentity.network.pneumatic;

import com.hbm.api.fluidmk2.PlainFluidReceiver;
import com.hbm.api.ntl.PneumaticNetwork;
import com.hbm.blockentity.ChunkPublication;
import com.hbm.blockentity.GhostFilters;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.PneumoTubeMenu;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.module.ModulePatternMatcher;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class PneumaticTubeBlockEntity extends PneumaticMachineBaseBlockEntity
        implements PlainFluidReceiver, FluidTankAccess, GhostFilters, SyncUnitSchema {

    public static final int SLOT_COUNT = 15;
    private static final Window FILTERS = new Window(0, SLOT_COUNT);

    public final FluidTankNTM gasTank = new FluidTankNTM(NTMFluids.AIR, 4000).withPressure(1);
    public final ModulePatternMatcher filterPatterns = new ModulePatternMatcher(SLOT_COUNT);
    public boolean requirePower;
    public boolean matchOnly;
    public byte sourceOrder;
    public byte destinationOrder;
    public @Nullable Direction intakeFace;
    public @Nullable Direction outletFace;
    public byte airFaces;
    public int sendCursor;
    private boolean powerSeen;
    private int audibleDelay;

    public PneumaticTubeBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.PNEUMATIC_TUBE.get(), pos, state);
    }

    protected PneumaticTubeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, SLOT_COUNT);
        gasTank.bindOwner(this);
    }

    public boolean hasIntake() {
        return intakeFace != null;
    }

    public boolean hasOutlet() {
        return outletFace != null;
    }

    public void refreshPower() {
        if (level instanceof ServerLevel server) observePower(server);
    }

    private void observePower(ServerLevel server) {
        boolean observed =
                ChunkPublication.signals(server, worldPosition).hasNeighborSignal(worldPosition);
        if (observed == powerSeen) return;
        powerSeen = observed;
        setChanged();
    }

    @Override
    public void onGraphLoad(ServerLevel server) {
        observePower(server);
    }

    @Override
    public void tickServer() {
        super.tickServer();
        if (audibleDelay > 0) audibleDelay--;
        if (!hasIntake() && !hasOutlet()) return;
        var server = (ServerLevel) level;
        PneumaticNetwork.setEndpoint(server, worldPosition, hasOutlet());
        if (hasIntake()
                && requirePower == powerSeen
                && TickPhase.every(this, 5)
                && gasTank.getFill() >= 50) {
            var network = net();
            if (network != null) {
                var source = worldPosition.relative(intakeFace);
                var sourceFace = intakeFace.getOpposite();
                if (PneumaticNetwork.presentAt(server, source, sourceFace)) {
                    boolean moved =
                            network.send(
                                    server,
                                    source,
                                    sourceFace,
                                    this,
                                    sourceOrder,
                                    destinationOrder,
                                    rangeForPressure(gasTank.getPressure()),
                                    sendCursor);
                    sendCursor++;
                    if (moved) {
                        gasTank.drain(50, true);
                        if (audibleDelay <= 0 && !isMuffled()) {
                            WorldSounds.machine(
                                    server,
                                    null,
                                    worldPosition.getX() + 0.5,
                                    worldPosition.getY() + 0.5,
                                    worldPosition.getZ() + 0.5,
                                    ModSounds.TUBE_FWOOMP.get(),
                                    SoundSource.BLOCKS,
                                    0.25F,
                                    0.9F + server.getRandom().nextFloat() * 0.2F);
                            audibleDelay = 20;
                        }
                    }
                    setChanged();
                }
            }
        }
        networkPackNT(15);
    }

    public static int rangeForPressure(int pressure) {
        return switch (pressure) {
            case 1 -> 10;
            case 2 -> 25;
            case 3 -> 100;
            case 4 -> 250;
            case 5 -> 1000;
            default -> 0;
        };
    }

    public static int locationKey(BlockPos pos) {
        return pos.getX() + 27644437 * (pos.getY() + pos.getZ() * 27644437);
    }

    public static int faceCode(@Nullable Direction face) {
        return face == null ? 6 : face.ordinal();
    }

    public static @Nullable Direction faceFromCode(int code) {
        return code >= 0 && code < 6 ? Direction.from3DDataValue(code) : null;
    }

    public boolean filterMatches(ItemStack offered) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            var filter = inventory.get(slot);
            if (!filter.isEmpty() && filterPatterns.isValidForFilter(filter, slot, offered))
                return true;
        }
        return false;
    }

    @Override
    public Window filterWindow() {
        return FILTERS;
    }

    @Override
    public void advanceFilterMode(int slot) {
        filterPatterns.nextMode(level, getItem(slot), slot);
        setChanged();
    }

    public void initializeFilter(ItemStack stack, int slot) {
        filterPatterns.initPatternSmart(level, stack, slot);
        setChanged();
    }

    public void toggleMatchPolicy() {
        hbm$syncSet$PneumaticTubeBlockEntity$matchOnly(!matchOnly);
        setChanged();
    }

    public void togglePowerGate() {
        hbm$syncSet$PneumaticTubeBlockEntity$requirePower(!requirePower);
        setChanged();
    }

    public void advancePressure() {
        gasTank.withPressure(gasTank.getPressure() % 5 + 1);
        setChanged();
    }

    public void advanceSourceOrder() {
        hbm$syncSet$PneumaticTubeBlockEntity$sourceOrder((byte) ((sourceOrder + 1) % 3));
        setChanged();
    }

    public void advanceDestinationOrder() {
        hbm$syncSet$PneumaticTubeBlockEntity$destinationOrder((byte) ((destinationOrder + 1) % 2));
        setChanged();
    }

    public boolean acceptsFluid(Fluid fluid, Direction face) {
        return hasIntake() && face != intakeFace && face != outletFace && gasTank.joins(fluid);
    }

    @Override
    public boolean canReceiveFluid(Fluid fluid) {
        return gasTank.joins(fluid);
    }

    @Override
    public void forEachTank(Consumer<FluidTankNTM> visitor) {
        visitor.accept(gasTank);
    }

    @Override
    public int tankCount() {
        return 1;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return switch (index) {
            case 0 -> gasTank;
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    @Override
    public long getDemand(Fluid fluid, int pressure) {
        return pressure == gasTank.getPressure() && gasTank.accepts(fluid)
                ? gasTank.getMaxFill() - gasTank.getFill()
                : 0;
    }

    @Override
    public long transferFluid(Fluid fluid, int pressure, long amount) {
        long accepted = Math.min(amount, getDemand(fluid, pressure));
        if (accepted <= 0) return amount;
        gasTank.receive(fluid, (int) accepted);
        setChanged();
        return amount - accepted;
    }

    @Override
    public int receivingPressureMask(Fluid fluid) {
        return 1 << gasTank.getPressure();
    }

    @Override
    public long getReceiverSpeed(Fluid fluid, int pressure) {
        return Math.clamp((gasTank.getMaxFill() - gasTank.getFill()) / 25, 1, 100);
    }

    public void setAirFaces(int faces) {
        byte next = (byte) faces;
        if (airFaces == next) return;
        hbm$syncSet$PneumaticTubeBlockEntity$airFaces(next);
        setChanged();
        redrawNozzles();
        if (level instanceof ServerLevel) syncToTracking();
    }

    private void redrawNozzles() {
        if (level == null || !level.isClientSide()) return;
        var state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, 0);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.pneumoTube");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new PneumoTubeMenu(id, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        var previousIntake = intakeFace;
        var previousOutlet = outletFace;
        byte previousAir = airFaces;
        hbm$syncSet$PneumaticTubeBlockEntity$intakeFace(
                faceFromCode(input.getByteOr("insertionDir", (byte) 6)));
        hbm$syncSet$PneumaticTubeBlockEntity$outletFace(
                faceFromCode(input.getByteOr("ejectionDir", (byte) 6)));
        hbm$syncSet$PneumaticTubeBlockEntity$airFaces(input.getByteOr("airMask", (byte) 0));
        input.child("tank").ifPresent(gasTank::deserialize);
        filterPatterns.load(input);
        hbm$syncSet$PneumaticTubeBlockEntity$sourceOrder(input.getByteOr("sendOrder", (byte) 0));
        hbm$syncSet$PneumaticTubeBlockEntity$destinationOrder(
                input.getByteOr("receiveOrder", (byte) 0));
        sendCursor = input.getIntOr("sendCounter", 0);
        hbm$syncSet$PneumaticTubeBlockEntity$matchOnly(input.getBooleanOr("whitelist", false));
        hbm$syncSet$PneumaticTubeBlockEntity$requirePower(input.getBooleanOr("redstone", false));
        powerSeen = input.getBooleanOr("powered", false);
        if (previousIntake != intakeFace || previousOutlet != outletFace || previousAir != airFaces)
            redrawNozzles();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putByte("insertionDir", (byte) faceCode(intakeFace));
        output.putByte("ejectionDir", (byte) faceCode(outletFace));
        output.putByte("airMask", airFaces);
        gasTank.serialize(output.child("tank"));
        filterPatterns.save(output);
        output.putByte("sendOrder", sourceOrder);
        output.putByte("receiveOrder", destinationOrder);
        output.putInt("sendCounter", sendCursor);
        output.putBoolean("whitelist", matchOnly);
        output.putBoolean("redstone", requirePower);
        output.putBoolean("powered", powerSeen);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 127;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> gasTank.packetSerialize(output);
            case 1 -> filterPatterns.serialize(output);
            case 2 -> output.writeBoolean(requirePower);
            case 3 -> output.writeBoolean(matchOnly);
            case 4 -> output.writeByte(sourceOrder);
            case 5 -> output.writeByte(destinationOrder);
            case 6 -> {
                output.writeByte(faceCode(intakeFace));
                output.writeByte(faceCode(outletFace));
                output.writeByte(airFaces);
            }
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> gasTank.packetDeserialize(input);
            case 1 -> filterPatterns.deserialize(input);
            case 2 -> hbm$syncSet$PneumaticTubeBlockEntity$requirePower(input.readBoolean());
            case 3 -> hbm$syncSet$PneumaticTubeBlockEntity$matchOnly(input.readBoolean());
            case 4 -> hbm$syncSet$PneumaticTubeBlockEntity$sourceOrder(input.readByte());
            case 5 -> hbm$syncSet$PneumaticTubeBlockEntity$destinationOrder(input.readByte());
            case 6 -> {
                var intake = faceFromCode(input.readByte());
                var outlet = faceFromCode(input.readByte());
                byte air = input.readByte();
                boolean changed = intakeFace != intake || outletFace != outlet || airFaces != air;
                hbm$syncSet$PneumaticTubeBlockEntity$intakeFace(intake);
                hbm$syncSet$PneumaticTubeBlockEntity$outletFace(outlet);
                hbm$syncSet$PneumaticTubeBlockEntity$airFaces(air);
                if (changed) redrawNozzles();
            }
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$PneumaticTubeBlockEntity$requirePower(boolean next) {
        if (this.requirePower != next) {
            this.requirePower = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$PneumaticTubeBlockEntity$matchOnly(boolean next) {
        if (this.matchOnly != next) {
            this.matchOnly = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    public final void hbm$syncSet$PneumaticTubeBlockEntity$sourceOrder(byte next) {
        if (this.sourceOrder != next) {
            this.sourceOrder = next;
            if ((1L << 4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 4);
        }
    }

    public final void hbm$syncSet$PneumaticTubeBlockEntity$destinationOrder(byte next) {
        if (this.destinationOrder != next) {
            this.destinationOrder = next;
            if ((1L << 5) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 5);
        }
    }

    public final void hbm$syncSet$PneumaticTubeBlockEntity$intakeFace(Direction next) {
        if (this.intakeFace != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.intakeFace);
            this.intakeFace = next;
            if (bound) bindSyncValue(next, 3, 1L << 6);
            if ((1L << 6) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 6);
        }
    }

    public final void hbm$syncSet$PneumaticTubeBlockEntity$outletFace(Direction next) {
        if (this.outletFace != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.outletFace);
            this.outletFace = next;
            if (bound) bindSyncValue(next, 3, 1L << 6);
            if ((1L << 6) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 6);
        }
    }

    public final void hbm$syncSet$PneumaticTubeBlockEntity$airFaces(byte next) {
        if (this.airFaces != next) {
            this.airFaces = next;
            if ((1L << 6) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 6);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(PneumaticTubeBlockEntity.class, "gasTank", 3, 1L);
        com.hbm.packet.SyncBindings.field(
                PneumaticTubeBlockEntity.class, "filterPatterns", 3, 1L << 1);
        com.hbm.packet.SyncBindings.field(PneumaticTubeBlockEntity.class, "intakeFace", 3, 1L << 6);
        com.hbm.packet.SyncBindings.field(PneumaticTubeBlockEntity.class, "outletFace", 3, 1L << 6);
    }
}
