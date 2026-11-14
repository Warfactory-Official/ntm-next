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

package com.hbm.blockentity.machine.fusion;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.SoundEmitter;
import com.hbm.blockentity.machine.fusion.FusionPorts;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.inventory.container.FusionKlystronMenu;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.platform.BlockLookupCache;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.sound.SoundHandle;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class FusionKlystronBlockEntity extends MachineBlockEntity
        implements SoundEmitter,
                AudioLoop,
                EnergyHandler,
                FluidTankHandler,
                MenuProvider,
                SyncUnitSchema {
    public static final long MAX_OUTPUT = 1_000_000;
    public static final int AIR_CONSUMPTION = 2_500;
    public static final float FAN_ACCELERATION = 0.125F;
    public static Consumer<FusionKlystronBlockEntity> CLIENT_SOUND = be -> {};
    public final FluidTankNTM compair;
    private final FluidTankNTM[] receiving;
    public long outputTarget;
    public long output;
    public long power;
    public long maxPower;
    public float fan;
    public float prevFan;
    public float fanSpeed;
    private @Nullable Direction peerFacing;
    private @Nullable BlockLookupCache<BlockEntity> torusPeer;

    public FusionKlystronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_KLYSTRON.get(), pos, state, 1);
        compair = new FluidTankNTM(NTMFluids.AIR, AIR_CONSUMPTION * 60);
        receiving = new FluidTankNTM[] {compair};
    }

    public static List<FusionPorts.Port> links(BlockPos core, Direction facing) {
        Direction dir = facing.getOpposite();
        return List.of(
                new FusionPorts.Port(
                        FusionPorts.Kind.KLYSTRON,
                        core.offset(dir.getStepX() * 4, 2, dir.getStepZ() * 4),
                        dir));
    }

    public static boolean provideKyU(BlockLookupCache<BlockEntity> peer, long output) {
        if (!(peer.find() instanceof FusionTorusBlockEntity torus)) return false;
        torus.hbm$syncSet$FusionTorusBlockEntity$klystronEnergy(
                (long) (torus.klystronEnergy + (output)));
        return true;
    }

    @Override
    public void tickServer() {
        hbm$syncSet$FusionKlystronBlockEntity$maxPower(Math.max(1_000_000L, outputTarget * 100L));
        hbm$syncSet$FusionKlystronBlockEntity$power(
                (long) (power + (ItemEnergyTransfer.extract(this, 0, maxPower - power, false))));

        hbm$syncSet$FusionKlystronBlockEntity$output(0);

        double powerFactor = FusionTorusBlockEntity.getSpeedScaled(maxPower, power);
        double airFactor =
                FusionTorusBlockEntity.getSpeedScaled(compair.getMaxFill(), compair.getFill());
        double factor = Math.min(powerFactor, airFactor);

        long powerReq = (long) Math.ceil(outputTarget * factor);
        int airReq = (int) Math.ceil(AIR_CONSUMPTION * factor);

        if (outputTarget > 0 && power >= powerReq && compair.getFill() >= airReq) {
            hbm$syncSet$FusionKlystronBlockEntity$output(powerReq);
            hbm$syncSet$FusionKlystronBlockEntity$power((long) (power - (powerReq)));
            compair.setFill(compair.getFill() - airReq);
            if (powerReq > 0) markChanged();
        }

        if (output < outputTarget / 50) hbm$syncSet$FusionKlystronBlockEntity$output(0);

        var facing = facing();
        var peer = torusPeer;
        if (peer == null || peerFacing != facing) {
            torusPeer =
                    peer =
                            FusionPorts.cache(
                                    (ServerLevel) level, links(worldPosition, facing).getFirst());
            peerFacing = facing;
        }
        provideKyU(peer, output);

        networkPackNT(100);
    }

    @Override
    public void tickClient() {
        CLIENT_SOUND.accept(this);

        tickSound();

        prevFan = fan;
        fan += fanSpeed;

        if (fan >= 360F) {
            fan -= 360F;
            prevFan -= 360F;
        }
    }

    private Direction facing() {
        return MultiblockCoreBlock.coreFacing(getBlockState());
    }

    @Override
    public SoundHandle createAudioLoop() {
        float speed = fanSpeed / 5F;
        return AudioSystem.getLoopedSound(
                ModSounds.FEL_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX() + 0.5F,
                worldPosition.getY() + 2.5F,
                worldPosition.getZ() + 0.5F,
                getVolume(speed),
                15F,
                speed,
                20);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.hbm$syncSet$FusionKlystronBlockEntity$power(power);
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }

    @Override
    public int tankCount() {
        return receiving.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return receiving[index];
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    public void setOutputTarget(long amount) {
        hbm$syncSet$FusionKlystronBlockEntity$outputTarget(Mth.clamp(amount, 0L, MAX_OUTPUT));
        setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("power").ifPresent(v -> hbm$syncSet$FusionKlystronBlockEntity$power(v));
        input.getLong("maxPower").ifPresent(v -> hbm$syncSet$FusionKlystronBlockEntity$maxPower(v));
        input.getLong("outputTarget")
                .ifPresent(v -> hbm$syncSet$FusionKlystronBlockEntity$outputTarget(v));
        input.child("t").ifPresent(compair::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putLong("maxPower", maxPower);
        output.putLong("outputTarget", outputTarget);
        compair.serialize(output.child("t"));
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.fusionKlystron");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new FusionKlystronMenu(containerId, playerInventory, this);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x1fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> output.writeLong(this.maxPower);
            case 2 -> output.writeLong(this.outputTarget);
            case 3 -> output.writeLong(this.output);
            case 4 -> this.compair.packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.hbm$syncSet$FusionKlystronBlockEntity$power(input.readLong());
            case 1 -> this.hbm$syncSet$FusionKlystronBlockEntity$maxPower(input.readLong());
            case 2 -> this.hbm$syncSet$FusionKlystronBlockEntity$outputTarget(input.readLong());
            case 3 -> this.hbm$syncSet$FusionKlystronBlockEntity$output(input.readLong());
            case 4 -> this.compair.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    public boolean tickSound() {
        double mult = FusionTorusBlockEntity.getSpeedScaled(outputTarget, output);
        if (output > 0) fanSpeed += FAN_ACCELERATION * mult;
        else fanSpeed -= FAN_ACCELERATION;

        fanSpeed = Mth.clamp(fanSpeed, 0F, 5F * (float) mult);
        return fanSpeed > 0F;
    }

    public final void hbm$syncSet$FusionKlystronBlockEntity$outputTarget(long next) {
        if (this.outputTarget != next) {
            this.outputTarget = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$FusionKlystronBlockEntity$output(long next) {
        if (this.output != next) {
            this.output = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    public final void hbm$syncSet$FusionKlystronBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1L << 0) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 0);
        }
    }

    public final void hbm$syncSet$FusionKlystronBlockEntity$maxPower(long next) {
        if (this.maxPower != next) {
            this.maxPower = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(FusionKlystronBlockEntity.class, "compair", 3, 1L << 4);
    }

    private boolean hbm$soundActive;

    public boolean soundStarted(boolean active) {
        boolean started = active && !hbm$soundActive;
        hbm$soundActive = active;
        return started;
    }

    private SoundHandle hbm$audio;

    public void audioLoop(boolean running, float volume) {
        hbm$audio = AudioLoop.loop(this, hbm$audio, running, volume, 0F, false);
    }

    public void audioLoop(boolean running, float volume, float pitch) {
        hbm$audio = AudioLoop.loop(this, hbm$audio, running, volume, pitch, true);
    }

    private void hbm$audioRemove() {
        audioLoop(false, 0F);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        hbm$audioRemove();
    }
}
