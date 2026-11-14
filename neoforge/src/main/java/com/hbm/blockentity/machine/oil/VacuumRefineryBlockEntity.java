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

package com.hbm.blockentity.machine.oil;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.PersistentDrop;
import com.hbm.blockentity.SoundEmitter;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.VacuumDistillMenu;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.VacuumRefineryRecipe;
import com.hbm.inventory.recipes.VacuumRefineryRecipes;
import com.hbm.items.ModDataComponents;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.sound.SoundHandle;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class VacuumRefineryBlockEntity extends MachineBlockEntity
        implements FluidTankAccess,
                EnergyHandler,
                FluidTankHandler,
                MenuProvider,
                PersistentDrop,
                FluidSettingsHandler,
                SoundEmitter,
                AudioLoop,
                SyncUnitSchema {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_FEED_IN = FractionColumn.SLOT_FEED_IN;
    public static final int SLOT_FEED_OUT = FractionColumn.SLOT_FEED_OUT;
    public static final int SLOT_FLUID_ID = 11;
    public static final int SLOT_COUNT = 12;

    public static final long MAX_POWER = 1_000_000L;

    private static final long BATCH_POWER = 10_000L;
    private static final int FEED_PRESSURE = 2;
    private static final int LINGER = 20;
    private static final String[] TANK_KEYS = {"input", "heavy", "reformate", "light", "gas"};

    public static Consumer<VacuumRefineryBlockEntity> CLIENT_SOUND = _ -> {};

    public long power;
    public final FluidTankNTM[] tanks;
    public boolean running;

    private final FractionColumn column;
    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private int linger;

    public VacuumRefineryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VACUUUM_DISTILL.get(), pos, state, SLOT_COUNT);
        column =
                new FractionColumn(
                        new FluidTankNTM(NTMFluids.OIL, FractionColumn.FEED_CAPACITY)
                                .withPressure(FEED_PRESSURE),
                        NTMFluids.HEAVYOIL_VACUUM,
                        NTMFluids.REFORMATE,
                        NTMFluids.LIGHTOIL_VACUUM,
                        NTMFluids.SOURGAS);
        tanks = column.tanks;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.vacuumDistill");
    }

    @Override
    public void tickServer() {
        long charged = ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);
        if (charged != 0L) hbm$syncSet$VacuumRefineryBlockEntity$power((long) (power + (charged)));
        boolean changed = column.fillFeed(inventory, SLOT_FLUID_ID);
        boolean ran = refine();
        changed |= column.drainFractions(inventory);
        if (changed) markChanged();
        flush.provide((ServerLevel) level, this);
        if (running != ran) hbm$syncSet$VacuumRefineryBlockEntity$running(ran);
        networkPackNT(150);
    }

    private boolean refine() {
        VacuumRefineryRecipe recipe =
                VacuumRefineryRecipes.INSTANCE.getVacuum(tanks[0].getTankType());
        if (recipe == null) return column.refine(null, false);
        if (!column.refine(recipe.outputFluid, power >= BATCH_POWER)) return false;
        hbm$syncSet$VacuumRefineryBlockEntity$power((long) (power - (BATCH_POWER)));
        markChanged();
        return true;
    }

    @Override
    public void tickClient() {
        CLIENT_SOUND.accept(this);
    }

    @Override
    public boolean tickSound() {
        if (running) linger = LINGER;
        if (linger <= 0) return false;
        linger--;
        return true;
    }

    @Override
    public @Nullable SoundHandle createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.BOILER_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                0.25F,
                15F,
                1.0F,
                20);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long stored) {
        hbm$syncSet$VacuumRefineryBlockEntity$power(Math.clamp(stored, 0L, MAX_POWER));
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return column.feed;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return column.fractions;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        for (FluidTankNTM tank : column.fractions) out.add(tank, FlushFaces.own());
    }

    @Override
    public int tankCount() {
        return tanks.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return tanks[index];
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new VacuumDistillMenu(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$VacuumRefineryBlockEntity$power(input.getLongOr("power", 0L));
        for (int i = 0; i < tanks.length; i++)
            input.child(TANK_KEYS[i]).ifPresent(tanks[i]::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        for (int i = 0; i < tanks.length; i++) tanks[i].serialize(output.child(TANK_KEYS[i]));
    }

    @Override
    public void writePersistent(DataComponentMap.Builder components) {
        if (column.holdsFluid())
            components.set(ModDataComponents.TANK_CONTENTS.get(), FluidStackNTM.snapshot(tanks));
    }

    @Override
    public void readPersistent(DataComponentGetter components) {
        List<FluidStackNTM> contents = components.get(ModDataComponents.TANK_CONTENTS.get());
        if (contents != null) FluidStackNTM.restore(contents, tanks);
    }

    @Override
    public String[] persistentKeys() {
        return TANK_KEYS;
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(power);
            case 1 -> {
                for (FluidTankNTM tank : tanks) tank.packetSerialize(output);
            }
            case 2 -> output.writeBoolean(running);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$VacuumRefineryBlockEntity$power(input.readLong());
            case 1 -> {
                for (FluidTankNTM tank : tanks) tank.packetDeserialize(input);
            }
            case 2 -> hbm$syncSet$VacuumRefineryBlockEntity$running(input.readBoolean());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$VacuumRefineryBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1L << 0) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 0);
        }
    }

    public final FluidTankNTM hbm$syncArraySet$VacuumRefineryBlockEntity$tanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.tanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.tanks) {
                syncArrayChanged(array, index, 3, 1L << 1);
            }
        }
        return next;
    }

    public final void hbm$syncSet$VacuumRefineryBlockEntity$running(boolean next) {
        if (this.running != next) {
            this.running = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(VacuumRefineryBlockEntity.class, "tanks", 3, 1L << 1);
        com.hbm.inventory.container.SyncedData.fields(VacuumRefineryBlockEntity.class, "power");
    }

    public void stripPersistent(ValueOutput output) {
        for (String key : persistentKeys()) output.discard(key);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        writePersistent(components);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        readPersistent(components);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        stripPersistent(output);
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
