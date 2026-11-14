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

package com.hbm.client.model;

import com.mojang.math.Quadrant;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public final class Boxes {
    public static final ModelState IDENTITY = new ModelState() {};
    public static final Vector3fc UNIT_FROM = new Vector3f(0F, 0F, 0F);
    public static final Vector3fc UNIT_TO = new Vector3f(16F, 16F, 16F);

    private Boxes() {}

    public static ModelState pose(Transformation transformation) {
        return new Posed(transformation);
    }

    public static BakedQuad face(
            ModelBaker baker,
            Vector3fc from,
            Vector3fc to,
            Direction dir,
            Material.Baked material) {
        return face(
                baker, from, to, dir, material, null, Quadrant.R0, CuboidFace.NO_TINT, IDENTITY);
    }

    public static BakedQuad face(
            ModelBaker baker,
            Vector3fc from,
            Vector3fc to,
            Direction dir,
            Material.Baked material,
            CuboidFace.@Nullable UVs uvs,
            Quadrant quadrant,
            int tint,
            ModelState state) {
        CuboidFace face = new CuboidFace(null, tint, "", uvs, quadrant);
        return FaceBakery.bakeQuad(baker, from, to, face, material, dir, state, null, null, 0);
    }

    public static void cube(
            QuadCollection.Builder builder, ModelBaker baker, Material.Baked material, int tint) {
        for (Direction dir : Direction.VALUES) {
            builder.addCulledFace(
                    dir,
                    face(
                            baker,
                            UNIT_FROM,
                            UNIT_TO,
                            dir,
                            material,
                            null,
                            Quadrant.R0,
                            tint,
                            IDENTITY));
        }
    }

    private record Posed(Transformation transformation) implements ModelState {}
}
