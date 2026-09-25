// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.turret;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.items.tool.ItemDesignatorArtyRange;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public abstract class TurretBaseArtillery extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {

    private static final int[] DIMENSIONS = {1, 0, 2, 1, 2, 1};

    private static final int MIN_X = -2;
    private static final int MAX_X = 1;
    private static final int MIN_Z = -2;
    private static final int MAX_Z = 1;
    private static final int MAX_Y = 1;

    protected TurretBaseArtillery(Properties props) {
        super(props);
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

        for (int ly = 0; ly <= MAX_Y; ly++) {
            for (int lz = MIN_Z; lz <= MAX_Z; lz++) {
                for (int lx = MIN_X; lx <= MAX_X; lx++) {
                    int mask = MASK_NONE;
                    if (lx == MIN_X) mask |= MASK_WEST;
                    if (lx == MAX_X) mask |= MASK_EAST;
                    if (lz == MIN_Z) mask |= MASK_NORTH;
                    if (lz == MAX_Z) mask |= MASK_SOUTH;
                    if (mask == MASK_NONE) continue;

                    visitor.cell(
                            core.offset(
                                    MultiblockMaskTable.worldX(lx, ly, lz, facing),
                                    ly,
                                    MultiblockMaskTable.worldZ(lx, ly, lz, facing)),
                            mask);
                }
            }
        }
    }

    @Override
    public int coreMask() {
        return MASK_NONE;
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        passiveEverywhere(core, facing, visitor, PASSIVE_POWER_IN | PASSIVE_ITEMS);
    }

    @Override
    protected InteractionResult useItemOnAtCore(
            ItemStack held,
            BlockState state,
            Level level,
            BlockPos core,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {

        if (held.getItem() instanceof ItemDesignatorArtyRange) return InteractionResult.PASS;
        return super.useItemOnAtCore(held, state, level, core, player, hand, hit);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.SUCCESS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(core) instanceof MenuProvider menu)
            openCoreMenu(player, core, menu);
        return InteractionResult.SUCCESS;
    }
}
