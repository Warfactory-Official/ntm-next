// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.RefreshesNeighborState;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.IBomb;
import com.hbm.tileentity.bomb.BlockEntityLaunchTable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

@RefreshesNeighborState(be = BlockEntityLaunchTable.class, calling = "refreshRedstone")
public class LaunchTable extends BlockMultiblockCore
        implements ITickingBlock, IBomb, ICapabilityBlock {

    private static final int[] DIMENSIONS = {0, 0, 4, 4, 4, 4};
    private static final int RADIUS = 4;

    private static final int SCAFFOLD_OFFSET = 3;
    private static final int SCAFFOLD_HEIGHT = 12;

    private static final VoxelShape TRENCH_GRATING = Shapes.box(0, 1 - 1.0E-4, 0, 1, 1, 1);

    public LaunchTable(Properties props) {
        super(props);
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
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {

        super.visitCells(core, facing, visitor);

        for (int i = -RADIUS; i <= RADIUS; i++) {
            int corner = Math.abs(i) == RADIUS ? (i > 0 ? MASK_EAST : MASK_WEST) : MASK_NONE;
            visitor.cell(core.offset(i, 0, RADIUS), MASK_SOUTH | corner);
            visitor.cell(core.offset(i, 0, -RADIUS), MASK_NORTH | corner);
            if (corner != MASK_NONE) continue;
            visitor.cell(core.offset(RADIUS, 0, i), MASK_EAST);
            visitor.cell(core.offset(-RADIUS, 0, i), MASK_WEST);
        }
    }

    @Override
    public @Nullable VoxelShape cellShape(
            int lx, int ly, int lz, Direction facing, boolean open, boolean forCollision) {
        return lx == 0 && lz != 0 ? TRENCH_GRATING : null;
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;

        BlockState core = level.getBlockState(pos);
        if (core.getBlock() != this) return;
        BlockPos foot = pos.relative(core.getValue(FACING).getClockWise(), SCAFFOLD_OFFSET);
        for (int i = 1; i < SCAFFOLD_HEIGHT; i++) level.removeBlock(foot.above(i), false);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {

        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityLaunchTable(pos, state);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        FoldedOwner owner = ownerOf(level, pos);
        BlockPos core = owner != null ? owner.pos() : pos;
        if (level.getBlockEntity(core) instanceof BlockEntityLaunchTable table) {
            if (!table.canLaunch()) return BombReturnCode.ERROR_MISSING_COMPONENT;
            return table.launchFromDesignator();
        }
        return BombReturnCode.UNDEFINED;
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.LARGE_LAUNCH_TABLE)
                .powerIn()
                .fluidIn()
                .items()
                .fe()
                .faces(BlockEntityLaunchTable.class, (be, side) -> side.getAxis().isHorizontal())
                .fluidFaces(
                        BlockEntityLaunchTable.class,
                        (be, face) -> face.side().getAxis().isHorizontal());
    }
}
