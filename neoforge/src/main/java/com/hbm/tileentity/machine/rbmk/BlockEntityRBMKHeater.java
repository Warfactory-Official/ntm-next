// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.MenuRBMKHeater;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityRBMKHeater extends BlockEntityRBMKBase
        implements FluidTankEndpoint, MenuProvider, SyncUnitSchema, IRORValueProvider {
    @SyncField(units = 1L << 4)
    public final FluidTankNTM feed = new FluidTankNTM(NTMFluids.COOLANT, 16_000);

    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;

    @SyncField(units = 1L << 5)
    public final FluidTankNTM steam = new FluidTankNTM(NTMFluids.COOLANT_HOT, 16_000);

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityRBMKHeater(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_HEATER.get(), pos, state, 1);
        receiving = new FluidTankNTM[] {feed};
        sending = new FluidTankNTM[] {steam};
    }

    private static double producedTemperature(@Nullable Fluid fluid) {
        if (fluid == null) return 0D;
        var props = NTMFluidProperties.get(fluid);
        return props != null ? props.temperature() : 0D;
    }

    @Override
    public void tickServer() {
        if ((heat <= 50 || feed.getFill() <= 0) && feed.setType(0, 0, inventory)) markChanged();

        FT_Heatable trait = NTMFluidProperties.getTrait(feed.getTankType(), FT_Heatable.class);
        if (trait != null) {
            FT_Heatable.HeatingStep step = trait.getFirstStep();
            steam.setTankType(step.typeProduced());
            double producedTemp = producedTemperature(steam.getTankType());
            double tempRange = this.heat - producedTemp;
            double eff = trait.getEfficiency(FT_Heatable.HeatingType.HEATEXCHANGER);

            if (tempRange > 0 && eff > 0) {
                double TU_PER_DEGREE = 2_000D * eff;
                int inputOps = feed.getFill() / step.amountReq;
                int outputOps = (steam.getMaxFill() - steam.getFill()) / step.amountProduced;
                int tempOps = (int) Math.floor((tempRange * TU_PER_DEGREE) / step.heatReq);
                int ops = Math.min(inputOps, Math.min(outputOps, tempOps));

                if (ops > 0) {
                    feed.setFill(feed.getFill() - step.amountReq * ops);
                    steam.setFill(steam.getFill() + step.amountProduced * ops);
                    this.heat -= (step.heatReq * ops / TU_PER_DEGREE) * eff;
                    markChanged();
                }
            }

            if (eff <= 0) {
                feed.setTankType(NTMFluids.NONE);
                steam.setTankType(NTMFluids.NONE);
            }
        } else {
            feed.setTankType(NTMFluids.NONE);
            steam.setTankType(NTMFluids.NONE);
        }

        flush.provide((ServerLevel) level, this);
        super.tickServer();
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(steam, COLUMN_OUTPUTS);
    }

    @Override
    public RBMKColumnType getConsoleType() {
        return RBMKColumnType.HEATEX;
    }

    @Override
    public RBMKColumn getConsoleData(RBMKColumn reuse) {
        RBMKColumn.HeaterColumn data = (RBMKColumn.HeaterColumn) super.getConsoleData(reuse);
        data.water = feed.getFill();
        data.maxWater = feed.getMaxFill();
        data.steam = steam.getFill();
        data.maxSteam = steam.getMaxFill();
        data.coldType = RBMKColumn.fluidId(feed.getFluid());
        data.hotType = RBMKColumn.fluidId(steam.getFluid());
        return data;
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
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("feed").ifPresent(feed::deserialize);
        input.child("steam").ifPresent(steam::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        feed.serialize(output.child("feed"));
        steam.serialize(output.child("steam"));
    }

    @Override
    public void writeDiagnostics(CompoundTag tag) {
        super.writeDiagnostics(tag);
        writeDiagnostics(tag, "feed", feed);
        writeDiagnostics(tag, "steam", steam);
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[] {PREFIX_VALUE + "in", PREFIX_VALUE + "out"};
    }

    @Override
    public @Nullable String provideRORValue(String name) {
        if ((PREFIX_VALUE + "in").equals(name)) return "" + feed.getFill();
        if ((PREFIX_VALUE + "out").equals(name)) return "" + steam.getFill();
        return null;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rbmkHeater");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MenuRBMKHeater(id, inv, this);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x30L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 4 -> this.feed.packetSerialize(output);
            case 5 -> this.steam.packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 4 -> this.feed.packetDeserialize(input);
            case 5 -> this.steam.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }
}
