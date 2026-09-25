// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
