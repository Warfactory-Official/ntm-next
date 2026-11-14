/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.interfaces.Extinguishable;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.CorrosiveTrait;
import com.hbm.inventory.fluid.trait.FlammableTrait;
import com.hbm.inventory.fluid.trait.FluidReleaseType;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Amat;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous_ART;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Liquid;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.GasFlameLayout;
import com.hbm.particle.CoolingTowerParticleOptions;
import com.hbm.util.TickPhase;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public final class FluidTankHazard {
    public interface Host {
        boolean isDamaged();

        void setDamaged(boolean damaged);
    }

    private final BlockEntity owner;
    private final Host host;
    private final FluidTankNTM tank;
    private boolean onFire;

    public <T extends BlockEntity & Host> FluidTankHazard(T owner, FluidTankNTM tank) {
        this.owner = owner;
        this.host = owner;
        this.tank = tank;
    }

    public boolean isOnFire() {
        return onFire;
    }

    public void setOnFire(boolean onFire) {
        this.onFire = onFire;
    }

    public void tickServer() {
        if (tank.getFill() > 0) {
            Fluid type = tank.getTankType();
            if (NTMFluidProperties.hasTrait(type, FT_Amat.class)) {
                antimatterBlast();
                explode();
                tank.setFill(0);
            }
            CorrosiveTrait corrosive = NTMFluidProperties.getTrait(type, CorrosiveTrait.class);
            if (corrosive != null && corrosive.severe()) explode();
            if (host.isDamaged()) {
                int amount =
                        NTMFluidProperties.hasTrait(type, FT_Amat.class)
                                ? tank.getFill()
                                : Math.min(
                                        tank.getFill(),
                                        tank.getMaxFill() / (gaseous(type) ? 100 : 10_000));
                updateLeak(amount);
            }
        }
    }

    private static boolean gaseous(Fluid type) {
        return NTMFluidProperties.hasTrait(type, FT_Gaseous.class)
                || NTMFluidProperties.hasTrait(type, FT_Gaseous_ART.class);
    }

    private void antimatterBlast() {
        BlockPos pos = owner.getBlockPos();
        new ComposableExplosion(
                        owner.getLevel(), pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 5F)
                .antimatter()
                .blockSelector(null)
                .blockProcessor(null)
                .explode();
    }

    public void explode() {
        host.setDamaged(true);
        onFire = NTMFluidProperties.hasTrait(tank.getTankType(), FlammableTrait.class);
        owner.setChanged();
    }

    public void updateLeak(int amount) {
        if (!host.isDamaged() || amount <= 0) return;
        tank.drain(amount, true);
        owner.setChanged();
        Level level = owner.getLevel();
        BlockPos pos = owner.getBlockPos();
        Fluid type = tank.getTankType();
        if (NTMFluidProperties.hasTrait(type, FT_Amat.class)) {
            antimatterBlast();
        } else if (NTMFluidProperties.hasTrait(type, FlammableTrait.class) && onFire) {
            AABB box =
                    new AABB(
                            pos.getX() - 1.5,
                            pos.getY(),
                            pos.getZ() - 1.5,
                            pos.getX() + 2.5,
                            pos.getY() + 5,
                            pos.getZ() + 2.5);
            for (Entity entity : level.getEntitiesOfClass(Entity.class, box))
                entity.igniteForSeconds(5F);
            var random = level.getRandom();
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + 0.5 + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            PacketWire.sendToAllAround(
                    GasFlameLayout.encode(
                            x,
                            y,
                            z,
                            random.nextGaussian() * 0.2,
                            0.1,
                            random.nextGaussian() * 0.2,
                            6.5F),
                    new TargetPoint((ServerLevel) level, x, y, z, 150));
            if (TickPhase.every(owner, 5)) {
                FluidReleaseType.BURN.dispatch(level, pos, type, tank, amount * 5);
            }
        } else if (gaseous(type) && TickPhase.every(owner, 5)) {
            var options =
                    new CoolingTowerParticleOptions.Builder()
                            .setLift(1F)
                            .setBaseScale(1F)
                            .setMaxScale(5F)
                            .setLife(100 + level.getRandom().nextInt(20))
                            .setColor(NTMFluidProperties.get(type).color())
                            .build();
            ServerLevel server = (ServerLevel) level;
            for (var player : server.players()) {
                if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 150 * 150) {
                    server.sendParticles(
                            player,
                            options,
                            true,
                            false,
                            pos.getX() + 0.5,
                            pos.getY() + 1,
                            pos.getZ() + 0.5,
                            0,
                            0,
                            0,
                            0,
                            0);
                }
            }
            FluidReleaseType.SPILL.dispatch(level, pos, type, tank, amount * 5);
        }
    }

    public void tryExtinguish(Level level, Extinguishable.Agent type) {
        if (!host.isDamaged() || !onFire) return;
        BlockPos pos = owner.getBlockPos();
        if (type == Extinguishable.Agent.WATER) {
            if (NTMFluidProperties.hasTrait(tank.getTankType(), FT_Liquid.class)) {
                level.explode(
                        null,
                        pos.getX() + 0.5,
                        pos.getY() + 1.5,
                        pos.getZ() + 0.5,
                        5F,
                        true,
                        Level.ExplosionInteraction.BLOCK);
            } else {
                onFire = false;
                owner.setChanged();
            }
        } else if (type == Extinguishable.Agent.FOAM || type == Extinguishable.Agent.CO2) {
            onFire = false;
            owner.setChanged();
        }
    }

    public void load(ValueInput input) {
        onFire = input.getBooleanOr("onFire", onFire);
    }

    public void save(ValueOutput output) {
        output.putBoolean("onFire", onFire);
    }
}
