// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.storage;

import com.hbm.blockentity.machine.storage.BlockEntityCrate;
import com.hbm.blockentity.machine.storage.CrateType;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.BlockMachineBase;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemLock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class BlockCrate extends BlockMachineBase implements ICapabilityBlock {

    private final CrateType crateType;

    public BlockCrate(Properties props, CrateType crateType) {
        super(props);
        this.crateType = crateType;
    }

    public CrateType crateType() {
        return crateType;
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(
                        switch (crateType) {
                            case IRON -> ModBlockEntities.CRATE_IRON;
                            case STEEL -> ModBlockEntities.CRATE_STEEL;
                            case DESH -> ModBlockEntities.CRATE_DESH;
                            case TUNGSTEN -> ModBlockEntities.CRATE_HOT;
                            case SAFE -> ModBlockEntities.SAFE;
                        })
                .items();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return crateType.newBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (stack.getItem() instanceof ItemLock || stack.is(ModItems.KEY_KIT.get())) {
            return InteractionResult.PASS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!level.isClientSide()
                && level.getBlockEntity(pos) instanceof BlockEntityCrate crate
                && crate.canAccess(player)) {
            player.openMenu(crate);
            crate.releaseSpiders(player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {}

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }
}
