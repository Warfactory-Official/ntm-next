// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.api.energymk2.EnergyCaps;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.AssembledMembers;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.platform.Services;
import com.hbm.registration.RegistryHandle;
import com.hbm.tileentity.machine.BlockEntityMachineICFController;
import com.hbm.util.ChunkUtil;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

public class BlockICF extends Block implements ICapabilityBlock, AssembledMembers.Member {

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    public BlockICF(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(PART, Part.CASING));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    public static BlockState shellOf(BlockState original) {
        return ModBlocks.ICF_BLOCK
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
        BlockEntityMachineICFController controller =
                owner == null
                        ? null
                        : ChunkUtil.blockEntityIfLoaded(
                                BlockEntityMachineICFController.class, level, owner);
        if (controller != null) controller.disassemble();
    }

    @Override
    public void verify(ServerLevel level, BlockPos pos, BlockState state, BlockPos owner) {
        if (level.getBlockEntity(owner) instanceof BlockEntityMachineICFController controller
                && controller.assembled) {
            return;
        }
        level.setBlock(pos, state.getValue(PART).original(), UPDATE_ALL);
    }

    @Override
    public int endpointDomains(BlockState state) {
        return state.getValue(PART) == Part.PORT ? MachineCaps.POWER_IN : 0;
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().powerIn().selfProvided();
    }

    @Override
    public void declareExtraCaps(RegistryHandle<? extends Block> self) {
        Services.CAPS.registerBlockProvider(EnergyCaps.RECEIVER, self, BlockICF::portCap);
    }

    private static @Nullable IEnergyHandlerMK2 portCap(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity be,
            @Nullable Direction side) {
        if (side == null
                || state.getValue(PART) != Part.PORT
                || !(level instanceof ServerLevel server)) return null;
        BlockPos owner = AssembledMembers.owner(server, pos);
        return owner == null
                ? null
                : ChunkUtil.blockEntityIfLoaded(
                        BlockEntityMachineICFController.class, level, owner);
    }

    public enum Part implements StringRepresentable {
        CASING("casing", () -> ModBlocks.ICF_CASING.get()),
        PORT("port", () -> ModBlocks.ICF_PORT.get()),
        CELL("cell", () -> ModBlocks.ICF_CELL.get()),
        EMITTER("emitter", () -> ModBlocks.ICF_EMITTER.get()),
        CAPACITOR("capacitor", () -> ModBlocks.ICF_CAPACITOR.get()),
        TURBOCHARGER("turbocharger", () -> ModBlocks.ICF_TURBOCHARGER.get());

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
