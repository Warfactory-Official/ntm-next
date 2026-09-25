// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.BlockEntityChimneyBrick;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachineChimneyBrick extends MachineChimneyBase {

    private static final int[] DIMENSIONS = {12, 0, 1, 1, 1, 1};

    public MachineChimneyBrick(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityChimneyBrick(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.CHIMNEY_BRICK)
                .fluidIn()
                .fluidFaces(
                        BlockEntityChimneyBrick.class,
                        (be, face) ->
                                face.fluid() == null || be.acceptsFluid(face.fluid(), face.side()));
    }
}
