// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.blocks.machine.rbmk.RBMKRod;
import com.hbm.entity.projectile.EntityRBMKDebris;
import com.hbm.handler.neutron.NeutronNodeWorld.StreamWorld;
import com.hbm.handler.neutron.NeutronNodeWorld;
import com.hbm.handler.neutron.NeutronStream;
import com.hbm.handler.neutron.RBMKNeutronHandler.RBMKNeutronNode;
import com.hbm.handler.neutron.RBMKNeutronHandler.RBMKType;
import com.hbm.handler.neutron.RBMKNeutronHandler;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.inventory.container.MenuRBMKRod;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.packet.toclient.EffectNTPayload;
import com.hbm.particle.HbmEffectNT;
import com.hbm.platform.Services;
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

public class BlockEntityRBMKRod extends BlockEntityRBMKBase
        implements IRBMKFluxReceiver,
                IRBMKLoadable,
                MenuProvider,
                SyncUnitSchema,
                IRORValueProvider {
    public static final Vec3[] fluxDirs = {
        new Vec3(0, 0, -1), new Vec3(1, 0, 0), new Vec3(0, 0, 1), new Vec3(-1, 0, 0)
    };
    public double fluxFastRatio;
    public double fluxQuantity;

    @SyncField(units = 1L << 4)
    public double lastFluxQuantity;

    @SyncField(units = 1L << 5)
    public double lastFluxRatio;

    @SyncField(units = 1L << 8)
    public double lastFluxOut;

    @SyncField(units = 1L << 6)
    public boolean hasRod;

    @SyncField(units = 1L << 7)
    public int rodColor = 0;

    public BlockEntityRBMKRod(BlockPos pos, BlockState state) {
        this(ModBlockEntities.RBMK_ROD.get(), pos, state);
    }

    protected BlockEntityRBMKRod(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1);
    }

    @Override
    public boolean isModerated() {
        return getBlockState().getBlock() instanceof RBMKRod rod && rod.moderated;
    }

    @Override
    public int trackingRange() {
        return 25;
    }

    @Override
    public void receiveFlux(NeutronStream stream) {
        double fastFlux = this.fluxQuantity * this.fluxFastRatio;
        double fastFluxIn = stream.fluxQuantity * stream.fluxRatio;
        this.fluxQuantity += stream.fluxQuantity;
        fluxFastRatio = (fastFlux + fastFluxIn) / fluxQuantity;
    }

    @Override
    public void tickServer() {
        ItemStack fuel = inventory.get(0);

        if (fuel.getItem() instanceof ItemRBMKRod rod) {

            if (fluxQuantity > 0 && level.getGameTime() % 200 == 0) {
                SatelliteRayEvents.report(
                        (ServerLevel) level,
                        worldPosition,
                        SatelliteRayEvents.NEUTRON_EMISSION,
                        300);
            }

            this.rodColor = rod.colorTint;

            double fluxRatioOut = rod.rType == NType.SLOW ? 0 : 1;
            double fluxIn = fluxFromType(rod.nType);
            double fluxQuantityOut = rod.burn(level, fuel, fluxIn);

            rod.updateHeat(level, fuel, 1.0D);
            this.heat += rod.provideHeat(level, fuel, heat, 1.0D);

            if (!this.hasLid()) {
                RadiationSystemNT.incrementRad(
                        (ServerLevel) level,
                        worldPosition,
                        this.fluxQuantity * 0.05D,
                        this.fluxQuantity * 0.05D * 1024D);
            }

            super.tickServer();

            if (this.heat > this.maxHeat()) {
                if (RBMKConfig.getMeltdownsDisabled(level)) {
                    double px = worldPosition.getX() + 0.5;
                    double py = worldPosition.getY() + RBMKConfig.getColumnHeight(level) + 0.5;
                    double pz = worldPosition.getZ() + 0.5;
                    Services.NETWORK.sendToAllAround(
                            new EffectNTPayload(HbmEffectNT.GasFlameVent, px, py, pz),
                            new TargetPoint((ServerLevel) level, px, py, pz, 150));
                } else {
                    this.meltdown();
                }
                this.lastFluxRatio = 0;
                this.lastFluxQuantity = 0;
                this.lastFluxOut = 0;
                this.fluxQuantity = 0;
                return;
            }

            if (this.heat > 10_000) this.heat = 10_000;

            this.lastFluxQuantity = this.fluxQuantity;
            this.lastFluxRatio = this.fluxFastRatio;
            this.lastFluxOut = fluxQuantityOut;
            this.fluxQuantity = 0;
            this.fluxFastRatio = 0;

            spreadFlux(fluxQuantityOut, fluxRatioOut);

            hasRod = true;

        } else {
            this.lastFluxRatio = 0;
            this.lastFluxQuantity = 0;
            this.lastFluxOut = 0;
            this.fluxQuantity = 0;
            this.fluxFastRatio = 0;
            hasRod = false;
            super.tickServer();
        }
    }

    private double fluxFromType(NType type) {
        double fastFlux = this.fluxQuantity * this.fluxFastRatio;
        double slowFlux = this.fluxQuantity * (1 - this.fluxFastRatio);
        return switch (type) {
            case SLOW -> slowFlux + fastFlux * 0.5;
            case FAST -> fastFlux + slowFlux * 0.3;
            case ANY -> this.fluxQuantity;
        };
    }

    public void spreadFlux(double flux, double ratio) {
        if (flux == 0) {
            NeutronNodeWorld.removeNode(level, worldPosition);
            return;
        }

        StreamWorld streamWorld = NeutronNodeWorld.getOrAddWorld(level);
        RBMKNeutronNode node = (RBMKNeutronNode) streamWorld.getNode(worldPosition);
        if (node == null) {
            node = RBMKNeutronHandler.makeNode(streamWorld, this);
            streamWorld.addNode(node);
        }

        for (Vec3 dir : fluxDirs) {
            new RBMKNeutronHandler.RBMKNeutronStream(node, dir, flux, ratio);
        }
    }

    @Override
    public void onMelt(int reduce) {
        boolean moderated = isModerated();
        int h = RBMKConfig.getColumnHeight(level);

        boolean fuelled = inventory.get(0).getItem() instanceof ItemRBMKRod;
        boolean drx = inventory.get(0).getItem() == ModItems.RBMK_FUEL_DRX.get();
        inventory.set(0, ItemStack.EMPTY);

        if (drx) RBMKBase.digamma = true;

        if (fuelled) {

            BlockState melt = ModBlocks.CORIUM.get().defaultBlockState();
            for (int i = h; i >= 0; i--) level.setBlock(worldPosition.above(i), melt, 3);
            int count = 1 + level.getRandom().nextInt(h);
            for (int i = 0; i < count; i++) spawnDebris(EntityRBMKDebris.DebrisType.FUEL);
        } else {
            standardMelt(reduce);
        }

        if (moderated) {
            int count = 2 + level.getRandom().nextInt(2);
            for (int i = 0; i < count; i++) spawnDebris(EntityRBMKDebris.DebrisType.GRAPHITE);
        }
        spawnDebris(EntityRBMKDebris.DebrisType.ELEMENT);
        if (RBMKBase.lidOf(getBlockState()) == RBMKBase.Lid.CONCRETE) {
            spawnDebris(EntityRBMKDebris.DebrisType.LID);
        }
    }

    @Override
    public RBMKType getRBMKType() {
        return RBMKType.ROD;
    }

    @Override
    public RBMKColumnType getConsoleType() {
        return RBMKColumnType.FUEL;
    }

    @Override
    public RBMKColumn getConsoleData(RBMKColumn reuse) {
        RBMKColumn.FuelColumn data = (RBMKColumn.FuelColumn) super.getConsoleData(reuse);
        ItemStack fuel = inventory.get(0);
        if (fuel.getItem() instanceof ItemRBMKRod rod) {
            data.enrichment = ItemRBMKRod.getEnrichment(fuel);
            data.xenon = ItemRBMKRod.getPoison(fuel);
            data.c_heat = ItemRBMKRod.getHullHeat(fuel);
            data.c_coreHeat = ItemRBMKRod.getCoreHeat(fuel);
            data.c_maxHeat = rod.meltingPoint;
        } else {
            data.enrichment = 0;
            data.xenon = 0;
            data.c_heat = 0;
            data.c_coreHeat = 0;
            data.c_maxHeat = 0;
        }
        return data;
    }

    public boolean coldEnoughForAutoloader() {
        return coldEnoughForAutoloader(inventory.get(0));
    }

    public static boolean coldEnoughForAutoloader(ItemStack rod) {
        return !(rod.getItem() instanceof ItemRBMKRod) || ItemRBMKRod.getHullHeat(rod) <= 1_000;
    }

    public boolean coldEnoughForManual() {
        return coldEnoughForManual(inventory.get(0));
    }

    public static boolean coldEnoughForManual(ItemStack rod) {
        return !(rod.getItem() instanceof ItemRBMKRod) || ItemRBMKRod.getHullHeat(rod) <= 200;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        ItemStack rod = inventory.get(0);
        if (rod.getItem() instanceof ItemRBMKRod
                && ItemRBMKRod.getHullHeat(rod) >= 1500
                && !RBMKConfig.getMeltdownsDisabled(level)) {
            meltdown();
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public boolean canLoad(ItemStack toLoad) {
        return !toLoad.isEmpty() && inventory.get(0).isEmpty();
    }

    @Override
    public void load(ItemStack toLoad) {
        inventory.set(0, toLoad.copy());
        markChanged();
    }

    @Override
    public boolean canUnload() {
        return !inventory.get(0).isEmpty();
    }

    @Override
    public ItemStack provideNext() {
        return inventory.get(0);
    }

    @Override
    public void unload() {
        inventory.set(0, ItemStack.EMPTY);
        markChanged();
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
            PREFIX_VALUE + "columnheat",
            PREFIX_VALUE + "rodheat",
            PREFIX_VALUE + "depletion",
            PREFIX_VALUE + "xenon",
            PREFIX_VALUE + "fastflux",
            PREFIX_VALUE + "slowflux",
            PREFIX_VALUE + "flux"
        };
    }

    @Override
    public @Nullable String provideRORValue(String name) {
        if ((PREFIX_VALUE + "columnheat").equals(name)) return "" + (int) this.heat;
        ItemStack fuel = inventory.get(0);
        if (fuel.getItem() instanceof ItemRBMKRod) {
            if ((PREFIX_VALUE + "rodheat").equals(name))
                return "" + (int) ItemRBMKRod.getHullHeat(fuel);
            if ((PREFIX_VALUE + "depletion").equals(name))
                return "" + (int) (100 - ItemRBMKRod.getEnrichment(fuel) * 100);
            if ((PREFIX_VALUE + "xenon").equals(name))
                return "" + (int) ItemRBMKRod.getPoison(fuel);
        }
        if ((PREFIX_VALUE + "fastflux").equals(name))
            return "" + (int) (lastFluxQuantity * lastFluxRatio);
        if ((PREFIX_VALUE + "slowflux").equals(name))
            return "" + (int) (lastFluxQuantity * (1 - lastFluxRatio));
        if ((PREFIX_VALUE + "flux").equals(name))
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
        return new MenuRBMKRod(id, inv, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fluxQuantity = input.getDoubleOr("fluxQuantity", fluxQuantity);
        fluxFastRatio = input.getDoubleOr("fluxMod", fluxFastRatio);
        hasRod = input.getBooleanOr("hasRod", hasRod);
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
        if (fuel.getItem() instanceof ItemRBMKRod rod) {
            tag.putString(
                    "f_yield",
                    ItemRBMKRod.getYield(fuel)
                            + " / "
                            + rod.yield
                            + " ("
                            + (ItemRBMKRod.getEnrichment(fuel) * 100)
                            + "%)");
            tag.putString("f_xenon", ItemRBMKRod.getPoison(fuel) + "%");
            tag.putString(
                    "f_heat",
                    String.format(Locale.ROOT, "%.6f", ItemRBMKRod.getCoreHeat(fuel))
                            + " / "
                            + String.format(Locale.ROOT, "%.6f", ItemRBMKRod.getHullHeat(fuel))
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
            case 6 -> this.hasRod = input.readBoolean();
            case 7 -> this.rodColor = input.readInt();
            case 8 -> this.lastFluxOut = input.readDouble();
            default -> super.readSyncUnit(unit, input);
        }
    }
}
