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

package com.hbm.blockentity.machine.pile;

import com.hbm.blockentity.BlockEntityTicks;
import com.hbm.blockentity.GraphResident;
import com.hbm.blockentity.Synced;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.pile.PileBlock.Role;
import com.hbm.blocks.machine.pile.PileBlock;
import com.hbm.blocks.machine.pile.PileError;
import com.hbm.blocks.multiblock.AssembledMembers;
import com.hbm.client.ClientSyncRecovery;
import com.hbm.entity.projectile.BulletBaseMK4Entity;
import com.hbm.items.machine.PileRodMK2Item;
import com.hbm.items.weapon.sedna.factory.XFactoryPile;
import com.hbm.lib.DirPos;
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
import com.hbm.particle.CoolingTowerParticleOptions;
import com.hbm.sound.WorldSounds;
import com.hbm.util.ChunkUtil;
import com.hbm.util.TickPhase;
import com.hbm.world.bulk.BulkAction;
import com.hbm.world.bulk.BulkDataFields;
import com.hbm.world.bulk.BulkTransform;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.VarLong;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class PileCoreBlockEntity extends BlockEntity
        implements BlockEntityTicks, GraphResident, Synced, SyncUnitSchema {
    private static final String NBT_MEMBERS = "members";
    private static final String NBT_HEIGHT = "height";
    private static final String NBT_WIDTH = "width";
    private static final String NBT_DEPTH = "depth";
    private static final String NBT_LEFT = "left";
    private static final String NBT_RIGHT = "right";
    private static final String NBT_UP = "up";
    private static final String NBT_ORIENTATION = "orientation";
    private static final String NBT_FRONT = "front";
    private static final String NBT_X = "_x";
    private static final String NBT_Y = "_y";
    private static final String NBT_Z = "_z";
    private static final String NBT_D = "_d";
    private static final String[] CHANNEL_COORDINATES = {NBT_X, NBT_Y, NBT_Z, NBT_D};

    public static final int MAX_HEAT = 800;
    public static boolean meltingDown;
    public PileOrientation orientation = PileOrientation.NEITHER;
    public Direction front = Direction.NORTH;
    public int height;
    public int width;
    public int depth;
    public int left;
    public int right;
    public int up;
    public double highestHeat;
    public List<PileChannel> fuelChannels = new ArrayList<>();
    public List<PileChannel> ventilationChannels = new ArrayList<>();
    public List<PileChannel> controlChannels = new ArrayList<>();
    public PileSegment[] segments = new PileSegment[0];
    private @Nullable BoundingBox members;

    public PileCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PILE_CORE.get(), pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        height = input.getIntOr(NBT_HEIGHT, 0);
        width = input.getIntOr(NBT_WIDTH, 0);
        depth = input.getIntOr(NBT_DEPTH, 0);
        left = input.getIntOr(NBT_LEFT, 0);
        right = input.getIntOr(NBT_RIGHT, 0);
        up = input.getIntOr(NBT_UP, 0);
        orientation = PileOrientation.values()[input.getIntOr(NBT_ORIENTATION, 2)];
        front = Direction.VALUES[input.getIntOr(NBT_FRONT, Direction.NORTH.get3DDataValue())];
        members = input.read(NBT_MEMBERS, BoundingBox.CODEC).orElse(null);

        for (var type : PileChannelType.values()) {
            var channels = getChannelList(type);
            channels.clear();
            int count = input.getIntOr(type.countKey, 0);
            for (int i = 0; i < count; i++)
                channels.add(readChannel(input, type.storagePrefix + i));
        }
        recalculateSegments();
    }

    public @Nullable PileChannel getFuelChannel(BlockPos pos) {
        return getChannel(pos, fuelChannels);
    }

    public @Nullable PileChannel getVentilationChannel(BlockPos pos) {
        return getChannel(pos, ventilationChannels);
    }

    public @Nullable PileChannel getControlChannel(BlockPos pos) {
        return getChannel(pos, controlChannels);
    }

    public @Nullable PileChannel getChannel(BlockPos pos, List<PileChannel> list) {
        for (PileChannel channel : list) if (channel.entry.pos().equals(pos)) return channel;
        return null;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt(NBT_HEIGHT, height);
        output.putInt(NBT_WIDTH, width);
        output.putInt(NBT_DEPTH, depth);
        output.putInt(NBT_LEFT, left);
        output.putInt(NBT_RIGHT, right);
        output.putInt(NBT_UP, up);
        output.putInt(NBT_ORIENTATION, orientation.ordinal());
        output.putInt(NBT_FRONT, front.get3DDataValue());
        if (members != null) output.store(NBT_MEMBERS, BoundingBox.CODEC, members);

        for (var type : PileChannelType.values()) {
            var channels = getChannelList(type);
            output.putInt(type.countKey, channels.size());
            for (int i = 0; i < channels.size(); i++)
                channels.get(i).writeChannel(output, type.storagePrefix + i);
        }
    }

    public PileCoreBlockEntity setupSize(
            int up,
            int down,
            int left,
            int right,
            int depth,
            Direction front,
            BoundingBox members) {
        height = up + 1 + down;
        width = left + 1 + right;
        this.depth = depth;
        this.up = up;
        this.left = left;
        this.right = right;
        this.front = front;
        this.members = members;
        segments = new PileSegment[width];
        return this;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        for (PileChannel channel : fuelChannels) channel.ejectAll();
        if (BulkAction.active()) return;
        if (meltingDown) return;
        if (members == null || !(level instanceof ServerLevel server)) return;
        BoundingBox bounds = members;
        members = null;
        for (BlockPos cell : AssembledMembers.members(server, worldPosition, bounds)) {
            server.setBlockAndUpdate(cell, ModBlocks.PILE_BRICK.get().defaultBlockState());
        }
    }

    public List<PileChannel> getChannelList(PileChannelType type) {
        if (type == PileChannelType.FUEL) return fuelChannels;
        if (type == PileChannelType.VENTILATION) return ventilationChannels;
        return controlChannels;
    }

    public boolean drillChannel(BlockPos pos, Direction dir, Player player) {
        Role startRole = level.getBlockState(pos).getValue(PileBlock.ROLE);
        PileChannelType type = PileChannelType.getChannelType(dir, orientation);

        int size =
                type == PileChannelType.CONTROL
                        ? height
                        : type == PileChannelType.FUEL ? depth : width;

        List<PileChannel> list = getChannelList(type);

        if (startRole == Role.FUEL_IN || startRole == Role.AIR_IN || startRole == Role.CONTROL) {
            for (int i = 0; i < list.size(); i++) {
                PileChannel chan = list.get(i);
                if (chan.entry.pos().equals(pos) && chan.entry.dir() == dir) {
                    if (type == PileChannelType.FUEL) chan.ejectAll();
                    list.remove(i);
                    for (int j = 0; j < size; j++) {
                        setRole(pos.relative(dir, j), Role.DUMMY);
                    }
                    recalculateSegments();
                    setChanged();
                    WorldSounds.machine(
                            level,
                            null,
                            pos,
                            SoundEvents.ITEM_BREAK.value(),
                            SoundSource.BLOCKS,
                            1F,
                            0.75F);
                    return true;
                }
            }
        }

        PileError report = new PileError();
        for (int i = 0; i < size; i++) {
            BlockPos probe = pos.relative(dir, i);
            BlockState state = level.getBlockState(probe);
            if (!state.is(ModBlocks.PILE_BLOCK.get())) {
                report.add(probe, Component.translatable("marker.hbm.pile.foreign_block"));
                continue;
            }
            Role role = state.getValue(PileBlock.ROLE);
            if (role == Role.EDGE)
                report.add(probe, Component.translatable("marker.hbm.pile.drill_edge"));
            else if (role == Role.CORE)
                report.add(probe, Component.translatable("marker.hbm.pile.intersect_core"));
            else if (role == Role.CHANNEL)
                report.add(probe, Component.translatable("marker.hbm.pile.intersect_channel"));
            else if (role != Role.DUMMY)
                report.add(probe, Component.translatable("marker.hbm.pile.intersect_io"));
        }

        if (report.send(player)) return false;

        for (int i = 0; i < size; i++) {
            BlockPos probe = pos.relative(dir, i);
            if (i == 0) {
                if (type == PileChannelType.FUEL) setRole(probe, Role.FUEL_IN);
                if (type == PileChannelType.VENTILATION) setRole(probe, Role.AIR_IN);
                if (type == PileChannelType.CONTROL) setRole(probe, Role.CONTROL);
            } else if (i == size - 1) {
                if (type == PileChannelType.FUEL) setRole(probe, Role.FUEL_OUT);
                if (type == PileChannelType.VENTILATION) setRole(probe, Role.AIR_OUT);
                if (type == PileChannelType.CONTROL) setRole(probe, Role.CONTROL);
            } else {
                setRole(probe, Role.CHANNEL);
            }
        }

        list.add(new PileChannel(pos, dir, size, type));

        setChanged();
        recalculateSegments();
        WorldSounds.machine(
                level, null, pos, SoundEvents.ITEM_BREAK.value(), SoundSource.BLOCKS, 1F, 1.25F);

        return true;
    }

    private void setRole(BlockPos pos, Role role) {
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.PILE_BLOCK.get()))
            level.setBlock(pos, state.setValue(PileBlock.ROLE, role), 3);
    }

    @Override
    public void tickServer() {
        boolean changed = runSimulation();
        changed |= handleVentilation();
        handleMeltdown();
        if (changed) markChanged();
        networkPackNT(25);
    }

    @Override
    public long syncUnitMask() {
        return 1L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit != 0) throw new IllegalArgumentException();
        output.writeDouble(highestHeat);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit != 0) throw new IllegalArgumentException();
        hbm$syncSet$PileCoreBlockEntity$highestHeat(input.readDouble());
    }

    private boolean runSimulation() {
        boolean changed = false;
        for (PileChannel channel : fuelChannels) {
            if (channel.length <= 0) continue;
            double produced = 0D;
            for (int i = 0; i < channel.rods.length; i++) {
                ItemStack stack = channel.rods[i];
                if (stack.getItem() instanceof PileRodMK2Item) {
                    double flux =
                            PileRodMK2Item.getReactivity(
                                    stack, channel.incomingNeutrons / channel.length);
                    produced += flux;
                    channel.heat += flux * PileRodMK2Item.getHeatPerNeutron(stack);
                    channel.rods[i] = PileRodMK2Item.react(stack, flux);
                }
            }
            if (produced != 0D || channel.incomingNeutrons != 0D) changed = true;
            channel.outgoingNeutrons = produced;
            channel.incomingNeutrons = 0D;
        }

        for (PileSegment segment : segments) {
            if (segment == null || segment.type != PileChannelType.FUEL) continue;
            double outgoing = 0D;
            for (PileChannel channel : segment.channels) outgoing += channel.outgoingNeutrons;
            for (PileChannel channel : segment.channels) channel.incomingNeutrons += outgoing;
        }

        for (int i = 1; i < segments.length - 1; i++) {
            PileSegment segment = segments[i];
            if (segment == null || segment.type != PileChannelType.FUEL) continue;
            double outgoing = 0D;
            for (PileChannel channel : segment.channels) outgoing += channel.outgoingNeutrons;

            double multiplier = 1D;
            for (int j = i - 1; j >= 1; j--) {
                PileSegment neighbor = segments[j];
                if (neighbor == null) continue;
                multiplier *= neighbor.getNeutronMult(depth);
                if (neighbor.type == PileChannelType.FUEL)
                    for (PileChannel channel : neighbor.channels)
                        channel.incomingNeutrons += outgoing * multiplier;
            }

            multiplier = 1D;
            for (int j = i + 1; j < segments.length - 1; j++) {
                PileSegment neighbor = segments[j];
                if (neighbor == null) continue;
                multiplier *= neighbor.getNeutronMult(depth);
                if (neighbor.type == PileChannelType.FUEL)
                    for (PileChannel channel : neighbor.channels)
                        channel.incomingNeutrons += outgoing * multiplier;
            }
        }
        return changed;
    }

    private boolean handleVentilation() {
        boolean changed = false;
        for (PileChannel channel : ventilationChannels) {
            if (channel.air <= 0) continue;
            changed = true;
            double airCap = (double) channel.air / PileChannel.MAX_AIR;
            for (PileChannel fuel : fuelChannels) {
                if (Math.abs(fuel.entry.pos().getY() - channel.entry.pos().getY()) <= 1)
                    fuel.heat *= 1D - airCap * 0.05D;
            }
            channel.air -= (int) Math.ceil(airCap * 5D);
            if (!TickPhase.every(this, 3)) continue;

            Direction dir = channel.entry.dir();
            BlockPos entry = channel.entry.pos();
            double x = entry.getX() + 0.5D + dir.getStepX() * (width - 0.375D);
            double y = entry.getY() + 0.5D;
            double z = entry.getZ() + 0.5D + dir.getStepZ() * (width - 0.375D);
            RandomSource rand = level.getRandom();
            CoolingTowerParticleOptions options =
                    new CoolingTowerParticleOptions.Builder()
                            .setLift(1F)
                            .setBaseScale((0.125F + rand.nextFloat() * 0.125F) * (float) airCap)
                            .setMaxScale((float) airCap)
                            .setStrafe(0.0025F)
                            .noWind()
                            .setLife(20 + rand.nextInt(30))
                            .setColor(0xa0a0a0)
                            .build();
            ServerLevel server = (ServerLevel) level;
            for (ServerPlayer player : server.players()) {
                if (player.distanceToSqr(
                                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())
                        > 150D * 150D) continue;
                server.sendParticles(player, options, true, true, x, y, z, 0, 0D, 0D, 0D, 0D);
            }
        }

        for (PileChannel channel : fuelChannels) {
            if (channel.heat != 20D) changed = true;
            channel.heat *= 0.999D;
            if (channel.heat < 20D) channel.heat = 20D;
        }
        return changed;
    }

    private void handleMeltdown() {
        hbm$syncSet$PileCoreBlockEntity$highestHeat(0D);
        for (PileChannel channel : fuelChannels)
            hbm$syncSet$PileCoreBlockEntity$highestHeat(Math.max(highestHeat, channel.heat));
        if (highestHeat <= MAX_HEAT) return;

        double x = 0D;
        double z = 0D;
        for (PileChannel channel : fuelChannels) {
            Direction dir = channel.entry.dir();
            x += channel.entry.pos().getX() + 0.5D + dir.getStepX() * (channel.length - 1) / 2D;
            z += channel.entry.pos().getZ() + 0.5D + dir.getStepZ() * (channel.length - 1) / 2D;
        }
        x /= fuelChannels.size();
        z /= fuelChannels.size();
        double y = worldPosition.getY() + up;
        ServerLevel server = (ServerLevel) level;
        List<BlockPos> claimed =
                members == null
                        ? List.of()
                        : AssembledMembers.members(server, worldPosition, members);
        meltingDown = true;
        destroy();
        server.explode(null, x, y, z, 15F, true, Level.ExplosionInteraction.BLOCK);
        for (BlockPos cell : claimed) {
            if (server.getBlockState(cell).is(ModBlocks.PILE_BLOCK.get()))
                server.setBlockAndUpdate(cell, ModBlocks.PILE_BRICK.get().defaultBlockState());
            AssembledMembers.release(server, cell);
        }
        members = null;
        meltingDown = false;

        for (int i = 0; i < 15; i++) {
            double motionY = level.getRandom().nextDouble() * 0.5D + 1D;
            BulletBaseMK4Entity debris =
                    new BulletBaseMK4Entity(
                            level,
                            null,
                            XFactoryPile.debris,
                            100F,
                            0.35F,
                            x,
                            y + 1D,
                            z,
                            0D,
                            motionY,
                            0D);
            level.addFreshEntity(debris);
        }
    }

    public void recalculateSegments() {
        segments = new PileSegment[width];
        for (PileChannel channel : fuelChannels) addSegment(channel);
        for (PileChannel channel : controlChannels) addSegment(channel);
    }

    private void addSegment(PileChannel channel) {
        Direction rightDir = front.getClockWise();
        BlockPos entry = channel.entry.pos();
        int index =
                (entry.getX() - worldPosition.getX()) * rightDir.getStepX()
                        + (entry.getZ() - worldPosition.getZ()) * rightDir.getStepZ()
                        + left;
        if (index < 0 || index >= segments.length) return;
        PileSegment segment = segments[index];
        if (segment == null) segments[index] = segment = new PileSegment(channel.type);
        if (segment.type == channel.type) segment.channels.add(channel);
    }

    public void destroy() {
        level.setBlockAndUpdate(worldPosition, ModBlocks.PILE_BRICK.get().defaultBlockState());
    }

    public PileChannel readChannel(ValueInput input, String name) {
        int x = input.getIntOr(name + NBT_X, 0);
        int y = input.getIntOr(name + NBT_Y, 0);
        int z = input.getIntOr(name + NBT_Z, 0);
        Direction dir = Direction.VALUES[input.getByteOr(name + NBT_D, (byte) 0)];
        PileChannel channel = new PileChannel(new BlockPos(x, y, z), dir);
        if (channel.type == PileChannelType.FUEL) {
            for (int i = 0; i < channel.rods.length; i++) {
                channel.rods[i] =
                        input.read(name + "item" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY);
            }
            channel.heat = input.getDoubleOr(name + "heat", 0D);
            channel.incomingNeutrons = input.getDoubleOr(name + "neutrons", 0D);
        } else if (channel.type == PileChannelType.VENTILATION) {
            channel.air = input.getIntOr(name + "air", 0);
        } else {
            channel.control = input.getDoubleOr(name + "control", 1D);
        }
        return channel;
    }

    public enum PileOrientation {
        NORTH_SOUTH,
        EAST_WEST,
        NEITHER;

        public static PileOrientation getOrientation(Direction dir) {
            if (dir == Direction.NORTH || dir == Direction.SOUTH) return NORTH_SOUTH;
            if (dir == Direction.EAST || dir == Direction.WEST) return EAST_WEST;
            return NEITHER;
        }
    }

    public enum PileChannelType {
        FUEL("f"),
        VENTILATION("v"),
        CONTROL("c");

        private final String storagePrefix;
        private final String countKey;

        PileChannelType(String storagePrefix) {
            this.storagePrefix = storagePrefix;
            countKey = storagePrefix + "c";
        }

        public static PileChannelType getChannelType(
                Direction channelDir, PileOrientation pileOrientation) {

            if (channelDir == Direction.UP || channelDir == Direction.DOWN) {
                return PileChannelType.CONTROL;
            } else if (PileOrientation.getOrientation(channelDir) == pileOrientation) {
                return PileChannelType.FUEL;
            } else {
                return PileChannelType.VENTILATION;
            }
        }
    }

    public static class PileSegment {
        public final PileChannelType type;
        public final List<PileChannel> channels = new ArrayList<>();

        public PileSegment(PileChannelType type) {
            this.type = type;
        }

        public double getNeutronMult(int depth) {
            if (type != PileChannelType.CONTROL) return 1D;
            int size = depth - 1;
            if (size < 3) return 0D;
            double total = 0D;
            for (PileChannel channel : channels) total += channel.control;
            return Math.clamp(total / size, 0D, 0.5D);
        }
    }

    public class PileChannel {

        public static final int MAX_AIR = 1_000;
        public final DirPos entry;
        public final int length;
        public final PileChannelType type;
        public final ItemStack[] rods;
        public double heat;
        public double outgoingNeutrons;
        public double incomingNeutrons;
        public int air;
        public double control = 1D;

        public PileChannel(BlockPos pos, Direction dir) {
            entry = new DirPos(pos, dir);
            type = PileChannelType.getChannelType(dir, orientation);
            length =
                    type == PileChannelType.CONTROL
                            ? height
                            : type == PileChannelType.FUEL ? depth : width;
            rods = new ItemStack[length];
            Arrays.fill(rods, ItemStack.EMPTY);
        }

        public PileChannel(BlockPos pos, Direction dir, int length, PileChannelType type) {
            entry = new DirPos(pos, dir);
            this.type = type;
            this.length = length;
            rods = new ItemStack[length];
            Arrays.fill(rods, ItemStack.EMPTY);
        }

        public void writeChannel(ValueOutput output, String name) {
            output.putInt(name + NBT_X, entry.pos().getX());
            output.putInt(name + NBT_Y, entry.pos().getY());
            output.putInt(name + NBT_Z, entry.pos().getZ());
            output.putByte(name + NBT_D, (byte) entry.dir().get3DDataValue());
            if (type == PileChannelType.FUEL) {
                for (int i = 0; i < rods.length; i++) {
                    if (!rods[i].isEmpty())
                        output.store(name + "item" + i, ItemStack.CODEC, rods[i]);
                }
                output.putDouble(name + "heat", heat);
                output.putDouble(name + "neutrons", incomingNeutrons);
            } else if (type == PileChannelType.VENTILATION) {
                output.putInt(name + "air", air);
            } else {
                output.putDouble(name + "control", control);
            }
        }

        public void loadItem(ItemStack incoming) {
            if (incoming.isEmpty()) return;
            ItemStack shifted = incoming;
            for (int i = 0; i < rods.length; i++) {
                if (rods[i].isEmpty()) {
                    rods[i] = shifted;
                    setChanged();
                    return;
                }
                ItemStack previous = rods[i];
                rods[i] = shifted;
                shifted = previous;
            }
            dropItem(shifted, length);
            setChanged();
        }

        public void coreChanged() {
            markChanged();
        }

        public void ejectAll() {
            for (int i = 0; i < rods.length; i++) {
                dropItem(rods[i], length);
                rods[i] = ItemStack.EMPTY;
            }
            setChanged();
        }

        private void dropItem(ItemStack stack, int depth) {
            if (stack.isEmpty()) return;
            PileRodMK2Item.clearDepletion(stack);
            BlockPos pos = entry.pos().relative(entry.dir(), depth);
            level.addFreshEntity(
                    new ItemEntity(
                            level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack));
        }
    }

    public boolean captureBulkCells(ServerLevel level, Consumer<BlockPos> cells) {
        if (members != null) AssembledMembers.members(level, worldPosition, members).forEach(cells);
        return true;
    }

    public void clearBulkInventory() {
        for (var channel : fuelChannels) Arrays.fill(channel.rods, ItemStack.EMPTY);
    }

    public static void relocateBulkData(CompoundTag tag, BulkTransform transform) {
        BulkDataFields.bounds(tag, NBT_MEMBERS, transform);
        for (var type : PileChannelType.values()) {
            for (int i = 0; i < tag.getIntOr(type.countKey, 0); i++) {
                var name = type.storagePrefix + i;
                var pos =
                        transform.apply(
                                new BlockPos(
                                        tag.getInt(name + NBT_X).orElseThrow(),
                                        tag.getInt(name + NBT_Y).orElseThrow(),
                                        tag.getInt(name + NBT_Z).orElseThrow()));
                tag.putInt(name + NBT_X, pos.getX());
                tag.putInt(name + NBT_Y, pos.getY());
                tag.putInt(name + NBT_Z, pos.getZ());
            }
        }
    }

    public static void transformBulkData(CompoundTag tag, BulkTransform transform) {
        relocateBulkData(tag, transform);
        var front = transform.direction(Direction.VALUES[tag.getIntOr(NBT_FRONT, 2)]);
        tag.putInt(NBT_FRONT, front.get3DDataValue());
        if (tag.getIntOr(NBT_ORIENTATION, PileOrientation.NEITHER.ordinal())
                != PileOrientation.NEITHER.ordinal())
            tag.putInt(NBT_ORIENTATION, PileOrientation.getOrientation(front).ordinal());
        var x = transform.apply(new BlockPos(1, 0, 0));
        var z = transform.apply(new BlockPos(0, 0, 1));
        if (x.getX() * z.getZ() - x.getZ() * z.getX() < 0) {
            int left = tag.getIntOr(NBT_LEFT, 0);
            tag.putInt(NBT_LEFT, tag.getIntOr(NBT_RIGHT, 0));
            tag.putInt(NBT_RIGHT, left);
        }
        for (var type : PileChannelType.values()) {
            for (int i = 0; i < tag.getIntOr(type.countKey, 0); i++) {
                var key = type.storagePrefix + i + NBT_D;
                var direction = Direction.VALUES[tag.getByte(key).orElseThrow()];
                tag.putByte(key, (byte) transform.direction(direction).get3DDataValue());
            }
        }
    }

    public static CompoundTag publicBulkData(CompoundTag original) {
        var tag =
                BulkDataFields.copy(
                        original,
                        NBT_MEMBERS,
                        NBT_HEIGHT,
                        NBT_WIDTH,
                        NBT_DEPTH,
                        NBT_LEFT,
                        NBT_RIGHT,
                        NBT_UP,
                        NBT_ORIENTATION,
                        NBT_FRONT);
        for (var type : PileChannelType.values()) {
            int count = original.getIntOr(type.countKey, 0);
            tag.putInt(type.countKey, count);
            for (int i = 0; i < count; i++) {
                for (var suffix : CHANNEL_COORDINATES) {
                    var key = type.storagePrefix + i + suffix;
                    tag.put(key, original.get(key).copy());
                }
            }
        }
        return tag;
    }

    public final void hbm$syncSet$PileCoreBlockEntity$highestHeat(double next) {
        if (Double.doubleToRawLongBits(this.highestHeat) != Double.doubleToRawLongBits(next)) {
            this.highestHeat = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
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
    }
}
