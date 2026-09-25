// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.inventory.IGUIProvider;
import com.hbm.tileentity.machine.BlockEntityForceField;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class MachineForceField extends Block implements ITickingBlock, ICapabilityBlock {

    private static final float EMITTER_HEIGHT = 2F;
    private static final int MOTES = 4;

    public MachineForceField(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityForceField(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()
                && !player.isSecondaryUseActive()
                && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            IGUIProvider.openBlockMenu(player, provider, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityForceField field)) return;

        ParticleOptions mote;
        if (field.isOn && field.cooldown == 0 && field.power > 0) {
            mote =
                    field.color == BlockEntityForceField.COLOR_HIT
                            ? ParticleTypes.LAVA
                            : DustParticleOptions.REDSTONE;
        } else if (field.cooldown > 0) {
            mote = ParticleTypes.SMOKE;
        } else {
            return;
        }

        for (int i = 0; i < MOTES; i++) {
            level.addParticle(
                    mote,
                    pos.getX() + random.nextFloat(),
                    pos.getY() + EMITTER_HEIGHT,
                    pos.getZ() + random.nextFloat(),
                    0D,
                    0D,
                    0D);
        }
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.FORCEFIELD)
                .powerIn()
                .fe()
                .items()
                .faces(BlockEntityForceField.class, (be, side) -> side != Direction.UP);
    }
}
