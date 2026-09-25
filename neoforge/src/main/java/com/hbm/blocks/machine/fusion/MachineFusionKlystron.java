// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.fusion;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionKlystron;
import com.hbm.tileentity.machine.fusion.FusionPorts;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class MachineFusionKlystron extends BlockFusionMachine implements ICapabilityBlock {

    private static final int[] PLACEMENT_DIMENSIONS = {
        3, 0, 4, 3, 2, 2, 0, 0, 0,
        4, -3, 4, 3, 1, 1, 0, 0, 0,
    };

    @Override
    protected int[] placementDimensionBoxes() {
        return PLACEMENT_DIMENSIONS;
    }

    private static final double[] PLACEMENT_EXTRAS = {1.5, 3.5, -4.5, -4.5, 1, -1};

    @Override
    protected double[] placementExtraBoxes() {
        return PLACEMENT_EXTRAS;
    }

    static final int[] DIMENSIONS = {3, 0, 4, 3, 2, 2};

    static final int[] WAVEGUIDE_BOX = {4, -3, 4, 3, 1, 1};

    public MachineFusionKlystron(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 3;
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placed, Direction dir, int o) {
        BlockPos origin = placed.offset(dir.getStepX() * o, dir.getStepY() * o, dir.getStepZ() * o);
        return super.checkRequirement(level, placed, dir, o)
                && MultiblockHandlerXR.checkSpace(level, origin, WAVEGUIDE_BOX, placed, dir);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        MultiblockHandlerXR.visitBox(core, WAVEGUIDE_BOX, facing, visitor);

        Direction rot = facing.getClockWise();
        visitor.cell(core.offset(facing.getStepX() * 3, 2, facing.getStepZ() * 3), MASK_SOUTH);
        visitor.cell(core.offset(rot.getStepX() * 2, 0, rot.getStepZ() * 2), MASK_WEST);
        visitor.cell(core.offset(-rot.getStepX() * 2, 0, -rot.getStepZ() * 2), MASK_EAST);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        visitor.passiveCell(
                core.offset(facing.getStepX() * 3, 2, facing.getStepZ() * 3), MASK_ALL, domains);
        visitor.passiveCell(
                core.offset(rot.getStepX() * 2, 0, rot.getStepZ() * 2), MASK_ALL, domains);
        visitor.passiveCell(
                core.offset(-rot.getStepX() * 2, 0, -rot.getStepZ() * 2), MASK_ALL, domains);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.SUCCESS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(core) instanceof MenuProvider menu)
            openCoreMenu(player, core, menu);
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityFusionKlystron(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.FUSION_KLYSTRON).powerIn().fluidIn().items().fe();
    }

    @Override
    public List<FusionPorts.Port> links(BlockPos core, Direction facing) {
        return BlockEntityFusionKlystron.links(core, facing);
    }
}
