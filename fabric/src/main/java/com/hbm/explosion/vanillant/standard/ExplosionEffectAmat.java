// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IExplosionSFX;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.packet.toclient.AmatFlashPayload;
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

public class ExplosionEffectAmat implements IExplosionSFX {

    @Override
    public void doEffect(
            ExplosionVNT explosion, Level world, double x, double y, double z, float size) {

        if (world.isClientSide()) return;
        if (!(world instanceof ServerLevel level)) return;

        RandomSource rand = world.getRandom();

        if (size < 15) {
            world.playSound(
                    null,
                    x,
                    y,
                    z,
                    SoundEvents.GENERIC_EXPLODE,
                    SoundSource.BLOCKS,
                    4.0F,
                    (1.4F + (rand.nextFloat() - rand.nextFloat()) * 0.2F) * 0.7F);
        } else {
            world.playSound(
                    null,
                    x,
                    y,
                    z,
                    ModSounds.GUN_MINI_NUKE_EXPLOSION.get(),
                    SoundSource.BLOCKS,
                    15.0F,
                    1.0F);
        }

        Services.NETWORK.sendToAllAround(
                new AmatFlashPayload(x, y, z, size), new TargetPoint(level, x, y, z, 200));
    }
}
