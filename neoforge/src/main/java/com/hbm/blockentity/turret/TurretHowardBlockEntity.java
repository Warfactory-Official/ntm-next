/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blockentity.turret;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.config.HbmConfig;
import com.hbm.config.RuntimeConfig;
import com.hbm.handler.CasingEjector;
import com.hbm.inventory.container.TurretBaseMenu;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryTurret;
import com.hbm.lib.ModDamageTypes;
import com.hbm.lib.ModifiedDamageSource;
import com.hbm.particle.casing.Casing;
import com.hbm.particle.casing.CasingShape;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class TurretHowardBlockEntity extends TurretBaseNTBlockEntity {
    public static final Casing DGK_CASING =
            Casing.template(CasingShape.STRAIGHT)
                    .scale(1.5F)
                    .bounce(1F, .5F)
                    .tints(Casing.BRASS)
                    .lifetime(60)
                    .register("DGK");

    private static final CasingEjector EJECTOR =
            new CasingEjector().setMotion(0.4, 0, 0).setAngleRange(0.02F, 0.03F);

    private static @Nullable List<BulletConfig> configs;

    public float spin;
    public float lastSpin;
    private int loaded;

    public TurretHowardBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.TURRET_HOWARD.get(), pos, state);
    }

    protected TurretHowardBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected @Nullable List<BulletConfig> getAmmoList() {
        if (configs == null) configs = List.of(XFactoryTurret.dgk_normal);
        return configs;
    }

    @Override
    public Component getName() {
        return Component.translatable("container.turretHoward");
    }

    @Override
    public double getHeightOffset() {
        return 2.25D;
    }

    @Override
    public double getDetectorGrace() {
        return 3D;
    }

    @Override
    public double getTurretYawSpeed() {
        return 12D;
    }

    @Override
    public double getTurretPitchSpeed() {
        return 8D;
    }

    @Override
    public double getTurretElevation() {
        return 90D;
    }

    @Override
    public double getTurretDepression() {
        return 50D;
    }

    @Override
    public double getDetectorRange() {
        return 250D;
    }

    @Override
    public double getBarrelLength() {
        return 3.25D;
    }

    @Override
    public long getMaxPower() {
        return 50000;
    }

    @Override
    public long getConsumption() {
        return 500;
    }

    @Override
    public void tickServer() {
        if (loaded <= 0) {
            BulletConfig conf = getFirstConfigLoaded();

            if (conf != null) {
                consumeAmmo(conf);
                WorldSounds.machine(
                        level,
                        null,
                        worldPosition,
                        ModSounds.TURRET_CIWS_RELOAD.get(),
                        SoundSource.BLOCKS,
                        4.0F,
                        1F);
                loaded = 200;
            }
        }

        super.tickServer();
    }

    @Override
    public void tickClient() {
        lastSpin = spin;
        if (tPos != null) spin += 45;

        if (spin >= 360F) {
            spin -= 360F;
            lastSpin -= 360F;
        }

        super.tickClient();
    }

    @Override
    public void updateFiringTick() {
        boolean onBeat = beat(2);
        if (loaded > 0 && tPos != null) {
            WorldSounds.machine(
                    level,
                    null,
                    worldPosition,
                    ModSounds.TURRET_HOWARD_FIRE.get(),
                    SoundSource.BLOCKS,
                    4.0F,
                    0.9F + level.getRandom().nextFloat() * 0.3F);
            WorldSounds.machine(
                    level,
                    null,
                    worldPosition,
                    ModSounds.TURRET_HOWARD_FIRE.get(),
                    SoundSource.BLOCKS,
                    4.0F,
                    1F + level.getRandom().nextFloat() * 0.3F);

            for (int i = 0; i < 2; i++) {
                cachedCasingConfig = DGK_CASING;
                spawnCasing();
            }

            if (onBeat) {
                loaded--;
                markChanged();

                if (level.getRandom().nextInt(100) + 1
                                <= ((RuntimeConfig) HbmConfig.RUNTIME).ciwsAccuracy()
                        && target != null) {
                    target.hurtServer(
                            (ServerLevel) target.level(),
                            ModifiedDamageSource.bypassingCooldown(
                                    level.damageSources().source(ModDamageTypes.SHRAPNEL)),
                            2F + level.getRandom().nextInt(2));
                }

                Vec3 tip = barrelTip();
                Vec3 hOff = alongBarrel(0, 0.25, 0);
                muzzleFlash(tip.add(hOff), 1.5F, 1);
                muzzleFlash(tip.subtract(hOff), 1.5F, 1);
            }
        }
    }

    @Override
    protected Vec3 getCasingSpawnPos() {
        return getTurretPos().add(alongBarrel(-0.875, 0.2, -0.125));
    }

    @Override
    protected @Nullable CasingEjector getEjector() {
        return EJECTOR;
    }

    @Override
    public boolean usesCasings() {
        return true;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        loaded = input.getIntOr("loaded", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("loaded", loaded);
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new TurretBaseMenu(containerId, playerInventory, this);
    }
}
