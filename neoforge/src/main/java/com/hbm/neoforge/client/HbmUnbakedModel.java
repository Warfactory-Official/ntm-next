// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.client;

import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.neoforged.neoforge.client.model.AbstractUnbakedModel;
import net.neoforged.neoforge.client.model.StandardModelParameters;

public final class HbmUnbakedModel extends AbstractUnbakedModel {

    private final UnbakedGeometry geometry;

    public HbmUnbakedModel(StandardModelParameters parameters, UnbakedGeometry geometry) {
        super(parameters);
        this.geometry = geometry;
    }

    @Override
    public UnbakedGeometry geometry() {
        return geometry;
    }
}
