// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.oil;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.ContainerSync;
import com.hbm.inventory.container.MenuMachineVacuumDistill;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.VacuumRefineryRecipe;
import com.hbm.inventory.recipes.VacuumRefineryRecipes;
import com.hbm.items.ModDataComponents;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.AudioLoop;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.PersistentDrop;
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

public class BlockEntityMachineVacuumDistill extends BlockEntityMachineBase
        implements AudioLoop,
                IEnergyHandlerMK2,
                FluidTankEndpoint,
                MenuProvider,
                PersistentDrop,
                IFluidCopiable,
                SyncUnitSchema {

    public static final int SLOT_BATTERY = 0;

    public static final int SLOT_CANISTER_DEAD_IN = 1;
    public static final int SLOT_CANISTER_DEAD_OUT = 2;

    public static final int SLOT_FLUID_ID = 11;
    public static final int SLOT_COUNT = 12;

    public static final long MAX_POWER = 1_000_000L;
    public static final int TANK_CAPACITY_INPUT = 64_000;
    public static final int TANK_CAPACITY_OUTPUT = 24_000;
    private static final long REFINE_POWER_COST = 10_000L;
    private static final int REFINE_FLUID_COST = 100;
    private static final String[] TANK_KEYS = {"input", "heavy", "reformate", "light", "gas"};

    public static Consumer<BlockEntityMachineVacuumDistill> CLIENT_SOUND = be -> {};

    @SyncField(units = 1L << 1)
    public final FluidTankNTM[] tanks = new FluidTankNTM[5];

    @SyncField(units = 1L << 0)
    @ContainerSync
    public long power;

    @SyncField(units = 1L << 2)
    public boolean isOn;

    public int audioTime;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;

    public BlockEntityMachineVacuumDistill(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VACUUUM_DISTILL.get(), pos, state, SLOT_COUNT);
        tanks[0] = new FluidTankNTM(NTMFluids.OIL, TANK_CAPACITY_INPUT).withPressure(2);
        tanks[1] = new FluidTankNTM(NTMFluids.HEAVYOIL_VACUUM, TANK_CAPACITY_OUTPUT);
        tanks[2] = new FluidTankNTM(NTMFluids.REFORMATE, TANK_CAPACITY_OUTPUT);
        tanks[3] = new FluidTankNTM(NTMFluids.LIGHTOIL_VACUUM, TANK_CAPACITY_OUTPUT);
        tanks[4] = new FluidTankNTM(NTMFluids.SOURGAS, TANK_CAPACITY_OUTPUT);
        receiving = new FluidTankNTM[] {tanks[0]};
        sending = new FluidTankNTM[] {tanks[1], tanks[2], tanks[3], tanks[4]};
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.vacuumDistill");
    }

    @Override
    public void tickClient() {
        CLIENT_SOUND.accept(this);
    }

    @Override
    public AudioWrapper createAudioLoop() {

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
    public void setPower(long p) {
        power = Math.max(0L, Math.min(p, MAX_POWER));
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public void tickServer() {
        isOn = false;

        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);
        boolean changed = tanks[0].setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory);

        changed |= tanks[0].loadTank(SLOT_CANISTER_DEAD_IN, SLOT_CANISTER_DEAD_OUT, inventory);

        refine();

        for (int i = 1; i < 5; i++) changed |= tanks[i].unloadTank(1 + i * 2, 2 + i * 2, inventory);
        if (changed) setChanged();

        flush.provide((ServerLevel) level, this);

        networkPackNT(150);
    }

    private void refine() {
        VacuumRefineryRecipe recipe =
                VacuumRefineryRecipes.INSTANCE.getVacuum(tanks[0].getTankType());
        if (recipe == null) {
            for (int i = 1; i < 5; i++) tanks[i].setTankType(null);
            return;
        }

        FluidStackNTM[] outputs = recipe.outputFluid;
        for (int i = 0; i < outputs.length; i++) tanks[i + 1].setTankType(outputs[i].type());

        if (power < REFINE_POWER_COST || tanks[0].getFill() < REFINE_FLUID_COST) return;
        for (int i = 0; i < outputs.length; i++) {
            if (tanks[i + 1].getFill() + outputs[i].amount() > tanks[i + 1].getMaxFill()) return;
        }

        isOn = true;
        power -= REFINE_POWER_COST;
        tanks[0].setFill(tanks[0].getFill() - REFINE_FLUID_COST);
        for (int i = 0; i < outputs.length; i++) {
            tanks[i + 1].setFill((int) (tanks[i + 1].getFill() + outputs[i].amount()));
        }
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineVacuumDistill(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("power").ifPresent(v -> power = v);
        for (int i = 0; i < 5; i++) {
            int idx = i;
            input.child(TANK_KEYS[i]).ifPresent(t -> tanks[idx].deserialize(t));
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        for (int i = 0; i < 5; i++) tanks[i].serialize(output.child(TANK_KEYS[i]));
    }

    @Override
    public void writePersistent(DataComponentMap.Builder components) {
        for (FluidTankNTM tank : tanks) {
            if (tank.getFill() > 0) {
                components.set(
                        ModDataComponents.TANK_CONTENTS.get(), FluidStackNTM.snapshot(tanks));
                return;
            }
        }
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

    private void writeTanks(ByteBuf output) {
        for (int i = 0; i < 5; i++) tanks[i].packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        for (int i = 0; i < 5; i++) tanks[i].packetDeserialize(input);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> writeTanks(output);
            case 2 -> output.writeBoolean(this.isOn);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> readTanks(input);
            case 2 -> this.isOn = input.readBoolean();
            default -> super.readSyncUnit(unit, input);
        }
    }
}
