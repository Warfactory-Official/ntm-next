// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.rbmk;

import com.hbm.blocks.ISectionGeometry;
import com.hbm.blocks.ITickingBlock;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKTerminal;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class RBMKTerminal extends RBMKMiniPanelBase implements ITickingBlock, ISectionGeometry {
    public static Consumer<BlockEntityRBMKTerminal> OPEN_SCREEN = terminal -> {};

    public RBMKTerminal(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.isClientSide()
                && level.getBlockEntity(pos) instanceof BlockEntityRBMKTerminal terminal) {
            OPEN_SCREEN.accept(terminal);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityRBMKTerminal(pos, state);
    }
}
