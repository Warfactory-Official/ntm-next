// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.BlockEntityMachineArcFurnace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class MachineArcFurnace extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {

    private static final int[] DIMENSIONS = {4, 0, 2, 2, 2, 2};

    public MachineArcFurnace(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 2;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        MultiblockHandlerXR.visitBox(core, DIMENSIONS, facing, visitor);

        Direction rot = facing.getClockWise();

        visitor.cell(core.relative(facing, 2).relative(rot), MASK_SOUTH);
        visitor.cell(core.relative(facing, 2).relative(rot, -1), MASK_SOUTH);
        visitor.cell(core.relative(rot, 2).relative(facing), MASK_WEST);
        visitor.cell(core.relative(rot, 2).relative(facing, -1), MASK_WEST);
        visitor.cell(core.relative(rot, -2).relative(facing), MASK_EAST);
        visitor.cell(core.relative(rot, -2).relative(facing, -1), MASK_EAST);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        visitor.passiveCell(
                core.relative(facing, 2).relative(rot), MASK_ALL, PASSIVE_POWER_IN | PASSIVE_ITEMS);
        visitor.passiveCell(
                core.relative(facing, 2).relative(rot, -1),
                MASK_ALL,
                PASSIVE_POWER_IN | PASSIVE_ITEMS);
        visitor.passiveCell(
                core.relative(rot, 2).relative(facing), MASK_ALL, PASSIVE_POWER_IN | PASSIVE_ITEMS);
        visitor.passiveCell(
                core.relative(rot, 2).relative(facing, -1),
                MASK_ALL,
                PASSIVE_POWER_IN | PASSIVE_ITEMS);
        visitor.passiveCell(
                core.relative(rot, -2).relative(facing),
                MASK_ALL,
                PASSIVE_POWER_IN | PASSIVE_ITEMS);
        visitor.passiveCell(
                core.relative(rot, -2).relative(facing, -1),
                MASK_ALL,
                PASSIVE_POWER_IN | PASSIVE_ITEMS);
    }

    @Override
    protected InteractionResult useItemOnAtCore(
            ItemStack held,
            BlockState coreState,
            Level level,
            BlockPos core,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!held.is(ItemTags.SHOVELS)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(core) instanceof BlockEntityMachineArcFurnace furnace))
            return InteractionResult.PASS;

        furnace.spillLiquids(player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineArcFurnace(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.ARC_FURNACE_LARGE)
                .powerIn()
                .items()
                .itemsAtCells()
                .fe();
    }
}
