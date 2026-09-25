// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.bomb;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.item.EntityFireworks;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityFireworks extends BlockEntity {

    private static final int SHOT_DELAY = 30;
    private static final int WORD_DELAY = 100;

    private static final int MUZZLES = 9;
    private static final double SPACING = 0.3125D;

    public int color = 0xff0000;
    public String message = "NUCLEAR TECH";
    public int charges;

    private int index;
    private int delay;

    public BlockEntityFireworks(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FIREWORK_BOX.get(), pos, state);
    }

    public void resetSequence() {
        delay = 0;
        index = 0;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BlockEntityFireworks be) {
        be.serverTick((ServerLevel) level, pos);
    }

    private void serverTick(ServerLevel level, BlockPos pos) {
        if (message.isEmpty() || charges <= 0) {
            resetSequence();
            return;
        }
        if (--delay > 0) return;
        delay = SHOT_DELAY;

        int mod = index % MUZZLES;
        double offX = (mod / 3 - 1) * SPACING;
        double offZ = (mod % 3 - 1) * SPACING;
        double x = pos.getX() + 0.5D + offX;
        double z = pos.getZ() + 0.5D + offZ;

        EntityFireworks shot =
                new EntityFireworks(level, x, pos.getY() + 1.5D, z, color, message.charAt(index));
        level.addFreshEntity(shot);
        level.playSound(
                null,
                shot.getX(),
                shot.getY(),
                shot.getZ(),
                ModSounds.WEAPON_ROCKET_FLAME.get(),
                SoundSource.BLOCKS,
                3.0F,
                1.0F);

        charges--;
        setChanged();

        level.sendParticles(ParticleTypes.FLAME, x, pos.getY() + 1.125D, z, 1, 0D, 0D, 0D, 0D);

        index++;
        if (index >= message.length()) {
            index = 0;
            delay = WORD_DELAY;
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        charges = input.getIntOr("charges", 0);
        color = input.getIntOr("color", color);
        message = input.getStringOr("message", message);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("charges", charges);
        output.putInt("color", color);
        output.putString("message", message);
    }
}
