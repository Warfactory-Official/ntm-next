// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.BlockEntityFoundryMold;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class FoundryMold extends FoundryCastingBase implements ICapabilityBlock {

    private static final VoxelShape SHAPE = Shapes.box(0D, 0D, 0D, 1D, 0.5D, 1D);
    private static final VoxelShape COLLISION =
            Shapes.or(
                    Shapes.box(0D, 0D, 0D, 1D, 0.125D, 1D),
                    Shapes.box(0D, 0D, 0D, 1D, 0.5D, 0.125D),
                    Shapes.box(0D, 0D, 0D, 0.125D, 0.5D, 1D),
                    Shapes.box(0.875D, 0D, 0D, 1D, 0.5D, 1D),
                    Shapes.box(0D, 0D, 0.875D, 1D, 0.5D, 1D));

    public FoundryMold(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityFoundryMold(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.FOUNDRY_MOLD).items();
    }
}
