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

package com.hbm.client.render;

import com.hbm.blocks.generic.BobbleBlock.Bobble;
import com.hbm.blocks.generic.BobbleBlock.BobbleType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BobbleFigures {

    private static final double[] ZERO = {0, 0, 0};
    public static final BobbleRenderer.Pose NO_POSE =
            new BobbleRenderer.Pose(ZERO, ZERO, ZERO, ZERO, 0, ZERO);
    private static final Map<Bobble, BobbleRenderer.Figure> FIGURES = new ConcurrentHashMap<>();

    private BobbleFigures() {}

    public static void register(Bobble type, BobbleRenderer.Figure figure) {
        if (type instanceof BobbleType || FIGURES.putIfAbsent(type, figure) != null) {
            throw new IllegalStateException("bobblehead " + type.id() + " already has a figure");
        }
    }

    public static BobbleRenderer.Figure get(Bobble type) {
        BobbleRenderer.Figure figure = FIGURES.get(type);
        if (figure == null)
            throw new IllegalStateException("bobblehead " + type.id() + " has no figure");
        return figure;
    }
}
