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

package com.hbm.fabric.compat;

import com.hbm.blockentity.security.LockableBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.util.RegistryUtil;
import com.mojang.serialization.MapCodec;
import com.zurrtum.create.api.contraption.storage.item.MountedItemStorageType;
import com.zurrtum.create.api.contraption.storage.item.simple.SimpleMountedStorage;
import com.zurrtum.create.api.contraption.storage.item.simple.SimpleMountedStorageType;
import com.zurrtum.create.api.registry.CreateRegistries;
import com.zurrtum.create.api.registry.CreateRegistryKeys;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public final class CreateMountedCrates {

    public static final CrateStorageType TYPE =
            Registry.register(
                    CreateRegistries.MOUNTED_ITEM_STORAGE_TYPE,
                    ResourceKey.create(
                            CreateRegistryKeys.MOUNTED_ITEM_STORAGE_TYPE, RegistryUtil.id("crate")),
                    new CrateStorageType());

    private CreateMountedCrates() {}

    public static void register() {
        for (var crate :
                List.of(
                        ModBlocks.CRATE_IRON,
                        ModBlocks.CRATE_STEEL,
                        ModBlocks.CRATE_DESH,
                        ModBlocks.CRATE_TUNGSTEN,
                        ModBlocks.SAFE)) {
            MountedItemStorageType.REGISTRY.register(crate.get(), TYPE);
        }
    }

    public static final class CrateStorageType extends SimpleMountedStorageType<Storage> {

        private CrateStorageType() {
            super(Storage.CODEC);
        }

        @Override
        protected @Nullable Container getHandler(Level level, BlockEntity be) {
            if (be instanceof LockableBlockEntity crate && crate.isLocked()) return null;
            return super.getHandler(level, be);
        }

        @Override
        protected Storage createStorage(Container handler) {
            return new Storage(handler);
        }
    }

    public static final class Storage extends SimpleMountedStorage {

        private static final MapCodec<Storage> CODEC = SimpleMountedStorage.codec(Storage::new);

        private Storage(Container handler) {
            super(TYPE, handler);
        }
    }
}
