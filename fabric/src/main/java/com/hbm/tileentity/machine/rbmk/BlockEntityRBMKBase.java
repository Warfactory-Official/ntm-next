// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

import com.hbm.advancement.AwardRegions;
import com.hbm.advancement.DetonationTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.api.fluidmk2.FluidCaps;
import com.hbm.api.fluidmk2.FluidFace;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.blocks.machine.rbmk.RBMKLoader;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.blocks.network.FluidPipeBlock;
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EntitySpear;
import com.hbm.entity.projectile.EntityRBMKDebris;
import com.hbm.handler.neutron.NeutronNodeWorld;
import com.hbm.handler.neutron.RBMKNeutronHandler.RBMKType;
import com.hbm.interfaces.IOverpressurable;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.SyncField;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.util.ChunkUtil;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.ArrayDeque;
import java.util.Queue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class BlockEntityRBMKBase extends BlockEntityMachineBase {

    public static final int maxWater = 16000;
    public static final int maxSteam = 16000;
    public static final ReferenceOpenHashSet<BlockEntityRBMKBase> columns =
            new ReferenceOpenHashSet<>();
    private static final Direction[] neighborDirs = {
        Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };
    protected final BlockEntityRBMKBase[] neighborCache = new BlockEntityRBMKBase[4];

    @SyncField(units = 1L << 0)
    public double heat = 20.0D;

    @SyncField(units = 1L << 1)
    public int reasimWater;

    @SyncField(units = 1L << 2)
    public int reasimSteam;

    @SyncField(units = 1L << 3)
    public int craneIndicator;

    private boolean topExtraChecked;

    protected BlockEntityRBMKBase(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
    }

    public boolean hasLid() {
        if (!isLidRemovable()) return true;
        return RBMKBase.lidOf(getBlockState()).present();
    }

    public boolean isLidRemovable() {
        return true;
    }

    public double maxHeat() {
        return 1500D;
    }

    public double passiveCooling(int neighbors) {
        double min = RBMKConfig.getPassiveCoolingInner(level);
        double max = RBMKConfig.getPassiveCooling(level);
        return min + (max - min) * ((4 - Math.clamp(neighbors, 0, 4)) / 4D);
    }

    public int trackingRange() {
        return 15;
    }

    @Override
    public void tickServer() {
        ensureTopExtra();
        if (this.craneIndicator > 0) this.craneIndicator--;
        moveHeat();
        if (RBMKConfig.getReasimBoilers(level)) boilWater();
        this.networkPackNT(trackingRange());
    }

    private void ensureTopExtra() {
        if (topExtraChecked) return;
        BlockPos top = worldPosition.above(RBMKConfig.getColumnHeight(level));
        BlockState state = ChunkUtil.blockStateIfLoaded(level, top);
        BlockState over = ChunkUtil.blockStateIfLoaded(level, top.above());
        if (state == null || over == null) return;
        topExtraChecked = true;
        if (state.getBlock() != getBlockState().getBlock()) return;
        if (state.getValue(RBMKBase.PART) == RBMKBase.Part.TOP) return;

        if (over.getBlock() == getBlockState().getBlock()) return;
        level.setBlock(top, state.setValue(RBMKBase.PART, RBMKBase.Part.TOP), 3);
    }

    private void boilWater() {
        if (heat < 100D) return;
        double heatConsumption = RBMKConfig.getBoilerHeatConsumption(level);
        double availableHeat = (this.heat - 100) / heatConsumption;
        double availableWater = this.reasimWater;
        double availableSpace = maxSteam - this.reasimSteam;
        int processedWater =
                (int)
                        Math.floor(
                                Math.min(availableHeat, Math.min(availableWater, availableSpace))
                                        * Math.clamp(
                                                RBMKConfig.getReaSimBoilerSpeed(level), 0D, 1D));
        if (processedWater <= 0) return;
        this.reasimWater -= processedWater;
        this.reasimSteam += processedWater;
        this.heat -= processedWater * heatConsumption;
    }

    private void moveHeat() {
        boolean reasim = RBMKConfig.getReasimBoilers(level);

        double heatTot = this.heat;
        int waterTot = this.reasimWater;
        int steamTot = this.reasimSteam;

        for (int i = 0; i < 4; i++) {
            if (neighborCache[i] != null && neighborCache[i].isRemoved()) neighborCache[i] = null;
            if (neighborCache[i] == null) {
                BlockPos p = worldPosition.relative(neighborDirs[i]);
                if (level.getBlockEntity(p) instanceof BlockEntityRBMKBase base)
                    neighborCache[i] = base;
            }
        }

        int members = 1;
        for (BlockEntityRBMKBase base : neighborCache) {
            if (base != null) {
                members++;
                heatTot += base.heat;
                if (reasim) {
                    waterTot += base.reasimWater;
                    steamTot += base.reasimSteam;
                }
            }
        }

        double stepSize = RBMKConfig.getColumnHeatFlow(level);

        if (members > 1) {
            double targetHeat = heatTot / (double) members;

            int tWater = waterTot / members;
            int rWater = waterTot % members;
            int tSteam = steamTot / members;
            int rSteam = steamTot % members;

            for (BlockEntityRBMKBase base : neighborCache) {
                if (base == null) continue;
                base.heat += (targetHeat - base.heat) * stepSize;
                if (reasim) {
                    base.reasimWater = tWater;
                    base.reasimSteam = tSteam;
                }
            }
            this.heat += (targetHeat - this.heat) * stepSize;
            if (reasim) {
                this.reasimWater = tWater;
                this.reasimSteam = tSteam;

                this.reasimWater += rWater;
                this.reasimSteam += rSteam;
            }
            this.markChanged();
        }

        coolPassively(members - 1);
    }

    protected void coolPassively(int neighbors) {
        this.heat -= this.passiveCooling(neighbors);
        if (heat < 20) heat = 20D;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null) NeutronNodeWorld.removeNode(level, worldPosition);
        if (level instanceof ServerLevel server) RBMKLoader.refreshBelow(server, this);
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level instanceof ServerLevel server) RBMKLoader.refreshBelow(server, this);
    }

    public RBMKType getRBMKType() {
        return RBMKType.OTHER;
    }

    public boolean isModerated() {
        return false;
    }

    public abstract RBMKColumnType getConsoleType();

    public RBMKColumn getConsoleData() {
        return getConsoleData(null);
    }

    public RBMKColumn getConsoleData(RBMKColumn reuse) {
        RBMKColumnType type = getConsoleType();
        RBMKColumn col =
                reuse != null && reuse.type == type ? reuse : RBMKColumn.createForType(type);
        col.heat = this.heat;
        col.maxHeat = this.maxHeat();
        col.moderated = this.isModerated();
        col.reasimWater = this.reasimWater;
        col.reasimSteam = this.reasimSteam;
        col.indicator = this.craneIndicator;
        return col;
    }

    public void onOverheat() {
        for (int i = 0; i < 4; i++) {
            level.setBlockAndUpdate(worldPosition.above(i), Blocks.LAVA.defaultBlockState());
        }
    }

    public void onMelt(int reduce) {
        standardMelt(reduce);

        if (RBMKBase.lidOf(getBlockState()) == RBMKBase.Lid.CONCRETE) {
            spawnDebris(EntityRBMKDebris.DebrisType.LID);
        }
    }

    protected void spawnDebris(EntityRBMKDebris.DebrisType type) {
        RandomSource rand = level.getRandom();
        EntityRBMKDebris debris =
                new EntityRBMKDebris(
                        level,
                        worldPosition.getX() + 0.5D,
                        worldPosition.getY() + 4D,
                        worldPosition.getZ() + 0.5D,
                        type);
        double mx = rand.nextGaussian() * 0.25D;
        double mz = rand.nextGaussian() * 0.25D;
        double my = 0.25D + rand.nextDouble() * 1.25D;
        if (type == EntityRBMKDebris.DebrisType.LID) {
            mx *= 0.5D;
            my += 0.5D;
            mz *= 0.5D;
        }
        debris.setDeltaMovement(mx, my, mz);
        level.addFreshEntity(debris);
    }

    protected void standardMelt(int reduce) {
        int h = RBMKConfig.getColumnHeight(level);
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
        RBMKBase.dropLids = false;
        columns.clear();
        getFF(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());

        int minX = worldPosition.getX(), maxX = minX, minZ = worldPosition.getZ(), maxZ = minZ;
        for (BlockEntityRBMKBase rbmk : columns) {
            BlockPos p = rbmk.worldPosition;
            if (p.getX() < minX) minX = p.getX();
            if (p.getX() > maxX) maxX = p.getX();
            if (p.getZ() < minZ) minZ = p.getZ();
            if (p.getZ() > maxZ) maxZ = p.getZ();
        }

        final int minX2 = minX, maxX2 = maxX, minZ2 = minZ, maxZ2 = maxZ;

        BlockMultiblockCore.withoutTeardown(
                () -> {
                    for (BlockEntityRBMKBase rbmk : columns) {
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
        level.playSound(
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
        if (RBMKConfig.getOverpressure(level) && level instanceof ServerLevel serverLevel) {
            overpressure(serverLevel);
        }

        if (RBMKBase.digamma) {
            RBMKBase.digamma = false;
            EntitySpear spear = new EntitySpear(ModEntities.SPEAR.get(), level);
            spear.setPos(avgX, worldPosition.getY() + 100, avgZ);
            level.addFreshEntity(spear);
        }
        RBMKBase.dropLids = true;
        columns.clear();
    }

    private void spreadHotDebris() {
        BlockState hot =
                (RBMKBase.digamma ? ModBlocks.RBMK_DEBRIS_DIGAMMA : ModBlocks.RBMK_DEBRIS_RADIATING)
                        .get()
                        .defaultBlockState();

        for (BlockEntityRBMKBase rbmk : columns) {
            if (!(rbmk instanceof BlockEntityRBMKRod)) continue;
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
        LongOpenHashSet pipes = new LongOpenHashSet();
        Queue<BlockPos> queue = new ArrayDeque<>();
        for (BlockEntityRBMKBase col : columns) {
            for (Direction dir : Direction.VALUES) {
                BlockPos p = col.worldPosition.relative(dir);
                if (level.getBlockState(p).getBlock() instanceof FluidPipeBlock
                        && pipes.add(p.asLong())) queue.add(p);
            }
        }
        if (pipes.isEmpty()) return;

        ReferenceOpenHashSet<BlockPos> receivers = new ReferenceOpenHashSet<>();
        int safety = 10_000;
        while (!queue.isEmpty() && safety-- > 0) {
            BlockPos p = queue.poll();
            for (Direction dir : Direction.VALUES) {
                BlockPos n = p.relative(dir);
                if (level.getBlockState(n).getBlock() instanceof FluidPipeBlock) {
                    if (pipes.add(n.asLong())) queue.add(n);
                } else if (Services.CAPS.find(
                                FluidCaps.RECEIVER, level, n, FluidFace.any(dir.getOpposite()))
                        != null) {
                    receivers.add(n.immutable());
                }
            }
        }

        int max = Math.min(pipes.size() / 5, 100);
        int count = 0;
        for (LongIterator it = pipes.iterator(); it.hasNext() && count < max; count++) {
            level.setBlock(BlockPos.of(it.nextLong()), Blocks.AIR.defaultBlockState(), 3);
        }
        for (BlockPos p : receivers) {
            BlockEntity be = level.getBlockEntity(p);
            if (be == null) {
                BlockPos core = MultiblockSurface.coreOfAny(level, p);
                if (core != null) be = level.getBlockEntity(core);
            }
            if (be instanceof IOverpressurable op) {
                op.explode(level, be.getBlockPos());
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

    private void getFF(int x, int y, int z) {

        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(new BlockPos(x, y, z));
        int safetyLimit = 50000;
        while (!queue.isEmpty() && safetyLimit-- > 0) {
            BlockPos current = queue.poll();
            if (ChunkUtil.blockEntityIfLoaded(level, current) instanceof BlockEntityRBMKBase rbmk
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
        heat = input.getDoubleOr("heat", heat);
        reasimWater = input.getIntOr("reasimWater", reasimWater);
        reasimSteam = input.getIntOr("reasimSteam", reasimSteam);
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
        craneIndicator = input.readByte();
    }

    protected static final FlushFaces COLUMN_OUTPUTS = BlockEntityRBMKBase::collectColumnOutputs;

    protected static boolean collectColumnOutputs(
            ServerLevel level, BlockPos core, FlushFaces.Visitor out) {
        out.contact(core.above(RBMKConfig.getColumnHeight(level) + 1), Direction.DOWN);
        for (int down = 1; down <= 2; down++) {
            BlockPos at = core.below(down);
            BlockState state = ChunkUtil.blockStateIfLoaded(level, at);
            if (state == null || !(state.getBlock() instanceof RBMKLoader)) continue;

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
            case 0 -> this.heat = input.readDouble();
            case 1 -> this.reasimWater = input.readInt();
            case 2 -> this.reasimSteam = input.readInt();
            case 3 -> readCraneIndicator(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
