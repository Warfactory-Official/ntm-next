// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
        return FaceBakery.bakeQuad(baker, from, to, face, material, dir, state, null, true, 0);
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
