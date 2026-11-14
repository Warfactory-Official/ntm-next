/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
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

import com.hbm.api.fluidmk2.PlainFluidReceiver;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.BlockEntityTicks;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.GraphResident;
import com.hbm.blockentity.Mufflable;
import com.hbm.blockentity.SoundEmitter;
import com.hbm.blockentity.Synced;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockTallPlant;
import com.hbm.blocks.machine.ThresherBlock;
import com.hbm.client.ClientSyncRecovery;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModItems;
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
import com.hbm.sound.AudioSystem;
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
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.VarLong;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class ThresherBlockEntity extends BlockEntity
        implements BlockEntityTicks,
                PlainFluidReceiver,
                FluidTankAccess,
                FluidSettingsHandler,
                GraphResident,
                Synced,
                SyncUnitSchema,
                AudioLoop,
                Mufflable,
                SoundEmitter,
                SoundEmitter.ManualTick {
    public static final double AUDIO_RANGE_SQ = 15D * 15D;
    public static Consumer<ThresherBlockEntity> CLIENT_SOUND = _ -> {};
    private static final float ARM_LIMIT = 82.5F;
    private static final float ARM_STEP = ARM_LIMIT / 60F;

    public boolean isOn;
    public boolean isSuspended;
    public float angle;
    public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.WOODOIL, 100);
    private boolean muffled;
    private final FluidTankNTM[] tanks = {tank};
    private final SyncInterpolation easing = new SyncInterpolation();
    private int motion;
    private int idleTicks;
    private DamageSource bladeDamage;
    public float trackedAngle;
    public float prevAngle;
    public float spin;
    public float lastSpin;

    public ThresherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.THRESHER.get(), pos, state);
        tank.bindOwner(this);
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level instanceof ServerLevel server)
            bladeDamage = server.damageSources().source(ModDamageTypes.BLENDER);
    }

    public static boolean acceptsFuel(Fluid fluid) {
        return fluid == NTMFluids.WOODOIL
                || fluid == NTMFluids.ETHANOL
                || fluid == NTMFluids.FISHOIL
                || fluid == NTMFluids.HEAVYOIL
                || fluid == NTMFluids.COALCREOSOTE;
    }

    public boolean acceptsFace(@Nullable Direction side) {
        return side == null
                || side == Direction.DOWN
                || side.getAxis().isHorizontal()
                        && side.getAxis()
                                != getBlockState().getValue(ThresherBlock.FACING).getAxis();
    }

    @Override
    public void tickServer() {
        if (!isSuspended) {
            if (TickPhase.every(this, 20)) {
                boolean fueled = tank.getFill() > 0;
                if (fueled) tank.setFill(tank.getFill() - 1);
                if (isOn != fueled) {
                    hbm$syncSet$ThresherBlockEntity$isOn(fueled);
                    setChanged();
                }
            }
            if (isOn) {
                if (motion == 0 && --idleTicks <= 0) motion = 1;
                if (motion == 1) {
                    hbm$syncSet$ThresherBlockEntity$angle(Math.min(ARM_LIMIT, angle + ARM_STEP));
                    if (angle == ARM_LIMIT) motion = 2;
                    setChanged();
                } else if (motion == 2) {
                    hbm$syncSet$ThresherBlockEntity$angle(Math.max(0F, angle - ARM_STEP));
                    if (angle == 0F) {
                        motion = 0;
                        idleTicks = 200 + level.getRandom().nextInt(100);
                    }
                    setChanged();
                }
                if (angle > 0F) harvest((ServerLevel) level);
            }
        }
        networkPackNT(100);
    }

    private void harvest(ServerLevel server) {
        var front = getBlockState().getValue(ThresherBlock.FACING);
        var rear = front.getOpposite();
        var lateral = rear.getClockWise();
        float radians = (float) Math.toRadians(ARM_LIMIT - angle);
        double reach = 3D + 8D * Mth.cos(radians);
        double x = worldPosition.getX() + 0.5D - rear.getStepX() * reach;
        double z = worldPosition.getZ() + 0.5D - rear.getStepZ() * reach;
        for (int offset = -3; offset <= 3; offset++) {
            var cutPos =
                    BlockPos.containing(
                            x + lateral.getStepX() * offset,
                            worldPosition.getY(),
                            z + lateral.getStepZ() * offset);
            var crop = server.getBlockState(cutPos);
            if (crop.isSolidRender() && !crop.isSignalSource() && !cuttable(crop)) {
                motion = 2;
                setChanged();
                break;
            }
            if (crop.is(Blocks.SUNFLOWER)) {
                regenerate(server, cutPos, crop, 250, Items.SUNFLOWER);
            } else if (crop.is(Blocks.TALL_GRASS)) {
                regenerate(server, cutPos, crop, 100, Items.WHEAT_SEEDS);
            } else if (crop.getBlock() instanceof BlockTallPlant) {
                cutCrown(server, cutPos, crop);
            } else if (crop.is(Blocks.SUGAR_CANE) || crop.is(Blocks.CACTUS)) {
                var base =
                        server.getBlockState(cutPos.below()).is(crop.getBlock())
                                ? cutPos.below()
                                : cutPos;
                removeCrop(server, base.above(2));
                removeCrop(server, base.above());
            } else if (crop.is(Blocks.BAMBOO)) {
                var base =
                        server.getBlockState(cutPos.below()).is(Blocks.BAMBOO)
                                ? cutPos.below()
                                : cutPos;
                var top = base.above();
                while (server.getBlockState(top).is(Blocks.BAMBOO)) top = top.above();
                for (var next = top.below(); next.getY() > base.getY(); next = next.below())
                    removeCrop(server, next);
            } else if (cuttable(crop) && mature(server, cutPos, crop)) {
                cutAndReplant(server, cutPos, crop);
            }
        }
        double halfX = 0.5D * Math.abs(rear.getStepX()) + 4.5D * Math.abs(lateral.getStepX());
        double halfZ = 0.5D * Math.abs(rear.getStepZ()) + 4.5D * Math.abs(lateral.getStepZ());
        var hazard =
                new AABB(
                        x - halfX,
                        worldPosition.getY(),
                        z - halfZ,
                        x + halfX,
                        worldPosition.getY() + 1D,
                        z + halfZ);
        for (var entity : server.getEntitiesOfClass(LivingEntity.class, hazard)) {
            if (!entity.isAlive() || !entity.hurtServer(server, bladeDamage, 100F)) continue;
            if (entity instanceof Enemy && !entity.isAlive())
                eject(ModItems.NITRA_SMALL.get().getDefaultInstance());
            server.playSound(
                    null,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                    SoundSource.BLOCKS,
                    2F,
                    0.95F + server.getRandom().nextFloat() * 0.2F);
            int count = 4 * Math.min(Mth.ceil(entity.getMaxHealth() / 4F), 250);
            double y = entity.getY() + entity.getBbHeight() * 0.5D;
            var packet =
                    new ClientboundLevelParticlesPacket(
                            new BlockParticleOption(
                                    ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                            false,
                            false,
                            entity.getX(),
                            y,
                            entity.getZ(),
                            0F,
                            0F,
                            0F,
                            0.1F,
                            count);
            for (var player : server.players()) {
                if (player.distanceToSqr(entity.getX(), y, entity.getZ()) <= 50D * 50D)
                    player.connection.send(packet);
            }
        }
    }

    private static boolean cuttable(BlockState state) {
        var block = state.getBlock();
        return !(block instanceof StemBlock)
                && (block instanceof BonemealableBlock
                        || state.is(Blocks.NETHER_WART)
                        || state.is(Blocks.MELON)
                        || state.is(Blocks.PUMPKIN)
                        || state.is(Blocks.SUGAR_CANE)
                        || state.is(Blocks.CACTUS)
                        || state.is(Blocks.BAMBOO));
    }

    private static boolean mature(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.is(Blocks.NETHER_WART))
            return state.getValue(NetherWartBlock.AGE) == NetherWartBlock.MAX_AGE;
        if (state.getBlock() instanceof BonemealableBlock plant) {
            return !plant.isValidBonemealTarget(level, pos, state, BonemealSource.INTERACTION);
        }
        return true;
    }

    private void regenerate(
            ServerLevel server, BlockPos pos, BlockState state, int chance, Item result) {
        if (server.getRandom().nextInt(chance) == 0) {
            server.levelEvent(2001, pos, Block.getId(state));
            eject(result.getDefaultInstance());
        }
    }

    private void cutCrown(ServerLevel server, BlockPos pos, BlockState state) {
        if (state.is(ModBlocks.PLANT_TALL_CD2.get()) || state.is(ModBlocks.PLANT_TALL_CD3.get()))
            return;
        if (state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER) {
            pos = pos.above();
            if (!server.getBlockState(pos).is(state.getBlock())) return;
        }
        removeCrop(server, pos);
    }

    private void removeCrop(ServerLevel server, BlockPos pos) {
        var state = server.getBlockState(pos);
        server.levelEvent(2001, pos, Block.getId(state));
        for (var drop : Block.getDrops(state, server, pos, null)) eject(drop);
        server.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    private void cutAndReplant(ServerLevel server, BlockPos pos, BlockState state) {
        var drops = Block.getDrops(state, server, pos, null);
        var replacement = Blocks.AIR.defaultBlockState();
        for (var drop : drops) {
            if (drop.getItem() instanceof BlockItem item
                    && item.getBlock() instanceof VegetationBlock) {
                var planted = item.getBlock().defaultBlockState();
                if (planted.canSurvive(server, pos)) {
                    replacement = planted;
                    drop.shrink(1);
                    break;
                }
            }
        }
        if (replacement.isAir() && state.is(Blocks.WHEAT))
            replacement = Blocks.WHEAT.defaultBlockState();
        server.levelEvent(2001, pos, Block.getId(state));
        for (var drop : drops) if (!drop.isEmpty()) eject(drop);
        server.setBlock(pos, replacement, Block.UPDATE_ALL);
    }

    private void eject(ItemStack stack) {
        var facing = getBlockState().getValue(ThresherBlock.FACING);
        var drop =
                new ItemEntity(
                        level,
                        worldPosition.getX() + 0.5D - facing.getStepX() * 0.75D,
                        worldPosition.getY(),
                        worldPosition.getZ() + 0.5D - facing.getStepZ() * 0.75D,
                        stack);
        drop.setPickUpDelay(10);
        drop.setDeltaMovement(
                -0.2D * facing.getStepX() + 0.2D,
                drop.getDeltaMovement().y,
                -0.2D * facing.getStepZ());
        level.addFreshEntity(drop);
    }

    @Override
    public void tickClient() {
        var state = getBlockState();
        CLIENT_SOUND.accept(this);
        this.lastSpin = this.spin;
        if (this.isOn && !this.isSuspended) {
            if (this.angle > 0F) this.spin += 15F;
            var rear = state.getValue(ThresherBlock.FACING).getOpposite();
            var lateral = rear.getClockWise();
            level.addParticle(
                    ParticleTypes.SMOKE,
                    worldPosition.getX()
                            + 0.5D
                            + rear.getStepX() * 0.8125D
                            + lateral.getStepX() * 0.375D,
                    worldPosition.getY() + 1.5625D,
                    worldPosition.getZ()
                            + 0.5D
                            + rear.getStepZ() * 0.8125D
                            + lateral.getStepZ() * 0.375D,
                    0D,
                    0D,
                    0D);
        }
        if (this.spin >= 360F) {
            this.spin -= 360F;
            this.lastSpin -= 360F;
        }
        this.prevAngle = this.angle;
        this.hbm$syncSet$ThresherBlockEntity$angle(
                this.easing.towardDegrees(this.angle, this.trackedAngle));
        this.easing.tick();
    }

    public void toggleSuspended() {
        hbm$syncSet$ThresherBlockEntity$isSuspended(!isSuspended);
        setChanged();
    }

    @Override
    public boolean tickSound() {
        return isOn && !isSuspended;
    }

    @Override
    public boolean isMuffled() {
        return muffled;
    }

    @Override
    public void setMuffled() {
        hbm$syncSet$ThresherBlockEntity$muffled(true);
        setChanged();
        syncToTracking();
    }

    @Override
    public SoundHandle createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.ENGINE_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX() + 0.5F,
                worldPosition.getY() + 0.5F,
                worldPosition.getZ() + 0.5F,
                1F,
                10F,
                1F + level.getRandom().nextFloat() * 0.1F,
                10);
    }

    @Override
    public boolean canReceiveFluid(Fluid fluid) {
        return tank.joins(fluid);
    }

    @Override
    public long getDemand(Fluid fluid, int pressure) {
        return tank.getPressure() == pressure && tank.accepts(fluid)
                ? tank.getMaxFill() - tank.getFill()
                : 0L;
    }

    @Override
    public long transferFluid(Fluid fluid, int pressure, long amount) {
        if (tank.getPressure() != pressure || !tank.accepts(fluid)) return amount;
        return amount - tank.receive(fluid, (int) Math.min(amount, Integer.MAX_VALUE));
    }

    @Override
    public int receivingPressureMask(Fluid fluid) {
        return tank.accepts(fluid) ? 1 << tank.getPressure() : 0;
    }

    @Override
    public void forEachTank(Consumer<FluidTankNTM> visitor) {
        visitor.accept(tank);
    }

    @Override
    public int tankCount() {
        return 1;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return tanks[index];
    }

    @Override
    public FluidTankNTM[] settingsTanks() {
        return tanks;
    }

    @Override
    public FluidTankNTM pasteTank() {
        return tank;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("isOn", isOn);
        output.putBoolean("isSuspended", isSuspended);
        output.putFloat("angle", angle);
        output.putInt("state", motion);
        tank.serialize(output.child("tank"));
        if (muffled) output.putBoolean("muffled", true);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$ThresherBlockEntity$isOn(input.getBooleanOr("isOn", false));
        hbm$syncSet$ThresherBlockEntity$isSuspended(input.getBooleanOr("isSuspended", false));
        hbm$syncSet$ThresherBlockEntity$angle(input.getFloatOr("angle", 0F));
        motion = input.getIntOr("state", 0);
        if (motion < 0 || motion > 2) throw new IllegalArgumentException("state");
        input.child("tank").ifPresent(tank::deserialize);
        hbm$syncSet$ThresherBlockEntity$muffled(input.getBooleanOr("muffled", false));
    }

    @Override
    public long syncUnitMask() {
        return 15L | MUFFLE_MASK;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(isOn);
            case 1 -> output.writeBoolean(isSuspended);
            case 2 -> output.writeFloat(angle);
            case 3 -> tank.packetSerialize(output);
            case MUFFLE_UNIT -> output.writeBoolean(muffled);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$ThresherBlockEntity$isOn(input.readBoolean());
            case 1 -> hbm$syncSet$ThresherBlockEntity$isSuspended(input.readBoolean());
            case 2 -> trackedAngle = input.readFloat();
            case 3 -> tank.packetDeserialize(input);
            case MUFFLE_UNIT -> hbm$syncSet$ThresherBlockEntity$muffled(input.readBoolean());
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void afterSyncUnits(long units) {
        easing.restart(3);
    }

    public final void hbm$syncSet$ThresherBlockEntity$isOn(boolean next) {
        if (this.isOn != next) {
            this.isOn = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$ThresherBlockEntity$isSuspended(boolean next) {
        if (this.isSuspended != next) {
            this.isSuspended = next;
            if ((2L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2L);
        }
    }

    public final void hbm$syncSet$ThresherBlockEntity$angle(float next) {
        if (Float.floatToRawIntBits(this.angle) != Float.floatToRawIntBits(next)) {
            this.angle = next;
            if ((4L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4L);
        }
    }

    public final void hbm$syncSet$ThresherBlockEntity$muffled(boolean next) {
        if (this.muffled != next) {
            this.muffled = next;
            if ((MUFFLE_MASK) == 0L) syncChanged(3);
            else syncUnitsChanged(3, MUFFLE_MASK);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(ThresherBlockEntity.class, "tank", 3, 8L);
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

    private boolean hbm$soundActive;

    public boolean soundStarted(boolean active) {
        boolean started = active && !hbm$soundActive;
        hbm$soundActive = active;
        return started;
    }
}
