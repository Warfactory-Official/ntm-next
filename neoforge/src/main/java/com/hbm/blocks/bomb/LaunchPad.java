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
import com.hbm.tileentity.bomb.BlockEntityLaunchPad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

@RefreshesNeighborState(be = BlockEntityLaunchPad.class, calling = "refreshRedstone")
public class LaunchPad extends BlockMultiblockCore
        implements ITickingBlock, IBomb, ICapabilityBlock {

    private static final int[] DIMENSIONS = {0, 0, 1, 1, 1, 1};

    public LaunchPad(Properties props) {
        super(props);
        this.bounding.add(new AABB(-1.5D, 0D, -1.5D, -0.5D, 1D, -0.5D));
        this.bounding.add(new AABB(0.5D, 0D, -1.5D, 1.5D, 1D, -0.5D));
        this.bounding.add(new AABB(-1.5D, 0D, 0.5D, -0.5D, 1D, 1.5D));
        this.bounding.add(new AABB(0.5D, 0D, 0.5D, 1.5D, 1D, 1.5D));
        this.bounding.add(new AABB(-0.5D, 0.5D, -1.5D, 0.5D, 1D, 1.5D));
        this.bounding.add(new AABB(-1.5D, 0.5D, -0.5D, 1.5D, 1D, 0.5D));
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction rot = facing.getClockWise();
        visitor.cell(core.relative(rot).relative(facing), MASK_WEST | MASK_SOUTH);
        visitor.cell(core.relative(rot).relative(facing.getOpposite()), MASK_WEST | MASK_NORTH);
        visitor.cell(core.relative(rot.getOpposite()).relative(facing), MASK_EAST | MASK_SOUTH);
        visitor.cell(
                core.relative(rot.getOpposite()).relative(facing.getOpposite()),
                MASK_EAST | MASK_NORTH);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        visitor.passiveCell(core.relative(rot).relative(facing), MASK_ALL, domains);
        visitor.passiveCell(core.relative(rot).relative(facing.getOpposite()), MASK_ALL, domains);
        visitor.passiveCell(core.relative(rot.getOpposite()).relative(facing), MASK_ALL, domains);
        visitor.passiveCell(
                core.relative(rot.getOpposite()).relative(facing.getOpposite()), MASK_ALL, domains);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityLaunchPad(pos, state);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        BlockPos core = ownerOf(level, pos) != null ? ownerOf(level, pos).pos() : pos;
        if (level.getBlockEntity(core) instanceof BlockEntityLaunchPad pad) {
            return pad.launchFromDesignator();
        }
        return BombReturnCode.UNDEFINED;
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.LAUNCH1)
                .powerIn()
                .fluidIn()
                .items()
                .fe()
                .faces(BlockEntityLaunchPad.class, (be, side) -> side.getAxis().isHorizontal())
                .fluidFaces(
                        BlockEntityLaunchPad.class,
                        (be, face) -> face.side().getAxis().isHorizontal());
    }
}
