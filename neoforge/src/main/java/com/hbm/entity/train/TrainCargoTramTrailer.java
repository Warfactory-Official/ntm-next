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

package com.hbm.entity.train;

import com.hbm.blocks.rail.RailGauge;
import com.hbm.inventory.container.TrainCargoTramTrailerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class TrainCargoTramTrailer extends RailCarCargoEntity {

    public static final int SLOT_COUNT = 45;

    private static final DummyConfig[] DUMMIES = {
        new DummyConfig(2F, 1F, 0, 0, 1.5),
        new DummyConfig(2F, 1F, 0, 0, 0),
        new DummyConfig(2F, 1F, 0, 0, -1.5)
    };

    public TrainCargoTramTrailer(EntityType<? extends TrainCargoTramTrailer> type, Level level) {
        super(type, level);
    }

    @Override
    public double getMaxRailSpeed() {
        return 1;
    }

    @Override
    public RailGauge getGauge() {
        return RailGauge.STANDARD;
    }

    @Override
    public double getLengthSpan() {
        return 1.5;
    }

    @Override
    public double getCollisionSpan() {
        return 2.5;
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public DummyConfig[] getDummies() {
        return DUMMIES;
    }

    @Override
    public double getCouplingDist(TrainCoupling coupling) {
        return coupling != null ? 2.75 : 0;
    }

    @Override
    public double getCurrentSpeed() {
        return 0;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!this.isRemoved()) this.discard();
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        InteractionResult coupled = super.interact(player, hand, location);
        if (coupled != InteractionResult.PASS) return coupled;

        if (player instanceof ServerPlayer server) server.openMenu(this);

        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getDisplayName() {
        return this.hasCustomName()
                ? this.getCustomName()
                : Component.translatable("container.trainTramTrailer");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new TrainCargoTramTrailerMenu(containerId, playerInv, this);
    }
}
