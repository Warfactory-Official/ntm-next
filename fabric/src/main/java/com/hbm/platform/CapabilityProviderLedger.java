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

package com.hbm.platform;

import com.hbm.registration.Registrar;
import com.hbm.registration.RegistryHandle;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class CapabilityProviderLedger {

    private final Map<Identifier, Reference2IntOpenHashMap<Object>> registrations =
            new LinkedHashMap<>();

    public void blocks(Identifier lookup, Block... blocks) {
        Reference2IntOpenHashMap<Object> keys = keys(lookup);
        for (Block block : blocks) keys.addTo(block, 1);
    }

    public void blockEntity(Identifier lookup, BlockEntityType<?> type) {
        keys(lookup).addTo(type, 1);
    }

    private Reference2IntOpenHashMap<Object> keys(Identifier lookup) {
        return registrations.computeIfAbsent(lookup, id -> new Reference2IntOpenHashMap<>());
    }

    public Map<Identifier, Reference2IntMap<Block>> providersPerBlock() {
        Map<Identifier, Reference2IntMap<Block>> out = new LinkedHashMap<>();
        registrations.forEach(
                (lookup, keys) -> {
                    Reference2IntOpenHashMap<Block> perBlock = new Reference2IntOpenHashMap<>();
                    for (Reference2IntMap.Entry<Object> entry : keys.reference2IntEntrySet()) {
                        if (entry.getKey() instanceof BlockEntityType<?> type) {
                            for (RegistryHandle<? extends Block> handle :
                                    Registrar.registeredBlocks) {
                                if (type.isValid(handle.get().defaultBlockState())) {
                                    perBlock.addTo(handle.get(), entry.getIntValue());
                                }
                            }
                        } else {
                            perBlock.addTo((Block) entry.getKey(), entry.getIntValue());
                        }
                    }
                    out.put(lookup, perBlock);
                });
        return out;
    }
}
