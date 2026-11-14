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

import com.hbm.blockentity.StructureAssemblyBlockEntity;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.util.GameTime;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.UvTransformedInstance;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public class StructurePreviewVisual<T extends StructureAssemblyBlockEntity>
        extends AbstractBoundedBlockEntityVisual<T> implements ShaderLightVisual {
    private final UvTransformedInstance[] ghosts;
    private final int[] masks;
    private Direction facing;
    private int variant = -1;

    public static <T extends StructureAssemblyBlockEntity> StructurePreviewVisual<T> create(
            VisualizationContext context, T blockEntity, float partialTick) {
        return blockEntity.getPattern().getVariantCount() > 1
                ? new Cycling<>(context, blockEntity, partialTick)
                : new StructurePreviewVisual<>(context, blockEntity, partialTick);
    }

    public StructurePreviewVisual(VisualizationContext context, T blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        var pattern = blockEntity.getPattern();
        ghosts = new UvTransformedInstance[pattern.size()];
        masks = new int[pattern.size()];
        facing = blockEntity.getAssemblyFacing();
        var model =
                blockEntity.usesSouthPreviewNormal() ? PreviewCube.SOUTH_MODEL : PreviewCube.MODEL;
        var instancer = instancerProvider().instancer(InstanceTypes.UV_TRANSFORMED, model);
        var pose = new Matrix4f();
        for (int cell = 0; cell < pattern.size(); cell++) {
            long offset = pattern.getOffset(cell);
            int x = BlockPos.getX(offset), y = BlockPos.getY(offset), z = BlockPos.getZ(offset);
            var sprite =
                    Minecraft.getInstance()
                            .getModelManager()
                            .getBlockStateModelSet()
                            .getParticleMaterial(pattern.getBlock(cell).defaultBlockState())
                            .sprite();
            var instance = instancer.createInstance();
            instance.uvRegion(
                    sprite.getU0(),
                    sprite.getV0(),
                    sprite.getU1() - sprite.getU0(),
                    sprite.getV1() - sprite.getV0());
            instance.setTransform(
                            pose.translation(
                                    visualPos.getX() + MultiblockMaskTable.worldX(x, y, z, facing),
                                    visualPos.getY() + y,
                                    visualPos.getZ() + MultiblockMaskTable.worldZ(x, y, z, facing)))
                    .setChanged();
            VisualTextures.animated(sprite);
            ghosts[cell] = instance;
            masks[cell] = pattern.getVariantMask(cell);
        }
        updateVariant();
    }

    @Override
    public void update(float partialTick) {
        var next = blockEntity.getAssemblyFacing();
        if (next == facing) return;
        facing = next;
        var pattern = blockEntity.getPattern();
        var pose = new Matrix4f();
        for (int cell = 0; cell < ghosts.length; cell++) {
            long offset = pattern.getOffset(cell);
            int x = BlockPos.getX(offset), y = BlockPos.getY(offset), z = BlockPos.getZ(offset);
            ghosts[cell]
                    .setTransform(
                            pose.translation(
                                    visualPos.getX() + MultiblockMaskTable.worldX(x, y, z, facing),
                                    visualPos.getY() + y,
                                    visualPos.getZ() + MultiblockMaskTable.worldZ(x, y, z, facing)))
                    .setChanged();
        }
    }

    protected final void updateVariant() {
        int wanted = blockEntity.getPreviewVariant(GameTime.millis());
        if (wanted == variant) return;
        for (int cell = 0; cell < ghosts.length; cell++) {
            boolean visible = (masks[cell] & 1 << wanted) != 0;
            if (variant < 0 || visible != ((masks[cell] & 1 << variant) != 0))
                ghosts[cell].setVisible(visible);
        }
        variant = wanted;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return blockEntity.getVisualBounds().move(pos);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        for (var ghost : ghosts) ghost.delete();
    }

    private static final class Cycling<T extends StructureAssemblyBlockEntity>
            extends StructurePreviewVisual<T> implements CulledDynamicVisual {
        private Cycling(VisualizationContext context, T blockEntity, float partialTick) {
            super(context, blockEntity, partialTick);
        }

        @Override
        public void frame(Context context) {
            updateVariant();
        }
    }
}
