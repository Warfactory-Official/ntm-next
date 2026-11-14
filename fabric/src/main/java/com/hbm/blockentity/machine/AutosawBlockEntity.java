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

package com.hbm.blockentity.machine;

import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.BlockEntityTicks;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.GraphResident;
import com.hbm.blockentity.SoundEmitter;
import com.hbm.blockentity.Synced;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.client.ClientSyncRecovery;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ModDamageTypes;
import com.hbm.packet.BeSyncTable;
import com.hbm.packet.BlobSynced;
import com.hbm.packet.ChunkTrackerIndex;
import com.hbm.packet.PacketWire;
import com.hbm.packet.SyncAudience;
import com.hbm.packet.SyncBindings;
import com.hbm.packet.SyncSource;
import com.hbm.packet.SyncUnitFrame;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.packet.SyncUnitState;
import com.hbm.packet.SyncWire;
import com.hbm.packet.WireReplayServer;
import com.hbm.packet.layout.BlockDustBurstLayout;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import com.hbm.sound.SoundHandle;
import com.hbm.util.ChunkUtil;
import com.hbm.util.SyncInterpolation;
import com.hbm.util.TickPhase;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.VarLong;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockItemTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class AutosawBlockEntity extends BlockEntity
        implements BlockEntityTicks,
                SoundEmitter,
                FluidTankAccess,
                Synced,
                GraphResident,
                AudioLoop,
                FluidTankHandler,
                FluidSettingsHandler,
                SyncUnitSchema {

    public static final double AUDIO_RANGE_SQ = 15 * 15;
    private static final int MIN_DIST = 2;
    private static final int MAX_DIST = 9;
    private static final int FELL_HORIZONTAL_RANGE = 10;
    private static final int FELL_BFS_RADIUS = MAX_DIST + FELL_HORIZONTAL_RANGE;
    private static final int FELL_VERTICAL_RANGE = 32;
    private static final int FELL_MAX_BASE_DEPTH = FELL_VERTICAL_RANGE / 2;
    private static final TagKey<Block> SPRUCE_LOGS = BlockItemTags.SPRUCE_LOGS.block();
    private static final TagKey<Block> BIRCH_LOGS = BlockItemTags.BIRCH_LOGS.block();
    private static final TagKey<Block> JUNGLE_LOGS = BlockItemTags.JUNGLE_LOGS.block();
    private static final TagKey<Block> ACACIA_LOGS = BlockItemTags.ACACIA_LOGS.block();
    private static final TagKey<Block> DARK_OAK_LOGS = BlockItemTags.DARK_OAK_LOGS.block();
    private static final TagKey<Block> PALE_OAK_LOGS = BlockItemTags.PALE_OAK_LOGS.block();
    private static final TagKey<Block> CHERRY_LOGS = BlockItemTags.CHERRY_LOGS.block();
    private static final TagKey<Block> MANGROVE_LOGS = BlockItemTags.MANGROVE_LOGS.block();
    private static final int[][] EIGHTEEN_DIRS = {
        {1, 0, 0},
        {-1, 0, 0},
        {0, 1, 0},
        {0, -1, 0},
        {0, 0, 1},
        {0, 0, -1},
        {1, 1, 0},
        {1, -1, 0},
        {-1, 1, 0},
        {-1, -1, 0},
        {1, 0, 1},
        {1, 0, -1},
        {-1, 0, 1},
        {-1, 0, -1},
        {0, 1, 1},
        {0, 1, -1},
        {0, -1, 1},
        {0, -1, -1}
    };
    public static @Nullable Consumer<AutosawBlockEntity> CLIENT_SOUND;
    public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.WOODOIL, 100);
    private final FluidTankNTM[] receiving;
    public boolean isOn;
    public boolean isSuspended;
    public float syncYaw;
    public float rotationYaw;
    public float prevRotationYaw;
    public float syncPitch;
    public float rotationPitch;
    public float prevRotationPitch;
    public float spin, lastSpin;
    private int forceSkip;
    private int state;
    private final SyncInterpolation ease = new SyncInterpolation();

    public AutosawBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTOSAW.get(), pos, state);
        receiving = new FluidTankNTM[] {tank};
    }

    private static boolean shouldIgnore(BlockState state) {
        return (state.is(ModBlocks.PLANT_TALL_CD2.get())
                        || state.is(ModBlocks.PLANT_TALL_CD3.get()))
                && state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER;
    }

    private static boolean canSupportSapling(ServerLevel level, BlockPos pos, Block sapling) {
        return sapling.defaultBlockState().canSurvive(level, pos);
    }

    private static Block saplingFor(BlockState log) {
        if (log.is(SPRUCE_LOGS)) return Blocks.SPRUCE_SAPLING;
        if (log.is(BIRCH_LOGS)) return Blocks.BIRCH_SAPLING;
        if (log.is(JUNGLE_LOGS)) return Blocks.JUNGLE_SAPLING;
        if (log.is(ACACIA_LOGS)) return Blocks.ACACIA_SAPLING;
        if (log.is(DARK_OAK_LOGS)) return Blocks.DARK_OAK_SAPLING;
        if (log.is(PALE_OAK_LOGS)) return Blocks.PALE_OAK_SAPLING;
        if (log.is(CHERRY_LOGS)) return Blocks.CHERRY_SAPLING;
        if (log.is(MANGROVE_LOGS)) return Blocks.MANGROVE_PROPAGULE;
        return Blocks.OAK_SAPLING;
    }

    private static boolean isWood(BlockState s) {
        return s.is(BlockTags.LOGS);
    }

    private static boolean isLeaves(BlockState s) {
        return s.is(BlockTags.LEAVES);
    }

    private static boolean isPlant(BlockState s) {
        return s.getBlock() instanceof VegetationBlock;
    }

    public void toggleSuspended() {
        hbm$syncSet$AutosawBlockEntity$isSuspended(!isSuspended);
        setChanged();
    }

    @Override
    public void tickServer() {
        var level = (ServerLevel) this.level;
        if (!isSuspended && TickPhase.every(this, SharedConstants.TICKS_PER_SECOND)) {
            if (tank.getFill() > 0) {
                tank.setFill(tank.getFill() - 1);
                hbm$syncSet$AutosawBlockEntity$isOn(true);
                markChanged();
            } else {
                if (isOn) markChanged();
                hbm$syncSet$AutosawBlockEntity$isOn(false);
            }
        }

        if (isOn && !isSuspended) {
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            Vec3 pivot = new Vec3(x + 0.5, y + 1.75, z + 0.5);
            Vec3 upperArm =
                    new Vec3(0, 0, -4)
                            .xRot((float) Math.toRadians(80 - rotationPitch))
                            .yRot(-(float) Math.toRadians(rotationYaw));
            Vec3 lowerArm =
                    new Vec3(0, 0, -4)
                            .xRot((float) -Math.toRadians(80 - rotationPitch))
                            .yRot(-(float) Math.toRadians(rotationYaw));
            Vec3 armTip = new Vec3(0, 0, -2).yRot(-(float) Math.toRadians(rotationYaw));
            double cX = pivot.x + upperArm.x + lowerArm.x + armTip.x;
            double cY = pivot.y;
            double cZ = pivot.z + upperArm.z + lowerArm.z + armTip.z;

            for (LivingEntity e :
                    level.getEntitiesOfClass(
                            LivingEntity.class,
                            new AABB(cX - 1, cY - 0.25, cZ - 1, cX + 1, cY + 0.25, cZ + 1))) {
                if (e.isAlive()
                        && e.hurtServer(
                                level, level.damageSources().source(ModDamageTypes.BLENDER), 100)) {
                    EntitySounds.play(
                            e,
                            e.getX(),
                            e.getY(),
                            e.getZ(),
                            SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                            SoundSource.BLOCKS,
                            2.0F,
                            0.95F + level.getRandom().nextFloat() * 0.2F);
                    int count = Math.min((int) Math.ceil(e.getMaxHealth() / 4), 250) * 4;
                    PacketWire.sendToAllAround(
                            BlockDustBurstLayout.encode(
                                    Block.BLOCK_STATE_REGISTRY.getId(
                                            Blocks.REDSTONE_BLOCK.defaultBlockState()),
                                    e.getX(),
                                    e.getY() + e.getBbHeight() * 0.5,
                                    e.getZ(),
                                    count,
                                    0.1D),
                            new TargetPoint(level, e.getX(), e.getY(), e.getZ(), 50));
                }
            }

            if (state == 0) {
                hbm$syncSet$AutosawBlockEntity$rotationYaw((float) (rotationYaw + (1)));
                if (rotationYaw >= 360)
                    hbm$syncSet$AutosawBlockEntity$rotationYaw((float) (rotationYaw - (360)));

                if (forceSkip > 0) {
                    forceSkip--;
                } else {
                    final double CUT_ANGLE = 0.08726646259971647;
                    double rotationYawRads = Math.toRadians((rotationYaw + 270) % 360);
                    outer:
                    for (int dx = -MAX_DIST; dx <= MAX_DIST; dx++) {
                        for (int dz = -MAX_DIST; dz <= MAX_DIST; dz++) {
                            int sqrDst = dx * dx + dz * dz;
                            if (sqrDst <= MIN_DIST * MIN_DIST || sqrDst > MAX_DIST * MAX_DIST)
                                continue;
                            double angle = Math.atan2(dz, dx);
                            double relAngle = Math.abs(angle - rotationYawRads);
                            relAngle = Math.abs((relAngle + Math.PI) % (2 * Math.PI) - Math.PI);
                            if (relAngle > CUT_ANGLE) continue;
                            BlockPos p = new BlockPos(x + dx, y + 1, z + dz);
                            BlockState bs = level.getBlockState(p);
                            if (!(isWood(bs) || isLeaves(bs) || isPlant(bs))) continue;
                            if (shouldIgnore(bs)) continue;
                            state = 1;
                            break outer;
                        }
                    }
                }
            }

            int hitY = Mth.floor(cY);
            int hitX0 = Mth.floor(cX - 0.5), hitZ0 = Mth.floor(cZ - 0.5);
            int hitX1 = Mth.floor(cX + 0.5), hitZ1 = Mth.floor(cZ + 0.5);
            tryInteract(level, hitX0, hitY, hitZ0);
            tryInteract(level, hitX1, hitY, hitZ0);
            tryInteract(level, hitX0, hitY, hitZ1);
            tryInteract(level, hitX1, hitY, hitZ1);

            if (state == 1) {
                hbm$syncSet$AutosawBlockEntity$rotationPitch((float) (rotationPitch + (2)));
                if (rotationPitch > 80) {
                    hbm$syncSet$AutosawBlockEntity$rotationPitch(80);
                    state = 2;
                }
            }
            if (state == 2) {
                hbm$syncSet$AutosawBlockEntity$rotationPitch((float) (rotationPitch - (2)));
                if (rotationPitch <= 0) {
                    hbm$syncSet$AutosawBlockEntity$rotationPitch(0);
                    state = 0;
                }
            }
            markChanged();
        }

        networkPackNT(100);
    }

    private void tryInteract(ServerLevel level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState bs = level.getBlockState(pos);
        if (!shouldIgnore(bs)) {
            if (isLeaves(bs) || isPlant(bs)) {
                cutCrop(level, pos, bs);
            } else if (isWood(bs)) {
                fellTree(level, x, y, z);
                if (state == 1) state = 2;
            }
        }
        BlockState ahead = level.getBlockState(pos);
        if (state == 1 && ahead.isSolidRender() && !ahead.isSignalSource()) {
            state = 2;
            forceSkip = 5;
        }
    }

    private void cutCrop(ServerLevel level, BlockPos pos, BlockState bs) {
        level.levelEvent(2001, pos, Block.getId(bs));
        for (ItemStack drop : Block.getDrops(bs, level, pos, null)) {
            float delta = 0.7F;
            double dx = level.getRandom().nextFloat() * delta + (1.0F - delta) * 0.5D;
            double dy = level.getRandom().nextFloat() * delta + (1.0F - delta) * 0.5D;
            double dz = level.getRandom().nextFloat() * delta + (1.0F - delta) * 0.5D;
            ItemEntity item =
                    new ItemEntity(level, pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz, drop);
            item.setPickUpDelay(10);
            level.addFreshEntity(item);
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    private void fellTree(ServerLevel level, int hitX, int hitY, int hitZ) {
        int cx = worldPosition.getX(), cz = worldPosition.getZ();
        int sawY = hitY;
        BlockPos hitCol = new BlockPos(hitX, -1, hitZ);

        HashMap<BlockPos, BlockPos> trunks = new HashMap<>();
        for (int dx = -MAX_DIST; dx <= MAX_DIST; dx++) {
            for (int dz = -MAX_DIST; dz <= MAX_DIST; dz++) {
                if (dx * dx + dz * dz > MAX_DIST * MAX_DIST) continue;
                int colX = cx + dx, colZ = cz + dz;
                if (!isWood(level.getBlockState(new BlockPos(colX, sawY, colZ)))) continue;
                int baseY = sawY;
                while (sawY - baseY < FELL_MAX_BASE_DEPTH
                        && isWood(level.getBlockState(new BlockPos(colX, baseY - 1, colZ))))
                    baseY--;
                BlockPos base = new BlockPos(colX, baseY, colZ);
                if (!canSupportSapling(level, base, saplingFor(level.getBlockState(base))))
                    continue;
                trunks.put(new BlockPos(colX, -1, colZ), base);
            }
        }
        if (!trunks.containsKey(hitCol)) {
            int baseY = hitY;
            while (sawY - baseY < FELL_MAX_BASE_DEPTH
                    && isWood(level.getBlockState(new BlockPos(hitX, baseY - 1, hitZ)))) baseY--;
            trunks.put(hitCol, new BlockPos(hitX, baseY, hitZ));
        }

        HashMap<BlockPos, BlockPos> blockOwner = new HashMap<>();
        ArrayDeque<BlockPos[]> deque = new ArrayDeque<>();
        int[] hitColCount = {1};
        int minY = Math.max(level.getMinY(), sawY - FELL_MAX_BASE_DEPTH);
        int maxY = Math.min(level.getMaxY(), sawY + FELL_VERTICAL_RANGE);

        for (Map.Entry<BlockPos, BlockPos> trunk : trunks.entrySet()) {
            deque.addFirst(new BlockPos[] {trunk.getValue(), trunk.getKey()});
        }

        while (!deque.isEmpty()) {
            BlockPos[] pair = deque.pollFirst();
            BlockPos current = pair[0];
            BlockPos currentCol = pair[1];

            if (blockOwner.containsKey(current)) {
                if (currentCol.equals(hitCol) && --hitColCount[0] == 0) break;
                continue;
            }
            blockOwner.put(current, currentCol);

            for (int[] dir : EIGHTEEN_DIRS) {
                int nx = current.getX() + dir[0],
                        ny = current.getY() + dir[1],
                        nz = current.getZ() + dir[2];
                int ndx = nx - cx, ndz = nz - cz;
                if (ndx * ndx + ndz * ndz > FELL_BFS_RADIUS * FELL_BFS_RADIUS) continue;
                if (ny < minY || ny > maxY) continue;
                BlockPos np = new BlockPos(nx, ny, nz);
                if (blockOwner.containsKey(np)) continue;
                BlockState bs = level.getBlockState(np);
                if (!isWood(bs) && !isLeaves(bs)) continue;
                boolean hasHorizontal = dir[0] != 0 || dir[2] != 0;
                BlockPos[] entry = new BlockPos[] {np, currentCol};
                if (!hasHorizontal) deque.addFirst(entry);
                else deque.addLast(entry);
                if (currentCol.equals(hitCol)) hitColCount[0]++;
            }

            if (currentCol.equals(hitCol) && --hitColCount[0] == 0) break;
        }

        for (Map.Entry<BlockPos, BlockPos> entry : blockOwner.entrySet()) {
            if (!entry.getValue().equals(hitCol)) continue;
            BlockPos pos = entry.getKey();
            BlockState bs = level.getBlockState(pos);
            if (isWood(bs) && isWithinWorkingArea(pos.getX(), pos.getZ())) {
                Block sapling = saplingFor(bs);
                boolean replant = canSupportSapling(level, pos, sapling);
                level.destroyBlock(pos, true);
                if (replant) level.setBlock(pos, sapling.defaultBlockState(), 3);
            } else {
                level.destroyBlock(pos, true);
            }
        }
    }

    private boolean isWithinWorkingArea(int x, int z) {
        int dx = x - worldPosition.getX(), dz = z - worldPosition.getZ();
        int distSq = dx * dx + dz * dz;
        return distSq > MIN_DIST * MIN_DIST && distSq <= MAX_DIST * MAX_DIST;
    }

    @Override
    public void tickClient() {
        if (CLIENT_SOUND != null) CLIENT_SOUND.accept(this);

        lastSpin = spin;
        if (isOn && !isSuspended) {
            spin += 15F;
            Vec3 vec = new Vec3(0.625, 0, 1.625).yRot(-(float) Math.toRadians(rotationYaw));
            level.addParticle(
                    ParticleTypes.SMOKE,
                    worldPosition.getX() + 0.5 + vec.x,
                    worldPosition.getY() + 2.0625,
                    worldPosition.getZ() + 0.5 + vec.z,
                    0,
                    0,
                    0);
        }
        if (spin >= 360F) {
            spin -= 360F;
            lastSpin -= 360F;
        }

        prevRotationYaw = rotationYaw;
        prevRotationPitch = rotationPitch;
        hbm$syncSet$AutosawBlockEntity$rotationYaw(ease.towardDegrees(rotationYaw, syncYaw));
        hbm$syncSet$AutosawBlockEntity$rotationPitch(ease.towardDegrees(rotationPitch, syncPitch));
        ease.tick();
        if (rotationYaw >= 360F) {
            hbm$syncSet$AutosawBlockEntity$rotationYaw((float) (rotationYaw - (360F)));
            prevRotationYaw -= 360F;
        } else if (rotationYaw < 0F) {
            hbm$syncSet$AutosawBlockEntity$rotationYaw((float) (rotationYaw + (360F)));
            prevRotationYaw += 360F;
        }
    }

    @Override
    public SoundHandle createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.ENGINE_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                1.0F,
                10F,
                1.0F + level.getRandom().nextFloat() * 0.1F,
                10);
    }

    public boolean acceptsFace(Direction dir) {
        return dir != Direction.UP;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public int tankCount() {
        return 1;
    }

    @Override
    public FluidTankNTM tank(int index) {
        if (index != 0) throw new IndexOutOfBoundsException(index);
        return tank;
    }

    @Override
    public FluidTankNTM pasteTank() {
        return tank;
    }

    private void readYaw(ByteBuf input) {
        syncYaw = input.readFloat();
        ease.restart(3);
    }

    private void readPitch(ByteBuf input) {
        syncPitch = input.readFloat();
        ease.restart(3);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$AutosawBlockEntity$isOn(input.getBooleanOr("isOn", isOn));
        hbm$syncSet$AutosawBlockEntity$isSuspended(input.getBooleanOr("isSuspended", isSuspended));
        forceSkip = input.getIntOr("skip", forceSkip);
        hbm$syncSet$AutosawBlockEntity$rotationYaw(input.getFloatOr("yaw", rotationYaw));
        hbm$syncSet$AutosawBlockEntity$rotationPitch(input.getFloatOr("pitch", rotationPitch));
        state = input.getIntOr("state", state);
        input.child("tank").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("isOn", isOn);
        output.putBoolean("isSuspended", isSuspended);
        output.putInt("skip", forceSkip);
        output.putFloat("yaw", rotationYaw);
        output.putFloat("pitch", rotationPitch);
        output.putInt("state", state);
        tank.serialize(output.child("tank"));
    }

    @Override
    public long syncUnitMask() {
        return 0x1fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(this.isOn);
            case 1 -> output.writeBoolean(this.isSuspended);
            case 2 -> output.writeFloat(this.rotationYaw);
            case 3 -> output.writeFloat(this.rotationPitch);
            case 4 -> this.tank.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.hbm$syncSet$AutosawBlockEntity$isOn(input.readBoolean());
            case 1 -> this.hbm$syncSet$AutosawBlockEntity$isSuspended(input.readBoolean());
            case 2 -> readYaw(input);
            case 3 -> readPitch(input);
            case 4 -> this.tank.packetDeserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public boolean tickSound() {
        return isOn && !isSuspended;
    }

    public final void hbm$syncSet$AutosawBlockEntity$isOn(boolean next) {
        if (this.isOn != next) {
            this.isOn = next;
            if ((1L << 0) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 0);
        }
    }

    public final void hbm$syncSet$AutosawBlockEntity$isSuspended(boolean next) {
        if (this.isSuspended != next) {
            this.isSuspended = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$AutosawBlockEntity$rotationYaw(float next) {
        if (Float.floatToRawIntBits(this.rotationYaw) != Float.floatToRawIntBits(next)) {
            this.rotationYaw = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$AutosawBlockEntity$rotationPitch(float next) {
        if (Float.floatToRawIntBits(this.rotationPitch) != Float.floatToRawIntBits(next)) {
            this.rotationPitch = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(AutosawBlockEntity.class, "tank", 3, 1L << 4);
    }

    private boolean hbm$soundActive;

    public boolean soundStarted(boolean active) {
        boolean started = active && !hbm$soundActive;
        hbm$soundActive = active;
        return started;
    }

    private int hbm$syncDirty = 3;

    private int hbm$syncVersion;

    private int hbm$syncSentVersion;

    private boolean hbm$syncBound;

    private boolean hbm$syncWatched = true;

    private boolean hbm$syncedOnce;

    private CompoundTag hbm$syncInitial;

    private int hbm$syncInitialVersion;

    private long hbm$syncUnits;

    private long hbm$syncClientUnitRevision;

    private SyncUnitState hbm$syncUnitState;

    private SyncUnitState.Snapshot hbm$syncPublishedUnits;

    public final boolean syncBound() {
        return hbm$syncBound;
    }

    public final void syncChanged(int mask) {
        hbm$syncDirty |= mask;
        if ((mask & 1) != 0 && (Object) this instanceof SyncUnitSchema schema)
            hbm$syncUnits |= schema.syncUnitMask();
    }

    public final void syncUnitsChanged(int mask, long units) {
        if ((mask & 1) != 0 && (Object) this instanceof SyncUnitSchema schema) {
            hbm$syncUnits |= units & schema.syncUnitMask();
        }
        hbm$syncDirty |= mask;
    }

    public final void markSyncEvent() {
        hbm$syncDirty |= 7;
    }

    public final void bindSync(SyncSource owner, int mask) {
        throw new IllegalStateException("A block entity cannot be another snapshot's child");
    }

    public final void unbindSync(SyncSource owner) {
        throw new IllegalStateException("A block entity cannot be another snapshot's child");
    }

    private void hbm$syncBind() {
        if (hbm$syncBound) return;
        SyncBindings.bindFields(this);
        hbm$syncBound = true;
    }

    public void networkPackNT(int range) {
        if (!hbm$syncWatched || (hbm$syncDirty & 1) == 0 && hbm$syncSentVersion == hbm$syncVersion)
            return;
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;
        ServerLevel server = (ServerLevel) level;
        SyncUnitState.Snapshot publication = hbm$syncGated(server);
        if (publication == null) return;
        hbm$syncedOnce = true;
        BeSyncTable.refresh(
                publication,
                (BlockEntity) this,
                server,
                getBlockPos(),
                BuiltInRegistries.BLOCK_ENTITY_TYPE.getId(getType()),
                range,
                (Object) this instanceof SyncAudience audience ? audience : null);
    }

    public void networkPackNTTracking() {
        if (!hbm$syncWatched || (hbm$syncDirty & 1) == 0 && hbm$syncSentVersion == hbm$syncVersion)
            return;
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;
        ServerLevel server = (ServerLevel) level;
        SyncUnitState.Snapshot publication = hbm$syncGated(server);
        if (publication != null) {
            PacketWire.sendSyncTracking(
                    publication,
                    server,
                    getBlockPos(),
                    BuiltInRegistries.BLOCK_ENTITY_TYPE.getId(getType()));
        }
    }

    public final void syncToTracking() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;
        ServerLevel server = (ServerLevel) level;
        SyncUnitState.Snapshot publication = hbm$syncGated(server, true);
        if (publication != null)
            PacketWire.sendSyncTracking(
                    publication,
                    server,
                    getBlockPos(),
                    BuiltInRegistries.BLOCK_ENTITY_TYPE.getId(getType()));
    }

    public void markChanged() {
        ChunkUtil.markUnsaved(this);
    }

    private SyncUnitState.Snapshot hbm$syncUnitSnapshot(SyncUnitSchema schema) {
        hbm$syncBind();
        if (!hbm$syncRelays()) {
            if ((hbm$syncDirty & 1) == 0 && hbm$syncPublishedUnits != null)
                return hbm$syncPublishedUnits;
            if (hbm$syncUnitState == null) {
                hbm$syncUnitState = new SyncUnitState(schema);
            }
            hbm$syncUnitState.publish(hbm$syncUnits, (hbm$syncDirty & 4) != 0);
        }
        hbm$syncUnits = 0;
        hbm$syncDirty &= ~5;
        SyncUnitState.Snapshot current = hbm$syncUnitState.snapshot();
        if (hbm$syncPublishedUnits == current) return current;
        if (hbm$syncPublishedUnits == null) SyncWire.retain(this);
        hbm$syncPublishedUnits = current;
        hbm$syncVersion++;
        return current;
    }

    private SyncUnitState hbm$syncCapture(SyncUnitSchema schema) {
        if (!SyncWire.replayCompat()) return null;
        Level level = getLevel();
        if (level == null
                || !level.isClientSide() && !(level.getServer() instanceof WireReplayServer))
            return null;
        if (hbm$syncUnitState == null) hbm$syncUnitState = new SyncUnitState(schema);
        return hbm$syncUnitState;
    }

    private boolean hbm$syncRelays() {
        return SyncWire.replayCompat()
                && hbm$syncUnitState != null
                && hbm$syncUnitState.captured()
                && !getLevel().isClientSide();
    }

    private SyncUnitState.Snapshot hbm$syncGated(ServerLevel server) {
        return hbm$syncGated(server, false);
    }

    private SyncUnitState.Snapshot hbm$syncGated(ServerLevel server, boolean includePublished) {
        if (!((Object) this instanceof SyncUnitSchema schema)) return null;
        if (!PacketWire.hasPlayersTracking(server, getBlockPos())) {
            syncWatching(false);
            if (hbm$syncedOnce) {
                hbm$syncedOnce = false;
                BeSyncTable.evict(server, getBlockPos(), (BlockEntity) this);
            }
            return null;
        }
        SyncUnitState.Snapshot publication = hbm$syncUnitSnapshot(schema);
        if (!includePublished && hbm$syncSentVersion == hbm$syncVersion) return null;
        hbm$syncSentVersion = hbm$syncVersion;
        return publication;
    }

    private void hbm$syncRemove() {
        releaseSync();
    }

    public final void releaseSync() {
        if ((Object) this instanceof BlobSynced source) source.syncBlob().release();
        hbm$syncClientUnitRevision = 0;
        if (getLevel() instanceof ServerLevel server) {
            PacketWire.dropSyncPosition(server, getBlockPos(), (BlockEntity) this);
        }
        if (getLevel() instanceof ServerLevel server) {
            hbm$syncedOnce = false;
            BeSyncTable.evict(server, getBlockPos(), (BlockEntity) this);
        }
        if (hbm$syncPublishedUnits != null) {
            hbm$syncPublishedUnits = null;
            SyncWire.release(this);
        }
        if (hbm$syncUnitState != null) hbm$syncUnitState.release();
        hbm$syncUnitState = null;
        hbm$syncUnits = 0;
        hbm$syncInitial = null;
        hbm$syncDirty = 3;
        hbm$syncWatched = true;
        if (hbm$syncBound) {
            SyncBindings.unbindFields(this);
            hbm$syncBound = false;
        }
    }

    public final void syncWatching(boolean watched) {
        hbm$syncWatched = watched;
        if ((Object) this instanceof BlobSynced source) source.syncBlob().watching(watched);
    }

    private void hbm$syncLoad(ValueInput input) {
        Optional<ByteBuffer> stored = input.read(SyncWire.KEY, Codec.BYTE_BUFFER);
        if (stored.isPresent()) {
            ByteBuf bytes = Unpooled.wrappedBuffer(stored.get());
            try {
                long revision = VarLong.read(bytes);
                SyncUnitSchema schema = (SyncUnitSchema) (Object) this;
                SyncUnitState capture = hbm$syncCapture(schema);
                if (capture == null) SyncUnitFrame.readUnits(schema, bytes, true);
                else
                    SyncUnitFrame.readUnits(
                            schema,
                            bytes,
                            true,
                            capture,
                            level.isClientSide() ? revision : SyncWire.nextRevision());
                schema.readInitialExtras(bytes);
                if (capture == null || level.isClientSide()) schema.afterInitialSyncUnits();
                else schema.afterRelayedSyncUnits(schema.syncUnitMask(), true);
                if (bytes.isReadable())
                    throw new DecoderException("Trailing machine sync unit data");
                hbm$syncClientUnitRevision =
                        schema.initialMatchesSyncUnits() ? revision : -revision;
            } finally {
                bytes.release();
            }
            if (level != null && level.isClientSide()) {
                int parts = (Object) this instanceof BlobSynced ? 3 : 1;
                ClientSyncRecovery.received(getBlockPos(), parts);
            }
        }
    }

    private boolean hbm$syncRecorded(ValueInput input) {
        if (!(getLevel() instanceof ServerLevel server)
                || !(server.getServer() instanceof WireReplayServer replay)) {
            return false;
        }
        Optional<ByteBuffer> frame = input.read(SyncWire.FRAME_KEY, Codec.BYTE_BUFFER);
        Optional<ByteBuffer> blob = input.read(SyncWire.BLOB_KEY, Codec.BYTE_BUFFER);
        if (frame.isEmpty() && blob.isEmpty()) return false;
        if (frame.isPresent()) {
            if (!((Object) this instanceof SyncUnitSchema schema)) {
                throw new DecoderException("Recorded unit state without a schema");
            }
            if (hbm$syncUnitState == null) hbm$syncUnitState = new SyncUnitState(schema);
            if (hbm$syncUnitState.snapshot() == null)
                hbm$syncUnitState.publish(schema.syncUnitMask());
            ByteBuf bytes = Unpooled.wrappedBuffer(frame.get());
            try {
                boolean full = bytes.readBoolean();
                long units =
                        SyncUnitFrame.readUnits(
                                schema, bytes, full, hbm$syncUnitState, SyncWire.nextRevision());
                if (bytes.isReadable())
                    throw new DecoderException("Trailing machine sync unit data");
                if (schema.afterRelayedSyncUnits(units, false) && replay.skipsViewerTicks()) {
                    replay.resendWhenCaughtUp(this);
                }
            } finally {
                bytes.release();
            }
        }
        if (blob.isPresent()) {
            if (!((Object) this instanceof BlobSynced source)) {
                throw new DecoderException("Recorded blob without a carrier");
            }
            ByteBuf bytes = Unpooled.wrappedBuffer(blob.get());
            try {
                source.syncBlob().ingest(bytes);
            } finally {
                bytes.release();
            }
            source.flushSyncBlob();
        }
        return true;
    }

    public void loadWithComponents(ValueInput input) {
        if (SyncWire.replayCompat() && hbm$syncRecorded(input)) return;
        super.loadWithComponents(input);
        hbm$syncLoad(input);
    }

    public void loadCustomOnly(ValueInput input) {
        super.loadCustomOnly(input);
        hbm$syncLoad(input);
    }

    public final boolean applyUnitSync(long revision, long base, ByteBuf body) {
        if (!((Object) this instanceof SyncUnitSchema schema)) return false;
        if (revision <= Math.abs(hbm$syncClientUnitRevision)) return true;
        if (base != 0 && base != hbm$syncClientUnitRevision) return false;
        SyncUnitState capture = hbm$syncCapture(schema);
        if (capture == null) {
            if (base == 0) SyncUnitFrame.readInitial(schema, body);
            else SyncUnitFrame.read(schema, body);
        } else if (base == 0) {
            SyncUnitFrame.readUnits(schema, body, true, capture, revision);
            schema.afterInitialSyncUnits();
        } else {
            schema.afterSyncUnits(SyncUnitFrame.readUnits(schema, body, false, capture, revision));
        }
        if (body.isReadable()) throw new DecoderException("Trailing machine sync unit data");
        hbm$syncClientUnitRevision = revision;
        return true;
    }

    public final void syncTo(ServerPlayer player) {
        if (!((Object) this instanceof SyncUnitSchema schema)) return;
        if ((SyncWire.initialLayout(getClass()) & SyncWire.INITIAL_EXTRAS) != 0
                && schema.initialMatchesSyncUnits()) {
            ChunkTrackerIndex.beginInitial(player);
            try {
                player.connection.send(ClientboundBlockEntityDataPacket.create(this));
            } finally {
                ChunkTrackerIndex.endInitial();
            }
            return;
        }
        SyncUnitState.Snapshot publication = hbm$syncUnitSnapshot(schema);
        int type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getId(getType());
        long base = publication.base(0);
        long units = publication.selectedUnits(base);
        PacketWire.sendTo(
                publication.retainFrame(getBlockPos().asLong(), type, base, units), player);
        ChunkTrackerIndex.snapshotSent(player, this, publication.revision());
        if ((Object) this instanceof BlobSynced source) source.syncBlob().sendFull(player);
    }

    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        if (!((Object) this instanceof SyncUnitSchema schema))
            return super.getUpdateTag(registries);
        if (SyncWire.replayCompat() && getLevel() != null && getLevel().isClientSide()) {
            return hbm$syncReceivedTag(registries, schema);
        }
        return hbm$syncUnitUpdateTag(registries, schema);
    }

    private CompoundTag hbm$syncReceivedTag(
            HolderLookup.Provider registries, SyncUnitSchema schema) {
        CompoundTag tag = super.getUpdateTag(registries);
        SyncUnitState state = hbm$syncUnitState;
        SyncUnitState.Snapshot received = state == null ? null : state.snapshot();
        if (received == null) return tag;
        ByteBuf scratch = SyncWire.SCRATCH.get();
        scratch.clear();
        VarLong.write(scratch, received.revision());
        received.writeBody(schema.syncUnitMask(), scratch);
        schema.writeInitialExtras(scratch);
        byte[] bytes = new byte[scratch.readableBytes()];
        scratch.getBytes(scratch.readerIndex(), bytes);
        tag.putByteArray(SyncWire.KEY, bytes);
        return tag;
    }

    private CompoundTag hbm$syncUnitUpdateTag(
            HolderLookup.Provider registries, SyncUnitSchema schema) {
        hbm$syncBind();
        SyncUnitState.Snapshot publication = hbm$syncUnitSnapshot(schema);
        int layout = SyncWire.initialLayout(getClass());
        boolean published = (layout & SyncWire.INITIAL_REPUBLISHES) != 0 || hbm$syncRelays();
        if (hbm$syncInitial != null
                && hbm$syncInitialVersion == hbm$syncVersion
                && (published && (layout & SyncWire.INITIAL_EXTRAS) == 0
                        || (hbm$syncDirty & 2) == 0 && !hbm$syncRelays())) {
            hbm$syncDirty &= ~2;
            ChunkTrackerIndex.initialSnapshot(this, publication, schema.initialMatchesSyncUnits());
            return hbm$syncInitial;
        }
        ByteBuf scratch = SyncWire.SCRATCH.get();
        scratch.clear();
        VarLong.write(scratch, publication.revision());
        if (published) publication.writeBody(schema.syncUnitMask(), scratch);
        else SyncUnitFrame.writeInitial(schema, scratch);
        schema.writeInitialExtras(scratch);
        byte[] bytes = new byte[scratch.readableBytes()];
        scratch.getBytes(scratch.readerIndex(), bytes);
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putByteArray(SyncWire.KEY, bytes);
        hbm$syncDirty &= ~2;
        hbm$syncInitialVersion = hbm$syncVersion;
        ChunkTrackerIndex.initialSnapshot(this, publication, schema.initialMatchesSyncUnits());
        return hbm$syncInitial = tag;
    }

    public Packet<ClientGamePacketListener> getUpdatePacket() {
        if ((Object) this instanceof SyncUnitSchema) syncToTracking();
        return null;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        hbm$syncRemove();

        com$hbm$blockentity$AudioLoop$setRemoved();
    }

    private SoundHandle hbm$audio;

    public void audioLoop(boolean running, float volume) {
        hbm$audio = AudioLoop.loop(this, hbm$audio, running, volume, 0F, false);
    }

    public void audioLoop(boolean running, float volume, float pitch) {
        hbm$audio = AudioLoop.loop(this, hbm$audio, running, volume, pitch, true);
    }

    private void hbm$audioRemove() {
        audioLoop(false, 0F);
    }

    private void com$hbm$blockentity$AudioLoop$setRemoved() {
        hbm$audioRemove();
    }
}
