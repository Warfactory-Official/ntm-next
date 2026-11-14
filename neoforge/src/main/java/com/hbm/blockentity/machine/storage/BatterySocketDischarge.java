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

package com.hbm.blockentity.machine.storage;

import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.entity.DischargeBeamEntity;
import com.hbm.entity.ModEntities;
import com.hbm.items.machine.BatterySCItem;
import com.hbm.items.machine.SelfChargingBatteryType;
import com.hbm.packet.SyncBindings;
import com.hbm.packet.SyncSource;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BatterySocketDischarge implements SyncSource {

    private final MachineBlockEntity owner;
    private int damageTimer;
    private int damageTarget;
    public double scPowerMult;

    {
        hbm$syncSet$BatterySocketDischarge$scPowerMult(1D);
    }

    public BatterySocketDischarge(MachineBlockEntity owner) {
        this.owner = owner;
    }

    public double powerMult() {
        return scPowerMult;
    }

    public void tickServer(ItemStack battery) {
        if (hasSCLoaded(battery)) {
            if (damageTarget == 0) pickNewSCTarget();
            damageTimer++;
            if (damageTimer >= damageTarget) discharge();
            fluctuate();
            owner.markChanged();
        }
    }

    private static boolean hasSCLoaded(ItemStack battery) {
        return battery.getItem() instanceof BatterySCItem sc
                && sc.tier != SelfChargingBatteryType.EMPTY;
    }

    private void pickNewSCTarget() {
        damageTimer = 0;
        damageTarget = 1200 + owner.getLevel().getRandom().nextInt(2400);
        owner.setChanged();
    }

    private void fluctuate() {
        hbm$syncSet$BatterySocketDischarge$scPowerMult(
                (double)
                        (scPowerMult
                                + ((1D / 100D)
                                        * (owner.getLevel().getRandom().nextDouble() * 2D - 1D))));
        hbm$syncSet$BatterySocketDischarge$scPowerMult(Mth.clamp(scPowerMult, 0.1D, 1D));
    }

    private void discharge() {
        pickNewSCTarget();

        Level level = owner.getLevel();
        BlockPos pos = owner.getBlockPos();
        Direction dir = owner.getBlockState().getValue(MultiblockCoreBlock.FACING);
        Direction rot = dir.getClockWise();
        double ex = pos.getX() + 0.5 - dir.getStepX() * 0.5 + rot.getStepX() * 0.5;
        double ey = pos.getY() + 1;
        double ez = pos.getZ() + 0.5 - dir.getStepZ() * 0.5 + rot.getStepX() * 0.5;

        double range = 15D;
        List<LivingEntity> targets =
                level.getEntitiesOfClass(
                        LivingEntity.class, new AABB(ex, ey, ez, ex, ey, ez).inflate(range));
        Collections.shuffle(targets);

        for (LivingEntity target : targets) {
            Vec3 delta =
                    new Vec3(
                            target.getX() - ex,
                            target.getY() + target.getBbHeight() / 2D - ey,
                            target.getZ() - ez);
            if (delta.length() > range) continue;
            Vec3 n = delta.normalize();
            double dominant = Math.max(Math.abs(n.x), Math.max(Math.abs(n.y), Math.abs(n.z)));
            Vec3 spawn = n.scale(1.125D / dominant);

            DischargeBeamEntity beam =
                    new DischargeBeamEntity(ModEntities.BEAM_DISCHARGE.get(), level);
            beam.setPos(pos.getX() + spawn.x, pos.getY() + spawn.y, pos.getZ() + spawn.z);
            Vec3 aim =
                    new Vec3(
                            target.getX() - beam.getX(),
                            target.getY() + target.getBbHeight() / 2D - beam.getY(),
                            target.getZ() - beam.getZ());
            beam.setRotationsFromVector(aim.x, aim.y, aim.z);
            beam.performHitscan(aim.length());
            level.addFreshEntity(beam);
        }

        DischargeBeamEntity.explodeDischarge(
                level,
                ex + level.getRandom().nextGaussian() * 0.5,
                ey + level.getRandom().nextGaussian() * 0.5,
                ez + level.getRandom().nextGaussian() * 0.5);
    }

    public void load(ValueInput input) {
        damageTimer = input.getIntOr("damageTimer", 0);
        damageTarget = input.getIntOr("damageTarget", 0);
        hbm$syncSet$BatterySocketDischarge$scPowerMult(input.getDoubleOr("scPowerMult", 1D));
    }

    public void save(ValueOutput output) {
        output.putInt("damageTimer", damageTimer);
        output.putInt("damageTarget", damageTarget);
        output.putDouble("scPowerMult", scPowerMult);
    }

    public final void hbm$syncSet$BatterySocketDischarge$scPowerMult(double next) {
        if (Double.doubleToRawLongBits(this.scPowerMult) != Double.doubleToRawLongBits(next)) {
            this.scPowerMult = next;
            if ((0L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 0L);
        }
    }

    private SyncSource hbm$syncOwner;

    private int hbm$syncMask;

    private int hbm$syncBindings;

    private long hbm$syncUnits;

    public final boolean syncBound() {
        return hbm$syncOwner != null;
    }

    public final void syncChanged(int mask) {
        if (hbm$syncOwner == null) return;
        if (hbm$syncUnits == 0) hbm$syncOwner.syncChanged(hbm$syncMask);
        else hbm$syncOwner.syncUnitsChanged(hbm$syncMask, hbm$syncUnits);
    }

    public final void syncUnitsChanged(int mask, long units) {
        syncChanged(mask);
    }

    public final void bindSync(SyncSource owner, int mask) {
        bindSyncUnits(owner, mask, 0);
    }

    public final void bindSyncUnits(SyncSource owner, int mask, long units) {
        if (hbm$syncOwner != null && hbm$syncOwner != owner) {
            throw new IllegalStateException("Mutable sync state has two owners");
        }
        hbm$syncOwner = owner;
        hbm$syncMask |= mask;
        hbm$syncUnits |= units;
        if (hbm$syncBindings++ == 0) SyncBindings.bindFields(this, owner);
    }

    public final void unbindSync(SyncSource owner) {
        assert hbm$syncOwner == owner;
        if (--hbm$syncBindings != 0) return;
        SyncBindings.unbindFields(this, owner);
        hbm$syncOwner = null;
        hbm$syncMask = 0;
        hbm$syncUnits = 0;
    }
}
