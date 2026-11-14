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
import com.hbm.items.weapon.sedna.factory.XFactoryAccelerator;
import com.hbm.lib.ModDamageTypes;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class TurretTauonBlockEntity extends TurretBaseNTBlockEntity {

    private static @Nullable List<BulletConfig> configs;

    public int beam;
    public float spin;
    public float lastSpin;
    public double lastDist;
    private int shotSequence;
    private int clientShotSequence;

    public TurretTauonBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_TAUON.get(), pos, state);
    }

    @Override
    protected @Nullable List<BulletConfig> getAmmoList() {
        if (configs == null) configs = List.of(XFactoryAccelerator.tau_uranium);
        return configs;
    }

    @Override
    public Component getName() {
        return Component.translatable("container.turretTauon");
    }

    @Override
    public double getDetectorGrace() {
        return 3D;
    }

    @Override
    public double getTurretYawSpeed() {
        return 9D;
    }

    @Override
    public double getTurretPitchSpeed() {
        return 6D;
    }

    @Override
    public double getTurretElevation() {
        return 35D;
    }

    @Override
    public double getTurretDepression() {
        return 35D;
    }

    @Override
    public double getDetectorRange() {
        return 128D;
    }

    @Override
    public double getBarrelLength() {
        return 2.0D - 0.0625D;
    }

    @Override
    public long getMaxPower() {
        return 100000;
    }

    @Override
    public long getConsumption() {
        return 1000;
    }

    @Override
    public void tickClient() {
        if (tPos != null) lastDist = tPos.subtract(getTurretPos()).length();

        if (beam > 0) beam--;

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
        if (beat(5)) {
            BulletConfig conf = getFirstConfigLoaded();

            if (conf != null && target != null && level instanceof ServerLevel server) {
                target.hurtServer(
                        server,
                        level.damageSources().source(ModDamageTypes.ELECTRICITY),
                        30F + level.getRandom().nextInt(11));
                consumeAmmo(conf);
                WorldSounds.machine(
                        level,
                        null,
                        worldPosition,
                        ModSounds.WEAPON_TAU_SHOOT.get(),
                        SoundSource.BLOCKS,
                        4.0F,
                        0.9F + level.getRandom().nextFloat() * 0.3F);

                hbm$syncSet$TurretTauonBlockEntity$shotSequence((int) (shotSequence + 1));
                networkPackNT(250);
            }
        }
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new TurretBaseMenu(containerId, playerInventory, this);
    }

    @Override
    public void afterSyncUnits(long units) {
        if (shotSequence != clientShotSequence) {
            beam = 3;
            clientShotSequence = shotSequence;
        }
    }

    @Override
    public void afterInitialSyncUnits() {
        clientShotSequence = shotSequence;
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 1L << 11;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 11 -> output.writeInt(this.shotSequence);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 11 -> this.hbm$syncSet$TurretTauonBlockEntity$shotSequence(input.readInt());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$TurretTauonBlockEntity$shotSequence(int next) {
        if (this.shotSequence != next) {
            this.shotSequence = next;
            if ((1L << 11) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 11);
        }
    }
}
