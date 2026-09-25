// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.MenuMachineLargeTurbine;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Coolable.CoolingType;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.AudioLoop;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineLargeTurbine extends BlockEntityMachineBase
        implements AudioLoop,
                IEnergyHandlerMK2,
                FluidFlushSender,
                MenuProvider,
                IFluidCopiable,
                SyncUnitSchema {

    public static final int SLOT_FLUID_ID = 0;
    public static final int SLOT_FLUID_ID_OUT = 1;
    public static final int SLOT_CONTAINER_IN = 2;
    public static final int SLOT_CONTAINER_OUT = 3;
    public static final int SLOT_BATTERY = 4;
    public static final int SLOT_UNLOAD_IN = 5;
    public static final int SLOT_UNLOAD_OUT = 6;
    public static final int SLOT_COUNT = 7;

    public static final long maxPower = 100_000_000L;
    public static final int inputTankSize = 512_000;
    public static final int outputTankSize = 10_240_000;
    public static final double efficiency = 1.0;
    public static @Nullable Consumer<BlockEntityMachineLargeTurbine> CLIENT_SOUND;

    @SyncField(units = 1L << 2)
    public final FluidTankNTM[] tanks = new FluidTankNTM[2];

    @SyncField(units = 1L << 0)
    public long power;

    @SyncField(units = 1L << 1)
    public boolean operational;

    public float rotor;
    public float lastRotor;
    public float fanAcceleration = 0F;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FluidTankNTM[] sending;

    public BlockEntityMachineLargeTurbine(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INDUSTRIAL_TURBINE.get(), pos, state, SLOT_COUNT);
        tanks[0] = new FluidTankNTM(NTMFluids.STEAM, inputTankSize);
        tanks[1] = new FluidTankNTM(NTMFluids.SPENTSTEAM, outputTankSize);
        sending = new FluidTankNTM[] {tanks[1]};
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineLargeTurbine");
    }

    @Override
    public void tickServer() {
        boolean changed = tanks[0].setType(SLOT_FLUID_ID, SLOT_FLUID_ID_OUT, inventory);
        changed |= tanks[0].loadTank(SLOT_CONTAINER_IN, SLOT_CONTAINER_OUT, inventory);
        if (changed) setChanged();

        power -= ItemEnergyTransfer.insert(this, SLOT_BATTERY, power, false);

        this.power = (long) (this.power * 0.95);

        Fluid in = tanks[0].getTankType();
        boolean valid = false;
        operational = false;
        if (in != null && NTMFluidProperties.hasTrait(in, FT_Coolable.class)) {
            FT_Coolable trait = NTMFluidProperties.getTrait(in, FT_Coolable.class);
            double eff = trait.getEfficiency(CoolingType.TURBINE) * efficiency;
            if (eff > 0) {
                tanks[1].setTankType(trait.coolsTo());
                int inputOps = tanks[0].getFill() / trait.amountReq;
                int outputOps = (tanks[1].getMaxFill() - tanks[1].getFill()) / trait.amountProduced;
                int cap = (int) Math.ceil((tanks[0].getFill() / trait.amountReq) / 5F);
                int ops = Math.min(inputOps, Math.min(outputOps, cap));
                tanks[0].setFill(tanks[0].getFill() - ops * trait.amountReq);
                tanks[1].setFill(tanks[1].getFill() + ops * trait.amountProduced);
                this.power += (long) (ops * trait.heatEnergy * eff);
                valid = true;
                operational = ops > 0;
            }
        }
        if (!valid) tanks[1].setTankType(null);
        if (power > maxPower) power = maxPower;

        if (tanks[1].unloadTank(SLOT_UNLOAD_IN, SLOT_UNLOAD_OUT, inventory)) setChanged();

        flush.provide((ServerLevel) level, this);

        this.networkPackNT(50);
    }

    @Override
    public void tickClient() {
        this.lastRotor = this.rotor;
        this.rotor += this.fanAcceleration;
        if (this.rotor >= 360) {
            this.rotor -= 360;
            this.lastRotor -= 360;
        }
        if (operational) {
            this.fanAcceleration =
                    Mth.clamp(this.fanAcceleration + 0.075F + audioDesync(), 0F, 15F);
        } else {
            this.fanAcceleration = Mth.clamp(this.fanAcceleration - 0.1F, 0F, 15F);
        }
        if (CLIENT_SOUND != null) CLIENT_SOUND.accept(this);
    }

    private float audioDesync() {
        return Mth.positiveModulo(worldPosition.hashCode(), 50) / 1000F;
    }

    public long getPowerScaled(int i) {
        return (power * i) / maxPower;
    }

    @Override
    public AudioWrapper createAudioLoop() {
        float speed = fanAcceleration / 15F;
        return AudioSystem.getLoopedSound(
                ModSounds.LARGE_TURBINE_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                getVolume(0.4F * speed),
                10F,
                0.25F + 0.75F * speed,
                20);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        this.power = Math.max(0L, Math.min(p, maxPower));
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (pressure != tanks[0].getPressure()) return 0L;
        if (!tanks[0].accepts(type)) return 0L;
        return (long) tanks[0].getMaxFill() - tanks[0].getFill();
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (pressure != tanks[0].getPressure()) return amount;
        int accepted = tanks[0].fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
        if (accepted > 0) setChanged();
        return amount - accepted;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        if (!tanks[1].provides(type) || tanks[1].getPressure() != pressure) return 0L;
        return tanks[1].getFill();
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        if (!tanks[1].provides(type) || tanks[1].getPressure() != pressure) return;
        if (tanks[1].drain((int) Math.min(amount, Integer.MAX_VALUE), true) > 0) setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
            case SLOT_CONTAINER_IN, SLOT_UNLOAD_IN -> FluidTankNTM.isFluidContainer(stack);
            case SLOT_BATTERY -> IBatteryItem.isBattery(stack);
            default -> false;
        };
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineLargeTurbine(containerId, playerInventory, this);
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    private void writeTanks(ByteBuf output) {
        tanks[0].packetSerialize(output);
        tanks[1].packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        tanks[0].packetDeserialize(input);
        tanks[1].packetDeserialize(input);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("power").ifPresent(v -> power = v);
        input.child("water").ifPresent(tanks[0]::deserialize);
        input.child("steam").ifPresent(tanks[1]::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        tanks[0].serialize(output.child("water"));
        tanks[1].serialize(output.child("steam"));
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> output.writeBoolean(this.operational);
            case 2 -> writeTanks(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> this.operational = input.readBoolean();
            case 2 -> readTanks(input);
            default -> super.readSyncUnit(unit, input);
        }
    }
}
