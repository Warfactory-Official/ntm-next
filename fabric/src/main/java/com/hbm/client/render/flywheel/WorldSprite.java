// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.render.ItemSprites;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.ModelUtil;
import dev.engine_room.flywheel.lib.model.SimpleModel;
import dev.engine_room.flywheel.lib.model.baked.BakedMesh;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class WorldSprite {
    private static final Map<StackKey, Model> MODELS = new ConcurrentHashMap<>();
    private static final Set<StackKey> NOT_SPRITES = ConcurrentHashMap.newKeySet();
    private static @Nullable ItemSprites sprites;

    private final InstancerProvider instancers;
    private final Matrix4f pose = new Matrix4f();
    private ItemStack stack = ItemStack.EMPTY;
    private volatile @Nullable Captured captured;
    private @Nullable Model applied;
    private @Nullable TransformedInstance instance;
    private boolean posed;

    public WorldSprite(InstancerProvider instancers) {
        this.instancers = instancers;
    }

    public static void clear() {
        assert Minecraft.getInstance().isSameThread();
        MODELS.clear();
        NOT_SPRITES.clear();
        sprites = null;
    }

    public void set(ItemStack next) {
        if (next.isEmpty()
                ? stack.isEmpty()
                : !stack.isEmpty() && ItemStack.isSameItemSameComponents(next, stack)) {
            return;
        }
        ItemStack request = next.isEmpty() ? ItemStack.EMPTY : next.copyWithCount(1);
        stack = request;
        if (request.isEmpty()) return;
        StackKey key = new StackKey(request);
        Model cached = MODELS.get(key);
        if (cached != null || NOT_SPRITES.contains(key)) {
            captured = new Captured(request, cached);
            return;
        }
        Minecraft.getInstance().execute(() -> captured = new Captured(request, resolve(key)));
    }

    public boolean notSprite() {
        Captured current = captured;
        return current != null && current.stack() == stack && current.model() == null;
    }

    public static boolean resolvedSprite(ItemStack stack) {
        return MODELS.containsKey(new StackKey(stack));
    }

    private static @Nullable Model resolve(StackKey key) {
        Model model = MODELS.get(key);
        if (model != null || NOT_SPRITES.contains(key)) return model;
        model = model(key);
        if (model == null) NOT_SPRITES.add(key);
        else MODELS.put(key, model);
        return model;
    }

    public void write(Matrix4fc at) {
        Captured current = captured;
        Model target = current != null && current.stack() == stack ? current.model() : null;
        boolean rebuilt = target != applied;
        if (rebuilt) {
            if (instance != null) instance.delete();
            instance =
                    target == null
                            ? null
                            : instancers
                                    .instancer(InstanceTypes.TRANSFORMED, target)
                                    .createInstance();
            applied = target;
        }
        if (instance == null || !rebuilt && posed && pose.equals(at)) return;
        pose.set(at);
        instance.setTransform(pose).light(0).setChanged();
        posed = true;
    }

    public void delete() {
        if (instance != null) instance.delete();
        instance = null;
        applied = null;
    }

    private static @Nullable Model model(StackKey key) {
        if (sprites == null)
            sprites = new ItemSprites(Minecraft.getInstance().getItemModelResolver());
        List<ItemSprites.Pass> passes = sprites.resolve(key.stack(), Minecraft.getInstance().level);
        if (passes == null) return null;
        PoseStack passPose = new PoseStack();
        ItemSprites.passPose(passPose);
        List<Model.ConfiguredMesh> meshes = new ArrayList<>();
        for (ItemSprites.Pass pass : passes) {
            boolean blocksAtlas =
                    pass.quads()
                            .getFirst()
                            .materialInfo()
                            .sprite()
                            .atlasLocation()
                            .equals(TextureAtlas.LOCATION_BLOCKS);
            Material material =
                    SimpleMaterial.builderOf(
                                    WorldItem.lit(
                                            ModelUtil.getItemMaterial(
                                                    ChunkSectionLayer.CUTOUT, blocksAtlas)))
                            .backfaceCulling(true)
                            .build();
            meshes.add(
                    new Model.ConfiguredMesh(
                            material,
                            mesh(pass, passPose.last().pose(), passPose.last().normal())));
        }
        return new SimpleModel(meshes);
    }

    private static BakedMesh mesh(ItemSprites.Pass pass, Matrix4fc pose, Matrix3f normal) {
        int vertices = pass.quads().size() * BakedQuad.VERTEX_COUNT;
        float[] positions = new float[vertices * 3];
        float[] uvs = new float[vertices * 2];
        float[] normals = new float[vertices * 3];
        int[] colors = new int[vertices];
        int[] overlays = new int[vertices];
        Vector3f scratch = new Vector3f();
        int v = 0;
        for (BakedQuad quad : pass.quads()) {
            int tint = quad.materialInfo().tintIndex();
            int color = tint >= 0 && tint < pass.tints().length ? pass.tints()[tint] : -1;
            normal.transform(quad.direction().getUnitVec3f(), scratch).normalize();
            float nx = scratch.x, ny = scratch.y, nz = scratch.z;
            for (int corner = 0; corner < BakedQuad.VERTEX_COUNT; corner++, v++) {
                pose.transformPosition(quad.position(corner), scratch);
                positions[v * 3] = scratch.x;
                positions[v * 3 + 1] = scratch.y;
                positions[v * 3 + 2] = scratch.z;
                long uv = quad.packedUV(corner);
                uvs[v * 2] = UVPair.unpackU(uv);
                uvs[v * 2 + 1] = UVPair.unpackV(uv);
                normals[v * 3] = nx;
                normals[v * 3 + 1] = ny;
                normals[v * 3 + 2] = nz;
                colors[v] = color;
                overlays[v] = OverlayTexture.NO_OVERLAY;
            }
        }
        return new BakedMesh(positions, uvs, normals, colors, overlays);
    }

    private record Captured(ItemStack stack, @Nullable Model model) {}

    private record StackKey(ItemStack stack) {
        @Override
        public boolean equals(Object o) {
            return o instanceof StackKey other
                    && ItemStack.isSameItemSameComponents(stack, other.stack);
        }

        @Override
        public int hashCode() {
            return ItemStack.hashItemAndComponents(stack);
        }
    }
}
