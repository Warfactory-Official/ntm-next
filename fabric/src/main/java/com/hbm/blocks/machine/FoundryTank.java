// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.items.machine.ItemScraps;
import com.hbm.tileentity.machine.BlockEntityFoundryTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class FoundryTank extends Block implements ITickingBlock {

    private static final VoxelShape SUPPORT_SHAPE =
            Shapes.join(
                    Shapes.block(),
                    Shapes.box(0.0625D, 0.9375D, 0.0625D, 0.9375D, 1.0D, 0.9375D),
                    BooleanOp.ONLY_FIRST);

    public FoundryTank(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return SUPPORT_SHAPE;
    }

    public static int mask(BlockGetter level, BlockPos pos) {
        int mask = 0;
        for (Direction dir : Direction.VALUES) {
            BlockState neighbour = level.getBlockState(pos.relative(dir));
            if (neighbour.is(ModBlocks.FOUNDRY_TANK.get())) {
                mask |= 1 << dir.ordinal();
            } else if (dir.getAxis().isHorizontal()
                    && neighbour.is(ModBlocks.FOUNDRY_OUTLET.get())
                    && neighbour.getValue(FoundryOutlet.FACING) == dir) {
                mask |= 1 << (4 + dir.ordinal());
            }
        }
        return mask;
    }

    private static @Nullable BlockEntityFoundryTank tank(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BlockEntityFoundryTank be ? be : null;
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack held,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!held.is(ItemTags.SHOVELS)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockEntityFoundryTank be = tank(level, pos);
        if (be == null) return InteractionResult.PASS;

        if (be.amount > 0 && be.type != null) {
            ItemStack scrap = ItemScraps.create(new MaterialStack(be.type, be.amount));
            player.getInventory().placeItemBackInInventory(scrap);
            be.amount = 0;
            be.type = null;
            be.setChanged();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityFoundryTank(pos, state);
    }
}
