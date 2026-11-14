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

package com.hbm.blockentity.machine.rbmk;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.projectile.RBMKDebrisEntity.DebrisType;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.registration.RegistryHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class RBMKPassiveBlockEntity extends RBMKBaseBlockEntity implements SyncUnitSchema {
    public record Kind(
            RegistryHandle<? extends BlockEntityType<?>> type,
            RBMKColumnType consoleType,
            FluxRole role,
            DebrisType debris,
            int debrisBase) {}

    public static final Kind MODERATOR =
            new Kind(
                    ModBlockEntities.RBMK_MODERATOR,
                    RBMKColumnType.MODERATOR,
                    FluxRole.MODERATOR,
                    DebrisType.GRAPHITE,
                    2);
    public static final Kind ABSORBER =
            new Kind(
                    ModBlockEntities.RBMK_ABSORBER,
                    RBMKColumnType.ABSORBER,
                    FluxRole.ABSORBER,
                    DebrisType.BLANK,
                    1);
    public static final Kind REFLECTOR =
            new Kind(
                    ModBlockEntities.RBMK_REFLECTOR,
                    RBMKColumnType.REFLECTOR,
                    FluxRole.REFLECTOR,
                    DebrisType.BLANK,
                    1);

    private final Kind kind;

    public RBMKPassiveBlockEntity(Kind kind, BlockPos pos, BlockState state) {
        super(kind.type().get(), pos, state, 0);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public FluxRole fluxRole() {
        return kind.role();
    }

    @Override
    public RBMKColumnType getConsoleType() {
        return kind.consoleType();
    }

    @Override
    public void onMelt(int reduce) {
        int count = kind.debrisBase() + level.getRandom().nextInt(2);
        for (int i = 0; i < count; i++) spawnDebris(kind.debris());
        super.onMelt(reduce);
    }
}
