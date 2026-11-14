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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.hbm.client.model.HbmModelJson;
import com.hbm.util.RegistryUtil;
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
                RegistryUtil.id(type), (json, ctx) -> read(json, ctx, geometry));
    }

    private static UnbakedModel read(
            JsonObject json,
            JsonDeserializationContext ctx,
            Function<JsonObject, UnbakedGeometry> geometry) {
        CuboidModel wrapped = ctx.deserialize(json, CuboidModel.class);
        return new HbmUnbakedModel(wrapped, geometry.apply(json));
    }
}
