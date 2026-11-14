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
import com.hbm.handler.CasingEjector;
import com.hbm.inventory.container.TurretBaseMenu;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactory50;
import com.hbm.sound.ModSounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class TurretChekhovBlockEntity extends TurretBaseNTBlockEntity {

    private static final int SPUN_UP = 20;
    private static final float SPIN_STEP = 2F;
    private static final float SPIN_MAX = 45F;
    private static final CasingEjector EJECTOR =
            new CasingEjector().setMotion(-0.8, 0.8, 0).setAngleRange(0.1F, 0.1F);

    public float spin;
    public float lastSpin;
    private float spinRate;
    private int spinUp;

    public TurretChekhovBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.TURRET_CHEKHOV.get(), pos, state);
    }

    protected TurretChekhovBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected int cadence() {
        return 2;
    }

    @Override
    protected List<BulletConfig> getAmmoList() {
        return Rounds.BMG50;
    }

    @Override
    public void updateFiringTick() {
        spinUp++;
        if (spinUp <= SPUN_UP || spinUp % cadence() != 0) return;
        BulletConfig round = getFirstConfigLoaded();
        if (round == null) return;
        fire(round, 10F, ModSounds.TURRET_50BMG.get(), 2.0F);
        muzzleFlash(barrelTip(), 1.5F, 1);
    }

    @Override
    public void tickServer() {
        super.tickServer();
        if (tPos == null) spinUp = Math.clamp(spinUp - 1, 0, SPUN_UP);
    }

    @Override
    public void tickClient() {
        super.tickClient();
        spinRate =
                tPos != null
                        ? Math.min(SPIN_MAX, spinRate + SPIN_STEP)
                        : Math.max(0F, spinRate - SPIN_STEP);
        lastSpin = spin;
        spin += spinRate;
        if (spin >= 360F) {
            spin -= 360F;
            lastSpin -= 360F;
        }
    }

    @Override
    public boolean usesCasings() {
        return true;
    }

    @Override
    protected Vec3 getCasingSpawnPos() {
        return getTurretPos().add(alongBarrel(-1.125, 0.125, 0.25));
    }

    @Override
    protected @Nullable CasingEjector getEjector() {
        return EJECTOR;
    }

    @Override
    public long getMaxPower() {
        return 10000;
    }

    @Override
    public double getTurretElevation() {
        return 45D;
    }

    @Override
    public double getAcceptableInaccuracy() {
        return 15D;
    }

    @Override
    public double getBarrelLength() {
        return 3.5D;
    }

    @Override
    public Component getName() {
        return Component.translatable("container.turretChekhov");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new TurretBaseMenu(containerId, playerInventory, this);
    }

    private static final class Rounds {
        static final List<BulletConfig> BMG50 =
                List.of(
                        XFactory50.bmg50_sp,
                        XFactory50.bmg50_fmj,
                        XFactory50.bmg50_jhp,
                        XFactory50.bmg50_ap,
                        XFactory50.bmg50_du);
    }
}
