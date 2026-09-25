// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.client;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.hbm.client.model.HbmModelJson;
import com.hbm.lib.Library;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.model.loading.v1.UnbakedModelDeserializer;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;

public final class HbmModelDeserializers {

    private HbmModelDeserializers() {}

    public static void register() {
        register("obj", HbmModelJson::obj);
    }

    private static void register(String type, Function<JsonObject, UnbakedGeometry> geometry) {
        UnbakedModelDeserializer.register(
                Library.id(type), (json, ctx) -> read(json, ctx, geometry));
    }

    private static UnbakedModel read(
            JsonObject json,
            JsonDeserializationContext ctx,
            Function<JsonObject, UnbakedGeometry> geometry) {
        CuboidModel wrapped = ctx.deserialize(json, CuboidModel.class);
        return new HbmUnbakedModel(wrapped, geometry.apply(json));
    }
}
