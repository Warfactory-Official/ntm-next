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
import com.hbm.tileentity.bomb.BlockEntityCompactLauncher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

@RefreshesNeighborState(be = BlockEntityCompactLauncher.class, calling = "refreshRedstone")
public class CompactLauncher extends BlockMultiblockCore
        implements ITickingBlock, IBomb, ICapabilityBlock {

    private static final int[] DIMENSIONS = {0, 0, 1, 1, 1, 1};

    public CompactLauncher(Properties props) {
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
        Direction rot = facing.getClockWise();
        visitor.cell(core.relative(rot).relative(facing), MASK_WEST | MASK_SOUTH | MASK_DOWN);
        visitor.cell(
                core.relative(rot).relative(facing.getOpposite()),
                MASK_WEST | MASK_NORTH | MASK_DOWN);
        visitor.cell(
                core.relative(rot.getOpposite()).relative(facing),
                MASK_EAST | MASK_SOUTH | MASK_DOWN);
        visitor.cell(
                core.relative(rot.getOpposite()).relative(facing.getOpposite()),
                MASK_EAST | MASK_NORTH | MASK_DOWN);
    }

    private static final VoxelShape DECK = Shapes.box(0, 1 - 1.0E-4, 0, 1, 1, 1);

    @Override
    public @Nullable VoxelShape cellShape(
            int lx, int ly, int lz, Direction facing, boolean open, boolean forCollision) {
        return lx == 0 || lz == 0 ? DECK : null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityCompactLauncher(pos, state);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        FoldedOwner owner = ownerOf(level, pos);
        BlockPos core = owner != null ? owner.pos() : pos;
        if (level.getBlockEntity(core) instanceof BlockEntityCompactLauncher launcher) {
            if (!launcher.canLaunch()) return BombReturnCode.ERROR_MISSING_COMPONENT;
            return launcher.launchFromDesignator();
        }
        return BombReturnCode.UNDEFINED;
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.SMALL_LAUNCHER)
                .powerIn()
                .fluidIn()
                .items()
                .fe()
                .faces(BlockEntityCompactLauncher.class, (be, side) -> side != Direction.UP)
                .fluidFaces(
                        BlockEntityCompactLauncher.class,
                        (be, face) -> face.side() != Direction.UP);
    }
}
