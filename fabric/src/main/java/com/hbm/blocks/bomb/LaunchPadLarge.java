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
import com.hbm.tileentity.bomb.BlockEntityLaunchPadLarge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

@RefreshesNeighborState(be = BlockEntityLaunchPadLarge.class, calling = "refreshRedstone")
public class LaunchPadLarge extends BlockMultiblockCore
        implements ITickingBlock, IBomb, ICapabilityBlock {

    private static final int[] DIMENSIONS = {0, 0, 4, 4, 4, 4};
    private static final int RADIUS = 4;

    private static final int[] ARMS = {-2, 2};

    public LaunchPadLarge(Properties props) {
        super(props);
        this.bounding.add(new AABB(-4.5D, 0D, -4.5D, 4.5D, 1D, -0.5D));
        this.bounding.add(new AABB(-4.5D, 0D, 0.5D, 4.5D, 1D, 4.5D));
        this.bounding.add(new AABB(-4.5D, 0.875D, -0.5D, 4.5D, 1D, 0.5D));
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return RADIUS;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {

        super.visitCells(core, facing, visitor);
        for (int arm : ARMS) {
            visitor.cell(core.offset(RADIUS, 0, arm), MASK_EAST);
            visitor.cell(core.offset(-RADIUS, 0, arm), MASK_WEST);
            visitor.cell(core.offset(arm, 0, RADIUS), MASK_SOUTH);
            visitor.cell(core.offset(arm, 0, -RADIUS), MASK_NORTH);
        }
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        passiveEverywhere(core, facing, visitor, PASSIVE_ITEMS);
        int supplied = PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        for (int arm : ARMS) {
            visitor.passiveCell(core.offset(RADIUS, 0, arm), MASK_ALL, supplied);
            visitor.passiveCell(core.offset(-RADIUS, 0, arm), MASK_ALL, supplied);
            visitor.passiveCell(core.offset(arm, 0, RADIUS), MASK_ALL, supplied);
            visitor.passiveCell(core.offset(arm, 0, -RADIUS), MASK_ALL, supplied);
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {

        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityLaunchPadLarge(pos, state);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        FoldedOwner owner = ownerOf(level, pos);
        BlockPos core = owner != null ? owner.pos() : pos;
        if (level.getBlockEntity(core) instanceof BlockEntityLaunchPadLarge pad) {
            return pad.launchFromDesignator();
        }
        return BombReturnCode.UNDEFINED;
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.LAUNCHPAD_LARGE)
                .powerIn()
                .fluidIn()
                .items()
                .fe()
                .faces(BlockEntityLaunchPadLarge.class, (be, side) -> side.getAxis().isHorizontal())
                .fluidFaces(
                        BlockEntityLaunchPadLarge.class,
                        (be, face) -> face.side().getAxis().isHorizontal());
    }
}
