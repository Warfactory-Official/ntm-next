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

package com.hbm.uninos.graph;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.server.level.ServerLevel;

public interface GraphProvider<D> {
    boolean dataCompatible(D a, D b);

    Codec<List<D>> dataTableCodec();

    default void onNetsMerged(NodeNetwork<D> keep, NodeNetwork<D> drain) {}

    default void onNetTopologyChanged(NodeNetwork<D> net) {}

    default void tick(ServerLevel level, LevelNodeGraph<D> graph) {}
}
