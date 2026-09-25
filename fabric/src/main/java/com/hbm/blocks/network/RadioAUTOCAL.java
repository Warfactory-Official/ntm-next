// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.tileentity.network.BlockEntityRadioAUTOCAL;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class RadioAUTOCAL extends BlockMultiblockCore implements ITickingBlock {

    private static final int[] DIMENSIONS = {1, 0, 0, 0, 0, 0};

    public static Consumer<BlockEntityRadioAUTOCAL> OPEN_GUI = autocal -> {};

    public RadioAUTOCAL(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityRadioAUTOCAL(pos, state);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.isClientSide()
                && level.getBlockEntity(core) instanceof BlockEntityRadioAUTOCAL autocal) {
            OPEN_GUI.accept(autocal);
        }
        return InteractionResult.SUCCESS;
    }
}
