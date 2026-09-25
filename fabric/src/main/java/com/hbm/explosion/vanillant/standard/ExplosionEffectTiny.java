// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IExplosionSFX;
import com.hbm.sound.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

public class ExplosionEffectTiny implements IExplosionSFX {

    @Override
    public void doEffect(
            ExplosionVNT explosion, Level world, double x, double y, double z, float size) {

        if (world.isClientSide()) return;
        if (!(world instanceof ServerLevel level)) return;

        world.playSound(
                null, x, y, z, ModSounds.EXPLOSION_TINY.get(), SoundSource.BLOCKS, 15.0F, 1.0F);
        level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }
}
