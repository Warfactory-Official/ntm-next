// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.lib.Library;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.BillboardInstance;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.util.OverlayTexture;
import java.util.Random;
import java.util.function.ToIntFunction;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import org.joml.FrustumIntersection;
import org.joml.Vector3f;

public final class MultiCloudVisual<T extends Entity> extends HbmDynamicEntityVisual<T> {
    private static final int STAGES = 8;
    private static final int PUFFS = 5;
    private static final int WHITE = ARGB.color(255, 255, 255, 255);
    private static final float[] OFFSET = new float[PUFFS * 3];
    private static final float[] SIZE = new float[PUFFS];
    private static final float REACH;
    private static final Mesh QUAD = quad();
    static final Model[] ORANGE = buildModels("orange");
    static final Model[] CHLORINE = buildModels("chlorine");
    static final Model[] CLOUD = buildModels("cloud");
    static final Model[] PINK = buildModels("pc");

    static {
        Random spread = new Random(100);
        float reach = 0F;
        for (int i = 0; i < PUFFS; i++) {
            float x = (float) ((spread.nextGaussian() - 1D) * 0.15D);
            float y = (float) ((spread.nextGaussian() - 1D) * 0.15D);
            float z = (float) ((spread.nextGaussian() - 1D) * 0.15D);
            float scale = (float) ((spread.nextDouble() * 0.5D + 0.25D) * 3.75D);
            OFFSET[i * 3] = x * 3.75F;
            OFFSET[i * 3 + 1] = y * 3.75F + scale * 0.25F;
            OFFSET[i * 3 + 2] = z * 3.75F;
            SIZE[i] = scale;
            float ox = OFFSET[i * 3], oy = OFFSET[i * 3 + 1], oz = OFFSET[i * 3 + 2];
            reach =
                    Math.max(
                            reach,
                            (float) Math.sqrt(ox * ox + oy * oy + oz * oz)
                                    + scale * (float) Math.sqrt(2D));
        }
        REACH = reach;
    }

    private final Vector3f interpolatedPosition = new Vector3f();
    private final Vector3f lastPosition = new Vector3f(Float.NaN);
    private final ToIntFunction<T> age;
    private final ToIntFunction<T> maxAge;
    private final BillboardInstance[] puffs = new BillboardInstance[STAGES * PUFFS];
    private final int[] brightness = new int[PUFFS];
    private int live = -1;
    private int lastLight;

    public MultiCloudVisual(
            VisualizationContext ctx,
            T entity,
            float partialTick,
            Model[] models,
            ToIntFunction<T> age,
            ToIntFunction<T> maxAge) {
        super(ctx, entity, partialTick);
        this.age = age;
        this.maxAge = maxAge;

        Random shade = new Random(entity.hashCode());
        for (int i = 0; i < PUFFS; i++) {
            float value = 1F - shade.nextInt(10) * 0.05F;
            brightness[i] = ARGB.colorFromFloat(1F, value, value, value);
        }

        for (int stage = 0; stage < STAGES; stage++) {
            for (int i = 0; i < PUFFS; i++) {
                BillboardInstance instance =
                        instancerProvider()
                                .instancer(InstanceTypes.BILLBOARD, models[stage])
                                .createInstance();
                instance.uvRegion(0F, 0F, 1F, 1F).overlay(OverlayTexture.NO_OVERLAY);
                instance.setVisible(false);
                puffs[stage * PUFFS + i] = instance;
            }
        }

        writeFrame(partialTick);
    }

    public static void initModels() {}

    private static Model[] buildModels(String name) {
        Model[] models = new Model[STAGES];
        for (int i = 0; i < STAGES; i++) {
            models[i] =
                    new SingleMeshModel(
                            QUAD,
                            SimpleMaterial.builderOf(Materials.CUTOUT)
                                    .texture(Library.id("textures/item/" + name + (i + 1) + ".png"))
                                    .mipmap(false)
                                    .cutout(CutoutShaders.ONE_TENTH)
                                    .useOverlay(false)
                                    .cardinalLightingMode(CardinalLightingMode.ENTITY)
                                    .build());
        }
        return models;
    }

    private static Mesh quad() {
        return PackedQuadMesh.builder(1)
                .normal(0F, 1F, 0F)
                .vertex(-0.5D, -0.5D, 0D, 0D, 1D, WHITE)
                .vertex(0.5D, -0.5D, 0D, 1D, 1D, WHITE)
                .vertex(0.5D, 0.5D, 0D, 1D, 0D, WHITE)
                .vertex(-0.5D, 0.5D, 0D, 0D, 0D, WHITE)
                .build();
    }

    @Override
    public boolean isVisible(FrustumIntersection frustum) {
        return sphereVisible(frustum, 0F, REACH);
    }

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        int max = maxAge.applyAsInt(entity);
        int stage = -1;
        if (max > 0) {
            int eighth = max / 8;
            stage = eighth <= 0 ? 0 : Math.clamp(age.applyAsInt(entity) / eighth, 0, 7);
        }

        boolean switched = stage != live;
        if (switched && live >= 0)
            for (int i = 0; i < PUFFS; i++) puffs[live * PUFFS + i].setVisible(false);
        live = stage;
        if (stage < 0) return;

        Vector3f visualPos = getVisualPosition(partialTick, interpolatedPosition);
        int light = computePackedLight(partialTick);
        if (!switched && light == lastLight && visualPos.equals(lastPosition)) return;
        lastLight = light;
        lastPosition.set(visualPos);
        for (int i = 0; i < PUFFS; i++) {
            BillboardInstance instance = puffs[stage * PUFFS + i];
            if (switched) {
                instance.setVisible(true);
                instance.size(SIZE[i]).colorArgb(brightness[i]);
            }
            instance.position(
                    visualPos.x + OFFSET[i * 3],
                    visualPos.y + OFFSET[i * 3 + 1],
                    visualPos.z + OFFSET[i * 3 + 2]);
            instance.light(light);
            instance.setChanged();
        }
    }

    @Override
    protected void _delete() {
        for (BillboardInstance instance : puffs) instance.delete();
    }
}
