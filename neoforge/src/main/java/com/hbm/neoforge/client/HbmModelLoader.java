// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.client;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.hbm.client.model.HbmModelJson;
import java.util.function.Function;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.neoforged.neoforge.client.model.StandardModelParameters;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;

public record HbmModelLoader(Function<JsonObject, UnbakedGeometry> geometry)
        implements UnbakedModelLoader<HbmUnbakedModel> {

    @Override
    public HbmUnbakedModel read(JsonObject json, JsonDeserializationContext ctx)
            throws JsonParseException {
        return new HbmUnbakedModel(StandardModelParameters.parse(json, ctx), geometry.apply(json));
    }
}
