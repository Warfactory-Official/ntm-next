/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.blockentity.turret;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.projectile.BulletBaseMK4Entity;
import com.hbm.inventory.container.TurretBaseMenu;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryRocket;
import com.hbm.sound.ModSounds;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class TurretRichardBlockEntity extends TurretBaseNTBlockEntity {

    private static final int MAGAZINE = 17;
    private static final int RELOAD_TICKS = 100;
    private static final int SHOT_PERIOD = 10;
    private static final int MAGAZINE_UNIT = 11;

    private static final byte SLOTS_UNREAD = 0, ROUNDS_LEFT = 1, NO_ROUNDS = 2;

    private int magazine;
    private int reload;
    private byte slotsAfterShot = SLOTS_UNREAD;

    public TurretRichardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_RICHARD.get(), pos, state);
    }

    public int magazine() {
        return magazine;
    }

    @Override
    protected List<BulletConfig> getAmmoList() {
        return Rounds.ROCKETS;
    }

    @Override
    public void tickServer() {
        int before = magazine;
        slotsAfterShot = SLOTS_UNREAD;
        super.tickServer();

        int fired = magazine;
        if (reload > 0 && --reload == 0) hbm$syncSet$TurretRichardBlockEntity$magazine(MAGAZINE);
        boolean rounds =
                switch (slotsAfterShot) {
                    case ROUNDS_LEFT -> true;
                    case NO_ROUNDS -> false;
                    default -> getFirstConfigLoaded() != null;
                };
        if (!rounds) hbm$syncSet$TurretRichardBlockEntity$magazine(0);
        else if (magazine <= 0 && reload <= 0) reload = RELOAD_TICKS;

        if (magazine != before) markChanged();
        if (magazine != fired) networkPackNT(250);
    }

    @Override
    public void updateFiringTick() {
        if (reload > 0 || !beat(SHOT_PERIOD)) return;
        BulletConfig round = getFirstConfigLoaded();
        if (round == null) {
            hbm$syncSet$TurretRichardBlockEntity$magazine(0);
            slotsAfterShot = NO_ROUNDS;
            return;
        }
        int slot = firstSlotHolding(round);
        fire(round, 30F, ModSounds.RICHARD_FIRE.get(), 2.0F);
        hbm$syncSet$TurretRichardBlockEntity$magazine((int) (magazine - 1));
        if (!getItem(slot).isEmpty()) slotsAfterShot = ROUNDS_LEFT;
    }

    private int firstSlotHolding(BulletConfig round) {
        int slot = SLOT_AMMO_FIRST;
        while (!round.matchesAmmo(getItem(slot))) slot++;
        return slot;
    }

    @Override
    public void spawnBullet(BulletConfig bullet, float baseDamage) {
        Vec3 tip = barrelTip();
        var rocket =
                new BulletBaseMK4Entity(
                        level,
                        bullet,
                        baseDamage,
                        bullet.spread,
                        (float) rotationYaw,
                        (float) rotationPitch);
        rocket.snapTo(tip.x, tip.y, tip.z, rocket.getYRot(), rocket.getXRot());
        rocket.lockonTarget = target;
        level.addFreshEntity(rocket);
    }

    @Override
    public long getMaxPower() {
        return 10000;
    }

    @Override
    public double getDetectorRange() {
        return 64D;
    }

    @Override
    public double getDetectorGrace() {
        return 8D;
    }

    @Override
    public double getTurretElevation() {
        return 25D;
    }

    @Override
    public double getTurretDepression() {
        return 25D;
    }

    @Override
    public double getBarrelLength() {
        return 1.25D;
    }

    @Override
    public Component getName() {
        return Component.translatable("container.turretRichard");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new TurretBaseMenu(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$TurretRichardBlockEntity$magazine(input.getIntOr("loaded", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("loaded", magazine);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 1L << MAGAZINE_UNIT;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit == MAGAZINE_UNIT) output.writeInt(magazine);
        else super.writeSyncUnit(unit, output);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit == MAGAZINE_UNIT) hbm$syncSet$TurretRichardBlockEntity$magazine(input.readInt());
        else super.readSyncUnit(unit, input);
    }

    private static final class Rounds {
        static final List<BulletConfig> ROCKETS = List.of(XFactoryRocket.rocket_ml);
    }

    public final void hbm$syncSet$TurretRichardBlockEntity$magazine(int next) {
        if (this.magazine != next) {
            this.magazine = next;
            if ((1L << MAGAZINE_UNIT) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << MAGAZINE_UNIT);
        }
    }
}
