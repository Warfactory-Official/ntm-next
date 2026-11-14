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
import com.hbm.inventory.container.TurretBaseMenu;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryTurret;
import com.hbm.particle.helper.CasingCreator;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class TurretJeremyBlockEntity extends TurretBaseNTBlockEntity {

    private static final int SHOT_PERIOD = 40;
    private static final int BREECH_TICKS = 20;
    private static final int CASING_DELAY = 22;

    private int breech;

    public TurretJeremyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_JEREMY.get(), pos, state);
    }

    @Override
    protected List<BulletConfig> getAmmoList() {
        return Rounds.SHELLS;
    }

    @Override
    public void tickServer() {
        if (breech > 0) breech--;
        if (breech == 1) {
            WorldSounds.machine(
                    level,
                    null,
                    worldPosition,
                    ModSounds.TURRET_JEREMY_RELOAD.get(),
                    SoundSource.BLOCKS,
                    2.0F,
                    1.0F);
        }
        super.tickServer();
    }

    @Override
    public void updateFiringTick() {
        if (!beat(SHOT_PERIOD)) return;
        BulletConfig round = getFirstConfigLoaded();
        if (round == null) return;
        fire(round, 50F, ModSounds.TURRET_JEREMY_FIRE.get(), 4.0F);
        breech = BREECH_TICKS;
        muzzleFlash(barrelTip(), 0F, 5);
    }

    @Override
    public boolean usesCasings() {
        return true;
    }

    @Override
    public int casingDelay() {
        return CASING_DELAY;
    }

    @Override
    protected Vec3 getCasingSpawnPos() {
        return getTurretPos().add(alongBarrel(-2, 0, 0));
    }

    @Override
    protected void spawnCasing() {
        if (cachedCasingConfig == null) return;
        Vec3 at = getCasingSpawnPos();
        CasingCreator.composeEffect(
                level,
                at.x,
                at.y,
                at.z,
                (float) Math.toDegrees(rotationYaw),
                (float) -Math.toDegrees(rotationPitch),
                -0.2,
                -0.2,
                0,
                0.01,
                -5F,
                0F,
                cachedCasingConfig.name(),
                true,
                100,
                0.5,
                20);
        cachedCasingConfig = null;
    }

    @Override
    public long getMaxPower() {
        return 10000;
    }

    @Override
    public double getDetectorRange() {
        return 80D;
    }

    @Override
    public double getDetectorGrace() {
        return 16D;
    }

    @Override
    public double getTurretDepression() {
        return 45D;
    }

    @Override
    public double getBarrelLength() {
        return 4.25D;
    }

    @Override
    public Component getName() {
        return Component.translatable("container.turretJeremy");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new TurretBaseMenu(containerId, playerInventory, this);
    }

    private static final class Rounds {
        static final List<BulletConfig> SHELLS =
                List.of(
                        XFactoryTurret.shell_normal,
                        XFactoryTurret.shell_explosive,
                        XFactoryTurret.shell_ap,
                        XFactoryTurret.shell_du,
                        XFactoryTurret.shell_w9);
    }
}
