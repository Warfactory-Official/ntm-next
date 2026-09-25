// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.api.tile.IHeatSource;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.container.MenuMachineHeatex;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Coolable.CoolingType;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockEntityHeaterHeatex extends BlockEntityMachineBase
        implements IHeatSource,
                FluidTankEndpoint,
                MenuProvider,
                IControlReceiver,
                IFluidCopiable,
                IRORValueProvider,
                SyncUnitSchema {

    public static final String[] ROR =
            new String[] {
                PREFIX_VALUE + "hotfluid", PREFIX_VALUE + "coldfluid", PREFIX_VALUE + "heat"
            };

    @SyncField(units = 0b11)
    public final FluidTankNTM[] tanks = new FluidTankNTM[2];

    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;

    @SyncField(units = 1L << 3)
    public int amountToCool = 24_000;

    @SyncField(units = 1L << 4)
    public int tickDelay = 1;

    @SyncField(units = 1L << 2)
    public int heatEnergy;

    @SyncField(units = 1L)
    private int syncInputFill;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityHeaterHeatex(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEATER_HEATEX.get(), pos, state, 1);
        tanks[0] = new FluidTankNTM(NTMFluids.COOLANT_HOT, 24_000);
        tanks[1] = new FluidTankNTM(NTMFluids.COOLANT, 24_000);
        receiving = new FluidTankNTM[] {tanks[0]};
        sending = new FluidTankNTM[] {tanks[1]};
    }

    @Override
    public void tickServer() {
        tanks[0].setType(0, 0, inventory);
        setupTanks();

        heatEnergy = (int) (heatEnergy * 0.999D);

        syncInputFill = tanks[0].getFill();
        tryConvert();
        networkPackNT(25);

        flush.provide((ServerLevel) level, this);
    }

    private void setupTanks() {
        FT_Coolable trait = NTMFluidProperties.getTrait(tanks[0].getTankType(), FT_Coolable.class);
        if (trait != null && trait.getEfficiency(CoolingType.HEATEXCHANGER) > 0) {
            tanks[1].setTankType(trait.coolsTo());
            return;
        }
        tanks[0].setTankType(NTMFluids.NONE);
        tanks[1].setTankType(NTMFluids.NONE);
    }

    private void tryConvert() {
        FT_Coolable trait = NTMFluidProperties.getTrait(tanks[0].getTankType(), FT_Coolable.class);
        if (trait == null) return;
        if (tickDelay < 1) tickDelay = 1;
        if (!TickPhase.every(this, tickDelay)) return;

        int inputOps = tanks[0].getFill() / trait.amountReq;
        int outputOps = (tanks[1].getMaxFill() - tanks[1].getFill()) / trait.amountProduced;
        int ops = Math.min(inputOps, Math.min(outputOps, amountToCool));

        tanks[0].setFill(tanks[0].getFill() - trait.amountReq * ops);
        tanks[1].setFill(tanks[1].getFill() + trait.amountProduced * ops);
        this.heatEnergy +=
                (int) (trait.heatEnergy * ops * trait.getEfficiency(CoolingType.HEATEXCHANGER));
        setChanged();
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
        if (data.contains("toCool")) {
            amountToCool =
                    Mth.clamp(data.getIntOr("toCool", amountToCool), 1, tanks[0].getMaxFill());
        }
        if (data.contains("delay")) {
            tickDelay = Math.max(data.getIntOr("delay", tickDelay), 1);
        }
        setChanged();
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.getEyePosition().distanceToSqr(Vec3.atCenterOf(worldPosition)) < 16 * 16;
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
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    @Override
    public @Nullable FluidTankNTM getTankToPaste() {
        return tanks[0];
    }

    @Override
    public CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag tag = IFluidCopiable.super.getSettings(level, pos);
        tag.putInt("toCool", amountToCool);
        return tag;
    }

    @Override
    public void pasteSettings(
            CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        IFluidCopiable.super.pasteSettings(nbt, index, level, player, pos);
        if (nbt.contains("toCool")) amountToCool = nbt.getIntOr("toCool", amountToCool);
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String provideRORValue(String name) {
        if ((PREFIX_VALUE + "hotfluid").equals(name)) return "" + tanks[0].getFill();
        if ((PREFIX_VALUE + "coldfluid").equals(name)) return "" + tanks[1].getFill();
        if ((PREFIX_VALUE + "heat").equals(name)) return "" + heatEnergy;
        return null;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.heaterHeatex");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineHeatex(containerId, playerInventory, this);
    }

    @Override
    public long syncUnitMask() {
        return 0b1_1111;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> tanks[0].packetSerialize(output, syncInputFill);
            case 1 -> tanks[1].packetSerialize(output);
            case 2 -> output.writeInt(heatEnergy);
            case 3 -> output.writeInt(amountToCool);
            case 4 -> output.writeInt(tickDelay);
            default -> throw new IllegalArgumentException("Invalid machine sync unit");
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> tanks[0].packetDeserialize(input);
            case 1 -> tanks[1].packetDeserialize(input);
            case 2 -> heatEnergy = input.readInt();
            case 3 -> amountToCool = input.readInt();
            case 4 -> tickDelay = input.readInt();
            default -> throw new IllegalArgumentException("Invalid machine sync unit");
        }
    }

    @Override
    public boolean initialMatchesSyncUnits() {
        return false;
    }

    @Override
    public void writeInitialSyncUnit(int unit, ByteBuf output) {
        if (unit == 0) tanks[0].packetSerialize(output);
        else writeSyncUnit(unit, output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("t0").ifPresent(tanks[0]::deserialize);
        input.child("t1").ifPresent(tanks[1]::deserialize);
        heatEnergy = input.getIntOr("heatEnergy", heatEnergy);
        amountToCool = input.getIntOr("toCool", amountToCool);
        tickDelay = input.getIntOr("delay", tickDelay);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tanks[0].serialize(output.child("t0"));
        tanks[1].serialize(output.child("t1"));
        output.putInt("heatEnergy", heatEnergy);
        output.putInt("toCool", amountToCool);
        output.putInt("delay", tickDelay);
    }
}
