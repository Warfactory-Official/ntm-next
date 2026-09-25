// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.turret;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.lib.ModDamageTypes;
import com.hbm.sound.ModSounds;
import com.hbm.util.EntityDamageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityTurretHowardDamaged extends BlockEntityTurretHoward {

    public BlockEntityTurretHowardDamaged(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_HOWARD_DAMAGED.get(), pos, state);
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
    protected boolean hasConnectorPlugs() {
        return false;
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
    public double getDetectorRange() {
        return 16D;
    }

    @Override
    public double getDetectorGrace() {
        return 5D;
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

        if (tPos != null && timer % 4 == 0) {
            level.playSound(
                    null,
                    worldPosition,
                    ModSounds.TURRET_HOWARD_FIRE.get(),
                    SoundSource.BLOCKS,
                    4.0F,
                    0.7F + level.getRandom().nextFloat() * 0.3F);

            cachedCasingConfig = DGK_CASING;
            spawnCasing();

            if (level.getRandom().nextInt(100) + 1 <= CIWS_HITRATE * 0.5 && target != null) {
                EntityDamageUtil.attackEntityFromIgnoreIFrame(
                        target,
                        level.damageSources().source(ModDamageTypes.SHRAPNEL),
                        2F + level.getRandom().nextInt(2));
            }

            muzzleFlash(barrelTip().add(alongBarrel(0, 0.25, 0)), 1.5F, 1);
        }
    }
}
