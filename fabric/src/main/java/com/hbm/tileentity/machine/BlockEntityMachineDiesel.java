// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MachineData;
import com.hbm.inventory.container.MenuMachineDiesel;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Combustible.FuelGrade;
import com.hbm.inventory.fluid.trait.FT_Combustible;
import com.hbm.inventory.fluid.trait.FluidTrait.FluidReleaseType;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.AudioLoop;
import com.hbm.tileentity.BlockEntityMachinePolluting;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineDiesel extends BlockEntityMachinePolluting
        implements AudioLoop,
                IEnergyHandlerMK2,
                FluidTankEndpoint,
                IControlReceiver,
                MenuProvider,
                IFluidCopiable,
                SyncUnitSchema {

    private boolean redstone;

    public static final int SLOT_FLUID_IN = 0;
    public static final int SLOT_CONTAINER_OUT = 1;
    public static final int SLOT_BATTERY = 2;
    public static final int SLOT_FLUID_ID = 3;
    public static final int SLOT_COUNT = 4;
    public static final int POLLUTION_PERIOD = 5;

    private static final int[] SLOTS_DOWN = {SLOT_CONTAINER_OUT, SLOT_BATTERY};
    private static final int[] SLOTS_UP = {SLOT_FLUID_IN};
    private static final int[] SLOTS_SIDE = {SLOT_BATTERY};
    public static @Nullable Consumer<BlockEntityMachineDiesel> CLIENT_SOUND;

    @SyncField(units = 1L << 3)
    public final FluidTankNTM tank =
            new FluidTankNTM(NTMFluids.DIESEL, MachineData.DIESEL_FUEL_CAP.get());

    private final FluidTankNTM[] receiving;
    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    @SyncField(units = 1L << 0)
    public long power;

    @SyncField(units = 1L << 1)
    public long powerCap = MachineData.DIESEL_MAX_POWER.get();

    @SyncField(units = 1L << 2)
    public boolean isOn;

    @SyncField(units = 1L << 2)
    public boolean wasOn = false;

    public BlockEntityMachineDiesel(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DIESEL_GENERATOR.get(), pos, state, SLOT_COUNT, 100);
        receiving = new FluidTankNTM[] {tank};
    }

    public static long getHEFromFuel(@Nullable Fluid type) {
        if (type == null) return 0;
        FT_Combustible fuel = NTMFluidProperties.getTrait(type, FT_Combustible.class);
        if (fuel != null && fuel.getGrade() != FuelGrade.LOW) {
            double efficiency = MachineData.DIESEL_EFFICIENCY.get().get(fuel.getGrade().ordinal());
            return (long) (fuel.getCombustionEnergy() / 1000L * efficiency);
        }
        return 0;
    }

    @Override
    public void tickServer() {
        wasOn = false;
        flush.provide((ServerLevel) level, this);

        boolean changed = tank.setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory);
        changed |= tank.loadTank(SLOT_FLUID_IN, SLOT_CONTAINER_OUT, inventory);
        if (changed) setChanged();

        powerCap = MachineData.DIESEL_MAX_POWER.get();

        power -= ItemEnergyTransfer.insert(this, SLOT_BATTERY, power, false);

        if (isOn) generate();

        networkPackNT(50);
    }

    public void refreshRedstone() {
        redstone = level.hasNeighborSignal(worldPosition);
    }

    private void generate() {
        if (!isOn) return;
        if (redstone) return;

        if (hasAcceptableFuel() && tank.getFill() > 0) {
            wasOn = true;
            tank.setFill(tank.getFill() - 1);

            if (TickPhase.every(this, POLLUTION_PERIOD)) {
                pollute(tank.getTankType(), FluidReleaseType.BURN, 5F);
            }

            power = Math.min(powerCap, power + getHEFromFuel());
            setChanged();
        }
    }

    @Override
    public void tickClient() {
        if (CLIENT_SOUND != null) CLIENT_SOUND.accept(this);
    }

    public boolean hasAcceptableFuel() {
        return getHEFromFuel(tank.getTankType()) > 0;
    }

    public long getHEFromFuel() {
        return getHEFromFuel(tank.getTankType());
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("turnOn")) isOn = !isOn;
        setChanged();
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(Vec3.atCenterOf(worldPosition)) < 25D * 25D;
    }

    @Override
    public AudioWrapper createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.ENGINE_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                1.0F,
                10F,
                1.0F,
                10);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        power = p;
    }

    @Override
    public long getMaxPower() {
        return MachineData.DIESEL_MAX_POWER.get();
    }

    @Override
    public long getProviderSpeed() {
        return MachineData.DIESEL_MAX_POWER.get();
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return getSmokeTanks();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_FLUID_IN -> tank.containerContent(stack) > 0;
            case SLOT_BATTERY -> IBatteryItem.isBattery(stack);
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? SLOTS_DOWN : (side == Direction.UP ? SLOTS_UP : SLOTS_SIDE);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {

        if (slot == SLOT_CONTAINER_OUT) {
            if (stack.getItem() == ModItems.TANK_STEEL.get()) return true;

            return stack.getItem() == ModItems.CANISTER.get()
                    && ModItems.CANISTER.get().getContent(stack).type() == Fluids.EMPTY;
        }
        if (slot == SLOT_BATTERY)
            return stack.getItem() instanceof IBatteryItem battery
                    && battery.getCharge(stack) >= battery.getMaxCharge(stack);
        return false;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[] {tank};
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineDiesel");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineDiesel(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        redstone = input.getBooleanOr("redstone", false);
        isOn = input.getBooleanOr("isOn", false);
        input.getLong("powerTime").ifPresent(v -> power = v);
        input.getLong("powerCap").ifPresent(v -> powerCap = v);
        input.child("fuel").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("powerTime", power);
        output.putBoolean("isOn", isOn);
        output.putLong("powerCap", powerCap);
        output.putBoolean("redstone", redstone);
        tank.serialize(output.child("fuel"));
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0xfL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> output.writeLong(this.powerCap);
            case 2 -> {
                output.writeBoolean(this.isOn);
                output.writeBoolean(this.wasOn);
            }
            case 3 -> this.tank.packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> this.powerCap = input.readLong();
            case 2 -> {
                this.isOn = input.readBoolean();
                this.wasOn = input.readBoolean();
            }
            case 3 -> this.tank.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }
}
