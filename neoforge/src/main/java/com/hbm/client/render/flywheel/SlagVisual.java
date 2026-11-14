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

package com.hbm.client.render.flywheel;

import com.hbm.blocks.generic.DynamicSlagBlock.BlockEntitySlag;
import com.hbm.client.render.SlagTextures;
import com.hbm.inventory.material.MaterialRegistry;
import com.hbm.util.RegistryUtil;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.AffineUvTransformedInstance;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class SlagVisual extends AbstractBoundedBlockEntityVisual<BlockEntitySlag>
        implements ShaderLightVisual, CulledDynamicVisual {
    private static final Identifier TEXTURE = RegistryUtil.id("textures/block/slag.png");
    private static final Model[] BASE_FACES = baseFaces();
    private static final Model[][] MATERIAL_FACES = materialFaces();
    private final AffineUvTransformedInstance[] faces = new AffineUvTransformedInstance[6];
    private final Matrix4f local = new Matrix4f(), pose = new Matrix4f();
    private Model[] drawParts;
    private float lastHeight = Float.NaN;
    private int lastColor = Integer.MIN_VALUE;
    private boolean lastPresent;

    public SlagVisual(
            VisualizationContext context, BlockEntitySlag blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    private static Material material(Identifier texture) {
        return SimpleMaterial.builderOf(MeshPart.litCutout(texture)).useOverlay(false).build();
    }

    private static Model[] baseFaces() {
        Material material = material(TEXTURE);
        var faces = new Model[Direction.values().length];
        for (Direction direction : Direction.values())
            faces[direction.ordinal()] = MeshPart.face(direction, material).model();
        return faces;
    }

    private static Model[] faces(Identifier texture) {
        Material material = material(texture);
        var faces = new Model[Direction.values().length];
        for (Direction direction : Direction.values())
            faces[direction.ordinal()] =
                    new SingleMeshModel(
                            BASE_FACES[direction.ordinal()].meshes().getFirst().mesh(), material);
        return faces;
    }

    private static Model[][] materialFaces() {
        int maxId = 0;
        for (var material : MaterialRegistry.ordered()) maxId = Math.max(maxId, material.id());
        var faces = new Model[maxId + 1][];
        for (var material : MaterialRegistry.ordered())
            if (material.lightColor() != material.darkColor())
                faces[material.id()] = faces(SlagTextures.texture(material));
        return faces;
    }

    @Override
    public void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float height = Math.min(1F, (float) blockEntity.amount / BlockEntitySlag.MAX_AMOUNT);
        if (height <= 0) {
            if (lastPresent) clear();
            lastPresent = false;
            lastHeight = height;
            return;
        }
        var type = blockEntity.mat;
        boolean recolor = type != null && type.lightColor() != type.darkColor();
        Model[] wanted = recolor ? MATERIAL_FACES[type.id()] : BASE_FACES;
        int color = type == null || recolor ? -1 : 0xFF000000 | type.moltenColor();
        boolean materialChanged = drawParts != wanted;
        if (materialChanged) {
            clear();
            drawParts = wanted;
        }
        if (lastPresent && !materialChanged && height == lastHeight && color == lastColor) return;
        lastPresent = true;
        lastHeight = height;
        lastColor = color;
        local.scaling(1, height, 1);
        pose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(local);
        for (Direction direction : Direction.VALUES) {
            int i = direction.ordinal();
            if (faces[i] == null) {
                var model = drawParts[i];
                faces[i] =
                        instancerProvider()
                                .instancer(AffineUvTransformedInstance.TYPE, model)
                                .createInstance();
            }
            var face = faces[i];
            face.setTransform(pose).colorArgb(color);
            face.light(0);
            float scaleV = direction.getAxis() == Direction.Axis.Y ? 1 : height;
            float offsetV = 1 - scaleV;
            face.uv(1, 0, 0, scaleV, 0, offsetV).setChanged();
        }
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(pos).inflate(1);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (var face : faces) if (face != null) consumer.accept(face);
    }

    private void clear() {
        for (int i = 0; i < 6; i++) {
            if (faces[i] == null) continue;
            faces[i].delete();
            faces[i] = null;
        }
    }

    @Override
    protected void _delete() {
        clear();
    }
}
