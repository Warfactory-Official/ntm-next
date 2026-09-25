// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.rbmk;

import com.hbm.blocks.ITickingBlock;
import com.hbm.interfaces.IToolable;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKKeyPad;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RBMKKeyPad extends RBMKMiniPanelBase implements ITickingBlock, IToolable {

    public static Consumer<BlockEntityRBMKKeyPad> OPEN_SCREEN = keypad -> {};

    public RBMKKeyPad(Properties props) {
        super(props);
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
        if (level.isClientSide()
                && level.getBlockEntity(pos) instanceof BlockEntityRBMKKeyPad keypad) {
            OPEN_SCREEN.accept(keypad);
        }
        return true;
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
        if (ToolType.getType(stack) == ToolType.SCREWDRIVER) return InteractionResult.PASS;
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;

        int column = MiniPanelHit.columnOf(state, hit);
        if (column < 0) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof BlockEntityRBMKKeyPad panel))
            return InteractionResult.SUCCESS;

        BlockEntityRBMKKeyPad.KeyUnit key = panel.keys[column + MiniPanelHit.rowOffset(hit)];
        if (!key.active) return InteractionResult.PASS;
        key.click(level);
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityRBMKKeyPad(pos, state);
    }
}
