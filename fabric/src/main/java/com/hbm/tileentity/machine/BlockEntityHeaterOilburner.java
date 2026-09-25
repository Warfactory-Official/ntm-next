// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.api.tile.IHeatSource;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.container.MenuMachineOilburner;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Flammable;
import com.hbm.inventory.fluid.trait.FluidTrait.FluidReleaseType;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachinePolluting;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityHeaterOilburner extends BlockEntityMachinePolluting
        implements FluidTankEndpoint,
                IHeatSource,
                IControlReceiver,
                IFluidCopiable,
                MenuProvider,
                IRORValueProvider,
                IRORInteractive,
                SyncUnitSchema {

    public static final String[] ROR =
            new String[] {
                PREFIX_VALUE + "heat",
                PREFIX_VALUE + "fuel",
                PREFIX_VALUE + "burnRate",
                PREFIX_VALUE + "state",
                PREFIX_FUNCTION + "setstate" + NAME_SEPARATOR + "active",
                PREFIX_FUNCTION + "setburnrate" + NAME_SEPARATOR + "rate"
            };
    public static final int SLOT_FLUID_IN = 0;
    public static final int SLOT_FLUID_OUT = 1;
    public static final int SLOT_FLUID_ID = 2;

    public static final int MAX_HEAT_ENERGY = 100_000;
    public static final int TANK_CAPACITY = 16_000;

    @SyncField(units = 1L << 0)
    public final FluidTankNTM tank;

    private final FluidTankNTM[] receiving;

    @SyncField(units = 1L << 1)
    public boolean isOn = false;

    @SyncField(units = 1L << 3)
    public int setting = 1;

    @SyncField(units = 1L << 2)
    public int heatEnergy;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityHeaterOilburner(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OILBURNER.get(), pos, state, 3, 100);
        tank = new FluidTankNTM(NTMFluids.HEATINGOIL, TANK_CAPACITY);
        receiving = new FluidTankNTM[] {tank};
    }

    @Override
    public void tickServer() {

        boolean changed = tank.loadTank(SLOT_FLUID_IN, SLOT_FLUID_OUT, inventory);
        changed |= tank.setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory);
        if (changed) setChanged();

        boolean shouldCool = true;

        if (isOn && heatEnergy < MAX_HEAT_ENERGY) {
            FT_Flammable trait =
                    NTMFluidProperties.getTrait(tank.getTankType(), FT_Flammable.class);
            if (trait != null) {
                int burnRate = setting;
                int toBurn = Math.min(burnRate, tank.getFill());
                tank.setFill(tank.getFill() - toBurn);

                int heat = (int) (trait.getHeatEnergy() / 1000L);
                this.heatEnergy += heat * toBurn;

                if (TickPhase.every(this, 5) && toBurn > 0) {
                    pollute(tank.getTankType(), FluidReleaseType.BURN, toBurn * 5);
                }

                shouldCool = false;
            }
        }

        if (this.heatEnergy >= MAX_HEAT_ENERGY) shouldCool = false;

        if (shouldCool) {
            this.heatEnergy = Math.max(this.heatEnergy - Math.max(this.heatEnergy / 1000, 1), 0);
        }

        flush.provide((ServerLevel) level, this);

        networkPackNT(25);
    }

    public void toggleSetting() {
        setting++;
        if (setting > 10) setting = 1;
    }

    @Override
    public int getHeatStored(Level level, BlockPos pos) {
        return BlockMultiblockCore.vendsHeatAt(this, pos) ? heatEnergy : 0;
    }

    @Override
    public void useUpHeat(Level level, BlockPos pos, int heat) {
        if (!BlockMultiblockCore.vendsHeatAt(this, pos)) return;
        this.heatEnergy = Math.max(0, this.heatEnergy - heat);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("toggle")) {
            isOn = !isOn;
            setChanged();
        }
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.getEyePosition()
                        .distanceToSqr(
                                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())
                <= 256.0;
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
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[] {tank};
    }

    @Override
    public @Nullable FluidTankNTM getTankToPaste() {
        return tank;
    }

    @Override
    public CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag tag = IFluidCopiable.super.getSettings(level, pos);
        tag.putInt("burnRate", setting);
        tag.putBoolean("isOn", isOn);
        return tag;
    }

    @Override
    public void pasteSettings(
            CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        IFluidCopiable.super.pasteSettings(nbt, index, level, player, pos);
        if (nbt.contains("isOn")) isOn = nbt.getBooleanOr("isOn", isOn);
        if (nbt.contains("burnRate")) setting = nbt.getIntOr("burnRate", setting);
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String provideRORValue(String name) {
        if ((PREFIX_VALUE + "heat").equals(name)) return "" + heatEnergy;
        if ((PREFIX_VALUE + "fuel").equals(name)) return "" + tank.getFill();
        if ((PREFIX_VALUE + "burnrate").equals(name)) return "" + setting;
        if ((PREFIX_VALUE + "state").equals(name)) return isOn ? "1" : "0";
        return null;
    }

    @Override
    public @Nullable String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setstate").equals(name) && params.length > 0) {
            this.isOn = params[0].equals("1");
            markChanged();
            return null;
        }
        if ((PREFIX_FUNCTION + "setburnrate").equals(name) && params.length > 0) {
            this.setting = IRORInteractive.parseInt(params[0], 1, 10);
            markChanged();
            return null;
        }
        return null;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
            case SLOT_FLUID_IN -> true;
            default -> false;
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.heaterOilburner");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineOilburner(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("tank").ifPresent(tank::deserialize);

        isOn = input.getBooleanOr("isOn", isOn);
        heatEnergy = input.getIntOr("heatEnergy", heatEnergy);
        setting = input.getIntOr("setting", setting);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.serialize(output.child("tank"));
        output.putBoolean("isOn", isOn);
        output.putInt("heatEnergy", heatEnergy);
        output.putInt("setting", setting);
    }

    private void writeSetting(ByteBuf output) {
        output.writeByte(setting);
    }

    private void readSetting(ByteBuf input) {
        setting = input.readByte();
    }

    @Override
    public long syncUnitMask() {
        return 0xfL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> this.tank.packetSerialize(output);
            case 1 -> output.writeBoolean(this.isOn);
            case 2 -> output.writeInt(this.heatEnergy);
            case 3 -> writeSetting(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.tank.packetDeserialize(input);
            case 1 -> this.isOn = input.readBoolean();
            case 2 -> this.heatEnergy = input.readInt();
            case 3 -> readSetting(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
