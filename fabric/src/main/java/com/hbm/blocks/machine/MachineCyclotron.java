// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.BlockEntityMachineCyclotron;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class MachineCyclotron extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {

    private static final int[] DIMENSIONS = {2, 0, 2, 2, 2, 2};

    private static final int REACH = 2;
    private static final int OFFSET = 1;

    public MachineCyclotron(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return REACH;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);

        visitor.cell(core.offset(REACH, 0, OFFSET), MASK_EAST, ROLE_ALL);
        visitor.cell(core.offset(REACH, 0, -OFFSET), MASK_EAST, ROLE_ALL);
        visitor.cell(core.offset(-REACH, 0, OFFSET), MASK_WEST, ROLE_ALL);
        visitor.cell(core.offset(-REACH, 0, -OFFSET), MASK_WEST, ROLE_ALL);
        visitor.cell(core.offset(OFFSET, 0, REACH), MASK_SOUTH, ROLE_ALL);
        visitor.cell(core.offset(-OFFSET, 0, REACH), MASK_SOUTH, ROLE_ALL);
        visitor.cell(core.offset(OFFSET, 0, -REACH), MASK_NORTH, ROLE_ALL);
        visitor.cell(core.offset(-OFFSET, 0, -REACH), MASK_NORTH, ROLE_ALL);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS;

        for (int off = -OFFSET; off <= OFFSET; off++) {
            visitor.passiveCell(core.offset(REACH, 0, off), MASK_ALL, domains);
            visitor.passiveCell(core.offset(-REACH, 0, off), MASK_ALL, domains);
            visitor.passiveCell(core.offset(off, 0, REACH), MASK_ALL, domains);
            visitor.passiveCell(core.offset(off, 0, -REACH), MASK_ALL, domains);
        }
    }

    @Override
    public int coreMask() {
        return MASK_NONE;
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(core) instanceof BlockEntityMachineCyclotron be))
            return InteractionResult.PASS;

        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty()) {
            for (int i = 0; i < BlockEntityMachineCyclotron.PLUGS; i++) {
                if (held.getItem() != BlockEntityMachineCyclotron.itemForPlug(i) || be.getPlug(i))
                    continue;
                held.consume(1, player);
                be.setPlug(i);
                level.playSound(
                        null, core, ModSounds.UPGRADE_PLUG.get(), SoundSource.PLAYERS, 1.5F, 1.0F);
                return InteractionResult.SUCCESS;
            }
        }

        if (level.getBlockEntity(core) instanceof MenuProvider menu) {
            openCoreMenu(player, core, menu);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineCyclotron(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.CYCLOTRON)
                .powerIn()
                .fluidIn()
                .fluidOut()
                .items()
                .itemsAtCells()
                .fe();
    }
}
