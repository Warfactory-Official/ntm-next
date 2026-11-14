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

package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.container.FELMenu;
import com.hbm.items.machine.EnumWavelengths;
import com.hbm.items.machine.FELCrystalItem;
import com.hbm.main.Polaroid;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.sound.SoundHandle;
import com.hbm.sound.WorldSounds;
import com.hbm.util.ContaminationUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class WavelengthLaserBlockEntity extends MachineBlockEntity
        implements EnergyHandler, MenuProvider, SyncUnitSchema, AudioLoop {
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_CRYSTAL = 1;
    public static final int SLOT_COUNT = 2;
    public static final int RANGE = 24;

    public long energy;
    public EnumWavelengths wavelength;

    {
        hbm$syncSet$WavelengthLaserBlockEntity$wavelength(EnumWavelengths.NULL);
    }

    public boolean enabled;
    public boolean receiverMissing;
    public int reach;
    public int soundDuration;
    private final BlockPos.MutableBlockPos scanPosition = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos receiverPosition = new BlockPos.MutableBlockPos();

    public WavelengthLaserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FEL.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public void tickServer() {
        long priorEnergy = energy;
        var priorWavelength = wavelength;
        int priorReach = reach;
        boolean priorMissing = receiverMissing;
        runServerOperation();
        long changed =
                (energy != priorEnergy ? 1L : 0L)
                        | (wavelength != priorWavelength ? 2L : 0L)
                        | (receiverMissing != priorMissing ? 8L : 0L)
                        | (reach != priorReach ? 16L : 0L);
        if (changed != 0) syncUnitsChanged(1, changed);
        if (energy != priorEnergy || wavelength != priorWavelength || reach != priorReach)
            markChanged();
        networkPackNT(250);
    }

    public void runServerOperation() {
        chargeAndSelectWavelength();
        boolean received = false;
        if (enabled && wavelength != EnumWavelengths.NULL) {
            long demand = workEnergy();
            if (energy < demand) hbm$syncSet$WavelengthLaserBlockEntity$energy(0);
            else {
                var direction = getBlockState().getValue(MultiblockCoreBlock.FACING);
                exposeLiving(direction);
                hbm$syncSet$WavelengthLaserBlockEntity$energy((long) (energy - (demand)));
                received = scan(direction);
            }
        }
        hbm$syncSet$WavelengthLaserBlockEntity$receiverMissing(!received);
    }

    public final void chargeAndSelectWavelength() {
        hbm$syncSet$WavelengthLaserBlockEntity$energy(
                (long)
                        (energy
                                + (ItemEnergyTransfer.extract(
                                        this, SLOT_BATTERY, getMaxPower() - energy, false))));
        var crystal = inventory.get(SLOT_CRYSTAL);
        hbm$syncSet$WavelengthLaserBlockEntity$wavelength(
                enabled && crystal.getItem() instanceof FELCrystalItem selector
                        ? selector.wavelength
                        : EnumWavelengths.NULL);
    }

    public long workEnergy() {
        return wavelength == EnumWavelengths.NULL
                ? 0L
                : (int) (1250D * Math.pow(3D, wavelength.ordinal()));
    }

    public double feedbackThreshold() {
        return 1250D * Math.pow(2D, wavelength.ordinal());
    }

    private void exposeLiving(Direction direction) {
        int distance = reach - 1;
        double x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        double endX = x + direction.getStepX() * distance;
        double endZ = z + direction.getStepZ() * distance;
        var bounds =
                new AABB(
                        Math.min(x, endX) + .2D,
                        y + .2D,
                        Math.min(z, endZ) + .2D,
                        Math.max(x, endX) + .8D,
                        y + 1.8D,
                        Math.max(z, endZ) + .8D);
        var entities =
                level.getEntitiesOfClass(LivingEntity.class, bounds, EntitySelector.NO_SPECTATORS);
        for (int index = 0; index < entities.size(); index++) {
            var entity = entities.get(index);
            if (wavelength == EnumWavelengths.VISIBLE)
                entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 235929600, 0));
            if (wavelength == EnumWavelengths.VISIBLE
                    || wavelength == EnumWavelengths.IR
                    || wavelength == EnumWavelengths.UV) entity.igniteForSeconds(10);
            else if (wavelength == EnumWavelengths.GAMMA)
                ContaminationUtil.contaminate(
                        entity,
                        ContaminationUtil.HazardType.RADIATION,
                        ContaminationUtil.ContaminationType.CREATIVE,
                        25F);
            else if (wavelength == EnumWavelengths.DRX)
                ContaminationUtil.applyDigammaData(entity, .1F);
        }
    }

    private boolean scan(Direction direction) {
        boolean received = false;
        boolean changedReceiver = false;
        for (int step = 3; step < RANGE; step++) {
            scanPosition.set(
                    worldPosition.getX() + direction.getStepX() * step,
                    worldPosition.getY() + 1,
                    worldPosition.getZ() + direction.getStepZ() * step);
            var state = level.getBlockState(scanPosition);
            if (isSeparatorSurface(level, scanPosition, state)) {
                receiverPosition.set(scanPosition).move(direction).move(Direction.DOWN);
                if (level.getBlockEntity(receiverPosition) instanceof SilexBlockEntity receiver) {
                    var receiverFacing =
                            receiver.getBlockState().getValue(MultiblockCoreBlock.FACING);
                    if (orientationCompatible(receiverFacing, direction)
                            && step >= 5
                            && !changedReceiver) {
                        received = true;
                        if (receiver.wavelength() != wavelength) {
                            receiver.supplyWavelength(wavelength);
                            changedReceiver = true;
                        }
                    } else level.destroyBlock(receiverPosition, true);
                }
                continue;
            }
            if (!beamOpaque(state) && !state.is(Blocks.TNT)) {
                hbm$syncSet$WavelengthLaserBlockEntity$reach(RANGE);
                changedReceiver = false;
                continue;
            }
            hbm$syncSet$WavelengthLaserBlockEntity$reach(step);
            affectBlock(scanPosition, state);
            break;
        }
        return received;
    }

    private void affectBlock(BlockPos position, BlockState state) {
        if (state.getBlock() instanceof LiquidBlock) {
            WorldSounds.machine(
                    level, null, position, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1F, 1F);
            level.removeBlock(position, false);
        } else if (state.getBlock().getExplosionResistance() < 75F
                && level.getRandom().nextInt(5) == 0) {
            WorldSounds.machine(
                    level, null, position, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1F, 1F);
            if (wavelength == EnumWavelengths.DRX) {
                var replacement =
                        Polaroid.isBalefireDay()
                                ? ModBlocks.DIGAMMA_MATTER
                                : ModBlocks.FIRE_DIGAMMA;
                level.setBlockAndUpdate(position, replacement.get().defaultBlockState());
                level.setBlockAndUpdate(
                        position.below(), ModBlocks.ASH_DIGAMMA.get().defaultBlockState());
            } else {
                level.setBlockAndUpdate(position, Blocks.FIRE.defaultBlockState());
            }
        }
    }

    public static boolean isSeparatorSurface(Level level, BlockPos position, BlockState state) {
        if (state.is(ModBlocks.MACHINE_SILEX.get())) return true;
        if (!MultiblockSurface.isFoldedCell(state)) return false;
        var owner = MultiblockSurface.coreOfAny(level, position, state);
        return owner != null && level.getBlockState(owner).is(ModBlocks.MACHINE_SILEX.get());
    }

    public static boolean orientationCompatible(Direction receiver, Direction emitter) {
        return receiver == emitter || receiver == emitter.getOpposite();
    }

    public static boolean beamOpaque(BlockState state) {
        return MultiblockSurface.isSurface(state)
                || state.blocksMotion()
                        && state.canOcclude()
                        && state.instrument() != NoteBlockInstrument.HAT
                        && !state.is(Blocks.GLOWSTONE);
    }

    @Override
    public void tickClient() {
        soundDuration = Math.clamp(soundDuration + (tickSound() ? 2 : -3), 0, 60);
        audioLoop(soundDuration > 10, 2F, .5F + (soundDuration - 10) / 100F);
    }

    public boolean tickSound() {
        return enabled
                && wavelength != EnumWavelengths.NULL
                && reach > 3
                && energy > feedbackThreshold();
    }

    @Override
    public @Nullable SoundHandle createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.FEL_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX() + .5F,
                worldPosition.getY() + .5F,
                worldPosition.getZ() + .5F,
                2F,
                10F,
                2F);
    }

    public void toggleFromControl() {
        hbm$syncSet$WavelengthLaserBlockEntity$enabled(!enabled);
        markChanged();
    }

    @Override
    public long getPower() {
        return energy;
    }

    @Override
    public void setPower(long stored) {
        hbm$syncSet$WavelengthLaserBlockEntity$energy(Math.clamp(stored, 0L, getMaxPower()));
    }

    @Override
    public long getMaxPower() {
        return 20_000_000L;
    }

    public long getPowerScaled(long scale) {
        return energy * scale / getMaxPower();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_BATTERY
                || slot == SLOT_CRYSTAL && stack.getItem() instanceof FELCrystalItem;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineFEL");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FELMenu(id, inventory, this);
    }

    @Override
    protected boolean syncMuffled() {
        return true;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$WavelengthLaserBlockEntity$energy(input.getLongOr("power", 0L));
        hbm$syncSet$WavelengthLaserBlockEntity$wavelength(
                EnumWavelengths.byName(input.getStringOr("mode", "NULL")));
        hbm$syncSet$WavelengthLaserBlockEntity$enabled(input.getBooleanOr("isOn", false));
        hbm$syncSet$WavelengthLaserBlockEntity$reach(input.getIntOr("distance", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", energy);
        output.putString("mode", wavelength.name());
        output.putBoolean("isOn", enabled);
        output.putInt("distance", reach);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 31L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(energy);
            case 1 -> output.writeInt(wavelength.ordinal());
            case 2 -> output.writeBoolean(enabled);
            case 3 -> output.writeBoolean(receiverMissing);
            case 4 -> output.writeInt(reach);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$WavelengthLaserBlockEntity$energy(input.readLong());
            case 1 ->
                    hbm$syncSet$WavelengthLaserBlockEntity$wavelength(
                            EnumWavelengths.values().get(input.readInt()));
            case 2 -> hbm$syncSet$WavelengthLaserBlockEntity$enabled(input.readBoolean());
            case 3 -> hbm$syncSet$WavelengthLaserBlockEntity$receiverMissing(input.readBoolean());
            case 4 -> hbm$syncSet$WavelengthLaserBlockEntity$reach(input.readInt());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$WavelengthLaserBlockEntity$energy(long next) {
        if (this.energy != next) {
            this.energy = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final void hbm$syncSet$WavelengthLaserBlockEntity$wavelength(EnumWavelengths next) {
        if (this.wavelength != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.wavelength);
            this.wavelength = next;
            if (bound) bindSyncValue(next, 3, 2);
            if ((2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2);
        }
    }

    public final void hbm$syncSet$WavelengthLaserBlockEntity$enabled(boolean next) {
        if (this.enabled != next) {
            this.enabled = next;
            if ((4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4);
        }
    }

    public final void hbm$syncSet$WavelengthLaserBlockEntity$receiverMissing(boolean next) {
        if (this.receiverMissing != next) {
            this.receiverMissing = next;
            if ((8) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 8);
        }
    }

    public final void hbm$syncSet$WavelengthLaserBlockEntity$reach(int next) {
        if (this.reach != next) {
            this.reach = next;
            if ((16) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 16);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(WavelengthLaserBlockEntity.class, "wavelength", 3, 2);
        com.hbm.inventory.container.SyncedData.fields(WavelengthLaserBlockEntity.class, "energy");
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
