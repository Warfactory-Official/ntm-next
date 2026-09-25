// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.fusion;

import com.hbm.blocks.machine.fusion.BlockFusionMachine;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.lib.Library;
import com.hbm.platform.Services;
import com.hbm.registration.RegistryHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jspecify.annotations.Nullable;

public final class FusionPorts {

    public static BlockCapability<BlockEntity, Direction> KLYSTRON;
    public static BlockCapability<BlockEntity, Direction> PLASMA;

    private FusionPorts() {}

    public static void register() {

        KLYSTRON =
                Services.CAPS.createToken(
                        Library.id("fusion_klystron"), BlockEntity.class, Direction.class);
        PLASMA =
                Services.CAPS.createToken(
                        Library.id("fusion_plasma"), BlockEntity.class, Direction.class);
    }

    public static BlockCapability<BlockEntity, Direction> token(Kind kind) {
        return kind == Kind.KLYSTRON ? KLYSTRON : PLASMA;
    }

    public static @Nullable BlockEntity peer(ServerLevel level, Port port) {
        return Services.CAPS.find(
                token(port.kind()),
                level,
                port.cell().relative(port.face()),
                port.face().getOpposite());
    }

    public static <T> @Nullable T peer(ServerLevel level, Port port, Class<T> type) {
        BlockEntity be = peer(level, port);
        return type.isInstance(be) ? type.cast(be) : null;
    }

    public static void declareAtCell(RegistryHandle<? extends Block> cell) {
        for (Kind kind : Kind.values()) {
            Services.CAPS.registerBlockProvider(
                    token(kind),
                    cell,
                    (level, pos, state, be, side) -> portCap(level, pos, side, kind));
        }
    }

    private static @Nullable BlockEntity portCap(
            Level level, BlockPos pos, @Nullable Direction side, Kind kind) {
        if (side == null) return null;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null) return null;
        BlockState coreState = level.getBlockState(core);
        if (!(coreState.getBlock() instanceof BlockFusionMachine machine)) return null;
        BlockEntity coreBe = level.getBlockEntity(core);
        if (coreBe == null || coreBe.isRemoved()) return null;
        Direction facing = coreState.getValue(BlockMultiblockCore.FACING);
        return exposes(machine.links(core, facing), kind, pos, side) ? coreBe : null;
    }

    public static boolean exposes(
            Iterable<Port> ports, Kind kind, BlockPos pos, @Nullable Direction side) {
        if (side == null) return false;
        for (Port port : ports) {
            if (port.kind() == kind && port.face() == side && port.cell().equals(pos)) return true;
        }
        return false;
    }

    public enum Kind {
        KLYSTRON,
        PLASMA
    }

    public record Port(Kind kind, BlockPos cell, Direction face) {}
}
