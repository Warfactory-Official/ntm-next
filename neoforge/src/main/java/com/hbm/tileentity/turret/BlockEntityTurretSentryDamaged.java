// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.turret;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactory9mm;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BlockEntityTurretSentryDamaged extends BlockEntityTurretSentry {

    public BlockEntityTurretSentryDamaged(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_SENTRY_DAMAGED.get(), pos, state);
    }

    @Override
    public boolean hasPower() {
        return true;
    }

    @Override
    public boolean isOn() {
        return true;
    }

    @Override
    public double getTurretYawSpeed() {
        return 3D;
    }

    @Override
    public double getTurretPitchSpeed() {
        return 2D;
    }

    @Override
    public boolean hasThermalVision() {
        return false;
    }

    @Override
    public boolean entityAcceptableTarget(Entity e) {
        if (e instanceof Player player && player.getAbilities().instabuild) return false;
        return e instanceof LivingEntity;
    }

    @Override
    public void updateFiringTick() {
        timer++;

        if (timer % 10 == 0) {
            BulletConfig conf = XFactory9mm.p9_fmj;
            Vec3 flash = getTurretPos();
            cachedCasingConfig = conf.casing;

            if (shotSide) {
                level.playSound(
                        null,
                        worldPosition,
                        ModSounds.TURRET_SENTRY_FIRE.get(),
                        SoundSource.BLOCKS,
                        2.0F,
                        1.0F);
                spawnBullet(conf, 5F);
                Vec3 side = new Vec3(0.125, 0, 0).yRot((float) -rotationYaw);
                flash = barrelTip().add(side.x, 0, side.z);
            } else {
                level.playSound(
                        null,
                        worldPosition,
                        ModSounds.TURRET_SENTRY_FIRE.get(),
                        SoundSource.BLOCKS,
                        2.0F,
                        0.75F);
                if (casingDelay() == 0) spawnCasing();
                else casingDelay = casingDelay();
            }

            muzzleFlash(flash, 1F, 1);

            if (shotSide) didJustShootLeft = true;
            else didJustShootRight = true;
            shotSide = !shotSide;
        }
    }
}
