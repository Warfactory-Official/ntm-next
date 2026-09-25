// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.rbmk;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.handler.BossSpawnHandler;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBase;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKRod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class RBMKRod extends RBMKBase implements ICapabilityBlock {

    public final boolean moderated;

    public RBMKRod(Properties properties, boolean moderated) {
        super(properties);
        this.moderated = moderated;
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
        BossSpawnHandler.markFBI(player);
        if (held.getItem() instanceof ItemRBMKRod) {
            BlockPos core = MultiblockSurface.coreOfAny(level, pos);
            if (core != null
                    && level.getBlockEntity(core) instanceof BlockEntityRBMKRod rod
                    && rod.canLoad(held)) {
                if (!level.isClientSide()) {
                    rod.load(held.copyWithCount(1));
                    if (!player.getAbilities().instabuild) held.shrink(1);
                }
                level.playSound(
                        player,
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        ModSounds.UPGRADE_PLUG.get(),
                        SoundSource.BLOCKS,
                        1.0F,
                        1.0F);
                return InteractionResult.SUCCESS;
            }
        }
        return super.useItemOn(held, state, level, pos, player, hand, hit);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.RBMK_ROD).itemsAtCells();
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        visitor.passiveCell(core.above(above()), MASK_ALL, PASSIVE_ITEMS);
    }

    @Override
    protected BlockEntityType<? extends BlockEntityRBMKBase> beType() {
        return ModBlockEntities.RBMK_ROD.get();
    }

    @Override
    protected BlockEntityRBMKBase createCore(BlockPos pos, BlockState state) {
        return new BlockEntityRBMKRod(pos, state);
    }
}
