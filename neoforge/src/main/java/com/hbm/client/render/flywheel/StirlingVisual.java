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

import com.hbm.blockentity.machine.thermal.StirlingGeneratorBlockEntity;
import com.hbm.blocks.machine.StirlingBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.main.ResourceManager;
import com.hbm.util.Facing;
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
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class StirlingVisual
        extends AbstractBoundedBlockEntityVisual<StirlingGeneratorBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual, CulledDynamicVisual {
    private static final int SKINS = 3;
    private static final String[] NAMES = {"Cog", "CogSmall", "Piston"};
    private static final Identifier[] TEXTURES = {
        ResourceManager.stirling_tex,
        ResourceManager.stirling_steel_tex,
        ResourceManager.stirling_creative_tex
    };
    private static final MeshPart[][] PARTS = buildParts();
    private static final ChainInstance.Chain[] CHAINS = {
        new ChainInstance.Chain()
                .translate(0F, 1.375F, 0F)
                .rotateZ(0, -Mth.DEG_TO_RAD)
                .translate(0F, -1.375F, 0F)
                .end(),
        new ChainInstance.Chain()
                .translate(0F, 1.375F, .25F)
                .rotateX(1, Mth.DEG_TO_RAD)
                .translate(0F, -1.375F, -.25F)
                .end(),
        new ChainInstance.Chain().sine(2).translate(.125F, 0F, 0F).slideX(2, .25F)
    };
    private final AABB lightBounds;
    private final ChainParts parts;
    private boolean lastHasCog;
    private boolean initialized;

    public StirlingVisual(
            VisualizationContext context,
            StirlingGeneratorBlockEntity blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        MeshPart[] skin = PARTS[((StirlingBlock) blockState.getBlock()).tier().gear];
        Matrix4f bodyPose =
                new Matrix4f()
                        .translate(.5F, 0F, .5F)
                        .rotateY(
                                (Facing.yaw(MultiblockCoreBlock.coreFacing(blockState), 180))
                                        * Mth.DEG_TO_RAD);
        AABB bodyBounds = LightBounds.of(ResourceManager.stirling, "Base", bodyPose, pos);
        parts =
                new ChainParts(
                        context,
                        visualPos,
                        bodyPose,
                        new Model[] {skin[0].model(), skin[1].model(), skin[2].model()},
                        CHAINS,
                        3);
        double[] bounds = new double[6];
        LightBounds.resetBounds(
                bounds, bodyBounds.minmax(new AABB(pos).expandTowards(1, 2, 1).inflate(1)));
        parts.includeLightBounds(bounds, pos);
        lightBounds = new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
        updateCog();
        updateSpin();
    }

    private static MeshPart[][] buildParts() {
        var parts = new MeshPart[SKINS][NAMES.length];
        for (int skin = 0; skin < SKINS; skin++)
            for (int i = 0; i < NAMES.length; i++)
                parts[skin][i] =
                        MeshPart.obj(
                                ResourceManager.stirling
                                        .groups[ResourceManager.stirling.partId(NAMES[i])],
                                ResourceManager.stirling.smoothing(),
                                MeshPart.litCutout(TEXTURES[skin]));
        return parts;
    }

    public static void initModels() {}

    @Override
    public void frame(DynamicVisual.Context context) {
        updateCog();
    }

    @Override
    public void tick(TickableVisual.Context context) {
        updateSpin();
    }

    private void updateCog() {
        boolean hasCog = blockEntity.hasCog;
        if (initialized && hasCog == lastHasCog) return;
        lastHasCog = hasCog;
        initialized = true;
        parts.instance(0).setVisible(hasCog);
    }

    private void updateSpin() {
        float from = blockEntity.lastSpin, to = blockEntity.spin;
        parts.set(0, from, to);
        parts.set(1, from * 2F + 3F, to * 2F + 3F);
        parts.set(2, (float) (from * Math.PI / 90D), (float) (to * Math.PI / 90D));
        parts.write();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return lightBounds;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        parts.collectCrumblingInstances(consumer);
    }

    @Override
    protected void _delete() {
        parts.delete();
    }
}
