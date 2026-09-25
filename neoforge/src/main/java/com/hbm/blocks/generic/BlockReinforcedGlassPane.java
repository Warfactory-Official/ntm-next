// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public class BlockReinforcedGlassPane extends IronBarsBlock {

    public static final MapCodec<BlockReinforcedGlassPane> CODEC =
            simpleCodec(BlockReinforcedGlassPane::new);

    public BlockReinforcedGlassPane(Properties props) {
        super(props);
    }

    @Override
    public MapCodec<? extends IronBarsBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return false;
    }

    private boolean attachsToReinforced(BlockState state, boolean faceSolid) {
        return attachsTo(state, faceSolid)
                || state.is(ModBlocks.REINFORCED_GLASS.get())
                || state.is(ModBlocks.REINFORCED_LAMINATE.get())
                || state.is(ModBlocks.GLASS_QUARTZ.get())
                || state.is(ModBlocks.GLASS_BORON.get())
                || state.is(ModBlocks.GLASS_LEAD.get());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockGetter level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        FluidState fluid = ctx.getLevel().getFluidState(pos);
        BlockPos north = pos.north(), south = pos.south(), west = pos.west(), east = pos.east();
        BlockState northState = level.getBlockState(north);
        BlockState southState = level.getBlockState(south);
        BlockState westState = level.getBlockState(west);
        BlockState eastState = level.getBlockState(east);
        return defaultBlockState()
                .setValue(
                        NORTH,
                        attachsToReinforced(
                                northState, northState.isFaceSturdy(level, north, Direction.SOUTH)))
                .setValue(
                        SOUTH,
                        attachsToReinforced(
                                southState, southState.isFaceSturdy(level, south, Direction.NORTH)))
                .setValue(
                        WEST,
                        attachsToReinforced(
                                westState, westState.isFaceSturdy(level, west, Direction.EAST)))
                .setValue(
                        EAST,
                        attachsToReinforced(
                                eastState, eastState.isFaceSturdy(level, east, Direction.WEST)))
                .setValue(WATERLOGGED, fluid.is(Fluids.WATER));
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction dir,
            BlockPos neighborPos,
            BlockState neighborState,
            RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (!dir.getAxis().isHorizontal()) {
            return super.updateShape(
                    state, level, ticks, pos, dir, neighborPos, neighborState, random);
        }
        return state.setValue(
                PROPERTY_BY_DIRECTION.get(dir),
                attachsToReinforced(
                        neighborState,
                        neighborState.isFaceSturdy(level, neighborPos, dir.getOpposite())));
    }
}
