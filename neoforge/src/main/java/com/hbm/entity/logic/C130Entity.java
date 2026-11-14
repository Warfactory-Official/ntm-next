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

package com.hbm.entity.logic;

import com.hbm.entity.ModEntities;
import com.hbm.entity.item.ParachuteCrateEntity;
import com.hbm.itempool.ItemPool;
import com.hbm.itempool.ItemPoolIds;
import com.hbm.sound.EntityAudioLoop;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.Vec3;

public class C130Entity extends PlaneBaseEntity {

    public C130PayloadType payload = C130PayloadType.SUPPLIES;
    private final EntityAudioLoop engine =
            new EntityAudioLoop(this, ModSounds.ENTITY_BOMBER_LOOP, SoundSource.NEUTRAL, 2F, 250F);

    public C130Entity(EntityType<? extends C130Entity> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            this.engine.tick(this.health > 0);
            return;
        }
        if (isRemoved() || this.health <= 0) return;

        WorldSounds.entityLoop(this, this.tickCount == 1);
        if (this.tickCount == getLifetime() / 2) dropCrate((ServerLevel) level());
    }

    private void dropCrate(ServerLevel level) {
        var crate = new ParachuteCrateEntity(ModEntities.PARACHUTE_CRATE.get(), level);
        Vec3 motion = getDeltaMovement();
        crate.setPos(getX() - motion.x * 7, getY() - 10, getZ() - motion.z * 7);
        this.payload.pack(crate.items, this.random, ItemPool.context(level, this.random));
        level.addFreshEntity(crate);
    }

    public void fac(Level world, double x, double y, double z, C130PayloadType payload) {
        this.payload = payload;
        PlaneApproach.lineUp(this, world.getRandom(), x, y, z, 2D, 100D);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.payload = C130PayloadType.VALUES[input.getIntOr("payload", 0)];
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("payload", this.payload.ordinal());
    }

    private record Cargo(String pool, int least, int most) {

        void roll(List<ItemStack> into, RandomSource random, LootContext loot) {
            int count =
                    this.least == this.most
                            ? this.least
                            : this.least + random.nextInt(this.most - this.least + 1);
            for (int i = 0; i < count; i++) into.add(ItemPool.getStack(this.pool, loot));
        }
    }

    public enum C130PayloadType {
        SUPPLIES(new Cargo(ItemPoolIds.POOL_SUPPLIES, 5, 5)),
        WEAPONS(new Cargo(ItemPoolIds.POOL_WEAPONS, 1, 2), new Cargo(ItemPoolIds.POOL_AMMO, 6, 6)),
        A_FUCKING_FUEL_TRUCK;

        public static final C130PayloadType[] VALUES = values();

        private final Cargo[] manifest;

        C130PayloadType(Cargo... manifest) {
            this.manifest = manifest;
        }

        void pack(List<ItemStack> into, RandomSource random, LootContext loot) {
            for (Cargo cargo : this.manifest) cargo.roll(into, random, loot);
        }
    }
}
