// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.rbmk;

import com.hbm.blocks.ITickingBlock;
import com.hbm.interfaces.IToolable;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKNumitron;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RBMKNumitron extends RBMKMiniPanelBase implements ITickingBlock, IToolable {

    public static Consumer<BlockEntityRBMKNumitron> OPEN_SCREEN = numitron -> {};

    public RBMKNumitron(Properties props) {
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
                && level.getBlockEntity(pos) instanceof BlockEntityRBMKNumitron numitron) {
            OPEN_SCREEN.accept(numitron);
        }
        return true;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityRBMKNumitron(pos, state);
    }
}
