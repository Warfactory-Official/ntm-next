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

import com.hbm.blocks.generic.PlushieBlock.Plushie;
import com.hbm.blocks.generic.PlushieBlock.PlushieType;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PlushieFigures {

    private static final Map<Plushie, PlushieRenderer.Figure> FIGURES = new ConcurrentHashMap<>();

    private PlushieFigures() {}

    public static void register(Plushie type, PlushieRenderer.Figure figure) {
        if (type instanceof PlushieType || FIGURES.putIfAbsent(type, figure) != null) {
            throw new IllegalStateException("plushie " + type.id() + " already has a figure");
        }
    }

    static PlushieRenderer.Figure get(Plushie type) {
        PlushieRenderer.Figure figure = FIGURES.get(type);
        if (figure == null)
            throw new IllegalStateException("plushie " + type.id() + " has no figure");
        return figure;
    }

    static Collection<PlushieRenderer.Figure> all() {
        return FIGURES.values();
    }
}
