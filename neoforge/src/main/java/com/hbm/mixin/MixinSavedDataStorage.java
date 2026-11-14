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

package com.hbm.mixin;

import com.hbm.explosion.FalloutJournal;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.nio.file.Path;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SavedDataStorage.class)
public abstract class MixinSavedDataStorage {
    @Shadow
    protected abstract Path getDataFile(Identifier id);

    @Shadow @Final private HolderLookup.Provider registries;

    @SuppressWarnings("unchecked")
    @WrapMethod(method = "readSavedData")
    private <T extends SavedData> @Nullable T hbm$readFalloutJournal(
            SavedDataType<T> type, Operation<T> original) {
        return FalloutJournal.isJournal(type)
                ? (T) FalloutJournal.readJournal(getDataFile(type.id()), registries)
                : original.call(type);
    }

    @WrapMethod(method = "tryWrite")
    private void hbm$writeFalloutJournal(
            SavedDataType<?> type, CompoundTag tag, Operation<Void> original) {
        if (FalloutJournal.isJournal(type)) {
            FalloutJournal.writeJournal(tag, getDataFile(type.id()));
        } else {
            original.call(type, tag);
        }
    }
}
