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

import com.hbm.advancement.AwardRegions;
import com.hbm.advancement.DetonationTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.api.fluidmk2.FluidCaps;
import com.hbm.api.fluidmk2.FluidFace;
import com.hbm.api.fluidmk2.FluidHandler;
import com.hbm.api.fluidmk2.FluidPipeGraph;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.PipeData;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.machine.rbmk.FluxRole;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.rbmk.RBMKBlock;
import com.hbm.blocks.machine.rbmk.RBMKLoaderBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.data.MachineData;
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.SpearEntity;
import com.hbm.entity.projectile.RBMKDebrisEntity;
import com.hbm.interfaces.OverpressureHandler;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import com.hbm.uninos.graph.GraphNode;
import com.hbm.uninos.graph.GraphSegment;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.NodeNetwork;
import com.hbm.util.ChunkUtil;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.ArrayDeque;
import java.util.Queue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class RBMKBaseBlockEntity extends MachineBlockEntity {

    public static final int maxWater = 16000;
    public static final int maxSteam = 16000;
    public static final ReferenceOpenHashSet<RBMKBaseBlockEntity> columns =
            new ReferenceOpenHashSet<>();
    public static final ReferenceOpenHashSet<NodeNetwork<PipeData>> pipes =
            new ReferenceOpenHashSet<>();
    private static final Direction[] neighborDirs = {
        Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };
    protected final RBMKBaseBlockEntity[] neighborCache = new RBMKBaseBlockEntity[4];
    public double heat;

    {
        hbm$syncSet$RBMKBaseBlockEntity$heat(20.0D);
    }

    public int reasimWater;
    public int reasimSteam;
    public int craneIndicator;
    private boolean topExtraChecked;

    protected RBMKBaseBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
    }

    public boolean hasLid() {
        if (!isLidRemovable()) return true;
        return RBMKBlock.lidOf(getBlockState()).present();
    }

    public boolean isLidRemovable() {
        return true;
    }

    public double maxHeat() {
        return 1500D;
    }

    public double passiveCooling(int neighbors) {
        double min = MachineData.RBMK_PASSIVE_COOLING_INNER.get();
        double max = MachineData.RBMK_PASSIVE_COOLING.get();
        return min + (max - min) * ((4 - Math.clamp(neighbors, 0, 4)) / 4D);
    }

    public int trackingRange() {
        return 15;
    }

    @Override
    public void tickServer() {
        ensureTopExtra();
        if (this.craneIndicator > 0)
            this.hbm$syncSet$RBMKBaseBlockEntity$craneIndicator((int) (this.craneIndicator - 1));
        moveHeat();
        if (RbmkParams.reasimBoilers()) boilWater();
        this.networkPackNT(trackingRange());
    }

    private void ensureTopExtra() {
        if (topExtraChecked) return;
        BlockPos top = worldPosition.above(RbmkParams.heightAboveCore());
        BlockState state = ChunkUtil.blockStateIfLoaded(level, top);
        BlockState over = ChunkUtil.blockStateIfLoaded(level, top.above());
        if (state == null || over == null) return;
        topExtraChecked = true;
        if (state.getBlock() != getBlockState().getBlock()) return;
        if (state.getValue(RBMKBlock.PART) == RBMKBlock.Part.TOP) return;
        if (over.getBlock() == getBlockState().getBlock()) return;
        level.setBlock(top, state.setValue(RBMKBlock.PART, RBMKBlock.Part.TOP), 3);
    }

    private void boilWater() {
        if (heat < 100D) return;
        double heatConsumption = MachineData.RBMK_BOILER_HEAT_CONSUMPTION.get();
        double availableHeat = (this.heat - 100) / heatConsumption;
        double availableWater = this.reasimWater;
        double availableSpace = maxSteam - this.reasimSteam;
        int processedWater =
                (int)
                        Math.floor(
                                Math.min(availableHeat, Math.min(availableWater, availableSpace))
                                        * Math.clamp(
                                                MachineData.RBMK_REASIM_BOILER_SPEED.get(),
                                                0D,
                                                1D));
        if (processedWater <= 0) return;
        this.hbm$syncSet$RBMKBaseBlockEntity$reasimWater(
                (int) (this.reasimWater - (processedWater)));
        this.hbm$syncSet$RBMKBaseBlockEntity$reasimSteam(
                (int) (this.reasimSteam + (processedWater)));
        this.hbm$syncSet$RBMKBaseBlockEntity$heat(
                (double) (this.heat - (processedWater * heatConsumption)));
        this.markChanged();
    }

    private void moveHeat() {
        boolean reasim = RbmkParams.reasimBoilers();

        double heatTot = this.heat;
        int waterTot = this.reasimWater;
        int steamTot = this.reasimSteam;

        for (int i = 0; i < 4; i++) {
            if (neighborCache[i] != null && neighborCache[i].isRemoved()) neighborCache[i] = null;
            if (neighborCache[i] == null) {
                BlockPos p = worldPosition.relative(neighborDirs[i]);
                if (level.getBlockEntity(p) instanceof RBMKBaseBlockEntity base)
                    neighborCache[i] = base;
            }
        }

        int members = 1;
        for (RBMKBaseBlockEntity base : neighborCache) {
            if (base != null) {
                members++;
                heatTot += base.heat;
                if (reasim) {
                    waterTot += base.reasimWater;
                    steamTot += base.reasimSteam;
                }
            }
        }

        double stepSize = MachineData.RBMK_COLUMN_HEAT_FLOW.get();

        if (members > 1) {
            double targetHeat = heatTot / (double) members;

            int tWater = waterTot / members;
            int rWater = waterTot % members;
            int tSteam = steamTot / members;
            int rSteam = steamTot % members;

            for (RBMKBaseBlockEntity base : neighborCache) {
                if (base == null) continue;
                base.hbm$syncSet$RBMKBaseBlockEntity$heat(
                        (double) (base.heat + ((targetHeat - base.heat) * stepSize)));
                if (reasim) {
                    base.hbm$syncSet$RBMKBaseBlockEntity$reasimWater(tWater);
                    base.hbm$syncSet$RBMKBaseBlockEntity$reasimSteam(tSteam);
                }
                base.markChanged();
            }
            this.hbm$syncSet$RBMKBaseBlockEntity$heat(
                    (double) (this.heat + ((targetHeat - this.heat) * stepSize)));
            if (reasim) {
                this.hbm$syncSet$RBMKBaseBlockEntity$reasimWater(tWater);
                this.hbm$syncSet$RBMKBaseBlockEntity$reasimSteam(tSteam);
                this.hbm$syncSet$RBMKBaseBlockEntity$reasimWater(
                        (int) (this.reasimWater + (rWater)));
                this.hbm$syncSet$RBMKBaseBlockEntity$reasimSteam(
                        (int) (this.reasimSteam + (rSteam)));
            }
            this.markChanged();
        }

        coolPassively(members - 1);
    }

    protected void coolPassively(int neighbors) {
        double before = this.heat;
        this.hbm$syncSet$RBMKBaseBlockEntity$heat(
                (double) (this.heat - (this.passiveCooling(neighbors))));
        if (heat < 20) hbm$syncSet$RBMKBaseBlockEntity$heat(20D);
        if (this.heat != before) this.markChanged();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel server) RBMKLoaderBlock.refreshBelow(server, this);
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level instanceof ServerLevel server) RBMKLoaderBlock.refreshBelow(server, this);
    }

    public FluxRole fluxRole() {
        return FluxRole.INERT;
    }

    public boolean isModerated() {
        return false;
    }

    public abstract RBMKColumnType getConsoleType();

    public ColumnSnapshot getConsoleData() {
        return getConsoleData(null);
    }

    public ColumnSnapshot getConsoleData(ColumnSnapshot reuse) {
        RBMKColumnType type = getConsoleType();
        ColumnSnapshot col = ColumnSnapshot.reuse(reuse, type);
        col.hbm$syncSet$ColumnSnapshot$heat(this.heat);
        col.hbm$syncSet$ColumnSnapshot$heatCeiling(this.maxHeat());
        col.hbm$syncSet$ColumnSnapshot$moderated(this.isModerated());
        col.hbm$syncSet$ColumnSnapshot$reasimWater(this.reasimWater);
        col.hbm$syncSet$ColumnSnapshot$reasimSteam(this.reasimSteam);
        col.hbm$syncSet$ColumnSnapshot$craneHighlight(this.craneIndicator);
        return col;
    }

    public void onOverheat() {
        for (int i = 0; i < 4; i++) {
            level.setBlockAndUpdate(worldPosition.above(i), Blocks.LAVA.defaultBlockState());
        }
    }

    public void onMelt(int reduce) {
        standardMelt(reduce);
        if (RBMKBlock.lidOf(getBlockState()) == RBMKBlock.Lid.CONCRETE) {
            spawnDebris(RBMKDebrisEntity.DebrisType.LID);
        }
    }

    protected void spawnDebris(RBMKDebrisEntity.DebrisType type) {
        RandomSource rand = level.getRandom();
        RBMKDebrisEntity debris =
                new RBMKDebrisEntity(
                        level,
                        worldPosition.getX() + 0.5D,
                        worldPosition.getY() + 4D,
                        worldPosition.getZ() + 0.5D,
                        type);
        double mx = rand.nextGaussian() * 0.25D;
        double mz = rand.nextGaussian() * 0.25D;
        double my = 0.25D + rand.nextDouble() * 1.25D;
        if (type == RBMKDebrisEntity.DebrisType.LID) {
            mx *= 0.5D;
            my += 0.5D;
            mz *= 0.5D;
        }
        debris.setDeltaMovement(mx, my, mz);
        level.addFreshEntity(debris);
    }

    protected void standardMelt(int reduce) {
        int h = RbmkParams.heightAboveCore();
        reduce = Math.clamp(reduce, 1, h);

        if (level.getRandom().nextInt(3) == 0) reduce++;

        for (int i = h; i >= 0; i--) {
            if (i <= h + 1 - reduce) {
                boolean boundary = reduce > 1 && i == h + 1 - reduce;
                level.setBlock(
                        worldPosition.above(i),
                        (boundary ? ModBlocks.RBMK_DEBRIS_BURNING : ModBlocks.RBMK_DEBRIS)
                                .get()
                                .defaultBlockState(),
                        3);
            } else {
                level.setBlock(worldPosition.above(i), Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    public void meltdown() {
        RBMKBlock.dropLids = false;
        columns.clear();
        pipes.clear();
        getFF(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());

        int minX = worldPosition.getX(), maxX = minX, minZ = worldPosition.getZ(), maxZ = minZ;
        for (RBMKBaseBlockEntity rbmk : columns) {
            BlockPos p = rbmk.worldPosition;
            if (p.getX() < minX) minX = p.getX();
            if (p.getX() > maxX) maxX = p.getX();
            if (p.getZ() < minZ) minZ = p.getZ();
            if (p.getZ() > maxZ) maxZ = p.getZ();
        }

        final int minX2 = minX, maxX2 = maxX, minZ2 = minZ, maxZ2 = maxZ;
        MultiblockCoreBlock.withoutTeardown(
                () -> {
                    for (RBMKBaseBlockEntity rbmk : columns) {
                        BlockPos p = rbmk.worldPosition;
                        int minDist =
                                Math.min(
                                        p.getX() - minX2,
                                        Math.min(
                                                maxX2 - p.getX(),
                                                Math.min(p.getZ() - minZ2, maxZ2 - p.getZ())));
                        rbmk.onMelt(minDist + 1);
                    }
                });

        spreadHotDebris();

        int smallDim = Math.min(maxX - minX, maxZ - minZ);
        double avgX = minX + (maxX - minX) / 2 + 0.5;
        double avgZ = minZ + (maxZ - minZ) / 2 + 0.5;
        ExplosionCreator.composeEffectRBMKMush(
                level, avgX, worldPosition.getY() + 1, avgZ, smallDim);
        WorldSounds.machine(
                level,
                null,
                avgX,
                worldPosition.getY() + 1,
                avgZ,
                ModSounds.RBMK_EXPLOSION.get(),
                SoundSource.BLOCKS,
                50.0F,
                1.0F);
        if (level instanceof ServerLevel server) {
            AwardRegions.within(
                    server,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5,
                    50,
                    p -> HbmCriteria.detonation(p, DetonationTrigger.Kind.RBMK));
        }
        if (MachineData.RBMK_ENABLE_MELTDOWN_OVERPRESSURE.get()
                && !pipes.isEmpty()
                && level instanceof ServerLevel serverLevel) {
            overpressure(serverLevel);
        }
        if (RBMKBlock.digamma) {
            RBMKBlock.digamma = false;
            SpearEntity spear = new SpearEntity(ModEntities.SPEAR.get(), level);
            spear.setPos(avgX, worldPosition.getY() + 100, avgZ);
            level.addFreshEntity(spear);
        }
        RBMKBlock.dropLids = true;
        columns.clear();
        pipes.clear();
    }

    private void spreadHotDebris() {
        BlockState hot =
                (RBMKBlock.digamma
                                ? ModBlocks.RBMK_DEBRIS_DIGAMMA
                                : ModBlocks.RBMK_DEBRIS_RADIATING)
                        .get()
                        .defaultBlockState();

        for (RBMKBaseBlockEntity rbmk : columns) {
            if (!(rbmk instanceof RBMKRodBlockEntity)) continue;
            if (!level.getBlockState(rbmk.worldPosition).is(ModBlocks.CORIUM.get())) continue;

            for (BlockPos pos :
                    BlockPos.betweenClosed(
                            rbmk.worldPosition.offset(-1, -1, -1),
                            rbmk.worldPosition.offset(1, 1, 1))) {
                if (level.getRandom().nextInt(3) != 0) continue;
                BlockState there = level.getBlockState(pos);
                if (there.is(ModBlocks.RBMK_DEBRIS.get())
                        || there.is(ModBlocks.RBMK_DEBRIS_BURNING.get())) {
                    level.setBlock(pos, hot, 3);
                }
            }
        }
    }

    private void overpressure(ServerLevel level) {
        LevelNodeGraph<PipeData> graph = FluidPipeGraph.get(level);
        LongArrayList cells = new LongArrayList();
        ReferenceOpenHashSet<BlockEntity> receivers = new ReferenceOpenHashSet<>();
        for (NodeNetwork<PipeData> net : pipes) {
            for (GraphNode<PipeData> node : net.nodes)
                collect(level, graph, node.posKey, cells, receivers);
            for (GraphSegment<PipeData> run : net.segments) {
                for (int i = 0; i < run.cellCount(); i++)
                    collect(level, graph, run.cellAt(i), cells, receivers);
            }
        }

        int max = Math.min(cells.size() / 5, 100);
        for (int i = 0; i < max; i++) {
            level.setBlock(BlockPos.of(cells.getLong(i)), Blocks.AIR.defaultBlockState(), 3);
        }
        for (BlockEntity be : receivers) {
            BlockPos p = be.getBlockPos();
            if (be instanceof OverpressureHandler op) {
                op.explode(level, p);
            } else {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                level.explode(
                        null,
                        p.getX() + 0.5,
                        p.getY() + 0.5,
                        p.getZ() + 0.5,
                        5F,
                        Level.ExplosionInteraction.NONE);
            }
        }
    }

    private static void collect(
            ServerLevel level,
            LevelNodeGraph<PipeData> graph,
            long cell,
            LongArrayList cells,
            ReferenceOpenHashSet<BlockEntity> receivers) {
        if (!LevelNodeGraph.resident(level, cell)) return;
        cells.add(cell);
        Fluid fluid = graph.dataAt(cell).fluid();
        BlockPos at = BlockPos.of(cell);
        for (Direction dir : Direction.VALUES) {
            if (!graph.isOpenAt(cell, dir)) continue;
            BlockPos n = at.relative(dir);
            BlockState state = ChunkUtil.blockStateIfLoaded(level, n);
            if (state == null) continue;
            FluidHandler receiver =
                    FluidCaps.RECEIVER.getCapability(
                            (Level) level, n, null, null, FluidFace.of(dir.getOpposite(), fluid));
            if (receiver == null || !subscribed(receiver, fluid)) continue;
            BlockEntity owner = receiver instanceof BlockEntity be ? be : null;
            if (owner == null) {
                BlockPos core = MultiblockSurface.coreOfAny(level, n, state);
                owner = ChunkUtil.blockEntityIfLoaded(level, core != null ? core : n);
            }
            if (owner != null) receivers.add(owner);
        }
    }

    private static boolean subscribed(FluidHandler receiver, Fluid fluid) {
        if (!receiver.netSubscribed() || !receiver.canReceiveFluid(fluid)) return false;
        for (int lanes = receiver.receivingPressureMask(fluid, DataComponentPatch.EMPTY);
                lanes != 0;
                lanes &= lanes - 1) {
            if (receiver.getReceiverSpeed(fluid, Integer.numberOfTrailingZeros(lanes)) > 0)
                return true;
        }
        return false;
    }

    private void getFF(int x, int y, int z) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(new BlockPos(x, y, z));
        int safetyLimit = 50000;
        while (!queue.isEmpty() && safetyLimit-- > 0) {
            BlockPos current = queue.poll();
            if (ChunkUtil.blockEntityIfLoaded(level, current) instanceof RBMKBaseBlockEntity rbmk
                    && columns.add(rbmk)) {
                queue.add(current.offset(1, 0, 0));
                queue.add(current.offset(-1, 0, 0));
                queue.add(current.offset(0, 0, 1));
                queue.add(current.offset(0, 0, -1));
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$RBMKBaseBlockEntity$heat(input.getDoubleOr("heat", heat));
        hbm$syncSet$RBMKBaseBlockEntity$reasimWater(input.getIntOr("reasimWater", reasimWater));
        hbm$syncSet$RBMKBaseBlockEntity$reasimSteam(input.getIntOr("reasimSteam", reasimSteam));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("heat", heat);
        output.putInt("reasimWater", reasimWater);
        output.putInt("reasimSteam", reasimSteam);
    }

    public void writeDiagnostics(CompoundTag tag) {
        tag.putDouble("heat", heat);
        tag.putInt("reasimWater", reasimWater);
        tag.putInt("reasimSteam", reasimSteam);
    }

    protected static void writeDiagnostics(CompoundTag tag, String name, FluidTankNTM tank) {
        tag.putInt(name, tank.getFill());
        tag.putInt(name + "_max", tank.getMaxFill());
        tag.putInt(name + "_type", NTMFluids.legacyId(tank.getTankType()));
        tag.putShort(name + "_p", (short) tank.getPressure());
    }

    private void writeCraneIndicator(ByteBuf output) {
        output.writeByte(craneIndicator);
    }

    private void readCraneIndicator(ByteBuf input) {
        hbm$syncSet$RBMKBaseBlockEntity$craneIndicator(input.readByte());
    }

    protected static final FlushFaces COLUMN_OUTPUTS = RBMKBaseBlockEntity::collectColumnOutputs;

    protected static boolean collectColumnOutputs(
            ServerLevel level, BlockPos core, FlushFaces.Visitor out) {
        out.contact(core.above(RbmkParams.heightAboveCore() + 1), Direction.DOWN);
        for (int down = 1; down <= 2; down++) {
            BlockPos at = core.below(down);
            BlockState state = ChunkUtil.blockStateIfLoaded(level, at);
            if (state == null || !(state.getBlock() instanceof RBMKLoaderBlock)) continue;
            out.contact(at.east(), Direction.WEST);
            out.contact(at.west(), Direction.EAST);
            out.contact(at.south(), Direction.NORTH);
            out.contact(at.north(), Direction.SOUTH);
            out.contact(at.below(), Direction.UP);
            return true;
        }
        return false;
    }

    public long syncUnitMask() {
        return 0xfL;
    }

    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeDouble(this.heat);
            case 1 -> output.writeInt(this.reasimWater);
            case 2 -> output.writeInt(this.reasimSteam);
            case 3 -> writeCraneIndicator(output);
            default -> throw new IllegalArgumentException();
        }
    }

    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.hbm$syncSet$RBMKBaseBlockEntity$heat(input.readDouble());
            case 1 -> this.hbm$syncSet$RBMKBaseBlockEntity$reasimWater(input.readInt());
            case 2 -> this.hbm$syncSet$RBMKBaseBlockEntity$reasimSteam(input.readInt());
            case 3 -> readCraneIndicator(input);
            default -> throw new IllegalArgumentException();
        }
    }

    public final void hbm$syncSet$RBMKBaseBlockEntity$heat(double next) {
        if (Double.doubleToRawLongBits(this.heat) != Double.doubleToRawLongBits(next)) {
            this.heat = next;
            if ((1L << 0) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 0);
        }
    }

    public final void hbm$syncSet$RBMKBaseBlockEntity$reasimWater(int next) {
        if (this.reasimWater != next) {
            this.reasimWater = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$RBMKBaseBlockEntity$reasimSteam(int next) {
        if (this.reasimSteam != next) {
            this.reasimSteam = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$RBMKBaseBlockEntity$craneIndicator(int next) {
        if (this.craneIndicator != next) {
            this.craneIndicator = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }
}
