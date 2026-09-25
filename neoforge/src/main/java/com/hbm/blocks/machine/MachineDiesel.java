// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.RefreshesNeighborState;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.inventory.IGUIProvider;
import com.hbm.tileentity.machine.BlockEntityMachineDiesel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

@RefreshesNeighborState(be = BlockEntityMachineDiesel.class, calling = "refreshRedstone")
public class MachineDiesel extends BlockMachineHorizontal implements ICapabilityBlock {

    public MachineDiesel(Properties props) {
        super(props);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineDiesel(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            IGUIProvider.openBlockMenu(player, provider, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityMachineDiesel be)) return;
        if (!be.isOn || !be.hasAcceptableFuel() || be.tank.getFill() <= 0) return;

        Direction dir = state.getValue(FACING);
        Direction rot = dir.getClockWise();
        level.addParticle(
                ParticleTypes.SMOKE,
                pos.getX() + 0.5 - dir.getStepX() * 0.6 + rot.getStepX() * 0.1875,
                pos.getY() + 0.3125,
                pos.getZ() + 0.5 - dir.getStepZ() * 0.6 + rot.getStepZ() * 0.1875,
                0,
                0,
                0);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.DIESEL_GENERATOR)
                .powerOut()
                .fluidIn()
                .fluidOut()
                .items()
                .fe();
    }
}
