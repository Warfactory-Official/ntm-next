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

package com.hbm.fabric.client;

import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public final class HbmUnbakedModel implements UnbakedModel {

    private final UnbakedModel wrapped;
    private final UnbakedGeometry geometry;

    public HbmUnbakedModel(UnbakedModel wrapped, UnbakedGeometry geometry) {
        this.wrapped = wrapped;
        this.geometry = geometry;
    }

    @Override
    public @Nullable Boolean ambientOcclusion() {
        return wrapped.ambientOcclusion();
    }

    @Override
    public @Nullable GuiLight guiLight() {
        return wrapped.guiLight();
    }

    @Override
    public @Nullable ItemTransforms transforms() {
        return wrapped.transforms();
    }

    @Override
    public TextureSlots.Data textureSlots() {
        return wrapped.textureSlots();
    }

    @Override
    public UnbakedGeometry geometry() {
        return geometry;
    }

    @Override
    public @Nullable Identifier parent() {
        return wrapped.parent();
    }
}
