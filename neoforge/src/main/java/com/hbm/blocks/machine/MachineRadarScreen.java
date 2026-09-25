// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.IGUIProvider;
import com.hbm.tileentity.machine.BlockEntityMachineRadar;
import com.hbm.tileentity.machine.BlockEntityMachineRadarScreen;
import com.hbm.util.ChunkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class MachineRadarScreen extends BlockMultiblockCore implements ITickingBlock {

    private static final int[] DIMENSIONS = {1, 0, 0, 0, 1, 0};

    public MachineRadarScreen(Properties props) {
        super(props);
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
        return new BlockEntityMachineRadarScreen(pos, state);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!(ChunkUtil.blockEntityIfLoaded(level, core)
                instanceof BlockEntityMachineRadarScreen screen)) {
            return InteractionResult.PASS;
        }

        BlockPos radarPos = screen.linkedRadar();
        if (radarPos == null) return InteractionResult.PASS;
        if (!(ChunkUtil.blockEntityIfLoaded(level, radarPos)
                instanceof BlockEntityMachineRadar radar)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            IGUIProvider.openBlockMenu(
                    player, radar, radarPos, BlockEntityMachineRadar.DISPATCH_MAP);
        }
        return InteractionResult.SUCCESS;
    }
}
