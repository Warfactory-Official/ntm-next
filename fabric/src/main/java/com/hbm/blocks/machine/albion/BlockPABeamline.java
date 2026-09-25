// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.albion;

import com.hbm.blocks.multiblock.BlockMultiblockCell;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.interfaces.IToolable;
import com.hbm.tileentity.machine.albion.BlockEntityPABeamline;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockPABeamline extends BlockMultiblockCore implements EntityBlock, IToolable {

    public static final BooleanProperty WINDOW = BooleanProperty.create("window");

    private static final int[] DIMENSIONS = {0, 0, 0, 0, 1, 1};

    public BlockPABeamline(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState().setValue(WINDOW, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WINDOW);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public int cellSound() {
        return BlockMultiblockCell.SOUND_METAL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityPABeamline(pos, state);
    }

    @Override
    public boolean onScrew(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        if (level.isClientSide()) return true;

        BlockState state = level.getBlockState(pos);
        if (state.getBlock() == this) {
            level.setBlock(pos, state.setValue(WINDOW, !state.getValue(WINDOW)), UPDATE_ALL);
        }

        return false;
    }
}
