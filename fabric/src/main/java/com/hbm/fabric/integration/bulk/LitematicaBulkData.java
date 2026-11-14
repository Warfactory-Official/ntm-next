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

package com.hbm.fabric.integration.bulk;

import com.hbm.world.bulk.BulkSnapshot;
import com.hbm.world.bulk.BulkTransform;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.converter.DataConverterNbt;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class LitematicaBulkData {

    private LitematicaBulkData() {}

    public static @Nullable CompoundTag transform(
            CompoundData data,
            BlockState state,
            Mirror mainMirror,
            Mirror subregionMirror,
            Rotation rotation,
            HolderLookup.Provider registries) {
        CompoundTag tag = DataConverterNbt.toVanillaCompound(data);
        if (!tag.contains(BulkSnapshot.KEY)) {
            return null;
        }
        return forPaste(
                BulkSnapshot.transform(
                        tag, BulkTransform.of(mainMirror, subregionMirror, rotation)),
                state,
                true,
                registries);
    }

    public static CompoundTag forPaste(
            CompoundTag tag,
            BlockState state,
            boolean includeNbt,
            HolderLookup.Provider registries) {
        if (!includeNbt) {
            return BulkSnapshot.publicData(tag);
        }
        if (Configs.Generic.PASTE_IGNORE_INVENTORY.getBooleanValue()) {
            return BulkSnapshot.withoutInventory(tag, state, registries);
        }
        return tag;
    }
}
