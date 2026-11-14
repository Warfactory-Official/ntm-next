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

import com.hbm.uninos.graph.EndpointRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface BlockEntityLifecycle extends BulkParticipant {
    default boolean hbm$bulkManaged() {
        throw new IllegalStateException("Bulk registration state is missing");
    }

    default void hbm$bulkManaged(boolean managed) {
        throw new IllegalStateException("Bulk registration state is missing");
    }

    default void hbm$declareEndpoint(ServerLevel level, BlockEntity be) {
        EndpointRegistry.declareSelfIfEndpoint(level, be);
    }

    default void hbm$blockEntityLoaded(ServerLevel level, BlockEntity be) {}

    default void hbm$blockEntityRemoved(ServerLevel level, BlockEntity be) {}
}
