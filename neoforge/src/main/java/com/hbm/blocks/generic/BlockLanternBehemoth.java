// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCell;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.RepairLookOverlay;
import com.hbm.extprop.HbmPlayerProps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.interfaces.IRepairable;
import com.hbm.interfaces.IToolable;
import com.hbm.tileentity.BlockEntityLanternBehemoth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class BlockLanternBehemoth extends BlockMultiblockCore
        implements ITickingBlock, IToolable, ILookOverlay {

    public static final BooleanProperty BROKEN = BooleanProperty.create("broken");
    private static final int[] DIMENSIONS = {4, 0, 0, 0, 0, 0};

    public BlockLanternBehemoth(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BROKEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BROKEN);
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
    public boolean onScrew(
            Level level,
            Player player,
            BlockPos core,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.TORCH || !IRepairable.tryRepairMultiblock(level, core, player))
            return false;
        if (!level.isClientSide()) {
            HbmPlayerProps props = HbmPlayerProps.getData(player);
            if (props.reputation < 25) props.reputation++;
        }
        return true;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        RepairLookOverlay.build(level, pos, info);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityLanternBehemoth(pos, state);
    }
}
