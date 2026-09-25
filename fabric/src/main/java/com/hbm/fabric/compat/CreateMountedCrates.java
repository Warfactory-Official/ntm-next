// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.compat;

import com.hbm.blocks.ModBlocks;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityLockableBase;
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
                            CreateRegistryKeys.MOUNTED_ITEM_STORAGE_TYPE, Library.id("crate")),
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
            if (be instanceof BlockEntityLockableBase crate && crate.isLocked()) return null;
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
