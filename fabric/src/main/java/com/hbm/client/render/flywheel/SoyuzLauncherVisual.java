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

import com.hbm.blockentity.machine.SoyuzLauncherBlockEntity;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class SoyuzLauncherVisual
        extends AbstractBoundedBlockEntityVisual<SoyuzLauncherBlockEntity>
        implements ShaderLightVisual, CulledDynamicVisual {
    private static final float OPEN = 45F;
    private static final int SWING_TICKS = 20;
    private static final double MESH_DROP = 4D;
    private static final double ROCKET_LIFT = 5D;
    private static final double TOWER_PIVOT_Y = 5.5D;
    private static final double TOWER_PIVOT_Z = 5.5D;
    private static final double SUPPORT_PIVOT_Z = -6.5D;
    private static final String[] SINGLE = {
        "EngineBlock",
        "BottomStage",
        "TopStage",
        "Payload",
        "PayloadBlocks",
        "LES",
        "LESThrusters",
        "MainEngines",
        "SideEngines"
    };
    private static final String[] BOOSTERS = {
        "Booster.000", "Booster.001", "Booster.002", "Booster.003"
    };
    private static final String[] BOOSTER_ENGINES = {
        "BoosterEngines.000", "BoosterEngines.001", "BoosterEngines.002", "BoosterEngines.003"
    };
    private static final String[] BOOSTER_SIDES = {
        "BoosterSide.000", "BoosterSide.001", "BoosterSide.002", "BoosterSide.003"
    };
    private static final MeshPart SOYUZ_LAUNCHER_TOWER =
            rawPart(
                    ResourceManager.soyuz_launcher_tower,
                    0,
                    ResourceManager.soyuz_launcher_tower_tex);
    private static final MeshPart SOYUZ_LAUNCHER_SUPPORT =
            rawPart(
                    ResourceManager.soyuz_launcher_support,
                    0,
                    ResourceManager.soyuz_launcher_support_tex);
    private static final ChainInstance.Chain TOWER_CHAIN =
            new ChainInstance.Chain()
                    .translate(0F, (float) TOWER_PIVOT_Y, (float) TOWER_PIVOT_Z)
                    .rotateX(0, Mth.DEG_TO_RAD)
                    .translate(0F, (float) -TOWER_PIVOT_Y, (float) -TOWER_PIVOT_Z)
                    .rotationMultiplyDivide(0, OPEN, SWING_TICKS);
    private static final ChainInstance.Chain SUPPORT_CHAIN =
            new ChainInstance.Chain()
                    .translate(0F, (float) TOWER_PIVOT_Y, (float) SUPPORT_PIVOT_Z)
                    .rotateX(0, -Mth.DEG_TO_RAD)
                    .translate(0F, (float) -TOWER_PIVOT_Y, (float) -SUPPORT_PIVOT_Z)
                    .rotationMultiplyDivide(0, OPEN, SWING_TICKS);
    private static final PackedQuadMesh[] ROCKET_MESHES = buildRocketMeshes();
    private static final MeshPart[][] ROCKETS = buildRockets();

    static MeshPart[] rocketParts(int skin) {
        return ROCKETS[skin];
    }

    private final ChainParts arms;
    private final AABB bodyBounds;
    private final Matrix4f rocketPose = new Matrix4f();
    private @Nullable RocketSet rocket;
    private int lastRocketType = Integer.MIN_VALUE;
    private int lastCountdown;
    private boolean lastStarting;
    private boolean initialized;

    public SoyuzLauncherVisual(
            VisualizationContext context, SoyuzLauncherBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f bodyPose = new Matrix4f().translate(.5F, (float) -MESH_DROP, .5F);
        HFRWavefrontObject[] body = {
            ResourceManager.soyuz_launcher_legs,
            ResourceManager.soyuz_launcher_table,
            ResourceManager.soyuz_launcher_tower_base,
            ResourceManager.soyuz_launcher_support_base
        };
        AABB extent = new AABB(pos).inflate(1);
        for (HFRWavefrontObject mesh : body)
            extent = extent.minmax(LightBounds.of(mesh.groups[0], bodyPose, pos));
        bodyBounds = extent;
        arms =
                new ChainParts(
                        context,
                        visualPos,
                        bodyPose,
                        new Model[] {SOYUZ_LAUNCHER_TOWER.model(), SOYUZ_LAUNCHER_SUPPORT.model()},
                        new ChainInstance.Chain[] {TOWER_CHAIN, SUPPORT_CHAIN},
                        1);
        Matrix4f localRocketPose = new Matrix4f(bodyPose).translate(0F, (float) ROCKET_LIFT, 0F);
        rocketPose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(localRocketPose);
        double[] lightBounds = new double[6];
        LightBounds.resetBounds(lightBounds, bodyBounds);
        arms.includeLightBounds(lightBounds, pos);
        for (MeshPart part : ROCKETS[0]) {
            LightBounds.includeLightBounds(lightBounds, part.model(), localRocketPose, pos);
        }
        LightBounds.sections(lightSections, lightBounds, null);
        updateMovingParts(partialTick);
    }

    private static PackedQuadMesh[] buildRocketMeshes() {
        var model = ResourceManager.soyuz;
        var meshes =
                new PackedQuadMesh
                        [SINGLE.length
                                + 1
                                + BOOSTERS.length
                                + BOOSTER_ENGINES.length
                                + BOOSTER_SIDES.length];
        int at = 0;
        for (String name : SINGLE)
            meshes[at++] = PackedQuadMesh.of(model.groups[model.partId(name)], model.smoothing());
        meshes[at++] = PackedQuadMesh.of(model.groups[model.partId("Memento")], model.smoothing());
        for (String name : BOOSTERS)
            meshes[at++] = PackedQuadMesh.of(model.groups[model.partId(name)], model.smoothing());
        for (String name : BOOSTER_ENGINES)
            meshes[at++] = PackedQuadMesh.of(model.groups[model.partId(name)], model.smoothing());
        for (String name : BOOSTER_SIDES)
            meshes[at++] = PackedQuadMesh.of(model.groups[model.partId(name)], model.smoothing());
        return meshes;
    }

    private static MeshPart[][] buildRockets() {
        var result = new MeshPart[ResourceManager.soyuz_skin_tex.length][];
        for (int i = 0; i < result.length; i++) result[i] = buildRocket(i);
        return result;
    }

    private static MeshPart rawPart(HFRWavefrontObject model, int group, Identifier texture) {
        return MeshPart.obj(
                model.groups[group],
                model.smoothing(),
                SimpleMaterial.builderOf(MeshPart.litCutout(texture))
                        .backfaceCulling(false)
                        .build());
    }

    private static MeshPart[] buildRocket(int skinIndex) {
        Identifier[] skin = ResourceManager.soyuz_skin_tex[skinIndex];
        MeshPart[] pieces = new MeshPart[ROCKET_MESHES.length];
        int at = 0;
        for (int i = 0; i < SINGLE.length; i++)
            pieces[at] = MeshPart.create(ROCKET_MESHES[at++], MeshPart.litCutout(skin[i]));
        pieces[at] =
                MeshPart.create(
                        ROCKET_MESHES[at++], MeshPart.litCutout(ResourceManager.soyuz_memento_tex));
        for (int i = 0; i < BOOSTERS.length; i++)
            pieces[at] = MeshPart.create(ROCKET_MESHES[at++], MeshPart.litCutout(skin[9]));
        for (int i = 0; i < BOOSTER_ENGINES.length; i++)
            pieces[at] = MeshPart.create(ROCKET_MESHES[at++], MeshPart.litCutout(skin[7]));
        for (int i = 0; i < BOOSTER_SIDES.length; i++)
            pieces[at] = MeshPart.create(ROCKET_MESHES[at++], MeshPart.litCutout(skin[10]));
        return pieces;
    }

    public static void initModels() {}

    private TransformedInstance piece(MeshPart part) {
        return instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, part.model())
                .createInstance();
    }

    private @Nullable RocketSet rocket(int skinIndex) {
        if (skinIndex < 0 || skinIndex >= ROCKETS.length) return null;
        MeshPart[] parts = ROCKETS[skinIndex];
        TransformedInstance[] pieces = new TransformedInstance[parts.length];
        for (int i = 0; i < parts.length; i++) pieces[i] = piece(parts[i]);
        return new RocketSet(skinIndex, pieces);
    }

    private void syncRocket(int skinIndex) {
        if (rocket != null && rocket.skin == skinIndex) return;
        if (rocket != null) rocket.delete();
        rocket = rocket(skinIndex);
        if (rocket != null) {
            for (TransformedInstance piece : rocket.pieces) {
                piece.setTransform(rocketPose).light(0).setChanged();
            }
        }
    }

    @Override
    public void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        int rocketType = blockEntity.rocketType;
        int countdown = blockEntity.countdown;
        boolean starting = blockEntity.starting;
        boolean rocketChanged = !initialized || rocketType != lastRocketType;
        if (rocketChanged) syncRocket(rocketType);
        if (!rocketChanged && countdown == lastCountdown && starting == lastStarting) return;
        lastRocketType = rocketType;
        lastCountdown = countdown;
        lastStarting = starting;
        initialized = true;
        float from = rocketType >= 0 ? 0F : SWING_TICKS;
        float to = from;
        if (starting && countdown < SWING_TICKS) {
            from = SWING_TICKS - countdown;
            to = from + 1F;
        }
        arms.set(0, from, to);
        arms.write();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 7,
                                pos.getY() - 4,
                                pos.getZ() - 9,
                                pos.getX() + 8,
                                pos.getY() + 58,
                                pos.getZ() + 10)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        arms.collectCrumblingInstances(consumer);
        if (rocket != null) for (TransformedInstance piece : rocket.pieces) consumer.accept(piece);
    }

    @Override
    protected void _delete() {
        arms.delete();
        if (rocket != null) rocket.delete();
    }

    private static final class RocketSet {
        private final int skin;
        private final TransformedInstance[] pieces;

        private RocketSet(int skin, TransformedInstance[] pieces) {
            this.skin = skin;
            this.pieces = pieces;
        }

        private void delete() {
            for (TransformedInstance piece : pieces) piece.delete();
        }
    }
}
