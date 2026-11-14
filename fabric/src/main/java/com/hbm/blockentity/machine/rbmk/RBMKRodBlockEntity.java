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

package com.hbm.blockentity.machine.rbmk;

import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.blockentity.machine.rbmk.CraneLoadable;
import com.hbm.blockentity.machine.rbmk.FluxAbsorber;
import com.hbm.blockentity.machine.rbmk.FluxRole;
import com.hbm.blockentity.machine.rbmk.NeutronSpeed;
import com.hbm.blockentity.machine.rbmk.RbmkRay;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.rbmk.RBMKBlock;
import com.hbm.blocks.machine.rbmk.RBMKRodBlock;
import com.hbm.data.MachineData;
import com.hbm.entity.projectile.RBMKDebrisEntity;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.inventory.container.RBMKRodMenu;
import com.hbm.items.ModItems;
import com.hbm.items.machine.RBMKRodItem;
import com.hbm.packet.PacketWire;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.packet.layout.EffectGasFlameVentLayout;
import com.hbm.saveddata.satellites.SatelliteRayEvents;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import static com.hbm.api.redstoneoverradio.RadioCommandParser.VAL;

public class RBMKRodBlockEntity extends RBMKBaseBlockEntity
        implements FluxAbsorber, CraneLoadable, MenuProvider, SyncUnitSchema, RadioValueProvider {
    public static final Vec3[] fluxDirs = {
        new Vec3(0, 0, -1), new Vec3(1, 0, 0), new Vec3(0, 0, 1), new Vec3(-1, 0, 0)
    };
    public double fluxFastRatio;
    public double fluxQuantity;
    public double lastFluxQuantity;
    public double lastFluxRatio;
    public double lastFluxOut;
    public boolean hasRod;
    public int rodColor;

    {
        hbm$syncSet$RBMKRodBlockEntity$rodColor(0);
    }

    public RBMKRodBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.RBMK_ROD.get(), pos, state);
    }

    protected RBMKRodBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1);
    }

    @Override
    public boolean isModerated() {
        return getBlockState().getBlock() instanceof RBMKRodBlock rod && rod.moderated;
    }

    @Override
    public int trackingRange() {
        return 25;
    }

    @Override
    public void absorbFlux(double flux, double fastRatio) {
        double fastFlux = this.fluxQuantity * this.fluxFastRatio;
        double fastFluxIn = flux * fastRatio;
        this.fluxQuantity += flux;
        fluxFastRatio = (fastFlux + fastFluxIn) / fluxQuantity;
    }

    @Override
    public void tickServer() {
        ItemStack fuel = inventory.get(0);

        if (fuel.getItem() instanceof RBMKRodItem rod) {

            if (fluxQuantity > 0 && level.getGameTime() % 200 == 0) {
                SatelliteRayEvents.report(
                        (ServerLevel) level,
                        worldPosition,
                        SatelliteRayEvents.NEUTRON_EMISSION,
                        300);
            }

            this.hbm$syncSet$RBMKRodBlockEntity$rodColor(rod.colorTint);

            double fluxRatioOut = rod.rType == NeutronSpeed.SLOW ? 0 : 1;
            double fluxIn = fluxFromType(rod.nType);
            double fluxQuantityOut = rod.burn(level, fuel, fluxIn);

            rod.updateHeat(level, fuel, 1.0D);
            this.hbm$syncSet$RBMKBaseBlockEntity$heat(
                    (double) (this.heat + (rod.provideHeat(level, fuel, heat, 1.0D))));
            this.markChanged();

            if (!this.hasLid()) {
                RadiationSystemNT.incrementRad(
                        (ServerLevel) level,
                        worldPosition,
                        this.fluxQuantity * 0.05D,
                        this.fluxQuantity * 0.05D * 1024D);
            }

            super.tickServer();

            if (this.heat > this.maxHeat()) {
                if (MachineData.RBMK_DISABLE_MELTDOWNS.get()) {
                    double px = worldPosition.getX() + 0.5;
                    double py = worldPosition.getY() + RbmkParams.heightAboveCore() + 0.5;
                    double pz = worldPosition.getZ() + 0.5;
                    PacketWire.sendToAllAround(
                            EffectGasFlameVentLayout.encode(px, py, pz),
                            new TargetPoint((ServerLevel) level, px, py, pz, 150));
                } else {
                    this.meltdown();
                }
                this.hbm$syncSet$RBMKRodBlockEntity$lastFluxRatio(0);
                this.hbm$syncSet$RBMKRodBlockEntity$lastFluxQuantity(0);
                this.hbm$syncSet$RBMKRodBlockEntity$lastFluxOut(0);
                this.fluxQuantity = 0;
                return;
            }

            if (this.heat > 10_000) this.hbm$syncSet$RBMKBaseBlockEntity$heat(10_000);

            this.hbm$syncSet$RBMKRodBlockEntity$lastFluxQuantity(this.fluxQuantity);
            this.hbm$syncSet$RBMKRodBlockEntity$lastFluxRatio(this.fluxFastRatio);
            this.hbm$syncSet$RBMKRodBlockEntity$lastFluxOut(fluxQuantityOut);
            this.fluxQuantity = 0;
            this.fluxFastRatio = 0;

            spreadFlux(fluxQuantityOut, fluxRatioOut);

            hbm$syncSet$RBMKRodBlockEntity$hasRod(true);

        } else {
            if (hasRod || this.lastFluxQuantity != 0 || this.lastFluxRatio != 0) this.markChanged();
            this.hbm$syncSet$RBMKRodBlockEntity$lastFluxRatio(0);
            this.hbm$syncSet$RBMKRodBlockEntity$lastFluxQuantity(0);
            this.hbm$syncSet$RBMKRodBlockEntity$lastFluxOut(0);
            this.fluxQuantity = 0;
            this.fluxFastRatio = 0;
            hbm$syncSet$RBMKRodBlockEntity$hasRod(false);
            super.tickServer();
        }
    }

    private double fluxFromType(NeutronSpeed type) {
        double fastFlux = this.fluxQuantity * this.fluxFastRatio;
        double slowFlux = this.fluxQuantity * (1 - this.fluxFastRatio);
        return switch (type) {
            case SLOW -> slowFlux + fastFlux * 0.5;
            case FAST -> fastFlux + slowFlux * 0.3;
            case ANY -> this.fluxQuantity;
        };
    }

    public void spreadFlux(double flux, double ratio) {
        for (Vec3 dir : fluxDirs) {
            RbmkRay.emit(this, dir, flux, ratio);
        }
    }

    @Override
    public void onMelt(int reduce) {
        boolean moderated = isModerated();
        int h = RbmkParams.heightAboveCore();
        boolean fuelled = inventory.get(0).getItem() instanceof RBMKRodItem;
        boolean drx = inventory.get(0).getItem() == ModItems.RBMK_FUEL_DRX.get();
        inventory.set(0, ItemStack.EMPTY);
        if (drx) RBMKBlock.digamma = true;

        if (fuelled) {
            BlockState melt = ModBlocks.CORIUM.get().defaultBlockState();
            for (int i = h; i >= 0; i--) level.setBlock(worldPosition.above(i), melt, 3);
            int count = 1 + level.getRandom().nextInt(h);
            for (int i = 0; i < count; i++) spawnDebris(RBMKDebrisEntity.DebrisType.FUEL);
        } else {
            standardMelt(reduce);
        }

        if (moderated) {
            int count = 2 + level.getRandom().nextInt(2);
            for (int i = 0; i < count; i++) spawnDebris(RBMKDebrisEntity.DebrisType.GRAPHITE);
        }
        spawnDebris(RBMKDebrisEntity.DebrisType.ELEMENT);
        if (RBMKBlock.lidOf(getBlockState()) == RBMKBlock.Lid.CONCRETE) {
            spawnDebris(RBMKDebrisEntity.DebrisType.LID);
        }
    }

    @Override
    public FluxRole fluxRole() {
        return FluxRole.FUEL;
    }

    @Override
    public RBMKColumnType getConsoleType() {
        return RBMKColumnType.FUEL;
    }

    @Override
    public ColumnSnapshot getConsoleData(ColumnSnapshot reuse) {
        ColumnSnapshot.Fuel data = (ColumnSnapshot.Fuel) super.getConsoleData(reuse);
        ItemStack fuel = inventory.get(0);
        if (fuel.getItem() instanceof RBMKRodItem rod) {
            data.hbm$syncSet$Fuel$enrichment(RBMKRodItem.getEnrichment(fuel));
            data.hbm$syncSet$Fuel$xenon(RBMKRodItem.getPoison(fuel));
            data.hbm$syncSet$Fuel$hullHeat(RBMKRodItem.getHullHeat(fuel));
            data.hbm$syncSet$Fuel$coreHeat(RBMKRodItem.getCoreHeat(fuel));
            data.hbm$syncSet$Fuel$hullMeltingPoint(rod.meltingPoint);
        } else {
            data.hbm$syncSet$Fuel$enrichment(0);
            data.hbm$syncSet$Fuel$xenon(0);
            data.hbm$syncSet$Fuel$hullHeat(0);
            data.hbm$syncSet$Fuel$coreHeat(0);
            data.hbm$syncSet$Fuel$hullMeltingPoint(0);
        }
        return data;
    }

    public boolean coldEnoughForAutoloader() {
        return coldEnoughForAutoloader(inventory.get(0));
    }

    public static boolean coldEnoughForAutoloader(ItemStack rod) {
        return !(rod.getItem() instanceof RBMKRodItem) || RBMKRodItem.getHullHeat(rod) <= 1_000;
    }

    public boolean coldEnoughForManual() {
        return coldEnoughForManual(inventory.get(0));
    }

    public static boolean coldEnoughForManual(ItemStack rod) {
        return !(rod.getItem() instanceof RBMKRodItem) || RBMKRodItem.getHullHeat(rod) <= 200;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        ItemStack rod = inventory.get(0);
        if (rod.getItem() instanceof RBMKRodItem
                && RBMKRodItem.getHullHeat(rod) >= 1500
                && !MachineData.RBMK_DISABLE_MELTDOWNS.get()) {
            meltdown();
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public boolean acceptsLoad(ItemStack toLoad) {
        return !toLoad.isEmpty() && inventory.get(0).isEmpty();
    }

    @Override
    public void takeLoad(ItemStack toLoad) {
        inventory.set(0, toLoad.copy());
        markChanged();
    }

    @Override
    public boolean hasUnload() {
        return !inventory.get(0).isEmpty();
    }

    @Override
    public ItemStack peekUnload() {
        return inventory.get(0);
    }

    @Override
    public void takeUnload() {
        inventory.set(0, ItemStack.EMPTY);
        markChanged();
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
            VAL + "columnheat",
            VAL + "rodheat",
            VAL + "depletion",
            VAL + "xenon",
            VAL + "fastflux",
            VAL + "slowflux",
            VAL + "flux"
        };
    }

    @Override
    public @Nullable String getRadioValue(String name) {
        if ((VAL + "columnheat").equals(name)) return "" + (int) this.heat;
        ItemStack fuel = inventory.get(0);
        if (fuel.getItem() instanceof RBMKRodItem) {
            if ((VAL + "rodheat").equals(name)) return "" + (int) RBMKRodItem.getHullHeat(fuel);
            if ((VAL + "depletion").equals(name))
                return "" + (int) (100 - RBMKRodItem.getEnrichment(fuel) * 100);
            if ((VAL + "xenon").equals(name)) return "" + (int) RBMKRodItem.getPoison(fuel);
        }
        if ((VAL + "fastflux").equals(name)) return "" + (int) (lastFluxQuantity * lastFluxRatio);
        if ((VAL + "slowflux").equals(name))
            return "" + (int) (lastFluxQuantity * (1 - lastFluxRatio));
        if ((VAL + "flux").equals(name))
            return ""
                    + ((int) (lastFluxQuantity * lastFluxRatio)
                            + (int) (lastFluxQuantity * (1 - lastFluxRatio)));
        return null;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rbmkRod");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new RBMKRodMenu(id, inv, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fluxQuantity = input.getDoubleOr("fluxQuantity", fluxQuantity);
        fluxFastRatio = input.getDoubleOr("fluxMod", fluxFastRatio);
        hbm$syncSet$RBMKRodBlockEntity$hasRod(input.getBooleanOr("hasRod", hasRod));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("fluxQuantity", this.lastFluxQuantity);
        output.putDouble("fluxMod", this.lastFluxRatio);
        output.putBoolean("hasRod", this.hasRod);
    }

    @Override
    public void writeDiagnostics(CompoundTag tag) {
        super.writeDiagnostics(tag);
        tag.putDouble("fluxSlow", lastFluxQuantity * (1 - lastFluxRatio));
        tag.putDouble("fluxFast", lastFluxQuantity * lastFluxRatio);
        tag.putBoolean("hasRod", hasRod);
        ItemStack fuel = inventory.get(0);
        if (fuel.getItem() instanceof RBMKRodItem rod) {
            tag.putString(
                    "f_yield",
                    RBMKRodItem.getYield(fuel)
                            + " / "
                            + rod.yield
                            + " ("
                            + (RBMKRodItem.getEnrichment(fuel) * 100)
                            + "%)");
            tag.putString("f_xenon", RBMKRodItem.getPoison(fuel) + "%");
            tag.putString(
                    "f_heat",
                    String.format(Locale.ROOT, "%.6f", RBMKRodItem.getCoreHeat(fuel))
                            + " / "
                            + String.format(Locale.ROOT, "%.6f", RBMKRodItem.getHullHeat(fuel))
                            + " / "
                            + String.format(Locale.ROOT, "%.2f", rod.meltingPoint));
        }
    }

    private void readFluxQuantity(ByteBuf input) {
        fluxQuantity = input.readDouble();
    }

    private void readFluxRatio(ByteBuf input) {
        fluxFastRatio = input.readDouble();
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x1f0L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 4 -> output.writeDouble(this.lastFluxQuantity);
            case 5 -> output.writeDouble(this.lastFluxRatio);
            case 6 -> output.writeBoolean(this.hasRod);
            case 7 -> output.writeInt(this.rodColor);
            case 8 -> output.writeDouble(this.lastFluxOut);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 4 -> readFluxQuantity(input);
            case 5 -> readFluxRatio(input);
            case 6 -> this.hbm$syncSet$RBMKRodBlockEntity$hasRod(input.readBoolean());
            case 7 -> this.hbm$syncSet$RBMKRodBlockEntity$rodColor(input.readInt());
            case 8 -> this.hbm$syncSet$RBMKRodBlockEntity$lastFluxOut(input.readDouble());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$RBMKRodBlockEntity$lastFluxQuantity(double next) {
        if (Double.doubleToRawLongBits(this.lastFluxQuantity) != Double.doubleToRawLongBits(next)) {
            this.lastFluxQuantity = next;
            if ((1L << 4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 4);
        }
    }

    public final void hbm$syncSet$RBMKRodBlockEntity$lastFluxRatio(double next) {
        if (Double.doubleToRawLongBits(this.lastFluxRatio) != Double.doubleToRawLongBits(next)) {
            this.lastFluxRatio = next;
            if ((1L << 5) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 5);
        }
    }

    public final void hbm$syncSet$RBMKRodBlockEntity$lastFluxOut(double next) {
        if (Double.doubleToRawLongBits(this.lastFluxOut) != Double.doubleToRawLongBits(next)) {
            this.lastFluxOut = next;
            if ((1L << 8) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 8);
        }
    }

    public final void hbm$syncSet$RBMKRodBlockEntity$hasRod(boolean next) {
        if (this.hasRod != next) {
            this.hasRod = next;
            if ((1L << 6) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 6);
        }
    }

    public final void hbm$syncSet$RBMKRodBlockEntity$rodColor(int next) {
        if (this.rodColor != next) {
            this.rodColor = next;
            if ((1L << 7) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 7);
        }
    }
}
