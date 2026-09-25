// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.RefreshesNeighborState;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.items.ModItems;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.BlockEntitySawmill;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

@RefreshesNeighborState(be = BlockEntitySawmill.class, calling = "refreshHeatBelow")
public class MachineSawmill extends BlockMultiblockCore
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {

    private static final int[] DIMENSIONS = {1, 0, 1, 1, 1, 1};

    public MachineSawmill(Properties props) {
        super(props);

        this.bounding.add(new AABB(-1.5D, 0D, -1.5D, 1.5D, 1D, 1.5D));
        this.bounding.add(new AABB(-1.25D, 1D, -0.5D, -0.625D, 1.875D, 0.5D));
        this.bounding.add(new AABB(-0.625D, 1D, -1D, 1.375D, 2D, 1D));
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
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        visitor.passiveCell(core.offset(1, 0, 0), MASK_ALL, PASSIVE_ITEMS);
        visitor.passiveCell(core.offset(-1, 0, 0), MASK_ALL, PASSIVE_ITEMS);
        visitor.passiveCell(core.offset(0, 0, 1), MASK_ALL, PASSIVE_ITEMS);
        visitor.passiveCell(core.offset(0, 0, -1), MASK_ALL, PASSIVE_ITEMS);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntitySawmill(pos, state);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        return interact(level, core, player, ItemStack.EMPTY)
                ? InteractionResult.SUCCESS
                : InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useItemOnAtCore(
            ItemStack stack,
            BlockState coreState,
            Level level,
            BlockPos core,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        return interact(level, core, player, stack)
                ? InteractionResult.SUCCESS
                : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    private boolean interact(Level level, BlockPos pos, Player player, ItemStack held) {
        if (level.isClientSide()) return true;
        if (!(level.getBlockEntity(pos) instanceof BlockEntitySawmill saw)) return false;

        if (!saw.hasBlade && !held.isEmpty() && held.is(ModItems.SAWBLADE.get())) {
            held.shrink(1);
            saw.hasBlade = true;
            saw.setChanged();
            level.playSound(
                    null,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    ModSounds.UPGRADE_PLUG.get(),
                    SoundSource.BLOCKS,
                    1.5F,
                    0.75F);
            return true;
        }

        if (!saw.getItem(1).isEmpty() || !saw.getItem(2).isEmpty()) {
            for (int i = 1; i < 3; i++) {
                ItemStack out = saw.getItem(i);
                if (!out.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(out.copy());
                    saw.setItem(i, ItemStack.EMPTY);
                }
            }
            saw.setChanged();
            return true;
        }

        if (saw.getItem(0).isEmpty() && !held.isEmpty() && saw.getOutput(held) != null) {
            ItemStack single = held.copyWithCount(1);
            saw.setItem(0, single);
            held.shrink(1);
            saw.setChanged();
            return true;
        }
        return false;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntitySawmill saw)) return;
        info.title(getName().getString(), 0xFFFF00, 0x404000);
        info.line(saw.heat + "TU/t");
        double percent = saw.heat / 300D;
        int color =
                percent > 1D
                        ? 0xFF0000
                        : (((int) (0xFF - 0xFF * percent)) << 16) | (((int) (0xFF * percent)) << 8);
        info.line(String.format(Locale.US, "%.1f", saw.heat * 1000 / 300 / 10D) + "%", color);
        int limiter = saw.progress * 26 / BlockEntitySawmill.PROCESSING_TIME;
        StringBuilder bar = new StringBuilder("[ ");
        for (int i = 0; i < 25; i++) bar.append(i < limiter ? "|" : ".");
        bar.append(" ]");
        info.line(bar.toString(), 0x55FF55);
        for (int i = 0; i < 3; i++) {
            ItemStack s = saw.getItem(i);
            if (!s.isEmpty())
                info.line(
                        (i == 0 ? "-> " : "<- ")
                                + s.getHoverName().getString()
                                + (s.getCount() > 1 ? " x" + s.getCount() : ""),
                        i == 0 ? 0x55FF55 : 0xFF5555);
        }
        if (saw.heat > 300) info.line("! ! ! OVERSPEED ! ! !", 0xFF0000);
        if (!saw.hasBlade) info.line("Blade missing!", 0xFF0000);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.SAWMILL).items().itemsAtCells();
    }
}
