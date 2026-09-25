// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.rbmk;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.handler.BossSpawnHandler;
import com.hbm.interfaces.IToolable;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemGuideBook;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKConsole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RBMKConsole extends BlockMultiblockCore implements ITickingBlock, IToolable {

    private static final int[] PLACEMENT_DIMENSIONS = {
        3, 0, 0, 0, 2, 2, 0, 0, 0,
        0, 0, 0, 1, 2, 2, 0, 0, 0,
    };

    @Override
    protected int[] placementDimensionBoxes() {
        return PLACEMENT_DIMENSIONS;
    }

    private static final int[] DIMENSIONS = {3, 0, 0, 0, 2, 2};
    private static final int[] DESK = {0, 0, 0, 1, 2, 2};

    public RBMKConsole(Properties props) {
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
    protected RenderShape getRenderShape(BlockState state) {

        return RenderShape.MODEL;
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placed, Direction dir, int o) {
        if (!super.checkRequirement(level, placed, dir, o)) return false;
        BlockPos origin = placed.offset(dir.getStepX() * o, dir.getStepY() * o, dir.getStepZ() * o);
        return MultiblockHandlerXR.checkSpace(level, origin, DESK, placed, dir);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        MultiblockHandlerXR.visitBox(core, DESK, facing, visitor);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) return InteractionResult.SUCCESS;
        BossSpawnHandler.markFBI(player);
        if (hit.getDirection() == Direction.UP) {
            int yaw =
                    switch (coreState.getValue(FACING)) {
                        case NORTH -> 90;
                        case SOUTH -> 270;
                        case WEST -> 180;
                        case EAST -> 0;
                        default -> throw new IllegalStateException();
                    };
            Vec3 book = new Vec3(1.375D, 0, 0.75D).yRot((float) Math.toRadians(yaw));
            Vec3 click = hit.getLocation();
            var manual = ModItems.GUIDE_BOOK.get(ItemGuideBook.BookType.RBMK);
            if (Math.abs(click.x - core.getX() - 0.5D - book.x) < 0.1875D
                    && Math.abs(click.z - core.getZ() - 0.5D - book.z) < 0.1875D
                    && !player.getInventory().contains(stack -> stack.is(manual))) {
                player.getInventory()
                        .placeItemBackInInventory(new net.minecraft.world.item.ItemStack(manual));
                player.inventoryMenu.broadcastChanges();
                return InteractionResult.SUCCESS;
            }
        }
        if (level.getBlockEntity(core) instanceof MenuProvider menu) {
            openCoreMenu(player, core, menu);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean onScrew(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        if (!level.isClientSide()
                && level.getBlockEntity(pos) instanceof BlockEntityRBMKConsole console) {
            console.rotate();
        }
        return true;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityRBMKConsole(pos, state);
    }
}
