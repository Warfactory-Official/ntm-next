// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.api.fluidmk2.FluidCaps;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.AssembledMembers;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.capability.NtmContracts.Contract;
import com.hbm.capability.NtmContracts;
import com.hbm.platform.Services;
import com.hbm.registration.RegistryHandle;
import com.hbm.tileentity.machine.BlockEntityMachinePWRController;
import com.hbm.util.ChunkUtil;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

public class BlockPWR extends Block implements ICapabilityBlock, AssembledMembers.Member {

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    public BlockPWR(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(PART, Part.CASING));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    public static BlockState shellOf(BlockState original) {
        return ModBlocks.PWR_BLOCK
                .get()
                .defaultBlockState()
                .setValue(PART, Part.of(original.getBlock()));
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        BlockPos owner = AssembledMembers.owner(level, pos);
        AssembledMembers.release(level, pos);
        BlockState original = state.getValue(PART).original();

        if (level.getBlockState(pos) == original) return;
        level.setBlock(pos, original, UPDATE_ALL);
        BlockEntityMachinePWRController controller =
                owner == null
                        ? null
                        : ChunkUtil.blockEntityIfLoaded(
                                BlockEntityMachinePWRController.class, level, owner);
        if (controller != null) controller.disassemble();
    }

    @Override
    public void verify(ServerLevel level, BlockPos pos, BlockState state, BlockPos owner) {
        if (level.getBlockEntity(owner) instanceof BlockEntityMachinePWRController controller
                && controller.assembled) {
            return;
        }
        level.setBlock(pos, state.getValue(PART).original(), UPDATE_ALL);
    }

    @Override
    public int endpointDomains(BlockState state) {
        return state.getValue(PART) == Part.PORT ? MachineCaps.FLUID_IN | MachineCaps.FLUID_OUT : 0;
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().itemsAtCells();
    }

    public static void declareProxies() {
        NtmContracts.proxy(
                NtmContracts.INVENTORY,
                ModBlocks.PWR_BLOCK,
                (level, pos, state) -> {
                    BlockEntityMachinePWRController core = ioController(level, pos, state);
                    return core == null ? null : core.getBlockPos();
                });

        for (Contract<?> ror :
                List.of(NtmContracts.ROR_VALUE_PROVIDER, NtmContracts.ROR_INTERACTIVE)) {
            NtmContracts.proxy(
                    ror,
                    ModBlocks.PWR_BLOCK,
                    (level, pos, state) -> {
                        BlockEntityMachinePWRController core =
                                level instanceof ServerLevel
                                        ? ioController(level, pos, state)
                                        : walkToController(level, pos, state);
                        return core == null ? null : core.getBlockPos();
                    });
        }
    }

    public static @Nullable BlockEntityMachinePWRController ioController(
            Level level, BlockPos pos, BlockState state) {
        if (!isPort(state)) return null;
        if (!(level instanceof ServerLevel server)) return null;
        BlockPos owner = AssembledMembers.owner(server, pos);
        return owner == null
                ? null
                : ChunkUtil.blockEntityIfLoaded(
                        BlockEntityMachinePWRController.class, level, owner);
    }

    public static @Nullable BlockEntityMachinePWRController walkToController(
            Level level, BlockPos port, BlockState state) {
        if (!isPort(state)) return null;
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        seen.add(port);
        open.add(port);
        while (!open.isEmpty()) {
            BlockPos at = open.poll();
            for (Direction side : Direction.Plane.HORIZONTAL) {
                BlockPos candidate = at.relative(side);
                BlockState controller = level.getBlockState(candidate);
                if (controller.getBlock() instanceof MachinePWRController
                        && controller.getValue(MachinePWRController.FACING) == side
                        && level.getBlockEntity(candidate)
                                instanceof BlockEntityMachinePWRController be
                        && be.assembled) {
                    return be;
                }
            }
            for (Direction side : Direction.VALUES) {
                BlockPos next = at.relative(side);
                BlockState shell = level.getBlockState(next);
                if (shell.getBlock() instanceof BlockPWR
                        && MachinePWRController.isValidCore(
                                shell.getValue(PART).original().getBlock())
                        && seen.add(next)) {
                    open.add(next);
                }
            }
        }
        return null;
    }

    private static boolean isPort(BlockState state) {
        return state.getBlock() instanceof BlockPWR && state.getValue(PART) == Part.PORT;
    }

    @Override
    public void declareExtraCaps(RegistryHandle<? extends Block> self) {
        Services.CAPS.registerBlockProvider(
                FluidCaps.RECEIVER,
                self,
                (level, pos, state, be, face) -> portCap(level, pos, state, face.side()));
        Services.CAPS.registerBlockProvider(
                FluidCaps.PROVIDER,
                self,
                (level, pos, state, be, face) -> portCap(level, pos, state, face.side()));
    }

    private static @Nullable IFluidHandlerMK2 portCap(
            Level level, BlockPos pos, BlockState state, @Nullable Direction side) {
        return side == null ? null : ioController(level, pos, state);
    }

    public enum Part implements StringRepresentable {
        CASING("casing", () -> ModBlocks.PWR_CASING.get()),
        PORT("port", () -> ModBlocks.PWR_PORT.get()),
        REFLECTOR("reflector", () -> ModBlocks.PWR_REFLECTOR.get()),
        FUEL("fuel", () -> ModBlocks.PWR_FUEL.get()),
        CONTROL("control", () -> ModBlocks.PWR_CONTROL.get()),
        CHANNEL("channel", () -> ModBlocks.PWR_CHANNEL.get()),
        HEATEX("heatex", () -> ModBlocks.PWR_HEATEX.get()),
        HEATSINK("heatsink", () -> ModBlocks.PWR_HEATSINK.get()),
        NEUTRON_SOURCE("neutron_source", () -> ModBlocks.PWR_NEUTRON_SOURCE.get());

        private final String name;

        private final Supplier<Block> block;

        Part(String name, Supplier<Block> block) {
            this.name = name;
            this.block = block;
        }

        public BlockState original() {
            return block.get().defaultBlockState();
        }

        public static Part of(Block original) {
            for (Part part : values()) {
                if (part.block.get() == original) return part;
            }
            throw new IllegalArgumentException(String.valueOf(original));
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
