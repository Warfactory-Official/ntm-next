// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.lib;

import com.hbm.NuclearTech;
import com.hbm.api.energymk2.CableData;
import com.hbm.api.energymk2.EnergyCaps;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.api.fluidmk2.FluidCaps;
import com.hbm.api.fluidmk2.FluidFace;
import com.hbm.api.fluidmk2.FluidPipeGraph;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.api.fluidmk2.PipeData;
import com.hbm.blocks.machine.MachineRTG;
import com.hbm.blocks.machine.rbmk.RBMKLoader;
import com.hbm.blocks.network.CableBlock;
import com.hbm.blocks.network.CableConductorBlockBase;
import com.hbm.blocks.network.CableDiodeBlock;
import com.hbm.blocks.network.FluidDuctBlockBase;
import com.hbm.blocks.network.PylonMediumBlock;
import com.hbm.capability.NtmCapabilities;
import com.hbm.platform.Services;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.util.ChunkUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;

public final class Library {

    private Library() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NuclearTech.MOD_ID, path);
    }

    public static Identifier resolve(String reference) {
        return reference.indexOf(':') < 0 ? id(reference) : Identifier.parse(reference);
    }

    public static float roundFloat(double number, int decimal) {
        double pow = Math.pow(10, decimal);
        return (float) (Math.round(number * pow) / pow);
    }

    public static BlockState predictArm(
            BlockState state, BooleanProperty arm, BlockState neighbour, boolean joins) {
        if (joins) return state.setValue(arm, true);
        return neighbour.isAir() ? state.setValue(arm, false) : state;
    }

    public static void redrawArms(BlockState state, ServerLevel level, BlockPos pos) {
        BlockState drawn = state;
        BlockPos.MutableBlockPos neighbour = new BlockPos.MutableBlockPos();
        for (Direction dir : Direction.VALUES) {
            neighbour.setWithOffset(pos, dir);
            if (level.isLoaded(neighbour)) {
                drawn =
                        drawn.updateShape(
                                level,
                                level,
                                pos,
                                dir,
                                neighbour,
                                level.getBlockState(neighbour),
                                level.getRandom());
            }
        }
        Block.updateOrDestroy(state, drawn, level, pos, Block.UPDATE_CLIENTS);
    }

    public static boolean armDrawerBeside(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos neighbour = new BlockPos.MutableBlockPos();
        for (Direction dir : Direction.VALUES) {
            neighbour.setWithOffset(pos, dir);
            BlockState state = ChunkUtil.blockStateIfLoaded(level, neighbour);
            if (state != null
                    && (state.getBlock() instanceof CableDiodeBlock
                            || state.getBlock() instanceof MachineRTG)) {
                return true;
            }
        }
        return false;
    }

    public static boolean canConnect(LevelReader level, BlockPos pos, Direction side) {
        if (!(level instanceof Level world)) return false;

        var state = ChunkUtil.blockStateIfLoaded(world, pos);
        if (state != null) {
            if (state.getBlock() instanceof CableBlock) return true;
            if (state.getBlock() instanceof CableConductorBlockBase cable)
                return cable.canConnectCable(state, side);
            if (state.getBlock() instanceof PylonMediumBlock pylon)
                return pylon.canConnectCable(state, side);
        }

        if (world instanceof ServerLevel server) {
            LevelNodeGraph<CableData> graph = PowerGraph.get(server);
            if (graph.containsCell(pos.asLong())) return graph.isOpenAt(pos.asLong(), side);
        }
        if (state == null) return false;

        if (Services.CAPS.find(EnergyCaps.PROVIDER, world, pos, side) != null
                || Services.CAPS.find(EnergyCaps.RECEIVER, world, pos, side) != null) {
            return true;
        }

        if (NtmCapabilities.ownsEnergyExposure(state)) return false;

        return Services.CAPS.findEnergyHandler(world, pos, side) != null;
    }

    public static boolean canConnectFluid(
            LevelReader level, BlockPos pos, Direction side, Fluid fluid) {
        if (!(level instanceof Level world)) return false;

        var state = ChunkUtil.blockStateIfLoaded(world, pos);
        if (state != null && state.getBlock() instanceof RBMKLoader) {
            return RBMKLoader.canConnectFluid(side, fluid);
        }
        if (state != null && state.getBlock() instanceof FluidDuctBlockBase duct) {
            return duct.canConnectFluid(world, pos, side, fluid);
        }

        if (world instanceof ServerLevel server) {
            LevelNodeGraph<PipeData> graph = FluidPipeGraph.get(server, fluid);
            if (graph.containsCell(pos.asLong())) {
                PipeData data = graph.dataAt(pos.asLong());
                return data != null && data.fluid() == fluid && graph.isOpenAt(pos.asLong(), side);
            }
        }
        if (state == null) return false;

        FluidFace face = FluidFace.of(side, fluid);
        if (Services.CAPS.find(FluidCaps.PROVIDER, world, pos, face) != null) return true;
        if (Services.CAPS.find(FluidCaps.RECEIVER, world, pos, face) != null) return true;

        if (NtmCapabilities.ownsFluidExposure(state)) return false;
        return Services.CAPS.findFluidHandler(world, pos, side) != null;
    }

    public static int blockPosToLocal(long serialized) {
        return ((int) (serialized & 0xFL) << 8)
                | (((int) (serialized >>> 12) & 0xF) << 4)
                | ((int) (serialized >>> 38) & 0xF);
    }

    public static int packLocal(int localX, int localY, int localZ) {
        return (localY << 8) | (localZ << 4) | localX;
    }

    public static int getLocalX(int packed) {
        return packed & 0xF;
    }

    public static int getLocalY(int packed) {
        return (packed >>> 8) & 0xF;
    }

    public static int getLocalZ(int packed) {
        return (packed >>> 4) & 0xF;
    }

    public static long sectionToLong(long ck, int subY) {
        return (ck << 42) | ((ck >>> 12) & 0x0000_03FF_FFF0_0000L) | (((long) subY) & 0xFFFFFL);
    }

    public static long setSectionY(long key, int subY) {
        return (key & ~0xFFFFFL) | (((long) subY) & 0xFFFFFL);
    }

    public static long shiftSectionX(long key, int dx) {
        return (key & ~(0x3FFFFFL << 42)) | (((key >>> 42) + dx & 0x3FFFFFL) << 42);
    }

    public static long shiftSectionZ(long key, int dz) {
        return (key & ~(0x3FFFFFL << 20)) | ((((key >>> 20) & 0x3FFFFFL) + dz & 0x3FFFFFL) << 20);
    }

    public static long sectionToChunkLong(long sck) {
        return (((sck << 22) >> 10) & 0xFFFF_FFFF_0000_0000L) | ((sck >> 42) & 0xFFFF_FFFFL);
    }

    public static List<int[]> getBlockPosInPath(int x, int y, int z, int length, Vec3 vec0) {
        List<int[]> list = new ArrayList<>();

        for (int i = 0; i <= length; i++) {
            list.add(new int[] {(int) (x + (vec0.x * i)), y, (int) (z + (vec0.z * i)), i});
        }

        return list;
    }
}
