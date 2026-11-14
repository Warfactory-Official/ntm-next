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

package com.hbm.interfaces.injected;

import com.hbm.explosion.FalloutReplay;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public interface ChunkExtension {

    String CORE_INDEX_NBT_KEY = "hbm_core_index";

    String PENDING_FALLERS_NBT_KEY = "hbm_pending_fallers";
    String DUCT_FLUIDS_NBT_KEY = "hbm_duct_fluids";
    String FALLOUT_APPLIED_THROUGH_NBT_KEY = "hbm_fallout_applied_through";
    String FALLOUT_REPLAY_NBT_KEY = "hbm_fallout_replay";

    long hbm$falloutAppliedThrough();

    void hbm$setFalloutAppliedThrough(long id);

    @Nullable FalloutReplay hbm$falloutReplay();

    void hbm$setFalloutReplay(@Nullable FalloutReplay replay);

    byte @Nullable [] hbm$getRadiation();

    void hbm$setRadiation(byte @Nullable [] bytes);

    long @Nullable [] hbm$coreIndex();

    int hbm$coreIndexSize();

    void hbm$setCoreIndex(long @Nullable [] entries, int size);

    long @Nullable [] hbm$pendingFallerPositions();

    BlockState @Nullable [] hbm$pendingFallerStates();

    int hbm$pendingFallerSize();

    void hbm$setPendingFallers(
            long @Nullable [] positions, BlockState @Nullable [] states, int size);

    @Nullable Long2ObjectOpenHashMap<String> hbm$ductFluids();

    void hbm$setDuctFluids(@Nullable Long2ObjectOpenHashMap<String> fluids);
}
