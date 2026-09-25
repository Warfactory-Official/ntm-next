// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.render.loader.GroupObject;
import com.hbm.render.loader.HFRWavefrontObject;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.IndexSequence;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.vertex.MutableVertexList;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.LineModelBuilder;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.joml.Vector4fc;

public final class MeshPart {
    private final Model model;

    public MeshPart(Model model) {
        this.model = model;
    }

    public static MeshPart create(Mesh mesh, Material material) {
        return new MeshPart(new SingleMeshModel(mesh, material));
    }

    public static Material litCutout(Identifier texture) {
        return SimpleMaterial.builderOf(Materials.CUTOUT)
                .texture(texture)
                .mipmap(false)
                .cutout(CutoutShaders.ONE_TENTH)
                .light(LightShaders.SMOOTH)
                .ambientOcclusion(false)
                .cardinalLightingMode(CardinalLightingMode.CHUNK)
                .build();
    }

    public static MeshPart obj(GroupObject group, boolean smooth, Material material) {
        return create(PackedQuadMesh.of(group, smooth), material);
    }

    public static MeshPart[] objParts(HFRWavefrontObject source, Material material) {
        return objParts(source, source.smoothing(), material);
    }

    public static MeshPart[] objParts(
            HFRWavefrontObject source, boolean smooth, Material material) {
        var parts = new MeshPart[source.groups.length];
        for (int i = 0; i < parts.length; i++) parts[i] = obj(source.groups[i], smooth, material);
        return parts;
    }

    public static Model line(float width, Material material) {
        return new SingleMeshModel(
                lines(new LineModelBuilder(1).line(0, 0, 0, 0, 1, 0), width), material);
    }

    public static Mesh lines(LineModelBuilder builder, float width) {
        Mesh source = builder.build().meshes().getFirst().mesh();
        return new Mesh() {
            @Override
            public int vertexCount() {
                return source.vertexCount();
            }

            @Override
            public int indexCount() {
                return source.indexCount();
            }

            @Override
            public IndexSequence indexSequence() {
                return source.indexSequence();
            }

            @Override
            public Vector4fc boundingSphere() {
                return source.boundingSphere();
            }

            @Override
            public void write(MutableVertexList vertices) {
                source.write(vertices);
                for (int i = 0; i < vertexCount(); i++) vertices.u(i, width);
            }
        };
    }

    public static MeshPart face(Direction direction, Material material) {
        float[][] vertices = faceVertices(direction);
        float[] data = new float[32];
        for (int i = 0; i < 4; i++) {
            System.arraycopy(vertices[i], 0, data, i * 8, 5);
            data[i * 8 + 5] = direction.getStepX();
            data[i * 8 + 6] = direction.getStepY();
            data[i * 8 + 7] = direction.getStepZ();
        }
        var mesh = PackedQuadMesh.of(data, new int[] {-1, -1, -1, -1}, new int[4]);
        return create(mesh, material);
    }

    private static float[][] faceVertices(Direction direction) {
        return switch (direction) {
            case UP ->
                    new float[][] {
                        {1, 1, 1, 1, 1}, {1, 1, 0, 1, 0}, {0, 1, 0, 0, 0}, {0, 1, 1, 0, 1}
                    };
            case DOWN ->
                    new float[][] {
                        {0, 0, 1, 0, 1}, {0, 0, 0, 0, 0}, {1, 0, 0, 1, 0}, {1, 0, 1, 1, 1}
                    };
            case NORTH ->
                    new float[][] {
                        {0, 1, 0, 1, 0}, {1, 1, 0, 0, 0}, {1, 0, 0, 0, 1}, {0, 0, 0, 1, 1}
                    };
            case SOUTH ->
                    new float[][] {
                        {0, 1, 1, 0, 0}, {0, 0, 1, 0, 1}, {1, 0, 1, 1, 1}, {1, 1, 1, 1, 0}
                    };
            case WEST ->
                    new float[][] {
                        {0, 1, 1, 1, 0}, {0, 1, 0, 0, 0}, {0, 0, 0, 0, 1}, {0, 0, 1, 1, 1}
                    };
            case EAST ->
                    new float[][] {
                        {1, 0, 1, 0, 1}, {1, 0, 0, 1, 1}, {1, 1, 0, 1, 0}, {1, 1, 1, 0, 0}
                    };
        };
    }

    public static MeshPart box(
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            Material material,
            int faces) {
        float[] data = new float[Integer.bitCount(faces) * 32];
        int at = 0;
        for (Direction direction : Direction.VALUES) {
            if ((faces & 1 << direction.ordinal()) == 0) continue;
            for (float[] vertex : faceVertices(direction)) {
                float x = vertex[0] == 0 ? x0 : x1;
                float y = vertex[1] == 0 ? y0 : y1;
                float z = vertex[2] == 0 ? z0 : z1;
                data[at++] = x;
                data[at++] = y;
                data[at++] = z;
                data[at++] =
                        switch (direction) {
                            case UP, DOWN, SOUTH -> x;
                            case NORTH -> 1 - x;
                            case WEST -> z;
                            case EAST -> 1 - z;
                        };
                data[at++] = direction.getAxis() == Direction.Axis.Y ? z : 1 - y;
                data[at++] = direction.getStepX();
                data[at++] = direction.getStepY();
                data[at++] = direction.getStepZ();
            }
        }
        var mesh = PackedQuadMesh.of(data, null, null);
        return create(mesh, material);
    }

    public Model model() {
        return model;
    }

    public MeshPart withMaterial(Material material) {
        return new MeshPart(new SingleMeshModel(model.meshes().getFirst().mesh(), material));
    }
}
