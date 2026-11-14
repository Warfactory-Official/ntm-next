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

import com.hbm.blockentity.processing.BurnerPressBlockEntity;
import com.hbm.client.render.FramedItem;
import com.hbm.client.render.PressRenderer;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.GroupObject;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class PressVisual extends AbstractBoundedBlockEntityVisual<BurnerPressBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual, CulledDynamicVisual {
    private static final MeshPart[] HEAD =
            MeshPart.objParts(
                    ResourceManager.press_head, MeshPart.litCutout(ResourceManager.press_head_tex));
    private static final ChainInstance.Chain HEAD_CHAIN =
            new ChainInstance.Chain().clamp(0).slideY(0, .875F);
    private static final float[] LIFT_LOW = {0F};
    private static final float[] LIFT_HIGH = {1F};
    private final ChainParts heads;
    private final AABB lightBounds;
    private final WorldItem item = new WorldItem(visualizationContext, level, pos);
    private final PoseStack itemPoses = new PoseStack();

    public PressVisual(
            VisualizationContext context, BurnerPressBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        var bodyLocal = new Matrix4f().translation(.5F, 0F, .5F).rotateY(Mth.PI);
        Matrix4f headBasePose = new Matrix4f().translation(.5F, 0F, .5F).scale(.99F, 1F, .99F);
        AABB extent = new AABB(pos);
        for (GroupObject group : ResourceManager.press_body.groups)
            extent = extent.minmax(LightBounds.of(group, bodyLocal, pos));
        AABB bodyBounds = extent;
        ChainInstance.Chain[] chains = new ChainInstance.Chain[HEAD.length];
        Model[] models = new Model[HEAD.length];
        for (int i = 0; i < HEAD.length; i++) {
            chains[i] = HEAD_CHAIN;
            models[i] = HEAD[i].model();
        }
        heads = new ChainParts(context, visualPos, headBasePose, models, chains, 1);
        double[] bounds = new double[6];
        LightBounds.resetBounds(
                bounds, bodyBounds.minmax(new AABB(pos).expandTowards(0, 2, 0).inflate(1)));
        heads.includeLightBounds(bounds, pos, LIFT_LOW, LIFT_HIGH);
        lightBounds = new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
        updateHead();
        updateItem(partialTick);
    }

    public static void initModels() {}

    public static boolean vanillaNeeded(BurnerPressBlockEntity be) {
        return !WorldItem.drawsFramed(be.getItem(BurnerPressBlockEntity.SLOT_INPUT));
    }

    @Override
    public void frame(DynamicVisual.Context context) {
        updateItem(context.partialTick());
    }

    @Override
    public void tick(TickableVisual.Context context) {
        updateHead();
    }

    private void updateHead() {
        heads.set(
                0,
                (float) (1D - blockEntity.lastPress / (double) BurnerPressBlockEntity.MAX_PROGRESS),
                (float)
                        (1D
                                - blockEntity.renderPress
                                        / (double) BurnerPressBlockEntity.MAX_PROGRESS));
        heads.write();
    }

    private void updateItem(float partialTick) {
        FramedItem.Arm arm =
                item.setFramed(blockEntity.getItem(BurnerPressBlockEntity.SLOT_INPUT), level);
        if (arm == null) return;
        itemPoses.setIdentity();
        itemPoses.translate(visualPos.getX(), visualPos.getY(), visualPos.getZ());
        PressRenderer.PRESS_ITEM.apply(itemPoses, 0F);
        FramedItem.decoArm(itemPoses, arm);
        item.write(itemPoses.last().pose(), partialTick);
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return lightBounds;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        heads.collectCrumblingInstances(consumer);
    }

    @Override
    protected void _delete() {
        heads.delete();
        item.delete();
    }
}
